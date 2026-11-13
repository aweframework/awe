package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Regression tests of the test screens. Every test starts logged in with the Test module selected and opens its own screen,
 * so a failing test does not leave the next ones without a session or a screen. The login and logout flow itself is proven
 * by IntegrationTestsIT, so this class has no login or logout test of its own.
 * <p>
 * The backend rules of the keep criteria (t007), wrong login (t009) and sort by a component column (t010) tests are not
 * specific to this engine: their flows run only in the React application ({@code awe-boot-react} RegressionTestsIT).
 * <p>
 * The former t003_suggestStrict was removed: the security audit screen is redesigned without the non-strict user suggest
 * criterion it drove, and no other screen of the test application reachable through the menu declares a suggest criterion
 * with strict="false" (the remaining ones, Usr, EmlSrv and Que, are deprecated and not in the menu).
 */
@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class RegressionTestsIT extends AbstractSessionTests {

  RegressionTestsIT() {
    super(Session.TEST_MODULE);
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
    clickTab("TabSelMat", "ENUM_MATRIX_EDITABLE");

    // Check row contents
    checkRowContents("Prueba - adminflare");
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

    // Write on criterion
    writeText("Unt", "325.274,50");

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
   * Sort a grid using a component column
   */
  @Test
  void t011_suggestRepeatsValues() {
    // Title
    setTestTitle("Check that suggested values are not repeated");

    // Go to screen: the side panel of a new profile has a theme suggest that searches the themes
    gotoScreen("tools", "profiles");

    // Open the side panel
    clickButton("ButNew");

    // Click on suggest
    openSuggest("IdeThm");

    // Write the search one letter at a time, waiting for each search to finish before the next one
    writeSuggestSearch("IdeThm", "a");
    waitForLoadingBar();
    writeSuggestSearch("IdeThm", "s");
    waitForLoadingBar();
    writeSuggestSearch("IdeThm", "p");
    waitForLoadingBar();

    // Check there's only one result: the check waits until the list of the last search replaces the earlier ones
    checkSuggestResultCount(1);

    // Click selector
    selectResult("asp");

    // Close the side panel without saving
    clickButton("ButPnlCnl");
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
    // The four profiles of the test data and the predefined Reader and Contributor
    checkSelectNumberOfResults("SelReq", 6);

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
    selectAllRowsOfGrid("GrdUsrAdm");

    // Wait for button
    checkAllRowsSelected("GrdUsrAdm");
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

    // Wait for button
    waitForButton("ButPrn");

    // Wait for button
    waitForButton("ButtonLoadSuggest");

    // Click on button
    clickButton("ButtonLoadSuggest", true);

    // Check suggest value
    checkSelectContents("SugTst", "DjrRepPth");

    // Click on button
    clickButton("ButtonLoadSuggest2", true);

    // Check suggest value
    checkSelectContents("SugTst", "DjrHdgPag");

    // Click on button
    clickButton("ButtonResetSuggest", true);

    // Check suggest value
    checkSelectContents("SugTst", "");
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
    checkMultipleSelectorContents("SugMul", "pei (pei@test.com)");
    checkMultipleSelectorContents("SugMul", "test (test@test.com)");
  }

  /**
   * Pivot table renders: the pivot locale is resolved from the application language (en-GB)
   */
  @Test
  void t060_pivotTableRenders() {
    // Title
    setTestTitle("Pivot table renders: the pivot locale is resolved from the application language (en-GB)");

    // Go to screen
    gotoScreen("test", "pivot-test");

    // Check the pivot table is drawn with its loaded data
    checkPivotTableVisible("listaDatos");
  }
}
