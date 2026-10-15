package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.enums.TriggerType;
import com.almis.awe.scheduler.factory.TriggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;

import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;

/**
 * Immediate task launches: manual launches with operator supplied values, dependency launches propagating the
 * parent parameter values, and the end-of-task handling that updates the parent status and launches dependencies.
 */
@Slf4j
public class TaskImmediateLauncher {

  // locales
  private static final String WARNING_MESSAGE_TASK_DEPENDENCY_ERROR = "WARNING_MESSAGE_TASK_DEPENDENCY_ERROR";

  private final TaskRepository taskRepository;
  private final TaskExecutionRepository executionRepository;
  private final TaskParameterOverrides parameterOverrides;
  private final ServiceConfig serviceConfig;

  /**
   * Constructor
   *
   * @param taskRepository      Task repository
   * @param executionRepository Task execution repository
   * @param parameterOverrides  Task parameter override rules
   * @param serviceConfig       Service configuration giving access to locales
   */
  public TaskImmediateLauncher(TaskRepository taskRepository, TaskExecutionRepository executionRepository,
                               TaskParameterOverrides parameterOverrides, ServiceConfig serviceConfig) {
    this.taskRepository = taskRepository;
    this.executionRepository = executionRepository;
    this.parameterOverrides = parameterOverrides;
    this.serviceConfig = serviceConfig;
  }

  /**
   * Launch a task manually, applying the operator supplied values for its parameters
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @param submitter Scheduling entry point
   * @throws AWException Error executing immediate task
   */
  public void launchNow(Integer taskId, String user, Map<String, String> variables, TaskSubmitter submitter)
    throws AWException {
    // Creates a task that is executed at the moment it is added to the scheduler
    launch(taskId, TriggerType.MANUAL, user, null, variables, submitter);

    // Log launched task
    log.info("Task launched manually: {}", taskId);
  }

  /**
   * Launch the task as dependency, propagating the given parent parameter values to the child.
   * The child defines the contract: only the child's VARIABLE (source="1") parameters whose name
   * is present in the map are overridden.
   *
   * @param taskId          Task identifier
   * @param parentExecution Parent execution
   * @param variables       Parent parameter values (name -&gt; value) to propagate; empty for none
   * @param submitter       Scheduling entry point
   * @throws AWException Error executing dependency task
   */
  public void launchDependency(Integer taskId, TaskExecution parentExecution, Map<String, String> variables,
                               TaskSubmitter submitter) throws AWException {
    // Creates a task that is executed at the moment it is added to the scheduler
    launch(taskId, TriggerType.DEPENDENCY, "#" + parentExecution.getTaskId(), parentExecution, variables, submitter);

    // Log launched task
    log.info("Task #{} launched as dependency from task #{}", taskId, parentExecution.getTaskId());
  }

  /**
   * Property keys of the task that do not resolve against the configuration of this instance,
   * taking the operator supplied values into account. An empty list means every PROPERTY
   * parameter of the task resolves and the task can be launched.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Property keys that do not resolve, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findUnresolvedProperties(Integer taskId, Map<String, String> variables) throws AWException {
    Task task = taskRepository.loadTask(taskId);
    parameterOverrides.apply(task, variables, parameterOverrides.overridableSources(TriggerType.MANUAL));
    return parameterOverrides.unresolvedProperties(task);
  }

  /**
   * On finish task event: update the parent status and launch the dependencies when the task configuration
   * allows it for the execution status
   *
   * @param task      Finished task
   * @param execution Task execution
   * @param submitter Scheduling entry point for the dependencies
   * @throws AWException Error updating the parent or launching the dependencies
   */
  public void onFinishTask(Task task, TaskExecution execution, TaskSubmitter submitter) throws AWException {
    // Update parent status if failed
    updateParentStatus(execution);

    // Check if the dependencies have to be executed
    if (checkDependencyExecution(task, execution)) {
      executeDependencies(task, execution, submitter);
    }
  }

  /**
   * Execute immediately a task
   *
   * @param taskId      Task id
   * @param triggerType Trigger type
   * @param launcher    Task launcher
   * @param parent      Parent execution
   * @param variables   Operator supplied values for variable parameters (null when not applicable)
   * @param submitter   Scheduling entry point
   * @throws AWException Error executing immediate task
   */
  private void launch(Integer taskId, TriggerType triggerType, String launcher, TaskExecution parent,
                      Map<String, String> variables, TaskSubmitter submitter) throws AWException {
    try {
      Task task = taskRepository.loadTask(taskId);
      task.setLauncher(launcher);
      task.setParentExecution(parent);

      // Override variable parameters with the operator supplied values
      parameterOverrides.apply(task, variables, parameterOverrides.overridableSources(triggerType));

      JobDataMap dataMap = new JobDataMap();
      dataMap.put(TASK, task);

      // Add a new trigger to task
      task.setTrigger(TriggerFactory.getInstance(triggerType, dataMap));

      // Set launcher to job data
      task.setJob(task.getJob()
        .getJobBuilder()
        .withIdentity(task.getTrigger().getKey().getName(), task.getTrigger().getKey().getGroup())
        .build());

      // Schedule task
      submitter.submit(task);
    } catch (Exception exc) {
      throw new AWException("Error launching immediate task: " + taskId, exc);
    }
  }

  /**
   * Set the parent status to warning when this dependency failed and the parent is configured so
   *
   * @param execution Task execution
   */
  private void updateParentStatus(TaskExecution execution) throws AWException {
    if (execution.getParentExecution() != null) {
      try {
        Task parentTask = taskRepository.loadTask(execution.getParentExecution().getTaskId());
        if (TaskStatus.JOB_ERROR.equals(TaskStatus.valueOf(execution.getStatus()))
          && parentTask.isSetTaskOnWarningIfDependencyError()) {
          executionRepository.changeStatus(execution.getParentExecution(), TaskStatus.JOB_WARNING,
            serviceConfig.getLocale(WARNING_MESSAGE_TASK_DEPENDENCY_ERROR,
              execution.getTaskId().toString(), execution.getExecutionId().toString()));
        }
      } catch (Exception exc) {
        throw new AWException("Error setting task status", exc);
      }
    }
  }

  /**
   * Check if the task has to execute dependencies depending on its
   * configuration
   *
   * @param task      Task
   * @param execution Task execution
   * @return boolean
   */
  private boolean checkDependencyExecution(Task task, TaskExecution execution) {
    // Check if the dependencies have to be launched
    return switch (TaskStatus.valueOf(execution.getStatus())) {
      case JOB_OK -> true;
      case JOB_WARNING -> task.isLaunchDependenciesOnWarning();
      case JOB_ERROR -> task.isLaunchDependenciesOnError();
      default -> false;
    };
  }

  /**
   * Executes the dependencies for the given task
   *
   * @param task      Task to execute dependencies
   * @param execution Task execution
   * @param submitter Scheduling entry point
   */
  private void executeDependencies(Task task, TaskExecution execution, TaskSubmitter submitter) throws AWException {
    // Propagate the parent parameter values to each dependent child (#724). The child decides which
    // apply: only the child's VARIABLE parameters whose name matches are overridden.
    // Building the map from all parent parameters keeps the origin (value/property/variable)
    // irrelevant — the contract is defined by the child. Cascading is automatic: the child's mutated
    // task travels in the JobDataMap, so its own dependents inherit the accumulated values.
    Map<String, String> parentValues = parameterOverrides.propagatedValues(task);

    // Iterate dependencies and create a new task for each.
    for (TaskDependency dependency : task.getDependencyList()) {
      // Execute task dependency
      launchDependency(dependency.getTaskId(), execution, parentValues, submitter);
    }
  }
}
