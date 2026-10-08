package com.almis.awe.testing.selenium;

import com.almis.awe.testing.enumerated.MenuBehavior;
import com.almis.awe.testing.enumerated.RowEditBehavior;
import com.almis.awe.testing.enumerated.SuggestBehavior;
import com.almis.awe.testing.model.SeleniumModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

import static com.almis.awe.testing.constants.TestingConstants.*;

/**
 * Selenium instructions for the React client.
 *
 * <p>Components are located through the {@code data-testid} hooks the client renders
 * ({@code awe-client-react/src/utilities/testIds.js}) and the attributes AWE already renders ({@code criterion-id},
 * {@code grid-id}, {@code row-id}, {@code column-id}...), never through the classes, the ARIA roles or the markup of
 * PrimeReact. The state that tests need is read from data attributes ({@code data-selected}, {@code data-active}...).
 * {@code ReactAweInstructionsSelectorGuardTest} fails if a selector depends on PrimeReact.</p>
 */
public class ReactAweInstructions implements IAweFrontEndInstructions {
  /** The React client has no popover (its help is a tooltip that ignores the mouse), so this selector never matches */
  private static final String NO_POPOVER_CSS = ".popover:not(.ng-hide)";

  /** Id that AWE gives to the control of the logged user */
  private static final String USER_ACTION_ID = "ButUsrAct";

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
   * The row being edited is not always the selected one: the row is edited with a double click, and in a grid with
   * multiple selection the two clicks may toggle its selection on and off, so the editors are located by the edit state
   */
  @Override
  public String getEditingParentCss(String gridId, String columnId) {
    return getGridScopeCss(gridId) + " " + css(TestIds.GRID_ROW) + stateCss(TestAttributes.EDITING, true)
      + " " + css(TestIds.GRID_CELL) + "[column-id='" + columnId + "'] ";
  }

  /**
   * Get grid scope in css
   *
   * @param gridId Grid identifier
   * @return Css selector of the grid scope
   */
  private String getGridScopeCss(String gridId) {
    return css(TestIds.GRID) + "[grid-id='" + gridId + "']";
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
    return By.xpath(containsGridOrTreeGrid(gridId) + "//*[" + xpath(TestIds.GRID_VIEWPORT) + "]");
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
    // The calendar overlay only exists while it is open
    return By.cssSelector(css(TestIds.DATEPICKER));
  }

  public By getDateCriterion(String parentSelector) {
    return By.cssSelector(parentSelector + " " + css(TestIds.CRITERION_INPUT));
  }

  public By getActiveDatepicker() {
    return By.cssSelector(css(TestIds.DATEPICKER) + " " + css(TestIds.DATEPICKER_DAY)
      + stateCss(TestAttributes.SELECTED, true));
  }

  public By getCellFromDatepicker(String type, String search) {
    String cellTestId;
    String exclusions = "";
    switch (type) {
      case MONTH:
        cellTestId = TestIds.DATEPICKER_MONTH;
        break;
      case YEAR:
        cellTestId = TestIds.DATEPICKER_YEAR;
        break;
      case DAY:
      default:
        // Days of the previous and the next month, and days that cannot be selected, are not valid cells
        cellTestId = TestIds.DATEPICKER_DAY;
        exclusions = " and not(" + stateXpath(TestAttributes.OUTSIDE_MONTH, true) + ") and not("
          + stateXpath(TestAttributes.DISABLED, true) + ")";
    }
    return By.xpath(String.format("//*[%s]//*[%s%s]//text()[.=%s]/..", xpath(TestIds.DATEPICKER),
      xpath(cellTestId), exclusions, XpathLiterals.of(search)));
  }

  public By getLoaderSelector() {
    // Any loader: the one of a view, the one of a grid and the one of a suggest
    return By.cssSelector(String.join(",", css(TestIds.LOADING_SPINNER), css(TestIds.GRID_LOADER), css(TestIds.LOADER)));
  }

  public By getLoadingBar() {
    // The React client has no loading bar: it shows a spinner while a view is loading
    return By.cssSelector(css(TestIds.LOADING_SPINNER));
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

  public By getColumnSuccessIcon(String columnId) {
    return By.cssSelector("[column-id='" + columnId + "'] " + css(TestIds.COLUMN_ICON) + " .text-success");
  }

  public By getGridSaveButton() {
    return By.cssSelector(css(TestIds.GRID_ROW_SAVE) + ":not([disabled])");
  }

  public By getGridSaveButton(String gridId) {
    // The save button of a tree grid is inside its "tree-grid-id" root
    return By.cssSelector(css(TestIds.GRID) + ":is([grid-id='" + gridId + "'],[tree-grid-id='" + gridId + "']) "
      + css(TestIds.GRID_ROW_SAVE) + ":not([disabled])");
  }

  public By getGridCellText(String gridId, String rowId, String columnId, String search) {
    return By.xpath(String.format("%s//text()[contains(.,%s)]/..", getParentXpath(gridId, rowId, columnId), XpathLiterals.of(search)));
  }

  public By findGridCell(String gridId, String search) {
    return By.xpath(String.format("%s//*[%s]//*[%s]//text()[contains(.,%s)]/..",
      getGridXpath(gridId), xpath(TestIds.GRID_ROW), xpath(TestIds.GRID_CELL), XpathLiterals.of(search)));
  }

  public By findGridRowSelection(String gridId, String search) {
    // A multiselect grid selects a row only through its checkbox (it comes before the cells in the row); the others,
    // by clicking the cell
    String literal = XpathLiterals.of(search);
    String cell = "//*[" + xpath(TestIds.GRID_CELL) + " and contains(normalize-space(.)," + literal + ")]";
    String row = getGridXpath(gridId) + "//*[" + xpath(TestIds.GRID_ROW) + "][." + cell + "]";
    return By.xpath("(" + row + "//*[" + xpath(TestIds.GRID_ROW_CHECKBOX) + "] | " + row + cell + ")[1]");
  }

  public By findGridSelectedRow(String gridId, String search) {
    return By.xpath(String.format("%s//*[%s and %s][.//*[%s and contains(normalize-space(.),%s)]]", getGridXpath(gridId),
      xpath(TestIds.GRID_ROW), stateXpath(TestAttributes.SELECTED, true), xpath(TestIds.GRID_CELL),
      XpathLiterals.of(search)));
  }

  public By getGridRowOfCell() {
    return By.xpath("ancestor-or-self::*[" + xpath(TestIds.GRID_ROW) + "][1]");
  }

  public By getGridEditingRow(String gridId, String rowId) {
    return By.xpath(getGridXpath(gridId) + "//*[" + xpath(TestIds.GRID_ROW) + " and "
      + stateXpath(TestAttributes.EDITING, true) + " and @row-id=" + XpathLiterals.of(rowId) + "]");
  }

  public RowEditBehavior getRowEditBehavior() {
    return RowEditBehavior.DOUBLE_CLICK;
  }

  public By getPopover() {
    return By.cssSelector(NO_POPOVER_CSS);
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
    // The side menu keeps its options open after a click, so clicking an open parent again would collapse it
    return MenuBehavior.CLICK_ALL;
  }

  public By getMenuOption(String option) {
    // The link is the element that reacts to the mouse
    return By.cssSelector(String.format("%s[name='%s']", css(TestIds.MENU_LINK), option));
  }

  public By getMenuOpenedChildren(String option) {
    return By.xpath(String.format("//*[%s and @option-name=%s and %s]",
      xpath(TestIds.MENU_OPTION), XpathLiterals.of(option), stateXpath(TestAttributes.OPEN, true)));
  }

  public By getMenuDropdown() {
    return By.cssSelector(css(TestIds.MENU_SUBMENU));
  }

  public By getMenuActiveOption(String option) {
    // The side menu does not collapse after a click: the option is marked as active once its screen is the current one
    return By.cssSelector(String.format("%s[option-name='%s']%s", css(TestIds.MENU_OPTION), option,
      stateCss(TestAttributes.ACTIVE, true)));
  }

  public By getButton(String buttonId) {
    // The button is identified by the id that AWE gives to it
    return By.cssSelector(String.format("#%s:not([disabled])", buttonId));
  }

  public List<By> getRequiredPostLoginShellControls() {
    // The sidebar shell shows the user as an avatar, the topbar shell as an info dropdown: both carry the same id
    return List.of(By.cssSelector(String.format("%s#%s,%s#%s", css(TestIds.AVATAR), USER_ACTION_ID,
      css(TestIds.INFO_DROPDOWN), USER_ACTION_ID)));
  }

  public List<By> getOptionalPostLoginShellControls() {
    return List.of(
      By.id("ButLogOut")
    );
  }

  public By getInfoButton(String buttonId) {
    // An info button, the button of an info dropdown and the avatar of the user menu carry the id that AWE gives to them
    return By.cssSelector(String.format("%s#%s,%s#%s,%s#%s", css(TestIds.INFO_BUTTON), buttonId,
      css(TestIds.INFO_DROPDOWN), buttonId, css(TestIds.AVATAR), buttonId));
  }

  public By getTreeButton(String gridId, String rowId) {
    // The rows of a tree grid have no hook: the icon carries the row identifier
    return By.cssSelector(String.format("[tree-grid-id='%s'] %s[row-id='%s']", gridId, css(TestIds.TREE_ICON), rowId));
  }

  public By getTreeButtonLoader() {
    // The tree grid shows the loader of the grids while it is loading
    return By.cssSelector(css(TestIds.GRID_LOADER));
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

  public By getTab(String tabId) {
    return By.cssSelector(getTabListCss(tabId));
  }

  public By getTab(String tabId, String tabLabel) {
    return By.xpath(String.format("//*[@criterion-id=%s]//*[%s and normalize-space(.)=%s]",
      XpathLiterals.of(tabId), xpath(TestIds.TAB_LABEL), XpathLiterals.of(tabLabel)));
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
    return By.xpath(String.format("//*[@criterion-id=%s]//*[%s and %s]//*[%s and normalize-space(.)=%s]",
      XpathLiterals.of(tabId), xpath(TestIds.TAB), stateXpath(TestAttributes.ACTIVE, true), xpath(TestIds.TAB_LABEL),
      XpathLiterals.of(tabLabel)));
  }

  public By getContextButton(String buttonId) {
    // In React the option identifier is rendered in the link
    return By.cssSelector(String.format("%s %s[option-id='%s']:not(%s)", css(TestIds.CONTEXT_MENU),
      css(TestIds.CONTEXT_MENU_LINK), buttonId, stateCss(TestAttributes.DISABLED, true)));
  }

  public By getCheckbox(String parentSelector) {
    // The control of a checkbox, a switch or a radio is the element that holds its state
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.CRITERION_INPUT)));
  }

  @Override
  public By getCheckboxOption(String criterionName, String optionId) {
    // A button group is one criterion: every option carries its value
    return By.cssSelector(String.format("%s %s[option-id='%s']", getCriterionCss(criterionName),
      css(TestIds.CRITERION_INPUT), optionId));
  }

  public By getCheckboxChecked(String criterionName, boolean isChecked) {
    return By.cssSelector(String.format("%s %s%s", getCriterionCss(criterionName), css(TestIds.CRITERION_INPUT),
      stateCss(TestAttributes.SELECTED, isChecked)));
  }

  public By getSelectChoice(String parentSelector) {
    // The arrow opens the panel: the middle of a short multiple select can be its clear icon, which empties the select
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_TRIGGER)));
  }

  public By getSelectLoader(String parentSelector) {
    // A select renders no loader, so nothing is found and there is nothing to wait for
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
   * Get the css of the options of the open select dropdown or suggest panel
   *
   * @return Css selector
   */
  private String getSelectOptionCss() {
    return css(TestIds.SELECT_DROPDOWN) + " " + css(TestIds.SELECT_OPTION);
  }

  /**
   * Get the xpath of the options of the open select dropdown or suggest panel
   *
   * @return Xpath
   */
  private String getSelectOptionXpath() {
    return String.format("//*[%s]//*[%s]", xpath(TestIds.SELECT_DROPDOWN), xpath(TestIds.SELECT_OPTION));
  }

  public By getSelectResult(String match) {
    return By.xpath(String.format("%s[contains(normalize-space(.),%s)]", getSelectOptionXpath(), XpathLiterals.of(match)));
  }

  public By getSelectChosen(String criterionName) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_VALUE)));
  }

  public By getSelectMultipleTextContainer(String criterionName) {
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_CHOICE)));
  }

  public SuggestBehavior getSuggestBehavior() { return SuggestBehavior.INPUT; }

  public By getSuggestChoice(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestLoader(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.LOADER)));
  }

  public By getSuggest(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestInput(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestResult(String match) {
    return By.xpath(String.format("%s[contains(normalize-space(.),%s)]", getSelectOptionXpath(), XpathLiterals.of(match)));
  }

  public By getSuggestChosen(String criterionName) {
    // The input of the suggest holds the chosen value
    return By.cssSelector(String.format("%s %s", getCriterionCss(criterionName), css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestDropdownList() {
    // The suggestions panel opens when the user types, so the wait after the click is for the search input
    return By.cssSelector(css(TestIds.SELECT_SEARCH));
  }

  public By getSuggestDropdownListLastElement() {
    return By.xpath("(" + getSelectOptionXpath() + ")[last()]");
  }

  public By getSuggestMultipleInput(String parentSelector) {
    // The search of a multiple suggest lives in the criterion; the one of a multiple select lives in its panel,
    // which is rendered outside the criterion. PrimeReact only renders a panel while it is open, and the steps close
    // the panel they open, so the panel search box belongs to the select being edited
    return By.cssSelector(String.format("%s %s, %s %s", parentSelector, css(TestIds.SELECT_SEARCH),
      css(TestIds.SELECT_DROPDOWN), css(TestIds.SELECT_SEARCH)));
  }

  public By getSuggestMultipleChoiceClose(String parentSelector) {
    return By.cssSelector(String.format("%s %s", parentSelector, css(TestIds.SELECT_CHOICE_CLOSE)));
  }

  public boolean datePickerRequiresManualClick() {
    return false;
  }

  @Override
  public boolean multipleChoiceUsesPanel() {
    // The filter of the multiple select is rendered inside its panel
    return true;
  }

  public By getLoggedUser() {
    return By.cssSelector(css(TestIds.AVATAR_NAME));
  }

  public String getUserMenuButtonId() {
    // The logout button is inside the menu of the avatar, which opens on click
    return USER_ACTION_ID;
  }

  @Override
  public boolean logoutNeedsConfirmation() {
    // The React reference application asks for a confirmation before logging out
    return true;
  }

  public By getLoginScreenMarker() {
    return By.cssSelector("#ButLogIn");
  }

  public String getLoginScreenText() {
    return "Login";
  }

  @Override
  public By getValidationError() {
    return By.cssSelector(css(TestIds.CRITERION_ERROR));
  }

  public By getCriterionUnit(String criterionName) {
    return By.cssSelector(getCriterionCss(criterionName) + " " + css(TestIds.CRITERION_UNIT));
  }

  public By getEnabledDatepickerDay() {
    // Days of the previous and the next month, and days that cannot be selected, are not valid cells
    return By.cssSelector(css(TestIds.DATEPICKER_DAY) + ":not(" + stateCss(TestAttributes.DISABLED, true) + "):not("
      + stateCss(TestAttributes.OUTSIDE_MONTH, true) + ")");
  }

  public By getActiveWizardStepNumber() {
    return By.cssSelector(css(TestIds.WIZARD_STEP) + stateCss(TestAttributes.ACTIVE, true) + " "
      + css(TestIds.WIZARD_STEP_NUMBER));
  }

  @Override
  public By getActiveWizardStep(String number) {
    // The step shows an icon instead of its number when it has one, so the number is read from the hook
    return By.xpath(String.format("//*[%s and %s and %s]", xpath(TestIds.WIZARD_STEP),
      stateXpath(TestAttributes.ACTIVE, true), stateXpath(TestAttributes.STEP_NUMBER, number)));
  }

  @Override
  public By getTagList(String tagListId) {
    return By.cssSelector(css(TestIds.TAG_LIST) + "[tag-list-id='" + tagListId + "']");
  }

  @Override
  public By getChart(String chartId) {
    // The chart carries its identifier and tells when it has been drawn
    return By.cssSelector(css(TestIds.CHART) + "[chart-id='" + chartId + "']" + stateCss(TestAttributes.RENDERED, true));
  }

  public By getContextMenuMask() {
    // The context menu closes with the keyboard: there is no mask to click
    return null;
  }

  public By getGridIcon(String gridId, String columnId, String icon) {
    // The hook carries the icon classes the column received: match one whole class (as in AngularJS), so that
    // "plus" does not also match "plus-circle"
    return By.cssSelector("[grid-id='" + gridId + "'] [column-id='" + columnId + "'] " + css(TestIds.COLUMN_ICON)
      + "[" + TestAttributes.ICON + "~='" + icon + "']");
  }

  public By getTreeRow(String gridId, String rowId) {
    return By.cssSelector(String.format("[tree-grid-id='%s'] %s[row-id='%s']", gridId, css(TestIds.GRID_ROW), rowId));
  }

  /**
   * A deleted row is marked with the deleted state of its row hook, not with the "DELETE" class AngularJS renders
   */
  @Override
  public By getDeletedTreeRow(String gridId, String rowId) {
    return By.cssSelector(String.format("[tree-grid-id='%s'] %s[row-id='%s']%s",
      gridId, css(TestIds.GRID_ROW), rowId, stateCss(TestAttributes.DELETED, true)));
  }

  public By getTreeRowIcon(String gridId, String rowId) {
    // The rows of a tree grid have no hook for their icon: the icon carries the row identifier
    return getTreeButton(gridId, rowId);
  }
}
