# Change Proposal: REST OAuth2 Resource Server support

## Summary
Add an optional, provider-agnostic OAuth2 Resource Server authentication mode to `awe-rest` so API clients can call protected REST endpoints with standard externally issued access tokens, while keeping the current local JWT login/token flow as the default and backward-compatible behavior.

## Intent
Enable stateless REST API admission with externally issued bearer tokens from standards-compliant OAuth2/OIDC providers such as Microsoft Entra ID or Keycloak. The REST layer should validate token issuer, signature, expiration, and explicit audience, then expose a minimal AWE-compatible principal for query/maintain execution without starting web sessions, redirects, `oauth2Login()`, or web SSO provisioning flows.

## Problem statement
`awe-rest` currently protects `/api/**` with a local JWT flow: clients call `/api/authenticate`, AWE authenticates local credentials, and AWE issues/verifies HMAC-signed tokens. Environments standardized on external identity providers need to present provider-issued access tokens directly to the REST API. Reusing the existing web OAuth2 login flow would introduce browser redirects, sessions, login-success handling, and optional `ope` user provisioning that are inappropriate for stateless REST APIs.

## Goals
- Add an opt-in OAuth2 Resource Server mode for `awe-rest` that validates standard OAuth2/OIDC bearer access tokens from providers such as Microsoft Entra ID or Keycloak.
- Keep local JWT authentication as the default mode and preserve existing `/api/authenticate` behavior for local JWT users.
- Keep REST security stateless: no HTTP sessions, no redirects, no `oauth2Login()`, and no web login success handlers.
- Do not reuse or couple REST OAuth2 admission to web SSO session/provisioning behavior.
- Construct a minimal/synthetic `AweUserDetails`-compatible principal from token claims without persistence.
- Support both delegated user tokens and machine-to-machine/client-credentials tokens at the admission/principal-mapping level.
- Validate issuer, signature/JWKs, expiration, and an explicit configured audience.
- Preserve the existing public query/maintain authorization behavior and protected query/maintain endpoint model.
- Update Swagger/OpenAPI configuration so the API documentation UI can authenticate against a configured OAuth2/OIDC provider when supported, enabling quick manual testing from the existing Swagger interface.

## Non-goals
- No local `ope` user auto-provisioning for REST OAuth2.
- No fine-grained REST query/maintain authorization model in v1.
- No new API-specific read/write role model in v1.
- No OAuth2 login, authorization-code browser flow, redirect UX, or session lifecycle for REST.
- No changes to web SSO login behavior except avoiding accidental coupling.
- No replacement or removal of the current local JWT mode.

## Scope
In scope for later spec/design/apply phases:
- REST starter security configuration for selecting local JWT versus OAuth2 Resource Server mode.
- Configuration properties for issuer/JWK discovery and required audience validation, independent of a specific identity provider.
- Spring Security Resource Server integration for `/api/**` only.
- JWT-to-AWE-principal conversion that creates a non-persistent minimal principal from claims.
- Tests for both local JWT default behavior and OAuth2 Resource Server behavior.
- Swagger/OpenAPI security scheme and UI configuration for OAuth2 authentication against a configured provider, where feasible.
- Documentation/OpenAPI notes that `/api/authenticate` is local-JWT-only and is not used for OAuth2 Resource Server authentication.

Out of scope:
- Database schema changes.
- `ope` user creation or profile synchronization for REST OAuth2.
- REST permission/role semantics beyond existing AWE public/protected endpoint checks.
- Browser login or web UI SSO changes.

## Proposed high-level approach
1. Introduce an explicit REST authentication mode, defaulting to the current local JWT behavior.
   - Example conceptual modes: `local-jwt` (default) and `oauth2-resource-server`.
   - Keep existing JWT properties and filters active only for local JWT mode.
2. In OAuth2 Resource Server mode, configure the REST `SecurityFilterChain` for `/api/**` with:
   - `SessionCreationPolicy.STATELESS`.
   - CSRF disabled for API endpoints.
   - `oauth2ResourceServer().jwt(...)` or equivalent Spring Security configuration.
   - No `oauth2Login()`, no login page, and no redirect-based entry point.
3. Validate external provider tokens with standard JWT validation plus explicit audience validation.
   - Issuer/signature/expiration validation should come from Spring Security JWT decoder configuration.
   - Audience must be configured and rejected if absent/mismatched.
   - Provider-specific setup, such as Microsoft Entra ID app registration or Keycloak client/audience configuration, should be documented as examples rather than hard-coded.
4. Add a REST-specific JWT authentication converter/principal mapper.
   - Delegated user tokens: derive username/display name/email from claims such as `preferred_username`, `upn`, `email`, `name`, `oid`, `sub`, or configurable equivalents.
   - Client credentials tokens: derive a stable machine principal from claims such as `azp`, `appid`, `client_id`, `clientId`, `sub`, or app display-name claims.
   - Set default enabled/non-expired flags and default language/theme/restriction/profile values needed by AWE runtime.
   - Use a configured/default application profile when no trusted profile claim is present.
   - Do not load or create local users as part of OAuth2 admission.
5. Keep `/api/authenticate` local-JWT-only.
   - OAuth2 clients obtain tokens from the configured identity provider and call protected endpoints with `Authorization: Bearer <access_token>`.
   - `/api/authenticate` should not exchange, validate, or mint external provider tokens.
6. Preserve existing endpoint authorization shape.
   - `/api/public/data/**` and `/api/public/maintain/**` continue to use existing public query/maintain checks.
   - Protected `/api/data/**`, `/api/maintain/**`, and `/api/maintain/async/**` require a valid authenticated principal.
   - v1 does not decide per-query or per-maintain permissions from provider scopes/roles.
7. Extend Swagger/OpenAPI support for manual API testing.
   - In local JWT mode, preserve the current bearer-token documentation behavior.
   - In OAuth2 Resource Server mode, expose an OpenAPI OAuth2 security scheme when enough provider metadata is configured.
   - Prefer standard OpenAPI OAuth2 flows supported by Swagger UI, such as authorization code with PKCE for delegated users.
   - Do not make Swagger UI token acquisition a server-side login flow; it is a documentation/testing client concern only.

## Compatibility and backward-compatible behavior
- Existing applications remain on local JWT mode unless they opt in to OAuth2 Resource Server mode.
- Existing local JWT token generation/verification and `/api/authenticate` semantics remain unchanged in default mode.
- Existing REST controller query/maintain contracts remain unchanged.
- Existing public query/maintain authorization remains unchanged.
- The OAuth2 mode is additive and should not require web SSO configuration to be enabled.
- External provider tokens are accepted only in OAuth2 Resource Server mode and only after configured issuer/signature/expiration/audience validation succeeds.
- Swagger/OpenAPI changes should be mode-aware and must not break existing local JWT Swagger usage.

## Affected areas
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/RestSecurityConfiguration.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/AweRestConfigProperties.java`
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/config/JWTProperties.java` or new REST auth/resource-server properties
- `awe-framework/awe-modules/awe-rest/src/main/java/com/almis/awe/rest/security/JWTAuthenticationFilter.java`
- `awe-framework/awe-modules/awe-rest/src/main/java/com/almis/awe/rest/security/JWTAuthorizationFilter.java`
- `awe-framework/awe-modules/awe-rest/src/main/java/com/almis/awe/rest/security/JWTAuthenticationEntryPoint.java` or a mode-neutral REST authentication entry point
- New REST OAuth2 Resource Server support classes for audience validation and token-claims principal conversion
- REST security/unit/integration tests in the relevant starter/module test locations
- REST API documentation/OpenAPI authentication notes
- `awe-framework/awe-starters/awe-rest-spring-boot-starter/src/main/java/com/almis/awe/rest/autoconfigure/OpenAPIConfiguration.java` and any related springdoc/Swagger UI configuration properties

## Risks
- A synthetic `AweUserDetails` may miss fields expected by downstream AWE query/maintain services; defaults must be selected carefully.
- Delegated and client-credentials tokens expose different claim sets across providers, so principal-name mapping must be robust and configurable enough for common deployments.
- Accepting tokens without strict audience validation would be unsafe; audience configuration should be mandatory in OAuth2 mode.
- Mixing local JWT and external provider JWT validation in one filter path could create confusing failure modes; the implementation should keep modes explicit.
- Existing controller code clears `SecurityContextHolder` after some calls; tests should verify this does not interfere with stateless Resource Server requests.
- `openspec/config.yaml` currently reflects an async-focused previous change. Later phases should update or override phase context for the REST scope without broad config edits during proposal creation.

## Open questions for spec/design
- Exact property names and defaults for REST authentication mode and resource-server settings.
- Whether OAuth2 mode should completely disable `/api/authenticate` or leave it present but documented as local-JWT-only/unsupported for OAuth2 deployments.
- Which token claim should be the default REST principal name for delegated tokens and for client-credentials tokens across common providers.
- Whether profile claim mapping should be configurable and whether profile-claim values need validation against existing AWE profiles without loading users.
- Whether Swagger/OpenAPI should advertise one bearer scheme that varies by mode or separate local-JWT and OAuth2 bearer schemes.
- Which provider-specific examples should be documented first, for example Microsoft Entra ID and Keycloak.
- How Swagger UI should discover or configure provider authorization/token URLs: explicit properties, OIDC discovery metadata, or provider-specific examples.
- Whether Swagger UI OAuth2 support should target delegated-user authorization-code testing only, while machine-to-machine/client-credentials testing remains documented for tools such as curl/Postman.
- Minimal set of tests feasible under the changed-line review budget and module boundaries.

## Expected follow-up spec/design areas
- Requirements/spec for authentication modes, protected/public endpoint behavior, token validation, and principal mapping.
- Design for conditional `SecurityFilterChain` configuration and bean wiring in the REST starter.
- Design for provider-agnostic audience validation and JWT decoder configuration.
- Design for REST-only synthetic `AweUserDetails` construction and default profile/language/theme/restriction handling.
- Test plan covering backward compatibility, invalid tokens, wrong audience, delegated tokens, client-credentials tokens, public endpoints, and `/api/authenticate` mode behavior.
- Documentation plan for configuring common providers such as Microsoft Entra ID and Keycloak, including issuer, JWK discovery, audiences, scopes/roles, Swagger UI OAuth2 testing, and client usage.

## Rollback plan
Because the change should be opt-in, rollback is primarily configuration-based: keep or return `awe-rest` deployments to local JWT mode. If implementation changes later introduce regressions, revert the REST starter/module changes for Resource Server support while leaving existing local JWT files and `/api/authenticate` behavior intact.

## Success criteria
- Local JWT mode remains the default and existing REST authentication behavior continues to work.
- OAuth2 Resource Server mode accepts valid standard OAuth2/OIDC JWT access tokens for configured issuer/audience and rejects expired, unsigned/invalid-signature, wrong-issuer, or wrong-audience tokens.
- REST remains stateless with no web sessions, no redirects, and no `oauth2Login()`.
- REST OAuth2 does not provision or synchronize local `ope` users.
- Authenticated REST requests receive an AWE-compatible synthetic/minimal principal derived from token claims.
- Both delegated user tokens and client-credentials tokens can be represented as authenticated principals.
- Swagger/OpenAPI can advertise the appropriate security scheme for the selected mode and, where feasible, allows delegated-user OAuth2 token acquisition from the configured provider for quick API testing.
- v1 does not add fine-grained REST query/maintain authorization or a new API read/write role model.
