package com.almis.awe.scheduler.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Unit tests for {@link SchedulerService#getExecutionLog()}: reads request parameters directly. */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

  private SchedulerService schedulerService;

  @Mock
  private TaskDAO taskDAO;
  @Mock
  private SchedulerDAO schedulerDAO;
  @Mock
  private CalendarDAO calendarDAO;
  @Mock
  private SchedulerExecutionCompletionService completionService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweRequest request;
  @Mock
  private AweElements aweElements;

  @BeforeEach
  void initService() {
    schedulerService = new SchedulerService(taskDAO, schedulerDAO, calendarDAO, completionService, false, false);
    schedulerService.setApplicationContext(context);
    lenient().doReturn(request).when(context).getBean(AweRequest.class);
    lenient().doReturn(aweElements).when(context).getBean(AweElements.class);
  }

  @Test
  void getExecutionLogDelegatesWithParametersReadFromTheRequest() throws Exception {
    given(request.getParameter("execution-log-task-id")).willReturn(IntNode.valueOf(2));
    given(request.getParameter("execution-log-execution-id")).willReturn(IntNode.valueOf(1));
    given(request.getParameter("offset")).willReturn(IntNode.valueOf(5));
    given(request.getParameter("version")).willReturn(StringNode.valueOf("v-echoed"));

    schedulerService.getExecutionLog();

    verify(taskDAO).getExecutionLog(2, 1, 5, "v-echoed");
  }

  @Test
  void getExecutionLogDefaultsOffsetToZeroAndVersionToNullWhenAbsent() throws Exception {
    given(request.getParameter("execution-log-task-id")).willReturn(IntNode.valueOf(2));
    given(request.getParameter("execution-log-execution-id")).willReturn(IntNode.valueOf(1));

    schedulerService.getExecutionLog();

    verify(taskDAO).getExecutionLog(2, 1, 0, null);
  }

  @Test
  void getExecutionLogReturnsEmptyServiceDataWithoutCallingTaskDAOWhenTaskIdIsMissing() throws Exception {
    given(request.getParameter("execution-log-task-id")).willReturn((JsonNode) null);
    given(request.getParameter("execution-log-execution-id")).willReturn(IntNode.valueOf(1));

    ServiceData actual = schedulerService.getExecutionLog();

    assertTrue(actual.getVariableMap().isEmpty());
    verifyNoInteractions(taskDAO);
  }

  @Test
  void getExecutionLogReturnsEmptyServiceDataWithoutCallingTaskDAOWhenExecutionIdIsMissing() throws Exception {
    given(request.getParameter("execution-log-task-id")).willReturn(IntNode.valueOf(2));
    given(request.getParameter("execution-log-execution-id")).willReturn((JsonNode) null);

    ServiceData actual = schedulerService.getExecutionLog();

    assertTrue(actual.getVariableMap().isEmpty());
    verifyNoInteractions(taskDAO);
  }

  @Test
  void savingDependenciesThatCloseACycleIsRejected() throws Exception {
    given(taskDAO.getChildrenByTask()).willReturn(Map.of(3, List.of(1)));

    assertThrows(AWException.class, () -> schedulerService.validateTaskDependencies(1, List.of(3)));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void dependenciesSentAsTextByTheClientAreChecked() throws Exception {
    given(taskDAO.getChildrenByTask()).willReturn(Map.of(3, List.of(1)));
    // The AngularJS client sends the suggested task ids as text inside the integer list
    List children = List.of("3", "");

    assertThrows(AWException.class, () -> schedulerService.validateTaskDependencies(1, children));
  }

  @Test
  void aManualLaunchWithAValueThatIsNotOfItsTypeIsRejectedWithoutLaunchingTheTask() throws Exception {
    Map<String, String> values = Map.of("count", "five");
    given(taskDAO.findInvalidParameterValues(4, values)).willReturn(List.of("count"));

    assertThrows(AWException.class, () -> schedulerService.executeTaskNow(4, "test", values));

    verify(taskDAO, never()).executeTaskNow(4, "test", values);
  }

  @Test
  void aManualLaunchThatChangesThePropertyKeyIsRejectedWithoutLaunchingTheTask() throws Exception {
    Map<String, String> values = Map.of("region", "other.key");
    given(taskDAO.findInvalidParameterValues(4, values)).willReturn(List.of());
    given(taskDAO.findChangedPropertyKeys(4, values)).willReturn(List.of("region"));

    assertThrows(AWException.class, () -> schedulerService.executeTaskNow(4, "test", values));
    assertEquals(AnswerType.ERROR, schedulerService.validateTaskParameters(4, values).getType());

    verify(taskDAO, never()).executeTaskNow(4, "test", values);
    verify(taskDAO, never()).findUnresolvedProperties(4, values);
  }

  @Test
  void aManualLaunchWithValidValuesLaunchesTheTaskWithThem() throws Exception {
    Map<String, String> values = Map.of("count", "7");
    given(taskDAO.findInvalidParameterValues(4, values)).willReturn(List.of());

    schedulerService.executeTaskNow(4, "test", values);

    verify(taskDAO).executeTaskNow(4, "test", values);
  }

  @Test
  void validatingAValueThatIsNotOfItsTypeAnswersAnErrorBeforeCheckingTheProperties() throws Exception {
    Map<String, String> values = Map.of("count", "five");
    given(taskDAO.findInvalidParameterValues(4, values)).willReturn(List.of("count"));

    assertEquals(AnswerType.ERROR, schedulerService.validateTaskParameters(4, values).getType());

    verify(taskDAO, never()).findUnresolvedProperties(4, values);
  }

  @Test
  void savingDependenciesWithoutACycleIsAccepted() throws Exception {
    given(taskDAO.getChildrenByTask()).willReturn(Map.of(3, List.of(4)));

    schedulerService.validateTaskDependencies(1, List.of(3));
  }

  @Test
  void stopTaskClosesTheDeferredExecutionsOfTheTask() throws Exception {
    schedulerService.stopTask(4);

    verify(completionService).stopTask(4);
  }

  @Test
  void activityCountersAddTheUpcomingFireTimesOfTheNext24HoursToTheExecutionCounters() throws Exception {
    DataList dataList = new DataList();
    dataList.addRow(new HashMap<>(Map.of("running", new CellData(3L))));
    given(taskDAO.getExecutionCounters()).willReturn(new ServiceData().setDataList(dataList));
    given(schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24))).willReturn(7L);

    Map<String, CellData> row = schedulerService.getActivityCounters().getDataList().getRows().get(0);

    assertEquals(3L, row.get("running").getObjectValue());
    assertEquals(7L, row.get("upcomingNext24h").getObjectValue());
  }

  @Test
  void activityCountersKeepTheExecutionCountersAndLeaveUpcomingEmptyWhenQuartzFails() throws Exception {
    DataList dataList = new DataList();
    dataList.addRow(new HashMap<>(Map.of("running", new CellData(3L), "waiting", new CellData(2L),
      "queued", new CellData(1L), "errorsLast24h", new CellData(4L))));
    given(taskDAO.getExecutionCounters()).willReturn(new ServiceData().setDataList(dataList));
    given(schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24))).willThrow(new AWException("quartz down"));
    Logger logger = (Logger) LoggerFactory.getLogger(SchedulerService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      Map<String, CellData> row = schedulerService.getActivityCounters().getDataList().getRows().get(0);

      assertEquals(List.of(3L, 2L, 1L, 4L), List.of(row.get("running").getObjectValue(),
        row.get("waiting").getObjectValue(), row.get("queued").getObjectValue(),
        row.get("errorsLast24h").getObjectValue()));
      assertTrue(row.get("upcomingNext24h").isEmpty());
      assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.WARN),
        "the failure of the upcoming count is logged as a warning");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void stopTaskStopsTheRunningJobOfTheTask() throws Exception {
    given(schedulerDAO.stopRunningJobs(4)).willReturn(true);

    ServiceData result = schedulerService.stopTask(4);

    verify(schedulerDAO).stopRunningJobs(4);
    assertEquals(AnswerType.OK, result.getType());
  }

  @Test
  void stopTaskWarnsWhenTheRunningJobCannotBeStopped() throws Exception {
    given(schedulerDAO.stopRunningJobs(4)).willReturn(false);

    ServiceData result = schedulerService.stopTask(4);

    assertEquals(AnswerType.WARNING, result.getType());
  }

  @Test
  void choosingAParentThatAlreadyRunsAfterTheTaskIsRejectedWithoutSavingTheParents() throws Exception {
    // 1 -> 3: task 1 cannot run after task 3 nor after itself
    given(taskDAO.getChildrenByTask()).willReturn(Map.of(1, List.of(3)));

    assertThrows(AWException.class, () -> schedulerService.validateTaskParents(1, List.of(3)));
    assertThrows(AWException.class, () -> schedulerService.saveTaskParents(1, List.of(2, 1)));

    verify(taskDAO, never()).saveTaskParents(any(), any());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void theChosenParentsAreSavedInTheirOrderOnceEach() throws Exception {
    given(taskDAO.getChildrenByTask()).willReturn(Map.of(1, List.of(3)));
    // The client may send the ids as numbers or as text
    List parents = Arrays.asList("4", 2, "", null, "4");

    schedulerService.saveTaskParents(1, parents);

    verify(taskDAO).saveTaskParents(1, List.of(4, 2));
  }

  @Test
  void theWeekdayOfADateOfTheCalendarPanelFillsTheDayOfItsRowAndIsEmptyWithoutADate() {
    given(aweElements.getLanguage()).willReturn("es-ES");
    given(aweElements.getLocaleWithLanguage("SCREEN_TEXT_TASK_CATALOG_WEEKDAY_6", "es-ES")).willReturn("vie");

    assertEquals(List.of("update-row GrdDatLst new-row-2 {DatWek=vie}"),
      actions(schedulerService.getConfigurationCalendarDateWeekday("25/12/2026", "new-row-2")));
    assertEquals(List.of("update-row GrdDatLst 7 {DatWek=}"),
      actions(schedulerService.getConfigurationCalendarDateWeekday("", "7")));
    assertEquals(List.of("update-row GrdDatLst 7 {DatWek=}"),
      actions(schedulerService.getConfigurationCalendarDateWeekday("31/02/2026", "7")));
    // No row being edited or selected: nothing to fill
    assertEquals(List.of(), actions(schedulerService.getConfigurationCalendarDateWeekday("25/12/2026", null)));
    assertEquals(List.of(), actions(schedulerService.getConfigurationCalendarDateWeekday("25/12/2026", " ")));
  }

  /**
   * Client actions as «type grid row data»
   */
  private static List<String> actions(ServiceData serviceData) {
    return serviceData.getClientActionList().stream()
      .map(action -> action.getType() + " " + action.getTarget() + " " + action.getParameterMap().get("rowId") + " "
        + action.getParameterMap().get("row"))
      .toList();
  }
}
