package com.almis.awe.scheduler.service;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.scheduled.MaintainJobService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

import static com.almis.awe.scheduler.constant.JobConstants.EXECUTION_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;

/**
 * Tells the external system that a deferred execution was cancelled (stopped by a user or past its deadline), with
 * the cancellation maintain of its task. The notice is sent once, by the call that closed the execution, and its
 * outcome is stored in the trace of the execution. It never throws: a failed notice does not reopen or block the
 * execution.
 */
@Slf4j
public class DeferredCancellationNotifier {

  public static final String SENT = "SENT";
  public static final String FAILED = "FAILED";
  private static final int MAX_ERROR_LENGTH = 2000;

  private final TaskDAO taskDAO;
  private final MaintainJobService maintainJobService;

  /**
   * Constructor
   *
   * @param taskDAO            Task DAO
   * @param maintainJobService Service that launches the maintains of the tasks
   */
  public DeferredCancellationNotifier(TaskDAO taskDAO, MaintainJobService maintainJobService) {
    this.taskDAO = taskDAO;
    this.maintainJobService = maintainJobService;
  }

  /**
   * Call the cancellation maintain of the task, when it has one, and store the outcome
   *
   * @param task     Task of the execution
   * @param deferred Deferred execution that was cancelled
   */
  public void notifyCancellation(Task task, DeferredExecution deferred) {
    String maintain = task.getCancellationMaintain();
    if (maintain == null || maintain.isBlank()) {
      return;
    }
    String error = send(maintain.trim(), deferred);
    try {
      taskDAO.recordCancellationNotice(deferred.getTaskId(), deferred.getExecutionId(), error == null ? SENT : FAILED,
        error);
    } catch (Exception exc) {
      log.error("[SCHEDULER] Error storing the cancellation notice of execution {}-{}", deferred.getTaskId(),
        deferred.getExecutionId(), exc);
    }
  }

  /**
   * Launch the cancellation maintain
   *
   * @return Error of a failed notice, or null when it was sent
   */
  private String send(String maintain, DeferredExecution deferred) {
    ObjectNode parameters = maintainJobService.newParameters();
    parameters.put(TASK_ID, deferred.getTaskId());
    parameters.put(EXECUTION_ID, deferred.getExecutionId());
    parameters.put("externalReference", deferred.getExternalReference());
    try {
      ServiceData answer = maintainJobService.launchMaintain(maintain, parameters,
        new TaskExecution().setTaskId(deferred.getTaskId()).setExecutionId(deferred.getExecutionId()));
      if (answer != null && AnswerType.ERROR.equals(answer.getType())) {
        return truncate(answer.getMessage());
      }
      return null;
    } catch (Exception exc) {
      // Only the message: the parameters and the credentials of the call are never stored
      log.warn("[SCHEDULER] Cancellation notice of execution {}-{} failed: {}", deferred.getTaskId(),
        deferred.getExecutionId(), exc.getClass().getSimpleName());
      return truncate(exc.getMessage() == null ? exc.getClass().getSimpleName() : exc.getMessage());
    }
  }

  private static String truncate(String text) {
    String value = text == null || text.isBlank() ? "Error" : text;
    return value.length() > MAX_ERROR_LENGTH ? value.substring(0, MAX_ERROR_LENGTH) : value;
  }
}
