package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.scheduler.util.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import static com.almis.awe.scheduler.constant.TaskConstants.TASK_SEPARATOR;

/**
 * Quartz side of the tasks: adds, reschedules, pauses and removes task jobs and triggers, and answers questions
 * about their scheduler state.
 */
@Slf4j
public class TaskScheduling {

  // locales
  private static final String TITLE_SCHEDULER_NEW_TASK = "TITLE_SCHEDULER_NEW_TASK";
  private static final String MESSAGE_SCHEDULER_NEW_TASK = "MESSAGE_SCHEDULER_NEW_TASK";

  private final Scheduler scheduler;
  private final FileChecker fileChecker;
  private final TaskRepository taskRepository;
  private final TaskExecutionRepository executionRepository;
  private final ServiceConfig serviceConfig;

  /**
   * Constructor
   *
   * @param scheduler           Scheduler
   * @param fileChecker         File checker
   * @param taskRepository      Task repository
   * @param executionRepository Task execution repository
   * @param serviceConfig       Service configuration giving access to locales
   */
  public TaskScheduling(Scheduler scheduler, FileChecker fileChecker, TaskRepository taskRepository,
                        TaskExecutionRepository executionRepository, ServiceConfig serviceConfig) {
    this.scheduler = scheduler;
    this.fileChecker = fileChecker;
    this.taskRepository = taskRepository;
    this.executionRepository = executionRepository;
    this.serviceConfig = serviceConfig;
  }

  /**
   * Check if task is allowed to be launched
   *
   * @param task Task
   * @return Task is allowed
   */
  public boolean isTaskExecutionAllowed(Task task) {
    if (task != null && TaskLaunchType.FILE_TRACKING.getValue().equals(task.getLaunchType())) {
      // Check if file has been updated
      String file = fileChecker.checkFile(task);

      // Put launcher in job detail
      task.setLauncher(file);

      // Veto if file has not been updated
      return file != null;
    }
    return true;
  }

  /**
   * Add task to the scheduler
   *
   * @param task Task to add
   * @throws AWException Error adding task to scheduler
   */
  public void addTaskToScheduler(Task task) throws AWException {
    // Add new scheduled job to the scheduler
    try {
      scheduleTask(task);
      log.debug("New task added to the scheduler: #{}", task.getTaskId());

      if (!task.isActive()) {
        scheduler.pauseTrigger(task.getTrigger().getKey());
      }
    } catch (Exception exc) {
      throw new AWException("Error adding scheduler task: #" + task.getTaskId(), exc);
    }
  }

  /**
   * Load a task and add it to the scheduler (unless it is manual)
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  public ServiceData insertTask(Integer taskId) throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      Task task = taskRepository.loadTask(taskId);

      // Clear file modification table from database
      if (TaskLaunchType.FILE_TRACKING.getValue().equals(task.getLaunchType())) {
        taskRepository.clearFileModificationTable(taskId);
        log.debug("[SCHEDULERS][TASK {}] The modification table has been cleared", taskId);
      }

      task.setGroup(TaskUtil.getGroupForLaunchType(task.getLaunchType()));

      // Add task to scheduler if not manual
      if (task.getLaunchType() != 0) {
        addTaskToScheduler(task);
      }

      log.info("[SCHEDULER][TASK {}] The task has been added", taskId);
      serviceData
        .setTitle(serviceConfig.getLocale(TITLE_SCHEDULER_NEW_TASK))
        .setMessage(serviceConfig.getLocale(MESSAGE_SCHEDULER_NEW_TASK));
    } catch (Exception exc) {
      throw new AWException("Error inserting new task: " + taskId, exc);
    }

    return serviceData;
  }

  /**
   * Delete a single task from scheduler, purging its execution logs and file modifications
   *
   * @param ideTsk Task identifier
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteTask(Integer ideTsk) throws AWException {
    ServiceData serviceData = new ServiceData();
    String taskId = ideTsk.toString();
    try {
      // Purge log files
      executionRepository.purgeExecutionLogFiles(ideTsk, 0);

      // Check if the job exists
      for (String group : scheduler.getTriggerGroupNames()) {
        TriggerKey triggerKey = new TriggerKey(taskId, group);
        if (triggerExists(triggerKey)) {

          // Delete job
          scheduler.deleteJob(scheduler.getTrigger(triggerKey).getJobKey());
          log.info("[SCHEDULER][TASK {}] The task has been deleted", triggerKey);

          // Clear file modification table from database
          taskRepository.clearFileModificationTable(ideTsk);
        }
      }

    } catch (Exception exc) {
      throw new AWException("[SCHEDULER] Error deleting task: " + taskId, exc);
    }
    return serviceData;
  }

  /**
   * Pause the selected task
   *
   * @param task Task to pause
   * @return ServiceData
   * @throws SchedulerException Error pausing task
   */
  public ServiceData pauseTask(Task task) throws SchedulerException {
    TriggerKey key = new TriggerKey(String.valueOf(task.getTaskId()), task.getGroup());
    // Pause trigger in scheduler
    if (triggerExists(key)) {
      scheduler.pauseTrigger(key);
      log.info("[SCHEDULER][TASK_QUERY {}] Task paused", key);
    }
    return new ServiceData();
  }

  /**
   * Resume the selected task
   *
   * @param task Task to resume
   * @return ServiceData
   * @throws AWException Error resuming task
   */
  public ServiceData resumeTask(Task task) throws AWException {
    if (task.getLaunchType() != 0) {
      scheduleTask(task);
    }
    return new ServiceData();
  }

  /**
   * Get task execution from trigger
   *
   * @param trigger Trigger
   * @return Task execution from trigger, or null when the trigger does not identify an execution
   */
  public TaskExecution getTaskExecution(Trigger trigger) {
    String[] splittedTriggerId = trigger.getKey().getName().split(TASK_SEPARATOR);
    if (splittedTriggerId.length > 1) {
      return new TaskExecution()
        .setTaskId(Integer.valueOf(splittedTriggerId[0]))
        .setExecutionId(Integer.valueOf(splittedTriggerId[1]))
        .setGroupId(trigger.getKey().getGroup());
    } else {
      return null;
    }
  }

  /**
   * Check if trigger exists
   *
   * @param triggerKey Trigger key
   * @return Trigger exists
   * @throws SchedulerException Error checking trigger
   */
  public boolean triggerExists(TriggerKey triggerKey) throws SchedulerException {
    return scheduler.checkExists(triggerKey);
  }

  /**
   * Check if the job is running
   *
   * @param job Job detail
   * @return boolean Job is running
   * @throws SchedulerException Error checking if job is running
   */
  public boolean isJobRunning(JobDetail job) throws SchedulerException {
    // check if the job is in the scheduler currently executing job list
    for (JobExecutionContext jobExeContext : scheduler.getCurrentlyExecutingJobs()) {
      if (jobExeContext.getJobDetail().equals(job)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Schedule a task, rescheduling its trigger when it already exists
   *
   * @param task Task to schedule
   * @throws AWException Error scheduling task
   */
  private void scheduleTask(Task task) throws AWException {
    try {
      TriggerKey triggerKey = task.getTrigger().getKey();

      if (triggerExists(triggerKey)) {
        Trigger oldTrigger = scheduler.getTrigger(triggerKey);
        TriggerBuilder<? extends Trigger> triggerBuilder = oldTrigger.getTriggerBuilder();

        // Create new trigger from old trigger builder
        Trigger newTrigger = triggerBuilder.build();

        // Reschedule Job with the new trigger and the old trigger key
        scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
        log.info("[SCHEDULER][TASK_QUERY {}] Task resumed", triggerKey);
      } else {
        scheduler.scheduleJob(task.getJob(), task.getTrigger());
      }
    } catch (Exception exc) {
      throw new AWException("Error scheduling task: " + task.getTaskId(), exc);
    }
  }
}
