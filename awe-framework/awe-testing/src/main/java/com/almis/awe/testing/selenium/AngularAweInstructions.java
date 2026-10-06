package com.almis.awe.testing.selenium;

import com.almis.awe.testing.constants.TestingConstants;
import com.almis.awe.testing.enumerated.MenuBehavior;
import com.almis.awe.testing.enumerated.RowEditBehavior;
import com.almis.awe.testing.enumerated.SuggestBehavior;
import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

public class AngularAweInstructions implements IAweFrontEndInstructions {

  // Match the whole label text: select2 splits it into several text nodes when it highlights the search term
  // (<span class="select2-match">B</span>ase), so a single text() node never contains the full option.
  private static final String SELECT_OPTION_CONTAINING = "%s[contains(normalize-space(.),%s)]";

  private SeleniumModel seleniumModel;

  /**
   * Get the Selenium driver
   *
   * @return Selenium driver
   * @throws UnsupportedOperationException When the tests run with a tool other than Selenium ({@code awe.test.tool})
   * @deprecated Selenium specific: it is only available when the tests run with the Selenium tool. Use the steps of
   * {@code SeleniumUtilities}, or its {@code getBrowser()} with a {@code Locator}. It stays available through the whole
   * 5.x line and is not removed before 6.0
   */
  @Deprecated
  public WebDriver getDriver() {
    return this.seleniumModel.getDriver();
  }

  public IAweInstructions setSeleniumModel(SeleniumModel seleniumModel) {
    this.seleniumModel = seleniumModel;
    return this;
  }

  private static String css(String testId) {
    return TestIds.css(testId);
  }

  private static String xpath(String testId) {
    return TestIds.xpath(testId);
  }

  private static String stateCss(String attribute, Object value) {
    return TestAttributes.css(attribute, value);
  }

  private static String stateXpath(String attribute, Object value) {
    return TestAttributes.xpath(attribute, value);
  }

  public String getCriterionCss(String criterionName) {
    return "[criterion-id='" + criterionName + "']";
  }

  public String getParentCss(String gridId, String rowId, String columnId) {
    if (rowId == null && columnId == null) {
      return getGridScopeCss(gridId) + " " + css(TestIds.GRID_HEADER_CHECKBOX);
    } else if (rowId == null) {
      return getGridScopeCss(gridId) + " " + css(TestIds.GRID_ROW) + stateCss(TestAttributes.SELECTED, true)
        + " " + css(TestIds.GRID_CELL) + "[column-id='" + columnId + "'] ";
    } else {
      return getGridScopeCss(gridId) + " " + css(TestIds.GRID_ROW) + "[row-id='" + rowId + "'] "
        + css(TestIds.GRID_CELL) + "[column-id='" + columnId + "'] ";
    }
  }

  /**
   * Get grid scope in css
   *
   * @param gridId Grid identifier
   * @return Css selector of the grid scope
   */
  private String getGridScopeCss(String gridId) {
    return css(TestIds.GRID) + " [id='scope-" + gridId + "']";
  }

  private String getParentXpath(String gridId, String rowId, String columnId) {
    String cell = "//*[" + xpath(TestIds.GRID_CELL) + " and @column-id=" + XpathLiterals.of(columnId) + "]";
    return containsGridOrTreeGrid(gridId) + Optional.ofNullable(rowId)
      .map(r -> "//*[" + xpath(TestIds.GRID_ROW) + " and @row-id=" + XpathLiterals.of(r) + "]" + cell)
      .orElse("//*[" + xpath(TestIds.GRID_ROW) + " and " + stateXpath(TestAttributes.SELECTED, true) + "]" + cell);
  }

  private String getGridXpath(String gridId) {
    return Optional.ofNullable(gridId).map(this::containsGridOrTreeGrid).orElse("");
  }

  private String getGridHeaderXpath(String gridId, String columnId) {
    return containsGridOrTreeGrid(gridId) + "//*[" + xpath(TestIds.GRID_HEADER_CELL) + " and @column-id=" + XpathLiterals.of(columnId) + "]";
  }

  public By getGridScrollZone(String gridId) {
    return By.xpath(containsGridOrTreeGrid(gridId) + "//*[" + xpath(TestIds.GRID_VIEWPORT) + " and "
      + stateXpath(TestAttributes.CONTAINER, "body") + "]");
  }

  public By getCriterionInput(String parentSelector) {
    return By.cssSelector(parentSelector + " " + css(TestIds.CRITERION_INPUT));
  }

  /**
   * Get xpath string for grid or treegrid
   *
   * @param gridId Grid identifier
   * @return Xpath string
   */
  private String containsGridOrTreeGrid(String gridId) {
    String literal = XpathLiterals.of(gridId);
    return String.format("//*[@grid-id=%s or @tree-grid-id=%s]", literal, literal);
  }

  public By getDatepicker() {
    return By.cssSelector(css(TestIds.DATEPICKER));
  }

  public By getDateCriterion(String parentSelector) {
    return By.cssSelector(parentSelector + " " + css(TestIds.CRITERION_INPUT));
  }

  public By getActiveDatepicker() {
    return By.cssSelector(css(TestIds.DATEPICKER) + " " + stateCss(TestAttributes.SELECTED, true));
  }

  public By getCellFromDatepicker(String type, String search) {
    return By.xpath(String.format("//*[%s]//*[%s and not(%s)]//text()[.=%s]/..",
      xpath(TestIds.DATEPICKER), xpath(getDatepickerCellTestId(type)), stateXpath(TestAttributes.OUTSIDE_MONTH, true),
      XpathLiterals.of(search)));
  }

  /**
   * Get the hook of the cells of a datepicker view
   *
   * @param type Cell type (day, month or year)
   * @return Hook of the cells
   */
  private String getDatepickerCellTestId(String type) {
    switch (type) {
      case TestingConstants.DAY:
        return TestIds.DATEPICKER_DAY;
      case TestingConstants.MONTH:
        return TestIds.DATEPICKER_MONTH;
      case TestingConstants.YEAR:
        return TestIds.DATEPICKER_YEAR;
      default:
        throw new IllegalArgumentException("Unsupported datepicker cell type [" + type + "]");
    }
  }

  public By getLoaderSelector() {
    return By.cssSelector(css(TestIds.LOADER) + "," + css(TestIds.GRID_LOADER));
  }

  public By getLoadingBar() {
    return By.cssSelector(css(TestIds.LOADING_BAR));
  }

  public By getGridLoaderSelector() {
    return By.cssSelector(css(TestIds.GRID_LOADER));
  }

  public By getGridHeader(String gridId, String columnId) {
    return By.xpath(getGridHeaderXpath(gridId, columnId));
  }

  public By getGridCell(String gridId, String rowId, String columnId) {
    return By.xpath(getParentXpath(gridId, rowId, columnId));
  }

  public By getGridSaveButton() {
    return By.cssSelector(css(TestIds.GRID_ROW_SAVE) + ":not([disabled])");
  }

  public By getGridSaveButton(String gridId) {
    // The save button of a grid is identified by the id that AWE gives to it
    return By.cssSelector(String.format("#%s-grid-row-save:not([disabled])", gridId));
  }

  public By getGridCellText(String gridId, String rowId, String columnId, String search) {
    return By.xpath(String.format("%s//text()[contains(.,%s)]/..", getParentXpath(gridId, rowId, columnId), XpathLiterals.of(search)));
  }

  public By findGridCell(String gridId, String search) {
    return By.xpath(String.format("%s//*[%s]//*[%s]//text()[contains(.,%s)]/..",
      getGridXpath(gridId), xpath(TestIds.GRID_ROW), xpath(TestIds.GRID_CELL), XpathLiterals.of(search)));
  }

  public RowEditBehavior getRowEditBehavior() {
    return RowEditBehavior.SINGLE_CLICK;
  }

  public By getPopover() {
    // Message popovers exist only while they are shown. The help popover (awe-help) is always rendered, so it counts
    // only while it is open. The mouse must be moved away from both, or they intercept the next click
    return By.cssSelector(css(TestIds.POPOVER) + "," + css(TestIds.HELP_POPOVER) + stateCss(TestAttributes.OPEN, true));
  }

  /**
   * The class to search is supplied by the caller, so it cannot be replaced by a test hook. Prefer a hook of the
   * vocabulary ({@link TestIds}) in new tests.
   */
  public By containsText(String clazz, String contains) {
    return By.xpath(String.format("//*[contains(@class,%s)]//text()[contains(.,%s)]/..",
      XpathLiterals.of(clazz), XpathLiterals.of(contains)));
  }

  public By getMessage(String type) {
    return By.cssSelector(css(TestIds.ALERT) + stateCss(TestAttributes.TYPE, type) + " " + css(TestIds.ALERT_CLOSE));
  }

  public MenuBehavior getMenuBehavior() {
    return MenuBehavior.CLICK_ALL;
  }

  public By getMenuOption(String option) {
    return By.cssSelector(String.format("%s[name='%s']", css(TestIds.MENU_LINK), option));
  }

  public By getMenuOpenedChildren(String option) {
    return By.xpath(String.format("//*[%s and @name=%s]/following-sibling::*[(%s or %s) and %s]",
      xpath(TestIds.MENU_LINK), XpathLiterals.of(option), xpath(TestIds.MENU_DROPDOWN), xpath(TestIds.MENU_SUBMENU),
      stateXpath(TestAttributes.OPEN, true)));
  }

  public By getMenuDropdown() {
    // AWE's own class, kept on purpose: the hook (menu-dropdown) is equivalent, but with it the local Regression suite
    // lost the text typed in the first login after a logout (see "Decisions for the parent" in the T3 report)
    return By.cssSelector(".mm-dropdown-first");
  }

  public By getButton(String buttonId) {
    return By.cssSelector(String.format("#%s:not([disabled])", buttonId));
  }

  public List<By> getRequiredPostLoginShellControls() {
    return List.of(By.id("ButUsrAct"));
  }

  public List<By> getOptionalPostLoginShellControls() {
    return List.of(
      By.id("main-menu-toggle"),
      By.id("ButLogOut")
    );
  }

  public By getInfoButton(String buttonId) {
    return By.cssSelector(String.format("[info-dropdown-id='%s'] %s", buttonId, css(TestIds.INFO_DROPDOWN_TOGGLE)));
  }

  public By getTreeButton(String gridId, String rowId) {
    return By.cssSelector(String.format("[tree-grid-id='%s'] %s[row-id='%s'] %s",
      gridId, css(TestIds.GRID_ROW), rowId, css(TestIds.TREE_ICON)));
  }

  public By getTreeButtonLoader() {
    return By.cssSelector(css(TestIds.TREE_ICON) + stateCss(TestAttributes.LOADING, true));
  }

  /**
   * Get the css of the tab list of an enabled tab criterion
   *
   * @param tabId Tab criterion identifier
   * @return Css selector
   */
  private String getTabListCss(String tabId) {
    return getCriterionCss(tabId) + " " + css(TestIds.TAB_LIST) + stateCss(TestAttributes.DISABLED, false);
  }

  /**
   * Get the css of the label of a tab
   *
   * @param tabLabel Tab label
   * @return Css selector
   */
  private String getTabLabelCss(String tabLabel) {
    return String.format("%s[translate-multiple='%s']", css(TestIds.TAB_LABEL), tabLabel);
  }

  public By getTab(String tabId) {
    return By.cssSelector(getTabListCss(tabId));
  }

  public By getTab(String tabId, String tabLabel) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(tabId), getTabLabelCss(tabLabel)));
  }

  public By getTabMenu(String tabId) {
    return By.cssSelector(String.format("%s %s", getTabListCss(tabId), css(TestIds.TABDROP_TOGGLE)));
  }

  public By getTabMenuDropdown(String tabId) {
    return By.cssSelector(String.format("%s %s", getTabListCss(tabId), css(TestIds.TABDROP_MENU)));
  }

  public By getTabMenuDropdownOption(String tabId, String tabLabel) {
    return By.cssSelector(String.format("%s %s %s", getTabListCss(tabId), css(TestIds.TABDROP_MENU), getTabLabelCss(tabLabel)));
  }

  public By getTabActive(String tabId, String tabLabel) {
    return By.cssSelector(String.format("%s %s%s %s", getCriterionCss(tabId), css(TestIds.TAB),
      stateCss(TestAttributes.ACTIVE, true), getTabLabelCss(tabLabel)));
  }

  public By getContextButton(String buttonId) {
    return By.cssSelector(String.format("%s %s[option-id='%s'] %s:not(%s)", css(TestIds.CONTEXT_MENU),
      css(TestIds.CONTEXT_MENU_OPTION), buttonId, css(TestIds.CONTEXT_MENU_LINK), stateCss(TestAttributes.DISABLED, true)));
  }

  public By getCheckbox(String parentSelector) {
    return By.cssSelector(String.format("%s .input label,%s", parentSelector, parentSelector));
  }

  public By getCheckboxChecked(String criterionName, boolean isChecked) {
    String checkedSelector = isChecked ? ":checked" : ":not(:checked)";
    return By.cssSelector(getCriterionCss(criterionName) + " " + css(TestIds.CRITERION_INPUT) + checkedSelector);
  }

  public By getSelectChoice(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT)));
  }

  public By getSelectLoader(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.LOADER)));
  }

  public By getSelectDropdownList() {
    return By.cssSelector(css(TestIds.SELECT_DROPDOWN));
  }

  public By getSelectDropdownListElements() {
    return By.cssSelector(getSelectOptionCss());
  }

  public By getSelectDropdownListFirstElement() {
    // The first element found is the first option of the open dropdown
    return By.cssSelector(getSelectOptionCss());
  }

  public By getSelectDropdownListLastElement() {
    return By.xpath("(" + getSelectOptionXpath() + ")[last()]");
  }

  /**
   * Get the css of the options of the open select dropdown
   *
   * @return Css selector
   */
  private String getSelectOptionCss() {
    return css(TestIds.SELECT_DROPDOWN) + " " + css(TestIds.SELECT_OPTION);
  }

  /**
   * Get the xpath of the options of the open select dropdown
   *
   * @return Xpath
   */
  private String getSelectOptionXpath() {
    return String.format("//*[%s]//*[%s]", xpath(TestIds.SELECT_DROPDOWN), xpath(TestIds.SELECT_OPTION));
  }

  public By getSelectResult(String match) {
    return By.xpath(String.format(SELECT_OPTION_CONTAINING, getSelectOptionXpath(), XpathLiterals.of(match)));
  }

  public By getSelectChosen(String criterionName) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_VALUE)));
  }

  public By getSelectMultipleTextContainer(String criterionName) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_CHOICE)));
  }

  public SuggestBehavior getSuggestBehavior() { return SuggestBehavior.TEXT; };

  public By getSuggestChoice(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT)));
  }

  public By getSuggestLoader(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.LOADER)));
  }

  public By getSuggest(String parentSelector) {
    return By.cssSelector(css(TestIds.SELECT_DROPDOWN) + " " + css(TestIds.SELECT_SEARCH));
  }

  public By getSuggestInput(String parentSelector) {
    return By.cssSelector(css(TestIds.SELECT_DROPDOWN) + " " + css(TestIds.SELECT_SEARCH));
  }

  public By getSuggestChosen(String criterionName) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_VALUE)));
  }

  public By getSuggestDropdownList() {
    return By.cssSelector(css(TestIds.SELECT_DROPDOWN));
  }

  public By getSuggestDropdownListLastElement() {
    return By.xpath("(" + getSelectOptionXpath() + ")[last()]");
  }

  public By getSuggestMultipleInput(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestMultipleChoiceClose(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_CHOICE_CLOSE)));
  }

  public By getSuggestResult(String match) {
    return By.xpath(String.format(SELECT_OPTION_CONTAINING, getSelectOptionXpath(), XpathLiterals.of(match)));
  }

  public boolean datePickerRequiresManualClick() {
    return true;
  }
}
