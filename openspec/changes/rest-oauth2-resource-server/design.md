# Design: REST OAuth2 Resource Server support

## Context and current state

The current `awe-rest` API security is local-JWT-only:

- `RestSecurityConfiguration` protects `/api/**`, `/v3/api-docs/**`, and `/swagger-ui/**` with one `SecurityFilterChain`.
- `/api/authenticate`, Swagger docs, and Swagger UI are `permitAll`; public query/maintain endpoints use `PublicQueryMaintainAuthorization`; all other API endpoints require authentication.
- Local JWT behavior is implemented by adding `JWTAuthenticationFilter` for `/api/authenticate` and `JWTAuthorizationFilter` for bearer-token verification to the chain unconditionally.
- `JWTAuthorizationFilter` verifies AWE-issued HMAC tokens through `JWTTokenService`, then loads a persisted local user through `UserDetailsService`.
- `JWTAuthenticationEntryPoint` already returns a JSON `401` body instead of redirecting.
- `OpenAPIConfiguration` statically declares `bearerAuth` as HTTP bearer JWT. Protected controller operations reference `@SecurityRequirement(name = "bearerAuth")`.
- `AweUserDetails` already contains the fields REST downstream code is likely to need: username, name, email, profile/profileName, restrictions, language, theme, initialScreen, enabled/expired/locked flags, authorities, and `fullyAuthenticated`.

The OAuth2 Resource Server mode must be REST-only and must not reuse the web SSO `oauth2Login()` / provisioning path in `AccessService` or `AweUserDetailService`.

## Design goals

1. Preserve current local JWT behavior by default.
2. Add an opt-in Resource Server branch for externally issued JWT access tokens.
3. Keep REST stateless: no sessions, no redirects, no `oauth2Login()`.
4. Require issuer/signature/expiration validation plus explicit audience validation.
5. Convert validated token claims into a minimal synthetic `AweUserDetails` without local lookup or provisioning.
6. Keep existing public/protected endpoint shape and v1 authorization scope.
7. Make Swagger/OpenAPI security documentation mode-aware while keeping controller annotations stable where possible.

## Maven dependency placement

Add Spring OAuth2 Resource Server support to the REST starter, not the core controller module:

- Add `org.springframework.boot:spring-boot-starter-oauth2-resource-server` to:
  - `awe-framework/awe-starters/awe-rest-spring-boot-starter/pom.xml`
- Do not declare an explicit version; `awe-dependencies` imports `spring-boot-dependencies` (`3.5.9`), which manages the starter and its Spring Security OAuth2/Jose transitive dependencies.
- Keep `com.auth0:java-jwt` in `awe-framework/awe-modules/awe-rest/pom.xml` for the existing local JWT implementation.
- Prefer placing new Resource Server-specific validator/converter classes in the REST starter package (for example `com.almis.awe.rest.autoconfigure.security`) so `awe-rest` does not need a direct OAuth2 Resource Server compile dependency. If implementation chooses to place reusable security classes under `awe-modules/awe-rest`, then add the same OAuth2 Resource Server/Jose dependency to `awe-rest` as well.
- For tests, add only targeted test dependencies required by the selected test style. If MockMvc/security test helpers are used directly in the starter tests, add `org.springframework.security:spring-security-test` with `test` scope to the starter or reuse existing project test infrastructure if already inherited.

## Configuration model

Extend `AweRestConfigProperties` under the existing prefix `awe.rest.api`. Existing `awe.rest.api.jwt.*` properties remain local-JWT settings.

### Authentication mode

Introduce an enum, e.g. `RestAuthenticationMode`, bound at:

```yaml
awe:
  rest:
    api:
      auth:
        mode: local-jwt # local-jwt | oauth2-resource-server
```

Behavior:

- Default: `local-jwt`.
- `local-jwt`: current filters and `/api/authenticate` semantics remain active.
- `oauth2-resource-server`: local JWT filters are not registered; Spring Security Resource Server JWT validation is configured for the REST chain.

### Resource Server JWT validation properties

Use AWE REST-specific properties instead of relying on global `spring.security.oauth2.resourceserver.*`, to avoid coupling REST API admission to web SSO or other application security.

```yaml
awe:
  rest:
    api:
      oauth2-resource-server:
        jwt:
          issuer-uri: https://issuer.example.com/realms/app
          jwk-set-uri: https://issuer.example.com/realms/app/protocol/openid-connect/certs
          audiences:
            - api://awe-rest
```

Rules:

- In `oauth2-resource-server` mode, `audiences` MUST be non-empty. Startup should fail fast if no audience is configured.
- At least one of `issuer-uri` or `jwk-set-uri` MUST be configured in OAuth2 mode.
- If `issuer-uri` is configured, use Spring Security issuer-based decoder support (`JwtDecoders.fromIssuerLocation` or equivalent) to get issuer validation and JWK discovery.
- If only `jwk-set-uri` is configured, build a `NimbusJwtDecoder` from that JWK Set URI. In this case, issuer validation is available only if `issuer-uri` is also configured; otherwise fail fast unless the implementation deliberately supports JWK-only deployments as an advanced mode. The preferred v1 behavior is to require `issuer-uri` for issuer validation and allow `jwk-set-uri` as an override for key discovery.
- Audience validation is always AWE-specific and is composed with Spring's default issuer/timestamp validators.
- `audiences` is an allow-list: a token is accepted if its `aud` claim intersects at least one configured audience.

### Principal claim mapping properties

```yaml
awe:
  rest:
    api:
      oauth2-resource-server:
        principal:
          delegated-username-claims:
            - preferred_username
            - upn
            - email
            - oid
            - sub
          delegated-display-name-claims:
            - name
            - preferred_username
            - email
          delegated-email-claims:
            - email
            - upn
            - preferred_username
          client-id-claims:
            - azp
            - appid
            - client_id
            - clientId
            - sub
          client-display-name-claims:
            - app_displayname
            - azp
            - appid
            - client_id
            - clientId
          client-principal-prefix: "client:"
          profile-claim: null
          default-profile: null
```

Defaults and semantics:

- Delegated-user tokens are preferred when any configured delegated username claim is present.
- Client-credentials tokens are detected when no delegated username claim is present and a configured client id claim is present.
- `profile-claim` is disabled by default. If configured and present/non-blank in the validated token, its value is used as both `AweUserDetails.profile` and `profileName`.
- If `profile-claim` is absent, blank, or not configured, use `default-profile` when set; otherwise use `BaseConfigProperties.defaultRole`.
- No REST OAuth2 mode local profile validation is performed in v1, because that would require local user/profile lookup and introduces provisioning-like coupling. Administrators should configure `profile-claim` only for trusted issuers/audiences.
- Authorities should be minimal and compatible with existing AWE expectations: add `ROLE_<profile>` (matching current `AweUserDetailService#getAuthorities`) and do not derive new REST read/write authorization from scopes/roles in v1.
- Defaults for non-identity fields come from existing AWE config:
  - `restrictions`: `SecurityConfigProperties.defaultRestriction`
  - `language`: `BaseConfigProperties.languageDefault`
  - `theme`: `BaseConfigProperties.theme`
  - `initialScreen`: `BaseConfigProperties.screen.initial`
  - enabled/account flags: enabled, non-expired, non-locked, credentials non-expired
  - `fullyAuthenticated`: true

### Swagger/OpenAPI OAuth2 properties

Keep OpenAPI configuration under `awe.rest.api` and make it mode-aware:

```yaml
awe:
  rest:
    api:
      openapi:
        oauth2:
          authorization-url: https://issuer.example.com/oauth2/v2.0/authorize
          token-url: https://issuer.example.com/oauth2/v2.0/token
          client-id: awe-swagger-ui
          use-pkce: true
          scopes:
            api://awe-rest/access: Access AWE REST API
```

Rules:

- Local JWT mode preserves the existing HTTP bearer JWT scheme behavior.
- OAuth2 mode should expose an OAuth2 security scheme only when `authorization-url` and `token-url` are configured.
- Authorization code with PKCE is the preferred Swagger UI flow for delegated-user testing.
- Do not configure or encourage a browser-held client secret.
- Client-credentials testing may be documented for curl/Postman rather than implemented in Swagger UI.
- If OAuth2 mode is enabled but OpenAPI OAuth2 URLs are missing, fall back to documenting an HTTP bearer scheme that lets users paste an already obtained access token.

## SecurityFilterChain branching

Refactor `RestSecurityConfiguration#restFilterChain` into common setup plus mode branches.

Common setup for both modes:

- `securityMatcher(API_URL_LIST)` remains scoped to `/api/**`, `/v3/api-docs/**`, `/swagger-ui/**`.
- CSRF disabled for API/docs paths.
- `SessionCreationPolicy.STATELESS`.
- JSON authentication entry point for unauthenticated protected requests.
- Optional JSON access-denied handler for `403` responses (especially useful when `/api/authenticate` is disabled in OAuth2 mode).
- Swagger endpoints remain `permitAll`.
- Public query/maintain endpoints keep `PublicQueryMaintainAuthorization`.
- Protected `/api/data/**`, `/api/maintain/**`, and `/api/maintain/async/**` require authenticated principal.

Local JWT branch (`auth.mode=local-jwt`):

- `/api/authenticate` remains `permitAll`.
- Register `JWTAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- Register `JWTAuthorizationFilter` before `UsernamePasswordAuthenticationFilter`.
- Keep `JWTTokenService` bean and `awe.rest.api.jwt.*` behavior unchanged.
- Do not configure `.oauth2ResourceServer()` in this branch.

OAuth2 Resource Server branch (`auth.mode=oauth2-resource-server`):

- Do not add `JWTAuthenticationFilter` or `JWTAuthorizationFilter`.
- Do not use `AuthenticationManager` or `UserDetailsService` for OAuth2 token admission.
- Configure `http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> ...))` with:
  - an AWE REST `JwtDecoder` containing issuer/timestamp/audience validators,
  - a JWT authentication converter that returns an `Authentication` whose principal is synthetic `AweUserDetails`.
- `/api/authenticate` is local-JWT-only and should be disabled in this branch, preferably by returning a JSON `403`/`404` style unsupported response through security handling. It must not call the existing controller path because that method expects the local JWT authentication filter to have written a local JWT response header.
- No `oauth2Login()`, no form login, no redirect entry point, no web SSO success/provisioning handlers.

## Audience validator design

Create a small validator, e.g. `AweRestAudienceValidator implements OAuth2TokenValidator<Jwt>`.

Inputs:

- `Collection<String> acceptedAudiences` from `awe.rest.api.oauth2-resource-server.jwt.audiences`.

Validation:

1. If `acceptedAudiences` is empty in OAuth2 mode, fail configuration before serving requests.
2. On each token, read `jwt.getAudience()`.
3. Reject when the token audience list is empty or has no intersection with configured audiences.
4. Return `OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token audience is not accepted", null))` for failures.
5. Pass when any configured audience is present in the token.

Decoder composition:

- For issuer-based setup, compose Spring's default issuer/timestamp validator with `AweRestAudienceValidator` using `DelegatingOAuth2TokenValidator`.
- For JWK-only advanced setup, compose timestamp validation plus audience validation, and issuer validation if `issuer-uri` is present.

## JWT-to-AWE principal converter design

Create a converter, e.g. `AweRestJwtAuthenticationConverter`, used only by the Resource Server branch.

Contract:

- Input: a validated Spring Security `Jwt`.
- Output: `AbstractAuthenticationToken` such as `JwtAuthenticationToken`, with an `AweUserDetails` principal and minimal authorities.
- No database or local user service calls.
- No local user creation, synchronization, profile update, or `ope` provisioning.

Mapping algorithm:

1. Resolve token kind:
   - Delegated user if the first non-blank configured delegated username claim exists.
   - Otherwise client credentials if the first non-blank configured client id claim exists.
   - Otherwise fallback to `sub`; if even `sub` is blank, reject conversion with an authentication exception.
2. Delegated user fields:
   - `username`: first non-blank delegated username claim.
   - `name`: first non-blank display name claim, fallback to username.
   - `email`: first non-blank email claim, fallback to username only if it looks email-like.
   - `dn`: optional issuer/subject composite such as `<iss>#<sub>` for traceability if useful.
3. Client credentials fields:
   - `username`: `client-principal-prefix + clientId`.
   - `name`: first non-blank client display name claim, fallback to username.
   - `email`: null.
   - `dn`: optional issuer/subject composite.
4. AWE defaults:
   - `profile` and `profileName`: configured trusted profile claim, else configured `default-profile`, else `BaseConfigProperties.defaultRole`.
   - `restrictions`: `SecurityConfigProperties.defaultRestriction`.
   - `language`: `BaseConfigProperties.languageDefault`.
   - `theme`: `BaseConfigProperties.theme`.
   - `initialScreen`: `BaseConfigProperties.screen.initial`.
   - security flags: account non-expired, account non-locked, credentials non-expired, enabled.
   - `fullyAuthenticated`: true.
   - no password, no 2FA secret, `enabled2fa=false`.
5. Authorities:
   - Add `ROLE_<profile>` for compatibility with existing AWE principal expectations.
   - Optionally keep raw JWT scopes/roles out of authorities in v1 to avoid creating accidental authorization semantics.

Downstream behavior:

- REST query/maintain services see an authenticated AWE-compatible principal.
- Existing public/protected endpoint checks remain unchanged.
- Any downstream code that assumes a persisted local user may still be a risk; tests should cover the common query/maintain admission path and principal fields.

## Swagger/OpenAPI mode-aware design

Refactor `OpenAPIConfiguration` from static annotation-only security to programmatic `OpenAPI` components.

Recommended approach:

- Keep the security scheme name `bearerAuth` because controller annotations already reference it.
- In local JWT mode, define `bearerAuth` as:
  - type: HTTP
  - scheme: bearer
  - bearerFormat: JWT
  - description: AWE local JWT obtained from `/api/authenticate`.
- In OAuth2 mode with `authorization-url` and `token-url`, define `bearerAuth` as:
  - type: OAuth2
  - flow: authorizationCode
  - authorizationUrl/tokenUrl from properties
  - scopes from properties
  - description: external provider access token for AWE REST Resource Server.
- In OAuth2 mode without OAuth2 flow URLs, define `bearerAuth` as HTTP bearer with description explaining that tokens must be obtained externally.
- Configure Swagger UI OAuth client id and PKCE when properties are set. Prefer Springdoc's `SwaggerUiConfigProperties` customization if available in the current version.
- Mark or describe `/api/authenticate` as local-JWT-only. In OAuth2 mode, it is not the OAuth2 token endpoint.

## Testing strategy and strict TDD RED/GREEN plan

Follow strict TDD in apply: write failing tests first, commit/verify small increments, then implement.

### RED 1: Property binding and defaults

Add tests for `AweRestConfigProperties` binding:

- default `auth.mode` is `local-jwt`.
- OAuth2 mode binds `issuer-uri`, `jwk-set-uri`, `audiences`, principal claim lists, profile claim, and OpenAPI OAuth2 settings.
- OAuth2 mode with empty audiences fails validation/configuration.

Expected initial result: tests fail because properties/enums do not exist.

### GREEN 1: Add properties/enums

Implement `RestAuthenticationMode` and nested properties with validation. No behavior change yet.

### RED 2: Audience validator

Unit tests for `AweRestAudienceValidator`:

- accepts token with matching audience.
- rejects wrong audience.
- rejects missing audience.
- accepts any of multiple configured audiences.

Expected initial result: validator class missing.

### GREEN 2: Implement validator

Add validator and decoder composition tests where practical with a mocked or constructed `JwtDecoder` path.

### RED 3: Principal converter

Unit tests for `AweRestJwtAuthenticationConverter`:

- delegated token maps username/name/email/profile/defaults correctly.
- client-credentials token maps `client:<id>`, no email, defaults correctly.
- profile claim overrides default only when configured and present.
- converter does not call `UserDetailsService` or DAO (constructor should not accept those dependencies).
- missing identity claims causes authentication failure.

Expected initial result: converter class missing.

### GREEN 3: Implement converter

Create the converter and ensure it builds `AweUserDetails` from `BaseConfigProperties` and `SecurityConfigProperties` defaults.

### RED 4: Security chain mode branching

Use targeted Spring context/MockMvc tests around `RestSecurityConfiguration`:

- local default mode registers/uses local JWT filters and preserves `/api/authenticate` admission.
- OAuth2 mode does not register local JWT filters.
- OAuth2 mode configures Resource Server JWT support with the custom converter.
- protected endpoint without token returns JSON `401` and does not redirect.
- `/api/authenticate` in OAuth2 mode is disabled/unsupported and does not invoke local JWT authentication.
- public query/maintain authorization wiring remains present.

Expected initial result: branch support missing and filters always present.

### GREEN 4: Implement security branch

Refactor `RestSecurityConfiguration` with minimal line churn and keep local behavior unchanged.

### RED 5: OpenAPI mode awareness

Tests for `OpenAPIConfiguration`:

- local mode exposes `bearerAuth` HTTP bearer JWT.
- OAuth2 mode with auth/token URLs exposes `bearerAuth` OAuth2 authorization-code flow and scopes.
- OAuth2 mode without URLs falls back to HTTP bearer external-token documentation.
- Swagger UI PKCE/client id properties are applied where feasible.

Expected initial result: static security scheme only.

### GREEN 5: Implement OpenAPI changes

Programmatically build security schemes based on properties while preserving existing API info fields.

### Regression commands

Targeted commands for apply/verify should use REST modules, not the async-focused default in `openspec/config.yaml`:

```bash
mvn test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -am
mvn test -pl awe-framework/awe-modules/awe-rest -am
mvn test -pl awe-framework/awe-controller -Dtest=AweUserDetailServiceTest
```

Run broader `mvn verify` only when time/budget permits.

## Review workload risks and implementation slices

The requested implementation can exceed the 400 changed-line review budget if delivered as one PR. Recommended slices:

1. **Slice A: Dependencies + properties + validators/converter tests**
   - Add dependency and config model.
   - Add audience validator and principal converter.
   - Unit tests only.
   - No behavior switch yet except property binding.
2. **Slice B: SecurityFilterChain branching**
   - Conditional local JWT vs Resource Server branch.
   - `/api/authenticate` disabled in OAuth2 mode.
   - Security context/MockMvc tests.
3. **Slice C: OpenAPI/Swagger mode awareness**
   - Programmatic scheme selection.
   - Swagger UI PKCE/client id support.
   - OpenAPI tests and docs notes.

If the parent/user wants a single apply phase, keep code churn low by placing Resource Server support classes in the starter and avoiding controller annotation changes. If changed-line forecasts exceed the SDD preflight review budget, pause before apply and ask for a delivery decision.

## Rollout and compatibility

- Rollout is opt-in through `awe.rest.api.auth.mode=oauth2-resource-server`.
- Existing deployments stay on local JWT by default.
- Rollback is configuration-only: set mode back to `local-jwt`.
- OAuth2 Resource Server deployments must configure issuer/JWKs and accepted audience before startup.
- Swagger OAuth2 delegated testing requires provider-side registration of a public Swagger UI client with redirect URI compatible with Springdoc Swagger UI.

## OpenSpec config mismatch note

`openspec/config.yaml` currently has async-focused context and `module_under_change: awe-framework/awe-controller`, with related files for task configuration. This change is REST-focused and should override that phase context during tasks/apply/verify. Do not broadly edit `openspec/config.yaml` as part of this design; use change-local artifacts and targeted REST module commands instead.

## Decisions summary

- Keep `local-jwt` as the default explicit REST auth mode.
- Add Resource Server dependency to the REST starter and keep local Auth0 JWT dependency unchanged.
- Use AWE REST-specific OAuth2 properties, independent of web SSO/global Spring Security properties.
- Require audience configuration and validate it with a custom Spring Security token validator.
- Build synthetic `AweUserDetails` from token claims with AWE config defaults and no persistence.
- Disable `/api/authenticate` in OAuth2 mode rather than allowing the existing local-JWT controller path to run.
- Preserve `bearerAuth` as the OpenAPI scheme name and vary its definition by mode.
