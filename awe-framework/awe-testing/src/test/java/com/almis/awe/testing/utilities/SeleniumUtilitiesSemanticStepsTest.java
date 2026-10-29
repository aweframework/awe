package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * The semantic steps of {@link SeleniumUtilities}: each one asks the front end instructions for what it needs, so the
 * tests that use it contain no selector.
 */
class SeleniumUtilitiesSemanticStepsTest {

  // Text of the Maximize buttons of grids, windows and tabs
  private static final String MAXIMIZE = "Maximize";

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

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> performedActions() {
    ArgumentCaptor<Collection<Sequence>> captor = ArgumentCaptor.forClass(Collection.class);
    verify((Interactive) driver, atLeast(0)).perform(captor.capture());
    return captor.getAllValues().stream()
      .flatMap(Collection::stream)
      .flatMap(sequence -> ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream())
      .toList();
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
  void shouldCheckThePopoverOfATargetMessageThroughTheInstructions() {
    show(instructions.getMessagePopover("Txt"), "Wrong value The value is not valid");
    show(instructions.getMessagePopoverTitle("Txt"), "Wrong value");
    show(instructions.getMessagePopoverText("Txt"), "The value is not valid");

    assertThatCode(() -> utilities.checkMessagePopover("Txt", "Wrong value", "The value is not valid"))
      .doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkMessagePopover("Txt", "Wrong value", "Other"));
    // It stays while it is shown
    assertThrows(AssertionFailedError.class, () -> utilities.waitForMessagePopoverToHide("Txt"));
  }

  @Test
  void shouldDismissThePopoverOfATargetMessageByClickingItsMask() {
    show(instructions.getMessagePopoverMask(), "");

    assertThatCode(() -> utilities.dismissMessagePopover("Txt")).doesNotThrowAnyException();

    assertThat(performedActions()).extracting(action -> action.get("type")).contains("pointerDown", "pointerUp");
    assertThat(performedActions()).extracting(action -> action.get("type")).doesNotContain("keyDown");
  }

  @Test
  void shouldDismissThePopoverOfATargetMessageWithTheEscapeKeyWhenThereIsNoMask() {
    use(new ReactAweInstructions());

    assertThatCode(() -> utilities.dismissMessagePopover("Txt")).doesNotThrowAnyException();

    assertThat(instructions.getMessagePopoverMask()).isNull();
    assertThat(performedActions()).extracting(action -> action.get("type")).contains("keyDown", "keyUp");
  }

  @Test
  void shouldFailWhenTheDismissedPopoverOfATargetMessageStays() {
    use(new ReactAweInstructions());
    show(instructions.getMessagePopover("Txt"), "Wrong value The value is not valid");

    assertThrows(AssertionFailedError.class, () -> utilities.dismissMessagePopover("Txt"));
  }

  @Test
  void shouldFailWhenTheMessagePopoverIsNeverShownAndWaitWhenItIsGone() {
    assertThrows(AssertionFailedError.class, () -> utilities.checkMessagePopover("Txt", "Title", "Text"));
    assertThatCode(() -> utilities.waitForMessagePopoverToHide("Txt")).doesNotThrowAnyException();
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
  void shouldWaitForTheFileViewerToShowTheText() {
    show(instructions.getFileViewer(), "FILE VIEWER TEST LINE 1");

    assertThatCode(() -> utilities.checkFileViewerContains("TEST LINE 1")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkFileViewerContains("[OTHER]"));
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
  void shouldCheckTheDescriptionOfALauncherCard() {
    show(instructions.getLauncherCardDescription("sales"), "Orders and sales reports");

    assertThatCode(() -> utilities.checkLauncherCardDescription("sales", "Orders and sales reports")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkLauncherCardDescription("sales", "Other text"));
    assertThatCode(() -> utilities.checkLauncherCardWithoutDescription("plain")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkLauncherCardWithoutDescription("sales"));
  }

  @Test
  void shouldCheckTheBreadcrumbOfThePage() {
    show(instructions.getBreadcrumb(), "Tests Launcher Sales");

    assertThatCode(() -> utilities.checkBreadcrumbContains("Launcher")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkBreadcrumbContains("Missing"));
  }

  @Test
  void shouldZoomAChartWithTheWheelAndCheckThatItIsZoomed() {
    use(new ReactAweInstructions());
    WebElement chart = show(instructions.getChart("ChrLinTst"), "");
    show(instructions.getChartZoomed("ChrLinTst"), "");

    assertThatCode(() -> utilities.zoomChart("ChrLinTst")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkChartZoomed("ChrLinTst")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkChartZoomed("Missing"));
    assertThat(chart).isNotNull();
  }

  @Test
  void shouldCheckAndClickTheMarkersOfAMap() {
    use(new ReactAweInstructions());
    show(instructions.getMap("MapTst"), "");
    show(instructions.getMapMarkers("MapTst"), "");
    show(instructions.getMapMarker("MapTst", "2"), "");
    show(instructions.getMapMarkerSelected("MapTst", "2"), "");

    assertThatCode(() -> {
      utilities.checkMapVisible("MapTst");
      utilities.checkMapMarkerCount("MapTst", 1);
      utilities.clickMapMarker("MapTst", "2");
      utilities.checkMapMarkerSelected("MapTst", "2");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkMapVisible("Missing"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkMapMarkerCount("MapTst", 4));
    assertThrows(AssertionFailedError.class, () -> utilities.clickMapMarker("MapTst", "9"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkMapMarkerSelected("MapTst", "9"));
  }

  @Test
  void shouldCheckTheRouteOfAMapAndThatItHidesTheMarkers() {
    use(new ReactAweInstructions());
    show(instructions.getMapRoute("MapTst"), "");

    assertThatCode(() -> {
      utilities.checkMapRouteVisible("MapTst");
      utilities.checkMapMarkerCount("MapTst", 0);
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkMapRouteVisible("Missing"));
  }

  @Test
  void shouldCheckThatAPivotTableIsDrawn() {
    show(instructions.getPivotTable("listaDatos"), "");

    assertThatCode(() -> utilities.checkPivotTableVisible("listaDatos")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkPivotTableVisible("UndrawnPivot"));
  }

  @Test
  void shouldFailToZoomAChartThatIsNotThere() {
    use(new ReactAweInstructions());

    assertThrows(AssertionFailedError.class, () -> utilities.zoomChart("Missing"));
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
  void shouldCloseADialogWithTheCloseIconOfItsHeader() {
    show(instructions.getDialogClose("Dia"), "");

    assertThatCode(() -> utilities.closeDialog("Dia")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.closeDialog("Missing"));

    verify((Interactive) driver, atLeast(1)).perform(any());
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
  void shouldAcceptTheCellsOfAGridThatShowOnlyText() {
    show(instructions.getGridCells("GrdXss"), "Alice");

    assertThatCode(() -> utilities.checkGridCellsHaveNoActiveContent("GrdXss")).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectTheCellsOfAGridThatHoldActiveContent() {
    show(instructions.getGridCells("GrdXss"), "Alice");
    show(instructions.getGridCellsActiveContent("GrdXss"), "");

    assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellsHaveNoActiveContent("GrdXss"));
  }

  @Test
  void shouldFailWhenTheGridShowsNoCells() {
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellsHaveNoActiveContent("GrdXss"));
  }

  @Test
  void shouldLookForActiveContentInTheCellsOfTheGridOnBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      String xpath = client.getGridCellsActiveContent("GrdXss").toString();

      assertThat(xpath).contains("'GrdXss'", "self::script", "self::iframe", "self::img", "starts-with(name(),'on')", "javascript:");
    }
  }

  private void showAll(By selector, String... texts) {
    List<WebElement> elements = new ArrayList<>();
    for (String text : texts) {
      WebElement element = mock(WebElement.class);
      when(element.isDisplayed()).thenReturn(true);
      when(element.getText()).thenReturn(text);
      elements.add(element);
    }
    By portSelector = Locator.from(selector).toBy();
    when(driver.findElements(argThat(portSelector::equals))).thenReturn(elements);
  }

  @Test
  void shouldAcceptTheRowsOfAGridInTheExpectedOrder() {
    showAll(instructions.getGridColumnCells("GrdMove", "name"), "Step two", " Step one ", "Step three");

    assertThatCode(() -> utilities.checkGridColumnOrder("GrdMove", "name", "Step two", "Step one", "Step three"))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectTheRowsOfAGridInAnotherOrder() {
    showAll(instructions.getGridColumnCells("GrdMove", "name"), "Step one", "Step two", "Step three");

    assertThrows(AssertionFailedError.class,
      () -> utilities.checkGridColumnOrder("GrdMove", "name", "Step two", "Step one", "Step three"));
  }

  @Test
  void shouldRejectAGridWithMoreRowsThanExpected() {
    showAll(instructions.getGridColumnCells("GrdMove", "name"), "Step one", "Step two");

    assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnOrder("GrdMove", "name", "Step one"));
  }

  @Test
  void shouldLookForTheCellsOfAColumnOfAGridOnBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      String xpath = client.getGridColumnCells("GrdMove", "name").toString();

      assertThat(xpath).contains("'GrdMove'", "@column-id='name'", "grid-id", "tree-grid-id");
    }
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
  void shouldCloseAMessageThatIsShown() {
    By portSelector = Locator.from(instructions.getMessage("success")).toBy();
    AtomicBoolean gone = new AtomicBoolean();
    WebElement message = mock(WebElement.class);
    when(message.isDisplayed()).thenReturn(true);
    when(message.isEnabled()).thenReturn(true);
    when(driver.findElements(argThat(portSelector::equals)))
      .thenAnswer(invocation -> gone.get() ? Collections.emptyList() : List.of(message));
    when(driver.findElement(argThat(portSelector::equals))).thenAnswer(invocation -> {
      if (gone.get()) {
        throw new NoSuchElementException("message closed");
      }
      return message;
    });
    // Clicking the message closes it
    doAnswer(invocation -> gone.getAndSet(true)).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.checkAndCloseMessage("success")).doesNotThrowAnyException();

    verify((Interactive) driver).perform(any());
  }

  @Test
  void shouldAcceptAMessageThatClosesItselfBeforeItIsClicked() {
    By close = instructions.getMessage("success");
    By portSelector = Locator.from(close).toBy();
    AtomicBoolean gone = new AtomicBoolean();
    WebElement message = mock(WebElement.class);
    when(message.isDisplayed()).thenReturn(true);
    // The message is seen clickable and leaves on its own right after (messageTimeout)
    when(message.isEnabled()).thenAnswer(invocation -> {
      gone.set(true);
      return true;
    });
    when(driver.findElements(argThat(portSelector::equals)))
      .thenAnswer(invocation -> gone.get() ? Collections.emptyList() : List.of(message));
    when(driver.findElement(argThat(portSelector::equals))).thenAnswer(invocation -> {
      if (gone.get()) {
        throw new NoSuchElementException("message left");
      }
      return message;
    });

    assertThatCode(() -> utilities.checkAndCloseMessage("success")).doesNotThrowAnyException();

    verify((Interactive) driver, never()).perform(any());
  }

  @Test
  void shouldFailToCloseAMessageThatIsNeverShown() {
    assertThrows(AssertionFailedError.class, () -> utilities.checkAndCloseMessage("success"));
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

  @Test
  void shouldCheckThatATabAndAWizardStepAreMarkedInvalidOrNot() {
    assertThatCode(() -> {
      utilities.checkTabInvalid("Tabs", false);
      utilities.checkWizardStepInvalid("Wiz", false);
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkTabInvalid("Tabs", true));
    assertThrows(AssertionFailedError.class, () -> utilities.checkWizardStepInvalid("Wiz", true));

    show(instructions.getInvalidTab("Tabs"), "Second");
    show(instructions.getInvalidWizardStep("Wiz"), "2");

    assertThatCode(() -> {
      utilities.checkTabInvalid("Tabs", true);
      utilities.checkWizardStepInvalid("Wiz", true);
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkTabInvalid("Tabs", false));
    assertThrows(AssertionFailedError.class, () -> utilities.checkWizardStepInvalid("Wiz", false));
  }

  @Test
  void shouldCheckThatTheFocusIsOnACriterion() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(anyString(), any(Object[].class))).thenReturn(false, true);

    assertThatCode(() -> utilities.checkCriterionFocused("Req")).doesNotThrowAnyException();

    verify(executor, atLeast(2)).executeScript(argThat((String script) -> script.contains("activeElement")),
      eq(instructions.getCriterionCss("Req")));
  }

  @Test
  void shouldCheckTheDatesAndThePresetsOfADateRange() {
    WebElement from = show(instructions.getDateRangeFrom("Rng"), "");
    WebElement to = show(instructions.getDateRangeTo("Rng"), "");
    when(from.getAttribute("value")).thenReturn("01/01/2026");
    when(to.getAttribute("value")).thenReturn("31/01/2026");
    show(instructions.getDateRangePresets("Rng"), "1M");
    show(instructions.getDateRangePreset("Rng", "1M"), "1M");
    show(instructions.getDateRangePresetActive("Rng", "1M"), "1M");

    assertThatCode(() -> {
      utilities.checkDateRange("Rng", "01/01/2026", "31/01/2026");
      utilities.checkDateRangePresetCount("Rng", 1);
      utilities.clickDateRangePreset("Rng", "1M");
      utilities.checkDateRangePresetActive("Rng", "1M");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkDateRange("Rng", "", ""));
    assertThrows(AssertionFailedError.class, () -> utilities.checkDateRangePresetCount("Rng", 2));
    assertThrows(AssertionFailedError.class, () -> utilities.clickDateRangePreset("Rng", "YTD"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkDateRangePresetActive("Rng", "YTD"));
    verify((Interactive) driver, atLeast(1)).perform(any());
  }

  @Test
  void shouldCheckThatADateRangeIsDisabledAndClearItsStartDate() {
    WebElement from = show(instructions.getDateRangeFrom("Rng"), "");
    WebElement to = show(instructions.getDateRangeTo("Rng"), "");
    when(from.isEnabled()).thenReturn(false);
    when(to.isEnabled()).thenReturn(false);
    when(from.getAttribute("value")).thenReturn("");

    assertThatCode(() -> utilities.checkDateRangeDisabled("Rng")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.clearDateRangeFrom("Rng")).doesNotThrowAnyException();

    when(to.isEnabled()).thenReturn(true);
    assertThrows(AssertionFailedError.class, () -> utilities.checkDateRangeDisabled("Rng"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldTypeTheDatesOfADateRangeAndSubmitThemWithEnter() {
    WebElement from = show(instructions.getDateRangeFrom("Rng"), "");
    WebElement to = show(instructions.getDateRangeTo("Rng"), "");
    when(from.getAttribute("value")).thenReturn("01/01/2026");
    when(to.getAttribute("value")).thenReturn("31/01/2026");
    List<Object> keysPressed = new ArrayList<>();
    doAnswer(invocation -> {
      ((Collection<Sequence>) invocation.getArgument(0)).forEach(sequence ->
        ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream()
          .filter(action -> "keyDown".equals(action.get("type"))).forEach(action -> keysPressed.add(action.get("value"))));
      return null;
    }).when((Interactive) driver).perform(any());

    assertThatCode(() -> {
      utilities.submitDateRangeFrom("Rng", "02/01/2026");
      utilities.submitDateRangeTo("Rng", "30/01/2026");
    }).doesNotThrowAnyException();

    // Each date replaces the previous one and is submitted with Enter, as the user does (the keys go one by one)
    verify(from).clear();
    verify(to).clear();
    assertThat(keysPressed.stream().map(String::valueOf).collect(Collectors.joining()))
      .isEqualTo("02/01/2026" + Keys.ENTER + "30/01/2026" + Keys.ENTER);
    assertThrows(AssertionFailedError.class, () -> utilities.submitDateRangeFrom("Missing", "02/01/2026"));
  }

  @Test
  void shouldDropAFileOnTheDropZoneOfAnUploader() {
    show(instructions.getUploaderDropZone("Upl"), "");

    utilities.dropFile("Upl", "pixel.png", "image/png", "iVBORw0KGgo=");

    verify((JavascriptExecutor) driver).executeScript(argThat((String script) -> script.contains("DragEvent")
      && script.contains("dragenter") && script.contains("drop")), any(Object[].class));
    assertThrows(AssertionFailedError.class, () -> utilities.dropFile("Missing", "pixel.png", "image/png", "AA=="));
  }

  @Test
  void shouldCheckThatAnUploaderOffersTheCameraButtonOrNot() {
    show(instructions.getUploaderCapture("Cam"), "Take photo");

    assertThatCode(() -> {
      utilities.checkUploadCaptureButton("Cam", true);
      utilities.checkUploadCaptureButton("Plain", false);
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkUploadCaptureButton("Plain", true));
    assertThrows(AssertionFailedError.class, () -> utilities.checkUploadCaptureButton("Cam", false));
  }

  @Test
  void shouldChooseAFileWithTheCameraInputOfAnUploader() {
    WebElement input = show(instructions.getUploaderCaptureInput("Cam"), "");

    utilities.chooseCapturedFile("Cam", "/tmp/photo.png");

    verify(input).sendKeys("/tmp/photo.png");
    assertThrows(AssertionFailedError.class, () -> utilities.chooseCapturedFile("Missing", "/tmp/photo.png"));
  }

  @Test
  void shouldCheckTheFileTheClearButtonAndThePreviewOfAnUploader() {
    WebElement filename = show(instructions.getUploaderFilename("Upl"), "pixel.png");
    WebElement preview = show(instructions.getUploaderPreview("Upl"), "");
    when(preview.getAttribute("src")).thenReturn("blob:http://localhost/1234");
    show(instructions.getUploaderClear("Upl"), "Clear");

    assertThatCode(() -> {
      utilities.checkUploadedFile("Upl", "pixel.png");
      utilities.checkUploadPreview("Upl");
      utilities.clearUpload("Upl");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkUploadedFile("Upl", "other.pdf"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkNoUploadedFile("Upl"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkNoUploadPreview("Upl"));

    // A client that shows the name in the value of an input, with nothing after the clear
    when(filename.getText()).thenReturn("");
    when(filename.getAttribute("value")).thenReturn("pixel.png");
    assertThatCode(() -> utilities.checkUploadedFile("Upl", "pixel.png")).doesNotThrowAnyException();
    when(filename.getAttribute("value")).thenReturn("");
    when(preview.isDisplayed()).thenReturn(false);
    assertThatCode(() -> {
      utilities.checkNoUploadedFile("Upl");
      utilities.checkNoUploadPreview("Upl");
    }).doesNotThrowAnyException();
    when(preview.isDisplayed()).thenReturn(true);
    when(preview.getAttribute("src")).thenReturn("http://localhost/other.png");
    assertThrows(AssertionFailedError.class, () -> utilities.checkUploadPreview("Upl"));
  }

  @Test
  void shouldCheckTheRangeAndTheTotalOfTheGridPaginator() {
    show(instructions.getGridPaginationInfo(), "View 1 - 10 of 25");

    assertThatCode(() -> utilities.checkGridPaginationInfo("View 1 - 10 of 25")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridPaginationInfo("View 11 - 20 of 25"));
  }

  @Test
  void shouldMeasureTheDialogWithItsLayoutWidthSoTheOpeningTransitionDoesNotChangeIt() {
    show(instructions.getDialogPanel("Dlg"), "");
    when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(300L);

    utilities.checkDialogWidth("Dlg", 300);

    ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
    verify((JavascriptExecutor) driver).executeScript(script.capture(), any());
    assertThat(script.getValue()).contains("offsetWidth").doesNotContain("getBoundingClientRect");
  }

  @Test
  void shouldGoToTheNextPageOfAGrid() {
    show(instructions.getGridPageNext(), "");

    assertThatCode(() -> utilities.clickGridNextPage()).doesNotThrowAnyException();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGoToAPageOfAGridTypingItOnTheGoToPageInputAndPressingEnter() {
    // The input shows the current page, which is replaced by the typed one
    WebElement input = show(instructions.getGridGotoPage(), "");
    when(input.getAttribute("value")).thenReturn("1");
    List<Object> keysPressed = new ArrayList<>();
    doAnswer(invocation -> {
      ((Collection<Sequence>) invocation.getArgument(0)).forEach(sequence ->
        ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream()
          .filter(action -> "keyDown".equals(action.get("type"))).forEach(action -> keysPressed.add(action.get("value"))));
      return null;
    }).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.goToGridPage(3)).doesNotThrowAnyException();

    // The current page is cleared, and the page is typed and applied with Enter, as the user does
    verify(input).clear();
    assertThat(keysPressed).containsExactly("3", Keys.ENTER.toString());
  }

  @Test
  void shouldCheckAndChooseTheOptionsOfTheRadioOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      // Row 1 has the option B chosen, row 2 has none
      show(instructions.getGridRadioChecked("Grd", "1", "Rad", "B", true), "");
      show(instructions.getGridRadioChecked("Grd", "1", "Rad", null, true), "");
      show(instructions.getGridRadioInput("Grd", "2", "Rad"), "");
      show(instructions.getGridRadioOption("Grd", "2", "Rad", "A"), "");

      assertThatCode(() -> {
        utilities.checkGridRadioChoice("Grd", "1", "Rad", "B");
        utilities.checkGridRadioNoChoice("Grd", "2", "Rad");
        utilities.selectGridRadioOption("Grd", "2", "Rad", "A");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRadioChoice("Grd", "1", "Rad", "C"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRadioNoChoice("Grd", "1", "Rad"));
      assertThrows(AssertionFailedError.class, () -> utilities.selectGridRadioOption("Grd", "2", "Rad", "Missing"));
    }
  }

  @Test
  void shouldMoveTheChoiceOfAGridRowWithTheArrowKeysInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      // The choice of the cell moves from the option that is chosen, or from the first one when there is none
      show(instructions.getGridRadioInput("Grd", "1", "Rad"), "");
      show(instructions.getGridRadioChecked("Grd", "1", "Rad", null, true), "");
      List<Object> keysPressed = new ArrayList<>();
      doAnswer(invocation -> {
        ((Collection<Sequence>) invocation.getArgument(0)).forEach(sequence ->
          ((List<Map<String, Object>>) sequence.toJson().get("actions")).stream()
            .filter(action -> "keyDown".equals(action.get("type"))).forEach(action -> keysPressed.add(action.get("value"))));
        return null;
      }).when((Interactive) driver).perform(any());

      assertThatCode(() -> {
        utilities.moveGridRadioChoiceNext("Grd", "1", "Rad");
        utilities.moveGridRadioChoicePrevious("Grd", "1", "Rad");
      }).doesNotThrowAnyException();

      // The next option is reached with the right arrow and the previous one with the left arrow
      assertThat(keysPressed).containsExactly(Keys.ARROW_RIGHT.toString(), Keys.ARROW_LEFT.toString());
      assertThat(instructions.getGridRadioInput("Grd", "1", "Rad").toString()).endsWith("input[type='radio']");
    }
  }

  @Test
  void shouldCheckAndUseTheEmptyStateOfAGrid() {
    show(instructions.getGridEmpty(), "");
    show(instructions.getGridEmptyTitle(), "No records found");
    show(instructions.getGridEmptyText(), "Nothing to show here");
    show(instructions.getGridEmptyAction(), "Clear");

    assertThatCode(() -> {
      utilities.checkGridEmptyState("No records found", "Nothing to show here");
      utilities.clickGridEmptyAction();
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridEmptyState("Other title", "Nothing to show here"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkGridEmptyStateNotVisible());
  }

  @Test
  void shouldCheckThatTheEmptyStateIsNotShown() {
    assertThatCode(() -> utilities.checkGridEmptyStateNotVisible()).doesNotThrowAnyException();
  }

  @Test
  void shouldOpenTheRowDetailOfAGridRowAndCheckItsTextItsToggleAndItsLabelInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      WebElement toggle = show(instructions.getGridRowDetailToggle("Grd", "12"), "");
      when(toggle.getAttribute("aria-expanded")).thenReturn("true");
      when(toggle.getAttribute("aria-controls")).thenReturn("Grd-row-detail-12");
      when(toggle.getAttribute("aria-label")).thenReturn("Hide row details");
      WebElement detail = show(instructions.getGridRowDetail("Grd", "12"), "Theme: adminflare\nCode: THM-12");
      when(detail.getAttribute("id")).thenReturn("Grd-row-detail-12");

      assertThatCode(() -> {
        utilities.clickGridRowDetailToggle("Grd", "12");
        utilities.checkGridRowDetailOpen("Grd", "12", "Theme: adminflare\nCode: THM-12");
        utilities.checkGridRowDetailToggleLabel("Grd", "12", "Hide row details");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailOpen("Grd", "12", "Theme: asphalt"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailToggleLabel("Grd", "12", "Show row details"));
      assertThrows(AssertionFailedError.class, () -> utilities.clickGridRowDetailToggle("Grd", "15"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailOpen("Grd", "15", "Theme: asphalt"));
      // The toggle points to another detail, or it says that the detail is closed
      when(toggle.getAttribute("aria-controls")).thenReturn("Grd-row-detail-15");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailOpen("Grd", "12", "Theme: adminflare"));
      when(toggle.getAttribute("aria-controls")).thenReturn("Grd-row-detail-12");
      when(toggle.getAttribute("aria-expanded")).thenReturn("false");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailOpen("Grd", "12", "Theme: adminflare"));
    }
  }

  @Test
  void shouldCheckThatTheRowDetailOfAGridRowIsClosedAndThatNoneIsOpenInBothClients() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(argThat((String script) -> script != null && script.contains("aria-expanded")), any())).thenReturn(0L);
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      WebElement toggle = show(instructions.getGridRowDetailToggle("Grd", "15"), "");
      when(toggle.getAttribute("aria-expanded")).thenReturn("false");

      assertThatCode(() -> {
        utilities.checkGridRowDetailClosed("Grd", "15");
        utilities.checkNoGridRowDetailOpen("Grd");
      }).doesNotThrowAnyException();
      // The toggles are looked up by their hook, and the script reads which ones say that their detail is open
      verify(executor, atLeast(1)).executeScript(argThat((String script) -> script != null && script.contains("aria-expanded")),
        eq(instructions.getGridRowDetailToggle("Grd", null).toString().replace("By.cssSelector: ", "")));
      // The toggle says that the detail is open, or it is missing
      when(toggle.getAttribute("aria-expanded")).thenReturn("true");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailClosed("Grd", "15"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailClosed("Grd", "12"));
    }

    // A toggle says that its detail is open
    when(executor.executeScript(argThat((String script) -> script != null && script.contains("aria-expanded")), any())).thenReturn(1L);
    assertThrows(AssertionFailedError.class, () -> utilities.checkNoGridRowDetailOpen("Grd"));
  }

  @Test
  void shouldFailWhenARowDetailIsShownAlthoughItShouldBeClosed() {
    when(((JavascriptExecutor) driver).executeScript(anyString(), any())).thenReturn(0L);
    WebElement toggle = show(instructions.getGridRowDetailToggle("Grd", "15"), "");
    when(toggle.getAttribute("aria-expanded")).thenReturn("false");
    show(instructions.getGridRowDetail("Grd", "15"), "Theme: asphalt");
    show(instructions.getGridRowDetail("Grd", null), "Theme: asphalt");

    assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailClosed("Grd", "15"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkNoGridRowDetailOpen("Grd"));
  }

  @Test
  void shouldCheckTheStyleClassesAndTheComputedStyleOfTheRowDetailInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRowDetailWithClass("Grd", "12", "row-detail-card"), "");
      show(instructions.getGridRowDetail("Grd", "12"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("4px");

      assertThatCode(() -> {
        utilities.checkGridRowDetailHasClass("Grd", "12", "row-detail-card");
        utilities.checkGridRowDetailComputedStyle("Grd", "12", "border-left-width", "4px");
      }).doesNotThrowAnyException();
      ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
      verify(executor, atLeast(1)).executeScript(argThat((String script) -> script.contains("getComputedStyle")),
        arguments.capture());
      assertThat(arguments.getValue()).contains("border-left-width");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHasClass("Grd", "12", "other-card"));
      assertThrows(AssertionFailedError.class,
        () -> utilities.checkGridRowDetailComputedStyle("Grd", "12", "border-left-width", "1px"));
    }
  }

  @Test
  void shouldCheckThatTheRowDetailIsRightUnderItsRowAndAsWideAsItInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRow("Grd", "12"), "");
      show(instructions.getGridRowDetail("Grd", "12"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;

      // Box of the row and box of the detail: top, bottom and width
      when(executor.executeScript(anyString(), any())).thenReturn("100|132|800", "133|233|790");
      assertThatCode(() -> utilities.checkGridRowDetailUnderItsRow("Grd", "12")).doesNotThrowAnyException();

      // Far below the row, over the row, or much narrower than the row
      when(executor.executeScript(anyString(), any())).thenReturn("100|132|800", "160|260|790");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailUnderItsRow("Grd", "12"));
      when(executor.executeScript(anyString(), any())).thenReturn("100|132|800", "120|220|790");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailUnderItsRow("Grd", "12"));
      when(executor.executeScript(anyString(), any())).thenReturn("100|132|800", "133|233|600");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailUnderItsRow("Grd", "12"));
    }
  }

  @Test
  void shouldCheckThatTheRowDetailIsAsWideAsTheVisiblePartOfItsGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRowDetail("Grd", "12"), "");
      show(instructions.getGridScrollZone("Grd"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;

      // Box of the detail (top, bottom and width) and width of the visible part of the grid
      when(executor.executeScript(anyString(), any())).thenReturn("133|233|1250", 1260L);
      assertThatCode(() -> utilities.checkGridRowDetailAcrossGrid("Grd", "12")).doesNotThrowAnyException();

      // Only as wide as the columns of the grid, or a grid without width
      when(executor.executeScript(anyString(), any())).thenReturn("133|233|390", 1260L);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailAcrossGrid("Grd", "12"));
      when(executor.executeScript(anyString(), any())).thenReturn("133|233|390", 0L);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailAcrossGrid("Grd", "12"));
    }
  }

  @Test
  void shouldCheckThatTheRowDetailShowsANestedGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRowDetailNestedGrid("Grd", "1", "GrdCus-1"), "");

      assertThatCode(() -> utilities.checkGridRowDetailHasNestedGrid("Grd", "1", "GrdCus-1")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHasNestedGrid("Grd", "6", "GrdCus-6"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHasNestedGrid("Grd", "1", "GrdCus-6"));
    }
  }

  @Test
  void shouldCheckThatTheRowDetailHoldsItsWholeNestedGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRowDetail("Grd", "1"), "");
      show(instructions.getGridRowDetailNestedGrid("Grd", "1", "GrdCus-1"), "");
      show(instructions.getGridRow("GrdCus-1", null), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;

      // Edges of the detail and of the nested grid (top, bottom, left and right), and bottom of the last nested row
      when(executor.executeScript(anyString(), any())).thenReturn("100|400|0|1000", "110|390|10|990", 380.0);
      assertThatCode(() -> utilities.checkGridRowDetailHoldsNestedGrid("Grd", "1", "GrdCus-1")).doesNotThrowAnyException();

      // The detail cuts the nested grid at the bottom or at a side, the last row is out of the nested grid, or the
      // nested grid has no height
      when(executor.executeScript(anyString(), any())).thenReturn("100|400|0|1000", "110|450|10|990", 380.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHoldsNestedGrid("Grd", "1", "GrdCus-1"));
      when(executor.executeScript(anyString(), any())).thenReturn("100|400|0|1000", "110|390|10|1100", 380.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHoldsNestedGrid("Grd", "1", "GrdCus-1"));
      when(executor.executeScript(anyString(), any())).thenReturn("100|400|0|1000", "110|390|10|990", 420.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHoldsNestedGrid("Grd", "1", "GrdCus-1"));
      when(executor.executeScript(anyString(), any())).thenReturn("100|400|0|1000", "110|110|10|990", 110.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowDetailHoldsNestedGrid("Grd", "1", "GrdCus-1"));
    }
  }

  @Test
  void shouldCheckThatAGridIsNotInThePageInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGrid("GrdCus-1"), "");

      assertThatCode(() -> utilities.checkGridNotPresent("GrdCus-6")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridNotPresent("GrdCus-1"));
    }
  }

  @Test
  void shouldCheckThatAGridRowIsNotSelectedInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridRowSelected("Grd", "12", false), "");

      assertThatCode(() -> utilities.checkGridRowNotSelected("Grd", "12")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowNotSelected("Grd", "15"));
    }
  }

  @Test
  void shouldToggleTheRowDetailWithEnterAndSpaceOnItsFocusedToggleInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      WebElement toggle = show(instructions.getGridRowDetailToggle("Grd", "10"), "");
      List<Object> keysPressed = new ArrayList<>();
      List<Object> pointerActions = new ArrayList<>();
      doAnswer(invocation -> {
        ((Collection<Sequence>) invocation.getArgument(0)).forEach(sequence ->
          ((List<Map<String, Object>>) sequence.toJson().get("actions")).forEach(action -> {
            if ("keyDown".equals(action.get("type"))) {
              keysPressed.add(action.get("value"));
            } else if ("pointerDown".equals(action.get("type"))) {
              pointerActions.add(action);
            }
          }));
        return null;
      }).when((Interactive) driver).perform(any());

      assertThatCode(() -> {
        utilities.pressEnterOnGridRowDetailToggle("Grd", "10");
        utilities.pressSpaceOnGridRowDetailToggle("Grd", "10");
      }).doesNotThrowAnyException();

      // The toggle gets the focus as when the user tabs to it, never a click, which would toggle it too
      verify((JavascriptExecutor) driver, times(2)).executeScript(argThat((String script) -> script.contains("focus()")), any());
      assertThat(keysPressed).containsExactly(Keys.ENTER.toString());
      verify(toggle).sendKeys(" ");
      assertThat(pointerActions).isEmpty();
      verify(toggle, times(0)).click();
      clearInvocations(driver);
      assertThrows(AssertionFailedError.class, () -> utilities.pressEnterOnGridRowDetailToggle("Grd", "11"));
    }
  }

  @Test
  void shouldCheckTheHeaderOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridHeader("Grd", "Col"), "Column");

      assertThatCode(() -> utilities.checkGridHeaderVisible("Grd", "Col")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridHeaderNotVisible("Grd", "Col"));
      assertThatCode(() -> utilities.checkGridHeaderNotVisible("Grd", "Other")).doesNotThrowAnyException();
    }
  }

  @Test
  void shouldCheckTheFirstRowOfAGridStartsAtTheTopOfTheGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "1", "Col"), "7");
      show(instructions.getGrid("Grd"), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(100.0, 100.5);

      assertThatCode(() -> utilities.checkGridFirstRowAtTop("Grd", "1", "Col")).doesNotThrowAnyException();

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(126.0, 100.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridFirstRowAtTop("Grd", "1", "Col"));
    }
  }

  @Test
  void shouldCheckTheStyleClassOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCellWithClass("Grd", "1", "Col", "scale-seq-3"), "7");

      assertThatCode(() -> utilities.checkCellHasClass("Grd", "1", "Col", "scale-seq-3")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkCellHasClass("Grd", "1", "Col", "scale-neg-1"));
    }
  }

  @Test
  void shouldCheckTheSecondaryLineOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "1", "Col"), "Javier");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith(".awe-cell-secondary"), any()))
        .thenReturn("Javier García\njgarcia j.garcia@almis.example");

      assertThatCode(() -> utilities.checkGridCellSecondaryLine("Grd", "1", "Col", "Javier García",
        "jgarcia j.garcia@almis.example")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellSecondaryLine("Grd", "1", "Col", "Javier García", "other"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellSecondaryLine("Grd", "1", "Col", "Javier García", null));

      // Without a secondary value the cell shows only the main line
      when(executor.executeScript(scriptWith(".awe-cell-secondary"), any()))
        .thenReturn("Pablo Díaz");
      assertThatCode(() -> utilities.checkGridCellSecondaryLine("Grd", "1", "Col", "Pablo Díaz", null)).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellSecondaryLine("Grd", "1", "Col", "Pablo Díaz", ""));
    }
  }

  @Test
  void shouldCheckTheStatusOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "1", "Col"), "Locked");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith(".awe-cell-status"), any()))
        .thenReturn("danger\ntrue\nLocked");

      assertThatCode(() -> utilities.checkGridCellStatus("Grd", "1", "Col", "Locked", "danger")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellStatus("Grd", "1", "Col", "Locked", "neutral"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellStatus("Grd", "1", "Col", "Active", "danger"));

      // The tone class must match the tone, and a cell without status fails
      when(executor.executeScript(scriptWith(".awe-cell-status"), any()))
        .thenReturn("danger\nfalse\nLocked");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellStatus("Grd", "1", "Col", "Locked", "danger"));
      when(executor.executeScript(scriptWith(".awe-cell-status"), any())).thenReturn("");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellStatus("Grd", "1", "Col", "Locked", "danger"));
    }
  }

  @Test
  void shouldCheckTheChipsOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "1", "Col"), "Sales");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith(".awe-cell-chips"), any()))
        .thenReturn("Sales\nFinance");
      when(executor.executeScript(scriptWith(".awe-cell-chip-more"), any()))
        .thenReturn("+2\nLogistics, Support\n2 more: Logistics, Support");

      assertThatCode(() -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("Sales", "Finance"),
        List.of("Logistics", "Support"))).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("Sales"),
        List.of("Logistics", "Support")));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("Sales", "Finance"),
        List.of("Logistics")));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("Sales", "Finance"),
        List.of()));

      // Without hidden items there is no "+n" chip
      when(executor.executeScript(scriptWith(".awe-cell-chips"), any())).thenReturn("A\nB");
      when(executor.executeScript(scriptWith(".awe-cell-chip-more"), any())).thenReturn("");
      assertThatCode(() -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("A", "B"), List.of())).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("A", "", "B"), List.of()));

      // A cell without chips fails
      when(executor.executeScript(scriptWith(".awe-cell-chips"), any())).thenReturn(null);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridCellChips("Grd", "1", "Col", List.of("A", "B"), List.of()));
    }
  }

  @Test
  void shouldCheckThatAGridRowIsNotCutInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "6", "Col"), "Carlos");
      JavascriptExecutor executor = (JavascriptExecutor) driver;

      // The script walks up from the row to the grid and lists the ancestors that clip it
      when(executor.executeScript(scriptWith("overflowY"), any())).thenReturn("");
      assertThatCode(() -> utilities.checkGridRowNotCut("Grd", "6", "Col")).doesNotThrowAnyException();

      // An ancestor that cuts the row, or a cell without its row, fails
      when(executor.executeScript(scriptWith("overflowY"), any())).thenReturn("DIV.ui-grid-canvas");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowNotCut("Grd", "6", "Col"));
      when(executor.executeScript(scriptWith("overflowY"), any())).thenReturn(null);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridRowNotCut("Grd", "6", "Col"));
    }
  }

  // Rows that a grouped grid shows, as the script that reads them returns them: two regions, the first one expanded
  private static final String GROUPED_ROWS = String.join("\n",
    "group|1|Europe|true|Europe 3",
    "group|2|Europe/Spain|true|Spain 2",
    "row|1",
    "row|2",
    "subtotal|2|Europe/Spain",
    "group|2|Europe/France|false|France 1",
    "subtotal|1|Europe",
    "group|1|America|false|America 4");

  @Test
  void shouldCountAndOrderTheGroupRowsOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGrid("Grd"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn(GROUPED_ROWS);

      assertThatCode(() -> {
        utilities.checkGridGroupRowCount("Grd", 1, 2);
        utilities.checkGridGroupRowCount("Grd", 2, 2);
        utilities.checkGridGroupRowCount("Grd", 3, 0);
        utilities.checkGridGroupOrder("Grd", 1, "Europe", "America");
        utilities.checkGridGroupOrder("Grd", 2, "Europe/Spain", "Europe/France");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupRowCount("Grd", 1, 3));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupOrder("Grd", 1, "America", "Europe"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupOrder("Grd", 2, "Europe/Spain"));
    }
  }

  @Test
  void shouldCheckTheLabelAndTheStateOfTheGroupsOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGrid("Grd"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn(GROUPED_ROWS);

      assertThatCode(() -> {
        utilities.checkGridGroupLabel("Grd", "Europe", "Europe", 3);
        utilities.checkGridGroupLabel("Grd", "Europe/Spain", "Spain", 2);
        utilities.checkGridGroupExpanded("Grd", "Europe");
        utilities.checkGridGroupExpanded("Grd", "Europe/Spain");
        utilities.checkGridGroupCollapsed("Grd", "Europe/France");
        utilities.checkGridGroupCollapsed("Grd", "America");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupLabel("Grd", "Europe", "Europe", 4));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupLabel("Grd", "Asia", "Asia", 1));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupCollapsed("Grd", "Europe"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupExpanded("Grd", "America"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupExpanded("Grd", "Asia"));

      // A group that says it is collapsed but still shows its rows, or one that is expanded without content
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any()))
        .thenReturn("group|1|Europe|false|Europe 1\nrow|1\ngroup|1|America|true|America 1\ngroup|1|Asia|true|Asia 1\nrow|2");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupCollapsed("Grd", "Europe"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupExpanded("Grd", "America"));
      assertThatCode(() -> utilities.checkGridGroupExpanded("Grd", "Asia")).doesNotThrowAnyException();
    }
  }

  @Test
  void shouldCheckThatAGridIsNotGroupedInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGrid("Grd"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn("row|1\nrow|2");
      assertThatCode(() -> utilities.checkGridNotGrouped("Grd")).doesNotThrowAnyException();

      // Group or subtotal rows, or no row at all
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn(GROUPED_ROWS);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridNotGrouped("Grd"));
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn("row|1\nsubtotal|1|Europe");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridNotGrouped("Grd"));
      when(executor.executeScript(scriptWith("grid-subtotal-row"), any())).thenReturn("");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridNotGrouped("Grd"));
    }
  }

  @Test
  void shouldCheckTheCellsOfTheGroupAndSubtotalRowsOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridGroupCell("Grd", "Europe/Spain", "sales"), "500");
      show(instructions.getGridSubtotalCell("Grd", "Europe/Spain", "sales"), "500");
      show(instructions.getGridSubtotalCell("Grd", "Europe/Spain", "region"), "Subtotal Spain");

      assertThatCode(() -> {
        utilities.checkGridGroupCell("Grd", "Europe/Spain", "sales", "500");
        utilities.checkGridSubtotalCell("Grd", "Europe/Spain", "sales", "500");
        utilities.checkGridSubtotalCell("Grd", "Europe/Spain", "region", "Subtotal Spain");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupCell("Grd", "Europe/Spain", "sales", "350"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSubtotalCell("Grd", "Europe/France", "sales", "350"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupCell("Grd", "Europe/Spain", "units", "50"));
    }
  }

  @Test
  void shouldLookTheGroupRowsUpByTheirPathInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      String cell = client.getGridGroupCell("Grd", "America/USA/New%20York", "sales").toString();
      String subtotal = client.getGridSubtotalCell("Grd", "Europe", "sales").toString();
      String toggle = client.getGridGroupToggle("Grd", "Europe").toString();

      assertThat(cell).contains("[grid-id='Grd']", TestIds.css(TestIds.GRID_GROUP_ROW),
        "[data-group-path='America/USA/New%20York']", TestIds.css(TestIds.GRID_CELL), "[column-id='sales']");
      assertThat(subtotal).contains(TestIds.css(TestIds.GRID_SUBTOTAL_ROW), "[data-group-path='Europe']", "[column-id='sales']");
      assertThat(toggle).contains(TestIds.css(TestIds.GRID_GROUP_ROW), "[data-group-path='Europe']", TestIds.css(TestIds.GRID_GROUP_TOGGLE));
      // The toolbar and the Group by panel of the grid are named by their owner, and the option of a column is its checkbox
      assertThat(client.getGridGroupExpandAll("Grd").toString()).contains(TestIds.css(TestIds.GRID_TOOLBAR),
        TestAttributes.css(TestAttributes.OWNER, "Grd"), TestIds.css(TestIds.GRID_GROUP_EXPAND_ALL));
      assertThat(client.getGridGroupOption("Grd", "city").toString()).contains(TestIds.css(TestIds.GRID_GROUP_BY_PANEL),
        TestAttributes.css(TestAttributes.OWNER, "Grd"), "[data-column='city']", "input");
    }
  }

  @Test
  void shouldToggleAGroupAndExpandAndCollapseEveryGroupOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridGroupToggle("Grd", "Europe"), "");
      show(instructions.getGridGroupExpandAll("Grd"), "");
      show(instructions.getGridGroupCollapseAll("Grd"), "");

      long start = performed();
      utilities.toggleGridGroup("Grd", "Europe");
      long toggled = performed();
      assertThat(toggled).isGreaterThan(start);
      utilities.collapseAllGridGroups("Grd");
      long collapsed = performed();
      assertThat(collapsed).isGreaterThan(toggled);
      utilities.expandAllGridGroups("Grd");
      assertThat(performed()).isGreaterThan(collapsed);
      assertThrows(AssertionFailedError.class, () -> utilities.toggleGridGroup("Grd", "America"));
      assertThrows(AssertionFailedError.class, () -> utilities.expandAllGridGroups("Other"));
    }
  }

  @Test
  void shouldChooseTheGroupByColumnsOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridGroupByButton("Grd"), "Group by");
      show(instructions.getGridGroupNone("Grd"), "No grouping");
      show(instructions.getGridGroupOption("Grd", "region"), "");
      show(instructions.getGridGroupOption("Grd", "city"), "");
      By panel = Locator.from(instructions.getGridGroupByPanel("Grd")).toBy();
      show(instructions.getGridGroupByPanel("Grd"), "");
      // The panel closes on the second click of the Group by button (the fifth click)
      AtomicInteger clicks = new AtomicInteger();
      doAnswer(invocation -> {
        if (clicks.incrementAndGet() == 5) {
          when(driver.findElements(argThat(panel::equals))).thenReturn(Collections.emptyList());
        }
        return null;
      }).when((Interactive) driver).perform(any());

      // No grouping, then each column in order: the button, None, two options and the button again
      assertThatCode(() -> utilities.groupGridBy("Grd", "region", "city")).doesNotThrowAnyException();
      assertThat(clicks.get()).isEqualTo(5);
      assertThrows(AssertionFailedError.class, () -> utilities.groupGridBy("Other"));
    }
  }

  @Test
  void shouldCheckTheTotalOfTheFooterOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGrid("Grd"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      when(executor.executeScript(scriptWith("tfoot"), any(), eq("sales"))).thenReturn("2.450");

      assertThatCode(() -> utilities.checkGridFooterTotal("Grd", "sales", "2.450")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridFooterTotal("Grd", "sales", "850"));
      when(executor.executeScript(scriptWith("tfoot"), any(), eq("sales"))).thenReturn(null);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridFooterTotal("Grd", "sales", "2.450"));
    }
  }

  @Test
  void shouldCheckThatNoCallReachedTheServerSinceTheWatchStarted() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(scriptWith("clearResourceTimings"))).thenReturn(null);
    when(executor.executeScript(scriptWith("getEntriesByType"))).thenReturn("");

    assertThatCode(() -> {
      utilities.watchServerCalls();
      utilities.checkNoServerCalls();
    }).doesNotThrowAnyException();
    verify(executor).executeScript(scriptWith("clearResourceTimings"));

    // A call to the server since the watch started
    when(executor.executeScript(scriptWith("getEntriesByType"))).thenReturn("http://localhost:8080/action/data/GroupRowsTst");
    assertThrows(AssertionFailedError.class, () -> utilities.checkNoServerCalls());
  }

  @Test
  void shouldCheckTheWidthOfAnOpenDialogInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getDialogPanel("Dlg"), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(900.4);

      assertThatCode(() -> utilities.checkDialogWidth("Dlg", 900)).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkDialogWidth("Dlg", 300));
    }
  }

  @Test
  void shouldCloseASidePanelWithItsCloseButtonAndCheckThatItIsClosed() {
    show(instructions.getSidePanelClose("Pnl"), "");
    show(instructions.getOpenSidePanel("Open"), "");

    assertThatCode(() -> {
      utilities.closeSidePanel("Pnl");
      utilities.checkSidePanelClosed("Closed");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.closeSidePanel("Missing"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkSidePanelClosed("Open"));

    verify((Interactive) driver, atLeast(1)).perform(any());
  }

  @Test
  void shouldCloseAnOpenSidePanelWithEscape() {
    WebElement panel = show(instructions.getOpenSidePanel("Pnl"), "");
    AtomicInteger escapes = new AtomicInteger();
    when(panel.isDisplayed()).thenAnswer(invocation -> escapes.get() == 0);
    doAnswer(invocation -> escapes.incrementAndGet()).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.closeSidePanelWithEscape("Pnl")).doesNotThrowAnyException();
    assertThat(escapes).hasValue(1);
    // A panel that is not open cannot be closed
    assertThrows(AssertionFailedError.class, () -> utilities.closeSidePanelWithEscape("Missing"));
    assertThat(escapes).hasValue(1);
  }

  @Test
  void shouldCheckThatASidePanelIsOpenOnItsEdgeWithItsMaximumWidthInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getSidePanelPanel("Pnl"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      // Gap to the left edge | gap to the right edge | width | maximum width in pixels (capped by the viewport)
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("544|0|480.4|480");

      assertThatCode(() -> utilities.checkSidePanelOpen("Pnl", "right", "30rem")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkSidePanelOpen("Pnl", "left", "30rem"));

      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("1|703|320|320");
      assertThatCode(() -> utilities.checkSidePanelOpen("Pnl", "left", "20rem")).doesNotThrowAnyException();

      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("0|704|300|320");
      assertThrows(AssertionFailedError.class, () -> utilities.checkSidePanelOpen("Pnl", "left", "20rem"));
    }
  }

  @Test
  void shouldMeasureTheSidePanelResolvingItsMaximumWidthInTheBrowser() {
    show(instructions.getSidePanelPanel("Pnl"), "");
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("544|0|480|480");

    utilities.checkSidePanelOpen("Pnl", "right", "30rem");

    ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
    verify(executor, atLeast(1)).executeScript(anyString(), arguments.capture());
    assertThat(arguments.getAllValues()).anySatisfy(values -> assertThat(values).contains("30rem"));
  }

  @Test
  void shouldCheckThatTheFocusStaysInsideTheSidePanelWhileTabbing() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    String panelCss = instructions.getSidePanelCss("Pnl");
    when(executor.executeScript(anyString(), eq(panelCss))).thenReturn(true);

    assertThatCode(() -> utilities.checkSidePanelKeepsTheFocus("Pnl", 3)).doesNotThrowAnyException();
    verify((Interactive) driver, times(3)).perform(any());

    when(executor.executeScript(anyString(), eq(panelCss))).thenReturn(true, false);
    assertThrows(AssertionFailedError.class, () -> utilities.checkSidePanelKeepsTheFocus("Pnl", 3));
  }

  @Test
  void shouldCheckThatAButtonHasTheFocus() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(anyString(), eq(instructions.getButtonCss("ButOpen")))).thenReturn(true);

    assertThatCode(() -> utilities.checkButtonFocused("ButOpen")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkButtonFocused("ButOther"));
  }

  @Test
  void shouldCloseAPopoverWithItsCloseButtonAndCheckThatItIsClosed() {
    show(instructions.getPopoverClose("Pop"), "");
    show(instructions.getOpenPopover("Open"), "");

    assertThatCode(() -> {
      utilities.closePopover("Pop");
      utilities.checkPopoverClosed("Closed");
    }).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.closePopover("Missing"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverClosed("Open"));

    verify((Interactive) driver, atLeast(1)).perform(any());
  }

  @Test
  void shouldCloseAnOpenPopoverWithEscape() {
    WebElement popover = show(instructions.getOpenPopover("Pop"), "");
    AtomicInteger escapes = new AtomicInteger();
    when(popover.isDisplayed()).thenAnswer(invocation -> escapes.get() == 0);
    doAnswer(invocation -> escapes.incrementAndGet()).when((Interactive) driver).perform(any());

    assertThatCode(() -> utilities.closePopoverWithEscape("Pop")).doesNotThrowAnyException();
    assertThat(escapes).hasValue(1);
    // A popover that is not open cannot be closed
    assertThrows(AssertionFailedError.class, () -> utilities.closePopoverWithEscape("Missing"));
    assertThat(escapes).hasValue(1);
  }

  @Test
  void shouldCheckThatAPopoverIsOpenNextToItsAnchorInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getOpenPopover("Pop"), "");
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      // Popover top|right|bottom|left, then anchor top|right|bottom|left: below the anchor, 8 pixels away
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("148|420|300|100|110|180|140|100");

      assertThatCode(() -> utilities.checkPopoverOpen("Pop", "ButPop", "bottom")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "right"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "top"));

      // At the right of the anchor, alongside it
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("90|668|250|188|110|180|140|100");
      assertThatCode(() -> utilities.checkPopoverOpen("Pop", "ButPop", "right")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "left"));

      // On its side but far from the anchor, or not alongside it, or the anchor is not found
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("200|420|300|100|110|180|140|100");
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "bottom"));
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("148|620|300|300|110|180|140|100");
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "bottom"));
      when(executor.executeScript(anyString(), any(Object[].class))).thenReturn(null);
      assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverOpen("Pop", "Missing", "bottom"));
    }
  }

  @Test
  void shouldMeasureThePopoverAgainstTheAnchorItIsGiven() {
    show(instructions.getOpenPopover("Pop"), "");
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    when(executor.executeScript(anyString(), any(Object[].class))).thenReturn("148|420|300|100|110|180|140|100");

    utilities.checkPopoverOpen("Pop", "ButPop", "bottom");

    ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
    verify(executor, atLeast(1)).executeScript(anyString(), arguments.capture());
    assertThat(arguments.getAllValues()).anySatisfy(values -> assertThat(values).contains("ButPop"));
    assertThrows(IllegalArgumentException.class, () -> utilities.checkPopoverOpen("Pop", "ButPop", "center"));
  }

  @Test
  void shouldCheckThatTheFocusStaysInsideThePopoverWhileTabbing() {
    JavascriptExecutor executor = (JavascriptExecutor) driver;
    String popoverCss = instructions.getPopoverCss("Pop");
    when(executor.executeScript(anyString(), eq(popoverCss))).thenReturn(true);

    assertThatCode(() -> utilities.checkPopoverKeepsTheFocus("Pop", 3)).doesNotThrowAnyException();
    verify((Interactive) driver, times(3)).perform(any());

    when(executor.executeScript(anyString(), eq(popoverCss))).thenReturn(true, false);
    assertThrows(AssertionFailedError.class, () -> utilities.checkPopoverKeepsTheFocus("Pop", 3));
  }
  @Test
  void shouldCheckTheValueOfACardWhateverTheSpacesBetweenItsNumberAndItsUnitInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getCardValue("Crd"), "412,600\n\u20ac");

      assertThatCode(() -> {
        utilities.checkCardValue("Crd", "412,600 \u20ac");
        utilities.checkCardValue("Crd", "412,600\u20ac");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkCardValue("Crd", "412,601 \u20ac"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkCardValue("Missing", "412,600 \u20ac"));
    }
  }

  @Test
  void shouldCheckTheTextTheTrendAndTheToneOfTheDeltaOfACard() {
    WebElement delta = show(instructions.getCardDelta("Crd"), "\u2198 \u22120.18 pt Down");
    when(delta.getAttribute("data-trend")).thenReturn("down");
    when(delta.getAttribute("data-tone")).thenReturn("success");
    show(instructions.getCardDeltaText("Crd"), "\u22120.18 pt");

    assertThatCode(() -> utilities.checkCardDelta("Crd", "\u22120.18 pt", "down", "success")).doesNotThrowAnyException();
    // The minus sign is U+2212, not a hyphen
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardDelta("Crd", "-0.18 pt", "down", "success"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardDelta("Crd", "\u22120.18 pt", "up", "success"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardDelta("Crd", "\u22120.18 pt", "down", "danger"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardDelta("Missing", "\u22120.18 pt", "down", "success"));
  }

  @Test
  void shouldCheckTheTextAndTheToneOfTheChipOfACard() {
    WebElement chip = show(instructions.getCardChip("Crd"), "On track");
    when(chip.getAttribute("data-tone")).thenReturn("success");

    assertThatCode(() -> utilities.checkCardChip("Crd", "On track", "success")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChip("Crd", "Off track", "success"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChip("Crd", "On track", "warning"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChip("Missing", "On track", "success"));
  }

  @Test
  void shouldCheckThatTheChipOfACardChangesWhenAButtonIsClicked() {
    WebElement chip = show(instructions.getCardChip("Crd"), "");
    // The chip shows the next status once the button is clicked
    when(chip.getText()).thenAnswer(invocation -> utilities.events.contains("click:ButReload") ? "Degraded" : "Operational");
    when(chip.getAttribute("data-tone")).thenAnswer(invocation -> utilities.events.contains("click:ButReload") ? "warning" : "success");

    assertThatCode(() -> utilities.checkCardChipChangesOnClick("Crd", "ButReload")).doesNotThrowAnyException();
    assertThat(utilities.events).containsExactly("click:ButReload");
  }

  @Test
  void shouldFailWhenTheChipOfACardDoesNotChangeOrHasAnUnknownToneAfterTheClick() {
    WebElement chip = show(instructions.getCardChip("Crd"), "Operational");
    when(chip.getAttribute("data-tone")).thenReturn("success");

    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChipChangesOnClick("Crd", "ButReload"));

    when(chip.getText()).thenAnswer(invocation -> utilities.events.isEmpty() ? "Operational" : "Degraded");
    when(chip.getAttribute("data-tone")).thenReturn("orange");
    utilities.events.clear();
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChipChangesOnClick("Crd", "ButReload"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardChipChangesOnClick("Missing", "ButReload"));
  }

  @Test
  void shouldCheckTheProgressOfACardTowardsItsTarget() {
    WebElement progress = show(instructions.getCardProgress("Crd"), "");
    when(progress.getAttribute("aria-valuenow")).thenReturn("83");

    assertThatCode(() -> utilities.checkCardProgress("Crd", 83)).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardProgress("Crd", 82));
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardProgress("Missing", 83));
  }

  @Test
  void shouldCheckThatAComponentIsInTheBodyOfACard() {
    show(instructions.getCardBodyComponent("CrdGrid", "GrdCrd"), "");

    assertThatCode(() -> utilities.checkCardBodyComponent("CrdGrid", "GrdCrd")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkCardBodyComponent("CrdChart", "GrdCrd"));
  }

  @Test
  void shouldOpenTheUserMenuToSwitchTheColorMode() {
    utilities.switchToDarkMode();
    utilities.switchToLightMode();

    assertThat(utilities.events).containsExactly("user-menu", "option:dark-mode", "user-menu", "option:light-mode");
  }

  @Test
  void shouldNotCloseTheUserMenuThatIsAlreadyOpenWhenSwitchingTheColorMode() {
    show(By.cssSelector(instructions.getCriterionCss("theme")), "Theme");

    utilities.switchToLightMode();

    assertThat(utilities.events).containsExactly("option:light-mode");
  }

  @Test
  void shouldChooseTheLightModeWhenTheUserMenuOffersIt() {
    show(instructions.getUserMenuOption("light-mode"), "Light mode");
    show(instructions.getColorModePage("light"), "");

    utilities.switchToLightModeIfDark();

    assertThat(utilities.events).containsExactly("user-menu", "option:light-mode");
  }

  @Test
  void shouldFailWhenTheChosenLightModeIsNeverApplied() {
    show(instructions.getUserMenuOption("light-mode"), "Light mode");

    assertThrows(AssertionFailedError.class, () -> utilities.switchToLightModeIfDark());
  }

  @Test
  void shouldKeepTheModeWhenTheUserMenuDoesNotOfferTheLightMode() {
    utilities.switchToLightModeIfDark();

    assertThat(utilities.events).containsExactly("user-menu");
  }

  @Test
  void shouldSelectAThemeFromTheUserMenu() {
    utilities.selectTheme("sunset");

    assertThat(utilities.events).containsExactly("user-menu", "select:theme:sunset");
  }

  @Test
  void shouldCheckThatAThemeAndAColorModeAreApplied() {
    show(instructions.getThemedPage("sunset"), "");
    show(instructions.getColorModePage("dark"), "");

    assertThatCode(() -> utilities.checkThemeApplied("sunset")).doesNotThrowAnyException();
    assertThatCode(() -> utilities.checkColorMode("dark")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkThemeApplied("sky"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkColorMode("light"));
    assertThrows(IllegalArgumentException.class, () -> utilities.checkColorMode("blue"));
  }

  @Test
  void shouldCheckTheModeAndThePrimaryOfTheThemeOnTheHtmlElement() {
    show(By.cssSelector("html.dark:not(.light)[data-awe-primary='red']"), "");

    assertThatCode(() -> utilities.checkThemeAppearance("dark", "red")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkThemeAppearance("light", "red"));
    assertThrows(AssertionFailedError.class, () -> utilities.checkThemeAppearance("dark", "blue"));
    assertThrows(IllegalArgumentException.class, () -> utilities.checkThemeAppearance("blue", "red"));
  }

  @Test
  void shouldCheckTheComputedStyleOfAnElementIgnoringCaseAndSpaces() {
    when(((JavascriptExecutor) driver).executeScript(anyString(), eq("body"), eq("background-color")))
      .thenReturn(" RGB(248, 250, 252) ");

    assertThatCode(() -> utilities.checkComputedStyle("body", "background-color", "rgb(248, 250, 252)"))
      .doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class,
      () -> utilities.checkComputedStyle("body", "background-color", "rgb(9, 9, 11)"));
    // An element that is not on the page has no style
    assertThrows(AssertionFailedError.class,
      () -> utilities.checkComputedStyle(".missing", "background-color", "null"));
  }

  @Test
  void shouldCheckTheValueOfADesignTokenIgnoringCaseAndSpaces() {
    when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(" #9D4444 ");

    assertThatCode(() -> utilities.checkDesignToken("--awe-color-primary", "#9d4444")).doesNotThrowAnyException();
    assertThrows(AssertionFailedError.class, () -> utilities.checkDesignToken("--awe-color-primary", "#000000"));
    assertThrows(IllegalArgumentException.class, () -> utilities.checkDesignToken("color-primary", "#000000"));
  }

  @Test
  void shouldCheckTheTypeAndThePointsOfTheSparklineOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridSparkline("Grd", "1", "Col"), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn("bar|3,5,-1,8");

      assertThatCode(() -> utilities.checkGridSparkline("Grd", "1", "Col", "bar", "3,5,-1,8")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparkline("Grd", "1", "Col", "line", "3,5,-1,8"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparkline("Grd", "1", "Col", "bar", "3,5,-1"));
    }
  }

  @Test
  void shouldCheckThatTheSparklineOfAGridCellIsEmptyInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "6", "Col"), "");
      show(instructions.getGridCell("Grd", "1", "Col"), "");
      show(instructions.getGridSparkline("Grd", "1", "Col"), "");

      assertThatCode(() -> utilities.checkGridSparklineEmpty("Grd", "6", "Col")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparklineEmpty("Grd", "1", "Col"));
    }
  }

  @Test
  void shouldNotCheckTheEmptySparklineWhileTheGridIsStillLoadingInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridCell("Grd", "6", "Col"), "");
      // The grid loader is still shown: the row may not be drawn yet, so the empty cell proves nothing
      show(instructions.getGridLoaderSelector(), "");

      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparklineEmpty("Grd", "6", "Col"));
    }
  }

  @Test
  void shouldCountTheMarkersOfTheSparklineOfAGridCellInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridSparkline("Grd", "1", "Col"), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(3L);

      assertThatCode(() -> utilities.checkGridSparklineMarkers("Grd", "1", "Col", 3)).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparklineMarkers("Grd", "1", "Col", 2));
    }
  }

  @Test
  void shouldCheckThatTheSparklineOfAGridCellDoesNotTakeTheFocusInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridSparkline("Grd", "1", "Col"), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn("false|0");

      assertThatCode(() -> utilities.checkGridSparklineNotFocusable("Grd", "1", "Col")).doesNotThrowAnyException();

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn("false|1");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparklineNotFocusable("Grd", "1", "Col"));
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn("true|0");
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridSparklineNotFocusable("Grd", "1", "Col"));
    }
  }

  private WebElement option(String text, boolean selected) {
    WebElement option = mock(WebElement.class);
    when(option.getText()).thenReturn(text);
    when(option.isSelected()).thenReturn(selected);
    return option;
  }

  /** Records the interactions that need a real browser */
  /**
   * Number of actions the browser has been asked to perform (a click is one)
   */
  private long performed() {
    return org.mockito.Mockito.mockingDetails(driver).getInvocations().stream()
      .filter(invocation -> invocation.getMethod().getName().equals("perform")).count();
  }

  @Test
  void shouldOpenAndCloseTheColumnsDialogOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsButton("Grd"), "Columns");
      show(instructions.getGridColumnsDialog("Grd"), "");
      show(instructions.getGridColumnsClose("Grd"), "Close");
      By dialog = Locator.from(instructions.getGridColumnsDialog("Grd")).toBy();
      long before = performed();

      utilities.openGridColumns("Grd");
      assertThat(performed()).isGreaterThan(before);

      // Once it is closed, the dialog is not in the page anymore
      doAnswer(invocation -> {
        when(driver.findElements(argThat(dialog::equals))).thenReturn(Collections.emptyList());
        return null;
      }).when((Interactive) driver).perform(any());
      assertThatCode(() -> utilities.closeGridColumns("Grd")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.openGridColumns("Other"));
    }
  }

  @Test
  void shouldClickCloseAgainWhenTheColumnsDialogStaysOpen() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsDialog("Grd"), "");
      show(instructions.getGridColumnsClose("Grd"), "Close");
      By dialog = Locator.from(instructions.getGridColumnsDialog("Grd")).toBy();
      long before = performed();

      // The first click misses the button (the outcome of a reset moved it), the second one closes the dialog
      int[] clicks = {0};
      doAnswer(invocation -> {
        if (++clicks[0] == 2) {
          when(driver.findElements(argThat(dialog::equals))).thenReturn(Collections.emptyList());
        }
        return null;
      }).when((Interactive) driver).perform(any());

      assertThatCode(() -> utilities.closeGridColumns("Grd")).doesNotThrowAnyException();
      assertThat(performed() - before).isEqualTo(2);
    }
  }

  @Test
  void shouldNotClickCloseAgainWhenTheColumnsDialogIsClosingSlowly() {
    properties.setTimeout(Duration.ofSeconds(1));
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsDialog("Grd"), "");
      show(instructions.getGridColumnsClose("Grd"), "Close");
      By dialog = Locator.from(instructions.getGridColumnsDialog("Grd")).toBy();
      By close = Locator.from(instructions.getGridColumnsClose("Grd")).toBy();
      long before = performed();

      // The click worked: the button is gone at once, the dialog only after longer than a retry wait (the timeout)
      long[] clickedAt = {0};
      doAnswer(invocation -> {
        clickedAt[0] = System.currentTimeMillis();
        when(driver.findElements(argThat(close::equals))).thenReturn(Collections.emptyList());
        return null;
      }).when((Interactive) driver).perform(any());
      WebElement shown = driver.findElements(dialog).get(0);
      when(driver.findElements(argThat(dialog::equals))).thenAnswer(invocation ->
        clickedAt[0] > 0 && System.currentTimeMillis() - clickedAt[0] > 1500 ? Collections.emptyList() : List.of(shown));

      assertThatCode(() -> utilities.closeGridColumns("Grd")).doesNotThrowAnyException();
      assertThat(performed() - before).isEqualTo(1);
    }
  }

  @Test
  void shouldCheckWhetherTheColumnsOfAGridAreADialogOrASidePanelInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(By.cssSelector(TestIds.css(TestIds.GRID_COLUMNS_DIALOG) + TestAttributes.css(TestAttributes.OWNER, "Grd")
        + TestAttributes.css(TestAttributes.TYPE, "side-panel")), "");

      assertThatCode(() -> utilities.checkGridColumnsPanel("Grd", "side-panel")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsPanel("Grd", "dialog"));
    }
  }

  @Test
  void shouldCheckThatTheColumnsOfAGridAreAPopoverInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(By.cssSelector(TestIds.css(TestIds.GRID_COLUMNS_DIALOG) + TestAttributes.css(TestAttributes.OWNER, "Grd")
        + TestAttributes.css(TestAttributes.TYPE, "popover")), "");

      assertThatCode(() -> utilities.checkGridColumnsPanel("Grd", "popover")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsPanel("Grd", "dialog"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsPanel("Grd", "side-panel"));
    }
  }

  @Test
  void shouldCheckTheCounterAndTheColumnsOfTheColumnsDialogInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsCounter("Grd", "24", "26"), "24 of 26 visible");
      show(instructions.getGridColumnsItem("Grd", "col01"), "Col 01");

      assertThatCode(() -> {
        utilities.checkGridColumnsCounter("Grd", 24, 26);
        utilities.checkGridColumnsListed("Grd", "col01");
        utilities.checkGridColumnsNotListed("Grd", "key");
      }).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsCounter("Grd", 26, 26));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsListed("Grd", "key"));
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsNotListed("Grd", "col01"));
    }
  }

  @Test
  void shouldShowAndHideAColumnWithItsCheckboxInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsCheckbox("Grd", "col01"), "");

      // Checked: showing it again does nothing, hiding it clicks the checkbox
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(true);
      long start = performed();
      utilities.showGridColumn("Grd", "col01");
      assertThat(performed()).isEqualTo(start);
      utilities.hideGridColumn("Grd", "col01");
      assertThat(performed()).isGreaterThan(start);

      // Not checked: hiding it again does nothing, showing it clicks the checkbox
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(false);
      long checked = performed();
      utilities.hideGridColumn("Grd", "col01");
      assertThat(performed()).isEqualTo(checked);
      utilities.showGridColumn("Grd", "col01");
      assertThat(performed()).isGreaterThan(checked);
    }
  }

  @Test
  void shouldMoveAColumnWithItsArrowsAndCheckItsLimitsInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsItem("Grd", "col05"), "");
      show(instructions.getGridColumnsMoveUp("Grd", "col05"), "");
      show(instructions.getGridColumnsMoveDown("Grd", "col05"), "");

      // The arrows only show on the row under the pointer: each move hovers the row and then clicks the arrow
      long start = performed();
      utilities.moveGridColumnUp("Grd", "col05");
      utilities.moveGridColumnDown("Grd", "col05");
      assertThat(performed()).isGreaterThanOrEqualTo(start + 4);
      assertThrows(AssertionFailedError.class, () -> utilities.moveGridColumnUp("Grd", "other"));

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(true);
      assertThatCode(() -> {
        utilities.checkGridColumnCannotMoveUp("Grd", "col05");
        utilities.checkGridColumnCannotMoveDown("Grd", "col05");
      }).doesNotThrowAnyException();
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(false);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnCannotMoveUp("Grd", "col05"));
    }
  }

  @Test
  void shouldClickTheActionsOfTheColumnsDialogInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsReset("Grd"), "");
      show(instructions.getGridColumnsCancel("Grd"), "");
      show(instructions.getGridColumnsSave("Grd"), "");

      long start = performed();
      utilities.clickGridColumnsReset("Grd");
      utilities.clickGridColumnsCancel("Grd");
      utilities.clickGridColumnsSave("Grd");

      assertThat(performed()).isGreaterThanOrEqualTo(start + 3);
      assertThrows(AssertionFailedError.class, () -> utilities.clickGridColumnsSave("Other"));
      assertThrows(AssertionFailedError.class, () -> utilities.clickGridColumnsCancel("Other"));
    }
  }

  @Test
  void shouldWaitForTheResetOfTheColumnsToEndInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      properties.setTimeout(Duration.ofSeconds(2));
      use(client);
      show(instructions.getGridColumnsReset("Grd"), "");
      show(instructions.getGridColumnsBusy("Grd"), "");
      By busy = Locator.from(instructions.getGridColumnsBusy("Grd")).toBy();
      WebElement root = driver.findElements(busy).get(0);

      // Restore default is silent: the Columns are aria-busy until the server answers, 300 ms after the click
      long[] clickedAt = {0};
      doAnswer(invocation -> {
        clickedAt[0] = System.currentTimeMillis();
        return null;
      }).when((Interactive) driver).perform(any());
      when(driver.findElements(argThat(busy::equals))).thenAnswer(invocation ->
        clickedAt[0] > 0 && System.currentTimeMillis() - clickedAt[0] < 300 ? List.of(root) : Collections.emptyList());

      utilities.clickGridColumnsReset("Grd");
      assertThat(System.currentTimeMillis() - clickedAt[0]).isGreaterThanOrEqualTo(300);

      // The server never answers
      properties.setTimeout(Duration.ofMillis(300));
      when(driver.findElements(argThat(busy::equals))).thenReturn(List.of(root));
      assertThrows(AssertionFailedError.class, () -> utilities.clickGridColumnsReset("Grd"));
    }
  }

  @Test
  void shouldWaitForTheSaveToCloseTheColumnsInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      properties.setTimeout(Duration.ofSeconds(2));
      use(client);
      show(instructions.getGridColumnsSave("Grd"), "");
      show(instructions.getGridColumnsDialog("Grd"), "");
      By dialog = Locator.from(instructions.getGridColumnsDialog("Grd")).toBy();
      WebElement root = driver.findElements(dialog).get(0);

      // Save closes the Columns when the server answers that the layout was saved, 300 ms after the click
      long[] clickedAt = {0};
      doAnswer(invocation -> {
        clickedAt[0] = System.currentTimeMillis();
        return null;
      }).when((Interactive) driver).perform(any());
      when(driver.findElements(argThat(dialog::equals))).thenAnswer(invocation ->
        clickedAt[0] == 0 || System.currentTimeMillis() - clickedAt[0] < 300 ? List.of(root) : Collections.emptyList());

      utilities.clickGridColumnsSave("Grd");
      assertThat(System.currentTimeMillis() - clickedAt[0]).isGreaterThanOrEqualTo(300);

      // The layout is not saved: the Columns stay open
      properties.setTimeout(Duration.ofMillis(300));
      when(driver.findElements(argThat(dialog::equals))).thenReturn(List.of(root));
      assertThrows(AssertionFailedError.class, () -> utilities.clickGridColumnsSave("Grd"));
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldKeepClickingTheDeprecatedShowAllOfTheColumnsInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridColumnsShowAll("Grd"), "");

      long start = performed();
      utilities.clickGridColumnsShowAll("Grd");

      assertThat(performed()).isGreaterThan(start);
    }
  }

  @Test
  void shouldCheckTheOrderOfTheColumnsOfAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridHeader("Grd", "a"), "A");
      show(instructions.getGridHeader("Grd", "b"), "B");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(10.0, 120.0);

      assertThatCode(() -> utilities.checkGridColumnsLeftToRight("Grd", "a", "b")).doesNotThrowAnyException();

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(120.0, 10.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnsLeftToRight("Grd", "a", "b"));
    }
  }

  @Test
  void shouldCheckThatAColumnIsNotShownInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridHeader("Grd", "a"), "A");

      assertThatCode(() -> utilities.checkGridColumnNotShown("Grd", "hidden")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridColumnNotShown("Grd", "a"));
    }
  }

  @Test
  void shouldCheckThatAGroupHeaderSpansOverItsVisibleColumnsInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridGroupHeader("Grd", "Group A"), "Group A");
      show(instructions.getGridHeader("Grd", "c3"), "C3");
      show(instructions.getGridHeader("Grd", "c6"), "C6");
      // Group: 100 - 400. First column: 100. Last column: 400
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(100.0, 400.0, 100.5, 399.0);
      assertThatCode(() -> utilities.checkGridGroupHeaderOver("Grd", "Group A", "c3", "c6")).doesNotThrowAnyException();

      // The header still spans over a column that was hidden
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(100.0, 520.0, 100.0, 400.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupHeaderOver("Grd", "Group A", "c3", "c6"));

      assertThatCode(() -> utilities.checkGridGroupHeaderNotShown("Grd", "Group B")).doesNotThrowAnyException();
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridGroupHeaderNotShown("Grd", "Group A"));
    }
  }

  @Test
  void shouldMaximizeAndRestoreAGridInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getGridMaximizeButton("Grd"), MAXIMIZE);
      show(instructions.getGridToolbarMaximized("Grd", true), "");
      show(instructions.getGridToolbarMaximized("Grd", false), "");
      show(instructions.getGrid("Grd"), "");

      long start = performed();
      utilities.maximizeGrid("Grd");
      utilities.restoreGrid("Grd");
      assertThat(performed()).isGreaterThanOrEqualTo(start + 2);

      // It fills the content area (maximize target) and the header stays above it
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.97, 40.0);
      assertThatCode(() -> {
        utilities.checkGridMaximized("Grd");
        utilities.checkGridNotMaximized("Grd");
      }).doesNotThrowAnyException();
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.5, 40.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridMaximized("Grd"));
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.97, 0.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkGridMaximized("Grd"));
    }
  }

  @Test
  void shouldMaximizeAndRestoreAWindowInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getWindowMaximizeButton("Win"), MAXIMIZE);
      show(instructions.getWindow("Win", true), "");
      show(instructions.getWindow("Win", false), "");
      show(instructions.getWindow("Win"), "");

      long start = performed();
      utilities.maximizeWindow("Win");
      utilities.restoreWindow("Win");
      assertThat(performed()).isGreaterThanOrEqualTo(start + 2);

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.97, 40.0);
      assertThatCode(() -> {
        utilities.checkWindowMaximized("Win");
        utilities.checkWindowNotMaximized("Win");
      }).doesNotThrowAnyException();
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.5, 40.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkWindowMaximized("Win"));
    }
  }

  @Test
  void shouldMaximizeAndRestoreATabComponentInBothClients() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getTabMaximizeButton("Tabs"), MAXIMIZE);
      show(instructions.getTabComponent("Tabs", true), "");
      show(instructions.getTabComponent("Tabs", false), "");
      show(instructions.getTabComponent("Tabs"), "");

      long start = performed();
      utilities.maximizeTab("Tabs");
      utilities.restoreTab("Tabs");
      assertThat(performed()).isGreaterThanOrEqualTo(start + 2);

      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.97, 40.0, "static");
      assertThatCode(() -> {
        utilities.checkTabMaximized("Tabs");
        utilities.checkTabNotMaximized("Tabs");
      }).doesNotThrowAnyException();
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn(0.5, 40.0);
      assertThrows(AssertionFailedError.class, () -> utilities.checkTabMaximized("Tabs"));
    }
  }

  @Test
  void shouldFailWhenAMaximizedPanelStaysFixedAfterRestoring() {
    for (IAweFrontEndInstructions client : List.of(new AngularAweInstructions(), new ReactAweInstructions())) {
      use(client);
      show(instructions.getWindow("Win", false), "");
      when(((JavascriptExecutor) driver).executeScript(any(String.class), any())).thenReturn("fixed");

      assertThrows(AssertionFailedError.class, () -> utilities.checkWindowNotMaximized("Win"));
    }
  }

  private static String scriptWith(String text) {
    return argThat((String script) -> script != null && script.contains(text));
  }

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

    @Override
    protected void clickUserMenuOption(String optionId) {
      events.add("option:" + optionId);
    }

    @Override
    protected void openUserMenu() {
      events.add("user-menu");
    }

    @Override
    protected void selectContain(String criterionName, String label) {
      events.add("select:" + criterionName + ":" + label);
    }
  }
}
