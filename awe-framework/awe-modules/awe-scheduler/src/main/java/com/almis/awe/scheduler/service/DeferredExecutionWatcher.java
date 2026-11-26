package com.almis.awe.scheduler.service;

import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.scheduler.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interrupts the deferred executions that are past their due date. It runs on every node: the atomic close lets only
 * one of them finish each execution, and, being based on the database, it keeps working across restarts.
 */
@Slf4j
public class DeferredExecutionWatcher {

  private final TaskDAO taskDAO;
  private final SchedulerExecutionCompletionService completionService;
  private final Duration interval;
  private ScheduledExecutorService executor;

  /**
   * Constructor
   *
   * @param taskDAO           Task DAO
   * @param completionService Service that closes the deferred executions
   * @param interval          Time between two checks
   */
  public DeferredExecutionWatcher(TaskDAO taskDAO, SchedulerExecutionCompletionService completionService,
                                  Duration interval) {
    this.taskDAO = taskDAO;
    this.completionService = completionService;
    this.interval = interval;
  }

  /**
   * Start the periodic check
   */
  public void start() {
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "awe-scheduler-deferred-execution-watcher");
      thread.setDaemon(true);
      return thread;
    });
    executor.scheduleWithFixedDelay(this::sweep, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Stop the periodic check
   */
  public void stop() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  /**
   * Interrupt the executions past their due date. It never throws, so a failure cannot cancel the periodic run
   */
  public void sweep() {
    try {
      for (DeferredExecution overdue : taskDAO.getRunningDeferredExecutions(null, Instant.now())) {
        interrupt(overdue);
      }
    } catch (Exception exc) {
      log.error("[SCHEDULER] Error looking for deferred executions past their due date", exc);
    }
  }

  private void interrupt(DeferredExecution overdue) {
    try {
      completionService.complete(overdue.getTaskId(), overdue.getExecutionId(), TaskStatus.JOB_INTERRUPTED, null,
        null, CompletionSource.DEADLINE);
    } catch (Exception exc) {
      log.error("[SCHEDULER] Error interrupting execution {}-{}", overdue.getTaskId(), overdue.getExecutionId(), exc);
    }
  }
}
