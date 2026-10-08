package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Calendar;

/**
 * Scheduled tasks of the scheduler: create, every launch type, run now, dependencies and delete. The custom launch needs a
 * calendar and the dependencies need another task, so the class creates its own prerequisites (a calendar and a manual
 * task, with names that no other class uses) at the start and deletes them at the end. It does not need any other class,
 * but "run now" needs the scheduler to be running: that is the state in which the application starts and in which
 * {@link SchedulerManagementTestsIT} always leaves it.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerScheduledTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Scheduled task";
  private static final String PREREQUISITE_TASK = "Prerequisite task";
  private static final String PREREQUISITE_CALENDAR = "Launch calendar";
  // Name and description that the update tests give to the task: the word is the launch type or step that the test changes
  private static final String RENAMED_TASK = "Scheduled task (%s)";
  private static final String RENAMED_TASK_DESCRIPTION = "Scheduled task description (%s)";
  private static final String CUSTOM = "custom";
  private static final String DEPENDENCIES = "dependencies";

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
   * Generate the calendar that the custom launch needs
   */
  @Test
  void t001_newPrerequisiteCalendar() {
    // Title
    setTestTitle("Generate the calendar that the scheduled task uses");

    createCalendar(PREREQUISITE_CALENDAR, "Calendar for the custom launch");


    // Check element

    verifyNew(CALENDAR_CRITERION, PREREQUISITE_CALENDAR);
  }

  /**
   * Generate the manual task that the dependencies need
   */
  @Test
  void t002_newPrerequisiteTask() {
    // Title
    setTestTitle("Generate the task that the scheduled task depends on");

    createManualTask(PREREQUISITE_TASK, "Prerequisite of the dependencies test", "1");


    // Check element

    verifyNew(TASK_CRITERION, PREREQUISITE_TASK);
  }

  /**
   * Generate a new scheduled task
   */
  @Test
  void t003_newScheduledTask() {
    // Title
    setTestTitle("Generate a new scheduled task");

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "Scheduled task description");

    // Insert text
    writeText("NumStoExe", "2");

    // Insert text
    writeText("TimOutExe", "59");

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
    writeText(PARAMETERS_GRID, "ParVal", "3");

    // Save row
    saveRow(PARAMETERS_GRID);

    // Click on next step
    clickButton("FwStep2");

    // FILL SCHEDULE

    // Suggest on selector
    selectContain("TypLch", "Scheduled");

    // Suggest on selector
    selectContain("RptTyp", "Seconds");

    // Insert text
    writeText("RptNum", "1200");

    // Calculate fire times
    clickButton("ButUpdateFireTimes", true);

    // Click on next step
    clickButton("FwStep3");

    // FILL DEPENDENCIES

    // Click on next step
    clickButton("FwStep4");

    // FILL REPORT

    // Suggest on selector
    selectContain("RepTyp", BROADCAST);

    // Suggest on selector
    suggestMultipleList("RepSndSta", "Warning", "Stopped");

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
   * Update the scheduled task (minutes)
   */
  @Test
  void t031_updateScheduledTaskMinutes() {
    // Title
    setTestTitle("Update the scheduled task in minutes");

    updateScheduledTaskEvery("minutes", 4, "Minutes");
  }

  /**
   * Update the scheduled task (hours)
   */
  @Test
  void t032_updateScheduledTaskHours() {
    // Title
    setTestTitle("Update the scheduled task in hours");

    updateScheduledTaskEvery("hours", 6, "Hours");
  }

  /**
   * Update the scheduled task (days)
   */
  @Test
  void t033_updateScheduledTaskDays() {
    // Title
    setTestTitle("Update the scheduled task in days");

    updateScheduledTaskEvery("days", 8, "Days");
  }

  /**
   * Update the scheduled task (months)
   */
  @Test
  void t034_updateScheduledTaskMonths() {
    // Title
    setTestTitle("Update the scheduled task in months");

    updateScheduledTaskEvery("months", 10, "Months");
  }

  /**
   * Update the scheduled task (years)
   */
  @Test
  void t035_updateScheduledTaskYears() {
    // Title
    setTestTitle("Update the scheduled task in years");

    updateScheduledTaskEvery("years", 12, "Years");
  }

  /**
   * Update the scheduled task (once)
   */
  @Test
  void t036_updateScheduledTaskOnce() {
    // Title
    setTestTitle("Update the scheduled task once");

    // Go to the task to update
    updateTask();

    // UPDATE TASK DATA
    changeTaskData(RENAMED_TASK.formatted("once"), RENAMED_TASK_DESCRIPTION.formatted("once"));

    // UPDATE TASK LAUNCH
    clickTab(TASK_FORM_SCREEN, "Launch");

    // Suggest on selector
    selectContain("RptTyp", "Once");

    // Select day
    selectDay("schExeDate", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

    // Write hour
    writeText("schExeTime", "23:59:59");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyUpdate(TASK_CRITERION, TASK, RENAMED_TASK.formatted("once"));
  }

  /**
   * Update the scheduled task (custom)
   */
  @Test
  void t037_updateScheduledTaskCustom() {
    // Title
    setTestTitle("Update the scheduled task custom");

    // Go to the task to update
    updateTask();

    // UPDATE TASK DATA
    changeTaskData(RENAMED_TASK.formatted(CUSTOM), RENAMED_TASK_DESCRIPTION.formatted(CUSTOM));

    // UPDATE TASK LAUNCH
    clickTab(TASK_FORM_SCREEN, "Launch");

    // Suggest on selector
    selectContain("RptTyp", "Custom");

    // Suggest on selector
    suggest("IdeCal", PREREQUISITE_CALENDAR, PREREQUISITE_CALENDAR);

    // Select years
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    suggestMultipleList("years", String.valueOf(currentYear), String.valueOf(currentYear + 1));

    // Select months
    suggestMultipleList("months", "October", "November", "December");

    // Select days
    suggestMultipleList("days", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

    // Select hours
    suggestMultipleList("hours", "00:00", "1:00", "2:00");

    // Select minutes
    suggestMultipleList("minutes", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

    // Select seconds
    suggestMultipleList("seconds", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

    // Select initial date
    selectDay("IniDat", getTodayDay());

    // Select end date
    selectDate("EndDat", "01/12/" + (currentYear + 5));

    // Calculate fire times
    clickButton("ButUpdateFireTimes", true);

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyUpdate(TASK_CRITERION, TASK, RENAMED_TASK.formatted(CUSTOM));
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
    selectRow(RENAMED_TASK.formatted(CUSTOM), SCHEDULER, TASK_LIST_SCREEN);

    // The task has variable parameters (its maintain variables), so manual launch opens
    // the values modal instead of launching directly.
    clickButton("ButRunVar");

    // Launch from the modal (values are pre-filled with the stored defaults)
    clickButton("ButLchTskVar", true);

    // Wait 5 seconds to finish the task
    pause(5000);

    // Check success execution
    checkColumnSuccessIcon("ExeStaIco");
  }

  /**
   * Update the scheduled task dependencies
   */
  @Test
  void t039_updateScheduledTaskDependencies() {
    // Title
    setTestTitle("Update the scheduled task dependencies");

    // Go to the task to update (it has the name that the custom launch test gave it)
    update(TASK_CRITERION, RENAMED_TASK.formatted(CUSTOM), TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // UPDATE TASK DATA
    changeTaskData(RENAMED_TASK.formatted(DEPENDENCIES), RENAMED_TASK_DESCRIPTION.formatted(DEPENDENCIES));

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
    verifyUpdate(TASK_CRITERION, TASK, RENAMED_TASK.formatted(DEPENDENCIES));
  }

  /**
   * Delete the scheduled task
   */
  @Test
  void t901_deleteScheduledTask() {
    // Title
    setTestTitle("Delete the scheduled task");

    // Delete the task
    delete(TASK_CRITERION, RENAMED_TASK.formatted(DEPENDENCIES), SCHEDULER, TASK_LIST_SCREEN);

    // Verify deleted task
    verifyDeleted(RENAMED_TASK.formatted(DEPENDENCIES));
  }

  /**
   * Delete the task that the dependencies needed
   */
  @Test
  void t902_deletePrerequisiteTask() {
    // Title
    setTestTitle("Delete the task that the scheduled task depended on");

    // Delete the task
    delete(TASK_CRITERION, PREREQUISITE_TASK, SCHEDULER, TASK_LIST_SCREEN);

    // Verify deleted task
    verifyDeleted(PREREQUISITE_TASK);
  }

  /**
   * Delete the calendar that the custom launch needed
   */
  @Test
  void t903_deletePrerequisiteCalendar() {
    // Title
    setTestTitle("Delete the calendar that the scheduled task used");

    // Delete the calendar
    delete(CALENDAR_CRITERION, PREREQUISITE_CALENDAR, SCHEDULER, CALENDARS_SCREEN);

    // Verify deleted calendar
    verifyDeleted(PREREQUISITE_CALENDAR);
  }

  /**
   * Go to the scheduled task to update it
   */
  private void updateTask() {
    update(TASK_CRITERION, TASK, TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);
  }

  /**
   * Update the scheduled task to launch every some time
   *
   * @param name   Word that the name and the description of the task get
   * @param number Repeat number
   * @param type   Repeat type
   */
  private void updateScheduledTaskEvery(String name, int number, String type) {
    // Go to the task to update
    updateTask();

    // UPDATE TASK DATA
    changeTaskData(RENAMED_TASK.formatted(name), RENAMED_TASK_DESCRIPTION.formatted(name));

    // UPDATE TASK LAUNCH
    changeTaskLaunch(number, type);

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyUpdate(TASK_CRITERION, TASK, RENAMED_TASK.formatted(name));
  }
}
