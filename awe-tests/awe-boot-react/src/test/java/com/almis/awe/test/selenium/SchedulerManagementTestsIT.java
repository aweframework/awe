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
 * The status and the jobs that each button leaves are proven by {@code SchedulerManagementTest}; the flow runs in this
 * engine only.
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
   * Press the four management buttons on one opening of the screen: clear the scheduler and stop it, restart it, stop it
   * and start it. Each button ends with its success message
   */
  @Test
  void t001_manageScheduler() {
    // Title
    setTestTitle("Clear and stop the scheduler, restart it, stop it and start it");

    // Go to management screen
    openScreen(SCHEDULER, MANAGEMENT_SCREEN);
    waitForLoadingBar();

    // From here on a failure leaves the scheduler restarted by the cleanup after the test
    schedulerStopped = true;
    pressButton("clearAndStopScheduler");
    pressButton("restartScheduler");
    pressButton("stopScheduler");
    pressButton("startScheduler");
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

    pressButton(button);
  }

  /**
   * Press a button of the management screen that is already open and wait for its success message
   *
   * @param button Button of the screen
   */
  private void pressButton(String button) {
    // Click button
    clickButton(button);

    // Wait for message
    checkAndCloseMessage(SUCCESS);
  }
}
