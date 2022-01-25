package com.almis.awe.scheduler.job.report;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
import org.quartz.TriggerBuilder;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingException;
import java.util.ArrayList;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;
import static com.almis.awe.scheduler.constant.ReportConstants.REPORT_MAINTAIN_TARGET;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing queries through ActionController
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class EmailReportJobTest {

  @InjectMocks
  private EmailReportJob emailReportJob;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private MaintainService maintainService;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    emailReportJob.setApplicationContext(context);
  }

  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(emailReportJob);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @param status Task status
   * @throws NamingException Test error
   */
  @ParameterizedTest
  @EnumSource(TaskStatus.class)
  void executeTask(TaskStatus status) throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters((String) eq(null))).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.findLabel(anyString(), anyString())).willReturn("LABEL");
    executeEmailJob(status);
  }

  /**
   * Execute email report job
   *
   * @throws Exception see {@link Exception}
   */
  private void executeEmailJob(TaskStatus status) throws Exception {
    JobExecutionContext executionContext = Mockito.mock(JobExecutionContext.class);
    JobDetail jobDetail = Mockito.mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    Task task = new Task()
            .setParameterList(new ArrayList<>())
            .setReport(new Report().setReportUserDestination(new ArrayList<>()))
            .setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    dataMap.put(TASK, task);
    dataMap.put(TASK_JOB_EXECUTION, new TaskExecution()
            .setExecutionId(1)
            .setGroupId("TEST_GROUP")
            .setStatus(status.getValue()));
    given(executionContext.getJobDetail()).willReturn(jobDetail);
    given(jobDetail.getJobDataMap()).willReturn(dataMap);
    emailReportJob.execute(executionContext);
    verify(maintainService, Mockito.times(1)).launchPrivateMaintain(eq(REPORT_MAINTAIN_TARGET), any(ObjectNode.class));
  }
}
