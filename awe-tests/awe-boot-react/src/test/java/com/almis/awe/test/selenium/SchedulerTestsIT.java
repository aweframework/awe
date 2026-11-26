package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Basic flows of the scheduler screens (#993), one per screen: the task editor creates a manual task and its task page
 * runs it, the activity lists its execution with its log and opens its flow run page, the task catalog opens the task
 * page, and the configuration screen switches its tabs. The class creates and deletes its own task and does not need any
 * other record or class; its tests are an ordered sequence on that record, and each one opens its own screen. "Run now"
 * needs the scheduler to be running, the state in which the application starts.
 *
 * <p>The rules behind these screens are proven in lower layers, not here: the editor and the stored task by
 * {@code SchedulerTaskEditorMaintainTest}, {@code TaskEditorPresenterTest} and {@code SchedulerTaskEditorScreenDescriptorTest},
 * the activity by {@code SchedulerActivityQueriesTest}, the flow run by {@code SchedulerFlowRunTest}, the catalog and the
 * task page by {@code SchedulerTaskCatalogQueriesTest} and {@code TaskPagePresenterTest}, and the configuration by the
 * {@code SchedulerConfiguration*Test} classes.</p>
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerTestsIT extends AbstractSessionTests {

  private static final String SCHEDULER = "scheduler";
  private static final String ACTIVITY_SCREEN = "scheduler-activity";
  private static final String CATALOG_SCREEN = "scheduler-task-catalog";
  private static final String CONFIGURATION_SCREEN = "scheduler-configuration";

  private static final String TASK = "Scheduler basic flow task";
  private static final String MAINTAIN_EXECUTION = "1";
  private static final String WAIT_MAINTAIN = "waitSomeSeconds";
  private static final String PARAMETERS_GRID = "ParameterList";

  private static final String CATALOG_GRID = "GrdTskLst";
  private static final String CATALOG_SEARCH = "CrtTskSch";
  private static final String TASK_HEADER_CARD = "CrdTskPag";
  private static final String ACTIVITY_GRID = "GrdExeAct";
  private static final String ACTIVITY_SEARCH = "CrtExeTsk";
  private static final String ACTIVITY_PANEL = "PnlExeAct";
  private static final String MORE_ACTIONS_COLUMN = "MorBtn";
  private static final String RUN_VIEWS_CARD = "CrdRunVw";
  private static final String RUN_VIEW_SWITCH = "RunPagVw";
  private static final String RUN_PANEL = "PnlRunExe";
  private static final String CONFIGURATION_TABS = "CfgTab";

  SchedulerTestsIT() {
    super(Session.LOGGED_IN);
  }

  /**
   * The task editor creates a manual task that runs the maintain that waits some seconds (its parameters come from the
   * maintain and are edited in the grid of the editor), saves it and opens its task page, which runs it with the values of
   * its parameters
   */
  @Test
  void t001_createAndRunATaskInTheEditor() {
    // Title
    setTestTitle("Create a manual task in the task editor and run it from its task page");

    // The new task button of the catalog opens the editor
    openScreen(CATALOG_SCREEN);
    clickButton("ButNewTsk", true);

    // What it runs: the maintain fills the parameters, and its wait is edited in the grid. The dummy service waits
    // between 2 seconds less and 1 second more than the value, so 2 is the shortest wait that is never negative
    writeText("Nam", TASK);
    writeText("Des", "Basic flow of the scheduler screens");
    clickCheckboxOption("TypExe", MAINTAIN_EXECUTION);
    suggest("maintain", WAIT_MAINTAIN, WAIT_MAINTAIN);
    editRow(PARAMETERS_GRID, "secondsToWait");
    writeText(PARAMETERS_GRID, "ParVal", "2");
    saveRow(PARAMETERS_GRID);

    // Save: the editor opens the task page of the new task, which runs it from the dialog of its parameters
    clickButton("ButTskEdtNew", true);
    clickButton("ButTskRunVar");
    clickButton("ButLchTskVar", true);
    checkDialogClosed("task-variables");
  }

  /**
   * The activity finds the execution of the task: its side panel shows its log, and its flow run link opens the flow run
   * page, whose node opens the detail of the execution and whose view switch shows the timeline and the list
   */
  @Test
  void t002_followTheExecutionInTheActivityAndItsFlowRun() {
    // Title
    setTestTitle("Find the execution in the activity, see its log and open its flow run page");

    // The search finds the execution, whose details show the log of its maintain
    openScreen(ACTIVITY_SCREEN);
    searchGrid(ACTIVITY_SEARCH, TASK);
    checkRowContentsGrid(ACTIVITY_GRID, TASK);
    clickGridRowButton(ACTIVITY_GRID, TASK, MORE_ACTIONS_COLUMN);
    clickContextButton("CtxExeDet");
    checkSidePanelTitle(ACTIVITY_PANEL, TASK);
    checkLogViewerContains("Waiting finished!");
    closeSidePanel(ACTIVITY_PANEL);
    checkSidePanelClosed(ACTIVITY_PANEL);

    // The flow run link opens the flow run page: the final status, the graph with the task
    clickGridRowLink(ACTIVITY_GRID, TASK, "ExeRun");
    waitForLoadingBar();
    checkCardChip("CrdRunPag", "Ok", "success");
    checkCardBodyComponent(RUN_VIEWS_CARD, "RunGph");
    checkFlowGraphContains(TASK);

    // The node of the task opens the detail of its execution, which closes
    clickFlowGraphNode(TASK);
    checkSidePanelTitle(RUN_PANEL, "Execution details");
    closeSidePanel(RUN_PANEL);
    checkSidePanelClosed(RUN_PANEL);

    // The view switch shows the timeline and the list of the run
    clickCheckboxOption(RUN_VIEW_SWITCH, "RunPagTml");
    checkCardBodyComponent(RUN_VIEWS_CARD, "RunTml");
    clickCheckboxOption(RUN_VIEW_SWITCH, "RunPagLst");
    checkCardBodyComponent(RUN_VIEWS_CARD, "RunLst");
  }

  /**
   * The catalog finds the task and its link opens its task page, whose header shows the status of its last execution
   */
  @Test
  void t003_openTheTaskPageFromTheCatalog() {
    // Title
    setTestTitle("Find the task in the catalog and open its task page");

    openScreen(CATALOG_SCREEN);
    searchGrid(CATALOG_SEARCH, TASK);
    clickGridRowLink(CATALOG_GRID, TASK, "TskNam");
    waitForLoadingBar();
    checkCardChip(TASK_HEADER_CARD, "Last: Ok", "success");
  }

  /**
   * The configuration screen switches its tabs: servers, calendars and engine, each one with its own action
   */
  @Test
  void t004_switchTheTabsOfTheConfiguration() {
    // Title
    setTestTitle("Switch the servers, calendars and engine tabs of the configuration");

    openScreen(CONFIGURATION_SCREEN);
    clickTabStartingWith(CONFIGURATION_TABS, "Servers");
    checkButtonVisible("ButSrvNew");
    clickTabStartingWith(CONFIGURATION_TABS, "Calendars");
    checkButtonVisible("ButCalNew");
    clickTabStartingWith(CONFIGURATION_TABS, "Engine");
    checkButtonVisible("ButEng");
  }

  /**
   * Delete the task from the row menu of the catalog. It is idempotent: a task that is not listed was deleted by an
   * earlier attempt of the test
   */
  @Test
  void t901_deleteTheTask() {
    // Title
    setTestTitle("Delete the task from the catalog");

    openScreen(CATALOG_SCREEN);
    searchGrid(CATALOG_SEARCH, TASK);
    if (hasRowContentsGrid(CATALOG_GRID, TASK)) {
      clickGridRowButton(CATALOG_GRID, TASK, MORE_ACTIONS_COLUMN);
      clickContextButton("CtxTskDel");
      acceptConfirm();
    }
    checkRowGone(CATALOG_GRID, TASK);
  }

  /**
   * Open a screen of the scheduler as new, even when it is the one that the previous test (or an earlier attempt of the
   * same test) left open: the client does not load again a screen that is already open, so the steps go through another
   * screen of the scheduler first
   *
   * @param screen Menu option of the screen
   */
  private void openScreen(String screen) {
    gotoScreen(SCHEDULER, ACTIVITY_SCREEN.equals(screen) ? CATALOG_SCREEN : ACTIVITY_SCREEN);
    gotoScreen(SCHEDULER, screen);
    waitForLoadingBar();
  }
}
