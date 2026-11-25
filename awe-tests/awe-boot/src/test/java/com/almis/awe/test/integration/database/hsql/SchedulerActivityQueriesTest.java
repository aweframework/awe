package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.scheduler.dao.TaskDAO;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SQL of the queries of the activity screen (#993) on a real database, through the action controller as the screen
 * launches them: the page of the executions of all tasks (filters, database pagination, newest first) and the
 * counters. Two tasks of their own, far from the ids of the task sequence, are stored with their executions and
 * removed after each test; the rows of other tests are left out by the task name filter. The deferred execution is due
 * tomorrow, so the deadline check of the scheduler never interrupts it while a test runs.
 */
@Tag("integration")
class SchedulerActivityQueriesTest extends AbstractSchedulerMaintainTest {

  private static final int ALPHA = 9101;
  private static final int BETA = 9102;
  private static final int OK = 0;
  private static final int ERROR = 1;
  private static final int RUNNING = 4;
  private static final int QUEUED = 5;
  private static final String QUERY = "getActivityExecutionList";
  private static final String NAME_FILTER = "activity probe";
  private static final String RUN_ID = "run-activity-probe";
  private static final String[] COUNTERS = {"running", "waiting", "queued", "errorsLast24h"};

  @Autowired
  private DataSource dataSource;
  @Autowired
  private JWTTokenService jwtTokenService;
  @Autowired
  private TaskDAO taskDAO;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeExecutions() {
    jdbc = new JdbcTemplate(dataSource);
    deleteExecutions();
    task(ALPHA, "Activity Probe Alpha");
    task(BETA, "Activity Probe Beta");
    execution(ALPHA, 1, "SCHEDULED_TASK", OK, LocalDateTime.of(2019, 2, 1, 10, 0), null);
    execution(ALPHA, 2, "MANUAL_TASK", ERROR, LocalDateTime.of(2019, 2, 2, 10, 0), null);
    execution(BETA, 1, "DEPENDANT_TASK", OK, LocalDateTime.of(2019, 2, 2, 12, 0), RUN_ID);
    // The run started by the first execution of Alpha
    jdbc.update("INSERT INTO AweSchRun (IdeRun, IdeTskRoo, ExeTskRoo, LchBy, IniDat, EndDat, Sta) "
        + "VALUES (?, ?, 1, 'test', ?, ?, 0)", RUN_ID, ALPHA, Timestamp.valueOf(LocalDateTime.of(2019, 2, 1, 10, 0)),
      Timestamp.valueOf(LocalDateTime.of(2019, 2, 2, 12, 1)));
    execution(ALPHA, 3, "MANUAL_TASK", RUNNING, LocalDateTime.of(2019, 2, 3, 10, 0), null);
    jdbc.update("INSERT INTO AweSchExeExt (IdeTsk, ExeTsk, ExtRef, DfrDat, DueDat) VALUES (?, 3, 'probe', ?, ?)",
      ALPHA, Timestamp.valueOf(LocalDateTime.of(2019, 2, 3, 10, 0)),
      Timestamp.valueOf(LocalDate.now().plusDays(1).atTime(12, 30)));
    execution(BETA, 2, "FILE_TRACKING_TASK", RUNNING, LocalDateTime.of(2019, 2, 4, 10, 0), null);
    execution(BETA, 3, "SCHEDULED_TASK", QUEUED, LocalDateTime.of(2019, 2, 5, 10, 0), null);
    execution(BETA, 4, "MANUAL_TASK", ERROR, LocalDateTime.now().minusHours(1), null);
  }

  @AfterEach
  void deleteExecutions() {
    for (String table : new String[]{"AweSchExeExt", "AweSchExe"}) {
      jdbc.update("DELETE FROM " + table + " WHERE IdeTsk IN (?, ?)", ALPHA, BETA);
    }
    jdbc.update("DELETE FROM AweSchTsk WHERE Ide IN (?, ?)", ALPHA, BETA);
    jdbc.update("DELETE FROM AweSchRun WHERE IdeRun = ?", RUN_ID);
  }

  @Test
  void theExecutionsOfAllTasksArePagedInTheDatabaseNewestFirst() throws Exception {
    JsonNode dataList = dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("page", 2).put("max", 3));

    assertThat(dataList.get("records").asInt()).isEqualTo(7);
    assertThat(dataList.get("total").asInt()).isEqualTo(3);
    assertThat(dataList.get("page").asInt()).isEqualTo(2);
    List<JsonNode> rows = rows(dataList);
    assertThat(keys(rows)).containsExactly(ALPHA + "-3", BETA + "-1", ALPHA + "-2");
    assertThat(column(rows, "TskNam")).containsExactly("Activity Probe Alpha", "Activity Probe Beta",
      "Activity Probe Alpha");
    // The run link keeps the run id as value and shows its root execution
    assertThat(rows.get(1).get("ExeRun").get("value").asString()).isEqualTo(RUN_ID);
    assertThat(rows.get(1).get("ExeRun").get("label").asString()).isEqualTo("#1");
    assertThat(rows.get(1).get("ExeRun").get("title").asString()).isEqualTo("Activity Probe Alpha #1");
    assertThat(rows.get(1).get("ExeLchBy").asString()).endsWith("test");
  }

  @Test
  void theSortOfTheGridSortsEveryExecutionBeforeThePageAndAnUnknownColumnIsRefused() throws Exception {
    List<JsonNode> byTask = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("page", 1).put("max", 3)
      .set("sort", sort("TskNam", "asc"))));
    // The executions that ended took 60 s (the newest first among them); the others are measured until now
    List<JsonNode> byElapsed = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("page", 2).put("max", 3)
      .set("sort", sort("ExeEla", "asc"))));
    JsonNode refused = find(actions(QUERY, params().put("CrtExeTsk", NAME_FILTER).set("sort", sort("ExeDes", "asc"))),
      "message");

    assertThat(keys(byTask)).containsExactly(ALPHA + "-3", ALPHA + "-2", ALPHA + "-1");
    assertThat(keys(byElapsed)).containsExactly(ALPHA + "-1", BETA + "-3", BETA + "-2");
    assertThat(byElapsed.get(0).get("ExeEla").asInt()).isEqualTo(60);
    assertThat(refused.get("parameters").get("title").asString()).isEqualTo("Invalid filter");
  }

  @Test
  void statusAndOriginFiltersKeepTheExecutionsInAnyOfTheirValues() throws Exception {
    List<JsonNode> rows = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER)
      .set("CrtExeSta", ints(RUNNING, QUEUED))
      .set("CrtExeOri", strings("file", "scheduled"))));

    assertThat(keys(rows)).containsExactly(BETA + "-3", BETA + "-2");
    assertThat(column(rows, "ExeOri")).containsExactly("scheduled", "file");
  }

  @Test
  void theDeferredFilterSplitsTheRunningExecutionsByTheirDeferredState() throws Exception {
    List<JsonNode> waiting = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeDfr", 1)));
    List<JsonNode> running = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeDfr", 0)));

    assertThat(keys(waiting)).containsExactly(ALPHA + "-3");
    assertThat(waiting.get(0).get("ExeDfr").asInt()).isEqualTo(1);
    // Due tomorrow: the day comes before the time, so it does not read as a time of today
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    String day = DateTimeFormatter.ofPattern(tomorrow.getYear() == LocalDate.now().getYear() ? "dd/MM" : "dd/MM/yyyy")
      .format(tomorrow);
    assertThat(waiting.get(0).get("ExeDueTim").asString()).isEqualTo(day + " 12:30");
    assertThat(keys(running)).containsExactly(BETA + "-2");
    assertThat(running.get(0).get("ExeDfr").asInt()).isZero();
  }

  @Test
  void theStateQuickFilterKeepsTheExecutionsOfItsOption() throws Exception {
    List<JsonNode> waiting = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeQck", "waiting")));
    List<JsonNode> running = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeQck", "running")));
    List<JsonNode> errors = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeQck", "error")));
    List<JsonNode> all = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeQck", "")));

    assertThat(keys(waiting)).containsExactly(ALPHA + "-3");
    assertThat(keys(running)).containsExactly(BETA + "-2");
    assertThat(keys(errors)).containsExactly(BETA + "-4", ALPHA + "-2");
    assertThat(all).hasSize(7);
  }

  @Test
  void theStateCountsCountEachOptionWithTheSearchAndTheOtherFiltersOfTheList() throws Exception {
    List<JsonNode> probes = rows(dataList("getActivityStateCounts", params().put("CrtExeTsk", NAME_FILTER)
      .put("CrtExeQck", "error")));
    List<JsonNode> manual = rows(dataList("getActivityStateCounts", params().put("CrtExeTsk", NAME_FILTER)
      .set("CrtExeOri", strings("manual"))));

    assertThat(column(probes, "value")).containsExactly("", "running", "waiting", "queued", "error", "ok");
    assertThat(probes.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(7, 1, 1, 1, 2, 2);
    assertThat(manual.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(3, 0, 1, 0, 2, 0);
    JsonNode message = find(actions("getActivityStateCounts", params().put("CrtExeTsk", NAME_FILTER)
      .set("CrtExeOri", strings("api"))), "message");
    assertThat(message.get("parameters").get("type").asString()).isEqualTo("error");
  }

  @Test
  void theStartFiltersKeepTheDaysOfTheRangeOrTheLastHours() throws Exception {
    List<JsonNode> range = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER)
      .put("CrtExeWin.from", "02/02/2019").put("CrtExeWin.to", "03/02/2019")));
    List<JsonNode> lastDay = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER).put("CrtExeWin", 24)));

    assertThat(keys(range)).containsExactly(ALPHA + "-3", BETA + "-1", ALPHA + "-2");
    assertThat(keys(lastDay)).containsExactly(BETA + "-4");
  }

  @Test
  void theScreenCountersCountEveryTaskWithTheQueriesOfTheProbeCounters() throws Exception {
    // The production path, as the screen launches it: data/getActivityCounters under scheduler.read. It counts every
    // task (the query has no task filter) and other tests or the scheduler may store executions meanwhile, so each
    // counter is at least the one of the probe tasks; countersCountExactlyTheirQuickFilter proves the exact counts
    Map<String, CellData> probes = taskDAO.getExecutionCounters(NAME_FILTER).getDataList().getRows().get(0);
    List<JsonNode> rows = rows(dataList("getActivityCounters", params()));

    assertThat(rows).hasSize(1);
    JsonNode counters = rows.get(0);
    for (String counter : COUNTERS) {
      assertThat(counters.get(counter).isNumber()).as(counter + ": " + counters).isTrue();
      assertThat(counters.get(counter).asLong()).as(counter).isGreaterThanOrEqualTo(count(probes, counter))
        .isPositive();
    }
    assertThat(counters.get("upcomingNext24h").asInt()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void theProgressOfARunningExecutionIsMeasuredWithTheAverageOfItsOwnTask() throws Exception {
    // Alpha takes 1 minute on average (its OK execution); Beta 5.5 minutes (1 and 10 minutes)
    jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy) "
        + "VALUES (?, 'MANUAL_TASK', 5, ?, ?, 600000, 0, 'test')", BETA,
      Timestamp.valueOf(LocalDateTime.of(2019, 2, 6, 10, 0)), Timestamp.valueOf(LocalDateTime.of(2019, 2, 6, 10, 10)));
    LocalDateTime start = LocalDateTime.now().minusSeconds(30).truncatedTo(ChronoUnit.SECONDS);
    long startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    execution(ALPHA, 4, "MANUAL_TASK", RUNNING, start, null);

    long before = System.currentTimeMillis();
    JsonNode progress = rows(dataList(QUERY, params().put("CrtExeTsk", NAME_FILTER))).stream()
      .filter(row -> (ALPHA + "-4").equals(row.get("IdeTsk").asInt() + "-" + row.get("ExeTsk").asInt()))
      .findFirst().orElseThrow().get("ExePrg");
    long after = System.currentTimeMillis();

    // The elapsed time of the execution, measured around the request, over Alpha's minute: at least 50 % (30 s),
    // whatever the request takes (Beta's average would give 9 %, none 100 %). Over 100 % the bar is full, unlabelled
    long lowest = Math.min(100, (before - startMillis) * 100 / 60_000);
    long highest = Math.min(100, (after - startMillis) * 100 / 60_000);
    long value = progress.get("value").asLong();
    assertThat(value).as(progress.toString()).isBetween(lowest, highest);
    assertThat(progress.get("label").asString()).isIn(value + "%", value == 100 ? "" : value + "%");
  }

  @Test
  void unknownOriginOrMalformedDateDoNotWidenTheList() throws Exception {
    List<ObjectNode> invalid = List.of(
      params().put("CrtExeTsk", NAME_FILTER).set("CrtExeOri", strings("api")),
      params().put("CrtExeTsk", NAME_FILTER).set("CrtExeOri", strings("api", "manual")),
      params().put("CrtExeTsk", NAME_FILTER).put("CrtExeWin.from", "2019-02-02"),
      params().put("CrtExeTsk", NAME_FILTER).put("CrtExeDfr", 2),
      params().put("CrtExeTsk", NAME_FILTER).put("CrtExeQck", "api"));

    for (ObjectNode parameters : invalid) {
      JsonNode actions = actions(QUERY, parameters);
      List<String> types = new ArrayList<>();
      actions.forEach(action -> types.add(action.get("type").asString()));
      assertThat(types).as("%s: %s", parameters, actions).doesNotContain("fill").contains("message");
      JsonNode message = find(actions, "message");
      assertThat(message.get("parameters").get("type").asString()).as(actions.toString()).isEqualTo("error");
      assertThat(message.get("parameters").get("title").asString()).isEqualTo("Invalid filter");
    }
  }

  @Test
  void countersCountExactlyTheirQuickFilter() throws Exception {
    // The queries of the screen counters scoped to the probe tasks (the production query has no task filter), so the
    // executions that other tests or the scheduler start meanwhile on the shared database do not count
    Map<String, CellData> with = taskDAO.getExecutionCounters(NAME_FILTER).getDataList().getRows().get(0);
    deleteExecutions();
    Map<String, CellData> without = taskDAO.getExecutionCounters(NAME_FILTER).getDataList().getRows().get(0);

    for (String counter : COUNTERS) {
      assertThat(count(with, counter) - count(without, counter)).as(counter).isEqualTo(1L);
    }
  }

  @Test
  void theActivityApiRequiresTheBearerAndRefusesAnInvalidFilterWith400() throws Exception {
    String body = "{\"taskName\":\"activity probe\",\"origins\":[\"manual\"],\"page\":1,\"max\":10}";

    assertThat(api(post("/scheduler/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(body), false)
      .getStatus()).isEqualTo(401);
    assertThat(api(get("/scheduler/api/v1/executions/counters"), false).getStatus()).isEqualTo(401);
    MockHttpServletResponse page = api(post("/scheduler/api/v1/executions")
      .contentType(MediaType.APPLICATION_JSON).content(body), true);
    assertThat(page.getStatus()).isEqualTo(200);
    JsonNode rows = objectMapper.readTree(page.getContentAsString()).get("dataList").get("rows");
    assertThat(rows).hasSize(3);
    assertThat(rows.get(1).get("ExeDfr").asInt()).isOne();
    assertThat(rows.get(1).has("DfrExe")).as("internal key").isFalse();
    assertThat(api(get("/scheduler/api/v1/executions/counters"), true).getStatus()).isEqualTo(200);
    MockHttpServletResponse invalid = api(post("/scheduler/api/v1/executions")
      .contentType(MediaType.APPLICATION_JSON).content("{\"origins\":[\"api\"]}"), true);
    assertThat(invalid.getStatus()).isEqualTo(400);
    assertThat(objectMapper.readTree(invalid.getContentAsString()).get("type").asString()).isEqualTo("invalid-request");
  }

  private long count(Map<String, CellData> counters, String counter) {
    return ((Number) counters.get(counter).getObjectValue()).longValue();
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

  private JsonNode find(JsonNode actions, String type) {
    for (JsonNode action : actions) {
      if (type.equals(action.get("type").asString())) {
        return action;
      }
    }
    throw new AssertionError("No " + type + " action: " + actions);
  }

  private void task(int id, String name) {
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, TypExe, CmdExe, TypLch, Act) VALUES (?, ?, 0, 'none', 0, 0)", id, name);
  }

  private void execution(int taskId, int executionId, String group, int status, LocalDateTime start, String runId) {
    jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy, IdeRun) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'test', ?)", taskId, group, executionId, Timestamp.valueOf(start),
      status == RUNNING || status == QUEUED ? null : Timestamp.valueOf(start.plusMinutes(1)),
      status == RUNNING || status == QUEUED ? null : 60_000, status, runId);
  }

  private JsonNode sort(String column, String direction) {
    return objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("id", column)
      .put("direction", direction));
  }

  private List<String> keys(List<JsonNode> rows) {
    return rows.stream().map(row -> row.get("IdeTsk").asInt() + "-" + row.get("ExeTsk").asInt()).toList();
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
