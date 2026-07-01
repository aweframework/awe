# Proposal: Merge multiple datalists by key in DataListUtil

> Source: GitLab issue #681

## Intent

AWE can read the same business entity from several sources (multi-database queries, restriction lookups in `MenuService`), but the only built-in merge — `DataListBuilder#doMerge` — appends every row and sums `records`, so duplicate business keys survive as duplicate rows. Callers have no reusable way to unify rows by a key column, forcing ad-hoc, error-prone manual merges. This adds a keyed merge so unified entity views are a one-call, testable operation.

## Scope

### In Scope
- New `DataListUtil.mergeByKey(...)` static API: N source datalists + one or more key columns → one merged `DataList`.
- Deterministic semantics for duplicate keys, null/missing keys, column union, and recomputed pagination metadata.
- Copy merged rows/cells and detach structural payloads (`JsonNode`, `Map`, `List`, `Date`) so sources stay isolated without assuming generic object deep-copy semantics.
- Unit tests in `awe-model` covering precedence, null keys, mixed columns, and metadata.

### Out of Scope
- Changing `DataListBuilder#doMerge` append behavior (must stay byte-compatible).
- Migrating existing callers (`MenuService`, `LogService`) — follow-up adoption.
- Cross-source alias normalization / column renaming (callers pre-align via `copyColumn`).
- Frontend, XML descriptor, or query-engine changes.

## Capabilities

### New Capabilities
- `datalist-merge`: keyed unification of multiple `DataList` instances, including precedence, null-key handling, column union, and pagination recomputation.

### Modified Capabilities
- None.

## Approach

Add a pure static `mergeByKey` to `DataListUtil` (Approach 1, exploration's recommendation). Iterate sources in argument order, build a composite key from the key columns per row, and accumulate into a key→row map preserving first-seen order. Apply the chosen precedence per non-key column, union all columns across sources, copy cells, then recompute `records` from the unique row set. Keep `DataListBuilder` untouched.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `awe-model/.../util/data/DataListUtil.java` | Modified | New `mergeByKey` API |
| `awe-model/.../dto/DataList.java` | Modified (optional) | Optional convenience wrapper |
| `awe-model` test sources | New | `DataListUtil` merge unit tests |
| `awe-controller/.../builder/DataListBuilder.java` | Unchanged | Append merge stays intact |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Undefined duplicate-key precedence | High | Lock rule in spec (decision below) |
| Null / missing / mixed-alias keys silently merge wrong rows | Med | Define explicit null-key rule + tests |
| Pagination metadata wrong after merge | Med | Recompute `records` from unique set |
| Shared mutable row maps leak | Med | Copy rows/cells and detach structural payloads on merge |

## Rollback Plan

Additive only. Revert by removing `mergeByKey` (and optional wrapper) and its tests; no callers depend on it yet, no persisted format or `DataListBuilder` behavior changes.

## Open Decisions (downstream phases MUST resolve)

1. **Duplicate-key precedence**: first-wins, last-wins, or fill-missing-only.
2. **Null / missing key columns**: skip row, error, or treat as distinct key.
3. **Column union + ordering**: union all columns, preserving first-seen row order.

## Dependencies

- None. Self-contained within `awe-model`.

## Success Criteria

- [ ] `DataListUtil.mergeByKey(...)` returns one unique row per key per locked precedence.
- [ ] Null/missing keys behave per locked rule; merged `records` matches unique rows.
- [ ] Source datalists are unmutated after merge.
- [ ] `DataListBuilder#doMerge` behavior and its existing tests unchanged.
- [ ] New `awe-model` unit tests pass via `mvn test -pl awe-framework/awe-model`.
