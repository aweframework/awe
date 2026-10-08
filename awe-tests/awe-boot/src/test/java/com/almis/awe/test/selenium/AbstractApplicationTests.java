package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the application screens share: each one opens its own screen, so a test does not depend on the
 * screen that the previous test left open
 */
abstract class AbstractApplicationTests extends AbstractSessionTests {

  protected AbstractApplicationTests() {
    super(Session.LOGGED_IN);
  }

  /**
   * Go to a screen to add a new option
   * @param options Options
   */
  void addNew(String... options) {
    // Go to screen
    gotoScreen(options);

    // Click on new button
    clickButton("ButNew", true);

    // Wait for button
    waitForButton("ButCnf");
  }

  /**
   * Go to a screen to add a new option
   *
   * @param suggest Suggest criteria
   * @param search Search
   * @param options Options
   */
  void update(String suggest, String search, String... options) {
    // Go to screen
    gotoScreen(options);

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Click on button
    clickButton("ButUpd", true);

    // Wait for button
    waitForButton("ButCnf");

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Delete from a screen
   * @param criterion Criterion to search
   * @param search Search text
   * @param options Screen options
   */
  void delete(String criterion, String search, String... options) {
    // Go to screen
    gotoScreen(options);

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest(criterion, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Store and confirm
    clickButtonAndConfirm("ButDel");
  }

  /**
   * Verify deleted
   * @param search Search
   */
  void verifyDeleted(String search) {
    // Wait for button
    clickButton("ButRst");

    // Search on grid
    searchAndWait();

    // Click row
    checkRowNotContains(search);
  }
}
