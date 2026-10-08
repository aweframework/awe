package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Regression tests of the test screens. Every test starts logged in with the Test module selected and opens its own screen,
 * so a failing test does not leave the next ones without a session or a screen. The wrong login test logs out and in again:
 * when it fails in between, the next test logs in again through the session setup.
 */
@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class RegressionTestsIT extends AbstractSessionTests {

  RegressionTestsIT() {
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
   * Select test module on select criterion
   */
  @Test
  @StartsFrom(Session.LOGGED_IN)
  void t001_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Check the menu of the module
    checkMenuOption("test", "Tests");
  }

  /**
   * Load suggest on grid: Test to check suggest initial load on grid (#30648)
   */
  @Test
  void t002_loadSuggestOnGrid() {
    // Title
    setTestTitle("Load suggest on grid: Test to check suggest initial load on grid (#30648)");

    // Go to matrix test
    gotoScreen("test", "matrix", "matrix-test");

    // Wait for button
    waitForButton("ButPrn");

    // Click on tab
    // The React client shows the translated label of the tab
    clickTab("TabSelMat", "Editable");

    // Check row contents
    checkRowContents("Prueba - adminflare");
  }

  /**
   * Test to check suggest criteria with 'strict' attribute set to false
   */
  @Test
  void t003_suggestStrict() {
    // Title
    setTestTitle("Suggest Strict: Test to check suggest criteria with 'strict' attribute set to false");

    // Go to screen
    gotoScreen("tools", "users");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggestLast("CrtUsr", "te");

    // Search and wait
    searchAndWait();

    // Check grid values
    checkRowContents("test");

    // Check criterion value
    checkSuggestContents("CrtUsr", "te");
  }

  /**
   * Test for read dependency
   */
  @Test
  void t004_readDependency() {
    // Title
    setTestTitle("Test for read dependency");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("TxtReq", "aaa");

    // Write on criterion (the numeric input of the React client takes a typed "." as its decimal separator, so the
    // thousands separator is not typed: the client adds it)
    writeText("Unt", "325274,50");

    // Assert text
    checkCriterionContents("Unt", "325.274,50");
  }

  /**
   * Quote check on unit label
   */
  @Test
  void t005_quoteCheckUnitLabel() {
    // Title
    setTestTitle("Quote check on unit label");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Tar", "\"");

    // Click on checkbox
    clickCheckbox("RadBox3");

    // Wait for text
    checkCriterionUnit("Unt", "USD");
  }

  /**
   * Check filtered date dependency (#31141)
   */
  @Test
  void t006_checkFilteredDateDependency() {
    // Title
    setTestTitle("Check filtered date dependency (#31141)");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "edita");

    // Check text
    checkCriterionContents("Txt", "edita");

    // Click on date
    clickDate("FilCalRea");

    // Click on selector
    clickEnabledDatepickerDay();
  }

  /**
   * Keep criteria test
   */
  @Test
  void t007_keepCriteria() {
    // Title
    setTestTitle("Keep criteria test");

    // Go to screen
    gotoScreen("tools", "users");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtUsr", "test", "test");

    // Search and wait
    searchAndWait();

    // Check grid values
    clickRowContents("test");

    // Click button
    clickButton("ButViw", true);

    // Click button
    clickButton("ButBck", true);

    // Wait for button
    waitForButton("ButRst");

    // Wait for button
    checkSuggestContents("CrtUsr", "test");
  }

  /**
   * Delayed suggest
   */
  @Test
  void t008_delayedSuggest() {
    // Title
    setTestTitle("Delayed suggest");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Click button
    clickButton("ButRst");

    // Suggest delayed
    suggestReplacingSearch("Sug", "tee", "test", "test", 800);

    // Suggest delayed
    suggestMultipleReplacingSearch("SugMulReq", "tee", "test", "test", 800);

    // Check selector
    checkMultipleSelectorContents("SugMulReq", "test (test@test.com)");
  }

  /**
   * Wrong login
   */
  @Test
  void t009_wrongLogin() {
    // Title
    setTestTitle("Wrong login");

    // Do logout
    checkLogoutWithConfirmation();

    // Check wrong login
    checkLoginRejected("test", "lala", "warning", "Invalid credentials",
      "The credentials entered for the user -test- are not valid");

    // Check wrong login
    checkLoginRejected("tutu", "lala", "warning", "Wrong username", "Username -tutu- is wrong or inactive");

    // Do right login
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Sort a grid using a component column
   */
  @Test
  void t010_sortComponentColumn() {
    // Title
    setTestTitle("Sort a grid using a component column");

    // Go to screen
    gotoScreen("tools", "users");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtUsr", "test", "test");

    // Search and wait
    searchAndWait();

    // Scroll grid to the right
    scrollGrid("GrdUsrLst", 10000, 0);

    // Sort by a component field
    sortGrid("GrdUsrLst", "StaIco");

    // Expect not to have an error message
    checkMessageMissing("danger");

    // Sort by a component field
    sortGrid("GrdUsrLst", "BlkIco");

    // Expect not to have an error message
    checkMessageMissing("danger");

    // Sort by a component field
    sortGrid("GrdUsrLst", "LanTxt");

    // Expect not to have an error message
    checkMessageMissing("danger");

    // Sort by a component field
    sortGrid("GrdUsrLst", "LanImg");

    // Expect not to have an error message
    checkMessageMissing("danger");
  }

  /**
   * Sort a grid using a component column
   */
  @Test
  void t011_suggestRepeatsValues() {
    // Title
    setTestTitle("Check that suggested values are not repeated");

    // Go to screen
    gotoScreen("tools", "themes");

    // Wait for button
    clickButton("ButRst");

    // Click on suggest
    openSuggest("CrtNam");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtNam", "a");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtNam", "s");

    // Pause
    pause(1000);

    // Write text
    writeSuggestSearch("CrtNam", "p");

    // Pause
    pause(1000);

    // Check there's only one result
    checkSuggestResultCount(1);

    // Click selector
    selectResult("asp");
  }

  /**
   * Test for fill over select
   */
  @Test
  void t012_fillOverSelect() {
    // Title
    setTestTitle("Test filling a select with less values than usual");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Assert text
    checkSelectNumberOfResults("SelReq", 2);

    // Select result
    selectResult("Yes");

    // Select a date
    selectDate("CalReq", "23/10/1978");

    // Wait for loading bar
    waitForLoadingBar();

    // Write hour
    writeText("Tim", "12:23:41");

    // Write hour
    writeText("TimReq", "12:23:41");

    // Assert text
    checkSelectNumberOfResults("SelReq", 4);

    // Select result
    selectResult("General");

    // Write on criterion
    writeText("Txt", "sino");

    // Wait for loading bar
    waitForLoadingBar();

    // Assert text
    checkSelectNumberOfResults("SelReq", 2);

    // Select result
    selectResult("No");
  }

  /**
   * Test for fill over select
   */
  @Test
  void t020_checkDependenciesAfterRestore() {
    // Title
    setTestTitle("Check if dependencies are working after restore (issue #279)");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "4decimales");

    // Check that Num criterion contains JPY
    checkCriterionContents("Num", "JPY");

    // Wait for button
    waitForButton("ButRst");

    // Click on button
    clickButton("ButRst");

    // Check that Txt criterion has been restored
    checkCriterionContents("Txt", "test");

    // Check that Num criterion contains EUR
    checkCriterionContents("Num", "EUR");

    // Write on criterion
    writeText("Txt", "4decimales");

    // Check that Num criterion contains JPY
    checkCriterionContents("Num", "JPY");
  }

  /**
   * Test for select all rows of grid
   */
  @Test
  void t030_selectAllRowsOfGrid() {

    // Title
    setTestTitle("Test select all rows of multi select grid");

    // Go to screen
    gotoScreen("tools", "users");

    // Wait for button
    waitForButton("ButPrn");

    // Click to select all rows of grid
    selectAllRowsOfGrid("GrdUsrLst");

    // Wait for button
    checkAllRowsSelected("GrdUsrLst");
  }

  /**
   * Test for select all rows of grid
   */
  @Test
  void t040_suggestChangeValue() {
    // Title
    setTestTitle("Launch 2 selected actions into a suggest and check value has changed successfully");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test-left");

    // The screen loads criteria from queries that do not exist, and the client keeps its buttons blocked until the user
    // has closed the six error messages
    closeMessages("danger", 6);

    // Wait for button
    waitForButton("ButPrn");

    // Wait for button
    waitForButton("ButtonLoadSuggest");

    // Click on button
    clickButton("ButtonLoadSuggest", true);

    // Check suggest value
    checkSuggestContents("SugTst", "DjrRepPth");

    // Click on button
    clickButton("ButtonLoadSuggest2", true);

    // Check suggest value
    checkSuggestContents("SugTst", "DjrHdgPag");

    // Click on button
    clickButton("ButtonResetSuggest", true);

    // Check suggest value
    checkSuggestContents("SugTst", "");
  }

  /**
   * Test update suggest multiple with dependency
   */
  @Test
  void t050_updateSuggestMultipleWithDependency() {
    // Title
    setTestTitle("Update suggest multiple with dependency");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButPrn");

    // Write on criterion
    writeText("Txt", "suggest-multiple");

    // Wait for button
    waitForButton("ButPrn");

    // Check suggest value
    // The database of the React test application has no user "pei", so the dependency selects only the user "test"
    checkMultipleSelectorContents("SugMul", "test (test@test.com)");
  }
}
