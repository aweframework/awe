package com.almis.awe.scheduler.job.report;

import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.service.report.SchedulerBroadcastReportService;
import com.almis.awe.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.NamingException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing BroadcastReportJob class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class SchedulerBroadcastReportServiceTest {

  @InjectMocks
  private SchedulerBroadcastReportService broadcastReportService;

  @Mock
  private BroadcastService broadcastService;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(broadcastReportService);
  }

  /**
   * Broadcast job test
   *
   * @throws Exception Test error
   */
  @ParameterizedTest
  @EnumSource(TaskStatus.class)
  void executeBroadcastJobTest(TaskStatus status) throws Exception {
    executeBroadcastJob(status);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  public void executeBroadcastJob(TaskStatus status) throws Exception {
    Task task = new Task().setReport(new Report().setReportUserDestination(new ArrayList<>()));
    TaskExecution execution = new TaskExecution().setStatus(status.getValue());

    broadcastReportService.execute(task, execution);
    verify(broadcastService, Mockito.times(1)).broadcastMessageToUsers(any(ClientAction.class), any());
  }
}
