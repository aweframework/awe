package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_DATES;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_ROWS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_ENGINE_TASKS;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;

/**
 * «Motor» tab of the scheduler configuration screen (#993): the card of the engine and the schedules it holds, in the
 * words of the user. The engine data (its status, its threads and its triggers) are read by the scheduler; this
 * presenter adds the tasks, the calendars and the executions waiting for a confirmation from the database. Pure
 * presentation: the texts come from the locales, so the user language is applied
 */
@Slf4j
public class SchedulerEnginePresenter {

  private static final String TEXT = "SCREEN_TEXT_SCHEDULER_ENGINE_";
  private static final String NO_VALUE = TaskWhenSummary.EMPTY_VALUE;
  private static final String FILTER_PAUSED = "paused";
  private static final String FILTER_ERROR = "error";
  private static final Set<String> FILTERS = Set.of(FILTER_PAUSED, FILTER_ERROR);
  private static final int MOST_EXCLUDED_DAYS = 3;
  private static final int MAINTAIN = 1;
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
  private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String STATUS_UNAVAILABLE = "unavailable";
  private static final String ENGINE_STORE = "EngStoTxt";
  private static final String ENGINE_SINCE_AGO = "EngSinAgo";
  private static final String ENGINE_SINCE = "EngSin";
  private static final String ENGINE_STATUS = "EngSta";
  private static final String STATUS_RUNNING = "running";
  private static final String ENGINE_INSTANCE_TEXT = "EngInsTxt";
  private static final String ENGINE_INSTANCE = "EngIns";
  private static final String ENGINE_VERSION = "EngVer";
  private static final String ENGINE_THREADS = "EngThrTxt";
  /**
   * Facts of the card, in the order of the mock-up: label key, field of the value and field of its hint (or the locale
   * key of a fixed hint), empty without one. The hints stay short: the long description of the job store is told in
   * the technical details. The running since fact is told by the status of the engine (since)
   */
  private static final String FACT_SINCE = "FACT_SINCE";
  private static final List<String[]> FACTS = List.of(
    new String[]{"FACT_INSTANCE", ENGINE_INSTANCE, ENGINE_INSTANCE_TEXT},
    new String[]{"FACT_VERSION", ENGINE_VERSION, ""},
    new String[]{"FACT_STORE", ENGINE_STORE, ""},
    new String[]{"FACT_THREADS", ENGINE_THREADS, ""},
    new String[]{FACT_SINCE, ENGINE_SINCE, ENGINE_SINCE_AGO},
    new String[]{"FACT_EXECUTED", "EngExe", TEXT + "EXECUTED_SINCE_START"});

  /**
   * Status of the engine as the scheduler reads it: running, paused (in standby, it keeps its schedules) or stopped
   * (never started, or shut down)
   */
  public enum EngineStatus {
    RUNNING, PAUSED, STOPPED
  }

  /**
   * What the scheduler tells of its engine
   *
   * @param instance        Name of the instance (its id, or the scheduler name when it is not clustered)
   * @param version         Quartz version
   * @param jobStore        Class of the job store
   * @param clustered       The job store is clustered
   * @param persistent      The job store keeps the jobs in a database
   * @param threadPool      Class of the thread pool
   * @param poolSize        Threads of the pool
   * @param threadsInUse    Jobs running now (one thread each)
   * @param runningSince    Last start or resume, null when it never started
   * @param executedJobs    Jobs executed since it started
   * @param status          Status
   * @param lastStartFailed The last start of the engine failed
   * @param remote          Quartz reaches the scheduler through RMI
   */
  public record Engine(String instance, String version, String jobStore, boolean clustered, boolean persistent,
                       String threadPool, int poolSize, int threadsInUse, Date runningSince, int executedJobs,
                       EngineStatus status, boolean lastStartFailed, boolean remote) {
  }

  /**
   * A trigger of a task with a schedule of its own (scheduled, or checking its files)
   *
   * @param taskId       Task, null when the key of the trigger is not a task
   * @param group        Group of the trigger (scheduled or file tracking)
   * @param calendar     Calendar of the trigger (its identifier), null without one
   * @param previousFire Previous fire time, null when it has not fired
   * @param nextFire     Next fire time, null when it does not fire again
   * @param state        Quartz state of the trigger (NORMAL, PAUSED, BLOCKED, ERROR, COMPLETE, NONE)
   */
  public record EngineTrigger(Integer taskId, String group, String calendar, Date previousFire, Date nextFire,
                              String state) {
  }

  /**
   * Executions waiting for the confirmation of a deferred answer
   */
  @FunctionalInterface
  public interface DeferredSource {
    /**
     * Running deferred executions
     *
     * @return The executions
     * @throws AWException They cannot be read
     */
    List<DeferredExecution> waiting() throws AWException;
  }

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final ServiceConfig serviceConfig;
  private final Clock clock;
  private final DeferredSource deferred;

  /**
   * Constructor
   *
   * @param queryService  Query service
   * @param queryUtil     Query utilities
   * @param serviceConfig Service configuration giving access to the locales
   * @param clock         Clock of the relative times
   * @param deferred      Executions waiting for a confirmation
   */
  public SchedulerEnginePresenter(QueryService queryService, QueryUtil queryUtil, ServiceConfig serviceConfig,
                                  Clock clock, DeferredSource deferred) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
    this.deferred = deferred;
  }

  /**
   * Card of the engine, one row: EngSta (running, paused, paused-empty, stopped or unavailable), EngTxt (the sentence
   * that tells it), EngErr (the last start failed, empty when it did not), EngIns and EngInsTxt (instance and where it
   * runs), EngVer, EngStoTxt and EngStoDet (job store), EngThrUse, EngThrSiz, EngThrTxt and EngThrPct (threads in use),
   * EngSin and EngSinAgo (running since), EngExe (jobs executed), EngSch and EngSchTit (schedules, and the title of
   * their list), EngTec (technical details), EngTil (the facts of the card as its tiles), EngDfrCnt and EngDfrTxt
   * (executions waiting for a confirmation, the nearest deadline first)
   *
   * @param engine   Engine, null when the scheduler cannot be read
   * @param triggers Schedules of the engine
   * @return One row
   * @throws AWException The tasks waiting for a confirmation cannot be read
   */
  public ServiceData getEngine(Engine engine, List<EngineTrigger> triggers) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    LocalDateTime now = LocalDateTime.now(clock);
    String status = status(engine, triggers);
    row.put(ENGINE_STATUS, new CellData(status));
    row.put("EngStaTxt", new CellData(TEXT + "STATUS_" + code(status)));
    row.put("EngSch", new CellData(triggers.size()));
    row.put("EngSchTit", new CellData(text(TEXT + "SCHEDULES_TITLE", String.valueOf(triggers.size()))));
    row.put("EngErr", new CellData(engine != null && engine.lastStartFailed() ? text(TEXT + "START_ERROR") : ""));
    if (engine == null) {
      row.put("EngTxt", new CellData(text(TEXT + "SUMMARY_UNAVAILABLE")));
      for (String field : List.of(ENGINE_INSTANCE, ENGINE_INSTANCE_TEXT, ENGINE_VERSION, ENGINE_STORE, "EngStoDet",
        ENGINE_THREADS, ENGINE_SINCE, ENGINE_SINCE_AGO, "EngTec")) {
        row.put(field, new CellData(""));
      }
    } else {
      putEngine(row, engine, status, triggers.size(), now);
    }
    putDeferred(row, now);
    row.put("EngTil", new CellData(tiles(row, this::text)));
    DataList dataList = new DataList();
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Schedules of the engine, one row per trigger, the next one to fire first: IdeTsk, TskNam, TskRun (what it runs),
   * TrgKnd and TrgKndTxt (scheduled or file), TrgWhn («When» summary), CalIde, CalNam and CalExc (its calendar and the
   * next days it excludes), PrvFir, NxtFir and NxtFirIn (its fire times), TrgSta and TrgStaTxt (its state)
   *
   * @param triggers Schedules of the engine
   * @param filter   paused or error; every schedule when blank
   * @return The rows
   * @throws AWException The filter is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getTriggers(List<EngineTrigger> triggers, String filter) throws AWException {
    validateFilter(filter);
    String state = isBlank(filter) ? null : filter.trim();
    Map<Integer, Map<String, CellData>> tasks = getTasks();
    Map<String, String> calendars = new HashMap<>();
    for (Map<String, CellData> calendar : rows(SCHEDULER_CONFIGURATION_CALENDAR_ROWS)) {
      calendars.put(String.valueOf(integer(calendar, "IdeCal")), Optional.ofNullable(text(calendar, "Nom")).orElse(""));
    }
    Map<String, List<LocalDate>> dates = new HashMap<>();
    LocalDateTime now = LocalDateTime.now(clock);
    for (Map<String, CellData> date : rows(SCHEDULER_CONFIGURATION_CALENDAR_DATES)) {
      CellData day = date.get("Dat");
      if (day != null && day.getObjectValue() != null) {
        dates.computeIfAbsent(String.valueOf(integer(date, "IdeCal")), key -> new ArrayList<>())
          .add(LegacyDateConverter.asLocalDate(day.getDateValue()));
      }
    }
    TaskWhenSummary summary = new TaskWhenSummary(this::text);
    DataList dataList = new DataList();
    triggers.stream()
      .filter(trigger -> state == null || state.equals(stateCode(trigger.state())))
      .sorted(Comparator.comparing(EngineTrigger::nextFire, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(EngineTrigger::taskId, Comparator.nullsLast(Comparator.naturalOrder())))
      .forEach(trigger -> dataList.addRow(getTrigger(trigger, tasks.get(trigger.taskId()), calendars, dates, summary,
        now)));
    int id = 1;
    for (Map<String, CellData> row : dataList.getRows()) {
      row.put("id", new CellData(id++));
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Options of the state filter of the schedules with their counts: every schedule, the paused ones and the ones in
   * error
   *
   * @param triggers Schedules of the engine
   * @return One row per option: value, label, count and tone
   */
  public ServiceData getTriggerCounts(List<EngineTrigger> triggers) {
    DataList dataList = new DataList();
    dataList.addRow(count("", TEXT + "FILTER_ALL", triggers.size()));
    dataList.addRow(count(FILTER_PAUSED, TEXT + "FILTER_PAUSED",
      triggers.stream().filter(trigger -> FILTER_PAUSED.equals(stateCode(trigger.state()))).count()));
    dataList.addRow(count(FILTER_ERROR, TEXT + "FILTER_ERROR",
      triggers.stream().filter(trigger -> FILTER_ERROR.equals(stateCode(trigger.state()))).count()));
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Validate the state filter of the schedules: a value that is not valid is refused, never ignored
   *
   * @param filter paused or error; every schedule when blank
   * @throws AWException The filter is not valid (InvalidExecutionFilterException)
   */
  public void validateFilter(String filter) throws AWException {
    if (!isBlank(filter) && !FILTERS.contains(filter.trim())) {
      // The value is not echoed: it comes from the request
      throw new InvalidExecutionFilterException(text("SCHEDULER_INVALID_FILTER_TITLE"),
        text("SCHEDULER_INVALID_FILTER_VALUE_MESSAGE", "state"));
    }
  }

  /**
   * Facts of the card of the engine as the tiles of a card (a JSON array of label, value and hint): instance, version,
   * job store, threads in use, running since and jobs executed; none when the engine cannot be read. A remote engine
   * tells them again once it is told as remote
   *
   * @param row    Row of the card, with its facts
   * @param locale Text of a locale key in the language of the user
   * @return The tiles, as a JSON array
   */
  public static String tiles(Map<String, CellData> row, UnaryOperator<String> locale) {
    ArrayNode tiles = JsonNodeFactory.instance.arrayNode();
    if (STATUS_UNAVAILABLE.equals(text(row, ENGINE_STATUS))) {
      return tiles.toString();
    }
    for (String[] fact : FACTS) {
      ObjectNode tile = tiles.addObject();
      if (FACT_SINCE.equals(fact[0]) && !STATUS_RUNNING.equals(text(row, ENGINE_STATUS))) {
        since(tile, row, locale);
        continue;
      }
      tile.put("label", locale.apply(TEXT + fact[0]));
      tile.put("value", Optional.ofNullable(text(row, fact[1])).orElse(NO_VALUE));
      String hint = fact[2].startsWith(TEXT) ? locale.apply(fact[2]) : text(row, fact[2]);
      if (!isBlank(hint)) {
        tile.put("hint", hint);
      }
    }
    return tiles.toString();
  }

  /**
   * Tile of the running time of an engine that does not run: Quartz does not tell since when it is paused or stopped,
   * so the tile tells its status («En pausa», «Detenido») and its last start under it, never «En marcha desde»
   */
  private static void since(ObjectNode tile, Map<String, CellData> row, UnaryOperator<String> locale) {
    boolean paused = Optional.ofNullable(text(row, ENGINE_STATUS)).orElse("").startsWith(FILTER_PAUSED);
    tile.put("label", locale.apply(TEXT + "FACT_STATUS"));
    tile.put("value", locale.apply(TEXT + (paused ? "TILE_PAUSED" : "STATUS_STOPPED")));
    String started = text(row, ENGINE_SINCE);
    if (started != null && !NO_VALUE.equals(started)) {
      tile.put("hint", MessageFormat.format(locale.apply(TEXT + "STARTED_ON"), started));
    }
  }

  /**
   * Status of the card: a paused engine without schedules is told apart, it has nothing to resume
   */
  private static String status(Engine engine, List<EngineTrigger> triggers) {
    if (engine == null) {
      return STATUS_UNAVAILABLE;
    }
    return switch (engine.status()) {
      case RUNNING -> STATUS_RUNNING;
      case PAUSED -> triggers.isEmpty() ? "paused-empty" : FILTER_PAUSED;
      case STOPPED -> "stopped";
    };
  }

  private void putEngine(Map<String, CellData> row, Engine engine, String status, int schedules, LocalDateTime now) {
    LocalDateTime since = engine.runningSince() == null ? null
      : LocalDateTime.ofInstant(engine.runningSince().toInstant(), clock.getZone());
    String runningFor = since == null ? ""
      : duration(Duration.between(since.atZone(zone()), now.atZone(zone())));
    String threads = text(TEXT + "THREADS_OF", String.valueOf(engine.threadsInUse()),
      String.valueOf(engine.poolSize()));
    String scheduleCount = schedules == 1 ? text(TEXT + "SCHEDULES_ONE")
      : text(TEXT + "SCHEDULES_MANY", String.valueOf(schedules));
    String threadsUsed = switch (engine.threadsInUse()) {
      case 0 -> text(TEXT + "THREADS_NONE");
      case 1 -> text(TEXT + "THREADS_ONE");
      default -> text(TEXT + "THREADS_MANY", String.valueOf(engine.threadsInUse()));
    };
    String sentence = switch (status) {
      case STATUS_RUNNING -> text(TEXT + "SUMMARY_RUNNING", engine.instance(), runningFor, scheduleCount, threadsUsed,
        String.valueOf(engine.threadsInUse()), String.valueOf(engine.poolSize()));
      case FILTER_PAUSED -> text(TEXT + "SUMMARY_PAUSED", engine.instance(), scheduleCount);
      case "paused-empty" -> text(TEXT + "SUMMARY_PAUSED_EMPTY", engine.instance());
      default -> text(TEXT + "SUMMARY_STOPPED", engine.instance());
    };
    row.put("EngTxt", new CellData(sentence));
    row.put(ENGINE_INSTANCE, new CellData(engine.instance()));
    row.put(ENGINE_INSTANCE_TEXT, new CellData(text(TEXT + "INSTANCE_LOCAL")));
    row.put(ENGINE_VERSION, new CellData(text(TEXT + "VERSION", engine.version())));
    row.put(ENGINE_STORE, new CellData(text(TEXT + (engine.persistent() ? "STORE_DATABASE" : "STORE_MEMORY"))));
    String cluster = text(TEXT + (engine.clustered() ? "STORE_CLUSTERED" : "STORE_NOT_CLUSTERED"));
    row.put("EngStoDet", new CellData(engine.persistent() ? cluster
      : cluster + " · " + text(TEXT + "STORE_LOADS_TASKS")));
    row.put("EngThrUse", new CellData(engine.threadsInUse()));
    row.put("EngThrSiz", new CellData(engine.poolSize()));
    row.put(ENGINE_THREADS, new CellData(threads));
    row.put("EngThrPct", new CellData(engine.poolSize() <= 0 ? 0
      : Math.min(100, engine.threadsInUse() * 100 / engine.poolSize())));
    row.put(ENGINE_SINCE, new CellData(since == null ? NO_VALUE : DATE_TIME.format(since)));
    row.put(ENGINE_SINCE_AGO, new CellData(since == null || engine.status() == EngineStatus.STOPPED ? ""
      : text(TEXT + "AGO", runningFor)));
    row.put("EngExe", new CellData(engine.executedJobs()));
    // The details tell what the job store does with the tasks: a store in memory loads them when it starts
    String technical = text(TEXT + "TECHNICAL", engine.jobStore(), engine.threadPool(),
      yesNo(engine.persistent()), yesNo(engine.clustered()), yesNo(engine.remote()));
    row.put("EngTec", new CellData(engine.persistent() ? technical
      : technical + " · " + text(TEXT + "STORE_LOADS_TASKS")));
  }

  /**
   * Executions waiting for a confirmation: how many, and the task and the deadline of the nearest one
   */
  private void putDeferred(Map<String, CellData> row, LocalDateTime now) throws AWException {
    List<DeferredExecution> waiting = deferred.waiting();
    row.put("EngDfrCnt", new CellData(waiting.size()));
    Optional<DeferredExecution> nearest = waiting.stream().filter(execution -> execution.getDueDate() != null)
      .min(Comparator.comparing(DeferredExecution::getDueDate));
    String sentence = "";
    if (!waiting.isEmpty()) {
      DeferredExecution first = nearest.orElse(waiting.get(0));
      String name = Optional.ofNullable(getTasks().get(first.getTaskId())).map(task -> text(task, "Nam"))
        .orElse(text(TEXT + "TASK_UNKNOWN", String.valueOf(first.getTaskId())));
      String deadline = NO_VALUE;
      if (first.getDueDate() != null) {
        LocalDateTime due = LocalDateTime.ofInstant(first.getDueDate().toInstant(), clock.getZone());
        deadline = (due.toLocalDate().equals(now.toLocalDate()) ? TIME : DAY_TIME).format(due);
      }
      sentence = text(TEXT + "DEFERRED", String.valueOf(waiting.size()), name, deadline);
    }
    row.put("EngDfrTxt", new CellData(sentence));
  }

  private Map<String, CellData> getTrigger(EngineTrigger trigger, Map<String, CellData> task,
                                           Map<String, String> calendars, Map<String, List<LocalDate>> dates,
                                           TaskWhenSummary summary, LocalDateTime now) {
    boolean file = FILE_TRACKING_GROUP.equals(trigger.group());
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", trigger.taskId() == null ? new CellData() : new CellData(trigger.taskId()));
    row.put("TskNam", new CellData(task == null ? text(TEXT + "TASK_UNKNOWN", String.valueOf(trigger.taskId()))
      : Optional.ofNullable(text(task, "Nam")).orElse("")));
    row.put("TskRun", new CellData(task == null ? "" : runs(task)));
    row.put("TrgKnd", new CellData(file ? "file" : "scheduled"));
    row.put("TrgKndTxt", new CellData(TEXT + (file ? "KIND_FILE" : "KIND_SCHEDULED")));
    row.put("TrgWhn", new CellData(task == null ? NO_VALUE : summary.when(getLaunch(task)).text()));
    String calendar = trigger.calendar();
    row.put("CalIde", calendar == null ? new CellData() : new CellData(calendar));
    row.put("CalNam", new CellData(calendar == null ? text(TEXT + "NO_CALENDAR")
      : calendars.getOrDefault(calendar, calendar)));
    row.put("CalExc", new CellData(calendar == null ? "" : excluded(dates.getOrDefault(calendar, List.of()), now)));
    LocalDateTime previous = dateTime(trigger.previousFire());
    LocalDateTime next = dateTime(trigger.nextFire());
    row.put("PrvFir", new CellData(previous == null ? NO_VALUE : summary.relative(previous, now)));
    row.put("NxtFir", new CellData(next == null ? NO_VALUE : summary.relative(next, now)));
    row.put("NxtFirIn", new CellData(next == null || next.isBefore(now) ? ""
      : text(TEXT + "IN", duration(Duration.between(now.atZone(zone()), next.atZone(zone()))))));
    String state = stateCode(trigger.state());
    row.put("TrgSta", new CellData(state));
    row.put("TrgStaTxt", new CellData(TEXT + "STATE_" + code(state)));
    return row;
  }

  /**
   * The next days a calendar excludes, from today: «excluye 08/12 y 25/12»; empty when it excludes no day
   */
  private String excluded(List<LocalDate> days, LocalDateTime now) {
    LocalDate today = now.toLocalDate();
    List<String> next = days.stream().distinct().filter(day -> !day.isBefore(today)).sorted()
      .limit(MOST_EXCLUDED_DAYS).map(day -> (day.getYear() == today.getYear() ? DAY : FULL_DAY).format(day)).toList();
    if (next.isEmpty()) {
      return "";
    }
    String list = next.size() == 1 ? next.get(0) : text("SCREEN_TEXT_TASK_CATALOG_AND",
      String.join(", ", next.subList(0, next.size() - 1)), next.get(next.size() - 1));
    return text(TEXT + "CALENDAR_EXCLUDES", list);
  }

  /**
   * What a task runs: «Maintain X» or «Comando x»
   */
  private String runs(Map<String, CellData> task) {
    String command = Optional.ofNullable(text(task, "CmdExe")).orElse("");
    return text(TEXT + (Integer.valueOf(MAINTAIN).equals(integer(task, "TypExe")) ? "RUNS_MAINTAIN" : "RUNS_COMMAND"),
      command).trim();
  }

  /**
   * Language independent code of a Quartz trigger state
   */
  private static String stateCode(String state) {
    return switch (Optional.ofNullable(state).orElse("NONE")) {
      case "NORMAL" -> "normal";
      case "PAUSED" -> FILTER_PAUSED;
      case "BLOCKED" -> "blocked";
      case "ERROR" -> FILTER_ERROR;
      case "COMPLETE" -> "complete";
      default -> "none";
    };
  }

  /**
   * Time in the two largest units, as the Activity screen tells it: «6 d 4 h», «11 h 45 min» or «5 min»
   */
  private String duration(Duration time) {
    Duration positive = time.isNegative() ? Duration.ZERO : time;
    if (positive.toDays() > 0) {
      return text("SCREEN_TEXT_ACTIVITY_DURATION_DAYS", String.valueOf(positive.toDays()),
        String.valueOf(positive.toHoursPart()));
    } else if (positive.toHours() > 0) {
      return text("SCREEN_TEXT_ACTIVITY_DURATION_HOURS", String.valueOf(positive.toHours()),
        String.valueOf(positive.toMinutesPart()));
    } else if (positive.toMinutes() > 0) {
      return text("SCREEN_TEXT_ACTIVITY_DURATION_MINUTES", String.valueOf(positive.toMinutes()));
    }
    return text("SCREEN_TEXT_ACTIVITY_DURATION_SECONDS", String.valueOf(positive.getSeconds()));
  }

  /**
   * Every task by its identifier, with what it runs and its launch
   */
  private Map<Integer, Map<String, CellData>> getTasks() throws AWException {
    Map<Integer, Map<String, CellData>> tasks = new HashMap<>();
    for (Map<String, CellData> task : rows(SCHEDULER_CONFIGURATION_ENGINE_TASKS)) {
      tasks.put(integer(task, "IdeTsk"), task);
    }
    return tasks;
  }

  /**
   * Launch of a task as its «When» summary reads it
   */
  private static TaskLaunch getLaunch(Map<String, CellData> row) {
    CellData date = row.get("ExeDte");
    return new TaskLaunch()
      .setLaunchType(integer(row, "TypLch"))
      .setRepeatType(integer(row, "RptTyp"))
      .setRepeatNumber(integer(row, "RptNum"))
      .setWeekDays(list(row, "WeekDays"))
      .setDays(list(row, "ExeDay"))
      .setHours(list(row, "ExeHrs"))
      .setMinutes(list(row, "ExeMin"))
      .setDate(date == null || date.getObjectValue() == null ? null
        : LegacyDateConverter.asLocalDate(date.getDateValue()))
      .setTime(text(row, "ExeTim"))
      .setServer(text(row, "SrvNam"))
      .setPath(text(row, "LchPth"))
      .setPattern(text(row, "LchPat"));
  }

  private List<Map<String, CellData>> rows(String query) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    return queryService.launchPrivateQuery(query, parameters).getDataList().getRows();
  }

  private LocalDateTime dateTime(Date date) {
    return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), zone());
  }

  /**
   * Zone of the clock of the screen, the zone of the system without one
   */
  private ZoneId zone() {
    return Optional.ofNullable(clock.getZone()).orElse(ZoneId.systemDefault());
  }

  private static Map<String, CellData> count(String value, String label, long count) {
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(value));
    row.put(JSON_LABEL_PARAMETER, new CellData(label));
    row.put("count", new CellData(count));
    row.put("tone", new CellData(""));
    return row;
  }

  private String yesNo(boolean value) {
    return text(TEXT + (value ? "YES" : "NO"));
  }

  private static String code(String value) {
    return value.toUpperCase(Locale.ROOT).replace('-', '_');
  }

  /**
   * Text of a locale with its parameters. The parameters are not escaped: the screen shows the texts as text
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String text(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    String value = cell == null ? null : cell.getStringValue();
    return isBlank(value) ? null : value.trim();
  }

  private static Integer integer(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null || isBlank(cell.getStringValue()) ? null
      : cell.getIntegerValue();
  }

  private static List<String> list(Map<String, CellData> row, String field) {
    String value = text(row, field);
    return value == null ? List.of() : Arrays.stream(value.split(",")).map(String::trim)
      .filter(item -> !item.isEmpty()).toList();
  }
}
