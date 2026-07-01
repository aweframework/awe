# screen-model-suggest-label-hydration Specification

## Purpose

Define how initial screen-model generation resolves labels for already selected suggest criteria while preserving existing fallback behavior.

## Requirements

### Requirement: Backend hydration for selected suggest values

The system MUST perform a server-side hydration pass during initial screen-model generation for suggest criteria that already contain selected values but lack resolved label rows.

#### Scenario: Hydrate labels in the initial response

- GIVEN a suggest criteria has selected value data and a configured `checkTarget`
- WHEN initial screen-model generation finishes applying screen-target and default selections
- THEN the system SHALL query label data before sending the screen response
- AND the criteria values SHALL include the resolved label rows in the initial response

#### Scenario: Hydrate multiple selected values

- GIVEN a suggest criteria contains multiple selected values without complete labels
- WHEN the backend hydration pass runs
- THEN the system SHALL resolve labels for each selected value supported by `checkTarget`

### Requirement: Compatibility fallback for unresolved labels

The system MUST preserve the existing client fallback path when backend hydration cannot resolve suggest labels during initial load.

#### Scenario: Fallback remains available

- GIVEN initial screen-model generation cannot resolve labels for a selected suggest value
- WHEN the client receives the screen model
- THEN the backend SHALL leave the existing fallback-compatible metadata intact
- AND the client MAY execute its current post-render reload behavior to resolve labels

#### Scenario: Empty hydration result does not fail initial load

- GIVEN the backend hydration query returns no label rows
- WHEN the initial response is built
- THEN the system SHALL complete screen loading without an error caused by missing labels

### Requirement: No overwrite of selected or default values

The system MUST add only missing label rows during hydration and MUST NOT replace previously applied selected values or default selections.

#### Scenario: Preserve selected value payload

- GIVEN screen-target or default processing has already populated selected values for a suggest criteria
- WHEN backend hydration adds missing labels
- THEN the system SHALL keep the existing selected value payload unchanged

#### Scenario: Preserve already resolved values

- GIVEN a suggest criteria already contains complete values for its selected entries
- WHEN the backend hydration pass evaluates the criteria
- THEN the system SHALL NOT overwrite the existing values data
- AND the system SHOULD skip unnecessary hydration work for that criteria
