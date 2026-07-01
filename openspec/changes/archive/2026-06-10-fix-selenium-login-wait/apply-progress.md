# Apply Progress - fix-selenium-login-wait

## Completed tasks

- [x] Tasks 1.1-1.3: confirmed the truthful Surefire selectors/commands, captured command-backed RED evidence, and documented why integration execution is the only practical RED gate for this Selenium utility change.
- [x] Tasks 2.1-2.3: added bounded private login actionability helpers and wired `checkLogin` to wait for actionable username/password inputs and clickable login submit.
- [x] Tasks 3.1-3.3: added authenticated-shell readiness helpers and made login completion depend on shell readiness before the final selector assertion.
- [x] Follow-up fix: tightened authenticated-shell readiness so avatar text alone is not enough; the shell now also requires actionable avatar/navigation controls before readiness succeeds.
- [x] Tasks 4.1-4.2: preserved centralized diagnostics through `waitUntil`/`assertWithScreenshot` and reused existing helper paths such as `waitForLoadingBar` and `clickButton` without changing public contracts.
- [x] Tasks 5.1-5.4: ran focused Selenium verification slices, recorded the apply-time environment blockers, then revalidated the same slices successfully during verify with the application pre-started and duplicate Selenium app startup disabled.

## Evidence

- `IntegrationTestsIT` exposes the targeted methods `t000_loginTest()` and `t030_screenModulesUsage()`.
- `RegressionTestsIT` does not expose `t210_wrongLogin`; the actual wrong-login method is `t009_wrongLogin()`. The executable regression selector is therefore `RegressionTestsIT#t009_wrongLogin` (or the ordered slice `RegressionTestsIT#t000_loginTest+t009_wrongLogin`).
- Focused Selenium execution in this module requires overriding the default skip flag with `-Dskip.selenium=false` and matching the class tag with `-Dtest.tags=ApplicationIntegrationIT` or `-Dtest.tags=RegressionWebsocketPrintIT`.
- The smallest feasible login-path baseline command was `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`; it executed the intended method and failed at `http://localhost:8080/` with `net::ERR_CONNECTION_REFUSED` before the login wait assertions could be exercised.
- There are no focused unit-style tests for `SeleniumUtilities` login waits in `awe-framework/awe-testing`; because the behavior is WebDriver-bound and private-helper-based, command-backed Selenium integration runs are the practical RED gate for this change.
- `inputToBeActionable(By selector, String stage)` exists and is used by `waitForLoginInputActionability(...)` to require visible and enabled login inputs.
- `elementToBeActionableForClick(By selector, String stage)` exists and is used by `waitForLoginButtonClickability(...)` to require clickable login submit behavior.
- `checkLogin(...)` now waits for login input actionability, writes credentials, waits for login button clickability, clicks `ButLogIn`, then waits for login result or authenticated-shell readiness before the final `checkText(...)` assertion.
- `waitForAuthenticatedShell(...)` and `authenticatedShellReady(...)` now require loading bar invisibility, hidden login form controls, expected post-login selector text, required frontend shell controls from `IAweFrontEndInstructions`, and optional frontend shell controls only when they are visibly present.
- Diagnostic stage names are encoded in the condition `toString()` messages, including `Login input actionability [...]`, `Login submit clickability [...]`, `Post-login selector readiness`, `Login form result`, and `Authenticated shell readiness`.
- All implemented code remains localized to `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java`.
- The focused screen-modules command `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage` ran both methods and recorded two blockers: `t000_loginTest` errored with `net::ERR_CONNECTION_REFUSED`, and `t030_screenModulesUsage` then failed waiting for `By.cssSelector: [name='tools']`, so the original login-wait symptom could not be revalidated in this workspace.
- The focused wrong-login command `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin` ran both methods and recorded the same environment blocker on `t000_loginTest`, then failed `t009_wrongLogin` waiting for `#ButLogOut:not([disabled])` because the authenticated prerequisite state was unavailable.
- Every executed Selenium slice also logged a secondary environment warning/failure because `ffmpeg` is not installed for the video recorder hook, but the primary test-stopping blocker was the missing application at `localhost:8080`.
- Verify established the working local recipe: start `awe-tests/awe-boot` first with `mvn spring-boot:run`, wait for `http://localhost:8080/`, then run focused slices with `mvn verify ... -Dskip.selenium=true -Dawe.test.allowed-recording=false` so Maven does not try to start a second application instance against the same HSQL database and JMX port.
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest` passed.
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage` passed.
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin` passed.
- Final MR-readiness rerun on the exact current tree restarted `awe-tests/awe-boot` manually with `mvn spring-boot:run`, waited for `http://localhost:8080/` to return HTTP 200, then reran the same focused Selenium slices against the live app.
- The fresh browser-level rerun again passed `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`.
- The fresh browser-level rerun again passed `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage`.
- The fresh browser-level rerun again passed `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin`.
- The fresh rerun used real Chrome/ChromeDriver execution. Selenium emitted the known CDP-version warning for Chrome 146, but every targeted slice still passed.
- Focused runtime coverage is now available inside `awe-framework/awe-testing/src/test/java/com/almis/awe/testing/utilities/SeleniumUtilitiesTest.java` using mocked `WebDriver` + real `WebDriverWait` execution, so the previously untested timeout-only scenarios no longer require brittle Selenium environment orchestration.
- `shouldFailWithinConfiguredTimeoutWhenConditionNeverBecomesTrue` executes the real `waitUntil(...)` path with a permanently false `ExpectedCondition` and proves the failure stays bounded by the configured wait rather than retrying indefinitely.
- `shouldReportLoginActionabilityStageWhenInputNeverBecomesActionable` executes the real login-input wait path and proves timeout diagnostics include the login actionability stage plus the affected selector.
- `shouldReportAuthenticatedShellStageWhenPostLoginReadinessNeverArrives` executes the real authenticated-shell wait path and proves timeout diagnostics include the shell-readiness stage plus the post-login selector context.
- `shouldKeepAuthenticatedShellNotReadyUntilShellControlsAppear` proves authenticated-shell readiness stays false when avatar text appears before the required actionable user control exists.
- `shouldRequireActionableShellControlsBeforeAuthenticatedShellIsReady` now uses the current production Angular selector shape (`By.id("main-menu-toggle")`) and proves authenticated-shell readiness becomes true when required shell controls are actionable.
- `shouldBlockShellReadinessWhenVisibleOptionalControlIsNotActionable` proves visible optional shell controls still block readiness when they are present but disabled.
- `shouldTreatAbsentAngularMenuControlsAsOptionalForShellReadiness` proves Angular readiness no longer fails just because menu/logout controls are absent from the currently loaded shell.
- `shouldUseFrontendSpecificShellSelectorsInsteadOfHardcodedAngularSelectors` proves the shared readiness helper now follows frontend-provided selectors instead of Angular-only shared selectors.
- `shouldWaitForLoginSubmitClickability` proves the login flow waits on the real `#ButLogIn:not([disabled])` clickable selector before submit.
- `shouldWaitForActionableLoginInputsBeforeTypingCredentialsInCheckLogin` proves `checkLogin(...)` waits for actionable username/password inputs before recording the credential writes on the happy path.
- `shouldReturnQuicklyWhenConditionIsImmediatelySatisfied` proves the bounded wait path still returns quickly on the fast-success path instead of consuming the full timeout budget.
- `verify-report.md` was corrected to describe this archive follow-up as focused module-local evidence rather than claiming full end-to-end strict-TDD/spec re-verification for the entire historical change.

## Remaining tasks

- None.

## TDD Cycle Evidence

Strict TDD evidence is now recorded, and it truthfully shows both the apply-time blockers and the later verify-time green reruns that used the correct startup recipe.

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `awe-tests/awe-boot/src/test/java/com/almis/awe/test/selenium/IntegrationTestsIT.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/selenium/RegressionTestsIT.java` | Integration / Selenium | N/A (artifact validation only) | ✅ Method selectors confirmed from source; discovered `t009_wrongLogin` replaces the requested `t210_wrongLogin`. | ✅ No code change required. | ➖ Not applicable. | ➖ None needed. |
| 1.2 | `IntegrationTestsIT` | Integration / Selenium | ✅ Focused command executed before any new code changes in this batch. | ✅ `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest` failed with `net::ERR_CONNECTION_REFUSED`. | ✅ Passed during verify after pre-starting `awe-boot` and rerunning `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`. | ➖ Single baseline command. | ➖ None needed. |
| 1.3 | `SeleniumUtilities` / existing Selenium ITs | Integration / Selenium | ✅ Existing focused test locations were inspected first. | ✅ Lack of focused unit-level wait tests was confirmed from source structure. | ✅ Documented integration evidence as the only practical RED gate. | ➖ Not applicable. | ➖ None needed. |
| 5.1 | `IntegrationTestsIT` | Integration / Selenium | ✅ Focused command executed. | ✅ Reproduced failing environment state on `t000_loginTest`. | ✅ Passed during verify with `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`. | ➖ Single flow. | ✅ No further refactor needed; change remains localized to `SeleniumUtilities`. |
| 5.2 | `IntegrationTestsIT` | Integration / Selenium | ✅ Ordered slice executed. | ✅ `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage` recorded failures before the original symptom could be rechecked. | ✅ Passed during verify with `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage`. | ✅ Combined login + screen-modules slice used to avoid the false single-method prerequisite gap. | ✅ No additional refactor applied. |
| 5.3 | `RegressionTestsIT` | Integration / Selenium | ✅ Ordered slice executed. | ✅ `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin` recorded the environment blocker and downstream logout-precondition failure. | ✅ Passed during verify with `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin`. | ✅ Combined login + wrong-login slice used to reflect class ordering needs. | ✅ No additional refactor applied. |
| 5.4 | `SeleniumUtilities.java` | Refactor evidence | ✅ Existing implementation inspected after verification runs. | ✅ No new failing duplication-specific evidence appeared. | ✅ Localized scope preserved. | ➖ Not applicable. | ✅ No change required beyond the already localized helper extraction in `SeleniumUtilities`. |
| 5.4 follow-up | `awe-framework/awe-testing/src/test/java/com/almis/awe/testing/utilities/SeleniumUtilitiesTest.java` | Focused module runtime | ✅ This archival correction batch added focused assertions for login submit clickability, wait-before-typing happy-path ordering, fast-path bounded waits, visible-but-disabled optional shell controls, and the current Angular selector shape used by shell readiness. | ✅ Added four new focused tests and aligned the existing actionable-shell test with the production selector shape. | ✅ `mvn test -pl awe-framework/awe-testing -Dtest=SeleniumUtilitiesTest` passed with the expanded runtime suite. | ✅ Covered eleven runtime paths: bounded timeout failure, fast-path success, login input diagnostics, wait-before-typing happy path, login submit clickability, authenticated-shell diagnostics, missing required shell control, required actionable shell controls, visible disabled optional shell controls, optional Angular control absence, and React-specific selectors. | ✅ No production refactor was needed; only focused test coverage and archive claims were corrected. |

## Test commands run

- `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`
- `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t030_screenModulesUsage`
- `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t009_wrongLogin`
- `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage`
- `mvn test -pl awe-tests/awe-boot -Dskip.selenium=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin`
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest`
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage`
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin`
- `mvn test -pl awe-framework/awe-testing -Dtest=SeleniumUtilitiesTest`
- `mvn spring-boot:run` (from `awe-tests/awe-boot`, final MR gate pre-start)
- `curl -I http://localhost:8080/` (final MR gate readiness check)
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest` (fresh final MR gate rerun)
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=ApplicationIntegrationIT -Dtest=IntegrationTestsIT#t000_loginTest+t030_screenModulesUsage` (fresh final MR gate rerun)
- `mvn verify -pl awe-tests/awe-boot -Dskip.selenium=true -Dawe.test.allowed-recording=false -Dtest.tags=RegressionWebsocketPrintIT -Dtest=RegressionTestsIT#t000_loginTest+t009_wrongLogin` (fresh final MR gate rerun)
- `mvn test -pl awe-framework/awe-testing -Dtest=SeleniumUtilitiesTest` (fresh focused runtime guardrail rerun)

## Deviations from design/tasks

- The implemented code matches the design scope and stays within `SeleniumUtilities`.
- The apply artifacts had not been updated when the code was previously added, so task completion and TDD/verification evidence were missing from OpenSpec even though the code existed.
- The requested regression selector `RegressionTestsIT#t210_wrongLogin` does not exist in the repository; the truthful executable target is `RegressionTestsIT#t009_wrongLogin`.
- The follow-up fix removed Angular-only post-login shell selectors from shared `SeleniumUtilities` by routing required and optional shell controls through `IAweFrontEndInstructions` implementations.
- A pre-started local application is mandatory in this workspace; when using `mvn verify` for focused Selenium slices, `-Dskip.selenium=true` is also required to prevent a duplicate Spring Boot start against the same HSQL database and JMX port.
- Although the original design considered focused unit tests impractical, deterministic module-local runtime coverage proved feasible by executing the real wait helpers against a mocked `WebDriver` and asserting both timeout diagnostics and shell-control readiness semantics directly.
- This archive-maintenance batch has now been followed by a fresh MR-readiness rerun of the browser-level Selenium slices on the exact current tree, so the archive evidence includes both deterministic utility coverage and live end-to-end confirmation.

## Files changed

- `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java`
- `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/IAweFrontEndInstructions.java`
- `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/AngularAweInstructions.java`
- `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/selenium/ReactAweInstructions.java`
- `awe-framework/awe-testing/src/test/java/com/almis/awe/testing/utilities/SeleniumUtilitiesTest.java`
- `openspec/changes/archive/2026-06-10-fix-selenium-login-wait/apply-progress.md`
- `openspec/changes/archive/2026-06-10-fix-selenium-login-wait/verify-report.md`

## Status

- 14 of 14 tasks are now marked complete in OpenSpec artifacts.
- Apply and focused re-verify are complete for this follow-up fix.
