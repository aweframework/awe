package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.ExecutionSort;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.enums.TaskLastStatus;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASKS_WITH_PARAMETERS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_CATALOG_HISTORY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_CATALOG_ROWS;

/**
 * Task catalog of the scheduler (tasks screen): every task with its last status, when it is launched («When», see
 * {@link TaskWhenSummary}), the strip of its last saved executions, its next execution, its average time and whether
 * it is active, filtered, sorted and paged. The tasks are few (one row each), so they are read at once and filtered,
 * sorted and paged here; the next fire times, the history and the parameters are read only for the tasks of the page,
 * each with one query for the whole page
 */
public class TaskCatalogPresenter {

  /**
   * Rows of a page of the catalog without a max
   */
  public static final int DEFAULT_PAGE_SIZE = 50;
  /**
   * Most rows of a page: a larger max (or a max below 1) is refused
   */
  public static final int MAX_PAGE_SIZE = 500;
  /**
   * Saved executions of a task shown in its history strip
   */
  public static final int HISTORY_SIZE = 20;
  private static final String TASK_ID = "IdeTsk";
  private static final String NAME = "Nam";
  private static final String ACTIVE = "Act";
  private static final String LAST_DATE = "TskLstDat";
  private static final String KIND = "TskKnd";
  private static final String NEXT_DATE = "TskNxtDat";
  private static final String AVERAGE = "TskAvgMs";
  /**
   * Columns of the catalog that can be sorted (the sort ids of the grid, and the column names), with the field of the
   * rows that sorts them. Any other column is refused: the next execution too, as it mixes fire times with what
   * launches the task and sorting it would ask the scheduler for the fire time of every task
   */
  public static final Map<String, String> SORT_FIELDS = Map.ofEntries(
    Map.entry(NAME, NAME),
    Map.entry("TskNam", NAME),
    Map.entry(TASK_ID, TASK_ID),
    Map.entry(LAST_DATE, LAST_DATE),
    Map.entry("TskStaTxt", LAST_DATE),
    Map.entry(KIND, KIND),
    Map.entry("TskWhnTxt", KIND),
    Map.entry(AVERAGE, AVERAGE),
    Map.entry("TskAvg", AVERAGE),
    Map.entry(ACTIVE, ACTIVE),
    Map.entry("TskActTxt", ACTIVE));
  private static final Set<String> SORT_DIRECTIONS = Set.of("ASC", "DESC");
  private static final String TASK_IDS = "catalogTaskIds";
  private static final String HISTORY_SIZE_PARAMETER = "catalogHistorySize";
  private static final String CATALOG = "SCREEN_TEXT_TASK_CATALOG_";
  private static final String DEFERRED_STATUS = "deferred";
  private static final String NO_EXECUTION_ICON = "lucide:circle-dashed";
  private static final String NEUTRAL_TONE = "neutral";
  private static final DateTimeFormatter HISTORY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final FlowRunRepository flowRunRepository;
  private final TaskListPresenter taskListPresenter;
  private final ServiceConfig serviceConfig;
  private final Clock clock;
  private final TaskWhenSummary summary;

  /**
   * Constructor
   *
   * @param queryService      Query service
   * @param queryUtil         Query utilities
   * @param flowRunRepository Flow run repository (the dependencies of the tasks)
   * @param taskListPresenter Task list presenter (the next fire times of the scheduler)
   * @param serviceConfig     Service configuration giving access to the locales
   * @param clock             Clock of the relative dates
   */
  public TaskCatalogPresenter(QueryService queryService, QueryUtil queryUtil, FlowRunRepository flowRunRepository,
                              TaskListPresenter taskListPresenter, ServiceConfig serviceConfig, Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.flowRunRepository = flowRunRepository;
    this.taskListPresenter = taskListPresenter;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
    this.summary = new TaskWhenSummary(this::text);
  }

  /**
   * Retrieve a page of the task catalog
   *
   * @param criteria Filters, sort and page
   * @return Page of tasks; the records count every task that matches the filters
   * @throws AWException A filter value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getCatalog(TaskCatalogCriteria criteria) throws AWException {
    validate(criteria);
    List<Entry> tasks = loadTasks();
    Map<Integer, Entry> byId = new HashMap<>();
    tasks.forEach(task -> byId.put(task.id, task));
    List<Entry> matching = tasks.stream().filter(matches(criteria, true)).toList();

    List<SortColumn> sort = getSort(criteria);
    DataList sorted = new DataList();
    for (Entry task : matching) {
      sorted.addRow(sortRow(task));
    }
    DataListUtil.sort(sorted, sort);

    int max = Optional.ofNullable(criteria.getMax()).orElse(DEFAULT_PAGE_SIZE);
    int page = Optional.ofNullable(criteria.getPage()).orElse(1);
    List<Entry> pageTasks = sorted.getRows().stream()
      .skip((long) (page - 1) * max)
      .limit(max)
      .map(row -> byId.get(row.get(TASK_ID).getIntegerValue()))
      .toList();

    List<Integer> pageIds = pageTasks.stream().map(task -> task.id).toList();
    Map<Integer, List<Map<String, CellData>>> history = getHistory(pageIds);
    Set<Integer> withParameters = getTasksWithParameters(pageIds);
    LocalDateTime now = LocalDateTime.now(clock);
    DataList dataList = new DataList();
    for (Entry task : pageTasks) {
      dataList.addRow(getRow(task, getNextFireTime(task), history.getOrDefault(task.id, List.of()),
        withParameters.contains(task.id), now));
    }
    dataList.setRecords(matching.size());
    dataList.setPage(page);
    dataList.setTotal(Math.max(1, (matching.size() + max - 1) / max));
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the options of the active filter of the catalog with their counts: every task, the active ones and the
   * inactive ones, counted with the search, launch, last status, server and calendar filters of the criteria (not with
   * its own)
   *
   * @param criteria Filters of the catalog
   * @return One row per option: value, label (locale key), count and tone
   * @throws AWException A filter value is not valid (InvalidExecutionFilterException) or the tasks cannot be read
   */
  public ServiceData getActiveCounts(TaskCatalogCriteria criteria) throws AWException {
    validate(criteria);
    List<Entry> matching = loadTasks().stream().filter(matches(criteria, false)).toList();
    long active = matching.stream().filter(task -> task.active).count();
    DataList dataList = new DataList();
    dataList.addRow(getCount("", CATALOG + "ALL", matching.size()));
    dataList.addRow(getCount("1", CATALOG + "ACTIVE_TASKS", active));
    dataList.addRow(getCount("0", CATALOG + "INACTIVE_TASKS", matching.size() - active));
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Summary of one task as the catalog shows it (its launch, «When», next execution, average time and last execution),
   * for the task screen
   *
   * @param taskId Task identifier
   * @return Summary of the task, empty when it does not exist
   * @throws AWException The tasks cannot be read
   */
  public Optional<TaskSummary> getSummary(Integer taskId) throws AWException {
    if (taskId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(getSummaries(List.of(taskId)).get(taskId));
  }

  /**
   * Summaries of some tasks as the catalog shows them (see {@link #getSummary(Integer)}), reading the tasks once: the
   * tasks of the flow graph of the task screen
   *
   * @param taskIds Task identifiers
   * @return Summary of each task that exists, by its identifier
   * @throws AWException The tasks cannot be read
   */
  public Map<Integer, TaskSummary> getSummaries(Collection<Integer> taskIds) throws AWException {
    Map<Integer, TaskSummary> summaries = new LinkedHashMap<>();
    if (taskIds.isEmpty()) {
      return summaries;
    }
    Set<Integer> wanted = new HashSet<>(taskIds);
    LocalDateTime now = LocalDateTime.now(clock);
    for (Entry task : loadTasks()) {
      if (wanted.contains(task.id)) {
        LocalDateTime nextFire = getNextFireTime(task);
        summaries.put(task.id, new TaskSummary(task.id, task.name, task.description, task.command, task.active,
          task.kind, task.launch, summary.when(task.launch), summary.next(task.launch, task.active, nextFire, now),
          nextFire, task.average, task.lastStatus, task.waiting, task.lastDate));
      }
    }
    return summaries;
  }

  /**
   * Every task with what the task editor shows of it as a parent: its name, whether it is active, when it launches the
   * tasks that run after it (its own flags) and its «When» summary. It does not read the scheduler
   *
   * @return Launch of every task, in the order of the query
   * @throws AWException The tasks cannot be read
   */
  public List<TaskLaunchSummary> getLaunches() throws AWException {
    List<TaskLaunchSummary> launches = new ArrayList<>();
    for (Entry task : loadTasks()) {
      launches.add(new TaskLaunchSummary(task.id, task.name, task.active, task.onWarning, task.onError,
        summary.when(task.launch)));
    }
    return launches;
  }

  /**
   * Launch of a task as a parent of another one
   *
   * @param id        Task identifier
   * @param name      Name
   * @param active    The task is active
   * @param onWarning It launches the tasks that run after it also when it ends with a warning
   * @param onError   It launches the tasks that run after it also when it ends with an error
   * @param when      «When» summary
   */
  public record TaskLaunchSummary(int id, String name, boolean active, boolean onWarning, boolean onError,
                                  TaskWhenSummary.When when) {
  }

  /**
   * Summary of one task: what the catalog knows of it
   *
   * @param id          Task identifier
   * @param name        Name
   * @param description Description, null without it
   * @param command     What it runs (maintain or command), null without it
   * @param active      The task is active
   * @param kind        Kind of launch
   * @param launch      Launch, with the parents of the task
   * @param when        «When» summary
   * @param next        Next execution and its detail
   * @param nextFire    Next fire time of the task in the scheduler, null when it has none
   * @param average     Average time of its OK executions in milliseconds, null without them
   * @param lastStatus  Status of its last execution, null without executions
   * @param waiting     The last execution waits for the confirmation of a deferred answer
   * @param lastDate    Start of its last execution, null without executions
   */
  public record TaskSummary(int id, String name, String description, String command, boolean active,
                            TaskLaunchKind kind, TaskLaunch launch, TaskWhenSummary.When when,
                            TaskWhenSummary.Line next, LocalDateTime nextFire, Long average, Integer lastStatus,
                            boolean waiting,
                            Date lastDate) {
  }

  /**
   * Validate the filters, the sort and the page of the catalog: a value that is not valid is refused, never ignored,
   * so the catalog is never wider than the filters ask for
   *
   * @param criteria Filters, sort and page
   * @throws InvalidExecutionFilterException A value is not valid
   */
  public void validate(TaskCatalogCriteria criteria) throws InvalidExecutionFilterException {
    validateFilters(criteria);
    if (criteria.getPage() != null && criteria.getPage() < 1) {
      throw invalidFilter("page", criteria.getPage());
    }
    if (criteria.getMax() != null && (criteria.getMax() < 1 || criteria.getMax() > MAX_PAGE_SIZE)) {
      throw invalidFilter("max", criteria.getMax());
    }
    validateSort(criteria);
  }

  /**
   * Validate the filters of the catalog: its launch type, its last status and whether the task is active
   */
  private void validateFilters(TaskCatalogCriteria criteria) throws InvalidExecutionFilterException {
    if (!isBlank(criteria.getLaunchType()) && TaskLaunchKind.fromCode(criteria.getLaunchType().trim()).isEmpty()) {
      throw invalidFilter("launchType", criteria.getLaunchType());
    }
    if (!isBlank(criteria.getLastStatus()) && TaskLastStatus.fromCode(criteria.getLastStatus().trim()).isEmpty()) {
      throw invalidFilter("lastStatus", criteria.getLastStatus());
    }
    if (!isBlank(criteria.getActive()) && !Set.of("0", "1").contains(criteria.getActive().trim())) {
      throw invalidFilter("active", criteria.getActive());
    }
  }

  /**
   * Validate the sort of the catalog: known columns and directions only
   */
  private void validateSort(TaskCatalogCriteria criteria) throws InvalidExecutionFilterException {
    for (ExecutionSort column : Optional.ofNullable(criteria.getSort()).orElse(List.of())) {
      if (column.getId() == null || !SORT_FIELDS.containsKey(column.getId())) {
        throw invalidFilter("sort", column.getId());
      }
      if (column.getDirection() == null || !SORT_DIRECTIONS.contains(column.getDirection().toUpperCase(Locale.ROOT))) {
        throw invalidFilter("sort", column.getDirection());
      }
    }
  }

  /**
   * Read every task with its launch, its last execution and its average time, and the parents of each one
   */
  private List<Entry> loadTasks() throws AWException {
    List<Map<String, CellData>> rows = queryService.launchPrivateQuery(SCHEDULER_TASK_CATALOG_ROWS,
      getParameters()).getDataList().getRows();
    // One row per task: two executions started at the same time keep the last one
    Map<Integer, Map<String, CellData>> byTask = new LinkedHashMap<>();
    for (Map<String, CellData> row : rows) {
      Integer taskId = integer(row, TASK_ID);
      Map<String, CellData> known = byTask.get(taskId);
      if (known == null || Optional.ofNullable(integer(row, "LstExe")).orElse(0)
        > Optional.ofNullable(integer(known, "LstExe")).orElse(0)) {
        byTask.put(taskId, row);
      }
    }
    Map<Integer, List<Integer>> parents = new HashMap<>();
    for (TaskDependency dependency : flowRunRepository.getDependencies()) {
      parents.computeIfAbsent(dependency.getTaskId(), key -> new ArrayList<>()).add(dependency.getParentId());
    }

    List<Entry> tasks = new ArrayList<>();
    for (Map<String, CellData> row : byTask.values()) {
      List<TaskLaunch.Parent> taskParents = new ArrayList<>();
      for (Integer parentId : parents.getOrDefault(integer(row, TASK_ID), List.of())) {
        Map<String, CellData> parent = byTask.get(parentId);
        if (parent != null) {
          taskParents.add(new TaskLaunch.Parent(text(parent, NAME), flag(parent, "LchDepWrn"),
            flag(parent, "LchDepErr")));
        }
      }
      tasks.add(new Entry(row, getLaunch(row, taskParents)));
    }
    return tasks;
  }

  /**
   * Launch of a task as its «When» summary reads it
   */
  private static TaskLaunch getLaunch(Map<String, CellData> row, List<TaskLaunch.Parent> parents) {
    Date date = date(row, "ExeDte");
    return new TaskLaunch()
      .setLaunchType(integer(row, "TypLch"))
      .setRepeatType(integer(row, "RptTyp"))
      .setRepeatNumber(integer(row, "RptNum"))
      .setWeekDays(list(row, "WeekDays"))
      .setDays(list(row, "ExeDay"))
      .setHours(list(row, "ExeHrs"))
      .setMinutes(list(row, "ExeMin"))
      .setDate(date == null ? null : LegacyDateConverter.asLocalDateTime(date).toLocalDate())
      .setTime(text(row, "ExeTim"))
      .setCalendar(text(row, "CalNam"))
      .setServer(text(row, "SrvNam"))
      .setPath(text(row, "LchPth"))
      .setPattern(text(row, "LchPat"))
      .setParents(parents);
  }

  /**
   * Filter of the catalog: the search (name, description or what the task runs), the launch, the last status, the
   * server it uses (to run or to check its files), the calendar of its launch and, when asked, the active state
   */
  private static Predicate<Entry> matches(TaskCatalogCriteria criteria, boolean withActive) {
    String search = isBlank(criteria.getSearch()) ? null : criteria.getSearch().trim().toLowerCase(Locale.ROOT);
    Optional<TaskLaunchKind> kind = isBlank(criteria.getLaunchType()) ? Optional.empty()
      : TaskLaunchKind.fromCode(criteria.getLaunchType().trim());
    Optional<TaskLastStatus> lastStatus = isBlank(criteria.getLastStatus()) ? Optional.empty()
      : TaskLastStatus.fromCode(criteria.getLastStatus().trim());
    String active = withActive && !isBlank(criteria.getActive()) ? criteria.getActive().trim() : null;
    return task -> (search == null || Arrays.asList(task.name, task.description, task.command).stream()
      .anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains(search)))
      && kind.map(option -> option == task.kind).orElse(true)
      && lastStatus.map(option -> TaskLastStatus.of(task.lastStatus, task.waiting).filter(option::equals).isPresent())
      .orElse(true)
      && (active == null || active.equals(task.active ? "1" : "0"))
      && (criteria.getServerId() == null || criteria.getServerId().equals(task.executionServer)
      || criteria.getServerId().equals(task.fileServer))
      && (criteria.getCalendarId() == null || criteria.getCalendarId().equals(task.calendar));
  }

  /**
   * Sort of the catalog: the columns of the criteria, then the name and the id; by name without them
   */
  private static List<SortColumn> getSort(TaskCatalogCriteria criteria) {
    List<SortColumn> sort = new ArrayList<>();
    for (ExecutionSort column : Optional.ofNullable(criteria.getSort()).orElse(List.of())) {
      sort.add(new SortColumn(SORT_FIELDS.get(column.getId()), column.getDirection().toLowerCase(Locale.ROOT)));
    }
    sort.add(new SortColumn(NAME, "asc"));
    sort.add(new SortColumn(TASK_ID, "asc"));
    return sort;
  }

  /**
   * Row of a task with the fields the catalog sorts by
   */
  private static Map<String, CellData> sortRow(Entry task) {
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_ID, new CellData(task.id));
    row.put(NAME, new CellData(task.name));
    row.put(ACTIVE, new CellData(task.active ? 1 : 0));
    row.put(KIND, new CellData(task.kind.ordinal()));
    row.put(LAST_DATE, millis(task.lastDate == null ? null : LegacyDateConverter.asLocalDateTime(task.lastDate)));
    row.put(AVERAGE, task.average == null ? new CellData() : new CellData(task.average));
    return row;
  }

  /**
   * Row of a task of the page, as the catalog shows it
   */
  private Map<String, CellData> getRow(Entry task, LocalDateTime next, List<Map<String, CellData>> history,
                                       boolean withParameters, LocalDateTime now) throws AWException {
    Map<String, CellData> row = sortRow(task);
    row.put("id", new CellData(task.id));
    row.put("Des", new CellData(Optional.ofNullable(task.description).orElse("")));
    row.put("CmdExe", new CellData(Optional.ofNullable(task.command).orElse("")));
    row.put("TypLch", new CellData(task.launch.getLaunchType()));
    row.put("Sta", task.lastStatus == null ? new CellData() : new CellData(task.lastStatus));
    row.put("TskVar", new CellData(withParameters ? 1 : 0));

    // The task links to its screen, with its description as the title of the link
    ObjectNode link = JsonNodeFactory.instance.objectNode();
    link.put(JSON_VALUE_PARAMETER, task.name);
    link.put(JSON_LABEL_PARAMETER, task.name);
    link.put(JSON_TITLE_PARAMETER, isBlank(task.description) ? task.name : task.description);
    row.put("TskNam", new CellData(link));

    // Last status: the pill of the activity screen and when it started
    if (task.lastStatus == null) {
      putPill(row, "TskSta", text(CATALOG + "NO_EXECUTIONS"), NO_EXECUTION_ICON, NEUTRAL_TONE);
      row.put("TskStaSub", new CellData(""));
    } else {
      String code = statusCode(task.lastStatus, task.waiting);
      putPill(row, "TskSta", serviceConfig.getLocale(queryService.findLabel("ActStaTyp", code)),
        queryService.findLabel("ActStaIco", code), queryService.findLabel("ActStaTon", code));
      row.put("TskStaSub", new CellData(summary.relative(LegacyDateConverter.asLocalDateTime(task.lastDate), now)));
    }

    TaskWhenSummary.When when = summary.when(task.launch);
    row.put("TskWhnTxt", new CellData(when.text()));
    row.put("TskWhnIcn", new CellData(when.icon()));
    row.put("TskWhnSub", new CellData(when.detail()));
    row.put("TskHis", new CellData(getHistoryStrip(history).toString()));

    TaskWhenSummary.Line nextLine = summary.next(task.launch, task.active, next, now);
    row.put("TskNxtTxt", new CellData(nextLine.text()));
    row.put("TskNxtSub", new CellData(nextLine.detail()));
    row.put(NEXT_DATE, millis(next));
    row.put("TskAvg", new CellData(task.average == null ? TaskWhenSummary.EMPTY_VALUE
      : TaskWhenSummary.duration(task.average.longValue())));
    putPill(row, "TskAct", text(CATALOG + (task.active ? "ACTIVE" : "INACTIVE")),
      task.active ? "lucide:power" : "lucide:power-off", task.active ? "success" : NEUTRAL_TONE);
    return row;
  }

  /**
   * Status strip of the last {@link #HISTORY_SIZE} saved executions of a task, from the oldest to the newest: the tone
   * and the label of each status, and as title its execution, start, duration and flow (also the strip of the task
   * screen)
   *
   * @param history Saved executions of the task, newest first (IdeTsk, ExeTsk, Sta, IniDat, ExeTim, RunRooExe, DfrExe)
   * @return Items of the strip
   */
  ArrayNode getHistoryStrip(List<Map<String, CellData>> history) throws AWException {
    ArrayNode strip = JsonNodeFactory.instance.arrayNode();
    List<Map<String, CellData>> executions = history.subList(0, Math.min(HISTORY_SIZE, history.size()));
    for (Map<String, CellData> execution : executions.reversed()) {
      Integer status = integer(execution, "Sta");
      String code = statusCode(status, status != null && status == TaskStatus.JOB_RUNNING.getValue()
        && integer(execution, "DfrExe") != null);
      Integer time = integer(execution, "ExeTim");
      Date start = date(execution, "IniDat");
      String flow = text(execution, "RunRooExe");
      String[] parameters = {String.valueOf(integer(execution, "ExeTsk")),
        start == null ? TaskWhenSummary.EMPTY_VALUE : HISTORY_DATE.format(LegacyDateConverter.asLocalDateTime(start)),
        time == null ? TaskWhenSummary.EMPTY_VALUE : TimeUtil.formatTime(time), flow};
      String tone = queryService.findLabel("ActStaTon", code);
      strip.addObject()
        .put("tone", "pending".equals(tone) ? "info" : tone)
        .put("label", serviceConfig.getLocale(queryService.findLabel("ActStaTyp", code)))
        .put("title", isBlank(flow) ? text(CATALOG + "HISTORY_TITLE", Arrays.copyOf(parameters, 3))
          : text(CATALOG + "HISTORY_TITLE_FLOW", parameters));
    }
    return strip;
  }

  /**
   * Last saved executions of the tasks of a page (at most {@link #HISTORY_SIZE} per task), newest first, with one query
   * for the whole page
   */
  private Map<Integer, List<Map<String, CellData>>> getHistory(List<Integer> taskIds) throws AWException {
    Map<Integer, List<Map<String, CellData>>> history = new HashMap<>();
    if (taskIds.isEmpty()) {
      return history;
    }
    // The database keeps the last HISTORY_SIZE executions of each task
    ObjectNode parameters = getTaskIdsParameters(taskIds);
    parameters.put(HISTORY_SIZE_PARAMETER, HISTORY_SIZE);
    for (Map<String, CellData> execution : queryService.launchPrivateQuery(SCHEDULER_TASK_CATALOG_HISTORY, parameters)
      .getDataList().getRows()) {
      history.computeIfAbsent(integer(execution, TASK_ID), key -> new ArrayList<>()).add(execution);
    }
    return history;
  }

  /**
   * Tasks of a page with parameters (their launch asks for their values), with one query for the whole page
   */
  private Set<Integer> getTasksWithParameters(List<Integer> taskIds) throws AWException {
    Set<Integer> tasks = new HashSet<>();
    if (!taskIds.isEmpty()) {
      queryService.launchPrivateQuery(SCHEDULER_TASKS_WITH_PARAMETERS, getTaskIdsParameters(taskIds)).getDataList()
        .getRows().forEach(row -> tasks.add(integer(row, TASK_ID)));
    }
    return tasks;
  }

  private ObjectNode getTaskIdsParameters(List<Integer> taskIds) {
    ObjectNode parameters = getParameters();
    ArrayNode ids = parameters.putArray(TASK_IDS);
    taskIds.forEach(ids::add);
    return parameters;
  }

  /**
   * Parameters of the queries of the catalog: every row, in the order of the query. The sort of the request names the
   * columns of the grid, not the fields of the queries, so it is always replaced
   */
  private ObjectNode getParameters() {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    return parameters;
  }

  /**
   * Next fire time of an active task that the scheduler fires (a scheduled or a file task)
   */
  private LocalDateTime getNextFireTime(Entry task) {
    if (!task.active || (task.kind != TaskLaunchKind.SCHEDULED && task.kind != TaskLaunchKind.FILE)) {
      return null;
    }
    try {
      return taskListPresenter.getNextFireTime(task.id, task.launch.getLaunchType())
        .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
        .orElse(null);
    } catch (AWException exc) {
      return null;
    }
  }

  private static String statusCode(Integer status, boolean waiting) {
    return waiting && status == TaskStatus.JOB_RUNNING.getValue() ? DEFERRED_STATUS : String.valueOf(status);
  }

  private static void putPill(Map<String, CellData> row, String prefix, String text, String icon, String tone) {
    row.put(prefix + "Txt", new CellData(text));
    row.put(prefix + "Icn", new CellData(icon));
    row.put(prefix + "Ton", new CellData(tone));
  }

  private Map<String, CellData> getCount(String value, String label, long count) {
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(value));
    row.put(JSON_LABEL_PARAMETER, new CellData(label));
    row.put("count", new CellData(count));
    row.put("tone", new CellData(""));
    return row;
  }

  /**
   * Text of a locale with its parameters. The parameters are not escaped: the grid shows the texts as text
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }

  private InvalidExecutionFilterException invalidFilter(String filter, Object value) {
    return new InvalidExecutionFilterException(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_TITLE"),
      serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", filter, String.valueOf(value)));
  }

  private static CellData millis(LocalDateTime date) {
    return date == null ? new CellData()
      : new CellData(date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
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

  private static Date date(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null ? null : cell.getDateValue();
  }

  private static boolean flag(Map<String, CellData> row, String field) {
    return Integer.valueOf(1).equals(integer(row, field));
  }

  private static List<String> list(Map<String, CellData> row, String field) {
    String value = text(row, field);
    return value == null ? null : Arrays.stream(value.split(",")).map(String::trim).toList();
  }

  /**
   * A task of the catalog
   */
  private static final class Entry {
    private final int id;
    private final String name;
    private final String description;
    private final String command;
    private final boolean active;
    private final Integer lastStatus;
    private final boolean waiting;
    private final Date lastDate;
    private final Long average;
    private final TaskLaunch launch;
    private final TaskLaunchKind kind;
    private final boolean onWarning;
    private final boolean onError;
    private final Integer executionServer;
    private final Integer fileServer;
    private final Integer calendar;

    private Entry(Map<String, CellData> row, TaskLaunch launch) {
      this.id = integer(row, TASK_ID);
      this.name = Optional.ofNullable(text(row, NAME)).orElse("");
      this.description = text(row, "Des");
      this.command = text(row, "CmdExe");
      this.active = flag(row, ACTIVE);
      this.lastStatus = integer(row, "LstSta");
      this.waiting = lastStatus != null && lastStatus == TaskStatus.JOB_RUNNING.getValue()
        && integer(row, "LstDfr") != null;
      this.lastDate = date(row, "LstDat");
      CellData average = row.get("AvgTim");
      this.average = average == null || !(average.getObjectValue() instanceof Number number) ? null
        : number.longValue();
      this.launch = launch;
      this.kind = TaskLaunchKind.of(launch.getLaunchType(), !launch.getParents().isEmpty());
      this.onWarning = flag(row, "LchDepWrn");
      this.onError = flag(row, "LchDepErr");
      this.executionServer = integer(row, "ExeSrv");
      this.fileServer = integer(row, "FilSrv");
      this.calendar = integer(row, "CalIde");
    }
  }
}
