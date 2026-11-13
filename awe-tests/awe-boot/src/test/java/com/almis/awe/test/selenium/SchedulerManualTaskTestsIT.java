package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Manual tasks of the scheduler, one flow on the task screens: create a task with the wizard (the editable grid of
 * parameters), update it in the tabs of the update screen (the same grid) and delete it. The class creates and deletes its
 * own task and does not need any other record or class; its tests are an ordered sequence on that record, and each one
 * opens its own screen. The stored configuration, parameters and report are proven by {@code SchedulerTaskMaintainTest}.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerManualTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Manual task";
  private static final String TASK_UPDATED = "Manual task updated";

  /**
   * Generate a new manual task with the wizard and update its parameters
   */
  @Test
  void t001_newAndUpdateManualTask() {
    // Title
    setTestTitle("Generate a new manual task and update its parameters");

    createManualTask(TASK, "Manual task description", "45");

    // Go to the task to update: the search finds the new task
    update(TASK_CRITERION, TASK, TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // UPDATE TASK DATA
    changeTaskData(TASK_UPDATED, "Manual task description updated");

    // Click on tab
    clickTab(TASK_FORM_SCREEN, "ENUM_TASK_STEP_PARAMS");

    // UPDATE PARAMETERS

    // Click on row
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);

    // Insert text
    writeText(PARAMETERS_GRID, "ParVal", "33");

    // Save row
    saveRow(PARAMETERS_GRID);

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
