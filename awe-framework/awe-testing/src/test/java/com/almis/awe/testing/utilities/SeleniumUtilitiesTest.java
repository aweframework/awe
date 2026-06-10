package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SeleniumUtilitiesTest {

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private SeleniumUtilities seleniumUtilities;

  @BeforeEach
  void setUp() throws Exception {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));

    File screenshot = Files.createTempFile(tempDir, "selenium-timeout", ".png").toFile();
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(screenshot);

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(150));

    SeleniumModel seleniumModel = new SeleniumModel()
      .setDriver(driver)
      .setCurrentOption("unit-test")
      .setProperties(properties);

    AngularAweInstructions frontEndInstructions = new AngularAweInstructions();
    frontEndInstructions.setSeleniumModel(seleniumModel);

    seleniumUtilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(seleniumUtilities, "properties", properties);
    ReflectionTestUtils.setField(seleniumUtilities, "seleniumModel", seleniumModel);
    ReflectionTestUtils.setField(seleniumUtilities, "frontEndInstructions", frontEndInstructions);
  }

  @Test
  void shouldFailWithinConfiguredTimeoutWhenConditionNeverBecomesTrue() {
    ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver ignored) {
        return false;
      }

      @Override
      public String toString() {
        return "Slow path bounded timeout";
      }
    };

    long startedAt = System.nanoTime();

    AssertionFailedError error = assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitUntil", condition));

    long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

    assertThat(error).hasMessageContaining("Slow path bounded timeout");
    assertThat(elapsedMillis).isLessThan(1500L);
  }

  @Test
  void shouldReturnQuicklyWhenConditionIsImmediatelySatisfied() {
    ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver ignored) {
        return true;
      }

      @Override
      public String toString() {
        return "Fast path immediate success";
      }
    };

    long startedAt = System.nanoTime();

    ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitUntil", condition);

    long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

    assertThat(elapsedMillis).isLessThan(200L);
  }

  @Test
  void shouldReportLoginActionabilityStageWhenInputNeverBecomesActionable() {
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing login input"));

    AssertionFailedError error = assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForInputActionability", "cod_usr"));

    assertThat(error)
      .hasMessageContaining("Input actionability [cod_usr]")
      .hasMessageContaining("selector")
      .hasMessageContaining("criterion-id='cod_usr'");
  }

  @Test
  void shouldWaitForLoginSubmitClickability() {
    By loginButtonSelector = By.cssSelector("#ButLogIn:not([disabled])");
    WebElement loginButton = mockVisibleElement("", true);

    when(driver.findElement(argThat(loginButtonSelector::equals))).thenReturn(loginButton);

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForButtonClickability", "ButLogIn"))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldReportAuthenticatedShellStageWhenPostLoginReadinessNeverArrives() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing authenticated shell"));

    AssertionFailedError error = assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities,
        "waitForAuthenticatedShell",
        avatarTextSelector,
        "Manager (test)"));

    assertThat(error)
      .hasMessageContaining("Authenticated shell readiness")
      .hasMessageContaining("#ButUsrAct span.avatar-text")
      .hasMessageContaining("visible frontend shell controls are actionable");
  }

  @Test
  void shouldKeepAuthenticatedShellNotReadyUntilShellControlsAppear() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    WebElement avatarText = mockVisibleElement("Manager (test)", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);

    ExpectedCondition<Boolean> condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.apply(driver)).isFalse();
  }

  @Test
  void shouldRequireActionableShellControlsBeforeAuthenticatedShellIsReady() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = By.id("ButUsrAct");
    By menuToggleSelector = By.id("main-menu-toggle");
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    WebElement menuToggle = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElement(argThat(menuToggleSelector::equals))).thenReturn(menuToggle);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));
    when(driver.findElements(argThat(menuToggleSelector::equals))).thenReturn(List.of(menuToggle));

    ExpectedCondition<Boolean> condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.apply(driver)).isTrue();
  }

  @Test
  void shouldBlockShellReadinessWhenVisibleOptionalControlIsNotActionable() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = By.id("ButUsrAct");
    By logoutSelector = By.id("ButLogOut");
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    WebElement logoutButton = mockVisibleElement("", false);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElement(argThat(logoutSelector::equals))).thenReturn(logoutButton);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));
    when(driver.findElements(argThat(logoutSelector::equals))).thenReturn(List.of(logoutButton));

    ExpectedCondition<Boolean> condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.apply(driver)).isFalse();
  }

  @Test
  void shouldTreatAbsentAngularMenuControlsAsOptionalForShellReadiness() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = By.id("ButUsrAct");
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));

    ExpectedCondition<Boolean> condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.apply(driver)).isTrue();
  }

  @Test
  void shouldUseFrontendSpecificShellSelectorsInsteadOfHardcodedAngularSelectors() {
    AweTestConfigProperties reactProperties = new AweTestConfigProperties();
    reactProperties.setFrontend(FrontendType.REACT);
    reactProperties.setStartUrl("http://localhost:8080/");
    reactProperties.setScreenshotPath(tempDir.toString());
    reactProperties.setTimeout(Duration.ofMillis(150));

    SeleniumModel reactModel = new SeleniumModel()
      .setDriver(driver)
      .setCurrentOption("react-unit-test")
      .setProperties(reactProperties);

    ReactAweInstructions reactInstructions = new ReactAweInstructions();
    reactInstructions.setSeleniumModel(reactModel);

    ReflectionTestUtils.setField(seleniumUtilities, "properties", reactProperties);
    ReflectionTestUtils.setField(seleniumUtilities, "seleniumModel", reactModel);
    ReflectionTestUtils.setField(seleniumUtilities, "frontEndInstructions", reactInstructions);

    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = By.id("ButUsrAct");
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));

    ExpectedCondition<Boolean> condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.apply(driver)).isTrue();
  }

  @Test
  void shouldWaitForActionableLoginInputsBeforeTypingCredentialsInCheckLogin() {
    TrackingSeleniumUtilities trackingUtilities = new TrackingSeleniumUtilities();
    AweTestConfigProperties trackingProperties = new AweTestConfigProperties();
    trackingProperties.setFrontend(FrontendType.ANGULAR);
    trackingProperties.setStartUrl("http://localhost:8080/");
    trackingProperties.setScreenshotPath(tempDir.toString());
    trackingProperties.setTimeout(Duration.ofMillis(800));

    SeleniumModel trackingModel = new SeleniumModel()
      .setDriver(driver)
      .setCurrentOption("unit-test")
      .setProperties(trackingProperties);

    AngularAweInstructions trackingInstructions = new AngularAweInstructions();
    trackingInstructions.setSeleniumModel(trackingModel);

    ReflectionTestUtils.setField(trackingUtilities, "properties", trackingProperties);
    ReflectionTestUtils.setField(trackingUtilities, "seleniumModel", trackingModel);
    ReflectionTestUtils.setField(trackingUtilities, "frontEndInstructions", trackingInstructions);

    By usernameSelector = By.cssSelector("[criterion-id='cod_usr'] input,[criterion-id='cod_usr'] textarea");
    By passwordSelector = By.cssSelector("[criterion-id='pwd_usr'] input,[criterion-id='pwd_usr'] textarea");
    By loginButtonSelector = By.cssSelector("#ButLogIn:not([disabled])");
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = By.id("ButUsrAct");
    WebElement usernameInput = mockVisibleElement("", true);
    WebElement passwordInput = mockVisibleElement("", true);
    WebElement loginButton = mockVisibleElement("", true);
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    AtomicInteger usernamePolls = new AtomicInteger();
    AtomicInteger passwordPolls = new AtomicInteger();

    when(usernameInput.isEnabled()).thenAnswer(invocation -> usernamePolls.incrementAndGet() >= 2);
    when(passwordInput.isEnabled()).thenAnswer(invocation -> passwordPolls.incrementAndGet() >= 2);
    when(driver.findElement(argThat(usernameSelector::equals))).thenReturn(usernameInput);
    when(driver.findElement(argThat(passwordSelector::equals))).thenReturn(passwordInput);
    when(driver.findElement(argThat(loginButtonSelector::equals))).thenReturn(loginButton);
    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));

    trackingUtilities.runCheckLogin("test", "test", "#ButUsrAct span.avatar-text", "Manager (test)");

    assertThat(trackingUtilities.events).containsExactly(
      "write:cod_usr:true:test",
      "write:pwd_usr:true:test",
      "click:ButLogIn:true",
      "check:#ButUsrAct span.avatar-text:Manager (test)"
    );
  }

  private WebElement mockVisibleElement(String text, boolean enabled) {
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(enabled);
    when(element.getText()).thenReturn(text);
    return element;
  }

  private static class TrackingSeleniumUtilities extends SeleniumUtilities {
    private final List<String> events = new ArrayList<>();

    void runCheckLogin(String username, String password, String cssSelector, String checkText) {
      checkLogin(username, password, cssSelector, checkText);
    }

    @Override
    protected void goToUrl(String url) {
      // No-op for focused checkLogin coverage.
    }

    @Override
    protected void setTestTitle(String title) {
      // No-op for focused checkLogin coverage.
    }

    @Override
    protected void waitForLoadingBar() {
      // No-op for focused checkLogin coverage.
    }

    @Override
    protected void writeText(String criterionName, CharSequence text, boolean clearText) {
      By selector = getFrontEndInstructions().getCriterionInput(getFrontEndInstructions().getCriterionCss(criterionName));
      boolean enabled = getDriver().findElement(selector).isEnabled();
      events.add("write:" + criterionName + ":" + enabled + ":" + text);
    }

    @Override
    protected void clickButton(String buttonId, boolean wait) {
      events.add("click:" + buttonId + ":" + wait);
    }

    @Override
    protected void checkText(String cssSelector, String checkText) {
      events.add("check:" + cssSelector + ":" + checkText);
    }

    private IAweFrontEndInstructions getFrontEndInstructions() {
      return (IAweFrontEndInstructions) ReflectionTestUtils.getField(this, "frontEndInstructions");
    }
  }
}
