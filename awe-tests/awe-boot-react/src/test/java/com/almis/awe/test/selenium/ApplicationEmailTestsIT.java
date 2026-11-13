package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * The log viewer: open a log file from the log screen and read its contents. The log list and the content the viewer
 * returns are proven by ToolsAndSettingsActionsTest.logListFindsTheFileByItsNameAndTheViewerReturnsItsContent in
 * awe-boot; this flow, kept in the React application only, proves the screen opens the viewer of the selected row and
 * goes back. The mail servers screen
 * (email-server-admin) is an instance of the side-panel admin pattern proven by UserAdminTestsIT: its queries and
 * maintains are proven by EmailServerListQueryTest and EmailServerAdminScreenTest in awe-boot.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationEmailTestsIT extends AbstractApplicationTests {

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
