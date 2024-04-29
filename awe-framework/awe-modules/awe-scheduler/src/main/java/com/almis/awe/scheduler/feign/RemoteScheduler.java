package com.almis.awe.scheduler.feign;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "remote-scheduler", url = "${awe.scheduler.remote-scheduler-url:http://localhost:8000/scheduler/api/v1}")
public interface RemoteScheduler {

  @PostMapping("/tasks")
  ServiceData getTaskList(@RequestBody TaskListCriteria taskListCriteria);

  @PostMapping("/tasks/executions")
  ServiceData getNextFireTimes(@RequestParam("numberOfFireTimes") int numberOfFireTimes, @RequestBody Schedule schedule);

  /**
   * Retrieve the task progress status
   *
   * @return Service Data
   */
  @GetMapping("/task/{taskId}/executions")
  ServiceData getTaskExecutionList(@PathVariable int taskId);

  /**
   * Pause the selected task
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/pause")
  ServiceData pauseTask(@PathVariable int taskId);

  /**
   * Resume the selected task
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/resume")
  ServiceData resumeTask(@PathVariable int taskId);

  /**
   * Execute the selected task now
   *
   * @param taskId Task identifier
   * @param user   Launch user
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/execute")
  ServiceData executeTaskNow(@PathVariable int taskId, @RequestParam String user);

  /**
   * Insert and schedule a new task
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PutMapping("/task/{taskId}/schedule")
  ServiceData scheduleTask(@PathVariable int taskId);

  /**
   * Update and reschedule a task
   *
   * @param taskId Task identifier list
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/schedule")
  ServiceData rescheduleTask(@PathVariable int taskId);

  /**
   * Delete some tasks from scheduler
   *
   * @param taskIdList Task identifier list
   * @return Service Data
   */
  @DeleteMapping("/tasks/schedule")
  ServiceData unscheduleTask(@RequestParam List<Integer> taskIdList);

  /**
   * Purge execution logs
   *
   * @param taskId Task identifier
   * @param keep   Number of executions to keep
   * @return Service Data
   */
  @DeleteMapping("/task/{taskId}/executions")
  ServiceData purgeExecutions(@PathVariable int taskId, @RequestParam("keep") int keep);

  /**
   * Get currently executing jobs from the scheduler instance
   *
   * @return Service Data
   */
  @GetMapping("/jobs/executing")
  ServiceData getExecutingJobs();

  /**
   * Get configured
   *
   * @return Service Data
   */
  @GetMapping("/jobs/configured")
  ServiceData getConfiguredJobs();

  /**
   * Insert and schedule a new calendar
   *
   * @param calendarId Calendar identifier
   * @return Service Data
   */
  @PutMapping("/calendar/{calendarId}")
  ServiceData insertCalendar(@PathVariable int calendarId);

  /**
   * Update and schedule a new calendar
   *
   * @param calendarId Calendar identifier
   * @return Service Data
   */
  @PostMapping("/calendar/{calendarId}")
  ServiceData updateCalendar(@PathVariable int calendarId);

  /**
   * Delete a calendar from scheduler
   *
   * @param calendarIdList Calendar identifier list
   * @return Service Data
   */
  @DeleteMapping("/calendar")
  ServiceData deleteCalendars(@RequestParam("calendarList") List<Integer> calendarIdList);

  /**
   * Check if the scheduler contains the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return Service Data
   */
  @GetMapping("/calendar/check")
  ServiceData checkCalendarInTriggers(@RequestParam("calendarList") List<Integer> calendarIdList);

  /**
   * Resume the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return Service Data
   */
  @PostMapping("/calendar/activate")
  ServiceData activateCalendar(@RequestParam("calendarList") List<Integer> calendarIdList);

  /**
   * Pause the selected calendar
   *
   * @param calendarIdList Calendar identifier list
   * @return Service Data
   */
  @PostMapping("/calendar/deactivate")
  ServiceData deactivateCalendar(@RequestParam("calendarList") List<Integer> calendarIdList);

  /**
   * Start the scheduler service
   *
   * @return Service Data
   */
  @PostMapping("/start")
  ServiceData startScheduler();

  /**
   * Stop the scheduler service
   *
   * @return Service Data
   */
  @PostMapping("/stop")
  ServiceData stopScheduler();

  /**
   * Scheduler's emergency reboot method
   *
   * @return Service Data
   */
  @PostMapping("/restart")
  ServiceData restartScheduler();

  /**
   * Clear all scheduled tasks and stop the scheduler.
   *
   * @return Service Data
   */
  @PostMapping("/clearAndStop")
  ServiceData clearAndStopScheduler();

  /**
   * Returns information about the configured scheduler
   *
   * @return Service Data
   */
  @GetMapping("/metadata")
  ServiceData getSchedulerMetadata();
}
