package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_GROUP;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_LAUNCHER;
import static com.almis.awe.scheduler.constant.MaintainConstants.GET_ALL_EXECUTIONS;
import static com.almis.awe.scheduler.constant.MaintainConstants.GET_ALL_EXECUTIONS_WITH_DATES;
import static com.almis.awe.scheduler.constant.MaintainConstants.PURGE_EXECUTION_LOGS;
import static com.almis.awe.scheduler.constant.MaintainConstants.TASK_END;
import static com.almis.awe.scheduler.constant.MaintainConstants.TASK_START;
import static com.almis.awe.scheduler.constant.MaintainConstants.TASK_UPDATE_STATUS;
import static com.almis.awe.scheduler.constant.MaintainConstants.UPDATE_INTERRUPTED_TASKS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_AVERAGE_TIME_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_LAST_TASK_EXECUTION;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_EXECUTION;

/**
 * Task execution persistence: start, end and status of executions, execution time, average time and purge of
 * old executions and their logs.
 */
@Slf4j
public class TaskExecutionRepository {

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final ExecutionLogStore executionLogStore;
  private final int defaultStoredExecutions;

  /**
   * Constructor
   *
   * @param queryService            Query service
   * @param maintainService         Maintain service
   * @param queryUtil               Query utilities
   * @param executionLogStore       Execution log store
   * @param defaultStoredExecutions Executions kept when the task does not define its own value
   */
  public TaskExecutionRepository(QueryService queryService, MaintainService maintainService, QueryUtil queryUtil,
                                 ExecutionLogStore executionLogStore, int defaultStoredExecutions) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.executionLogStore = executionLogStore;
    this.defaultStoredExecutions = defaultStoredExecutions;
  }

  /**
   * Start a task
   *
   * @param task Task
   * @return Task execution
   * @throws AWException Error starting the task
   */
  public TaskExecution startTask(Task task) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, task.getTaskId());
    parameters.put(TASK_GROUP, task.getGroup());
    parameters.put(TASK_LAUNCHER, task.getLauncher());
    parameters.put("executions", task.getStoredExecutions() != null
      ? task.getStoredExecutions()
      : defaultStoredExecutions);
    maintainService.launchPrivateMaintain(TASK_START, parameters);

    // Retrieve task execution
    TaskExecution execution = getLastExecution(task, task.getTrigger().getKey().getGroup());

    // Set parent execution if defined
    if (execution != null) {
      execution.setParentExecution(task.getParentExecution());
    }

    return execution;
  }

  /**
   * End a task
   *
   * @param task      Task
   * @param execution Task execution
   * @return Task execution
   * @throws AWException Error ending the task
   */
  public TaskExecution endTask(Task task, TaskExecution execution) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, task.getTaskId());
    parameters.put(TASK_JOB_EXECUTION, execution.getExecutionId());
    parameters.put("status", task.getStatus().getValue());
    parameters.put("taskDescription", execution.getDescription());
    maintainService.launchPrivateMaintain(TASK_END, parameters);

    // Retrieve updated task execution from database
    TaskExecution updated = getTaskExecution(task.getTaskId(), execution.getExecutionId());
    updated.setParentExecution(task.getParentExecution());
    return updated;
  }

  /**
   * Retrieve executions to purge (sorted)
   *
   * @param taskId     Task identifier
   * @param executions Executions to keep
   * @return Purge results
   * @throws AWException Error retrieving executions to be purged
   */
  public ServiceData getExecutionsToPurge(Integer taskId, Integer executions) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    ServiceData serviceData = queryService.launchPrivateQuery(GET_ALL_EXECUTIONS_WITH_DATES, parameters);
    DataList dataList = serviceData.getDataList();

    // Filter rows
    dataList.setRows(serviceData.getDataList().getRows().stream().skip(executions).toList());
    dataList.getRows().forEach(row -> row.remove("id"));

    // Set records
    dataList.setRecords(serviceData.getDataList().getRows().size());

    return serviceData;
  }

  /**
   * Purge execution log files
   *
   * @param taskId     Task identifier
   * @param executions Executions to keep
   * @return Purge results
   * @throws AWException Error on purge
   */
  public ServiceData purgeExecutionLogFiles(Integer taskId, Integer executions) throws AWException {
    ArrayNode executionsToPurge = JsonNodeFactory.instance.arrayNode();
    DataList dataList = getExecutionsToPurge(taskId, executions).getDataList();
    List<TaskExecution> taskExecutionList = DataListUtil.asBeanList(dataList, TaskExecution.class);
    List<Integer> executionIds = taskExecutionList.stream().map(TaskExecution::getExecutionId).toList();
    executionIds.forEach(executionsToPurge::add);

    executionLogStore.purge(taskId, executionIds);

    // Add executions id to parameters
    ObjectNode parameters = queryUtil.getParameters();
    parameters.set("executionId", executionsToPurge);
    parameters.put(TASK_ID, taskId);
    if (!executionsToPurge.isEmpty()) {
      return maintainService.launchPrivateMaintain(PURGE_EXECUTION_LOGS, parameters);
    } else {
      return new ServiceData();
    }
  }

  /**
   * Purge execution logs which do not belong to any stored execution
   *
   * @return Purge results
   * @throws AWException Error on purge
   */
  public ServiceData purgeExecutionsAtStart() throws AWException {
    log.info("===== Deleting old execution logs =====");

    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    DataList dataList = queryService.launchPrivateQuery(GET_ALL_EXECUTIONS, parameters).getDataList();
    List<TaskExecution> taskExecutionList = DataListUtil.asBeanList(dataList, TaskExecution.class);
    Set<ExecutionKey> validExecutions = taskExecutionList
      .stream()
      .map(e -> new ExecutionKey(e.getTaskId(), e.getExecutionId()))
      .collect(Collectors.toSet());

    executionLogStore.purgeOrphans(validExecutions);

    return new ServiceData();
  }

  /**
   * Update running task status [4] to interrupted [6]
   *
   * @return Interrupt task status
   * @throws AWException Error updating the tasks
   */
  public ServiceData updateInterruptedTasks() throws AWException {
    return maintainService.launchPrivateMaintain(UPDATE_INTERRUPTED_TASKS);
  }

  /**
   * Change an execution status
   *
   * @param execution Task execution
   * @param status    New status
   * @param reason    Status description
   * @return Change status
   * @throws AWException Error storing the status
   */
  public ServiceData changeStatus(TaskExecution execution, TaskStatus status, String reason) throws AWException {
    // Set execution status
    execution.setStatus(status.getValue());
    execution.setDescription(reason);

    // Update execution status
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, execution.getTaskId());
    parameters.put(TASK_JOB_EXECUTION, execution.getExecutionId());
    parameters.put("status", status.getValue());
    parameters.put("description", reason);
    return maintainService.launchPrivateMaintain(TASK_UPDATE_STATUS, parameters);
  }

  /**
   * Update execution time with the difference between the stored end and initial dates
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return Execution time
   * @throws AWException Error updating execution time
   */
  public ServiceData updateExecutionTime(Integer taskId, Integer executionId) throws AWException {
    // Get task execution
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    ServiceData serviceData = queryService.launchPrivateQuery(SCHEDULER_TASK_EXECUTION, parameters);

    // Get dates
    DataList dataList = serviceData.getDataList();
    Instant initialDate = asInstant(DataListUtil.getCellData(dataList, 0, "initialDate"));
    Instant endDate = asInstant(DataListUtil.getCellData(dataList, 0, "endDate"));

    // Update time in execution
    parameters.put("executionTime", Duration.between(initialDate, endDate).toMillis());
    return maintainService.launchPrivateMaintain("updateExecutionTime", parameters);
  }

  /**
   * Get execution from Database
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return Task execution, or null when not found
   * @throws AWException Error retrieving task execution from database
   */
  public TaskExecution getTaskExecution(Integer taskId, Integer executionId) throws AWException {
    // Set context from the query
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);

    return getTaskExecution(SCHEDULER_TASK_EXECUTION, parameters);
  }

  /**
   * Get the average execution time
   *
   * @param taskId Task identifier
   * @return Average time in milliseconds, 0 when not stored
   * @throws AWException Error retrieving average time
   */
  public Integer getAverageTime(Integer taskId) throws AWException {
    // Get task average time datalist
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    ServiceData serviceData = queryService.launchPrivateQuery(SCHEDULER_AVERAGE_TIME_QUERY, parameters);
    CellData cellData = DataListUtil.getCellData(serviceData.getDataList(), 0, "averageTime");
    return cellData == null || cellData.isEmpty() ? 0 : cellData.getIntegerValue();
  }

  /**
   * Read a date cell as instant
   *
   * @param cell Date cell
   * @return Instant, or null when the cell holds no date
   */
  private Instant asInstant(CellData cell) {
    return LegacyDateConverter.asInstant(cell.getDateValue());
  }

  /**
   * Get last execution from Database
   *
   * @param task      Task
   * @param taskGroup Task group
   * @return Task execution
   * @throws AWException Error retrieving last task execution from database
   */
  private TaskExecution getLastExecution(Task task, String taskGroup) throws AWException {
    // Set context from the query
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, task.getTaskId());
    parameters.put(TASK_GROUP, taskGroup);

    return getTaskExecution(SCHEDULER_LAST_TASK_EXECUTION, parameters);
  }

  /**
   * Launch a query and retrieve the first task execution from it
   *
   * @param query      Query
   * @param parameters Parameters
   * @return Task execution
   * @throws AWException Error retrieving task execution from database
   */
  private TaskExecution getTaskExecution(String query, ObjectNode parameters) throws AWException {
    ServiceData serviceData = queryService.launchPrivateQuery(query, parameters);
    List<TaskExecution> executionList = DataListUtil.asBeanList(serviceData.getDataList(), TaskExecution.class);
    return executionList.isEmpty() ? null : executionList.get(0);
  }
}
