package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Settings of the application: themes, sequences and queues. The tests of each one create, update and
 * delete their own record, and every test opens its own screen.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationSettingsTestsIT extends AbstractApplicationTests {

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
   * Click on confirm button, accept confirmation and accept message
   */
  void verifyView(String suggest, String search) {
    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Click on button
    clickButton("ButViw", true);

    // Wait for button
    waitForButton("ButBck");
  }

  /**
   * Add a new theme
   */
  @Test
  void t001_newTheme() {
    // Title
    setTestTitle("Add a new theme");

    addNew("tools", "themes");

    // Insert text
    writeText("Nam", "Theme test");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyView("CrtNam", "test");

    // Check contents
    checkCriterionContents("Nam", "Theme test");
  }

  /**
   * Update a theme
   */
  @Test
  void t003_updateTheme() {
    // Title
    setTestTitle("Update a theme");

    // Go to update
    update("CrtNam", "test", "tools", "themes");

    // Insert text
    writeText("Nam", "Theme changed");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyView("CrtNam", "Theme changed");

    // Check contents
    checkCriterionContents("Nam", "Theme changed");
  }

  /**
   * Delete a theme
   */
  @Test
  void t005_deleteTheme() {
    // Title
    setTestTitle("Delete a theme");

    // Delete a theme
    delete("CrtNam", "Theme changed", "tools", "themes");

    // Verify
    verifyDeleted("Theme changed");
  }

  /**
   * Add a new sequence
   */
  @Test
  void t011_newSequence() {
    // Title
    setTestTitle("Add a new sequence");

    // Go to screen
    gotoScreen("tools", "sequences");

    // Wait for button
    clickButton("ButRst");

    // Wait for button
    clickButton("ButGrdKeyLstAdd");

    // Insert text
    writeText("GrdKeyLst", "KeyNam", "testKey");

    // Insert text
    writeText("GrdKeyLst", "KeyVal", "0");

    // Select on selector
    selectContain("GrdKeyLst", "Act",  "Yes");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtKeyNam", "test", "test");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("testKey");
  }

  /**
   * Update a sequence
   */
  @Test
  void t013_updateSequence() {
    // Title
    setTestTitle("Update a sequence");

    // Go to screen
    gotoScreen("tools", "sequences");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtKeyNam", "test", "test");

    // Wait for button
    searchAndWait();

    // Click on row
    editRow("testKey");

    // Insert text
    writeText("GrdKeyLst", "KeyVal", "1");

    // Select on selector
    selectContain("GrdKeyLst", "Act",  "No");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest
    suggest("CrtKeyNam", "test", "test");

    // Wait for button
    searchAndWait();

    // Check contents
    checkRowContents("testKey", "1", "No");
  }

  /**
   * Delete a sequence
   */
  @Test
  void t015_deleteSequence() {
    // Title
    setTestTitle("Delete a sequence");

    // Go to screen
    gotoScreen("tools", "sequences");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtKeyNam", "test", "test");

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents("test");

    // Click on delete button
    clickButton("ButGrdKeyLstDel");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyDeleted("test");
  }

  /**
   * Add a new queue
   */
  @Test
  void t061_newQueue() {
    // Title
    setTestTitle("Add a new queue");

    addNew("tools", "queues");

    // Insert text
    writeText("Als", "Queue test");

    // Insert text
    writeText("JmsBrk", "Broker");

    // Insert text
    writeText("DstNam", "Destination");

    // Select on selector
    selectContain("ConTyp", "JNDI");

    // Insert text
    writeText("Des", "Queue description");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtAls", "test", "test");

    // Search on grid
    searchAndWait();

    // Check contents
    checkRowContents("Queue test", "Broker", "Destination", "JNDI", "Queue description");
  }

  /**
   * Update a theme
   */
  @Test
  void t063_updateQueue() {
    // Title
    setTestTitle("Update a queue");

    // Go to update
    update("CrtAls", "test", "tools", "queues");

    // Insert text
    writeText("Als", "Queue changed");

    // Insert text
    writeText("JmsBrk", "New broker");

    // Insert text
    writeText("DstNam", "New destination");

    // Insert text
    writeText("Des", "Queue changed description");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtAls", "changed", "changed");

    // Search on grid
    searchAndWait();

    // Check contents
    checkRowContents("Queue changed", "New broker", "New destination", "JNDI", "Queue changed description");
  }

  /**
   * Delete a queue
   */
  @Test
  void t065_deleteQueue() {
    // Title
    setTestTitle("Delete a queue");

    // Delete a theme
    delete("CrtAls", "Queue changed", "tools", "queues");

    // Verify
    verifyDeleted("Queue changed");
  }
}
