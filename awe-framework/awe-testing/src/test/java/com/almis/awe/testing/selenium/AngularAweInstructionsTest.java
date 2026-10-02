package com.almis.awe.testing.selenium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;

import javax.xml.xpath.XPathFactory;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The selectors of {@link AngularAweInstructions}: each component is located with the AWE attribute that identifies the
 * instance and the test hook that names the part, never with a library class.
 */
class AngularAweInstructionsTest {

  private AngularAweInstructions instructions;

  @BeforeEach
  void setUp() {
    instructions = new AngularAweInstructions();
  }

  @Test
  void shouldLocateCriteriaByTheirInputHook() {
    String criterion = instructions.getCriterionCss("Txt");

    assertThat(criterion).isEqualTo("[criterion-id='Txt']");
    assertThat(instructions.getCriterionInput(criterion))
      .hasToString("By.cssSelector: [criterion-id='Txt'] [data-testid='criterion-input']");
    assertThat(instructions.getDateCriterion(criterion))
      .hasToString("By.cssSelector: [criterion-id='Txt'] [data-testid='criterion-input']");
    assertThat(instructions.getCheckboxChecked("Chk", true))
      .hasToString("By.cssSelector: [criterion-id='Chk'] [data-testid='criterion-input']:checked");
    assertThat(instructions.getCheckboxChecked("Chk", false))
      .hasToString("By.cssSelector: [criterion-id='Chk'] [data-testid='criterion-input']:not(:checked)");
  }

  @Test
  void shouldLocateGridPartsByTheirHooksAndAweIdentifiers() {
    assertThat(instructions.getParentCss("Grd", null, null))
      .isEqualTo("[data-testid='grid'] [id='scope-Grd'] [data-testid='grid-header-checkbox']");
    assertThat(instructions.getParentCss("Grd", null, "Col")).isEqualTo(
      "[data-testid='grid'] [id='scope-Grd'] [data-testid='grid-row'][data-selected='true'] [data-testid='grid-cell'][column-id='Col'] ");
    assertThat(instructions.getParentCss("Grd", "R1", "Col")).isEqualTo(
      "[data-testid='grid'] [id='scope-Grd'] [data-testid='grid-row'][row-id='R1'] [data-testid='grid-cell'][column-id='Col'] ");

    assertThat(instructions.getGridCell("Grd", "R1", "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']");
    assertThat(instructions.getGridCell("Grd", null, "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @data-selected='true']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']");
    assertThat(instructions.getGridHeader("Grd", "Col")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-header-cell' and @column-id='Col']");
    assertThat(instructions.getGridScrollZone("Grd")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-viewport' and @data-container='body']");
    assertThat(instructions.getGridCellText("Grd", "R1", "Col", "abc")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']//text()[contains(.,'abc')]/..");
    assertThat(instructions.findGridCell(null, "abc")).hasToString("By.xpath: "
      + "//*[@data-testid='grid-row']//*[@data-testid='grid-cell']//text()[contains(.,'abc')]/..");
    // A row is selected by clicking on its cell
    assertThat(instructions.findGridRowSelection("Grd", "abc")).isEqualTo(instructions.findGridCell("Grd", "abc"));
    assertThat(instructions.getGridSaveButton()).hasToString("By.cssSelector: [data-testid='grid-row-save']:not([disabled])");
    assertThat(instructions.getGridSaveButton("Grd")).hasToString("By.cssSelector: #Grd-grid-row-save:not([disabled])");
    assertThat(instructions.getGridLoaderSelector()).hasToString("By.cssSelector: [data-testid='grid-loader']");
    assertThat(instructions.getTreeButton("Tre", "R1")).hasToString(
      "By.cssSelector: [tree-grid-id='Tre'] [data-testid='grid-row'][row-id='R1'] [data-testid='tree-icon']");
    assertThat(instructions.getTreeButtonLoader())
      .hasToString("By.cssSelector: [data-testid='tree-icon'][data-loading='true']");
  }

  @Test
  void shouldLocateSelectPartsByTheirHooks() {
    String criterion = instructions.getCriterionCss("Sel");

    assertThat(instructions.getSelectChoice(criterion)).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select']");
    assertThat(instructions.getSelectChosen("Sel")).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-value']");
    assertThat(instructions.getSelectMultipleTextContainer("Sel"))
      .hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-choice']");
    assertThat(instructions.getSelectLoader(criterion)).hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='loader']");
    assertThat(instructions.getSelectDropdownList()).hasToString("By.cssSelector: [data-testid='select-dropdown']");
    assertThat(instructions.getSelectDropdownListElements())
      .hasToString("By.cssSelector: [data-testid='select-dropdown'] [data-testid='select-option']");
    assertThat(instructions.getSelectDropdownListLastElement()).hasToString("By.xpath: "
      + "(//*[@data-testid='select-dropdown']//*[@data-testid='select-option'])[last()]");
    assertThat(instructions.getSelectResult("Yes")).hasToString("By.xpath: "
      + "//*[@data-testid='select-dropdown']//*[@data-testid='select-option'][contains(normalize-space(.),'Yes')]");
    assertThat(instructions.getSuggestInput(criterion))
      .hasToString("By.cssSelector: [data-testid='select-dropdown'] [data-testid='select-search']");
    assertThat(instructions.getSuggestMultipleInput(criterion))
      .hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-search']");
    assertThat(instructions.getSuggestMultipleChoiceClose(criterion))
      .hasToString("By.cssSelector: [criterion-id='Sel'] [data-testid='select-choice-close']");
  }

  @Test
  void shouldLocateDatepickerCellsByTheirHooksAndState() {
    assertThat(instructions.getDatepicker()).hasToString("By.cssSelector: [data-testid='datepicker']");
    assertThat(instructions.getActiveDatepicker())
      .hasToString("By.cssSelector: [data-testid='datepicker'] [data-selected='true']");
    assertThat(instructions.getCellFromDatepicker("day", "23")).hasToString("By.xpath: "
      + "//*[@data-testid='datepicker']//*[@data-testid='datepicker-day' and not(@data-outside-month='true')]//text()[.='23']/..");
    assertThat(instructions.getCellFromDatepicker("month", "Oct").toString()).contains("@data-testid='datepicker-month'");
    assertThat(instructions.getCellFromDatepicker("year", "1978").toString()).contains("@data-testid='datepicker-year'");
    assertThatThrownBy(() -> instructions.getCellFromDatepicker("decade", "1970"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("decade");
  }

  @Test
  void shouldLocateTabsByTheirHooksAndState() {
    assertThat(instructions.getTab("Tab"))
      .hasToString("By.cssSelector: [criterion-id='Tab'] [data-testid='tab-list'][data-disabled='false']");
    assertThat(instructions.getTab("Tab", "Lbl"))
      .hasToString("By.cssSelector: [criterion-id='Tab'] [data-testid='tab-label'][translate-multiple='Lbl']");
    assertThat(instructions.getTabActive("Tab", "Lbl")).hasToString("By.cssSelector: "
      + "[criterion-id='Tab'] [data-testid='tab'][data-active='true'] [data-testid='tab-label'][translate-multiple='Lbl']");
    assertThat(instructions.getTabMenu("Tab")).hasToString("By.cssSelector: "
      + "[criterion-id='Tab'] [data-testid='tab-list'][data-disabled='false'] [data-testid='tabdrop-toggle']");
    assertThat(instructions.getTabMenuDropdown("Tab")).hasToString("By.cssSelector: "
      + "[criterion-id='Tab'] [data-testid='tab-list'][data-disabled='false'] [data-testid='tabdrop-menu']");
    assertThat(instructions.getTabMenuDropdownOption("Tab", "Lbl")).hasToString("By.cssSelector: "
      + "[criterion-id='Tab'] [data-testid='tab-list'][data-disabled='false'] [data-testid='tabdrop-menu'] "
      + "[data-testid='tab-label'][translate-multiple='Lbl']");
  }

  @Test
  void shouldLocateMenusMessagesAndLoadersByTheirHooks() {
    assertThat(instructions.getMenuOption("Opt")).hasToString("By.cssSelector: [data-testid='menu-link'][name='Opt']");
    assertThat(instructions.getMenuOpenedChildren("Opt")).hasToString("By.xpath: "
      + "//*[@data-testid='menu-link' and @name='Opt']/following-sibling::*"
      + "[(@data-testid='menu-dropdown' or @data-testid='menu-submenu') and @data-open='true']");
    assertThat(instructions.getMenuDropdown()).hasToString("By.cssSelector: .mm-dropdown-first");
    assertThat(instructions.getContextButton("Ctx")).hasToString("By.cssSelector: [data-testid='context-menu'] "
      + "[data-testid='context-menu-option'][option-id='Ctx'] [data-testid='context-menu-link']:not([data-disabled='true'])");
    assertThat(instructions.getInfoButton("Inf"))
      .hasToString("By.cssSelector: [info-dropdown-id='Inf'] [data-testid='info-dropdown-toggle']");
    assertThat(instructions.getMessage("danger"))
      .hasToString("By.cssSelector: [data-testid='alert'][data-type='danger'] [data-testid='alert-close']");
    assertThat(instructions.getPopover())
      .hasToString("By.cssSelector: [data-testid='popover'],[data-testid='help-popover'][data-open='true']");
    assertThat(instructions.getLoaderSelector())
      .hasToString("By.cssSelector: [data-testid='loader'],[data-testid='grid-loader']");
    assertThat(instructions.getLoadingBar()).hasToString("By.cssSelector: [data-testid='loading-bar']");
  }

  @Test
  void shouldKeepSelectorsBasedOnAweIdentifiers() {
    assertThat(instructions.getButton("ButOk")).hasToString("By.cssSelector: #ButOk:not([disabled])");
    assertThat(instructions.getRequiredPostLoginShellControls()).containsExactly(By.id("ButUsrAct"));
    assertThat(instructions.getOptionalPostLoginShellControls()).containsExactly(By.id("main-menu-toggle"), By.id("ButLogOut"));
    assertThat(instructions.containsText("some-class", "abc"))
      .hasToString("By.xpath: //*[contains(@class,'some-class')]//text()[contains(.,'abc')]/..");
  }

  @Test
  void shouldEscapeQuotesOfTheTextThatTestsSearch() throws Exception {
    assertThat(instructions.getGridCellText("Grd", "R1", "Col", "It's")).hasToString("By.xpath: "
      + "//*[@grid-id='Grd' or @tree-grid-id='Grd']//*[@data-testid='grid-row' and @row-id='R1']"
      + "//*[@data-testid='grid-cell' and @column-id='Col']//text()[contains(.,\"It's\")]/..");
    assertThat(instructions.getSelectResult("It's")).hasToString("By.xpath: "
      + "//*[@data-testid='select-dropdown']//*[@data-testid='select-option'][contains(normalize-space(.),\"It's\")]");
    assertThat(instructions.containsText("clazz", "It's")).hasToString("By.xpath: "
      + "//*[contains(@class,'clazz')]//text()[contains(.,\"It's\")]/..");

    List<By> locators = List.of(
      instructions.getGridCellText("Gr'd", "R'1", "Co'l", "a'b\"c"), instructions.findGridCell("Gr'd", "It's"),
      instructions.getGridHeader("Gr'd", "Co'l"), instructions.getGridCell("Gr'd", null, "Co'l"),
      instructions.getGridScrollZone("Gr'd"), instructions.getCellFromDatepicker("day", "It's"),
      instructions.getMenuOpenedChildren("Op't"), instructions.getSelectResult("It's"),
      instructions.getSuggestResult("a'b\"c"), instructions.containsText("cla'ss", "It's"));
    for (By locator : locators) {
      String xpath = locator.toString().substring("By.xpath: ".length());
      assertThat(XPathFactory.newInstance().newXPath().compile(xpath)).as(xpath).isNotNull();
    }
  }
  @Test
  void shouldLocateTheSessionStepsThroughTheAngularShell() {
    assertThat(instructions.getLoggedUser()).hasToString("By.cssSelector: #ButUsrAct span.avatar-text");
    // The logout button is visible in the shell: there is no user menu to open
    assertThat(instructions.getUserMenuButtonId()).isNull();
    assertThat(instructions.getLoginScreenMarker()).hasToString("By.cssSelector: .slogan");
    assertThat(instructions.getLoginScreenText()).isEqualTo("Almis Web Engine");
  }

  @Test
  void shouldLocateTheTitleAndTheTextOfAMessageByItsType() {
    assertThat(instructions.getMessageTitle("warning")).hasToString(
      "By.cssSelector: [data-testid='alert'][data-type='warning'] [data-testid='alert-title']");
    assertThat(instructions.getMessageText("warning")).hasToString(
      "By.cssSelector: [data-testid='alert'][data-type='warning'] [data-testid='alert-message']");
  }

  @Test
  void shouldLocateTheElementsOfTheCriteriaAndTheScreen() {
    assertThat(instructions.getMenuOptionItem("test"))
      .hasToString("By.cssSelector: [data-testid='menu-option'][option-name='test']");
    assertThat(instructions.getCriterionLabel("Unt")).hasToString("By.cssSelector: label[for='Unt']");
    assertThat(instructions.getCriterionUnit("Unt")).hasToString("By.cssSelector: [criterion-id='Unt'] .unit");
    assertThat(instructions.getValidationError()).hasToString("By.cssSelector: div.error-container");
    assertThat(instructions.getEnabledDatepickerDay())
      .hasToString("By.cssSelector: [data-testid='datepicker-day'][data-disabled='false']");
    assertThat(instructions.getActiveWizardStepNumber()).hasToString(
      "By.cssSelector: [data-testid='wizard-step'][data-active='true'] > span.wizard-step-number");
    assertThat(instructions.getTagList("tags")).hasToString("By.cssSelector: [awe-tag-list='tags'] span");
    assertThat(instructions.getChart("Chr")).hasToString("By.cssSelector: [chart-id='Chr'] svg");
    assertThat(instructions.getLogViewer()).hasToString("By.cssSelector: [data-testid='log-viewer']");
    assertThat(instructions.getEmbeddedFrame()).hasToString("By.cssSelector: iframe");
    assertThat(instructions.getOpenDialog("PrnOpt")).hasToString(
      "By.cssSelector: [data-testid='dialog'][data-testid-owner='PrnOpt'][data-open='true']");
  }

  @Test
  void shouldLocateButtonsInAnyState() {
    assertThat(instructions.getAnyButton("ButAct")).hasToString("By.cssSelector: #ButAct");
    assertThat(instructions.getDisabledButton("ButAct")).hasToString("By.cssSelector: #ButAct[disabled]");
  }

  @Test
  void shouldLocateTheGridsTheContextMenuAndTheTreeRows() {
    assertThat(instructions.getGrid("Grd")).hasToString("By.cssSelector: [grid-id='Grd']");
    assertThat(instructions.getGridHeaderCheckboxSelected("Grd")).hasToString("By.cssSelector: "
      + "[data-testid='grid'] [id='scope-Grd'] [data-testid='grid-header-checkbox'][data-selected='true']");
    assertThat(instructions.getContextMenu()).hasToString("By.cssSelector: [data-testid='context-menu']");
    assertThat(instructions.getContextMenuMask()).hasToString("By.cssSelector: div.component-mask");
    assertThat(instructions.getGridPageSize()).hasToString("By.cssSelector: [data-testid='grid-page-size']");
    assertThat(instructions.getGridIcon("Grd", "Ico", "plus")).hasToString("By.cssSelector: "
      + "[grid-id='Grd'] [column-id='Ico'] [data-testid='column-icon'][data-icon~='fa-plus']");
    assertThat(instructions.getColumnSuccessIcon("Sta"))
      .hasToString("By.cssSelector: [column-id='Sta']:first-child span.text-success");
    assertThat(instructions.getTreeRow("Tre", "R1")).hasToString("By.cssSelector: [tree-grid-id='Tre'] [row-id='R1']");
    assertThat(instructions.getTreeRowIcon("Tre", "R1"))
      .hasToString("By.cssSelector: [tree-grid-id='Tre'] [row-id='R1'] [data-testid='tree-icon']");
    assertThat(instructions.getDeletedTreeRow("Tre", "R1"))
      .hasToString("By.cssSelector: [tree-grid-id='Tre'] .DELETE [row-id='R1']");
  }

  @Test
  void shouldLocateTheNthOptionOfTheOpenDropdown() {
    assertThat(instructions.getSelectOption(2)).hasToString(
      "By.xpath: (//*[@data-testid='select-dropdown']//*[@data-testid='select-option'])[2]");
  }
}
