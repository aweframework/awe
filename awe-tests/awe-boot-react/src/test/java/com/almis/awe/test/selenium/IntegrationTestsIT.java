package com.almis.awe.test.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests of the framework screens. Every test starts logged in with the Test module selected and opens its own
 * screen, so a failing test does not leave the next ones without a session or a screen. The test that stores a screen
 * configuration removes it even when it fails.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("ApplicationIntegrationIT")
class IntegrationTestsIT extends AbstractSessionTests {

  private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestsIT.class);
  private static final String SCREEN_CONFIGURATION = "screen-configuration";
  private static final String SETTINGS = "settings";
  private static final String DATABASES = "databases";
  private static final String PRINT_BUTTON = "ButPrn";

  /**
   * The test stored the screen configuration of the databases screen and did not remove it yet
   */
  private boolean screenConfigurationStored = false;

  IntegrationTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Remove the screen configuration that a test stored and left, so a failing test does not hide the button of the
   * databases screen for the next tests. It runs only after a test that failed before removing it (the flag is set before
   * the step that may store it, so the row may not exist when that step failed): a cleanup that fails is logged and does not
   * add a second failure to the one of the test.
   */
  @AfterEach
  void removeStoredScreenConfiguration() {
    if (screenConfigurationStored) {
      try {
        removeScreenConfiguration();
      } catch (RuntimeException | AssertionError exc) {
        LOG.warn("The screen configuration could not be removed after the test (it may not have been stored): {}", exc.getMessage());
      }
    }
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
   * Test screen configuration usage (hide ButPrn button on databases screen)
   */
  @Test
  void t010_screenConfigurationUsage() {
    // Title
    setTestTitle("Test screen configuration usage");

    // Go to screen
    gotoScreen(SETTINGS, SCREEN_CONFIGURATION);

    // Click button
    clickButton("ButRst");

    // Click button
    clickButton("ButGrdAdd");

    // Select on selector
    suggest("GrdScrCnf", "Scr",  "Dbs", "Dbs");

    // Select on selector
    suggest("GrdScrCnf", "IdeOpe",  "test", "test");

    // Select on selector
    suggest("GrdScrCnf", "Nam",  PRINT_BUTTON, PRINT_BUTTON);

    // Select text
    suggest("GrdScrCnf", "Atr", "visible", "Visible");

    // Select text
    writeText("GrdScrCnf", "Val", "false");

    // Scroll grid to the right
    scrollGrid("GrdScrCnf", 10000, 0);

    // Select text
    selectContain("GrdScrCnf", "Act", "Yes");

    // Save row
    saveRow();

    // Store and confirm (from here the configuration may be stored, so the cleanup after the test removes it)
    screenConfigurationStored = true;
    clickButtonAndConfirm("ButCnf");

    // Go to databases screen
    gotoScreen("tools", DATABASES);

    // Wait for button
    waitForButton("ButRst");

    // Verify that ButPrn button is not visible
    checkButtonNotVisible(PRINT_BUTTON);

    // Remove the configuration and verify that the button is visible again
    removeScreenConfiguration();

    // Go to databases screen
    gotoScreen("tools", DATABASES);

    // Click button
    waitForButton(PRINT_BUTTON);
  }

  /**
   * Select test module on select criterion
   */
  @Test
  @StartsFrom(Session.LOGGED_IN)
  void t020_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Check the menu of the module
    checkMenuOption("test", "Tests");
  }

  /**
   * Test screen modules usage (edit module to change order and check modules selector gets the proper order)
   */
  @Test
  void t030_screenModulesUsage() {
    // Title
    setTestTitle("Test screen modules usage");

    // Go to screen
    gotoScreen("tools", "modules");

    // Click on row
    clickRowContents("Test");

    // Click button
    clickButton("ButUpd");

    // Wait for button
    waitForButton("ButCnf");

    // Select text
    writeText("Ord", "0");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    waitForButton("ButRst");

    checkLogoutWithConfirmation();

    checkLogin("test", "test", "Manager (test)");

    // Verify that ButPrn button is not visible
    checkMenuOption("test", "Tests");

  }

  /**
   * Grid test: Having query
   */
  @Test
  void t040_gridTestHaving() {
    // Title
    setTestTitle("Grid test: Having query");

    // Go to screen
    gotoScreen("test", "matrix", "matrix-test-having");

    // Search and wait
      searchAndWait("ButGrdAccept");

    // Check row contents
    checkRowContents("Spain");
  }

  /**
   * Grid test: cells with markup are shown as text or sanitized, and nothing in them runs
   */
  @Test
  void t045_gridCellsWithMarkup() {
    // Title
    setTestTitle("Grid test: cells with markup");

    // Go to screen (the grid loads a row whose cells carry images, links, scripts, handlers and frames; the texts of the
    // cells are not the labels of the column headers, so a header cannot satisfy the checks)
    gotoScreen("test", "matrix", "matrix-test-xss-cells");

    // Check that the text around the markup that is removed is still shown
    checkRowContentsGrid("GrdXssCells", "Alpha", "omega", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");

    // Check that the safe markup is kept (its text is shown)
    checkRowContentsGrid("GrdXssCells", "Golf", "Hotel");

    // Check that a value that the server escaped is shown as text, tags included
    checkRowContentsGrid("GrdXssCells", "<b>India</b> text");

    // Check that no cell holds a script, frame, image, handler or script link
    checkGridCellsHaveNoActiveContent("GrdXssCells");
  }

  /**
   * Chart test
   */
  @Test
  void t050_chartTest() {
    // Title
    setTestTitle("Chart test");

    // Go to screen
    gotoScreen("test", "chart", "chart-test");

    // Check visible
    checkChartVisible("ChrLinTst");

    // Check visible
    checkChartVisible("ChrBarTst");

    // Check visible
    checkChartVisible("ChrAreTst");

    // Check visible
    checkChartVisible("ChrPieTst");

    // Check visible
    checkChartVisible("ChrDonutTst");

    // Check visible
    checkChartVisible("ChrStockTst");

    // Check visible
    checkChartVisible("ChrBarHorTst");

    // Check visible
    checkChartVisible("ChrSemiCircleTst");
  }

  /**
   * Chart test with advanced Highcharts options
   */
  @Test
  void t051_advancedChartTest() {
    // Title
    setTestTitle("Advanced chart test");

    // Go to screen
    gotoScreen("test", "chart", "chart-advanced-test");

    // Pyramid with an html tooltip, gradient area, bubbles, rounded stacked columns and pies
    checkChartVisible("ChrAdvPyramid");
    checkChartVisible("ChrAdvArea");
    checkChartVisible("ChrAdvBubble");
    checkChartVisible("ChrAdvColumns");
    checkChartVisible("ChrAdvPie3d");
    checkChartVisible("ChrAdvPie");
  }

  /**
   * Wizard test
   */
  @Test
  void t060_wizardTest() {
    // Title
    setTestTitle("Wizard test");

    // Go to screen
    gotoScreen("test", "wizard-test");

    // Check visibility
    checkActiveWizardStep("1");

    // Check visibility
    checkTagListContains("wizard-tag-list-1", "Manager (test)");

    // Check visibility
    checkTagListContains("wizard-tag-list-2", "MANAGER (TEST)");

    // Write text
    writeText("epa", "aaa");

    // Write text
    writeText("tutu", "bbb");

    // Click button
    clickButton("FwStep2");

    // Check visibility and content
    checkActiveWizardStep("2");

    // Write text
    writeText("lala", "aaa");

    // Write text
    writeText("prueba", "bbb");

    // Write text
    writeText("pwd_usr", "ccc");

    // Click button
    clickButton("FwStep3");

    // Check visibility and content
    checkActiveWizardStep("3");

    // Write text
    writeText("epa12", "aaa");

    // Write text
    writeText("tutu12", "bbb");

    // Click button
    clickButton("FwStep4");

    // Check visibility and content
    checkActiveWizardStep("4");

    // Write text
    writeText("epa121", "aaa");

    // Write text
    writeText("tutu121", "bbb");

    // Click button
    clickButton("Finish", true);
  }

  /**
   * SQL extractor engine test
   */
  @Test
  void t070_sqlExtractorEngine() {
    // Title
    setTestTitle("SQL extractor engine test");

    // Go to screen
    gotoScreen("tools", "sqlExtractor");

    // Write text on criteria
    writeText("selectCriteria", "select * from awekey");

    // Search and wait
    searchAndWait();

    // Check visible
    checkRowContents("OpeKey");
  }

  /**
   * File Manager test
   */
  @Test
  void t080_fileManager() {
    // Title
    setTestTitle("File Manager test");

    // Go to screen
    gotoScreen("test", "filemanager-test");

    // Check the content of the embedded application (its selector is not an AWE one)
    checkTextInEmbeddedFrame("ol.breadcrumb a", "angular-filemanager");
  }

  /**
   * Remove the screen configuration of the databases screen for the test user that t010 stores
   */
  private void removeScreenConfiguration() {
    // Whatever happens next, the cleanup after the test must not try it again
    screenConfigurationStored = false;

    // Go to screen
    gotoScreen(SETTINGS, SCREEN_CONFIGURATION);

    // Click button
    clickButton("ButRst");

    // Select on selector
    suggest("CrtScr",  "Dbs", "Dbs");

    // Select on selector
    suggest("CrtUsr",  "test", "test");

    // Select text
    selectContain("CrtAct", "Yes");

    // Search and wait
    searchAndWait();

    // Click on row
    clickRowContents("Dbs");

    // Click on delete button
    clickButton("ButGrdDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");
  }
}
