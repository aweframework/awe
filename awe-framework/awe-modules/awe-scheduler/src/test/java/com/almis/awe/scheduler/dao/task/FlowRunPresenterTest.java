package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Flow run page ({@link FlowRunPresenter}) of a run that has not ended: the summary points to what the run would end
 * with, a task waiting for a confirmation is deferred with its deferred and due dates, and the edges of a parent that
 * has not ended are pending. The ended run and the SQL are the Spring test of awe-boot (SchedulerFlowRunTest)
 */
@ExtendWith(MockitoExtension.class)
class FlowRunPresenterTest {

  private static final String RUN = "00000000-0000-4000-8000-000000004812";
  private static final LocalDateTime TODAY = LocalDateTime.of(2026, 11, 17, 8, 0);
  private static final Clock CLOCK = Clock.fixed(TODAY.plusHours(2).atZone(ZoneId.systemDefault()).toInstant(),
    ZoneId.systemDefault());
  // Locale texts with parameters of the page (en-GB); any other key is its own text
  private static final Map<String, String> TEMPLATES = Map.of(
    "SCREEN_TEXT_FLOW_RUN_POINTS_RUNNING", "Right now it points to {0}: {1}.",
    "SCREEN_TEXT_FLOW_RUN_CAUSE_ENDED", "{0} ended in {1}",
    "SCREEN_TEXT_FLOW_RUN_JOINED_BY", "Joined by {0}",
    "SCREEN_TEXT_FLOW_RUN_JOIN_WAIT", "waited {0}",
    "SCREEN_TEXT_FLOW_RUN_FACT_START", "Started {0}",
    "SCREEN_TEXT_FLOW_RUN_FACT_END", "Ended {0}",
    "SCREEN_TEXT_FLOW_RUN_FACT_DURATION", "Duration {0}",
    "SCREEN_TEXT_FLOW_RUN_PARAMETERS_LINE", "Parameters: {0}");

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private FlowRunRepository flowRunRepository;
  @Mock
  private TaskExecutionPresenter executionPresenter;
  @Mock
  private DeferredExecutionRepository deferredRepository;
  @Mock
  private ServiceConfig serviceConfig;

  private FlowRunPresenter presenter;

  @BeforeEach
  void initPresenter() throws Exception {
    presenter = new FlowRunPresenter(queryService, queryUtil, flowRunRepository, executionPresenter, deferredRepository,
      serviceConfig, CLOCK);
    lenient().when(queryUtil.getParameters(any(), any(), any())).thenAnswer(call -> JsonNodeFactory.instance.objectNode());
    lenient().when(queryService.findLabel(anyString(), anyString()))
      .thenAnswer(call -> call.getArgument(0) + ":" + call.getArgument(1));
    lenient().when(serviceConfig.getLocale(anyString()))
      .thenAnswer(call -> TEMPLATES.getOrDefault(call.<String>getArgument(0), call.getArgument(0)));
    // As AweElements: the parameters are HTML-escaped before they are formatted
    lenient().when(serviceConfig.getLocale(anyString(), any(String[].class))).thenAnswer(call ->
      MessageFormat.format(TEMPLATES.getOrDefault(call.<String>getArgument(0), call.getArgument(0)),
        Arrays.stream(Arrays.copyOfRange(call.getArguments(), 1, call.getArguments().length))
          .map(parameter -> StringEscapeUtils.escapeHtml4((String) parameter)).toArray()));
    lenient().doAnswer(call -> {
      Map<String, CellData> row = call.getArgument(0);
      row.put("ExeStaTxt", new CellData("ActStaTyp:" + call.getArgument(1)));
      return null;
    }).when(executionPresenter).setStatus(any(), anyString());

    // 11 (root, ok) -> 12 (waits for a confirmation, due at 12:15) -> 13 (not reached yet)
    Map<String, CellData> header = row("IdeRun", RUN, "RunRooTsk", 11, "RunRooNam", "Import", "RunRooExe", 7,
      "RunLchBy", "scheduler", "RunIni", date(0), "RunSta", TaskStatus.JOB_RUNNING.getValue());
    lenient().when(queryService.launchPrivateQuery(eq("getFlowRunHeaderRow"), any(ObjectNode.class)))
      .thenReturn(rows(List.of(header)));
    lenient().when(flowRunRepository.getDependencies()).thenReturn(List.of(dependency(12, 11), dependency(13, 12),
      dependency(99, 98)));
    lenient().when(queryService.launchPrivateQuery(eq("getFlowRunGraphTasks"), any(ObjectNode.class))).thenReturn(rows(
      List.of(row("taskId", 11, "name", "Import", "onWarning", 0, "onError", 0),
        row("taskId", 12, "name", "Send SEPA", "onWarning", 1, "onError", 0),
        row("taskId", 13, "name", "Report", "onWarning", 0, "onError", 0))));
    lenient().when(executionPresenter.getRunExecutions(RUN)).thenReturn(List.of(
      row("IdeTsk", 11, "ExeTsk", 7, "Sta", TaskStatus.JOB_OK.getValue(), "ExeLstTim", date(0), "ExeEnd", date(1),
        "ExeDfr", 0, "LchBy", "scheduler"),
      row("IdeTsk", 12, "ExeTsk", 3, "Sta", TaskStatus.JOB_RUNNING.getValue(), "ExeLstTim", date(1), "ExeDfr", 1,
        "ExeDue", new CellData(Date.from(TODAY.plusHours(4).plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant())))));
    lenient().when(flowRunRepository.getJoins(RUN)).thenReturn(List.of(new JoinRow(12, "LAUNCHED", 3)));
    lenient().when(deferredRepository.find(12, 3)).thenReturn(new DeferredExecution()
      .setAcceptedDate(Date.from(TODAY.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant())));
  }

  @Test
  void aRunThatHasNotEndedRunsPointsToWhatItWouldEndWithAndCountsItsTasks() throws Exception {
    Map<String, CellData> summary = presenter.getSummary(RUN).getDataList().getRows().get(0);

    assertEquals("4", summary.get("Sta").getStringValue());
    assertEquals(0, summary.get("RunEnded").getIntegerValue());
    // Nothing ended in error or warning yet: it would end in OK
    assertEquals("Right now it points to ActStaTyp:0: SCREEN_TEXT_FLOW_RUN_CAUSE_NONE.",
      summary.get("RunPntTxt").getStringValue());
    assertEquals("Import #7", summary.get("RunLbl").getStringValue());
    assertEquals("17/11/2026 08:00:00", summary.get("startDate").getStringValue());
    assertEquals("7200000", summary.get("duration").getStringValue());
    assertEquals("1|1|1|1|3", List.of("RunCntOk", "RunCntDfr", "RunCntPnd", "RunTskEnd", "RunTskTot").stream()
      .map(field -> summary.get(field).getStringValue()).collect(Collectors.joining("|")));
  }

  @Test
  void aTaskWaitingForAConfirmationIsDeferredWithItsDatesAndTheEdgesAfterItArePending() throws Exception {
    Map<String, Map<String, CellData>> graph = presenter.getGraph(RUN).getDataList().getRows().stream()
      .collect(Collectors.toMap(row -> row.get("id").getStringValue(), row -> row));

    Map<String, CellData> waiting = graph.get("12-11");
    // The condition is the one of the options of its parent (OK only), not of its own; it runs until now: no end yet
    assertEquals("deferred|1|17/11/2026 08:15:00|17/11/2026 12:15:00||7140000|MET|SCREEN_TEXT_FLOW_RUN_CONDITION_OK",
      String.join("|", waiting.get("state").getStringValue(), waiting.get("deferred").getStringValue(),
        waiting.get("deferredDate").getStringValue(), waiting.get("dueDate").getStringValue(),
        waiting.get("endDate").getStringValue(), waiting.get("duration").getStringValue(),
        waiting.get("edgeState").getStringValue(), waiting.get("conditionLabel").getStringValue()));
    Map<String, CellData> notReached = graph.get("13-12");
    assertEquals("pending|PENDING|", String.join("|", notReached.get("state").getStringValue(),
      notReached.get("edgeState").getStringValue(), notReached.get("executionId").getStringValue()));
    assertEquals(RUN, graph.get("11-").get("rootRun").getStringValue());
  }

  @Test
  void theTextsNameTheTasksAsTheyAreAndAJoinClosedWithoutWaitingSaysNoWait() throws Exception {
    // 11 (ok) -> 12 (Conciliación & co, warning) and 14 (ok), both ended at 08:03 -> 15 joins them, running since then
    given(flowRunRepository.getDependencies()).willReturn(List.of(dependency(12, 11), dependency(14, 11),
      dependency(15, 12), dependency(15, 14)));
    given(queryService.launchPrivateQuery(eq("getFlowRunGraphTasks"), any(ObjectNode.class))).willReturn(rows(
      List.of(row("taskId", 11, "name", "Import", "onWarning", 0, "onError", 0),
        row("taskId", 12, "name", "Conciliación & co", "onWarning", 1, "onError", 0),
        row("taskId", 14, "name", "Report", "onWarning", 0, "onError", 0),
        row("taskId", 15, "name", "Close", "onWarning", 0, "onError", 0))));
    given(executionPresenter.getRunExecutions(RUN)).willReturn(List.of(
      row("IdeTsk", 11, "ExeTsk", 7, "Sta", TaskStatus.JOB_OK.getValue(), "ExeLstTim", date(0), "ExeEnd", date(1),
        "ExeDfr", 0, "LchBy", "scheduler"),
      row("IdeTsk", 12, "ExeTsk", 4, "Sta", TaskStatus.JOB_WARNING.getValue(), "ExeLstTim", date(1), "ExeEnd", date(3),
        "ExeDfr", 0),
      row("IdeTsk", 14, "ExeTsk", 5, "Sta", TaskStatus.JOB_OK.getValue(), "ExeLstTim", date(1), "ExeEnd", date(3),
        "ExeDfr", 0),
      row("IdeTsk", 15, "ExeTsk", 6, "Sta", TaskStatus.JOB_RUNNING.getValue(), "ExeLstTim", date(3), "ExeDfr", 0)));
    given(flowRunRepository.getJoins(RUN)).willReturn(List.of());

    Map<String, CellData> summary = presenter.getSummary(RUN).getDataList().getRows().get(0);
    Map<String, CellData> join = presenter.getGraph(RUN).getDataList().getRows().stream()
      .filter(row -> "15-12".equals(row.get("id").getStringValue())).findFirst().orElseThrow();

    assertEquals("Right now it points to ActStaTyp:2: Conciliación & co ended in ActStaTyp:2.",
      summary.get("RunPntTxt").getStringValue());
    assertEquals("12|Joined by Conciliación & co|0|", String.join("|", join.get("joinedBy").getStringValue(),
      join.get("joinedByText").getStringValue(), join.get("joinWait").getStringValue(),
      join.get("joinWaitText").getStringValue()));
  }

  @Test
  void theFactsOfTheHeaderAreItsPillsWithItsEndOnlyOnceItEnded() throws Exception {
    String running = presenter.getSummary(RUN).getDataList().getRows().get(0).get("RunFct").getStringValue();

    // The same run ended at 08:05 in OK, its root launched by a file
    given(queryService.launchPrivateQuery(eq("getFlowRunHeaderRow"), any(ObjectNode.class))).willReturn(rows(List.of(
      row("IdeRun", RUN, "RunRooTsk", 11, "RunRooNam", "Import", "RunRooExe", 7, "RunIni", date(0), "RunEnd", date(5),
        "RunSta", TaskStatus.JOB_OK.getValue()))));
    given(executionPresenter.getRunExecutions(RUN)).willReturn(List.of(
      row("IdeTsk", 11, "ExeTsk", 7, "Sta", TaskStatus.JOB_OK.getValue(), "ExeLstTim", date(0), "ExeEnd", date(1),
        "ExeDfr", 0, "ExeOriTxt", "File", "ExeOriIcn", "lucide:file", "LchBy", "/in/n43/bbva.n43")));
    Map<String, CellData> ended = presenter.getSummary(RUN).getDataList().getRows().get(0);

    // Running: who launched it, its start, its duration until now and its progress; no end yet
    assertEquals("[{\"label\":\"scheduler\",\"icon\":\"lucide:user\"},"
      + "{\"label\":\"Started 17/11/2026 08:00:00\",\"icon\":\"lucide:calendar-clock\"},"
      + "{\"label\":\"Duration 2h\",\"icon\":\"lucide:timer\"},"
      + "{\"label\":\"SCREEN_TEXT_FLOW_RUN_PROGRESS\",\"icon\":\"lucide:list-checks\"}]", running);
    // Ended: its origin with its launcher as code, and its end
    assertEquals("[{\"label\":\"File\",\"icon\":\"lucide:file\",\"code\":\"/in/n43/bbva.n43\"},"
      + "{\"label\":\"Started 17/11/2026 08:00:00\",\"icon\":\"lucide:calendar-clock\"},"
      + "{\"label\":\"Ended 17/11/2026 08:05:00\",\"icon\":\"lucide:flag\"},"
      + "{\"label\":\"Duration 5m\",\"icon\":\"lucide:timer\"},"
      + "{\"label\":\"SCREEN_TEXT_FLOW_RUN_PROGRESS\",\"icon\":\"lucide:list-checks\"}]",
      ended.get("RunFct").getStringValue());
  }

  @Test
  void theLaunchParametersAreOneLineByNameAndARunWithoutThemHasNoLine() throws Exception {
    String none = presenter.getSummary(RUN).getDataList().getRows().get(0).get("RunParTxt").getStringValue();
    given(flowRunRepository.getLaunchParameters(RUN)).willReturn(new TreeMap<>(Map.of("secondsToWait", "5",
      "fichero", "extracto {bbva}.n43")));

    assertEquals("Parameters: fichero=extracto {bbva}.n43 · secondsToWait=5",
      presenter.getSummary(RUN).getDataList().getRows().get(0).get("RunParTxt").getStringValue());
    assertEquals("", none);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void aRunThatDoesNotExistHasNoSummaryAndNoGraph() throws Exception {
    given(queryService.launchPrivateQuery(eq("getFlowRunHeaderRow"), any(ObjectNode.class)))
      .willReturn(rows(List.of()));

    assertEquals(0, presenter.getSummary(RUN).getDataList().getRows().size());
    assertEquals(0, presenter.getGraph(RUN).getDataList().getRows().size());
    verify(queryService, never()).launchPrivateQuery(eq("getFlowRunGraphTasks"), any(ObjectNode.class));
  }

  @Test
  void theSummaryAndTheGraphOfARequestReadTheRunOnceAndOnlyTheTasksOfTheRun() throws Exception {
    inRequest();
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);

    presenter.getSummary(RUN);
    presenter.getGraph(RUN);

    verify(queryService, times(1)).launchPrivateQuery(eq("getFlowRunHeaderRow"), any(ObjectNode.class));
    verify(queryService, times(1)).launchPrivateQuery(eq("getFlowRunGraphTasks"), parameters.capture());
    assertEquals("[11,12,13]", parameters.getValue().get("taskIds").toString());
  }

  /**
   * A request of its own, whose attributes are kept as the servlet request keeps them
   */
  private static void inRequest() {
    Map<String, Object> attributes = new HashMap<>();
    HttpServletRequest request = mock(HttpServletRequest.class);
    given(request.getAttribute(anyString())).willAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
    doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
      .when(request).setAttribute(anyString(), any());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private static TaskDependency dependency(int taskId, int parentId) {
    return new TaskDependency().setTaskId(taskId).setParentId(parentId);
  }

  private static CellData date(int minutes) {
    return new CellData(Date.from(TODAY.plusMinutes(minutes).atZone(ZoneId.systemDefault()).toInstant()));
  }

  private static ServiceData rows(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, CellData> row(Object... fields) {
    Map<String, CellData> row = new HashMap<>();
    for (int index = 0; index < fields.length; index += 2) {
      Object value = fields[index + 1];
      row.put((String) fields[index], value instanceof CellData cell ? cell
        : value instanceof Integer number ? new CellData(number) : new CellData((String) value));
    }
    return row;
  }
}
