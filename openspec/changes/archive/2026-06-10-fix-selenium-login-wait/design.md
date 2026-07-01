# Design: Fix Selenium Login Wait

## Technical Approach
Harden only `awe-testing` by making login synchronization express the real browser states required by the authenticated AWE shell without hardcoding Angular-only selectors in shared utilities. `checkLogin` will still drive the same public flow (`goToUrl`, write credentials, click `ButLogIn`, assert avatar text), but it will use bounded ExpectedConditions for actionable login fields/buttons and an authenticated-shell readiness condition before asserting the requested selector text. No sleeps, no global timeout inflation, and no app/auth/XML/CI changes.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Login readiness boundary | Add private helpers in `SeleniumUtilities` for actionable inputs/buttons and authenticated shell readiness. | Change every Selenium test, increase timeout, or add sleeps. | The flaky behavior is in shared Selenium synchronization; local helpers fix all login callers while preserving test intent. |
| Shell-ready signal | Wait for: loading bar invisible, login button no longer visible or stale when successful, requested post-login selector visible with expected text, required frontend shell controls actionable, and optional frontend shell controls actionable only when visibly present. | Use only `#ButUsrAct span.avatar-text`, or require a route-specific screen. | The observed failure stayed on the login screen after click; readiness must prove transition into the authenticated shell, not just wait for one late selector, while avoiding false negatives from frontend-specific or layout-specific optional controls. |
| Failure diagnostics | Keep existing `waitUntil`/`assertWithScreenshot` path and condition messages. | Throw raw Selenium exceptions or custom retry loops. | Current utilities already centralize screenshots and test failure reporting. |
| Timeout strategy | Reuse `properties.getTimeout()` through `WebDriverWait`; do not alter global timeouts. | Increase CI/job timeout or hard-code longer waits. | Polling ExpectedConditions remain fast on success and bounded on failure. |

## Data Flow

```
checkLogin
  -> goToUrl(baseUrl)
  -> waitForActionableLoginForm
  -> write username/password using clickable/enabled inputs
  -> click login button when clickable
  -> waitForAuthenticatedShell(cssSelector, checkText)
  -> checkText(cssSelector, checkText)
```

## File Changes

| File | Action | Description |
|---|---|---|
| `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java` | Modify | Add private ExpectedCondition helpers and wire them into `checkLogin`; optionally reuse actionable selector helpers from `writeText`/`clickButton` only where login needs them. |
| `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/IAweFrontEndInstructions.java` | Modify | Expose required vs optional post-login shell control selectors through the frontend abstraction. |
| `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/AngularAweInstructions.java` | Modify | Provide Angular-specific required and optional post-login shell control selectors. |
| `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/ReactAweInstructions.java` | Modify | Provide React-specific required and optional post-login shell control selectors so shared waits stay frontend-agnostic. |
| `awe-tests/awe-boot/src/test/java/com/almis/awe/test/selenium/IntegrationTestsIT.java` | No change | Existing `checkLogin("test", "test", "#ButUsrAct span.avatar-text", "Manager (test)")` calls should keep working. |

## Interfaces / Contracts

No public API change for Selenium callers. New helpers remain private/protected inside `SeleniumUtilities`, and the existing frontend abstraction gains post-login shell selector accessors:

```java
private ExpectedCondition<Boolean> actionable(By selector);
private ExpectedCondition<Boolean> authenticatedShellReady(By postLoginSelector, String expectedText);
private void waitForAuthenticatedShell(By postLoginSelector, String expectedText);
```

Contract: `checkLogin` returns only after the requested post-login selector contains the expected text and the authenticated shell is interactable, or fails with the existing screenshot diagnostics within the configured timeout.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Focused runtime tests for WebDriver-bound private waits. | Exercise the real wait helpers with mocked `WebDriver` state transitions so timeout diagnostics and frontend-specific shell readiness stay deterministic. |
| Integration/Selenium | Successful login reaches avatar and shell controls. | Run `mvn test -pl awe-tests/awe-boot -Dtest=IntegrationTestsIT#t000_loginTest` and the previously failing `IntegrationTestsIT#t030_screenModulesUsage` if selectable by Surefire/Failsafe naming. |
| Regression | Wrong-login expectations still fail on alert text. | Run `RegressionTestsIT#t009_wrongLogin` or the existing ordered class slice covering wrong login. |

## Migration / Rollout

No migration required. The rollout is a test-utility-only change in `awe-testing`; rollback is reverting `SeleniumUtilities.java`.

## Open Questions

- [ ] Confirm the exact method selector for the previously failing `t030_screenModulesUsage` in the local test runner, because class/method names may be handled by Failsafe rather than Surefire.
