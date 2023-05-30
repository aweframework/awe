package com.almis.awe.testing.selenium;

import com.almis.awe.testing.enumerated.MenuBehavior;
import com.almis.awe.testing.enumerated.RowEditBehavior;
import com.almis.awe.testing.enumerated.SuggestBehavior;
import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

import static com.almis.awe.testing.constants.TestingConstants.*;

public class ReactAweInstructions implements IAweFrontEndInstructions {
  private SeleniumModel seleniumModel;

  public WebDriver getDriver() {
    return this.seleniumModel.getDriver();
  }

  public IAweInstructions setSeleniumModel(SeleniumModel seleniumModel) {
    this.seleniumModel = seleniumModel;
    return this;
  }

  public String getCriterionCss(String criterionName) {
    return "[criterion-id='" + criterionName + "']";
  }

  public String getParentCss(String gridId, String rowId, String columnId) {
    if (rowId == null && columnId == null) {
      return getGridScopeCss(gridId) + " th[role=columnheader] .p-checkbox";
    } else if (rowId == null) {
      return getGridScopeCss(gridId) + " [role=row].p-highlight [role=cell]." + columnId + " ";
    } else {
      return getGridScopeCss(gridId) + " [role=row]." + rowId + " [role=cell]." + columnId + " ";
    }
  }

  /**
   * Get grid scope in css
   *
   * @param gridId
   * @return
   */
  private String getGridScopeCss(String gridId) {
    return ".p-datatable[id='" + gridId + "']";
  }

  private String getParentXpath(String gridId, String rowId, String columnId) {
    return containsGridOrTreeGrid(gridId) + Optional.ofNullable(rowId)
      .map(r -> "//*[@role='row' and contains(@class, '" + rowId + "')]//*[@role='cell' and contains(@class, '" + columnId + "')]")
      .orElse("//*[@role='row' and contains(@class, 'p-highlight')]//*[@role='cell' and contains(@class, '" + columnId + "')]");
  }

  private String getGridXpath(String gridId) {
    return Optional.ofNullable(gridId).map(this::containsGridOrTreeGrid).orElse("");
  }

  private String getGridHeaderXpath(String gridId, String columnId) {
    return containsGridOrTreeGrid(gridId) + "//*[@role='row']//*[@role='columnheader' and contains(@class, '" + columnId + "')]";
  }

  public By getGridScrollZone(String gridId) {
    return By.xpath(containsGridOrTreeGrid(gridId) + "//*[contains(@class, 'p-datatable-wrapper')]");
  }

  public By getCriterionInput(String parentSelector) {
    return By.cssSelector(parentSelector + " input," + parentSelector + " textarea");
  }

  /**
   * Get xpath string for grid or treegrid
   *
   * @param gridId Grid identifier
   * @return Xpath string
   */
  private String containsGridOrTreeGrid(String gridId) {
    return String.format("//*[(contains(@class,'p-datatable') or contains(@class,'p-treetable')) and @id='%s']", gridId);
  }

  public By getDatepicker() {
    return By.cssSelector(".p-calendar");
  }

  public By getDateCriterion(String parentSelector) {
    return By.cssSelector(parentSelector + " input");
  }

  public By getActiveDatepicker() {
    return By.xpath("//*[contains(@class,'p-calendar')]//*[contains(@class,'active')]");
  }

  public By getCellFromDatepicker(String type, String search) {
    switch (type) {
      case MONTH:
        return By.xpath(String.format("//*[contains(@class,'p-datepicker')]//*[contains(@class,'p-monthpicker')]//text()[.='%s']/..", search));
      case YEAR:
        return By.xpath(String.format("//*[contains(@class,'p-datepicker')]//*[contains(@class,'p-yearpicker')]//text()[.='%s']/..", search));
      case DAY:
      default:
        return By.xpath(String.format("//*[contains(@class,'p-datepicker')]//*[contains(@class,'p-datepicker-calendar')]//*[not(contains(@class, 'p-disabled'))]//text()[.='%s']/..", search));
    }
  }

  public By getLoaderSelector() {
    return By.cssSelector(".p-progress-spinner");
  }

  public By getLoadingBar() {
    return By.cssSelector(".p-progress-spinner");
  }

  public By getGridLoaderSelector() {
    return By.cssSelector(".p-progress-spinner");
  }

  public By getGridHeader(String gridId, String columnId) {
    return By.xpath(getGridHeaderXpath(gridId, columnId));
  }

  public By getGridCell(String gridId, String rowId, String columnId) {
    return By.xpath(getParentXpath(gridId, rowId, columnId));
  }

  public By getGridSaveButton() {
    return By.cssSelector("[role=save-edit-row]:not([disabled])");
  }

  public By getGridSaveButton(String gridId) {
    return By.cssSelector(getGridScopeCss(gridId) + " [role=save-edit-row]:not([disabled])");
  }

  public By getGridCellText(String gridId, String rowId, String columnId, String search) {
    return By.xpath(String.format("%s//text()[contains(.,'%s')]/..", getParentXpath(gridId, rowId, columnId), search));
  }

  public By findGridCell(String gridId, String search) {
    return By.xpath(String.format("%s//*[@role='row']//*[@role='cell']//text()[contains(.,'%s')]/..", getGridXpath(gridId), search));
  }

  public RowEditBehavior getRowEditBehavior() {
    return RowEditBehavior.DOUBLE_CLICK;
  }

  public By getPopover() {
    return By.cssSelector(".popover:not(.ng-hide)");
  }

  public By containsText(String clazz, String contains) {
    return By.xpath(String.format("//*[contains(@class,'%s')]//text()[contains(.,'%s')]/..", clazz, contains));
  }

  public By getMessage(String type) {
    return By.cssSelector(String.format(".p-toast .p-toast-message-%s .p-toast-icon-close", type));
  }

  public MenuBehavior getMenuBehavior() {
    return MenuBehavior.CLICK_FIRST_AND_OPTION;
  }

  public By getMenuOption(String option) {
    return By.cssSelector(String.format("li[role='none'].p-menuitem.%s", option));
  }

  public By getMenuOpenedChildren(String option) {
    return By.xpath(String.format("//*[contains(@class,'%s') and contains(@class,'p-menuitem-active')]//ul", option));
  }

  public By getMenuDropdown() {
    return By.cssSelector(".p-submenu-list");
  }

  public By getButton(String buttonId) {
    return By.cssSelector(String.format("#%s:not([disabled])", buttonId));
  }

  public By getInfoButton(String buttonId) {
    return By.cssSelector(String.format(".p-overlay-badge button#%s", buttonId));
  }

  public By getTreeButton(String gridId, String rowId) {
    return By.cssSelector(String.format("#%s.p-treetable [role=row].%s .p-treetable-toggler", gridId, rowId));
  }

  public By getTreeButtonLoader() {
    return By.cssSelector(".fa-spin");
  }

  public By getTab(String tabId) {
    return By.cssSelector(String.format("#%s [role='tab']:not(.disabled)", tabId));
  }

  public By getTab(String tabId, String tabLabel) {
    return By.cssSelector(String.format("#%s [role='tab'].label-%s", tabId, tabLabel));
  }

  public By getTabMenu(String tabId) {
    // No tabmenu in react
    return null;
  }

  public By getTabMenuDropdown(String tabId) {
    // No tabmenu in react
    return null;
  }

  public By getTabMenuDropdownOption(String tabId, String tabLabel) {
    // No tabmenu in react
    return null;
  }

  public By getTabActive(String tabId, String tabLabel) {
    return By.cssSelector(String.format("#%s [role='tab'].p-highlight.label-%s", tabId, tabLabel));
  }

  public By getContextButton(String buttonId) {
    return By.cssSelector(String.format(".context-menu [option-id='%s'] a:not([disabled])", buttonId));
  }

  public By getCheckbox(String parentSelector) {
    return By.cssSelector(String.format("%s .p-checkbox", parentSelector));
  }

  public By getCheckboxChecked(String criterionName, boolean isChecked) {
    String checkedSelector = isChecked ? ".p-checkbox-checked" : ":not(.p-checkbox-checked)";
    String criterionSelector = getCriterionCss(criterionName);
    return By.cssSelector(String.format("%s .p-checkbox%s", criterionSelector, checkedSelector));
  }

  public By getSelectChoice(String parentSelector) {
    return By.cssSelector(String.format("%s .p-dropdown", parentSelector));
  }

  public By getSelectLoader(String parentSelector) {
    return By.cssSelector(String.format("%s .p-dropdown-loader", parentSelector));
  }

  public By getSelectDropdownList() {
    return By.cssSelector(".p-dropdown-panel");
  }

  public By getSelectDropdownListElements() {
    return By.cssSelector(".p-dropdown-panel li[role='option']");
  }

  public By getSelectDropdownListFirstElement() {
    return By.cssSelector(".p-dropdown-panel li[role='option']:first-of-type");
  }

  public By getSelectDropdownListLastElement() {
    return By.cssSelector(".p-dropdown-panel li[role='option']:last-of-type");
  }

  public By getSelectResult(String match) {
    return By.xpath(String.format("//*[contains(@class,'p-dropdown-panel')]//li[@role = 'option']//text()[contains(.,'%s')]/..", match));
  }

  public By getSelectChosen(String criterionName) {
    return By.cssSelector(String.format("%s .p-dropdown-label", getCriterionCss(criterionName)));
  }

  public By getSelectMultipleTextContainer(String criterionName) {
    return By.cssSelector(String.format("%s .select2-search-choice div", getCriterionCss(criterionName)));
  }

  public SuggestBehavior getSuggestBehavior() { return SuggestBehavior.INPUT; }

  public By getSuggestChoice(String parentSelector) {
    return By.cssSelector(String.format("%s input", parentSelector));
  }

  public By getSuggestLoader(String parentSelector) {
    return By.cssSelector(String.format("%s .p-autocomplete-loader", parentSelector));
  }

  public By getSuggest(String parentSelector) {
    return By.cssSelector(parentSelector + " input");
  }

  public By getSuggestInput(String parentSelector) {
    return By.cssSelector(parentSelector + " input");
  }

  public By getSuggestResult(String match) {
    return By.xpath(String.format("//*[contains(@class,'p-autocomplete-panel')]//li[@role = 'option']//text()[contains(.,'%s')]/..", match));
  }

  public By getSuggestChosen(String criterionName) {
    return By.cssSelector(String.format("%s .p-autocomplete-input", getCriterionCss(criterionName)));
  }

  public By getSuggestDropdownList() {
    return By.cssSelector("input");
  }

  public By getSuggestDropdownListLastElement() {
    return By.cssSelector(".p-autocomplete-panel li[role='option']:last-of-type");
  }

  public By getSuggestMultipleInput(String parentSelector) {
    return By.cssSelector(String.format("%s input", parentSelector));
  }

  public By getSuggestMultipleChoiceClose(String parentSelector) {
    return By.cssSelector(String.format("%s .p-autocomplete-token-icon", parentSelector));
  }
}
