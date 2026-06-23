# Design: Merge multiple datalists by key in DataListUtil

> Source: GitLab issue #681 — references `proposal.md` and the locked
> `specs/datalist-merge/spec.md`.

## Technical Approach

Add a pure, static `mergeByKey` to `DataListUtil` (a `final` util class of `public static`
methods). It iterates source `DataList`s in argument order, builds a composite key per row from the
declared key columns, and accumulates into a `LinkedHashMap<String, Map<String, CellData>>` to
preserve first-seen order. **Before accumulating any row, every declared key column is validated;
if any row is missing a key column or carries a null/empty key value, the merge fails immediately
with an explicit invalid-key error** (spec: "Invalid keys fail the merge explicitly"). Valid rows
seed the accumulator first-wins and later rows only fill columns that are still absent/empty. All
columns are unioned across every row, cells are copied, structural payloads are detached where safe,
then a fresh `DataList` is rebuilt with `records` recomputed from the unique row set.
`DataListBuilder#doMerge` is never touched.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|-----------------------|-----------|
| Location | New `mergeByKey` in `DataListUtil` | New class; change `DataListBuilder#doMerge` | Matches existing static-util convention; proposal requires `doMerge` byte-compatible |
| Duplicate-key precedence | **first-wins + fill-missing**: first source seeds the row; later sources only populate columns absent/empty in the accumulated row, never overwriting an existing non-empty cell | last-wins; strict first-wins | Locked by spec "Duplicate keys enrich without overwriting earlier values"; deterministic and testable |
| Invalid key handling | **Fail the merge** if ANY declared key column is missing from a row OR resolves to a null/empty value, for ANY source row. Throw `IllegalArgumentException` with the offending key column name | skip the row; treat as a distinct key; non-throwing | Locked by spec "Invalid keys fail the merge explicitly" — invalid keys MUST NOT be skipped, merged, or treated as implicit unique keys |
| Key validity test | A key column is invalid for a row when `row.get(col) == null` (column absent) OR `cell.isEmpty()` (trimmed `getStringValue()` empty, which also covers `CellDataType.NULL`) | `containsKey`-only; object-identity null only | `CellData.getStringValue()` never returns null and trims; a NULL-type, blank, or whitespace-only cell is exactly the invalid key value the spec rejects |
| Composite key encoding | Join validated key-column string values with a non-data separator (`\u0000`) | concatenation; hashing | Prevents `"a"+"bc"` colliding with `"ab"+"c"`; stable map key (only reached after validation) |
| Column union + order | Union built by iterating **every row's keyset across all sources** into an insertion-ordered `LinkedHashSet`; emitted rows expose that union | seed only from `getColumnList` (first row); intersection | `DataListUtil.getColumnList` reads only the first row's keyset, so it cannot represent the true union; the union must scan all contributing rows. Preserves first-seen order, lossless |
| Copy semantics | Every emitted cell is a fresh `new CellData(sourceCell)` and rows are new maps; structural payloads (`JsonNode`, `Map`, `List`, `Date`) are detached; generic unknown `OBJECT` payloads keep their original runtime type/reference | shallow row/cell reference reuse; generic object normalization | Sources must stay unmutated while avoiding unsafe assumptions about arbitrary object deep-copy semantics |
| Metadata | `records = uniqueRows.size()`; `page = 1`, `total = 1` on result | sum records (`doMerge` behavior) | Spec: merged output is a single in-memory page; `records` equals unique merged rows |

## Data Flow

```
mergeByKey(keyColumns, src1, src2, ... srcN)
        │  validate keyColumns non-empty (else IllegalArgumentException)
        │  iterate sources in order, then rows in order
        ▼
  for each row:
        │
        ├─ any key col absent OR cell null/empty ──→ FAIL
        │       (IllegalArgumentException: invalid key column)
        │
        ├─ build compositeKey(keyColumns, row)   (all parts valid)
        │
        ├─ key absent in map ──→ put copied row (first-wins seed)
        │
        └─ key present ──→ fill-missing only: copy a column from this
                            row solely when the accumulated row has no
                            value (absent/empty) for it; never overwrite
        ▼
  LinkedHashMap<key, row>  (first-seen order preserved)
        ▼
  build DataList: rows = map.values() (copied),
                  columns = union of all contributing rows' keys,
                  records = size, page = 1, total = 1
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `awe-framework/awe-model/.../util/data/DataListUtil.java` | Modify | Add `mergeByKey(List<String> keyColumns, DataList... sources)` + private helpers `buildCompositeKey` (validates + encodes) and `mergeRowInto` (fill-missing) |
| `awe-framework/awe-model/src/test/java/com/almis/awe/model/util/data/DataListUtilTest.java` | Create | New JUnit 5 + AssertJ test class (no existing `DataListUtilTest`) |
| `awe-framework/awe-controller/.../builder/DataListBuilder.java` | Unchanged | `doMerge` append behavior stays byte-compatible |

`DataList.java` convenience wrapper is **out of scope** for this design (proposal marks it optional;
keep surface minimal — revisit only if a caller needs it).

## Interfaces / Contracts

```java
/**
 * Merge several datalists into one, unifying rows by a composite key.
 * First source wins per key; later sources fill only columns the accumulated
 * row still lacks. Existing non-empty cells are never overwritten. The emitted
 * rows expose the union of all contributing rows' columns. Sources are not
 * mutated. Emitted cells are copied; structural object payloads that can be
 * safely detached by this utility (JsonNode, Map, List, Date) are copied,
 * while arbitrary object payloads keep their original runtime type and reference.
 *
 * The merge fails fast if any source row is missing a declared key column or
 * carries a null/empty value for one (no row is skipped or treated as a
 * distinct key on invalid input).
 *
 * @param keyColumns ordered, non-empty key column names
 * @param sources    datalists to merge, in precedence order
 * @return new DataList with one copied row per key; records = unique rows,
 *         page = 1, total = 1
 * @throws IllegalArgumentException if keyColumns is null/empty, or if any source
 *         row is missing a declared key column or has a null/empty key value
 */
public static DataList mergeByKey(List<String> keyColumns, DataList... sources)
```

Edge contracts: `null`/empty `sources` → empty `DataList` (records = 0). `null`/empty `keyColumns`
→ `IllegalArgumentException` (a keyless merge is undefined). An absent key column or a null/empty
key cell in ANY row → `IllegalArgumentException` (invalid-key failure, per spec).

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | First-wins on duplicate key, no overwrite | Two sources, same key, different non-key values → assert first value kept |
| Unit | Fill-missing populates sparse columns from later source | First row missing colB; second supplies it → assert filled, position kept |
| Unit | Missing key column fails the merge | Row lacking a declared key column → assert `IllegalArgumentException` |
| Unit | Null/empty key value fails the merge | Row with NULL-type, blank, or whitespace-only key cell → assert `IllegalArgumentException` |
| Unit | Column union + first-seen order | Sources with disjoint columns across rows → assert union covers every contributing column, order preserved |
| Unit | Composite (multi-column) key, no collision | Keys `(a,bc)` vs `(ab,c)` → assert distinct rows |
| Unit | `records` = unique rows; `page`/`total` = 1 | Assert recomputed metadata |
| Unit | Sources unmutated | Snapshot source rows/records before; mutate merged result; assert sources unchanged |
| Unit | Guards | `null`/empty sources → empty DataList; `null`/empty keyColumns → `IllegalArgumentException` |

Run: `mvn test -pl awe-framework/awe-model -Dtest=DataListUtilTest`.

## Migration / Rollout

No migration required. Additive, self-contained in `awe-model`. No callers, no persisted format,
no `DataListBuilder` change. Rollback = delete `mergeByKey` and its test.

## Open Questions

None. Precedence (first-wins + fill-missing), invalid-key handling (fail explicitly), and column
union are all locked by `specs/datalist-merge/spec.md`.
