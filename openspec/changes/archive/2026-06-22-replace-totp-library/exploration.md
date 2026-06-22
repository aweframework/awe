## Exploration: replace-totp-library

### Current State
AWE's second-factor authentication is an authenticator-app TOTP flow, not Spring Security OTT. The current implementation is split across login routing, AWE XML descriptors, and a small TOTP service:

- Maven dependency management pins `dev.samstevens.totp:totp-spring-boot-starter:1.7.1` and `dev.samstevens.totp:totp:1.7.1`. `com.google.zxing:javase:3.5.3` is already directly managed because the Sam Stevens starter transitively brought a vulnerable ZXing version.
- `awe-spring-boot-starter` imports `TotpAutoConfiguration` and injects Sam Stevens beans (`SecretGenerator`, `QrDataFactory`, `QrGenerator`, `CodeVerifier`) into `TotpService`.
- `TotpService` generates Base32 secrets, stores them in `ope.secret2fa`, verifies user-entered codes with `CodeVerifier`, and generates PNG QR codes from OTPAuth data using the current user's username, localized `APP_NAME` issuer, and stored secret.
- The QR display path is descriptor-driven: `qr-code` and `force-qr-code` queries call the `qr-code` service, screens render `/access/qr-code?a=[random]`, and `TotpController` returns `image/png` bytes for that endpoint.
- Configuration knobs owned by AWE are `awe.security.totp.enabled` (`disabled`, `optional`, `force`), `awe.security.totp.initial-screen` (`check-2fa`), and `awe.security.totp.activate-screen` (`activate-2fa`). The current cryptographic defaults to preserve are Base32 secrets, SHA1, 6 digits, 30-second period, and ±1 time-step validation tolerance.

### Affected Areas
- `awe-framework/awe-dependencies/pom.xml` — central dependency versions and replacement of Sam Stevens TOTP artifacts; should also manage/override `commons-codec` if `otp-java` is adopted.
- `awe-framework/awe-controller/pom.xml` — direct runtime dependency on `dev.samstevens.totp:totp` and direct ZXing dependency used for QR generation.
- `awe-framework/awe-starters/awe-spring-boot-starter/pom.xml` — dependency on `totp-spring-boot-starter` and startup wiring currently supplied by Sam Stevens auto-configuration.
- `awe-framework/awe-starters/awe-spring-boot-starter/src/main/java/com/almis/awe/autoconfigure/SecurityConfig.java` — imports Sam Stevens auto-configuration and wires Sam Stevens beans into `TotpService`.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/service/TotpService.java` — main adapter around secret generation, OTPAuth/QR generation, and code validation.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/TotpController.java` — exposes `/access/qr-code` and currently throws Sam Stevens `QrGenerationException`.
- `awe-framework/awe-controller/src/main/java/com/almis/awe/service/AccessService.java` — login decision flow calls `TotpService.verify2faCode` and must preserve optional/force/disabled semantics.
- `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/{Queries.xml,Services.xml,Maintain.xml}` — descriptor entry points for QR list loading, secret generation, status updates, and verification.
- `awe-framework/awe-generic-screens/src/main/resources/application/awe/screen/security/{activate-2fa.xml,check-2fa.xml}` and user settings screens — QR and code-entry UI paths that must keep the same service contract.
- `awe-framework/awe-controller/src/test/java/com/almis/awe/service/{TotpServiceTest,AccessServiceTest}.java` — existing tests mock Sam Stevens types and need replacement with behavior-level tests.

### Approaches
1. **Keep Sam Stevens TOTP** — Leave the discontinued library in place and only document the risk.
   - Pros: No functional migration risk; smallest change.
   - Cons: Does not resolve GitLab #577; keeps an abandoned dependency and the starter workaround.
   - Effort: Low

2. **Spring Security OTT** — Replace with Spring Security One-Time Token support.
   - Pros: Uses Spring-maintained APIs.
   - Cons: OTT is not an RFC-6238 authenticator-app TOTP replacement; it does not preserve OTPAuth QR provisioning or existing stored TOTP secrets.
   - Effort: High

3. **`otp-java` + direct ZXing + AWE adapter** — Replace Sam Stevens with `com.github.bastiaanjansen:otp-java:2.1.0`, keep ZXing for PNG QR generation, and introduce AWE-owned adapter wiring.
   - Pros: Preserves authenticator-app TOTP model; removes discontinued Sam Stevens starter; centralizes defaults under AWE control; can keep descriptor/controller contracts stable.
   - Cons: Requires careful mapping of Base32 secret handling, SHA1/6-digit/30-second/tolerance semantics, OTPAuth URI output, QR image generation, and dependency management for transitive `commons-codec`.
   - Effort: Medium

4. **Internal RFC-6238 implementation** — Implement Base32 secret generation, HOTP/TOTP calculation, validation window, OTPAuth URI generation, and QR PNG generation directly in AWE.
   - Pros: Full control and no TOTP library lifecycle risk.
   - Cons: Higher security maintenance burden; more custom cryptographic code to test and audit; unnecessary unless library compatibility fails.
   - Effort: High

### Recommendation
Use `otp-java` + direct ZXing behind an AWE adapter. Keep the existing public AWE contract stable: persisted `secret2fa` values remain Base32 and valid, `/access/qr-code` still returns PNG, XML target actions keep their current names, and `AccessService` keeps the `disabled` / `optional` / `force` login semantics. Do not use Spring Security OTT for this issue because it solves a different problem than authenticator-app TOTP.

Acceptance criteria candidates for the proposal/spec:
- Existing Sam Stevens-generated Base32 secrets MUST continue validating against SHA1, 6 digits, 30 seconds, and ±1 time-step tolerance.
- New generated secrets MUST be Base32 and usable by common authenticator apps through an OTPAuth URI/QR code.
- `/access/qr-code` MUST continue returning `image/png` for the authenticated user's stored secret.
- `awe.security.totp.enabled=disabled|optional|force`, `initial-screen`, and `activate-screen` behavior MUST remain unchanged.
- The dependency graph MUST remove Sam Stevens TOTP artifacts and directly manage any replacement transitive dependencies that are security-sensitive or outdated, especially `commons-codec` if `otp-java` is adopted.
- Tests SHOULD include fixed-time TOTP vectors, previous/current/next time-step validation, rejection outside the tolerance window, OTPAuth URI content, QR PNG generation, and login routing for disabled/optional/force modes.

### Risks
- Silent incompatibility in Base32 normalization, padding, issuer/label encoding, or validation window could lock out users with existing 2FA secrets.
- `otp-java` depends on older `commons-codec:1.15`; AWE should manage a vetted version explicitly if this option is selected.
- QR generation will move from Sam Stevens abstractions to AWE/ZXing code, so MIME type, PNG byte generation, and error handling must be covered by tests.
- Some current unit tests mock library types rather than TOTP behavior; migration must add behavior-level tests to catch semantic drift.
- Current documentation has at least one apparent property-name typo (`awe.totp.security.enabled`) while code uses `awe.security.totp.enabled`; docs may need follow-up in a later phase if in scope.

### Ready for Proposal
Yes — propose replacing Sam Stevens with `otp-java` + direct ZXing via an AWE-owned adapter, while explicitly rejecting Spring Security OTT as non-equivalent and making backward compatibility for existing `secret2fa` records the primary requirement.
