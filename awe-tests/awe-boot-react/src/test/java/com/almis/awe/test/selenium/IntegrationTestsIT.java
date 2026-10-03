package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("ApplicationIntegrationIT")
class IntegrationTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   */
  @Test
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
    gotoScreen("settings", "screen-configuration");

    // Click button
    clickButton("ButRst");

    // Click button
    clickButton("ButGrdAdd");

    // Select on selector
    suggest("GrdScrCnf", "Scr",  "Dbs", "Dbs");

    // Select on selector
    suggest("GrdScrCnf", "IdeOpe",  "test", "test");

    // Select on selector
    suggest("GrdScrCnf", "Nam",  "ButPrn", "ButPrn");

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

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Go to databases screen
    gotoScreen("tools", "databases");

    // Wait for button
    waitForButton("ButRst");

    // Verify that ButPrn button is not visible
    checkButtonNotVisible("ButPrn");

    // Go to screen
    gotoScreen("settings", "screen-configuration");

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

    // Go to databases screen
    gotoScreen("tools", "databases");

    // Click button
    waitForButton("ButPrn");
  }

  /**
   * Select test module on select criterion
   */
  @Test
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
}
