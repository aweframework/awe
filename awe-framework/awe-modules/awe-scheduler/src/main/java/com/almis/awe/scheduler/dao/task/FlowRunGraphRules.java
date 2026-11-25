package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.dao.task.FlowRules.Outcome;
import com.almis.awe.scheduler.enums.TaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure rules of the flow run page (#993, no Spring, no database): the state of each task of a run, its last execution,
 * the parent that closed a join («joined by») and how long the join waited, the condition and the state of each edge,
 * why a task was skipped and which task the status of the run points to. Nothing of it is stored: it is derived from
 * the executions of the run, its join rows and the options of the parents (LchDepWrn, LchDepErr; LchSetWrn marks the
 * parent and does not change the condition of its edges)
 */
public final class FlowRunGraphRules {

  /**
   * State of a task in a run: its value (the state of the flow-graph widget) and its option of the activity status
   * groups (ActStaTyp, ActStaIco, ActStaTon)
   */
  public enum NodeState {
    OK("ok", "0"), ERROR("error", "1"), WARNING("warning", "2"), STOPPED("stopped", "3"), RUNNING("running", "4"),
    QUEUED("queued", "5"), INTERRUPTED("interrupted", "6"), INFO("info", "7"), DEFERRED("deferred", "deferred"),
    SKIPPED("skipped", "skipped"), PENDING("pending", "pending");

    private static final Set<NodeState> OPEN = Set.of(RUNNING, QUEUED, DEFERRED, PENDING);
    private final String value;
    private final String statusOption;

    NodeState(String value, String statusOption) {
      this.value = value;
      this.statusOption = statusOption;
    }

    public String getValue() {
      return value;
    }

    public String getStatusOption() {
      return statusOption;
    }

    /**
     * The task will not run any more in the run: it ended or it was skipped
     *
     * @return True when the task is done in the run
     */
    public boolean isDone() {
      return !OPEN.contains(this);
    }

    static NodeState of(int status) {
      return switch (TaskStatus.valueOf(status)) {
        case JOB_OK -> OK;
        case JOB_ERROR -> ERROR;
        case JOB_WARNING -> WARNING;
        case JOB_STOPPED -> STOPPED;
        case JOB_QUEUED -> QUEUED;
        case JOB_INTERRUPTED -> INTERRUPTED;
        case JOB_INFO -> INFO;
        default -> RUNNING;
      };
    }
  }

  /**
   * Condition a parent puts on the edges to its children: the worst status of the parent that still launches them
   */
  public enum Condition {
    OK, WARNING, ERROR;

    public String getValue() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * State of an edge in a run
   */
  public enum EdgeState {
    /**
     * The parent met the condition
     */
    MET,
    /**
     * The parent ended without meeting the condition
     */
    NOT_MET,
    /**
     * The parent never ran (skipped, or the run ended without it) or the child was skipped by another parent
     */
    CUT,
    /**
     * The parent has not ended yet
     */
    PENDING,
    /**
     * The parent met the condition and was the last one the join waited for
     */
    CLOSED_JOIN;

    public String getValue() {
      return name();
    }
  }

  /**
   * Why a task is what it is: a task that ended (with its status) or a task that was skipped
   */
  public enum CauseKind { ENDED, SKIPPED }

  /**
   * Task that explains a skipped task or the status of the run
   *
   * @param taskId Task identifier
   * @param kind   It ended (with its status) or it was skipped
   */
  public record Cause(Integer taskId, CauseKind kind) {
  }

  /**
   * Execution of a task in a run
   *
   * @param executionId Execution identifier
   * @param status      Execution status
   * @param start       Start
   * @param end         End, null while it has not ended
   */
  public record Execution(Integer executionId, Integer status, Instant start, Instant end) {
  }

  /**
   * Parent of an edge in a run
   *
   * @param taskId    Task identifier
   * @param status    Status of its last execution in the run, null without execution
   * @param skipped   The run skipped it
   * @param onWarning It launches its dependencies on warning (LchDepWrn)
   * @param onError   It launches its dependencies on error (LchDepErr)
   */
  public record Parent(Integer taskId, Integer status, boolean skipped, boolean onWarning, boolean onError) {
  }

  /**
   * Task of a run with its state
   *
   * @param taskId Task identifier
   * @param state  State in the run
   */
  public record Node(Integer taskId, NodeState state) {
  }

  private static final Pattern RUN_ID = Pattern.compile(
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  private FlowRunGraphRules() {
  }

  /**
   * State of a task in a run: the status of its last execution (waiting for a confirmation when it runs with a deferred
   * answer), else its join row: skipped, launched but not started (queued), or pending (waiting or without row)
   *
   * @param joinStatus      Join state of the task (WAITING, LAUNCHED, SKIPPED), null without row
   * @param executionStatus Status of its last execution in the run, null without execution
   * @param waiting         Its execution waits for the confirmation of a deferred answer (AweSchExeExt row)
   * @return Node state
   */
  public static NodeState nodeState(String joinStatus, Integer executionStatus, boolean waiting) {
    if (executionStatus != null) {
      NodeState state = NodeState.of(executionStatus);
      return state == NodeState.RUNNING && waiting ? NodeState.DEFERRED : state;
    }
    if (FlowRules.SKIPPED.equals(joinStatus)) {
      return NodeState.SKIPPED;
    }
    return FlowRules.LAUNCHED.equals(joinStatus) ? NodeState.QUEUED : NodeState.PENDING;
  }

  /**
   * State of a task in a run that may have ended: a task that is still pending when the run has ended (no execution,
   * waiting or without join row) never ran and never will, so it was skipped (and is done), as the edges to it are cut
   *
   * @param joinStatus      Join state of the task (WAITING, LAUNCHED, SKIPPED), null without row
   * @param executionStatus Status of its last execution in the run, null without execution
   * @param waiting         Its execution waits for the confirmation of a deferred answer (AweSchExeExt row)
   * @param runEnded        The run has ended
   * @return Node state
   */
  public static NodeState nodeState(String joinStatus, Integer executionStatus, boolean waiting, boolean runEnded) {
    NodeState state = nodeState(joinStatus, executionStatus, waiting);
    return runEnded && state == NodeState.PENDING ? NodeState.SKIPPED : state;
  }

  /**
   * Status of a run: running while it has not ended, then the one stored with it; Info (neutral) when an ended run
   * has no status or one that is not a task status
   *
   * @param stored Status stored with the run (AweSchRun.Sta), null without status
   * @param ended  The run has ended
   * @return Run status
   */
  public static TaskStatus runStatus(Integer stored, boolean ended) {
    if (!ended) {
      return TaskStatus.JOB_RUNNING;
    }
    return Arrays.stream(TaskStatus.values()).filter(status -> stored != null && status.getValue() == stored)
      .findFirst().orElse(TaskStatus.JOB_INFO);
  }

  /**
   * A duration as the text of TimeUtil.formatTime («1h 2m 3s 4ms», empty units left out) from a long, so a duration
   * longer than the int range (about 24.8 days) is not cut
   *
   * @param millis Duration in milliseconds
   * @param showMs Show the milliseconds
   * @return Text of the duration
   */
  public static String formatDuration(long millis, boolean showMs) {
    Duration duration = Duration.ofMillis(Math.max(0L, millis));
    List<String> units = new ArrayList<>();
    if (duration.toHours() > 0) {
      units.add(duration.toHours() + "h");
    }
    if (duration.toMinutesPart() > 0) {
      units.add(duration.toMinutesPart() + "m");
    }
    if (duration.toSecondsPart() > 0) {
      units.add(duration.toSecondsPart() + "s");
    }
    if (showMs && duration.toMillisPart() > 0) {
      units.add(duration.toMillisPart() + "ms");
    }
    return String.join(" ", units);
  }

  /**
   * End of a parent for the join of a child: the latest end of its executions in the run at or before the start of the
   * child, so a parent relaunched after the child started still closed the join with its earlier execution
   *
   * @param childStart Start of the child, null when it has not started
   * @param executions Executions of the parent in the run
   * @return End, empty when the child has not started or no execution of the parent ended before it started
   */
  public static Optional<Instant> endBefore(Instant childStart, Collection<Execution> executions) {
    if (childStart == null) {
      return Optional.empty();
    }
    return executions.stream().map(Execution::end).filter(Objects::nonNull).filter(end -> !end.isAfter(childStart))
      .max(Comparator.naturalOrder());
  }

  /**
   * Whether an id has the shape of the ids of the flow runs (AweSchRun.IdeRun, a UUID): anything else is not a run and
   * must not reach a query or the URL of the remote scheduler
   *
   * @param runId Flow run identifier
   * @return True with the shape of a run id
   */
  public static boolean isRunId(String runId) {
    return runId != null && RUN_ID.matcher(runId).matches();
  }

  /**
   * Last execution of a task in a run (a relaunched task has several): one that has not started yet (queued after a
   * relaunch) before any started one, else the latest started; the highest on the same start
   *
   * @param executions Executions of the task in the run
   * @return Last execution, empty without executions
   */
  public static Optional<Execution> lastExecution(Collection<Execution> executions) {
    return executions.stream().max(Comparator.comparing(Execution::start, Comparator.nullsLast(Comparator.naturalOrder()))
      .thenComparing(Execution::executionId, Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  /**
   * Parent that closed a join: the one whose execution ended last at or before the start of the child. Only a join (a
   * task with two parents or more in the run) has one; the first one on the same end
   *
   * @param childStart Start of the child, null when it has not started
   * @param parentEnds End of the execution of each parent of the child in the run (null when it has not ended), in
   *                   the order of the parents
   * @return Parent that closed the join, empty when it is not a join or no parent ended before the child started
   */
  public static Optional<Integer> joinedBy(Instant childStart, Map<Integer, Instant> parentEnds) {
    if (childStart == null || parentEnds.size() < 2) {
      return Optional.empty();
    }
    Integer closing = null;
    Instant latest = null;
    for (Map.Entry<Integer, Instant> parent : parentEnds.entrySet()) {
      Instant end = parent.getValue();
      if (end != null && !end.isAfter(childStart) && (latest == null || end.isAfter(latest))) {
        closing = parent.getKey();
        latest = end;
      }
    }
    return Optional.ofNullable(closing);
  }

  /**
   * Time a join waited: from the end of the first parent that ended to the end of the parent that closed it
   *
   * @param joinedBy   Parent that closed the join, null when there is none
   * @param parentEnds End of the execution of each parent of the child in the run (null when it has not ended)
   * @return Wait, empty without a parent that closed the join
   */
  public static Optional<Duration> joinWait(Integer joinedBy, Map<Integer, Instant> parentEnds) {
    Instant closed = joinedBy == null ? null : parentEnds.get(joinedBy);
    if (closed == null) {
      return Optional.empty();
    }
    return parentEnds.values().stream().filter(Objects::nonNull).min(Comparator.naturalOrder())
      .map(first -> Duration.between(first, closed));
  }

  /**
   * Condition of the edges of a parent, from its options (as getCondition of flowGraph.js)
   *
   * @param onWarning It launches its dependencies on warning (LchDepWrn)
   * @param onError   It launches its dependencies on error (LchDepErr)
   * @return Condition
   */
  public static Condition condition(boolean onWarning, boolean onError) {
    if (onError) {
      return Condition.ERROR;
    }
    return onWarning ? Condition.WARNING : Condition.OK;
  }

  /**
   * Locale of the label of the edges of a parent: the endings of the parent that launch the child, OK and also
   * Warning and Error when it launches them on them (the four cases of getConditionLabel of flowGraph.js)
   *
   * @param onWarning It launches its dependencies on warning (LchDepWrn)
   * @param onError   It launches its dependencies on error (LchDepErr)
   * @return Locale key
   */
  public static String conditionLabel(boolean onWarning, boolean onError) {
    if (onWarning && onError) {
      return "SCREEN_TEXT_FLOW_RUN_CONDITION_ANY";
    }
    if (onWarning) {
      return "SCREEN_TEXT_FLOW_RUN_CONDITION_OK_WARNING";
    }
    return onError ? "SCREEN_TEXT_FLOW_RUN_CONDITION_OK_ERROR" : "SCREEN_TEXT_FLOW_RUN_CONDITION_OK";
  }

  /**
   * State of an edge in a run: not met when the parent ended without meeting its condition; cut when the parent never
   * ran (skipped, or the run ended without it) or when the child was skipped by another parent; pending while the
   * parent has not ended; else met, or closed the join when the parent closed the join of the child
   *
   * @param parent       Parent of the edge
   * @param childSkipped The run skipped the child
   * @param closesJoin   The parent closed the join of the child
   * @param runEnded     The run has ended
   * @return Edge state
   */
  public static EdgeState edgeState(Parent parent, boolean childSkipped, boolean closesJoin, boolean runEnded) {
    if (parent.skipped()) {
      return EdgeState.CUT;
    }
    Outcome outcome = FlowRules.parentOutcome(parent.status(), false, parent.onWarning(), parent.onError());
    if (outcome == Outcome.FAILED) {
      return EdgeState.NOT_MET;
    }
    if (outcome == Outcome.PENDING) {
      return runEnded ? EdgeState.CUT : EdgeState.PENDING;
    }
    if (childSkipped) {
      return EdgeState.CUT;
    }
    return closesJoin ? EdgeState.CLOSED_JOIN : EdgeState.MET;
  }

  /**
   * Why the run skipped a task: the first parent that ended without meeting its condition, else the first parent that
   * was skipped
   *
   * @param parents Parents of the task in the run, in their order
   * @return Cause, empty when no parent explains it
   */
  public static Optional<Cause> skipCause(List<Parent> parents) {
    return parents.stream()
      .filter(parent -> !parent.skipped() && FlowRules.parentOutcome(parent.status(), false, parent.onWarning(),
        parent.onError()) == Outcome.FAILED)
      .findFirst()
      .map(parent -> new Cause(parent.taskId(), CauseKind.ENDED))
      .or(() -> parents.stream().filter(Parent::skipped).findFirst()
        .map(parent -> new Cause(parent.taskId(), CauseKind.SKIPPED)));
  }

  /**
   * Task the status of a run points to (FlowRules.runStatus): with error, the first task that ended in error, stopped
   * or interrupted; with warning, the first task that ended in warning, else the first skipped one; none with ok
   *
   * @param status Status of the run
   * @param nodes  Tasks of the run with their state, in the order they ran
   * @return Cause, empty when the status points to no task
   */
  public static Optional<Cause> pointsTo(TaskStatus status, List<Node> nodes) {
    if (status == TaskStatus.JOB_ERROR) {
      return first(nodes, Set.of(NodeState.ERROR, NodeState.STOPPED, NodeState.INTERRUPTED), CauseKind.ENDED);
    }
    if (status == TaskStatus.JOB_WARNING) {
      return first(nodes, Set.of(NodeState.WARNING), CauseKind.ENDED)
        .or(() -> first(nodes, Set.of(NodeState.SKIPPED), CauseKind.SKIPPED));
    }
    return Optional.empty();
  }

  private static Optional<Cause> first(List<Node> nodes, Set<NodeState> states, CauseKind kind) {
    return nodes.stream().filter(node -> states.contains(node.state())).findFirst()
      .map(node -> new Cause(node.taskId(), kind));
  }
}
