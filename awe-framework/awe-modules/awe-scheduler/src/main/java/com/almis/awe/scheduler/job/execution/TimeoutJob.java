package com.almis.awe.scheduler.job.execution;

import lombok.extern.slf4j.Slf4j;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.UnableToInterruptJobException;

import java.util.concurrent.Future;

@Slf4j
public class TimeoutJob implements InterruptableJob {

  @Override
  public void execute(JobExecutionContext context) {
    Future process = (Future) context.getJobDetail().getJobDataMap().get("process");
    // Interrupt the task
    process.cancel(true);
    log.info("[TIMEOUT] The timeout thread has interrupted a task: {}", context.getTrigger().getKey());
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    // Do nothing
  }
}
