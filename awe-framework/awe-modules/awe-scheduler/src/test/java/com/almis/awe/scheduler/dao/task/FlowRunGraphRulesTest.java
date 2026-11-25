package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Cause;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.CauseKind;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Condition;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.EdgeState;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Execution;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Node;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.NodeState;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules.Parent;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests of the pure rules of the flow run page: node state, last execution of a task, «joined by», join wait,
 * edge condition, edge state, skip cause and the task the run status points to.
 */
class FlowRunGraphRulesTest {

  private static final int OK = TaskStatus.JOB_OK.getValue();
  private static final int ERROR = TaskStatus.JOB_ERROR.getValue();
  private static final int WARNING = TaskStatus.JOB_WARNING.getValue();
  private static final int RUNNING = TaskStatus.JOB_RUNNING.getValue();
  private static final int QUEUED = TaskStatus.JOB_QUEUED.getValue();
  private static final Instant T0 = Instant.parse("2026-11-17T08:00:00Z");

  @Test
  void theNodeStateIsTheStatusOfItsLastExecutionElseItsJoinRowAndPendingWithoutRow() {
    assertEquals(NodeState.PENDING, FlowRunGraphRules.nodeState(null, null, false));
    assertEquals(NodeState.PENDING, FlowRunGraphRules.nodeState("WAITING", null, false));
    assertEquals(NodeState.SKIPPED, FlowRunGraphRules.nodeState("SKIPPED", null, false));
    assertEquals(NodeState.QUEUED, FlowRunGraphRules.nodeState("LAUNCHED", null, false));
    assertEquals(NodeState.WARNING, FlowRunGraphRules.nodeState("LAUNCHED", WARNING, false));
    assertEquals(NodeState.OK, FlowRunGraphRules.nodeState(null, OK, false));
    assertEquals(NodeState.RUNNING, FlowRunGraphRules.nodeState("LAUNCHED", RUNNING, false));
    // Waiting for the confirmation of a deferred answer: running with an AweSchExeExt row
    assertEquals(NodeState.DEFERRED, FlowRunGraphRules.nodeState("LAUNCHED", RUNNING, true));
    assertEquals("deferred", NodeState.DEFERRED.getValue());
    assertEquals("2", NodeState.WARNING.getStatusOption());
    assertEquals("pending", NodeState.PENDING.getStatusOption());
  }

  @Test
  void aTaskThatNeverRanInAnEndedRunIsSkippedAndDone() {
    assertEquals(NodeState.SKIPPED, FlowRunGraphRules.nodeState(null, null, false, true));
    assertEquals(NodeState.SKIPPED, FlowRunGraphRules.nodeState("WAITING", null, false, true));
    assertEquals(NodeState.PENDING, FlowRunGraphRules.nodeState(null, null, false, false));
    assertEquals(NodeState.QUEUED, FlowRunGraphRules.nodeState("LAUNCHED", null, false, true));
    assertEquals(NodeState.OK, FlowRunGraphRules.nodeState(null, OK, false, true));
    assertEquals(true, NodeState.SKIPPED.isDone());
  }

  @Test
  void theStatusOfARunIsRunningUntilItEndsThenTheStoredOneAndInfoWhenItHasNone() {
    assertEquals(TaskStatus.JOB_RUNNING, FlowRunGraphRules.runStatus(ERROR, false));
    assertEquals(TaskStatus.JOB_ERROR, FlowRunGraphRules.runStatus(ERROR, true));
    assertEquals(TaskStatus.JOB_INFO, FlowRunGraphRules.runStatus(null, true));
    assertEquals(TaskStatus.JOB_INFO, FlowRunGraphRules.runStatus(99, true));
  }

  @Test
  void aDurationIsFormattedInHoursMinutesSecondsAndMillisecondsBeyondTheIntRange() {
    assertEquals("720h", FlowRunGraphRules.formatDuration(Duration.ofDays(30).toMillis(), true));
    assertEquals("1h 2m 3s 4ms", FlowRunGraphRules.formatDuration(3_723_004L, true));
    assertEquals("1h 2m 3s", FlowRunGraphRules.formatDuration(3_723_004L, false));
    assertEquals("", FlowRunGraphRules.formatDuration(0L, true));
  }

  @Test
  void theLastExecutionOfATaskInTheRunIsTheLatestStartedOneAndTheHighestOnTheSameStart() {
    Execution first = new Execution(4, ERROR, T0, T0.plusSeconds(60));
    Execution relaunched = new Execution(5, WARNING, T0.plusSeconds(120), T0.plusSeconds(180));
    Execution sameStart = new Execution(6, OK, T0.plusSeconds(120), T0.plusSeconds(150));

    assertEquals(Optional.of(relaunched), FlowRunGraphRules.lastExecution(List.of(relaunched, first)));
    assertEquals(Optional.of(sameStart), FlowRunGraphRules.lastExecution(List.of(first, sameStart, relaunched)));
    assertEquals(Optional.empty(), FlowRunGraphRules.lastExecution(List.of()));
  }

  @Test
  void aRelaunchedExecutionThatHasNotStartedYetIsTheLastOneAndTheHighestAmongThem() {
    Execution ended = new Execution(4, ERROR, T0, T0.plusSeconds(60));
    Execution queued = new Execution(5, QUEUED, null, null);
    Execution queuedAgain = new Execution(6, QUEUED, null, null);

    assertEquals(Optional.of(queued), FlowRunGraphRules.lastExecution(List.of(queued, ended)));
    assertEquals(Optional.of(queuedAgain), FlowRunGraphRules.lastExecution(List.of(queuedAgain, ended, queued)));
  }

  @Test
  void theEndOfAParentForAJoinIsItsLatestExecutionEndedAtOrBeforeTheChildStart() {
    Execution first = new Execution(4, OK, T0, T0.plusSeconds(60));
    Execution relaunched = new Execution(5, QUEUED, null, null);
    Execution late = new Execution(6, OK, T0.plusSeconds(200), T0.plusSeconds(300));

    // A relaunched parent still closed the join with the execution that ended before the child started
    assertEquals(Optional.of(T0.plusSeconds(60)), FlowRunGraphRules.endBefore(T0.plusSeconds(120),
      List.of(first, relaunched, late)));
    assertEquals(Optional.of(T0.plusSeconds(300)), FlowRunGraphRules.endBefore(T0.plusSeconds(300),
      List.of(first, late)));
    assertEquals(Optional.empty(), FlowRunGraphRules.endBefore(T0.plusSeconds(30), List.of(first, relaunched)));
    assertEquals(Optional.empty(), FlowRunGraphRules.endBefore(null, List.of(first)));
  }

  @Test
  void theJoinIsJoinedByTheParentWhoseExecutionEndedLastAtOrBeforeTheChildStart() {
    Map<Integer, Instant> ends = new LinkedHashMap<>();
    ends.put(12, T0.plusSeconds(60));
    ends.put(13, T0.plusSeconds(240));
    ends.put(14, T0.plusSeconds(900));
    Instant start = T0.plusSeconds(240);

    // 14 ended after the child started: it did not close the join; 13 ended at its start
    assertEquals(Optional.of(13), FlowRunGraphRules.joinedBy(start, ends));
    Map<Integer, Instant> notEnded = new LinkedHashMap<>();
    notEnded.put(12, T0.plusSeconds(60));
    notEnded.put(13, null);
    assertEquals(Optional.of(12), FlowRunGraphRules.joinedBy(start, notEnded));
    // A task with one parent is not a join, and a child that has not started has no «joined by»
    assertEquals(Optional.empty(), FlowRunGraphRules.joinedBy(start, Map.of(12, T0)));
    assertEquals(Optional.empty(), FlowRunGraphRules.joinedBy(null, ends));
  }

  @Test
  void theJoinWaitRunsFromTheEndOfTheFirstParentToTheEndOfTheParentThatClosedIt() {
    Map<Integer, Instant> ends = new LinkedHashMap<>();
    ends.put(12, T0.plusSeconds(60));
    ends.put(13, T0.plusSeconds(155));
    ends.put(14, null);

    assertEquals(Optional.of(Duration.ofSeconds(95)), FlowRunGraphRules.joinWait(13, ends));
    assertEquals(Optional.of(Duration.ZERO), FlowRunGraphRules.joinWait(12, ends));
    assertEquals(Optional.empty(), FlowRunGraphRules.joinWait(null, ends));
  }

  @Test
  void theEdgeConditionAndItsLabelComeFromTheOptionsOfTheParent() {
    assertEquals(Condition.OK, FlowRunGraphRules.condition(false, false));
    assertEquals(Condition.WARNING, FlowRunGraphRules.condition(true, false));
    assertEquals(Condition.ERROR, FlowRunGraphRules.condition(false, true));
    assertEquals(Condition.ERROR, FlowRunGraphRules.condition(true, true));
    assertEquals("SCREEN_TEXT_FLOW_RUN_CONDITION_OK", FlowRunGraphRules.conditionLabel(false, false));
    assertEquals("SCREEN_TEXT_FLOW_RUN_CONDITION_OK_WARNING", FlowRunGraphRules.conditionLabel(true, false));
    assertEquals("SCREEN_TEXT_FLOW_RUN_CONDITION_OK_ERROR", FlowRunGraphRules.conditionLabel(false, true));
    assertEquals("SCREEN_TEXT_FLOW_RUN_CONDITION_ANY", FlowRunGraphRules.conditionLabel(true, true));
    assertEquals("warning", Condition.WARNING.getValue());
  }

  @Test
  void theEdgeIsMetNotMetCutClosesTheJoinOrPendingInTheRun() {
    Parent ok = new Parent(12, OK, false, false, false);
    Parent warningLaunches = new Parent(12, WARNING, false, true, false);
    Parent warningStops = new Parent(12, WARNING, false, false, false);
    Parent running = new Parent(12, RUNNING, false, false, false);
    Parent neverRan = new Parent(12, null, false, false, false);
    Parent skipped = new Parent(12, null, true, true, true);

    assertEquals(EdgeState.MET, FlowRunGraphRules.edgeState(ok, false, false, false));
    assertEquals(EdgeState.MET, FlowRunGraphRules.edgeState(warningLaunches, false, false, true));
    assertEquals(EdgeState.CLOSED_JOIN, FlowRunGraphRules.edgeState(ok, false, true, true));
    assertEquals(EdgeState.NOT_MET, FlowRunGraphRules.edgeState(warningStops, true, false, true));
    // Cut: the parent never ran (skipped, or the run ended without it) or the child was skipped by another parent
    assertEquals(EdgeState.CUT, FlowRunGraphRules.edgeState(skipped, true, false, true));
    assertEquals(EdgeState.CUT, FlowRunGraphRules.edgeState(neverRan, false, false, true));
    assertEquals(EdgeState.CUT, FlowRunGraphRules.edgeState(ok, true, false, true));
    assertEquals(EdgeState.PENDING, FlowRunGraphRules.edgeState(neverRan, false, false, false));
    assertEquals(EdgeState.PENDING, FlowRunGraphRules.edgeState(running, false, false, false));
    assertEquals("NOT_MET", EdgeState.NOT_MET.getValue());
    assertEquals("CLOSED_JOIN", EdgeState.CLOSED_JOIN.getValue());
  }

  @Test
  void aSkippedTaskWasSkippedByTheParentThatDidNotMeetItsConditionElseByASkippedParent() {
    Parent met = new Parent(12, OK, false, false, false);
    Parent failed = new Parent(13, ERROR, false, true, false);
    Parent skipped = new Parent(14, null, true, false, false);

    assertEquals(Optional.of(new Cause(13, CauseKind.ENDED)),
      FlowRunGraphRules.skipCause(List.of(met, skipped, failed)));
    assertEquals(Optional.of(new Cause(14, CauseKind.SKIPPED)), FlowRunGraphRules.skipCause(List.of(met, skipped)));
    assertEquals(Optional.empty(), FlowRunGraphRules.skipCause(List.of(met)));
  }

  @Test
  void theRunStatusPointsToTheFirstTaskThatGivesIt() {
    List<Node> nodes = List.of(new Node(11, NodeState.OK), new Node(12, NodeState.WARNING),
      new Node(13, NodeState.STOPPED), new Node(14, NodeState.ERROR), new Node(15, NodeState.SKIPPED));

    assertEquals(Optional.of(new Cause(13, CauseKind.ENDED)), FlowRunGraphRules.pointsTo(TaskStatus.JOB_ERROR, nodes));
    assertEquals(Optional.of(new Cause(12, CauseKind.ENDED)), FlowRunGraphRules.pointsTo(TaskStatus.JOB_WARNING, nodes));
    assertEquals(Optional.of(new Cause(15, CauseKind.SKIPPED)), FlowRunGraphRules.pointsTo(TaskStatus.JOB_WARNING,
      List.of(new Node(11, NodeState.OK), new Node(15, NodeState.SKIPPED))));
    assertEquals(Optional.empty(), FlowRunGraphRules.pointsTo(TaskStatus.JOB_OK, nodes));
  }

  @Test
  void boundaryCasesOfJoinedByJoinWaitEdgeStateAndPointsTo() {
    Map<Integer, Instant> tie = new LinkedHashMap<>();
    tie.put(2, T0.plusSeconds(10));
    tie.put(1, T0.plusSeconds(10));
    assertEquals(Optional.of(2), FlowRunGraphRules.joinedBy(T0.plusSeconds(20), tie));
    Map<Integer, Instant> late = new LinkedHashMap<>();
    late.put(1, T0.plusSeconds(30));
    late.put(2, T0.plusSeconds(40));
    assertEquals(Optional.empty(), FlowRunGraphRules.joinedBy(T0.plusSeconds(20), late));
    Map<Integer, Instant> notEnded = new LinkedHashMap<>();
    notEnded.put(1, T0);
    notEnded.put(2, null);
    assertEquals(Optional.empty(), FlowRunGraphRules.joinWait(2, notEnded));
    assertEquals(EdgeState.MET, FlowRunGraphRules.edgeState(new Parent(1, ERROR, false, false, true), false, false, true));
    assertEquals(EdgeState.NOT_MET, FlowRunGraphRules.edgeState(new Parent(1, ERROR, false, true, false), false, false, true));
    assertEquals(EdgeState.PENDING, FlowRunGraphRules.edgeState(
      new Parent(1, TaskStatus.JOB_QUEUED.getValue(), false, false, false), false, false, false));
    assertEquals(NodeState.QUEUED, FlowRunGraphRules.nodeState("LAUNCHED", TaskStatus.JOB_QUEUED.getValue(), true));
    assertEquals(Optional.of(new Cause(2, CauseKind.ENDED)), FlowRunGraphRules.pointsTo(TaskStatus.JOB_ERROR,
      List.of(new Node(1, NodeState.OK), new Node(2, NodeState.INTERRUPTED), new Node(3, NodeState.ERROR))));
    assertEquals(Optional.empty(), FlowRunGraphRules.pointsTo(TaskStatus.JOB_RUNNING, List.of(new Node(1, NodeState.ERROR))));
  }
}
