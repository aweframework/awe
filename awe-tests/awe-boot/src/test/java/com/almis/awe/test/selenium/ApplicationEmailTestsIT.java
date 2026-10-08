package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Email servers and the log viewer. The tests of each email server (with and without authentication) create,
 * update and delete a server of their own name, and every test opens its own screen.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationEmailTestsIT extends AbstractApplicationTests {

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
   * Verify an email server exists
   *
   * @param search Text to search in the name of the email server
   * @param name   Name of the email server
   * @param host   Host of the email server
   */
  private void verifyEmailServer(String search, String name, String host) {
    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtNam", search, search);

    // Search on grid
    searchAndWait();

    // Check row contents
    checkRowContents(name, host);
  }

  /**
   * Verify an email server has been updated
   *
   * @param name Name of the email server
   */
  private void verifyUpdatedEmailServer(String name) {
    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtNam", name, name);

    // Search on grid
    searchAndWait();

    // Check row contents
    checkRowContents(name);
  }

  /**
   * Add a new email server
   */
  @Test
  void t031_newEmailServer() {
    // Title
    setTestTitle("Add a new email server");

    // Go for new screen
    addNew("tools", "email-servers");

    // Insert text
    writeText("SrvNam", "auth server");

    // Insert text
    writeText("Hst", "localhost");

    // Check box
    clickCheckbox("Ath");

    // Insert text
    writeText("EmlUsr", "test");

    // Insert text
    writeText("EmlPwd", "test");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify new email server
    verifyEmailServer("auth", "auth server", "localhost");
  }

  /**
   * Update an email server
   */
  @Test
  void t033_updateEmailServer() {
    // Title
    setTestTitle("Update an email server");

    // Go to update
    update("CrtNam", "auth", "tools", "email-servers");

    // Insert text
    writeText("SrvNam", "auth updated");

    // Insert text
    writeText("EmlUsr", "test2");

    // Insert text
    writeText("EmlPwd", "test2");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify the updated email server
    verifyUpdatedEmailServer("auth updated");

    // Check row contents
    checkRowContents("test2");
  }

  /**
   * Delete an email server
   */
  @Test
  void t035_deleteEmailServer() {
    // Title
    setTestTitle("Delete an email server");

    // Delete email server
    delete("CrtNam", "auth updated", "tools", "email-servers");

    // Verify deleted
    verifyDeleted("auth updated");
  }

  /**
   * Add a new email server without authentication
   */
  @Test
  void t041_newEmailServerNoAuth() {
    // Title
    setTestTitle("Add a new email server without authentication");

    // Go for new screen
    addNew("tools", "email-servers");

    // Insert text
    writeText("SrvNam", "plain server");

    // Insert text
    writeText("Hst", "localhost");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify new email server
    verifyEmailServer("plain", "plain server", "localhost");
  }

  /**
   * Update an email server without authentication
   */
  @Test
  void t043_updateEmailServerNoAuth() {
    // Title
    setTestTitle("Update an email server without authentication");

    // Go to update
    update("CrtNam", "plain", "tools", "email-servers");

    // Insert text
    writeText("SrvNam", "plain updated");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Verify
    verifyUpdatedEmailServer("plain updated");
  }

  /**
   * Delete an email server without authentication
   */
  @Test
  void t045_deleteEmailServerNoAuth() {
    // Title
    setTestTitle("Delete an email server without authentication");

    // Delete email server
    delete("CrtNam", "plain updated", "tools", "email-servers");

    // Verify deleted email server
    verifyDeleted("plain updated");
  }

  /**
   * View a log file
   */
  @Test
  void t081_viewLog() {
    // Title
    setTestTitle("View a log file");

    // Go to log screen
    gotoScreen("tools", "log");

    // Wait for reset button
    waitForButton("ButRst");

    // Write on criterion
    writeText("CrtFil", "SCHEDULER.log");

    // Search and wait
    searchAndWait();

    // Click on row
    clickRowContents("SCHEDULER.log");

    // Click on button
    clickButton("ButViw", true);

    // Wait for button
    waitForButton("ButBck");

    // Check text
    checkLogViewerContains("[SCHEDULER]");

    // Click back button
    clickButton("ButBck", true);
  }
}
