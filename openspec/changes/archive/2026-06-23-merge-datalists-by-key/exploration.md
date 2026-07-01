## Exploration: merge-datalists-by-key

### Current State
`DataList` is a lightweight DTO that stores pagination metadata plus mutable row maps. `DataListUtil` provides row/column helpers, filtering, sorting, distinct, and column-copy operations, but it has no keyed merge API. The only built-in merge behavior today is `DataListBuilder#doMerge`, which appends all rows from every source `DataList` and sums `records`, so duplicate business keys are preserved instead of unified. Existing code already mixes results manually in places like `MenuService`, either through `DataListBuilder.addDataList(...)` or direct `rows().addAll(...)`.

### Affected Areas
- `awe-framework/awe-model/src/main/java/com/almis/awe/model/util/data/DataListUtil.java` — best fit for a reusable keyed merge API in the lowest shared module.
- `awe-framework/awe-model/src/main/java/com/almis/awe/model/dto/DataList.java` — optional convenience wrapper if the public API should also expose merge behavior on the DTO.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/builder/DataListBuilder.java` — current merge path is append-only and must stay backward-compatible.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/service/MenuService.java` — current caller already merges restriction datalists and is a likely early adopter or regression seam.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/connector/query/AbstractQueryConnector.java` — shows existing column-alignment patterns (`copyColumn`) that matter when sources do not expose identical aliases.
- `awe-framework/awe-controller/src/test/java/com/almis/awe/service/data/builder/DataListBuilderTest.java` — current unit coverage already verifies append-merge behavior and is the nearest existing seam for merge-related tests.
- `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml` — `getScreenRestrictions` / `getOptionRestrictionFromDatabase` illustrate real cross-source data that can require key-based unification.

### Approaches
1. **Add `DataListUtil.mergeByKey(...)`** — introduce a static utility that accepts multiple datalists plus one or more key columns and returns a new merged datalist.
   - Pros: reusable across modules, lives in `awe-model`, preserves existing builder behavior, easiest to unit-test without Spring wiring.
   - Cons: needs explicit conflict rules for non-key columns and duplicate keys inside a single source.
   - Effort: Medium

2. **Extend `DataListBuilder` with keyed merge mode** — keep merge orchestration in the builder and add optional key-aware behavior alongside `addDataList(...)`.
   - Pros: fluent for existing controller/service callers, can combine keyed merge with later builder transforms.
   - Cons: wrong abstraction layer for a generic datalist operation, less accessible to `awe-model` consumers, higher risk of accidental behavior changes in current append merge.
   - Effort: Medium/High

### Recommendation
Implement a new `DataListUtil` API and keep `DataListBuilder` append semantics unchanged. That keeps the change additive, reusable for multi-database query results, and compatible with current callers like `MenuService` and `LogService` that rely on simple concatenation.

### Risks
- Merge precedence is not defined yet: when the same key appears in multiple datalists, the code must specify whether first value wins, last value wins, or only missing columns are filled.
- Key quality is a real constraint: null keys, missing key columns, mixed aliases, or casing differences can silently produce duplicate or overwritten rows.
- Pagination metadata cannot be blindly summed for keyed merges; `records`, `page`, and `total` likely need recomputation from the merged unique row set.
- Source rows are mutable maps; merge logic should copy rows/cells instead of reusing references, otherwise later mutations can leak across datalists.

### Ready for Proposal
Yes — but the orchestrator should ask the user to lock down three semantics in the proposal/spec: duplicate-key precedence, treatment of null/missing keys, and whether merged output should union all columns while preserving source row order.
