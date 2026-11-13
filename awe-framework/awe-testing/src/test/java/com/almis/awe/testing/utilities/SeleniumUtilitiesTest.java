package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.extensions.FailureEvidence;
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
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
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

    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});

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
    BrowserCondition condition = BrowserCondition.of("Slow path bounded timeout", browser -> false);

    long startedAt = System.nanoTime();

    AssertionFailedError error = assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitUntil", condition));

    long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

    assertThat(error).hasMessageContaining("Slow path bounded timeout");
    assertThat(elapsedMillis).isLessThan(1500L);
  }

  @Test
  void shouldNotWriteOnSearchBoxThatExistsButIsHidden() {
    WebElement hiddenSearch = mock(WebElement.class);
    when(hiddenSearch.isDisplayed()).thenReturn(false);
    WebElement visibleSearch = mock(WebElement.class);
    when(visibleSearch.isDisplayed()).thenReturn(true);
    By hidden = By.cssSelector("[data-testid='hidden-search']");
    By visible = By.cssSelector("[data-testid='visible-search']");
    doReturn(hiddenSearch).when(driver).findElement(hidden);
    doReturn(visibleSearch).when(driver).findElement(visible);

    assertThat((Boolean) ReflectionTestUtils.invokeMethod(seleniumUtilities, "isWritable", hidden)).isFalse();
    assertThat((Boolean) ReflectionTestUtils.invokeMethod(seleniumUtilities, "isWritable", visible)).isTrue();
    assertThat((Boolean) ReflectionTestUtils.invokeMethod(seleniumUtilities, "isWritable", By.id("missing"))).isFalse();
  }

  @Test
  void shouldMarkScreenshotTakenAndStoreErrorNamedFileWhenAssertionFails() throws Exception {
    SeleniumModel model = (SeleniumModel) ReflectionTestUtils.getField(seleniumUtilities, "seleniumModel");
    assertThat(model.isScreenshotTaken()).isFalse();

    assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "assertWithScreenshot", "Some failure", false,
        new Throwable[0]));

    assertThat(model.isScreenshotTaken()).isTrue();
    try (java.util.stream.Stream<Path> files = Files.list(tempDir)) {
      assertThat(files.map(path -> path.getFileName().toString()))
        .anyMatch(name -> name.startsWith("SeleniumUtilities-") && name.contains("-[ERROR]-unit_test-some_failure")
          && name.endsWith(".png"));
    }
  }

  @Test
  void shouldPrintExactlyOneAttachmentMarkerWhenAssertionFails() {
    ByteArrayOutputStream console = new ByteArrayOutputStream();
    Map<String, String> environment = Map.of("CI_PROJECT_DIR", tempDir.getParent().toString());
    ReflectionTestUtils.setField(seleniumUtilities, "failureEvidence", new FailureEvidence(Clock.systemDefaultZone(),
      environment::get, new PrintStream(console, true, StandardCharsets.UTF_8)));

    assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "assertWithScreenshot", "Some failure", false,
        new Throwable[0]));

    String output = new String(console.toByteArray(), StandardCharsets.UTF_8);
    assertThat(output.split("\\[\\[ATTACHMENT\\|", -1)).hasSize(2);
    assertThat(output).contains("[[ATTACHMENT|" + tempDir.getFileName() + "/SeleniumUtilities-")
      .contains("Failure screenshot: ");
  }

  @Test
  void shouldNotMarkScreenshotTakenWhenTheFileCannotBeStored() throws Exception {
    // A regular file where the screenshot directory should be makes createDirectories fail
    Path notADirectory = Files.createFile(tempDir.resolve("not-a-directory"));
    AweTestConfigProperties properties = (AweTestConfigProperties) ReflectionTestUtils
      .getField(seleniumUtilities, "properties");
    properties.setScreenshotPath(notADirectory.toString());
    SeleniumModel model = (SeleniumModel) ReflectionTestUtils.getField(seleniumUtilities, "seleniumModel");

    assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "assertWithScreenshot", "Some failure", false,
        new Throwable[0]));

    assertThat(model.isScreenshotTaken()).isFalse();
  }

  @Test
  void shouldReturnQuicklyWhenConditionIsImmediatelySatisfied() {
    BrowserCondition condition = BrowserCondition.of("Fast path immediate success", browser -> true);

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

    BrowserCondition condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.isMet(seleniumUtilitiesBrowser())).isFalse();
  }

  @Test
  void shouldRequireActionableShellControlsBeforeAuthenticatedShellIsReady() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = portSelector(By.id("ButUsrAct"));
    By menuToggleSelector = portSelector(By.id("main-menu-toggle"));
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    WebElement menuToggle = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElement(argThat(menuToggleSelector::equals))).thenReturn(menuToggle);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));
    when(driver.findElements(argThat(menuToggleSelector::equals))).thenReturn(List.of(menuToggle));

    BrowserCondition condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.isMet(seleniumUtilitiesBrowser())).isTrue();
  }

  @Test
  void shouldBlockShellReadinessWhenVisibleOptionalControlIsNotActionable() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = portSelector(By.id("ButUsrAct"));
    By logoutSelector = portSelector(By.id("ButLogOut"));
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    WebElement logoutButton = mockVisibleElement("", false);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElement(argThat(logoutSelector::equals))).thenReturn(logoutButton);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));
    when(driver.findElements(argThat(logoutSelector::equals))).thenReturn(List.of(logoutButton));

    BrowserCondition condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.isMet(seleniumUtilitiesBrowser())).isFalse();
  }

  @Test
  void shouldTreatAbsentAngularMenuControlsAsOptionalForShellReadiness() {
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = portSelector(By.id("ButUsrAct"));
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));

    BrowserCondition condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.isMet(seleniumUtilitiesBrowser())).isTrue();
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

    // The React shell shows the logged user with an avatar (it carries the id) and its name, both with a test hook
    By avatarTextSelector = By.cssSelector("[data-testid='avatar-name']");
    By userActionSelector = portSelector(reactInstructions.getRequiredPostLoginShellControls().get(0));
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);

    when(driver.findElement(argThat(avatarTextSelector::equals))).thenReturn(avatarText);
    when(driver.findElement(argThat(userActionSelector::equals))).thenReturn(userAction);
    when(driver.findElements(argThat(userActionSelector::equals))).thenReturn(List.of(userAction));

    BrowserCondition condition = ReflectionTestUtils.invokeMethod(
      seleniumUtilities,
      "authenticatedShellReady",
      avatarTextSelector,
      "Manager (test)");

    assertThat(condition.isMet(seleniumUtilitiesBrowser())).isTrue();
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

    By usernameSelector = By.cssSelector("[criterion-id='cod_usr'] [data-testid='criterion-input']");
    By passwordSelector = By.cssSelector("[criterion-id='pwd_usr'] [data-testid='criterion-input']");
    By loginButtonSelector = By.cssSelector("#ButLogIn:not([disabled])");
    By avatarTextSelector = By.cssSelector("#ButUsrAct span.avatar-text");
    By userActionSelector = portSelector(By.id("ButUsrAct"));
    WebElement usernameInput = mockVisibleElement("", true);
    WebElement passwordInput = mockVisibleElement("", true);
    WebElement loginButton = mockVisibleElement("", true);
    WebElement avatarText = mockVisibleElement("Manager (test)", true);
    WebElement userAction = mockVisibleElement("", true);
    AtomicInteger usernamePolls = new AtomicInteger();
    AtomicInteger passwordPolls = new AtomicInteger();

    when(usernameInput.isEnabled()).thenAnswer(invocation -> usernamePolls.incrementAndGet() >= 2);
    when(passwordInput.isEnabled()).thenAnswer(invocation -> passwordPolls.incrementAndGet() >= 2);
    // The fields keep what was typed, so the login form is not filled again
    when(usernameInput.getAttribute("value")).thenReturn("test");
    when(passwordInput.getAttribute("value")).thenReturn("test");
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

  @Test
  void shouldWaitForTheCurrentOptionInsteadOfTheMenuDropdownWhenTheClientKeepsItsMenuOpen() {
    useReactInstructions();
    // The React side menu does not collapse after a click: its nested options stay displayed
    WebElement openSubmenu = mockVisibleElement("", true);
    when(driver.findElements(argThat(By.cssSelector("[data-testid='menu-submenu']")::equals)))
      .thenReturn(List.of(openSubmenu));
    By activeOption = By.cssSelector("[data-testid='menu-option'][option-name='criteria-test'][data-active='true']");
    WebElement activeElement = mockVisibleElement("Criteria test", true);
    when(driver.findElement(argThat(activeOption::equals))).thenReturn(activeElement);
    when(driver.findElements(argThat(activeOption::equals))).thenReturn(List.of(activeElement));

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForMenuOption", "criteria-test"))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldFailWhenTheScreenOfTheOptionNeverBecomesTheCurrentOne() {
    useReactInstructions();

    assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForMenuOption", "criteria-test"));
  }

  @Test
  void shouldKeepWaitingForTheMenuDropdownToCloseWhenTheClientCollapsesItsMenu() {
    IAweFrontEndInstructions angular = (IAweFrontEndInstructions) ReflectionTestUtils
      .getField(seleniumUtilities, "frontEndInstructions");
    assertThat(angular.getMenuActiveOption("criteria-test")).isNull();

    // The dropdown is closed
    assertThatCode(() -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForMenuOption", "criteria-test"))
      .doesNotThrowAnyException();

    // The dropdown stays open
    WebElement openDropdown = mockVisibleElement("", true);
    when(driver.findElements(any(By.class))).thenReturn(List.of(openDropdown));
    when(driver.findElement(any(By.class))).thenReturn(openDropdown);
    assertThrows(AssertionFailedError.class,
      () -> ReflectionTestUtils.invokeMethod(seleniumUtilities, "waitForMenuOption", "criteria-test"));
  }

  private void useReactInstructions() {
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
  }

  private BrowserDriver seleniumUtilitiesBrowser() {
    return ((SeleniumModel) ReflectionTestUtils.getField(seleniumUtilities, "seleniumModel")).getBrowser();
  }

  /**
   * The port looks an id up as the equivalent css selector
   */
  private static By portSelector(By selector) {
    return Locator.from(selector).toBy();
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
