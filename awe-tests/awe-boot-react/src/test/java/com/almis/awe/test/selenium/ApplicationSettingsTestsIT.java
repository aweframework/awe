package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Settings of the application: sequences and queues. The tests of each one create, update and delete their own record,
 * and every test opens its own screen. The themes have their own screen and test (ThemeAdminTestsIT).
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
