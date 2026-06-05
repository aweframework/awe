# Tasks — REST OAuth2 Resource Server

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 520–900 (code + tests + docs) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (config+validator+converter) → PR 2 (security chain branching) → PR 3 (OpenAPI/docs) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

## Scope guards (must remain true)

- [x] No local `ope` provisioning or synchronization in REST OAuth2 mode.
- [x] No web session, redirect login, or `oauth2Login()` usage in REST security chain.
- [x] No fine-grained REST query/maintain authorization model added in this change.

## PR 1 — Configuration model + audience validator + JWT→AWE principal converter

### 1) RED: property binding/defaults tests for auth mode and OAuth2 settings
- [x] Add failing property-binding tests in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/config/AweRestConfigPropertiesTest.java` using `WebApplicationContextRunner`.
- [x] Cover defaults: `awe.rest.api.auth.mode=local-jwt` when unset; existing `awe.rest.api.jwt.*` still binds.
- [x] Cover OAuth2 bindings: issuer/jwk/audiences, principal claim lists, default profile, OpenAPI OAuth2 URLs/scopes/client-id/PKCE.
- [x] Add failing validation test: OAuth2 mode + empty audiences should fail context startup.

### 2) GREEN: implement auth-mode and nested OAuth2 properties
- [x] Update `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/AweRestConfigProperties.java` with nested classes for `auth`, `oauth2-resource-server.jwt`, `oauth2-resource-server.principal`, and `openapi.oauth2`.
- [x] Add `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/RestAuthenticationMode.java` (`local-jwt`, `oauth2-resource-server`) defaulting to `local-jwt`.
- [x] Keep backward-compatible `JWTProperties` usage at `awe.rest.api.jwt.*` in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/JWTProperties.java`.

### 3) TRIANGULATE: broaden property test matrix
- [x] Extend `AweRestConfigPropertiesTest` for both issuer-only and issuer+jwk configurations, multi-audience list acceptance, and OpenAPI OAuth2 optional/missing values behavior.

### 4) REFACTOR: stabilize property structure
- [x] Normalize naming and JavaDoc in `AweRestConfigProperties` so generated metadata/readability matches naming in `proposal.md` and `design.md`.

### 5) RED: audience validator tests
- [x] Add failing tests in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/AweRestAudienceValidatorTest.java`.
- [x] Cases: matching audience accepted, wrong audience rejected, missing `aud` rejected, any-match across configured audience list accepted.

### 6) GREEN: implement audience validator
- [x] Create `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/security/AweRestAudienceValidator.java` implementing `OAuth2TokenValidator<Jwt>`.
- [x] Return `invalid_token` failures with clear audience mismatch/missing messages.

### 7) TRIANGULATE: validator composition coverage
- [x] Add test coverage in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/JwtDecoderValidationCompositionTest.java` for `DelegatingOAuth2TokenValidator` composition (issuer/timestamp + audience).

### 8) REFACTOR: consolidate token validation wiring helpers
- [x] Extract reusable decoder/validator wiring helpers in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/RestSecurityConfiguration.java` (or adjacent helper class) to reduce branching complexity before PR 2.

### 9) RED: JWT→AWE synthetic principal converter tests
- [x] Add failing tests in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/security/AweRestJwtAuthenticationConverterTest.java`.
- [x] Cases: delegated token mapping (username/name/email), client-credentials mapping (`client:<id>`), profile claim override, default profile fallback, missing identity claims rejected.
- [x] Add explicit assertion that converter has no dependency on `UserDetailsService`/DAO/provisioning services.

### 10) GREEN: implement converter
- [x] Create `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/security/AweRestJwtAuthenticationConverter.java` producing an authentication principal of type `AweUserDetails` with AWE defaults from `BaseConfigProperties`/`SecurityConfigProperties`.
- [x] Populate compatibility authority `ROLE_<profile>` only (no new fine-grained REST authorization semantics).

### 11) TRIANGULATE: delegated vs machine-token edge claims
- [x] Expand converter tests for provider claim variants (`preferred_username`, `upn`, `azp`, `appid`, `client_id`, `sub`) and blank/null value handling.

### 12) REFACTOR: claim resolution utilities
- [x] Extract deterministic “first non-blank claim” utility in converter class/package and remove duplicated claim lookup code.

## PR 2 — SecurityFilterChain mode branching + `/api/authenticate` behavior

### 13) RED: security branch behavior tests
- [x] Add failing tests in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/RestSecurityConfigurationModeTest.java`.
- [x] Local mode assertions: JWT filters active, `/api/authenticate` permitted and keeps existing semantics.
- [x] OAuth2 mode assertions: JWT filters not added, protected endpoints require bearer auth via resource server, unauthorized responses are JSON `401` with no redirect.
- [x] OAuth2 mode assertion: `/api/authenticate` is local-JWT-only (disabled/unsupported in OAuth2 mode, and never used as OAuth2 exchange endpoint).

### 14) GREEN: implement mode branching in security chain
- [x] Update `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/RestSecurityConfiguration.java` to branch by `awe.rest.api.auth.mode`.
- [x] Keep local-jwt branch behavior backward-compatible (existing filters and `JWTTokenService`).
- [x] Add OAuth2 Resource Server branch with JWT decoder + `AweRestJwtAuthenticationConverter`, stateless session policy, and same public/protected endpoint shape.
- [x] Add dependency in `awe-framework/awe-starters/awe-rest-spring-boot-starter/pom.xml`: `spring-boot-starter-oauth2-resource-server`.

### 15) TRIANGULATE: endpoint matrix tests
- [x] Extend `RestSecurityConfigurationModeTest` with endpoint matrix coverage: `/api/public/data/**`, `/api/public/maintain/**`, `/api/data/**`, `/api/maintain/**`, `/api/maintain/async/**`, `/v3/api-docs/**`, `/swagger-ui/**`.

### 16) REFACTOR: security config decomposition
- [x] Split `RestSecurityConfiguration` into focused private methods (common chain setup vs local-jwt vs oauth2-resource-server) to keep reviewable diff size and reduce regression risk.

## PR 3 — OpenAPI mode-aware security schemes + docs/config examples

### 17) RED: OpenAPI mode-aware tests
- [x] Add failing tests in `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/test/java/com/almis/awe/rest/autoconfigure/OpenAPIConfigurationTest.java`.
- [x] Local mode: `bearerAuth` remains HTTP bearer JWT.
- [x] OAuth2 mode + metadata present: `bearerAuth` becomes OAuth2 authorizationCode (+ scopes).
- [x] OAuth2 mode without metadata: fallback to HTTP bearer with external-token guidance.

### 18) GREEN: implement OpenAPI branching
- [x] Update `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/OpenAPIConfiguration.java` to build security schemes programmatically from `AweRestConfigProperties` while retaining scheme name `bearerAuth`.
- [x] Configure Swagger UI OAuth client settings (client-id/PKCE) only when provided by properties.

### 19) TRIANGULATE: OpenAPI compatibility checks
- [x] Add assertions that existing controller `@SecurityRequirement(name = "bearerAuth")` mappings continue to work unchanged in both modes.

### 20) REFACTOR: isolate OpenAPI scheme builders
- [x] Extract private builder methods for local-jwt scheme, oauth2 scheme, and fallback scheme in `OpenAPIConfiguration` to keep behavior explicit and testable.

### 21) RED/GREEN docs and examples task
- [x] Update `website/docs/rest-module.md` with two mode sections: local-jwt (default) and OAuth2 Resource Server (opt-in), including explicit statement that `/api/authenticate` is local-jwt-only.
- [x] Add provider-agnostic property examples (issuer, jwk, audiences, claim mapping, OpenAPI OAuth2 URLs/scopes) and quick-start examples for delegated-user Swagger testing plus client-credentials testing via curl/Postman.
- [x] Mirror doc updates in current versioned docs file `website/versioned_docs/version-4.11.0/rest-module.md`.

## Verification commands (targeted to REST modules)

Run during each PR and again in final verify:

- `mvn test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am`
- `mvn test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -Dtest=AweRestConfigPropertiesTest,AweRestAudienceValidatorTest,AweRestJwtAuthenticationConverterTest,RestSecurityConfigurationModeTest,OpenAPIConfigurationTest`
- `mvn test -pl awe-framework/awe-modules/awe-rest -am`

Optional broader regression after stacked chain merge:

- `mvn verify -pl awe-framework/awe-starters/awe-rest-spring-boot-starter,awe-framework/awe-modules/awe-rest -am`

## Session-sized boundaries and rollback points

- PR 1 rollback boundary: remove new OAuth2 properties/validator/converter classes and tests; local-jwt path untouched.
- PR 2 rollback boundary: revert `RestSecurityConfiguration` branching and starter dependency; restore always-local behavior.
- PR 3 rollback boundary: revert OpenAPI/doc changes only; security behavior remains from PR 1–2.
