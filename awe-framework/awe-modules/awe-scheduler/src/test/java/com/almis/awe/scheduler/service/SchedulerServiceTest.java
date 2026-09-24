package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
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
  private ApplicationContext context;
  @Mock
  private AweRequest request;

  @BeforeEach
  void initService() {
    schedulerService = new SchedulerService(taskDAO, schedulerDAO, calendarDAO, false, false);
    schedulerService.setApplicationContext(context);
    doReturn(request).when(context).getBean(AweRequest.class);
  }

  @Test
  void getExecutionLogDelegatesWithParametersReadFromTheRequest() throws Exception {
    given(request.getParameter("execution-log-task-id")).willReturn(IntNode.valueOf(2));
    given(request.getParameter("execution-log-execution-id")).willReturn(IntNode.valueOf(1));
    given(request.getParameter("offset")).willReturn(IntNode.valueOf(5));
    given(request.getParameter("version")).willReturn(TextNode.valueOf("v-echoed"));

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
}
