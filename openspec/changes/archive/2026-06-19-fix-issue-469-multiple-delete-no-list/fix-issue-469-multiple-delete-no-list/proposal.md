# Proposal: Validate Multiple Maintain Requires a List Variable

## Intent

Prevent `multiple="true"` maintain operations from silently doing nothing when the request has no valid non-audit list variable. This is a descriptor/request configuration error and must fail early, before SQL build/execution, while UI responses reuse the existing generic maintain messages and logs keep the technical detail.

## Scope

### In Scope
- Validate any maintain operation with `multiple="true"`, not only deletes.
- Fail only when no valid non-audit list variable exists.
- Allow a valid list variable even when the list is empty.
- Preserve equivalent REST/API and UI functional behavior.
- Add focused regression coverage in `SQLMaintainConnectorTest`.

### Out of Scope
- Fallback from multiple maintain to single maintain.
- New locale keys or UI-specific maintain messages.
- Descriptor XML semantic changes beyond this validation.

## Capabilities

### New Capabilities
- `maintain-execution`: Maintain execution behavior, including multiple-operation validation and error handling.

### Modified Capabilities
- None.

## Approach

Add connector-level validation in `SQLMaintainConnector.launch` after `queryUtil.addToVariableMap(...)` prepares variables and before dispatching to `launchMultipleMaintain`. For `query.getMultiple() == true`, inspect `query.getVariableDefinitionList()` and the prepared `parameterMap` for at least one non-audit variable whose `QueryParameter` is a list. If none exists, throw a controlled maintain exception using `ERROR_TITLE_DURING_MAINTAIN` and `ERROR_MESSAGE_DURING_MAINTAIN`, with maintain id/type and missing-list detail preserved in the exception/cause for logs. Empty lists pass validation and naturally produce zero iterations.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `awe-framework/awe-controller/src/main/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnector.java` | Modified | Add early validation before multiple dispatch. |
| `awe-framework/awe-controller/src/test/java/com/almis/awe/service/data/connector/maintain/SQLMaintainConnectorTest.java` | Modified | Cover no-list failure, empty-list pass, and valid-list behavior. |
| `awe-framework/awe-generic-screens/src/main/resources/application/awe/locale/Locale-*.xml` | Unchanged | Reuse existing generic locale keys. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Audit-only list variables treated as valid | Medium | Exclude audit variables from validation. |
| Empty selected-grid requests rejected | Low | Test empty list as valid configuration. |
| Divergent API/UI errors | Low | Use existing maintain exception flow and locale keys. |

## Rollback Plan

Revert the connector validation and tests; previous multiple maintain dispatch and `hasNext` behavior will be restored.

## Dependencies

- Existing `QueryUtil` list detection and `QueryParameter.isList()` semantics.
- Existing locale keys: `ERROR_TITLE_DURING_MAINTAIN`, `ERROR_MESSAGE_DURING_MAINTAIN`.

## Success Criteria

- [ ] `multiple="true"` without a non-audit list variable fails before SQL build/execution.
- [ ] Empty valid lists do not trigger this validation error.
- [ ] Valid list-based multiple maintain behavior remains unchanged.
- [ ] REST/API and UI expose the same generic maintain error while logs keep technical details.
