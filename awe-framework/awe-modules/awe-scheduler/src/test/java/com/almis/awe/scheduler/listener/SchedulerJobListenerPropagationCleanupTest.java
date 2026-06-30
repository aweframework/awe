package com.almis.awe.scheduler.listener;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

/**
 * Regression tests for the non-web / scheduler pending-propagation cleanup fix.
 *
 * <h3>Problem being guarded</h3>
 * <p>{@link com.almis.awe.config.ServiceConfig#putPropagatedRequestParameter} and
 * {@link com.almis.awe.config.ServiceConfig#mergePropagatedRequestParameters} write to the
 * {@code pendingPropagation} ThreadLocal in {@link PrototypeRequestBeanHolder}.  For HTTP
 * requests, {@link com.almis.awe.component.AwePropagationCleanupFilter} clears this overlay in
 * a {@code finally} block.  Quartz/scheduler threads never pass through that filter, so without
 * an explicit cleanup the overlay would leak onto the next job that reuses the same thread.</p>
 *
 * <h3>Fix being validated</h3>
 * <p>{@link SchedulerJobListener#jobWasExecuted} — the Quartz post-execution hook (always
 * called after {@code execute()}, regardless of exception) — now calls
 * {@link PrototypeRequestBeanHolder#clearPendingPropagation()}, providing the same
 * lifecycle guarantee as the servlet filter for non-web threads.</p>
 */
@ExtendWith(MockitoExtension.class)
class SchedulerJobListenerPropagationCleanupTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private JobExecutionContext context;

  private PrototypeRequestBeanHolder holder;
  private SchedulerJobListener listener;

  @BeforeEach
  void setUp() {
    holder = new PrototypeRequestBeanHolder();
    listener = new SchedulerJobListener(eventPublisher, holder);
    // Ensure a clean state at the start of every test
    holder.clearPendingPropagation();
    holder.clear();
  }

  @AfterEach
  void tearDown() {
    holder.clearPendingPropagation();
    holder.clear();
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  /** Simulates the job instance NOT being a SchedulerJob (e.g. TimeoutJob, ProgressJob). */
  private void stubNonSchedulerJob() {
    // Use a mock Job that is not a SchedulerJob
    when(context.getJobInstance()).thenReturn(mock(Job.class));
  }

  /** Returns a populated ObjectNode with a single key for use as a propagated overlay. */
  private ObjectNode overlayWith(String key, String value) {
    return JsonNodeFactory.instance.objectNode().put(key, value);
  }

  // ── Core regression tests ────────────────────────────────────────────────────

  /**
   * Core regression: a propagated overlay written during a job must be cleared by
   * {@code jobWasExecuted}, preventing it from leaking onto subsequent jobs.
   *
   * <p>Mirrors the lifecycle of a scheduler job that calls
   * {@code putPropagatedRequestParameter} (or an internal service does so) and then
   * finishes.  Without the fix, the overlay would survive on the Quartz thread.</p>
   */
  @Test
  void jobWasExecuted_whenOverlayWasWrittenDuringJob_overlayIsClearedAfterExecution() {
    // GIVEN: a propagated overlay is written during job execution (simulated here)
    holder.mergeRequestData(overlayWith("job-param", "should-not-survive"));

    assertNotNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be present before jobWasExecuted runs (pre-condition)");

    // Non-SchedulerJob instance: event publishing path is bypassed, but cleanup must still run
    stubNonSchedulerJob();

    // WHEN: Quartz calls jobWasExecuted after execution completes
    listener.jobWasExecuted(context, null);

    // THEN: overlay is cleared — cannot leak to the next job on this thread
    assertNull(holder.getPendingPropagationSnapshot(),
        "Pending propagation overlay must be cleared by jobWasExecuted — no cross-job leakage");
  }

  /**
   * Sequential jobs on a reused thread must not inherit overlays from a previous job.
   *
   * <p>This is the primary contamination scenario: Quartz reuses threads from its pool, so
   * job 2 runs on the same OS thread as job 1.  If job 1 wrote a propagated parameter and the
   * listener did NOT clear it, job 2 would see a stale overlay it never wrote.</p>
   *
   * <p>This test directly validates the isolation guarantee across job boundaries.</p>
   */
  @Test
  void sequentialJobsOnSameThread_job2DoesNotInheritOverlayFromJob1() {
    // ── JOB 1 lifecycle ──────────────────────────────────────────────────────
    stubNonSchedulerJob();

    // Job 1 writes a propagated parameter
    holder.mergeRequestData(overlayWith("job1-secret", "value-from-job1"));

    assertNotNull(holder.getPendingPropagationSnapshot(),
        "Job 1 overlay must be present during its execution (pre-condition)");

    // Quartz fires jobWasExecuted for job 1
    listener.jobWasExecuted(context, null);

    // ── Verify cleanup after job 1 ───────────────────────────────────────────
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be cleared after job 1 completes — job 2 must start with a clean slate");

    // ── JOB 2 lifecycle (same thread, new context — no overlay written) ──────
    // Job 2 does NOT write any propagated parameter
    // Simulate job 2's jobWasExecuted — should not throw and overlay should stay null
    listener.jobWasExecuted(context, null);

    // THEN: job 2 still sees no overlay — no contamination from job 1
    assertNull(holder.getPendingPropagationSnapshot(),
        "Job 2 must not inherit job 1's overlay — cross-job contamination regression");
  }

  /**
   * Cleanup when no overlay was written is a safe no-op.
   *
   * <p>Guards against regressions where calling {@code clearPendingPropagation()} on an
   * absent ThreadLocal value might throw or corrupt state.</p>
   */
  @Test
  void jobWasExecuted_whenNoOverlayPresent_safeNoOp() {
    // Ensure there is no overlay
    assertNull(holder.getPendingPropagationSnapshot(), "Pre-condition: no overlay");

    stubNonSchedulerJob();

    // WHEN / THEN: must not throw
    listener.jobWasExecuted(context, null);

    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must still be absent after no-op cleanup");
  }

  /**
   * Cleanup runs even when the job threw a {@link JobExecutionException}.
   *
   * <p>Quartz always calls {@code jobWasExecuted} after execution, passing the exception
   * (non-null) when the job failed.  The listener must clear the overlay regardless.</p>
   */
  @Test
  void jobWasExecuted_whenJobThrewException_overlayStillCleared() {
    // GIVEN: overlay written before a failing job
    holder.mergeRequestData(overlayWith("failing-job-param", "should-not-survive-failure"));

    stubNonSchedulerJob();

    JobExecutionException exception = new JobExecutionException("simulated job failure");

    // WHEN: Quartz reports the job failed (non-null exception)
    listener.jobWasExecuted(context, exception);

    // THEN: overlay is still cleared despite the job failure
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be cleared even when the job threw a JobExecutionException");
  }

  /**
   * Overlay written before {@code jobWasExecuted} is accessible via snapshot.
   *
   * <p>Validates the invariant that the overlay is <em>present</em> just before
   * {@code jobWasExecuted} is called and absent just after — confirming the boundary.</p>
   */
  @Test
  void overlayIsAvailableBeforeCleanupAndAbsentAfter() {
    // Write overlay to simulate a job that propagated a parameter
    holder.mergeRequestData(overlayWith("boundary-key", "boundary-value"));

    // Before cleanup: overlay must be readable
    ObjectNode snapshot = holder.getPendingPropagationSnapshot();
    assertNotNull(snapshot, "Overlay must be readable before jobWasExecuted (during-job window)");
    assert snapshot.has("boundary-key") : "Overlay must contain 'boundary-key'";

    stubNonSchedulerJob();

    // Trigger cleanup
    listener.jobWasExecuted(context, null);

    // After cleanup: overlay must be gone
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be absent after jobWasExecuted — thread is returned to Quartz pool clean");
  }

  /**
   * Regression: cleanup MUST run even when {@code eventPublisher.publishEvent()} throws.
   *
   * <p>Before the {@code finally} fix, this was the exact failure path: if
   * {@code publishEvent} threw a {@link RuntimeException}, the
   * {@code clearPendingPropagation()} call on the line after it was never reached,
   * leaking the overlay on the Quartz launcher thread.</p>
   */
  @Test
  void jobWasExecuted_whenPublishEventThrows_overlayStillClearedInFinally() {
    // GIVEN: a real SchedulerJob so publishEvent is actually called
    SchedulerJob schedulerJob = mock(SchedulerJob.class);
    Task mockTask = mock(Task.class);
    TaskExecution mockExecution = mock(TaskExecution.class);
    when(context.getJobInstance()).thenReturn(schedulerJob);
    when(schedulerJob.getTask()).thenReturn(mockTask);
    when(schedulerJob.getExecution()).thenReturn(mockExecution);
    when(mockExecution.getExecutionTime()).thenReturn(null);

    // GIVEN: publishEvent throws
    doThrow(new RuntimeException("Simulated event publication failure"))
        .when(eventPublisher).publishEvent(any());

    // GIVEN: a pending overlay exists on this thread
    holder.mergeRequestData(overlayWith("leak-key", "must-be-cleaned"));
    assertNotNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be present before jobWasExecuted (pre-condition)");

    // WHEN: jobWasExecuted runs — publishEvent throws, but finally must still clear
    try {
      listener.jobWasExecuted(context, null);
    } catch (RuntimeException expected) {
      // Exception propagation is expected and correct — verify cause
      org.junit.jupiter.api.Assertions.assertEquals("Simulated event publication failure",
          expected.getMessage(), "Exception must be the original one, not wrapped");
    }

    // THEN: overlay is cleared despite the exception
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be cleared even when publishEvent throws — finally block guarantees cleanup");
  }

  /**
   * The RuntimeException from {@code publishEvent} propagates after the finally
   * block runs, so callers see the failure but the thread state is clean.
   */
  @Test
  void jobWasExecuted_whenPublishEventThrows_exceptionPropagatesAfterCleanup() {
    // GIVEN: a real SchedulerJob so publishEvent is actually called
    SchedulerJob schedulerJob = mock(SchedulerJob.class);
    Task mockTask = mock(Task.class);
    TaskExecution mockExecution = mock(TaskExecution.class);
    when(context.getJobInstance()).thenReturn(schedulerJob);
    when(schedulerJob.getTask()).thenReturn(mockTask);
    when(schedulerJob.getExecution()).thenReturn(mockExecution);
    when(mockExecution.getExecutionTime()).thenReturn(null);

    RuntimeException expected = new RuntimeException("Simulated event publication failure");
    doThrow(expected).when(eventPublisher).publishEvent(any());

    // GIVEN: a pending overlay exists
    holder.mergeRequestData(overlayWith("another-key", "must-be-cleaned"));

    // WHEN / THEN: exception propagates to the caller
    RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class,
        () -> listener.jobWasExecuted(context, null),
        "RuntimeException from publishEvent must propagate"
    );
    org.junit.jupiter.api.Assertions.assertSame(expected, thrown,
        "The thrown exception must be the original one, not wrapped");

    // THEN: overlay is still cleared despite exception propagation
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be cleared despite exception propagation — finally runs first");
  }
}
