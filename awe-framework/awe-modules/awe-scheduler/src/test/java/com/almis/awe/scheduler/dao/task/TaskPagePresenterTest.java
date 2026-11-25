package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskSummary;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Task screen of the scheduler (#993): its summary is computed over the saved executions of the task (the ones that
 * ended: the running and queued ones and the ones without a status are left out), its header tells the last status
 * and the facts of the task, its last execution is the newest row of the activity list of the task, a task without
 * executions says so, and the screen and its cards share one computation of the page in a request
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskPagePresenterTest {

  private static final LocalDateTime START = LocalDateTime.of(2026, 11, 17, 8, 6);
  private static final int TASK = 13;

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskCatalogPresenter catalogPresenter;
  @Mock
  private TaskExecutionPresenter executionPresenter;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskPagePresenter presenter;
  private boolean editor;

  @BeforeEach
  void setUp() throws Exception {
    editor = true;
    presenter = new TaskPagePresenter(queryService, queryUtil, catalogPresenter, executionPresenter, serviceConfig,
      query -> editor && "getTaskPageTaskRestricted".equals(query));
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(serviceConfig.getLocale(anyString())).willAnswer(invocation -> {
      String key = invocation.getArgument(0);
      return key.startsWith("SCREEN_TEXT_TASK_PAGE_") ? key + "[{0}]" : key;
    });
    given(queryService.findLabel(anyString(), anyString())).willAnswer(invocation -> {
      String value = invocation.getArgument(1);
      return switch ((String) invocation.getArgument(0)) {
        case "ActStaTon" -> switch (value) {
          case "1" -> "danger";
          case "2" -> "warning";
          case "6" -> "pending";
          default -> "success";
        };
        case "RepTyp" -> "ENUM_REPEAT_EMAIL";
        default -> "STATUS_" + value;
      };
    });
    given(catalogPresenter.getHistoryStrip(anyList())).willReturn(JsonNodeFactory.instance.arrayNode());
    given(queryService.launchPrivateQuery(eq("getTasksWithParameters"), any(ObjectNode.class)))
      .willReturn(data(List.of()));
    given(queryService.launchPrivateQuery(eq("getTaskPageTask"), any(ObjectNode.class)))
      .willReturn(data(List.of(settings())));
    given(queryService.launchPrivateQuery(eq("getTaskPageTaskRestricted"), any(ObjectNode.class)))
      .willReturn(data(List.of(restricted())));
    given(executionPresenter.getExecutionList(any(ExecutionListCriteria.class))).willReturn(data(List.of()));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void aSavedExecutionWithoutStatusIsLeftOutOfTheSummaryAndOfTheDurations() throws Exception {
    givenTask(0, 60_000L);
    givenSaved(execution(3, 0, 60_000), execution(2, null, 90_000), execution(1, 1, 30_000));

    Map<String, CellData> row = page();

    assertThat(row.get("TskOkVal").getObjectValue()).isEqualTo(50L);
    assertThat(row.get("TskOkDes").getStringValue())
      .isEqualTo("1 STATUS_0, 1 STATUS_1 · SCREEN_TEXT_TASK_PAGE_SAVED_EXECUTIONS[2]");
    assertThat(row.get("TskAvgSpk").getStringValue()).isEqualTo("[30.0,60.0]");
  }

  @Test
  void theCountOfTheLastExecutionsTellsHowManyBarsOfEachLabelTheStripHas() throws Exception {
    givenTask(0, 60_000L);
    givenSaved(execution(3, 0, 60_000), execution(2, 1, 60_000), execution(1, 0, 60_000));
    ArrayNode strip = JsonNodeFactory.instance.arrayNode();
    strip.addObject().put("tone", "success").put("label", "Ok");
    strip.addObject().put("tone", "danger").put("label", "Error");
    strip.addObject().put("tone", "success").put("label", "Ok");
    given(catalogPresenter.getHistoryStrip(anyList())).willReturn(strip);

    Map<String, CellData> row = page();

    assertThat(row.get("TskHis").getStringValue()).isEqualTo(strip.toString());
    assertThat(row.get("TskHisTxt").getStringValue()).isEqualTo("2 Ok, 1 Error");
  }

  @Test
  void theScreenAndItsCardsShareOneComputationOfThePageInARequestAndEachGetsItsOwnCopy() throws Exception {
    givenTask(0, 60_000L);
    givenSaved(execution(1, 0, 60_000));
    inRequest();

    Map<String, CellData> screen = page();
    screen.put("TskPagTit", new CellData("changed by the screen"));
    Map<String, CellData> card = page();

    verify(catalogPresenter, times(1)).getSummary(TASK);
    assertThat(card.get("TskPagTit").getStringValue()).isEqualTo("Bank reconciliation");

    // The next request (an update after an action) computes the page again
    inRequest();
    page();
    verify(catalogPresenter, times(2)).getSummary(TASK);
  }

  @Test
  void anErrorComputingThePageAlsoFailsTheOtherCallsOfTheRequestInsteadOfLeavingThemWaiting() throws Exception {
    givenTask(0, 60_000L);
    given(catalogPresenter.getSummary(TASK)).willThrow(new StackOverflowError("probe"));
    inRequest();

    org.assertj.core.api.Assertions.assertThatThrownBy(this::page).isInstanceOf(StackOverflowError.class);
    // The second card of the same request finds the shared page completed with the error: it fails at once
    var request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    java.util.concurrent.CompletableFuture<Void> second = java.util.concurrent.CompletableFuture.runAsync(() -> {
      org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(request);
      org.assertj.core.api.Assertions.assertThatThrownBy(this::page).isInstanceOf(StackOverflowError.class);
    });
    second.get(5, java.util.concurrent.TimeUnit.SECONDS);
  }

  @Test
  void whereTheTaskRunsAndWhoGetsItsReportAreOnlyReadForTheUsersWithThePermissionOfTheirQuery() throws Exception {
    givenTask(0, 60_000L);
    givenSaved(execution(1, 0, 60_000));

    Map<String, CellData> allowed = page();
    editor = false;
    Map<String, CellData> refused = page();

    assertThat(allowed.get("TskCfgWhr").getStringValue())
      .isEqualTo("SCREEN_TEXT_TASK_PAGE_ON_SERVER[probe-srv] · /opt/probe/bin");
    assertThat(allowed.get("TskCfgRpt").getStringValue())
      .isEqualTo("ENUM_REPEAT_EMAIL · ops@probe.example · probe-report");
    assertThat(refused.get("TskCfgWhr").getStringValue()).isEqualTo("—");
    assertThat(refused.get("TskCfgRpt").getStringValue()).isEqualTo("ENUM_REPEAT_EMAIL");
    // What it runs is the same for every reader
    assertThat(refused.get("TskCfgRun").getStringValue()).isEqualTo(allowed.get("TskCfgRun").getStringValue());
    assertThat(refused.get("TskPagFct").getStringValue()).isEqualTo(allowed.get("TskPagFct").getStringValue());
    verify(queryService, times(1)).launchPrivateQuery(eq("getTaskPageTaskRestricted"), any(ObjectNode.class));
  }

  @Test
  void withoutARequestThePageIsComputedOnEveryCall() throws Exception {
    givenTask(0, 60_000L);
    givenSaved(execution(1, 0, 60_000));

    page();
    page();

    verify(catalogPresenter, times(2)).getSummary(TASK);
  }

  @Test
  void theSummaryCountsTheSavedExecutionsThatEndedAndTheAverageTellsTheMaximumAllowed() throws Exception {
    givenTask(2, 191_000L);
    // Newest first: one running, then OK, error, OK, warning and OK
    givenSaved(execution(6, 4, null), execution(5, 0, 180_000), execution(4, 1, 62_000), execution(3, 0, 200_000),
      execution(2, 2, 240_000), execution(1, 0, 190_000));

    Map<String, CellData> row = page();

    assertThat(row.get("TskOkVal").getObjectValue()).isEqualTo(60L);
    assertThat(row.get("TskOkDes").getStringValue())
      .isEqualTo("3 STATUS_0, 1 STATUS_2, 1 STATUS_1 · SCREEN_TEXT_TASK_PAGE_SAVED_EXECUTIONS[5]");
    assertThat(row.get("TskAvgVal").getStringValue()).isEqualTo("3m 11s");
    assertThat(row.get("TskAvgDes").getStringValue()).isEqualTo("SCREEN_TEXT_TASK_PAGE_MAX_ALLOWED[30m]");
    // The durations of the executions that ended, in seconds, oldest first
    assertThat(row.get("TskAvgSpk").getStringValue()).isEqualTo("[190.0,240.0,200.0,62.0,180.0]");
  }

  @Test
  void theStackedBarOfTheSuccessRateHasOneSegmentPerStatusOfTheSavedExecutionsThatEnded() throws Exception {
    givenTask(0, 60_000L);
    // Newest first: one running (left out), then OK, error, OK, a status with the pending tone and warning
    givenSaved(execution(6, 4, null), execution(5, 0, 60_000), execution(4, 1, 60_000), execution(3, 0, 60_000),
      execution(2, 6, 60_000), execution(1, 2, 60_000));

    Map<String, CellData> row = page();

    // In the order of the counts, each with the tone of its status (pending is drawn as info) and its count
    assertThat(new JsonMapper().readTree(row.get("TskOkStk").getStringValue())).isEqualTo(new JsonMapper().readTree("""
      [{"tone": "success", "label": "STATUS_0", "value": 2},
       {"tone": "warning", "label": "STATUS_2", "value": 1},
       {"tone": "danger", "label": "STATUS_1", "value": 1},
       {"tone": "info", "label": "STATUS_6", "value": 1}]"""));

    // Without saved executions that ended there is no bar
    givenSaved(execution(1, 4, null));
    assertThat(page().get("TskOkStk").getStringValue()).isEmpty();
  }

  @Test
  void theHeaderTellsTheLastStatusAndTheFactsOfTheTask() throws Exception {
    givenTask(2, 191_000L);
    givenSaved(execution(1, 2, 240_000));

    Map<String, CellData> row = page();

    assertThat(row.get("TskPagTit").getStringValue()).isEqualTo("Bank reconciliation");
    assertThat(row.get("TskPagSub").getStringValue()).isEqualTo("Matches the movements");
    assertThat(row.get("TskPagChp").getStringValue()).isEqualTo("SCREEN_TEXT_TASK_PAGE_LAST_STATUS[STATUS_2]");
    assertThat(row.get("TskPagChpTon").getStringValue()).isEqualTo("warning");
    // The facts are the pills of the header: what it runs with its maintain as code
    assertThat(new JsonMapper().readTree(row.get("TskPagFct").getStringValue())).isEqualTo(new JsonMapper().readTree("""
      [{"label": "#13"},
       {"label": "ENUM_EXECUTE_MAINTAIN", "icon": "lucide:database", "code": "BankReconciliation"},
       {"label": "After Import", "icon": "%s"},
       {"label": "SCREEN_TEXT_TASK_PAGE_KEEPS[50]", "icon": "lucide:archive"},
       {"label": "SCREEN_TEXT_TASK_PAGE_MAX_TIME[30m]", "icon": "lucide:timer"},
       {"label": "SCREEN_TEXT_TASK_PAGE_REPORT[ENUM_REPEAT_EMAIL]", "icon": "lucide:mail"}]
      """.formatted(row.get("TskPagIcn").getStringValue())));
    // Without a next fire time the card has no figure: what launches the task goes in words
    assertThat(row.get("TskNxtVal").getStringValue()).isEmpty();
    assertThat(row.get("TskNxtDes").getStringValue())
      .isEqualTo("When Import ends · if it ends OK or with a warning");
    assertThat(row.get("TskCfgPrn").getStringValue()).isEqualTo("Import");
    assertThat(row.get("TskCfgChl").getStringValue()).isEqualTo("SCREEN_TEXT_TASK_WHEN_AFTER_IF_OK_WARNING");
  }

  @Test
  void theLastExecutionIsTheNewestRowOfTheActivityListOfTheTask() throws Exception {
    givenTask(2, 191_000L);
    givenSaved(execution(214, 2, 220_000));
    Map<String, CellData> last = new HashMap<>();
    last.put("ExeStaTxt", new CellData("ENUM_STATUS_WARNING"));
    last.put("ExeStaTon", new CellData("warning"));
    last.put("ExeTsk", new CellData(214));
    last.put("ExeRunLbl", new CellData("Import #12"));
    last.put("ExeOriTxt", new CellData("ENUM_LAUNCH_DEPENDENCY"));
    last.put("LchBy", new CellData("scheduler"));
    last.put("ExeLstTim", new CellData(date(START)));
    last.put("ExeTim", new CellData("3m 40s"));
    given(executionPresenter.getExecutionList(any(ExecutionListCriteria.class))).willReturn(data(List.of(last)));

    Map<String, CellData> row = page();

    ArgumentCaptor<ExecutionListCriteria> criteria = ArgumentCaptor.forClass(ExecutionListCriteria.class);
    verify(executionPresenter).getExecutionList(criteria.capture());
    assertThat(criteria.getValue().getTaskId()).isEqualTo(TASK);
    assertThat(criteria.getValue().getMax()).isOne();
    assertThat(row.get("TskLstHas").getObjectValue()).isEqualTo(1);
    assertThat(row.get("TskLstSta").getStringValue()).isEqualTo("ENUM_STATUS_WARNING");
    assertThat(row.get("TskLstExe").getStringValue()).isEqualTo("214");
    assertThat(row.get("TskLstRun").getStringValue()).isEqualTo("Import #12");
    assertThat(row.get("TskLstOri").getStringValue()).isEqualTo("ENUM_LAUNCH_DEPENDENCY");
    assertThat(row.get("TskLstIni").getStringValue()).isEqualTo("17/11/2026 08:06");
    assertThat(row.get("TskLstDur").getStringValue()).isEqualTo("3m 40s");
    assertThat(row.get("TskLstBy").getStringValue()).isEqualTo("scheduler");
    assertThat(row.get("TskLstDes").getStringValue()).isEqualTo("—");
  }

  @Test
  void aTaskWithoutExecutionsSaysSoInItsHeaderSummaryAndLastExecution() throws Exception {
    givenTask(null, null);
    givenSaved();

    Map<String, CellData> row = page();

    assertThat(row.get("TskPagChp").getStringValue()).isEqualTo("SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS");
    assertThat(row.get("TskPagChpTon").getStringValue()).isEqualTo("neutral");
    assertThat(row.get("TskOkVal").getStringValue()).isEqualTo("—");
    assertThat(row.get("TskOkDes").getStringValue()).isEqualTo("SCREEN_TEXT_TASK_PAGE_NO_SAVED_EXECUTIONS[{0}]");
    assertThat(row.get("TskAvgVal").getStringValue()).isEqualTo("—");
    assertThat(row.get("TskAvgSpk").getStringValue()).isEmpty();
    assertThat(row.get("TskLstHas").getObjectValue()).isEqualTo(0);
    assertThat(row.get("TskLstExe").getStringValue()).isEqualTo("—");
  }

  @Test
  void aTaskThatDoesNotExistHasNoRow() throws Exception {
    given(catalogPresenter.getSummary(TASK)).willReturn(Optional.empty());

    assertThat(presenter.getPage(TASK).getDataList().getRows()).isEmpty();
  }

  private Map<String, CellData> page() throws Exception {
    List<Map<String, CellData>> rows = presenter.getPage(TASK).getDataList().getRows();
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private void givenTask(Integer lastStatus, Long average) throws Exception {
    TaskLaunch launch = new TaskLaunch().setLaunchType(0)
      .setParents(List.of(new TaskLaunch.Parent("Import", true, false)));
    TaskWhenSummary.When when = new TaskWhenSummary.When(TaskLaunchKind.DEPENDENCY, "lucide:git-merge",
      "After Import", "if it ends OK or with a warning");
    given(catalogPresenter.getSummary(TASK)).willReturn(Optional.of(new TaskSummary(TASK, "Bank reconciliation",
      "Matches the movements", "BankReconciliation", true, TaskLaunchKind.DEPENDENCY, launch, when,
      new TaskWhenSummary.Line("When Import ends", ""), null, average, lastStatus, false,
      lastStatus == null ? null : date(START))));
  }

  @SafeVarargs
  private void givenSaved(Map<String, CellData>... executions) throws Exception {
    given(queryService.launchPrivateQuery(eq("getTaskPageExecutions"), any(ObjectNode.class)))
      .willReturn(data(new ArrayList<>(List.of(executions))));
  }

  private static Map<String, CellData> settings() {
    Map<String, CellData> row = new HashMap<>();
    row.put("TypExe", new CellData(1));
    row.put("NumStoExe", new CellData(50));
    row.put("TimOutExe", new CellData(1800));
    row.put("RepTyp", new CellData(1));
    row.put("LchDepWrn", new CellData(1));
    row.put("LchDepErr", new CellData(0));
    return row;
  }

  private static Map<String, CellData> restricted() {
    Map<String, CellData> row = new HashMap<>();
    row.put("SrvExeNam", new CellData("probe-srv"));
    row.put("CmdExePth", new CellData("/opt/probe/bin"));
    row.put("RepEmaDst", new CellData("ops@probe.example"));
    row.put("RepMntId", new CellData("probe-report"));
    return row;
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

  private static Map<String, CellData> execution(int executionId, Integer status, Integer time) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(TASK));
    row.put("ExeTsk", new CellData(executionId));
    row.put("Sta", status == null ? new CellData() : new CellData(status));
    row.put("IniDat", new CellData(date(START.minusDays(10L - executionId))));
    row.put("ExeTim", time == null ? new CellData() : new CellData(time));
    return row;
  }

  private static Date date(LocalDateTime date) {
    return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
  }

  private static ServiceData data(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }
}
