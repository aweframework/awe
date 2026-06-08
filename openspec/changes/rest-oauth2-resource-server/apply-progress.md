# Apply Progress — rest-oauth2-resource-server

## Completed tasks (PR 1 / Slice A)

- [x] Scope guards validated for this slice:
  - [x] No local `ope` provisioning or synchronization in REST OAuth2 mode.
  - [x] No web session, redirect login, or `oauth2Login()` usage added.
  - [x] No fine-grained REST query/maintain authorization model added.
- [x] Tasks 1–4: auth mode/property model tests + implementation + expanded matrix + property refactor.
- [x] Tasks 5–8: audience validator tests + implementation + validator composition test + reusable composition helper.
- [x] Tasks 9–12: JWT→AWE synthetic principal converter tests + implementation + claim-variant coverage + claim resolution helper logic.

## Completed tasks (PR 2 / Slice B)

- [x] Tasks 13–16: security branch RED/GREEN tests, mode-aware `SecurityFilterChain` branching, endpoint matrix triangulation, and refactor decomposition.
- [x] `/api/authenticate` is local-jwt-only (unsupported in oauth2 mode).
- [x] OAuth2 mode does not register local JWT filters and remains stateless.

## Completed tasks (PR 3 / Slice C)

- [x] Tasks 17–20: OpenAPI mode-aware RED/GREEN tests and implementation using a programmatic `bearerAuth` security scheme by mode.
- [x] Local JWT mode keeps `bearerAuth` as HTTP bearer JWT.
- [x] OAuth2 Resource Server mode exposes `bearerAuth` as OAuth2 authorizationCode flow when authorization/token URLs are configured.
- [x] OAuth2 mode without metadata falls back to HTTP bearer external-token guidance.
- [x] Swagger UI OAuth client-id + PKCE customization is applied when client-id is configured (no browser-held client secret handling added).
- [x] Task 21 docs/examples updated in both current and versioned docs.

## TDD Cycle Evidence

| Area | RED (failing evidence first) | GREEN (fix) | TRIANGULATE | REFACTOR |
|---|---|---|---|---|
| Properties/auth mode | Added new config tests; first run failed with missing test dependencies (`junit-jupiter`, `context runner`) and missing new config classes/properties. | Added module test dependency and implemented `RestAuthenticationMode` plus nested auth/oauth2/openapi property model with validation. | Added issuer+jwk and optional OpenAPI OAuth2 bindings scenarios. | Stabilized nested property defaults and validation messages while preserving legacy `awe.rest.api.jwt.*` binding. |
| Audience validator | Added validator tests before validator class existed. | Implemented `AweRestAudienceValidator`. | Added composition behavior test (`JwtDecoderValidationCompositionTest`). | Extracted reusable composition helper `AweRestJwtValidationFactory`. |
| JWT→AWE converter | Added converter tests before converter class existed. | Implemented `AweRestJwtAuthenticationConverter` with synthetic principal mapping and no user/provisioning service dependencies. | Added edge-claim coverage (`upn`, `azp`, `client_id`) and blank-claim handling tests. | Consolidated deterministic claim resolution via `firstNonBlankClaim` and `asString` helpers. |
| Security mode branching | Added `RestSecurityConfigurationModeTest` first. Initial RED failures covered missing test scaffolding (`HttpSecurity`/MVC introspector/base config bindings), and then a behavior failure proving oauth2 mode still registered local JWT filters. | Implemented mode branching in `RestSecurityConfiguration` for `local-jwt` vs `oauth2-resource-server`, disabled `/api/authenticate` in oauth2 mode, and wired Resource Server JWT decoder + synthetic principal converter. | Expanded endpoint matrix checks in `RestSecurityConfigurationModeTest` across public/protected/docs/authenticate routes. | Refactored `RestSecurityConfiguration` into focused private methods: common chain, local-jwt branch, oauth2 branch, decoder builder, mode predicate. |
| OpenAPI mode-aware schemes | Added `OpenAPIConfigurationTest` first and verified RED compile/test failures before implementation (`swaggerUiOAuthCustomizer` missing, then fallback-description assertion mismatch). | Implemented programmatic `bearerAuth` scheme selection in `OpenAPIConfiguration` and added Swagger UI OAuth client-id/PKCE customization hook. | Added mode matrix assertions for local JWT, OAuth2 with metadata, OAuth2 fallback, plus compatibility check that scheme name remains `bearerAuth`. | Extracted explicit private scheme-builder methods (`buildLocalJwtScheme`, `buildOAuth2AuthorizationCodeScheme`, `buildExternalBearerScheme`) and metadata predicate helper. |

## Files changed

- `awe-framework/awe-starters/awe-rest-spring-boot-starter/pom.xml`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/AweRestConfigProperties.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/RestAuthenticationMode.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/security/AweRestAudienceValidator.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/security/AweRestJwtAuthenticationConverter.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/security/AweRestJwtValidationFactory.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/config/AweRestConfigPropertiesTest.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/AweRestAudienceValidatorTest.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/AweRestJwtAuthenticationConverterTest.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/JwtDecoderValidationCompositionTest.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/RestSecurityConfiguration.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/OpenAPIConfiguration.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/RestSecurityConfigurationModeTest.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/OpenAPIConfigurationTest.java`
- `website/docs/rest-module.md`
- `website/versioned_docs/version-4.11.0/rest-module.md`
- `openspec/changes/rest-oauth2-resource-server/tasks.md`
- `openspec/changes/rest-oauth2-resource-server/apply-progress.md`

## Test commands run

- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -Dtest=AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,AweRestJwtAuthenticationConverterTest` ❌ (expected RED setup: unresolved local snapshot dependencies without `-am`).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,AweRestJwtAuthenticationConverterTest -Dsurefire.failIfNoSpecifiedTests=false` ❌ (RED compile failures, then GREEN iterations).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=AweRestJwtAuthenticationConverterTest -Dsurefire.failIfNoSpecifiedTests=false` ❌ (intermediate converter expectation mismatch during TDD).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,JwtDecoderValidationCompositionTest,AweRestJwtAuthenticationConverterTest -Dsurefire.failIfNoSpecifiedTests=false` ✅ (all PR1 tests pass).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=RestSecurityConfigurationModeTest -Dsurefire.failIfNoSpecifiedTests=false` ❌ (RED progression: failing with local JWT filters still present in oauth2 mode).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=RestSecurityConfigurationModeTest -Dsurefire.failIfNoSpecifiedTests=false` ✅ (GREEN/TRIANGULATE after branching + endpoint matrix).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,JwtDecoderValidationCompositionTest,AweRestJwtAuthenticationConverterTest,RestSecurityConfigurationModeTest -Dsurefire.failIfNoSpecifiedTests=false` ✅ (PR1+PR2 focused suite passes: 22 tests).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=OpenAPIConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false` ❌ (RED: missing OpenAPI customizer method, then assertion mismatch).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=OpenAPIConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false` ✅ (GREEN/TRIANGULATE for OpenAPI mode-aware behavior).
- `mvn -s ~/.m2/settings-direct.xml test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am -Dtest=OpenAPIConfigurationTest,AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,JwtDecoderValidationCompositionTest,AweRestJwtAuthenticationConverterTest,RestSecurityConfigurationModeTest -Dsurefire.failIfNoSpecifiedTests=false` ✅ (final focused verification: 26 tests, 0 failures).

## Deviations from design

- Added `AweRestJwtValidationFactory` helper class in PR1 (instead of waiting for PR2 wiring) to satisfy validator-composition TRIANGULATE coverage while keeping security-chain behavior untouched.
- Kept provider claim defaults where client display name may come from `appid`/`azp` in client-credentials tokens.

## Remaining tasks

- None for this internal slice chain (tasks 1–21 completed).

## Workload / PR boundary

- Completed PR1 + PR2 + PR3 internal slices (tasks 1–21).
- Delivery strategy remains one final MR into `develop` with internal slice commits.
