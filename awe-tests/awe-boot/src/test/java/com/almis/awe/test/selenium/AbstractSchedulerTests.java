package com.almis.awe.test.selenium;

/**
 * Steps that the tests of the scheduler screens share. Each one opens its own screen, so a test does not depend on the
 * screen that the previous test left open, and every scheduler test class creates, updates and deletes records with names
 * of its own (no name, and no description, of a record of one class contains the name of a record of another class), so the
 * classes do not share rows. The grids are searched by contained text, so a test that updates a record finds it by its
 * original name or by the name that an earlier test gave it, and the tests that delete it use its last name.
 */
abstract class AbstractSchedulerTests extends AbstractSessionTests {

  static final String SCHEDULER = "scheduler";
  static final String TASK_LIST_SCREEN = "scheduler-tasks";
  static final String CALENDARS_SCREEN = "scheduler-calendars";
  static final String SERVERS_SCREEN = "scheduler-servers";
  static final String MANAGEMENT_SCREEN = "scheduler-management";

  static final String TASK_CRITERION = "CrtTsk";
  static final String CALENDAR_CRITERION = "CrtCal";
  static final String SERVER_CRITERION = "CrtSrv";

  static final String CONFIRM_BUTTON = "ButCnf";
  static final String TASK_CHECK_BUTTON = "ButHlp";
  /** Screen of a task that is being updated (a form with tabs), not the list of tasks */
  static final String TASK_FORM_SCREEN = "update-scheduler-task";

  static final String NAME = "Nam";
  static final String DESCRIPTION = "Des";
  static final String RESET_BUTTON = "ButRst";
  static final String PARAMETERS_GRID = "ParameterList";
  static final String EDITED_PARAMETER = "secondsToWait";
  static final String BROADCAST = "Broadcast";

  protected AbstractSchedulerTests() {
    super(Session.LOGGED_IN);
  }

  /**
   * Open a screen as new, even when it is the one that the previous test left open. The client does not load again a screen
   * that is already open, so its criteria, its pending reloads and its selected row would still be there when the test
   * starts (a row that the previous test selected is taken as selected and then lost when the grid reloads): the steps go
   * through another screen of the scheduler first.
   *
   * @param options Screen option
   */
  void openScreen(String... options) {
    String screen = options[options.length - 1];
    gotoScreen(SCHEDULER, SERVERS_SCREEN.equals(screen) ? CALENDARS_SCREEN : SERVERS_SCREEN);
    gotoScreen(options);
  }

  /**
   * Go to a screen to add a new option
   *
   * @param checkButton Button that the new screen shows when it is loaded
   * @param options     Screen option
   */
  void addNew(String checkButton, String... options) {
    // Go to screen
    openScreen(options);

    // Click on new button
    clickButton("ButNew", true);

    // Wait for button
    waitForButton(checkButton);
  }

  /**
   * Check on list the new values
   */
  void verifyNew(String suggest, String search) {
    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    checkRowContents(search);
  }

  /**
   * Check on list the updated values
   */
  void verifyUpdate(String suggest, String search, String... checkContents) {
    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(suggest, search, search);

    // Search on grid
    searchAndWait();

    // Check row contents
    checkRowContents(checkContents);
  }

  /**
   * Go to a screen to update the option
   *
   * @param criterion   Criterion to search option
   * @param search      Search string
   * @param checkButton Check button to verify update screen loaded
   * @param options     Option navigation
   */
  void update(String criterion, String search, String checkButton, String... options) {
    // Go to screen
    openScreen(options);

    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(criterion, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Click on update button
    clickButton("ButUpd", true);

    // Wait for button
    waitForButton(checkButton);
  }

  /**
   * Delete from a screen
   *
   * @param criterion Criterion to search
   * @param search    Search text
   * @param options   Screen options
   */
  void delete(String criterion, String search, String... options) {
    // Go to screen
    openScreen(options);

    // Wait for button
    clickButton(RESET_BUTTON);

    // Suggest on column selector
    suggest(criterion, search, search);

    // Search on grid
    searchAndWait();

    // Click row
    clickRowContents(search);

    // Store and confirm
    clickButtonAndConfirm("ButDel");
  }

  /**
   * Verify deleted
   *
   * @param search search
   */
  void verifyDeleted(String search) {
    // Wait for button
    clickButton(RESET_BUTTON);

    // Search on grid
    searchAndWait();

    // Click row
    checkRowNotContains(search);
  }

  /**
   * Go to a screen, list its rows and select the one with a text
   *
   * @param search  Text of the row
   * @param options Screen options
   */
  void selectRow(String search, String... options) {
    // Go to screen
    openScreen(options);

    // Wait for button
    clickButton(RESET_BUTTON);

    // Search on grid
    searchAndWait();

    // Select the row
    clickRowContents(search);
  }

  /**
   * Change task data
   *
   * @param title       Task title
   * @param description Task description
   */
  void changeTaskData(String title, String description) {
    // Insert text
    writeText(NAME, title);

    // Insert text
    writeText(DESCRIPTION, description);
  }

  /**
   * Change task launch type
   *
   * @param number Repeat number
   * @param type   Repeat type
   */
  void changeTaskLaunch(Integer number, String type) {
    clickTab(TASK_FORM_SCREEN, "ENUM_TASK_STEP_LAUNCH");

    // Insert text
    writeText("RptNum", number.toString());

    // Suggest on selector
    suggest("RptTyp", type, type);

    // Calculate fire times
    clickButton("ButUpdateFireTimes", true);
  }

  /**
   * Create a manual task that runs the maintain that waits some seconds
   *
   * @param name        Task name
   * @param description Task description
   * @param seconds     Seconds that the maintain waits
   */
  void createManualTask(String name, String description, String seconds) {
    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(name, description);

    // Suggest on selector
    suggest("TypExe", "Maintain", "Maintain");

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

    // Edit row
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);

    // Insert text
    writeText(PARAMETERS_GRID, "ParVal", seconds);

    // Save row
    saveRow(PARAMETERS_GRID);

    // Click on next step
    clickButton("FwStep2");

    // FILL SCHEDULE

    // Click on next step
    clickButton("FwStep3");

    // FILL DEPENDENCIES

    // Click on next step
    clickButton("FwStep4");

    // FILL REPORT

    // Suggest on selector
    suggest("RepTyp", BROADCAST, BROADCAST);

    // Suggest on selector
    suggestMultipleList("RepSndSta", "Warning", "Error");

    // Suggest on selector
    suggestMultiple("RepUsrDst", "test", "test");

    // Insert text
    writeText("RepMsg", "Test broadcast message");

    // Store and confirm
    clickButtonAndConfirm("Finish");
  }

  /**
   * Create a calendar that is active and has no dates
   *
   * @param name        Calendar name
   * @param description Calendar description
   */
  void createCalendar(String name, String description) {
    // New calendar
    addNew(CONFIRM_BUTTON, SCHEDULER, CALENDARS_SCREEN);

    // Insert text
    writeText("Nom", name);

    // Insert text
    writeText(DESCRIPTION, description);

    // Suggest on selector
    suggest("Act", "Yes", "Yes");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);
  }
}
