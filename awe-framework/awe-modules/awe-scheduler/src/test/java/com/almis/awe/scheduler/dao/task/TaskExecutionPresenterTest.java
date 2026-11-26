package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogPage;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_LOG_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_PROGRESS;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

  @Mock
  private DeferredExecutionRepository deferredRepository;

  private TaskExecutionPresenter presenter;

  @BeforeEach
  void initPresenter() {
    presenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore, executionRepository,
      deferredRepository, serviceConfig, Clock.fixed(NOW, ZoneOffset.UTC));
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
  void finishedDependencyExecutionShowsItsTimeItsParentAndItsLogLocator() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(serviceConfig.getLocale("PARAMETER_TASK")).willReturn("Task");
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_EXECUTION_ID, new CellData(11));
    row.put(TASK_STATUS, new CellData(TaskStatus.JOB_OK.getValue()));
    row.put(TASK_LAUNCH_TYPE_LIST, new CellData(DEPENDENCY_GROUP));
    row.put(TASK_LAUNCHED_BY_LIST, new CellData("Parent"));
    row.put(TASK_EXECUTION_TIME, new CellData(65_000));
    row.put("ExeLstTim", new CellData(Date.from(NOW.minusSeconds(600))));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(eq("getTaskExecutionList"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(dataList));
    given(executionRepository.getAverageTime(5)).willReturn(240_000);
    ObjectNode locator = JsonNodeFactory.instance.objectNode().put("value", "5-11");
    given(executionLogStore.locatorNode(new ExecutionKey(5, 11))).willReturn(locator);

    Map<String, CellData> result = presenter.getTaskExecutionList(5).getDataList().getRows().get(0);

    assertTrue(result.get(TASK_EXECUTION_PROGRESS).isEmpty());
    assertEquals(TimeUtil.formatTime(65_000), result.get(TASK_EXECUTION_TIME).getStringValue());
    assertEquals("Task Parent", result.get(TASK_LAUNCHED_BY_LIST).getStringValue());
    assertEquals(locator, result.get(TASK_EXECUTION_LOG_PATH).getObjectValue());
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
  void executionScreenShowsADashForTheValuesARunningExecutionDoesNotHaveYet() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, null));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.reloadExecutionScreen(1, 2);

    assertEquals("04/03/2026 05:06:07.000", selectedValue(serviceData, "execution-start-time"));
    // No end, no time and no description yet: the dash of the empty values of the side panels
    assertEquals("\u2014", selectedValue(serviceData, "execution-end-time"));
    assertEquals("\u2014", selectedValue(serviceData, "execution-time"));
    assertEquals("\u2014", selectedValue(serviceData, "execution-information"));
  }

  @Test
  void theProgressOfARunningExecutionIsItsElapsedTimeAgainstTheAverageOfItsTask() throws Exception {
    given(executionRepository.getTaskExecution(1, 2))
      .willReturn(execution(Date.from(NOW.minusSeconds(90)), null).setStatus(TaskStatus.JOB_RUNNING.getValue()));
    given(executionRepository.getAverageTime(1)).willReturn(100_000);
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_AVERAGE", TimeUtil.formatTime(100_000, false)))
      .willReturn("Average 1m 40s");

    Map<String, CellData> card = progressCard(1, 2);

    assertEquals("SCREEN_TEXT_ACTIVITY_ELAPSED", card.get("title").getStringValue());
    assertEquals(90.0, card.get("value").getDoubleValue());
    assertEquals("s", card.get("unit").getStringValue());
    assertEquals(100.0, card.get("target").getDoubleValue());
    assertEquals(0, card.get("numberDecimals").getIntegerValue());
    assertEquals("Average 1m 40s", card.get("description").getStringValue());
    assertEquals("info", card.get("iconTone").getStringValue());
  }

  @Test
  void theProgressOfAnEndedExecutionIsItsExecutionTimeAgainstTheAverageInMinutesWhenItIsLong() throws Exception {
    given(executionRepository.getTaskExecution(1, 2))
      .willReturn(execution(Date.from(NOW.minusSeconds(600)), Date.from(NOW.minusSeconds(300))).setExecutionTime(300_000));
    given(executionRepository.getAverageTime(1)).willReturn(240_000);
    given(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_AVERAGE", TimeUtil.formatTime(240_000, false)))
      .willReturn("Average 4m");

    Map<String, CellData> card = progressCard(1, 2);

    assertEquals("SCREEN_TEXT_EXECUTION_TIME", card.get("title").getStringValue());
    assertEquals(5.0, card.get("value").getDoubleValue());
    assertEquals("min", card.get("unit").getStringValue());
    assertEquals(4.0, card.get("target").getDoubleValue());
    assertEquals(1, card.get("numberDecimals").getIntegerValue());
    assertEquals("Average 4m", card.get("description").getStringValue());
  }

  @Test
  void withoutAverageTheProgressHasNoTargetAndSaysSo() throws Exception {
    given(executionRepository.getTaskExecution(1, 2))
      .willReturn(execution(Date.from(NOW.minusSeconds(30)), null).setStatus(TaskStatus.JOB_RUNNING.getValue()));
    given(executionRepository.getAverageTime(1)).willReturn(0);

    Map<String, CellData> card = progressCard(1, 2);

    assertEquals(30.0, card.get("value").getDoubleValue());
    assertTrue(card.get("target") == null || card.get("target").isEmpty(), "no target, no bar");
    assertEquals("SCREEN_TEXT_ACTIVITY_NO_AVERAGE", card.get("description").getStringValue());
  }

  @Test
  void theProgressOfNoExecutionIsAnEmptyCard() throws Exception {
    assertTrue(presenter.getExecutionProgress(null, null).getDataList().getRows().isEmpty());
    assertTrue(presenter.getExecutionProgress(1, 3).getDataList().getRows().isEmpty());
  }

  @Test
  void theLaunchParametersAreRowsOfNameAndValueInTheirOrderWithTheDashForAnEmptyValue() {
    Map<String, String> launch = new LinkedHashMap<>();
    launch.put("date", "2026-11-18");
    launch.put("empty", "");
    launch.put("none", null);

    List<Map<String, CellData>> rows = presenter.getExecutionParameters(launch).getDataList().getRows();

    assertEquals(List.of("date", "empty", "none"), rows.stream().map(row -> row.get("ParNam").getStringValue()).toList());
    assertEquals(List.of("2026-11-18", "—", "—"),
      rows.stream().map(row -> row.get("ParVal").getStringValue()).toList());
    assertTrue(presenter.getExecutionParameters(Map.of()).getDataList().getRows().isEmpty());
  }

  private Map<String, CellData> progressCard(Integer taskId, Integer executionId) throws Exception {
    List<Map<String, CellData>> rows = presenter.getExecutionProgress(taskId, executionId).getDataList().getRows();
    assertEquals(1, rows.size());
    return rows.get(0);
  }

  @Test
  void theExecutionScreenWithoutButtonAddressReadsTheExecutionOfItsLocator() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, null));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.loadExecutionScreen("1-2", null);

    verify(executionLogStore).applyViewerSelection("1-2", new ExecutionKey(1, 2), serviceData);
    assertEquals(2, selectedValue(serviceData, "execution-id"));
  }

  @Test
  void theExecutionScreenWithoutButtonValueNorAddressReadsTheExecutionOfTheSelectedRow() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, null));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.loadExecutionScreen(null, null, "1-2");

    verify(executionLogStore).applyViewerSelection("1-2", new ExecutionKey(1, 2), serviceData);
    assertEquals(2, selectedValue(serviceData, "execution-id"));
  }

  @Test
  void theExecutionScreenWithoutAnExecutionShowsNothingInsteadOfFailing() throws Exception {
    ServiceData serviceData = presenter.loadExecutionScreen(null, null, null);

    assertTrue(serviceData.getClientActionList().isEmpty());
    verify(executionLogStore, never()).applyViewerSelection(any(), any(), any());
    verify(executionRepository, never()).getTaskExecution(any(), any());
  }

  @Test
  void aDeferredExecutionShowsItsReferenceDeadlineAndTrace() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    Date due = Date.from(LocalDateTime.of(2026, 3, 5, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, start));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");
    given(deferredRepository.find(1, 2)).willReturn(new DeferredExecution().setTaskId(1).setExecutionId(2)
      .setExternalReference("job-77").setDueDate(due).setCompletionDate(start).setCompletionSource("USER")
      .setCancellationStatus("FAILED").setCancellationError("unknown job"));
    given(serviceConfig.getLocale(anyString())).willAnswer(invocation -> invocation.getArgument(0));
    given(serviceConfig.getLocale(anyString(), any(String[].class))).willAnswer(invocation ->
      invocation.getArgument(0) + String.join(",", (String[]) invocation.getRawArguments()[1]));

    ServiceData serviceData = presenter.reloadExecutionScreen(1, 2);

    assertEquals("SCHEDULER_DEFERRED_TRACEjob-77,05/03/2026 05:06:07.000"
        + " · SCHEDULER_DEFERRED_CLOSEDSCHEDULER_DEFERRED_SOURCE_USER,04/03/2026 05:06:07.000"
        + " · SCHEDULER_DEFERRED_NOTICE_FAILEDunknown job",
      selectedValue(serviceData, "execution-deferred"));
  }

  @Test
  void anExecutionThatWasNeverDeferredShowsNoTrace() throws Exception {
    Date start = Date.from(LocalDateTime.of(2026, 3, 4, 5, 6, 7).atZone(ZoneId.systemDefault()).toInstant());
    given(executionRepository.getTaskExecution(1, 2)).willReturn(execution(start, start));
    given(queryService.findLabel(anyString(), any())).willReturn("Label");

    ServiceData serviceData = presenter.reloadExecutionScreen(1, 2);

    assertEquals("", selectedValue(serviceData, "execution-deferred"));
  }

  @Test
  void theLogOfAnEndedExecutionTurnsTheAutorefreshOfTheViewerOffAndItIsReadAfterTheStatus() throws Exception {
    given(executionRepository.getTaskExecution(1, 2)).willReturn(new TaskExecution().setStatus(TaskStatus.JOB_OK.getValue()));
    given(executionLogStore.read(new ExecutionKey(1, 2), 3, "v1"))
      .willReturn(new ExecutionLogPage(List.of("last"), false, 4L, 0L, "v2"));

    ServiceData serviceData = presenter.getExecutionLog(1, 2, 3, "v1");

    assertEquals(List.of("last"), serviceData.getVariable("LOG_CONTENT").getValue());
    assertEquals(1, serviceData.getClientActionList().size());
    ClientAction stop = serviceData.getClientActionList().get(0);
    assertEquals("update-controller", stop.getType());
    // No target: the action runs on the viewer that asked for the log
    assertNull(stop.getTarget());
    assertEquals("autorefresh", stop.getParameterMap().get("attribute"));
    assertEquals(0, stop.getParameterMap().get("value"));
    assertEquals(Boolean.TRUE, stop.getSilent());
    assertEquals(Boolean.TRUE, stop.getAsync());
    // The status is read before the lines: an execution that had ended has all its lines in this read
    InOrder order = inOrder(executionRepository, executionLogStore);
    order.verify(executionRepository).getTaskExecution(1, 2);
    order.verify(executionLogStore).read(new ExecutionKey(1, 2), 3, "v1");
  }

  @Test
  void theLogOfARunningOrQueuedOrUnknownExecutionKeepsTheViewerFollowingIt() throws Exception {
    given(executionLogStore.read(new ExecutionKey(1, 2), 0, null)).willReturn(ExecutionLogPage.empty());
    for (TaskStatus status : List.of(TaskStatus.JOB_RUNNING, TaskStatus.JOB_QUEUED)) {
      given(executionRepository.getTaskExecution(1, 2)).willReturn(new TaskExecution().setStatus(status.getValue()));
      assertEquals(0, presenter.getExecutionLog(1, 2, 0, null).getClientActionList().size(), status.name());
    }
    given(executionRepository.getTaskExecution(1, 2)).willReturn(null);
    assertEquals(0, presenter.getExecutionLog(1, 2, 0, null).getClientActionList().size());
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
