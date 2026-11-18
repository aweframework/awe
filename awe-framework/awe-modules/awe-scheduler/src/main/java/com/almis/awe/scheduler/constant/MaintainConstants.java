package com.almis.awe.scheduler.constant;

/**
 *
 * @author dfuentes
 */
public class MaintainConstants {

  private MaintainConstants(){}

  // Scheduler related maintains
  public static final String UPDATE_TASKS_CALENDAR_MAINTAIN = "UpdSchTskWthCal";
  public static final String REMOVE_SELECTED_CALENDAR_FROM_TASKS = "UpdSchTskWthCal";
  public static final String FILE_INSERT_MODIFICATION_QUERY = "insertFile";
  public static final String FILE_DELETE_MODIFICATION_QUERY = "deleteFile";
  public static final String FILE_UPDATE_MODIFICATION_QUERY = "updateFile";
  public static final String TASK_START = "startTask";
  public static final String TASK_END = "endTask";
  public static final String GET_ALL_EXECUTIONS_WITH_DATES = "getSortedExecutions";
  public static final String PURGE_EXECUTION_LOGS = "purgeExecutionLogs";
  public static final String GET_ALL_EXECUTIONS = "getAllExecutions";
  public static final String UPDATE_INTERRUPTED_TASKS = "updateInterruptedTasks";
  public static final String TASK_UPDATE_STATUS = "updateTaskStatus";
  public static final String CLOSE_RUNNING_EXECUTION = "closeRunningExecution";
  public static final String INSERT_DEFERRED_EXECUTION = "insertDeferredExecution";
  public static final String INSERT_WEBHOOK_HOOK = "insertWebhookHook";
  public static final String DELETE_WEBHOOK_HOOK = "deleteWebhookHook";
  public static final String INSERT_WEBHOOK_MESSAGE = "insertWebhookMessage";
  public static final String CLAIM_WEBHOOK_MESSAGE = "claimWebhookMessage";
  public static final String FINISH_WEBHOOK_MESSAGE = "finishWebhookMessage";
  public static final String FAIL_WEBHOOK_MESSAGE = "failWebhookMessage";
  public static final String PURGE_WEBHOOK_MESSAGES = "purgeWebhookMessages";
  public static final String UPDATE_DEFERRED_COMPLETION = "updateDeferredCompletion";
  public static final String UPDATE_DEFERRED_CANCELLATION = "updateDeferredCancellation";
}
