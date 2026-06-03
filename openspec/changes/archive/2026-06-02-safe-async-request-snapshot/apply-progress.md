# Apply Progress — safe-async-request-snapshot

## Completed tasks
- ✅ Created `AweMDCTaskDecoratorTest` with fixture setup and cleanup hooks.
- ✅ Implemented no-throw behavior for inactive/missing request scope in `AweMDCTaskDecorator`.
- ✅ Added ancestor snapshot fallback support via `PrototypeRequestBeanHolder` helper.
- ✅ Implemented ordered fallback at decoration time: **live request -> ancestor snapshot -> empty snapshot**.
- ✅ Added tests for precedence, nested propagation (parent -> child -> grandchild), and pooled-thread cleanup safety.
- ✅ Updated `tasks.md` checklist to completed.

## Files changed
- `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`
- `awe-framework/awe-model/src/main/java/com/almis/awe/model/component/PrototypeRequestBeanHolder.java`
- `awe-framework/awe-controller/src/test/java/com/almis/awe/component/AweMDCTaskDecoratorTest.java`
- `openspec/changes/safe-async-request-snapshot/tasks.md`
- `openspec/changes/safe-async-request-snapshot/apply-progress.md`

## TDD Cycle Evidence

| Step | Phase | Command | Expected | Actual |
|---|---|---|---|---|
| 1 | RED 1 | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow` | Fails on current implementation due inactive request exception | ❌ Failed with `IllegalStateException` from `AweMDCTaskDecorator.decorate` |
| 2 | GREEN 1 | same command as step 1 | Pass after guarding live request lookup and empty fallback | ✅ Passed |
| 3 | RED 2 | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot` | Fails before ancestor fallback implementation | ❌ Failed (`NullPointerException` because snapshot was empty instead of ancestor payload) |
| 4 | GREEN 2 | same command as step 3 | Pass after ancestor snapshot fallback implementation | ✅ Passed |
| 5 | Regression check | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow` | Still passes | ✅ Passed |
| 6 | TRIANGULATE (precedence) | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot` | Validate live request wins over ancestor snapshot | ✅ Passed (behavior already satisfied by ordered resolver) |
| 7 | TRIANGULATE (nested) | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild` | Validate parent->child->grandchild propagation after request ends | ✅ Passed |
| 8 | REFACTOR safety | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns` | Validate no leakage across sequential runs | ✅ Passed |
| 9 | Final verify | `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest` | Full new test class passes | ✅ Passed (5/5) |

## Test commands run
- `mvn test -pl awe-framework/awe-controller -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow`
- `mvn test -pl awe-framework/awe-controller -am -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest#decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns`
- `mvn test -pl awe-framework/awe-controller -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AweMDCTaskDecoratorTest`

## Deviations from design/tasks
- Added `-am` to run reactor dependencies required by local module tests.
- Added `-Dsurefire.failIfNoSpecifiedTests=false` because the reactor includes upstream modules where the targeted test pattern does not exist.
- TRIANGULATE precedence and nested propagation tests passed on first run after GREEN 2 because the implemented resolver already satisfied those scenarios.

## Remaining tasks
- None for apply scope.

## Workload / PR boundary
- Delivery strategy: single PR.
- Scope stayed within the planned change boundary (decorator + holder + tests).
