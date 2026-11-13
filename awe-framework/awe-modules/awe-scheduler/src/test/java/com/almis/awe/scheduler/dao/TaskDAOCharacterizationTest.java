package com.almis.awe.scheduler.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.maintain.Update;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.MaintainConstants.FILE_DELETE_MODIFICATION_QUERY;
import static com.almis.awe.scheduler.constant.MaintainConstants.GET_ALL_EXECUTIONS_WITH_DATES;
import static com.almis.awe.scheduler.constant.MaintainConstants.PURGE_EXECUTION_LOGS;
import static com.almis.awe.scheduler.constant.MaintainConstants.TASK_END;
import static com.almis.awe.scheduler.constant.MaintainConstants.UPDATE_INTERRUPTED_TASKS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_AVERAGE_TIME_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_EXECUTION;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_QUERY;
import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_AVERAGE_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_LOG_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_PROGRESS;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE_TASK;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_NEXT_EXECUTION;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_NAME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_SOURCE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_TYPE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_VALUE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Characterization tests pinning the current behavior of the {@link TaskDAO} public methods that
 * {@link TaskDAOTest} does not cover, so the class can be split by responsibility (#782) safely.
 */
@ExtendWith(MockitoExtension.class)
class TaskDAOCharacterizationTest {

  private TaskDAO taskDAO;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private Scheduler scheduler;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private CalendarDAO calendarDAO;

  @Mock
  private ServerDAO serverDAO;

  @Mock
  private FileChecker fileChecker;

  @Mock
  private ExecutionLogStore executionLogStore;

  @BeforeEach
  void initBeans() {
    taskDAO = new TaskDAO(scheduler, 5, queryService, maintainService, queryUtil, calendarDAO, serverDAO, fileChecker, executionLogStore);
    taskDAO.setApplicationContext(context);
  }

  // ---------------------------------------------------------------------------------------------
  // insertTask / updateTask
  // ---------------------------------------------------------------------------------------------

  /**
   * A manual task is loaded but never handed to the scheduler; the answer carries the new task locales.
   */
  @Test
  void insertManualTaskReturnsLocalesAndDoesNotScheduleIt() throws Exception {
    givenLocales();
    givenTaskQueries(TaskLaunchType.MANUAL.getValue(), false);

    ServiceData serviceData = taskDAO.insertTask(7);

    assertEquals("TITLE_SCHEDULER_NEW_TASK text", serviceData.getTitle());
    assertEquals("MESSAGE_SCHEDULER_NEW_TASK text", serviceData.getMessage());
    verify(queryService).launchPrivateQuery(eq(SCHEDULER_TASK_QUERY), any(ObjectNode.class));
    verifyNoInteractions(scheduler, maintainService);
  }

  /**
   * A scheduled inactive task is added to the scheduler and its trigger paused right away.
   */
  @Test
  void insertScheduledInactiveTaskSchedulesAndPausesIt() throws Exception {
    givenLocales();
    givenTaskQueries(TaskLaunchType.SCHEDULED.getValue(), false);

    taskDAO.insertTask(7);

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).checkExists(any(TriggerKey.class));
    verify(scheduler).scheduleJob(any(JobDetail.class), triggerCaptor.capture());
    verify(scheduler).pauseTrigger(triggerCaptor.getValue().getKey());
    Task scheduled = (Task) triggerCaptor.getValue().getJobDataMap().get(TASK);
    assertEquals(SCHEDULED_GROUP, scheduled.getGroup());
    verifyNoInteractions(maintainService);
  }

  /**
   * A scheduled active task is added to the scheduler and left running.
   */
  @Test
  void insertScheduledActiveTaskSchedulesWithoutPausing() throws Exception {
    givenLocales();
    givenTaskQueries(TaskLaunchType.SCHEDULED.getValue(), true);

    taskDAO.insertTask(7);

    verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    verify(scheduler, never()).pauseTrigger(any(TriggerKey.class));
  }

  /**
   * Any failure while loading or scheduling is wrapped with the task identifier.
   */
  @Test
  void insertTaskWrapsAnyFailure() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    AWException cause = new AWException("query failed");
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_QUERY), any(ObjectNode.class))).willThrow(cause);

    AWException exception = assertThrows(AWException.class, () -> taskDAO.insertTask(7));

    assertEquals("Error inserting new task: 7", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  /**
   * Updating deletes the task from the scheduler first and then inserts it again, answering the insert result.
   */
  @Test
  void updateTaskDeletesThenInsertsTheTask() throws Exception {
    givenLocales();
    givenTaskQueries(TaskLaunchType.MANUAL.getValue(), false);
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));
    given(scheduler.getTriggerGroupNames()).willReturn(List.of());

    ServiceData serviceData = taskDAO.updateTask(7);

    assertEquals("TITLE_SCHEDULER_NEW_TASK text", serviceData.getTitle());
    InOrder order = inOrder(executionLogStore, scheduler, queryService);
    order.verify(executionLogStore).purge(7, List.of());
    order.verify(scheduler).getTriggerGroupNames();
    order.verify(queryService).launchPrivateQuery(eq(SCHEDULER_TASK_QUERY), any(ObjectNode.class));
  }

  // ---------------------------------------------------------------------------------------------
  // deleteTask
  // ---------------------------------------------------------------------------------------------

  /**
   * Deleting a task purges its execution logs, deletes the job of every group holding its trigger and
   * clears its file modification table once per deleted job.
   */
  @Test
  void deleteTaskPurgesLogsDeletesJobAndClearsFileModifications() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    ObjectNode purgeParameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters()).willReturn(purgeParameters);
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(executionDataList(3, 7, 8)));
    given(scheduler.getTriggerGroupNames()).willReturn(List.of(SCHEDULED_GROUP, MANUAL_GROUP));
    TriggerKey scheduledKey = new TriggerKey("3", SCHEDULED_GROUP);
    given(scheduler.checkExists(scheduledKey)).willReturn(true);
    given(scheduler.checkExists(new TriggerKey("3", MANUAL_GROUP))).willReturn(false);
    Trigger trigger = mock(Trigger.class);
    JobKey jobKey = new JobKey("3", SCHEDULED_GROUP);
    given(trigger.getJobKey()).willReturn(jobKey);
    given(scheduler.getTrigger(scheduledKey)).willReturn(trigger);
    doReturn(aweRequest).when(context).getBean(AweRequest.class);

    ServiceData serviceData = taskDAO.deleteTask(3);

    assertNotNull(serviceData);
    assertEquals("", serviceData.getTitle());
    verify(executionLogStore).purge(3, List.of(7, 8));
    verify(maintainService).launchPrivateMaintain(PURGE_EXECUTION_LOGS, purgeParameters);
    assertEquals("[7,8]", purgeParameters.get("executionId").toString());
    assertEquals(3, purgeParameters.get("taskId").asInt());
    verify(scheduler).deleteJob(jobKey);
    verify(aweRequest).setParameter(TASK_IDE_TASK, JsonNodeFactory.instance.numberNode(3));
    verify(maintainService, times(1)).launchPrivateMaintain(FILE_DELETE_MODIFICATION_QUERY);
  }

  /**
   * A task without stored executions nor triggers issues neither the purge maintain nor any deletion.
   */
  @Test
  void deleteTaskWithoutExecutionsNorTriggersOnlyPurgesTheLogStore() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));
    given(scheduler.getTriggerGroupNames()).willReturn(List.of(SCHEDULED_GROUP));

    taskDAO.deleteTask(3);

    verify(executionLogStore).purge(3, List.of());
    verify(scheduler, never()).deleteJob(any(JobKey.class));
    verifyNoInteractions(maintainService);
  }

  /**
   * Any failure while deleting is wrapped with the task identifier.
   */
  @Test
  void deleteTaskWrapsAnyFailure() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));
    SchedulerException cause = new SchedulerException("boom");
    given(scheduler.getTriggerGroupNames()).willThrow(cause);

    AWException exception = assertThrows(AWException.class, () -> taskDAO.deleteTask(3));

    assertEquals("[SCHEDULER] Error deleting task: 3", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  /**
   * The list overload deletes every task in order.
   */
  @Test
  void deleteTaskListDeletesEveryTaskInOrder() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));
    given(scheduler.getTriggerGroupNames()).willReturn(List.of());

    ServiceData serviceData = taskDAO.deleteTask(List.of(3, 4));

    assertNotNull(serviceData);
    InOrder order = inOrder(executionLogStore);
    order.verify(executionLogStore).purge(3, List.of());
    order.verify(executionLogStore).purge(4, List.of());
  }

  /**
   * An empty list deletes nothing and answers an empty service data.
   */
  @Test
  void deleteTaskEmptyListDeletesNothing() throws Exception {
    ServiceData serviceData = taskDAO.deleteTask(List.of());

    assertNotNull(serviceData);
    verifyNoInteractions(scheduler, executionLogStore, maintainService, queryService);
  }

  // ---------------------------------------------------------------------------------------------
  // getTaskList
  // ---------------------------------------------------------------------------------------------

  /**
   * Every task list row is enriched with the scheduler state: running status and next fire time for
   * normal triggers, OK status and no next time for paused or missing triggers, and the formatted
   * average time.
   */
  @Test
  void getTaskListAddsSchedulerStateToEveryRow() throws Exception {
    TaskListCriteria criteria = new TaskListCriteria();
    ObjectNode criteriaParameters = JsonNodeFactory.instance.objectNode();
    ObjectNode queryParameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters(criteria)).willReturn(criteriaParameters);
    given(queryUtil.getParameters(criteriaParameters, null, "1", "0")).willReturn(queryParameters);
    DataList dataList = new DataList();
    dataList.addRow(taskListRow(1, "1", 1500));
    dataList.addRow(taskListRow(2, "0", 2500));
    dataList.addRow(taskListRow(3, "1", 0));
    given(queryService.launchPrivateQuery(SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY, queryParameters))
      .willReturn(new ServiceData().setDataList(dataList));

    // Task 1: scheduled, normal and running
    Date nextFireTime = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7, 890_000_000).atZone(ZoneId.systemDefault()).toInstant());
    JobDetail runningJob = givenTrigger(new TriggerKey("1", SCHEDULED_GROUP), Trigger.TriggerState.NORMAL, nextFireTime);
    // Task 2: manual, not in the scheduler
    given(scheduler.checkExists(new TriggerKey("2", MANUAL_GROUP))).willReturn(false);
    // Task 3: scheduled but paused
    givenTrigger(new TriggerKey("3", SCHEDULED_GROUP), Trigger.TriggerState.PAUSED, nextFireTime);
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    given(runningContext.getJobDetail()).willReturn(runningJob);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(List.of(runningContext));

    ServiceData serviceData = taskDAO.getTaskList(criteria);

    // CellData trims the stored " - " placeholder when read as string
    List<Map<String, CellData>> rows = serviceData.getDataList().getRows();
    assertEquals(3, rows.size());
    assertEquals(TaskStatus.JOB_RUNNING.getValue(), rows.get(0).get(TASK_STATUS).getIntegerValue());
    assertEquals("04/03/2026 05:06:07", rows.get(0).get(TASK_NEXT_EXECUTION).getStringValue());
    assertEquals(TimeUtil.formatTime(1500), rows.get(0).get(TASK_AVERAGE_TIME).getStringValue());
    assertEquals(TaskStatus.JOB_OK.getValue(), rows.get(1).get(TASK_STATUS).getIntegerValue());
    assertEquals("-", rows.get(1).get(TASK_NEXT_EXECUTION).getStringValue());
    assertEquals(TimeUtil.formatTime(2500), rows.get(1).get(TASK_AVERAGE_TIME).getStringValue());
    assertEquals(TaskStatus.JOB_OK.getValue(), rows.get(2).get(TASK_STATUS).getIntegerValue());
    assertEquals("-", rows.get(2).get(TASK_NEXT_EXECUTION).getStringValue());
    assertEquals(3, serviceData.getDataList().getRecords());
  }

  /**
   * A scheduler failure while enriching a row is wrapped with the trigger key as message.
   */
  @Test
  void getTaskListWrapsSchedulerFailures() throws Exception {
    TaskListCriteria criteria = new TaskListCriteria();
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters(criteria)).willReturn(parameters);
    given(queryUtil.getParameters(parameters, null, "1", "0")).willReturn(parameters);
    DataList dataList = new DataList();
    dataList.addRow(taskListRow(1, "1", 1500));
    given(queryService.launchPrivateQuery(SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY, parameters))
      .willReturn(new ServiceData().setDataList(dataList));
    SchedulerException cause = new SchedulerException("boom");
    given(scheduler.checkExists(any(TriggerKey.class))).willThrow(cause);

    AWException exception = assertThrows(AWException.class, () -> taskDAO.getTaskList(criteria));

    assertEquals("Error adding scheduler parameters to the task datalist: {}", exception.getTitle());
    assertEquals(new TriggerKey("1", SCHEDULED_GROUP).toString(), exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  // ---------------------------------------------------------------------------------------------
  // getTaskExecutionList
  // ---------------------------------------------------------------------------------------------

  /**
   * Running executions get an overflowing progress bar when no average time exists and their elapsed
   * time without milliseconds; finished ones get no progress and their stored execution time. Every
   * row gets the launched-by text and the execution log locator from the store.
   */
  @Test
  void getTaskExecutionListDecoratesRunningAndFinishedExecutions() throws Exception {
    givenLocales();
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    Date runningStart = new Date(System.currentTimeMillis() - 120_000L);
    DataList dataList = new DataList();
    dataList.addRow(executionListRow(10, TaskStatus.JOB_RUNNING, null, DEPENDENCY_GROUP, "#3", runningStart));
    dataList.addRow(executionListRow(11, TaskStatus.JOB_OK, 1500, MANUAL_GROUP, "admin", new Date(0)));
    given(queryService.launchPrivateQuery(eq("getTaskExecutionList"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_AVERAGE_TIME_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));
    ObjectNode locator10 = JsonNodeFactory.instance.objectNode().put("value", "log-10");
    ObjectNode locator11 = JsonNodeFactory.instance.objectNode().put("value", "log-11");
    given(executionLogStore.locatorNode(new ExecutionKey(5, 10))).willReturn(locator10);
    given(executionLogStore.locatorNode(new ExecutionKey(5, 11))).willReturn(locator11);

    ServiceData serviceData = taskDAO.getTaskExecutionList(5);

    Map<String, CellData> running = serviceData.getDataList().getRows().get(0);
    ObjectNode progress = (ObjectNode) running.get(TASK_EXECUTION_PROGRESS).getObjectValue();
    assertEquals(100, progress.get("value").asInt());
    assertEquals("progress-bar-danger", progress.get("style").asString());
    assertEquals("", progress.get("label").asString());
    String elapsed = running.get(TASK_EXECUTION_TIME).getStringValue();
    assertTrue(Set.of(TimeUtil.formatTime(120_000, false), TimeUtil.formatTime(121_000, false)).contains(elapsed), elapsed);
    assertEquals("PARAMETER_TASK text #3", running.get(TASK_LAUNCHED_BY_LIST).getStringValue());
    assertEquals(locator10, running.get(TASK_EXECUTION_LOG_PATH).getObjectValue());

    Map<String, CellData> finished = serviceData.getDataList().getRows().get(1);
    assertTrue(finished.get(TASK_EXECUTION_PROGRESS).isEmpty());
    assertEquals(TimeUtil.formatTime(1500), finished.get(TASK_EXECUTION_TIME).getStringValue());
    assertEquals("admin", finished.get(TASK_LAUNCHED_BY_LIST).getStringValue());
    assertEquals(locator11, finished.get(TASK_EXECUTION_LOG_PATH).getObjectValue());
  }

  /**
   * A running execution with an average time shows its percentage of that average; a non-running
   * execution without stored execution time gets an empty time.
   */
  @Test
  void getTaskExecutionListComputesProgressAgainstTheAverageTime() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    Date runningStart = new Date(System.currentTimeMillis() - 100_000L);
    DataList dataList = new DataList();
    dataList.addRow(executionListRow(10, TaskStatus.JOB_RUNNING, null, MANUAL_GROUP, "admin", runningStart));
    dataList.addRow(executionListRow(11, TaskStatus.JOB_ERROR, null, MANUAL_GROUP, "admin", new Date(0)));
    given(queryService.launchPrivateQuery(eq("getTaskExecutionList"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_AVERAGE_TIME_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(averageTimeDataList(200_000)));

    ServiceData serviceData = taskDAO.getTaskExecutionList(5);

    ObjectNode progress = (ObjectNode) serviceData.getDataList().getRows().get(0).get(TASK_EXECUTION_PROGRESS).getObjectValue();
    long percentage = progress.get("value").asLong();
    assertTrue(percentage == 50L || percentage == 51L, String.valueOf(percentage));
    assertEquals(percentage + "%", progress.get("label").asString());
    assertFalse(progress.has("style"));
    assertTrue(serviceData.getDataList().getRows().get(1).get(TASK_EXECUTION_TIME).isEmpty());
  }

  // ---------------------------------------------------------------------------------------------
  // loadMaintainVariables
  // ---------------------------------------------------------------------------------------------

  /**
   * The maintain query variables are listed once per name (first definition wins), skipping unnamed
   * variables, as VARIABLE-source parameters with an empty value.
   */
  @Test
  void loadMaintainVariablesListsUniqueNamedVariables() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    List<MaintainQuery> queries = new ArrayList<>();
    queries.add(maintainQuery(new Insert(), variable("date", "STRING"), variable("count", "INTEGER")));
    queries.add(maintainQuery(new Update(), variable("date", "DATE"), variable(null, "STRING")));
    queries.add(new Update());
    given(aweElements.getMaintain("myMaintain")).willReturn(new Target().setQueryList(queries));

    ServiceData serviceData = taskDAO.loadMaintainVariables("myMaintain");

    List<Map<String, CellData>> rows = serviceData.getDataList().getRows();
    assertEquals(2, rows.size());
    Map<String, String> typeByName = rows.stream().collect(Collectors.toMap(
      row -> row.get(TASK_PARAMETER_NAME).getStringValue(), row -> row.get(TASK_PARAMETER_TYPE).getStringValue()));
    assertEquals(Map.of("date", "STRING", "count", "INTEGER"), typeByName);
    rows.forEach(row -> {
      assertEquals("1", row.get(TASK_PARAMETER_SOURCE).getStringValue());
      assertEquals("", row.get(TASK_PARAMETER_VALUE).getStringValue());
    });
  }

  // ---------------------------------------------------------------------------------------------
  // getProgress / getElapsedTime / getProgressNode / getStatusIcon
  // ---------------------------------------------------------------------------------------------

  /**
   * Without an average execution time the progress is always 101 (overflow).
   */
  @Test
  void getProgressWithoutAverageIsOverflow() {
    assertEquals(101L, taskDAO.getProgress(new Date(), 0));
  }

  /**
   * The progress is the elapsed time since the start as an integer percentage of the average time.
   */
  @Test
  void getProgressIsElapsedPercentageOfTheAverage() {
    long progress = taskDAO.getProgress(new Date(System.currentTimeMillis() - 30_000L), 60_000L);

    assertTrue(progress == 50L || progress == 51L, String.valueOf(progress));
  }

  /**
   * A start date in the future gives a negative progress.
   */
  @Test
  void getProgressOfFutureStartIsNegative() {
    assertTrue(taskDAO.getProgress(new Date(System.currentTimeMillis() + 60_000L), 60_000L) < 0);
  }

  /**
   * The elapsed time is the milliseconds since the start date.
   */
  @Test
  void getElapsedTimeIsMillisecondsSinceStart() {
    Integer elapsed = taskDAO.getElapsedTime(new Date(System.currentTimeMillis() - 5_000L));

    assertTrue(elapsed >= 5_000 && elapsed < 6_000, String.valueOf(elapsed));
  }

  /**
   * A null start date fails with a null pointer exception.
   */
  @Test
  void getElapsedTimeAndProgressRejectNullDates() {
    assertThrows(NullPointerException.class, () -> taskDAO.getElapsedTime((Date) null));
    assertThrows(NullPointerException.class, () -> taskDAO.getProgress((Date) null, 10L));
  }

  /**
   * Percentages up to 100 are shown as is; above 100 the bar is full and flagged as danger.
   */
  @Test
  void getProgressNodeCapsAtOneHundred() {
    assertEquals("{\"value\":100,\"label\":\"100%\"}", taskDAO.getProgressNode(100L).toString());
    assertEquals("{\"value\":0,\"label\":\"0%\"}", taskDAO.getProgressNode(0L).toString());
    assertEquals("{\"value\":100,\"style\":\"progress-bar-danger\",\"label\":\"\"}", taskDAO.getProgressNode(101L).toString());
  }

  /**
   * The status icon holds the status value plus its label, icon and style enumerations.
   */
  @Test
  void getStatusIconLooksUpTheStatusEnumerations() throws Exception {
    given(queryService.findLabel("StaTyp", "4")).willReturn("Running");
    given(queryService.findLabel("StaIco", "4")).willReturn("fa-spin");
    given(queryService.findLabel("StaSty", "4")).willReturn("text-info");

    ObjectNode icon = taskDAO.getStatusIcon(new TaskExecution().setStatus(4));

    assertEquals("{\"value\":4,\"label\":\"Running\",\"icon\":\"fa-spin\",\"style\":\"text-info\"}", icon.toString());
  }

  /**
   * A failing enumeration lookup is logged and leaves only the status value.
   */
  @Test
  void getStatusIconKeepsOnlyTheValueWhenTheLookupFails() throws Exception {
    given(queryService.findLabel("StaTyp", "4")).willThrow(new AWException("missing"));

    ObjectNode icon = taskDAO.getStatusIcon(new TaskExecution().setStatus(4));

    assertEquals("{\"value\":4}", icon.toString());
  }

  // ---------------------------------------------------------------------------------------------
  // Execution persistence
  // ---------------------------------------------------------------------------------------------

  /**
   * The execution time is the difference between the stored end and initial dates.
   */
  @Test
  void updateExecutionTimeStoresEndMinusInitialDate() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters()).willReturn(parameters);
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("initialDate", new CellData(new Date(1_000L)));
    row.put("endDate", new CellData(new Date(4_500L)));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(SCHEDULER_TASK_EXECUTION, parameters)).willReturn(new ServiceData().setDataList(dataList));
    ServiceData result = new ServiceData();
    given(maintainService.launchPrivateMaintain("updateExecutionTime", parameters)).willReturn(result);

    assertSame(result, taskDAO.updateExecutionTime(1, 2));
    assertEquals("{\"taskId\":1,\"taskExecution\":2,\"executionTime\":3500}", parameters.toString());
  }

  /**
   * Ending a task stores its status and description and reloads the execution, keeping its parent.
   */
  @Test
  void endTaskStoresTheEndAndReloadsTheExecution() throws Exception {
    ObjectNode endParameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters()).willReturn(endParameters);
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_IDE, new CellData(1));
    row.put("executionId", new CellData(12));
    row.put("status", new CellData(TaskStatus.JOB_OK.getValue()));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_EXECUTION), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));
    TaskExecution parent = new TaskExecution().setTaskId(9);
    Task task = new Task().setTaskId(1).setStatus(TaskStatus.JOB_OK).setParentExecution(parent);

    TaskExecution execution = taskDAO.endTask(task, new TaskExecution().setExecutionId(12).setDescription("done"));

    verify(maintainService).launchPrivateMaintain(TASK_END, endParameters);
    assertEquals("{\"taskId\":1,\"taskExecution\":12,\"status\":0,\"taskDescription\":\"done\"}", endParameters.toString());
    assertEquals(12, execution.getExecutionId());
    assertSame(parent, execution.getParentExecution());
  }

  /**
   * The executions to purge skip the most recent ones, lose their id column and update the record count.
   */
  @Test
  void getExecutionsToPurgeSkipsTheKeptExecutions() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(GET_ALL_EXECUTIONS_WITH_DATES), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(executionDataList(1, 30, 20, 10)));

    ServiceData serviceData = taskDAO.getExecutionsToPurge(1, 1);

    List<Map<String, CellData>> rows = serviceData.getDataList().getRows();
    assertEquals(2, rows.size());
    assertEquals(20, rows.get(0).get("executionId").getIntegerValue());
    assertEquals(10, rows.get(1).get("executionId").getIntegerValue());
    assertFalse(rows.get(0).containsKey("id"));
    assertEquals(2, serviceData.getDataList().getRecords());
  }

  /**
   * The interrupted tasks update is a plain maintain launch.
   */
  @Test
  void updateInterruptedTasksLaunchesTheMaintain() throws Exception {
    ServiceData result = new ServiceData();
    given(maintainService.launchPrivateMaintain(UPDATE_INTERRUPTED_TASKS)).willReturn(result);

    assertSame(result, taskDAO.updateInterruptedTasks());
  }

  /**
   * The average time is zero when no value is stored.
   */
  @Test
  void getAverageTimeDefaultsToZero() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq(SCHEDULER_AVERAGE_TIME_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()), new ServiceData().setDataList(averageTimeDataList(321)));

    assertEquals(0, taskDAO.getAverageTime(1));
    assertEquals(321, taskDAO.getAverageTime(1));
  }

  // ---------------------------------------------------------------------------------------------
  // Task loading and scheduling
  // ---------------------------------------------------------------------------------------------

  /**
   * The task travels in the job data map of both triggers and job details.
   */
  @Test
  void getTaskReadsTheJobDataMap() {
    Task task = new Task().setTaskId(1);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put(TASK, task);
    Trigger trigger = TriggerBuilder.newTrigger().usingJobData(dataMap).build();
    JobDetail jobDetail = mock(JobDetail.class);
    given(jobDetail.getJobDataMap()).willReturn(dataMap);

    assertSame(task, taskDAO.getTask(trigger));
    assertSame(task, taskDAO.getTask(jobDetail));
  }

  /**
   * Called directly (no Spring proxy), the asynchronous load answers an already completed future.
   */
  @Test
  void getTaskByIdAnswersACompletedFuture() throws Exception {
    givenTaskQueries(TaskLaunchType.MANUAL.getValue(), false);

    Task task = taskDAO.getTask(7).get();

    assertEquals(7, task.getTaskId());
    assertEquals(MANUAL_GROUP, task.getGroup());
  }

  /**
   * A scheduling failure is wrapped twice: scheduling error inside the add-to-scheduler error.
   */
  @Test
  void addTaskToSchedulerWrapsSchedulingFailures() throws Exception {
    SchedulerException cause = new SchedulerException("boom");
    given(scheduler.checkExists(any(TriggerKey.class))).willThrow(cause);
    Task task = new Task().setTaskId(1);
    task.setTrigger(TriggerBuilder.newTrigger().withIdentity("1", SCHEDULED_GROUP).build());

    AWException exception = assertThrows(AWException.class, () -> taskDAO.addTaskToScheduler(task));

    assertEquals("Error adding scheduler task: #1", exception.getMessage());
    AWException scheduling = assertInstanceOf(AWException.class, exception.getCause());
    assertEquals("Error scheduling task: 1", scheduling.getMessage());
    assertSame(cause, scheduling.getCause());
  }

  /**
   * Only file tracking tasks are checked: they are vetoed when the file did not change.
   */
  @Test
  void isTaskExecutionAllowedOnlyChecksFileTrackingTasks() {
    Task scheduled = new Task().setLaunchType(TaskLaunchType.SCHEDULED.getValue());
    Task fileTracking = new Task().setLaunchType(TaskLaunchType.FILE_TRACKING.getValue()).setLauncher("previous");
    given(fileChecker.checkFile(fileTracking)).willReturn(null);

    assertTrue(taskDAO.isTaskExecutionAllowed(null));
    assertTrue(taskDAO.isTaskExecutionAllowed(scheduled));
    assertFalse(taskDAO.isTaskExecutionAllowed(fileTracking));
    assertNull(fileTracking.getLauncher());
    verify(fileChecker, never()).checkFile(scheduled);
  }

  /**
   * Launching a task now without variables schedules an immediate manual trigger launched by the user.
   */
  @Test
  void executeTaskNowSchedulesAManualLaunch() throws Exception {
    givenTaskQueries(TaskLaunchType.SCHEDULED.getValue(), true);

    ServiceData serviceData = taskDAO.executeTaskNow(7, "admin");

    assertNotNull(serviceData);
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
    verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
    Task launched = (Task) triggerCaptor.getValue().getJobDataMap().get(TASK);
    assertEquals("admin", launched.getLauncher());
    assertNull(launched.getParentExecution());
    assertEquals(triggerCaptor.getValue().getKey().getName(), jobCaptor.getValue().getKey().getName());
    assertEquals(triggerCaptor.getValue().getKey().getGroup(), jobCaptor.getValue().getKey().getGroup());
  }

  /**
   * Launching a dependency without variables schedules it launched by the parent task, keeping the parent.
   */
  @Test
  void executeDependencySchedulesALaunchFromTheParent() throws Exception {
    givenTaskQueries(TaskLaunchType.SCHEDULED.getValue(), true);
    TaskExecution parent = new TaskExecution().setTaskId(2).setExecutionId(20);

    taskDAO.executeDependency(7, parent);

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(any(JobDetail.class), triggerCaptor.capture());
    Task launched = (Task) triggerCaptor.getValue().getJobDataMap().get(TASK);
    assertEquals("#2", launched.getLauncher());
    assertSame(parent, launched.getParentExecution());
  }

  /**
   * A failure while launching an immediate task is wrapped with the task identifier.
   */
  @Test
  void executeTaskNowWrapsLaunchFailures() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    AWException cause = new AWException("query failed");
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_QUERY), any(ObjectNode.class))).willThrow(cause);

    AWException exception = assertThrows(AWException.class, () -> taskDAO.executeTaskNow(7, "admin"));

    assertEquals("Error launching immediate task: 7", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  // ---------------------------------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------------------------------

  private void givenLocales() {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLocaleWithLanguage(anyString(), eq(null))).willAnswer(invocation -> invocation.getArgument(0) + " text");
  }

  private void givenTaskQueries(Integer launchType, boolean active) throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList taskDataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_LAUNCH_TYPE, new CellData(launchType));
    row.put(TASK_IDE, new CellData(7));
    row.put("repeatType", new CellData(2));
    row.put("repeatNumber", new CellData(2));
    row.put("executionType", new CellData(1));
    row.put("active", new CellData(active));
    taskDataList.addRow(row);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(taskDataList));
  }

  private JobDetail givenTrigger(TriggerKey key, Trigger.TriggerState state, Date nextFireTime) throws Exception {
    Trigger trigger = mock(Trigger.class);
    JobKey jobKey = new JobKey(key.getName(), key.getGroup());
    JobDetail jobDetail = mock(JobDetail.class);
    given(scheduler.checkExists(key)).willReturn(true);
    given(scheduler.getTrigger(key)).willReturn(trigger);
    given(scheduler.getTriggerState(key)).willReturn(state);
    if (state == Trigger.TriggerState.NORMAL) {
      given(trigger.getNextFireTime()).willReturn(nextFireTime);
    }
    given(trigger.getJobKey()).willReturn(jobKey);
    given(scheduler.getJobDetail(jobKey)).willReturn(jobDetail);
    return jobDetail;
  }

  private Map<String, CellData> taskListRow(int taskId, String launchType, int averageTime) {
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_IDE_LIST, new CellData(taskId));
    row.put(TASK_LAUNCH_TYPE_LIST, new CellData(launchType));
    row.put("averageTime", new CellData(averageTime));
    return row;
  }

  private Map<String, CellData> executionListRow(int executionId, TaskStatus status, Integer executionTime, String group,
                                                 String launchedBy, Date initialDate) {
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_EXECUTION_ID, new CellData(executionId));
    row.put(TASK_STATUS, new CellData(status.getValue()));
    if (executionTime != null) {
      row.put(TASK_EXECUTION_TIME, new CellData(executionTime));
    }
    row.put(TASK_LAUNCH_TYPE_LIST, new CellData(group));
    row.put(TASK_LAUNCHED_BY_LIST, new CellData(launchedBy));
    row.put("ExeLstTim", new CellData(initialDate));
    return row;
  }

  private DataList executionDataList(int taskId, int... executionIds) {
    DataList dataList = new DataList();
    for (int executionId : executionIds) {
      Map<String, CellData> row = new HashMap<>();
      row.put("id", new CellData(executionId));
      row.put(TASK_IDE, new CellData(taskId));
      row.put("executionId", new CellData(executionId));
      dataList.addRow(row);
    }
    return dataList;
  }

  private DataList averageTimeDataList(int averageTime) {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("averageTime", new CellData(averageTime));
    dataList.addRow(row);
    return dataList;
  }

  private MaintainQuery maintainQuery(MaintainQuery query, Variable... variables) {
    query.setVariableDefinitionList(List.of(variables));
    return query;
  }

  private Variable variable(String name, String type) {
    return new Variable().setName(name).setType(type);
  }
}
