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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;
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
  /**
   * Groups of the trigger of a task (named by its identifier): the scheduled and the file tasks. The other launches
   * have triggers of their own, named by the task and a sequence
   */
  private static final List<String> TASK_GROUPS = List.of(SCHEDULED_GROUP, FILE_TRACKING_GROUP);
  /**
   * Years after which the next fire of a trigger excluded by its calendar is no longer searched (as Quartz does)
   */
  private static final int YEARS_TO_GIVE_UP = 100;

  private final Scheduler scheduler;
  private final FileChecker fileChecker;
  private final TaskRepository taskRepository;
  private final TaskExecutionRepository executionRepository;
  private final ServiceConfig serviceConfig;
  private final Clock clock;

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
    this(scheduler, fileChecker, taskRepository, executionRepository, serviceConfig, Clock.systemDefaultZone());
  }

  /**
   * Constructor
   *
   * @param scheduler           Scheduler
   * @param fileChecker         File checker
   * @param taskRepository      Task repository
   * @param executionRepository Task execution repository
   * @param serviceConfig       Service configuration giving access to locales
   * @param clock               Clock that tells the next fire of a refreshed task and of a restored trigger
   */
  public TaskScheduling(Scheduler scheduler, FileChecker fileChecker, TaskRepository taskRepository,
                        TaskExecutionRepository executionRepository, ServiceConfig serviceConfig, Clock clock) {
    this.clock = clock;
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
      if (!TaskUtil.isLaunchedOnDemand(task.getLaunchType())) {
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
   * Refresh an edited task in the scheduler with its stored launch, so a change of launch type is applied (scheduled to
   * manual removes the trigger, manual to scheduled creates it). Unlike an update it does not purge its executions nor
   * their logs nor the files it has seen. A trigger is never live when it should not fire, by construction (no window
   * between an add and a pause):
   * <ol>
   *   <li>The new trigger and job are built from the stored task and checked first: a task that cannot be loaded or
   *   whose calendar is not in the scheduler fails here and its job is kept.</li>
   *   <li>Its job is removed only from the groups of a task trigger ({@link #TASK_GROUPS}): the other groups (timeouts,
   *   reports, immediate launches) are not touched.</li>
   *   <li>The new job is added only when the task is scheduled (not launched on demand), active and its trigger fires
   *   after now. Otherwise the task is left without trigger: an inactive task is not added paused, and a trigger
   *   without any fire after now is not added (Quartz would fire its past first fire as a misfire). Activating the task
   *   ({@link #resumeTask(Task)}) adds it from the stored task.</li>
   *   <li>If the add fails, the new job is removed and the previous one comes back: a trigger that was paused is not
   *   added again (the task is left without trigger until it is activated), and an active one is rebuilt to start at its
   *   previous next fire, or at its first fire after now when that one has passed, so no past fire is taken as a
   *   misfire (each error of the restore is logged).</li>
   * </ol>
   *
   * @param taskId Task identifier
   * @throws AWException Error refreshing the task (its previous job is kept or restored)
   */
  public void refreshTask(Integer taskId) throws AWException {
    Task task = taskRepository.loadTask(taskId);
    boolean scheduled = !TaskUtil.isLaunchedOnDemand(task.getLaunchType());
    Date nextFire = scheduled ? getNextFire(task) : null;
    boolean added = scheduled && task.isActive() && nextFire != null;
    List<RemovedJob> previous = removeTaskJobs(taskId);
    if (added) {
      task.setGroup(TaskUtil.getGroupForLaunchType(task.getLaunchType()));
      try {
        scheduler.scheduleJob(task.getJob(), task.getTrigger());
      } catch (SchedulerException exc) {
        AWException error = new AWException("Error refreshing task: " + taskId, exc);
        removeNewJob(task, error);
        restore(taskId, previous, error);
        throw error;
      }
    } else if (scheduled && !task.isActive()) {
      log.info("[SCHEDULER][TASK {}] The task is inactive: it is left without trigger until it is activated", taskId);
    } else if (scheduled) {
      log.warn("[SCHEDULER][TASK {}] The task does not fire after now: it is left without trigger", taskId);
    }
    log.info("[SCHEDULER][TASK {}] The task has been refreshed", taskId);
  }

  /**
   * Check the new trigger of a refreshed task before its job is removed and get its next fire
   *
   * @return Next fire after now with its calendar, null when it does not fire again
   * @throws AWException The calendar of the trigger is not in the scheduler
   */
  private Date getNextFire(Task task) throws AWException {
    try {
      return getFireAfterNow(task.getTrigger());
    } catch (SchedulerException exc) {
      throw new AWException("Error refreshing task: " + task.getTaskId(), exc);
    }
  }

  /**
   * First fire of a trigger after now that its calendar includes
   *
   * @param trigger Trigger
   * @return Fire time, null when it does not fire after now
   * @throws SchedulerException The calendar of the trigger is not in the scheduler
   */
  private Date getFireAfterNow(Trigger trigger) throws SchedulerException {
    org.quartz.Calendar calendar = null;
    if (trigger.getCalendarName() != null) {
      calendar = scheduler.getCalendar(trigger.getCalendarName());
      if (calendar == null) {
        throw new SchedulerException("Calendar " + trigger.getCalendarName() + " of trigger " + trigger.getKey()
          + " is not in the scheduler");
      }
    }
    Instant now = clock.instant();
    Instant giveUp = now.atZone(ZoneOffset.UTC).plusYears(YEARS_TO_GIVE_UP).toInstant();
    Date fire = trigger.getFireTimeAfter(Date.from(now));
    while (fire != null && calendar != null && !calendar.isTimeIncluded(fire.getTime())) {
      fire = fire.toInstant().isAfter(giveUp) ? null : trigger.getFireTimeAfter(fire);
    }
    return fire;
  }

  /**
   * Remove the job of a task from the groups of a task trigger, keeping what is needed to restore it
   */
  private List<RemovedJob> removeTaskJobs(Integer taskId) throws AWException {
    List<RemovedJob> removed = new ArrayList<>();
    try {
      for (String group : TASK_GROUPS) {
        TriggerKey triggerKey = new TriggerKey(String.valueOf(taskId), group);
        Trigger trigger = triggerExists(triggerKey) ? scheduler.getTrigger(triggerKey) : null;
        if (trigger != null) {
          JobDetail job = scheduler.getJobDetail(trigger.getJobKey());
          boolean paused = Trigger.TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey));
          scheduler.deleteJob(trigger.getJobKey());
          removed.add(new RemovedJob(job, trigger, paused));
        }
      }
    } catch (SchedulerException exc) {
      AWException error = new AWException("Error refreshing task: " + taskId, exc);
      restore(taskId, removed, error);
      throw error;
    }
    return removed;
  }

  /**
   * Remove the new job of a refreshed task that could not be added or paused
   */
  private void removeNewJob(Task task, AWException error) {
    try {
      if (task.getJob() != null && scheduler.checkExists(task.getJob().getKey())) {
        scheduler.deleteJob(task.getJob().getKey());
      }
    } catch (SchedulerException exc) {
      log.error("[SCHEDULER][TASK {}] The new job could not be removed", task.getTaskId(), exc);
      error.addSuppressed(exc);
    }
  }

  /**
   * Add again the active jobs removed from a refreshed task, each trigger rebuilt to start at its next fire after now.
   * A paused trigger, or one that does not fire again, is not added: the task is left without trigger
   */
  private void restore(Integer taskId, List<RemovedJob> removed, AWException error) {
    for (RemovedJob previous : removed) {
      try {
        Date start = previous.paused() ? null : getRestartFire(previous.trigger());
        if (start == null) {
          log.warn("[SCHEDULER][TASK {}] The previous trigger {} is not restored ({}): the task is left without trigger",
            taskId, previous.trigger().getKey(), previous.paused() ? "it was paused" : "it does not fire again");
          continue;
        }
        Trigger trigger = previous.trigger().getTriggerBuilder().startAt(start).build();
        scheduler.scheduleJob(previous.job(), trigger);
        log.warn("[SCHEDULER][TASK {}] The previous trigger {} has been restored from {}", taskId,
          previous.trigger().getKey(), start);
      } catch (SchedulerException exc) {
        log.error("[SCHEDULER][TASK {}] The previous trigger {} could not be restored", taskId,
          previous.trigger().getKey(), exc);
        error.addSuppressed(exc);
      }
    }
  }

  /**
   * Fire a restored trigger starts at: its previous next fire, or its first fire after now when that one has passed
   */
  private Date getRestartFire(Trigger trigger) throws SchedulerException {
    Date next = trigger.getNextFireTime();
    return next != null && next.toInstant().isAfter(clock.instant()) ? next : getFireAfterNow(trigger);
  }

  /**
   * Job removed from the scheduler with its trigger and whether the trigger was paused
   */
  private record RemovedJob(JobDetail job, Trigger trigger, boolean paused) {
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
    if (!TaskUtil.isLaunchedOnDemand(task.getLaunchType())) {
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
