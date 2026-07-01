package com.almis.awe.component;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AweMDCTaskDecoratorTest {

  @Mock
  private ObjectProvider<RequestDataHolder> requestDataHolderProvider;

  @Mock
  private RequestAttributes requestAttributes;

  @Mock
  private AweRequest aweRequest;

  private PrototypeRequestBeanHolder prototypeRequestBeanHolder;
  private AweMDCTaskDecorator decorator;

  @BeforeEach
  void setUp() {
    prototypeRequestBeanHolder = new PrototypeRequestBeanHolder();
    when(requestDataHolderProvider.getObject()).thenAnswer(invocation -> new RequestDataHolder());
    decorator = new AweMDCTaskDecorator(requestDataHolderProvider, prototypeRequestBeanHolder);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    prototypeRequestBeanHolder.clear();
    prototypeRequestBeanHolder.clearPendingPropagation();
    MDC.clear();
  }

  @Test
  void decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow() {
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();

    Runnable decoratedRunnable = assertDoesNotThrow(() -> decorator.decorate(() -> {
      RequestDataHolder requestDataHolder = prototypeRequestBeanHolder.getPrototypeBean();
      assertNotNull(requestDataHolder);
      seenSnapshot.set(requestDataHolder.getRequestData());
    }));

    assertDoesNotThrow(decoratedRunnable::run);

    assertNotNull(seenSnapshot.get());
    assertTrue(seenSnapshot.get().isEmpty());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());
  }

  @Test
  void decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot() {
    ObjectNode ancestorSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "bar");
    RequestDataHolder ancestorHolder = new RequestDataHolder();
    ancestorHolder.setRequestData(ancestorSnapshot);
    prototypeRequestBeanHolder.setPrototypeBean(ancestorHolder);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();
    AtomicReference<RequestDataHolder> seenHolder = new AtomicReference<>();

    Runnable decoratedRunnable = assertDoesNotThrow(() -> decorator.decorate(() -> {
      RequestDataHolder requestDataHolder = prototypeRequestBeanHolder.getPrototypeBean();
      seenHolder.set(requestDataHolder);
      seenSnapshot.set(requestDataHolder.getRequestData());
    }));

    assertDoesNotThrow(decoratedRunnable::run);

    assertNotNull(seenSnapshot.get());
    assertEquals("bar", seenSnapshot.get().get("foo").asText());
    assertTrue(seenSnapshot.get().size() > 0);
    assertNotNull(seenHolder.get());
    assertNotSame(ancestorHolder, seenHolder.get());
  }

  /**
   * Multi-hop precedence: when both a live AweRequest and an inherited ancestor snapshot are
   * present (the case on a worker thread after ContextAwareCallable re-installs RequestAttributes),
   * the ancestor snapshot wins on key conflict so that propagated values are not silently overwritten
   * by the stale live request on the second hop (child → grandchild).
   *
   * <p>This is the core behaviour change that fixes the grandchild null-propagation bug.
   * Previously the decorator treated the two sources as mutually exclusive, discarding the
   * ancestor snapshot whenever a live request was present.</p>
   */
  @Test
  void decorate_withActiveRequestScopeAndAncestorSnapshot_ancestorWinsOnKeyConflict() {
    // Ancestor snapshot carries the propagated "foo" value from the parent hop
    ObjectNode ancestorSnapshot = JsonNodeFactory.instance.objectNode()
        .put("foo", "ancestor")
        .put("ancestor-only", "from-ancestor");
    RequestDataHolder ancestorHolder = new RequestDataHolder();
    ancestorHolder.setRequestData(ancestorSnapshot);
    prototypeRequestBeanHolder.setPrototypeBean(ancestorHolder);

    // Live AweRequest has a stale "foo" and a live-only param
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode()
        .put("foo", "live")
        .put("live-only", "from-live");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();

    Runnable decoratedRunnable = decorator.decorate(() -> seenSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData()));
    decoratedRunnable.run();

    assertNotNull(seenSnapshot.get());
    // Ancestor wins on conflict — this is the fix for the grandchild null-propagation bug
    assertEquals("ancestor", seenSnapshot.get().get("foo").asText(),
        "Ancestor snapshot must win over stale live request on key conflict");
    // Live-only params are still present (live request provides base defaults)
    assertEquals("from-live", seenSnapshot.get().get("live-only").asText(),
        "Live-only params must still be visible as base defaults");
    // Ancestor-only params are present
    assertEquals("from-ancestor", seenSnapshot.get().get("ancestor-only").asText(),
        "Ancestor-only params must be visible");
  }

  @Test
  void decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild() throws Exception {
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "live");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    AtomicReference<ObjectNode> parentSnapshot = new AtomicReference<>();
    AtomicReference<ObjectNode> childSnapshot = new AtomicReference<>();
    AtomicReference<ObjectNode> grandchildSnapshot = new AtomicReference<>();
    AtomicReference<Runnable> childDecorated = new AtomicReference<>();
    AtomicReference<Runnable> grandchildDecorated = new AtomicReference<>();

    Runnable parentDecorated = decorator.decorate(() -> {
      parentSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy());
      childDecorated.set(decorator.decorate(() -> {
        childSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy());
        grandchildDecorated.set(decorator.decorate(() ->
          grandchildSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy())));
      }));
    });

    RequestContextHolder.resetRequestAttributes();

    executeInSingleThread(parentDecorated);
    executeInSingleThread(childDecorated.get());
    executeInSingleThread(grandchildDecorated.get());

    assertNotNull(parentSnapshot.get());
    assertEquals(parentSnapshot.get(), childSnapshot.get());
    assertEquals(childSnapshot.get(), grandchildSnapshot.get());
    assertEquals("live", grandchildSnapshot.get().get("foo").asText());
  }

  @Test
  void decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns() {
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "first");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    MDC.put("trace", "first-run");
    AtomicReference<ObjectNode> firstSnapshot = new AtomicReference<>();
    AtomicReference<String> firstMdc = new AtomicReference<>();

    Runnable firstDecorated = decorator.decorate(() -> {
      firstSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData());
      firstMdc.set(MDC.get("trace"));
    });
    firstDecorated.run();

    assertNotNull(firstSnapshot.get());
    assertEquals("first", firstSnapshot.get().get("foo").asText());
    assertEquals("first-run", firstMdc.get());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> secondSnapshot = new AtomicReference<>();
    AtomicReference<String> secondMdc = new AtomicReference<>();

    Runnable secondDecorated = decorator.decorate(() -> {
      secondSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData());
      secondMdc.set(MDC.get("trace"));
    });
    secondDecorated.run();

    assertNotNull(secondSnapshot.get());
    assertTrue(secondSnapshot.get().isEmpty());
    assertNull(secondMdc.get());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());
  }

  private void executeInSingleThread(Runnable runnable) throws Exception {
    assertNotNull(runnable);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<?> task = executorService.submit(runnable);
      task.get(10, TimeUnit.SECONDS);
    } finally {
      executorService.shutdownNow();
    }
  }

  // ─── Regression: first-hop propagated parameter bug ────────────────────────

  /**
   * Regression for the first-hop propagated parameter bug.
   *
   * <p>Before the fix, a parameter written on the request thread via
   * {@code putPropagatedRequestParameter} was silently dropped because
   * {@code PrototypeRequestBeanHolder.mergeRequestData()} returned early when no async holder
   * was active, and {@code AweMDCTaskDecorator} never consulted the pending overlay.</p>
   *
   * <p>The test simulates exactly what the user reported:
   * <ol>
   *   <li>Request thread adds "seed" to the propagated snapshot.</li>
   *   <li>First async child (decorated while live request is still active) sees "seed".</li>
   *   <li>Grandchild (decorated from inside the child) also sees "seed".</li>
   * </ol>
   * </p>
   */
  @Test
  void decorate_propagatesParameterAddedOnRequestThread_toFirstChildAndGrandchild() throws Exception {
    // Simulate the live AweRequest on the parent/request thread
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("existing", "live");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    // Parent request thread writes a NEW propagated parameter BEFORE dispatching async work
    prototypeRequestBeanHolder.mergeRequestData(
      JsonNodeFactory.instance.objectNode().put("seed", "hello-from-parent"));

    AtomicReference<String> childSeed = new AtomicReference<>();
    AtomicReference<String> grandchildSeed = new AtomicReference<>();
    AtomicReference<Runnable> grandchildDecorated = new AtomicReference<>();

    // Decorate the child runnable on the request thread (as @Async would)
    Runnable childDecorated = decorator.decorate(() -> {
      // Child reads the propagated parameter
      RequestDataHolder childHolder = prototypeRequestBeanHolder.getPrototypeBean();
      assertNotNull(childHolder);
      ObjectNode childParams = childHolder.getRequestData();
      childSeed.set(childParams != null && childParams.has("seed") ? childParams.get("seed").asText() : null);

      // Child decorates a grandchild runnable
      grandchildDecorated.set(decorator.decorate(() -> {
        RequestDataHolder grandchildHolder = prototypeRequestBeanHolder.getPrototypeBean();
        assertNotNull(grandchildHolder);
        ObjectNode grandchildParams = grandchildHolder.getRequestData();
        grandchildSeed.set(grandchildParams != null && grandchildParams.has("seed") ? grandchildParams.get("seed").asText() : null);
      }));
    });

    // After decoration, the pending overlay must still be available (it was only snapshotted,
    // not cleared — AwePropagationCleanupFilter is responsible for the final cleanup).
    assertNotNull(prototypeRequestBeanHolder.getPendingPropagationSnapshot(),
      "Pending propagation overlay must remain available after decorate() — it is a request-scoped resource cleared only by AwePropagationCleanupFilter");

    // Simulate request scope going away before the tasks actually run
    RequestContextHolder.resetRequestAttributes();

    executeInSingleThread(childDecorated);
    executeInSingleThread(grandchildDecorated.get());

    assertEquals("hello-from-parent", childSeed.get(),
      "Child async thread must see the parameter written on the request thread");
    assertEquals("hello-from-parent", grandchildSeed.get(),
      "Grandchild async thread must inherit the parameter through the child hop");
  }

  /**
   * Regression: pending overlay is merged ON TOP of the live request snapshot,
   * not instead of it.  Both the original live parameters AND the new propagated
   * parameter must reach the child.
   */
  @Test
  void decorate_pendingOverlayMergedOnTopOfLiveSnapshot_childSeesAllParameters() throws Exception {
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode()
      .put("req-param", "from-request")
      .put("shared", "from-request");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    // Propagated parameter added on the request thread (overrides "shared")
    prototypeRequestBeanHolder.mergeRequestData(
      JsonNodeFactory.instance.objectNode()
        .put("seed", "propagated")
        .put("shared", "from-propagated"));

    AtomicReference<ObjectNode> childParams = new AtomicReference<>();
    Runnable childDecorated = decorator.decorate(() -> {
      RequestDataHolder h = prototypeRequestBeanHolder.getPrototypeBean();
      assertNotNull(h);
      childParams.set(h.getRequestData());
    });

    RequestContextHolder.resetRequestAttributes();
    executeInSingleThread(childDecorated);

    assertNotNull(childParams.get());
    // Original live parameter preserved
    assertEquals("from-request", childParams.get().get("req-param").asText());
    // New propagated parameter visible
    assertEquals("propagated", childParams.get().get("seed").asText());
    // Overlay wins when key conflicts
    assertEquals("from-propagated", childParams.get().get("shared").asText());
  }

  // ─── Regression: cross-request leakage when no async task is submitted ────────

  /**
   * Regression: if a request writes propagated params but NEVER submits a decorated task,
   * the overlay must NOT survive to a subsequent unrelated request on the same thread.
   *
   * <p>This test simulates two sequential "requests" on the same thread:
   * <ol>
   *   <li>Request A writes a propagated param but submits no async task.</li>
   *   <li>Simulated end-of-request cleanup (as performed by
   *       {@link com.almis.awe.component.AwePropagationCleanupFilter}) clears the overlay.</li>
   *   <li>Request B (unrelated) must not see any param from Request A.</li>
   * </ol>
   * </p>
   */
  @Test
  void decorate_afterRequestCleanup_laterUnrelatedWorkOnSameThreadDoesNotInheritStaleOverlay() throws Exception {
    // Request A: writes a propagated param but never calls decorate()
    ObjectNode requestASnapshot = JsonNodeFactory.instance.objectNode().put("req-a", "from-request-a");
    when(aweRequest.getParametersSafe()).thenReturn(requestASnapshot);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    prototypeRequestBeanHolder.mergeRequestData(
      JsonNodeFactory.instance.objectNode().put("secret", "should-not-leak"));

    // AwePropagationCleanupFilter runs at end of Request A — simulated here
    prototypeRequestBeanHolder.clearPendingPropagation();
    RequestContextHolder.resetRequestAttributes();

    // Request B: unrelated request on same thread, different live params
    ObjectNode requestBSnapshot = JsonNodeFactory.instance.objectNode().put("req-b", "from-request-b");
    when(aweRequest.getParametersSafe()).thenReturn(requestBSnapshot);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    AtomicReference<ObjectNode> taskBParams = new AtomicReference<>();
    Runnable taskB = decorator.decorate(() ->
      taskBParams.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData())
    );
    RequestContextHolder.resetRequestAttributes();
    executeInSingleThread(taskB);

    assertNotNull(taskBParams.get());
    assertNull(taskBParams.get().get("secret"),
      "Request B must NOT inherit the stale param written by Request A — cross-request leakage must be prevented");
    assertEquals("from-request-b", taskBParams.get().get("req-b").asText(),
      "Request B must see its own live request params");
  }

  /**
   * Sibling task semantics: the request-scoped pending overlay is NOT consumed by the first
   * {@code decorate()} call. Every sibling task submitted from the same request thread during
   * the same request must receive the same propagated parameters.
   *
   * <p>This is the deliberate lifecycle: the overlay is request-scoped and is cleared only by
   * {@link com.almis.awe.component.AwePropagationCleanupFilter} at request end — not by
   * {@code decorate()}.  Multiple sibling async tasks therefore all get a consistent,
   * independent snapshot of the propagated parameters.</p>
   *
   * <p>Replacing the old test that incorrectly expected the overlay to be consumed on the first
   * {@code decorate()} call.</p>
   */
  @Test
  void decorate_siblingTasks_bothSeeRequestOverlay() throws Exception {
    // Use thenAnswer so each call to getParametersSafe() returns a fresh independent copy
    // — mirrors AweRequest.getParametersSafe() (deepCopy).
    ObjectNode baseSnapshot = JsonNodeFactory.instance.objectNode().put("req-param", "x");
    when(aweRequest.getParametersSafe()).thenAnswer(inv -> baseSnapshot.deepCopy());
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenReturn(aweRequest);

    // Request thread writes a propagated parameter before any task is submitted
    prototypeRequestBeanHolder.mergeRequestData(
      JsonNodeFactory.instance.objectNode().put("seed", "shared-by-all-siblings"));

    // First sibling task
    AtomicReference<ObjectNode> firstTaskParams = new AtomicReference<>();
    Runnable firstDecorated = decorator.decorate(() ->
      firstTaskParams.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData()));

    // Overlay must still be present after first decoration (snapshot only — not cleared)
    assertNotNull(prototypeRequestBeanHolder.getPendingPropagationSnapshot(),
      "Overlay must remain available after first decorate() — request lifecycle, not task lifecycle");

    // Second sibling task decorated from the same request thread
    AtomicReference<ObjectNode> secondTaskParams = new AtomicReference<>();
    Runnable secondDecorated = decorator.decorate(() ->
      secondTaskParams.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData()));

    // Simulate AwePropagationCleanupFilter running at request end (before tasks execute)
    prototypeRequestBeanHolder.clearPendingPropagation();
    RequestContextHolder.resetRequestAttributes();

    executeInSingleThread(firstDecorated);
    executeInSingleThread(secondDecorated);

    // Both tasks must see the propagated parameter — sibling semantics
    assertEquals("shared-by-all-siblings", firstTaskParams.get().get("seed").asText(),
      "First sibling task must see the propagated parameter");
    assertEquals("shared-by-all-siblings", secondTaskParams.get().get("seed").asText(),
      "Second sibling task must also see the propagated parameter — overlay is request-scoped");

    // Both tasks also retain the base live-request param
    assertEquals("x", firstTaskParams.get().get("req-param").asText());
    assertEquals("x", secondTaskParams.get().get("req-param").asText());
  }

  // ─── Regression: worker-thread authoritative cleanup (pendingPropagation) ──

  /**
   * Worker-thread overlay isolation on a single-thread pool.
   *
   * <p>Task A writes a propagated parameter during its execution (simulating a scheduled job
   * that calls {@code putPropagatedRequestParameter}).  Task B, reusing the same pool thread,
   * must NOT see Task A's overlay — the decorator's {@code finally} must have cleared it.</p>
   *
   * <p>Before the fix, the worker-thread {@code pendingPropagation} slot was never cleared
   * by the decorator, so Task B would inherit Task A's overlay on a single-thread pool.</p>
   */
  @Test
  void decorate_workerThreadOverlay_isNotInheritedByNextTaskOnSameThread() throws Exception {
    ExecutorService singleThread = Executors.newSingleThreadExecutor();
    try {
      AtomicReference<String> taskBOverlay = new AtomicReference<>();

      // Task A: decorated without a live request (simulates a scheduler worker)
      Runnable taskADecorated = decorator.decorate(() -> {
        // Task A writes to pendingPropagation on the worker thread
        prototypeRequestBeanHolder.mergeRequestData(
            JsonNodeFactory.instance.objectNode().put("task-a-secret", "should-not-leak"));
      });

      // Task B: also decorated without a live request — should see nothing from Task A
      Runnable taskBDecorated = decorator.decorate(() -> {
        ObjectNode snapshot = prototypeRequestBeanHolder.getPendingPropagationSnapshot();
        taskBOverlay.set(snapshot != null && snapshot.has("task-a-secret")
            ? snapshot.get("task-a-secret").asText() : null);
      });

      // Run both tasks on the same single-thread pool (Task A then Task B)
      Future<?> a = singleThread.submit(taskADecorated);
      a.get(10, TimeUnit.SECONDS);

      Future<?> b = singleThread.submit(taskBDecorated);
      b.get(10, TimeUnit.SECONDS);

      assertNull(taskBOverlay.get(),
          "Task B must NOT inherit the pendingPropagation overlay written by Task A on the same thread — " +
          "AweMDCTaskDecorator finally must clear it between tasks");
    } finally {
      singleThread.shutdownNow();
    }
  }

  /**
   * Nested async hop (child decorates grandchild) still works correctly after the fix.
   *
   * <p>Verifies that clearing {@code pendingPropagation} in the worker-thread {@code finally}
   * does not break the grandchild propagation path: the grandchild snapshot is built at
   * <em>decoration time</em> on the child thread (before the child's finally runs), so the
   * propagated value is already captured in the grandchild's {@link RequestDataHolder} and
   * is unaffected by the subsequent cleanup.</p>
   */
  @Test
  void decorate_nestedAsyncHop_grandchildStillReceivesPropagatedParameter() throws Exception {
    // Parent request: write a param to be propagated
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("base", "from-request");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
        .thenReturn(aweRequest);

    prototypeRequestBeanHolder.mergeRequestData(
        JsonNodeFactory.instance.objectNode().put("propagated", "must-reach-grandchild"));

    AtomicReference<String> grandchildValue = new AtomicReference<>();
    AtomicReference<Runnable> grandchildDecorated = new AtomicReference<>();

    // Child decorated on request thread — captures overlay at decoration time
    Runnable childDecorated = decorator.decorate(() -> {
      // Child also writes its own overlay during execution
      prototypeRequestBeanHolder.mergeRequestData(
          JsonNodeFactory.instance.objectNode().put("child-extra", "from-child"));

      // Grandchild decorated on worker thread — captures child's overlay BEFORE child's finally
      grandchildDecorated.set(decorator.decorate(() -> {
        RequestDataHolder h = prototypeRequestBeanHolder.getPrototypeBean();
        assertNotNull(h);
        ObjectNode params = h.getRequestData();
        grandchildValue.set(params != null && params.has("propagated")
            ? params.get("propagated").asText() : null);
      }));
    });

    // Simulate request scope going away
    prototypeRequestBeanHolder.clearPendingPropagation();
    RequestContextHolder.resetRequestAttributes();

    executeInSingleThread(childDecorated);
    assertNotNull(grandchildDecorated.get(), "Grandchild Runnable must have been created");
    executeInSingleThread(grandchildDecorated.get());

    assertEquals("must-reach-grandchild", grandchildValue.get(),
        "Grandchild must receive the parameter propagated from the parent request thread " +
        "through the child hop — worker-thread finally cleanup must not break nested hops");
  }

  /**
   * Sequential scheduler work on a single-thread pool does not leak overlay across tasks.
   *
   * <p>Simulates two independent scheduler jobs reusing the same worker thread.
   * Each job writes a unique overlay; each job must only see its own overlay and never
   * the previous job's values.  This covers the {@code schedulerJobPool} / {@code schedulerTaskPool}
   * scenario described in the fix design.</p>
   */
  @Test
  void decorate_sequentialSchedulerJobsOnSameThread_overlayDoesNotLeakAcrossJobs() throws Exception {
    ExecutorService singleThread = Executors.newSingleThreadExecutor();
    try {
      AtomicReference<String> job2SawFromJob1 = new AtomicReference<>();
      AtomicBoolean job2OverlayClean = new AtomicBoolean(false);

      // Job 1: writes an overlay, simulating ReportGenerator or similar in a scheduled task
      Runnable job1Decorated = decorator.decorate(() ->
          prototypeRequestBeanHolder.mergeRequestData(
              JsonNodeFactory.instance.objectNode().put("job1-report-id", "abc-123")));

      // Job 2: checks whether job1's overlay leaked
      Runnable job2Decorated = decorator.decorate(() -> {
        ObjectNode snapshot = prototypeRequestBeanHolder.getPendingPropagationSnapshot();
        if (snapshot == null || !snapshot.has("job1-report-id")) {
          job2OverlayClean.set(true);
          job2SawFromJob1.set(null);
        } else {
          job2SawFromJob1.set(snapshot.get("job1-report-id").asText());
        }
      });

      Future<?> f1 = singleThread.submit(job1Decorated);
      f1.get(10, TimeUnit.SECONDS);

      Future<?> f2 = singleThread.submit(job2Decorated);
      f2.get(10, TimeUnit.SECONDS);

      assertTrue(job2OverlayClean.get(),
          "Job 2 must NOT see Job 1's pendingPropagation overlay — sequential scheduler jobs " +
          "on the same thread must be fully isolated; leaked value was: " + job2SawFromJob1.get());
    } finally {
      singleThread.shutdownNow();
    }
  }
}
