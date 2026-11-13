package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_PROGRESS;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link TaskExecutionPresenter}: java.time progress and elapsed time against a fixed clock and the
 * date texts of the execution screen.
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionPresenterTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private ExecutionLogStore executionLogStore;

  @Mock
  private TaskExecutionRepository executionRepository;

  @Mock
  private ServiceConfig serviceConfig;

  private TaskExecutionPresenter presenter;

  @BeforeEach
  void initPresenter() {
    presenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore, executionRepository,
      serviceConfig, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void progressIsTheElapsedPercentageOfTheAverage() {
    assertEquals(50L, presenter.getProgress(NOW.minusSeconds(30), 60_000L));
    assertEquals(150L, presenter.getProgress(NOW.minusSeconds(90), 60_000L));
    assertEquals(-50L, presenter.getProgress(NOW.plusSeconds(30), 60_000L));
  }

  @Test
  void progressWithoutAverageIsOverflow() {
    assertEquals(101L, presenter.getProgress(NOW.minusSeconds(30), 0L));
  }

  @Test
  void elapsedTimeIsMillisecondsSinceStart() {
    assertEquals(5_250, presenter.getElapsedTime(NOW.minusMillis(5_250)));
  }

  @Test
  void progressAndElapsedTimeRejectNullStarts() {
    assertThrows(NullPointerException.class, () -> presenter.getProgress(null, 10L));
    assertThrows(NullPointerException.class, () -> presenter.getElapsedTime(null));
  }

  @Test
  void runningExecutionUsesTheClockForProgressAndElapsedTime() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_EXECUTION_ID, new CellData(10));
    row.put(TASK_STATUS, new CellData(TaskStatus.JOB_RUNNING.getValue()));
    row.put(TASK_LAUNCH_TYPE_LIST, new CellData(MANUAL_GROUP));
    row.put(TASK_LAUNCHED_BY_LIST, new CellData("admin"));
    row.put("ExeLstTim", new CellData(Date.from(NOW.minusSeconds(120))));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(eq("getTaskExecutionList"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
    given(executionRepository.getAverageTime(5)).willReturn(240_000);
    given(executionLogStore.locatorNode(new ExecutionKey(5, 10))).willReturn(JsonNodeFactory.instance.objectNode());

    Map<String, CellData> result = presenter.getTaskExecutionList(5).getDataList().getRows().get(0);

    assertEquals("{\"value\":50,\"label\":\"50%\"}", result.get(TASK_EXECUTION_PROGRESS).getObjectValue().toString());
    assertEquals(TimeUtil.formatTime(120_000, false), result.get(TASK_EXECUTION_TIME).getStringValue());
  }

  @Test
  void executionScreenShowsStartAndEndTimesWithMilliseconds() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7, 89_000_000)
      .atZone(ZoneId.systemDefault()).toInstant());
    Timestamp end = Timestamp.valueOf(LocalDateTime.of(2026, 3, 4, 6, 0, 0));
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, end));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.reloadExecutionScreen(1, 2);

    assertEquals("04/03/2026 05:06:07.089", selectedValue(serviceData, "execution-start-time"));
    assertEquals("04/03/2026 06:00:00.000", selectedValue(serviceData, "execution-end-time"));
  }

  @Test
  void executionScreenShowsAnEmptyEndTimeWhileRunning() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, null));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.reloadExecutionScreen(1, 2);

    assertEquals("04/03/2026 05:06:07.000", selectedValue(serviceData, "execution-start-time"));
    assertEquals("", selectedValue(serviceData, "execution-end-time"));
  }

  private TaskExecution execution(Date start, Date end) {
    return new TaskExecution()
      .setTaskId(1)
      .setExecutionId(2)
      .setName("Task")
      .setStatus(TaskStatus.JOB_OK.getValue())
      .setGroupId(MANUAL_GROUP)
      .setInitialDate(start)
      .setEndDate(end);
  }

  private Object selectedValue(ServiceData serviceData, String target) {
    ClientAction action = serviceData.getClientActionList().stream()
      .filter(clientAction -> target.equals(clientAction.getTarget()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No action for " + target));
    return ((Object[]) action.getParameterMap().get("values"))[0];
  }
}
