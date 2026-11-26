package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.model.util.data.date.DateTimeFormatting;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.util.TaskUtil;
import com.almis.awe.service.QueryService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_RUNNING_DEFERRED;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_AVERAGE_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_NEXT_EXECUTION;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_NAME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_SOURCE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_TYPE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_PARAMETER_VALUE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_STATUS;

/**
 * Data of the task management screens: the task list enriched with the scheduler state and the variables a
 * maintain offers as task parameters.
 */
public class TaskListPresenter {

  private final Scheduler scheduler;
  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final TaskScheduling taskScheduling;
  private final ServiceConfig serviceConfig;

  /**
   * Constructor
   *
   * @param scheduler      Scheduler
   * @param queryService   Query service
   * @param queryUtil      Query utilities
   * @param taskScheduling Task scheduling
   * @param serviceConfig  Service configuration giving access to the application elements
   */
  public TaskListPresenter(Scheduler scheduler, QueryService queryService, QueryUtil queryUtil,
                           TaskScheduling taskScheduling, ServiceConfig serviceConfig) {
    this.scheduler = scheduler;
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.taskScheduling = taskScheduling;
    this.serviceConfig = serviceConfig;
  }

  /**
   * Retrieve the task list
   *
   * @param taskListCriteria Task list filter
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList(TaskListCriteria taskListCriteria) throws AWException {
    // Get task list details
    ObjectNode parameters = queryUtil.getParameters(queryUtil.getParameters(taskListCriteria), null, "1", "0");
    ServiceData serviceData = queryService.launchPrivateQuery(SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY,
      parameters);
    Set<Integer> withDeferredExecution = getTasksWithDeferredExecution();
    DataList filtered = new DataList();
    for (Map<String, CellData> row : serviceData.getDataList().getRows()) {
      // Add other parameters from the scheduler to the datalist
      filtered.addRow(addSchedulerParameters(row, withDeferredExecution));
    }

    return serviceData.setDataList(filtered);
  }

  /**
   * Load needed variables from the selected maintain
   *
   * @param maintainStr Maintain identifier
   * @return ServiceData
   * @throws AWException Error loading maintain variables
   */
  public ServiceData loadMaintainVariables(String maintainStr) throws AWException {
    ServiceData serviceData = new ServiceData();
    // Get the selected maintain
    Target maintain = serviceConfig.getElements().getMaintain(maintainStr);
    // Get the maintain query list
    List<MaintainQuery> queryList = maintain.getQueryList();
    DataList dataList = new DataList();
    // Create a variables hashmap without duplicate variable names
    Map<String, Variable> variables = new HashMap<>();

    // Fill hashmap
    for (MaintainQuery query : queryList) {
      List<Variable> variableList = new ArrayList<>();
      List<Variable> tmpLst = query.getVariableDefinitionList();
      if (tmpLst != null) {
        variableList.addAll(tmpLst);
      }
      for (Variable variable : variableList) {
        // check if the variable already exist in the hasmap
        if (variable.getName() != null && !variables.containsKey(variable.getName())) {
          variables.put(variable.getName(), variable);
        }
      }
    }
    // Get hashmap keys
    Set<String> keys = variables.keySet();
    for (String key : keys) {
      // fill datalist rows with each variable data
      Map<String, CellData> row = new HashMap<>();
      Variable variable = variables.get(key);
      row.put(TASK_PARAMETER_NAME, new CellData(variable.getName()));
      row.put(TASK_PARAMETER_SOURCE, new CellData("1"));
      row.put(TASK_PARAMETER_TYPE, new CellData(variable.getType()));
      row.put(TASK_PARAMETER_VALUE, new CellData(""));
      dataList.getRows().add(row);
    }
    return serviceData.setDataList(dataList);
  }

  /**
   * Get the tasks with a deferred execution still running: they run without a scheduler thread, so Quartz does
   * not know them. One query for the whole list
   *
   * @return Task identifiers
   * @throws AWException Error retrieving the executions
   */
  private Set<Integer> getTasksWithDeferredExecution() throws AWException {
    ServiceData waiting = queryService.launchPrivateQuery(SCHEDULER_RUNNING_DEFERRED,
      queryUtil.getParameters(null, "1", "0"));
    Set<Integer> taskIds = new HashSet<>();
    for (Map<String, CellData> row : waiting.getDataList().getRows()) {
      taskIds.add(row.get("taskId").getIntegerValue());
    }
    return taskIds;
  }

  /**
   * Next fire time of a task in the scheduler: the next fire time of its trigger while its state is normal (not paused,
   * blocked, completed nor in error)
   *
   * @param taskId     Task identifier
   * @param launchType Launch type of the task
   * @return Next fire time, empty when the task has no trigger or its trigger does not fire
   * @throws AWException Error reading the scheduler
   */
  public Optional<Instant> getNextFireTime(Integer taskId, Integer launchType) throws AWException {
    TriggerKey triggerKey = new TriggerKey(taskId.toString(), TaskUtil.getGroupForLaunchType(launchType));
    try {
      return taskScheduling.triggerExists(triggerKey)
        ? getNextFireTime(scheduler.getTrigger(triggerKey), triggerKey)
        : Optional.empty();
    } catch (SchedulerException exc) {
      throw new AWException("Error reading the next fire time of the task", triggerKey.toString(), exc);
    }
  }

  /**
   * Next fire time of a trigger while its state is normal
   */
  private Optional<Instant> getNextFireTime(Trigger trigger, TriggerKey triggerKey) throws SchedulerException {
    if (trigger == null || scheduler.getTriggerState(triggerKey) != Trigger.TriggerState.NORMAL) {
      return Optional.empty();
    }
    return Optional.ofNullable(LegacyDateConverter.asInstant(trigger.getNextFireTime()));
  }

  /**
   * Add parameters from the scheduler to the task list
   *
   * @param row                   Parameters row
   * @param withDeferredExecution Tasks with a deferred execution still running
   * @return Parameters row
   * @throws AWException Error adding scheduler parameters
   */
  private Map<String, CellData> addSchedulerParameters(Map<String, CellData> row, Set<Integer> withDeferredExecution)
    throws AWException {
    // Get trigger identifier
    Integer ide = row.get(TASK_IDE_LIST).getIntegerValue();
    String group = TaskUtil.getGroupForLaunchType(Integer.valueOf(row.get(TASK_LAUNCH_TYPE_LIST).getStringValue()));

    try {
      // Check if the trigger exists
      TriggerKey triggerKey = new TriggerKey(ide.toString(), group);
      if (taskScheduling.triggerExists(triggerKey)) {

        Trigger trigger = scheduler.getTrigger(triggerKey);

        // Next execution time, when the trigger state is normal
        Instant nextTime = getNextFireTime(trigger, triggerKey).orElse(null);

        // get the job associated to the trigger
        JobDetail job = scheduler.getJobDetail(trigger.getJobKey());
        row.put(TASK_STATUS, new CellData(withDeferredExecution.contains(ide) || taskScheduling.isJobRunning(job)
          ? TaskStatus.JOB_RUNNING.getValue()
          : TaskStatus.JOB_OK.getValue()));

        // fill row data from scheduler parameters
        row.put(TASK_NEXT_EXECUTION, new CellData(nextTime != null
          ? DateTimeFormatting.formatWebDateTime(LocalDateTime.ofInstant(nextTime, ZoneId.systemDefault()))
          : " - "));
      } else {
        // Initialize job status to OK if it's not currently running on the
        // scheduler
        row.put(TASK_STATUS, new CellData(withDeferredExecution.contains(ide)
          ? TaskStatus.JOB_RUNNING.getValue()
          : TaskStatus.JOB_OK.getValue()));

        // If the trigger does not exist, try to load last execution from the
        // database
        row.put(TASK_NEXT_EXECUTION, new CellData(" - "));
      }

      // Get the maximum number of stored executions
      row.put(TASK_AVERAGE_TIME, new CellData(TimeUtil.formatTime(row.get("averageTime").getIntegerValue())));
    } catch (Exception exc) {
      throw new AWException("Error adding scheduler parameters to the task datalist: {}",
        new TriggerKey(ide.toString(), group).toString(), exc);
    }

    return row;
  }
}
