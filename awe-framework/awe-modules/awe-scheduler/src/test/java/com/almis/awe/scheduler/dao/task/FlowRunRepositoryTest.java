package com.almis.awe.scheduler.dao.task;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Launch parameters of a flow run (AweSchRunPar): the operator rows, which the dependencies of the run receive, and the
 * resolved parameters of the root task, stored with a reserved prefix only to show them.
 */
@ExtendWith(MockitoExtension.class)
class FlowRunRepositoryTest {

  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;

  private FlowRunRepository repository;

  @BeforeEach
  void init() {
    repository = new FlowRunRepository(queryService, maintainService, queryUtil);
  }

  @Test
  void theRunStoresTheOperatorRowsAsTheyAreAndTheOtherValueAndVariableParametersWithThePrefix() throws Exception {
    given(queryUtil.getParameters()).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    Map<String, String> operator = new LinkedHashMap<>();
    operator.put("date", "2026-11-18");
    operator.put("key", "operator.typed.key");
    Task task = new Task().setTaskId(1).setLauncher("pvidal").setRunParameters(operator).setParameterList(List.of(
      parameter("date", "1", "2026-01-01"),
      parameter("mode", "0", "FULL"),
      parameter("env", "1", "PRE"),
      parameter("empty", "0", null),
      parameter("key", "2", "task.key"),
      parameter("secret", "2", "mail.password")));

    repository.createRun("run-1", task, new TaskExecution().setExecutionId(4));

    assertEquals(Map.of("date", "2026-11-18", "key", "operator.typed.key", "=mode", "FULL", "=env", "PRE",
      "=empty", ""), storedRows("run-1", 5));
  }

  @Test
  void aRunWithoutOperatorValuesStoresTheResolvedParametersOnly() throws Exception {
    given(queryUtil.getParameters()).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    Task task = new Task().setTaskId(1).setLauncher("scheduler")
      .setParameterList(List.of(parameter("mode", "0", "FULL"), parameter("file", "1", "a.n43")));

    repository.createRun("run-2", task, new TaskExecution().setExecutionId(1));

    assertEquals(Map.of("=mode", "FULL", "=file", "a.n43"), storedRows("run-2", 2));
  }

  @Test
  void aNameWithNoRoomForThePrefixIsNotStoredAsResolved() throws Exception {
    given(queryUtil.getParameters()).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    String longest = "n".repeat(40);
    Task task = new Task().setTaskId(1).setLauncher("scheduler")
      .setParameterList(List.of(parameter(longest, "0", "x"), parameter("mode", "0", "FULL")));

    repository.createRun("run-3", task, new TaskExecution().setExecutionId(1));

    assertEquals(Map.of("=mode", "FULL"), storedRows("run-3", 1));
  }

  @Test
  void theDependenciesReceiveOnlyTheOperatorRowsOfTheRun() throws Exception {
    givenStoredRows(Map.of("date", "2026-11-18", "=mode", "FULL", "=date", "2026-01-01"));

    assertEquals(Map.of("date", "2026-11-18"), repository.getParameters("run-1"));
  }

  @Test
  void theLaunchParametersAreBothKindsWithoutThePrefixAndTheOperatorValueWinsSortedByName() throws Exception {
    givenStoredRows(Map.of("date", "2026-11-18", "=mode", "FULL", "=date", "2026-01-01", "=env", ""));

    Map<String, String> launch = repository.getLaunchParameters("run-1");

    assertEquals(List.of("date", "env", "mode"), List.copyOf(launch.keySet()));
    assertEquals("2026-11-18", launch.get("date"));
    assertEquals("FULL", launch.get("mode"));
  }

  @Test
  void theDependenciesKeepTheOrderOfTheirQueryWhateverTheSortOfTheRequest() throws Exception {
    // A launch from a sorted grid carries the sort of its columns, which are not fields of the dependencies
    given(queryUtil.getParameters(null, "1", "0")).willAnswer(call -> {
      ObjectNode request = JsonNodeFactory.instance.objectNode();
      request.putArray("sort").addObject().put("id", "TskNam").put("direction", "desc");
      return request;
    });
    DataList dataList = new DataList();
    for (int[] dependency : new int[][]{{3, 1}, {2, 1}, {4, 3}}) {
      Map<String, CellData> row = new HashMap<>();
      row.put("taskId", new CellData(dependency[0]));
      row.put("parentId", new CellData(dependency[1]));
      dataList.addRow(row);
    }
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    given(queryService.launchPrivateQuery(eq("allTaskDependencies"), parameters.capture()))
      .willReturn(new ServiceData().setDataList(dataList));

    List<TaskDependency> dependencies = repository.getDependencies();

    assertEquals(0, parameters.getValue().get("sort").size());
    assertEquals(List.of(3, 2, 4), dependencies.stream().map(TaskDependency::getTaskId).toList());
    assertEquals(List.of(1, 1, 3), dependencies.stream().map(TaskDependency::getParentId).toList());
  }

  private Map<String, String> storedRows(String runId, int count) throws Exception {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, org.mockito.Mockito.times(count))
      .launchPrivateMaintain(eq("insertFlowRunParameter"), captor.capture());
    Map<String, String> rows = new HashMap<>();
    for (ObjectNode row : captor.getAllValues()) {
      assertEquals(runId, row.get("runId").asString());
      rows.put(row.get("name").asString(), row.get("value").isNull() ? "" : row.get("value").asString());
    }
    return rows;
  }

  private void givenStoredRows(Map<String, String> stored) throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willAnswer(call -> JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    stored.forEach((name, value) -> {
      Map<String, CellData> row = new HashMap<>();
      row.put("name", new CellData(name));
      row.put("value", new CellData(value));
      dataList.addRow(row);
    });
    given(queryService.launchPrivateQuery(eq("flowRunParameters"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
  }

  private static TaskParameter parameter(String name, String source, String value) {
    return new TaskParameter().setName(name).setSource(source).setValue(value);
  }

  @Test
  void theParentsOfATaskReplaceOnlyItsOwnParentRowsWithoutAnOrder() throws Exception {
    given(queryUtil.getParameters()).willAnswer(call -> JsonNodeFactory.instance.objectNode());

    repository.replaceParents(5, List.of(4, 2));

    ArgumentCaptor<String> targets = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ObjectNode> rows = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, org.mockito.Mockito.times(3)).launchPrivateMaintain(targets.capture(), rows.capture());
    assertEquals(List.of("deleteTaskParents", "insertTaskParent", "insertTaskParent"), targets.getAllValues());
    assertEquals("{\"taskId\":5}", rows.getAllValues().get(0).toString());
    // DpnOrd orders the children of a parent in its flows: the parents saved from the child leave it empty
    assertEquals("{\"taskId\":5,\"parentId\":4}", rows.getAllValues().get(1).toString());
    assertEquals("{\"taskId\":5,\"parentId\":2}", rows.getAllValues().get(2).toString());
  }
}
