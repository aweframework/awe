package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.quartz.Scheduler;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskListPresenterTest {

  @Mock
  private Scheduler scheduler;
  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskScheduling taskScheduling;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskListPresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    presenter = new TaskListPresenter(scheduler, queryService, queryUtil, taskScheduling, serviceConfig);
    given(queryUtil.getParameters(any(), any(), any(), any()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters(null, "1", "0")).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq("javaSchedulerTaskListWfilter"), any(ObjectNode.class)))
      .willReturn(rows(List.of(task(1), task(2))));
    given(queryService.launchPrivateQuery(eq("getRunningDeferredExecutions"), any(ObjectNode.class)))
      .willReturn(rows(List.of(waiting(2))));
  }

  @Test
  void aTaskInAnExternalServiceCountsAsRunning() throws Exception {
    ServiceData serviceData = presenter.getTaskList(new TaskListCriteria());

    assertThat(status(serviceData, 0)).isEqualTo(TaskStatus.JOB_OK.getValue());
    assertThat(status(serviceData, 1)).isEqualTo(TaskStatus.JOB_RUNNING.getValue());
  }

  private int status(ServiceData serviceData, int row) {
    return serviceData.getDataList().getRows().get(row).get("Sta").getIntegerValue();
  }

  private Map<String, CellData> task(int id) {
    Map<String, CellData> row = new HashMap<>();
    row.put("Ide", new CellData(id));
    row.put("TypLch", new CellData("0"));
    row.put("averageTime", new CellData(0));
    return row;
  }

  private Map<String, CellData> waiting(int taskId) {
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(taskId));
    return row;
  }

  private ServiceData rows(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }
}
