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
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
}
