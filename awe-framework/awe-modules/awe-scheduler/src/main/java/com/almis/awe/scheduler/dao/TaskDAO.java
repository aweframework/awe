package com.almis.awe.scheduler.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.task.TaskExecutionPresenter;
import com.almis.awe.scheduler.dao.task.TaskExecutionRepository;
import com.almis.awe.scheduler.dao.task.TaskImmediateLauncher;
import com.almis.awe.scheduler.dao.task.TaskListPresenter;
import com.almis.awe.scheduler.dao.task.TaskParameterOverrides;
import com.almis.awe.scheduler.dao.task.TaskRepository;
import com.almis.awe.scheduler.dao.task.TaskScheduling;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.scheduling.annotation.Async;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;

/**
 * Scheduler task facade. Every responsibility lives in a collaborator of the
 * {@code com.almis.awe.scheduler.dao.task} package; this class keeps the public API and delegates:
 * <ul>
 *   <li>{@link TaskRepository}: task loading and file modification table</li>
 *   <li>{@link TaskExecutionRepository}: execution persistence and purge</li>
 *   <li>{@link TaskScheduling}: Quartz jobs and triggers</li>
 *   <li>{@link TaskListPresenter}: task list and maintain variables screens</li>
 *   <li>{@link TaskExecutionPresenter}: execution list, execution screen and progress widgets</li>
 *   <li>{@link TaskImmediateLauncher} and {@link TaskParameterOverrides}: immediate launches, parameter
 *   overrides and dependencies</li>
 * </ul>
 * Immediate launches are handed to the scheduler through {@link #addTaskToScheduler(Task)}.
 */
@Slf4j
public class TaskDAO extends ServiceConfig {

  private final TaskRepository taskRepository;
  private final TaskExecutionRepository executionRepository;
  private final TaskScheduling taskScheduling;
  private final TaskListPresenter taskListPresenter;
  private final TaskExecutionPresenter executionPresenter;
  private final TaskParameterOverrides parameterOverrides;
  private final TaskImmediateLauncher immediateLauncher;

  /**
   * Autowired constructor
   *
   * @param scheduler         Scheduler
   * @param storedExecutions  Stored execution
   * @param queryService      Query service
   * @param maintainService   Maintain service
   * @param queryUtil         Query utilities
   * @param calendarDAO       Calendar DAO
   * @param serverDAO         Server DAO
   * @param fileChecker       File checker
   * @param executionLogStore Execution log store
   */
  public TaskDAO(Scheduler scheduler, int storedExecutions, QueryService queryService, MaintainService maintainService, QueryUtil queryUtil,
                 CalendarDAO calendarDAO, ServerDAO serverDAO, FileChecker fileChecker, ExecutionLogStore executionLogStore) {
    this.taskRepository = new TaskRepository(scheduler, queryService, maintainService, queryUtil, calendarDAO,
      serverDAO, this);
    this.executionRepository = new TaskExecutionRepository(queryService, maintainService, queryUtil,
      executionLogStore, storedExecutions);
    this.taskScheduling = new TaskScheduling(scheduler, fileChecker, taskRepository, executionRepository, this);
    this.taskListPresenter = new TaskListPresenter(scheduler, queryService, queryUtil, taskScheduling, this);
    this.executionPresenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore,
      executionRepository, this);
    this.parameterOverrides = new TaskParameterOverrides(this::getProperty);
    this.immediateLauncher = new TaskImmediateLauncher(taskRepository, executionRepository, parameterOverrides,
      this);
  }

  /**
   * Load task from trigger
   *
   * @param trigger Trigger
   * @return Generated task
   */
  public Task getTask(Trigger trigger) {
    return (Task) trigger.getJobDataMap().get(TASK);
  }

  /**
   * Load task from job detail
   *
   * @param jobDetail Job detail
   * @return Generated task
   */
  public Task getTask(JobDetail jobDetail) {
    return (Task) jobDetail.getJobDataMap().get(TASK);
  }

  /**
   * Load task from task id
   *
   * @param taskId Task id
   * @return Generated task
   * @throws AWException Error retrieving task
   */
  @Async("schedulerTaskPool")
  public Future<Task> getTask(Integer taskId) throws AWException {
    return CompletableFuture.completedFuture(taskRepository.loadTask(taskId));
  }

  /**
   * Check if task is allowed to be launched
   *
   * @param task Task
   * @return Task is allowed
   */
  public boolean isTaskExecutionAllowed(Task task) {
    return taskScheduling.isTaskExecutionAllowed(task);
  }

  /**
   * Start a task
   *
   * @param task Task
   * @return Task execution
   */
  public TaskExecution startTask(Task task) throws AWException {
    return executionRepository.startTask(task);
  }

  /**
   * End a task
   *
   * @param task      Task
   * @param execution Task execution
   * @return Task execution
   */
  public TaskExecution endTask(Task task, TaskExecution execution) throws AWException {
    return executionRepository.endTask(task, execution);
  }

  /**
   * Retrieve executions to purge (sorted)
   *
   * @param taskId     Task identifier
   * @param executions Executions to purge
   * @return Purge results
   * @throws AWException Error retrieving executions to be purged
   */
  public ServiceData getExecutionsToPurge(Integer taskId, Integer executions) throws AWException {
    return executionRepository.getExecutionsToPurge(taskId, executions);
  }

  /**
   * Purge execution log files
   *
   * @param taskId Task identifier
   * @throws AWException Error on purge
   */
  public ServiceData purgeExecutionLogFiles(Integer taskId, Integer executions) throws AWException {
    return executionRepository.purgeExecutionLogFiles(taskId, executions);
  }

  /**
   * Purge executions at start
   *
   * @throws AWException Error on purge
   */
  public ServiceData purgeExecutionsAtStart() throws AWException {
    return executionRepository.purgeExecutionsAtStart();
  }

  /**
   * Update running task status [4] to interrupted [6]
   *
   * @return Interrupt task status
   */
  public ServiceData updateInterruptedTasks() throws AWException {
    return executionRepository.updateInterruptedTasks();
  }

  /**
   * Change a task status
   *
   * @param execution Task execution
   * @param status    New status
   * @return Change status
   */
  public ServiceData changeStatus(Task task, TaskExecution execution, TaskStatus status, String reason) throws AWException {
    return executionRepository.changeStatus(execution, status, reason);
  }

  /**
   * Add task to the scheduler
   *
   * @param task Task to add
   * @throws AWException Error adding task to scheduler
   */
  public void addTaskToScheduler(Task task) throws AWException {
    taskScheduling.addTaskToScheduler(task);
  }

  /**
   * Insert and schedule a new task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  public ServiceData insertTask(Integer taskId) throws AWException {
    return taskScheduling.insertTask(taskId);
  }

  /**
   * Update and reschedule a task
   *
   * @param taskId Task identifier list
   * @return ServiceData
   * @throws AWException Error updating task
   */
  public ServiceData updateTask(Integer taskId) throws AWException {
    // delete the task to update from the scheduler instance
    deleteTask(taskId);

    // insert the updated task to the scheduler
    ServiceData serviceData = insertTask(taskId);
    log.info("[SCHEDULER][TASK_QUERY {}] The task has been updated", taskId);

    return serviceData;
  }

  /**
   * Delete a task from scheduler
   *
   * @param ideTsk Task identifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteTask(List<Integer> ideTsk) throws AWException {
    ServiceData serviceData = new ServiceData();
    for (Integer id : ideTsk) {
      serviceData = deleteTask(id);
    }
    return serviceData;
  }

  /**
   * Delete a single task from scheduler
   *
   * @param ideTsk Task i dentifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteTask(Integer ideTsk) throws AWException {
    return taskScheduling.deleteTask(ideTsk);
  }

  /**
   * Update execution time
   *
   * @param taskId Task id
   * @return Execution time
   * @throws AWException Error updating execution time
   */
  public ServiceData updateExecutionTime(Integer taskId, Integer executionId) throws AWException {
    return executionRepository.updateExecutionTime(taskId, executionId);
  }

  /**
   * Retrieve task execution list fixed
   *
   * @param taskId Task id
   * @return Task execution list fixed
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    return executionPresenter.getTaskExecutionList(taskId);
  }

  /**
   * Retrieve progress node
   *
   * @param percentage Percentage
   * @return Progress node
   */
  public ObjectNode getProgressNode(long percentage) {
    return executionPresenter.getProgressNode(percentage);
  }

  /**
   * Generate status icon
   *
   * @param execution Execution
   * @return Status icon
   */
  public ObjectNode getStatusIcon(TaskExecution execution) {
    return executionPresenter.getStatusIcon(execution);
  }

  /**
   * Retrieve progress
   *
   * @param initialDate      Initial date
   * @param averageExecution Average execution
   * @return Progress
   * @deprecated use {@link #getProgress(Instant, long)}
   */
  @Deprecated(since = "5.0.0")
  public long getProgress(Date initialDate, long averageExecution) {
    return getProgress(Instant.ofEpochMilli(initialDate.getTime()), averageExecution);
  }

  /**
   * Retrieve the progress of a running execution as a percentage of the average execution time
   *
   * @param initialDate      Execution start
   * @param averageExecution Average execution time in milliseconds
   * @return Progress percentage, 101 when there is no average execution time
   */
  public long getProgress(Instant initialDate, long averageExecution) {
    return executionPresenter.getProgress(initialDate, averageExecution);
  }

  /**
   * Retrieve elapsed time from a start date (without ms)
   *
   * @param startDate Start date
   * @return Elapsed time
   * @deprecated use {@link #getElapsedTime(Instant)}
   */
  @Deprecated(since = "5.0.0")
  public Integer getElapsedTime(Date startDate) {
    return getElapsedTime(Instant.ofEpochMilli(startDate.getTime()));
  }

  /**
   * Retrieve elapsed time from a start instant
   *
   * @param startDate Start instant
   * @return Elapsed time in milliseconds
   */
  public Integer getElapsedTime(Instant startDate) {
    return executionPresenter.getElapsedTime(startDate);
  }

  /**
   * Execute the selected task now
   *
   * @param taskId Task identifier
   * @param user   Launch user
   * @return ServiceData
   * @throws AWException Error executing immediate task
   */
  public ServiceData executeTaskNow(Integer taskId, String user) throws AWException {
    return executeTaskNow(taskId, user, null);
  }

  /**
   * Execute the selected task now, applying the operator supplied values for its variable parameters
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values (parameter name -&gt; value) for variable parameters
   * @return ServiceData
   * @throws AWException Error executing immediate task
   */
  public ServiceData executeTaskNow(Integer taskId, String user, Map<String, String> variables) throws AWException {
    immediateLauncher.launchNow(taskId, user, variables, this::addTaskToScheduler);

    // Return ok service data
    return new ServiceData();
  }

  /**
   * Apply the supplied values to the task {@link ParameterConstants#VARIABLE} parameters.
   * Only parameters whose name is present in the provided map are overridden; every other
   * parameter keeps its stored value.
   *
   * @param task      Task holding the parameter list to update
   * @param variables Supplied values (parameter name -&gt; value); null or empty is a no-op
   */
  void applyOperatorValues(Task task, Map<String, String> variables) {
    applyOperatorValues(task, variables, TaskParameterOverrides.VARIABLE_SOURCES);
  }

  /**
   * Apply the supplied values to the task parameters whose source is overridable.
   * On a {@link ParameterConstants#PROPERTY} parameter the supplied value is the property key,
   * resolved at execution time.
   *
   * @param task                Task holding the parameter list to update
   * @param variables           Supplied values (parameter name -&gt; value); null or empty is a no-op
   * @param overridableSources  Parameter sources the supplied values may override
   */
  void applyOperatorValues(Task task, Map<String, String> variables, Set<String> overridableSources) {
    parameterOverrides.apply(task, variables, overridableSources);
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
    return immediateLauncher.findUnresolvedProperties(taskId, variables);
  }

  /**
   * Execute the task as dependency, without propagating any parent parameter value.
   *
   * @param taskId          Task identifier
   * @param parentExecution Parent execution
   * @throws AWException Error executing dependency task
   */
  public void executeDependency(Integer taskId, TaskExecution parentExecution) throws AWException {
    executeDependency(taskId, parentExecution, Collections.emptyMap());
  }

  /**
   * Execute the task as dependency, propagating the given parent parameter values to the child.
   * The child defines the contract: {@link #applyOperatorValues(Task, Map)} only overrides the
   * child's VARIABLE (source="1") parameters whose name is present in the map.
   *
   * @param taskId          Task identifier
   * @param parentExecution Parent execution
   * @param variables       Parent parameter values (name -&gt; value) to propagate; empty for none
   * @throws AWException Error executing dependency task
   */
  public void executeDependency(Integer taskId, TaskExecution parentExecution, Map<String, String> variables) throws AWException {
    immediateLauncher.launchDependency(taskId, parentExecution, variables, this::addTaskToScheduler);
  }

  /**
   * Pause the selected task
   *
   * @param task Task to pause
   * @return ServiceData
   * @throws SchedulerException Error pausing task
   */
  public ServiceData pauseTask(Task task) throws SchedulerException {
    return taskScheduling.pauseTask(task);
  }

  /**
   * Resume the selected task
   *
   * @param task Task to resume
   * @return ServiceData
   * @throws AWException Error resuming task
   */
  public ServiceData resumeTask(Task task) throws AWException {
    return taskScheduling.resumeTask(task);
  }

  /**
   * Retrieve the task list
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList(TaskListCriteria taskListCriteria) throws AWException {
    return taskListPresenter.getTaskList(taskListCriteria);
  }

  /**
   * Load needed variables from the selected maintain
   *
   * @param maintainStr Maintain identifier
   * @return ServiceData
   * @throws AWException Error loading maintain variables
   */
  public ServiceData loadMaintainVariables(String maintainStr) throws AWException {
    return taskListPresenter.loadMaintainVariables(maintainStr);
  }

  /**
   * Load execution screen parameters
   *
   * @param path    Log path
   * @param address Button address
   * @return ServiceData
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address) throws AWException {
    return executionPresenter.loadExecutionScreen(path, address);
  }

  /**
   * Retrieve the database-mode execution log window from a line offset (get-execution-log action).
   *
   * @param taskId        Task identifier
   * @param executionId   Execution identifier
   * @param offset        Line offset to read from
   * @param clientVersion Window version the caller currently holds, as previously returned in
   *                      {@code LOG_CONTENT_VERSION}; {@code null} when absent
   * @return ServiceData
   * @throws AWException Error reading the execution log
   * @see TaskExecutionPresenter#getExecutionLog(Integer, Integer, Integer, String)
   */
  public ServiceData getExecutionLog(Integer taskId, Integer executionId, Integer offset, String clientVersion) throws AWException {
    return executionPresenter.getExecutionLog(taskId, executionId, offset, clientVersion);
  }

  /**
   * Reload execution task parameters
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service Data
   * @throws AWException Error reloading execution screen
   */
  public ServiceData reloadExecutionScreen(Integer taskId, Integer executionId) throws AWException {
    return executionPresenter.reloadExecutionScreen(taskId, executionId);
  }

  /**
   * Get task execution from trigger
   *
   * @param trigger Trigger
   * @return Task execution from trigger
   */
  public TaskExecution getTaskExecution(Trigger trigger) {
    return taskScheduling.getTaskExecution(trigger);
  }

  /**
   * Get execution from Database
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return Task execution
   * @throws AWException Error retrieving task execution from database
   */
  public TaskExecution getTaskExecution(Integer taskId, Integer executionId) throws AWException {
    return executionRepository.getTaskExecution(taskId, executionId);
  }

  /**
   * Get execution from Database
   *
   * @param task        Task
   * @param executionId Execution id
   * @return Task execution
   * @throws AWException Error retrieving task execution from database
   */
  public TaskExecution getTaskExecution(Task task, Integer executionId) throws AWException {
    return getTaskExecution(task.getTaskId(), executionId);
  }

  /**
   * Get the average time as string
   *
   * @param taskId Task identifier
   * @return Average time as string
   * @throws AWException Error retrieving average time
   */
  public Integer getAverageTime(Integer taskId) throws AWException {
    return executionRepository.getAverageTime(taskId);
  }

  /**
   * On finish task event
   *
   * @param task      Finished task
   * @param execution Task execution
   */
  public void onFinishTask(Task task, TaskExecution execution) throws AWException {
    immediateLauncher.onFinishTask(task, execution, this::addTaskToScheduler);
  }
}
