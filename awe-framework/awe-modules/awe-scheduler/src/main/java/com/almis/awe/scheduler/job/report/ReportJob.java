package com.almis.awe.scheduler.job.report;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.service.report.ReportJobService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;

@Slf4j
@Getter
@Setter
public class ReportJob extends ServiceConfig implements InterruptableJob {

  private final ReportJobService reportJobService;

  /**
   * Autowired constructor
   *
   * @param reportJobService Report job service
   */
  public ReportJob(ReportJobService reportJobService) {
    this.reportJobService = reportJobService;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    reportJobService.execute((Task) context.getJobDetail().getJobDataMap().get(TASK), (TaskExecution) context.getJobDetail().getJobDataMap().get(TASK_JOB_EXECUTION));
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    // Do nothing
  }
}
