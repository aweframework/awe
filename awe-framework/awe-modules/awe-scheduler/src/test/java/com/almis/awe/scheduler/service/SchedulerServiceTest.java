package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
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
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
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
}
