package com.almis.awe.scheduler.dao;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.task.DeferredExecutionRepository;
import com.almis.awe.scheduler.dao.task.FlowRunPresenter;
import com.almis.awe.scheduler.dao.task.FlowRunRepository;
import com.almis.awe.scheduler.dao.task.SchedulerConfigurationPresenter;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter;
import com.almis.awe.scheduler.dao.task.TaskEditorPresenter;
import com.almis.awe.scheduler.dao.task.TaskExecutionPresenter;
import com.almis.awe.scheduler.dao.task.TaskExecutionRepository;
import com.almis.awe.scheduler.dao.task.TaskFlowGraphPresenter;
import com.almis.awe.scheduler.dao.task.TaskImmediateLauncher;
import com.almis.awe.scheduler.dao.task.TaskListPresenter;
import com.almis.awe.scheduler.dao.task.TaskPagePresenter;
import com.almis.awe.scheduler.dao.task.TaskParameterOverrides;
import com.almis.awe.scheduler.dao.task.TaskRepository;
import com.almis.awe.scheduler.dao.task.TaskScheduling;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.ServerTestResultStore;
import com.almis.awe.scheduler.util.TaskSchedulePreview;
import com.almis.awe.security.authorization.PermissionTargetAuthorizer;
import com.almis.awe.security.authorization.TargetKind;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.scheduling.annotation.Async;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
 *   <li>{@link TaskCatalogPresenter}: task catalog (tasks screen)</li>
 *   <li>{@link TaskPagePresenter}: task screen</li>
 *   <li>{@link TaskFlowGraphPresenter}: one-level flow graph of the task screen</li>
 *   <li>{@link FlowRunPresenter}: summary and graph of one flow run (flow run page)</li>
 *   <li>{@link TaskEditorPresenter}: parents and copy of a task in the task editor</li>
 *   <li>{@link SchedulerConfigurationPresenter}: servers and calendars of the configuration screen</li>
 *   <li>{@link TaskExecutionPresenter}: execution list, execution screen and progress widgets</li>
 *   <li>{@link TaskImmediateLauncher} and {@link TaskParameterOverrides}: immediate launches, parameter
 *   overrides and dependencies</li>
 * </ul>
 * Immediate launches are handed to the scheduler through {@link #addTaskToScheduler(Task)}.
 */
@Slf4j
public class TaskDAO extends ServiceConfig {

  private final TaskRepository taskRepository;
  private final FlowRunRepository flowRunRepository;
  private final TaskExecutionRepository executionRepository;
  private final DeferredExecutionRepository deferredRepository;
  private final TaskScheduling taskScheduling;
  private final TaskListPresenter taskListPresenter;
  private final TaskCatalogPresenter taskCatalogPresenter;
  private final TaskExecutionPresenter executionPresenter;
  private final TaskPagePresenter taskPagePresenter;
  private final TaskFlowGraphPresenter taskFlowGraphPresenter;
  private final FlowRunPresenter flowRunPresenter;
  private final TaskEditorPresenter taskEditorPresenter;
  private final SchedulerConfigurationPresenter configurationPresenter;
  private final SchedulerEnginePresenter enginePresenter;
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
    this.flowRunRepository = new FlowRunRepository(queryService, maintainService, queryUtil);
    this.executionRepository = new TaskExecutionRepository(queryService, maintainService, queryUtil,
      executionLogStore, storedExecutions, flowRunRepository);
    this.deferredRepository = new DeferredExecutionRepository(queryService, maintainService, queryUtil);
    this.taskScheduling = new TaskScheduling(scheduler, fileChecker, taskRepository, executionRepository, this);
    this.taskListPresenter = new TaskListPresenter(scheduler, queryService, queryUtil, taskScheduling, this);
    this.taskCatalogPresenter = new TaskCatalogPresenter(queryService, queryUtil, flowRunRepository,
      taskListPresenter, this, Clock.systemDefaultZone());
    this.executionPresenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore,
      executionRepository, deferredRepository, this);
    this.taskPagePresenter = new TaskPagePresenter(queryService, queryUtil, taskCatalogPresenter, executionPresenter,
      this, this::canLaunchQuery);
    this.taskFlowGraphPresenter = new TaskFlowGraphPresenter(queryService, queryUtil, taskCatalogPresenter, this,
      Clock.systemDefaultZone());
    this.flowRunPresenter = new FlowRunPresenter(queryService, queryUtil, flowRunRepository, executionPresenter,
      deferredRepository, this, Clock.systemDefaultZone());
    this.taskEditorPresenter = new TaskEditorPresenter(queryService, queryUtil, flowRunRepository, taskCatalogPresenter,
      this);
    this.configurationPresenter = new SchedulerConfigurationPresenter(queryService, queryUtil, this,
      Clock.systemDefaultZone());
    this.enginePresenter = new SchedulerEnginePresenter(queryService, queryUtil, this, Clock.systemDefaultZone(),
      () -> deferredRepository.findRunning(null, null));
    this.parameterOverrides = new TaskParameterOverrides(this::getProperty);
    this.immediateLauncher = new TaskImmediateLauncher(taskRepository, executionRepository, parameterOverrides,
      this, flowRunRepository);
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
   * Close a running execution: only the call that updates it owns the end of the execution
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @param status      Final status
   * @return True when this call closed the running execution
   */
  public boolean closeRunningExecution(Integer taskId, Integer executionId, TaskStatus status) throws AWException {
    return executionRepository.closeRunningExecution(taskId, executionId, status);
  }

  /**
   * Register an execution that was accepted and finishes later
   *
   * @param taskId            Task id
   * @param executionId       Execution id
   * @param externalReference Opaque reference of the work in the external system (null when there is none)
   * @param acceptedDate      Date the execution was accepted
   * @param dueDate           Date from which the execution is overdue
   */
  public void insertDeferredExecution(Integer taskId, Integer executionId, String externalReference,
                                      Instant acceptedDate, Instant dueDate) throws AWException {
    deferredRepository.insert(taskId, executionId, externalReference, acceptedDate, dueDate);
  }

  /**
   * Store who closed a deferred execution and when
   *
   * @param taskId         Task id
   * @param executionId    Execution id
   * @param source         Who closed the execution
   * @param completionDate Date the execution was closed
   */
  public void recordDeferredCompletion(Integer taskId, Integer executionId, CompletionSource source,
                                       Instant completionDate) throws AWException {
    deferredRepository.complete(taskId, executionId, source, completionDate);
  }

  /**
   * Store the outcome of the cancellation notice of a deferred execution
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @param status      Outcome of the notice
   * @param error       Error of a failed notice, or null
   */
  public void recordCancellationNotice(Integer taskId, Integer executionId, String status, String error)
    throws AWException {
    deferredRepository.cancellationNotice(taskId, executionId, status, error);
  }

  /**
   * Find the deferred state of an execution, whether it is still running or not
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return Deferred state, or null when the execution was never deferred
   */
  public DeferredExecution getDeferredExecution(Integer taskId, Integer executionId) throws AWException {
    return deferredRepository.find(taskId, executionId);
  }

  /**
   * Get the deferred executions that are still running
   *
   * @param taskId    Task id to filter by, or null for every task
   * @param dueBefore Only the executions whose due date is before this date, or null for all
   * @return Running deferred executions
   */
  public List<DeferredExecution> getRunningDeferredExecutions(Integer taskId, Instant dueBefore) throws AWException {
    return deferredRepository.findRunning(taskId, dueBefore);
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
   * Refresh an edited task in the scheduler with its stored launch, without purging its executions, their logs nor
   * the files it has seen (see {@link TaskScheduling#refreshTask(Integer)})
   *
   * @param taskId Task identifier
   * @throws AWException Error refreshing the task
   */
  public void refreshTask(Integer taskId) throws AWException {
    taskScheduling.refreshTask(taskId);
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
   * Retrieve a page of the executions of all tasks, newest first, with the decoration of the task execution list
   *
   * @param criteria Filters and page
   * @return Execution list page
   * @throws AWException Error retrieving the executions
   */
  public ServiceData getExecutionList(ExecutionListCriteria criteria) throws AWException {
    return executionPresenter.getExecutionList(criteria);
  }

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run: the runs and the executions without run as the
   * top-level rows, the executions of each run as its children
   *
   * @param criteria Filters and page of the top-level rows
   * @return Page of runs with their executions
   * @throws AWException Error retrieving the executions
   */
  public ServiceData getRunList(ExecutionListCriteria criteria) throws AWException {
    return executionPresenter.getRunList(criteria);
  }

  /**
   * Validate the filters and the page of the execution list of all tasks
   *
   * @param criteria Filters and page
   * @throws AWException A value is not valid
   */
  public void validateExecutionListCriteria(ExecutionListCriteria criteria) throws AWException {
    executionPresenter.validate(criteria);
  }

  /**
   * Retrieve a page of the task catalog (tasks screen)
   *
   * @param criteria Filters, sort and page
   * @return Page of tasks
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getTaskCatalog(TaskCatalogCriteria criteria) throws AWException {
    return taskCatalogPresenter.getCatalog(criteria);
  }

  /**
   * Retrieve the options of the active filter of the task catalog with their counts
   *
   * @param criteria Filters of the catalog (search, launch and last status)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getTaskCatalogActiveCounts(TaskCatalogCriteria criteria) throws AWException {
    return taskCatalogPresenter.getActiveCounts(criteria);
  }

  /**
   * Retrieve the tabs of the configuration screen with the count of servers and calendars and the state of the engine
   *
   * @param engineStatus Locale key of the state of the engine, null when the engine cannot be read
   * @return One row per tab: value, label and icon
   * @throws AWException The servers or the calendars cannot be read
   */
  public ServiceData getConfigurationTabs(String engineStatus) throws AWException {
    return configurationPresenter.getTabs(engineStatus);
  }

  /**
   * Retrieve the servers of the configuration screen with their connection and the tasks that use them
   *
   * @param search Text contained in the name or in the host
   * @param active 1 active servers, 0 inactive servers; every server when blank
   * @return Servers sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the servers cannot be read
   */
  public ServiceData getConfigurationServers(String search, String active) throws AWException {
    return configurationPresenter.getServers(search, active,
      new ServerTestResultStore(this::getSession).getResults());
  }

  /**
   * Server for the side panel of the configuration screen, without its secrets: its connection, whether each secret is
   * saved, and the select actions that fill the panel; a new server without one
   *
   * @param serverId Server, null for a new server
   * @return One row and one select action per field of the panel
   * @throws AWException The server no longer exists or cannot be read
   */
  public ServiceData loadConfigurationServer(Integer serverId) throws AWException {
    return configurationPresenter.loadServer(serverId);
  }

  /**
   * Calendar for the side panel of the configuration screen: the values of its criteria and how many dates it has, or a
   * new calendar
   *
   * @param calendarId Calendar, null for a new calendar
   * @return One silent select action per criterion of the panel
   * @throws AWException The calendar no longer exists or cannot be read
   */
  public ServiceData loadConfigurationCalendar(Integer calendarId) throws AWException {
    return configurationPresenter.loadCalendar(calendarId);
  }

  /**
   * Dates of the calendar side panel of the configuration screen, in order, each one with its weekday
   *
   * @param calendarId Calendar, null for a new calendar
   * @return One row per date
   * @throws AWException The dates cannot be read
   */
  public ServiceData getConfigurationCalendarPanelDates(Integer calendarId) throws AWException {
    return configurationPresenter.getCalendarDates(calendarId);
  }

  /**
   * «En uso por» of the server side panel of the configuration screen: the tasks that use the server, sorted by name,
   * with their launch and their last status
   *
   * @param serverId Server, null for a new server
   * @return One row per task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getConfigurationServerTasks(Integer serverId) throws AWException {
    return configurationPresenter.getServerTasks(serverId, taskCatalogPresenter::getSummaries);
  }

  /**
   * Choice cards of the connection type of the server side panel of the configuration screen
   *
   * @return One row per type: value, label, icon and description
   */
  public ServiceData getConfigurationServerTypes() {
    return configurationPresenter.getServerTypes();
  }

  /**
   * Refuse to delete a server that a task uses
   *
   * @param serverIds Servers to delete
   * @throws AWException A server is used by a task (the message names the tasks), or the tasks cannot be read
   */
  public void validateServersNotInUse(List<?> serverIds) throws AWException {
    configurationPresenter.validateServersNotInUse(serverIds);
  }

  /**
   * Retrieve the options of the active filter of the servers of the configuration screen with their counts
   *
   * @param search Text contained in the name or in the host
   * @return One row per option: value, label, count and tone
   * @throws AWException The servers cannot be read
   */
  public ServiceData getConfigurationServerActiveCounts(String search) throws AWException {
    return configurationPresenter.getServerActiveCounts(search);
  }

  /**
   * Retrieve the calendars of the configuration screen with their dates, their next date and the tasks that use them
   *
   * @param search Text contained in the name
   * @param active 1 active calendars, 0 inactive calendars; every calendar when blank
   * @return Calendars sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the calendars cannot be
   *                     read
   */
  public ServiceData getConfigurationCalendars(String search, String active) throws AWException {
    return configurationPresenter.getCalendars(search, active);
  }

  /**
   * Retrieve the options of the active filter of the calendars of the configuration screen with their counts
   *
   * @param search Text contained in the name
   * @return One row per option: value, label, count and tone
   * @throws AWException The calendars cannot be read
   */
  public ServiceData getConfigurationCalendarActiveCounts(String search) throws AWException {
    return configurationPresenter.getCalendarActiveCounts(search);
  }

  /**
   * Before deleting a calendar of the configuration screen: the tasks that use it and the next days they would run on
   * without it
   *
   * @param calendarId Calendar to delete
   * @return One row per task, sorted by name
   * @throws AWException The tasks or the calendars cannot be read
   */
  public ServiceData getConfigurationCalendarDeleteCheck(Integer calendarId) throws AWException {
    return configurationPresenter.getCalendarDeleteCheck(calendarId);
  }

  /**
   * «Motor» tab of the configuration screen: the card of the engine
   *
   * @param engine   Engine, null when the scheduler cannot be read
   * @param triggers Schedules of the engine
   * @return One row
   * @throws AWException The executions waiting for a confirmation or the tasks cannot be read
   */
  public ServiceData getConfigurationEngine(SchedulerEnginePresenter.Engine engine,
                                            List<SchedulerEnginePresenter.EngineTrigger> triggers) throws AWException {
    return enginePresenter.getEngine(engine, triggers);
  }

  /**
   * «Motor» tab of the configuration screen: the schedules of the engine, filtered by their state
   *
   * @param triggers Schedules of the engine
   * @param filter   paused or error; every schedule when blank
   * @return One row per schedule, the next one to fire first
   * @throws AWException The filter is not valid or the tasks cannot be read
   */
  public ServiceData getConfigurationEngineTriggers(List<SchedulerEnginePresenter.EngineTrigger> triggers,
                                                    String filter) throws AWException {
    return enginePresenter.getTriggers(triggers, filter);
  }

  /**
   * «Motor» tab of the configuration screen: the options of the state filter of the schedules with their counts
   *
   * @param triggers Schedules of the engine
   * @return One row per option
   */
  public ServiceData getConfigurationEngineTriggerCounts(List<SchedulerEnginePresenter.EngineTrigger> triggers) {
    return enginePresenter.getTriggerCounts(triggers);
  }

  /**
   * Validate the state filter of the schedules of the engine
   *
   * @param filter paused or error; every schedule when blank
   * @throws AWException The filter is not valid
   */
  public void validateConfigurationEngineFilter(String filter) throws AWException {
    enginePresenter.validateFilter(filter);
  }

  /**
   * Retrieve what the task screen shows of a task: its header, the summary of its saved executions, its last execution
   * and its configuration
   *
   * @param taskId Task identifier
   * @return One row, none when the task does not exist
   * @throws AWException The task cannot be read
   */
  public ServiceData getTaskPage(Integer taskId) throws AWException {
    return taskPagePresenter.getPage(taskId);
  }

  /**
   * Retrieve the one-level flow graph of the task screen: the rows of the flow graph, the tasks of the level with when
   * they last ran and, the scheduled ones, their next execution
   *
   * @param taskId Task identifier
   * @return Rows of the graph
   * @throws AWException The graph or the tasks cannot be read
   */
  public ServiceData getTaskFlowGraph(Integer taskId) throws AWException {
    return taskFlowGraphPresenter.getGraph(taskId);
  }

  /**
   * Retrieve the summary of a flow run (flow run page): its status with the rule written out and the task it points
   * to, who launched it, its dates, its root and its tasks by state
   *
   * @param runId Flow run identifier
   * @return One row, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunSummary(String runId) throws AWException {
    return flowRunPresenter.getSummary(runId);
  }

  /**
   * Retrieve the graph of a flow run (flow run page): one row per task of the run and parent, each task with its last
   * execution in the run and each edge with its state
   *
   * @param runId Flow run identifier
   * @return Rows of the graph, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunGraph(String runId) throws AWException {
    return flowRunPresenter.getGraph(runId);
  }

  /**
   * Retrieve the tasks a task can run after, for the parents picker of the task editor
   *
   * @param taskId     Task edited, null for a new task
   * @param copyTaskId Task duplicated by a new task, whose parents are the chosen ones
   * @return One row per task
   * @throws AWException The tasks cannot be read
   * @see TaskEditorPresenter#getParents(Integer, Integer)
   */
  public ServiceData getTaskEditorParents(Integer taskId, Integer copyTaskId) throws AWException {
    return taskEditorPresenter.getParents(taskId, copyTaskId);
  }

  /**
   * Retrieve the data of a task to duplicate it in the task editor
   *
   * @param taskId Task to duplicate
   * @return One row, none when the task does not exist
   * @throws AWException The task cannot be read
   * @see TaskEditorPresenter#getCopy(Integer)
   */
  public ServiceData getTaskEditorCopy(Integer taskId) throws AWException {
    return taskEditorPresenter.getCopy(taskId);
  }

  /**
   * Retrieve the data of the task editor screen for a mode (new, edit or copy)
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return One row
   * @throws AWException The task cannot be read
   * @see TaskEditorPresenter#getEditor(String, Integer)
   */
  public ServiceData getTaskEditor(String mode, Integer taskId) throws AWException {
    return taskEditorPresenter.getEditor(mode, taskId);
  }

  /**
   * Retrieve the options of the parents picker of the task editor
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return One row per task
   * @throws AWException The tasks cannot be read
   * @see TaskEditorPresenter#getEditorParents(String, Integer)
   */
  public ServiceData getTaskEditorParentOptions(String mode, Integer taskId) throws AWException {
    return taskEditorPresenter.getEditorParents(mode, taskId);
  }

  /**
   * Retrieve the parameters of the task of the task editor
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return The parameters
   * @throws AWException The parameters cannot be read
   * @see TaskEditorPresenter#getEditorParameters(String, Integer)
   */
  public ServiceData getTaskEditorParameters(String mode, Integer taskId) throws AWException {
    return taskEditorPresenter.getEditorParameters(mode, taskId);
  }

  /**
   * Retrieve one side of the one-level graph of the task editor
   *
   * @param mode    Mode of the editor
   * @param taskId  Task of the screen that opened the editor
   * @param parents Parents chosen in the editor
   * @param side    Side of the graph: parents or children
   * @return Rows of the graph
   * @throws AWException The tasks cannot be read
   * @see TaskEditorPresenter#getEditorGraph(String, Integer, List, String)
   */
  public ServiceData getTaskEditorGraph(String mode, Integer taskId, List<Integer> parents, String side)
    throws AWException {
    return taskEditorPresenter.getEditorGraph(mode, taskId, parents, side);
  }

  /**
   * Options of a choice of the task editor drawn as cards
   *
   * @param group Choice: when or execution
   * @return One row per option
   * @see TaskEditorPresenter#getEditorOptions(String)
   */
  public ServiceData getTaskEditorOptions(String group) {
    return taskEditorPresenter.getEditorOptions(group);
  }

  /**
   * Live preview of the «When» of a scheduled task in the task editor
   *
   * @param input What the preview is built from
   * @return Row of the preview card
   * @see TaskEditorPresenter#schedulePreview(TaskSchedulePreview.Input, LocalDateTime)
   */
  public ServiceData getTaskEditorSchedule(TaskSchedulePreview.Input input) {
    return taskEditorPresenter.schedulePreview(input, LocalDateTime.now());
  }

  /**
   * Answer of a save of the task editor: its message and the saved task as the task of the screen
   *
   * @param taskId Task saved
   * @return Message and select action
   * @see TaskEditorPresenter#saved(Integer)
   */
  public ServiceData getTaskEditorSaved(Integer taskId) {
    return taskEditorPresenter.saved(taskId);
  }

  /**
   * Replace the parents of a task (the dependency rows where it is the child) with these ones
   *
   * @param taskId  Task that runs after the parents
   * @param parents Parents, each once
   * @throws AWException Error writing the dependencies
   */
  public void saveTaskParents(Integer taskId, List<Integer> parents) throws AWException {
    flowRunRepository.replaceParents(taskId, parents);
  }

  /**
   * Validate the filters, the sort and the page of the task catalog
   *
   * @param criteria Filters, sort and page
   * @throws AWException A value is not valid (InvalidExecutionFilterException)
   */
  public void validateTaskCatalogCriteria(TaskCatalogCriteria criteria) throws AWException {
    taskCatalogPresenter.validate(criteria);
  }

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts
   *
   * @param criteria Filters of the execution list (search, origin and start)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid or the executions cannot be counted
   */
  public ServiceData getExecutionStateCounts(ExecutionListCriteria criteria) throws AWException {
    return executionPresenter.getExecutionStateCounts(criteria);
  }

  /**
   * Retrieve the execution counters of the activity screen: running, waiting for confirmation, queued and errors in
   * the last 24 hours
   *
   * @return One row with the counters
   * @throws AWException Error counting the executions
   */
  public ServiceData getExecutionCounters() throws AWException {
    return executionPresenter.getExecutionCounters();
  }

  /**
   * Retrieve the execution counters of the activity screen for the tasks whose name contains a text. The screen and
   * the API count every task ({@link #getExecutionCounters()}, the same queries without the name filter); this scope
   * lets a test on a shared database count exactly the executions of its own tasks
   *
   * @param taskName Text contained in the task name, case insensitive; every task when null or blank
   * @return One row with the counters
   * @throws AWException Error counting the executions
   */
  public ServiceData getExecutionCounters(String taskName) throws AWException {
    return executionPresenter.getExecutionCounters(taskName);
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
   * Launch a task because a webhook arrived; the data gives the values of its mapped variable parameters
   *
   * @param taskId  Task identifier
   * @param hookKey Key of the hook that received the webhook
   * @param data    Data of the webhook
   * @throws AWException Error executing the task
   */
  public void launchWebhook(Integer taskId, String hookKey, JsonNode data) throws AWException {
    immediateLauncher.launchWebhook(taskId, hookKey, data, this::addTaskToScheduler);
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
   * Parameters of the task whose operator supplied value is not a value of their type. An empty list means every
   * supplied value can be used.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Names of the parameters with a value that is not valid, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findInvalidParameterValues(Integer taskId, Map<String, String> variables) throws AWException {
    return immediateLauncher.findInvalidValues(taskId, variables);
  }

  /**
   * Property parameters of the task whose operator supplied value changes their key: a launch cannot change it. An
   * empty list means no key changes.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Names of the property parameters whose key would change, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findChangedPropertyKeys(Integer taskId, Map<String, String> variables) throws AWException {
    return immediateLauncher.findChangedPropertyKeys(taskId, variables);
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
   * @param path    Execution log locator ({@code "{taskId}-{executionId}"})
   * @param address Button address
   * @return ServiceData
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address) throws AWException {
    return executionPresenter.loadExecutionScreen(path, address);
  }

  /**
   * Load execution screen parameters of the execution of the button address, else of the button value, else of the
   * selected row
   *
   * @param path            Execution log locator of the button value ({@code "{taskId}-{executionId}"})
   * @param address         Button address
   * @param selectedLocator Execution log locator of the selected row
   * @return ServiceData
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address, String selectedLocator) throws AWException {
    return executionPresenter.loadExecutionScreen(path, address, selectedLocator);
  }

  /**
   * Retrieve the execution log window from a line offset (get-execution-log action), in both store modes.
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
   * Progress of an execution as the fields of a card
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service data
   * @throws AWException Error reading the execution
   * @see TaskExecutionPresenter#getExecutionProgress(Integer, Integer)
   */
  public ServiceData getExecutionProgress(Integer taskId, Integer executionId) throws AWException {
    return executionPresenter.getExecutionProgress(taskId, executionId);
  }

  /**
   * Launch parameters of an execution: the ones stored with its flow run, operator values and resolved parameters of
   * the root task
   *
   * @param runId Flow run identifier of the execution
   * @return Service data with one row per parameter, none without run
   * @throws AWException Error reading the run parameters
   * @see FlowRunRepository#getLaunchParameters(String)
   */
  public ServiceData getExecutionParameters(String runId) throws AWException {
    return executionPresenter.getExecutionParameters(runId == null || runId.isBlank()
      ? Map.of()
      : flowRunRepository.getLaunchParameters(runId));
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
   * Children of every task, from the stored dependencies
   *
   * @return Children by task identifier
   * @throws AWException Error reading the dependencies
   */
  public Map<Integer, List<Integer>> getChildrenByTask() throws AWException {
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (TaskDependency dependency : flowRunRepository.getDependencies()) {
      children.computeIfAbsent(dependency.getParentId(), key -> new ArrayList<>()).add(dependency.getTaskId());
    }
    return children;
  }

  /**
   * Check if the session user holds the permission of a query, as the launches of the clients are checked: when the
   * authorization is not enforced every query can be launched. Without the security beans (outside the application)
   * nothing is granted
   *
   * @param queryId Query identifier
   * @return The user may read what the query reads
   */
  private boolean canLaunchQuery(String queryId) {
    try {
      return getBean(SecurityConfigProperties.class).getAuthorization().getMode() != AuthorizationMode.ENFORCE
        || getBean(PermissionTargetAuthorizer.class).canLaunch(queryId, TargetKind.QUERY);
    } catch (RuntimeException exc) {
      log.warn("The permission of the query {} cannot be checked: it is not granted", queryId, exc);
      return false;
    }
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
