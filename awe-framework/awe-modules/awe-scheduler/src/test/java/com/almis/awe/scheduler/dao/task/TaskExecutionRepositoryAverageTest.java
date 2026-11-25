package com.almis.awe.scheduler.dao.task;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests of the average time of the tasks of a page, read with one query
 * ({@link TaskExecutionRepository#getAverageTimes(java.util.Set)}).
 */
class TaskExecutionRepositoryAverageTest {

  private final QueryService queryService = mock(QueryService.class);
  private final QueryUtil queryUtil = mock(QueryUtil.class);
  private final TaskExecutionRepository repository = new TaskExecutionRepository(queryService,
    mock(MaintainService.class), queryUtil, mock(ExecutionLogStore.class), 5, mock(FlowRunRepository.class));

  @Test
  void theAverageOfTheGivenTasksIsReadWithOneQueryRestrictedToThemByTaskId() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    given(queryUtil.getParameters(null, "1", "0")).willReturn(parameters);
    DataList dataList = new DataList();
    dataList.addRow(average(3, 1500));
    dataList.addRow(average(8, 250));
    given(queryService.launchPrivateQuery(eq("getAverageTimesOfTasks"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));

    assertEquals(Map.of(3, 1500L, 8, 250L), repository.getAverageTimes(new TreeSet<>(Set.of(8, 3))));
    assertEquals("[3,8]", parameters.get("taskIds").toString());
  }

  @Test
  void theSortOfTheGridRequestIsNotTheSortOfTheAverageQuery() throws Exception {
    // getParameters copies the request: the sort of the activity grid names columns this query does not have
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.putArray("sort").addObject().put("id", "TskNam").put("direction", "asc");
    given(queryUtil.getParameters(null, "1", "0")).willReturn(parameters);
    given(queryService.launchPrivateQuery(eq("getAverageTimesOfTasks"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));

    repository.getAverageTimes(Set.of(3));

    assertEquals("[]", parameters.get("sort").toString());
  }

  @Test
  void noTaskReadsNoAverage() throws Exception {
    assertEquals(Map.of(), repository.getAverageTimes(Set.of()));
    verifyNoInteractions(queryService);
  }

  private Map<String, CellData> average(int taskId, int averageTime) {
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(taskId));
    row.put("averageTime", new CellData(averageTime));
    return row;
  }
}
