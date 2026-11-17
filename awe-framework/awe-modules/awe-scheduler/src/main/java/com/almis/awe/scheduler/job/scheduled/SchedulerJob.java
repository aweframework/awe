package com.almis.awe.scheduler.job.scheduled;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.constant.JobConstants;
import com.almis.awe.scheduler.service.scheduled.JobService;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
@Setter
public abstract class SchedulerJob implements InterruptableJob {

  private Task task;
  private TaskExecution execution;
  // The execution was accepted and stays running, to be completed later
  private boolean deferred;
  // Stopped by the user: its process was cancelled
  private volatile boolean stopped;
  @Getter(AccessLevel.NONE)
  private final AtomicReference<Future<ServiceData>> process = new AtomicReference<>();

  // Autowired services
  private JobService jobService;

  /**
   * Constructor
   *
   * @param jobService Job service
   */
  protected SchedulerJob(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobDataMap dataMap = context.getJobDetail().getJobDataMap();
    setTask((Task) dataMap.get(JobConstants.TASK));

    try {
      setExecution(jobService.startTask(task));
      launch(dataMap);
    } catch (CancellationException exc) {
      log.warn("[SCHEDULER] Task was cancelled: {}", context.getTrigger().getKey().toString(), exc);
    } catch (InterruptedException exc) {
      log.error("[SCHEDULER] Task execution was interrupted: {}", context.getTrigger().getKey().toString(), exc);
      Thread.currentThread().interrupt();
      throw new JobExecutionException(exc.getMessage(), exc);
    } catch (Exception exc) {
      log.error("[SCHEDULER] Error on task execution: {}", context.getTrigger().getKey().toString(), exc);
      throw new JobExecutionException(exc.getMessage(), exc);
    }
  }

  /**
   * Launch the task and wait for its end
   *
   * @param dataMap Job data map
   * @throws AWException          Error launching the task
   * @throws InterruptedException Job interrupted
   */
  protected void launch(JobDataMap dataMap) throws AWException, InterruptedException {
    Future<ServiceData> result = jobService.executeJob(getTask(), getExecution(), dataMap);
    process.set(result);
    deferred = SchedulerDeferred.isDeferred(jobService.launchBatch(this, result));
  }

  /**
   * Stop the job on the user's request: cancel its process, interrupting the thread that runs it
   * (command executors kill their process when interrupted)
   *
   * @return false when the job has not launched its process yet and cannot be stopped
   */
  public boolean stop() {
    stopped = true;
    Future<ServiceData> running = process.get();
    return running != null && (running.cancel(true) || running.isDone());
  }

  @Override
  public void interrupt() {
    // Do nothing
  }
}
