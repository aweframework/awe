package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Security of the application: screen configuration and encryption tools. The tests of the screen configuration create,
 * update and delete their own record, and every test opens its own screen. The access window (access-admin, #791) has its
 * browser flow in the React application only: its rules are covered by the unit and Spring tests of the access services
 * and its engine pieces by the Jest tests of each client.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationSecurityTestsIT extends AbstractApplicationTests {

  // The screen of the databases option, whose print button the screen configuration tests configure
  private static final String DATABASES_SCREEN = "database-admin";

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
