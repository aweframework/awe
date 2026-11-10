package com.almis.awe.test.selenium;

import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;

/**
 * Steps of the default search of the saved searches of a window (#915) or a grid (#945): the Open with this search option
 * of the Save search dialog and the chip of the default search applied on load, and Clear all of the column filters of a
 * grid. The other saved search steps (#790) are the ones of {@code SeleniumUtilities}, which take a grid identifier as
 * window identifier.
 */
abstract class AbstractSavedSearchTests extends AbstractCrudTests {

  // Clear all of the column filters in effect of a grid (#912), the same hook in both engines
  private static final String GRID_COLUMN_FILTER_CLEAR_ALL = "grid-column-filter-clear-all";

  /**
   * @param classSession State that the tests of the class start from
   */
  protected AbstractSavedSearchTests(Session classSession) {
    super(classSession);
  }

  /**
   * Get the css selector of a part of the saved searches of a window, found by its hook
   *
   * @param windowId Window identifier
   * @param testId   Hook of the part
   * @return Css selector
   */
  private static String savedSearchesPart(String windowId, String testId) {
    return TestIds.css(testId) + TestAttributes.css(TestAttributes.OWNER, windowId);
  }

  /**
   * Get the css selector of a checkbox of the Save search dialog of a window in a state
   *
   * @param windowId Window identifier
   * @param testId   Hook of the checkbox
   * @param selected True when it must be checked
   * @return Css selector
   */
  private static String saveOption(String windowId, String testId, boolean selected) {
    return savedSearchesPart(windowId, testId) + TestAttributes.css(TestAttributes.SELECTED, selected);
  }

  /**
   * Save the current search of a window as the default search of the user: Save current search, the name, Keep layout
   * (unchecked by default), Open with this search and Save. Keep values is left as the dialog offers it, checked. It
   * waits for the dialog to close
   *
   * @param windowId Window identifier
   * @param name     Name of the search
   */
  protected void saveCurrentSearchAsDefault(String windowId, String name) {
    openSaveSearchWithName(windowId, name);
    click(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_LAYOUT, false));
    checkVisible(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_LAYOUT, true));
    checkVisible(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_VALUES, true));
    saveAsDefault(windowId);
  }

  /**
   * Save the current search of a grid (its column filters, quick filter, search and sort) as the default search of the
   * user (#945): Save current search, the name, Open with this search and Save. The dialog of a grid has no layout or
   * values to keep. It waits for the dialog to close
   *
   * @param gridId Grid identifier
   * @param name   Name of the search
   */
  protected void saveCurrentGridSearchAsDefault(String gridId, String name) {
    openSaveSearchWithName(gridId, name);
    checkNotVisible(savedSearchesPart(gridId, TestIds.WINDOW_SAVED_SEARCH_KEEP_LAYOUT));
    saveAsDefault(gridId);
  }

  /**
   * Open the Save search dialog of a window or a grid and write the name of the search
   *
   * @param ownerId Window or grid identifier
   * @param name    Name of the search
   */
  private void openSaveSearchWithName(String ownerId, String name) {
    openSaveSearch(ownerId);
    String input = savedSearchesPart(ownerId, TestIds.WINDOW_SAVED_SEARCH_NAME);
    checkVisible(input);
    clearText(input);
    writeText(Locator.css(input), name);
  }

  /**
   * Check Open with this search in the open Save search dialog of a window or a grid and save, waiting for the dialog to
   * close
   *
   * @param ownerId Window or grid identifier
   */
  private void saveAsDefault(String ownerId) {
    click(saveOption(ownerId, TestIds.WINDOW_SAVED_SEARCH_OPEN_WITH, false));
    checkVisible(saveOption(ownerId, TestIds.WINDOW_SAVED_SEARCH_OPEN_WITH, true));
    click(savedSearchesPart(ownerId, TestIds.WINDOW_SAVED_SEARCH_ACCEPT));
    checkNotVisible(savedSearchesPart(ownerId, TestIds.WINDOW_SAVED_SEARCH_DIALOG));
    waitForLoadingBar();
  }

  /**
   * Click Clear all in the column filters in effect of a grid (#912): the grid is reloaded without its column filters
   *
   * @param gridId Grid identifier
   */
  protected void clearAllGridColumnFilters(String gridId) {
    click(TestIds.css(GRID_COLUMN_FILTER_CLEAR_ALL) + TestAttributes.css(TestAttributes.OWNER, gridId));
  }

  /**
   * Check that a window (or a grid) shows the chip of the default search applied on load, with the name of the search
   *
   * @param windowId Window (or grid) identifier
   * @param name     Name of the search
   */
  protected void checkDefaultSearchApplied(String windowId, String name) {
    String chip = savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_APPLIED_DEFAULT);
    checkVisible(chip);
    checkTextContains(chip, name);
  }

  /**
   * Check that a window (or a grid) shows no chip of a default search
   *
   * @param windowId Window (or grid) identifier
   */
  protected void checkNoDefaultSearchApplied(String windowId) {
    checkNotVisible(savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_APPLIED_DEFAULT));
  }
}
