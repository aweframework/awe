package com.almis.awe.testing.selenium;

import com.almis.awe.testing.enumerated.MenuBehavior;
import com.almis.awe.testing.enumerated.RowEditBehavior;
import com.almis.awe.testing.enumerated.SuggestBehavior;
import org.openqa.selenium.By;

import java.util.List;

public interface IAweFrontEndInstructions extends IAweInstructions {

  /**
   * Retrieve criterion selector in css
   *
   * @param criterionName Criterion name
   * @return Css parent selector
   */
  String getCriterionCss(String criterionName);

  /**
   * Retrieve parent selector in css
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @return Css parent selector
   */
  String getParentCss(String gridId, String rowId, String columnId);

  /**
   * Retrieve the parent selector in css of a cell of the row being edited, where the editor of the cell is. By default
   * it is the selected row, as the clients edit the row the user selects; a client that can edit a row that is not
   * selected overrides it
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @return Css parent selector
   */
  default String getEditingParentCss(String gridId, String columnId) {
    return getParentCss(gridId, null, columnId);
  }

  /**
   * Retrieve criterion css selector
   *
   * @param parentSelector Parent selector
   * @return Criterion input selector
   */
  By getCriterionInput(String parentSelector);

  /**
   * Get loader selector
   *
   * @return loader selector
   */
  By getLoaderSelector();

  /**
   * Get loading bar
   *
   * @return loading bar selector
   */
  By getLoadingBar();

  /**
   * Get popover selector
   *
   * @return Popover selector
   */
  By getPopover();

  /**
   * Get a tag which contains a text
   *
   * @return tag which contains text selector
   */
  By containsText(String clazz, String text);

  /*
  =================================
  MESSAGES
  =================================
  */

  By getMessage(String type);

  /*
  =================================
  MENU
  =================================
  */

  /**
   * Get menu behavior
   * @return Menu click behavior
   */
  MenuBehavior getMenuBehavior();

  /**
   * Get menu option
   *
   * @param option Option to check
   * @return Opened children selector
   */
  By getMenuOption(String option);

  /**
   * Get opened children in menu
   *
   * @param option Option to check
   * @return Opened children selector
   */
  By getMenuOpenedChildren(String option);

  /**
   * Get menu dropdown. It must disappear once an option has been clicked, unless the client keeps its menu open (see
   * {@link #getMenuActiveOption(String)})
   *
   * @return Menu dropdown selector
   */
  By getMenuDropdown();

  /**
   * Get a menu option once its screen is the current one. A client whose menu stays open after a click (a side menu
   * that does not collapse) returns it so that the tests wait for the screen instead of for the dropdown to close.
   * By default it returns null: the tests wait for {@link #getMenuDropdown()} to disappear.
   *
   * @param option Option to check
   * @return Selector of the option while its screen is the current one, or null when the client closes its dropdown
   */
  default By getMenuActiveOption(String option) {
    return null;
  }

  /*
  =================================
  BUTTON
  =================================
  */

  /**
   * Get button
   *
   * @param buttonId Button identifier
   * @return Button selector
   */
  By getButton(String buttonId);

  /**
   * Get shell controls that must be actionable after successful login.
   *
   * @return Required post-login shell control selectors
   */
  List<By> getRequiredPostLoginShellControls();

  /**
   * Get shell controls that must be actionable when present after successful login.
   *
   * @return Post-login shell control selectors
   */
  List<By> getOptionalPostLoginShellControls();

  /**
   * Get info button
   *
   * @param buttonId Button identifier
   * @return Button selector
   */
  By getInfoButton(String buttonId);

  /**
   * Get tree button
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   * @return Button selector
   */
  By getTreeButton(String gridId, String rowId);

  /**
   * Get tree button loader
   *
   * @return Button loader selector
   */
  By getTreeButtonLoader();

  /*
  =================================
  TABS
  =================================
  */

  /**
   * Get tab
   *
   * @param tabId Tab identifier
   * @return Tab selector
   */
  By getTab(String tabId);

  /**
   * Get tab
   *
   * @param tabId    Tab identifier
   * @param tabLabel Tab label
   * @return Tab selector
   */
  By getTab(String tabId, String tabLabel);

  /**
   * Get tab active
   *
   * @param tabId    Tab identifier
   * @param tabLabel Tab label
   * @return Tab selector
   */
  By getTabActive(String tabId, String tabLabel);

  /**
   * Get tab menu
   *
   * @param tabId Tab identifier
   * @return Tab menu button selector
   */
  By getTabMenu(String tabId);

  /**
   * Get tab menu dropdown
   *
   * @param tabId Tab identifier
   * @return Tab menu dropdown selector
   */
  By getTabMenuDropdown(String tabId);

  /**
   * Get tab menu dropdown option
   *
   * @param tabId Tab identifier
   * @param tabLabel Tab label
   * @return Tab menu dropdown option selector
   */
  By getTabMenuDropdownOption(String tabId, String tabLabel);

  /*
  =================================
  CONTEXT BUTTON
  =================================
  */

  /**
   * Get context button
   *
   * @param buttonId Button identifier
   * @return Button selector
   */
  By getContextButton(String buttonId);

  /*
  =================================
  DATEPICKER
  =================================
  */

  /**
   * Get datepicker selector
   *
   * @return Datepicker selector
   */
  By getDatepicker();

  /**
   * Get date criterion selector
   *
   * @param parentSelector Parent selector
   * @return Date criterion selector
   */
  By getDateCriterion(String parentSelector);

  /**
   * Get active datepicker selector
   *
   * @return Active datepicker selector
   */
  By getActiveDatepicker();

  /**
   * Return cell from datepicker (year, month or day)
   *
   * @param type   Datepicker type
   * @param search Cell to search
   * @return Cell selector
   */
  By getCellFromDatepicker(String type, String search);

  /*
  =================================
  GRID
  =================================
  */

  /**
   * Retrieve grid scroll zone
   *
   * @param gridId Grid identifier
   * @return Scroll zone
   */
  By getGridScrollZone(String gridId);

  /**
   * Get grid loader selector
   *
   * @return grid loader selector
   */
  By getGridLoaderSelector();

  /**
   * Get grid header
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   * @return Grid header selector
   */
  By getGridHeader(String gridId, String columnId);

  /**
   * Get grid cell
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @return Grid cell selector
   */
  By getGridCell(String gridId, String rowId, String columnId);

  /**
   * Get grid save button
   *
   * @return Grid save button selector
   */
  By getGridSaveButton();

  /**
   * Get grid save button
   *
   * @param gridId   Grid identifier
   * @return Grid save button selector
   */
  By getGridSaveButton(String gridId);

  /**
   * Get grid cell text
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param search Text to search
   * @return Grid cell selector
   */
  By getGridCellText(String gridId, String rowId, String columnId, String search);

  /**
   * Find a cell containing a text
   *
   * @param gridId Grid identifier
   * @param search Text to search
   * @return Grid cell selector
   */
  By findGridCell(String gridId, String search);

  /**
   * Find the element to click to select the row that contains a text. By default it is the cell that contains it; a
   * client whose multiselect grids select a row only through its checkbox returns the checkbox.
   *
   * @param gridId Grid identifier (null for any grid)
   * @param search Text to search
   * @return Selector of the element that selects the row
   */
  default By findGridRowSelection(String gridId, String search) {
    return findGridCell(gridId, search);
  }

  /**
   * Find the row that contains a text when it is already selected. A client that keeps the selection of a grid when
   * the user comes back to its screen or searches again, and that toggles a selected row when it is selected again,
   * returns it, so that selecting a row that is already selected leaves it as it is. By default there is no such row.
   *
   * @param gridId Grid identifier (null for any grid)
   * @param search Text to search
   * @return Selector of the selected row, or null if the client does not keep the selection
   */
  default By findGridSelectedRow(String gridId, String search) {
    return null;
  }

  /**
   * Find the row that contains an element of a grid cell, relative to that element. A client whose rows are edited with
   * a double click returns it, together with {@link #getGridEditingRow}, so that a double click that did not start the
   * edition of the row can be repeated. By default the row is not identified.
   *
   * @return Selector of the row, to be searched from an element of a cell, or null if the client does not identify it
   */
  default By getGridRowOfCell() {
    return null;
  }

  /**
   * Find a row of a grid when it is being edited
   *
   * @param gridId Grid identifier (null for any grid)
   * @param rowId  Row identifier
   * @return Selector of the row being edited, or null if the client does not tell it
   */
  default By getGridEditingRow(String gridId, String rowId) {
    return null;
  }

  /**
   * Get row edit behavior
   * @return Row edit behavior
   */
  RowEditBehavior getRowEditBehavior();

  /*
  =================================
  CHECKBOX
  =================================
  */

  /**
   * Get checkbox selector
   *
   * @param parentSelector Parent selector
   * @return Checkbox selector
   */
  By getCheckbox(String parentSelector);

  /**
   * Get an option of a button group (a button checkbox or a button radio that the client renders as a group of
   * buttons). By default every option is a criterion of its own, as the AngularJS client renders it
   *
   * @param criterionName Criterion (group) name
   * @param optionId      Value of the option
   * @return Option selector
   */
  default By getCheckboxOption(String criterionName, String optionId) {
    return getCheckbox(getCriterionCss(optionId));
  }

  /**
   * Get checkbox checked or not
   * @param criterionName Criterion name
   * @param isChecked Checked or not
   * @return Checkbox checked selector
   */
  By getCheckboxChecked(String criterionName, boolean isChecked);

  /*
  =================================
  SELECT
  =================================
  */

  /**
   * Get select choice button
   *
   * @param parentSelector Parent selector in CSS
   * @return Select choice button selector
   */
  By getSelectChoice(String parentSelector);

  /**
   * Get select loader
   *
   * @param parentSelector Parent selector in CSS
   * @return Select loader selector
   */
  By getSelectLoader(String parentSelector);

  /**
   * Get select dropdown list
   *
   * @return Select dropdown list selector
   */
  By getSelectDropdownList();

  /**
   * Get select dropdown list elements
   *
   * @return Select dropdown list elements selector
   */
  By getSelectDropdownListElements();

  /**
   * Get select dropdown list first element
   *
   * @return Select dropdown list first element selector
   */
  By getSelectDropdownListFirstElement();

  /**
   * Get select dropdown list last element
   *
   * @return Select dropdown list last element selector
   */
  By getSelectDropdownListLastElement();

  /**
   * Get select result
   * @param search Result to search
   * @return Select result selector
   */
  By getSelectResult(String search);

  /**
   * Get select chosen element
   * @param criterionName Criterion name
   * @return Select chosen element selector
   */
  By getSelectChosen(String criterionName);

  /*
  =================================
  SELECT MULTIPLE
  =================================
  */

  /**
   * Get select multiple text container
   * @param criterionName Criterion name
   * @return Select multiple text container
   */
  By getSelectMultipleTextContainer(String criterionName);

  /*
  =================================
  SUGGEST
  =================================
  */

  /**
   * Get suggest behavior
   * @return Suggest text behavior
   */
  SuggestBehavior getSuggestBehavior();

  /**
   * Get suggest
   *
   * @param parent Parent CSS selector
   * @return Suggest selector
   */
  By getSuggest(String parent);

  /**
   * Get suggest with input
   *
   * @param parent Parent CSS selector
   * @return Suggest with input not hidden selector
   */
  By getSuggestInput(String parent);

  /**
   * Get suggest result
   *
   * @param match Match result
   * @return Suggest result element
   */
  By getSuggestResult(String match);

  /**
   * Get suggest dropdown list
   *
   * @return Suggest dropdown list selector
   */
  By getSuggestDropdownList();

  /**
   * Get suggest dropdown list last element
   *
   * @return Suggest dropdown list last element selector
   */
  By getSuggestDropdownListLastElement();

  /**
   * Get suggest choice button
   *
   * @param parentSelector Parent selector in CSS
   * @return Suggest choice button selector
   */
  By getSuggestChoice(String parentSelector);

  /**
   * Get suggest loader
   *
   * @param parentSelector Parent selector in CSS
   * @return Suggest loader selector
   */
  By getSuggestLoader(String parentSelector);

  /**
   * Get suggest chosen element
   * @param criterionName Criterion name
   * @return Select chosen element selector
   */
  By getSuggestChosen(String criterionName);

  /*
  =================================
  SUGGEST MULTIPLE
  =================================
  */

  /**
   * Get suggest multiple input
   *
   * @param parentSelector Parent selector in CSS
   * @return Suggest multiple input selector
   */
  By getSuggestMultipleInput(String parentSelector);

  /**
   * Get suggest multiple choice close
   *
   * @param parentSelector Parent selector in CSS
   * @return Suggest multiple choice close selector
   */
  By getSuggestMultipleChoiceClose(String parentSelector);

  /**
   * Check if the datepicker component requires an explicit click
   * @return true if manual click is needed, false otherwise.
   */
  boolean datePickerRequiresManualClick();

  /**
   * Check if the search box of a multiple choice (multiple select or suggest) lives inside a panel that must be
   * opened first, and stays open after choosing. Engines whose search box is always on the page keep the default.
   * @return true if the panel must be opened before searching, false otherwise.
   */
  default boolean multipleChoiceUsesPanel() {
    return false;
  }

  /*
  =================================
  SEMANTIC STEPS

  The methods below locate what the semantic steps of SeleniumUtilities ask for (the logged user, the title of a
  message, the active step of a wizard...). They are default methods so that implementations written before them keep
  compiling: the default is the rendering of the AngularJS client (through the shared hooks whenever there is one) and
  the clients that render something else override it.
  =================================
  */

  /**
   * Get the element that shows the name of the logged user once the user is logged in
   *
   * @return Logged user selector
   */
  default By getLoggedUser() {
    return By.cssSelector("#ButUsrAct span.avatar-text");
  }

  /**
   * Get the identifier of the button that opens the user menu, when the logout button is inside it. By default it returns
   * null: the logout button is visible in the shell
   *
   * @return Identifier of the user menu button, or null when there is no menu to open
   */
  default String getUserMenuButtonId() {
    return null;
  }

  /**
   * Tell whether the application asks for a confirmation before it logs the user out
   *
   * @return true when the logout must be confirmed. By default it is false
   */
  default boolean logoutNeedsConfirmation() {
    return false;
  }

  /**
   * Get the element that shows the login screen once the user has logged out
   *
   * @return Login screen marker selector
   * @see #getLoginScreenText()
   */
  default By getLoginScreenMarker() {
    return By.cssSelector(".slogan");
  }

  /**
   * Get the text that {@link #getLoginScreenMarker()} shows
   *
   * @return Text of the login screen marker
   */
  default String getLoginScreenText() {
    return "Almis Web Engine";
  }

  /**
   * Get the title of a message
   *
   * @param type Message type (success, info, warning, danger)
   * @return Message title selector
   */
  default By getMessageTitle(String type) {
    return By.cssSelector(TestIds.css(TestIds.ALERT) + TestAttributes.css(TestAttributes.TYPE, type) + " "
      + TestIds.css(TestIds.ALERT_TITLE));
  }

  /**
   * Get the text of a message
   *
   * @param type Message type (success, info, warning, danger)
   * @return Message text selector
   */
  default By getMessageText(String type) {
    return By.cssSelector(TestIds.css(TestIds.ALERT) + TestAttributes.css(TestAttributes.TYPE, type) + " "
      + TestIds.css(TestIds.ALERT_MESSAGE));
  }

  /**
   * Get an option of the application menu (the element that holds its link and its children)
   *
   * @param option Option name
   * @return Menu option selector
   */
  default By getMenuOptionItem(String option) {
    return By.cssSelector(TestIds.css(TestIds.MENU_OPTION) + "[option-name='" + option + "']");
  }

  /**
   * Get the label of a criterion
   *
   * @param criterionName Criterion name
   * @return Criterion label selector
   */
  default By getCriterionLabel(String criterionName) {
    return By.cssSelector("label[for='" + criterionName + "']");
  }

  /**
   * Get the unit addon of a criterion (the text after the input, such as "EUR")
   *
   * @param criterionName Criterion name
   * @return Criterion unit selector
   */
  default By getCriterionUnit(String criterionName) {
    return By.cssSelector(getCriterionCss(criterionName) + " .unit");
  }

  /**
   * Get the container of the validation errors of the screen
   *
   * @return Validation error container selector
   */
  default By getValidationError() {
    return By.cssSelector("div.error-container");
  }

  /**
   * Get a day of the open datepicker that can be picked
   *
   * @return Enabled day selector
   */
  default By getEnabledDatepickerDay() {
    return By.cssSelector(TestIds.css(TestIds.DATEPICKER_DAY) + TestAttributes.css(TestAttributes.DISABLED, false));
  }

  /**
   * Get the number of the active step of a wizard
   *
   * @return Active wizard step number selector
   */
  default By getActiveWizardStepNumber() {
    return By.cssSelector(TestIds.css(TestIds.WIZARD_STEP) + TestAttributes.css(TestAttributes.ACTIVE, true)
      + " > span.wizard-step-number");
  }

  /**
   * Get the active step of a wizard, identified by its number. The client may show something else than the number in
   * the step (an icon), so the number is part of the selector
   *
   * @param number Number of the step, starting at 1
   * @return Active wizard step selector
   */
  default By getActiveWizardStep(String number) {
    return By.xpath("//*[" + TestIds.xpath(TestIds.WIZARD_STEP) + " and " + TestAttributes.xpath(TestAttributes.ACTIVE, true)
      + "]/span[contains(@class,'wizard-step-number') and contains(normalize-space(.)," + XpathLiterals.of(number) + ")]");
  }

  /**
   * Get the tag list of a screen (the AWE component that shows a list of tags)
   *
   * @param tagListId Tag list identifier
   * @return Tag list selector
   */
  default By getTagList(String tagListId) {
    return By.cssSelector("[awe-tag-list='" + tagListId + "'] span");
  }

  /**
   * Get a chart
   *
   * @param chartId Chart identifier
   * @return Selector of what the chart renders
   */
  default By getChart(String chartId) {
    return By.cssSelector("[chart-id='" + chartId + "'] svg");
  }

  /**
   * Get the element that holds the text of the log viewer
   *
   * @return Log viewer selector
   */
  default By getLogViewer() {
    return By.cssSelector(TestIds.css(TestIds.LOG_VIEWER));
  }

  /**
   * Get the frame that embeds an external application in a screen
   *
   * @return Frame selector
   */
  default By getEmbeddedFrame() {
    return By.cssSelector("iframe");
  }

  /**
   * Get an open modal dialog
   *
   * @param dialogId Dialog identifier
   * @return Open dialog selector
   */
  default By getOpenDialog(String dialogId) {
    return By.cssSelector(TestIds.css(TestIds.DIALOG) + TestAttributes.css(TestAttributes.OWNER, dialogId)
      + TestAttributes.css(TestAttributes.OPEN, true));
  }

  /**
   * Get a button whatever its state (enabled or disabled)
   *
   * @param buttonId Button identifier
   * @return Button selector
   */
  default By getAnyButton(String buttonId) {
    return By.cssSelector("#" + buttonId);
  }

  /**
   * Get a disabled button
   *
   * @param buttonId Button identifier
   * @return Disabled button selector
   */
  default By getDisabledButton(String buttonId) {
    return By.cssSelector("#" + buttonId + "[disabled]");
  }

  /**
   * Get a grid (or a tree grid)
   *
   * @param gridId Grid identifier
   * @return Grid selector
   */
  default By getGrid(String gridId) {
    return By.cssSelector("[grid-id='" + gridId + "']");
  }

  /**
   * Get the checkbox of the header of a grid once all its rows are selected
   *
   * @param gridId Grid identifier
   * @return Selected header checkbox selector
   */
  default By getGridHeaderCheckboxSelected(String gridId) {
    return By.cssSelector(getParentCss(gridId, null, null) + TestAttributes.css(TestAttributes.SELECTED, true));
  }

  /**
   * Get the open context menu
   *
   * @return Context menu selector
   */
  default By getContextMenu() {
    return By.cssSelector(TestIds.css(TestIds.CONTEXT_MENU));
  }

  /**
   * Get the mask that covers the screen while a context menu is open, and closes the menu when it is clicked. By default
   * it returns null: the client closes its context menu with the keyboard
   *
   * @return Context menu mask selector, or null when the client has no mask
   */
  default By getContextMenuMask() {
    return By.cssSelector("div.component-mask");
  }

  /**
   * Get the selector of the page size of a grid
   *
   * @return Grid page size selector
   */
  default By getGridPageSize() {
    return By.cssSelector(TestIds.css(TestIds.GRID_PAGE_SIZE));
  }

  /**
   * Get the icon that a column of a grid shows for a row
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   * @param icon     Name of the icon, without the prefix of the icon library (for instance {@code plus})
   * @return Icon selector
   */
  default By getGridIcon(String gridId, String columnId, String icon) {
    return By.cssSelector("[grid-id='" + gridId + "'] [column-id='" + columnId + "'] " + TestIds.css(TestIds.COLUMN_ICON)
      + "[" + TestAttributes.ICON + "~='fa-" + icon + "']");
  }

  /**
   * Get the success icon of a column of a grid
   *
   * @param columnId Column identifier
   * @return Success icon selector
   */
  default By getColumnSuccessIcon(String columnId) {
    return By.cssSelector("[column-id='" + columnId + "']:first-child span.text-success");
  }

  /**
   * Get a row of a tree grid
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   * @return Tree row selector
   */
  default By getTreeRow(String gridId, String rowId) {
    return By.cssSelector("[tree-grid-id='" + gridId + "'] [row-id='" + rowId + "']");
  }

  /**
   * Get the expand/collapse icon of a row of a tree grid
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   * @return Tree row icon selector
   */
  default By getTreeRowIcon(String gridId, String rowId) {
    return By.cssSelector("[tree-grid-id='" + gridId + "'] [row-id='" + rowId + "'] " + TestIds.css(TestIds.TREE_ICON));
  }

  /**
   * Get a row of a tree grid that has been marked as deleted
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   * @return Deleted tree row selector
   */
  default By getDeletedTreeRow(String gridId, String rowId) {
    return By.cssSelector("[tree-grid-id='" + gridId + "'] .DELETE [row-id='" + rowId + "']");
  }

  /**
   * Get every option of the open select dropdown (or suggest list)
   *
   * @return Options selector
   */
  default By getSelectOptions() {
    return By.xpath("//*[" + TestIds.xpath(TestIds.SELECT_DROPDOWN) + "]//*[" + TestIds.xpath(TestIds.SELECT_OPTION) + "]");
  }

  /**
   * Get an option of the open select dropdown (or suggest list) by its position
   *
   * @param position Position of the option (the first is 1)
   * @return Option selector
   */
  default By getSelectOption(int position) {
    return By.xpath("(//*[" + TestIds.xpath(TestIds.SELECT_DROPDOWN) + "]//*[" + TestIds.xpath(TestIds.SELECT_OPTION)
      + "])[" + position + "]");
  }
}
