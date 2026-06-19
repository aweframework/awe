# Delta for maintain-execution

## ADDED Requirements

### Requirement: Validate list input for multiple maintain

The system MUST validate any maintain operation with `multiple="true"` before SQL build or execution. It MUST fail with a controlled maintain error if, and only if, no valid non-audit list variable exists in the request. The system MUST ignore audit variables for this check and MUST NOT fall back to single-maintain execution. A valid list variable with zero items MUST NOT fail validation.

#### Scenario: Missing non-audit list variable fails early

- GIVEN a maintain target with `multiple="true"`
- AND the request contains no valid non-audit list variable
- WHEN the maintain is launched
- THEN the system returns a controlled maintain error
- AND SQL is not built or executed

#### Scenario: Empty list is accepted

- GIVEN a maintain target with `multiple="true"`
- AND the request includes a valid non-audit list variable with zero items
- WHEN the maintain is launched
- THEN validation passes
- AND the maintain does not fail before SQL execution

#### Scenario: Valid list proceeds normally

- GIVEN a maintain target with `multiple="true"`
- AND the request includes a valid non-audit list variable with one or more items
- WHEN the maintain is launched
- THEN multiple maintain execution proceeds normally

### Requirement: Preserve generic maintain error behavior across channels

The system MUST expose the same functional behavior for REST/API and UI requests when this validation fails. The client response MUST use the existing generic maintain title and message keys.

#### Scenario: REST/API returns the generic maintain error

- GIVEN a request that fails the multiple-maintain list validation
- WHEN the request is handled through REST/API
- THEN the response uses the generic maintain error title and message

#### Scenario: UI returns the same generic maintain error

- GIVEN a request that fails the multiple-maintain list validation
- WHEN the request is handled through the UI
- THEN the response uses the same generic maintain error title and message
