# TOTP Authentication Specification

## Purpose

Define AWE authenticator-app TOTP provisioning and verification while replacing the discontinued Sam Stevens library without changing the user-visible 2FA flow. Also establishes the authorization policy for the 2FA enrollment bootstrap and QR code endpoint across all TOTP modes.

## Requirements

### Requirement: Preserve existing 2FA secrets transparently

The system MUST continue to accept stored `ope.secret2fa` values without reactivation or data migration. It MUST preserve Base32 secrets, SHA1, 6 digits, 30-second periods, and ±1 time-step tolerance for both existing and newly generated secrets. The system MUST fail fast on incompatible secret handling and MUST NOT fall back to Sam Stevens behavior.

#### Scenario: Existing user completes 2FA with a stored secret

- GIVEN a user already has a stored `ope.secret2fa` value
- AND the user enters a valid current TOTP code
- WHEN the code is verified
- THEN the system MUST accept the code using the preserved TOTP semantics
- AND no reactivation or data migration MUST be required

#### Scenario: Incompatible secret handling is rejected early

- GIVEN a stored secret cannot be interpreted with the preserved contract
- WHEN the system attempts to verify or provision TOTP data
- THEN the system MUST fail fast
- AND it MUST NOT switch to a Sam Stevens fallback path

### Requirement: Generate authenticator-compatible provisioning data

The system MUST generate OTPAuth URIs and PNG QR codes that common authenticator apps can import. Issuer and label normalization MAY be applied only when compatibility is preserved and existing behavior remains equivalent.

#### Scenario: QR provisioning includes compatible OTPAuth content

- GIVEN a user is entitled to view 2FA provisioning data
- WHEN the system generates the QR payload
- THEN the URI MUST represent the user, issuer, secret, algorithm, digits, and period expected by authenticator apps
- AND the PNG output MUST remain consumable by the existing QR endpoint

#### Scenario: Normalization does not change the effective account identity

- GIVEN issuer or label formatting requires normalization for app compatibility
- WHEN the system generates the OTPAuth data
- THEN the resulting code MUST still map to the same user account
- AND the observable provisioning behavior MUST remain equivalent

### Requirement: Preserve current login and screen flow

The system MUST preserve the existing 2FA login, activation, and settings flow, including `disabled`, `optional`, and `force` modes. It MUST NOT introduce visible UI or flow changes unless required for compatibility.

#### Scenario: Existing flow remains unchanged for enabled 2FA

- GIVEN 2FA is enabled for the user
- WHEN the user logs in and completes verification
- THEN the system MUST follow the same screen and client-action flow as before

#### Scenario: QR endpoint still serves the same image type

- GIVEN the UI requests provisioning for an authenticated user
- WHEN the QR image is returned
- THEN the response MUST remain `image/png`
- AND the response MUST include `Cache-Control: no-store, private, must-revalidate`, `Pragma: no-cache`, and `Expires: 0` headers because the PNG carries the TOTP secret
- AND existing screen contracts MUST continue to work

#### Scenario: Settings activation preserves private authentication after enabling 2FA

- GIVEN a user is already fully authenticated (non-2FA login)
- AND the user enables 2FA from the settings screen
- WHEN the system generates and stores the secret
- THEN the user's session MUST remain fully authenticated
- AND the in-memory principal MUST only be mutated AFTER the secret is successfully persisted

#### Scenario: Force/login activation does not bypass TOTP verification

- GIVEN a user without 2FA enabled reaches the force/login 2FA activation screen
- WHEN the system generates and stores a new secret
- THEN the session MUST NOT be set to fully authenticated before the user enters a valid TOTP code

### Requirement: Replace Sam Stevens runtime dependency with an AWE adapter

The system MUST route TOTP behavior through an internal AWE adapter backed by `otp-java` for TOTP logic and direct ZXing for QR PNG generation. It MUST explicitly manage sensitive transitive dependencies, especially `commons-codec`, and Sam Stevens artifacts MUST be absent from the resolved runtime graph.

#### Scenario: Build resolves without Sam Stevens artifacts

- GIVEN the project dependencies are resolved
- WHEN the TOTP capability is built or tested
- THEN no `dev.samstevens.totp` artifact MUST remain in the runtime dependency graph
- AND the replacement dependency set MUST be explicitly controlled by AWE

#### Scenario: Replacement semantics are validated before release

- GIVEN the new adapter cannot preserve the required TOTP contract
- WHEN validation is executed
- THEN the change MUST be rejected rather than shipping a partial fallback
- AND compatibility checks MUST cover existing secrets, OTPAuth generation, and QR output

### Requirement: Enforce FORCE-mode enrollment bootstrap authorization

When TOTP mode is FORCE, a user who does not have an existing secret MUST be redirected to the 2FA activation screen. The activation screen MUST be able to load the enrollment query (`force-qr-code`) and generate a secret. General private application access MUST remain denied until the user completes TOTP verification. The `force-qr-code` query MUST be marked `public="true"` in the descriptor, and the service layer MUST enforce that only users in a legitimate provisioning state can complete secret generation.

#### Scenario: FORCE enrollment bootstrap query is accessible during pending enrollment

- GIVEN a user is in FORCE mode with no TOTP secret (pendingTotpEnrollment=true)
- WHEN the activate-2fa screen autoloads the force-qr-code query
- THEN the query MUST be accessible (not blocked by the private-query authentication gate)
- AND the service MUST generate and store a new TOTP secret
- AND pendingTotpEnrollment MUST be cleared after successful secret generation
- AND freshEnrollment MUST be set so the user can retrieve the QR image

#### Scenario: General private access remains blocked during pending enrollment

- GIVEN a user is in FORCE mode with pendingTotpEnrollment=true
- WHEN the user attempts to access a private application resource
- THEN AweSession.isAuthenticated() MUST return false
- AND the request MUST be denied by the private-query gate

#### Scenario: FORCE enrollment does not bypass TOTP verification

- GIVEN a user has completed secret generation (pendingTotpEnrollment cleared, freshEnrollment=true)
- WHEN the user is redirected to the TOTP verification screen
- THEN fullyAuthenticated MUST still be false
- AND only after submitting a valid TOTP code via verify2faCode MUST the session be elevated to fully-authenticated

### Requirement: Enforce fail-closed QR code endpoint authorization

The `/access/qr-code` endpoint and the underlying `TotpService.getQRCode()` MUST only be reachable in two explicitly allowed states: fully authenticated (settings activation flow) or fresh enrollment window (freshEnrollment=true set by generate2faSecret). All other states MUST be denied with an explicit AWException, not by accidental runtime failure (e.g., null-secret NPE).

#### Scenario: QR access denied for pending-enrollment user before secret generation

- GIVEN a user has pendingTotpEnrollment=true and no secret yet
- WHEN getQRCode() is called (e.g., by direct URL access)
- THEN the service MUST throw an explicit AWException before reaching the QR generator
- AND the error MUST NOT surface as a NullPointerException or uncontrolled runtime error

#### Scenario: QR access denied for enrolled user not yet TOTP-verified (re-login)

- GIVEN a user has an existing 2FA secret (enabled2fa=true)
- AND the user has logged in but not yet passed TOTP verification (fullyAuthenticated=false, freshEnrollment=false)
- WHEN getQRCode() is called
- THEN the service MUST throw an explicit AWException

#### Scenario: QR access allowed for fresh enrollment (after generate2faSecret)

- GIVEN a user has just completed secret generation in the enrollment flow
- AND freshEnrollment=true
- WHEN getQRCode() is called
- THEN the QR PNG MUST be returned

#### Scenario: QR access allowed for fully authenticated settings user

- GIVEN a user is fully authenticated (fullyAuthenticated=true)
- WHEN getQRCode() is called
- THEN the QR PNG MUST be returned

### Requirement: Production-visible signal on fail-fast TOTP/QR runtime failures

TOTP verification failures, QR generation failures, and secret storage failures MUST produce a log entry visible to operators. TOTP verification failure MUST log at WARN level. QR generation or secret storage failures MUST log at ERROR level. The log entry MUST include the username and failure message to support incident investigation.

#### Scenario: TOTP verification failure is logged

- GIVEN a user submits an invalid TOTP code
- WHEN verify2faCode returns false
- THEN a WARN-level log entry MUST be emitted containing the username

#### Scenario: QR generation failure is logged

- GIVEN the QR generator throws a runtime exception
- WHEN getQRCode() is called
- THEN an ERROR-level log entry MUST be emitted containing the username and error message
- AND the exception MUST be re-thrown as an AWException

#### Scenario: Secret storage failure is logged

- GIVEN the persist-secret maintain call throws an exception
- WHEN generate2faSecret is called
- THEN an ERROR-level log entry MUST be emitted containing the username and error message
- AND the principal MUST NOT be mutated (session state preserved from before the call)
