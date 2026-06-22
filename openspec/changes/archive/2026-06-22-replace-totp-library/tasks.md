# Tasks: Replace TOTP Library

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | ~650-900 |
| 800-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 deps + adapter → PR 2 wiring + service/controller → PR 3 tests + docs |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|---|---|---|---|
| 1 | Replace Sam Stevens dependencies and add AWE TOTP/QR ports | PR 1 | `awe-dependencies`, controller/starter POMs, new adapter package |
| 2 | Wire the adapter through the public TOTP service and security config | PR 2 | Preserve XML/controller contracts and runtime behavior |
| 3 | Prove compatibility and remove stale library references | PR 3 | Tests, dependency graph checks, changelog |

## Phase 1: Foundation / Dependency Boundary

- [x] 1.1 Update `awe-framework/awe-dependencies/pom.xml` to remove Sam Stevens management, add `com.github.bastiaanjansen:otp-java:2.1.0`, keep ZXing, and explicitly manage `commons-codec`.
- [x] 1.2 Update `awe-framework/awe-controller/pom.xml` and `awe-framework/awe-starters/awe-spring-boot-starter/pom.xml` to drop Sam Stevens artifacts and depend only on AWE-controlled TOTP/QR libraries.
- [x] 1.3 Create `awe-framework/awe-controller/src/main/java/com/almis/awe/service/totp/` with `AweTotpOperations`, `OtpJavaTotpOperations`, and `ZxingQrPngGenerator`.

## Phase 2: Core Implementation

- [x] 2.1 Refactor `awe-framework/awe-controller/src/main/java/com/almis/awe/service/TotpService.java` to delegate secret generation, verification, OTPAuth data, and PNG rendering to the new adapter.
- [x] 2.2 Preserve SHA1, 6 digits, 30s period, ±1 window, and Base32 secrets in the adapter; fail fast on incompatible secrets with no Sam Stevens fallback.
- [x] 2.3 Remove `TotpAutoConfiguration` and Sam Stevens bean wiring from `awe-framework/awe-starters/awe-spring-boot-starter/src/main/java/com/almis/awe/autoconfigure/SecurityConfig.java` and inject the new adapter into `TotpService`.
- [x] 2.4 Update `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/TotpController.java` to keep `/access/qr-code` returning `image/png` with the new QR path.

## Phase 3: Testing / Verification

- [x] 3.1 Add adapter tests for stored Base32 secrets, fixed-time vectors, current/previous/next window acceptance, and out-of-window rejection.
- [x] 3.2 Add QR tests that assert OTPAuth issuer/label/algorithm/digits/period and verify ZXing PNG generation.
- [x] 3.3 Replace Sam Stevens-mock tests in `TotpServiceTest`, `AuthenticationConfigTest`, and related access/controller coverage with behavior and wiring tests.
- [x] 3.4 Verify `disabled`, `optional`, and `force` login, activation, settings, and `/access/qr-code` behavior remain unchanged.
- [x] 3.5 Add dependency-graph checks proving `dev.samstevens.totp` is absent and `commons-codec` is explicitly controlled.

## Phase 4: Cleanup / Documentation

- [x] 4.1 Update `CHANGELOG.md` with a short release note for the TOTP library swap if this change ships in the next release. *(Automated release notes; no manual `CHANGELOG.md` edit required per maintainer clarification.)*
- [x] 4.2 Remove obsolete Sam Stevens imports, mocks, and stale comments after tests pass.
