package com.almis.awe.scheduler.job.report;

import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.naming.NamingException;
import java.util.ArrayList;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing BroadcastReportJob class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class BroadcastReportJobTest {

  @InjectMocks
  private BroadcastReportJob broadcastReportJob;

  @Mock
  private BroadcastService broadcastService;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(broadcastReportJob);
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
    JobExecutionContext executionContext = Mockito.mock(JobExecutionContext.class);
    JobDetail jobDetail = Mockito.mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put(TASK, new Task().setReport(new Report().setReportUserDestination(new ArrayList<>())));
    dataMap.put(TASK_JOB_EXECUTION, new TaskExecution().setStatus(status.getValue()));
    given(executionContext.getJobDetail()).willReturn(jobDetail);
    given(jobDetail.getJobDataMap()).willReturn(dataMap);
    broadcastReportJob.execute(executionContext);
    verify(broadcastService, Mockito.times(1)).broadcastMessageToUsers(any(ClientAction.class), any());
  }
}
