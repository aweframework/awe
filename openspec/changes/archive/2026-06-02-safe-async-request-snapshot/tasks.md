# Tasks: safe-async-request-snapshot

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 220-360 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Implementation Tasks (STRICT TDD: RED → GREEN → TRIANGULATE → REFACTOR)

1. [x] **Test harness setup (RED-prep, no production code)**  
   **Targets:**
   - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java` (create)
   - Discovery target: `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   - Discovery target: `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
   **Actions:**
   - Build test fixture with real `PrototypeRequestBeanHolder`, mocked `ObjectProvider<RequestDataHolder>`, mocked `RequestAttributes`, mocked `AweRequest`.
   - Add `@AfterEach` cleanup: `RequestContextHolder.resetRequestAttributes()`, `prototypeRequestBeanHolder.clear()`, `MDC.clear()`.
   - Add evidence logging block in test comments or session notes for per-step command/fail/pass outcomes.
   **Verification:**
   - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest` (expected to fail until methods are added).

2. [x] **RED 1: failing test for inactive request scope without ancestor snapshot**  
   **Target test file:**
   - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
   **Add test:**
   - `decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow`
   **Required assertions:**
   - `decorate(...)` does not throw when `RequestAttributes#getAttribute("scopedTarget.aweRequest", SCOPE_REQUEST)` throws runtime exception.
   - Decorated runnable observes empty `ObjectNode` request snapshot.
   - `PrototypeRequestBeanHolder` and MDC are cleared after run.
   **Verification (RED evidence):**
   - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow` (must fail first on current implementation).

3. [x] **GREEN 1: minimal fix for missing/inactive request scope fallback to empty**  
   **Production target:**
   - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   **Actions:**
   - Guard live request attribute resolution against missing/inactive scope runtime exceptions.
   - Ensure fallback to `JsonNodeFactory.instance.objectNode()` when no live request data is resolvable.
   - Preserve existing wrapper lifecycle (`setPrototypeBean` before run, `MDC.clear`/`clear` in `finally`).
   **Verification (GREEN evidence):**
   - Re-run RED 1 method command; must pass.

4. [x] **TRIANGULATE RED 2: failing test for ancestor snapshot fallback**  
   **Target test file:**
   - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
   **Add test:**
   - `decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot`
   **Required assertions:**
   - When live scope unavailable and ancestor snapshot exists in holder, child receives ancestor snapshot content (non-empty).
   - Child async context uses a fresh `RequestDataHolder` instance (no holder instance reuse).
   **Verification (RED evidence):**
   - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot` (must fail before GREEN 2).

5. [x] **GREEN 2: implement ancestor fallback path (live → ancestor → empty)**  
   **Production targets:**
   - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   - `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
   **Actions:**
   - Implement explicit resolution order: live request snapshot, then propagated ancestor snapshot, then empty snapshot.
   - Add holder helper to expose propagated snapshot with copy semantics (deep copy) for descendant hops.
   **Verification (GREEN evidence):**
   - Re-run RED 2 method command; must pass.
   - Re-run RED 1 method command; must remain green.

6. [x] **TRIANGULATE RED 3: failing test for precedence (live overrides ancestor)**  
   **Target test file:**
   - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
   **Add test:**
   - `decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot`
   **Required assertions:**
   - If both sources are present, resolved snapshot equals live request parameters.
   - Ancestor snapshot is not selected when live request scope is active.
   **Verification (RED evidence):**
   - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot` (must fail before GREEN 3 if order incorrect).

7. [x] **GREEN 3: finalize ordered precedence behavior**  
   **Production target:**
   - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   **Actions:**
   - Refine helper methods so precedence is deterministic and readable (`resolveRequestSnapshot`, `resolveLiveRequestSnapshot`, optional empty helper).
   - Keep no-throw guarantee for unavailable/inactive request scope.
   **Verification (GREEN evidence):**
   - Re-run RED 3 method command; must pass.
   - Re-run RED 1 and RED 2 method commands; must stay green.

8. [x] **TRIANGULATE RED 4: failing nested propagation test (parent → child → grandchild)**  
   **Target test file:**
   - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
   **Add test:**
   - `decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild`
   **Required assertions:**
   - Parent, child, and grandchild hops observe equal snapshot content.
   - Child/grandchild retain non-empty snapshot even after original request scope is inactive.
   **Verification (RED evidence):**
   - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild` (must fail before GREEN 4 if propagation incomplete).

9. [x] **GREEN 4: satisfy nested propagation contract**  
   **Production targets:**
   - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
   - `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
   **Actions:**
   - Ensure descendant decoration can read ancestor propagated snapshot after original request ends.
   - Ensure snapshot copy semantics prevent mutable cross-hop leakage.
   **Verification (GREEN evidence):**
   - Re-run RED 4 method command; must pass.
   - Re-run RED 1–3 method commands; must stay green.

10. [x] **REFACTOR safety lock: pooled-thread cleanup regression test + cleanup refactor**  
    **Targets:**
    - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
    - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
    **Add test:**
    - `decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns`
    **Required assertions:**
    - Sequential decorated runs on reused thread do not leak snapshot or MDC state.
    - Second run with no sources resolves to empty snapshot.
    **Verification:**
    - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns`

11. [x] **Final verification and evidence capture**  
    **Targets:**
    - `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
    - `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
    - `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
    **Actions:**
    - Run full class test pass and record command outputs as TDD evidence table (RED fail → GREEN pass per step).
    - Run focused module sanity test if needed by reviewer.
    **Verification commands:**
    - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest`
    - `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest,QueryUtilTest` (optional focused regression if `QueryUtilTest` exists)

## Evidence Recording Template (for apply/verify phase)

For each RED/GREEN/TRIANGULATE/REFACTOR step, record:
- Command executed
- Expected result
- Actual result
- Minimal files changed to make GREEN
- Notes on rollback boundary
