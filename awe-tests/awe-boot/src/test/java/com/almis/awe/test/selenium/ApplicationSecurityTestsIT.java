package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Security of the application: screen restrictions, screen configuration and encryption tools. The tests of
 * each one create, update and delete their own record, and every test opens its own screen.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationSecurityTestsIT extends AbstractApplicationTests {

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
    checkLogout();
  }

  /**
   * Add a new screen restriction
   */
  @Test
  void t021_newRestriction() {
    // Title
    setTestTitle("Add a new screen restriction");

    // Go to screen
    gotoScreen("settings", "security", "screen-access");

    // Wait for button
    clickButton("ButRst");

    // Wait for button
    clickButton("ButGrdAdd");

    // Select on selector
    suggest("GrdScrAccLst", "IdeOpe",  "test", "test");

    // Select on selector
    suggest("GrdScrAccLst", "Opt",  "application-info", "application-info");

    // Select text
    selectContain("GrdScrAccLst", "AccMod", "Restricted");

    // Select text
    selectContain("GrdScrAccLst", "Act", "Yes");

    // Save row
    saveRow();

    // Check row contents
    checkRowContents("application-info", "Restricted", "Yes");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggestMultiple("CrtOpe", "test", "test");

    // Suggest
    suggestMultiple("CrtOpc", "application-info", "application-info");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("application-info");
  }

  /**
   * Update a screen restriction
   */
  @Test
  void t023_updateRestriction() {
    // Title
    setTestTitle("Update a screen restriction");

    // Go to screen
    gotoScreen("settings", "security", "screen-access");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggestMultiple("CrtOpe", "test", "test");

    // Suggest
    suggestMultiple("CrtOpc", "application-info", "application-info");

    // Wait for button
    searchAndWait();

    // Edit row
    editRow("application-info");

    // Select on selector
    selectContain("GrdScrAccLst", "AccMod",  "Restricted");

    // Select on selector
    selectContain("GrdScrAccLst", "Act",  "No");

    // Save row
    saveRow();

    // Check row contents
    checkRowContents("Restricted", "No");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggestMultiple("CrtOpe", "test", "test");

    // Suggest
    suggestMultiple("CrtOpc", "application-info", "application-info");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("application-info", "Restricted", "No");
  }

  /**
   * Delete a screen restriction
   */
  @Test
  void t025_deleteRestriction() {
    // Title
    setTestTitle("Delete a screen restriction");

    // Go to screen
    gotoScreen("settings", "security", "screen-access");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggestMultiple("CrtOpe", "test", "test");

    // Suggest
    suggestMultiple("CrtOpc", "application-info", "application-info");

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents("application-info");

    // Click on delete button
    clickButton("ButGrdDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyDeleted("application-info");
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
    suggest("GrdScrCnf", "Scr",  "Dbs", "Dbs");

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
    suggest("CrtScr", "Dbs", "Dbs");

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("Dbs", "test", "ButPrn", "Visible", "true");
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
    suggest("CrtScr", "Dbs", "Dbs");

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Click on row
    editRow("Dbs");

    // Select on selector
    writeText("GrdScrCnf", "Val",  "false");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtScr", "Dbs", "Dbs");

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("Dbs", "test", "ButPrn", "Visible", "false");
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
    suggest("CrtScr", "Dbs", "Dbs");

    // Suggest
    suggest("CrtUsr", "test", "test");

    // Select
    selectContain("CrtAct", "Yes");

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents("Dbs");

    // Click on delete button
    clickButton("ButGrdDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyDeleted("Dbs");
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
