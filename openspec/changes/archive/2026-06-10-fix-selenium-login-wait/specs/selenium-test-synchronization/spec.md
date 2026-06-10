# Delta for selenium-test-synchronization

## ADDED Requirements

### Requirement: Bounded login form actionability

Selenium login helpers MUST wait for login inputs and the login submit control to be actionable before interacting with them. Actionable means the target element is present, visible, enabled, and not blocked from the requested Selenium action according to an explicit bounded wait.

#### Scenario: Credentials are entered only when fields are interactable

- GIVEN the login screen is displayed but its username or password control is not yet interactable
- WHEN `checkLogin` attempts to authenticate
- THEN the helper MUST wait within the configured timeout until the required controls are visible and enabled
- AND it MUST enter credentials only after the controls can receive input

#### Scenario: Login submit waits for clickability

- GIVEN credentials have been entered and the login submit control is visible
- WHEN `checkLogin` submits the login form
- THEN the helper MUST wait within the configured timeout until the submit control is clickable and enabled
- AND it MUST not rely on element presence alone before clicking

### Requirement: Authenticated shell readiness after login

After submitting credentials, Selenium login synchronization MUST wait for the authenticated AWE shell to be ready before returning control to tests. Shell readiness MUST be based on stable authenticated UI signals, including the visible user avatar and enabled shell controls such as logout and navigation/menu controls when those controls are part of the loaded shell.

#### Scenario: Login form remains visible after submit

- GIVEN credentials were submitted
- AND the login page remains visible with the filled credentials
- WHEN `checkLogin` waits for post-login readiness
- THEN the helper MUST continue polling for authenticated shell readiness until the bounded timeout expires or readiness is reached
- AND it MUST not report successful login while the authenticated shell is not ready

#### Scenario: Avatar appears before shell controls are enabled

- GIVEN the authenticated avatar is visible
- AND logout or menu controls are still disabled or not actionable
- WHEN a test continues after login
- THEN the login wait MUST keep synchronizing until the required shell controls are enabled or clickable within the bounded timeout
- AND subsequent helpers such as screen navigation and logout SHOULD be able to reuse the same readiness semantics

### Requirement: No blind sleeps or unbounded retry loops

Selenium synchronization for login, navigation readiness, and logout readiness MUST use explicit bounded waits with polling conditions. The implementation MUST NOT add fixed sleeps, indefinite loops, or retries that can exceed the existing timeout conventions without a clear bound.

#### Scenario: Fast application path stays fast

- GIVEN login completes quickly and authenticated shell signals are immediately ready
- WHEN `checkLogin` runs
- THEN the helper MUST return as soon as the required readiness conditions are satisfied
- AND it MUST not wait for an arbitrary fixed delay

#### Scenario: Slow application path remains bounded

- GIVEN the browser or AWE shell is slow to enable avatar, menu, or logout controls
- WHEN synchronization waits for readiness
- THEN the helper MUST poll until the configured timeout is reached
- AND it MUST fail within that timeout rather than retrying indefinitely

### Requirement: Diagnostic wait failures

When a login or shell-readiness wait times out, the failure MUST make the unmet condition diagnosable. Diagnostics SHOULD identify the wait stage and relevant selector or readiness condition so intermittent Selenium failures can distinguish login form actionability, submit clickability, avatar readiness, menu readiness, and logout readiness issues.

#### Scenario: Login actionability timeout identifies the blocked stage

- GIVEN the username, password, or submit control never becomes actionable
- WHEN the bounded login wait times out
- THEN the failure MUST identify which login actionability condition failed
- AND it SHOULD include the selector involved where applicable

#### Scenario: Post-login readiness timeout identifies shell condition

- GIVEN login was submitted but the authenticated shell never reaches readiness
- WHEN the bounded post-login wait times out
- THEN the failure MUST identify the missing authenticated shell readiness condition
- AND it SHOULD preserve enough context to correlate failures with avatar, menu, or logout synchronization.
