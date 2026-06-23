# Verification Report: merge-datalists-by-key

**Change**: merge-datalists-by-key
**Source issue**: GitLab issue #681
**Version**: spec `datalist-merge` (locked)
**Mode**: Strict TDD
**Date**: 2026-06-23

## Environment Note (honest reproduction)

`mvn` is not on this workstation's PATH. Verification used the IntelliJ-bundled Maven at
`C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd` with
`JAVA_HOME=C:\Users\lukel\.jdks\semeru-17.0.4` (Semeru 17), exactly as the apply phase reported was
required in this environment. Both commands ran green under that JDK.

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 11 |
| Tasks complete | 11 |
| Tasks incomplete | 0 |

All 11 tasks across Phases 1-4 are checked complete in `tasks.md` and corroborated by the
apply-progress artifact (`sdd/merge-datalists-by-key/apply-progress`, Engram #12).

## Build & Tests Execution

**Build**: ✅ Passed (clean compile + test, `awe-model` module)

```text
mvn clean test -pl awe-framework/awe-model
Building AWE Model (V4.12.1-SNAPSHOT)
BUILD SUCCESS — Total time: 26.003 s
```

**Tests (targeted)**: ✅ 14 passed / 0 failed / 0 skipped

```text
mvn test -pl awe-framework/awe-model -Dtest=DataListUtilTest
Running com.almis.awe.model.util.data.DataListUtilTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Tests (full module, regression)**: ✅ 151 passed / 0 failed / 0 skipped

```text
mvn clean test -pl awe-framework/awe-model
DataListUtilTest      Tests run: 14
DateUtilTest          Tests run: 58
QueryUtilTest         Tests run: 19
StringUtilTest        Tests run: 1
TimeUtilTest          Tests run: 8
FileUtilTest          Tests run: 2
(+ remaining model tests)
Results: Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> Note: the full-module log contains `DateTimeParseException` stack traces. These are EXPECTED —
> `DateUtilTest` error-path cases (`web2TimeErrorParse_returnNullValue`, `sql2WebTimestamp_Error_*`,
> etc.) feed deliberately unparseable input and assert the fallback. The logger emits at ERROR but
> the tests pass. No real failures; the suite is green.

**Coverage**: ➖ Not available — no coverage plugin (JaCoCo) is wired into the `awe-model` test run.
Per Strict TDD rules, missing coverage tooling is informational, not a failure.

## Spec Compliance Matrix

Every locked scenario maps to a covering test that PASSED at runtime.

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Merge by composite key | Unique rows preserved in first-seen order | `DataListUtilTest > shouldKeepUniqueRowsInFirstSeenOrder` | ✅ COMPLIANT |
| Merge by composite key | Matching rows contribute different columns | `DataListUtilTest > shouldUnionColumnsForDuplicateKeys` | ✅ COMPLIANT |
| Duplicate keys enrich without overwriting | Later source fills a missing value | `DataListUtilTest > shouldFillMissingValueFromLaterSourceWithoutChangingRowPosition` | ✅ COMPLIANT |
| Duplicate keys enrich without overwriting | Later source conflicts with existing value | `DataListUtilTest > shouldNotOverwriteExistingNonEmptyValueWithLaterConflict` | ✅ COMPLIANT |
| Invalid keys fail explicitly | Missing key column fails the merge | `DataListUtilTest > shouldFailWhenRowIsMissingADeclaredKeyColumn` | ✅ COMPLIANT |
| Invalid keys fail explicitly | Null, blank, or whitespace-only key value fails the merge | `DataListUtilTest > shouldFailWhenDeclaredKeyValueIsNullOrEmpty` | ✅ COMPLIANT |
| Recompute metadata & isolate sources | Merge recomputes records from unique rows | `DataListUtilTest > shouldRecomputeMetadataFromUniqueRowsAndResetToSinglePage` | ✅ COMPLIANT |
| Recompute metadata & isolate sources | Source DataLists remain unchanged after merge | `DataListUtilTest > shouldKeepSourceDataListsUnchangedWhenMergedResultMutates` | ✅ COMPLIANT |

**Compliance summary**: 8/8 spec scenarios COMPLIANT.

**Beyond-spec hardening tests (6)**, all passing:
- `shouldAvoidCompositeKeyCollisionsWhenPartsConcatenateTheSameWay` — proves `(a,bc)` ≠ `(ab,c)` via `\u0000` separator.
- `shouldReturnEmptyDataListForNullOrEmptySources` — guard: null/empty/`new DataList()` sources → records=0.
- `shouldRejectNullOrEmptyKeyColumns` — guard: `null` and `List.of()` keyColumns → `IllegalArgumentException`.
- `shouldIsolateNestedObjectPayloadsWhenMergedResultMutates` — structural `Map` payloads are detached.
- `shouldIsolateNestedJsonPayloadsWhenMergedResultMutates` — structural `JsonNode` payloads are detached.
- `shouldPreserveGenericObjectPayloadsWithoutNormalizing` — generic object payloads keep their runtime type and reference.

## Correctness (Static Evidence)

| Requirement | Status | Notes |
|------------|--------|-------|
| Composite key merge, first-seen order, column union | ✅ Implemented | `mergeByKey` uses `LinkedHashMap` for order, `LinkedHashSet` `mergedColumns` for union (DataListUtil.java:405-430) |
| First-wins + fill-missing, no overwrite | ✅ Implemented | `computeIfAbsent` seeds first row; `mergeRowInto` copies only when target cell `null`/`isEmpty()` (617-633) |
| Invalid key fails fast with column name | ✅ Implemented | `getValidatedKeyPart` throws `IllegalArgumentException("Invalid key column: " + col)` on `null`/`isEmpty()` (605-615) |
| Null/empty key detection covers blank + NULL-type | ✅ Implemented | `CellData.isEmpty()` → `getStringValue().isEmpty()`; `getStringValue()` trims (CellData.java:127-128,259-261). Verified `"   "` is rejected |
| Source isolation | ✅ Implemented | Rows and `CellData` instances are copied; structural object payloads (`JsonNode`, `Map`, `List`, `Date`) are detached; generic object payloads preserve their original runtime type/reference |
| Metadata recompute | ✅ Implemented | `records = rows.size()`, `page = 1`, `total = 1` (654-657) |

## Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| New `mergeByKey` in `DataListUtil`; `DataListBuilder#doMerge` untouched | ✅ Yes | `doMerge` still present at DataListBuilder.java:492,692; no diff to that file |
| first-wins + fill-missing precedence | ✅ Yes | Matches design row exactly |
| Invalid key → `IllegalArgumentException` with offending column | ✅ Yes | Message contains the column name |
| Key validity = absent OR `isEmpty()` (trimmed, covers NULL) | ✅ Yes | Implemented as designed |
| Composite key `\u0000` separator | ✅ Yes | `KEY_SEPARATOR = "\u0000"` |
| Column union via all-rows `LinkedHashSet` (not `getColumnList`) | ✅ Yes | `mergedColumns.addAll(row.keySet())` per row |
| Copied cells, new maps, structural payload detachment | ✅ Yes | Generic object payloads are intentionally not deep-copied; callers must handle custom deep copy explicitly |
| `records=unique`, `page/total=1` | ✅ Yes | As designed |
| `DataList` convenience wrapper out of scope | ✅ Yes | Not added; surface stays minimal |

## TDD Compliance

| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | ✅ | Found in apply-progress (Engram #12) with full TDD Cycle Evidence table |
| All tasks have tests | ✅ | All implementation tasks covered by `DataListUtilTest` |
| RED confirmed (tests exist) | ✅ | `DataListUtilTest.java` exists on disk (235 lines); apply reports compile-fail RED before `mergeByKey` existed |
| GREEN confirmed (tests pass) | ✅ | 14/14 pass on independent re-execution (not trusting the report) |
| Triangulation adequate | ✅ | 14 cases with distinct expected values (order B/A/C, union Alice/Sales/ES, conflict-keep-first, fill-from-later, composite first/second, metadata, isolation, structural payload detachment, generic object preservation) |
| Safety Net for modified files | ✅ | `DataListUtil.java` modified; full module 151/151 green proves no regression to `DataList`/`CellData`/siblings |

**TDD Compliance**: 6/6 checks passed.

## Test Layer Distribution

| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit | 14 | 1 | JUnit 5 (Jupiter) + Surefire 3.5.4 |
| Integration | 0 | 0 | not applicable (pure static util) |
| E2E | 0 | 0 | not applicable |
| **Total** | **14** | **1** | |

A pure static merge utility is correctly tested at the unit layer. No integration/E2E layer is
warranted, so the unit-only distribution is appropriate (no SUGGESTION raised).

## Changed File Coverage

| File | Line % | Branch % | Uncovered Lines | Rating |
|------|--------|----------|-----------------|--------|
| `DataListUtil.java` (mergeByKey + helpers) | ➖ | ➖ | — | ➖ |
| `DataListUtilTest.java` | ➖ | ➖ | — | ➖ |

Coverage analysis skipped — no coverage tool (JaCoCo) detected in the `awe-model` build. Not a
failure. Behavioral coverage is established via the 8/8 scenario→test mapping above; every code path
(seed, fill-missing, no-overwrite, invalid-missing, invalid-null/blank/whitespace, composite encoding,
empty-source skip, keyColumns guard, metadata rebuild, structural payload detachment, and generic object
reference preservation) is exercised by a passing test.

## Assertion Quality

Audited all 14 tests in `DataListUtilTest.java` (Strict TDD Step 5f, mandatory):

- No tautologies (`assertTrue(true)` / `assertEquals(1,1)`) — none present.
- No assertions without a production call — every test invokes `DataListUtil.mergeByKey(...)`.
- No ghost loops — no assertions iterate a possibly-empty collection.
- Empty-collection assertions are legitimate: `shouldReturnEmptyDataListForNullOrEmptySources`
  asserts empty as the CORRECT guard outcome and has companion non-empty tests on the same API.
- No type-only assertions — every assertion checks a concrete value, order, count, or a thrown
  exception WITH message-content verification (`error.getMessage().contains("id")`).
- No smoke-tests — no "constructs without crash" filler.
- No implementation-detail coupling — assertions target behavior (cell values, row order,
  `records`/`page`/`total`, exceptions), never private helpers or internal fields.
- Zero mocks — pure unit tests; mock/assertion ratio is 0 (ideal).
- Source-isolation assertion is genuine: `@Data` gives `DataList`/`CellData` value-based `equals`,
  and `snapshot()` uses the deep-copy constructor `new DataList(other)` BEFORE the merged result is
  mutated, so `assertEquals(snapshot, source)` truly detects source mutation.

**Assertion quality**: ✅ All assertions verify real behavior. 0 CRITICAL, 0 WARNING.

## Quality Metrics

**Linter**: ➖ Not available — no Checkstyle/SpotBugs gate observed in the `awe-model` test run.
**Type Checker**: ✅ No errors — `javac` (compiler 3.11.0) compiled cleanly under Semeru 17.

## Issues Found

**CRITICAL**: None.

**WARNING**: None.

**SUGGESTION**:
- Test assertion style: `DataListUtilTest` uses JUnit 5 Jupiter assertions (`assertAll`,
  `assertEquals`, `assertThrows`). AGENTS.md states the Java test convention is AssertJ
  (`assertThat(result).isNotNull()`), and both `design.md` (File Changes table) and apply-progress
  describe the file as "JUnit 5 + AssertJ" — but no AssertJ (`assertThat`) calls are present. This
  is a convention/documentation mismatch only; the assertions are correct, behavioral, and all pass.
  Optional follow-up: either restyle to AssertJ to match the house convention, or correct the
  design/apply wording to say "JUnit 5 Jupiter assertions". Non-blocking.

## Verdict

**PASS**

All 11 tasks complete; 8/8 locked spec scenarios are COMPLIANT with passing covering tests
(re-executed independently, not trusted from the report); full design coherence; full-module
regression green at 151/151; TDD compliance 6/6; zero trivial assertions. The only finding is a
non-blocking test-style SUGGESTION (Jupiter vs AssertJ). Ready for `sdd-archive`.
