package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * File triggered tasks of the scheduler, with the scheduler server that they watch. The class creates its own server and
 * task, changes the server to a folder one (so activating the task does not try to reach an FTP server), activates the task
 * and deletes both, the task first because it uses the server. It does not need any other record or class.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerFileTaskTestsIT extends AbstractSchedulerTests {

  private static final String SERVER = "File server";
  private static final String SERVER_UPDATED = "File server updated";
  private static final String TASK = "File task";

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
    // The application asks for a confirmation before logging out
    checkLogoutWithConfirmation();
  }

  /**
   * Generate a new scheduler server
   */
  @Test
  void t001_newSchedulerServer() {
    // Title
    setTestTitle("Generate a new scheduler server");

    // New server
    addNew(CONFIRM_BUTTON, SCHEDULER, SERVERS_SCREEN);

    // Insert text
    writeText("Nom", SERVER);

    // Insert text
    writeText("Hst", "127.0.0.1");

    // Insert text
    writeText("Prt", "21212");

    // Suggest on selector
    selectContain("Pro", "FTP");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyNew(SERVER_CRITERION, SERVER);
  }

  /**
   * Generate a new file triggered task
   */
  @Test
  void t002_newFileTask() {
    // Title
    setTestTitle("Generate a new file triggered task");

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "File task description");

    // Insert text
    writeText("NumStoExe", "15");

    // Insert text
    writeText("TimOutExe", "60");

    // Suggest on selector
    selectContain("Act", "No");

    // Suggest on selector
    selectContain("TypExe", "Maintain");

    // Suggest on selector
    suggest("maintain", "waitSomeSeconds", "waitSomeSeconds");

    // Check
    clickCheckbox("LchDepWrn");

    // Check
    clickCheckbox("LchDepErr");

    // Check
    clickCheckbox("LchSetWrn");

    // Click on next step
    clickButton("FwStep1");

    // FILL PARAMETERS

    // Click on row
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);

    // Insert text
    writeText(PARAMETERS_GRID, "ParVal", "45");

    // Save row
    saveRow(PARAMETERS_GRID);

    // Click on next step
    clickButton("FwStep2");

    // FILL SCHEDULE

    // Suggest on selector
    selectContain("TypLch", "File");

    // Suggest on selector
    selectContain("RptTyp", "Hours");

    // Insert text
    writeText("RptNum", "1");

    // Suggest on selector
    suggest("LchSrv", SERVER, SERVER);

    // Insert text
    writeText("LchPth", "/test");

    // Insert text
    writeText("LchPat", ".*\\.txt");

    // Click on next step
    clickButton("FwStep3");

    // FILL DEPENDENCIES

    // Click on next step
    clickButton("FwStep4");

    // FILL REPORT

    // Suggest on selector
    selectContain("RepTyp", BROADCAST);

    // Suggest on selector
    suggestMultipleList("RepSndSta", "Error", "Stopped");

    // Suggest on selector
    suggestMultiple("RepUsrDst", "test", "test");

    // Insert text
    writeText("RepMsg", "Test broadcast message");

    // Store and confirm
    clickButtonAndConfirm("Finish");

    // Check element
    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Update a scheduler server
   */
  @Test
  void t011_updateSchedulerServer() {
    // Title
    setTestTitle("Update a scheduler server");

    // Go to server to update
    update(SERVER_CRITERION, SERVER, CONFIRM_BUTTON, SCHEDULER, SERVERS_SCREEN);

    // Insert text
    writeText("Nom", SERVER_UPDATED);

    // Suggest on selector
    selectContain("Pro", "Folder");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyNew(SERVER_CRITERION, SERVER_UPDATED);
  }

  /**
   * Activate a task
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
   * Delete the file task
   */
  @Test
  void t901_deleteFileTask() {
    // Title
    setTestTitle("Delete the file task");

    // Delete the task
    delete(TASK_CRITERION, TASK, SCHEDULER, TASK_LIST_SCREEN);

    // Verify deleted task
    verifyDeleted(TASK);
  }

  /**
   * Delete the scheduler server
   */
  @Test
  void t911_deleteSchedulerServer() {
    // Title
    setTestTitle("Delete the scheduler server");

    // Delete the server
    delete(SERVER_CRITERION, SERVER, SCHEDULER, SERVERS_SCREEN);

    // Verify deleted server
    verifyDeleted(SERVER);
  }
}
