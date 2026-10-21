package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Dependencies of the dependency test screen: the conditions (contains, not contains, in) and the source events (dialog
 * open and close, grid check-one-selected and delete-row). Every test opens its own screen.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class DependencyTestsIT extends AbstractSessionTests {

  private static final String MODULE = "test";
  private static final String SCREEN = "dependency-test";
  private static final String TEXT = "DepTxt";
  private static final String COLOUR = "DepCol";
  private static final String DIALOG_RESULT = "DepDlg";
  private static final String GRID_RESULT = "DepGrd";
  private static final String GRID = "GrdDep";
  private static final String CONTAINS_BUTTON = "ButDepContains";
  private static final String NOT_CONTAINS_BUTTON = "ButDepNotContains";
  private static final String IN_BUTTON = "ButDepIn";

  DependencyTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogoutWithConfirmation();
  }

  /**
   * Conditions contains and not contains: case insensitive, and an empty value does not contain the text
   */
  @Test
  void t010_containsConditions() {
    setTestTitle("Dependencies: contains and not contains");
    gotoScreen(MODULE, SCREEN);

    // Nothing written yet: the value does not contain the text
    waitForButton(NOT_CONTAINS_BUTTON);
    checkButtonNotVisible(CONTAINS_BUTTON);

    // The text is inside the value, in another case
    writeText(TEXT, "My AWE application");
    waitForButton(CONTAINS_BUTTON);
    checkButtonNotVisible(NOT_CONTAINS_BUTTON);

    // The text is not inside the value any more
    writeText(TEXT, "Another application");
    waitForButton(NOT_CONTAINS_BUTTON);
    checkButtonNotVisible(CONTAINS_BUTTON);
  }

  /**
   * Condition in: the value has to be one of the values of the list, not a part of one of them
   */
  @Test
  void t020_inCondition() {
    setTestTitle("Dependencies: in");
    gotoScreen(MODULE, SCREEN);

    // Nothing written yet: the value is not in the list
    checkButtonNotVisible(IN_BUTTON);

    // A value of the list
    writeText(COLOUR, "green");
    waitForButton(IN_BUTTON);

    // A value that is not in the list
    writeText(COLOUR, "blue");
    checkButtonNotVisible(IN_BUTTON);

    // A part of a value of the list is not a value of the list
    writeText(COLOUR, "gree");
    checkButtonNotVisible(IN_BUTTON);

    // Another value of the list
    writeText(COLOUR, "red");
    waitForButton(IN_BUTTON);
  }

  /**
   * Events open and close of a dialog
   */
  @Test
  void t030_dialogEvents() {
    setTestTitle("Dependencies: dialog open and close events");
    gotoScreen(MODULE, SCREEN);
    writeText(DIALOG_RESULT, "");

    // Opening the dialog raises its open event
    clickButton("ButDepDialog");
    checkCriterionContents(DIALOG_RESULT, "Opened");

    // Closing it raises its close event
    closeDialog("DlgDep");
    checkDialogClosed("DlgDep");
    checkCriterionContents(DIALOG_RESULT, "Closed");
  }

  /**
   * Events check-one-selected and delete-row of a grid
   */
  @Test
  void t040_gridEvents() {
    setTestTitle("Dependencies: grid check-one-selected and delete-row events");
    gotoScreen(MODULE, SCREEN);
    writeText(GRID_RESULT, "");

    // The check raises its event when one row is selected
    clickRowContents(GRID, "awedb1");
    clickButton("ButDepCheckOne");
    checkCriterionContents(GRID_RESULT, "One selected");

    // Deleting the selected row raises the delete event
    clickButton("ButDepDelete");
    checkCriterionContents(GRID_RESULT, "Deleted");
    checkRowNotContains("awedb1");
  }
}
