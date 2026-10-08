package com.almis.awe.test.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Management of the scheduler: clear and stop, restart, stop and start. It acts on the one scheduler of the application,
 * which the tasks of the other scheduler test classes need to be running, so it is a class of its own and it always leaves
 * the scheduler running: a test that stops it restarts it as its last step, and when the test fails in between, the cleanup
 * after the test restarts it (a restart works whatever state the scheduler is in), so a failure here does not leave the
 * next classes without a scheduler. The scheduler tasks that were cleared are loaded again from the database by the restart.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerManagementTestsIT extends AbstractSchedulerTests {

  private static final String SUCCESS = "success";

  /**
   * The test stopped the scheduler and has not started it again yet
   */
  private boolean schedulerStopped = false;

  /**
   * Restart the scheduler when a test left it stopped
   */
  @AfterEach
  void restoreScheduler() {
    if (schedulerStopped) {
      pressManagementButton("restartScheduler");
      schedulerStopped = false;
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
    checkLogout();
  }

  /**
   * Clear the scheduler and stop it, and restart it
   */
  @Test
  void t001_clearAndStopAndRestartScheduler() {
    // Title
    setTestTitle("Clear and stop the scheduler, and restart it");

    schedulerStopped = true;
    pressManagementButton("clearAndStopScheduler");

    pressManagementButton("restartScheduler");
    schedulerStopped = false;
  }

  /**
   * Stop the scheduler and start it
   */
  @Test
  void t002_stopAndStartScheduler() {
    // Title
    setTestTitle("Stop the scheduler and start it");

    schedulerStopped = true;
    pressManagementButton("stopScheduler");

    pressManagementButton("startScheduler");
    schedulerStopped = false;
  }

  /**
   * Open the management screen as new, press a button and wait for its success message
   *
   * @param button Button of the screen
   */
  private void pressManagementButton(String button) {
    // Go to management screen
    openScreen(SCHEDULER, MANAGEMENT_SCREEN);

    // Wait for loading bar
    waitForLoadingBar();

    // Click button
    clickButton(button);

    // Wait for message
    checkAndCloseMessage(SUCCESS);
  }
}
