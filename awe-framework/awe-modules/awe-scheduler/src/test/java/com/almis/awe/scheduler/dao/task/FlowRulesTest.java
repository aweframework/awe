package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.dao.task.FlowRules.ExecutionRow;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.almis.awe.scheduler.dao.task.FlowRules.Decision.LAUNCH;
import static com.almis.awe.scheduler.dao.task.FlowRules.Decision.SKIP;
import static com.almis.awe.scheduler.dao.task.FlowRules.Decision.WAIT;
import static com.almis.awe.scheduler.dao.task.FlowRules.Outcome.FAILED;
import static com.almis.awe.scheduler.dao.task.FlowRules.Outcome.MET;
import static com.almis.awe.scheduler.dao.task.FlowRules.Outcome.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of the pure flow run rules: parent condition, join decision, scope, pending and run status.
 */
class FlowRulesTest {

  private static final int OK = TaskStatus.JOB_OK.getValue();
  private static final int ERROR = TaskStatus.JOB_ERROR.getValue();
  private static final int WARNING = TaskStatus.JOB_WARNING.getValue();
  private static final int RUNNING = TaskStatus.JOB_RUNNING.getValue();

  @Test
  void parentConditionDependsOnItsStatusAndFlags() {
    assertEquals(MET, FlowRules.parentOutcome(OK, false, false, false));
    assertEquals(FAILED, FlowRules.parentOutcome(WARNING, false, false, false));
    assertEquals(MET, FlowRules.parentOutcome(WARNING, false, true, false));
    assertEquals(FAILED, FlowRules.parentOutcome(ERROR, false, true, false));
    assertEquals(MET, FlowRules.parentOutcome(ERROR, false, false, true));
    assertEquals(FAILED, FlowRules.parentOutcome(TaskStatus.JOB_STOPPED.getValue(), false, true, true));
  }

  @Test
  void parentWithoutEndedExecutionIsPendingAndSkippedParentFails() {
    assertEquals(PENDING, FlowRules.parentOutcome(null, false, true, true));
    assertEquals(PENDING, FlowRules.parentOutcome(RUNNING, false, true, true));
    assertEquals(FAILED, FlowRules.parentOutcome(null, true, true, true));
  }

  @Test
  void joinDecisionSkipsOnAnyFailureLaunchesWhenAllMetAndWaitsOtherwise() {
    assertEquals(LAUNCH, FlowRules.decide(List.of(MET, MET)));
    assertEquals(SKIP, FlowRules.decide(List.of(MET, FAILED)));
    assertEquals(SKIP, FlowRules.decide(List.of(PENDING, FAILED)));
    assertEquals(WAIT, FlowRules.decide(List.of(MET, PENDING)));
  }

  @Test
  void scopeIsEverythingReachableFromTheRootThroughChildren() {
    Map<Integer, List<Integer>> children = Map.of(1, List.of(2, 3), 2, List.of(4), 3, List.of(4), 9, List.of(1));

    assertEquals(Set.of(1, 2, 3, 4), FlowRules.scope(1, children));
    assertEquals(Set.of(3, 4), FlowRules.scope(3, children));
  }

  @Test
  void runIsPendingWhileAJoinWaitsIsLaunchedWithoutExecutionOrAnExecutionRuns() {
    assertTrue(FlowRules.isPending(List.of(new JoinRow(2, "WAITING", null)), List.of()));
    assertTrue(FlowRules.isPending(List.of(new JoinRow(2, "LAUNCHED", null)), List.of()));
    assertTrue(FlowRules.isPending(List.of(), List.of(new ExecutionRow(1, 1, RUNNING))));
    assertFalse(FlowRules.isPending(List.of(new JoinRow(2, "LAUNCHED", 7), new JoinRow(3, "SKIPPED", null)),
      List.of(new ExecutionRow(1, 1, OK), new ExecutionRow(2, 7, ERROR))));
  }

  @Test
  void runStatusIsTheWorstOfErrorWarningAndOk() {
    assertEquals(TaskStatus.JOB_OK, FlowRules.runStatus(List.of(OK, TaskStatus.JOB_INFO.getValue()), false));
    assertEquals(TaskStatus.JOB_WARNING, FlowRules.runStatus(List.of(OK, WARNING), false));
    assertEquals(TaskStatus.JOB_WARNING, FlowRules.runStatus(List.of(OK), true));
    assertEquals(TaskStatus.JOB_ERROR, FlowRules.runStatus(List.of(WARNING, ERROR), true));
    assertEquals(TaskStatus.JOB_ERROR, FlowRules.runStatus(List.of(OK, TaskStatus.JOB_STOPPED.getValue()), false));
    assertEquals(TaskStatus.JOB_ERROR, FlowRules.runStatus(List.of(TaskStatus.JOB_INTERRUPTED.getValue()), false));
  }
}
