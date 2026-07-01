# Proposal: Replace TOTP Library

## Intent

Replace the discontinued Sam Stevens TOTP dependencies while preserving AWE's authenticator-app 2FA behavior. Existing users with `ope.secret2fa` MUST keep using 2FA without reactivation or data migration.

## Scope

### In Scope
- Remove `dev.samstevens.totp` starter/core usage and dependency management.
- Introduce an AWE-owned TOTP adapter using `com.github.bastiaanjansen:otp-java:2.1.0` for RFC-6238 behavior.
- Generate OTPAuth URI/PNG QR codes with direct ZXing usage behind AWE code.
- Preserve Base32 secrets, SHA1, 6 digits, 30s period, and ±1 time-step tolerance.
- Manage/override security-sensitive transitive dependencies, especially `commons-codec`.

### Out of Scope
- No Spring Security OTT adoption; it is not an authenticator-app TOTP replacement.
- No visible login, activation, settings, XML action, or database-flow changes unless required for compatibility.
- No temporary Sam Stevens fallback if validation is incompatible; fail fast instead.
- Documentation cleanup for unrelated property-name typos is deferred.

## Capabilities

### New Capabilities
- `totp-authentication`: AWE authenticator-app TOTP provisioning and verification behavior.

### Modified Capabilities
- None.

## Approach

Use `otp-java` for secret/TOTP verification and AWE-owned QR generation with ZXing. Keep controller, service, descriptor, and login-routing contracts stable. Normalize QR issuer/label only where authenticator-app compatible. Add behavior-level tests instead of library-mock tests.

## Alternatives Considered

| Alternative | Decision | Reason |
|-------------|----------|--------|
| Keep Sam Stevens | Rejected | Does not solve discontinued dependency. |
| Spring Security OTT | Rejected | Different token model; no OTPAuth QR or existing-secret compatibility. |
| Internal RFC-6238 | Deferred | Higher crypto maintenance burden. |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `awe-framework/awe-dependencies/pom.xml` | Modified | Replace managed TOTP deps; manage `commons-codec`. |
| `awe-framework/awe-controller` | Modified | TOTP adapter, QR PNG generation, tests. |
| `awe-framework/awe-starters/awe-spring-boot-starter` | Modified | Remove Sam Stevens auto-configuration wiring. |
| `awe-framework/awe-generic-screens/.../security` | Stable | Existing XML/screen contracts should remain unchanged. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing users locked out | Medium | Fixed-time vectors and existing-secret compatibility tests. |
| QR URI encoding drift | Medium | Assert issuer, label, secret, algorithm, digits, period. |
| Dependency regression | Low | Central dependency management and vulnerability review. |
| Review size >800 lines | Medium | Forecast adapter/tests/dependency edits as one PR unless tasks phase recommends slicing. |

## Rollback Plan

Revert dependency, starter wiring, adapter, and test changes in one commit before release. If released and incompatibility appears, fail fast, revert to the last known-good implementation, and patch with corrected compatibility tests before retrying.

## Dependencies

- `com.github.bastiaanjansen:otp-java:2.1.0`
- Direct ZXing PNG generation dependency already managed by AWE.
- AWE-managed `commons-codec` override.

## Success Criteria

- [ ] Existing Base32 secrets validate without user reactivation.
- [ ] New QR provisioning works in common authenticator apps.
- [ ] `/access/qr-code` still returns `image/png`.
- [ ] `disabled`, `optional`, `force`, `initial-screen`, and `activate-screen` behavior is unchanged.
- [ ] Sam Stevens dependencies are removed from the resolved graph.
