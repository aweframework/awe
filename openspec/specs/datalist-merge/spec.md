# datalist-merge Specification

## Purpose

Define a reusable keyed merge for `DataList` instances that unifies duplicate business rows without changing existing append-merge behavior elsewhere.

## Requirements

### Requirement: Merge DataLists by composite key

The system MUST accept one or more source `DataList` instances and one or more key columns and MUST return one merged `DataList` with at most one row per distinct composite key. The merged result MUST preserve the order in which each distinct key is first encountered across the source sequence. The merged rows MUST expose the union of columns contributed by matching rows.

#### Scenario: Unique rows are preserved in first-seen order

- GIVEN two source `DataList` instances with distinct key values
- WHEN they are merged by the declared key columns
- THEN the result contains one row for each distinct key
- AND the row order matches the first time each key appeared

#### Scenario: Matching rows contribute different columns

- GIVEN two rows with the same key and non-overlapping non-key columns
- WHEN the rows are merged
- THEN the result contains one row for that key
- AND the merged row includes the union of both rows' columns

### Requirement: Duplicate keys enrich without overwriting earlier values

When multiple rows share the same composite key, the system MUST keep the first encountered non-key cell value for each column. The system MAY populate a column from a later row only when the merged row has no value for that column. The system MUST NOT overwrite an existing non-null cell with a later conflicting value.

#### Scenario: Later source fills a missing value

- GIVEN the first row for a key lacks a non-key value and a later row provides it
- WHEN the rows are merged
- THEN the merged row keeps the first row position
- AND the missing value is populated from the later row

#### Scenario: Later source conflicts with an existing value

- GIVEN the first row for a key already contains a non-null non-key value
- WHEN a later row for the same key provides a different value for that column
- THEN the merged row keeps the earlier value

### Requirement: Invalid keys fail the merge explicitly

The system MUST fail the merge if any source row is missing a declared key column or contains a null, blank, or whitespace-only key value for any declared key column. The system MUST NOT treat invalid-key rows as mergeable rows, skipped rows, or implicit unique keys.

#### Scenario: Missing key column fails the merge

- GIVEN a source row that does not contain one declared key column
- WHEN the merge is requested
- THEN the merge fails with an explicit invalid-key error

#### Scenario: Null, blank, or whitespace-only key value fails the merge

- GIVEN a source row whose declared key column contains a null, blank, or whitespace-only value
- WHEN the merge is requested
- THEN the merge fails with the same invalid-key behavior

### Requirement: Merged results recompute metadata and isolate sources

On successful merge, the system MUST return rows and cells that are isolated copies of source data. Structural object payloads (`JsonNode`, `Map`, `List`, `Date`) MAY be safely detached by the utility. Generic unknown `OBJECT` payloads MUST preserve their original runtime type and reference; callers that require custom deep-copy behavior MUST handle it explicitly. The system MUST leave every source `DataList` unmodified. The merged result MUST report `records` equal to the number of unique merged rows and SHALL report the merged output as a single in-memory page.

#### Scenario: Merge recomputes records from unique rows

- GIVEN multiple source rows that collapse into fewer unique keys
- WHEN the merge succeeds
- THEN `records` equals the number of unique merged rows

#### Scenario: Source DataLists remain unchanged after merge

- GIVEN source `DataList` instances used in a successful merge
- WHEN the caller mutates the merged result
- THEN the source rows and cells remain unchanged
