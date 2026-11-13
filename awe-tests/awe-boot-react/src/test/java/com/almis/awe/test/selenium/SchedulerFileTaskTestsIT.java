package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * File triggered tasks of the scheduler, with the scheduler server that they watch, one flow on their screens: create a
 * folder server (so activating the task does not try to reach an FTP server) and an inactive task with a file launch on it
 * in the wizard, activate the task (each button replaces the other one) and delete both, the task first because it uses
 * the server. The class does not need any other record or class. The stored server, launch and states are proven by
 * {@code SchedulerTaskMaintainTest} and {@code SchedulerServerCredentialMaintainTest}; the flow runs in this engine only.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerFileTaskTestsIT extends AbstractSchedulerTests {

  private static final String SERVER = "File server";
  private static final String TASK = "File task";

  /**
   * Generate a new scheduler server and a new inactive file triggered task that watches it
   */
  @Test
  void t001_newServerAndFileTask() {
    // Title
    setTestTitle("Generate a new scheduler server and a file triggered task");

    // New server
    addNew(CONFIRM_BUTTON, SCHEDULER, SERVERS_SCREEN);

    // Insert text
    writeText("Nom", SERVER);

    // Insert text
    writeText("Hst", "127.0.0.1");

    // Insert text
    writeText("Prt", "21212");

    // Suggest on selector
    selectContain("Pro", "Folder");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "File task description");

    // Suggest on selector
    selectContain("Act", "No");

    // Suggest on selector
    selectContain("TypExe", "Maintain");

    // Suggest on selector
    suggest("maintain", "waitSomeSeconds", "waitSomeSeconds");

    // Click on next step
    clickButton("FwStep1");

    // Click on next step
    clickButton("FwStep2");

    // FILL SCHEDULE

    // Suggest on selector
    selectContain("TypLch", "File");

    // Suggest on selector
    selectContain("RptTyp", "Hours");

    // Insert text
    writeText("RptNum", "1");

    // Suggest on selector: the search finds the new server
    suggest("LchSrv", SERVER, SERVER);

    // Insert text
    writeText("LchPth", "/test");

    // Insert text
    writeText("LchPat", ".*\\.txt");

    // Click on next step
    clickButton("FwStep3");

    // Click on next step
    clickButton("FwStep4");

    // Store and confirm
    clickButtonAndConfirm("Finish");

    // Check element
    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Activate the file task
   */
  @Test
  void t051_activateTask() {
    // Title
    setTestTitle("Activate task");

    // Select the file task
    selectRow(TASK, SCHEDULER, TASK_LIST_SCREEN);

    // Activate it
    clickButton("ButAct");

    // Check success execution
    checkButtonNotVisible("ButAct");
    checkButtonVisible("ButDea");
  }

  /**
   * Delete the file task and then the scheduler server that it used
   */
  @Test
  void t901_deleteFileTaskAndServer() {
    // Title
    setTestTitle("Delete the file task and the scheduler server");

    // Delete the task and then the server, the server even when the task could not be deleted
    cleanUp(
      () -> {
        delete(TASK_CRITERION, TASK, SCHEDULER, TASK_LIST_SCREEN);
        verifyDeleted(TASK);
      },
      () -> {
        delete(SERVER_CRITERION, SERVER, SCHEDULER, SERVERS_SCREEN);
        verifyDeleted(SERVER);
      });
  }
}
