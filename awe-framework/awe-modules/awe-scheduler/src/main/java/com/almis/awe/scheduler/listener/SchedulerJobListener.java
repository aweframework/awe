package com.almis.awe.scheduler.listener;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.context.ApplicationEventPublisher;

import static com.almis.awe.scheduler.constant.ListenerConstants.JOB_LISTENER_NAME;

/**
 * Quartz {@link JobListener} that publishes task lifecycle events and clears the
 * {@code pendingPropagation} overlay on the Quartz launcher thread after each job.
 *
 * <p>The launcher thread runs {@link com.almis.awe.scheduler.job.scheduled.SchedulerJob#execute}
 * and dispatches real work to pool workers via {@code @Async}.  Any overlay written on the
 * launcher thread itself must be cleaned here since pool workers already have their own
 * cleanup in {@link com.almis.awe.component.AweMDCTaskDecorator}'s {@code finally} block.
 * Cleaning when nothing was written is a safe no-op.</p>
 *
 * @author dfuentes
 */
@Slf4j
public class SchedulerJobListener extends ServiceConfig implements JobListener {

  private final ApplicationEventPublisher eventPublisher;
  private final PrototypeRequestBeanHolder prototypeRequestBeanHolder;

  public SchedulerJobListener(ApplicationEventPublisher eventPublisher,
                               PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    this.eventPublisher = eventPublisher;
    this.prototypeRequestBeanHolder = prototypeRequestBeanHolder;
  }

  @Override
  public String getName() {
    return JOB_LISTENER_NAME;
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    // The started event is published by JobService#startTask, once the execution is stored (#815)
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    // no action needed
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {
    try {
      if (context.getJobInstance() instanceof SchedulerJob) {
        SchedulerJob job = (SchedulerJob) context.getJobInstance();
        eventPublisher.publishEvent(new SchedulerTaskFinishedEvent(this,
            job.getTask(), job.getExecution(), job.getExecution().getExecutionTime()));
      }
    } finally {
      // Launcher-thread safety-net: clear any overlay so it cannot leak across jobs
      prototypeRequestBeanHolder.clearPendingPropagation();
      log.trace("[SCHEDULER] Cleared pending propagation overlay on launcher thread '{}'",
          Thread.currentThread().getName());
    }
  }
}
