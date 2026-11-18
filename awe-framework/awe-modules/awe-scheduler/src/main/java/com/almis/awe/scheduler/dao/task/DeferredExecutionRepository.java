package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;
import static com.almis.awe.scheduler.constant.MaintainConstants.INSERT_DEFERRED_EXECUTION;
import static com.almis.awe.scheduler.constant.MaintainConstants.UPDATE_DEFERRED_CANCELLATION;
import static com.almis.awe.scheduler.constant.MaintainConstants.UPDATE_DEFERRED_COMPLETION;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_DEFERRED_EXECUTION;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_RUNNING_DEFERRED;

/**
 * Persistence of the state of the deferred executions (table AweSchExeExt)
 */
public class DeferredExecutionRepository {

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;

  /**
   * Constructor
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param queryUtil       Query utilities
   */
  public DeferredExecutionRepository(QueryService queryService, MaintainService maintainService,
                                     QueryUtil queryUtil) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
  }

  /**
   * Register an execution that was accepted and finishes later
   *
   * @param taskId            Task id
   * @param executionId       Execution id
   * @param externalReference Opaque reference of the work in the external system (null when there is none)
   * @param acceptedDate      Date the execution was accepted
   * @param dueDate           Date from which the execution is overdue
   * @throws AWException Error storing the state
   */
  public void insert(Integer taskId, Integer executionId, String externalReference, Instant acceptedDate,
                     Instant dueDate) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    parameters.put("externalReference", externalReference);
    parameters.put("acceptedDate", asTimestamp(acceptedDate));
    parameters.put("dueDate", asTimestamp(dueDate));
    maintainService.launchPrivateMaintain(INSERT_DEFERRED_EXECUTION, parameters);
  }

  /**
   * Store who closed a deferred execution and when
   *
   * @param taskId         Task id
   * @param executionId    Execution id
   * @param source         Who closed the execution
   * @param completionDate Date the execution was closed
   * @throws AWException Error storing the trace
   */
  public void complete(Integer taskId, Integer executionId, CompletionSource source, Instant completionDate)
    throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    parameters.put("completionSource", source.name());
    parameters.put("completionDate", asTimestamp(completionDate));
    maintainService.launchPrivateMaintain(UPDATE_DEFERRED_COMPLETION, parameters);
  }

  /**
   * Store the outcome of the cancellation notice of a deferred execution
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @param status      Outcome of the notice
   * @param error       Error of a failed notice, or null
   * @throws AWException Error storing the outcome
   */
  public void cancellationNotice(Integer taskId, Integer executionId, String status, String error)
    throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    parameters.put("cancellationStatus", status);
    parameters.put("cancellationError", error);
    maintainService.launchPrivateMaintain(UPDATE_DEFERRED_CANCELLATION, parameters);
  }

  /**
   * Find the deferred state of an execution, whether it is still running or not
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return State, or null when the execution was never deferred
   * @throws AWException Error retrieving the state
   */
  public DeferredExecution find(Integer taskId, Integer executionId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    List<DeferredExecution> found = query(parameters, SCHEDULER_DEFERRED_EXECUTION);
    return found.isEmpty() ? null : found.get(0);
  }

  /**
   * List the deferred executions that are still running
   *
   * @param taskId    Task id to filter by, or null for every task
   * @param dueBefore Only the executions whose due date is before this date, or null for all
   * @return Running deferred executions
   * @throws AWException Error retrieving the executions
   */
  public List<DeferredExecution> findRunning(Integer taskId, Instant dueBefore) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    if (taskId != null) {
      parameters.put(TASK_ID, taskId);
    }
    if (dueBefore != null) {
      parameters.put("limitDate", asTimestamp(dueBefore));
    }
    return query(parameters, SCHEDULER_RUNNING_DEFERRED);
  }

  private List<DeferredExecution> query(ObjectNode parameters, String queryId) throws AWException {
    DataList dataList = queryService.launchPrivateQuery(queryId, parameters).getDataList();
    return DataListUtil.asBeanList(dataList, DeferredExecution.class);
  }

  /**
   * Format an instant as the timestamp text of the query and maintain variables
   *
   * @param instant Instant
   * @return Timestamp text
   */
  private String asTimestamp(Instant instant) {
    return DateUtil.TIMESTAMP_FORMAT_WEB_MS.format(instant.atZone(ZoneId.systemDefault()));
  }
}
