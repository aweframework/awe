package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.dao.task.FlowRunRepository;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The guarded updates of the flow run tables decide who launches a child and who ends the run: the first caller
 * changes the row and the next one does not.
 */
@Tag("integration")
@WithMockUser(username = "test", password = "test")
class SchedulerFlowRunTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private QueryService queryService;

  @Autowired
  private MaintainService maintainService;

  @Autowired
  private QueryUtil queryUtil;

  @Autowired
  private DataSource dataSource;

  @Test
  void guardedUpdatesChangeTheRowOnlyForTheFirstCaller() throws Exception {
    FlowRunRepository runs = new FlowRunRepository(queryService, maintainService, queryUtil);
    String runId = UUID.randomUUID().toString();
    runs.createRun(runId, new Task().setTaskId(1).setLauncher("test").setRunParameters(Map.of("date", "2026-11-16")),
      new TaskExecution().setExecutionId(4));

    runs.register(runId, 7, List.of());
    runs.register(runId, 7, List.of());

    assertThat(runs.getRootTaskId(runId)).isEqualTo(1);
    assertThat(runs.getParameters(runId)).containsExactly(Map.entry("date", "2026-11-16"));
    assertThat(runs.getJoins(runId)).containsExactly(new JoinRow(7, "WAITING", null));
    assertThat(runs.transition(runId, 7, "WAITING", "LAUNCHED")).isTrue();
    assertThat(runs.transition(runId, 7, "WAITING", "LAUNCHED")).isFalse();
    runs.markStarted(runId, 7, 3);
    assertThat(runs.getJoins(runId)).containsExactly(new JoinRow(7, "LAUNCHED", 3));
    assertThat(runs.endRun(runId, TaskStatus.JOB_OK)).isTrue();
    assertThat(runs.endRun(runId, TaskStatus.JOB_ERROR)).isFalse();
  }

  @Test
  void theRunShowsTheResolvedParametersOfTheRootButItsDependenciesReceiveOnlyTheOperatorValues() throws Exception {
    FlowRunRepository runs = new FlowRunRepository(queryService, maintainService, queryUtil);
    String runId = UUID.randomUUID().toString();
    Task root = new Task().setTaskId(1).setLauncher("test").setRunParameters(Map.of("date", "2026-11-18"))
      .setParameterList(List.of(
        new TaskParameter().setName("date").setSource("1").setValue("2026-01-01"),
        new TaskParameter().setName("mode").setSource("0").setValue("FULL"),
        new TaskParameter().setName("env").setSource("1"),
        new TaskParameter().setName("password").setSource("2").setValue("mail.password")));
    runs.createRun(runId, root, new TaskExecution().setExecutionId(4));

    // What a dependency of the run receives is the same as before the resolved parameters were stored
    assertThat(runs.getParameters(runId)).containsExactly(Map.entry("date", "2026-11-18"));

    ObjectNode byRun = queryUtil.getParameters(null, "1", "0");
    byRun.put("ExeRun.selected", runId);
    DataList shown = queryService.launchPrivateQuery("getActivityExecutionParameters", byRun).getDataList();
    assertThat(shown.getRows()).extracting(row -> row.get("ParNam").getStringValue() + "=" + row.get("ParVal").getStringValue())
      .containsExactly("date=2026-11-18", "env=—", "mode=FULL");
  }

  @Test
  void flowGraphQueryMarksTheRootAndTheStateOfEachTaskOfTheRun() throws Exception {
    // Diamond 9901 -> 9902, 9901 -> 9903, 9902 -> 9904, 9903 -> 9904 where 9903 ends in error and 9904 is skipped
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    FlowRunRepository runs = new FlowRunRepository(queryService, maintainService, queryUtil);
    String runId = UUID.randomUUID().toString();
    try {
      for (int task = 9901; task <= 9904; task++) {
        jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, TypExe, CmdExe, TypLch) VALUES (?, ?, 0, 'cmd', 0)", task,
          "Graph " + task);
      }
      jdbc.update("INSERT INTO AweSchTskDpn (IdeTsk, IdePrn) "
        + "VALUES (9902, 9901), (9903, 9901), (9904, 9902), (9904, 9903)");
      runs.createRun(runId, new Task().setTaskId(9901).setLauncher("test"), new TaskExecution().setExecutionId(1));
      for (int[] execution : new int[][]{{9901, 0}, {9902, 0}, {9903, 1}}) {
        jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, Sta, IdeRun) "
          + "VALUES (?, 'g', 1, CURRENT_TIMESTAMP, ?, ?)", execution[0], execution[1], runId);
      }
      runs.register(runId, 9904, List.of());
      runs.transition(runId, 9904, "WAITING", "SKIPPED");

      ObjectNode byRun = queryUtil.getParameters(null, "1", "0");
      byRun.put("IdeRun.selected", runId);
      ObjectNode byTask = queryUtil.getParameters(null, "1", "0");
      byTask.put("IdeTsk.selected", 9902);

      Map<String, String> run = graphRows(byRun);
      Map<String, String> definition = graphRows(byTask);

      assertThat(run).containsEntry("9901-", "root=" + runId + " task= run= exe=0")
        .containsEntry("9903-9901", "root= task= run= exe=1")
        .containsEntry("9904-9902", "root= task= run=SKIPPED exe=")
        .containsEntry("9904-9903", "root= task= run=SKIPPED exe=");
      assertThat(definition).containsEntry("9902-9901", "root= task=9902 run= exe=")
        .containsEntry("9901-", "root= task= run= exe=");
    } finally {
      jdbc.update("DELETE FROM AweSchRunTsk WHERE IdeRun = ?", runId);
      jdbc.update("DELETE FROM AweSchRun WHERE IdeRun = ?", runId);
      jdbc.update("DELETE FROM AweSchExe WHERE IdeTsk BETWEEN 9901 AND 9904");
      jdbc.update("DELETE FROM AweSchTskDpn WHERE IdeTsk BETWEEN 9901 AND 9904");
      jdbc.update("DELETE FROM AweSchTsk WHERE Ide BETWEEN 9901 AND 9904");
    }
  }

  @Test
  void flowRunPageQueriesGiveTheSummaryTheLastExecutionOfEachTaskAndTheStateOfEachEdge() throws Exception {
    // 9911 -> 9912 (launches on warning, relaunched: error then warning), 9911 -> 9913, {9912, 9913} -> 9914 (join),
    // 9911 -> 9916 (error) -> 9915 (skipped) -> 9917 (never reached: the run ended without it, so it was skipped)
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    FlowRunRepository runs = new FlowRunRepository(queryService, maintainService, queryUtil);
    String runId = UUID.randomUUID().toString();
    try {
      for (int task = 9911; task <= 9917; task++) {
        jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, TypExe, CmdExe, TypLch, LchDepWrn) VALUES (?, ?, 0, 'cmd', 0, ?)",
          task, "Graph " + task, task == 9912 ? 1 : 0);
      }
      jdbc.update("INSERT INTO AweSchTskDpn (IdeTsk, IdePrn) VALUES (9912, 9911), (9913, 9911), (9914, 9912), "
        + "(9914, 9913), (9916, 9911), (9915, 9916), (9917, 9915)");
      runs.createRun(runId, new Task().setTaskId(9911).setLauncher("test").setRunParameters(Map.of("date", "2026-11-24")),
        new TaskExecution().setExecutionId(1));
      execution(jdbc, runId, 9911, 1, 0, "08:00", "08:01");
      execution(jdbc, runId, 9912, 1, 1, "08:01", "08:02");
      execution(jdbc, runId, 9912, 2, 2, "08:03", "08:05");
      execution(jdbc, runId, 9913, 1, 0, "08:01", "08:04");
      execution(jdbc, runId, 9914, 1, 0, "08:06", "08:07");
      execution(jdbc, runId, 9916, 1, 1, "08:01", "08:02");
      runs.register(runId, 9915, List.of());
      runs.transition(runId, 9915, "WAITING", "SKIPPED");
      runs.endRun(runId, TaskStatus.JOB_ERROR);

      ObjectNode byRun = queryUtil.getParameters(null, "1", "0");
      byRun.put("IdeRun", runId);
      List<Map<String, CellData>> graph = queryService.launchPrivateQuery("getFlowRunGraph", byRun).getDataList()
        .getRows();
      Map<String, Map<String, CellData>> rows = graph.stream().collect(Collectors.toMap(
        row -> row.get("taskId").getStringValue() + "-" + row.get("parentId").getStringValue(), row -> row));

      // One row per task and parent of the run: the relaunched task once, with its last execution
      assertThat(rows).hasSize(8).containsOnlyKeys("9911-", "9912-9911", "9913-9911", "9914-9912", "9914-9913",
        "9916-9911", "9915-9916", "9917-9915");
      assertThat(text(rows.get("9911-"), "rootRun", "executionId", "executionStatus", "edgeState"))
        .isEqualTo(runId + "|1|0|");
      assertThat(text(rows.get("9912-9911"), "executionId", "executionStatus", "startDate", "endDate", "duration",
        "edgeState")).isEqualTo("2|2|17/11/2026 08:03:00|17/11/2026 08:05:00|120000|MET");
      assertThat(text(rows.get("9914-9912"), "joinedBy", "joinWait", "edgeState")).isEqualTo("9912|60000|CLOSED_JOIN");
      assertThat(text(rows.get("9914-9913"), "joinedBy", "joinWait", "edgeState")).isEqualTo("9912|60000|MET");
      assertThat(text(rows.get("9915-9916"), "runStatus", "executionId", "edgeState")).isEqualTo("SKIPPED||NOT_MET");
      assertThat(rows.get("9915-9916").get("skipCause").getStringValue()).contains("Graph 9916");
      assertThat(text(rows.get("9917-9915"), "runStatus", "executionId", "state", "edgeState")).isEqualTo("||skipped|CUT");
      assertThat(rows.get("9917-9915").get("skipCause").getStringValue()).contains("Graph 9915");

      Map<String, CellData> summary = queryService.launchPrivateQuery("getFlowRunSummary", byRun).getDataList()
        .getRows().get(0);
      assertThat(text(summary, "Sta", "RunRooTsk", "RunRooExe", "RunPntTsk", "RunTskTot", "RunTskEnd", "RunCntOk",
        "RunCntWrn", "RunCntErr", "RunCntSkp", "RunCntPnd")).isEqualTo("1|9911|1|9916|7|7|3|1|1|2|0");
      assertThat(summary.get("RunPntTxt").getStringValue()).contains("Graph 9916");

      DataList parameters = queryService.launchPrivateQuery("getFlowRunParameters", byRun).getDataList();
      assertThat(parameters.getRows()).extracting(row -> row.get("ParNam").getStringValue() + "="
        + row.get("ParVal").getStringValue()).containsExactly("date=2026-11-24");
    } finally {
      jdbc.update("DELETE FROM AweSchRunTsk WHERE IdeRun = ?", runId);
      jdbc.update("DELETE FROM AweSchRunPar WHERE IdeRun = ?", runId);
      jdbc.update("DELETE FROM AweSchRun WHERE IdeRun = ?", runId);
      jdbc.update("DELETE FROM AweSchExe WHERE IdeTsk BETWEEN 9911 AND 9917");
      jdbc.update("DELETE FROM AweSchTskDpn WHERE IdeTsk BETWEEN 9911 AND 9917");
      jdbc.update("DELETE FROM AweSchTsk WHERE Ide BETWEEN 9911 AND 9917");
    }
  }

  private static void execution(JdbcTemplate jdbc, String runId, int task, int execution, int status, String start,
                                String end) {
    jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, Sta, IdeRun) VALUES (?, 'g', ?, ?, ?, ?, ?)",
      task, execution, Timestamp.valueOf("2026-11-17 " + start + ":00"), Timestamp.valueOf("2026-11-17 " + end + ":00"),
      status, runId);
  }

  private static String text(Map<String, CellData> row, String... fields) {
    return Stream.of(fields).map(field -> row.get(field) == null ? "<none>" : row.get(field).getStringValue())
      .collect(Collectors.joining("|"));
  }

  private Map<String, String> graphRows(ObjectNode parameters) throws Exception {
    return queryService.launchPrivateQuery("getFlowGraph", parameters).getDataList().getRows().stream()
      .filter(row -> row.get("taskId").getIntegerValue() >= 9901 && row.get("taskId").getIntegerValue() <= 9904)
      .collect(Collectors.toMap(row -> row.get("taskId").getStringValue() + "-" + row.get("parentId").getStringValue(),
        row -> "root=" + row.get("rootRun").getStringValue() + " task=" + row.get("rootTask").getStringValue()
          + " run=" + row.get("runStatus").getStringValue() + " exe=" + row.get("executionStatus").getStringValue()));
  }
}
