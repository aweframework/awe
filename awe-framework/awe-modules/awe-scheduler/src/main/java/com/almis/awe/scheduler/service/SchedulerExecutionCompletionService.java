package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.CompletionResult.Outcome;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Closes the deferred executions: the ones that answered accepted, finishes later. It relies only on the database, so
 * it works on any node of a cluster and after a restart. Whoever updates the running execution first closes it; any
 * other call (a repeated one, the due date racing a completion) does nothing.
 */
@Slf4j
public class SchedulerExecutionCompletionService extends ServiceConfig {

  private static final String SCHEDULER_ERROR_MESSAGE_TIMEOUT = "SCHEDULER_ERROR_MESSAGE_TIMEOUT";
  private static final String STOPPED_BY_USER = "SCHEDULER_MESSAGE_STOPPED_BY_USER";
  private static final Set<TaskStatus> CLOSING_STATUSES = EnumSet.of(TaskStatus.JOB_OK, TaskStatus.JOB_WARNING,
    TaskStatus.JOB_ERROR, TaskStatus.JOB_STOPPED, TaskStatus.JOB_INTERRUPTED, TaskStatus.JOB_INFO);

  private final TaskDAO taskDAO;
  private final ExecutionService executionService;
  private final ExecutionLogStore executionLogStore;
  private final ApplicationEventPublisher eventPublisher;
  private final DeferredCancellationNotifier cancellationNotifier;
  private final Duration deferredDefaultTimeout;

  /**
   * Autowired constructor
   *
   * @param taskDAO                Task DAO
   * @param executionService       Execution service
   * @param executionLogStore      Active execution log store
   * @param eventPublisher         Event publisher
   * @param cancellationNotifier   Notice to the external system of a stopped or expired execution
   * @param deferredDefaultTimeout Timeout of a deferred execution whose task has none
   */
  public SchedulerExecutionCompletionService(TaskDAO taskDAO, ExecutionService executionService,
                                             ExecutionLogStore executionLogStore,
                                             ApplicationEventPublisher eventPublisher,
                                             DeferredCancellationNotifier cancellationNotifier,
                                             Duration deferredDefaultTimeout) {
    this.taskDAO = taskDAO;
    this.executionService = executionService;
    this.executionLogStore = executionLogStore;
    this.eventPublisher = eventPublisher;
    this.cancellationNotifier = cancellationNotifier;
    this.deferredDefaultTimeout = deferredDefaultTimeout;
  }

  /**
   * Complete a deferred execution. Only the call that closes it completes its end
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @param status      Final status: OK, warning, error, stopped, interrupted or info
   * @param description Description of the execution (stored truncated to the column length); an interruption
   *                    without description gets the timeout text
   * @param outputs     Output values of the execution: the dependencies launched when it closes receive them as
   *                    parameters, and each one applies its VARIABLE parameters of the same name
   * @return Result of the completion. A running execution that is not registered as deferred yet answers
   * {@code NOT_DEFERRED} with the current status {@code JOB_RUNNING}; a client that expects the execution to be
   * deferred should retry shortly
   * @throws AWException Error closing the execution
   */
  public CompletionResult complete(Integer taskId, Integer executionId, TaskStatus status, String description,
                                   Map<String, String> outputs) throws AWException {
    return complete(taskId, executionId, status, description, outputs, CompletionSource.API);
  }

  /**
   * Complete a deferred execution on behalf of a source. Only the call that closes it completes its end
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @param status      Final status: OK, warning, error, stopped, interrupted or info
   * @param description Description of the execution (see {@link #complete(Integer, Integer, TaskStatus, String, Map)})
   * @param outputs     Output values of the execution
   * @param source      Who closes the execution, stored in its trace
   * @return Result of the completion
   * @throws AWException Error closing the execution
   */
  public CompletionResult complete(Integer taskId, Integer executionId, TaskStatus status, String description,
                                   Map<String, String> outputs, CompletionSource source) throws AWException {
    if (!CLOSING_STATUSES.contains(status)) {
      throw new IllegalArgumentException("Status not allowed: " + status);
    }

    TaskExecution execution = taskDAO.getTaskExecution(taskId, executionId);
    if (execution == null) {
      return new CompletionResult(Outcome.UNKNOWN, null);
    }
    DeferredExecution deferred = taskDAO.getDeferredExecution(taskId, executionId);
    if (deferred == null) {
      return new CompletionResult(Outcome.NOT_DEFERRED, TaskStatus.valueOf(execution.getStatus()));
    }

    // Everything that can fail and is needed goes before the gate: a failure here leaves the execution running
    Task task = loadTask(taskId);
    task.setStatus(status);
    addOutputs(task, outputs);
    execution.setDescription(describe(task, status, description));

    // Atomic gate: only the call that closes the running execution completes its end
    if (!taskDAO.closeRunningExecution(taskId, executionId, status)) {
      TaskExecution current = taskDAO.getTaskExecution(taskId, executionId);
      return current == null ? new CompletionResult(Outcome.UNKNOWN, null)
        : new CompletionResult(Outcome.NOT_RUNNING, TaskStatus.valueOf(current.getStatus()));
    }

    // From here the execution is closed and nobody can retry: a failing step must not skip the following ones
    TaskExecution closed = execution;
    try {
      closed = taskDAO.endTask(task, execution);
    } catch (AWException | RuntimeException exc) {
      log.error("[SCHEDULER] Error ending deferred execution {}", execution.getKey(), exc);
    }
    TaskExecution ended = closed;

    runGuarded(ended, () -> recordCompletion(taskId, executionId, source));
    // The external system did not close it: tell it the work is cancelled (a failed notice changes nothing here)
    if (source != CompletionSource.API) {
      runGuarded(ended, () -> cancellationNotifier.notifyCancellation(task, deferred));
    }
    runGuarded(ended, () -> executionService.startReportJob(task, ended));
    runGuarded(ended, () -> executionLogStore.complete(new ExecutionKey(ended.getTaskId(), ended.getExecutionId()),
      ExecutionLogOrigin.SCHEDULER));

    // The listener refreshes the screens and launches the dependencies with the usual rules for the status
    runGuarded(ended, () -> eventPublisher.publishEvent(
      new SchedulerTaskFinishedEvent(this, task, ended, ended.getExecutionTime())));
    return new CompletionResult(Outcome.CLOSED, status);
  }

  /**
   * Stop the deferred executions of a task that are still running: they have no thread to interrupt
   *
   * @param taskId Task identifier
   * @return Number of executions this call closed as stopped
   * @throws AWException Error closing an execution
   */
  public int stopTask(Integer taskId) throws AWException {
    int stopped = 0;
    for (DeferredExecution running : taskDAO.getRunningDeferredExecutions(taskId, null)) {
      CompletionResult result = complete(running.getTaskId(), running.getExecutionId(), TaskStatus.JOB_STOPPED,
        getLocale(STOPPED_BY_USER), null, CompletionSource.USER);
      if (result.outcome() == Outcome.CLOSED) {
        stopped++;
      }
    }
    return stopped;
  }

  private void runGuarded(TaskExecution execution, Runnable step) {
    try {
      step.run();
    } catch (RuntimeException exc) {
      log.error("[SCHEDULER] Error finishing deferred execution {}", execution.getKey(), exc);
    }
  }

  /**
   * Make the outputs parameters of the task, so the dependencies receive them like the other values of the parent.
   * An output replaces a parameter of the same name, as a plain value
   */
  private void addOutputs(Task task, Map<String, String> outputs) {
    if (outputs == null || outputs.isEmpty()) {
      return;
    }
    List<TaskParameter> parameters = task.getParameterList() == null ? new ArrayList<>()
      : new ArrayList<>(task.getParameterList());
    outputs.forEach((name, value) -> {
      if (name != null && !name.isBlank() && value != null) {
        parameters.removeIf(parameter -> name.equals(parameter.getName()));
        parameters.add(new TaskParameter().setName(name).setSource(String.valueOf(ParameterConstants.VALUE))
          .setValue(value));
      }
    });
    task.setParameterList(parameters);
  }

  private void recordCompletion(Integer taskId, Integer executionId, CompletionSource source) {
    try {
      taskDAO.recordDeferredCompletion(taskId, executionId, source, Instant.now());
    } catch (AWException exc) {
      throw new IllegalStateException("Error storing the completion trace", exc);
    }
  }

  private Task loadTask(Integer taskId) throws AWException {
    try {
      return taskDAO.getTask(taskId).get();
    } catch (ExecutionException exc) {
      throw new AWException("Error loading task " + taskId, exc);
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new AWException("Interrupted loading task " + taskId, exc);
    }
  }

  private String describe(Task task, TaskStatus status, String description) {
    String text = description;
    if (text == null && TaskStatus.JOB_INTERRUPTED.equals(status)) {
      long timeout = task.getExecutionTimeout() != null ? task.getExecutionTimeout()
        : deferredDefaultTimeout.getSeconds();
      text = getLocale(SCHEDULER_ERROR_MESSAGE_TIMEOUT, TimeUtil.formatTime((int) timeout * 1000, false));
    }
    return text != null && text.length() > SchedulerDeferred.MAX_DESCRIPTION_LENGTH
      ? text.substring(0, SchedulerDeferred.MAX_DESCRIPTION_LENGTH) : text;
  }
}
