package com.almis.awe.scheduler.service;

import com.almis.awe.builder.client.grid.UpdateRowActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter;
import com.almis.awe.scheduler.util.ActivityViewFilters;
import com.almis.awe.scheduler.util.CalendarDatesImport;
import com.almis.awe.scheduler.util.DependencyCycles;
import com.almis.awe.scheduler.util.TaskSchedulePreview;
import com.almis.awe.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author pgarcia
 */
@Slf4j
public class SchedulerService extends ServiceConfig {

  private static final String SCHEDULER_PROPERTY_NOT_FOUND_TITLE = "SCHEDULER_PROPERTY_NOT_FOUND_TITLE";
  private static final String SCHEDULER_PROPERTY_NOT_FOUND_MESSAGE = "SCHEDULER_PROPERTY_NOT_FOUND_MESSAGE";
  private static final String SCHEDULER_PARAMETER_INVALID_TITLE = "SCHEDULER_PARAMETER_INVALID_TITLE";
  private static final String SCHEDULER_PARAMETER_INVALID_MESSAGE = "SCHEDULER_PARAMETER_INVALID_MESSAGE";
  private static final String SCHEDULER_PROPERTY_KEY_CHANGED_MESSAGE = "SCHEDULER_PROPERTY_KEY_CHANGED_MESSAGE";
  private static final String TASK_LAUNCH_NOTE = "SCREEN_TEXT_TASK_LAUNCH_NOTE";

  private static final Duration UPCOMING_WINDOW = Duration.ofHours(24);

  private static final String TASK_DEPENDENCY_CYCLE_TITLE = "SCHEDULER_TASK_DEPENDENCY_CYCLE_TITLE";
  private static final String TASK_DEPENDENCY_CYCLE_MESSAGE = "SCHEDULER_TASK_DEPENDENCY_CYCLE_MESSAGE";

  // Locales
  private static final String ERROR_MESSAGE_SCHEDULER_PAUSE_TASK = "ERROR_MESSAGE_SCHEDULER_PAUSE_TASK";
  private static final String ERROR_TITLE_SCHEDULER_PAUSE_TASK = "ERROR_TITLE_SCHEDULER_PAUSE_TASK";
  private static final String ERROR_MESSAGE_SCHEDULER_RESUME_TASK = "ERROR_MESSAGE_SCHEDULER_RESUME_TASK";
  private static final String ERROR_TITLE_SCHEDULER_RESUME_TASK = "ERROR_TITLE_SCHEDULER_RESUME_TASK";
  private static final String STOP_UNAVAILABLE_TITLE = "SCHEDULER_STOP_UNAVAILABLE_TITLE";
  private static final String STOP_UNAVAILABLE_MESSAGE = "SCHEDULER_STOP_UNAVAILABLE_MESSAGE";
  // Autowired services
  private final TaskDAO taskDAO;
  private final SchedulerDAO schedulerDAO;
  private final CalendarDAO calendarDAO;
  private final SchedulerExecutionCompletionService completionService;
  private final boolean isRemoteEnabled;
  private final boolean isSchedulerInstance;

  /**
   * Constructor
   */
  public SchedulerService(TaskDAO taskDAO, SchedulerDAO schedulerDAO, CalendarDAO calendarDAO,
                          SchedulerExecutionCompletionService completionService, boolean isRemoteEnabled,
                          boolean isSchedulerInstance) {
    this.taskDAO = taskDAO;
    this.schedulerDAO = schedulerDAO;
    this.calendarDAO = calendarDAO;
    this.completionService = completionService;
    this.isRemoteEnabled = isRemoteEnabled;
    this.isSchedulerInstance = isSchedulerInstance;
  }

  /**
   * Start the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData startOnInit() throws AWException {
    if (!isRemoteEnabled || isSchedulerInstance) {
      return schedulerDAO.start();
    }
    return new ServiceData();
  }

  /**
   * Start the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData start() throws AWException {
    return schedulerDAO.start();
  }

  /**
   * Start the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData startNoQuartz() throws AWException {
    return schedulerDAO.startNoQuartz();
  }

  /**
   * Stop the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error stopping scheduler
   */
  public ServiceData stop() throws AWException {
    return schedulerDAO.stop();
  }

  /**
   * Scheduler's emergency reboot method
   * Requires QUARTZ
   *
   * @throws AWException Error restarting scheduler
   */
  public ServiceData restart() throws AWException {
    return schedulerDAO.restart();
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   * Requires QUARTZ
   *
   * @return Service data
   */
  public ServiceData clearAndStop() throws AWException {
    return schedulerDAO.clearAndStop();
  }

  /**
   * Get currently executing jobs from the scheduler instance
   * Requires QUARTZ
   *
   * @return Service data
   * @throws AWException Error retrieving currently executing jobs
   */
  public ServiceData currentlyExecutingJobs() throws AWException {
    return schedulerDAO.getCurrentlyExecutingJobs();
  }


  /**
   * Get configured
   * Requires QUARTZ
   *
   * @return Configured jobs
   * @throws AWException Error retrieving configured jobs
   */
  public ServiceData getConfiguredJobs() throws AWException {
    return schedulerDAO.getConfiguredJobs();
  }

  /**
   * Returns information about the configured scheduler
   * Requires QUARTZ
   *
   * @return Scheduler metadata
   * @throws AWException Error retrieving scheduler metadata
   */
  public ServiceData getSchedulerMetadata() throws AWException {
    return schedulerDAO.getSchedulerMetadata();
  }

  /**
   * Retrieve the task list
   * Requires QUARTZ
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList(TaskListCriteria taskListCriteria) throws AWException {
    return taskDAO.getTaskList(taskListCriteria);
  }

  /**
   * Retrieve the task progress status
   * Requires LOG FILES
   *
   * @return ServiceData
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    return taskDAO.getTaskExecutionList(taskId);
  }

  /**
   * Retrieve a page of the executions of all tasks (activity screen)
   *
   * @param criteria Filters and page
   * @return Execution list page
   * @throws AWException Error retrieving the executions
   */
  public ServiceData getExecutionList(ExecutionListCriteria criteria) throws AWException {
    return taskDAO.getExecutionList(criteria);
  }

  /**
   * Switch the view of the activity screen: give the filters of the toolbar that is left to the toolbar of the view
   * that is shown (see {@link ActivityViewFilters})
   *
   * @param view   View that is shown: exe or run
   * @param search Search by task name
   * @param state  State quick filter
   * @param origin Origin quick filter
   * @param window Preset of the start quick filter
   * @return Select actions of the toolbar filters of the view
   */
  public ServiceData switchActivityView(String view, String search, String state, String origin, String window) {
    return ActivityViewFilters.copy(view, search, state, origin, window);
  }

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run (activity screen)
   *
   * @param criteria Filters and page of the top-level rows
   * @return Page of runs with their executions
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the executions cannot be read
   */
  public ServiceData getRunList(ExecutionListCriteria criteria) throws AWException {
    return taskDAO.getRunList(criteria);
  }

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts
   *
   * @param criteria Filters of the execution list (search, origin and start)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the executions cannot be counted
   */
  public ServiceData getExecutionStateCounts(ExecutionListCriteria criteria) throws AWException {
    return taskDAO.getExecutionStateCounts(criteria);
  }

  /**
   * Retrieve a page of the task catalog (tasks screen)
   * Requires QUARTZ (next fire times)
   *
   * @param criteria Filters, sort and page
   * @return Page of tasks
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getTaskCatalog(TaskCatalogCriteria criteria) throws AWException {
    return taskDAO.getTaskCatalog(criteria);
  }

  /**
   * Retrieve the options of the active filter of the task catalog with their counts
   *
   * @param criteria Filters of the catalog (search, launch and last status)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getTaskCatalogActiveCounts(TaskCatalogCriteria criteria) throws AWException {
    return taskDAO.getTaskCatalogActiveCounts(criteria);
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
    return taskDAO.getTaskPage(taskId);
  }

  /**
   * Retrieve the one-level flow graph of the task screen (the getFlowGraph rows of the task), where the tasks of the
   * level tell when they last ran and, the scheduled ones, their next execution
   *
   * @param taskId Task identifier
   * @return Rows of the graph
   * @throws AWException The graph or the tasks cannot be read
   */
  public ServiceData getTaskFlowGraph(Integer taskId) throws AWException {
    return taskDAO.getTaskFlowGraph(taskId);
  }

  /**
   * Retrieve the summary of a flow run (flow run page)
   *
   * @param runId Flow run identifier
   * @return One row, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunSummary(String runId) throws AWException {
    return taskDAO.getFlowRunSummary(runId);
  }

  /**
   * Retrieve the graph of a flow run (flow run page): one row per task of the run and parent
   *
   * @param runId Flow run identifier
   * @return Rows of the graph, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunGraph(String runId) throws AWException {
    return taskDAO.getFlowRunGraph(runId);
  }

  /**
   * Retrieve the tabs of the configuration screen: the servers and the calendars with how many there are, and the engine
   * with its state
   *
   * @param engineStatus Locale key of the state of the engine, null when the engine cannot be read
   * @return One row per tab: value, label and icon
   * @throws AWException The servers or the calendars cannot be read
   */
  public ServiceData getConfigurationTabs(String engineStatus) throws AWException {
    return taskDAO.getConfigurationTabs(engineStatus);
  }

  /**
   * Retrieve the servers of the configuration screen with their connection and the tasks that use them, never their
   * secrets
   *
   * @param search Text contained in the name or in the host; every server when blank
   * @param active 1 active servers, 0 inactive servers; every server when blank
   * @return Servers sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the servers cannot be read
   */
  public ServiceData getConfigurationServers(String search, String active) throws AWException {
    return taskDAO.getConfigurationServers(search, active);
  }

  /**
   * Refuse to delete a server that a task uses (it runs on it or checks its files in it): checked before deleting, so
   * the refusal changes nothing
   *
   * @param serverIds Servers to delete
   * @throws AWException A server is used by a task (the message names the tasks), or the tasks cannot be read
   */
  public void validateServersNotInUse(List<?> serverIds) throws AWException {
    taskDAO.validateServersNotInUse(serverIds);
  }

  /**
   * Server for the side panel of the configuration screen: its connection and whether each secret is saved, never the
   * secrets, as the select actions that fill the panel; a new server (an active SSH server on port 22) without one
   *
   * @param serverId Server, null for a new server
   * @return One row and one select action per field of the panel
   * @throws AWException The server no longer exists or cannot be read
   */
  public ServiceData loadConfigurationServer(Integer serverId) throws AWException {
    return taskDAO.loadConfigurationServer(serverId);
  }

  /**
   * Calendar for the side panel of the configuration screen: its name, description, active state and how many dates it
   * has, as the select actions that fill the panel; a new calendar (active, no dates) without one
   *
   * @param calendarId Calendar, null for a new calendar
   * @return One select action per criterion of the panel
   * @throws AWException The calendar no longer exists or cannot be read
   */
  public ServiceData loadConfigurationCalendar(Integer calendarId) throws AWException {
    return taskDAO.loadConfigurationCalendar(calendarId);
  }

  /**
   * Dates of the calendar side panel of the configuration screen, in order, each one with its weekday
   *
   * @param calendarId Calendar, null for a new calendar (no dates)
   * @return One row per date: IdeDat, Dat, Nam and DatWek
   * @throws AWException The dates cannot be read
   */
  public ServiceData getConfigurationCalendarPanelDates(Integer calendarId) throws AWException {
    return taskDAO.getConfigurationCalendarPanelDates(calendarId);
  }

  /**
   * «En uso por» of the server side panel of the configuration screen: the tasks that use the server, sorted by name,
   * with a link to each task page, when it is launched and its last status
   *
   * @param serverId Server, null for a new server (no task)
   * @return One row per task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getConfigurationServerTasks(Integer serverId) throws AWException {
    return taskDAO.getConfigurationServerTasks(serverId);
  }

  /**
   * Choice cards of the connection type of the server side panel of the configuration screen (SSH, FTP, folder)
   *
   * @return One row per type: value, label, icon and description
   */
  public ServiceData getConfigurationServerTypes() {
    return taskDAO.getConfigurationServerTypes();
  }

  /**
   * Retrieve the options of the active filter of the servers of the configuration screen with their counts
   *
   * @param search Text contained in the name or in the host
   * @return One row per option: value, label, count and tone
   * @throws AWException The servers cannot be read
   */
  public ServiceData getConfigurationServerActiveCounts(String search) throws AWException {
    return taskDAO.getConfigurationServerActiveCounts(search);
  }

  /**
   * Retrieve the calendars of the configuration screen with their dates, their next date and the tasks that use them
   *
   * @param search Text contained in the name; every calendar when blank
   * @param active 1 active calendars, 0 inactive calendars; every calendar when blank
   * @return Calendars sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the calendars cannot be
   *                     read
   */
  public ServiceData getConfigurationCalendars(String search, String active) throws AWException {
    return taskDAO.getConfigurationCalendars(search, active);
  }

  /**
   * Retrieve the options of the active filter of the calendars of the configuration screen with their counts
   *
   * @param search Text contained in the name
   * @return One row per option: value, label, count and tone
   * @throws AWException The calendars cannot be read
   */
  public ServiceData getConfigurationCalendarActiveCounts(String search) throws AWException {
    return taskDAO.getConfigurationCalendarActiveCounts(search);
  }

  /**
   * Before deleting a calendar of the configuration screen: the tasks that use it, each one with its «When» summary and
   * the next days (at most 3) it would run on without it
   *
   * @param calendarId Calendar to delete
   * @return One row per task, sorted by name
   * @throws AWException The tasks or the calendars cannot be read
   */
  public ServiceData getConfigurationCalendarDeleteCheck(Integer calendarId) throws AWException {
    return taskDAO.getConfigurationCalendarDeleteCheck(calendarId);
  }

  /**
   * «Motor» tab of the configuration screen: the card of the engine (status, data, schedules, threads, the execution
   * waiting for a confirmation). The engine and its schedules are read apart: when the scheduler cannot be read the card
   * says so, and when only its schedules cannot be read the card still tells the engine (logged as warnings)
   * Requires QUARTZ
   *
   * @return One row
   * @throws AWException The executions waiting for a confirmation or the tasks cannot be read
   */
  public ServiceData getConfigurationEngine() throws AWException {
    SchedulerEnginePresenter.Engine engine;
    try {
      engine = schedulerDAO.getEngine();
    } catch (AWException | RuntimeException exc) {
      log.warn("[SCHEDULER] The engine could not be read for the configuration screen", exc);
      return getConfigurationEngineUnavailable();
    }
    return taskDAO.getConfigurationEngine(engine, readEngineTriggers());
  }

  /**
   * «Motor» tab of the configuration screen: the card of an engine that cannot be read (also a remote one that cannot
   * be reached): it says so
   *
   * @return One row
   * @throws AWException The executions waiting for a confirmation cannot be read
   */
  public ServiceData getConfigurationEngineUnavailable() throws AWException {
    return taskDAO.getConfigurationEngine(null, List.of());
  }

  /**
   * Schedules of the engine, none when they cannot be read (logged as a warning): the screen shows an empty list and
   * zero counts instead of an error
   */
  private List<SchedulerEnginePresenter.EngineTrigger> readEngineTriggers() {
    try {
      return schedulerDAO.getEngineTriggers();
    } catch (AWException | RuntimeException exc) {
      log.warn("[SCHEDULER] The schedules of the engine could not be read for the configuration screen", exc);
      return List.of();
    }
  }

  /**
   * «Motor» tab of the configuration screen: the schedules of the engine, filtered by their state
   * Requires QUARTZ
   *
   * @param filter paused or error; every schedule when blank
   * @return One row per schedule, the next one to fire first; none when the engine cannot be read
   * @throws AWException The filter is not valid (refused before the engine is read), or the tasks cannot be read
   */
  public ServiceData getConfigurationEngineTriggers(String filter) throws AWException {
    taskDAO.validateConfigurationEngineFilter(filter);
    return taskDAO.getConfigurationEngineTriggers(readEngineTriggers(), filter);
  }

  /**
   * «Motor» tab of the configuration screen: the options of the state filter of the schedules with their counts
   * Requires QUARTZ
   *
   * @return One row per option: value, label, count and tone; zero counts when the engine cannot be read
   */
  public ServiceData getConfigurationEngineTriggerCounts() {
    return taskDAO.getConfigurationEngineTriggerCounts(readEngineTriggers());
  }

  /**
   * «Motor» tab of the configuration screen: the options of the state filter of an engine that cannot be read (also a
   * remote one that cannot be reached), with zero counts
   *
   * @return One row per option: value, label, count and tone
   */
  public ServiceData getConfigurationEngineEmptyTriggerCounts() {
    return taskDAO.getConfigurationEngineTriggerCounts(List.of());
  }

  /**
   * Validate the state filter of the schedules of the engine
   *
   * @param filter paused or error; every schedule when blank
   * @throws AWException The filter is not valid
   */
  public void validateConfigurationEngineFilter(String filter) throws AWException {
    taskDAO.validateConfigurationEngineFilter(filter);
  }

  /**
   * «Importar…» of the calendar side panel: read the dates of an .ics or a .csv file staged by the uploader and return
   * them for the dates grid without saving them (the staged file is deleted once read)
   *
   * @param file Value of the uploader: the staged file
   * @return The dates (Dat dd/MM/yyyy, Nam) and a message with the lines that cannot be read
   * @throws AWException The file is not a staged file, cannot be read, is too large or is neither .ics nor .csv
   */
  public ServiceData importConfigurationCalendarDates(String file) throws AWException {
    return new CalendarDatesImporter(getBean(FileService.class), this::getLocale).importDates(file);
  }

  /**
   * Day («Día») of a date added or changed in the dates grid of the calendar side panel: the weekday the saved and the
   * imported dates show, as an update of the row being edited; empty when the date is empty or not a date
   *
   * @param date Date of the row (dd/MM/yyyy)
   * @param row  Row of the dates grid being edited, none when no row is being edited or selected
   * @return The update of the day of the row, no action without a row
   */
  public ServiceData getConfigurationCalendarDateWeekday(String date, String row) {
    ServiceData serviceData = new ServiceData();
    if (row == null || row.isBlank()) {
      return serviceData;
    }
    String weekday = CalendarDatesImport.weekdayKeyOf(date).map(this::getLocale).orElse("");
    return serviceData.addClientAction(new UpdateRowActionBuilder(CalendarDatesImporter.DATES_GRID, row,
      Map.<String, Object>of("DatWek", weekday)).setSilent(true).build());
  }

  /**
   * Validate the filters, the sort and the page of the task catalog
   *
   * @param criteria Filters, sort and page
   * @throws AWException A value is not valid (InvalidExecutionFilterException)
   */
  public void validateTaskCatalogCriteria(TaskCatalogCriteria criteria) throws AWException {
    taskDAO.validateTaskCatalogCriteria(criteria);
  }

  /**
   * Validate the filters and the page of the execution list of all tasks
   *
   * @param criteria Filters and page
   * @throws AWException A value is not valid (InvalidExecutionFilterException)
   */
  public void validateExecutionListCriteria(ExecutionListCriteria criteria) throws AWException {
    taskDAO.validateExecutionListCriteria(criteria);
  }

  /**
   * Retrieve the counters of the activity screen: the execution counters (running, waiting, queued and errors in the
   * last 24 hours) and the fire times of the next 24 hours (upcomingNext24h). When Quartz cannot be read the
   * execution counters are still returned and upcomingNext24h is empty (logged as a warning)
   * Requires QUARTZ
   *
   * @return One row with the counters
   * @throws AWException Error retrieving the counters
   */
  public ServiceData getActivityCounters() throws AWException {
    ServiceData counters = taskDAO.getExecutionCounters();
    CellData upcoming;
    try {
      upcoming = new CellData(schedulerDAO.countUpcomingFireTimes(UPCOMING_WINDOW));
    } catch (AWException | RuntimeException exc) {
      // The execution counters come from the database: they are still shown when Quartz cannot be read
      log.warn("[SCHEDULER] The upcoming fire times could not be counted, the counter is left empty", exc);
      upcoming = new CellData();
    }
    counters.getDataList().getRows().get(0).put("upcomingNext24h", upcoming);
    return counters;
  }

  /**
   * Retrieve the executions to purge
   *
   * @return ServiceData
   * @throws AWException Error retrieving executions to purge
   */
  public ServiceData getExecutionsToPurge(Integer taskId, Integer executions) throws AWException {
    return taskDAO.getExecutionsToPurge(taskId, executions);
  }

  /**
   * Execute the selected task now
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @param user   Launch user
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user) throws AWException {
    // Backward-compatible signature bound by the AWE JavaConnector from Services.xml (LchTsk manual launch, 2 params).
    // Do not remove: the connector resolves the target method by EXACT parameter types.
    return executeTaskNow(taskId, user, null);
  }

  /**
   * Execute the selected task now, applying the operator supplied values for its parameters (any source on a manual
   * launch). A changed value that is not a value of its parameter type rejects the launch
   * Requires QUARTZ
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values (parameter name -&gt; value)
   * @return ServiceData
   * @throws AWException A value is not of its type, or error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user, Map<String, String> variables) throws AWException {
    // A value that is not of its type or a changed property key never reaches the execution, whoever calls the launch
    // (screen, API, remote)
    if (variables != null && !variables.isEmpty()) {
      Optional<ServiceData> rejected = rejectedValues(taskId, variables);
      if (rejected.isPresent()) {
        throw new AWException(rejected.get().getTitle(), rejected.get().getMessage());
      }
    }
    return taskDAO.executeTaskNow(taskId, user, variables);
  }

  /**
   * Operator supplied values a launch refuses: a changed value that is not of its parameter type, else a property
   * parameter whose key changes
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value)
   * @return The ERROR answer that names the parameters, empty when every value can be used
   * @throws AWException Error loading the task
   */
  private Optional<ServiceData> rejectedValues(Integer taskId, Map<String, String> variables) throws AWException {
    List<String> invalid = taskDAO.findInvalidParameterValues(taskId, variables);
    if (!invalid.isEmpty()) {
      return Optional.of(rejection(getLocale(SCHEDULER_PARAMETER_INVALID_MESSAGE, String.join(", ", invalid))));
    }
    List<String> changedKeys = taskDAO.findChangedPropertyKeys(taskId, variables);
    if (!changedKeys.isEmpty()) {
      return Optional.of(rejection(getLocale(SCHEDULER_PROPERTY_KEY_CHANGED_MESSAGE, String.join(", ", changedKeys))));
    }
    return Optional.empty();
  }

  private ServiceData rejection(String message) {
    return new ServiceData()
      .setType(AnswerType.ERROR)
      .setTitle(getLocale(SCHEDULER_PARAMETER_INVALID_TITLE))
      .setMessage(message);
  }

  /**
   * Note of the manual launch dialog: how the launch is recorded (origin Manual, launched by the user)
   *
   * @param user User of the session, who launches the task
   * @return One row with the note as description (the field of the card that shows it)
   */
  public ServiceData getTaskLaunchNote(String user) {
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "description", getLocale(TASK_LAUNCH_NOTE, user));
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Check the operator supplied values before a manual launch: every changed value is a value of its parameter type and
   * no property key changes, then every PROPERTY parameter of the task resolves against the configuration of THIS instance, which is the one
   * that resolves them at execution time.
   *
   * <p>Returns an ERROR ServiceData naming the offending parameters or keys rather than throwing, so the remote
   * endpoint can hand the outcome back to the caller over HTTP.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return ServiceData, of type ERROR when a value is not of its type or a property key does not resolve
   * @throws AWException Error loading the task
   */
  public ServiceData validateTaskParameters(Integer taskId, Map<String, String> variables) throws AWException {
    Optional<ServiceData> rejected = rejectedValues(taskId, variables);
    if (rejected.isPresent()) {
      return rejected.get();
    }

    List<String> unresolved = taskDAO.findUnresolvedProperties(taskId, variables);
    if (unresolved.isEmpty()) {
      return new ServiceData();
    }

    return new ServiceData()
      .setType(AnswerType.ERROR)
      .setTitle(getLocale(SCHEDULER_PROPERTY_NOT_FOUND_TITLE))
      .setMessage(getLocale(SCHEDULER_PROPERTY_NOT_FOUND_MESSAGE, String.join(", ", unresolved)));
  }

  /**
   * Reject the dependencies a task is saved with when they close a cycle: a task that depends on itself, directly or
   * through other tasks, would never finish its flow
   *
   * @param taskId   Task identifier
   * @param children Tasks launched when the task ends
   * @throws AWException The dependencies close a cycle
   */
  public void validateTaskDependencies(Integer taskId, List<?> children) throws AWException {
    if (DependencyCycles.closesCycle(taskId, toTaskIds(children), taskDAO.getChildrenByTask())) {
      throw dependencyCycle();
    }
  }

  /**
   * Reject the parents a task is saved with in the task editor when they close a cycle: a parent that is the task
   * itself or already runs after it, directly or through other tasks. A new task (without identifier) has nothing
   * running after it yet
   *
   * @param taskId  Task identifier, null for a new task
   * @param parents Tasks the task runs after
   * @throws AWException The parents close a cycle
   */
  public void validateTaskParents(Integer taskId, List<?> parents) throws AWException {
    if (DependencyCycles.closesCycleFromParents(taskId, toTaskIds(parents), taskDAO.getChildrenByTask())) {
      throw dependencyCycle();
    }
  }

  /**
   * Save the parents of a task from the task editor: the dependency rows where the task is the child are replaced by
   * the chosen parents, each once (DpnOrd is left empty, as the old maintains leave it); the rows of the other tasks are
   * kept. The parents are validated first, so a cycle changes nothing
   *
   * @param taskId  Task identifier
   * @param parents Tasks the task runs after
   * @throws AWException The parents close a cycle or cannot be saved
   */
  public void saveTaskParents(Integer taskId, List<?> parents) throws AWException {
    validateTaskParents(taskId, parents);
    taskDAO.saveTaskParents(taskId, toTaskIds(parents).stream().distinct().toList());
  }

  /**
   * Task identifiers sent by the client, as numbers or as text, without the empty ones
   */
  private static List<Integer> toTaskIds(List<?> ids) {
    return ids == null ? List.of() : ids.stream()
      .filter(Objects::nonNull)
      .map(String::valueOf)
      .filter(id -> !id.isBlank())
      .map(Integer::valueOf)
      .toList();
  }

  private AWException dependencyCycle() {
    return new AWException(getLocale(TASK_DEPENDENCY_CYCLE_TITLE), getLocale(TASK_DEPENDENCY_CYCLE_MESSAGE));
  }

  /**
   * Retrieve the tasks a task can run after, for the parents picker of the task editor
   *
   * @param taskId     Task edited, null for a new task
   * @param copyTaskId Task duplicated by a new task, whose parents are the chosen ones
   * @return One row per task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getTaskEditorParents(Integer taskId, Integer copyTaskId) throws AWException {
    return taskDAO.getTaskEditorParents(taskId, copyTaskId);
  }

  /**
   * Retrieve the data of a task to duplicate it in the task editor
   *
   * @param taskId Task to duplicate
   * @return One row, none when the task does not exist
   * @throws AWException The task cannot be read
   */
  public ServiceData getTaskEditorCopy(Integer taskId) throws AWException {
    return taskDAO.getTaskEditorCopy(taskId);
  }

  /**
   * Retrieve the data of the task editor screen for a mode: the task with its parents (edit), its copy (copy) or only
   * the fields of a new task, with its «When» and its header
   *
   * @param mode   Mode of the editor (new, edit or copy)
   * @param taskId Task of the screen that opened the editor
   * @return One row
   * @throws AWException The task cannot be read
   */
  public ServiceData getTaskEditor(String mode, Integer taskId) throws AWException {
    return taskDAO.getTaskEditor(mode, taskId);
  }

  /**
   * Options of a choice of the task editor drawn as cards (when, execution), with their icon and description
   *
   * @param group Choice: when or execution
   * @return One row per option
   */
  public ServiceData getTaskEditorOptions(String group) {
    return taskDAO.getTaskEditorOptions(group);
  }

  /**
   * Live preview of the «When» of a scheduled task in the task editor: the repetition in a sentence with the name of
   * its calendar and its range, its next runs and the first holiday of its calendar it skips
   *
   * @param schedule Stored fields of the repetition (with the preset applied)
   * @param preset   Preset of the «Repetition» of the screen
   * @param next     Next runs, as computed by {@link #computeNextFireTimes(Integer, Schedule)}
   * @param complete The next runs could be computed
   * @return Row of the preview card
   */
  public ServiceData getTaskEditorSchedule(Schedule schedule, String preset, List<Map<String, CellData>> next,
                                           boolean complete) {
    String calendar = null;
    List<LocalDate> holidays = List.of();
    if (schedule.getCalendarId() != null) {
      try {
        com.almis.awe.scheduler.bean.calendar.Calendar found = calendarDAO.getCalendar(schedule.getCalendarId());
        calendar = found.getName();
        holidays = found.getExcludedDates().stream()
          .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).toList();
      } catch (Exception exc) {
        log.debug("[SCHEDULER] Calendar {} of the task editor preview not found", schedule.getCalendarId(), exc);
      }
    }
    return taskDAO.getTaskEditorSchedule(new TaskSchedulePreview.Input(schedule, preset, calendar, holidays, next,
      complete));
  }

  /**
   * Retrieve the options of the parents picker of the task editor (groups, disabled options and their line)
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return One row per task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getTaskEditorParentOptions(String mode, Integer taskId) throws AWException {
    return taskDAO.getTaskEditorParentOptions(mode, taskId);
  }

  /**
   * Retrieve the parameters of the task of the task editor: none for a new task, without identifiers for a copy
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return The parameters
   * @throws AWException The parameters cannot be read
   */
  public ServiceData getTaskEditorParameters(String mode, Integer taskId) throws AWException {
    return taskDAO.getTaskEditorParameters(mode, taskId);
  }

  /**
   * Retrieve one side of the one-level graph of the task editor: the chosen parents or the tasks it launches
   *
   * @param mode    Mode of the editor
   * @param taskId  Task of the screen that opened the editor
   * @param parents Parents chosen in the editor
   * @param side    Side of the graph: parents or children
   * @return Rows of the graph
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getTaskEditorGraph(String mode, Integer taskId, List<?> parents, String side) throws AWException {
    return taskDAO.getTaskEditorGraph(mode, taskId, toTaskIds(parents), side);
  }

  /**
   * Answer of a save of the task editor: its message and the saved task as the task of the screen, so the task screen
   * opened next shows it
   *
   * @param taskId Task saved
   * @return Message and select action
   */
  public ServiceData getTaskEditorSaved(Integer taskId) {
    return taskDAO.getTaskEditorSaved(taskId);
  }

  /**
   * Pause the selected task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error pausing task
   */
  public ServiceData pauseTask(Integer taskId) throws AWException {
    try {
      Task task = taskDAO.getTask(taskId).get();
      return taskDAO.pauseTask(task);
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new AWException(getLocale(ERROR_TITLE_SCHEDULER_PAUSE_TASK), getLocale(ERROR_MESSAGE_SCHEDULER_PAUSE_TASK), exc);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_SCHEDULER_PAUSE_TASK), getLocale(ERROR_MESSAGE_SCHEDULER_PAUSE_TASK), exc);
    }
  }

  /**
   * Stop the running jobs and the deferred executions of the selected task (a task without any does nothing)
   *
   * @param taskId Task identifier
   * @return ServiceData, a warning when a running job could not be stopped
   * @throws AWException Error stopping the executions
   */
  public ServiceData stopTask(Integer taskId) throws AWException {
    completionService.stopTask(taskId);
    boolean stopped;
    try {
      stopped = schedulerDAO.stopRunningJobs(taskId);
    } catch (SchedulerException exc) {
      throw new AWException("Error stopping the running jobs of task " + taskId, exc);
    }
    return stopped ? new ServiceData() : new ServiceData()
      .setType(AnswerType.WARNING)
      .setTitle(getLocale(STOP_UNAVAILABLE_TITLE))
      .setMessage(getLocale(STOP_UNAVAILABLE_MESSAGE));
  }

  /**
   * Resume the selected task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error resuming task
   */
  public ServiceData resumeTask(Integer taskId) throws AWException {
    try {
      Task task = taskDAO.getTask(taskId).get();
      return taskDAO.resumeTask(task);
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new AWException(getLocale(ERROR_TITLE_SCHEDULER_RESUME_TASK), getLocale(ERROR_MESSAGE_SCHEDULER_RESUME_TASK), exc);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_SCHEDULER_RESUME_TASK), getLocale(ERROR_MESSAGE_SCHEDULER_RESUME_TASK), exc);
    }
  }

  /**
   * Insert and schedule a new task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  public ServiceData insertSchedulerTask(Integer taskId) throws AWException {
    return taskDAO.insertTask(taskId);
  }

  /**
   * Update and reschedule a task
   * Requires QUARTZ
   *
   * @param taskId Task identifier list
   * @return ServiceData
   * @throws AWException Error updating task
   */
  public ServiceData updateSchedulerTask(Integer taskId) throws AWException {
    return taskDAO.updateTask(taskId);
  }

  /**
   * Refresh an edited task in the scheduler with its stored launch, without purging its executions, their logs nor
   * the files it has seen
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error refreshing the task
   */
  public ServiceData refreshSchedulerTask(Integer taskId) throws AWException {
    taskDAO.refreshTask(taskId);
    return new ServiceData();
  }

  /**
   * Delete a task from scheduler
   * Requires QUARTZ
   *
   * @param ideTsk Task identifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteSchedulerTask(List<Integer> ideTsk) throws AWException {
    return taskDAO.deleteTask(ideTsk);
  }

  /**
   * Delete a single task from scheduler
   * Requires QUARTZ
   *
   * @param ideTsk Task identifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteSchedulerTask(Integer ideTsk) throws AWException {
    return taskDAO.deleteTask(ideTsk);
  }

  /**
   * Update execution time
   *
   * @param taskId Task identifier list
   * @return ServiceData
   * @throws AWException Error updating execution time
   */
  public ServiceData updateExecutionTime(Integer taskId, Integer taskExecution) throws AWException {
    return taskDAO.updateExecutionTime(taskId, taskExecution);
  }

  /**
   * Load needed variables from the selected maintain
   *
   * @param maintainStr Maintain identifier
   * @return ServiceData
   * @throws AWException Error loading maintain variables
   */
  public ServiceData loadMaintainVariables(String maintainStr) throws AWException {
    return taskDAO.loadMaintainVariables(maintainStr);
  }

  /**
   * Load execution screen
   *
   * @param path    Execution path
   * @param address Execution address
   * @return Service data
   * @throws AWException Error loading execution screen
   */
  public ServiceData loadExecutionScreen(String path, JsonNode address) throws AWException {
    return taskDAO.loadExecutionScreen(path, (ObjectNode) address);
  }

  /**
   * Load execution screen of the execution of the button address, else of the button value, else of the selected row
   * (a client that sends no button address nor value, as the React cell buttons)
   *
   * @param path            Execution log locator of the button value
   * @param address         Button address
   * @param selectedLocator Execution log locator of the selected row
   * @return Service data
   * @throws AWException Error loading execution screen
   */
  public ServiceData loadExecutionScreen(String path, JsonNode address, String selectedLocator) throws AWException {
    return taskDAO.loadExecutionScreen(path, address instanceof ObjectNode node ? node : null, selectedLocator);
  }

  /**
   * Reload execution screen
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service data
   * @throws AWException Error reloading execution screen
   */
  public ServiceData reloadExecutionScreen(Integer taskId, Integer executionId) throws AWException {
    return taskDAO.reloadExecutionScreen(taskId, executionId);
  }

  /**
   * Progress of an execution as the fields of the card of the activity side panel
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service data with one row, none without execution
   * @throws AWException Error reading the execution
   */
  public ServiceData getExecutionProgress(Integer taskId, Integer executionId) throws AWException {
    return taskDAO.getExecutionProgress(taskId, executionId);
  }

  /**
   * Launch parameters of an execution, the rows of the parameters grid of the activity side panel
   *
   * @param runId Flow run identifier of the execution
   * @return Service data with one row per parameter, none without run
   * @throws AWException Error reading the run parameters
   */
  public ServiceData getExecutionParameters(String runId) throws AWException {
    return taskDAO.getExecutionParameters(runId);
  }

  /**
   * Retrieve the execution log window from a line offset (get-execution-log action), in both
   * store modes. Parameterless: services invoked through an Actions.xml &lt;call service&gt; receive
   * no parameter map, so the id/offset values are read directly from the request instead of
   * declared as service-parameters.
   *
   * @return Service data
   * @throws AWException Error reading the execution log
   */
  public ServiceData getExecutionLog() throws AWException {
    Integer taskId = readRequestInteger("execution-log-task-id");
    Integer executionId = readRequestInteger("execution-log-execution-id");
    if (taskId == null || executionId == null) {
      return new ServiceData();
    }

    Integer offset = readRequestInteger("offset");
    String version = readRequestString("version");
    return taskDAO.getExecutionLog(taskId, executionId, offset == null ? 0 : offset, version);
  }

  /**
   * Read a request parameter as an integer.
   *
   * @param name Parameter name
   * @return Parameter value, or {@code null} when absent
   */
  private Integer readRequestInteger(String name) {
    JsonNode parameter = getRequest().getParameter(name);
    return parameter == null || parameter.isNull() ? null : parameter.asInt(0);
  }

  /**
   * Read a request parameter as a string.
   *
   * @param name Parameter name
   * @return Parameter value, or {@code null} when absent
   */
  private String readRequestString(String name) {
    JsonNode parameter = getRequest().getParameter(name);
    return parameter == null || parameter.isNull() ? null : parameter.asString("");
  }

  /**
   * Purge execution logs
   * Requires LOG FILES
   *
   * @param taskId     Task identifier
   * @param executions Number of executions to purge
   * @return Service data
   * @throws AWException Error purging execution logs
   */
  public ServiceData purgeExecutionLogs(Integer taskId, Integer executions) throws AWException {
    return taskDAO.purgeExecutionLogFiles(taskId, executions);
  }

  /**
   * Purge execution logs on application start
   * Requires LOG FILES
   *
   * @return Service data
   * @throws AWException Error purging executions
   */
  public ServiceData purgeExecutionsAtStart() throws AWException {
    if (!isRemoteEnabled || isSchedulerInstance) {
      return taskDAO.purgeExecutionsAtStart();
    }

    return new ServiceData();
  }

  /**
   * Pause the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deactivating calendars
   */
  public ServiceData deactivateCalendar(List<Integer> calendarIdList) throws AWException {
    return calendarDAO.deactivateCalendars(calendarIdList);
  }

  /**
   * Resume the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error activating calendars
   */
  public ServiceData activateCalendar(List<Integer> calendarIdList) throws AWException {
    return calendarDAO.activateCalendars(calendarIdList);
  }

  /**
   * Check if the scheduler contains the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error checking calendars existence
   */
  public ServiceData checkCalendarExist(List<Integer> calendarIdList) throws AWException {
    return calendarDAO.checkTriggersContainsCalendar(calendarIdList.toArray(new Integer[0]));
  }

  /**
   * Insert and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarIde Calendar identifier
   * @return ServiceData
   * @throws AWException Error inserting scheduler calendar
   */
  public ServiceData insertSchedulerCalendar(Integer calendarIde) throws AWException {
    return calendarDAO.insertSchedulerCalendar(calendarIde, false, false);
  }

  /**
   * Insert and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarId     Calendar identifier
   * @param replace        Replace calendar
   * @param updateTriggers Update task triggers
   * @return ServiceData
   * @throws AWException Error inserting scheduler calendar
   */
  public ServiceData insertSchedulerCalendar(Integer calendarId, boolean replace, boolean updateTriggers) throws AWException {
    return calendarDAO.insertSchedulerCalendar(calendarId, replace, updateTriggers);
  }

  /**
   * Update and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error updating scheduler calendar
   */
  public ServiceData updateSchedulerCalendar(Integer calendarId) throws AWException {
    return calendarDAO.updateSchedulerCalendar(calendarId);
  }

  /**
   * Delete selected calendars
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deleting scheduler calendars
   */
  public ServiceData deleteSchedulerCalendar(List<Integer> calendarIdList) throws AWException {
    return calendarDAO.deleteSchedulerCalendar(calendarIdList);
  }

  /**
   * Delete a calendar from scheduler
   * Requires QUARTZ
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error deleting scheduler calendar
   */
  public ServiceData deleteSchedulerCalendar(Integer calendarId) throws AWException {
    return calendarDAO.deleteSchedulerCalendar(Collections.singletonList(calendarId));
  }

  /**
   * Retrieves next 100 years
   *
   * @return ServiceData
   */
  public ServiceData yearSelectService() {
    return calendarDAO.yearSelectService();
  }

  /**
   * Compute next fire times
   * Requires QUARTZ
   *
   * @param times    # of fire times to compute
   * @param schedule Schedule
   * @return Service data
   * @throws AWException Error computing next fire times
   */
  public ServiceData computeNextFireTimes(Integer times, Schedule schedule) throws AWException {
    return calendarDAO.computeNextFireTimes(times, schedule);
  }
}
