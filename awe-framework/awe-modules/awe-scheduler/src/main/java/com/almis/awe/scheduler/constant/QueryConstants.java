package com.almis.awe.scheduler.constant;

/**
 *
 * @author dfuentes
 */
public class QueryConstants {

  private QueryConstants(){}

  // Scheduler related queries
  public static final String SCHEDULER_TASK_QUERY = "taskData";
  public static final String SCHEDULER_TASK_PARAMETERS_QUERY = "taskParameters";
  public static final String SCHEDULER_TASK_DEPENDENCIES_QUERY = "taskDependencies";
  public static final String SCHEDULER_TASK_CALENDAR_QUERY = "taskCalendar";
  public static final String SCHEDULER_TASK_CALENDAR_DATES_QUERY = "taskCalendarDates";


  public static final String SCHEDULER_SERVER_DATA = "serverData";
  public static final String SCHEDULER_LOAD_TASK_DETAILS_QUERY = "javaSchedulerTaskList";
  public static final String SCHEDULER_LOAD_TASK_DETAILS_WITH_FILTER_QUERY = "javaSchedulerTaskListWfilter";
  public static final String SCHEDULER_AVERAGE_TIME_QUERY = "getTaskAverageTime";
  public static final String SCHEDULER_AVERAGE_TIMES_QUERY = "getAverageTimesOfTasks";
  public static final String SCHEDULER_ACTIVITY_EXECUTIONS = "getActivityExecutionRows";
  public static final String SCHEDULER_ACTIVITY_RUN_GROUPS = "getActivityRunGroups";
  public static final String SCHEDULER_ACTIVITY_RUN_SKIPPED = "getActivityRunSkippedRows";
  // Task catalog (Java only): every task with its launch and last execution, the saved executions of the tasks of a
  // page and the tasks of a page with parameters
  public static final String SCHEDULER_TASK_CATALOG_ROWS = "getTaskCatalogRows";
  public static final String SCHEDULER_TASK_CATALOG_HISTORY = "getTaskCatalogHistory";
  public static final String SCHEDULER_TASK_PAGE_TASK = "getTaskPageTask";
  // Configuration screen (Java only): the servers without their secrets, the calendars, their dates and the tasks that
  // use them
  public static final String SCHEDULER_CONFIGURATION_SERVER_ROWS = "getConfigurationServerRows";
  public static final String SCHEDULER_CONFIGURATION_SERVER_USAGE = "getConfigurationServerUsage";
  // One server for its side panel: its connection and whether each secret is saved, never the secrets
  public static final String SCHEDULER_CONFIGURATION_SERVER = "getConfigurationServer";
  public static final String SCHEDULER_CONFIGURATION_CALENDAR_ROWS = "getConfigurationCalendarRows";
  public static final String SCHEDULER_CONFIGURATION_CALENDAR_DATES = "getConfigurationCalendarDates";
  public static final String SCHEDULER_CONFIGURATION_CALENDAR_USAGE = "getConfigurationCalendarUsage";
  public static final String SCHEDULER_CONFIGURATION_CALENDAR_TASKS = "getConfigurationCalendarTasks";
  public static final String SCHEDULER_CONFIGURATION_ENGINE_TASKS = "getConfigurationEngineTasks";
  // Task screen (Java only): where a task runs and who gets its report, for the users with its permission
  public static final String SCHEDULER_TASK_PAGE_TASK_RESTRICTED = "getTaskPageTaskRestricted";
  public static final String SCHEDULER_TASK_PAGE_EXECUTIONS = "getTaskPageExecutions";
  public static final String SCHEDULER_FLOW_GRAPH = "getFlowGraph";
  // Flow run page (Java only): the run with the name of its root, and every task with its parents and options
  public static final String SCHEDULER_FLOW_RUN_HEADER_ROW = "getFlowRunHeaderRow";
  public static final String SCHEDULER_FLOW_RUN_GRAPH_TASKS = "getFlowRunGraphTasks";
  public static final String SCHEDULER_TASKS_WITH_PARAMETERS = "getTasksWithParameters";
  public static final String SCHEDULER_LAST_TASK_EXECUTION = "getLastExecution";
  public static final String SCHEDULER_TASK_EXECUTION = "getTaskExecution";
  public static final String SCHEDULER_DEFERRED_EXECUTION = "getDeferredExecution";
  public static final String SCHEDULER_RUNNING_DEFERRED = "getRunningDeferredExecutions";
  public static final String SCHEDULER_WEBHOOK_HOOK = "getWebhookHook";
  public static final String SCHEDULER_WEBHOOK_MESSAGE = "getWebhookMessage";
  public static final String SCHEDULER_DUE_WEBHOOK_MESSAGES = "getDueWebhookMessages";
  public static final String SCHEDULER_CALENDAR_LIST_QUERY = "javaSchCalLst";
  public static final String SCHEDULER_FILE_MODIFICATIONS_QUERY = "SchFil";
  public static final String SCHEDULER_GET_TASKS_WITH_CALENDARS = "tasksUsingCalendars";

}
