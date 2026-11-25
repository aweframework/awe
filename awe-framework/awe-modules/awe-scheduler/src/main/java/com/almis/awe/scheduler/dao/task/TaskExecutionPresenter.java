package com.almis.awe.scheduler.dao.task;

import com.almis.awe.builder.client.ClientActionBuilder;
import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.client.UpdateControllerActionBuilder;
import com.almis.awe.builder.client.css.AddCssClassActionBuilder;
import com.almis.awe.builder.client.css.RemoveCssClassActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.model.util.data.date.DateFormats;
import com.almis.awe.model.util.data.date.DateTimeFormatting;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.ExecutionSort;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ExecutionOrigin;
import com.almis.awe.scheduler.enums.ExecutionState;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogPage;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_ACTIVITY_EXECUTIONS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_ACTIVITY_RUN_GROUPS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_ACTIVITY_RUN_SKIPPED;
import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_LOG_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_PROGRESS;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;

/**
 * Data of the task execution screens: the execution list with progress and elapsed time, the execution detail
 * screen and its log window, the progress and status widgets pushed while a task runs, and the execution list and
 * counters of all tasks of the activity screen.
 */
@Slf4j
public class TaskExecutionPresenter {

  private static final int ACTIVITY_WINDOW_HOURS = 24;
  /**
   * Rows of a page of the execution list of all tasks without a max
   */
  public static final int DEFAULT_PAGE_SIZE = 50;
  /**
   * Most rows of a page of the execution list of all tasks: a larger max (or a max below 1) is refused
   */
  public static final int MAX_PAGE_SIZE = 500;
  private static final int SHORT_RUN_ID_LENGTH = 8;
  /**
   * Raw status code of an execution row ({@link TaskStatus} value)
   */
  private static final String STATUS = "Sta";
  private static final String EXECUTION_STATUS = "ExeStaIco";
  private static final String EXECUTION_RUN = "ExeRun";
  private static final String EXECUTION_RUN_LABEL = "ExeRunLbl";
  private static final String RUN_ROOT_NAME = "RunRooNam";
  private static final String RUN_ROOT_EXECUTION = "RunRooExe";
  /**
   * Option of the activity status groups (ActStaIco, ActStaSty, ActStaTyp) of a running execution that is waiting
   * for the confirmation of a deferred answer: "waiting" in this class and in the counters, "deferred" in the
   * enumerated groups, SCREEN_TEXT_ACTIVITY_WAITING in the locales
   */
  private static final String DEFERRED_STATUS = "deferred";
  private static final int NO_DEFERRED_ROW = -1;
  private static final String TASK_ID_LIST = "IdeTsk";
  private static final String EXECUTION_ORIGIN = "ExeOri";
  private static final String DEFERRED_EXECUTION = "DfrExe";
  private static final String EXECUTION_DEFERRED = "ExeDfr";
  private static final String EXECUTION_DUE_DATE = "ExeDue";
  private static final String EXECUTION_DUE_TIME = "ExeDueTim";
  /**
   * Text, icon and tone of the status pill of the activity grid, and the due time shown under it while waiting
   */
  private static final String EXECUTION_STATUS_TEXT = "ExeStaTxt";
  private static final String EXECUTION_STATUS_ICON = "ExeStaIcn";
  private static final String EXECUTION_STATUS_TONE = "ExeStaTon";
  private static final String EXECUTION_DUE_LABEL = "ExeDueLbl";
  /**
   * Tone of the origin pill: the same neutral tone for every origin (the icon and the text tell them apart)
   */
  private static final String EXECUTION_ORIGIN_TONE = "ExeOriTon";
  private static final String NEUTRAL_TONE = "neutral";
  private static final String STATE_FILTER = "state";
  private static final String ALL_STATES_LABEL = "SCREEN_TEXT_ACTIVITY_ALL";
  private static final DateTimeFormatter DUE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DUE_DAY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private static final DateTimeFormatter DUE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter STRICT_WEB_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT);
  private static final String SORT_FILTER = "sort";
  private static final String TASK_NAME = "TskNam";
  private static final String EXECUTION_START = "ExeLstTim";
  private static final String EXECUTION_END = "ExeEnd";
  private static final String EXECUTION_ELAPSED = "ExeEla";
  private static final String EXECUTION_ORIGIN_TEXT = "ExeOriTxt";
  private static final String RUN_IDS_PARAMETER = "exeRunIds";
  /**
   * Columns of the execution list that can be sorted (the sort ids of the grid, and the column names), with the field
   * of the execution rows query that sorts them in the database. Any other column is refused
   */
  private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
    Map.entry(EXECUTION_START, EXECUTION_START),
    Map.entry(TASK_NAME, TASK_NAME),
    Map.entry(STATUS, STATUS),
    Map.entry(EXECUTION_STATUS_TEXT, STATUS),
    Map.entry(EXECUTION_ELAPSED, EXECUTION_ELAPSED),
    Map.entry(TASK_EXECUTION_PROGRESS, EXECUTION_ELAPSED),
    Map.entry(TASK_EXECUTION_TIME, EXECUTION_ELAPSED),
    Map.entry("ExeLchBy", TASK_LAUNCHED_BY_LIST),
    Map.entry(TASK_LAUNCH_TYPE_LIST, TASK_LAUNCH_TYPE_LIST),
    Map.entry(EXECUTION_ORIGIN_TEXT, TASK_LAUNCH_TYPE_LIST));
  private static final Set<String> SORT_DIRECTIONS = Set.of("ASC", "DESC");
  /**
   * Text of an empty value in the execution views, as the other side panels show it
   */
  private static final String EMPTY_VALUE = "\u2014";
  /**
   * Longest time shown in seconds, and in minutes, by the progress card of an execution
   */
  private static final long PROGRESS_SECONDS_LIMIT = 120_000L;
  private static final long PROGRESS_MINUTES_LIMIT = 7_200_000L;
  /**
   * Fields of the executions grouped by flow run (a tree grid): the row id (the run id of a run, the execution log
   * locator of an execution, {@code <run id>/<task id>} of a skipped task), the id of its parent row (empty for a
   * top-level row), whether it is a leaf, and the executions of a run (number and text)
   */
  private static final String ROW_ID = "id";
  private static final String RUN_PARENT = "RunPar";
  private static final String RUN_LEAF = "RunLeaf";
  private static final String RUN_EXECUTION_COUNT = "RunExeCnt";
  private static final String RUN_EXECUTION_TEXT = "RunExeTxt";
  /**
   * Option of the activity status groups of a task that its flow run skipped (it never ran)
   */
  private static final String SKIPPED_STATUS = "skipped";
  /**
   * Value of the filter of the executions without flow run (COALESCE of the run id)
   */
  private static final String NO_RUN = "-";
  /**
   * Every field of a row of the executions grouped by flow run: a field a row does not have is sent empty
   */
  private static final List<String> RUN_VIEW_FIELDS = List.of(ROW_ID, RUN_PARENT, RUN_LEAF, RUN_EXECUTION_COUNT,
    RUN_EXECUTION_TEXT, TASK_ID_LIST, TASK_NAME, TASK_EXECUTION_ID, TASK_LAUNCH_TYPE_LIST, EXECUTION_ORIGIN, STATUS,
    EXECUTION_STATUS_TEXT, EXECUTION_STATUS_ICON, EXECUTION_STATUS_TONE, EXECUTION_DUE_LABEL, EXECUTION_ORIGIN_TEXT,
    "ExeOriIcn", EXECUTION_ORIGIN_TONE, EXECUTION_START, EXECUTION_END, TASK_EXECUTION_TIME, TASK_EXECUTION_PROGRESS,
    EXECUTION_RUN, EXECUTION_RUN_LABEL, TASK_LAUNCHED_BY_LIST, "ExeDes", EXECUTION_DEFERRED, EXECUTION_DUE_TIME,
    TASK_EXECUTION_LOG_PATH, EXECUTION_ELAPSED);

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final ExecutionLogStore executionLogStore;
  private final TaskExecutionRepository executionRepository;
  private final DeferredExecutionRepository deferredRepository;
  private final ServiceConfig serviceConfig;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param queryService        Query service
   * @param queryUtil           Query utilities
   * @param executionLogStore   Execution log store
   * @param executionRepository Task execution repository
   * @param deferredRepository  Deferred execution repository
   * @param serviceConfig       Service configuration giving access to locales
   */
  public TaskExecutionPresenter(QueryService queryService, QueryUtil queryUtil, ExecutionLogStore executionLogStore,
                                TaskExecutionRepository executionRepository,
                                DeferredExecutionRepository deferredRepository, ServiceConfig serviceConfig) {
    this(queryService, queryUtil, executionLogStore, executionRepository, deferredRepository, serviceConfig,
      Clock.systemDefaultZone());
  }

  /**
   * Constructor with an explicit clock for the progress and elapsed time computations
   */
  TaskExecutionPresenter(QueryService queryService, QueryUtil queryUtil, ExecutionLogStore executionLogStore,
                         TaskExecutionRepository executionRepository,
                         DeferredExecutionRepository deferredRepository, ServiceConfig serviceConfig, Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.executionLogStore = executionLogStore;
    this.executionRepository = executionRepository;
    this.deferredRepository = deferredRepository;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
  }

  /**
   * Retrieve the execution list of a task, with progress, time, launcher and log locator of each execution
   *
   * @param taskId Task id
   * @return Task execution list
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    ServiceData serviceData = queryService.launchPrivateQuery("getTaskExecutionList", parameters);
    long averageExecution = executionRepository.getAverageTime(taskId);

    for (Map<String, CellData> row : serviceData.getDataList().getRows()) {
      decorateExecution(row, taskId, averageExecution);
    }

    return serviceData;
  }

  /**
   * Retrieve a page of the executions of all tasks, newest first (activity screen). Each row has the decoration of
   * the task execution list, with the progress computed from the average of its own task, plus its origin code and,
   * when it is waiting for the confirmation of a deferred answer, its due date
   *
   * @param criteria Filters and page
   * @return Execution list page
   * @throws AWException Error retrieving the executions
   */
  public ServiceData getExecutionList(ExecutionListCriteria criteria) throws AWException {
    validate(criteria);
    String page = String.valueOf(Optional.ofNullable(criteria.getPage()).orElse(1));
    int rows = Optional.ofNullable(criteria.getMax()).orElse(DEFAULT_PAGE_SIZE);
    ServiceData serviceData = queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_EXECUTIONS,
      getExecutionListParameters(criteria, page, String.valueOf(rows)));
    decorateActivityRows(serviceData.getDataList().getRows());
    return serviceData;
  }

  /**
   * Decorate execution rows of the activity screen as {@link #getExecutionList} shows them, with the progress computed
   * from the average of their own task (read once for the tasks of the rows)
   *
   * @param rows Execution rows
   * @throws AWException Error reading the averages or the waiting status
   */
  private void decorateActivityRows(List<Map<String, CellData>> rows) throws AWException {
    // Only the average times of the tasks of the rows
    Set<Integer> taskIds = rows.stream().map(row -> row.get(TASK_ID_LIST).getIntegerValue())
      .collect(Collectors.toCollection(TreeSet::new));
    Map<Integer, Long> averages = taskIds.isEmpty() ? Map.of() : executionRepository.getAverageTimes(taskIds);

    for (Map<String, CellData> row : rows) {
      Integer taskId = row.get(TASK_ID_LIST).getIntegerValue();
      decorateExecution(row, taskId, averages.getOrDefault(taskId, 0L));
      decorateActivity(row);
      // Internal keys of the deferred state and of the root of the flow run, not columns of the list
      row.remove(DEFERRED_EXECUTION);
      row.remove(RUN_ROOT_NAME);
      row.remove(RUN_ROOT_EXECUTION);
    }
  }

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run (activity screen, a tree grid). The top-level
   * rows are the flow runs and the executions without run, newest first: the filters select the runs with at least one
   * execution that matches them (and the executions without run that match them), and the page and the sort apply to
   * these top-level rows (a run sorts by its stored start, status, root task, launcher and origin, and by its elapsed
   * time). Each run row shows its root ({@code <root task> #<root execution>}), its aggregated status (running when an
   * execution runs, else queued when one is queued, else error when one ended with error, stopped or interrupted, else
   * warning when one ended with warning or the run skipped a task, else ok), its start (the first start of its
   * executions), its duration (from that start to the last end, or until now while one has not ended), the origin and
   * the launcher of its root execution and its number of executions. Its children are every execution of the run,
   * first started first, decorated as the rows of {@link #getExecutionList}, followed by the tasks the run skipped
   *
   * @param criteria Filters and page
   * @return Page of top-level rows, each run followed by its children; the records count the top-level rows
   * @throws AWException A filter value is not valid (InvalidExecutionFilterException) or the executions cannot be read
   */
  public ServiceData getRunList(ExecutionListCriteria criteria) throws AWException {
    validate(criteria);
    String page = String.valueOf(Optional.ofNullable(criteria.getPage()).orElse(1));
    int rows = Optional.ofNullable(criteria.getMax()).orElse(DEFAULT_PAGE_SIZE);
    DataList groups = queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_RUN_GROUPS,
      getExecutionListParameters(criteria, page, String.valueOf(rows))).getDataList();
    List<String> runIds = groups.getRows().stream().map(TaskExecutionPresenter::getRawRunId)
      .filter(runId -> !runId.isEmpty()).distinct().toList();
    List<Map<String, CellData>> withoutRun = groups.getRows().stream()
      .filter(group -> getRawRunId(group).isEmpty()).toList();

    // Every execution of the runs of the page and the executions without run of the page, decorated together
    List<Map<String, CellData>> executions = new ArrayList<>();
    if (!runIds.isEmpty()) {
      ObjectNode parameters = getRowsParameters();
      runIds.forEach(parameters.putArray(RUN_IDS_PARAMETER)::add);
      executions.addAll(queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_EXECUTIONS, parameters).getDataList()
        .getRows());
    }
    executions.addAll(getExecutionsWithoutRun(withoutRun));
    Map<String, List<Map<String, CellData>>> executionsByRun = new HashMap<>();
    Map<String, Map<String, CellData>> executionsWithoutRun = new HashMap<>();
    for (Map<String, CellData> execution : executions) {
      String runId = getRawRunId(execution);
      if (runId.isEmpty()) {
        executionsWithoutRun.put(getExecutionKey(execution), execution);
      } else {
        executionsByRun.computeIfAbsent(runId, key -> new ArrayList<>()).add(execution);
      }
    }
    decorateActivityRows(executions);

    DataList dataList = new DataList();
    Map<String, List<Map<String, CellData>>> skippedByRun = getSkippedTasks(runIds);
    for (Map<String, CellData> group : groups.getRows()) {
      String runId = getRawRunId(group);
      if (runId.isEmpty()) {
        Optional.ofNullable(executionsWithoutRun.get(getExecutionKey(group)))
          .ifPresent(execution -> dataList.addRow(asTreeRow(execution, getExecutionKey(execution), "", true)));
      } else {
        addRun(dataList, group, runId, executionsByRun.getOrDefault(runId, List.of()),
          skippedByRun.getOrDefault(runId, List.of()));
      }
    }
    dataList.setRecords(groups.getRecords());
    dataList.setPage(groups.getPage());
    dataList.setTotal(groups.getTotal());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Every execution of one flow run, decorated as the rows of {@link #getExecutionList} (status pill, waiting for a
   * confirmation with its due date, time, launcher and log locator), newest first
   *
   * @param runId Flow run identifier
   * @return Execution rows of the run
   * @throws AWException Error reading the executions
   */
  List<Map<String, CellData>> getRunExecutions(String runId) throws AWException {
    ObjectNode parameters = getRowsParameters();
    parameters.putArray(RUN_IDS_PARAMETER).add(runId);
    List<Map<String, CellData>> rows = queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_EXECUTIONS, parameters)
      .getDataList().getRows();
    decorateActivityRows(rows);
    return rows;
  }

  /**
   * Parameters of a query of execution rows that is not the page of the list: every row, no sort of the request, the
   * elapsed time until now
   */
  private ObjectNode getRowsParameters() {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray(SORT_FILTER);
    parameters.put("exeNow", formatTimestamp(LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault())));
    return parameters;
  }

  /**
   * Executions without flow run of a page: the executions without run of their tasks started between the first and
   * the last start of the page (the page keeps only its own)
   *
   * @param groups Top-level rows of the page without run (task, execution and start)
   * @return Execution rows
   * @throws AWException Error reading the executions
   */
  private List<Map<String, CellData>> getExecutionsWithoutRun(List<Map<String, CellData>> groups) throws AWException {
    List<Date> starts = groups.stream().map(group -> group.get(EXECUTION_START))
      .filter(start -> start != null && start.getDateValue() != null).map(CellData::getDateValue).toList();
    if (groups.isEmpty() || starts.isEmpty()) {
      return List.of();
    }
    ObjectNode parameters = getRowsParameters();
    parameters.put("exeNoRun", NO_RUN);
    ArrayNode taskIds = parameters.putArray("exeTaskIds");
    groups.stream().map(group -> group.get(TASK_ID_LIST).getIntegerValue()).distinct().forEach(taskIds::add);
    parameters.put("exeStartFrom", formatTimestamp(LegacyDateConverter.asLocalDateTime(
      starts.stream().min(Comparator.naturalOrder()).orElseThrow())));
    parameters.put("exeStartUntil", formatTimestamp(LegacyDateConverter.asLocalDateTime(
      starts.stream().max(Comparator.naturalOrder()).orElseThrow())));
    return queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_EXECUTIONS, parameters).getDataList().getRows();
  }

  /**
   * Tasks skipped by the flow runs of a page
   *
   * @param runIds Flow runs
   * @return Skipped tasks (ExeRun, IdeTsk, TskNam, ExeEnd) by run, in the order they were skipped
   * @throws AWException Error reading the skipped tasks
   */
  private Map<String, List<Map<String, CellData>>> getSkippedTasks(List<String> runIds) throws AWException {
    if (runIds.isEmpty()) {
      return Map.of();
    }
    ObjectNode parameters = getRowsParameters();
    runIds.forEach(parameters.putArray(RUN_IDS_PARAMETER)::add);
    Map<String, List<Map<String, CellData>>> skipped = new LinkedHashMap<>();
    for (Map<String, CellData> task : queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_RUN_SKIPPED, parameters)
      .getDataList().getRows()) {
      skipped.computeIfAbsent(getRawRunId(task), key -> new ArrayList<>()).add(task);
    }
    return skipped;
  }

  /**
   * Add a flow run to the page: its row and, as its children, its executions (first started first) and the tasks it
   * skipped
   */
  private void addRun(DataList dataList, Map<String, CellData> group, String runId,
                      List<Map<String, CellData>> executions, List<Map<String, CellData>> skipped) throws AWException {
    List<Map<String, CellData>> children = executions.stream()
      .sorted(Comparator.comparing((Map<String, CellData> execution) -> execution.get(EXECUTION_START).getDateValue())
        .thenComparing(execution -> execution.get(TASK_ID_LIST).getIntegerValue())
        .thenComparing(execution -> execution.get(TASK_EXECUTION_ID).getIntegerValue()))
      .toList();
    dataList.addRow(getRunRow(group, runId, children, skipped));
    for (Map<String, CellData> child : children) {
      dataList.addRow(asTreeRow(child, getExecutionKey(child), runId, true));
    }
    for (Map<String, CellData> task : skipped) {
      dataList.addRow(getSkippedRow(task, runId, group));
    }
  }

  /**
   * Row of a flow run: its root, its aggregated status, start and duration, the origin and the launcher of its root
   * execution and its number of executions
   */
  private Map<String, CellData> getRunRow(Map<String, CellData> group, String runId,
                                          List<Map<String, CellData>> executions,
                                          List<Map<String, CellData>> skipped) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    Integer rootTask = group.get(TASK_ID_LIST).getIntegerValue();
    Integer rootExecution = group.get(TASK_EXECUTION_ID).getIntegerValue();
    Map<String, CellData> runRoot = getRunRoot(group, runId);
    String label = getFlowRunLabel(runRoot, runId);
    row.put(TASK_ID_LIST, new CellData(rootTask));
    row.put(TASK_NAME, new CellData(label));
    row.put(EXECUTION_RUN, getFlowRun(runRoot));
    row.put(EXECUTION_RUN_LABEL, new CellData(label));

    // Status: the aggregated status of its executions and skipped tasks
    String status = String.valueOf(getRunStatus(executions.stream()
      .map(execution -> execution.get(STATUS).getIntegerValue()).toList(), !skipped.isEmpty()));
    row.put(STATUS, new CellData(status));
    setStatus(row, status);
    row.put(EXECUTION_DEFERRED, new CellData(0));

    // Start, end and duration: from the first start to the last end, or until now while an execution has not ended
    Optional<Date> start = executions.stream().map(execution -> execution.get(EXECUTION_START).getDateValue())
      .filter(Objects::nonNull).min(Comparator.naturalOrder());
    boolean open = executions.stream().anyMatch(execution -> execution.get(EXECUTION_END) == null
      || execution.get(EXECUTION_END).getDateValue() == null);
    Optional<Date> end = open ? Optional.empty() : executions.stream()
      .map(execution -> execution.get(EXECUTION_END).getDateValue()).max(Comparator.naturalOrder());
    if (start.isPresent()) {
      long startMillis = start.get().getTime();
      long millis = Math.max(0L, end.map(Date::getTime).orElse(clock.millis()) - startMillis);
      row.put(EXECUTION_START, new CellData(start.get()));
      row.put(TASK_EXECUTION_TIME, new CellData(open ? TimeUtil.formatTime((int) millis, false)
        : TimeUtil.formatTime((int) millis)));
      row.put(EXECUTION_ELAPSED, new CellData((int) (millis / 1000L)));
    }
    end.ifPresent(date -> row.put(EXECUTION_END, new CellData(date)));

    // Origin and launcher of its root execution (else of its first execution)
    executions.stream()
      .filter(execution -> rootTask != null && rootTask.equals(execution.get(TASK_ID_LIST).getIntegerValue())
        && rootExecution != null && rootExecution.equals(execution.get(TASK_EXECUTION_ID).getIntegerValue()))
      .findFirst()
      .or(() -> executions.stream().findFirst())
      .ifPresent(root -> List.of(TASK_LAUNCH_TYPE_LIST, EXECUTION_ORIGIN, EXECUTION_ORIGIN_TEXT, "ExeOriIcn",
        EXECUTION_ORIGIN_TONE, TASK_LAUNCHED_BY_LIST).forEach(field -> row.put(field, root.get(field))));

    row.put(RUN_EXECUTION_COUNT, new CellData(executions.size()));
    row.put(RUN_EXECUTION_TEXT, new CellData(skipped.isEmpty()
      ? serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_RUN_EXECUTIONS", String.valueOf(executions.size()))
      : serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_RUN_EXECUTIONS_SKIPPED", String.valueOf(executions.size()),
      String.valueOf(skipped.size()))));
    // The line under the status of a run (the due time of a waiting execution in an execution row)
    row.put(EXECUTION_DUE_LABEL, row.get(RUN_EXECUTION_TEXT));
    row.put(TASK_EXECUTION_LOG_PATH, getHiddenButton());
    return asTreeRow(row, runId, "", false);
  }

  /**
   * Row of a task skipped by a flow run: skipped status, no execution
   */
  private Map<String, CellData> getSkippedRow(Map<String, CellData> task, String runId, Map<String, CellData> group)
    throws AWException {
    Map<String, CellData> row = new HashMap<>();
    Integer taskId = task.get(TASK_ID_LIST).getIntegerValue();
    row.put(TASK_ID_LIST, new CellData(taskId));
    row.put(TASK_NAME, task.get(TASK_NAME));
    setStatus(row, SKIPPED_STATUS);
    row.put(EXECUTION_DEFERRED, new CellData(0));
    Optional.ofNullable(task.get(EXECUTION_END)).ifPresent(updated -> row.put(EXECUTION_END, updated));
    Map<String, CellData> runRoot = getRunRoot(group, runId);
    row.put(EXECUTION_RUN, getFlowRun(runRoot));
    row.put(EXECUTION_RUN_LABEL, new CellData(getFlowRunLabel(runRoot, runId)));
    row.put(TASK_EXECUTION_LOG_PATH, getHiddenButton());
    return asTreeRow(row, runId + "/" + taskId, runId, true);
  }

  /**
   * Cell of a button column that hides its button (a run or a skipped task has no log)
   */
  private static CellData getHiddenButton() {
    return new CellData(JsonNodeFactory.instance.objectNode().put(JSON_VALUE_PARAMETER, "").put(JSON_STYLE_PARAMETER, "hidden"));
  }

  /**
   * Status pill of a run or a skipped task: text, icon and tone of an option of the activity status groups
   */
  void setStatus(Map<String, CellData> row, String status) throws AWException {
    row.put(EXECUTION_STATUS_TEXT, new CellData(serviceConfig.getLocale(queryService.findLabel("ActStaTyp", status))));
    row.put(EXECUTION_STATUS_ICON, new CellData(queryService.findLabel("ActStaIco", status)));
    row.put(EXECUTION_STATUS_TONE, new CellData(queryService.findLabel("ActStaTon", status)));
  }

  /**
   * Aggregated status of a flow run
   *
   * @param statuses Statuses of its executions
   * @param skipped  The run skipped a task
   * @return Running when an execution runs, else queued when one is queued, else error when one ended with error,
   * stopped or interrupted, else warning when one ended with warning or the run skipped a task, else ok
   */
  static int getRunStatus(List<Integer> statuses, boolean skipped) {
    if (statuses.contains(TaskStatus.JOB_RUNNING.getValue())) {
      return TaskStatus.JOB_RUNNING.getValue();
    }
    if (statuses.contains(TaskStatus.JOB_QUEUED.getValue())) {
      return TaskStatus.JOB_QUEUED.getValue();
    }
    if (statuses.contains(TaskStatus.JOB_ERROR.getValue()) || statuses.contains(TaskStatus.JOB_STOPPED.getValue())
      || statuses.contains(TaskStatus.JOB_INTERRUPTED.getValue())) {
      return TaskStatus.JOB_ERROR.getValue();
    }
    if (skipped || statuses.contains(TaskStatus.JOB_WARNING.getValue())) {
      return TaskStatus.JOB_WARNING.getValue();
    }
    return TaskStatus.JOB_OK.getValue();
  }

  /**
   * Root of a flow run as the fields the run link reads (run id, root task name and root execution)
   */
  private static Map<String, CellData> getRunRoot(Map<String, CellData> group, String runId) {
    Map<String, CellData> root = new HashMap<>();
    root.put(EXECUTION_RUN, new CellData(runId));
    root.put(RUN_ROOT_NAME, Optional.ofNullable(group.get(TASK_NAME)).orElseGet(CellData::new));
    root.put(RUN_ROOT_EXECUTION, Optional.ofNullable(group.get(TASK_EXECUTION_ID)).orElseGet(CellData::new));
    return root;
  }

  /**
   * A row as a row of the tree: its id, its parent and whether it is a leaf (only a run row has children), with every
   * field of the view (empty when the row has none)
   */
  private static Map<String, CellData> asTreeRow(Map<String, CellData> row, String id, String parent, boolean leaf) {
    row.put(ROW_ID, new CellData(id));
    row.put(RUN_PARENT, new CellData(parent));
    row.put(RUN_LEAF, new CellData(leaf ? 1 : 0));
    RUN_VIEW_FIELDS.forEach(field -> row.computeIfAbsent(field, key -> new CellData()));
    return row;
  }

  /**
   * Run id of a row as the query reads it (not the link of a decorated row)
   */
  private static String getRawRunId(Map<String, CellData> row) {
    CellData run = row.get(EXECUTION_RUN);
    return run == null || run.isEmpty() || run.getObjectValue() instanceof ObjectNode ? ""
      : Optional.ofNullable(run.getStringValue()).orElse("");
  }

  /**
   * Execution log locator of an execution row ({@code <task id>-<execution id>})
   */
  private static String getExecutionKey(Map<String, CellData> row) {
    return row.get(TASK_ID_LIST).getIntegerValue() + "-" + row.get(TASK_EXECUTION_ID).getIntegerValue();
  }

  /**
   * Retrieve the execution counters of the activity screen: running executions, executions waiting for the
   * confirmation of a deferred answer, queued executions and executions ended with error in the last 24 hours. Each
   * counter is the record count of the execution list with the filter of its quick filter
   *
   * @return One row with the counters running, waiting, queued and errorsLast24h
   * @throws AWException Error counting the executions
   */
  public ServiceData getExecutionCounters() throws AWException {
    return getExecutionCounters(null);
  }

  /**
   * Retrieve the execution counters of the activity screen for the tasks whose name contains a text
   *
   * @param taskName Text contained in the task name, case insensitive; every task when null or blank
   * @return One row with the counters running, waiting, queued and errorsLast24h
   * @throws AWException Error counting the executions
   */
  public ServiceData getExecutionCounters(String taskName) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    row.put("running", new CellData(countExecutions(new ExecutionListCriteria().setTaskName(taskName)
      .setStatuses(List.of(TaskStatus.JOB_RUNNING.getValue())).setDeferred(0))));
    row.put("waiting", new CellData(countExecutions(new ExecutionListCriteria().setTaskName(taskName)
      .setStatuses(List.of(TaskStatus.JOB_RUNNING.getValue())).setDeferred(1))));
    row.put("queued", new CellData(countExecutions(new ExecutionListCriteria().setTaskName(taskName)
      .setStatuses(List.of(TaskStatus.JOB_QUEUED.getValue())))));
    row.put("errorsLast24h", new CellData(countExecutions(new ExecutionListCriteria().setTaskName(taskName)
      .setStatuses(List.of(TaskStatus.JOB_ERROR.getValue())).setWithinHours(ACTIVITY_WINDOW_HOURS))));

    DataList dataList = new DataList();
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts: every state first, then
   * each {@link ExecutionState}. Each count is the record count of the execution list with the search, origin and
   * start filters of the criteria and the filter of its option; the state, statuses, deferred and page of the criteria
   * are not used
   *
   * @param criteria Filters of the execution list
   * @return One row per option: value, label (locale key), count and tone
   * @throws AWException A filter value is not valid (InvalidExecutionFilterException) or the executions cannot be counted
   */
  public ServiceData getExecutionStateCounts(ExecutionListCriteria criteria) throws AWException {
    validate(criteria);
    DataList dataList = new DataList();
    dataList.addRow(getStateCount("", ALL_STATES_LABEL, "", countExecutions(getCountCriteria(criteria))));
    for (ExecutionState state : ExecutionState.values()) {
      dataList.addRow(getStateCount(state.getCode(), state.getLabel(), state.getTone(),
        countExecutions(getCountCriteria(criteria).setState(state.getCode()))));
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Filters of the counts of the state quick filter: the task, search, origin and start filters of the criteria
   *
   * @param criteria Filters of the execution list
   * @return Filters of a count
   */
  private ExecutionListCriteria getCountCriteria(ExecutionListCriteria criteria) {
    return new ExecutionListCriteria()
      .setTaskId(criteria.getTaskId())
      .setTaskName(criteria.getTaskName())
      .setOrigins(criteria.getOrigins())
      .setStartFrom(criteria.getStartFrom())
      .setStartTo(criteria.getStartTo())
      .setWithinHours(criteria.getWithinHours());
  }

  /**
   * Option row of the state quick filter
   */
  private Map<String, CellData> getStateCount(String value, String label, String tone, Long count) {
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(value));
    row.put(JSON_LABEL_PARAMETER, new CellData(label));
    row.put("count", new CellData(count));
    row.put("tone", new CellData(tone));
    return row;
  }

  /**
   * Count the executions that match a criteria
   *
   * @param criteria Filters
   * @return Number of executions
   * @throws AWException Error counting the executions
   */
  private Long countExecutions(ExecutionListCriteria criteria) throws AWException {
    return queryService.launchPrivateQuery(SCHEDULER_ACTIVITY_EXECUTIONS, getExecutionListParameters(criteria, "1", "1"))
      .getDataList().getRecords();
  }

  /**
   * Translate the execution list criteria into the parameters of the execution rows query
   *
   * @param criteria Filters
   * @param page     Page
   * @param max      Rows per page
   * @return Query parameters
   */
  private ObjectNode getExecutionListParameters(ExecutionListCriteria criteria, String page, String max)
    throws InvalidExecutionFilterException {
    ObjectNode parameters = queryUtil.getParameters(null, page, max);
    // The sort of the list, always set: the sort of the request names the columns of the grid, not the fields of the
    // query. The database sorts before it pages; without sort the query keeps its order, newest first
    ArrayNode sort = parameters.putArray(SORT_FILTER);
    for (ExecutionSort column : Optional.ofNullable(criteria.getSort()).orElse(List.of())) {
      sort.addObject()
        .put("id", SORT_FIELDS.get(column.getId()))
        .put("direction", column.getDirection().toUpperCase(Locale.ROOT));
    }
    // End of the elapsed time of the executions that have not ended (the elapsed time sort)
    parameters.put("exeNow", formatTimestamp(LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault())));
    if (criteria.getTaskId() != null) {
      parameters.putArray("exeTaskIds").add(criteria.getTaskId());
    }
    if (criteria.getTaskName() != null && !criteria.getTaskName().isBlank()) {
      parameters.put("exeTaskName", criteria.getTaskName().trim());
    }
    // The state quick filter sets the status and the deferred filter (validated not to come with them)
    Optional<ExecutionState> state = getState(criteria);
    List<Integer> statusList = state.map(option -> List.of(option.getStatus().getValue()))
      .orElse(criteria.getStatuses());
    Integer deferred = state.map(ExecutionState::getDeferred).orElse(criteria.getDeferred());
    if (statusList != null && !statusList.isEmpty()) {
      ArrayNode statuses = parameters.putArray("exeStatuses");
      statusList.forEach(statuses::add);
    }
    List<String> groups = Optional.ofNullable(criteria.getOrigins()).orElse(List.of()).stream()
      .map(ExecutionOrigin::fromCode)
      .flatMap(Optional::stream)
      .map(ExecutionOrigin::getGroup)
      .toList();
    if (!groups.isEmpty()) {
      ArrayNode groupList = parameters.putArray("exeGroups");
      groups.forEach(groupList::add);
    }
    if (deferred != null) {
      // Waiting (1) or not waiting (0) for the confirmation of a deferred answer: running executions with or without
      // an AweSchExeExt row, compared through COALESCE(Ext.ExeTsk, -1)
      parameters.put("exeRunningStatus", TaskStatus.JOB_RUNNING.getValue());
      parameters.put(deferred == 1 ? "exeWaiting" : "exeNotWaiting", NO_DEFERRED_ROW);
    }
    parseWebDate("startFrom", criteria.getStartFrom())
      .ifPresent(day -> parameters.put("exeStartFrom", formatTimestamp(day.atStartOfDay())));
    parseWebDate("startTo", criteria.getStartTo())
      .ifPresent(day -> parameters.put("exeStartBefore", formatTimestamp(day.plusDays(1).atStartOfDay())));
    if (criteria.getWithinHours() != null) {
      LocalDateTime since = LocalDateTime.ofInstant(clock.instant().minus(Duration.ofHours(criteria.getWithinHours())),
        ZoneId.systemDefault());
      parameters.put("exeStartSince", formatTimestamp(since));
    }
    return parameters;
  }

  /**
   * Validate the filters and the page of the execution list: a value that is not valid is refused, never ignored,
   * so the list is never wider than the filters ask for
   *
   * @param criteria Filters and page
   * @throws InvalidExecutionFilterException A value is not valid
   */
  public void validate(ExecutionListCriteria criteria) throws InvalidExecutionFilterException {
    validateStatusesAndOrigins(criteria);
    validateDeferredAndState(criteria);
    parseWebDate("startFrom", criteria.getStartFrom());
    parseWebDate("startTo", criteria.getStartTo());
    checkMinimum("taskId", criteria.getTaskId(), 1);
    checkMinimum("withinHours", criteria.getWithinHours(), 1);
    checkMinimum("page", criteria.getPage(), 1);
    checkMinimum("max", criteria.getMax(), 1);
    if (criteria.getMax() != null && criteria.getMax() > MAX_PAGE_SIZE) {
      throw invalidFilter("max", criteria.getMax());
    }
    validateSort(criteria);
  }

  /**
   * Validate the statuses and the origins of the execution list: every one has to be known
   */
  private void validateStatusesAndOrigins(ExecutionListCriteria criteria) throws InvalidExecutionFilterException {
    for (Integer status : Optional.ofNullable(criteria.getStatuses()).orElse(List.of())) {
      if (Arrays.stream(TaskStatus.values()).noneMatch(known -> Integer.valueOf(known.getValue()).equals(status))) {
        throw invalidFilter("statuses", status);
      }
    }
    for (String origin : Optional.ofNullable(criteria.getOrigins()).orElse(List.of())) {
      if (ExecutionOrigin.fromCode(origin).isEmpty()) {
        throw invalidFilter("origins", origin);
      }
    }
  }

  /**
   * Validate the deferred filter (0 or 1) and the state quick filter: an unknown state, or a state with its own status
   * or deferred filter, is refused
   */
  private void validateDeferredAndState(ExecutionListCriteria criteria) throws InvalidExecutionFilterException {
    if (criteria.getDeferred() != null && criteria.getDeferred() != 0 && criteria.getDeferred() != 1) {
      throw invalidFilter(DEFERRED_STATUS, criteria.getDeferred());
    }
    if (hasState(criteria) && (ExecutionState.fromCode(criteria.getState().trim()).isEmpty()
      || (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) || criteria.getDeferred() != null)) {
      throw invalidFilter(STATE_FILTER, criteria.getState());
    }
  }

  /**
   * Validate the sort of the execution list: known columns and directions only
   */
  private void validateSort(ExecutionListCriteria criteria) throws InvalidExecutionFilterException {
    for (ExecutionSort column : Optional.ofNullable(criteria.getSort()).orElse(List.of())) {
      if (column.getId() == null || !SORT_FIELDS.containsKey(column.getId())) {
        throw invalidFilter(SORT_FILTER, column.getId());
      }
      if (column.getDirection() == null || !SORT_DIRECTIONS.contains(column.getDirection().toUpperCase(Locale.ROOT))) {
        throw invalidFilter(SORT_FILTER, column.getDirection());
      }
    }
  }

  /**
   * Check if the criteria sets the state quick filter (a blank state is every state)
   */
  private static boolean hasState(ExecutionListCriteria criteria) {
    return criteria.getState() != null && !criteria.getState().isBlank();
  }

  /**
   * Option of the state quick filter of a validated criteria
   */
  private static Optional<ExecutionState> getState(ExecutionListCriteria criteria) {
    return hasState(criteria) ? ExecutionState.fromCode(criteria.getState().trim()) : Optional.empty();
  }

  /**
   * Check that an optional number is not below its minimum
   */
  private void checkMinimum(String filter, Integer value, int minimum) throws InvalidExecutionFilterException {
    if (value != null && value < minimum) {
      throw invalidFilter(filter, value);
    }
  }

  /**
   * Translated validation error of a filter value
   *
   * @param filter Filter name (field of the criteria)
   * @param value  Refused value
   * @return Validation error
   */
  private InvalidExecutionFilterException invalidFilter(String filter, Object value) {
    return new InvalidExecutionFilterException(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_TITLE"),
      serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", filter, String.valueOf(value)));
  }

  /**
   * Parse a web date (dd/MM/yyyy, a real day)
   *
   * @param filter Filter name
   * @param date   Web date
   * @return Day, empty when there is no date
   * @throws InvalidExecutionFilterException The date is not a web date
   */
  private Optional<LocalDate> parseWebDate(String filter, String date) throws InvalidExecutionFilterException {
    if (date == null || date.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(date.trim(), STRICT_WEB_DATE));
    } catch (DateTimeParseException exc) {
      throw invalidFilter(filter, date);
    }
  }

  /**
   * Format a date time as the timestamp text of the query variables
   *
   * @param dateTime Date time
   * @return Timestamp text
   */
  private String formatTimestamp(LocalDateTime dateTime) {
    return DateFormats.WEB_TIMESTAMP_MS.format(dateTime);
  }

  /**
   * Decorate an execution row with its progress (running executions), its elapsed or execution time, the text of who
   * launched it and its execution log locator
   *
   * @param row              Execution row
   * @param taskId           Task id of the execution
   * @param averageExecution Average execution time of the task in milliseconds
   */
  private void decorateExecution(Map<String, CellData> row, Integer taskId, long averageExecution) {
    CellData progress = new CellData();
    CellData averageTime = new CellData();
    Integer executionId = row.get(TASK_EXECUTION_ID).getIntegerValue();

    // Calculate progress
    CellData status = row.get(STATUS);
    CellData executionTime = row.get(TASK_EXECUTION_TIME);
    CellData launchType = row.get(TASK_LAUNCH_TYPE_LIST);
    CellData launchedBy = new CellData(getLaunchedByText(launchType.getStringValue(),
      row.get(TASK_LAUNCHED_BY_LIST).getStringValue()));
    CellData executionLogPathCell = new CellData(executionLogStore.locatorNode(
      new ExecutionKey(taskId, executionId)));
    Instant initialDate = LegacyDateConverter.asInstant(row.get(EXECUTION_START).getDateValue());

    if (TaskStatus.JOB_RUNNING.equals(TaskStatus.valueOf(status.getIntegerValue()))) {
      progress = new CellData(getProgressNode(getProgress(initialDate, averageExecution)));
      averageTime = new CellData(TimeUtil.formatTime(getElapsedTime(initialDate), false));
    } else if (executionTime != null && executionTime.getIntegerValue() != null) {
      averageTime = new CellData(TimeUtil.formatTime(executionTime.getIntegerValue()));
    }

    row.put(TASK_EXECUTION_PROGRESS, progress);
    row.put(TASK_EXECUTION_TIME, averageTime);
    row.put(TASK_LAUNCHED_BY_LIST, launchedBy);
    row.put(TASK_EXECUTION_LOG_PATH, executionLogPathCell);
  }

  /**
   * Decorate an execution row of the activity screen with its origin code, its deferred state and its flow run: a
   * running execution with an AweSchExeExt row is waiting for the confirmation of its deferred answer until its due
   * date, and its status shows that wait instead of the running status
   *
   * @param row Execution row
   * @throws AWException Error reading the waiting status
   */
  private void decorateActivity(Map<String, CellData> row) throws AWException {
    row.put(EXECUTION_ORIGIN, new CellData(ExecutionOrigin.fromGroup(row.get(TASK_LAUNCH_TYPE_LIST).getStringValue())
      .map(ExecutionOrigin::getCode).orElse("")));
    row.put(EXECUTION_ORIGIN_TONE, new CellData(NEUTRAL_TONE));

    CellData deferredExecution = row.get(DEFERRED_EXECUTION);
    boolean waiting = TaskStatus.JOB_RUNNING.equals(TaskStatus.valueOf(row.get(STATUS).getIntegerValue()))
      && deferredExecution != null && !deferredExecution.isEmpty();
    Date dueDate = waiting ? row.get(EXECUTION_DUE_DATE).getDateValue() : null;
    row.put(EXECUTION_DEFERRED, new CellData(waiting ? 1 : 0));
    row.put(EXECUTION_DUE_DATE, dueDate == null ? new CellData() : new CellData(dueDate));
    String dueTime = dueDate == null ? null : formatDueTime(LegacyDateConverter.asLocalDateTime(dueDate));
    row.put(EXECUTION_DUE_TIME, dueTime == null ? new CellData() : new CellData(dueTime));
    row.put(EXECUTION_DUE_LABEL, dueTime == null ? new CellData()
      : new CellData(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_DUE_SHORT", dueTime)));
    if (waiting) {
      ObjectNode waitingStatus = getWaitingStatus(row.get(STATUS).getStringValue());
      row.put(EXECUTION_STATUS, new CellData(waitingStatus));
      row.put(EXECUTION_STATUS_TEXT, new CellData(waitingStatus.get(JSON_LABEL_PARAMETER).asString()));
      row.put(EXECUTION_STATUS_ICON, new CellData(waitingStatus.get(JSON_ICON_PARAMETER).asString()));
      row.put(EXECUTION_STATUS_TONE, new CellData(queryService.findLabel("ActStaTon", DEFERRED_STATUS)));
    }
    String runId = Optional.ofNullable(row.get(EXECUTION_RUN)).map(CellData::getStringValue).orElse("");
    // The readable run alone (the root task and execution, else the short id), for the views that read a plain value
    // of the row (the side panel)
    row.put(EXECUTION_RUN_LABEL, new CellData(runId.isEmpty() ? EMPTY_VALUE : getFlowRunLabel(row, runId)));
    row.put(EXECUTION_RUN, getFlowRun(row));
  }

  /**
   * Due time of an execution waiting for a confirmation: the time alone when it is due today, the day and the time
   * (dd/MM HH:mm) on another day of this year, and the full date (dd/MM/yyyy HH:mm) in another year, so a due time of
   * tomorrow never reads as a time of today
   *
   * @param due Due date, local time
   * @return Due time text
   */
  private String formatDueTime(LocalDateTime due) {
    LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.systemDefault());
    if (due.toLocalDate().equals(today)) {
      return DUE_TIME_FORMAT.format(due);
    }
    return (due.getYear() == today.getYear() ? DUE_DAY_TIME_FORMAT : DUE_DATE_TIME_FORMAT).format(due);
  }

  /**
   * Status of an execution waiting for the confirmation of its deferred answer: icon, tone and label of the
   * waiting status of the activity enumerated groups (ActStaIco, ActStaSty, ActStaTyp)
   *
   * @param status Execution status
   * @return Status node, as the ExeStaIco compound of the query
   * @throws AWException Error reading the enumerated groups
   */
  private ObjectNode getWaitingStatus(String status) throws AWException {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(JSON_ICON_PARAMETER, queryService.findLabel("ActStaIco", DEFERRED_STATUS));
    node.put(JSON_STYLE_PARAMETER, queryService.findLabel("ActStaSty", DEFERRED_STATUS));
    node.put(JSON_VALUE_PARAMETER, status);
    node.put(JSON_LABEL_PARAMETER, serviceConfig.getLocale(queryService.findLabel("ActStaTyp", DEFERRED_STATUS)));
    return node;
  }

  /**
   * Flow run of an execution: the run id as value (the run screen reads it), as label its root execution in short
   * ({@code #<root execution>}) and as title the whole run ({@code <root task name> #<root execution>}); when the run
   * has no stored root, the first characters of its id as label and the whole id as title
   *
   * @param row Execution row
   * @return Flow run cell, empty when the execution has no run
   */
  private CellData getFlowRun(Map<String, CellData> row) {
    String runId = Optional.ofNullable(row.get(EXECUTION_RUN)).map(CellData::getStringValue).orElse("");
    if (runId.isEmpty()) {
      return new CellData();
    }
    String rootExecution = Optional.ofNullable(row.get(RUN_ROOT_EXECUTION)).map(CellData::getStringValue).orElse("");
    boolean withRoot = !getRunRootName(row).isEmpty() && !rootExecution.isEmpty();
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(JSON_VALUE_PARAMETER, runId);
    node.put(JSON_LABEL_PARAMETER, withRoot ? "#" + rootExecution : getShortRunId(runId));
    node.put(JSON_TITLE_PARAMETER, withRoot ? getFlowRunLabel(row, runId) : runId);
    return new CellData(node);
  }

  /**
   * Readable flow run of an execution: {@code <root task name> #<root execution>}, or the first characters of the run
   * id when the run has no stored root
   *
   * @param row   Execution row
   * @param runId Flow run id
   * @return Flow run label
   */
  static String getFlowRunLabel(Map<String, CellData> row, String runId) {
    String rootName = getRunRootName(row);
    String rootExecution = Optional.ofNullable(row.get(RUN_ROOT_EXECUTION)).map(CellData::getStringValue).orElse("");
    return rootName.isEmpty() || rootExecution.isEmpty() ? getShortRunId(runId) : rootName + " #" + rootExecution;
  }

  private static String getRunRootName(Map<String, CellData> row) {
    return Optional.ofNullable(row.get(RUN_ROOT_NAME)).map(CellData::getStringValue).orElse("");
  }

  private static String getShortRunId(String runId) {
    return runId.substring(0, Math.min(SHORT_RUN_ID_LENGTH, runId.length()));
  }

  /**
   * Retrieve progress node
   *
   * @param percentage Percentage
   * @return Progress node
   */
  public ObjectNode getProgressNode(long percentage) {
    ObjectNode progressBar = JsonNodeFactory.instance.objectNode();
    if (percentage > 100L) {
      progressBar.put(JSON_VALUE_PARAMETER, 100);
      progressBar.put(JSON_STYLE_PARAMETER, "progress-bar-danger");
      progressBar.put(JSON_LABEL_PARAMETER, "");
    } else {
      progressBar.put(JSON_VALUE_PARAMETER, percentage);
      progressBar.put(JSON_LABEL_PARAMETER, percentage + "%");
    }
    return progressBar;
  }

  /**
   * Retrieve the progress of a running execution as a percentage of the average execution time
   *
   * @param initialDate      Execution start
   * @param averageExecution Average execution time in milliseconds
   * @return Progress percentage, 101 when there is no average execution time
   */
  public long getProgress(Instant initialDate, long averageExecution) {
    long currentExecution = clock.millis() - initialDate.toEpochMilli();
    return averageExecution == 0 ? 101L : (currentExecution * 100) / averageExecution;
  }

  /**
   * Retrieve elapsed time from a start instant
   *
   * @param startDate Start instant
   * @return Elapsed time in milliseconds
   */
  public Integer getElapsedTime(Instant startDate) {
    return (int) (clock.millis() - startDate.toEpochMilli());
  }

  /**
   * Load execution screen parameters
   *
   * @param path    Execution log locator ({@code "{taskId}-{executionId}"})
   * @param address Button address
   * @return ServiceData
   * @throws AWException Error loading the execution screen
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address) throws AWException {
    return loadExecutionScreen(path, address, null);
  }

  /**
   * Load execution screen parameters. The execution is the row of the button address (a cell button that sends its
   * address), else the execution log locator of the button value, else the locator of the selected row (a client that
   * sends neither, as the React cell buttons, selects the row of the cell before the call)
   *
   * @param path            Execution log locator of the button value ({@code "{taskId}-{executionId}"})
   * @param address         Button address
   * @param selectedLocator Execution log locator of the selected row
   * @return ServiceData, without actions when no execution is given
   * @throws AWException Error loading the execution screen
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address, String selectedLocator) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Refresh execution screen
    String addressRow = address == null || address.get("row") == null ? null : address.get("row").asString("");
    ExecutionKey key = Optional.ofNullable(ExecutionKey.of(addressRow))
      .or(() -> Optional.ofNullable(ExecutionKey.of(path)))
      .or(() -> Optional.ofNullable(ExecutionKey.of(selectedLocator)))
      .orElse(null);
    if (key == null) {
      log.warn("[Execution screen] No execution to load: row {}, locator {}, selected locator {}", addressRow, path,
        selectedLocator);
      return serviceData;
    }
    executionLogStore.applyViewerSelection(path == null || path.isBlank() ? key.mdcKey() : path, key, serviceData);

    TaskExecution taskExecution = executionRepository.getTaskExecution(key.taskId(), key.executionId());
    if (taskExecution != null) {
      refreshExecutionScreen(taskExecution, serviceData);
    }

    return serviceData;
  }

  /**
   * Retrieve the execution log window from a line offset (get-execution-log action), in both file and
   * database mode: the log is resolved by task and execution id through the active store, never by a client path.
   * <p>
   * Single-response delivery: the store's lines always land in {@code LOG_CONTENT}, paired with
   * {@code LOG_CONTENT_REPLACE} telling the widget whether to append them or atomically replace
   * its current content, never a separate reset/filter round trip.
   *
   * <p>
   * The log of an execution that has ended (neither running nor queued) also carries an {@code update-controller}
   * action without target that turns the autorefresh of the viewer that asked for it off: the screens do not hear
   * when an execution ends, so the viewer stops following it here. The status is read before the lines, so an
   * execution that had ended before this read has all its lines in it.
   *
   * @param taskId        Task identifier
   * @param executionId   Execution identifier
   * @param offset        Line offset to read from
   * @param clientVersion Window version the caller currently holds, as previously returned in
   *                      {@code LOG_CONTENT_VERSION}; {@code null} when absent
   * @return ServiceData
   * @throws AWException Error reading the execution log
   */
  public ServiceData getExecutionLog(Integer taskId, Integer executionId, Integer offset, String clientVersion)
    throws AWException {
    ServiceData serviceData = new ServiceData();
    boolean ended = hasEnded(executionRepository.getTaskExecution(taskId, executionId));
    ExecutionLogPage page = executionLogStore.read(new ExecutionKey(taskId, executionId), offset, clientVersion);

    serviceData.addVariable("LOG_CONTENT", new CellData(page.lines()));
    serviceData.addVariable("LOG_CONTENT_REPLACE", new CellData(page.replace()));
    serviceData.addVariable("LOG_CONTENT_VERSION", new CellData(page.version()));

    if (ended) {
      addSilentAction(serviceData, new UpdateControllerActionBuilder()
        .addParameter("attribute", "autorefresh")
        .addParameter("value", 0));
    }

    return serviceData;
  }

  /**
   * Check whether an execution has ended: it exists and it is neither running (nor waiting) nor queued
   *
   * @param execution Execution, null when it does not exist
   * @return The execution has ended
   */
  private static boolean hasEnded(TaskExecution execution) {
    if (execution == null || execution.getStatus() == null) {
      return false;
    }
    TaskStatus status = TaskStatus.valueOf(execution.getStatus());
    return status != TaskStatus.JOB_RUNNING && status != TaskStatus.JOB_QUEUED;
  }

  /**
   * Reload execution task parameters
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service Data
   * @throws AWException Error reloading execution screen
   */
  public ServiceData reloadExecutionScreen(Integer taskId, Integer executionId) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Refresh execution screen
    if (taskId != null && executionId != null) {
      TaskExecution taskExecution = executionRepository.getTaskExecution(taskId, executionId);
      if (taskExecution != null) {
        refreshExecutionScreen(taskExecution, serviceData);
      }
    }

    return serviceData;
  }

  /**
   * Refresh execution screen data
   *
   * @param taskExecution Task execution
   * @param serviceData   Service data
   */
  private void refreshExecutionScreen(TaskExecution taskExecution, ServiceData serviceData) throws AWException {
    String status = taskExecution.getStatus().toString();
    String group = taskExecution.getGroupId();
    String startTime = formatTimestamp(taskExecution.getInitialDate());
    String endTime = taskExecution.getEndDate() == null ? EMPTY_VALUE : formatTimestamp(taskExecution.getEndDate());
    String executionTime = taskExecution.getExecutionTime() == null
      ? EMPTY_VALUE
      : TimeUtil.formatTime(taskExecution.getExecutionTime());
    addSilentAction(serviceData, new SelectActionBuilder("task-id", taskExecution.getName()));
    addSilentAction(serviceData, new SelectActionBuilder("execution-id", taskExecution.getExecutionId()));
    addSilentAction(serviceData, new SelectActionBuilder("execution-start-time", startTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-end-time", endTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-time", executionTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-information",
      orEmpty(taskExecution.getDescription())));
    addSilentAction(serviceData, new SelectActionBuilder("execution-deferred",
      deferredTrace(deferredRepository.find(taskExecution.getTaskId(), taskExecution.getExecutionId()))));
    addSilentAction(serviceData, new SelectActionBuilder("execution-title",
      serviceConfig.getLocale(queryService.findLabel("StaTit", status))));
    addSilentAction(serviceData, new SelectActionBuilder("execution-launched-by",
      orEmpty(getLaunchedByText(group, taskExecution.getLaunchedBy()))));
    addSilentAction(serviceData, new UpdateControllerActionBuilder("execution-launched-by", "icon",
      queryService.findLabel("LchTxtIcoNam", group)));
    addSilentAction(serviceData, new RemoveCssClassActionBuilder("#execution-header,#execution-icon,"
      + "[criterion-id='execution-launched-by'] .text-icon,[criterion-id='execution-information'] .input",
      "bg-success", "bg-danger", "bg-warning", "bg-info",
      "fa-check", "fa-times", "fa-exclamation-triangle", "fa-chain-broken", "fa-refresh", "fa-spin",
      "text-danger", "text-info", "text-success", "text-light-gray", "input-sm"));
    addSilentAction(serviceData, new AddCssClassActionBuilder("#execution-header",
      queryService.findLabel("StaBg", status)));
    addSilentAction(serviceData, new AddCssClassActionBuilder("#execution-icon",
      queryService.findLabel("StaIco", status)));
    addSilentAction(serviceData, new AddCssClassActionBuilder("[criterion-id='execution-launched-by'] .text-icon",
      queryService.findLabel("LchTxtSty", group)));
  }

  /**
   * Progress of an execution as the fields of a card: its elapsed time while it runs, else its execution time, as the
   * value and the average time of its task as the target (the card draws the bar), in seconds, minutes or hours as
   * the longest of both needs
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return One row with the fields of the card (title, value, unit, target, numberDecimals, description, iconTone),
   * no row without execution or start
   * @throws AWException Error reading the execution or the average time
   */
  public ServiceData getExecutionProgress(Integer taskId, Integer executionId) throws AWException {
    DataList dataList = new DataList();
    TaskExecution execution = taskId == null || executionId == null
      ? null
      : executionRepository.getTaskExecution(taskId, executionId);
    Long time = execution == null ? null : getExecutionMillis(execution);
    if (time != null) {
      boolean running = TaskStatus.JOB_RUNNING.equals(TaskStatus.valueOf(execution.getStatus()));
      long average = Optional.ofNullable(executionRepository.getAverageTime(taskId)).orElse(0);
      long longest = Math.max(time, average);
      ProgressUnit unit = ProgressUnit.of(longest);
      Map<String, CellData> row = new HashMap<>();
      row.put("title", new CellData(running ? "SCREEN_TEXT_ACTIVITY_ELAPSED" : "SCREEN_TEXT_EXECUTION_TIME"));
      row.put(JSON_VALUE_PARAMETER, new CellData(unit.convert(time)));
      row.put("unit", new CellData(unit.getSymbol()));
      row.put("numberDecimals", new CellData(unit.getDecimals()));
      if (average > 0) {
        row.put("target", new CellData(unit.convert(average)));
        row.put("description", new CellData(serviceConfig.getLocale("SCREEN_TEXT_ACTIVITY_AVERAGE",
          TimeUtil.formatTime((int) average, false))));
      } else {
        row.put("description", new CellData("SCREEN_TEXT_ACTIVITY_NO_AVERAGE"));
      }
      row.put("iconTone", new CellData("info"));
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Launch parameters of an execution as the rows of the parameters grid of the activity side panel
   *
   * @param parameters Launch parameters of the flow run of the execution, by name in display order
   * @return One row per parameter (ParNam, ParVal), the dash as the value of an empty one
   */
  public ServiceData getExecutionParameters(Map<String, String> parameters) {
    DataList dataList = new DataList();
    parameters.forEach((name, value) -> {
      Map<String, CellData> row = new HashMap<>();
      row.put("ParNam", new CellData(name));
      row.put("ParVal", new CellData(orEmpty(value)));
      dataList.addRow(row);
    });
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Time of an execution: the elapsed time while it runs, else its stored execution time, else the time between its
   * start and its end
   *
   * @param execution Execution
   * @return Time in milliseconds, null when it has not started
   */
  private Long getExecutionMillis(TaskExecution execution) {
    Date initialDate = execution.getInitialDate();
    if (initialDate == null) {
      return null;
    }
    long start = initialDate.getTime();
    if (TaskStatus.JOB_RUNNING.equals(TaskStatus.valueOf(execution.getStatus()))) {
      return Math.max(0L, clock.millis() - start);
    }
    if (execution.getExecutionTime() != null) {
      return execution.getExecutionTime().longValue();
    }
    Date endDate = execution.getEndDate();
    return endDate == null ? null : endDate.getTime() - start;
  }

  /**
   * Unit of the progress card of an execution
   */
  private enum ProgressUnit {
    SECONDS("s", 1_000L, 0),
    MINUTES("min", 60_000L, 1),
    HOURS("h", 3_600_000L, 1);

    private final String symbol;
    private final long millis;
    private final int decimals;

    ProgressUnit(String symbol, long millis, int decimals) {
      this.symbol = symbol;
      this.millis = millis;
      this.decimals = decimals;
    }

    static ProgressUnit of(long longest) {
      if (longest < PROGRESS_SECONDS_LIMIT) {
        return SECONDS;
      }
      return longest < PROGRESS_MINUTES_LIMIT ? MINUTES : HOURS;
    }

    String getSymbol() {
      return symbol;
    }

    int getDecimals() {
      return decimals;
    }

    /**
     * Time in this unit, rounded to its decimals
     */
    double convert(long time) {
      double scale = Math.pow(10, decimals);
      return Math.round(time * scale / millis) / scale;
    }
  }

  /**
   * Text of a value, or the dash of the empty values
   *
   * @param value Value
   * @return Value, or the dash when it is null or blank
   */
  private static String orEmpty(String value) {
    return value == null || value.isBlank() ? EMPTY_VALUE : value;
  }

  /**
   * Text of the deferred state of an execution: reference and deadline, who closed it and the cancellation notice
   *
   * @param deferred Deferred state, or null when the execution was never deferred
   * @return Trace text, empty when the execution was never deferred
   */
  private String deferredTrace(DeferredExecution deferred) {
    if (deferred == null) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    parts.add(serviceConfig.getLocale("SCHEDULER_DEFERRED_TRACE",
      deferred.getExternalReference() == null ? "-" : deferred.getExternalReference(),
      orDash(deferred.getDueDate())));
    if (deferred.getCompletionSource() != null) {
      parts.add(serviceConfig.getLocale("SCHEDULER_DEFERRED_CLOSED",
        serviceConfig.getLocale("SCHEDULER_DEFERRED_SOURCE_" + deferred.getCompletionSource()),
        orDash(deferred.getCompletionDate())));
    }
    if ("SENT".equals(deferred.getCancellationStatus())) {
      parts.add(serviceConfig.getLocale("SCHEDULER_DEFERRED_NOTICE_SENT"));
    } else if ("FAILED".equals(deferred.getCancellationStatus())) {
      parts.add(serviceConfig.getLocale("SCHEDULER_DEFERRED_NOTICE_FAILED",
        deferred.getCancellationError() == null ? "-" : deferred.getCancellationError()));
    }
    return String.join(" · ", parts);
  }

  private String orDash(Date date) {
    return date == null ? "-" : formatTimestamp(date);
  }

  /**
   * Add an asynchronous and silent client action
   *
   * @param serviceData Service data
   * @param builder     Client action builder
   */
  private void addSilentAction(ServiceData serviceData, ClientActionBuilder<?> builder) {
    builder.setAsync(true);
    builder.setSilent(true);
    serviceData.addClientAction(builder.build());
  }

  /**
   * Format an execution date as web timestamp with milliseconds
   *
   * @param date Execution date (TaskExecution bean boundary)
   * @return Web timestamp with milliseconds
   */
  private String formatTimestamp(Date date) {
    return DateTimeFormatting.formatWebTimestampWithMs(LegacyDateConverter.asLocalDateTime(date));
  }

  /**
   * Retrieve launchedBy text
   *
   * @param launchType Launch type
   * @param launchedBy Launched by
   * @return Launched by text
   */
  private String getLaunchedByText(String launchType, String launchedBy) {
    return DEPENDENCY_GROUP.equalsIgnoreCase(launchType)
      ? serviceConfig.getLocale("PARAMETER_TASK") + " " + launchedBy
      : launchedBy;
  }
}
