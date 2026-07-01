# Tasks: Validate Multiple Maintain Requires a List Variable

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 120-220 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add failing regression tests for multiple-maintain list validation | PR 1 | `SQLMaintainConnectorTest.java`; cover no-list failure, empty-list acceptance, valid-list path, audit-only rejection |
| 2 | Implement early connector guard and preserve generic maintain error flow | PR 1 | `SQLMaintainConnector.java`; validate before `launchMultipleMaintain`, no fallback to single maintain |

## Phase 1: RED — Regression Tests

- [x] 1.1 Add tests in `awe-framework/awe-controller/src/test/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnectorTest.java` for `multiple="true"` with no non-audit list variable: assert `AWException`, generic maintain locale keys, and no `SQLMaintainBuilder.build()` call.
- [x] 1.2 Add tests in the same class for a valid empty list and a non-empty list: empty list must pass validation; non-empty list must still execute the multiple-maintain path.

## Phase 2: GREEN — Connector Guard

- [x] 2.1 Add a private validation helper in `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnector.java` that checks declared non-audit variables against `parameterMap` and `QueryParameter.isList()`.
- [x] 2.2 Call the helper in `launch(...)` immediately after `queryUtil.addToVariableMap(...)` and before `launchMultipleMaintain(...)`; throw controlled `AWException` with `ERROR_TITLE_DURING_MAINTAIN` / `ERROR_MESSAGE_DURING_MAINTAIN`.

## Phase 3: Verification / Channel Parity

- [x] 3.1 Add a service-layer smoke test in `awe-framework/awe-controller/src/test/java/com/almis/awe/service/MaintainServiceConnectionTest.java` (or the narrowest existing maintain service test) proving the `AWException` is rethrown unchanged through `MaintainService`.
- [x] 3.2 Run `mvn test -pl awe-framework/awe-controller -Dtest=SQLMaintainConnectorTest,MaintainServiceConnectionTest` to verify the failure and success paths.

## Phase 4: Cleanup

- [x] 4.1 Update connector comments/Javadocs to document that missing list input is a controlled configuration error, not a single-maintain fallback.
