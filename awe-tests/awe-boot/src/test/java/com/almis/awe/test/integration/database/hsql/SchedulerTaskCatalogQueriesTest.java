package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.service.QueryService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SQL and wiring of the task catalog of the scheduler (#993) on a real database, through the action controller as the
 * screen launches it: the «When» summary of every launch type read from the launch, the calendar, the file server and
 * the parents of a task; the search, the launch, active and last status filters and the counts of the active filter;
 * the sort and the page; and the history strip of the last 20 saved executions with their flow. Six tasks of their
 * own, far from the ids of the task sequence and outside Quartz, are stored and removed after each test; the rows of
 * other tests are left out by the search.
 */
@Tag("integration")
class SchedulerTaskCatalogQueriesTest extends AbstractSchedulerMaintainTest {

  private static final int IMPORT = 9201;
  private static final int RECONCILE = 9202;
  private static final int CLOSE = 9203;
  private static final int PURGE = 9204;
  private static final int MANUAL = 9205;
  private static final int HOOK = 9206;
  private static final int[] TASKS = {IMPORT, RECONCILE, CLOSE, PURGE, MANUAL, HOOK};
  private static final int OK = 0;
  private static final int ERROR = 1;
  private static final int WARNING = 2;
  private static final int RUNNING = 4;
  private static final String QUERY = "getTaskCatalog";
  private static final String SEARCH = "catalog probe";
  private static final String RUN_ID = "run-catalog-probe";

  @Autowired
  private DataSource dataSource;
  @Autowired
  private JWTTokenService jwtTokenService;
  @Autowired
  private QueryService queryService;
  @Autowired
  private QueryUtil queryUtil;
  @Autowired
  private TaskDAO taskDAO;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeTasks() {
    jdbc = new JdbcTemplate(dataSource);
    deleteTasks();
    jdbc.update("INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act) VALUES (?, 'probe-sftp', 'ssh', 'host', '22', 1)",
      IMPORT);
    jdbc.update("INSERT INTO AweSchCal (Ide, Des, Act, Nom) VALUES (?, 'Probe holidays', 1, 'Probe holidays')", CLOSE);
    // The file task launches its dependencies also on warning
    task(IMPORT, "Catalog Probe Import", "Imports the probe statements", "importStatements", 2, 1, 1);
    task(RECONCILE, "Catalog Probe Reconcile", "Matches the movements", "reconcileProbe", 0, 1, 0);
    task(CLOSE, "Catalog Probe Close", "Closes the day", "closeDay", 1, 0, 0);
    task(PURGE, "Catalog Probe Purge", "Purges the logs", "purgeLogs", 1, 1, 0);
    task(MANUAL, "Catalog Probe Manual", "Recalculates on demand", "recalculate", 0, 1, 0);
    task(HOOK, "Catalog Probe Hook", "Receives the bank answer", "bankAnswer", 4, 1, 0);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp, IdSrv, Pth, Pat) VALUES (?, ?, 5, 1, ?, '/in', "
      + "'*.n43')", IMPORT, IMPORT, IMPORT);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptTyp, IdeCal, WeekDays, ExeHrs, ExeMin, ExeSec) "
      + "VALUES (?, ?, 7, ?, '1,2,3,4,5', '22', '30', '0')", CLOSE, CLOSE, CLOSE);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp) VALUES (?, ?, 15, 1)", PURGE, PURGE);
    jdbc.update("INSERT INTO AweSchTskDpn (IdeTsk, IdePrn, IsBlk, DpnOrd) VALUES (?, ?, NULL, 1)", RECONCILE, IMPORT);
    jdbc.update("INSERT INTO AweSchTskPar (Ide, IdeTsk, Nam, Val, Src, Typ) VALUES (?, ?, 'amount', '1', 1, 'STRING')",
      MANUAL, MANUAL);

    // 22 saved executions of the file task, the last one ended with warning in the flow run it started
    for (int execution = 1; execution <= 22; execution++) {
      execution(IMPORT, execution, "FILE_TRACKING_TASK", execution == 22 ? WARNING : OK,
        LocalDateTime.of(2019, 3, 1, 8, 0).plusDays(execution), execution == 22 ? RUN_ID : null);
    }
    jdbc.update("INSERT INTO AweSchRun (IdeRun, IdeTskRoo, ExeTskRoo, LchBy, IniDat, Sta) VALUES (?, ?, 22, 'test', ?, 0)",
      RUN_ID, IMPORT, Timestamp.valueOf(LocalDateTime.of(2019, 3, 23, 8, 0)));
    execution(RECONCILE, 1, "DEPENDANT_TASK", ERROR, LocalDateTime.of(2019, 3, 23, 8, 2), RUN_ID);
    execution(HOOK, 1, "WEBHOOK_TASK", OK, LocalDateTime.of(2019, 2, 1, 8, 0), null);
    // The closing waits for the confirmation of its deferred answer, due tomorrow
    execution(CLOSE, 1, "SCHEDULED_TASK", RUNNING, LocalDateTime.of(2019, 3, 24, 22, 30), null);
    jdbc.update("INSERT INTO AweSchExeExt (IdeTsk, ExeTsk, ExtRef, DfrDat, DueDat) VALUES (?, 1, 'probe', ?, ?)", CLOSE,
      Timestamp.valueOf(LocalDateTime.of(2019, 3, 24, 22, 31)), Timestamp.valueOf(LocalDate.now().plusDays(1)
        .atTime(12, 0)));
  }

  @AfterEach
  void deleteTasks() {
    for (int task : TASKS) {
      for (String table : new String[]{"AweSchExeExt", "AweSchExe", "AweSchTskPar", "AweSchTskLch"}) {
        jdbc.update("DELETE FROM " + table + " WHERE IdeTsk = ?", task);
      }
      jdbc.update("DELETE FROM AweSchTskDpn WHERE IdeTsk = ? OR IdePrn = ?", task, task);
      jdbc.update("DELETE FROM AweSchTsk WHERE Ide = ?", task);
    }
    jdbc.update("DELETE FROM AweSchRun WHERE IdeRun = ?", RUN_ID);
    jdbc.update("DELETE FROM AweSchCal WHERE Ide = ?", CLOSE);
    jdbc.update("DELETE FROM AweSchSrv WHERE Ide = ?", IMPORT);
  }

  @Test
  void everyTaskTellsWhenItIsLaunchedFromItsLaunchCalendarServerAndParents() throws Exception {
    List<JsonNode> rows = rows(dataList(QUERY, params().put("CrtTskSch", SEARCH)));

    assertThat(column(rows, "Nam")).containsExactly("Catalog Probe Close", "Catalog Probe Hook",
      "Catalog Probe Import", "Catalog Probe Manual", "Catalog Probe Purge", "Catalog Probe Reconcile");
    assertThat(column(rows, "TskWhnTxt")).containsExactly("Weekdays at 22:30", "When a webhook arrives",
      "When a file arrives in probe-sftp:/in", "Launched by hand", "Every 15 minutes", "After Catalog Probe Import");
    assertThat(column(rows, "TskWhnSub")).containsExactly("calendar Probe holidays", "signed call from outside",
      "*.n43 · checks every 5 min", "no schedule", "scheduled", "if it ends OK or with a warning");
    assertThat(column(rows, "TskKnd")).containsExactly("1", "4", "2", "0", "1", "3");
    // The inactive closing is not executed; the reconciliation runs when its parent ends
    assertThat(rows.get(0).get("TskNxtSub").asString()).isEqualTo("task inactive");
    assertThat(rows.get(5).get("TskNxtTxt").asString()).isEqualTo("When Catalog Probe Import ends");
    // The task links to its screen with its description as the title of the link
    assertThat(rows.get(2).get("TskNam").get("label").asString()).isEqualTo("Catalog Probe Import");
    assertThat(rows.get(2).get("TskNam").get("title").asString()).isEqualTo("Imports the probe statements");
  }

  @Test
  void theSearchFindsATaskByItsNameDescriptionOrWhatItRuns() throws Exception {
    assertThat(names(params().put("CrtTskSch", "reconcileprobe"))).containsExactly("Catalog Probe Reconcile");
    assertThat(names(params().put("CrtTskSch", "imports the PROBE statements"))).containsExactly("Catalog Probe Import");
    assertThat(names(params().put("CrtTskSch", "catalog probe hook"))).containsExactly("Catalog Probe Hook");
  }

  @Test
  void theLaunchActiveAndLastStatusFiltersKeepTheirTasksAndTheActiveFilterCountsWithTheOthers() throws Exception {
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskLch", "dependency")))
      .containsExactly("Catalog Probe Reconcile");
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskLch", "manual")))
      .containsExactly("Catalog Probe Manual");
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskAct", "0"))).containsExactly("Catalog Probe Close");
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskSta", "waiting")))
      .containsExactly("Catalog Probe Close");
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskSta", "none")))
      .containsExactly("Catalog Probe Manual", "Catalog Probe Purge");
    assertThat(names(params().put("CrtTskSch", SEARCH).put("CrtTskSta", "error")))
      .containsExactly("Catalog Probe Reconcile");

    List<JsonNode> counts = rows(dataList("getTaskCatalogActiveCounts", params().put("CrtTskSch", SEARCH)
      .put("CrtTskAct", "0")));
    List<JsonNode> scheduled = rows(dataList("getTaskCatalogActiveCounts", params().put("CrtTskSch", SEARCH)
      .put("CrtTskLch", "scheduled")));
    assertThat(column(counts, "value")).containsExactly("", "1", "0");
    assertThat(counts.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(6, 5, 1);
    assertThat(scheduled.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(2, 1, 1);
  }

  @Test
  void theSortOrdersEveryTaskBeforeThePageAndAColumnItDoesNotSortIsRefused() throws Exception {
    JsonNode page = dataList(QUERY, params().put("CrtTskSch", SEARCH).put("page", 1).put("max", 2)
      .set("sort", sort("TskLstDat", "desc")));
    JsonNode refused = find(actions(QUERY, params().put("CrtTskSch", SEARCH).set("sort", sort("Des", "asc"))),
      "message");
    JsonNode wrongStatus = find(actions(QUERY, params().put("CrtTskSch", SEARCH).put("CrtTskSta", "lost")), "message");

    assertThat(page.get("records").asInt()).isEqualTo(6);
    assertThat(page.get("total").asInt()).isEqualTo(3);
    assertThat(column(rows(page), "Nam")).containsExactly("Catalog Probe Close", "Catalog Probe Reconcile");
    assertThat(refused.get("parameters").get("title").asString()).isEqualTo("Invalid filter");
    assertThat(wrongStatus.get("parameters").get("type").asString()).isEqualTo("error");
  }

  @Test
  void theHistoryShowsTheLast20SavedExecutionsWithTheirFlowAndTheLastStatusIsAPill() throws Exception {
    List<JsonNode> rows = rows(dataList(QUERY, params().put("CrtTskSch", SEARCH)));
    JsonNode importTask = rows.get(2);
    JsonNode strip = objectMapper.readTree(importTask.get("TskHis").asString());

    assertThat(strip).hasSize(20);
    assertThat(strip.get(0).get("title").asString()).startsWith("Execution 3 · 04/03/2019 08:00 · ");
    assertThat(strip.get(19).get("title").asString()).startsWith("Execution 22 · 23/03/2019 08:00 · ")
      .endsWith(" · flow #22");
    assertThat(strip.get(19).get("tone").asString()).isEqualTo("warning");
    assertThat(importTask.get("TskStaTxt").asString()).isEqualTo("Warning");
    assertThat(importTask.get("TskStaTon").asString()).isEqualTo("warning");
    assertThat(importTask.get("TskStaSub").asString()).isEqualTo("23/03/2019 08:00");
    // The waiting closing shows the waiting pill, the task without executions says so
    assertThat(rows.get(0).get("TskStaTxt").asString()).isEqualTo("Waiting for confirmation");
    assertThat(rows.get(3).get("TskStaTxt").asString()).isEqualTo("No executions");
    assertThat(objectMapper.readTree(rows.get(3).get("TskHis").asString())).isEmpty();
    // Run opens the launch dialog of the task with parameters
    assertThat(rows.get(3).get("TskVar").asInt()).isOne();
    assertThat(rows.get(4).get("TskVar").asInt()).isZero();
  }

  @Test
  void theHistoryQueryReadsOnlyTheLast20SavedExecutionsOfEachTaskOfThePage() throws Exception {
    // 25 saved executions of the purge too, besides the 22 of the import and the one of the reconciliation
    for (int execution = 1; execution <= 25; execution++) {
      execution(PURGE, execution, "SCHEDULED_TASK", OK, LocalDateTime.of(2019, 4, 1, 8, 0).plusHours(execution), null);
    }
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    parameters.put("catalogHistorySize", 20);
    parameters.putArray("catalogTaskIds").add(IMPORT).add(PURGE).add(RECONCILE);

    Map<Integer, List<Integer>> executions = new LinkedHashMap<>();
    for (Map<String, CellData> row : queryService.launchPrivateQuery("getTaskCatalogHistory", parameters).getDataList()
      .getRows()) {
      executions.computeIfAbsent(row.get("IdeTsk").getIntegerValue(), key -> new ArrayList<>())
        .add(row.get("ExeTsk").getIntegerValue());
    }

    // Each task brings its newest 20 executions, newest first; a task with fewer brings all of them
    assertThat(executions.keySet()).containsExactly(IMPORT, RECONCILE, PURGE);
    assertThat(executions.get(IMPORT)).containsExactlyElementsOf(IntStream.rangeClosed(3, 22).map(n -> 25 - n).boxed()
      .toList());
    assertThat(executions.get(PURGE)).containsExactlyElementsOf(IntStream.rangeClosed(6, 25).map(n -> 31 - n).boxed()
      .toList());
    assertThat(executions.get(RECONCILE)).containsExactly(1);
  }

  @Test
  void theHistoryQueryOrdersTheExecutionsThatStartedAtTheSameTimeByTheirNumber() throws Exception {
    // 21 executions of the purge started at the same time: the newest 20 by number, highest first
    for (int execution = 1; execution <= 21; execution++) {
      execution(PURGE, execution, "SCHEDULED_TASK", OK, LocalDateTime.of(2019, 4, 1, 8, 0), null);
    }
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    parameters.put("catalogHistorySize", 20);
    parameters.putArray("catalogTaskIds").add(PURGE);

    List<Integer> executions = queryService.launchPrivateQuery("getTaskCatalogHistory", parameters).getDataList()
      .getRows().stream().map(row -> row.get("ExeTsk").getIntegerValue()).toList();

    assertThat(executions).containsExactlyElementsOf(IntStream.rangeClosed(2, 21).map(n -> 23 - n).boxed().toList());
  }

  @Test
  void theFlowGraphHasOneRowPerTaskAndParentWhenTheLastExecutionsStartedAtTheSameTime() throws Exception {
    // A second execution of the import started at the same time as its last one, with error: the higher number wins
    execution(IMPORT, 23, "FILE_TRACKING_TASK", ERROR, LocalDateTime.of(2019, 3, 23, 8, 0), null);

    List<JsonNode> graph = rows(dataList("getFlowGraph", params().put("IdeTsk.selected", RECONCILE)));

    List<JsonNode> imports = graph.stream().filter(row -> row.get("taskId").asInt() == IMPORT).toList();
    assertThat(imports).hasSize(1);
    assertThat(imports.get(0).get("lastStatus").asInt()).isEqualTo(ERROR);
    assertThat(graph.stream().filter(row -> row.get("taskId").asInt() == RECONCILE).count()).isOne();
  }

  @Test
  void theTaskScreenGraphTellsWhenTheTasksOfTheLevelLastRanAndTheOtherParentsAChildWaitsFor() throws Exception {
    // The reconciliation also waits for the purge, a task out of the level of the import
    jdbc.update("INSERT INTO AweSchTskDpn (IdeTsk, IdePrn, IsBlk, DpnOrd) VALUES (?, ?, NULL, 2)", RECONCILE, PURGE);

    List<JsonNode> graph = rows(dataList("getTaskFlowGraph", params().put("IdeTsk.selected", IMPORT)));

    JsonNode root = graph.stream().filter(row -> row.get("taskId").asInt() == IMPORT).findFirst().orElseThrow();
    assertThat(root.get("rootTask").asInt()).isEqualTo(IMPORT);
    assertThat(root.get("lastWhen").asString()).isEqualTo("23/03/2019 08:00");
    assertThat(root.get("nextRun").asString()).isEmpty();
    List<JsonNode> child = graph.stream().filter(row -> row.get("taskId").asInt() == RECONCILE).toList();
    assertThat(child.stream().map(row -> row.get("parentId").asInt()).toList()).containsExactlyInAnyOrder(IMPORT, PURGE);
    assertThat(child.get(0).get("lastWhen").asString()).isEqualTo("23/03/2019 08:02");
    assertThat(graph.stream().filter(row -> row.get("taskId").asInt() == PURGE).findFirst().orElseThrow()
      .get("lastWhen").asString()).isEmpty();
  }

  @Test
  void theFlowLaunchReadsTheChildrenOfEveryTaskInTheirDependencyOrder() throws Exception {
    // The reconciliation and the closing run after the import: the closing first (the order of the dependencies)
    jdbc.update("INSERT INTO AweSchTskDpn (IdeTsk, IdePrn, IsBlk, DpnOrd) VALUES (?, ?, NULL, 0)", CLOSE, IMPORT);

    assertThat(taskDAO.getChildrenByTask().get(IMPORT)).containsExactly(CLOSE, RECONCILE);
  }

  @Test
  void theTaskScreenSummarizesTheSavedExecutionsOfItsTaskAndItsHistoryKeepsOnlyThem() throws Exception {
    JsonNode page = rows(dataList("getTaskPage", params().put("IdeTsk.selected", IMPORT))).get(0);
    JsonNode success = rows(dataList("getTaskPageSuccess", params().put("IdeTsk.selected", IMPORT))).get(0);
    JsonNode history = dataList("getActivityExecutionList", params().put("CrtExeTskIde", IMPORT).put("max", 5));
    List<JsonNode> graph = rows(dataList("getFlowGraph", params().put("IdeTsk.selected", RECONCILE)));

    // 21 OK and the last one with warning, in the flow run it started
    assertThat(page.get("TskPagTit").asString()).isEqualTo("Catalog Probe Import");
    assertThat(page.get("TskPagChp").asString()).isEqualTo("Last: Warning");
    assertThat(page.get("TskPagChpTon").asString()).isEqualTo("warning");
    JsonNode facts = objectMapper.readTree(page.get("TskPagFct").asString());
    assertThat(facts.get(0).get("label").asString()).isEqualTo("#" + IMPORT);
    assertThat(facts.get(1).get("label").asString()).isEqualTo("Command");
    assertThat(facts.get(1).get("code").asString()).isEqualTo("importStatements");
    assertThat(success.get("value").asInt()).isEqualTo(95);
    assertThat(success.get("description").asString()).isEqualTo("21 Ok, 1 Warning · of 22 saved executions");
    assertThat(objectMapper.readTree(page.get("TskHis").asString())).hasSize(20);
    assertThat(page.get("TskLstExe").asString()).isEqualTo("22");
    assertThat(page.get("TskLstIni").asString()).isEqualTo("23/03/2019 08:00");
    assertThat(page.get("TskVar").asInt()).isZero();
    // The history is the activity list of the task
    assertThat(history.get("records").asInt()).isEqualTo(22);
    assertThat(column(rows(history), "TskNam")).containsOnly("Catalog Probe Import");
    // The one-level graph of the reconciliation: its parent with the status of its last execution
    assertThat(graph.stream().filter(row -> row.get("taskId").asInt() == IMPORT).findFirst().orElseThrow()
      .get("lastStatus").asInt()).isEqualTo(WARNING);
    assertThat(graph.stream().filter(row -> row.get("taskId").asInt() == RECONCILE).findFirst().orElseThrow()
      .get("rootTask").asInt()).isEqualTo(RECONCILE);
  }

  @Test
  void theHeaderAndHistoryCardsReadTheFieldsOfThePageWithTheNamesOfTheCardAttributes() throws Exception {
    ObjectNode task = params().put("IdeTsk.selected", IMPORT);
    JsonNode page = rows(dataList("getTaskPage", task)).get(0);
    JsonNode header = rows(dataList("getTaskPageHeader", task)).get(0);
    JsonNode history = rows(dataList("getTaskPageHistoryCount", task)).get(0);

    assertThat(header.get("title").asString()).isEqualTo("Catalog Probe Import");
    assertThat(header.get("subtitle").asString()).isEqualTo(page.get("TskPagSub").asString());
    assertThat(header.get("chip").asString()).isEqualTo("Last: Warning");
    assertThat(header.get("chipTone").asString()).isEqualTo("warning");
    JsonNode pills = objectMapper.readTree(header.get("pills").asString());
    assertThat(pills).isEqualTo(objectMapper.readTree(page.get("TskPagFct").asString()));
    assertThat(pills.get(0).get("label").asString()).isEqualTo("#" + IMPORT);
    assertThat(history.get("subtitle").asString()).isNotEmpty().isEqualTo(page.get("TskHisTxt").asString());
    assertThat(objectMapper.readTree(history.get("strip").asString())).hasSize(20);
  }

  @Test
  void theCatalogApiRequiresTheBearerAndRefusesAnInvalidFilterWith400() throws Exception {
    String body = "{\"search\":\"catalog probe\",\"launchType\":\"file\"}";

    assertThat(api(post("/scheduler/api/v1/tasks/catalog").contentType(MediaType.APPLICATION_JSON).content(body),
      false).getStatus()).isEqualTo(401);
    MockHttpServletResponse page = api(post("/scheduler/api/v1/tasks/catalog").contentType(MediaType.APPLICATION_JSON)
      .content(body), true);
    assertThat(page.getStatus()).isEqualTo(200);
    JsonNode rows = objectMapper.readTree(page.getContentAsString()).get("dataList").get("rows");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("TskKnd").asInt()).isEqualTo(2);
    MockHttpServletResponse counts = api(post("/scheduler/api/v1/tasks/catalog/active-counts")
      .contentType(MediaType.APPLICATION_JSON).content(body), true);
    assertThat(counts.getStatus()).isEqualTo(200);
    MockHttpServletResponse invalid = api(post("/scheduler/api/v1/tasks/catalog")
      .contentType(MediaType.APPLICATION_JSON).content("{\"launchType\":\"api\"}"), true);
    assertThat(invalid.getStatus()).isEqualTo(400);
    assertThat(objectMapper.readTree(invalid.getContentAsString()).get("type").asString()).isEqualTo("invalid-request");
  }

  private List<String> names(ObjectNode parameters) throws Exception {
    return column(rows(dataList(QUERY, parameters)), "Nam");
  }

  private void task(int id, String name, String description, String command, int launchType, int active,
                    int onWarning) {
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, CmdExe, TypLch, Act, LchDepWrn) "
      + "VALUES (?, ?, ?, 0, ?, ?, ?, ?)", id, name, description, command, launchType, active, onWarning);
  }

  private void execution(int taskId, int executionId, String group, int status, LocalDateTime start, String runId) {
    jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy, IdeRun) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'test', ?)", taskId, group, executionId, Timestamp.valueOf(start),
      status == RUNNING ? null : Timestamp.valueOf(start.plusMinutes(1)), status == RUNNING ? null : 60_000, status,
      runId);
  }

  private MockHttpServletResponse api(MockHttpServletRequestBuilder request, boolean withBearer) throws Exception {
    request.with(anonymous());
    return mockMvc.perform(withBearer ? request.header("Authorization", "Bearer " + bearer()) : request)
      .andReturn().getResponse();
  }

  private String bearer() {
    return JWT.create()
      .withSubject("test")
      .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
  }

  private JsonNode sort(String column, String direction) {
    return objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("id", column)
      .put("direction", direction));
  }

  private JsonNode find(JsonNode actions, String type) {
    for (JsonNode action : actions) {
      if (type.equals(action.get("type").asString())) {
        return action;
      }
    }
    throw new AssertionError("No " + type + " action: " + actions);
  }

  private List<JsonNode> rows(JsonNode dataList) {
    List<JsonNode> rows = new ArrayList<>();
    dataList.get("rows").forEach(rows::add);
    return rows;
  }

  private JsonNode dataList(String query, ObjectNode parameters) throws Exception {
    return find(actions(query, parameters), "fill").get("parameters").get("datalist");
  }

  private JsonNode actions(String query, ObjectNode parameters) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .content(objectMapper.writeValueAsString(parameters))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response);
  }
}
