package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.ExecutionSort;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Executions of the activity screen grouped by flow run ({@link TaskExecutionPresenter#getRunList}): one top-level row
 * per flow run with its aggregated status, start and duration, its executions (and the tasks it skipped) as its
 * children, and the executions without a run as top-level leaves; the page, the sort and the filters apply to the
 * top-level rows
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionPresenterRunListTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
  private static final String GROUPS = "getActivityRunGroups";
  private static final String ROWS = "getActivityExecutionRows";
  private static final String SKIPPED = "getActivityRunSkippedRows";
  private static final String RUN = "00000000-0000-4000-8000-000000004812";
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private TaskExecutionRepository executionRepository;
  @Mock
  private ServiceConfig serviceConfig;
  @Mock
  private DeferredExecutionRepository deferredRepository;

  private TaskExecutionPresenter presenter;

  @BeforeEach
  void initPresenter() throws Exception {
    presenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore, executionRepository,
      deferredRepository, serviceConfig, Clock.fixed(NOW, ZoneOffset.UTC));
    lenient().when(queryUtil.getParameters(any(), any(), any())).thenAnswer(call -> JsonNodeFactory.instance.objectNode());
    lenient().when(executionLogStore.locatorNode(any())).thenAnswer(call ->
      JsonNodeFactory.instance.objectNode().put("value", call.getArgument(0, ExecutionKey.class).mdcKey()));
    // Labels of the status groups and locales as their own keys, so the rows show which ones they use
    lenient().when(queryService.findLabel(anyString(), anyString()))
      .thenAnswer(call -> call.getArgument(0) + ":" + call.getArgument(1));
    lenient().when(serviceConfig.getLocale(anyString())).thenAnswer(call -> call.getArgument(0));
    lenient().when(serviceConfig.getLocale(anyString(), any(String[].class))).thenAnswer(call ->
      call.getArgument(0) + Arrays.toString(Arrays.copyOfRange(call.getArguments(), 1, call.getArguments().length)));
    lenient().when(queryService.launchPrivateQuery(eq(SKIPPED), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
  }

  @Test
  void aRunIsATopLevelRowWithItsRootItsAggregatedStatusAndItsExecutionsAsChildrenInStartOrder() throws Exception {
    givenGroups(5, runGroup(RUN, 12, 7, "Import statements"));
    givenRunExecutions(
      execution(14, 5, TaskStatus.JOB_RUNNING, DEPENDENCY_GROUP, NOW.minusSeconds(60), null, "12", RUN),
      execution(12, 7, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW.minusSeconds(600), 72_000, "/in/n43/a.n43", RUN),
      execution(13, 6, TaskStatus.JOB_WARNING, DEPENDENCY_GROUP, NOW.minusSeconds(500), 220_000, "12", RUN));

    DataList page = presenter.getRunList(new ExecutionListCriteria()).getDataList();
    List<Map<String, CellData>> rows = page.getRows();

    assertEquals(4, rows.size());
    Map<String, CellData> run = rows.get(0);
    assertEquals(RUN, run.get("id").getStringValue());
    assertEquals("", run.get("RunPar").getStringValue());
    assertEquals(0, run.get("RunLeaf").getIntegerValue());
    assertEquals("Import statements #7", run.get("TskNam").getStringValue());
    assertEquals(3, run.get("RunExeCnt").getIntegerValue());
    assertEquals("SCREEN_TEXT_ACTIVITY_RUN_EXECUTIONS[3]", run.get("RunExeTxt").getStringValue());
    // Shown under its status pill
    assertEquals("SCREEN_TEXT_ACTIVITY_RUN_EXECUTIONS[3]", run.get("ExeDueLbl").getStringValue());
    assertEquals("4", run.get("Sta").getStringValue());
    assertEquals("ActStaTyp:4", run.get("ExeStaTxt").getStringValue());
    assertEquals("ActStaIco:4", run.get("ExeStaIcn").getStringValue());
    assertEquals("ActStaTon:4", run.get("ExeStaTon").getStringValue());
    // Started by its first execution, running for the time since then
    assertEquals(Date.from(NOW.minusSeconds(600)), run.get("ExeLstTim").getDateValue());
    assertEquals(TimeUtil.formatTime(600_000, false), run.get("ExeTim").getStringValue());
    assertEquals(600, run.get("ExeEla").getIntegerValue());
    // Origin and launcher of its root execution, and the link to the run
    assertEquals("file", run.get("ExeOri").getStringValue());
    assertEquals("/in/n43/a.n43", run.get("LchBy").getStringValue());
    assertEquals(RUN, ((ObjectNode) run.get("ExeRun").getObjectValue()).get("value").asString());
    assertEquals("Import statements #7", run.get("ExeRunLbl").getStringValue());
    // No log of its own: its log button is hidden
    assertEquals("{\"value\":\"\",\"style\":\"hidden\"}", run.get("ExecutionLogPath").getObjectValue().toString());
    assertTrue(run.get("ExePrg").isEmpty());
    // Its executions as its children, first started first, decorated as the rows of the flat list
    assertEquals(List.of("12-7", "13-6", "14-5"), cells(rows.subList(1, 4), "id"));
    rows.subList(1, 4).forEach(child -> {
      assertEquals(RUN, child.get("RunPar").getStringValue());
      assertEquals(1, child.get("RunLeaf").getIntegerValue());
    });
    assertEquals("12-7", ((ObjectNode) rows.get(1).get("ExecutionLogPath").getObjectValue()).get("value").asString());
    assertEquals("PARAMETER_TASK 12", rows.get(3).get("LchBy").getStringValue());
    assertEquals("{\"value\":100,\"style\":\"progress-bar-danger\",\"label\":\"\"}",
      rows.get(3).get("ExePrg").getObjectValue().toString());
    // The page counts the top-level rows
    assertEquals(5L, page.getRecords());
  }

  @ParameterizedTest
  @CsvSource({
    "'0,2,0', false, 2",
    "'0,0', true, 2",
    "'0,7', false, 0",
    "'0,1,2', false, 1",
    "'0,3', false, 1",
    "'0,6', true, 1",
    "'1,4', false, 4",
    "'0,5', false, 5"})
  void theStatusOfARunIsRunningThenQueuedThenErrorThenWarningThenOk(String statuses, boolean skipped, int expected)
    throws Exception {
    givenGroups(1, runGroup(RUN, 12, 1, "Import"));
    List<Map<String, CellData>> executions = new ArrayList<>();
    int executionId = 1;
    for (String status : statuses.split(",")) {
      executions.add(execution(12, executionId, TaskStatus.valueOf(Integer.parseInt(status)), MANUAL_GROUP,
        NOW.minusSeconds(100L - executionId), 1_000, "a", RUN));
      executionId++;
    }
    givenRunExecutions(executions.toArray(new Map[0]));
    if (skipped) {
      givenSkipped(skippedTask(RUN, 15, "Daily report"));
    }

    Map<String, CellData> run = presenter.getRunList(new ExecutionListCriteria()).getDataList().getRows().get(0);

    assertEquals(String.valueOf(expected), run.get("Sta").getStringValue());
  }

  @Test
  void anEndedRunLastsFromItsFirstStartToItsLastEndAndListsTheTasksItSkipped() throws Exception {
    givenGroups(1, runGroup(RUN, 12, 6, "Import"));
    givenRunExecutions(
      execution(12, 6, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW.minusSeconds(1000), 65_000, "/in/b.n43", RUN),
      execution(13, 4, TaskStatus.JOB_ERROR, DEPENDENCY_GROUP, NOW.minusSeconds(930), 95_000, "12", RUN));
    givenSkipped(skippedTask(RUN, 15, "Daily report"));

    List<Map<String, CellData>> rows = presenter.getRunList(new ExecutionListCriteria()).getDataList().getRows();

    Map<String, CellData> run = rows.get(0);
    assertEquals("1", run.get("Sta").getStringValue());
    assertEquals(TimeUtil.formatTime(165_000), run.get("ExeTim").getStringValue());
    assertEquals(165, run.get("ExeEla").getIntegerValue());
    assertEquals(2, run.get("RunExeCnt").getIntegerValue());
    assertEquals("SCREEN_TEXT_ACTIVITY_RUN_EXECUTIONS_SKIPPED[2, 1]", run.get("RunExeTxt").getStringValue());
    // The skipped task is the last child: skipped status, no execution, no log
    assertEquals(4, rows.size());
    Map<String, CellData> skipped = rows.get(3);
    assertEquals(RUN + "/15", skipped.get("id").getStringValue());
    assertEquals(RUN, skipped.get("RunPar").getStringValue());
    assertEquals("Daily report", skipped.get("TskNam").getStringValue());
    assertEquals(15, skipped.get("IdeTsk").getIntegerValue());
    assertEquals("ActStaTyp:skipped", skipped.get("ExeStaTxt").getStringValue());
    assertEquals("ActStaIco:skipped", skipped.get("ExeStaIcn").getStringValue());
    assertEquals("ActStaTon:skipped", skipped.get("ExeStaTon").getStringValue());
    assertTrue(skipped.get("ExeTsk").isEmpty());
    assertEquals("{\"value\":\"\",\"style\":\"hidden\"}", skipped.get("ExecutionLogPath").getObjectValue().toString());
  }

  @Test
  void anExecutionWithoutRunIsATopLevelLeafDecoratedAsInTheFlatList() throws Exception {
    Map<String, CellData> withoutRun = runGroup(null, 18, 8, "Purge logs");
    withoutRun.put("ExeLstTim", new CellData(Date.from(NOW.minusSeconds(700))));
    givenGroups(2, withoutRun, runGroup(RUN, 12, 7, "Import"));
    givenRunExecutions(execution(12, 7, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW.minusSeconds(900), 1_000, "f", RUN));
    givenLegacyExecutions(
      execution(18, 8, TaskStatus.JOB_STOPPED, MANUAL_GROUP, NOW.minusSeconds(700), 12_000, "mgarcia", null),
      // Another execution of the same task in the window of the page: not a row of the page
      execution(18, 9, TaskStatus.JOB_OK, MANUAL_GROUP, NOW.minusSeconds(700), 12_000, "mgarcia", null));

    List<Map<String, CellData>> rows = presenter.getRunList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals(List.of("18-8", RUN, "12-7"), cells(rows, "id"));
    Map<String, CellData> legacy = rows.get(0);
    assertEquals("", legacy.get("RunPar").getStringValue());
    assertEquals(1, legacy.get("RunLeaf").getIntegerValue());
    assertEquals("mgarcia", legacy.get("LchBy").getStringValue());
    assertEquals("manual", legacy.get("ExeOri").getStringValue());
    assertEquals("18-8", ((ObjectNode) legacy.get("ExecutionLogPath").getObjectValue()).get("value").asString());
    ObjectNode legacyQuery = launched(ROWS).stream().filter(parameters -> parameters.has("exeNoRun")).findFirst()
      .orElseThrow();
    assertEquals("[18]", legacyQuery.get("exeTaskIds").toString());
    assertEquals(TIMESTAMP.format(NOW.minusSeconds(700).atZone(ZoneId.systemDefault())),
      legacyQuery.get("exeStartFrom").asString());
    assertEquals(TIMESTAMP.format(NOW.minusSeconds(700).atZone(ZoneId.systemDefault())),
      legacyQuery.get("exeStartUntil").asString());
  }

  @Test
  void thePageTheSortAndTheFiltersSelectTheTopLevelRowsAndTheChildrenAreAllTheExecutionsOfTheirRuns()
    throws Exception {
    givenGroups(1, runGroup(RUN, 12, 7, "Import"));
    givenRunExecutions(execution(12, 7, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW.minusSeconds(900), 1_000, "f", RUN));

    presenter.getRunList(new ExecutionListCriteria().setPage(2).setMax(25).setTaskName("Concil").setState("error")
      .setSort(List.of(new ExecutionSort("ExeStaTxt", "asc"))));

    verify(queryUtil).getParameters(null, "2", "25");
    ObjectNode groups = launched(GROUPS).get(0);
    assertEquals("Concil", groups.get("exeTaskName").asString());
    assertEquals("[1]", groups.get("exeStatuses").toString());
    assertEquals("[{\"id\":\"Sta\",\"direction\":\"ASC\"}]", groups.get("sort").toString());
    ObjectNode children = launched(ROWS).get(0);
    assertEquals("[\"" + RUN + "\"]", children.get("exeRunIds").toString());
    assertTrue(!children.has("exeTaskName") && !children.has("exeStatuses"), "every execution of the run");
  }

  @Test
  void withoutMaxAPageHasFiftyTopLevelRowsAndAnInvalidFilterIsRefusedBeforeAnyQuery() throws Exception {
    givenGroups(0);

    presenter.getRunList(new ExecutionListCriteria());

    verify(queryUtil).getParameters(null, "1", "50");
    verify(queryService, never()).launchPrivateQuery(eq(ROWS), any(ObjectNode.class));
    given(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", "max", "501")).willReturn("Invalid max");
    ExecutionListCriteria tooMany = new ExecutionListCriteria().setMax(501);
    InvalidExecutionFilterException error = assertThrows(InvalidExecutionFilterException.class,
      () -> presenter.getRunList(tooMany));
    assertEquals("Invalid max", error.getMessage());
    verify(queryService).launchPrivateQuery(eq(GROUPS), any(ObjectNode.class));
  }

  private static List<String> cells(List<Map<String, CellData>> rows, String column) {
    return rows.stream().map(row -> row.get(column).getStringValue()).toList();
  }

  @SafeVarargs
  private void givenGroups(long records, Map<String, CellData>... groups) throws Exception {
    DataList dataList = new DataList();
    for (Map<String, CellData> group : groups) {
      dataList.addRow(group);
    }
    dataList.setRecords(records);
    given(queryService.launchPrivateQuery(eq(GROUPS), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
  }

  @SafeVarargs
  private void givenRunExecutions(Map<String, CellData>... executions) throws Exception {
    lenient().when(queryService.launchPrivateQuery(eq(ROWS), argThat((ObjectNode parameters) -> parameters != null
      && parameters.has("exeRunIds")))).thenReturn(new ServiceData().setDataList(dataList(executions)));
  }

  @SafeVarargs
  private void givenLegacyExecutions(Map<String, CellData>... executions) throws Exception {
    given(queryService.launchPrivateQuery(eq(ROWS), argThat((ObjectNode parameters) -> parameters != null
      && parameters.has("exeNoRun")))).willReturn(new ServiceData().setDataList(dataList(executions)));
  }

  @SafeVarargs
  private void givenSkipped(Map<String, CellData>... tasks) throws Exception {
    given(queryService.launchPrivateQuery(eq(SKIPPED), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList(tasks)));
  }

  @SafeVarargs
  private static DataList dataList(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(row);
    }
    return dataList;
  }

  private List<ObjectNode> launched(String query) throws Exception {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, org.mockito.Mockito.atLeastOnce()).launchPrivateQuery(eq(query), captor.capture());
    return new ArrayList<>(captor.getAllValues());
  }

  /**
   * Top-level row of the page query: a flow run (with its root execution) or an execution without run
   */
  private static Map<String, CellData> runGroup(String runId, int taskId, int executionId, String taskName) {
    Map<String, CellData> row = new HashMap<>();
    row.put("ExeRun", runId == null ? new CellData() : new CellData(runId));
    row.put("IdeTsk", new CellData(taskId));
    row.put("ExeTsk", new CellData(executionId));
    row.put("TskNam", new CellData(taskName));
    return row;
  }

  private static Map<String, CellData> skippedTask(String runId, int taskId, String taskName) {
    Map<String, CellData> row = new HashMap<>();
    row.put("ExeRun", new CellData(runId));
    row.put("IdeTsk", new CellData(taskId));
    row.put("TskNam", new CellData(taskName));
    row.put("ExeEnd", new CellData(Date.from(NOW.minusSeconds(900))));
    return row;
  }

  private static Map<String, CellData> execution(int taskId, int executionId, TaskStatus status, String group,
                                                 Instant start, Integer executionTime, String launchedBy,
                                                 String runId) {
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(taskId + "-" + executionId));
    row.put("IdeTsk", new CellData(taskId));
    row.put("ExeTsk", new CellData(executionId));
    row.put("TskNam", new CellData("Task " + taskId));
    row.put("Sta", new CellData(status.getValue()));
    row.put("TypLch", new CellData(group));
    row.put("LchBy", new CellData(launchedBy));
    row.put("ExeLstTim", new CellData(Date.from(start)));
    row.put("ExeEnd", executionTime == null || status == TaskStatus.JOB_RUNNING ? new CellData()
      : new CellData(Date.from(start.plusMillis(executionTime))));
    row.put("ExeTim", executionTime == null ? new CellData() : new CellData(executionTime));
    row.put("ExeRun", runId == null ? new CellData() : new CellData(runId));
    row.put("ExeStaIco", new CellData(JsonNodeFactory.instance.objectNode().put("icon", "x")));
    return row;
  }
}
