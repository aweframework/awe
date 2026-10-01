package com.almis.awe.test.selenium;

import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;
import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;

@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class RegressionTestsIT extends SeleniumUtilities {

  /** Day of the open datepicker that can be picked */
  private static final String ENABLED_DATEPICKER_DAY = TestIds.css(TestIds.DATEPICKER_DAY)
    + TestAttributes.css(TestAttributes.DISABLED, false);

  /** Search input of the open select dropdown */
  private static final String OPEN_SELECT_SEARCH = TestIds.css(TestIds.SELECT_DROPDOWN) + " " + TestIds.css(TestIds.SELECT_SEARCH);

  /** Options of the open select dropdown */
  private static final String SELECT_OPTIONS_XPATH = "//*[" + TestIds.xpath(TestIds.SELECT_DROPDOWN) + "]//*["
    + TestIds.xpath(TestIds.SELECT_OPTION) + "]";

  /** Title and message of the warning alert */
  private static final String WARNING_ALERT = TestIds.css(TestIds.ALERT) + TestAttributes.css(TestAttributes.TYPE, "warning");
  private static final String WARNING_ALERT_TITLE = WARNING_ALERT + " " + TestIds.css(TestIds.ALERT_TITLE);
  private static final String WARNING_ALERT_MESSAGE = WARNING_ALERT + " " + TestIds.css(TestIds.ALERT_MESSAGE);

  /**
   * Log into the application
   */
  @Test
  void t000_loginTest() {
    checkLogin("test", "test", "#ButUsrAct span.avatar-text", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout(".slogan", "Almis Web Engine");
  }

  /**
   * Select test module on select criterion
   */
  @Test
  void t001_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectTestModule();
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
    checkSelectContents("CrtUsr", "te");
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

    // Write on criterion
    writeText("Tar", "\"");

    // Click on checkbox
    clickCheckbox("RadBox3");

    // Wait for text
    checkText("[criterion-id='Unt'] .unit", "USD");
  }

  /**
   * Check filtered date dependency (#31141)
   */
  @Test
  void t006_checkFilteredDateDependency() {
    // Title
    setTestTitle("Check filtered date dependency (#31141)");

    // Write on criterion
    writeText("Txt", "edita");

    // Check text
    checkCriterionContents("Txt", "edita");

    // Click on date
    clickDate("FilCalRea");

    // Click on selector
    click(ENABLED_DATEPICKER_DAY);
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
    checkSelectContents("CrtUsr", "test");
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
    click("[criterion-id='Sug'] " + TestIds.css(TestIds.SELECT));
    suggestDelayed(OPEN_SELECT_SEARCH, "tee", "test", "test", 800);

    // Suggest delayed
    suggestDelayed("[criterion-id='SugMulReq'] " + TestIds.css(TestIds.SELECT_SEARCH), "tee", "test", "test", 800);

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
    checkLogout(".slogan", "Almis Web Engine");

    // Check wrong login
    checkLogin("test", "lala", WARNING_ALERT_MESSAGE, "The credentials entered for the user -test- are not valid");
    checkText(WARNING_ALERT_TITLE, "Invalid credentials");

    // Check wrong login
    checkLogin("tutu", "lala", WARNING_ALERT_MESSAGE, "Username -tutu- is wrong or inactive");
    checkText(WARNING_ALERT_TITLE, "Wrong username");

    // Do right login
    checkLogin("test", "test", "#ButUsrAct span.avatar-text", "Manager (test)");
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
    click("[criterion-id='CrtNam'] " + TestIds.css(TestIds.SELECT));

    // Pause
    pause(1000);

    // Write text
    writeText(By.cssSelector(OPEN_SELECT_SEARCH), "a");

    // Pause
    pause(1000);

    // Write text
    writeText(By.cssSelector(OPEN_SELECT_SEARCH), "s");

    // Pause
    pause(1000);

    // Write text
    writeText(By.cssSelector(OPEN_SELECT_SEARCH), "p");

    // Pause
    pause(1000);

    // Check there's one result
    checkVisible(By.xpath("(" + SELECT_OPTIONS_XPATH + ")[1]"));

    // Check there's only one result
    checkNotVisible(By.xpath("(" + SELECT_OPTIONS_XPATH + ")[2]"));

    // Click selector
    selectResult("asp");
  }

  /**
   * Test for fill over select
   */
  @Test
  void t012_fillOverSelect() {
    selectTestModule();

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
    selectTestModule();

    // Title
    setTestTitle("Check if dependencies are working after restore (issue #279)");

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
    checkPresence(TestIds.css(TestIds.GRID) + " [id='scope-GrdUsrLst'] " + TestIds.css(TestIds.GRID_HEADER_CHECKBOX)
      + TestAttributes.css(TestAttributes.SELECTED, true));
  }

  /**
   * Test for select all rows of grid
   */
  @Test
  void t040_suggestChangeValue() {
    selectTestModule();

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
    selectTestModule();

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
   * Suggest delayed
   *
   * @param searchInput Selector of the search input
   * @param search1  Search on first case
   * @param search2  Search on second case
   * @param match    Match result
   * @param pause    Pause
   */
  private void suggestDelayed(String searchInput, String search1, String search2, String match, Integer pause) {
    // Write text
    writeText(By.cssSelector(searchInput), search1);

    // Pause
    pause(pause);

    // Clear text
    clearText(searchInput);

    // Write select
    writeTextOnDriver(By.cssSelector(searchInput), search2);

    // Click selector
    selectResult(match);
  }

  /**
   * Select test module
   */
  private void selectTestModule() {
    // Select module
    selectModule("Test");

    // Wait for text
    waitForText("mm-text", "Tests");

    // Check text
    checkVisible("[translate-multiple='MENU_TEST'");
  }
}
