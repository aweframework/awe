package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Calendar;

/**
 * Scheduled tasks of the scheduler, one flow on the task screens: create a task with the wizard and the selectors of a
 * custom launch on a calendar, run it now (the scheduler pushes the result of the execution), add a dependency in the
 * editable grid of the update screen and delete it. The custom launch needs a calendar and the dependency another task, so
 * the class creates its own prerequisites (a calendar and a manual task, with names that no other class uses) at the start
 * and deletes them at the end. It does not need any other class, but "run now" needs the scheduler to be running, the state
 * in which the application starts. The other launch types and the stored launch, dependencies and states are proven by
 * {@code SchedulerTaskMaintainTest}.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerScheduledTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Scheduled task";
  private static final String TASK_UPDATED = "Scheduled task (dependencies)";
  private static final String PREREQUISITE_TASK = "Prerequisite task";
  private static final String PREREQUISITE_CALENDAR = "Launch calendar";

  /**
   * Generate the calendar that the custom launch needs and the manual task that the dependencies need
   */
  @Test
  void t001_newPrerequisites() {
    // Title
    setTestTitle("Generate the calendar and the task that the scheduled task uses");

    createCalendar(PREREQUISITE_CALENDAR, "Calendar for the custom launch");
    openScreen(SCHEDULER, CALENDARS_SCREEN);
    verifyNew(CALENDAR_CRITERION, PREREQUISITE_CALENDAR);

    createManualTask(PREREQUISITE_TASK, "Prerequisite of the dependencies test", "1");
    openScreen(SCHEDULER, TASK_LIST_SCREEN);
    verifyNew(TASK_CRITERION, PREREQUISITE_TASK);
  }

  /**
   * Generate a new scheduled task with a custom launch on the calendar
   */
  @Test
  void t003_newScheduledTask() {
    // Title
    setTestTitle("Generate a new scheduled task with a custom launch");

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "Scheduled task description");

    // Suggest on selector
    selectContain("TypExe", "Maintain");

    // Suggest on selector
    suggest("maintain", "waitSomeSeconds", "waitSomeSeconds");

    // Click on next step
    clickButton("FwStep1");

    // FILL PARAMETERS

    // Click on row
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);

    // Insert text
    writeText(PARAMETERS_GRID, "ParVal", "3");

    // Save row
    saveRow(PARAMETERS_GRID);

    // Click on next step
    clickButton("FwStep2");

    // FILL SCHEDULE

    // Suggest on selector
    selectContain("TypLch", "Scheduled");

    // Suggest on selector
    selectContain("RptTyp", "Custom");

    // Suggest on selector
    suggest("IdeCal", PREREQUISITE_CALENDAR, PREREQUISITE_CALENDAR);

    // Select years
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    suggestMultipleList("years", String.valueOf(currentYear), String.valueOf(currentYear + 1));

    // Select months
    suggestMultipleList("months", "October", "November");

    // Select days
    suggestMultipleList("days", "1", "2");

    // Select hours
    suggestMultipleList("hours", "00:00", "1:00");

    // Select minutes
    suggestMultipleList("minutes", "0");

    // Select seconds
    suggestMultipleList("seconds", "0");

    // Select initial date
    selectDay("IniDat", getTodayDay());

    // Select end date
    selectDate("EndDat", "01/12/" + (currentYear + 5));

    // Calculate fire times
    clickButton("ButUpdateFireTimes", true);

    // Click on next step
    clickButton("FwStep3");

    // FILL DEPENDENCIES

    // Click on next step
    clickButton("FwStep4");

    // Store and confirm
    clickButtonAndConfirm("Finish");

    // Check element
    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Run a task immediately. It runs before the dependencies are added: a task with dependencies launches them when it
   * finishes, and they would still be running (and refreshing the task grid) while the next tests start
   */
  @Test
  void t038_runTaskNow() {
    // Title
    setTestTitle("Run task immediately");

    // Select the scheduled task
    selectRow(TASK, SCHEDULER, TASK_LIST_SCREEN);

    // The task has variable parameters (its maintain variables), so manual launch opens
    // the values modal instead of launching directly.
    clickButton("ButRunVar");

    // Launch from the modal (values are pre-filled with the stored defaults)
    clickButton("ButLchTskVar", true);

    // Check success execution: nothing reloads the executions grid after the launch, so the icon is the live update
    // that the scheduler pushes when the task ends (#815); the check waits for it within the test timeout
    checkColumnSuccessIcon("ExeStaIco");
  }

  /**
   * Add a dependency to the scheduled task in the update screen
   */
  @Test
  void t039_updateScheduledTaskDependencies() {
    // Title
    setTestTitle("Update the scheduled task dependencies");

    // Go to the task to update
    update(TASK_CRITERION, TASK, TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // UPDATE TASK DATA
    changeTaskData(TASK_UPDATED, "Scheduled task description (dependencies)");

    // UPDATE TASK DEPENDENCIES
    clickTab(TASK_FORM_SCREEN, "Dependencies");

    // Click on add button
    clickButton("ButAddDependency");

    // Suggest on grid
    suggest("DependencyList", "DepTskIde", PREREQUISITE_TASK, PREREQUISITE_TASK);

    // Save row
    saveRow("DependencyList");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyUpdate(TASK_CRITERION, TASK, TASK_UPDATED);
  }

  /**
   * Delete the scheduled task, then the task that it depended on and the calendar that its launch used
   */
  @Test
  void t901_deleteScheduledTaskAndPrerequisites() {
    // Title
    setTestTitle("Delete the scheduled task and its prerequisites");

    // Delete the task (it has the name that the dependencies test gave it), the task that the dependencies needed and the
    // calendar that the custom launch needed: each one even when another fails
    cleanUp(
      () -> {
        delete(TASK_CRITERION, TASK_UPDATED, SCHEDULER, TASK_LIST_SCREEN);
        verifyDeleted(TASK_UPDATED);
      },
      () -> {
        delete(TASK_CRITERION, PREREQUISITE_TASK, SCHEDULER, TASK_LIST_SCREEN);
        verifyDeleted(PREREQUISITE_TASK);
      },
      () -> {
        delete(CALENDAR_CRITERION, PREREQUISITE_CALENDAR, SCHEDULER, CALENDARS_SCREEN);
        verifyDeleted(PREREQUISITE_CALENDAR);
      });
  }
}
