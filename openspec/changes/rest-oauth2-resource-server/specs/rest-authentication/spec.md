# REST Authentication Specification

## Purpose

Define authentication and API admission behavior for `awe-rest` so local JWT remains the default while an opt-in OAuth2 Resource Server mode can accept standards-compliant external access tokens in a stateless, provider-agnostic way.

## Requirements

### Requirement: Authentication mode compatibility and default behavior

The system MUST support an explicit REST authentication mode with `local-jwt` as the default mode.

The system MUST treat OAuth2 Resource Server support as opt-in and MUST NOT change existing local JWT behavior unless OAuth2 mode is explicitly configured.

The `/api/authenticate` endpoint MUST remain local-JWT-only and MUST NOT be used to validate, exchange, or mint externally issued OAuth2/OIDC access tokens.

#### Scenario: Default mode remains local JWT

- GIVEN a deployment that does not explicitly configure OAuth2 Resource Server mode
- WHEN a client authenticates through existing REST login behavior
- THEN local JWT authentication MUST remain active by default
- AND `/api/authenticate` MUST behave as before

#### Scenario: OAuth2 mode is opt-in

- GIVEN a deployment that explicitly configures OAuth2 Resource Server mode
- WHEN a client calls protected REST endpoints with a valid provider-issued bearer token
- THEN the request MUST be evaluated by OAuth2 Resource Server token validation
- AND local JWT default behavior MUST still remain available for deployments not opting in

#### Scenario: `/api/authenticate` remains local-JWT-only

- GIVEN OAuth2 Resource Server mode is enabled
- WHEN a client attempts to use `/api/authenticate` as an OAuth2 token endpoint
- THEN the system MUST NOT treat `/api/authenticate` as OAuth2 token issuance or exchange
- AND OAuth2 clients MUST obtain tokens from their configured identity provider

### Requirement: Provider-agnostic OAuth2 token validation with mandatory audience check

In OAuth2 Resource Server mode, the system MUST accept standards-compliant OAuth2/OIDC JWT bearer access tokens from external providers, including providers such as Microsoft Entra ID and Keycloak.

In OAuth2 Resource Server mode, the system MUST validate token issuer, signature (including JWK-based key resolution), and expiration using Spring Security JWT Resource Server facilities.

In OAuth2 Resource Server mode, the system MUST require explicit audience validation and MUST reject tokens with missing or mismatched audience.

#### Scenario: Valid token is admitted

- GIVEN OAuth2 Resource Server mode is enabled
- AND a bearer token has valid issuer, signature, expiration, and required audience
- WHEN the token is presented to a protected REST endpoint
- THEN authentication MUST succeed
- AND the request MUST be admitted to endpoint authorization checks

#### Scenario: Wrong audience is rejected

- GIVEN OAuth2 Resource Server mode is enabled
- AND a bearer token has valid issuer and signature but an audience that does not match configured required audience
- WHEN the token is presented to a protected REST endpoint
- THEN authentication MUST fail
- AND the endpoint MUST return an unauthorized response

#### Scenario: Missing or invalid token is rejected

- GIVEN OAuth2 Resource Server mode is enabled
- WHEN a protected REST endpoint is called without a token or with an invalid token
- THEN authentication MUST fail
- AND the endpoint MUST return an unauthorized response

### Requirement: REST authentication flow remains stateless and non-interactive

In OAuth2 Resource Server mode, REST authentication MUST be stateless and MUST NOT create or rely on HTTP web sessions.

In OAuth2 Resource Server mode, the system MUST NOT use redirect-based login flows, MUST NOT use `oauth2Login()`, and MUST NOT invoke web SSO login-success or provisioning handlers as part of REST API admission.

#### Scenario: Unauthorized REST call has no web redirect semantics

- GIVEN OAuth2 Resource Server mode is enabled
- WHEN an unauthenticated client calls a protected REST endpoint
- THEN the response MUST be an API-style unauthorized error
- AND the system MUST NOT initiate browser redirects or interactive login flow

#### Scenario: REST call processing is sessionless

- GIVEN OAuth2 Resource Server mode is enabled
- WHEN authenticated and unauthenticated REST requests are processed
- THEN request handling MUST NOT require persisted web session state

### Requirement: Token-claim principal mapping MUST be AWE-compatible and non-persistent

In OAuth2 Resource Server mode, the system MUST construct a minimal synthetic AWE-compatible principal from trusted token claims without requiring local user lookup.

The principal mapping MUST support delegated-user tokens and client-credentials (machine-to-machine) tokens.

When no configured and trusted profile claim is available, the system MUST assign the configured default application profile.

OAuth2 REST admission MUST NOT create, update, synchronize, or otherwise provision local `ope` users.

#### Scenario: Delegated-user token maps to synthetic user principal

- GIVEN OAuth2 Resource Server mode is enabled
- AND a delegated-user token contains user identity claims
- WHEN the token is authenticated
- THEN the system MUST create a synthetic AWE-compatible user principal for request processing
- AND local user persistence MUST NOT be performed

#### Scenario: Client-credentials token maps to synthetic machine principal

- GIVEN OAuth2 Resource Server mode is enabled
- AND a client-credentials token contains machine/client identity claims
- WHEN the token is authenticated
- THEN the system MUST create a synthetic AWE-compatible machine principal for request processing
- AND local user persistence MUST NOT be performed

#### Scenario: Default profile fallback is used

- GIVEN OAuth2 Resource Server mode is enabled
- AND an authenticated token does not provide a configured trusted profile claim
- WHEN principal mapping is performed
- THEN the principal MUST be assigned the configured default application profile

### Requirement: Authorization scope in v1 remains API admission only

This change MUST be limited to API admission/authentication behavior in v1.

This change MUST NOT introduce new fine-grained query/maintain authorization semantics or a new API read/write role model in v1.

Existing public query/maintain behavior MUST remain unchanged.

#### Scenario: Protected endpoints still require authentication

- GIVEN either local JWT mode or OAuth2 Resource Server mode
- WHEN a client calls protected `/api/data/**`, `/api/maintain/**`, or `/api/maintain/async/**` endpoints without valid authentication
- THEN access MUST be denied as unauthorized

#### Scenario: Public endpoints retain existing behavior

- GIVEN either local JWT mode or OAuth2 Resource Server mode
- WHEN a client calls existing public query/maintain endpoints
- THEN behavior MUST remain consistent with existing public endpoint rules

### Requirement: Swagger/OpenAPI security documentation MUST be mode-aware

The OpenAPI/Swagger configuration MUST remain compatible with local JWT mode and preserve existing local JWT documentation behavior.

When OAuth2 Resource Server mode is enabled and sufficient provider metadata is configured, OpenAPI/Swagger MUST expose an OAuth2 security scheme for API testing.

For delegated-user testing in Swagger UI, authorization code with PKCE SHOULD be the preferred OAuth2 flow.

If Swagger UI client-credentials flow is not appropriate for the deployment, documentation MAY direct client-credentials testing to tools such as curl or Postman.

#### Scenario: Local JWT documentation remains available

- GIVEN local JWT mode is active
- WHEN API documentation is rendered
- THEN local JWT security documentation MUST remain available and usable

#### Scenario: OAuth2 documentation appears in OAuth2 mode

- GIVEN OAuth2 Resource Server mode is active
- AND provider metadata required by OpenAPI OAuth2 configuration is available
- WHEN API documentation is rendered
- THEN an OAuth2 security scheme MUST be exposed for bearer token testing
- AND delegated-user testing SHOULD prefer authorization code with PKCE

#### Scenario: Client-credentials testing guidance remains available

- GIVEN OAuth2 Resource Server mode is active
- WHEN Swagger UI does not support the intended client-credentials testing model for a deployment
- THEN documentation MUST provide guidance for client-credentials testing via non-browser API tools
