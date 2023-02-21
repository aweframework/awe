package com.almis.awe.testing.selenium;

import org.openqa.selenium.By;

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
   * Get opened children in menu
   *
   * @param option Option to check
   * @return Opened children selector
   */
  By getMenuOpenedChildren(String option);

  /**
   * Get menu dropdown
   *
   * @return Menu dropdown selector
   */
  By getMenuDropdown();

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
   * Get suggest
   *
   * @return Suggest selector
   */
  By getSuggest();

  /**
   * Get suggest with input
   *
   * @return Suggest with input not hidden selector
   */
  By getSuggestInput();

  /**
   * Get suggest dropdown list last element
   *
   * @return Suggest dropdown list last element selector
   */
  By getSuggestDropdownListLastElement();

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
}
