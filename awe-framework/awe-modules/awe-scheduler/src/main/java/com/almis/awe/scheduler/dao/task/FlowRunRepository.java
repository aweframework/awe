package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.MaintainType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.task.FlowRules.ExecutionRow;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_LAUNCHER;

/**
 * Flow run persistence: the run, its parameters, the join state of its tasks and the dependency graph. The guarded
 * updates return whether this caller changed the row, which is what makes a join launch a child only once.
 */
@Slf4j
public class FlowRunRepository {

  private static final String RUN_ID = "runId";

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
  public FlowRunRepository(QueryService queryService, MaintainService maintainService, QueryUtil queryUtil) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
  }

  /**
   * Create the run of a root execution, with the parameters of its launch
   *
   * @param runId     Run identifier
   * @param task      Root task (its run parameters are stored)
   * @param execution Root execution
   * @throws AWException Error storing the run
   */
  public void createRun(String runId, Task task, TaskExecution execution) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(RUN_ID, runId);
    parameters.put(TASK_ID, task.getTaskId());
    parameters.put(TASK_JOB_EXECUTION, execution.getExecutionId());
    parameters.put(TASK_LAUNCHER, task.getLauncher());
    maintainService.launchPrivateMaintain("insertFlowRun", parameters);

    if (task.getRunParameters() != null) {
      for (Map.Entry<String, String> entry : task.getRunParameters().entrySet()) {
        ObjectNode parameter = queryUtil.getParameters();
        parameter.put(RUN_ID, runId);
        parameter.put("name", entry.getKey());
        parameter.put("value", entry.getValue());
        maintainService.launchPrivateMaintain("insertFlowRunParameter", parameter);
      }
    }
  }

  /**
   * Root task of the run
   *
   * @param runId Run identifier
   * @return Root task identifier, null when the run does not exist
   * @throws AWException Error reading the run
   */
  public Integer getRootTaskId(String runId) throws AWException {
    DataList dataList = query("flowRun", runId);
    return dataList.getRows().isEmpty() ? null : DataListUtil.getCellData(dataList, 0, "rootTaskId").getIntegerValue();
  }

  /**
   * Parameters of the run
   *
   * @param runId Run identifier
   * @return Values by parameter name
   * @throws AWException Error reading the parameters
   */
  public Map<String, String> getParameters(String runId) throws AWException {
    DataList dataList = query("flowRunParameters", runId);
    Map<String, String> values = new LinkedHashMap<>();
    for (int row = 0; row < dataList.getRows().size(); row++) {
      values.put(DataListUtil.getCellData(dataList, row, "name").getStringValue(),
        DataListUtil.getCellData(dataList, row, "value").getStringValue());
    }
    return values;
  }

  /**
   * Join state of the tasks of the run
   *
   * @param runId Run identifier
   * @return Join rows
   * @throws AWException Error reading the join state
   */
  public List<JoinRow> getJoins(String runId) throws AWException {
    return DataListUtil.asBeanList(query("flowRunTasks", runId), JoinRow.class);
  }

  /**
   * Executions of the run
   *
   * @param runId Run identifier
   * @return Executions
   * @throws AWException Error reading the executions
   */
  public List<ExecutionRow> getExecutions(String runId) throws AWException {
    return DataListUtil.asBeanList(query("flowRunExecutions", runId), ExecutionRow.class);
  }

  /**
   * Every task dependency (child and parent)
   *
   * @return Dependencies
   * @throws AWException Error reading the dependencies
   */
  public List<TaskDependency> getDependencies() throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    return DataListUtil.asBeanList(queryService.launchPrivateQuery("allTaskDependencies", parameters).getDataList(),
      TaskDependency.class);
  }

  /**
   * Register a task of the run as waiting for its parents, when it is not registered yet
   *
   * @param runId  Run identifier
   * @param taskId Task identifier
   * @param joins  Join rows already read
   * @throws AWException Error storing the join row
   */
  public void register(String runId, Integer taskId, List<JoinRow> joins) throws AWException {
    if (joins.stream().anyMatch(join -> join.taskId().equals(taskId))) {
      return;
    }
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(RUN_ID, runId);
    parameters.put(TASK_ID, taskId);
    try {
      maintainService.launchPrivateMaintain("insertFlowRunTask", parameters);
    } catch (AWException exc) {
      // Another parent registered it first: the primary key rejected the duplicate
      log.debug("[SCHEDULER][FLOW] Task {} already registered in run {}", taskId, runId, exc);
    }
  }

  /**
   * Change the join state of a task only when it holds the expected one
   *
   * @param runId  Run identifier
   * @param taskId Task identifier
   * @param from   Expected state
   * @param to     New state
   * @return True when this call changed the row
   * @throws AWException Error updating the join state
   */
  public boolean transition(String runId, Integer taskId, String from, String to) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(RUN_ID, runId);
    parameters.put(TASK_ID, taskId);
    parameters.put("fromStatus", from);
    parameters.put("toStatus", to);
    return rowsAffected(maintainService.launchPrivateMaintain("transitionFlowRunTask", parameters)) == 1;
  }

  /**
   * Store the execution of a launched task of the run
   *
   * @param runId       Run identifier
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @throws AWException Error updating the join row
   */
  public void markStarted(String runId, Integer taskId, Integer executionId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(RUN_ID, runId);
    parameters.put(TASK_ID, taskId);
    parameters.put(TASK_JOB_EXECUTION, executionId);
    maintainService.launchPrivateMaintain("startFlowRunTask", parameters);
  }

  /**
   * End the run, only the first call changes it
   *
   * @param runId  Run identifier
   * @param status Final status
   * @return True when this call ended the run
   * @throws AWException Error ending the run
   */
  public boolean endRun(String runId, TaskStatus status) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(RUN_ID, runId);
    parameters.put("status", status.getValue());
    return rowsAffected(maintainService.launchPrivateMaintain("endFlowRun", parameters)) == 1;
  }

  private DataList query(String query, String runId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(RUN_ID, runId);
    return queryService.launchPrivateQuery(query, parameters).getDataList();
  }

  private long rowsAffected(ServiceData result) {
    return result.getResultDetails().stream()
      .filter(details -> details.getOperationType() != MaintainType.AUDIT)
      .map(MaintainResultDetails::getRowsAffected)
      .mapToLong(Long::longValue)
      .sum();
  }
}
