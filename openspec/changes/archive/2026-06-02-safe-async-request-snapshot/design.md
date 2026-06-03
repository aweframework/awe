# Design: safe-async-request-snapshot

## Metadata
- Change: `safe-async-request-snapshot`
- Phase: design
- Skill resolution: `paths-injected`
- Artifact store: OpenSpec
- Engram persistence: unavailable in this session, so no memory write was possible
- Forecast: small implementation, expected under the 400-line review budget

## Current-state evidence
- `AweMDCTaskDecorator.decorate(...)` currently resolves request parameters with only two outcomes: live request scope or empty object. It does **not** consult any propagated async snapshot and it does **not** guard `RequestAttributes#getAttribute(...)` against inactive-scope exceptions.
- `QueryUtil#getParametersFromRequest()` already uses a safer runtime fallback order: live `AweRequest` -> `PrototypeRequestBeanHolder` -> empty `ObjectNode`.
- No existing `AweMDCTaskDecoratorTest` was found under `awe-framework/awe-controller/src/test/java`.
- No Maven tests were run in this phase because the user requested a design-only artifact and no implementation yet.

## Goal
Make async request snapshot capture deterministic at task-decoration time so nested `@Async` hops keep the original request parameters even after Spring request scope disappears.

---

## 1. Exact resolution algorithm inside `AweMDCTaskDecorator`

### Decision
`AweMDCTaskDecorator` should resolve request parameters with this exact precedence:

1. **Live request scope**: use request-scoped `AweRequest` when it is available and active.
2. **Propagated ancestor snapshot**: if live request scope is unavailable, reuse the snapshot already stored in `PrototypeRequestBeanHolder` for the decorating thread.
3. **Empty snapshot**: if neither source exists, use `JsonNodeFactory.instance.objectNode()`.

### Proposed algorithm
At decoration time:

```java
Map<String, String> contextMap = MDC.getCopyOfContextMap();
RequestDataHolder requestDataHolder = requestDataHolderProvider.getObject();
ObjectNode requestSnapshot = resolveRequestSnapshot();
requestDataHolder.setRequestData(requestSnapshot);

return () -> {
  MDC.setContextMap(Optional.ofNullable(contextMap).orElse(Collections.emptyMap()));
  prototypeRequestBeanHolder.setPrototypeBean(requestDataHolder);
  try {
    runnable.run();
  } finally {
    MDC.clear();
    prototypeRequestBeanHolder.clear();
  }
};
```

Where `resolveRequestSnapshot()` behaves as:

```java
ObjectNode resolveRequestSnapshot() {
  ObjectNode liveSnapshot = resolveLiveRequestSnapshot();
  if (liveSnapshot != null) {
    return liveSnapshot;
  }

  ObjectNode ancestorSnapshot = prototypeRequestBeanHolder.getRequestDataSnapshot();
  if (ancestorSnapshot != null) {
    return ancestorSnapshot;
  }

  return JsonNodeFactory.instance.objectNode();
}
```

And `resolveLiveRequestSnapshot()` behaves as:

```java
ObjectNode resolveLiveRequestSnapshot() {
  try {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return null;
    }

    Object scopedRequest = requestAttributes.getAttribute(SCOPED_AWE_REQUEST_ATTRIBUTE,
        RequestAttributes.SCOPE_REQUEST);

    if (scopedRequest instanceof AweRequest aweRequest) {
      return aweRequest.getParametersSafe();
    }

    return null;
  } catch (RuntimeException exc) {
    return null;
  }
}
```

### Why this algorithm
- It matches the approved contract exactly.
- It treats inactive request scope as an unavailable source, not as an error.
- It aligns decoration-time capture with the fallback order already used later by `QueryUtil` during query execution.
- It preserves current MDC setup/cleanup semantics.

### Exception-handling rule
Only the **live request resolution path** should swallow exceptions, because the spec says request-scope absence/inactivity must not fail task decoration. Catching `RuntimeException` here is intentional because inactive-scope failures may surface as different framework runtime exceptions depending on proxy/request state.

---

## 2. How `PrototypeRequestBeanHolder` should participate in ancestor-snapshot reuse

### Decision
`PrototypeRequestBeanHolder` should be the authoritative source for the already-resolved async snapshot on the current decorating thread.

### Participation model
- When a decorated async task starts running, the wrapper already calls `prototypeRequestBeanHolder.setPrototypeBean(requestDataHolder)`.
- If that running async task schedules another async task, the child decoration happens on the parent async thread.
- The child decorator should read the parent snapshot from `PrototypeRequestBeanHolder` and copy it into a **new** `RequestDataHolder` for the child task.
- The same pattern repeats for grandchild hops.

### Important constraint
The child should **not** reuse the parent `RequestDataHolder` instance directly.

Instead, it should reuse the **snapshot payload** by copying the `ObjectNode`. That gives the same logical snapshot while avoiding mutable cross-task sharing.

### Recommended small holder enhancement
Add a convenience helper to `PrototypeRequestBeanHolder`:

```java
public ObjectNode getRequestDataSnapshot()
```

Behavior:
- Returns `null` when there is no current prototype bean or no request data.
- Returns `requestData.deepCopy()` when snapshot data exists.

### Why put this helper on the holder
- It centralizes the meaning of “current propagated async snapshot”.
- It hides the thread-local + `RequestDataHolder` unwrap logic from `AweMDCTaskDecorator`.
- It ensures copy semantics stay consistent anywhere ancestor reuse is needed.

---

## 3. Helper methods and small refactors needed

### Required refactor inside `AweMDCTaskDecorator`
Extract the inline request-resolution expression into small private helpers:

1. `resolveRequestSnapshot()`
2. `resolveLiveRequestSnapshot()`
3. optional `emptyRequestSnapshot()` helper, if it improves readability

### Small readability improvements
- Introduce a constant for the scoped request attribute name:
  - `private static final String SCOPED_AWE_REQUEST_ATTRIBUTE = "scopedTarget.aweRequest";`
- Replace direct cast with `instanceof AweRequest` to avoid accidental `ClassCastException` behavior.

### Deliberate non-changes
- No executor configuration changes.
- No `TaskConfig` or scheduler config changes.
- No public behavioral changes in downstream services.
- No redesign of `RequestDataHolder` bean scope.

### QueryUtil refactor status
A `QueryUtil` change is **not required** for this change to work. The current fallback behavior is already compatible. If the new holder helper is added, `QueryUtil` can stay untouched in this change to keep scope tight.

---

## 4. Avoiding snapshot leakage across pooled threads

### Risks to avoid
- Leaving old `RequestDataHolder` in the `ThreadLocal` after task completion.
- Leaving stale MDC values on a reused worker thread.
- Sharing the same mutable `ObjectNode` instance across parent/child tasks.

### Design rules
1. **Fresh holder per decoration**
   - Keep using `requestDataHolderProvider.getObject()` for every `decorate(...)` call.
   - Do not cache `RequestDataHolder` on the decorator instance.

2. **Copy the snapshot payload**
   - Live request scope already gives a copy via `AweRequest#getParametersSafe()`.
   - Ancestor reuse should also return a copy (`deepCopy()`).
   - This preserves the same snapshot content without shared mutable state.

3. **Install holder only for the runnable execution window**
   - `prototypeRequestBeanHolder.setPrototypeBean(...)` stays inside the returned wrapper.
   - `prototypeRequestBeanHolder.clear()` remains in `finally`.

4. **Clear MDC in the same `finally` block**
   - Existing `MDC.clear()` must remain unchanged.

5. **Do not fall back to previous worker state**
   - If a later task has no request scope and no ancestor snapshot, it must get a brand-new empty `ObjectNode`, not stale state from a prior task.

### Consequence
This keeps pooled-thread reuse safe: thread-local state exists only while a decorated runnable is executing.

---

## 5. Strict-TDD-oriented test strategy for `AweMDCTaskDecorator`

## Test file
Create:
- `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`

## Test fixture shape
- Use a **real** `PrototypeRequestBeanHolder`.
- Mock `ObjectProvider<RequestDataHolder>` with `thenAnswer` returning a fresh `RequestDataHolder` per call.
- Use mocked `RequestAttributes` and mocked `AweRequest` for live/inactive request-scope cases.
- Add cleanup in `@AfterEach`:
  - `RequestContextHolder.resetRequestAttributes()`
  - `prototypeRequestBeanHolder.clear()`
  - `MDC.clear()`

## TDD sequence

### RED 1
Add the first failing test for the bug:

`decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow`

Setup:
- `RequestContextHolder` contains mocked `RequestAttributes`.
- `getAttribute("scopedTarget.aweRequest", SCOPE_REQUEST)` throws `IllegalStateException` (or equivalent runtime exception).
- No ancestor snapshot is present in `PrototypeRequestBeanHolder`.

Assertions:
- `decorate(...)` does not throw.
- Running the decorated runnable sees an empty `requestData` in the holder.
- Holder and MDC are cleared after execution.

Why this is the first RED:
- Current code will fail during `decorate(...)` because the live-scope lookup is unguarded.

Command:
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow
```

### GREEN 1
Implement only the minimal live-scope guard + empty fallback needed to satisfy RED 1.

### TRIANGULATE / RED 2
Add a test proving ancestor reuse:

`decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot`

Setup:
- No active request scope, or request scope throws on access.
- Seed `PrototypeRequestBeanHolder` on the decorating thread with a `RequestDataHolder` containing a non-empty snapshot.

Assertions:
- Decorated runnable receives the same snapshot content.
- Snapshot is not empty.
- A new holder instance is used for the child task.

Why this matters:
- A simple “catch exception and return empty” implementation would still fail this scenario.

Command:
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot
```

### GREEN 2
Implement ancestor fallback using `PrototypeRequestBeanHolder`.

### TRIANGULATE / RED 3
Add the active request case:

`decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot`

Setup:
- Both live request scope and ancestor snapshot exist.

Assertions:
- Live request parameters win.
- Ancestor snapshot is ignored when live request is available.

Why this matters:
- It verifies the exact order, not just the presence of fallback.

Command:
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot
```

### TRIANGULATE / RED 4
Add the nested propagation scenario required by spec:

`decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild`

Recommended harness:
- Main thread decorates parent with live request scope.
- Parent wrapper runs on worker thread A and captures a child wrapper while request scope is gone.
- Child wrapper runs on worker thread B and captures a grandchild wrapper.
- Grandchild wrapper runs on worker thread C.

Assertions:
- Parent, child, and grandchild each observe equal snapshot content.
- Child and grandchild remain non-empty even though original request scope is no longer active.

Why this harness matters:
- It matches real async-hop timing more closely than nested same-thread execution.
- It proves propagation relies on the holder snapshot captured at each hop, not on residual request scope.

Command:
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild
```

### REFACTOR SAFETY TEST
Add an explicit pooled-thread cleanup regression test:

`decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns`

Setup:
- Run one decorated task with non-empty snapshot and MDC values.
- Then run a second decorated task on the same test thread with no request scope and no ancestor snapshot.

Assertions:
- Second run sees only its own context (empty snapshot / empty-or-current MDC).
- No state survives from the first run.

Command:
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns
```

### Final class run after refactor
```bash
mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest
```

## Evidence to record during apply/verify
For each TDD step, capture:
- the exact single-test command run,
- whether it failed first as expected,
- the minimal code change made,
- the passing rerun result.

---

## 6. Compatibility considerations with current `QueryUtil` fallback behavior

### Current behavior
`QueryUtil#getParametersFromRequest()` already resolves parameters in this order:
1. current live `AweRequest`
2. `PrototypeRequestBeanHolder`
3. empty `ObjectNode`

### Compatibility decision
Do **not** change `QueryUtil` behavior for this change.

### Why this is compatible
- The new decorator algorithm intentionally mirrors `QueryUtil`'s existing runtime fallback order.
- Downstream code still receives an `ObjectNode` exactly as before.
- Scheduled jobs and non-request async work still degrade to an empty parameter object.
- Existing code paths that already rely on `QueryUtil` fallback stay valid.

### Important nuance
The bug exists because nested async hops resolve their snapshot **before** `QueryUtil` is consulted. So `AweMDCTaskDecorator` must adopt the same fallback order at decoration time; otherwise descendants lose the ancestor snapshot and degrade to empty too early.

### Optional future cleanup
If reviewers want stronger internal consistency later, `QueryUtil` could eventually call the same holder helper (`getRequestDataSnapshot()`), but that is optional and not required for this change.

---

## Planned file changes for apply phase
1. `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   - implement ordered snapshot resolution helpers
   - guard inactive request-scope access
   - keep wrapper lifecycle unchanged

2. `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
   - add `getRequestDataSnapshot()` helper with `deepCopy()` semantics

3. `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
   - add strict-TDD coverage for live scope, inactive scope, ancestor fallback, nested propagation, and cleanup

## Rollout and risk
- No config changes.
- No public API break expected.
- Behavioral change is limited to formerly failing async-decoration paths.
- Main risk is accidental thread-local leakage; the dedicated cleanup test is intended to lock that down.
