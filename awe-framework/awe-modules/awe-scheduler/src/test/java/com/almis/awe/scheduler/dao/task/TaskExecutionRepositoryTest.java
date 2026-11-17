package com.almis.awe.scheduler.dao.task;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests of the run id handling of {@link TaskExecutionRepository#startTask(Task)}.
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionRepositoryTest {

  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private FlowRunRepository flowRuns;

  private TaskExecutionRepository repository;

  @BeforeEach
  void init() throws Exception {
    repository = new TaskExecutionRepository(queryService, maintainService, queryUtil, executionLogStore, 5, flowRuns);
    given(queryUtil.getParameters()).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters(any(), any(), any())).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(3));
    row.put("executionId", new CellData(7));
    DataList dataList = new DataList();
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
  }

  @Test
  void aTaskWithoutRunCreatesItsRunAndKeepsTheTaskUntouched() throws Exception {
    Task task = task(3);

    TaskExecution execution = repository.startTask(task);

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("startTask"), parameters.capture());
    String runId = parameters.getValue().get("runId").asString();
    assertNotNull(runId);
    assertEquals(runId, execution.getRunId());
    verify(flowRuns).createRun(runId, task, execution);
    assertNull(task.getRunId());
  }

  @Test
  void aDependencyInheritsTheRunAndMarksItsJoinRowAsStarted() throws Exception {
    Task task = task(3).setRunId("run-1");

    TaskExecution execution = repository.startTask(task);

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("startTask"), parameters.capture());
    assertEquals("run-1", parameters.getValue().get("runId").asString());
    assertEquals("run-1", execution.getRunId());
    verify(flowRuns).markStarted("run-1", 3, 7);
    verify(flowRuns, never()).createRun(any(), any(), any());
  }

  @Test
  void theExecutionIdIsReadBeforeTheInsertSoTheInsertHasNoSubqueryOnItsOwnTable() throws Exception {
    // MySQL rejects an INSERT ... VALUES with a subquery on the target table (#991)
    Map<String, CellData> max = new HashMap<>();
    max.put("maxExecution", new CellData(6));
    DataList maxList = new DataList();
    maxList.addRow(max);
    given(queryService.launchPrivateQuery(eq("getMaxTaskExecution"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(maxList));

    repository.startTask(task(3));

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("startTask"), parameters.capture());
    assertEquals(7, parameters.getValue().get("taskExecution").asInt());
  }

  @Test
  void theStartedExecutionIsReadBackByTheIdItWasInsertedWith() throws Exception {
    // MySQL stores the start date in seconds: two starts in the same second tie on the latest date (#991)
    repository.startTask(task(3));

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("getTaskExecution"), parameters.capture());
    assertEquals(1, parameters.getValue().get("taskExecution").asInt());
    verify(queryService, never()).launchPrivateQuery(eq("getLastExecution"), any(ObjectNode.class));
  }

  private Task task(int taskId) {
    Task task = new Task().setTaskId(taskId);
    task.setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    return task;
  }
}
