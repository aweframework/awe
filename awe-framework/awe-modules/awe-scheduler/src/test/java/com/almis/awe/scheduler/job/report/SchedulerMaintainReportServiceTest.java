package com.almis.awe.scheduler.job.report;

import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.service.report.SchedulerMaintainReportService;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing the SchedulerMaintainReportService (mirrors the email report twin).
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class SchedulerMaintainReportServiceTest {

  @InjectMocks
  private SchedulerMaintainReportService schedulerMaintainReportService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private MaintainService maintainService;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(schedulerMaintainReportService);
  }

  /**
   * Execute maintain report job: verify the alias-free {@code getParameters()} call
   * (no per-task datasource routing - see #685) and that the maintain is launched with
   * the configured report maintain id.
   */
  @Test
  void executeMaintainReport() throws Exception {
    // Mock the alias-free parameters
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());

    Task task = new Task()
      .setTaskId(1)
      .setReport(new Report().setReportMaintainId("reportMaintain"));
    TaskExecution execution = new TaskExecution()
      .setExecutionId(1)
      .setGroupId("TEST_GROUP")
      .setStatus(TaskStatus.JOB_OK.getValue());

    // Run method
    schedulerMaintainReportService.execute(task, execution);

    // Assert the maintain is launched with the report maintain id
    verify(maintainService, times(1)).launchPrivateMaintain(eq("reportMaintain"), any(ObjectNode.class));
  }
}
