package com.almis.awe.test.selenium;

import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;

/**
 * Steps of the default search of the saved searches of a window (#915): the Open with this search option of the Save
 * search dialog and the chip of the default search applied on load. The other saved search steps (#790) are the ones of
 * {@code SeleniumUtilities}.
 */
abstract class AbstractSavedSearchTests extends AbstractCrudTests {

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
    openSaveSearch(windowId);
    String input = savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_NAME);
    checkVisible(input);
    clearText(input);
    writeText(Locator.css(input), name);
    click(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_LAYOUT, false));
    checkVisible(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_LAYOUT, true));
    checkVisible(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_KEEP_VALUES, true));
    click(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_OPEN_WITH, false));
    checkVisible(saveOption(windowId, TestIds.WINDOW_SAVED_SEARCH_OPEN_WITH, true));
    click(savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_ACCEPT));
    checkNotVisible(savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_DIALOG));
    waitForLoadingBar();
  }

  /**
   * Check that a window shows the chip of the default search applied on load, with the name of the search
   *
   * @param windowId Window identifier
   * @param name     Name of the search
   */
  protected void checkDefaultSearchApplied(String windowId, String name) {
    String chip = savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_APPLIED_DEFAULT);
    checkVisible(chip);
    checkTextContains(chip, name);
  }

  /**
   * Check that a window shows no chip of a default search
   *
   * @param windowId Window identifier
   */
  protected void checkNoDefaultSearchApplied(String windowId) {
    checkNotVisible(savedSearchesPart(windowId, TestIds.WINDOW_SAVED_SEARCH_APPLIED_DEFAULT));
  }
}
