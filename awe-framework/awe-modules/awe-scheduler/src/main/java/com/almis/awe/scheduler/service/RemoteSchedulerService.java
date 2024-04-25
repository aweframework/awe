package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.almis.awe.scheduler.constant.CronConstants.*;

/**
 * @author pgarcia
 */
@Slf4j
public class RemoteSchedulerService extends ServiceConfig {

  // Locales
  private final SchedulerService schedulerService;
  private final RemoteScheduler remoteScheduler;
  private final ObjectMapper mapper;
  private final boolean remote;

  /**
   * Constructor
   */
  public RemoteSchedulerService(SchedulerService schedulerService, RemoteScheduler remoteScheduler, ObjectMapper mapper,
                                boolean remote) {
    this.schedulerService = schedulerService;
    this.remoteScheduler = remoteScheduler;
    this.mapper = mapper;
    this.remote = remote;
  }

  /**
   * Retrieve the task list from service
   * Requires QUARTZ
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList(TaskListCriteria taskListCriteria) throws AWException {
    return remote ? remoteScheduler.getTaskList(taskListCriteria) : schedulerService.getTaskList(taskListCriteria);
  }

  /**
   * Compute next fire times
   * Requires QUARTZ
   *
   * @param times # of fire times to compute
   * @return Service data
   * @throws AWException Error computing next fire times
   */
  public ServiceData computeNextFireTimes(Integer times) throws AWException {
    return remote ? remoteScheduler.getNextFireTimes(times, getSchedule()) : schedulerService.computeNextFireTimes(times, getSchedule());
  }

  /**
   * Retrieve the task progress status
   * Requires LOG FILES
   *
   * @return ServiceData
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    return remote ? remoteScheduler.getTaskExecutionList(taskId) : schedulerService.getTaskExecutionList(taskId);
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
    return remote ? remoteScheduler.pauseTask(taskId) : schedulerService.pauseTask(taskId);
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
    return remote ? remoteScheduler.resumeTask(taskId) : schedulerService.resumeTask(taskId);
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
    return remote ? remoteScheduler.executeTaskNow(taskId, user) : schedulerService.executeTaskNow(taskId, user);
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
    return remote ? remoteScheduler.scheduleTask(taskId) : schedulerService.insertSchedulerTask(taskId);
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
    return remote ? remoteScheduler.rescheduleTask(taskId) : schedulerService.updateSchedulerTask(taskId);
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
    return remote ? remoteScheduler.unscheduleTask(ideTsk) : schedulerService.deleteSchedulerTask(ideTsk);
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
    return remote ? remoteScheduler.purgeExecutions(taskId, executions) : schedulerService.purgeExecutionLogs(taskId, executions);
  }

  /**
   * Get currently executing jobs from the scheduler instance
   * Requires QUARTZ
   *
   * @return Service data
   * @throws AWException Error retrieving currently executing jobs
   */
  public ServiceData currentlyExecutingJobs() throws AWException {
    return remote ? remoteScheduler.getExecutingJobs() : schedulerService.currentlyExecutingJobs();
  }

  /**
   * Get configured
   * Requires QUARTZ
   *
   * @return Configured jobs
   * @throws AWException Error retrieving configured jobs
   */
  public ServiceData getConfiguredJobs() throws AWException {
    return remote ? remoteScheduler.getConfiguredJobs() : schedulerService.getConfiguredJobs();
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
    return remote ? remoteScheduler.insertCalendar(calendarIde) : schedulerService.insertSchedulerCalendar(calendarIde);
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
    return remote ? remoteScheduler.updateCalendar(calendarId) : schedulerService.updateSchedulerCalendar(calendarId);
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
    return remote ? remoteScheduler.deleteCalendars(calendarIdList) : schedulerService.deleteSchedulerCalendar(calendarIdList);
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
    return remote ? remoteScheduler.checkCalendarInTriggers(calendarIdList) : schedulerService.checkCalendarExist(calendarIdList);
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
    return remote ? remoteScheduler.activateCalendar(calendarIdList) : schedulerService.activateCalendar(calendarIdList);
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
    return remote ? remoteScheduler.deactivateCalendar(calendarIdList) : schedulerService.deactivateCalendar(calendarIdList);
  }

  /**
   * Start the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData start() throws AWException {
    return remote ? remoteScheduler.startScheduler() : schedulerService.start();
  }

  /**
   * Stop the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error stopping scheduler
   */
  public ServiceData stop() throws AWException {
    return remote ? remoteScheduler.stopScheduler() : schedulerService.stop();
  }

  /**
   * Scheduler's emergency reboot method
   * Requires QUARTZ
   *
   * @throws AWException Error restarting scheduler
   */
  public ServiceData restart() throws AWException {
    return remote ? remoteScheduler.restartScheduler() : schedulerService.restart();
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   * Requires QUARTZ
   *
   * @return Service data
   */
  public ServiceData clearAndStop() throws AWException {
    return remote ? remoteScheduler.clearAndStopScheduler() : schedulerService.clearAndStop();
  }

  /**
   * Returns information about the configured scheduler
   * Requires QUARTZ
   *
   * @return Scheduler metadata
   * @throws AWException Error retrieving scheduler metadata
   */
  public ServiceData getSchedulerMetadata() throws AWException {
    return remote ? remoteScheduler.getSchedulerMetadata() : schedulerService.getSchedulerMetadata();
  }


  /**
   * get parameters for generating cron from context
   *
   * @return Schedule
   */
  private Schedule getSchedule() {
    return new Schedule()
      .setRepeatType(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_REPEAT_TYPE)))
      .setRepeatNumber(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_REPEAT_NUMBER)))
      .setCalendarId(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_CALENDAR_IDE)))
      .setInitialDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_INITIAL_DATE)))
      .setInitialTime(getRequest().getParameterAsString(CRON_PARAMETER_INITIAL_TIME))
      .setEndDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_END_DATE)))
      .setEndTime(getRequest().getParameterAsString(CRON_PARAMETER_END_TIME))
      .setDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_DATE)))
      .setTime(getRequest().getParameterAsString(CRON_PARAMETER_TIME))
      .setYearList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_YEARS)))
      .setMonthList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_MONTHS)))
      .setWeekList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_WEEKS)))
      .setDayList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_DAYS)))
      .setWeekDayList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_WEEKDAYS)))
      .setHourList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_HOURS)))
      .setMinuteList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_MINUTES)))
      .setSecondList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_SECONDS)));
  }

  /**
   * Read json node as integer
   *
   * @param jsonNode Json node
   * @return Integer value
   */
  private Integer readJsonAsInteger(JsonNode jsonNode) {
    return jsonNode.isNull() ? null : Integer.parseInt(jsonNode.asText());
  }

  /**
   * Read json node as integer list
   *
   * @param jsonNode Node
   * @return Integer list
   */
  private List<String> readJsonAsList(JsonNode jsonNode) {
    return jsonNode.isNull() ? null : mapper.convertValue(jsonNode, new TypeReference<List<String>>() {
    });
  }
}
