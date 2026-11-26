package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.enums.TaskStatus;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure rules of a flow run (no Spring, no database): when a parent counts as met, what a join decides, which tasks
 * belong to a run, when a run is still pending and which status it ends with.
 */
public final class FlowRules {

  public static final String WAITING = "WAITING";
  public static final String LAUNCHED = "LAUNCHED";
  public static final String SKIPPED = "SKIPPED";

  /**
   * Situation of a parent in a join
   */
  public enum Outcome { PENDING, MET, FAILED }

  /**
   * What a join does with its child
   */
  public enum Decision { WAIT, LAUNCH, SKIP }

  /**
   * Row of the join state table
   *
   * @param taskId      Task identifier
   * @param status      WAITING, LAUNCHED or SKIPPED
   * @param executionId Execution of the task in the run, null until it starts
   */
  public record JoinRow(Integer taskId, String status, Integer executionId) {
  }

  /**
   * Execution of a task inside a run
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @param status      Execution status
   */
  public record ExecutionRow(Integer taskId, Integer executionId, Integer status) {
  }

  private FlowRules() {
  }

  /**
   * Outcome of a parent for the join of its child
   *
   * @param status      Status of the parent execution in the run, null when it has none
   * @param skipped     The parent was skipped in the run
   * @param onWarning   The parent launches its dependencies on warning
   * @param onError     The parent launches its dependencies on error
   * @return Parent outcome
   */
  public static Outcome parentOutcome(Integer status, boolean skipped, boolean onWarning, boolean onError) {
    if (skipped) {
      return Outcome.FAILED;
    }
    if (status == null || status == TaskStatus.JOB_RUNNING.getValue() || status == TaskStatus.JOB_QUEUED.getValue()) {
      return Outcome.PENDING;
    }
    boolean met = switch (TaskStatus.valueOf(status)) {
      case JOB_OK -> true;
      case JOB_WARNING -> onWarning;
      case JOB_ERROR -> onError;
      default -> false;
    };
    return met ? Outcome.MET : Outcome.FAILED;
  }

  /**
   * Decide what to do with a child given the outcomes of its parents inside the run
   *
   * @param outcomes Parent outcomes
   * @return Decision
   */
  public static Decision decide(Collection<Outcome> outcomes) {
    if (outcomes.contains(Outcome.FAILED)) {
      return Decision.SKIP;
    }
    return outcomes.contains(Outcome.PENDING) ? Decision.WAIT : Decision.LAUNCH;
  }

  /**
   * Tasks reachable from the root through the children graph, root included
   *
   * @param root     Run root task
   * @param children Children by task
   * @return Tasks of the run
   */
  public static Set<Integer> scope(Integer root, Map<Integer, List<Integer>> children) {
    Set<Integer> reached = new HashSet<>();
    Deque<Integer> pending = new ArrayDeque<>(List.of(root));
    while (!pending.isEmpty()) {
      Integer taskId = pending.pop();
      if (reached.add(taskId)) {
        pending.addAll(children.getOrDefault(taskId, List.of()));
      }
    }
    return reached;
  }

  /**
   * The run is pending while a join waits, a launched task has not started or an execution runs
   *
   * @param joins      Join rows of the run
   * @param executions Executions of the run
   * @return True when the run has not ended
   */
  public static boolean isPending(Collection<JoinRow> joins, Collection<ExecutionRow> executions) {
    return joins.stream().anyMatch(join -> WAITING.equals(join.status())
      || LAUNCHED.equals(join.status()) && join.executionId() == null)
      || executions.stream().anyMatch(execution -> execution.status() == TaskStatus.JOB_RUNNING.getValue());
  }

  /**
   * Final run status: error (error, stopped, interrupted) over warning (warning, skipped) over ok
   *
   * @param statuses   Execution statuses of the run
   * @param anySkipped A task of the run was skipped
   * @return Run status
   */
  public static TaskStatus runStatus(Collection<Integer> statuses, boolean anySkipped) {
    boolean error = statuses.stream().anyMatch(status -> status == TaskStatus.JOB_ERROR.getValue()
      || status == TaskStatus.JOB_STOPPED.getValue() || status == TaskStatus.JOB_INTERRUPTED.getValue());
    if (error) {
      return TaskStatus.JOB_ERROR;
    }
    boolean warning = anySkipped || statuses.stream().anyMatch(status -> status == TaskStatus.JOB_WARNING.getValue());
    return warning ? TaskStatus.JOB_WARNING : TaskStatus.JOB_OK;
  }
}
