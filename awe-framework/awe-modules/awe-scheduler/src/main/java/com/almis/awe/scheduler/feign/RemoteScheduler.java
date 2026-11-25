package com.almis.awe.scheduler.feign;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "remote-scheduler", url = "${awe.scheduler.remote-scheduler-url:http://localhost:8000/scheduler/api/v1}",
  configuration = RemoteSchedulerClientConfiguration.class)
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
   * Retrieve a page of the executions of all tasks (activity screen)
   *
   * @param criteria Filters and page
   * @return Service Data
   */
  @PostMapping("/executions")
  ServiceData getExecutionList(@RequestBody ExecutionListCriteria criteria);

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run (activity screen)
   *
   * @param criteria Filters and page of the top-level rows
   * @return Service Data
   */
  @PostMapping("/executions/runs")
  ServiceData getRunList(@RequestBody ExecutionListCriteria criteria);

  /**
   * Retrieve the summary of a flow run (flow run page)
   *
   * @param runId Flow run identifier
   * @return Service Data
   */
  @GetMapping("/runs/{runId}")
  ServiceData getFlowRunSummary(@PathVariable("runId") String runId);

  /**
   * Retrieve the graph of a flow run (flow run page)
   *
   * @param runId Flow run identifier
   * @return Service Data
   */
  @GetMapping("/runs/{runId}/graph")
  ServiceData getFlowRunGraph(@PathVariable("runId") String runId);

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts
   *
   * @param criteria Filters of the execution list (search, origin and start)
   * @return Service Data
   */
  @PostMapping("/executions/state-counts")
  ServiceData getExecutionStateCounts(@RequestBody ExecutionListCriteria criteria);

  /**
   * Retrieve a page of the task catalog (tasks screen)
   *
   * @param criteria Filters, sort and page
   * @return Service Data
   */
  @PostMapping("/tasks/catalog")
  ServiceData getTaskCatalog(@RequestBody TaskCatalogCriteria criteria);

  /**
   * Retrieve the options of the active filter of the task catalog with their counts
   *
   * @param criteria Filters of the catalog (search, launch and last status)
   * @return Service Data
   */
  @PostMapping("/tasks/catalog/active-counts")
  ServiceData getTaskCatalogActiveCounts(@RequestBody TaskCatalogCriteria criteria);

  /**
   * Retrieve the counters of the activity screen
   *
   * @return Service Data
   */
  @GetMapping("/executions/counters")
  ServiceData getActivityCounters();

  /**
   * Pause the selected task
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/pause")
  ServiceData pauseTask(@PathVariable int taskId);

  /**
   * Stop the deferred executions of the selected task
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/stop")
  ServiceData stopTask(@PathVariable int taskId);

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
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values for variable parameters; empty map when none
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/execute")
  ServiceData executeTaskNow(@PathVariable int taskId, @RequestParam String user, @RequestBody Map<String, String> variables);

  /**
   * Validate the task parameters against the remote scheduler configuration
   *
   * <p>The remote side reports an unresolved property through the returned ServiceData rather than
   * by failing, so the outcome survives the HTTP hop.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values for the task parameters; empty map when none
   * @return Service Data, of type ERROR when a property key does not resolve
   */
  @PostMapping("/task/{taskId}/parameters/validate")
  ServiceData validateTaskParameters(@PathVariable int taskId, @RequestBody Map<String, String> variables);

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
   * Refresh an edited task with its stored launch, without purging its executions
   *
   * @param taskId Task identifier
   * @return Service Data
   */
  @PostMapping("/task/{taskId}/refresh")
  ServiceData refreshTask(@PathVariable int taskId);

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

  /**
   * Card of the engine of the configuration screen
   *
   * @return Service Data
   */
  @GetMapping("/engine")
  ServiceData getConfigurationEngine();

  /**
   * Schedules of the engine of the configuration screen, filtered by their state
   *
   * @param filter paused or error; every schedule when blank
   * @return Service Data
   */
  @GetMapping("/engine/triggers")
  ServiceData getConfigurationEngineTriggers(@RequestParam(value = "filter", required = false) String filter);

  /**
   * Options of the state filter of the schedules of the engine with their counts
   *
   * @return Service Data
   */
  @GetMapping("/engine/triggers/counts")
  ServiceData getConfigurationEngineTriggerCounts();
}
