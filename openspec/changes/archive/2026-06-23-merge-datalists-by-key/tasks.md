# Tasks: Merge DataLists by Key

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 260-340 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | single-pr |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add keyed merge API with tests | PR 1 | Single PR; tests and Javadoc ship together |

## Phase 1: RED / Test Scaffold

- [x] 1.1 Create `awe-framework/awe-model/src/test/java/com/almis/awe/model/util/data/DataListUtilTest.java` with row-builder helpers and source snapshots for merge scenarios.
- [x] 1.2 Add failing tests for spec scenarios: unique rows keep first-seen order, duplicate keys union columns, and composite keys avoid `(a,bc)` / `(ab,c)` collisions.
- [x] 1.3 Add failing tests for fill-missing vs no-overwrite, invalid missing/null-empty keys, null-or-empty sources, and null-or-empty `keyColumns` guards.

## Phase 2: GREEN / Core Merge API

- [x] 2.1 Modify `awe-framework/awe-model/src/main/java/com/almis/awe/model/util/data/DataListUtil.java` to add `mergeByKey(List<String> keyColumns, DataList... sources)` with null-or-empty input guards.
- [x] 2.2 Add private key helpers in `DataListUtil.java` that validate every declared key column, reject null/empty `CellData`, and encode composite keys with `\u0000` separators.
- [x] 2.3 Add accumulator helpers in `DataListUtil.java` using `LinkedHashMap` and `LinkedHashSet` so first-seen keys keep order and later rows fill only absent/empty non-key cells.
- [x] 2.4 Rebuild the merged `DataList` in `DataListUtil.java` with copied rows/cells, detached structural payloads, unioned columns, `records = uniqueRows`, and `page/total = 1`.

## Phase 3: REFACTOR / Compatibility

- [x] 3.1 Refactor `DataListUtil.java` Javadoc, helper names, and imports so the new API is readable while keeping `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/builder/DataListBuilder.java` unchanged.
- [x] 3.2 Extend `DataListUtilTest.java` with source-isolation assertions by mutating merged rows and proving original `DataList` rows and metadata stay unchanged.

## Phase 4: Verification

- [x] 4.1 Run `mvn test -pl awe-framework/awe-model -Dtest=DataListUtilTest` to prove every locked spec scenario passes in the targeted module.
- [x] 4.2 Run `mvn test -pl awe-framework/awe-model` to catch regressions around `DataList`, `CellData`, and existing utility behavior before handoff.
