package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests of the execution list of all tasks and the execution counters of the activity screen
 * ({@link TaskExecutionPresenter#getExecutionList}, {@link TaskExecutionPresenter#getExecutionCounters}).
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionPresenterActivityTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
  private static final String QUERY = "getActivityExecutionRows";
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
  void initPresenter() {
    presenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore, executionRepository,
      deferredRepository, serviceConfig, Clock.fixed(NOW, ZoneOffset.UTC));
    lenient().when(queryUtil.getParameters(any(), any(), any())).thenAnswer(call -> JsonNodeFactory.instance.objectNode());
    lenient().when(executionLogStore.locatorNode(any())).thenAnswer(call ->
      JsonNodeFactory.instance.objectNode().put("value", call.getArgument(0, ExecutionKey.class).mdcKey()));
  }

  @Test
  void eachRowIsDecoratedWithTheAverageOfItsOwnTaskReadOnceForTheTasksOfThePage() throws Exception {
    given(serviceConfig.getLocale("PARAMETER_TASK")).willReturn("Task");
    givenRows(
      row(1, 7, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "admin"),
      row(2, 3, TaskStatus.JOB_RUNNING, SCHEDULED_GROUP, NOW.minusSeconds(60), null, "scheduler"),
      row(2, 2, TaskStatus.JOB_OK, DEPENDENCY_GROUP, NOW.minusSeconds(600), 65_000, "Parent"));
    given(executionRepository.getAverageTimes(Set.of(1, 2))).willReturn(Map.of(1, 120_000L, 2, 240_000L));

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals("{\"value\":50,\"label\":\"50%\"}", rows.get(0).get("ExePrg").getObjectValue().toString());
    assertEquals("{\"value\":25,\"label\":\"25%\"}", rows.get(1).get("ExePrg").getObjectValue().toString());
    assertTrue(rows.get(2).get("ExePrg").isEmpty());
    assertEquals(TimeUtil.formatTime(60_000, false), rows.get(0).get("ExeTim").getStringValue());
    assertEquals(TimeUtil.formatTime(65_000), rows.get(2).get("ExeTim").getStringValue());
    assertEquals("admin", rows.get(0).get("LchBy").getStringValue());
    assertEquals("Task Parent", rows.get(2).get("LchBy").getStringValue());
    assertEquals("2-3", ((ObjectNode) rows.get(1).get("ExecutionLogPath").getObjectValue()).get("value").asString());
    verify(executionRepository, times(1)).getAverageTimes(any());
    verify(executionRepository, never()).getAverageTime(anyInt());
  }

  @Test
  void anEmptyPageReadsNoAverage() throws Exception {
    givenRows();

    presenter.getExecutionList(new ExecutionListCriteria());

    verify(executionRepository, never()).getAverageTimes(any());
  }

  @Test
  void aRunningExecutionWaitingForAConfirmationShowsTheWaitingStatusInsteadOfRunning() throws Exception {
    given(queryService.findLabel("ActStaIco", "deferred")).willReturn("lucide:hourglass");
    given(queryService.findLabel("ActStaSty", "deferred")).willReturn("text-warning");
    given(queryService.findLabel("ActStaTyp", "deferred")).willReturn("SCREEN_TEXT_ACTIVITY_WAITING");
    given(queryService.findLabel("ActStaTon", "deferred")).willReturn("pending");
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_WAITING")).willReturn("Esperando confirmación");
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_DUE_SHORT", "13:45")).willReturn("vence 13:45");
    Map<String, CellData> waiting = row(1, 7, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    waiting.put("DfrExe", new CellData(7));
    waiting.put("ExeDue", new CellData(Date.from(LocalDateTime.of(2026, 10, 15, 13, 45)
      .atZone(ZoneId.systemDefault()).toInstant())));
    Map<String, CellData> running = row(1, 5, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    givenRows(waiting, running);

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals("{\"icon\":\"lucide:hourglass\",\"style\":\"text-warning\",\"value\":\"4\","
      + "\"label\":\"Esperando confirmación\"}", rows.get(0).get("ExeStaIco").getObjectValue().toString());
    assertEquals("{\"icon\":\"lucide:circle-play\"}", rows.get(1).get("ExeStaIco").getObjectValue().toString(),
      "a running execution keeps the status of the query");
  }

  @Test
  void aWaitingExecutionIsAPendingStatusWithItsDueTimeUnderItAndTheOthersHaveNoDueLine() throws Exception {
    // Built first: its lenient stub of the deferred labels is replaced by the stubs below
    Map<String, CellData> waiting = waitingRow(7, LocalDateTime.of(2026, 10, 15, 13, 45));
    given(queryService.findLabel("ActStaIco", "deferred")).willReturn("lucide:hourglass");
    given(queryService.findLabel("ActStaSty", "deferred")).willReturn("text-warning");
    given(queryService.findLabel("ActStaTyp", "deferred")).willReturn("SCREEN_TEXT_ACTIVITY_WAITING");
    given(queryService.findLabel("ActStaTon", "deferred")).willReturn("pending");
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_WAITING")).willReturn("Esperando confirmación");
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_DUE_SHORT", "13:45")).willReturn("vence 13:45");
    Map<String, CellData> running = row(1, 5, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    running.put("ExeStaTxt", new CellData("En ejecución"));
    running.put("ExeStaIcn", new CellData("lucide:circle-play"));
    running.put("ExeStaTon", new CellData("info"));
    givenRows(waiting, running);

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals("Esperando confirmación", rows.get(0).get("ExeStaTxt").getStringValue());
    assertEquals("lucide:hourglass", rows.get(0).get("ExeStaIcn").getStringValue());
    assertEquals("pending", rows.get(0).get("ExeStaTon").getStringValue());
    assertEquals("vence 13:45", rows.get(0).get("ExeDueLbl").getStringValue());
    // A running execution keeps the status of the query and has no due line
    assertEquals("En ejecución", rows.get(1).get("ExeStaTxt").getStringValue());
    assertEquals("lucide:circle-play", rows.get(1).get("ExeStaIcn").getStringValue());
    assertEquals("info", rows.get(1).get("ExeStaTon").getStringValue());
    assertTrue(rows.get(1).get("ExeDueLbl").isEmpty());
  }

  @Test
  void everyOriginHasTheSameNeutralTone() throws Exception {
    givenRows(
      row(1, 1, TaskStatus.JOB_OK, SCHEDULED_GROUP, NOW, 1, "a"),
      row(1, 2, TaskStatus.JOB_OK, MANUAL_GROUP, NOW, 1, "a"),
      row(1, 3, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW, 1, "a"),
      row(1, 4, TaskStatus.JOB_OK, DEPENDENCY_GROUP, NOW, 1, "a"));

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals(List.of("neutral", "neutral", "neutral", "neutral"), cells(rows, "ExeOriTon"));
  }

  @Test
  void theFlowRunShowsItsRootExecutionOrAShortIdAndKeepsTheRunIdAsValue() throws Exception {
    String runId = "0f8fad5b-d9cb-469f-a165-70867728950e";
    Map<String, CellData> withRoot = row(1, 7, TaskStatus.JOB_OK, DEPENDENCY_GROUP, NOW, 1, "a");
    withRoot.put("ExeRun", new CellData(runId));
    withRoot.put("RunRooNam", new CellData("Nightly load"));
    withRoot.put("RunRooExe", new CellData(12));
    Map<String, CellData> withoutRoot = row(1, 6, TaskStatus.JOB_OK, MANUAL_GROUP, NOW, 1, "a");
    withoutRoot.put("ExeRun", new CellData(runId));
    withoutRoot.put("RunRooNam", new CellData());
    withoutRoot.put("RunRooExe", new CellData());
    Map<String, CellData> withoutRun = row(1, 5, TaskStatus.JOB_OK, MANUAL_GROUP, NOW, 1, "a");
    withoutRun.put("ExeRun", new CellData());
    givenRows(withRoot, withoutRoot, withoutRun);

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    // The link shows the short root execution (#12) and its tooltip the whole run (or the whole id without root)
    assertEquals("{\"value\":\"" + runId + "\",\"label\":\"#12\",\"title\":\"Nightly load #12\"}",
      rows.get(0).get("ExeRun").getObjectValue().toString());
    assertEquals("{\"value\":\"" + runId + "\",\"label\":\"0f8fad5b\",\"title\":\"" + runId + "\"}",
      rows.get(1).get("ExeRun").getObjectValue().toString());
    assertTrue(rows.get(2).get("ExeRun").isEmpty());
    // The side panel reads the label of the run as a plain value, a dash without run
    assertEquals(List.of("Nightly load #12", "0f8fad5b", "\u2014"),
      rows.stream().map(row -> row.get("ExeRunLbl").getStringValue()).toList());
    for (Map<String, CellData> row : rows) {
      assertFalse(row.containsKey("RunRooNam") || row.containsKey("RunRooExe"), "internal keys are not exposed");
    }
  }

  @Test
  void onlyRunningExecutionsWithADeferredRowAreWaitingAndCarryTheirDueDate() throws Exception {
    Date due = Date.from(LocalDateTime.of(2026, 10, 15, 13, 45).atZone(ZoneId.systemDefault()).toInstant());
    Map<String, CellData> waiting = row(1, 7, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    waiting.put("DfrExe", new CellData(7));
    waiting.put("ExeDue", new CellData(due));
    Map<String, CellData> closed = row(1, 6, TaskStatus.JOB_OK, MANUAL_GROUP, NOW.minusSeconds(600), 1_000, "a");
    closed.put("DfrExe", new CellData(6));
    closed.put("ExeDue", new CellData(due));
    Map<String, CellData> running = row(1, 5, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    givenRows(waiting, closed, running);

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals(1, rows.get(0).get("ExeDfr").getIntegerValue());
    assertFalse(rows.get(0).containsKey("DfrExe"), "the internal deferred key is not exposed");
    assertEquals(due, rows.get(0).get("ExeDue").getDateValue());
    assertEquals("13:45", rows.get(0).get("ExeDueTim").getStringValue());
    for (Map<String, CellData> row : List.of(rows.get(1), rows.get(2))) {
      assertEquals(0, row.get("ExeDfr").getIntegerValue());
      assertTrue(row.get("ExeDue").isEmpty());
      assertTrue(row.get("ExeDueTim").isEmpty());
    }
  }

  @Test
  void aDueDateOnAnotherDayShowsItsDayBeforeItsTime() throws Exception {
    Map<String, CellData> tomorrow = waitingRow(7, LocalDateTime.of(2026, 10, 16, 9, 30));
    Map<String, CellData> nextYear = waitingRow(6, LocalDateTime.of(2027, 1, 2, 8, 0));
    Map<String, CellData> today = waitingRow(5, LocalDateTime.of(2026, 10, 15, 23, 59));
    givenRows(tomorrow, nextYear, today);

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals(List.of("16/10 09:30", "02/01/2027 08:00", "23:59"),
      rows.stream().map(row -> row.get("ExeDueTim").getStringValue()).toList());
  }

  @Test
  void theOriginOfEachRowIsDerivedFromItsLaunchGroup() throws Exception {
    givenRows(
      row(1, 1, TaskStatus.JOB_OK, SCHEDULED_GROUP, NOW, 1, "a"),
      row(1, 2, TaskStatus.JOB_OK, MANUAL_GROUP, NOW, 1, "a"),
      row(1, 3, TaskStatus.JOB_OK, FILE_TRACKING_GROUP, NOW, 1, "a"),
      row(1, 4, TaskStatus.JOB_OK, DEPENDENCY_GROUP, NOW, 1, "a"),
      row(1, 5, TaskStatus.JOB_OK, "OTHER", NOW, 1, "a"));

    List<Map<String, CellData>> rows = presenter.getExecutionList(new ExecutionListCriteria()).getDataList().getRows();

    assertEquals(List.of("scheduled", "manual", "file", "dependency", ""),
      rows.stream().map(row -> row.get("ExeOri").getStringValue()).toList());
  }

  @Test
  void theCriteriaBecomeTheParametersOfThePageQuery() throws Exception {
    givenRows();
    ExecutionListCriteria criteria = new ExecutionListCriteria()
      .setTaskName("nightly")
      .setStatuses(List.of(4, 1))
      .setOrigins(List.of("scheduled", "dependency"))
      .setDeferred(1)
      .setStartFrom("01/10/2026")
      .setStartTo("15/10/2026")
      .setWithinHours(24)
      .setPage(2)
      .setMax(25);

    presenter.getExecutionList(criteria);

    verify(queryUtil).getParameters(null, "2", "25");
    ObjectNode parameters = launchedParameters().get(0);
    assertEquals("nightly", parameters.get("exeTaskName").asString());
    assertEquals("[4,1]", parameters.get("exeStatuses").toString());
    assertEquals("[\"SCHEDULED_TASK\",\"DEPENDANT_TASK\"]", parameters.get("exeGroups").toString());
    assertEquals(TaskStatus.JOB_RUNNING.getValue(), parameters.get("exeRunningStatus").asInt());
    assertEquals(-1, parameters.get("exeWaiting").asInt());
    assertFalse(parameters.has("exeNotWaiting"));
    assertEquals("01/10/2026 00:00:00.000", parameters.get("exeStartFrom").asString());
    assertEquals("16/10/2026 00:00:00.000", parameters.get("exeStartBefore").asString());
    assertEquals(TIMESTAMP.format(NOW.minusSeconds(24 * 3600L).atZone(ZoneId.systemDefault())),
      parameters.get("exeStartSince").asString());
  }

  @Test
  void anEmptyCriteriaReadsEveryExecutionWithoutFilters() throws Exception {
    givenRows();

    presenter.getExecutionList(new ExecutionListCriteria().setDeferred(0));

    verify(queryUtil).getParameters(null, "1", "50");
    ObjectNode parameters = launchedParameters().get(0);
    assertEquals(-1, parameters.get("exeNotWaiting").asInt());
    assertEquals(TaskStatus.JOB_RUNNING.getValue(), parameters.get("exeRunningStatus").asInt());
    for (String filter : new String[]{"exeTaskName", "exeStatuses", "exeGroups", "exeWaiting", "exeStartFrom",
      "exeStartBefore", "exeStartSince"}) {
      assertFalse(parameters.has(filter), filter);
    }
  }

  @Test
  void withoutMaxThePageHasTheDefaultSizeAndUpToTheCapIsAccepted() throws Exception {
    givenRows();

    presenter.getExecutionList(new ExecutionListCriteria());
    presenter.getExecutionList(new ExecutionListCriteria().setPage(3).setMax(500));

    verify(queryUtil).getParameters(null, "1", "50");
    verify(queryUtil).getParameters(null, "3", "500");
  }

  @Test
  void aMaxOfZeroOrAboveTheCapIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setMax(0), "max", "0");
    assertRejected(new ExecutionListCriteria().setMax(501), "max", "501");
  }

  @Test
  void anUnknownOriginIsRejectedInsteadOfWideningTheList() throws Exception {
    assertRejected(new ExecutionListCriteria().setOrigins(List.of("api")), "origins", "api");
    assertRejected(new ExecutionListCriteria().setOrigins(List.of("api", "manual")), "origins", "api");
  }

  @Test
  void aStartDayThatIsNotAWebDateIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setStartFrom("2019-02-02"), "startFrom", "2019-02-02");
    assertRejected(new ExecutionListCriteria().setStartTo("31/02/2019"), "startTo", "31/02/2019");
  }

  @Test
  void aDeferredValueOtherThanZeroOrOneIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setDeferred(2), "deferred", "2");
  }

  @Test
  void anUnknownStatusIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setStatuses(List.of(4, 99)), "statuses", "99");
  }

  @Test
  void aWindowOrAPageOutOfRangeIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setWithinHours(0), "withinHours", "0");
    assertRejected(new ExecutionListCriteria().setPage(0), "page", "0");
    assertRejected(new ExecutionListCriteria().setMax(-1), "max", "-1");
  }

  @ParameterizedTest
  @CsvSource({"running, [4], exeNotWaiting", "waiting, [4], exeWaiting", "queued, [5], ", "error, [1], ",
    "ok, [0], "})
  void theStateQuickFilterBecomesTheStatusAndDeferredFilterOfItsOption(String state, String statuses, String deferred)
    throws Exception {
    givenRows();

    presenter.getExecutionList(new ExecutionListCriteria().setState(state));

    ObjectNode parameters = launchedParameters().get(0);
    assertEquals(statuses, parameters.get("exeStatuses").toString());
    assertEquals("exeWaiting".equals(deferred), parameters.has("exeWaiting"), "exeWaiting");
    assertEquals("exeNotWaiting".equals(deferred), parameters.has("exeNotWaiting"), "exeNotWaiting");
    assertEquals(deferred != null, parameters.has("exeRunningStatus"), "exeRunningStatus");
  }

  @Test
  void aBlankStateIsTheAllOptionAndFiltersNothing() throws Exception {
    givenRows();

    presenter.getExecutionList(new ExecutionListCriteria().setState("").setStatuses(List.of(1)));

    ObjectNode parameters = launchedParameters().get(0);
    assertEquals("[1]", parameters.get("exeStatuses").toString());
    assertFalse(parameters.has("exeRunningStatus"));
  }

  @Test
  void anUnknownStateIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setState("api"), "state", "api");
  }

  @Test
  void aStateTogetherWithStatusesOrTheDeferredFilterIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setState("error").setStatuses(List.of(0)), "state", "error");
    assertRejected(new ExecutionListCriteria().setState("running").setDeferred(1), "state", "running");
  }

  @Test
  void theStateCountsCountEveryOptionWithTheSearchOriginAndDatesOfTheListButNotItsStateNorItsPage() throws Exception {
    given(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class))).willAnswer(call -> {
      ObjectNode parameters = call.getArgument(1);
      String statuses = parameters.has("exeStatuses") ? parameters.get("exeStatuses").toString() : "";
      long records = switch (statuses) {
        case "" -> 20;
        case "[4]" -> parameters.has("exeWaiting") ? 2 : 3;
        case "[5]" -> 4;
        case "[1]" -> 5;
        case "[0]" -> 6;
        default -> 99;
      };
      DataList dataList = new DataList();
      dataList.setRecords(records);
      return new ServiceData().setDataList(dataList);
    });
    ExecutionListCriteria criteria = new ExecutionListCriteria().setTaskName("nightly").setOrigins(List.of("manual"))
      .setStartFrom("01/10/2026").setStartTo("15/10/2026").setWithinHours(24).setState("error").setPage(3).setMax(10);

    List<Map<String, CellData>> rows = presenter.getExecutionStateCounts(criteria).getDataList().getRows();

    assertEquals(List.of("", "running", "waiting", "queued", "error", "ok"), cells(rows, "value"));
    assertEquals(List.of("SCREEN_TEXT_ACTIVITY_ALL", "ENUM_STATUS_RUNNING", "SCREEN_TEXT_ACTIVITY_WAITING",
      "ENUM_STATUS_QUEUED", "ENUM_STATUS_ERROR", "ENUM_STATUS_OK"), cells(rows, "label"));
    assertEquals(List.of(20L, 3L, 2L, 4L, 5L, 6L), rows.stream().map(row -> row.get("count").getObjectValue()).toList());
    assertEquals(List.of("", "", "warning", "", "danger", ""), cells(rows, "tone"));
    verify(queryUtil, times(6)).getParameters(null, "1", "1");
    for (ObjectNode parameters : launchedParameters()) {
      assertEquals("nightly", parameters.get("exeTaskName").asString());
      assertEquals("[\"MANUAL_TASK\"]", parameters.get("exeGroups").toString());
      assertEquals("01/10/2026 00:00:00.000", parameters.get("exeStartFrom").asString());
      assertEquals("16/10/2026 00:00:00.000", parameters.get("exeStartBefore").asString());
      assertTrue(parameters.has("exeStartSince"));
    }
  }

  @Test
  void theTaskFilterKeepsTheExecutionsOfOneTaskInTheListAndInTheStateCounts() throws Exception {
    givenRows();
    ExecutionListCriteria criteria = new ExecutionListCriteria().setTaskId(13);

    presenter.getExecutionList(criteria);
    presenter.getExecutionStateCounts(criteria);

    List<ObjectNode> launched = launchedParameters();
    assertEquals(7, launched.size());
    launched.forEach(parameters -> assertEquals("[13]", parameters.get("exeTaskIds").toString()));
  }

  @Test
  void aTaskFilterBelowOneIsRejected() throws Exception {
    assertRejected(new ExecutionListCriteria().setTaskId(0), "taskId", "0");
  }

  @Test
  void theStateCountsRejectAnInvalidFilterOfTheList() throws Exception {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setOrigins(List.of("api"));

    assertRejected(() -> presenter.getExecutionStateCounts(criteria), "origins", "api");
  }

  /**
   * An invalid filter value raises the translated validation error and reads no execution
   */
  private void assertRejected(ExecutionListCriteria criteria, String filter, String value) throws Exception {
    assertRejected(() -> presenter.getExecutionList(criteria), filter, value);
  }

  /**
   * An invalid filter value raises the translated validation error and reads no execution
   */
  private void assertRejected(Executable call, String filter, String value) throws Exception {
    given(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_TITLE")).willReturn("Invalid filter");
    given(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", filter, value))
      .willReturn("Invalid " + filter + " " + value);

    InvalidExecutionFilterException error = assertThrows(InvalidExecutionFilterException.class, call);

    assertEquals("Invalid filter", error.getTitle());
    assertEquals("Invalid " + filter + " " + value, error.getMessage());
    verify(queryService, never()).launchPrivateQuery(eq(QUERY), any(ObjectNode.class));
  }

  @Test
  void countersCountRunningWaitingQueuedAndTheErrorsOfTheLast24Hours() throws Exception {
    given(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class))).willAnswer(call -> {
      ObjectNode parameters = call.getArgument(1);
      String statuses = parameters.get("exeStatuses").toString();
      long records = switch (statuses) {
        case "[4]" -> parameters.has("exeWaiting") ? 2 : 3;
        case "[5]" -> 4;
        case "[1]" -> 5;
        default -> 99;
      };
      DataList dataList = new DataList();
      dataList.setRecords(records);
      return new ServiceData().setDataList(dataList);
    });

    Map<String, CellData> counters = presenter.getExecutionCounters().getDataList().getRows().get(0);

    assertEquals(3L, counters.get("running").getObjectValue());
    assertEquals(2L, counters.get("waiting").getObjectValue());
    assertEquals(4L, counters.get("queued").getObjectValue());
    assertEquals(5L, counters.get("errorsLast24h").getObjectValue());
    verify(queryUtil, times(4)).getParameters(null, "1", "1");
    ObjectNode errors = launchedParameters().stream()
      .filter(parameters -> "[1]".equals(parameters.get("exeStatuses").toString())).findFirst().orElseThrow();
    assertEquals(TIMESTAMP.format(NOW.minusSeconds(24 * 3600L).atZone(ZoneId.systemDefault())),
      errors.get("exeStartSince").asString());
    ObjectNode running = launchedParameters().stream()
      .filter(parameters -> parameters.has("exeNotWaiting")).findFirst().orElseThrow();
    assertEquals("[4]", running.get("exeStatuses").toString());
  }

  @Test
  void countersOfATaskNameCountOnlyTheExecutionsOfThoseTasks() throws Exception {
    given(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));

    presenter.getExecutionCounters("activity probe");

    List<ObjectNode> launched = launchedParameters();
    assertEquals(4, launched.size());
    launched.forEach(parameters -> assertEquals("activity probe", parameters.get("exeTaskName").asString()));
  }

  private static List<String> cells(List<Map<String, CellData>> rows, String column) {
    return rows.stream().map(row -> row.get(column).getStringValue()).toList();
  }

  /**
   * Running execution waiting for a confirmation until a due date (local time)
   */
  private Map<String, CellData> waitingRow(int executionId, LocalDateTime due) throws Exception {
    lenient().when(queryService.findLabel(any(), eq("deferred"))).thenReturn("x");
    Map<String, CellData> row = row(1, executionId, TaskStatus.JOB_RUNNING, MANUAL_GROUP, NOW.minusSeconds(60), null, "a");
    row.put("DfrExe", new CellData(executionId));
    row.put("ExeDue", new CellData(Date.from(due.atZone(ZoneId.systemDefault()).toInstant())));
    return row;
  }

  @SafeVarargs
  private void givenRows(Map<String, CellData>... rows) throws Exception {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(row);
    }
    given(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
  }

  private List<ObjectNode> launchedParameters() throws Exception {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, org.mockito.Mockito.atLeastOnce()).launchPrivateQuery(eq(QUERY), captor.capture());
    return new ArrayList<>(captor.getAllValues());
  }

  private Map<String, CellData> row(int taskId, int executionId, TaskStatus status, String group, Instant start,
                                    Integer executionTime, String launchedBy) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(taskId));
    row.put("ExeTsk", new CellData(executionId));
    row.put("Sta", new CellData(status.getValue()));
    row.put("TypLch", new CellData(group));
    row.put("LchBy", new CellData(launchedBy));
    row.put("ExeLstTim", new CellData(Date.from(start)));
    row.put("ExeTim", executionTime == null ? new CellData() : new CellData(executionTime));
    row.put("ExeStaIco", new CellData(JsonNodeFactory.instance.objectNode().put("icon", "lucide:circle-play")));
    return row;
  }
}
