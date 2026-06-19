# Design: Validate Multiple Maintain Requires a List Variable

## Technical Approach

Add an early connector-level guard in `SQLMaintainConnector.launch` for `multiple="true"` maintains. After `queryUtil.addToVariableMap(...)` has normalized descriptor variables into `parameterMap`, validate that at least one declared, non-audit variable resolves to a `QueryParameter` marked as a list. If not, throw a controlled `AWException` before `launchMultipleMaintain`, `SQLMaintainBuilder.build()`, or SQL execution. Empty arrays pass because the check validates list shape, not item count.

This implements the `maintain-execution` requirement without changing descriptors, REST/UI controllers, locale files, or single-maintain behavior.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|----------|--------|--------------------------|-----------|
| Validation location | `SQLMaintainConnector.launch`, immediately after variable map preparation and before multiple dispatch | Validate in `hasNext`; validate inside `SQLMaintainBuilder`; fallback to single maintain | The connector owns multiple dispatch, can fail before SQL build/execution, and avoids making iteration helpers responsible for configuration errors. |
| Valid-list definition | Declared variable is non-audit, present in `parameterMap`, and `QueryParameter.isList()` is true | Require non-empty array; inspect raw JSON only; count audit variables | Empty lists are valid no-op requests; `parameterMap` is the normalized source already used by execution; audit-only lists must not drive business maintain iteration. |
| Error path | Throw `AWException(getLocale("ERROR_TITLE_DURING_MAINTAIN"), getLocale("ERROR_MESSAGE_DURING_MAINTAIN"), cause)` with technical cause text containing maintain id/type and missing list detail | New locale keys; `AWEQueryException`; silent OK | Existing generic maintain messages keep UI/API parity while the cause preserves diagnostics in logs. This is not a SQL query failure, so `AWEQueryException` is not the primary type. |

## Data Flow

```text
REST/UI request
  -> MaintainService.launchMaintain(...)
  -> SQLMaintainConnector.launch(...)
  -> queryUtil.addToVariableMap(parameterMap, query, parameters)
  -> validateMultipleMaintainList(query, parameterMap)
       -> fail with controlled AWException when no valid non-audit list exists
       -> otherwise launchMultipleMaintain(...)
  -> SQLMaintainBuilder / QueryDSL execution
```

On validation failure, `MaintainService` keeps the existing rollback/rethrow path for `AWException`, so REST/API and UI observe the same generic maintain title/message.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnector.java` | Modify | Add private validation helpers and invoke them for `multiple="true"` before `launchMultipleMaintain`. Make the check null-safe for missing optional variables. |
| `awe-framework/awe-controller/src/test/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnectorTest.java` | Modify | Add regression tests for missing list failure, empty list acceptance, valid list execution, and no SQL build/execution on failure. |
| `awe-framework/awe-generic-screens/src/main/resources/application/awe/locale/Locale-*.xml` | Unchanged | Reuse `ERROR_TITLE_DURING_MAINTAIN` and `ERROR_MESSAGE_DURING_MAINTAIN`. |

## Interfaces / Contracts

No public API or XML contract changes. Internal helper contract:

```java
private void validateMultipleMaintainList(MaintainQuery query,
                                          Map<String, QueryParameter> parameterMap) throws AWException
```

The helper must return normally when `query.getMultiple()` is not `true`, when a non-audit list variable exists, or when that list is empty. It must throw only when no valid non-audit list variable exists.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `multiple="true"` with only scalar/no variables fails early | `SQLMaintainConnectorTest`: assert `AWException` title/message use generic maintain locales and verify `SQLMaintainBuilder` is not requested. |
| Unit | Empty non-audit list passes validation | Configure a declared variable with an empty `ArrayNode`; assert `ServiceData` is OK and no SQL execution occurs because `hasNext` is false. |
| Unit | Non-empty non-audit list proceeds normally | Existing mocked builder path with one list item; verify multiple maintain path builds/executes as before. |
| Regression | Audit-only list is rejected | Add a test with only `Variable#setAudit(true)` list to prove audit variables do not satisfy the guard. |

Target command: `mvn test -pl awe-framework/awe-controller -Dtest=SQLMaintainConnectorTest`.

## Migration / Rollout

No migration required. Rollout is a framework behavior fix; descriptors with invalid `multiple="true"` configuration will now fail explicitly instead of silently producing zero business operations.

## Open Questions

None.
