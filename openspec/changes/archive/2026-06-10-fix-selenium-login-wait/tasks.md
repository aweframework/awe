# Tasks: Fix Selenium Login Wait

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 80-180 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR: Selenium wait hardening plus verification evidence |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Harden Selenium login/post-login waits in `awe-testing` | PR 1 | Single localized change; include tests or command evidence. |

## Phase 1: RED / Baseline Verification

- [x] 1.1 Confirm the executable selector for `awe-tests/awe-boot` failing coverage: `IntegrationTestsIT#t030_screenModulesUsage`, `IntegrationTestsIT#t000_loginTest`, and the truthful wrong-login regression selector `RegressionTestsIT#t009_wrongLogin`.
- [x] 1.2 Before implementation, run the smallest feasible baseline command for the previously failing login path and capture whether it fails or is flaky without changing product code.
- [x] 1.3 If `awe-framework/awe-testing` has focused Selenium utility tests, add a failing test for login input actionability and submit clickability; otherwise document why integration evidence is the RED gate.

## Phase 2: GREEN / Login Actionability

- [x] 2.1 Modify `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java` to add bounded private helpers for visible+enabled input actionability and clickable submit controls.
- [x] 2.2 Wire `checkLogin` to wait for username/password actionability before sending credentials and for `ButLogIn` clickability before clicking.
- [x] 2.3 Keep timeout behavior based on existing `properties.getTimeout()` / `WebDriverWait`; do not add sleeps, unbounded loops, or global timeout changes.

## Phase 3: GREEN / Authenticated Shell Readiness

- [x] 3.1 Add `waitForAuthenticatedShell(By postLoginSelector, String expectedText)` in `SeleniumUtilities` using bounded `ExpectedCondition` composition.
- [x] 3.2 Require loading bar invisibility, login form/button absence or staleness, requested post-login selector visibility/text, required frontend shell controls to be enabled/clickable, and optional frontend shell controls only when they are visibly present.
- [x] 3.3 Call authenticated-shell readiness from `checkLogin` before the final `checkText(cssSelector, checkText)` assertion.

## Phase 4: Diagnostics and Reuse

- [x] 4.1 Ensure timeout messages identify the failed stage: login input actionability, submit clickability, avatar/post-login selector, menu readiness, or logout readiness.
- [x] 4.2 Reuse existing helper paths (`waitUntil`, `waitForSelector`, `waitForLoadingBar`, `clickButton`, `gotoScreen`) where it improves consistency without changing their public contract.

## Phase 5: REFACTOR / Verification

- [x] 5.1 Run `mvn test -pl awe-tests/awe-boot -Dtest=IntegrationTestsIT#t000_loginTest` and record result.
- [x] 5.2 Run the confirmed command for `IntegrationTestsIT#t030_screenModulesUsage` and record result against the original job #14766571985 symptom.
- [x] 5.3 Run the wrong-login regression slice (`RegressionTestsIT#t009_wrongLogin`, or the ordered class slice when needed) to prove login failure diagnostics still behave correctly.
- [x] 5.4 Refactor duplicated wait predicates inside `SeleniumUtilities` only; keep changes localized to `awe-testing` and avoid XML, app login, CI, or real auth changes.
