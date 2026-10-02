package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the schedule that the service reads from the request of the screen
 */
@ExtendWith(MockitoExtension.class)
class RemoteSchedulerServiceScheduleTest {

  @Mock
  private SchedulerService schedulerService;

  @Mock
  private RemoteScheduler remoteScheduler;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweRequest request;

  private final ObjectMapper mapper = new ObjectMapper();
  private RemoteSchedulerService service;
  private ObjectNode parameters;

  @BeforeEach
  void setUp() {
    parameters = JsonNodeFactory.instance.objectNode();
    when(request.getParameter(anyString())).thenAnswer(call -> parameters.get(call.<String>getArgument(0)));
    lenient().when(request.getParameterAsString(anyString())).thenAnswer(call -> {
      JsonNode parameter = parameters.get(call.<String>getArgument(0));
      return parameter == null || parameter.isNull() ? null : parameter.asText();
    });
    when(context.getBean(AweRequest.class)).thenReturn(request);
    service = new RemoteSchedulerService(schedulerService, remoteScheduler, mapper, false);
    service.setApplicationContext(context);
  }

  private Schedule computeSchedule() throws Exception {
    service.computeNextFireTimes(10);
    ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
    verify(schedulerService).computeNextFireTimes(eq(10), captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldComputeTheFireTimesWhenTheClientSendsEmptyCriteriaAsNull() throws Exception {
    parameters.put("RptTyp", "0").put("RptNum", 1200).putNull("IdeCal");
    parameters.putArray("years");
    parameters.putNull("months");

    Schedule schedule = computeSchedule();

    assertThat(schedule.getRepeatNumber()).isEqualTo(1200);
    assertThat(schedule.getCalendarId()).isNull();
    assertThat(schedule.getYearList()).isEmpty();
    assertThat(schedule.getMonthList()).isNull();
  }

  @Test
  void shouldComputeTheFireTimesWhenTheClientDoesNotSendEmptyCriteriaAtAll() throws Exception {
    parameters.put("RptTyp", "0").put("RptNum", 1200);

    Schedule schedule = computeSchedule();

    assertThat(schedule.getRepeatType()).isZero();
    assertThat(schedule.getRepeatNumber()).isEqualTo(1200);
    assertThat(schedule.getCalendarId()).isNull();
    assertThat(schedule.getInitialDate()).isNull();
    assertThat((List<String>) schedule.getYearList()).isNull();
    assertThat(schedule.getSecondList()).isNull();
  }
}
