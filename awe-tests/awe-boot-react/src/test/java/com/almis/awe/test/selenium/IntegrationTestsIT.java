package com.almis.awe.test.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  private static final String DATABASES_SCREEN = "database-admin";
  private static final String PRINT_BUTTON = "ButPrn";
  private static final String XSS_CELLS_GRID = "GrdXssCells";

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
    checkLogin("test", "test", "Manager");
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
    suggest("GrdScrCnf", "Scr",  DATABASES_SCREEN, DATABASES_SCREEN);

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
    waitForButton("ButNew");

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

    // Search the module and open it in the side panel with the pencil of its row
    searchGrid("CrtModTxt", "Test");
    clickGridRowButton("GrdModAdm", "Test", "EdtBtn");

    // Select text
    writeText("Ord", "0");

    // Store and confirm
    clickButtonAndConfirm("ButPnlUpd");

    checkLogoutWithConfirmation();

    checkLogin("test", "test", "Manager");

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
    checkRowContentsGrid(XSS_CELLS_GRID, "Alpha", "omega", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");

    // Check that the safe markup is kept (its text is shown)
    checkRowContentsGrid(XSS_CELLS_GRID, "Golf", "Hotel");

    // Check that a value that the server escaped is shown as text, tags included
    checkRowContentsGrid(XSS_CELLS_GRID, "<b>India</b> text");

    // Check that the formatted text column keeps the safe markup of its html label
    checkRowContentsGrid(XSS_CELLS_GRID, "Juliett", "Kilo");

    // Check that no cell holds a script, frame, image, handler or script link
    checkGridCellsHaveNoActiveContent(XSS_CELLS_GRID);
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
   * Charts linked by zoom: zooming one of them zooms the other one through its dependency on xMin and xMax
   */
  @Test
  void t052_linkedChartZoomTest() {
    // Title
    setTestTitle("Linked chart zoom test");

    // Go to screen
    gotoScreen("test", "chart", "chart-test");
    checkChartVisible("ChrLinTst");
    checkChartVisible("ChrStockTst");

    // Zoom the line chart: the stock chart follows it
    zoomChart("ChrLinTst");
    checkChartZoomed("ChrLinTst");
    checkChartZoomed("ChrStockTst");
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
   * Reveal the first invalid field: the validation opens the tab, the accordion item or the wizard step that holds it,
   * focuses it and marks the tabs and steps that hold fields with errors
   */
  @Test
  void t065_revealInvalidField() {
    // Title
    setTestTitle("Reveal invalid field: Test to reveal the invalid fields of tabs, accordions and wizards");

    // Go to screen
    gotoScreen("test", "reveal-invalid-test");

    // Wait for button
    waitForButton("ButRevealValidate");

    // Validate with the three required fields empty: the second tab opens, its field is focused and the tab is marked
    clickButton("ButRevealValidate");
    waitForInputActionability("RevTabRequired");
    checkCriterionFocused("RevTabRequired");
    checkTabInvalid("RevTabs", true);

    // The step of the wizard that holds an empty required field is marked too
    checkWizardStepInvalid("RevWizard", true);

    // Fill the field of the tab and validate again: the tab is not marked anymore and the accordion item opens
    writeText("RevTabRequired", "tab");
    clickButton("ButRevealValidate");
    waitForInputActionability("RevAccRequired");
    checkCriterionFocused("RevAccRequired");
    checkTabInvalid("RevTabs", false);

    // Fill the field of the accordion item and validate again: the second step of the wizard opens
    writeText("RevAccRequired", "accordion");
    clickButton("ButRevealValidate");
    waitForInputActionability("RevStepRequired");
    checkCriterionFocused("RevStepRequired");
    checkWizardStepInvalid("RevWizard", true);

    // Fill the last required field and validate: there is nothing left to reveal and nothing is marked
    writeText("RevStepRequired", "step");
    clickButton("ButRevealValidate");
    checkTabInvalid("RevTabs", false);
    checkWizardStepInvalid("RevWizard", false);
  }

  /**
   * Unsaved grid rows: an action blocked by check-records-saved opens the tab that hides the grid with the row that is
   * still being edited, marks the tab while the row is not saved and lets the action run once the row is saved
   */
  @Test
  void t066_revealUnsavedGridRow() {
    // Title
    setTestTitle("Unsaved grid rows: an action blocked by check-records-saved opens the tab of the grid and marks it");

    // Go to screen
    gotoScreen("test", "unsaved-grid-test");

    // Open the side panel with the three tabs and their editable grids
    waitForButton("ButUnsOpen");
    clickButton("ButUnsOpen");
    waitForButton("ButUnsSave");

    // Add a row in the grid of the second tab: the tab is marked while the row is not saved
    clickTab("UnsTabs", "Access");
    waitForButton("ButUnsAccessAdd");
    clickButton("ButUnsAccessAdd");
    checkTabInvalid("UnsTabs", true);

    // The panel is on another tab, so the grid that holds the unsaved row is hidden
    clickTab("UnsTabs", "General");
    checkTabActive("UnsTabs", "General");

    // Save: the action is blocked and the tab with the grid opens again
    clickButton("ButUnsSave");
    checkTabActive("UnsTabs", "Access");
    checkTabInvalid("UnsTabs", true);
    assertEquals("", getText("UnsResult"));

    // Save the row: the tab is not marked anymore and the same action runs
    clickButton("ButUnsSaveRow");
    checkTabInvalid("UnsTabs", false);
    clickButton("ButUnsSave");
    assertEquals("saved", getText("UnsResult"));

    // Close the panel, so the next test opens its screen with nothing over it
    clickButton("ButUnsClose");
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
   * File viewer test: the widget shows the text that the test endpoint answers with the created status
   */
  @Test
  void t081_fileViewer() {
    // Title
    setTestTitle("File viewer test");

    // Go to screen
    gotoScreen("test", "file-viewer-test");

    // The widget reads the file on load
    checkFileViewerContains("FILE VIEWER TEST LINE 1");
    checkFileViewerContains("FILE VIEWER TEST LINE 3");
  }

  /**
   * Launcher pages: the cards show the description of the options, a launcher card opens another launcher (3 levels) and
   * the breadcrumb follows the levels. The option without description renders as before (card without description line)
   */
  @Test
  void t085_launcherPages() {
    // Title
    setTestTitle("Launcher pages with descriptions and nested launchers");

    // First level: the card of the nested launcher shows its description, the plain option has none
    gotoScreen("test", "launcher-menu");
    checkLauncherCardDescription("launcher-sales", "Orders and sales reports");
    checkButtonVisible("launcher-plain");
    checkLauncherCardWithoutDescription("launcher-plain");

    // Second level
    clickButton("launcher-sales", true);
    checkLauncherCardDescription("launcher-reports", "Monthly and yearly sales");
    checkLauncherCardDescription("launcher-orders", "Look up the orders of a customer");
    checkBreadcrumbContains("Launcher");

    // Third level
    clickButton("launcher-reports", true);
    checkLauncherCardDescription("launcher-monthly", "Sales grouped by month");
    checkButtonVisible("launcher-yearly");
    checkLauncherCardWithoutDescription("launcher-yearly");
    checkBreadcrumbContains("Sales");

    // Open a screen from the last launcher
    clickButton("launcher-monthly", true);
    checkBreadcrumbContains("Reports");
  }

  /**
   * Navigable breadcrumb: the page the menu path leads to is the current page and the level of its launcher goes back to
   * it, opening the launcher like the menu does
   */
  @Test
  void t086_navigableBreadcrumb() {
    // Title
    setTestTitle("Navigable breadcrumb of a nested menu option");

    // The launcher is a menu screen (a leaf of the side menu): its nested options open from its cards
    gotoScreen("test", "launcher-menu");
    clickButton("launcher-sales", true);
    clickButton("launcher-reports", true);
    clickButton("launcher-monthly", true);

    // The last level is the current page of the breadcrumb
    checkBreadcrumbCurrentPage("Monthly report");

    // The level of the launcher opens it again, with the card of the page the user came from
    clickBreadcrumbLevel("launcher-reports");
    checkLauncherCardDescription("launcher-monthly", "Sales grouped by month");
    checkBreadcrumbCurrentPage("Reports");
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
    suggest("CrtScr",  DATABASES_SCREEN, DATABASES_SCREEN);

    // Select on selector
    suggest("CrtUsr",  "test", "test");

    // Select text
    selectContain("CrtAct", "Yes");

    // Search and wait
    searchAndWait();

    // Click on row
    clickRowContents(DATABASES_SCREEN);

    // Click on delete button
    clickButton("ButGrdDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");
  }
}
