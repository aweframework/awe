package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Manual tasks of the scheduler: create, update and delete. The class creates and deletes its own task and does not need
 * any other record or class. The tests of the task are an ordered sequence on one record, and each one opens its own
 * screen.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerManualTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Manual task";
  private static final String TASK_UPDATED = "Manual task updated";

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
   * Generate a new manual task
   */
  @Test
  void t001_newManualTask() {
    // Title
    setTestTitle("Generate a new manual task");

    createManualTask(TASK, "Manual task description", "45");


    // Check element

    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Update the manual task
   */
  @Test
  void t021_updateManualTask() {
    // Title
    setTestTitle("Update the manual task");

    // Go to the task to update
    update(TASK_CRITERION, TASK, TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // UPDATE TASK DATA
    changeTaskData(TASK_UPDATED, "Manual task description updated");

    // Insert text
    writeText("NumStoExe", "60");

    // Insert text
    writeText("TimOutExe", "120");

    // Check
    clickCheckbox("LchDepWrn");

    // Check
    clickCheckbox("LchDepErr");

    // Check
    clickCheckbox("LchSetWrn");

    // Click on tab
    clickTab(TASK_FORM_SCREEN, "Parameters");

    // UPDATE PARAMETERS

    // Click on row
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);

    // Insert text
    writeText(PARAMETERS_GRID, "ParVal", "33");

    // Save row
    saveRow(PARAMETERS_GRID);

    // Click on tab
    clickTab(TASK_FORM_SCREEN, "Report");

    // UPDATE REPORT

    // Suggest on selector
    selectContain("RepTyp", "E-mail");

    // Suggest on selector
    suggestMultiple("RepSndSta", "Ok", "Ok");

    // Suggest on selector
    suggestMultiple("RepEmaDst", "test", "test");

    // Insert text
    writeText("RepTit", "Test email title");

    // Insert text
    writeText("RepMsg", "Test email message");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyUpdate(TASK_CRITERION, TASK, TASK_UPDATED);
  }

  /**
   * Delete the manual task
   */
  @Test
  void t901_deleteManualTask() {
    // Title
    setTestTitle("Delete the manual task");

    // Delete the task
    delete(TASK_CRITERION, TASK, SCHEDULER, TASK_LIST_SCREEN);

    // Verify deleted task
    verifyDeleted(TASK);
  }
}
