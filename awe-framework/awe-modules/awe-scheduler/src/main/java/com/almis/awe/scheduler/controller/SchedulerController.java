package com.almis.awe.scheduler.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.service.SchedulerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scheduler/api/v1")
public class SchedulerController {

  private final SchedulerService schedulerService;

  /**
   * Constructor
   *
   * @param schedulerService Scheduler service
   */
  public SchedulerController(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /**
   * Retrieve the task list
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  @GetMapping("/tasks")
  ServiceData getTaskList() throws AWException {
    return schedulerService.getTaskList();
  }

  /**
   * Compute next fire times
   *
   * @param numberOfFireTimes # of fire times to compute
   * @param schedule Schedule
   * @return Service data
   * @throws AWException Error computing next fire times
   */
  @PostMapping("/tasks/executions")
  ServiceData getNextFireTimes(@RequestParam("numberOfFireTimes") int numberOfFireTimes, @RequestBody Schedule schedule) throws AWException {
    return schedulerService.computeNextFireTimes(numberOfFireTimes, schedule);
  }

  /**
   * Retrieve the task progress status
   *
   * @return ServiceData
   * @throws AWException Error retrieving task execution list
   */
  @GetMapping("/task/{taskId}/executions")
  ServiceData getTaskExecutionList(@PathVariable int taskId) throws AWException {
    return schedulerService.getTaskExecutionList(taskId);
  }

  /**
   * Pause the selected task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error pausing task
   */
  @PostMapping("/task/{taskId}/pause")
  ServiceData pauseTask(@PathVariable int taskId) throws AWException {
    return schedulerService.pauseTask(taskId);
  }

  /**
   * Resume the selected task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error resuming task
   */
  @PostMapping("/task/{taskId}/resume")
  ServiceData resumeTask(@PathVariable int taskId) throws AWException {
    return schedulerService.resumeTask(taskId);
  }

  /**
   * Execute the selected task now
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error executing task
   */
  @PostMapping("/task/{taskId}/execute")
  ServiceData executeTaskNow(@PathVariable int taskId, @RequestParam String user) throws AWException {
    return schedulerService.executeTaskNow(taskId, user);
  }

  /**
   * Insert and schedule a new task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  @PutMapping("/task/{taskId}/schedule")
  ServiceData scheduleTask(@PathVariable int taskId) throws AWException {
    return schedulerService.insertSchedulerTask(taskId);
  }

  /**
   * Update and reschedule a task
   *
   * @param taskId Task identifier list
   * @return ServiceData
   * @throws AWException Error updating task
   */
  @PostMapping("/task/{taskId}/schedule")
  ServiceData rescheduleTask(@PathVariable int taskId) throws AWException {
    return schedulerService.updateSchedulerTask(taskId);
  }

  /**
   * Delete a single task from scheduler
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  @DeleteMapping("/task/{taskId}/schedule")
  ServiceData unscheduleTask(@PathVariable int taskId) throws AWException {
    return schedulerService.deleteSchedulerTask(taskId);
  }

  /**
   * Delete some tasks from scheduler
   *
   * @param taskIdList Task identifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  @DeleteMapping("/tasks/schedule")
  ServiceData unscheduleTask(@RequestParam List<Integer> taskIdList) throws AWException {
    return schedulerService.deleteSchedulerTask(taskIdList);
  }

  /**
   * Purge execution logs
   *
   * @param taskId Task identifier
   * @param keep   Number of executions to keep
   * @return Service data
   * @throws AWException Error purging execution logs
   */
  @DeleteMapping("/task/{taskId}/executions")
  ServiceData purgeExecutions(@PathVariable int taskId, @RequestParam("keep") int keep) throws AWException {
    return schedulerService.purgeExecutionLogs(taskId, keep);
  }

  /**
   * Get currently executing jobs from the scheduler instance
   *
   * @return Service data
   * @throws AWException Error retrieving currently executing jobs
   */
  @GetMapping("/jobs/executing")
  ServiceData getExecutingJobs() throws AWException {
    return schedulerService.currentlyExecutingJobs();
  }

  /**
   * Get configured
   *
   * @return Configured jobs
   * @throws AWException Error retrieving configured jobs
   */
  @GetMapping("/jobs/configured")
  ServiceData getConfiguredJobs() throws AWException {
    return schedulerService.getConfiguredJobs();
  }

  /**
   * Insert and schedule a new calendar
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error inserting scheduler calendar
   */
  @PutMapping("/calendar/{calendarId}")
  ServiceData insertCalendar(@PathVariable int calendarId) throws AWException {
    return schedulerService.insertSchedulerCalendar(calendarId);
  }

  /**
   * Update and schedule a new calendar
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error updating scheduler calendar
   */
  @PostMapping("/calendar/{calendarId}")
  ServiceData updateCalendar(@PathVariable int calendarId) throws AWException {
    return schedulerService.updateSchedulerCalendar(calendarId);
  }

  /**
   * Delete a calendar from scheduler
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error deleting scheduler calendar
   */
  @DeleteMapping("/calendar/{calendarId}")
  ServiceData deleteCalendars(@PathVariable int calendarId) throws AWException {
    return schedulerService.deleteSchedulerCalendar(calendarId);
  }

  /**
   * Delete a calendar from scheduler
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deleting scheduler calendar
   */
  @DeleteMapping("/calendar")
  ServiceData deleteCalendars(@RequestParam("calendarList") List<Integer> calendarIdList) throws AWException {
    return schedulerService.deleteSchedulerCalendar(calendarIdList);
  }

  /**
   * Check if the scheduler contains the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error checking calendars existence
   */
  @GetMapping("/calendar/check")
  ServiceData checkCalendarInTriggers(@RequestParam("calendarList") List<Integer> calendarIdList) throws AWException {
    return schedulerService.checkCalendarExist(calendarIdList);
  }

  /**
   * Resume the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error activating calendars
   */
  @PostMapping("/calendar/activate")
  ServiceData activateCalendar(@RequestParam("calendarList") List<Integer> calendarIdList) throws AWException {
    return schedulerService.activateCalendar(calendarIdList);
  }

  /**
   * Pause the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deactivating calendars
   */
  @PostMapping("/calendar/deactivate")
  ServiceData deactivateCalendar(@RequestParam("calendarList") List<Integer> calendarIdList) throws AWException {
    return schedulerService.deactivateCalendar(calendarIdList);
  }

  /**
   * Start the scheduler service
   *
   * @throws AWException Error starting scheduler
   */
  @PostMapping("/start")
  ServiceData startScheduler() throws AWException {
    return schedulerService.start();
  }

  /**
   * Stop the scheduler service
   *
   * @throws AWException Error stopping scheduler
   */
  @PostMapping("/stop")
  ServiceData stopScheduler() throws AWException {
    return schedulerService.stop();
  }

  /**
   * Scheduler's emergency reboot method
   *
   * @throws AWException Error restarting scheduler
   */
  @PostMapping("/restart")
  ServiceData restartScheduler() throws AWException {
    return schedulerService.restart();
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   *
   * @return Service data
   */
  @PostMapping("/clearAndStop")
  ServiceData clearAndStopScheduler() throws AWException {
    return schedulerService.clearAndStop();
  }

  /**
   * Returns information about the configured scheduler
   *
   * @return Scheduler metadata
   * @throws AWException Error retrieving scheduler metadata
   */
  @GetMapping("/metadata")
  ServiceData getSchedulerMetadata() throws AWException {
    return schedulerService.getSchedulerMetadata();
  }

}
