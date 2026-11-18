package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
  void runScreenQueriesListTheRunAndItsSkippedTasks() throws Exception {
    FlowRunRepository runs = new FlowRunRepository(queryService, maintainService, queryUtil);
    String runId = UUID.randomUUID().toString();
    runs.createRun(runId, new Task().setTaskId(1).setLauncher("test"), new TaskExecution().setExecutionId(4));
    runs.register(runId, 7, List.of());
    runs.transition(runId, 7, "WAITING", "SKIPPED");

    ObjectNode byRun = queryUtil.getParameters(null, "1", "0");
    byRun.put("ExeRun.selected", runId);
    ObjectNode selected = queryUtil.getParameters(null, "1", "0");
    selected.put("IdeRun.selected", runId);

    DataList runList = queryService.launchPrivateQuery("getFlowRunList", byRun).getDataList();
    DataList executions = queryService.launchPrivateQuery("getFlowRunExecutionList", selected).getDataList();
    DataList skipped = queryService.launchPrivateQuery("getFlowRunSkippedList", selected).getDataList();

    assertThat(runList.getRows()).hasSize(1);
    assertThat(runList.getRows().get(0).get("RunExe").getIntegerValue()).isEqualTo(4);
    assertThat(executions.getRows()).isEmpty();
    assertThat(skipped.getRows()).hasSize(1);
    assertThat(skipped.getRows().get(0).get("RsTskId").getIntegerValue()).isEqualTo(7);
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

  private Map<String, String> graphRows(ObjectNode parameters) throws Exception {
    return queryService.launchPrivateQuery("getFlowGraph", parameters).getDataList().getRows().stream()
      .filter(row -> row.get("taskId").getIntegerValue() >= 9901 && row.get("taskId").getIntegerValue() <= 9904)
      .collect(Collectors.toMap(row -> row.get("taskId").getStringValue() + "-" + row.get("parentId").getStringValue(),
        row -> "root=" + row.get("rootRun").getStringValue() + " task=" + row.get("rootTask").getStringValue()
          + " run=" + row.get("runStatus").getStringValue() + " exe=" + row.get("executionStatus").getStringValue()));
  }
}
