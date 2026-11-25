package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Cause;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.CauseKind;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.EdgeState;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Execution;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Node;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.NodeState;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Parent;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.QueryService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_FLOW_RUN_GRAPH_TASKS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_FLOW_RUN_HEADER_ROW;

/**
 * Flow run page (#993): one flow run, by its id. The summary (one row) tells its status with the rule written out and
 * the task the status points to, who launched it, its start, end and duration, its root and how many tasks it has in
 * each state. The graph has one row per task of the run and parent (the root without parent), as the getFlowGraph rows
 * the flow-graph widget reads: each task with its last execution in the run (a relaunched task once), its state, dates,
 * duration, deferred and due dates, why it was skipped and, in a join, the parent that closed it and how long it
 * waited; each row with a parent also has the state of that edge in the run. Nothing of it is stored: the rules are
 * {@link FlowRunGraphRules}
 */
public class FlowRunPresenter {

  private static final String RUNS = FlowRunPresenter.class.getName() + ".runs";
  private static final Object RUNS_LOCK = new Object();
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final String RUN_ID = "IdeRun";
  private static final String TASK_ID = "taskId";
  private static final String PARENT_ID = "parentId";
  private static final String TASK_LIST_ID = "IdeTsk";
  private static final String TASK_NAME = "name";
  private static final String EXECUTION_ID = "ExeTsk";
  private static final String STATUS = "Sta";
  private static final String START = "ExeLstTim";
  private static final String END = "ExeEnd";
  private static final String WAITING = "ExeDfr";
  private static final String DUE = "ExeDue";
  private static final String ROOT_TASK = "RunRooTsk";
  private static final String ROOT_NAME = "RunRooNam";
  private static final String ROOT_EXECUTION = "RunRooExe";
  private static final String RUN_START = "RunIni";
  private static final String RUN_END = "RunEnd";
  private static final String RUN_STATUS = "RunSta";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String DURATION = "duration";
  private static final String LAUNCHED_BY = "LchBy";
  private static final String RUN_DURATION = "ExeTim";
  private static final String RUN_PROGRESS = "RunPrgTxt";
  private static final String DESCRIPTION = "ExeDes";
  private static final String FACT_SEPARATOR = " · ";
  /**
   * Fields of the root execution that tell who launched the run (its origin and its launcher)
   */
  private static final List<String> LAUNCH_FIELDS = List.of("TypLch", "ExeOri", "ExeOriTxt", "ExeOriIcn", "ExeOriTon",
    LAUNCHED_BY);
  /**
   * Fields of the last execution of a task copied to its graph rows: its status pill, the due time of a waiting one,
   * its description, its time and its log locator
   */
  private static final List<String> EXECUTION_FIELDS = List.of("ExeStaTxt", "ExeStaIcn", "ExeStaTon", "ExeDueTim",
    "ExeDueLbl", DESCRIPTION, RUN_DURATION, "ExeEla", LAUNCHED_BY, "TypLch", "ExecutionLogPath");
  /**
   * Field of the summary that counts the tasks of each state
   */
  private static final Map<NodeState, String> COUNT_FIELDS = new EnumMap<>(Map.ofEntries(
    Map.entry(NodeState.OK, "RunCntOk"), Map.entry(NodeState.ERROR, "RunCntErr"),
    Map.entry(NodeState.WARNING, "RunCntWrn"), Map.entry(NodeState.STOPPED, "RunCntStp"),
    Map.entry(NodeState.RUNNING, "RunCntRun"), Map.entry(NodeState.QUEUED, "RunCntQue"),
    Map.entry(NodeState.INTERRUPTED, "RunCntInt"), Map.entry(NodeState.INFO, "RunCntInf"),
    Map.entry(NodeState.DEFERRED, "RunCntDfr"), Map.entry(NodeState.SKIPPED, "RunCntSkp"),
    Map.entry(NodeState.PENDING, "RunCntPnd")));

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final FlowRunRepository flowRunRepository;
  private final TaskExecutionPresenter executionPresenter;
  private final DeferredExecutionRepository deferredRepository;
  private final ServiceConfig serviceConfig;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param queryService       Query service
   * @param queryUtil          Query utilities
   * @param flowRunRepository  Flow run repository (join rows)
   * @param executionPresenter Execution presenter (the executions of the run decorated as the activity rows)
   * @param deferredRepository Deferred execution repository (when a waiting execution was deferred)
   * @param serviceConfig      Service configuration giving access to the locales
   * @param clock              Clock of the durations of what has not ended
   */
  public FlowRunPresenter(QueryService queryService, QueryUtil queryUtil, FlowRunRepository flowRunRepository,
                          TaskExecutionPresenter executionPresenter, DeferredExecutionRepository deferredRepository,
                          ServiceConfig serviceConfig, Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.flowRunRepository = flowRunRepository;
    this.executionPresenter = executionPresenter;
    this.deferredRepository = deferredRepository;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
  }

  /**
   * Summary of a flow run
   *
   * @param runId Flow run identifier
   * @return One row, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getSummary(String runId) throws AWException {
    DataList dataList = new DataList();
    Optional<Run> run = read(runId);
    if (run.isPresent()) {
      dataList.addRow(getSummaryRow(run.get()));
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Graph of a flow run: one row per task of the run and parent
   *
   * @param runId Flow run identifier
   * @return Rows of the graph, none when the run does not exist
   * @throws AWException The run cannot be read
   */
  public ServiceData getGraph(String runId) throws AWException {
    DataList dataList = new DataList();
    Optional<Run> run = read(runId);
    if (run.isPresent()) {
      for (Integer taskId : run.get().order()) {
        Map<String, CellData> node = getNode(run.get(), taskId);
        List<Integer> parents = run.get().parents(taskId);
        if (parents.isEmpty()) {
          dataList.addRow(getGraphRow(run.get(), taskId, null, node));
        }
        for (Integer parent : parents) {
          dataList.addRow(getGraphRow(run.get(), taskId, parent, node));
        }
      }
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Everything the page reads of a run
   *
   * @param runId      Run identifier
   * @param header     The run (AweSchRun with the name of its root)
   * @param root       Root task
   * @param tasks      Definition of each task of the run (name, launch, options)
   * @param allParents Parents of each task (every dependency), in their order
   * @param scope      Tasks of the run
   * @param executions Executions of each task in the run
   * @param last       Last execution of each task in the run, decorated as an activity row
   * @param joins      Join state of each task of the run
   * @param states     State of each task of the run
   * @param order      Tasks of the run in the order they ran (the ones without execution last)
   * @param ended      The run has ended
   */
  private record Run(String runId, Map<String, CellData> header, Integer root, Map<Integer, Map<String, CellData>> tasks,
                     Map<Integer, List<Integer>> allParents, Set<Integer> scope,
                     Map<Integer, List<Execution>> executions,
                     Map<Integer, Map<String, CellData>> last, Map<Integer, String> joins,
                     Map<Integer, NodeState> states, List<Integer> order, boolean ended) {

    List<Integer> parents(Integer taskId) {
      return allParents.getOrDefault(taskId, List.of()).stream().filter(scope::contains).toList();
    }

    Integer status(Integer taskId) {
      return Optional.ofNullable(last.get(taskId)).map(row -> integer(row, STATUS)).orElse(null);
    }

    Parent parent(Integer taskId) {
      Map<String, CellData> task = tasks.get(taskId);
      return new Parent(taskId, status(taskId), states.get(taskId) == NodeState.SKIPPED,
        isTrue(task, "onWarning"), isTrue(task, "onError"));
    }

    String name(Integer taskId) {
      return Optional.ofNullable(tasks.get(taskId)).map(task -> text(task, TASK_NAME)).orElse("");
    }
  }

  /**
   * The run as the page reads it, read once per request and run (the summary and the graph of a page load share it);
   * outside a request it is read on every call
   */
  private Optional<Run> read(String runId) throws AWException {
    if (!FlowRunGraphRules.isRunId(runId)) {
      return Optional.empty();
    }
    RequestAttributes request = RequestContextHolder.getRequestAttributes();
    if (request == null) {
      return readRun(runId);
    }
    CompletableFuture<Optional<Run>> run = new CompletableFuture<>();
    CompletableFuture<Optional<Run>> shared = getRequestRuns(request).putIfAbsent(runId, run);
    if (shared != null) {
      return join(shared);
    }
    try {
      run.complete(readRun(runId));
    } catch (AWException | RuntimeException exc) {
      run.completeExceptionally(exc);
    } catch (Error error) {
      // Complete the shared run anyway, so the other threads of the request fail instead of waiting for ever
      run.completeExceptionally(error);
      throw error;
    }
    return join(run);
  }

  /**
   * Runs read in the request, by id: one map per request, shared by the threads of the request
   */
  @SuppressWarnings("unchecked")
  private static Map<String, CompletableFuture<Optional<Run>>> getRequestRuns(RequestAttributes request) {
    synchronized (RUNS_LOCK) {
      Object runs = request.getAttribute(RUNS, RequestAttributes.SCOPE_REQUEST);
      if (runs == null) {
        runs = new ConcurrentHashMap<String, CompletableFuture<Optional<Run>>>();
        request.setAttribute(RUNS, runs, RequestAttributes.SCOPE_REQUEST);
      }
      return (Map<String, CompletableFuture<Optional<Run>>>) runs;
    }
  }

  private static Optional<Run> join(CompletableFuture<Optional<Run>> run) throws AWException {
    try {
      return run.get();
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new AWException("Flow run", exc.getMessage(), exc);
    } catch (ExecutionException exc) {
      if (exc.getCause() instanceof AWException error) {
        throw error;
      }
      if (exc.getCause() instanceof Error error) {
        throw error;
      }
      throw (RuntimeException) exc.getCause();
    }
  }

  private Optional<Run> readRun(String runId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("runId", runId);
    List<Map<String, CellData>> headers = queryService.launchPrivateQuery(SCHEDULER_FLOW_RUN_HEADER_ROW, parameters)
      .getDataList().getRows();
    if (headers.isEmpty()) {
      return Optional.empty();
    }
    Map<String, CellData> header = headers.get(0);
    Integer root = integer(header, ROOT_TASK);

    // Dependencies of every task (only the ids): the run is what the root reaches, plus what ran or waited in it
    Map<Integer, List<Integer>> parents = new HashMap<>();
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (TaskDependency dependency : flowRunRepository.getDependencies()) {
      Integer taskId = dependency.getTaskId();
      Integer parentId = dependency.getParentId();
      if (taskId != null && parentId != null
        && !parents.computeIfAbsent(taskId, key -> new ArrayList<>()).contains(parentId)) {
        parents.get(taskId).add(parentId);
        children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(taskId);
      }
    }

    Map<Integer, List<Map<String, CellData>>> executions = new HashMap<>();
    for (Map<String, CellData> execution : executionPresenter.getRunExecutions(runId)) {
      executions.computeIfAbsent(integer(execution, TASK_LIST_ID), key -> new ArrayList<>()).add(execution);
    }
    Map<Integer, String> joins = new HashMap<>();
    for (JoinRow join : flowRunRepository.getJoins(runId)) {
      joins.put(join.taskId(), join.status());
    }

    Set<Integer> scope = new LinkedHashSet<>(root == null ? Set.of() : FlowRules.scope(root, children));
    scope.addAll(executions.keySet());
    scope.addAll(joins.keySet());

    // Definition of the tasks of the run only (name, launch, options)
    Map<Integer, Map<String, CellData>> tasks = new LinkedHashMap<>();
    if (!scope.isEmpty()) {
      ObjectNode byTask = queryUtil.getParameters(null, "1", "0");
      byTask.putArray("sort");
      scope.stream().sorted().forEach(byTask.putArray("taskIds")::add);
      for (Map<String, CellData> row : queryService.launchPrivateQuery(SCHEDULER_FLOW_RUN_GRAPH_TASKS, byTask)
        .getDataList().getRows()) {
        tasks.putIfAbsent(integer(row, TASK_ID), row);
      }
    }
    scope.retainAll(tasks.keySet());

    boolean ended = date(header, RUN_END) != null;
    Map<Integer, List<Execution>> runExecutions = new HashMap<>();
    Map<Integer, Map<String, CellData>> last = new HashMap<>();
    Map<Integer, NodeState> states = new HashMap<>();
    for (Integer taskId : scope) {
      List<Map<String, CellData>> rows = executions.getOrDefault(taskId, List.of());
      Map<Integer, Map<String, CellData>> byExecution = new HashMap<>();
      rows.forEach(row -> byExecution.put(integer(row, EXECUTION_ID), row));
      List<Execution> taskExecutions = rows.stream().map(FlowRunPresenter::asExecution).toList();
      runExecutions.put(taskId, taskExecutions);
      FlowRunGraphRules.lastExecution(taskExecutions)
        .ifPresent(execution -> last.put(taskId, byExecution.get(execution.executionId())));
      Map<String, CellData> execution = last.get(taskId);
      states.put(taskId, FlowRunGraphRules.nodeState(joins.get(taskId),
        execution == null ? null : integer(execution, STATUS), execution != null && isTrue(execution, WAITING), ended));
    }
    List<Integer> order = scope.stream()
      .sorted(Comparator.comparing((Integer taskId) -> Optional.ofNullable(last.get(taskId))
          .map(row -> date(row, START)).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Comparator.naturalOrder()))
      .toList();
    return Optional.of(new Run(runId, header, root, tasks, parents, scope, runExecutions, last, joins, states, order,
      ended));
  }

  /**
   * Node columns of a task, the same on every row of the task
   */
  private Map<String, CellData> getNode(Run run, Integer taskId) throws AWException {
    Map<String, CellData> node = new HashMap<>();
    NodeState state = run.states().get(taskId);
    Map<String, CellData> execution = run.last().get(taskId);
    node.put("state", new CellData(state.getValue()));
    node.put(TASK_LIST_ID, new CellData(taskId));
    node.put("ExeRun", new CellData(run.runId()));
    if (execution == null) {
      executionPresenter.setStatus(node, state.getStatusOption());
      node.put("executionId", new CellData());
      node.put(EXECUTION_ID, new CellData());
      node.put("deferred", new CellData(0));
    } else {
      EXECUTION_FIELDS.forEach(field -> node.put(field, Optional.ofNullable(execution.get(field)).orElseGet(CellData::new)));
      node.put("executionId", execution.get(EXECUTION_ID));
      node.put(EXECUTION_ID, execution.get(EXECUTION_ID));
      Date start = date(execution, START);
      Date end = date(execution, END);
      node.put(START_DATE, format(start));
      node.put(END_DATE, format(end));
      node.put(DURATION, start == null ? new CellData()
        : new CellData(Math.max(0L, (end == null ? clock.millis() : end.getTime()) - start.getTime())));
      boolean deferred = state == NodeState.DEFERRED;
      node.put("deferred", new CellData(deferred ? 1 : 0));
      node.put("dueDate", deferred ? format(date(execution, DUE)) : new CellData());
      node.put("deferredDate", deferred ? format(getDeferredDate(taskId, integer(execution, EXECUTION_ID)))
        : new CellData());
    }
    node.put(DESCRIPTION, Optional.ofNullable(node.get(DESCRIPTION)).orElseGet(CellData::new));

    // Skipped: the parent that did not meet its condition, else the skipped parent
    List<Integer> parents = run.parents(taskId);
    Optional<Cause> skipCause = state == NodeState.SKIPPED
      ? FlowRunGraphRules.skipCause(parents.stream().map(run::parent).toList()) : Optional.empty();
    node.put("skipCause", skipCause.isEmpty() ? new CellData()
      : new CellData(text("SCREEN_TEXT_FLOW_RUN_SKIP_CAUSE", getCauseText(run, skipCause.get()))));

    // Join: the parent that closed it and how long it waited, with the executions of each parent that ended at or
    // before the child started (a parent relaunched afterwards still closed it)
    Instant start = Optional.ofNullable(execution).map(row -> date(row, START)).map(Date::toInstant).orElse(null);
    Map<Integer, Instant> parentEnds = new LinkedHashMap<>();
    parents.forEach(parent -> parentEnds.put(parent, FlowRunGraphRules.endBefore(start,
      run.executions().getOrDefault(parent, List.of())).orElse(null)));
    Optional<Integer> joinedBy = FlowRunGraphRules.joinedBy(start, parentEnds);
    Optional<Duration> wait = FlowRunGraphRules.joinWait(joinedBy.orElse(null), parentEnds);
    node.put("joinedBy", joinedBy.map(CellData::new).orElseGet(CellData::new));
    node.put("joinedByName", joinedBy.map(parent -> new CellData(run.name(parent))).orElseGet(CellData::new));
    node.put("joinedByText", joinedBy.map(parent -> new CellData(text("SCREEN_TEXT_FLOW_RUN_JOINED_BY",
      run.name(parent)))).orElseGet(CellData::new));
    node.put("joinWait", wait.map(Duration::toMillis).map(CellData::new).orElseGet(CellData::new));
    // «waited …» only when it waited: a join whose parents ended at the same time has no wait to tell
    node.put("joinWaitText", wait.filter(duration -> duration.toMillis() > 0)
      .map(duration -> new CellData(text("SCREEN_TEXT_FLOW_RUN_JOIN_WAIT",
        FlowRunGraphRules.formatDuration(duration.toMillis(), true)))).orElseGet(CellData::new));
    return node;
  }

  /**
   * Row of a task and one of its parents (null for a task without parent in the run): the columns of the getFlowGraph
   * rows, the node columns of the task and, with a parent, the condition and the state of the edge
   */
  private Map<String, CellData> getGraphRow(Run run, Integer taskId, Integer parentId, Map<String, CellData> node)
    throws AWException {
    Map<String, CellData> task = run.tasks().get(taskId);
    Map<String, CellData> row = new HashMap<>(node);
    row.put("id", new CellData(taskId + "-" + (parentId == null ? "" : parentId)));
    row.put(TASK_ID, new CellData(taskId));
    row.put(PARENT_ID, parentId == null ? new CellData() : new CellData(parentId));
    for (String field : List.of(TASK_NAME, "launchIcon", "launchLabel", "active", "onWarning", "onError")) {
      row.put(field, Optional.ofNullable(task.get(field)).orElseGet(CellData::new));
    }
    row.put("rootTask", new CellData());
    row.put("rootRun", taskId.equals(run.root()) ? new CellData(run.runId()) : new CellData());
    row.put("runStatus", Optional.ofNullable(run.joins().get(taskId)).map(CellData::new).orElseGet(CellData::new));
    row.put("executionStatus", Optional.ofNullable(run.status(taskId)).map(CellData::new).orElseGet(CellData::new));
    if (parentId == null) {
      List.of("edgeState", "condition", "conditionLabel").forEach(field -> row.put(field, new CellData()));
      return row;
    }
    Parent parent = run.parent(parentId);
    boolean closesJoin = parentId.equals(integer(node, "joinedBy"));
    EdgeState edge = FlowRunGraphRules.edgeState(parent, run.states().get(taskId) == NodeState.SKIPPED, closesJoin,
      run.ended());
    row.put("edgeState", new CellData(edge.getValue()));
    row.put("condition", new CellData(FlowRunGraphRules.condition(parent.onWarning(), parent.onError()).getValue()));
    row.put("conditionLabel", new CellData(serviceConfig.getLocale(
      FlowRunGraphRules.conditionLabel(parent.onWarning(), parent.onError()))));
    return row;
  }

  /**
   * Summary row: status with its rule and the task it points to, launch, dates, root and counts by state
   */
  private Map<String, CellData> getSummaryRow(Run run) throws AWException {
    Map<String, CellData> header = run.header();
    Map<String, CellData> row = new HashMap<>();
    row.put(RUN_ID, new CellData(run.runId()));
    row.put("RunLbl", new CellData(TaskExecutionPresenter.getFlowRunLabel(header, run.runId())));
    for (String field : List.of(ROOT_TASK, ROOT_NAME, ROOT_EXECUTION)) {
      row.put(field, Optional.ofNullable(header.get(field)).orElseGet(CellData::new));
    }

    // Status: the stored one of an ended run, running while it has not ended; and what it points to (or would end with)
    List<Node> nodes = run.order().stream().map(taskId -> new Node(taskId, run.states().get(taskId))).toList();
    TaskStatus status = FlowRunGraphRules.runStatus(integer(header, RUN_STATUS), run.ended());
    TaskStatus pointed = run.ended() ? status : FlowRules.runStatus(nodes.stream()
        .filter(node -> node.state().isDone() && run.status(node.taskId()) != null)
        .map(node -> run.status(node.taskId())).toList(),
      nodes.stream().anyMatch(node -> node.state() == NodeState.SKIPPED));
    row.put(STATUS, new CellData(String.valueOf(status.getValue())));
    row.put("RunEnded", new CellData(run.ended() ? 1 : 0));
    executionPresenter.setStatus(row, String.valueOf(status.getValue()));
    Optional<Cause> cause = FlowRunGraphRules.pointsTo(pointed, nodes);
    row.put("RunRule", new CellData(serviceConfig.getLocale("SCREEN_TEXT_FLOW_RUN_RULE")));
    String causeText = cause.isPresent() ? getCauseText(run, cause.get())
      : serviceConfig.getLocale("SCREEN_TEXT_FLOW_RUN_CAUSE_NONE");
    row.put("RunPntTxt", new CellData(text(run.ended() ? "SCREEN_TEXT_FLOW_RUN_POINTS_ENDED"
      : "SCREEN_TEXT_FLOW_RUN_POINTS_RUNNING", getStatusText(String.valueOf(pointed.getValue())), causeText)));
    row.put("RunPntSta", new CellData(String.valueOf(pointed.getValue())));
    row.put("RunPntTsk", cause.map(Cause::taskId).map(CellData::new).orElseGet(CellData::new));
    row.put("RunPntNam", cause.map(found -> new CellData(run.name(found.taskId()))).orElseGet(CellData::new));
    row.put("RunPntExe", cause.map(found -> run.last().get(found.taskId())).map(execution -> execution.get(EXECUTION_ID))
      .orElseGet(CellData::new));

    // Who launched it: the origin and the launcher of its root execution, else the launcher stored with the run
    Integer rootExecution = integer(header, ROOT_EXECUTION);
    Optional<Map<String, CellData>> root = Optional.ofNullable(run.last().get(run.root()))
      .filter(execution -> rootExecution == null || rootExecution.equals(integer(execution, EXECUTION_ID)));
    LAUNCH_FIELDS.forEach(field -> row.put(field, root.map(execution -> execution.get(field)).orElseGet(CellData::new)));
    if (root.isEmpty()) {
      row.put(LAUNCHED_BY, Optional.ofNullable(header.get("RunLchBy")).orElseGet(CellData::new));
    }

    // Start, end and duration: until now while it has not ended
    Date start = date(header, RUN_START);
    Date end = date(header, RUN_END);
    row.put(START_DATE, format(start));
    row.put(END_DATE, format(end));
    if (start != null) {
      long millis = Math.max(0L, (end == null ? clock.millis() : end.getTime()) - start.getTime());
      row.put(DURATION, new CellData(millis));
      row.put(RUN_DURATION, new CellData(FlowRunGraphRules.formatDuration(millis, end != null)));
      row.put("ExeEla", new CellData((int) (millis / 1000L)));
    }

    // Tasks of the run by state
    Map<NodeState, Integer> counts = new EnumMap<>(NodeState.class);
    nodes.forEach(node -> counts.merge(node.state(), 1, Integer::sum));
    COUNT_FIELDS.forEach((state, field) -> row.put(field, new CellData(counts.getOrDefault(state, 0))));
    long done = nodes.stream().filter(node -> node.state().isDone()).count();
    row.put("RunTskTot", new CellData(nodes.size()));
    row.put("RunTskEnd", new CellData((int) done));
    row.put(RUN_PROGRESS, new CellData(text("SCREEN_TEXT_FLOW_RUN_PROGRESS", String.valueOf(done),
      String.valueOf(nodes.size()))));

    // The header of the page: its facts as pills and the parameters of its launch as one line
    row.put("RunFct", new CellData(getFacts(row)));
    String parameters = flowRunRepository.getLaunchParameters(run.runId()).entrySet().stream()
      .map(parameter -> parameter.getKey() + "=" + Optional.ofNullable(parameter.getValue()).orElse(""))
      .collect(Collectors.joining(FACT_SEPARATOR));
    row.put("RunParTxt", parameters.isEmpty() ? new CellData()
      : new CellData(text("SCREEN_TEXT_FLOW_RUN_PARAMETERS_LINE", parameters)));
    return row;
  }

  /**
   * Facts of the run as the pills of the header (a JSON array of {label, icon, code}, as the task page): who launched
   * it (its origin with its launcher as code, else its launcher), its start, its end once it ended, its duration and
   * how many of its tasks are done. The root task and its execution are the title of the header
   */
  private String getFacts(Map<String, CellData> row) {
    ArrayNode facts = JsonNodeFactory.instance.arrayNode();
    String origin = text(row, "ExeOriTxt");
    String launcher = text(row, LAUNCHED_BY);
    if (!origin.isBlank()) {
      String icon = text(row, "ExeOriIcn");
      facts.add(TaskPagePresenter.pill(origin, icon.isBlank() ? null : icon, launcher));
    } else if (!launcher.isBlank()) {
      facts.add(TaskPagePresenter.pill(launcher, "lucide:user", null));
    }
    String start = text(row, START_DATE);
    if (!start.isBlank()) {
      facts.add(TaskPagePresenter.pill(text("SCREEN_TEXT_FLOW_RUN_FACT_START", start), "lucide:calendar-clock", null));
    }
    String end = text(row, END_DATE);
    if (!end.isBlank()) {
      facts.add(TaskPagePresenter.pill(text("SCREEN_TEXT_FLOW_RUN_FACT_END", end), "lucide:flag", null));
    }
    String duration = text(row, RUN_DURATION);
    if (!duration.isBlank()) {
      facts.add(TaskPagePresenter.pill(text("SCREEN_TEXT_FLOW_RUN_FACT_DURATION", duration), "lucide:timer", null));
    }
    facts.add(TaskPagePresenter.pill(text(row, RUN_PROGRESS), "lucide:list-checks", null));
    return facts.toString();
  }

  /**
   * «X ended in Error» or «X was skipped»
   */
  private String getCauseText(Run run, Cause cause) throws AWException {
    String name = run.name(cause.taskId());
    if (cause.kind() == CauseKind.SKIPPED) {
      return text("SCREEN_TEXT_FLOW_RUN_CAUSE_SKIPPED", name);
    }
    return text("SCREEN_TEXT_FLOW_RUN_CAUSE_ENDED", name,
      getStatusText(run.states().get(cause.taskId()).getStatusOption()));
  }

  /**
   * Text of a locale with its parameters. The parameters are not escaped (as the other scheduler presenters): the page
   * shows the texts as text, so an escaped task name would show its entities («Conciliaci&amp;oacute;n»)
   */
  private String text(String key, String... parameters) {
    return MessageFormat.format(serviceConfig.getLocale(key), (Object[]) parameters);
  }

  /**
   * Text of an option of the activity status groups
   */
  private String getStatusText(String option) throws AWException {
    return serviceConfig.getLocale(queryService.findLabel("ActStaTyp", option));
  }

  /**
   * When an execution that waits for a confirmation was deferred
   */
  private Date getDeferredDate(Integer taskId, Integer executionId) throws AWException {
    DeferredExecution deferred = deferredRepository.find(taskId, executionId);
    return deferred == null ? null : deferred.getAcceptedDate();
  }

  private static Execution asExecution(Map<String, CellData> row) {
    return new Execution(integer(row, EXECUTION_ID), integer(row, STATUS),
      Optional.ofNullable(date(row, START)).map(Date::toInstant).orElse(null),
      Optional.ofNullable(date(row, END)).map(Date::toInstant).orElse(null));
  }

  private static CellData format(Date date) {
    return date == null ? new CellData() : new CellData(DATE_FORMAT.format(LegacyDateConverter.asLocalDateTime(date)));
  }

  private static Date date(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.isEmpty() ? null : cell.getDateValue();
  }

  private static Integer integer(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null || cell.getStringValue().isBlank() ? null
      : cell.getIntegerValue();
  }

  private static String text(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getStringValue() == null ? "" : cell.getStringValue();
  }

  private static boolean isTrue(Map<String, CellData> row, String field) {
    Integer value = row == null ? null : integer(row, field);
    return value != null && value == 1;
  }
}
