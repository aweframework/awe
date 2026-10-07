package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import com.almis.awe.testing.selenium.TestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.interactions.Sequence;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class, Interactive.class));
    when(driver.findElements(any(By.class))).thenReturn(Collections.emptyList());
    when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("missing element"));
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});

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
    // The port looks an id up as the equivalent css selector
    By portSelector = Locator.from(selector).toBy();
    when(driver.findElement(argThat(portSelector::equals))).thenReturn(element);
    when(driver.findElements(argThat(portSelector::equals))).thenReturn(List.of(element));
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
  void shouldWaitForTheValueOfACriterionThatAnAsynchronousActionIsStillChanging() {
    WebElement input = show(instructions.getCriterionInput(instructions.getCriterionCss("SugTst")), "");
    when(input.getAttribute("value")).thenReturn("5 (DjrRepPth)", "5 (DjrRepPth)", "6 (DjrHdgPag)");

    assertThatCode(() -> utilities.checkCriterionContents("SugTst", "DjrHdgPag")).doesNotThrowAnyException();
  }

  @Test
  void shouldWaitForTheChosenTextOfASelectThatIsStillLoading() {
    // The screen is still fading in: the select shows its placeholder until the value arrives
    WebElement chosen = show(instructions.getSelectChosen("Sug"), "");
    when(chosen.getText()).thenReturn("Search value", "test (Manager)");

    assertThatCode(() -> utilities.checkSelectContents("Sug", "test (Manager)")).doesNotThrowAnyException();
  }

  @Test
  void shouldFailWithTheTextThatWasReadWhenTheSelectNeverContainsTheText() {
    WebElement chosen = show(instructions.getSelectChosen("Sug"), "");
    when(chosen.getText()).thenReturn("Search value");

    assertThatThrownBy(() -> utilities.checkSelectContents("Sug", "test (Manager)"))
      .isInstanceOf(AssertionFailedError.class)
      .hasMessageContaining(" text: 'Search value' doesn't contain test (Manager)");
  }

  @Test
  void shouldWaitForTheTextOfAnElementThatIsStillLoading() {
    By selector = By.cssSelector(".loading");
    WebElement element = show(selector, "");
    when(element.getText()).thenReturn("Loading", "Done");

    assertThatCode(() -> utilities.checkTextContains(".loading", "Done")).doesNotThrowAnyException();
    when(element.getText()).thenReturn("Loading", "DONE");
    assertThatCode(() -> utilities.checkText(".loading", "done")).doesNotThrowAnyException();
  }

  @Test
  void shouldFailWithTheTextThatWasReadWhenAnElementNeverHasTheExpectedText() {
    WebElement element = show(By.cssSelector(".loading"), "");
    when(element.getText()).thenReturn("Loading");

    assertThatThrownBy(() -> utilities.checkText(".loading", "Done"))
      .isInstanceOf(AssertionFailedError.class)
      .hasMessageContaining(" text: 'Loading' isn't equal to Done");
  }

  @Test
  void shouldWaitForAnOptionOfAMultipleSelectThatIsStillLoading() {
    WebElement option = show(instructions.getSelectMultipleTextContainer("SugMulReq"), "");
    when(option.getText()).thenReturn("", "test (test@test.com)");

    assertThatCode(() -> utilities.checkMultipleSelectorContents("SugMulReq", "test (test@test.com)")).doesNotThrowAnyException();
  }

  @Test
  void shouldFailWhenTheOptionsOfAMultipleSelectNeverContainTheText() {
    WebElement option = show(instructions.getSelectMultipleTextContainer("SugMulReq"), "");
    when(option.getText()).thenReturn("other");

    assertThatThrownBy(() -> utilities.checkMultipleSelectorContents("SugMulReq", "test (test@test.com)"))
      .isInstanceOf(AssertionFailedError.class)
      .hasMessageContaining(" list doesn't contain test (test@test.com)");
  }

  @Test
  void shouldFailWhenTheValueOfACriterionNeverContainsTheText() {
    WebElement input = show(instructions.getCriterionInput(instructions.getCriterionCss("SugTst")), "");
    when(input.getAttribute("value")).thenReturn("5 (DjrRepPth)");

    assertThrows(AssertionFailedError.class, () -> utilities.checkCriterionContents("SugTst", "DjrHdgPag"));
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
    show(instructions.getActiveWizardStep("2"), "2");

    assertThatCode(() -> utilities.checkCriterionLabel("Unt", "Texto Normal")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkCriterionUnit("Unt", "EUR")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkActiveWizardStep("2")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkActiveWizardStep("3"));
  }

  @Test
  void shouldReadTheCellOfTheRowBeingEditedEvenWhenTheClientDoesNotKeepItSelected() {
    use(new ReactAweInstructions());
    WebElement input = show(instructions.getCriterionInput(instructions.getEditingParentCss("Grd", "Dat")), "");
    when(input.getAttribute("value")).thenReturn("23/10/1978");

    assertThat(utilities.getText("Grd", "Dat")).isEqualTo("23/10/1978");
  }

  @Test
  void shouldClickAnOptionOfAButtonGroupThroughTheInstructions() {
    use(new ReactAweInstructions());
    show(instructions.getCheckboxOption("Grp", "Opt1"), "One");

    assertThatCode(() -> utilities.clickCheckboxOption("Grp", "Opt1")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.clickCheckboxOption("Grp", "Missing"));

    verify((Interactive) driver, atLeast(1)).perform(any());
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
  void shouldReadThePageSizeFromItsValueWhenTheTextRepeatsIt() {
    // A dropdown holds a hidden native selector whose option repeats the visible label
    WebElement pageSize = show(instructions.getGridPageSize(), "25\n25");
    when(pageSize.getTagName()).thenReturn("div");
    when(pageSize.getAttribute(TestAttributes.VALUE)).thenReturn("25");

    assertThatCode(() -> utilities.checkGridPageSize("25")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridPageSize("10"));
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
  void shouldFillTheLoginFormAgainWhenItWasClearedWhileBeingFilled() {
    show(instructions.getLoggedUser(), "Manager (test)");
    show(By.id("ButUsrAct"), "");
    // The form was initialized again after the user name was typed (right after a logout): the user name is lost
    WebElement user = show(instructions.getCriterionInput(instructions.getCriterionCss("cod_usr")), "");
    WebElement password = show(instructions.getCriterionInput(instructions.getCriterionCss("pwd_usr")), "");
    when(user.getAttribute("value")).thenReturn("", "test");
    when(password.getAttribute("value")).thenReturn("test");

    utilities.checkLogin("test", "test", "Manager (test)");

    assertThat(utilities.events).containsExactly("write:cod_usr:test", "write:pwd_usr:test",
      "write:cod_usr:test", "write:pwd_usr:test", "click:ButLogIn");
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

  @Test
  void shouldAcceptTheConfirmationOfAnApplicationThatAsksForItBeforeLoggingOut() {
    use(new ReactAweInstructions());
    show(instructions.getLoginScreenMarker(), "Login");

    utilities.checkLogoutWithConfirmation();

    assertThat(utilities.events).containsExactly("menu:ButUsrAct", "click:ButLogOut", "accept-confirm");
  }

  @Test
  void shouldFailWhenTheLoginScreenIsNotShownAfterConfirmingTheLogout() {
    use(new ReactAweInstructions());
    show(instructions.getLoginScreenMarker(), "Other");

    assertThrows(AssertionFailedError.class, () -> utilities.checkLogoutWithConfirmation());
  }

  @Test
  void shouldOpenThePanelOfAMultipleSelectBeforeSearchingInItAndCloseItAfterwards() throws Exception {
    use(new ReactAweInstructions());
    String parent = instructions.getCriterionCss("Months");
    By searchBox = instructions.getSuggestMultipleInput(parent);
    show(instructions.getSelectChoice(parent), "");
    show(instructions.getSelectDropdownList(), "");
    show(instructions.getSuggestResult("October"), "October");
    // The search box only exists once the panel is open: the first action on the page (the click) opens it
    doAnswer(invocation -> {
      WebElement search = show(searchBox, "");
      when(search.getAttribute("value")).thenReturn("");
      return null;
    }).doNothing().doNothing().doAnswer(invocation -> {
      // Closing the panel removes it
      when(driver.findElement(argThat(searchBox::equals))).thenThrow(new NoSuchElementException("panel closed"));
      return null;
    }).when((Interactive) driver).perform(any());

    utilities.suggestMultiple("Months", "October", "October");

    // Open the panel, type, choose the option and close the panel
    verify((Interactive) driver, atLeast(4)).perform(any());
  }

  @Test
  void shouldWaitForThePanelToBeGoneBeforeTheNextStepSoItIsNotTakenForAnOpenOne() {
    use(new ReactAweInstructions());
    String parent = instructions.getCriterionCss("Months");
    By searchBox = instructions.getSuggestMultipleInput(parent);
    show(instructions.getSelectChoice(parent), "");
    show(instructions.getSelectDropdownList(), "");
    show(instructions.getSuggestResult("October"), "October");
    AtomicInteger performed = new AtomicInteger();
    AtomicInteger checksAfterEscape = new AtomicInteger();
    WebElement search = mock(WebElement.class);
    when(search.isEnabled()).thenReturn(true);
    when(search.getAttribute("value")).thenReturn("");
    // The panel is still displayed when the escape key is pressed and takes some checks to leave (exit animation)
    properties.setTimeout(Duration.ofSeconds(5));
    when(search.isDisplayed()).thenAnswer(invocation -> performed.get() < 4 || checksAfterEscape.incrementAndGet() < 3);
    doAnswer(invocation -> {
      if (performed.incrementAndGet() == 1) {
        when(driver.findElement(argThat(searchBox::equals))).thenReturn(search);
        when(driver.findElements(argThat(searchBox::equals))).thenReturn(List.of(search));
      }
      return null;
    }).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.suggestMultiple("Months", "October", "October")).doesNotThrowAnyException();

    // The step did not finish before the panel was gone
    assertThat(checksAfterEscape.get()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void shouldNotOpenAnyPanelWhenTheSearchBoxOfAMultipleChoiceIsAlwaysThere() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");

    utilities.suggestMultiple("Months", "October", "October");

    // Type and choose the option, nothing else
    verify((Interactive) driver, times(2)).perform(any());
  }

  /**
   * The options of the open list, that answer a list of texts per lookup (the last one stays) and count the lookups
   */
  private AtomicInteger showOptionsThatChange(String[]... lookups) {
    AtomicInteger looked = new AtomicInteger();
    By options = Locator.from(instructions.getSelectOptions()).toBy();
    when(driver.findElements(argThat(options::equals))).thenAnswer(invocation -> {
      String[] texts = lookups[Math.min(looked.getAndIncrement(), lookups.length - 1)];
      List<WebElement> elements = new ArrayList<>();
      for (String text : texts) {
        WebElement option = mock(WebElement.class);
        when(option.isDisplayed()).thenReturn(true);
        when(option.getText()).thenReturn(text);
        elements.add(option);
      }
      return elements;
    });
    return looked;
  }

  @Test
  void shouldWaitForTheOptionsToSettleBeforeChoosingOneOfAMultipleSuggest() {
    // Typing "1" filters the list in steps: the option that was found is not where it was when the pointer arrives
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("1"), "1");
    properties.setTimeout(Duration.ofSeconds(5));
    AtomicInteger looked = showOptionsThatChange(new String[]{"1", "10", "11"}, new String[]{"1"}, new String[]{"1"});
    AtomicInteger lookedWhenChoosing = new AtomicInteger();
    AtomicInteger performed = new AtomicInteger();
    doAnswer(invocation -> {
      if (performed.incrementAndGet() == 2) {
        lookedWhenChoosing.set(looked.get());
      }
      return null;
    }).when((Interactive) driver).perform(any());

    utilities.suggestMultiple("Months", "1", "1");

    // Two lookups in a row gave the same options before the click
    assertThat(lookedWhenChoosing.get()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void shouldTakeOnlyOneMoreLookWhenTheOptionsAreAlreadySettled() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("1"), "1");
    properties.setTimeout(Duration.ofSeconds(5));
    AtomicInteger looked = showOptionsThatChange(new String[]{"1", "10"});
    AtomicInteger lookedWhenChoosing = new AtomicInteger();
    AtomicInteger performed = new AtomicInteger();
    doAnswer(invocation -> {
      if (performed.incrementAndGet() == 2) {
        lookedWhenChoosing.set(looked.get());
      }
      return null;
    }).when((Interactive) driver).perform(any());

    utilities.suggestMultiple("Months", "1", "1");

    assertThat(lookedWhenChoosing.get()).isEqualTo(2);
  }

  @Test
  void shouldWaitForThePendingServerTasksBeforeChoosingTheOptionsOfTheMenu() {
    // A restore that still loads its suggests: the client drops the actions that are waiting when an option is chosen, so
    // the screen would not change
    SeleniumUtilities navigating = new SeleniumUtilities() {
    };
    ReflectionTestUtils.setField(navigating, "properties", properties);
    ReflectionTestUtils.setField(navigating, "seleniumModel", model);
    ReflectionTestUtils.setField(navigating, "frontEndInstructions", instructions);
    properties.setTimeout(Duration.ofSeconds(5));
    show(instructions.getMenuOption("test"), "Tests");
    WebElement loadingBar = show(instructions.getLoadingBar(), "");
    AtomicInteger barLooks = new AtomicInteger();
    when(loadingBar.isDisplayed()).thenAnswer(invocation -> barLooks.incrementAndGet() <= 2);
    AtomicInteger looksWhenChoosing = new AtomicInteger(-1);
    doAnswer(invocation -> {
      looksWhenChoosing.compareAndSet(-1, barLooks.get());
      return null;
    }).when((Interactive) driver).perform(any());

    navigating.gotoScreen("test");

    // The bar was looked at until it was gone (two looks displayed, one not) before the first gesture on the menu
    assertThat(looksWhenChoosing.get()).isGreaterThanOrEqualTo(3);
  }

  private SeleniumUtilities navigatingUtilities() {
    SeleniumUtilities navigating = new SeleniumUtilities() {
      @Override
      protected void waitForLoadingBar() {
        // No browser
      }
    };
    ReflectionTestUtils.setField(navigating, "properties", properties);
    ReflectionTestUtils.setField(navigating, "seleniumModel", model);
    ReflectionTestUtils.setField(navigating, "frontEndInstructions", instructions);
    return navigating;
  }

  private AtomicInteger menuOptionBecomesActiveAfterClick(int clicksNeeded) {
    use(new ReactAweInstructions());
    properties.setTimeout(Duration.ofMillis(300));
    show(instructions.getMenuOption("test"), "Tests");
    AtomicInteger clicks = new AtomicInteger();
    doAnswer(invocation -> {
      clicks.incrementAndGet();
      return null;
    }).when((Interactive) driver).perform(any());
    WebElement active = show(instructions.getMenuActiveOption("test"), "Tests");
    when(active.isDisplayed()).thenAnswer(invocation -> clicks.get() >= clicksNeeded);
    return clicks;
  }

  @Test
  void shouldClickTheLastMenuOptionAgainWhenTheFirstClickWasIgnored() {
    // The browser reports the click as done but the screen does not change (the option only gets the focus)
    AtomicInteger clicks = menuOptionBecomesActiveAfterClick(2);

    assertThatCode(() -> navigatingUtilities().gotoScreen("test")).doesNotThrowAnyException();

    assertThat(clicks.get()).isEqualTo(2);
  }

  @Test
  void shouldNotClickTheLastMenuOptionAgainWhenTheFirstClickTookEffect() {
    AtomicInteger clicks = menuOptionBecomesActiveAfterClick(1);

    assertThatCode(() -> navigatingUtilities().gotoScreen("test")).doesNotThrowAnyException();

    assertThat(clicks.get()).isEqualTo(1);
  }

  @Test
  void shouldFailAfterClickingTheLastMenuOptionAgainWhenTheClickNeverTakesEffect() {
    AtomicInteger clicks = menuOptionBecomesActiveAfterClick(Integer.MAX_VALUE);
    SeleniumUtilities navigating = navigatingUtilities();

    assertThatThrownBy(() -> navigating.gotoScreen("test"))
      .isInstanceOf(AssertionFailedError.class)
      .hasMessageContaining("data-active");

    // The click and two more
    assertThat(clicks.get()).isEqualTo(3);
  }

  @Test
  void shouldChooseTheOptionAfterABoundedWaitWhenTheOptionsNeverSettle() {
    // A list that changes at every look never settles: the step waits for the configured time at most, then chooses
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("1"), "1");
    properties.setTimeout(Duration.ofSeconds(1));
    AtomicInteger looked = new AtomicInteger();
    By options = Locator.from(instructions.getSelectOptions()).toBy();
    when(driver.findElements(argThat(options::equals))).thenAnswer(invocation -> {
      WebElement option = mock(WebElement.class);
      when(option.isDisplayed()).thenReturn(true);
      when(option.getText()).thenReturn("option " + looked.incrementAndGet());
      return List.of(option);
    });
    AtomicInteger performed = new AtomicInteger();
    doAnswer(invocation -> {
      performed.incrementAndGet();
      return null;
    }).when((Interactive) driver).perform(any());

    utilities.suggestMultiple("Months", "1", "1");

    // It chose, after looking more than once and not for ever: a look is 200 ms after the last one for at most the timeout
    // of 1 s, so there are six at the most whatever the speed of the machine
    assertThat(performed.get()).isGreaterThanOrEqualTo(2);
    assertThat(looked.get()).isBetween(2, 7);
  }

  @Test
  void shouldChooseTheOptionAfterABoundedWaitWhenThereAreNoOptionsToLookAt() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("1"), "1");
    properties.setTimeout(Duration.ofSeconds(1));
    AtomicInteger looked = showOptionsThatChange(new String[0]);
    AtomicInteger performed = new AtomicInteger();
    doAnswer(invocation -> {
      performed.incrementAndGet();
      return null;
    }).when((Interactive) driver).perform(any());

    utilities.suggestMultiple("Months", "1", "1");

    // It chose, after looking more than once and not for ever: a look is 200 ms after the last one for at most the timeout
    // of 1 s, so there are six at the most whatever the speed of the machine
    assertThat(performed.get()).isGreaterThanOrEqualTo(2);
    assertThat(looked.get()).isBetween(2, 7);
  }

  @Test
  void shouldClickAgainAnOptionThatTheClientReplacedWhileItWasBeingChosen() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");
    // Typing, then the first click finds the option stale (the list was filtered meanwhile) and the second one works
    doNothing().doThrow(new StaleElementReferenceException("replaced by the filtered list")).doNothing()
      .when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.suggestMultiple("Months", "October", "October")).doesNotThrowAnyException();

    verify((Interactive) driver, times(3)).perform(any());
  }

  @Test
  void shouldClickAgainAnOptionThatFirefoxReportsAsNotDisplayedAfterTheClientReplacedIt() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");
    // Firefox does not report a replaced element as stale when the action is already running: it finds no box for it
    doNothing().doThrow(new MoveTargetOutOfBoundsException("Origin element <li> is not displayed")).doNothing()
      .when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.suggestMultiple("Months", "October", "October")).doesNotThrowAnyException();

    verify((Interactive) driver, times(3)).perform(any());
  }

  @Test
  void shouldCloseEveryMessageOfAStackOneByOne() {
    By close = instructions.getMessage("danger");
    List<WebElement> stack = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      WebElement message = mock(WebElement.class);
      when(message.isDisplayed()).thenReturn(true);
      when(message.isEnabled()).thenReturn(true);
      stack.add(message);
    }
    when(driver.findElements(argThat(close::equals))).thenAnswer(invocation -> new ArrayList<>(stack));
    when(driver.findElement(argThat(close::equals))).thenAnswer(invocation -> stack.get(0));
    // Closing a message leaves the rest of the stack where it was
    doAnswer(invocation -> stack.remove(0)).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.closeMessages("danger", 3)).doesNotThrowAnyException();

    assertThat(stack).isEmpty();
  }

  @Test
  void shouldFailWhenTheStackHasFewerMessagesThanExpected() {
    By close = instructions.getMessage("danger");
    show(close, "");

    assertThrows(AssertionFailedError.class, () -> utilities.closeMessages("danger", 2));
  }

  @Test
  void shouldFailWhenAnOptionIsNeverDisplayed() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");
    doNothing().doThrow(new MoveTargetOutOfBoundsException("Origin element <li> is not displayed"))
      .when((Interactive) driver).perform(any());

    assertThrows(AssertionFailedError.class, () -> utilities.suggestMultiple("Months", "October", "October"));
  }

  @Test
  void shouldBringAnElementCloseToTheEdgeOfTheViewportToTheCenterBeforeClickingIt() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");

    utilities.suggestMultiple("Months", "October", "October");

    // A click on a button placed on the last pixels of the viewport is lost by the browser
    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("scrollIntoView") && script.contains("innerHeight")), any());
  }

  @Test
  void shouldBringACellInsideItsScrollContainerBeforeDoubleClickingIt() {
    use(new ReactAweInstructions());
    WebElement cell = show(instructions.findGridCell("GrdEdi", "clean"), "clean");
    // The client does not identify the row of the cell: Selenium reports it as a missing element
    when(cell.findElement(any(By.class))).thenThrow(new NoSuchElementException("no row"));

    utilities.editRow("GrdEdi", "clean");

    // A grid wider than its container is scrolled by the client (the save button of the edited row is brought into
    // view): the cell can be left clipped by the container, under the menu, where the pointer would not reach it. The
    // scroll must be immediate: a smooth one is still moving when the pointer is placed over the cell
    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("scrollIntoView") && script.contains("inline: 'nearest'")
        && script.contains("behavior: 'instant'")), eq(cell));
  }

  /**
   * A cell of the row "3" of a grid, shown by the instructions of the React client, whose row is being edited as soon as
   * the given number of double clicks has been performed
   */
  private AtomicInteger editRowAfterDoubleClicks(int doubleClicksToEdit) {
    use(new ReactAweInstructions());
    WebElement cell = show(instructions.findGridCell("GrdEdi", "clean"), "clean");
    WebElement row = mock(WebElement.class);
    when(row.getAttribute("row-id")).thenReturn("3");
    when(cell.findElement(instructions.getGridRowOfCell())).thenReturn(row);
    AtomicInteger doubleClicks = new AtomicInteger();
    doAnswer(invocation -> doubleClicks.incrementAndGet()).when((Interactive) driver).perform(any());
    By editingRow = instructions.getGridEditingRow("GrdEdi", "3");
    when(driver.findElements(argThat(editingRow::equals)))
      .thenAnswer(invocation -> doubleClicks.get() >= doubleClicksToEdit ? List.of(row) : Collections.emptyList());
    return doubleClicks;
  }

  @Test
  void shouldNotRepeatTheDoubleClickThatStartedTheEditionOfTheRow() {
    AtomicInteger doubleClicks = editRowAfterDoubleClicks(1);

    utilities.editRow("GrdEdi", "clean");

    assertThat(doubleClicks).hasValue(1);
  }

  @Test
  void shouldRepeatTheDoubleClickThatDidNotStartTheEditionOfTheRow() {
    // On a loaded machine the browser takes more time than the double click interval between the two clicks of the
    // action, so it only sees two clicks that select the row, and the row is never edited
    AtomicInteger doubleClicks = editRowAfterDoubleClicks(2);

    utilities.editRow("GrdEdi", "clean");

    assertThat(doubleClicks).hasValue(2);
  }

  @Test
  void shouldGiveUpRepeatingTheDoubleClickWhenTheRowIsNeverEdited() {
    // The client may reject the edition on purpose (the row being edited has errors): the step does not fail by itself
    AtomicInteger doubleClicks = editRowAfterDoubleClicks(Integer.MAX_VALUE);

    assertThatCode(() -> utilities.editRow("GrdEdi", "clean")).doesNotThrowAnyException();

    assertThat(doubleClicks).hasValue(3);
  }

  @Test
  void shouldNotCheckTheEditionOfARowThatTheClientDoesNotIdentify() {
    use(new ReactAweInstructions());
    WebElement cell = show(instructions.findGridCell("GrdEdi", "clean"), "clean");
    // Selenium reports the row that the client does not identify as a missing element
    when(cell.findElement(any(By.class))).thenThrow(new NoSuchElementException("no row"));
    AtomicInteger doubleClicks = new AtomicInteger();
    doAnswer(invocation -> doubleClicks.incrementAndGet()).when((Interactive) driver).perform(any());

    utilities.editRow("GrdEdi", "clean");

    assertThat(doubleClicks).hasValue(1);
  }

  @Test
  void shouldBringACellInsideItsScrollContainerBeforeOpeningItsContextMenu() {
    use(new ReactAweInstructions());
    WebElement cell = show(instructions.findGridCell("GrdEdi", "asphalt"), "asphalt");

    utilities.contextMenuRowContents("GrdEdi", "asphalt");

    // The same cell that a double click could not reach (clipped by the scroll of the grid, under the menu) would
    // receive the right click on the menu instead, and the context menu of the grid would never open
    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("scrollIntoView") && script.contains("inline: 'nearest'")
        && script.contains("behavior: 'instant'")), eq(cell));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldJumpToTheNestedOptionOfAContextMenuWithoutCrossingTheOptionsBetween() {
    use(new ReactAweInstructions());
    show(instructions.getContextButton("CtxNew"), "New");
    show(instructions.getContextButton("CtxNewChild"), "Child");

    utilities.clickContextButton("CtxNew", "CtxNewChild");

    // A pointer that travels (as the one of Firefox does) from an option to its nested option crosses the options that
    // lie between them, and the menu closes the nested options when the pointer leaves their parent
    ArgumentCaptor<Collection<Sequence>> captor = ArgumentCaptor.forClass(Collection.class);
    verify((Interactive) driver, atLeast(2)).perform(captor.capture());
    List<Object> hoverMoves = new ArrayList<>();
    captor.getAllValues().stream().limit(2).flatMap(Collection::stream)
      .forEach(sequence -> ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream()
        .filter(action -> "pointerMove".equals(action.get("type"))).forEach(action -> hoverMoves.add(action.get("duration"))));
    assertThat(hoverMoves).isNotEmpty().allMatch(duration -> Long.valueOf(0).equals(duration) || Integer.valueOf(0).equals(duration));
  }

  @Test
  void shouldBringTheSearchBoxOfAMultipleSuggestToTheCenterBeforeTypingInIt() {
    WebElement searchBox = show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");

    utilities.suggestMultiple("Months", "October", "October");

    // The suggestions panel is aligned when it opens: a later scroll leaves it misplaced until the next render, and the
    // option moves from under the pointer when the search box loses the focus on the press of the click
    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("block: 'center'") && !script.contains("innerHeight")), eq(searchBox));
  }

  @Test
  void shouldBringTheSearchBoxOfAReplacedSearchToTheCenterBeforeTypingInIt() {
    WebElement searchBox = show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    when(searchBox.getAttribute("value")).thenReturn("");
    show(instructions.getSuggestResult("October"), "October");

    utilities.suggestMultipleReplacingSearch("Months", "Oct", "October", "October", 10);

    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("block: 'center'") && !script.contains("innerHeight")), eq(searchBox));
  }

  @Test
  void shouldScrollAnOptionIntoItsOwnScrollableListBeforeClickingIt() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");

    utilities.suggestMultiple("Months", "October", "October");

    // The last option of a list with its own scroll can be clipped by the list: a click on it would reach what lies below
    verify((JavascriptExecutor) driver, atLeast(1)).executeScript(
      argThat((String script) -> script.contains("innerHeight") && script.contains("block: 'nearest'")), any());
  }

  @Test
  void shouldFailWhenAnOptionIsReplacedOnEveryAttempt() {
    show(instructions.getSuggestMultipleInput(instructions.getCriterionCss("Months")), "");
    show(instructions.getSuggestResult("October"), "October");
    doNothing().doThrow(new StaleElementReferenceException("replaced again")).when((Interactive) driver).perform(any());

    assertThrows(AssertionFailedError.class, () -> utilities.suggestMultiple("Months", "October", "October"));
  }

  @Test
  void shouldLeaveARowAsItIsWhenTheClientKeepsItSelected() {
    use(new ReactAweInstructions());
    // The element to click is never shown: the step must not try to click it
    show(instructions.findGridSelectedRow("Grd", "abc"), "abc");

    assertThatCode(() -> utilities.clickRowContents("Grd", "abc")).doesNotThrowAnyException();
  }

  @Test
  void shouldClickARowThatIsSelectedWhenTheStepIsToToggleIt() {
    use(new ReactAweInstructions());
    show(instructions.findGridSelectedRow("Grd", "abc"), "abc");
    show(instructions.findGridRowSelection("Grd", "abc"), "abc");

    assertThatCode(() -> utilities.toggleRowContents("Grd", "abc")).doesNotThrowAnyException();

    verify((Interactive) driver, atLeast(1)).perform(any());
  }

  @Test
  void shouldClickARowThatIsNotSelectedYet() {
    use(new ReactAweInstructions());

    assertThrows(AssertionFailedError.class, () -> utilities.clickRowContents("Grd", "abc"));
  }

  @Test
  void shouldAlwaysClickTheRowWhenTheClientDoesNotKeepItsSelection() {
    assertThat(instructions.findGridSelectedRow("Grd", "abc")).isNull();
    assertThrows(AssertionFailedError.class, () -> utilities.clickRowContents("Grd", "abc"));
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
    protected void acceptConfirm() {
      events.add("accept-confirm");
    }

    @Override
    protected void clickInfoButton(String infoButtonName) {
      events.add("menu:" + infoButtonName);
    }
  }
}
