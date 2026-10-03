package com.almis.awe.testing.selenium;

import com.almis.awe.testing.enumerated.MenuBehavior;
import com.almis.awe.testing.enumerated.RowEditBehavior;
import com.almis.awe.testing.enumerated.SuggestBehavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The selectors of {@link ReactAweInstructions}: each component is located with the AWE attribute that identifies the
 * instance and the test hook that names the part, never with a PrimeReact class. React differs from AngularJS in the
 * state of the checkboxes (a data attribute of the root element), the user shell (an avatar) and the grid trees (the
 * rows have no hook, the icon carries the row identifier).
 */
class ReactAweInstructionsTest {

  private ReactAweInstructions instructions;

  @BeforeEach
  void setUp() {
    instructions = new ReactAweInstructions();
  }

  @Test
  void shouldLocateCriteriaByTheirInputHook() {
    String criterion = instructions.getCriterionCss("Txt");

    assertThat(criterion).isEqualTo("[criterion-id='Txt']");
    assertThat(instructions.getCriterionInput(criterion))
      .hasToString("By.cssSelector: [criterion-id='Txt'] [data-testid='criterion-input']");
    assertThat(instructions.getDateCriterion(criterion))
      .hasToString("By.cssSelector: [criterion-id='Txt'] [data-testid='criterion-input']");
  }

  @Test
  void shouldReadTheStateOfCheckboxesSwitchesAndRadiosFromTheirData() {
    // The control is the root element of PrimeReact, which exposes its state as data-selected (not :checked)
    assertThat(instructions.getCheckbox("[criterion-id='Chk']"))
      .hasToString("By.cssSelector: [criterion-id='Chk'] [data-testid='criterion-input']");
    assertThat(instructions.getCheckboxChecked("Chk", true))
      .hasToString("By.cssSelector: [criterion-id='Chk'] [data-testid='criterion-input'][data-selected='true']");
    assertThat(instructions.getCheckboxChecked("Chk", false))
      .hasToString("By.cssSelector: [criterion-id='Chk'] [data-testid='criterion-input'][data-selected='false']");
  }

  @Test
  void shouldLocateGridPartsByTheirHooksAndAweIdentifiers() {
    assertThat(instructions.getParentCss("Grd", null, null))
      .isEqualTo("[data-testid='grid'][grid-id='Grd'] [data-testid='grid-header-checkbox']");
    assertThat(instructions.getParentCss("Grd", null, "Col")).isEqualTo("[data-testid='grid'][grid-id='Grd'] "
      + "[data-testid='grid-row'][data-selected='true'] [data-testid='grid-cell'][column-id='Col'] ");
    assertThat(instructions.getParentCss("Grd", "R1", "Col")).isEqualTo("[data-testid='grid'][grid-id='Grd'] "
      + "[data-testid='grid-row'][row-id='R1'] [data-testid='grid-cell'][column-id='Col'] ");
    assertThat(instructions.getEditingParentCss("Grd", "Col")).isEqualTo("[data-testid='grid'][grid-id='Grd'] "
      + "[data-testid='grid-row'][data-editing='true'] [data-testid='grid-cell'][column-id='Col'] ");

    assertThat(instructions.getGridCell("Grd", "R1", "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']");
    assertThat(instructions.getGridCell("Grd", null, "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @data-selected='true']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']");
    assertThat(instructions.getGridHeader("Grd", "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-header-cell' and @column-id='Col']");
    assertThat(instructions.getGridScrollZone("Grd")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-viewport']");
    assertThat(instructions.getGridCellText("Grd", "R1", "Col", "abc")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']//text()[contains(.,'abc')]/..");
    assertThat(instructions.findGridCell("Grd", "abc")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row']//*[@data-testid='grid-cell']"
      + "//text()[contains(.,'abc')]/..");
    assertThat(instructions.findGridCell(null, "abc")).hasToString("By.xpath: "
      + "//*[@data-testid='grid-row']//*[@data-testid='grid-cell']//text()[contains(.,'abc')]/..");
    assertThat(instructions.getRowEditBehavior()).isEqualTo(RowEditBehavior.DOUBLE_CLICK);
  }

  @Test
  void shouldLocateTheSuccessIconOfAColumnInsideItsCell() {
    assertThat(instructions.getColumnSuccessIcon("Sta"))
      .hasToString("By.cssSelector: [column-id='Sta'] [data-testid='column-icon'] .text-success");
  }

  @Test
  void shouldSelectARowThroughItsCheckboxWhenTheGridHasOneAndThroughItsCellOtherwise() {
    String row = "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row']"
      + "[.//*[@data-testid='grid-cell' and contains(normalize-space(.),'abc')]]";
    assertThat(instructions.findGridRowSelection("Grd", "abc")).hasToString("By.xpath: ("
      + row + "//*[@data-testid='grid-row-checkbox'] | "
      + row + "//*[@data-testid='grid-cell' and contains(normalize-space(.),'abc')])[1]");
    assertThat(instructions.findGridRowSelection(null, "It's").toString())
      .contains("contains(normalize-space(.),\"It's\")").startsWith("By.xpath: (//*[@data-testid='grid-row']");
  }

  @Test
  void shouldFindARowThatIsAlreadySelectedByItsState() {
    assertThat(instructions.findGridSelectedRow("Grd", "abc")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @data-selected='true']"
      + "[.//*[@data-testid='grid-cell' and contains(normalize-space(.),'abc')]]");
    assertThat(instructions.findGridSelectedRow(null, "It's").toString())
      .startsWith("By.xpath: //*[@data-testid='grid-row' and @data-selected='true']")
      .contains("contains(normalize-space(.),\"It's\")");
  }

  @Test
  void shouldLocateGridSaveButtonsAndLoaders() {
    assertThat(instructions.getGridSaveButton()).hasToString("By.cssSelector: [data-testid='grid-row-save']:not([disabled])");
    assertThat(instructions.getGridSaveButton("Grd")).hasToString("By.cssSelector: "
      + "[data-testid='grid'][grid-id='Grd'] [data-testid='grid-row-save']:not([disabled])");
    assertThat(instructions.getGridLoaderSelector()).hasToString("By.cssSelector: [data-testid='grid-loader']");
    assertThat(instructions.getLoadingBar()).hasToString("By.cssSelector: [data-testid='loading-spinner']");
    assertThat(instructions.getLoaderSelector()).hasToString(
      "By.cssSelector: [data-testid='loading-spinner'],[data-testid='grid-loader'],[data-testid='loader']");
  }

  @Test
  void shouldLocateTreeIconsByTheRowIdentifierBecauseTreeRowsHaveNoHook() {
    assertThat(instructions.getTreeButton("Tre", "R1"))
      .hasToString("By.cssSelector: [tree-grid-id='Tre'] [data-testid='tree-icon'][row-id='R1']");
    assertThat(instructions.getTreeButtonLoader()).hasToString("By.cssSelector: [data-testid='grid-loader']");
  }

  @Test
  void shouldLocateSelectPartsByTheirHooks() {
    String criterion = instructions.getCriterionCss("Sel");

    // The arrow opens the panel: the middle of a short multiple select can be its clear icon
    assertThat(instructions.getSelectChoice(criterion)).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-trigger']");
    assertThat(instructions.getSelectChosen("Sel")).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-value']");
    assertThat(instructions.getSelectMultipleTextContainer("Sel"))
      .hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-choice']");
    assertThat(instructions.getSelectLoader(criterion)).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='loader']");
    assertThat(instructions.getSelectDropdownList()).hasToString("By.cssSelector: [data-testid='select-dropdown']");
    assertThat(instructions.getSelectDropdownListElements())
      .hasToString("By.cssSelector: [data-testid='select-dropdown'] [data-testid='select-option']");
    assertThat(instructions.getSelectDropdownListFirstElement())
      .hasToString("By.cssSelector: [data-testid='select-dropdown'] [data-testid='select-option']");
    assertThat(instructions.getSelectDropdownListLastElement()).hasToString("By.xpath: "
      + "(//*[@data-testid='select-dropdown']//*[@data-testid='select-option'])[last()]");
    assertThat(instructions.getSelectResult("Yes")).hasToString("By.xpath: "
      + "//*[@data-testid='select-dropdown']//*[@data-testid='select-option'][contains(normalize-space(.),'Yes')]");
  }

  @Test
  void shouldLocateSuggestPartsByTheirHooks() {
    String criterion = instructions.getCriterionCss("Sug");

    assertThat(instructions.getSuggestBehavior()).isEqualTo(SuggestBehavior.INPUT);
    assertThat(instructions.getSuggestChoice(criterion)).hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='select-search']");
    assertThat(instructions.getSuggest(criterion)).hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='select-search']");
    assertThat(instructions.getSuggestInput(criterion)).hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='select-search']");
    assertThat(instructions.getSuggestChosen("Sug")).hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='select-search']");
    assertThat(instructions.getSuggestLoader(criterion)).hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='loader']");
    assertThat(instructions.getSuggestDropdownList()).hasToString("By.cssSelector: [data-testid='select-search']");
    assertThat(instructions.getSuggestDropdownListLastElement()).hasToString("By.xpath: "
      + "(//*[@data-testid='select-dropdown']//*[@data-testid='select-option'])[last()]");
    assertThat(instructions.getSuggestResult("Yes")).hasToString("By.xpath: "
      + "//*[@data-testid='select-dropdown']//*[@data-testid='select-option'][contains(normalize-space(.),'Yes')]");
    assertThat(instructions.getSuggestMultipleInput(criterion)).hasToString("By.cssSelector: "
      + "[criterion-id='Sug'] [data-testid='select-search'], [data-testid='select-dropdown'] [data-testid='select-search']");
    assertThat(instructions.getSuggestMultipleChoiceClose(criterion))
      .hasToString("By.cssSelector: [criterion-id='Sug'] [data-testid='select-choice-close']");
  }

  @Test
  void shouldLocateDatepickerCellsByTheirHooksAndState() {
    assertThat(instructions.datePickerRequiresManualClick()).isFalse();
    assertThat(instructions.multipleChoiceUsesPanel()).isTrue();
    assertThat(instructions.getDatepicker()).hasToString("By.cssSelector: [data-testid='datepicker']");
    assertThat(instructions.getActiveDatepicker())
      .hasToString("By.cssSelector: [data-testid='datepicker'] [data-testid='datepicker-day'][data-selected='true']");
    assertThat(instructions.getCellFromDatepicker("day", "23")).hasToString("By.xpath: "
      + "//*[@data-testid='datepicker']//*[@data-testid='datepicker-day' and not(@data-outside-month='true') "
      + "and not(@data-disabled='true')]//text()[.='23']/..");
    assertThat(instructions.getCellFromDatepicker("month", "Oct")).hasToString("By.xpath: "
      + "//*[@data-testid='datepicker']//*[@data-testid='datepicker-month']//text()[.='Oct']/..");
    assertThat(instructions.getCellFromDatepicker("year", "1978")).hasToString("By.xpath: "
      + "//*[@data-testid='datepicker']//*[@data-testid='datepicker-year']//text()[.='1978']/..");
    // Unknown types keep selecting days, as before
    assertThat(instructions.getCellFromDatepicker("decade", "23").toString()).contains("datepicker-day");
  }

  @Test
  void shouldLocateTabsByTheirHooksAndState() {
    assertThat(instructions.getTab("Tab"))
      .hasToString("By.cssSelector: [criterion-id='Tab'] [data-testid='tab-list'][data-disabled='false']");
    assertThat(instructions.getTab("Tab", "Lbl")).hasToString("By.xpath: "
      + "//*[@criterion-id='Tab']//*[@data-testid='tab-label' and normalize-space(.)='Lbl']");
    assertThat(instructions.getTabActive("Tab", "Lbl")).hasToString("By.xpath: "
      + "//*[@criterion-id='Tab']//*[@data-testid='tab' and @data-active='true']"
      + "//*[@data-testid='tab-label' and normalize-space(.)='Lbl']");
    // The React tab list has no "more" menu
    assertThat(instructions.getTabMenu("Tab")).isNull();
    assertThat(instructions.getTabMenuDropdown("Tab")).isNull();
    assertThat(instructions.getTabMenuDropdownOption("Tab", "Lbl")).isNull();
  }

  @Test
  void shouldLocateMenusMessagesAndButtonsByTheirHooksAndIds() {
    assertThat(instructions.getMenuBehavior()).isEqualTo(MenuBehavior.CLICK_ALL);
    assertThat(instructions.getMenuOption("Opt")).hasToString("By.cssSelector: [data-testid='menu-link'][name='Opt']");
    assertThat(instructions.getMenuOpenedChildren("Opt")).hasToString("By.xpath: "
      + "//*[@data-testid='menu-option' and @option-name='Opt' and @data-open='true']");
    assertThat(instructions.getMenuDropdown()).hasToString("By.cssSelector: [data-testid='menu-submenu']");
    assertThat(instructions.getMenuActiveOption("Opt")).hasToString("By.cssSelector: "
      + "[data-testid='menu-option'][option-name='Opt'][data-active='true']");
    assertThat(instructions.getContextButton("Ctx")).hasToString("By.cssSelector: [data-testid='context-menu'] "
      + "[data-testid='context-menu-link'][option-id='Ctx']:not([data-disabled='true'])");
    assertThat(instructions.getInfoButton("Inf")).hasToString("By.cssSelector: "
      + "[data-testid='info-button']#Inf,[data-testid='info-dropdown']#Inf,[data-testid='avatar']#Inf");
    assertThat(instructions.getButton("ButOk")).hasToString("By.cssSelector: #ButOk:not([disabled])");
  }

  @Test
  void shouldLocateMessagesOfEveryTypeThroughTheirAlertHook() {
    for (String type : List.of("success", "info", "warning", "danger")) {
      assertThat(instructions.getMessage(type)).hasToString(
        "By.cssSelector: [data-testid='alert'][data-type='" + type + "'] [data-testid='alert-close']");
    }
  }

  @Test
  void shouldRequireTheControlOfTheLoggedUserInTheShell() {
    // The sidebar shell shows an avatar, the topbar shell an info dropdown
    assertThat(instructions.getRequiredPostLoginShellControls())
      .containsExactly(By.cssSelector("[data-testid='avatar']#ButUsrAct,[data-testid='info-dropdown']#ButUsrAct"));
    assertThat(instructions.getOptionalPostLoginShellControls()).containsExactly(By.id("ButLogOut"));
  }

  @Test
  void shouldKeepWhatCannotBeHookedInReact() {
    // React has no popover: the selector never matches so the mouse is never moved
    assertThat(instructions.getPopover()).hasToString("By.cssSelector: .popover:not(.ng-hide)");
    assertThat(instructions.containsText("some-class", "abc"))
      .hasToString("By.xpath: //*[contains(@class,'some-class')]//text()[contains(.,'abc')]/..");
  }

  @Test
  void shouldEscapeQuotesOfTheTextThatTestsSearch() throws Exception {
    assertThat(instructions.getGridCellText("Grd", "R1", "Col", "It's")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']//text()[contains(.,\"It's\")]/..");
    assertThat(instructions.getTab("Tab", "Men's")).hasToString("By.xpath: "
      + "//*[@criterion-id='Tab']//*[@data-testid='tab-label' and normalize-space(.)=\"Men's\"]");
    assertThat(instructions.containsText("clazz", "It's")).hasToString("By.xpath: "
      + "//*[contains(@class,'clazz')]//text()[contains(.,\"It's\")]/..");

    List<By> locators = List.of(
      instructions.getGridCellText("Gr'd", "R'1", "Co'l", "a'b\"c"), instructions.findGridCell("Gr'd", "It's"),
      instructions.getGridHeader("Gr'd", "Co'l"), instructions.getGridCell("Gr'd", null, "Co'l"),
      instructions.getCellFromDatepicker("day", "It's"), instructions.getTab("Ta'b", "Men's"),
      instructions.getTabActive("Ta'b", "Men's"), instructions.getMenuOpenedChildren("Op't"),
      instructions.getSelectResult("It's"), instructions.getSuggestResult("a'b\"c"),
      instructions.containsText("cla'ss", "It's"));
    for (By locator : locators) {
      String xpath = locator.toString().substring("By.xpath: ".length());
      assertThat(XPathFactory.newInstance().newXPath().compile(xpath)).as(xpath).isNotNull();
    }
  }

  @Test
  void shouldProduceWellFormedXpathExpressions() throws XPathExpressionException {
    List<By> locators = List.of(
      instructions.getGridCell("Grd", "R1", "Col"), instructions.getGridCell("Grd", null, "Col"),
      instructions.getGridHeader("Grd", "Col"), instructions.getGridScrollZone("Grd"),
      instructions.getGridCellText("Grd", "R1", "Col", "abc"), instructions.findGridCell(null, "abc"),
      instructions.getCellFromDatepicker("day", "23"), instructions.getCellFromDatepicker("month", "Oct"),
      instructions.getCellFromDatepicker("year", "1978"), instructions.getTab("Tab", "Lbl"),
      instructions.getTabActive("Tab", "Lbl"), instructions.getMenuOpenedChildren("Opt"),
      instructions.getSelectDropdownListLastElement(), instructions.getSelectResult("Yes"),
      instructions.getSuggestResult("Yes"), instructions.getSuggestDropdownListLastElement(),
      instructions.findGridRowSelection("Grd", "abc"), instructions.findGridRowSelection(null, "It's"),
      instructions.findGridSelectedRow("Grd", "abc"), instructions.findGridSelectedRow(null, "It's"));

    for (By locator : locators) {
      String xpath = locator.toString().substring("By.xpath: ".length());
      assertThat(XPathFactory.newInstance().newXPath().compile(xpath)).as(xpath).isNotNull();
    }
  }
  @Test
  void shouldLocateTheSessionStepsThroughTheAvatarAndTheLoginButton() {
    assertThat(instructions.getLoggedUser()).hasToString("By.cssSelector: [data-testid='avatar-name']");
    // The logout button is inside the user menu, which opens on click
    assertThat(instructions.getUserMenuButtonId()).isEqualTo("ButUsrAct");
    assertThat(instructions.getLoginScreenMarker()).hasToString("By.cssSelector: #ButLogIn");
    assertThat(instructions.getLoginScreenText()).isEqualTo("Login");
  }

  @Test
  void shouldLocateTheUnitTheWizardStepAndTheDatepickerDayThroughTheirHooks() {
    assertThat(instructions.getCriterionUnit("Unt"))
      .hasToString("By.cssSelector: [criterion-id='Unt'] [data-testid='criterion-unit']");
    assertThat(instructions.getActiveWizardStepNumber()).hasToString(
      "By.cssSelector: [data-testid='wizard-step'][data-active='true'] [data-testid='wizard-step-number']");
    assertThat(instructions.getEnabledDatepickerDay()).hasToString("By.cssSelector: "
      + "[data-testid='datepicker-day']:not([data-disabled='true']):not([data-outside-month='true'])");
  }

  @Test
  void shouldLocateTheTreeRowsThroughTheGridRowHook() {
    assertThat(instructions.getTreeRow("Tre", "R1")).hasToString(
      "By.cssSelector: [tree-grid-id='Tre'] [data-testid='grid-row'][row-id='R1']");
    assertThat(instructions.getTreeRowIcon("Tre", "R1")).hasToString(
      "By.cssSelector: [tree-grid-id='Tre'] [data-testid='tree-icon'][row-id='R1']");
  }

  @Test
  void shouldLocateTheIconOfAColumnByTheNameOfTheIcon() {
    assertThat(instructions.getGridIcon("Grd", "Ico", "plus")).hasToString("By.cssSelector: "
      + "[grid-id='Grd'] [column-id='Ico'] [data-testid='column-icon'][data-icon~='plus']");
  }

  @Test
  void shouldCloseTheContextMenuWithoutAMask() {
    assertThat(instructions.getContextMenuMask()).isNull();
  }

  @Test
  void shouldShareTheHooksOfTheMessagesTheDialogsAndTheLogViewerWithAngularJs() {
    assertThat(instructions.getMessageTitle("warning")).hasToString(
      "By.cssSelector: [data-testid='alert'][data-type='warning'] [data-testid='alert-title']");
    assertThat(instructions.getOpenDialog("PrnOpt")).hasToString(
      "By.cssSelector: [data-testid='dialog'][data-testid-owner='PrnOpt'][data-open='true']");
    assertThat(instructions.getLogViewer()).hasToString("By.cssSelector: [data-testid='log-viewer']");
    assertThat(instructions.getSelectOption(1)).hasToString(
      "By.xpath: (//*[@data-testid='select-dropdown']//*[@data-testid='select-option'])[1]");
  }
}
