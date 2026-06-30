package com.almis.awe.thread;

import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.executor.ContextAwarePoolExecutor;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Real async-path regression for parameter propagation.
 *
 * <p>Unlike {@link com.almis.awe.component.AweMDCTaskDecoratorTest}, which operates on
 * the decorator snapshot in isolation, this suite exercises the <em>complete effective path</em>:
 *
 * <ol>
 *   <li>A live {@link AweRequest} is present on the parent (web) thread.</li>
 *   <li>The parent calls {@link PrototypeRequestBeanHolder#mergeRequestData(ObjectNode)} to
 *       register a propagated overlay (mirrors {@code putPropagatedRequestParameter}).</li>
 *   <li>A task is submitted through {@link ContextAwarePoolExecutor}, which wraps it in
 *       {@link ContextAwareCallable}.  That callable re-installs the captured
 *       {@link RequestAttributes} on the async thread, which re-exposes the live
 *       {@link AweRequest} — but that live request does NOT contain the propagated param.</li>
 *   <li>{@link AweMDCTaskDecorator} snapshot (built with the overlay) is available via
 *       {@link PrototypeRequestBeanHolder#getPrototypeBean()}.</li>
 *   <li>The child reads via {@link QueryUtil#getParameters()} — the same code path used by
 *       {@code ServiceConfig.getRequestParameterAsString} — and must see the merged value.</li>
 * </ol>
 *
 * <p>This is a <strong>false-positive detector</strong>: if the fix in
 * {@code QueryUtil.getParametersFromRequest()} is reverted, the test fails with
 * {@code null} on the child/grandchild assertion because the propagated snapshot is
 * never overlaid on the re-installed live request.
 */
@ExtendWith(MockitoExtension.class)
class AsyncParameterPropagationRegressionTest {

  // ── Framework collaborators ─────────────────────────────────────────────────

  @Mock
  private ObjectProvider<RequestDataHolder> requestDataHolderProvider;

  @Mock
  private RequestAttributes requestAttributes;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private ApplicationContext applicationContext;

  // ── Real instances ──────────────────────────────────────────────────────────

  /** Caller thread id — used to distinguish web-thread from async threads. */
  private long webThreadId;

  private PrototypeRequestBeanHolder prototypeRequestBeanHolder;
  private AweMDCTaskDecorator decorator;
  private QueryUtil queryUtil;
  private ContextAwarePoolExecutor executor;

  @BeforeEach
  void setUp() {
    webThreadId = Thread.currentThread().getId();

    prototypeRequestBeanHolder = new PrototypeRequestBeanHolder();
    when(requestDataHolderProvider.getObject()).thenAnswer(inv -> new RequestDataHolder());
    decorator = new AweMDCTaskDecorator(requestDataHolderProvider, prototypeRequestBeanHolder);

    // Real QueryUtil wired with the real PrototypeRequestBeanHolder.
    // ApplicationContext is mocked: getBean(AweRequest.class) returns the mocked live request
    // on the web thread, and throws on async threads (simulating expired request scope).
    BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
    DatabaseConfigProperties databaseConfigProperties = new DatabaseConfigProperties();
    queryUtil = new QueryUtil(baseConfigProperties, databaseConfigProperties, new ObjectMapper(), prototypeRequestBeanHolder);
    queryUtil.setApplicationContext(applicationContext);

    // ContextAwarePoolExecutor with the AweMDCTaskDecorator applied
    executor = new ContextAwarePoolExecutor();
    executor.setTaskDecorator(decorator);
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(4);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
  }

  @AfterEach
  void tearDown() {
    executor.shutdown();
    RequestContextHolder.resetRequestAttributes();
    prototypeRequestBeanHolder.clear();
    prototypeRequestBeanHolder.clearPendingPropagation();
  }

  // ── Helper ──────────────────────────────────────────────────────────────────

  /**
   * Configures the mocks to replicate the real runtime:
   *
   * <ul>
   *   <li>On the web thread: {@code getBean(AweRequest.class)} returns the mocked request
   *       and the mocked {@link RequestAttributes} resolves the scoped bean — so
   *       {@link com.almis.awe.component.AweMDCTaskDecorator} can capture a live snapshot.</li>
   *   <li>On async threads: {@code getBean(AweRequest.class)} throws
   *       {@link IllegalStateException} (request scope expired), which
   *       {@link com.almis.awe.config.ServiceConfig#getRequest()} catches and translates to
   *       {@code null}, making the QueryUtil fall through to the propagated snapshot.</li>
   * </ul>
   */
  private void stubContextBeans(ObjectNode liveParams) {
    long capturedWebId = webThreadId;

    when(aweRequest.getParametersSafe()).thenReturn(liveParams);
    when(applicationContext.getBean(AweRequest.class)).thenAnswer(inv -> {
      if (Thread.currentThread().getId() == capturedWebId) {
        return aweRequest;
      }
      // Async thread — request scope gone; ServiceConfig.getRequest() catches this
      throw new IllegalStateException("No request-scoped bean available");
    });
    when(requestAttributes.getAttribute(
        eq("scopedTarget.aweRequest"), eq(RequestAttributes.SCOPE_REQUEST)))
        .thenReturn(aweRequest);
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  // ── Tests ───────────────────────────────────────────────────────────────────

  /**
   * Core regression: a parameter written on the parent/web thread via
   * {@code mergeRequestData} (i.e. {@code putPropagatedRequestParameter}) must be
   * visible in the first async child through {@code QueryUtil.getParameters()}.
   *
   * <p>Root cause without the fix: {@code getParametersFromRequest()} preferred the
   * re-installed live AweRequest (via {@link ContextAwareCallable}) and never consulted
   * the {@link PrototypeRequestBeanHolder} snapshot that carries the merged overlay.
   */
  @Test
  void childAsyncThread_seesParameterWrittenOnParentThread_throughQueryUtilPath() throws Exception {
    // GIVEN: live AweRequest present on the web thread
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("existingParam", "original");
    stubContextBeans(liveParams);

    // Parent writes a new propagated parameter BEFORE submitting the task
    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("seed", "hello-from-parent"));

    // WHEN: submit through the real ContextAwarePoolExecutor
    AtomicReference<String> childSeed = new AtomicReference<>();
    AtomicReference<String> childExistingParam = new AtomicReference<>();

    executor.submitCompletable(() -> {
      // Read via QueryUtil.getParameters() — same path as getRequestParameterAsString
      ObjectNode params = queryUtil.getParameters();
      childSeed.set(params.has("seed") ? params.get("seed").asText() : null);
      childExistingParam.set(params.has("existingParam") ? params.get("existingParam").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    // THEN: propagated param present, original live-request param preserved
    assertEquals("hello-from-parent", childSeed.get(),
        "Child must see the parameter written on the parent thread via mergeRequestData/putPropagatedRequestParameter");
    assertEquals("original", childExistingParam.get(),
        "Child must also retain the original live-request parameters");
  }

  /**
   * Multi-hop regression: grandchild inherits the propagated parameter.
   */
  @Test
  void grandchildAsyncThread_seesParameterWrittenOnParentThread_throughQueryUtilPath() throws Exception {
    // GIVEN
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("existingParam", "original");
    stubContextBeans(liveParams);

    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("seed", "hello-from-parent"));

    AtomicReference<String> grandchildSeed = new AtomicReference<>();
    AtomicReference<String> grandchildExistingParam = new AtomicReference<>();

    // WHEN: child submits grandchild — multi-hop propagation
    executor.submitCompletable(() -> {
      // Child reads (optional assertion — covered by the single-hop test)
      executor.submitCompletable(() -> {
        // Grandchild reads
        ObjectNode params = queryUtil.getParameters();
        grandchildSeed.set(params.has("seed") ? params.get("seed").asText() : null);
        grandchildExistingParam.set(params.has("existingParam") ? params.get("existingParam").asText() : null);
        return null;
      }).get(10, TimeUnit.SECONDS);
      return null;
    }).get(15, TimeUnit.SECONDS);

    // THEN
    assertEquals("hello-from-parent", grandchildSeed.get(),
        "Grandchild must inherit the propagated parameter through parent→child→grandchild hops");
    assertEquals("original", grandchildExistingParam.get(),
        "Grandchild must also retain the original live-request parameters");
  }

  /**
   * Precedence rule: propagated overlay wins over live-request on key conflict.
   */
  @Test
  void propagatedOverlay_winsOverLiveRequestOnKeyConflict_throughQueryUtilPath() throws Exception {
    // GIVEN: live request has "shared" = "from-request"
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode()
        .put("shared", "from-request")
        .put("live-only", "live-value");
    stubContextBeans(liveParams);

    // Parent overrides "shared" and adds "propagated-only"
    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode()
            .put("shared", "from-propagated")
            .put("propagated-only", "added-by-parent"));

    AtomicReference<String> childShared = new AtomicReference<>();
    AtomicReference<String> childLiveOnly = new AtomicReference<>();
    AtomicReference<String> childPropagatedOnly = new AtomicReference<>();

    // WHEN
    executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      childShared.set(params.has("shared") ? params.get("shared").asText() : null);
      childLiveOnly.set(params.has("live-only") ? params.get("live-only").asText() : null);
      childPropagatedOnly.set(params.has("propagated-only") ? params.get("propagated-only").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    // THEN: overlay wins on conflict, live-only and propagated-only both present
    assertEquals("from-propagated", childShared.get(),
        "Propagated overlay must win over live-request on key conflict");
    assertEquals("live-value", childLiveOnly.get(),
        "Live-only param must still be visible (not wiped by the overlay)");
    assertEquals("added-by-parent", childPropagatedOnly.get(),
        "New param from propagated overlay must be visible in child");
  }

  /**
   * No regression for ordinary parameters: when no propagated overlay is set,
   * the child still sees live-request parameters normally.
   */
  @Test
  void childAsyncThread_seesOrdinaryLiveRequestParams_whenNoPropagatedOverlayExists() throws Exception {
    // GIVEN: live request only, no propagated overlay
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("ordinary", "from-request");
    stubContextBeans(liveParams);

    AtomicReference<String> childOrdinary = new AtomicReference<>();

    // WHEN
    executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      childOrdinary.set(params.has("ordinary") ? params.get("ordinary").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    // THEN: ordinary params still flow as before
    assertEquals("from-request", childOrdinary.get(),
        "Ordinary live-request params must still reach child when no overlay is set");
  }

  /**
   * Targeted regression for the exact grandchild null-propagation failure.
   *
   * <p><strong>Root cause being tested:</strong> {@link ContextAwareCallable} re-installs the
   * original live {@link RequestAttributes} on the child worker thread before running the child
   * task body.  When the child body then submits a grandchild task via
   * {@link ContextAwarePoolExecutor#submitCompletable}, the decorator runs on the child thread —
   * where the live request is now active again.  The old decorator code treated the live request
   * and the inherited ancestor snapshot as mutually exclusive: because the live request was
   * non-null, the ancestor snapshot (which carries the propagated "seed" param) was silently
   * discarded.  The grandchild's {@link RequestDataHolder} therefore only contained the stale
   * live-request params — no "seed" — and all reads via {@code QueryUtil.getParameters()} /
   * {@code getRequestParameterAsString} returned null.</p>
   *
   * <p><strong>Fix being validated:</strong> {@code AweMDCTaskDecorator.resolveRequestSnapshot()}
   * now performs a three-layer merge: live request (base) → ancestor snapshot (wins on conflict
   * so propagated values survive the re-install) → pending overlay (highest priority).  The
   * grandchild therefore receives the full merged parameter set across all hops.</p>
   *
   * <p>This test will FAIL if the fix is reverted to the mutually-exclusive branch.</p>
   */
  @Test
  void grandchildAsyncThread_contextAwareCallableReinstall_propagatedParamNotLostOnSecondHop() throws Exception {
    // GIVEN: live AweRequest present (simulates a Spring web request with existing params)
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode()
        .put("existingParam", "from-request")
        .put("shared", "from-live-request");
    stubContextBeans(liveParams);

    // Parent thread writes a propagated parameter that the live request does NOT contain.
    // This mirrors: serviceConfig.putPropagatedRequestParameter("seed", "hello-from-parent")
    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode()
            .put("seed", "hello-from-parent")
            .put("shared", "from-propagated"));  // should win over live "shared" at all hops

    AtomicReference<String> childSeed = new AtomicReference<>();
    AtomicReference<String> childShared = new AtomicReference<>();
    AtomicReference<String> grandchildSeed = new AtomicReference<>();
    AtomicReference<String> grandchildShared = new AtomicReference<>();
    AtomicReference<String> grandchildExisting = new AtomicReference<>();

    // WHEN: child submitted from web thread; grandchild submitted from inside child worker thread.
    // ContextAwareCallable re-installs the original RequestAttributes on each worker thread,
    // making the live AweRequest visible again from RequestContextHolder — this is the exact
    // interference path that caused the grandchild to see null before the fix.
    executor.submitCompletable(() -> {
      // ── CHILD THREAD ─────────────────────────────────────────────────────────
      // ContextAwareCallable has re-installed the original RequestAttributes here.
      // Validate child sees the correct merged params (covered by existing test too).
      ObjectNode childParams = queryUtil.getParameters();
      childSeed.set(childParams.has("seed") ? childParams.get("seed").asText() : null);
      childShared.set(childParams.has("shared") ? childParams.get("shared").asText() : null);

      // Submit grandchild FROM the child worker thread — this triggers the exact failure path:
      // the decorator runs on this child thread where live RequestAttributes are active,
      // and ContextAwarePoolExecutor captures the current (re-installed) RequestAttributes.
      executor.submitCompletable(() -> {
        // ── GRANDCHILD THREAD ─────────────────────────────────────────────────
        ObjectNode grandchildParams = queryUtil.getParameters();
        grandchildSeed.set(grandchildParams.has("seed") ? grandchildParams.get("seed").asText() : null);
        grandchildShared.set(grandchildParams.has("shared") ? grandchildParams.get("shared").asText() : null);
        grandchildExisting.set(grandchildParams.has("existingParam") ? grandchildParams.get("existingParam").asText() : null);
        return null;
      }).get(10, TimeUnit.SECONDS);

      return null;
    }).get(15, TimeUnit.SECONDS);

    // THEN — child assertions
    assertEquals("hello-from-parent", childSeed.get(),
        "Child must see the propagated parameter (first hop — regression guard)");
    assertEquals("from-propagated", childShared.get(),
        "Child: propagated overlay must win over live-request on key conflict (first hop)");

    // THEN — grandchild assertions (these are the ones that failed before the fix)
    assertEquals("hello-from-parent", grandchildSeed.get(),
        "Grandchild must inherit the propagated parameter despite ContextAwareCallable " +
        "re-installing the live RequestAttributes on the child worker thread (second hop)");
    assertEquals("from-propagated", grandchildShared.get(),
        "Grandchild: propagated ancestor snapshot must still win over stale live request on conflict");
    assertEquals("from-request", grandchildExisting.get(),
        "Grandchild must still receive live-request-only params as base defaults");
  }

  /**
   * Sibling task lifecycle: multiple async tasks submitted within the same request all
   * receive the propagated overlay — not just the first one.
   *
   * <p>This guards against the earlier (incorrect) implementation where the pending overlay
   * was consumed (cleared) on the first {@code decorate()} call, causing sibling tasks
   * submitted later in the same request to see no propagated parameters.</p>
   *
   * <p>The correct lifecycle:</p>
   * <ol>
   *   <li>Request thread writes the overlay via {@code mergeRequestData}.</li>
   *   <li>Every call to {@code decorate()} takes an immutable snapshot copy — overlay stays.</li>
   *   <li>{@link com.almis.awe.component.AwePropagationCleanupFilter} clears the overlay
   *       at request end (simulated here by an explicit call to
   *       {@link PrototypeRequestBeanHolder#clearPendingPropagation()}).</li>
   *   <li>After the filter runs, the overlay is gone and later submissions see nothing.</li>
   * </ol>
   */
  @Test
  void siblingAsyncTasks_bothSeeRequestOverlay_overlayPersistsUntilFilterCleanup() throws Exception {
    // GIVEN: live request with an existing param
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("base", "live");
    stubContextBeans(liveParams);

    // Parent writes a propagated parameter shared by all sibling tasks
    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("shared-seed", "from-parent"));

    AtomicReference<String> firstSiblingResult = new AtomicReference<>();
    AtomicReference<String> secondSiblingResult = new AtomicReference<>();

    // WHEN: submit two sibling tasks through the real executor
    // (Decorator takes two independent snapshot copies — overlay stays alive between them)
    CompletableFuture<Void> first = executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      firstSiblingResult.set(params.has("shared-seed") ? params.get("shared-seed").asText() : null);
      return null;
    });

    CompletableFuture<Void> second = executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      secondSiblingResult.set(params.has("shared-seed") ? params.get("shared-seed").asText() : null);
      return null;
    });

    first.get(10, TimeUnit.SECONDS);
    second.get(10, TimeUnit.SECONDS);

    // THEN: both siblings see the propagated parameter
    assertEquals("from-parent", firstSiblingResult.get(),
        "First sibling must see the propagated parameter");
    assertEquals("from-parent", secondSiblingResult.get(),
        "Second sibling must also see the propagated parameter — overlay is request-scoped, not consumed by first decorate()");

    // Simulate AwePropagationCleanupFilter at request end
    prototypeRequestBeanHolder.clearPendingPropagation();

    // Any task decorated AFTER the filter runs must see no overlay
    AtomicReference<String> lateTaskResult = new AtomicReference<>();
    executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      lateTaskResult.set(params.has("shared-seed") ? params.get("shared-seed").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    assertNull(lateTaskResult.get(),
        "A task decorated after filter cleanup must not see the overlay — no cross-request leakage");
  }

  /**
   * Rejected-first-submission lifecycle: the propagated overlay must survive a rejected
   * executor submission and remain available for a subsequent retry within the same request.
   *
   * <p><strong>Scenario (mirrors real production path):</strong></p>
   * <ol>
   *   <li>Parent request thread writes a propagated parameter via {@code mergeRequestData}.</li>
   *   <li>First submission attempt to the executor is <em>rejected</em> (queue full, etc.).</li>
   *   <li>The caller catches the {@link RejectedExecutionException} and retries via the main
   *       executor.</li>
   *   <li>The retried task must still see the propagated parameter — the overlay must not have
   *       been cleared by the first (failed) decoration attempt.</li>
   * </ol>
   *
   * <p><strong>Why this can fail without the fix:</strong> if {@code decorate()} had ever called
   * {@code clearPendingPropagation()} on success, a rejected submission (where the task is
   * decorated but never actually runs) would leave the overlay in an indeterminate state. With the
   * current design, {@code decorate()} only takes an immutable snapshot and never clears the
   * overlay — so rejection is safe. This test is a permanent regression guard for that contract.</p>
   *
   * <p><strong>Determinism guarantee:</strong> a {@link CountDownLatch} ({@code blockerStarted})
   * ensures the single worker thread is provably occupied <em>before</em> the second submission
   * is attempted. Without this latch the window between the blocker submit and the rejection
   * attempt could allow the blocker to finish first, making the second submission succeed and
   * the rejection never happen.</p>
   */
  @Test
  void retryAfterRejection_propagatedOverlayAvailableForRetryWithinSameRequest() throws Exception {
    // GIVEN: live request on web thread
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("base", "live");
    stubContextBeans(liveParams);

    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("seed", "from-parent"));

    // Saturated executor: 1 thread, no queue — any submission while the thread is busy is rejected.
    ContextAwarePoolExecutor saturatedExecutor = new ContextAwarePoolExecutor();
    saturatedExecutor.setTaskDecorator(decorator);
    saturatedExecutor.setCorePoolSize(1);
    saturatedExecutor.setMaxPoolSize(1);
    saturatedExecutor.setQueueCapacity(0);
    saturatedExecutor.initialize();

    // Latch: the blocker signals this the moment it starts running, so the test thread
    // knows the single worker is provably occupied before it makes the rejection attempt.
    CountDownLatch blockerStarted = new CountDownLatch(1);
    CountDownLatch blockerRelease = new CountDownLatch(1);

    CompletableFuture<Void> blocker = saturatedExecutor.submitCompletable(() -> {
      blockerStarted.countDown();               // signal: worker thread is now busy
      blockerRelease.await(5, TimeUnit.SECONDS); // hold until test lets it go
      return null;
    });

    // Wait until the worker thread is provably occupied — eliminates the timing race.
    assertTrue(blockerStarted.await(5, TimeUnit.SECONDS),
        "Blocker task must start within 5 s — executor appears stuck");

    // First submission attempt — rejected because the single thread is still blocked.
    AtomicInteger rejectionCount = new AtomicInteger(0);
    try {
      saturatedExecutor.submitCompletable(() -> {
        // This task body must never run — the submission should be rejected immediately.
        return null;
      });
      fail("Expected RejectedExecutionException — the saturated executor should have rejected the submission");
    } catch (RejectedExecutionException ex) {
      // Expected: Spring's executor throws RejectedExecutionException directly on submit.
      rejectionCount.incrementAndGet();
    }

    // Confirm the rejection actually happened (test would have failed above, but be explicit).
    assertEquals(1, rejectionCount.get(), "Exactly one rejection must have occurred");

    // Release the blocker and let the saturated executor finish cleanly.
    blockerRelease.countDown();
    blocker.get(5, TimeUnit.SECONDS);
    saturatedExecutor.shutdown();

    // THEN: overlay must still be intact despite the rejected first attempt.
    // decorate() only takes an immutable snapshot — it must never clear pendingPropagation.
    assertNotNull(prototypeRequestBeanHolder.getPendingPropagationSnapshot(),
        "Pending propagation overlay must survive a rejected submission — decorate() must never clear it");

    // WHEN: retry via the main executor (which has capacity)
    AtomicReference<String> retryResult = new AtomicReference<>();
    executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      retryResult.set(params.has("seed") ? params.get("seed").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    // THEN: retry sees the propagated parameter
    assertEquals("from-parent", retryResult.get(),
        "Retry task must see the propagated overlay that survived the rejected first submission");
  }

  /**
   * Failed-task-body lifecycle: a decorated task that throws during execution must not
   * corrupt the propagated overlay for subsequent tasks in the same request.
   *
   * <p>The {@code finally} block in the decorated {@link Runnable} clears the worker-thread's
   * {@link PrototypeRequestBeanHolder} (prototype bean only), but must not touch the request
   * thread's {@code pendingPropagation} overlay. A sibling task submitted after the failure
   * must therefore still receive the full overlay.</p>
   */
  @Test
  void failedFirstTask_siblingTaskStillSeesPropagatedOverlay() throws Exception {
    // GIVEN: propagated overlay set before any tasks are submitted
    ObjectNode liveParams = JsonNodeFactory.instance.objectNode().put("base", "live");
    stubContextBeans(liveParams);

    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("seed", "should-survive-failure"));

    // First task: decorated successfully, but its body throws at runtime
    CompletableFuture<Void> failingTask = executor.submitCompletable(() -> {
      // Simulate a business-logic exception inside the task body
      throw new RuntimeException("intentional failure");
    });

    // Absorb the failure — we expect an ExecutionException wrapping RuntimeException
    try {
      failingTask.get(10, TimeUnit.SECONDS);
    } catch (java.util.concurrent.ExecutionException ex) {
      assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
    }

    // THEN: overlay must still be present on the request thread
    assertNotNull(prototypeRequestBeanHolder.getPendingPropagationSnapshot(),
        "Request-thread overlay must survive a failed task body — the task's finally block must not clear pendingPropagation");

    // WHEN: sibling task submitted after the failure
    AtomicReference<String> siblingResult = new AtomicReference<>();
    executor.submitCompletable(() -> {
      ObjectNode params = queryUtil.getParameters();
      siblingResult.set(params.has("seed") ? params.get("seed").asText() : null);
      return null;
    }).get(10, TimeUnit.SECONDS);

    // THEN: sibling sees the overlay
    assertEquals("should-survive-failure", siblingResult.get(),
        "Sibling task must still see the propagated overlay after a prior task failed");
  }
}
