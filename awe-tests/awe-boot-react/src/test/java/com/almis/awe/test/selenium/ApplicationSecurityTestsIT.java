package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Security of the application: the access window, screen configuration and encryption tools. Every test opens its own
 * screen and leaves no data behind: the access window removes the rule it adds, and the tests of the screen
 * configuration create, update and delete their own record.
 *
 * <p>The access window (access-admin, #791) has one flow: the cascade, its reasons, the filters, the counts and the
 * changes are covered by the unit and Spring tests of the access services.</p>
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationSecurityTestsIT extends AbstractApplicationTests {

  // A screen of the menu with a print button: the screen suggest lists only the screens of the menu
  private static final String DATABASES_SCREEN = "database-admin";
  // Access window
  private static final String ACCESS_GRID = "GrdAcc";
  private static final String WHY_PANEL = "PnlAccWhy";
  private static final String WHY_GRID = "GrdAccWhy";
  private static final String RULES_GRID = "GrdAccRul";
  // The step of the cascade of the profile rules: module, base profile, global rule, profile rule, user rule, inheritance
  private static final String PROFILE_STEP = "4";
  // A root option of the menu that the profile OPE can open: no rule and no restriction of its base profile file
  private static final String PROFILE = "OPE";
  private static final String OPTION = "help";
  private static final String ACCESS = "access-text";

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
    // The application asks for a confirmation before logging out
    checkLogoutWithConfirmation();
  }

  /**
   * Access window: choose the profile OPE, deny an option from its row (Denied, decided by the rule of the profile), open
   * Why to see the layer that decides, and remove the rule from the panel, which gives the access back
   */
  @Test
  void t021_denyAnOptionToAProfileAndRemoveTheRuleFromWhy() {
    setTestTitle("Deny an option to a profile in the access window, see why and remove the rule");

    gotoScreen("settings", "security", "screen-access");
    clickCheckboxOption("subject-type", "profile");
    suggest("IdePro", PROFILE, PROFILE);

    clickGridCellButton(ACCESS_GRID, OPTION, "DnyBtn");
    checkAndCloseMessage("success");
    checkCellContents(ACCESS_GRID, OPTION, ACCESS, "Denied");
    checkCellContents(ACCESS_GRID, OPTION, "reason", "Rule of profile " + PROFILE);

    // The panel shows the option and its access, and the profile rule is the layer that decides
    clickGridCellButton(ACCESS_GRID, OPTION, "WhyBtn");
    checkSidePanelTitle(WHY_PANEL, "Help");
    checkSidePanelTag(WHY_PANEL, "Denied");
    checkCellContents(WHY_GRID, PROFILE_STEP, "title", "Rule of profile " + PROFILE + " on the option");
    checkCellContents(WHY_GRID, PROFILE_STEP, "decides-text", "Decides");

    clickGridRowButton(RULES_GRID, PROFILE, "RmvBtn");
    acceptConfirm();
    checkAndCloseMessage("success");
    checkRowGone(RULES_GRID, PROFILE);
    checkSidePanelTag(WHY_PANEL, "Allowed");

    closeSidePanel(WHY_PANEL);
    checkCellContents(ACCESS_GRID, OPTION, ACCESS, "Allowed");
  }

  /**
   * Add a new screen configuration
   */
  @Test
  void t051_newScreenConfiguration() {
    // Title
    setTestTitle("Add a new screen configuration");

    // Go to screen
    gotoScreen("settings", "screen-configuration");

    // Wait for button
    clickButton("ButRst");

    // Wait for button
    clickButton("ButGrdAdd");

    // Select on selector
    suggest("GrdScrCnf", "Scr",  DATABASES_SCREEN, DATABASES_SCREEN);

    // Select on selector
    suggest("GrdScrCnf", "IdeOpe",  "test", "test");

    // Select on selector
    suggest("GrdScrCnf", "Nam",  "ButPrn", "ButPrn");

    // Select text
    suggest("GrdScrCnf", "Atr", "visible", "Visible");

    // Select text
    writeText("GrdScrCnf", "Val", "true");

    // Scroll grid to the right
    scrollGrid("GrdScrCnf", 10000, 0);

    // Select text
    selectContain("GrdScrCnf", "Act", "Yes");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtScr", DATABASES_SCREEN, DATABASES_SCREEN);

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents(DATABASES_SCREEN, "test", "ButPrn", "Visible", "true");
  }

  /**
   * Update a screen configuration
   */
  @Test
  void t053_updateScreenConfiguration() {
    // Title
    setTestTitle("Update a screen configuration");

    // Go to screen
    gotoScreen("settings", "screen-configuration");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtScr", DATABASES_SCREEN, DATABASES_SCREEN);

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Click on row
    editRow(DATABASES_SCREEN);

    // Select on selector
    writeText("GrdScrCnf", "Val",  "false");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtScr", DATABASES_SCREEN, DATABASES_SCREEN);

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents(DATABASES_SCREEN, "test", "ButPrn", "Visible", "false");
  }

  /**
   * Delete a screen configuration
   */
  @Test
  void t055_deleteScreenConfiguration() {
    // Title
    setTestTitle("Delete a screen configuration");

    // Go to screen
    gotoScreen("settings", "screen-configuration");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtScr", DATABASES_SCREEN, DATABASES_SCREEN);

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(DATABASES_SCREEN);

    // Click on delete button
    clickButton("ButGrdDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyDeleted(DATABASES_SCREEN);
  }

  /**
   * Encrypt text with encryption tools
   */
  @Test
  void t071_encryptText() {
    // Title
    setTestTitle("Encrypt text with encryption tools");

    // Go to log screen
    gotoScreen("settings", "security", "encrypt-tools");

    // Wait for reset button
    waitForButton("ButRst");

    // Write on criterion
    writeText("CrtTxt", "Texto de prueba");

    // Wait for reset button
    clickButton("ButEnc", true);

    // Wait for reset button
    waitForButton("ButRst");

    // Check criterion contents
    checkCriterionContents("CrtEnc", "dOakAf2lwfqAke4O41A0Ww==");
  }
}
