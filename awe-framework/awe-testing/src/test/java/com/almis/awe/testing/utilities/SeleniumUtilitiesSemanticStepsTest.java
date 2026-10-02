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
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * The semantic steps of {@link SeleniumUtilities}: each one asks the front end instructions for what it needs, so the
 * tests that use it contain no selector.
 */
class SeleniumUtilitiesSemanticStepsTest {

  @TempDir
  Path tempDir;

  private WebDriver driver;
  private RecordingSeleniumUtilities utilities;
  private IAweFrontEndInstructions instructions;
  private AweTestConfigProperties properties;
  private SeleniumModel model;

  @BeforeEach
  void setUp() throws Exception {
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));
    File screenshot = Files.createTempFile(tempDir, "semantic-steps", ".png").toFile();
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(screenshot);

    properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(150));
    model = new SeleniumModel().setDriver(driver).setCurrentOption("unit-test").setProperties(properties);

    use(new AngularAweInstructions());
  }

  private void use(IAweFrontEndInstructions frontEndInstructions) {
    frontEndInstructions.setSeleniumModel(model);
    instructions = frontEndInstructions;
    utilities = new RecordingSeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", frontEndInstructions);
  }

  private WebElement show(By selector, String text) {
    WebElement element = mock(WebElement.class);
    when(element.isDisplayed()).thenReturn(true);
    when(element.isEnabled()).thenReturn(true);
    when(element.getText()).thenReturn(text);
    when(driver.findElement(argThat(selector::equals))).thenReturn(element);
    when(driver.findElements(argThat(selector::equals))).thenReturn(List.of(element));
    return element;
  }

  @Test
  void shouldCheckTheTitleAndTheTextOfAMessageThroughTheInstructions() {
    show(instructions.getMessageTitle("warning"), "Invalid credentials");
    show(instructions.getMessageText("warning"), "The credentials are not valid");

    assertThatCode(() -> utilities.checkMessageTitle("warning", "Invalid credentials")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkMessageText("warning", "The credentials are not valid"))
      .doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkMessageTitle("warning", "Other title"));
  }

  @Test
  void shouldFailWhenTheMessageIsNeverShown() {
    assertThrows(AssertionFailedError.class, () -> utilities.checkMessageTitle("danger", "Title"));
  }

  @Test
  void shouldCheckTheStateOfAButtonWithoutItsSelector() {
    show(instructions.getAnyButton("ButOk"), "");
    show(instructions.getDisabledButton("ButOff"), "");

    assertThatCode(() -> utilities.checkButtonVisible("ButOk")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkButtonNotVisible("ButGone")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkButtonDisabled("ButOff")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkButtonNotVisible("ButOk"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkButtonDisabled("ButOk"));
  }

  @Test
  void shouldWaitForTheLogViewerToShowTheText() {
    show(instructions.getLogViewer(), "[SCHEDULER] started");

    assertThatCode(() -> utilities.checkLogViewerContains("[SCHEDULER]")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkLogViewerContains("[OTHER]"));
  }

  @Test
  void shouldCheckTheCriterionLabelAndUnitAndTheActiveWizardStep() {
    show(instructions.getCriterionLabel("Unt"), "Texto Normal");
    show(instructions.getCriterionUnit("Unt"), "EUR");
    show(instructions.getActiveWizardStepNumber(), "2");

    assertThatCode(() -> utilities.checkCriterionLabel("Unt", "Texto Normal")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkCriterionUnit("Unt", "EUR")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkActiveWizardStep("2")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkActiveWizardStep("3"));
  }

  @Test
  void shouldCheckAMenuOptionAndATagListByTheirText() {
    show(instructions.getMenuOptionItem("test"), "Tests Charts");
    show(instructions.getTagList("tags"), "Manager (test)");

    assertThatCode(() -> utilities.checkMenuOption("test", "Tests")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkTagListContains("tags", "Manager")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkMenuOption("test", "Missing"));
  }

  @Test
  void shouldCheckTheGridsTheTreeRowsAndTheirIcons() {
    show(instructions.getGrid("Grd"), "");
    show(instructions.getGridHeaderCheckboxSelected("Grd"), "");
    show(instructions.getTreeRow("Tre", "R1"), "");
    show(instructions.getTreeRowIcon("Tre", "R1"), "");
    show(instructions.getDeletedTreeRow("Tre", "R2"), "");
    show(instructions.getGridIcon("Grd", "Ico", "plus"), "");
    show(instructions.getColumnSuccessIcon("Sta"), "");

    assertThatCode(() -> {
      utilities.checkGridPresent("Grd");
      utilities.checkAllRowsSelected("Grd");
      utilities.checkTreeRowVisible("Tre", "R1");
      utilities.checkTreeRowNotVisible("Tre", "Gone");
      utilities.checkTreeIconVisible("Tre", "R1");
      utilities.checkTreeIconNotVisible("Tre", "Gone");
      utilities.checkTreeRowDeleted("Tre", "R2");
      utilities.checkGridIconVisible("Grd", "Ico", "plus");
      utilities.checkColumnSuccessIcon("Sta");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridIconVisible("Grd", "Ico", "trash"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkTreeRowNotVisible("Tre", "R1"));
  }

  @Test
  void shouldCheckThatTheContextMenuAndTheDialogAreClosed() {
    show(instructions.getOpenDialog("Open"), "");

    assertThatCode(() -> {
      utilities.checkContextMenuNotVisible();
      utilities.checkDialogClosed("Closed");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkDialogClosed("Open"));
  }

  @Test
  void shouldReadThePageSizeOfANativeSelectFromItsSelectedOption() {
    WebElement pageSize = show(instructions.getGridPageSize(), "10 25 50");
    when(pageSize.getTagName()).thenReturn("select");
    WebElement ten = option("10", false);
    WebElement twentyFive = option("25", true);
    when(pageSize.findElements(By.tagName("option"))).thenReturn(List.of(ten, twentyFive));

    assertThatCode(() -> utilities.checkGridPageSize("25")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridPageSize("10"));
  }

  @Test
  void shouldReadThePageSizeOfAnyOtherElementFromItsText() {
    WebElement pageSize = show(instructions.getGridPageSize(), "25");
    when(pageSize.getTagName()).thenReturn("div");

    assertThatCode(() -> utilities.checkGridPageSize("25")).doesNotThrowAnyException();
  }

  @Test
  void shouldCheckTheNumberOfResultsOfTheOpenList() {
    show(instructions.getSelectOption(1), "asp");

    assertThatCode(() -> utilities.checkSuggestResultCount(1)).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkSuggestResultCount(0)).isInstanceOf(AssertionFailedError.class);
    show(instructions.getSelectOption(2), "other");
    assertThrows(AssertionFailedError.class, () -> utilities.checkSuggestResultCount(1));
  }

  @Test
  void shouldReadTheEmbeddedApplicationInsideItsFrameAndComeBack() {
    WebDriver.TargetLocator locator = mock(WebDriver.TargetLocator.class);
    when(driver.switchTo()).thenReturn(locator);
    WebElement frame = show(instructions.getEmbeddedFrame(), "");
    show(By.cssSelector("ol.breadcrumb a"), "angular-filemanager");

    utilities.checkTextInEmbeddedFrame("ol.breadcrumb a", "angular-filemanager");

    var order = inOrder(locator);
    order.verify(locator).frame(frame);
    order.verify(locator).defaultContent();
  }

  @Test
  void shouldComeBackFromTheFrameWhenTheTextIsNotTheExpectedOne() {
    WebDriver.TargetLocator locator = mock(WebDriver.TargetLocator.class);
    when(driver.switchTo()).thenReturn(locator);
    show(instructions.getEmbeddedFrame(), "");
    show(By.cssSelector("ol.breadcrumb a"), "other");

    assertThrows(AssertionFailedError.class, () -> utilities.checkTextInEmbeddedFrame("ol.breadcrumb a", "expected"));

    verify(locator).defaultContent();
  }

  @Test
  void shouldInvalidateTheSessionFromAnotherWindow() {
    utilities.invalidateSession();

    ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
    verify((JavascriptExecutor) driver).executeScript(script.capture());
    assertThat(script.getValue()).contains("window.open('http://localhost:8080/session/invalidate'");
  }

  @Test
  void shouldCheckTheLoggedUserAfterLoggingIn() {
    show(instructions.getLoggedUser(), "Manager (test)");
    show(By.id("ButUsrAct"), "");

    utilities.checkLogin("test", "test", "Manager (test)");

    assertThat(utilities.events).containsExactly("write:cod_usr:test", "write:pwd_usr:test", "click:ButLogIn");
  }

  @Test
  void shouldCheckTheMessageOfARejectedLogin() {
    show(instructions.getMessageText("warning"), "The credentials entered for the user -test- are not valid");
    show(instructions.getMessageTitle("warning"), "Invalid credentials");
    show(By.id("ButLogIn"), "Login");

    utilities.checkLoginRejected("test", "lala", "warning", "Invalid credentials",
      "The credentials entered for the user -test- are not valid");

    assertThat(utilities.events).containsExactly("write:cod_usr:test", "write:pwd_usr:lala", "click:ButLogIn");
  }

  @Test
  void shouldLogOutAndCheckTheLoginScreenOfAngularJs() {
    show(instructions.getLoginScreenMarker(), "Almis Web Engine");

    utilities.checkLogout();

    assertThat(utilities.events).containsExactly("click:ButLogOut");
  }

  @Test
  void shouldFailWhenTheLoginScreenIsNotTheExpectedOne() {
    show(instructions.getLoginScreenMarker(), "Other");

    assertThrows(AssertionFailedError.class, () -> utilities.checkLogout());
  }

  @Test
  void shouldLogOutAndCheckTheLoginButtonOfReact() {
    use(new ReactAweInstructions());
    show(instructions.getLoginScreenMarker(), "Login");

    utilities.checkLogout();

    assertThat(utilities.events).containsExactly("menu:ButUsrAct", "click:ButLogOut");
  }

  private WebElement option(String text, boolean selected) {
    WebElement option = mock(WebElement.class);
    when(option.getText()).thenReturn(text);
    when(option.isSelected()).thenReturn(selected);
    return option;
  }

  /** Records the interactions that need a real browser */
  private static class RecordingSeleniumUtilities extends SeleniumUtilities {
    private final List<String> events = new ArrayList<>();

    @Override
    protected void goToUrl(String url) {
      // No browser
    }

    @Override
    protected void setTestTitle(String title) {
      // No browser
    }

    @Override
    protected void waitForLoadingBar() {
      // No browser
    }

    @Override
    protected void waitForInputActionability(String criterionName) {
      // Inputs are always actionable here
    }

    @Override
    protected void waitForButtonClickability(String buttonName) {
      // Buttons are always actionable here
    }

    @Override
    protected void writeText(String criterionName, CharSequence text, boolean clearText) {
      events.add("write:" + criterionName + ":" + text);
    }

    @Override
    protected void clickButton(String buttonId, boolean wait) {
      events.add("click:" + buttonId);
    }

    @Override
    protected void clickInfoButton(String infoButtonName) {
      events.add("menu:" + infoButtonName);
    }
  }
}
