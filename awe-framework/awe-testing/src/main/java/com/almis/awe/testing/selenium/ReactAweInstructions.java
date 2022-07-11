package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

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
      return getGridScopeCss(gridId) + " .ui-grid-header-checkbox label.checkbox";
    } else if (rowId == null) {
      return getGridScopeCss(gridId) + " .ui-grid-row-selected [column-id='" + columnId + "'] ";
    } else {
      return getGridScopeCss(gridId) + " [row-id='" + rowId + "'] [column-id='" + columnId + "'] ";
    }
  }

  /**
   * Get grid scope in css
   *
   * @param gridId
   * @return
   */
  private String getGridScopeCss(String gridId) {
    return ".grid [id='scope-" + gridId + "']";
  }

  private String getParentXpath(String gridId, String rowId, String columnId) {
    return containsGridOrTreeGrid(gridId) + Optional.ofNullable(rowId)
      .map(r -> "//*[@row-id='" + r + "']//*[@column-id='" + columnId + "']")
      .orElse("//*[contains(@class, 'ui-grid-row-selected')]//*[@column-id='" + columnId + "']");
  }

  private String getGridXpath(String gridId) {
    return Optional.ofNullable(gridId).map(this::containsGridOrTreeGrid).orElse("");
  }

  private String getGridHeaderXpath(String gridId, String columnId) {
    return containsGridOrTreeGrid(gridId) + "//*[contains(@class, 'ui-grid-header-cell-row')]//*[@column-id='" + columnId + "']";
  }

  public By getGridScrollZone(String gridId) {
    return By.xpath(containsGridOrTreeGrid(gridId) + "//*[contains(@class, 'ui-grid-render-container-body')]//*[contains(@class, 'ui-grid-render-container')]//*[contains(@class, 'ui-grid-viewport')]");
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
    return String.format("//*[@grid-id='%s' or @tree-grid-id='%s']", gridId, gridId);
  }

  public By getDatepicker() {
    return By.cssSelector(".datepicker");
  }

  public By getDateCriterion(String parentSelector) {
    return By.cssSelector(parentSelector + " input");
  }

  public By getActiveDatepicker() {
    return By.xpath("//*[contains(@class,'datepicker')]//*[contains(@class,'active')]");
  }

  public By getCellFromDatepicker(String type, String search) {
    return By.xpath(String.format("//*[contains(@class,'datepicker')]//*[contains(@class,'datepicker-%ss')]//*[contains(@class,'%s') and not(contains(@class, 'old')) and not(contains(@class, 'new'))]//text()[.='%s']/..", type, type, search));
  }

  public By getLoaderSelector() {
    return By.cssSelector(".loader");
  }

  public By getLoadingBar() {
    return By.id("loading-bar");
  }

  public By getGridLoaderSelector() {
    return By.cssSelector(".grid-loader");
  }

  public By getGridHeader(String gridId, String columnId) {
    return By.xpath(getGridHeaderXpath(gridId, columnId));
  }

  public By getGridCell(String gridId, String rowId, String columnId) {
    return By.xpath(getParentXpath(gridId, rowId, columnId));
  }

  public By getGridSaveButton() {
    return By.cssSelector(".grid-row-save:not([disabled])");
  }

  public By getGridSaveButton(String gridId) {
    return By.cssSelector(String.format("#%s-grid-row-save:not([disabled])", gridId));
  }

  public By getGridCellText(String gridId, String rowId, String columnId, String search) {
    return By.xpath(String.format("%s//text()[contains(.,'%s')]/..", getParentXpath(gridId, rowId, columnId), search));
  }

  public By findGridCell(String gridId, String search) {
    return By.xpath(String.format("%s//*[contains(@class,'ui-grid-row')]//*[contains(@class,'ui-grid-cell-contents')]//text()[contains(.,'%s')]/..", getGridXpath(gridId), search));
  }

  public By getPopover() {
    return By.cssSelector(".popover:not(.ng-hide)");
  }

  public By containsText(String clazz, String contains) {
    return By.xpath(String.format("//*[contains(@class,'%s')]//text()[contains(.,'%s')]/..", clazz, contains));
  }

  public By getMessage(String type) {
    return By.cssSelector(String.format(".alert-zone .alert-%s button.close", type));
  }

  public By getMenuOpenedChildren(String option) {
    return By.xpath(String.format("//*[@name='%s']/following-sibling::ul[contains(@class,'opened')]", option));
  }

  public By getMenuDropdown() {
    return By.cssSelector(".mm-dropdown-first");
  }

  public By getButton(String buttonId) {
    return By.cssSelector(String.format("#%s:not([disabled])", buttonId));
  }

  public By getInfoButton(String buttonId) {
    return By.cssSelector(String.format("[info-dropdown-id='%s'] a", buttonId));
  }

  public By getTreeButton(String gridId, String rowId) {
    return By.cssSelector(String.format("[tree-grid-id='%s'] [row-id='%s'] i.tree-icon", gridId, rowId));
  }

  public By getTreeButtonLoader() {
    return By.cssSelector(".fa-spin");
  }

  public By getTab(String tabId) {
    return By.cssSelector(String.format("[criterion-id='%s'] .nav-tabs:not(.disabled)", tabId));
  }

  public By getTab(String tabId, String tabLabel) {
    return By.cssSelector(String.format("%s span[translate-multiple*='%s']", getCriterionCss(tabId), tabLabel));
  }

  public By getTabActive(String tabId, String tabLabel) {
    return By.cssSelector(String.format("%s li.active span[translate-multiple*='%s']", getCriterionCss(tabId), tabLabel));
  }

  public By getContextButton(String buttonId) {
    return By.cssSelector(String.format(".context-menu [option-id='%s'] a:not([disabled])", buttonId));
  }

  public By getCheckbox(String parentSelector) {
    return By.cssSelector(String.format("%s .input label,%s", parentSelector, parentSelector));
  }

  public By getCheckboxChecked(String criterionName, boolean isChecked) {
    String checkedSelector = isChecked ? ":checked" : ":not(:checked)";
    String activeSelector = isChecked ? ".active" : ":not(.active)";
    String criterionSelector = getCriterionCss(criterionName);
    return By.cssSelector(String.format("%s .input label input%s,%s%s", criterionSelector, checkedSelector, criterionSelector, activeSelector));
  }

  public By getSelectChoice(String parentSelector) {
    return By.cssSelector(String.format("%s .select2-choice", parentSelector));
  }

  public By getSelectLoader(String parentSelector) {
    return By.cssSelector(String.format("%s .loader", parentSelector));
  }

  public By getSelectDropdownList() {
    return By.cssSelector("#select2-drop");
  }

  public By getSelectDropdownListElements() {
    return By.cssSelector("#select2-drop .select2-results li");
  }

  public By getSelectDropdownListFirstElement() {
    return By.cssSelector("#select2-drop li:first-of-type");
  }

  public By getSelectDropdownListLastElement() {
    return By.cssSelector("#select2-drop li:last-of-type");
  }

  public By getSelectResult(String match) {
    return By.xpath(String.format("//*[@id='select2-drop']//*[contains(@class,'select2-result-label')]//text()[contains(.,'%s')]/..", match));
  }

  public By getSelectChosen(String criterionName) {
    return By.cssSelector(String.format("%s .select2-chosen", getCriterionCss(criterionName)));
  }

  public By getSelectMultipleTextContainer(String criterionName) {
    return By.cssSelector(String.format("%s .select2-search-choice div", getCriterionCss(criterionName)));
  }

  public By getSuggest() {
    return By.cssSelector("#select2-drop input.select2-input");
  }

  public By getSuggestInput() {
    return By.cssSelector("#select2-drop :not(.select2-search-hidden) input.select2-input");
  }

  public By getSuggestDropdownListLastElement() {
    return By.cssSelector("#select2-drop li:last-of-type .select2-result-label");
  }

  public By getSuggestMultipleInput(String parentSelector) {
    return By.cssSelector(String.format("%s input.select2-input", parentSelector));
  }

  public By getSuggestMultipleChoiceClose(String parentSelector) {
    return By.cssSelector(String.format("%s .select2-search-choice-close", parentSelector));
  }
}
