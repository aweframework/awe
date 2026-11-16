package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tasks of the scheduler whose maintain answers deferred, one flow on the task screen: create a maintain task with the
 * wizard, launch it and see it running, stop it from the task list and see its execution stopped, then delete it. Nothing
 * completes the execution, so it keeps waiting until the stop. The class creates and deletes its own task; its tests are
 * an ordered sequence on that record, and each one opens its own screen. The stored state, the completion, the deadline
 * and the restart are proven by {@code SchedulerDeferredExecutionTest}.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerDeferredTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Deferred task flow";
  private static final String EXECUTIONS_GRID = "GrdExeLst";
  private static final String STATUS_COLUMN = "ExeStaIco";
  private static final String RUNNING_ICON = "fa-spin";
  private static final String STOPPED_ICON = "fa-pause";

  /**
   * Create a deferred task with the wizard
   */
  @Test
  void t001_newDeferredTask() {
    // Title
    setTestTitle("Generate a new deferred task");

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "Deferred task description");
    selectContain("TypExe", "Maintain");
    suggest("maintain", "deferredTestLaunch", "deferredTestLaunch");

    // Parameters (the task has none), schedule and dependencies
    clickButton("FwStep1");
    clickButton("FwStep2");
    clickButton("FwStep3");
    clickButton("FwStep4");

    // Store and confirm
    clickButtonAndConfirm("Finish");

    // Check element
    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Launch the task: its execution keeps running while nothing completes it
   */
  @Test
  void t002_launchDeferredTask() {
    // Title
    setTestTitle("Launch the deferred task and see it running");

    // Launch the selected task (it has no parameters, so the launch is direct)
    selectRow(TASK, SCHEDULER, TASK_LIST_SCREEN);
    clickButton("ButRun", true);

    // Nothing ends the execution: the scheduler pushes the running icon and it stays
    checkGridIconVisible(EXECUTIONS_GRID, STATUS_COLUMN, RUNNING_ICON);
  }

  /**
   * Stop the waiting execution from the task list
   */
  @Test
  void t003_stopDeferredTask() {
    // Title
    setTestTitle("Stop the deferred task");

    // The task list shows the waiting task as running, so it can be stopped
    selectRow(TASK, SCHEDULER, TASK_LIST_SCREEN);
    clickButton("ButStp", true);

    // The execution is loaded again from the database with its final status
    selectRow(TASK, SCHEDULER, TASK_LIST_SCREEN);
    checkGridIconVisible(EXECUTIONS_GRID, STATUS_COLUMN, STOPPED_ICON);
  }

  /**
   * Delete the deferred task
   */
  @Test
  void t901_deleteDeferredTask() {
    // Title
    setTestTitle("Delete the deferred task");

    // Delete the task
    delete(TASK_CRITERION, TASK, SCHEDULER, TASK_LIST_SCREEN);

    // Verify deleted task
    verifyDeleted(TASK);
  }
}
