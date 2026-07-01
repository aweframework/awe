# Proposal: Fix Selenium Login Wait

## Intent
Harden Selenium login and post-login synchronization in `awe-testing` so integration tests wait for the authenticated AWE shell only when it is actually ready. The change targets intermittent failures where login is clicked, credentials remain visible, and subsequent waits look for avatar, menu, or logout controls before the application has reached a stable authenticated state.

## Scope

### In Scope
- Improve login/post-login waits inside `SeleniumUtilities`.
- Prefer efficient, bounded waits based on interactability, clickability, enabled state, and shell readiness.
- Keep waits reusable for selectors involved in login, navigation, and logout readiness.
- Preserve current Selenium test intent and public helper behavior where practical.

### Out of Scope
- No changes to real login functionality, authentication flow, or application code.
- No screen XML, menu XML, or descriptor changes.
- No CI configuration, browser image, pipeline timeout, or test suite restructuring.
- No blind sleeps or unbounded retry loops.

## Capabilities
No product capability changes.

### New Capabilities
- None.

### Modified Capabilities
- None. This is test infrastructure behavior only: Selenium helpers should synchronize more accurately with the existing UI.

## Approach
Refine `checkLogin` and supporting wait helpers to use explicit, short, bounded Selenium waits for actionable UI states instead of simple presence/visibility alone. After submitting credentials, wait for an authenticated-shell readiness condition that can succeed only when login has completed, such as visible avatar plus enabled/logout/menu readiness, while still failing fast with useful diagnostics. Avoid fixed sleeps; use polling-based conditions with existing timeout conventions so normal fast paths remain fast.

## Affected Areas
| Area | Impact | Description |
|------|--------|-------------|
| `awe-framework/awe-testing/src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java` | Modified | Harden `checkLogin`, selector writing/clicking, and post-login readiness waits. |
| `awe-framework/awe-testing` Selenium tests | Indirect | Tests should become less flaky without changing expected behavior. |

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Wait condition is too strict for valid slow states | Medium | Combine a minimal set of stable shell signals and keep bounded diagnostics. |
| Waits slow down the suite | Medium | Use explicit polling conditions, no blind sleeps, and fast success paths. |
| Helper behavior changes affect unrelated tests | Low | Keep changes localized and preserve existing helper contracts. |

## Rollback Plan
Revert the `SeleniumUtilities` changes. Because no app, XML, or CI behavior changes are included, rollback is limited to test utility code.

## Success Criteria
- `IntegrationTestsIT.t030_screenModulesUsage` no longer fails waiting for login/avatar/menu/logout readiness under the observed scenario.
- Login helper waits for interactable/clickable/enabled states where relevant.
- Post-login readiness remains bounded and efficient, with no blind sleeps.
- `ApplicationTestsIT` and existing Selenium flows keep passing without product behavior changes.
