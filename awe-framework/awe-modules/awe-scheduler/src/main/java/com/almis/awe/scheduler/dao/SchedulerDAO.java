package com.almis.awe.scheduler.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.Engine;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineStatus;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineTrigger;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_VISIBLE;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;

@Slf4j
public class SchedulerDAO extends ServiceConfig {

  private static final String SCHEDULER_STATUS = "schedulerStatus";
  private static final String SCHEDULER_STATUS_CODE = "schedulerStatusCode";
  private static final String STATUS_STARTED = "started";
  private static final String STATUS_STANDBY = "standby";
  private static final String STATUS_STOPPED = "stopped";
  private static final String ENGINE_LINE_PREFIX = "SCREEN_TEXT_ACTIVITY_ENGINE_LINE_";
  // Instance id Quartz gives every scheduler that is not clustered
  private static final String NON_CLUSTERED_INSTANCE = "NON_CLUSTERED";
  private static final List<String> STARTUP_HARDENED_GROUPS = Arrays.asList(SCHEDULED_GROUP, FILE_TRACKING_GROUP);
  private static final long STARTUP_SWEEP_GUARD_MILLIS = 1000L;
  /**
   * Most fire times of one trigger counted by the upcoming count (one per minute in 24 hours): a trigger that fires
   * more often counts this many, so the count of a window is bounded and never builds the list of its fire times
   */
  public static final int UPCOMING_FIRE_TIMES_CAP = 1440;
  /**
   * Most fire times of one trigger that its calendar excludes visited by the upcoming count, apart from the counted
   * ones (one every 10 seconds in 24 hours): an excluded fire time does not use the cap of the counted ones, and past
   * this budget the trigger stops counting
   */
  public static final int UPCOMING_EXCLUDED_FIRE_TIMES_BUDGET = 8640;
  private static final Set<Trigger.TriggerState> NOT_FIRING_STATES = EnumSet.of(Trigger.TriggerState.PAUSED,
    Trigger.TriggerState.ERROR, Trigger.TriggerState.COMPLETE, Trigger.TriggerState.NONE);

  private final boolean loadSchedulerTasks;
  private final StartupHardeningConfig startupHardeningConfig;

  // Autowired services
  private final Scheduler scheduler;
  private final CalendarDAO calendarDAO;
  private final TaskService taskService;
  private final SchedulerTriggerListener triggerListener;
  private final SchedulerJobListener jobListener;
  // Instant of the last start or resume of the engine by this instance: Quartz keeps the one of its first start
  // through standby, start and clear, so it cannot tell how long the engine has been running since it was resumed
  private volatile Instant lastStartedAt;
  // The last start of the engine failed: told by the configuration screen until it starts again
  private volatile boolean lastStartFailed;

  /**
   * Constructor
   * @param scheduler           Scheduler
   * @param loadSchedulerTasks  Scheduler load task flag
   * @param calendarDAO         Calendar DAO
   * @param taskService         Task service
   * @param triggerListener     Trigger listener
   * @param jobListener         Job listener
   */
  public SchedulerDAO(Scheduler scheduler, boolean loadSchedulerTasks, CalendarDAO calendarDAO, TaskService taskService,
                      SchedulerTriggerListener triggerListener, SchedulerJobListener jobListener) {
    this(scheduler, loadSchedulerTasks, calendarDAO, taskService, triggerListener, jobListener,
      new StartupHardeningConfig(Clock.systemDefaultZone(), STARTUP_SWEEP_GUARD_MILLIS));
  }

  SchedulerDAO(Scheduler scheduler, boolean loadSchedulerTasks, CalendarDAO calendarDAO, TaskService taskService,
                SchedulerTriggerListener triggerListener, SchedulerJobListener jobListener,
                StartupHardeningConfig startupHardeningConfig) {
    this.scheduler = scheduler;
    this.loadSchedulerTasks = loadSchedulerTasks;
    this.calendarDAO = calendarDAO;
    this.taskService = taskService;
    this.triggerListener = triggerListener;
    this.jobListener = jobListener;
    this.startupHardeningConfig = startupHardeningConfig;
  }

  /**
   * Start scheduler
   *
   * @throws SchedulerException
   */
  private String startScheduler() throws SchedulerException {
    scheduler.start();
    lastStartedAt = startupHardeningConfig.startupClock().instant();
    lastStartFailed = false;

    log.info("[SCHEDULER] The scheduler has been started");

    return getLocale("MESSAGE_SCHEDULER_STARTED");
  }

  /**
   * Prepare scheduler before loading calendars and tasks.
   *
   * @throws SchedulerException Scheduler exception
   */
  private void prepareSchedulerForLoading() throws SchedulerException {
    log.info("===== Starting scheduler tasks ========");

    // Add listeners to scheduler before resuming Quartz execution
    ensureSchedulerListeners();

    // Keep scheduler in standby while calendars and tasks are loaded
    if (scheduler.isStarted() && !scheduler.isInStandbyMode()) {
      scheduler.standby();
    }

    log.debug("[SCHEDULER] Scheduler prepared in standby mode for task loading");
  }

  /**
   * Register Quartz listeners once.
   *
   * @throws SchedulerException Scheduler exception
   */
  private void ensureSchedulerListeners() throws SchedulerException {
    ListenerManager listenerManager = scheduler.getListenerManager();

    if (listenerManager.getTriggerListeners().stream().noneMatch(listener -> triggerListener.getName().equals(listener.getName()))) {
      listenerManager.addTriggerListener(triggerListener);
    }

    if (listenerManager.getJobListeners().stream().noneMatch(listener -> jobListener.getName().equals(listener.getName()))) {
      listenerManager.addJobListener(jobListener);
    }

    log.debug("[SCHEDULER] Trigger and Job listeners added to the scheduler");
  }

  /**
   * Prevent startup-loaded planned tasks from firing immediately when their next fire time is due now or in the past.
   *
   * @throws SchedulerException Scheduler exception
   */
  private void hardenLoadedTasksBeforeStartup(Date startupCutoff) throws SchedulerException {
    for (String group : STARTUP_HARDENED_GROUPS) {
      hardenLoadedTaskGroupBeforeStartup(group, startupCutoff);
    }
  }

  private void hardenLoadedTaskGroupBeforeStartup(String group, Date startupCutoff) throws SchedulerException {
    for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group))) {
      hardenLoadedTriggerBeforeStartup(triggerKey, startupCutoff);
    }
  }

  private void hardenLoadedTriggerBeforeStartup(TriggerKey triggerKey, Date startupCutoff) throws SchedulerException {
    Trigger trigger = scheduler.getTrigger(triggerKey);

    if (shouldSkipStartupHardening(triggerKey, trigger, startupCutoff)) {
      return;
    }

    Date nextValidFireTime = trigger.getFireTimeAfter(startupCutoff);
    if (nextValidFireTime == null) {
      scheduler.deleteJob(trigger.getJobKey());
      log.info("[SCHEDULER][{}] Removed startup-loaded trigger without future fire time", triggerKey);
      return;
    }

    Trigger adjustedTrigger = trigger.getTriggerBuilder()
      .startAt(nextValidFireTime)
      .build();

    scheduler.rescheduleJob(triggerKey, adjustedTrigger);
    log.info("[SCHEDULER][{}] Deferred startup-loaded trigger to {}", triggerKey, nextValidFireTime);
  }

  private boolean shouldSkipStartupHardening(TriggerKey triggerKey, Trigger trigger, Date startupCutoff) throws SchedulerException {
    if (trigger == null) {
      return true;
    }

    if (Trigger.TriggerState.PAUSED.equals(scheduler.getTriggerState(triggerKey))) {
      log.debug("[SCHEDULER][{}] Skipped startup hardening because trigger is paused", triggerKey);
      return true;
    }

    Date nextFireTime = Optional.ofNullable(trigger.getNextFireTime()).orElse(trigger.getStartTime());
    return nextFireTime == null || nextFireTime.after(startupCutoff);
  }

  /**
   * Compute the startup hardening cutoff as close as possible to the actual scheduler resume boundary.
   *
   * @return Startup cutoff with guard window
   */
  private Date getStartupHardeningCutoff() {
    Instant startupCutoff = startupHardeningConfig.startupClock().instant()
      .plusMillis(startupHardeningConfig.startupSweepGuardMillis());
    return Date.from(startupCutoff);
  }

  record StartupHardeningConfig(Clock startupClock, long startupSweepGuardMillis) {

    StartupHardeningConfig {
      startupClock = Objects.requireNonNull(startupClock, "startupClock");
      startupSweepGuardMillis = Math.max(0L, startupSweepGuardMillis);
    }
  }

  /**
   * Start the scheduler service
   *
   * @throws AWException
   */
  public ServiceData start() throws AWException {
    return startNoQuartz();
  }

  /**
   * Start the scheduler service
   *
   * @throws AWException
   */
  public ServiceData startNoQuartz() throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      // Prepare scheduler and keep Quartz paused while data is loaded
      prepareSchedulerForLoading();

      // Load calendars from DB to the scheduler
      calendarDAO.loadSchedulerCalendar();

      log.debug("[SCHEDULER] Calendars loaded from database and added to the scheduler");

      // Update interrupted task status
      taskService.updateInterruptedTasks();

      // Check if tasks have to be loaded
      if (loadSchedulerTasks) {
        // Load Tasks from DB to scheduler
        taskService.loadSchedulerTasks();

        log.debug("[SCHEDULER] Tasks loaded from database and added to the scheduler");

        // Prevent startup-loaded planned tasks from firing immediately on startup
        hardenLoadedTasksBeforeStartup(getStartupHardeningCutoff());
      }

      // Start scheduler after calendars and tasks are loaded
      serviceData.setTitle(startScheduler());
    } catch (SchedulerException exc) {
      lastStartFailed = true;
      throw new AWException("Error trying to start the scheduler", exc);
    } catch (AWException | RuntimeException exc) {
      lastStartFailed = true;
      throw exc;
    }

    return serviceData;
  }

  /**
   * Stop the scheduler service
   *
   * @throws AWException
   */
  public ServiceData stop() throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      log.debug("[SCHEDULER] The scheduler has started shutting down");

      // Stop jobs that are currently been executed
      interruptCurrentlyExecutingJobs();

      // Shutdown the scheduler
      scheduler.standby();
      log.info("[SCHEDULER] The scheduler has been shutdown correctly");
      serviceData.setTitle(getLocale("MESSAGE_SCHEDULER_SHUTDOWN"));
    } catch (Exception exc) {
      throw new AWException("Error trying to shutdown the scheduler", exc);
    }

    return serviceData;
  }

  /**
   * Scheduler's emergency reboot method
   *
   * @throws AWException
   */
  public ServiceData restart() throws AWException {
    try {
      // Stop jobs that are currently been executed
      stop();
      return startNoQuartz();
    } catch (Exception exc) {
      throw new AWException("Error trying to restart the scheduler", exc);
    }
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   *
   * @return
   */
  public ServiceData clearAndStop() throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      scheduler.clear();
      stop();
      return serviceData.setTitle(getLocale("MESSAGE_SCHEDULER_STOPPED"));
    } catch (Exception exc) {
      throw new AWException("Error trying to shutdown the scheduler", exc);
    }
  }

  /*
   * *******************************************
   * SCHEDULER MANAGEMENT METHODS
   * *******************************************
   */

  /**
   * Get currently executing job instances from the scheduler isntance
   *
   * @return
   * @throws AWException
   */
  public ServiceData getCurrentlyExecutingJobs() throws AWException {
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    serviceData.setDataList(dataList);

    try {
      if (!isSchedulerUp()) {
        return serviceData;
      }

      List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
      for (JobExecutionContext executingJob : executingJobs) {
        Map<String, CellData> row = new HashMap<>();
        row.put("taskKey", new CellData(executingJob.getTrigger().getKey().getName()));
        row.put("triggerKey", new CellData(executingJob.getTrigger().getKey().toString()));
        row.put("calendar", new CellData(retrieveCalendarName(executingJob.getTrigger().getCalendarName())));
        row.put("executionTime", new CellData(executingJob.getFireTime() != null ? DateUtil.dat2WebTimestamp(executingJob.getFireTime()) : ""));
        row.put("scheduledExecutionTime", new CellData(executingJob.getScheduledFireTime() != null ? DateUtil.dat2WebTimestamp(executingJob.getScheduledFireTime()) : ""));
        row.put("nextExecutionTime", new CellData(executingJob.getNextFireTime() != null ? DateUtil.dat2WebTimestamp(executingJob.getNextFireTime()) : ""));
        row.put("previousExecutionTime", new CellData(executingJob.getPreviousFireTime() != null ? DateUtil.dat2WebTimestamp(executingJob.getPreviousFireTime()) : ""));
        row.put("jobRuntime", new CellData(executingJob.getJobRunTime()));
        row.put("jobStatus", new CellData(getTriggerState(executingJob.getTrigger().getKey().getGroup(), executingJob.getTrigger().getKey().getName())));
        dataList.addRow(row);
      }
      return serviceData;
    } catch (Exception e) {
      throw new AWException("[SCHEDULER-MANAGER] Error loading currently executing tasks", e);
    }
  }

  /**
   * Get configured
   *
   * @return
   * @throws AWException
   */
  public ServiceData getConfiguredJobs() throws AWException {
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    serviceData.setDataList(dataList);

    try {
      if (!isSchedulerUp()) {
        return serviceData;
      }

      // Retrieve all jobs but TIMEOUT ones
      List<Trigger> schedulerJobs = getAllJobs()
        .stream()
        .filter(t -> (boolean) Optional.ofNullable(t.getJobDataMap().get(TASK_VISIBLE)).orElse(false))
        .collect(Collectors.toList());

      for (Trigger currentTrigger : schedulerJobs) {
        Map<String, CellData> row = new HashMap<>();
        row.put("taskKey", new CellData(currentTrigger.getKey().getName()));
        row.put("triggerKey", new CellData(currentTrigger.getKey().toString()));
        row.put("taskCalendar", new CellData(retrieveCalendarName(currentTrigger.getCalendarName())));
        row.put("taskStartTime", new CellData(currentTrigger.getStartTime() == null ? "-" : DateUtil.dat2WebTimestamp(currentTrigger.getStartTime())));
        row.put("taskEndTime", new CellData(currentTrigger.getEndTime() == null ? "-" : DateUtil.dat2WebTimestamp(currentTrigger.getEndTime())));
        row.put("taskFinalExecutionTime", new CellData(currentTrigger.getFinalFireTime() == null ? "-" : DateUtil.dat2WebTimestamp(currentTrigger.getFinalFireTime())));
        row.put("taskNextExecutionTime", new CellData(currentTrigger.getNextFireTime() == null ? "-" : DateUtil.dat2WebTimestamp(currentTrigger.getNextFireTime())));
        row.put("taskPreviousExecutionTime", new CellData(currentTrigger.getPreviousFireTime() == null ? "-" : DateUtil.dat2WebTimestamp(currentTrigger.getPreviousFireTime())));
        dataList.addRow(row);
      }

      return serviceData;
    } catch (Exception exc) {
      throw new AWException("[SCHEDULER-MANAGER] Error loading currently executing tasks", exc);
    }
  }

  /**
   * Retrieve the readable calendar name for a trigger calendar name.
   * <p>
   * The trigger calendar name holds the calendar identifier (stored as a string).
   * The scheduler registers the AWE {@code Calendar} bean under that identifier,
   * so we retrieve it and return its human-readable name. If the calendar is not
   * found or is not an AWE calendar, the raw trigger calendar name is returned
   * as a fallback so the column never renders empty for an assigned calendar.
   *
   * @param triggerCalendarName Trigger calendar name (calendar identifier as string)
   * @return Calendar readable name, or empty string if the trigger has no calendar
   */
  private String retrieveCalendarName(String triggerCalendarName) {
    if (triggerCalendarName == null) {
      return "";
    }
    try {
      org.quartz.Calendar quartzCalendar = scheduler.getCalendar(triggerCalendarName);
      if (quartzCalendar instanceof com.almis.awe.scheduler.bean.calendar.Calendar aweCalendar) {
        return aweCalendar.getName();
      }
    } catch (SchedulerException exc) {
      log.warn("[SCHEDULER-MANAGER] Could not retrieve calendar '{}'", triggerCalendarName, exc);
    }
    return triggerCalendarName;
  }

  /**
   * Returns the job group names
   *
   * @return
   * @throws AWException
   */
  private List<Trigger> getAllJobs() throws AWException {

    List<Trigger> triggerDetails = new ArrayList<>();
    try {
      List<String> triggerGroupNames = scheduler.getTriggerGroupNames();

      for (String triggerGroupName : triggerGroupNames) {
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupName));
        for (TriggerKey triggerKey : triggerKeys) {
          triggerDetails.add(scheduler.getTrigger(triggerKey));
        }
      }
    } catch (Exception e) {
      throw new AWException(e.getLocalizedMessage(), e);
    }

    return triggerDetails;
  }

  /**
   * Returns information about the configured scheduler
   *
   * @return
   * @throws AWException
   */
  public ServiceData getSchedulerMetadata() throws AWException {
    ServiceData serviceData = new ServiceData();

    try {
      DataList dataList = new DataList();
      Map<String, CellData> row = new HashMap<>();
      SchedulerMetaData metadata = scheduler.getMetaData();
      row.put("schedulerInstanceId", new CellData(metadata.getSchedulerInstanceId()));
      row.put("schedulerName", new CellData(metadata.getSchedulerName()));
      row.put("schedulerVersion", new CellData(metadata.getVersion()));
      row.put("schedulerRemote", new CellData(String.valueOf(metadata.isSchedulerRemote())));
      row.put("schedulerJobstoreClass", new CellData(metadata.getJobStoreClass().getName()));
      row.put("schedulerJobstoreCluster", new CellData(String.valueOf(metadata.isJobStoreClustered())));
      row.put("schedulerJobstorePersistence", new CellData(String.valueOf(metadata.isJobStoreSupportsPersistence())));
      row.put("schedulerExecutedJobs", new CellData(metadata.getNumberOfJobsExecuted()));
      Date runningSince = getRunningSince(metadata);
      row.put("schedulerRunningSince", new CellData(runningSince == null ? "Stopped" : DateUtil.dat2WebTimestamp(runningSince)));
      row.put("schedulerThreadPoolClass", new CellData(metadata.getThreadPoolClass().getName()));
      row.put("schedulerThreadPoolSize", new CellData(metadata.getThreadPoolSize()));

      String schedulerStatus = "Stopped";
      String schedulerStatusCode = STATUS_STOPPED;
      // A scheduler that has been shut down stays in standby mode: it is stopped
      boolean shutdown = metadata.isShutdown();
      if (!shutdown && metadata.isInStandbyMode()) {
        schedulerStatus = "Standby";
        schedulerStatusCode = STATUS_STANDBY;
      } else if (!shutdown && metadata.isStarted()) {
        schedulerStatus = "Started and running";
        schedulerStatusCode = STATUS_STARTED;
      }
      row.put(SCHEDULER_STATUS, new CellData(schedulerStatus));
      // Language independent code of the status, for the screens that translate it
      row.put(SCHEDULER_STATUS_CODE, new CellData(schedulerStatusCode));
      // The line of the engine under its status, in the language of the user: its instance and how long it has been
      // running (only while it runs)
      String runningFor = STATUS_STARTED.equals(schedulerStatusCode) && runningSince != null
        ? formatRunningTime(Duration.between(runningSince.toInstant(), startupHardeningConfig.startupClock().instant()))
        : "";
      row.put("schedulerRunningFor", new CellData(runningFor));
      row.put("schedulerEngineLine", new CellData(getLocale(ENGINE_LINE_PREFIX + schedulerStatusCode.toUpperCase(Locale.ROOT),
        getInstanceName(metadata), runningFor)));

      dataList.addRow(row);
      serviceData.setDataList(dataList);

      metadata.getSummary();
    } catch (Exception e) {
      throw new AWException(e.getLocalizedMessage(), e);
    }
    return serviceData;
  }

  /**
   * The engine for the «Motor» tab of the configuration screen: its status (running, paused in standby, or stopped
   * when it never started or was shut down), its threads in use (its running jobs), its data and whether its last
   * start failed
   *
   * @return The engine
   * @throws AWException The scheduler cannot be read
   */
  public Engine getEngine() throws AWException {
    try {
      SchedulerMetaData metadata = scheduler.getMetaData();
      EngineStatus status = EngineStatus.STOPPED;
      if (!metadata.isShutdown() && metadata.isStarted()) {
        status = metadata.isInStandbyMode() ? EngineStatus.PAUSED : EngineStatus.RUNNING;
      }
      int threadsInUse = metadata.isShutdown() ? 0 : scheduler.getCurrentlyExecutingJobs().size();
      return new Engine(getInstanceName(metadata), metadata.getVersion(), metadata.getJobStoreClass().getName(),
        metadata.isJobStoreClustered(), metadata.isJobStoreSupportsPersistence(),
        metadata.getThreadPoolClass().getName(), metadata.getThreadPoolSize(), threadsInUse,
        getRunningSince(metadata), metadata.getNumberOfJobsExecuted(), status, lastStartFailed,
        metadata.isSchedulerRemote());
    } catch (SchedulerException | RuntimeException exc) {
      throw new AWException("[SCHEDULER-MANAGER] Error reading the engine", exc);
    }
  }

  /**
   * The schedules the engine holds: the triggers of the tasks with a schedule of their own (scheduled, or checking
   * their files), whatever their state. The triggers of a launch on demand (manual, webhook, dependency) and the
   * internal ones (timeout and progress of a running execution) are not schedules. Nothing when it was shut down
   *
   * @return The schedules
   * @throws AWException The scheduler cannot be read
   */
  public List<EngineTrigger> getEngineTriggers() throws AWException {
    List<EngineTrigger> triggers = new ArrayList<>();
    try {
      if (scheduler.isShutdown()) {
        return triggers;
      }
      for (String group : STARTUP_HARDENED_GROUPS) {
        for (TriggerKey key : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group))) {
          getEngineTrigger(key, group).ifPresent(triggers::add);
        }
      }
    } catch (SchedulerException | RuntimeException exc) {
      throw new AWException("[SCHEDULER-MANAGER] Error reading the engine schedules", exc);
    }
    return triggers;
  }

  /**
   * One schedule of the engine: a trigger that cannot be read (its data or its state) is left out and logged, so it does
   * not hide the others
   */
  private Optional<EngineTrigger> getEngineTrigger(TriggerKey key, String group) {
    try {
      Trigger trigger = scheduler.getTrigger(key);
      if (trigger == null) {
        return Optional.empty();
      }
      return Optional.of(new EngineTrigger(taskId(key.getName()), group, trigger.getCalendarName(),
        trigger.getPreviousFireTime(), trigger.getNextFireTime(), scheduler.getTriggerState(key).name()));
    } catch (SchedulerException | RuntimeException exc) {
      log.warn("[SCHEDULER-MANAGER] The schedule {} of the engine could not be read: left out", key, exc);
      return Optional.empty();
    }
  }

  /**
   * Task of a trigger key, null when it is not a task identifier
   */
  private static Integer taskId(String name) {
    try {
      return Integer.valueOf(name);
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  /**
   * Instant since the engine runs: its last start or resume by this instance, else the start Quartz keeps (an engine
   * started before this instance started it, or by the application start up)
   *
   * @param metadata Scheduler metadata
   * @return Running since, null when Quartz has never started
   */
  private Date getRunningSince(SchedulerMetaData metadata) {
    Instant started = lastStartedAt;
    return started != null ? Date.from(started) : metadata.getRunningSince();
  }

  /**
   * Name of the engine instance: its instance id, or the scheduler name when it is not clustered (every scheduler that
   * is not clustered has the same instance id)
   *
   * @param metadata Scheduler metadata
   * @return Instance name
   */
  private String getInstanceName(SchedulerMetaData metadata) {
    String instanceId = metadata.getSchedulerInstanceId();
    return instanceId == null || instanceId.isBlank() || NON_CLUSTERED_INSTANCE.equals(instanceId)
      ? metadata.getSchedulerName() : instanceId;
  }

  /**
   * Time the engine has been running, in the two largest units in the language of the user: «6 d 4 h», «3 h 12 min»,
   * «7 min» or «45 s»
   *
   * @param runningTime Running time
   * @return Running time text
   */
  private String formatRunningTime(Duration runningTime) {
    Duration time = runningTime.isNegative() ? Duration.ZERO : runningTime;
    if (time.toDays() > 0) {
      return getLocale("SCREEN_TEXT_ACTIVITY_DURATION_DAYS", String.valueOf(time.toDays()), String.valueOf(time.toHoursPart()));
    } else if (time.toHours() > 0) {
      return getLocale("SCREEN_TEXT_ACTIVITY_DURATION_HOURS", String.valueOf(time.toHours()), String.valueOf(time.toMinutesPart()));
    } else if (time.toMinutes() > 0) {
      return getLocale("SCREEN_TEXT_ACTIVITY_DURATION_MINUTES", String.valueOf(time.toMinutes()));
    }
    return getLocale("SCREEN_TEXT_ACTIVITY_DURATION_SECONDS", String.valueOf(time.getSeconds()));
  }

  /**
   * Count the fire times of the visible triggers in a window starting now, as the scheduler fires them: paused (also
   * paused while blocked), finished and in error triggers do not fire, a blocked trigger fires once its job ends, and
   * the calendar of each trigger excludes its dates. Each trigger counts at most {@link #UPCOMING_FIRE_TIMES_CAP} fire
   * times, visited one by one, skipping at most {@link #UPCOMING_EXCLUDED_FIRE_TIMES_BUDGET} that its calendar
   * excludes. Nothing fires while the scheduler is not running
   *
   * @param window Window from now
   * @return Number of fire times in the window
   * @throws AWException Error reading the triggers
   */
  public long countUpcomingFireTimes(Duration window) throws AWException {
    try {
      if (!isSchedulerUp()) {
        return 0L;
      }

      Instant now = startupHardeningConfig.startupClock().instant();
      Date from = Date.from(now);
      Date to = Date.from(now.plus(window));
      long count = 0L;
      for (Trigger trigger : getAllJobs()) {
        if (isUpcoming(trigger)) {
          org.quartz.Calendar calendar = trigger.getCalendarName() == null ? null
            : scheduler.getCalendar(trigger.getCalendarName());
          count += countFireTimes(trigger, calendar, from, to);
        }
      }
      return count;
    } catch (SchedulerException exc) {
      throw new AWException("[SCHEDULER-MANAGER] Error counting the upcoming fire times", exc);
    }
  }

  /**
   * Count the fire times of a trigger in a window, included its limits, skipping the ones its calendar excludes. The
   * fire times are visited one by one: at most {@link #UPCOMING_FIRE_TIMES_CAP} are counted and at most
   * {@link #UPCOMING_EXCLUDED_FIRE_TIMES_BUDGET} excluded ones are skipped
   *
   * @param trigger  Trigger
   * @param calendar Calendar of the trigger, null without calendar
   * @param from     Window start
   * @param to       Window end
   * @return Fire times in the window
   */
  private long countFireTimes(Trigger trigger, org.quartz.Calendar calendar, Date from, Date to) {
    long count = 0L;
    int excluded = 0;
    Date fireTime = trigger.getFireTimeAfter(new Date(from.getTime() - 1));
    while (fireTime != null && !fireTime.after(to) && count < UPCOMING_FIRE_TIMES_CAP
      && excluded < UPCOMING_EXCLUDED_FIRE_TIMES_BUDGET) {
      if (calendar == null || calendar.isTimeIncluded(fireTime.getTime())) {
        count++;
      } else {
        excluded++;
      }
      fireTime = trigger.getFireTimeAfter(fireTime);
    }
    return count;
  }

  /**
   * Check if a trigger fires again: a visible task trigger with a next fire time whose state lets it fire (not paused,
   * finished nor in error). A visibility that is not a boolean is read as its text ("true")
   *
   * @param trigger Trigger
   * @return The trigger fires again
   * @throws SchedulerException Error reading the trigger state
   */
  private boolean isUpcoming(Trigger trigger) throws SchedulerException {
    return Boolean.parseBoolean(String.valueOf(trigger.getJobDataMap().get(TASK_VISIBLE)))
      && trigger.getNextFireTime() != null
      && !NOT_FIRING_STATES.contains(scheduler.getTriggerState(trigger.getKey()));
  }

  private boolean isSchedulerUp() throws SchedulerException {
    return !scheduler.isInStandbyMode() && scheduler.isStarted();
  }

  /**
   * Get the trigger state for the given group and trigger key
   *
   * @param group
   * @param key
   * @return
   * @throws AWException
   */
  public String getTriggerState(String group, String key) throws AWException {
    try {
      return scheduler.getTriggerState(new TriggerKey(key, group)).name();
    } catch (SchedulerException exc) {
      throw new AWException(getLocale("[SCHEDULER] Error trying to retrieve trigger state from group: {} and key {}", group, key), exc);
    }
  }

  /**
   * Interrupt currently executing jobs
   */
  private void interruptCurrentlyExecutingJobs() throws SchedulerException {
    // Iterate through and stop all the executing jobs
    for (JobExecutionContext job : scheduler.getCurrentlyExecutingJobs()) {
      scheduler.interrupt(job.getJobDetail().getKey());
      log.debug("[SCHEDULER][TASK_QUERY {}] The task has been interrupted", job.getTrigger().getKey());
    }
  }

  /**
   * Stop the running jobs of a task on this node
   *
   * @param taskId Task identifier
   * @return false when a running job of the task could not be stopped
   * @throws SchedulerException Error reading the executing jobs
   */
  public boolean stopRunningJobs(Integer taskId) throws SchedulerException {
    boolean allStopped = true;
    for (JobExecutionContext executing : scheduler.getCurrentlyExecutingJobs()) {
      if (executing.getJobInstance() instanceof SchedulerJob job && job.getTask() != null
        && taskId.equals(job.getTask().getTaskId())) {
        allStopped &= job.stop();
        log.info("[SCHEDULER] Stop requested by the user for task {}", taskId);
      }
    }
    return allStopped;
  }
}
