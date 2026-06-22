# Design: Replace TOTP Library

## Technical Approach

Replace Sam Stevens runtime wiring with an AWE-owned TOTP boundary in `awe-controller`. `TotpService` keeps the public AWE service methods used by XML descriptors and controllers, but delegates cryptographic work to a small internal adapter backed by `otp-java`. QR PNG generation moves to AWE code using direct ZXing. Existing login, activation, settings, `/access/qr-code`, and XML contracts remain stable.

## Architecture / Components

```
XML queries/maintain ──→ TotpService ──→ AweTotpOperations
                              │              ├─ otp-java: secret/TOTP/OTPAuth URI
TotpController /access/qr-code ┘              └─ ZXing: PNG bytes
AccessService ───────────────→ TotpService.verify2faCode(...)
```

| Component | Responsibility |
|-----------|----------------|
| `TotpService` | Preserve current AWE methods: `getQRCode`, `getQRCodeList`, `generate2faSecret`, `update2faStatus`, `verify2faCode`. |
| `AweTotpOperations` | Internal interface for secret generation, code generation/verification, OTPAuth URI creation, and QR PNG rendering. |
| `OtpJavaTotpOperations` | Uses `otp-java` defaults explicitly configured as SHA1, 6 digits, 30s period, ±1 step verification window. |
| `ZxingQrPngGenerator` | Encodes OTPAuth URI to PNG with ZXing and throws AWE runtime/configuration errors on generation failure. |

## Architecture Decisions

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Keep public `TotpService` and hide replacement behind an internal adapter | Slightly more classes, but isolates third-party APIs | Chosen to preserve XML/controller contracts and remove Sam Stevens bean types from the runtime graph. |
| Use `otp-java` for TOTP and direct ZXing for QR | Requires compatibility tests and AWE QR error handling | Chosen because it preserves authenticator-app RFC-6238 behavior without Sam Stevens auto-configuration. |
| Fail fast with no Sam Stevens fallback | Operationally stricter during rollout | Chosen because fallback would keep the discontinued dependency and hide incompatibilities. |

## Data / Contracts

- Stored data remains `ope.secret2fa`: Base32-encoded secret string; no schema change.
- TOTP contract: SHA1, 6 digits, 30-second period, ±1 time-step tolerance.
- OTPAuth URI contract: `otpauth://totp/{username}?secret=...&issuer={issuer}&algorithm=SHA1&digits=6&period=30` with compatible URL encoding, preserving the historical account-only path plus issuer query behavior.
  - Controller contract remains `GET /access/qr-code`, `produces = image/png`, **`no-store` / `private` / `must-revalidate` cache control** plus `Pragma: no-cache` and `Expires: 0` headers (intentional security behavior: the PNG response carries the TOTP secret and must never be cached), `byte[]` body.
- XML descriptor contracts remain unchanged: `qr-code`, `force-qr-code`, `generate2faSecret`, `verify2faCode`, `update2faStatus`, `store2faSecret`.

## Dependency Changes

| File | Action | Description |
|------|--------|-------------|
| `awe-framework/awe-dependencies/pom.xml` | Modify | Replace `dev.samstevens.totp` management with `com.github.bastiaanjansen:otp-java:2.1.0`; keep/manage `com.google.zxing:javase`; explicitly manage `commons-codec`. |
| `awe-framework/awe-controller/pom.xml` | Modify | Replace `dev.samstevens.totp:totp` with `otp-java`; keep direct ZXing and commons-codec dependency where needed. |
| `awe-framework/awe-starters/awe-spring-boot-starter/pom.xml` | Modify | Remove `totp-spring-boot-starter`. |
| `SecurityConfig.java` | Modify | Remove `@Import(TotpAutoConfiguration.class)` and Sam Stevens bean parameters; wire `TotpService(AweTotpOperations)`. |
| `TotpController.java` | Modify | Remove `QrGenerationException` from the signature/import. |
| `TotpService.java` | Modify | Delegate TOTP/QR work to AWE adapter; retain public methods and service behavior. |
| `awe-framework/awe-controller/src/main/java/com/almis/awe/service/totp/*` | Create | Internal adapter interfaces/implementations. |
| Tests in controller/starter modules | Modify/Create | Replace library-mock tests with behavior and wiring tests. |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Existing Base32 secret compatibility, fixed-time TOTP generation, current/previous/next step accepted, outside window rejected | Adapter tests with fixed `Clock` and known RFC-style vectors/secrets. |
| Unit | OTPAuth URI content and PNG bytes | Assert account-path compatibility, issuer query/encoding, secret/algorithm/digits/period, and PNG signature/decodability. |
| Service | `TotpService` XML-facing behavior | Keep row counts/columns, secret persistence call, user detail updates. |
| Integration/Wiring | No Sam Stevens beans/artifacts required | Starter context test without `TotpAutoConfiguration`; dependency tree check in verification. |
| Regression | Login/activation/settings flows | Existing `AccessServiceTest` coverage for disabled/optional/force plus QR endpoint contract test. |

## Migration / Rollout

No data migration required. Deploy as an internal dependency/runtime replacement. Startup or first use MUST fail fast for invalid secrets/configuration rather than silently accepting altered semantics. Validate against representative stored Base32 secrets before release.

## Risk Mitigations

- Lock semantics in tests before code replacement.
- Treat OTPAuth account path and issuer query preservation as compatibility-only; do not change effective user identity.
- Remove all Sam Stevens imports and starter dependencies in the same work unit to avoid mixed graphs.
- Verify dependency resolution includes managed `otp-java`, ZXing, and commons-codec versions.

## Rollback Considerations

Rollback is a code/dependency revert only because persisted secrets and XML contracts remain unchanged. If production incompatibility is found, revert the replacement and add a compatibility test that reproduces the secret/configuration failure before retrying.

## Open Questions

- None. `commons-codec` is explicitly managed at `1.18.0` in the shipped dependency policy.
