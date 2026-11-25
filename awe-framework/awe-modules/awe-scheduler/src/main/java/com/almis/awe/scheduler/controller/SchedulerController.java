package com.almis.awe.scheduler.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.service.CompletionResult;
import com.almis.awe.scheduler.service.SchedulerExecutionCompletionService;
import com.almis.awe.scheduler.service.SchedulerService;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

@RestController
@RequestMapping("/scheduler/api/v1")
public class SchedulerController {

  private static final String INVALID_REQUEST = "invalid-request";
  private static final String DETAIL = "detail";
  private static final Map<String, TaskStatus> COMPLETION_STATUSES = Map.of(
    "OK", TaskStatus.JOB_OK,
    "WARNING", TaskStatus.JOB_WARNING,
    "ERROR", TaskStatus.JOB_ERROR,
    "STOPPED", TaskStatus.JOB_STOPPED,
    "INTERRUPTED", TaskStatus.JOB_INTERRUPTED,
    "INFO", TaskStatus.JOB_INFO);

  private final SchedulerService schedulerService;
  private final SchedulerExecutionCompletionService completionService;

  /**
   * Constructor
   *
   * @param schedulerService  Scheduler service
   * @param completionService Service that completes the deferred executions
   */
  public SchedulerController(SchedulerService schedulerService,
                             SchedulerExecutionCompletionService completionService) {
    this.schedulerService = schedulerService;
    this.completionService = completionService;
  }

  /**
   * Body of the completion of a deferred execution
   *
   * @param status      OK, WARNING, ERROR, STOPPED, INTERRUPTED or INFO (required, case insensitive)
   * @param description Description of the execution (optional, 2000 characters at most)
   * @param outputs     Output values (optional), received as parameters by the dependent tasks
   */
  public record CompletionRequest(String status, String description, Map<String, String> outputs) {
  }

  /**
   * Complete a deferred execution
   *
   * A running execution that is not registered as deferred yet answers {@code execution-not-deferred} with
   * {@code currentStatus} {@code JOB_RUNNING}; a client that expects the execution to be deferred should retry
   * shortly.
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @param request     Completion body
   * @return 200 with the applied status; 409 when the execution is not running or was never deferred; 404 when it
   * does not exist; 400 when the body is not valid
   * @throws AWException Error closing the execution
   */
  @PostMapping("/tasks/{taskId}/executions/{executionId}/complete")
  ResponseEntity<Map<String, Object>> completeExecution(@PathVariable int taskId, @PathVariable int executionId,
                                                        @RequestBody CompletionRequest request) throws AWException {
    TaskStatus status = request.status() == null ? null
      : COMPLETION_STATUSES.get(request.status().trim().toUpperCase(Locale.ROOT));
    if (status == null) {
      return invalid("status must be one of " + new TreeSet<>(COMPLETION_STATUSES.keySet()));
    }
    if (request.description() != null && request.description().length() > SchedulerDeferred.MAX_DESCRIPTION_LENGTH) {
      return invalid("description must have " + SchedulerDeferred.MAX_DESCRIPTION_LENGTH + " characters at most");
    }

    CompletionResult result = completionService.complete(taskId, executionId, status, request.description(),
      request.outputs());
    return switch (result.outcome()) {
      case CLOSED -> ResponseEntity.ok(Map.of("taskId", taskId, "executionId", executionId,
        "status", result.currentStatus().name()));
      case NOT_RUNNING -> conflict("execution-not-running", result);
      case NOT_DEFERRED -> conflict("execution-not-deferred", result);
      case UNKNOWN -> ResponseEntity.notFound().build();
    };
  }

  private ResponseEntity<Map<String, Object>> invalid(String detail) {
    return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, detail));
  }

  private ResponseEntity<Map<String, Object>> conflict(String type, CompletionResult result) {
    return ResponseEntity.status(409).body(Map.of("type", type, "currentStatus", result.currentStatus().name()));
  }

  /**
   * Retrieve the task list
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  @PostMapping("/tasks")
  ServiceData getTaskList(@RequestBody TaskListCriteria taskListCriteria) throws AWException {
    return schedulerService.getTaskList(taskListCriteria);
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
   * Retrieve a page of the executions of all tasks (activity screen)
   *
   * @param criteria Filters and page
   * @return 200 with the ServiceData of the page; 400 when a filter value is not valid
   * @throws AWException Error retrieving the executions
   */
  @PostMapping("/executions")
  ResponseEntity<Object> getExecutionList(@RequestBody ExecutionListCriteria criteria) throws AWException {
    try {
      return ResponseEntity.ok(schedulerService.getExecutionList(criteria));
    } catch (InvalidExecutionFilterException exc) {
      return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, exc.getMessage()));
    }
  }

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run (activity screen): the flow runs with an
   * execution that matches the filters and the matching executions without run are the top-level rows of the page, the
   * executions of each run (and the tasks it skipped) its children
   *
   * @param criteria Filters and page of the top-level rows
   * @return 200 with the ServiceData of the page; 400 when a filter value is not valid
   * @throws AWException Error retrieving the executions
   */
  @PostMapping("/executions/runs")
  ResponseEntity<Object> getRunList(@RequestBody ExecutionListCriteria criteria) throws AWException {
    try {
      return ResponseEntity.ok(schedulerService.getRunList(criteria));
    } catch (InvalidExecutionFilterException exc) {
      return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, exc.getMessage()));
    }
  }

  /**
   * Retrieve the summary of a flow run (flow run page)
   *
   * @param runId Flow run identifier
   * @return One row, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  @GetMapping("/runs/{runId}")
  ServiceData getFlowRunSummary(@PathVariable("runId") String runId) throws AWException {
    return schedulerService.getFlowRunSummary(runId);
  }

  /**
   * Retrieve the graph of a flow run (flow run page): one row per task of the run and parent
   *
   * @param runId Flow run identifier
   * @return Rows of the graph, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  @GetMapping("/runs/{runId}/graph")
  ServiceData getFlowRunGraph(@PathVariable("runId") String runId) throws AWException {
    return schedulerService.getFlowRunGraph(runId);
  }

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts
   *
   * @param criteria Filters of the execution list (search, origin and start)
   * @return 200 with the ServiceData of the options (value, label, count and tone); 400 when a filter value is not
   * valid
   * @throws AWException Error counting the executions
   */
  @PostMapping("/executions/state-counts")
  ResponseEntity<Object> getExecutionStateCounts(@RequestBody ExecutionListCriteria criteria) throws AWException {
    try {
      return ResponseEntity.ok(schedulerService.getExecutionStateCounts(criteria));
    } catch (InvalidExecutionFilterException exc) {
      return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, exc.getMessage()));
    }
  }

  /**
   * Retrieve a page of the task catalog (tasks screen): every task with its last status, when it is launched, the
   * strip of its last saved executions, its next execution, its average time and whether it is active
   *
   * @param criteria Filters, sort and page
   * @return 200 with the ServiceData of the page; 400 when a filter value is not valid
   * @throws AWException Error retrieving the tasks
   */
  @PostMapping("/tasks/catalog")
  ResponseEntity<Object> getTaskCatalog(@RequestBody TaskCatalogCriteria criteria) throws AWException {
    try {
      return ResponseEntity.ok(schedulerService.getTaskCatalog(criteria));
    } catch (InvalidExecutionFilterException exc) {
      return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, exc.getMessage()));
    }
  }

  /**
   * Retrieve the options of the active filter of the task catalog with their counts
   *
   * @param criteria Filters of the catalog (search, launch and last status)
   * @return 200 with the ServiceData of the options (value, label, count and tone); 400 when a filter value is not
   * valid
   * @throws AWException Error counting the tasks
   */
  @PostMapping("/tasks/catalog/active-counts")
  ResponseEntity<Object> getTaskCatalogActiveCounts(@RequestBody TaskCatalogCriteria criteria) throws AWException {
    try {
      return ResponseEntity.ok(schedulerService.getTaskCatalogActiveCounts(criteria));
    } catch (InvalidExecutionFilterException exc) {
      return ResponseEntity.badRequest().body(Map.of("type", INVALID_REQUEST, DETAIL, exc.getMessage()));
    }
  }

  /**
   * Retrieve the counters of the activity screen
   *
   * @return ServiceData
   * @throws AWException Error retrieving the counters
   */
  @GetMapping("/executions/counters")
  ServiceData getActivityCounters() throws AWException {
    return schedulerService.getActivityCounters();
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
   * Stop the deferred executions of the selected task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error stopping the executions
   */
  @PostMapping("/task/{taskId}/stop")
  ServiceData stopTask(@PathVariable int taskId) throws AWException {
    return schedulerService.stopTask(taskId);
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
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values for variable parameters
   * @return ServiceData
   * @throws AWException Error executing task
   */
  @PostMapping("/task/{taskId}/execute")
  ServiceData executeTaskNow(@PathVariable int taskId, @RequestParam String user, @RequestBody(required = false) Map<String, String> variables) throws AWException {
    return schedulerService.executeTaskNow(taskId, user, variables);
  }

  /**
   * Validate the task parameters against the configuration of this instance
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values for the task parameters
   * @return ServiceData, of type ERROR when a property key does not resolve
   * @throws AWException Error loading the task
   */
  @PostMapping("/task/{taskId}/parameters/validate")
  ServiceData validateTaskParameters(@PathVariable int taskId, @RequestBody Map<String, String> variables) throws AWException {
    return schedulerService.validateTaskParameters(taskId, variables);
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
   * Refresh an edited task with its stored launch, without purging its executions
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error refreshing task
   */
  @PostMapping("/task/{taskId}/refresh")
  ServiceData refreshTask(@PathVariable int taskId) throws AWException {
    return schedulerService.refreshSchedulerTask(taskId);
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

  /**
   * Card of the engine of the configuration screen
   *
   * @return One row
   * @throws AWException Error building the card
   */
  @GetMapping("/engine")
  ServiceData getConfigurationEngine() throws AWException {
    return schedulerService.getConfigurationEngine();
  }

  /**
   * Schedules of the engine of the configuration screen, filtered by their state
   *
   * @param filter paused or error; every schedule when blank
   * @return One row per schedule
   * @throws AWException The filter is not valid or the engine cannot be read
   */
  @GetMapping("/engine/triggers")
  ServiceData getConfigurationEngineTriggers(@RequestParam(value = "filter", required = false) String filter)
    throws AWException {
    return schedulerService.getConfigurationEngineTriggers(filter);
  }

  /**
   * Options of the state filter of the schedules of the engine with their counts
   *
   * @return One row per option
   * @throws AWException The engine cannot be read
   */
  @GetMapping("/engine/triggers/counts")
  ServiceData getConfigurationEngineTriggerCounts() throws AWException {
    return schedulerService.getConfigurationEngineTriggerCounts();
  }

}
