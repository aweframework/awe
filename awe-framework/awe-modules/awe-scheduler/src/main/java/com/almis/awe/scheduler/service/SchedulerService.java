package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.Task;
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

  /**
   * Constructor
   */
  public SchedulerService(TaskDAO taskDAO, SchedulerDAO schedulerDAO, CalendarDAO calendarDAO) {
    this.taskDAO = taskDAO;
    this.schedulerDAO = schedulerDAO;
    this.calendarDAO = calendarDAO;
  }

  /**
   * Start the scheduler service
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData start() throws AWException {
    return schedulerDAO.start();
  }

  /**
   * Start the scheduler service
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData startNoQuartz() throws AWException {
    return schedulerDAO.startNoQuartz();
  }

  /**
   * Stop the scheduler service
   *
   * @throws AWException Error stopping scheduler
   */
  public ServiceData stop() throws AWException {
    return schedulerDAO.stop();
  }

  /**
   * Scheduler's emergency reboot method
   *
   * @throws AWException Error restarting scheduler
   */
  public ServiceData restart() throws AWException {
    return schedulerDAO.restart();
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   *
   * @return Service data
   */
  public ServiceData clearAndStop() throws AWException {
    return schedulerDAO.clearAndStop();
  }

  /**
   * Get currently executing jobs from the scheduler instance
   *
   * @return Service data
   * @throws AWException Error retrieving currently executing jobs
   */
  public ServiceData currentlyExecutingJobs() throws AWException {
    return schedulerDAO.getCurrentlyExecutingJobs();
  }


  /**
   * Get configured
   *
   * @return Configured jobs
   * @throws AWException Error retrieving configured jobs
   */
  public ServiceData getConfiguredJobs() throws AWException {
    return schedulerDAO.getConfiguredJobs();
  }

  /**
   * Returns information about the configured scheduler
   *
   * @return Scheduler metadata
   * @throws AWException Error retrieving scheduler metadata
   */
  public ServiceData getSchedulerMetadata() throws AWException {
    return schedulerDAO.getSchedulerMetadata();
  }

  /**
   * Retrieve the task list
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList() throws AWException {
    return taskDAO.getTaskList();
  }

  /**
   * Retrieve the task progress status
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
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId) throws AWException {
    return taskDAO.executeTaskNow(taskId);
  }

  /**
   * Pause the selected task
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
   * OBJECT
   * Insert and schedule a new task
   *
   * @param taskId          Task identifier
   * @param sendStatus      Status to send list
   * @param sendDestination Destination target list
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  public ServiceData insertSchedulerTask(Integer taskId, List<Integer> sendStatus, List<Integer> sendDestination) throws AWException {
    return taskDAO.insertTask(taskId);
  }

  /**
   * Update and reschedule a task
   *
   * @param taskId          Task identifier
   * @param sendStatus      Status to send list
   * @param sendDestination Destination target list
   * @return ServiceData
   * @throws AWException Error updating task
   */
  public ServiceData updateSchedulerTask(Integer taskId, List<Integer> sendStatus, List<Integer> sendDestination) throws AWException {
    return taskDAO.updateTask(taskId);
  }


  /**
   * Update and reschedule a task
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
   *
   * @return Service data
   * @throws AWException Error purging executions
   */
  public ServiceData purgeExecutionsAtStart() throws AWException {
    return taskDAO.purgeExecutionsAtStart();
  }

  /**
   * Pause the selected calendar
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
   *
   * @param times # of fire times to compute
   * @return Service data
   * @throws AWException Error computing next fire times
   */
  public ServiceData computeNextFireTimes(Integer times) throws AWException {
    return calendarDAO.computeNextFireTimes(times);
  }
}
