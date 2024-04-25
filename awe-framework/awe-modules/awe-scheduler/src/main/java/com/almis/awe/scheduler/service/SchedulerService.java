package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * @author pgarcia
 */
@Slf4j
public class SchedulerService extends ServiceConfig {

  // Locales
  private static final String ERROR_MESSAGE_SCHEDULER_PAUSE_TASK = "ERROR_MESSAGE_SCHEDULER_PAUSE_TASK";
  private static final String ERROR_TITLE_SCHEDULER_PAUSE_TASK = "ERROR_TITLE_SCHEDULER_PAUSE_TASK";
  private static final String ERROR_MESSAGE_SCHEDULER_RESUME_TASK = "ERROR_MESSAGE_SCHEDULER_RESUME_TASK";
  private static final String ERROR_TITLE_SCHEDULER_RESUME_TASK = "ERROR_TITLE_SCHEDULER_RESUME_TASK";
  // Autowired services
  private final TaskDAO taskDAO;
  private final SchedulerDAO schedulerDAO;
  private final CalendarDAO calendarDAO;
  private final boolean isRemoteEnabled;
  private final boolean isSchedulerInstance;

  /**
   * Constructor
   */
  public SchedulerService(TaskDAO taskDAO, SchedulerDAO schedulerDAO, CalendarDAO calendarDAO,
                          boolean isRemoteEnabled, boolean isSchedulerInstance) {
    this.taskDAO = taskDAO;
    this.schedulerDAO = schedulerDAO;
    this.calendarDAO = calendarDAO;
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
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user) throws AWException {
    return taskDAO.executeTaskNow(taskId, user);
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
    return calendarDAO.insertSchedulerCalendar(null, calendarId, replace, updateTriggers);
  }

  /**
   * Insert and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarId     Calendar identifier
   * @param replace        Replace calendar
   * @param updateTriggers Update task triggers
   * @param alias          Calendar alias
   * @return ServiceData
   * @throws AWException Error inserting scheduler calendar
   */
  public ServiceData insertSchedulerCalendar(String alias, Integer calendarId, boolean replace, boolean updateTriggers) throws AWException {
    return calendarDAO.insertSchedulerCalendar(alias, calendarId, replace, updateTriggers);
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
