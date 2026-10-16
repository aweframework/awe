---
id: idp-first
title: IdP-first security mode
sidebar_label: IdP-first mode
---

# IdP-first security mode

This page is the decision document for running an AWE application with an external identity provider (IdP) as the
only way to sign in. It is written for application teams that configure AWE and for maintainers that evolve it.
It explains what the IdP decides, what AWE keeps, who can block a user, and which configuration controls each part. Every
part described here exists in the current code.

:::info Reading guide
Everything on this page describes the behaviour of the current code. To configure SSO itself, start with [Single sign-on](authentication-sso.md).
:::

## What "IdP-first" means

In an IdP-first deployment the identity provider (for example Keycloak, or Microsoft Entra ID) is the single
authority for **who the user is** and **whether the account may sign in**. AWE trusts the provider for those two
questions and keeps ownership of **what the user can do inside the application**.

IdP-first is a deployment profile, not a rewrite: the same AWE application can run with local users (database,
LDAP, in-memory or custom providers) or with an IdP, and the profile is selected by configuration.

## Trust boundaries

| Decision | Owner | Where it happens in the code |
| --- | --- | --- |
| Authentication (password, MFA, federation, brute-force lockout) | IdP | OIDC authorization code flow handled by Spring Security `oauth2Login`, configured in `AweWebSecurityConfig` |
| Account state at the provider (disabled, locked, deleted) | IdP | The provider refuses to authenticate the user |
| Role claim sent to the application | IdP | `AweWebSecurityConfig.realmRolesAuthoritiesConverter` reads the claims in `awe.security.sso.roles-claims` (default `realm_access.roles`) |
| Which AWE profile a role maps to | AWE | `AweUserDetailService.mapGrantedAuthorityProfile` and `AccessService` role synchronization |
| Screen and option authorization (profiles, restrictions) | **AWE** | `MenuService` and `ScreenRestrictionGenerator`, driven by the profile and the user restrictions loaded at login |
| Theme, language and initial screen | AWE | `AweUserDetailService.getAweUserDetails`, stored in the session by `AweSessionDetails.storeUserDetails` |
| AWE account state (enabled, locked) | AWE | User record, read by `AweUserDetailService` |

:::important Authorization stays in AWE in every mode
Screen and option authorization is **never** delegated to the identity provider, whatever mode is configured. The
provider only supplies a role name; AWE resolves it to a profile and then applies that profile's restrictions
(and any restriction stored for the individual user) to the menu, the screens and their options. A product keeps
its own profile and restriction model; IdP-first only changes how the user gets in.
:::

### What the provider is not asked to do

- It does not know AWE screens, options or restrictions, and it must not be configured to model them.
- It does not store the user's AWE preferences (theme, language, initial screen). These stay in the AWE user
  record.
- Two-factor authentication through AWE's own TOTP screens is part of the **local** login flow
  (`AccessService.login`). The SSO flow (`AccessService.onAuthenticationSuccess`) goes straight to the home
  screen, so in IdP-first the second factor must be enforced by the provider.

## Trust matrix

### Who can block a user

Both sides can block a user, by decision:

| Blocked at | Effect | Notes |
| --- | --- | --- |
| IdP | The user cannot authenticate. | Existing AWE sessions end at once with back-channel logout when the provider ends the user's sessions; otherwise they live until they end (see below). |
| AWE (`enabled` / locked flags in the user record) | The user is rejected after a successful IdP login. | Current behaviour since AWE 5.0.0: the SSO login checks both flags and shows an error page; no session is created. AWE 4.x and earlier did not check them on the SSO path, so there an AWE-disabled or AWE-locked user could still sign in through the provider — block such users in the provider too. |

Block a user in the provider when the user must lose access everywhere; block in AWE when the user must lose
access to this application only.

### Session lifetime and IdP revocation

After login, the AWE session is a regular HTTP session (see [Session config](../session.md)). The user's profile,
restrictions, language, theme and initial screen are loaded into it at login (`AweSessionDetails`). AWE does not
call the provider again while the session lives, so:

- Without back-channel logout, disabling or deleting the user in the IdP does **not** end an open AWE session; it
  ends when it expires or when the user logs out.
- With `awe.security.sso.back-channel-logout.enabled=true` (since AWE 5, off by default), the provider ends AWE
  sessions directly: when the provider session of the user ends (logout elsewhere, an administrator signs the user
  out), it posts a logout token and AWE ends the matching sessions, including the AWE state kept for them (session
  data and websocket connections). Disabling a user in the provider ends AWE sessions only when the provider also
  ends the user's provider sessions and sends the logout; check the provider's behaviour. Details and caveats
  (internal call, cookie name, clusters) are in [Single sign-on](authentication-sso.md#back-channel-logout).
- A role or profile change only takes effect at the next login.
- Keep the session timeout of the application short enough to match your revocation requirements.

### Logout

Logout is **RP-initiated** today: `/action/logout` clears the AWE session and, when SSO is enabled, redirects the
browser to the provider's end-session endpoint through `OidcClientInitiatedLogoutSuccessHandler`, which ends the
provider session too. This requires the browser to complete the redirect. A logout at the provider by another channel only reaches AWE
with back-channel logout enabled (see above).

### Role mapping

- Authorities from the provider are filtered by `awe.security.sso.filter-authority-prefix`: only authorities that
  start with the prefix are considered and the prefix is stripped to obtain the AWE profile name. With an empty
  prefix, the whole authority is used.
- Since AWE 5, when a user carries several matching roles, the **first name in alphabetical order that exists as an
  AWE profile** wins, whatever the order of the roles in the token (the first name in alphabetical order when none
  exists). AWE 4.x took the first role of an unordered set, so the choice was not stable.
- A role that does not exist as an AWE profile is ignored for existing users (their profile is kept) and replaced
  by the default role for newly provisioned users, unless a role is required (see below).
- Since AWE 5, the claims are configurable with `awe.security.sso.roles-claims` (default `realm_access.roles`):
  `resource_access.<client-id>.roles` for Keycloak client roles, `roles` for Entra ID app roles, or several of them.
  Details in [Single sign-on](authentication-sso.md#roles-claim).

### Provisioning and the default role

Property names below are the ones in `SecurityConfigProperties.Sso` and `BaseConfigProperties`.

When a role is required (`awe.security.sso.require-role`, required by default in IdP-first, see below), a login
whose provider roles map to no existing AWE profile is refused before any of these rows apply.

| Situation | Behaviour when no role is required |
| --- | --- |
| Unknown user, `awe.security.sso.auto-provision-user=true` (default) | A user record is created with the mapped role, or with `awe.application.default-role` (default `operator`) when no role maps. |
| Unknown user, `auto-provision-user=false` | No record is created, but the login still proceeds with a session built from the token claims and the same role fallback. Turning this flag off does not by itself deny unknown users. |
| Existing user, provider sends no usable role | The stored profile is kept, unless `awe.security.sso.overwrite-profile-with-default-role=true`. |
| New user, provider sends no usable role | Falls back to `awe.application.default-role`. |

New users take their login name from the `preferred_username` claim, while existing users are looked up with
`awe.security.sso.user-name-attribute` (default `preferred_username`). Keep both aligned.

### Fail-closed role mapping

Since AWE 5, in IdP-first mode (`awe.security.sso.local-login=false`) a login with no mappable role is **denied**
(fail closed): neither the stored profile nor the default role is used, and the user sees a localized error page.
This applies to **existing users too**: the profile stored in AWE is not a fallback. With local login on, the
previous behaviour stays unless `require-role=true` is set.
The default-role fallback is an explicit opt-in with `awe.security.sso.require-role=false`, so it can never grant
access by accident. `awe.security.sso.require-role=true` requires a role in any SSO setup. Details in
[Single sign-on](authentication-sso.md#requiring-a-role-fail-closed).

### Multi-tenant registrations

With `awe.security.sso.multitenant.enabled=true`, `MultiTenantFilter` derives the tenant from the first label of
the host name and `MultiTenantOAuth2Config` maps it to an OAuth2 registration.

- Without `tenant-mappings`, **any** subdomain is accepted as a tenant, and a subdomain with no matching
  registration falls back to the default registration (`MultiTenantOAuth2AuthenticationEntryPoint`,
  `OAuth2UrlService`).
- With explicit `awe.security.sso.multitenant.tenant-mappings`, only the listed subdomains are valid tenants; any
  other host name uses the default tenant.
- AWE users are identified by user name only. The same user name issued by two different tenants' providers
  resolves to the **same** AWE user.

Recommendation: in IdP-first deployments always declare explicit tenant mappings, and make sure user names are
unique across the providers of all tenants.

## Configuration: today and planned

### Available today

```properties title="Minimal IdP-first baseline (current behaviour)"
awe.security.sso.enabled=true
awe.security.sso.auto-launch=true
awe.security.sso.filter-authority-prefix=role_
# Do not store unknown users in AWE. This does NOT deny them: an unknown user still signs in with a session
# built from the token claims and the default role (see Provisioning). Deny unknown users in the provider.
awe.security.sso.auto-provision-user=false
# Keep profiles assigned in AWE when the provider sends no role
awe.security.sso.overwrite-profile-with-default-role=false
```

:::warning Auto-launch is not a switch for local login
`awe.security.sso.auto-launch=true` only skips the native login screen. By default the local login endpoint
(`POST /action/login`, `JsonAuthenticationFilter`) and the configured authentication provider
(`awe.security.auth-mode`: database, LDAP, in-memory or custom) stay registered and active while SSO is enabled, so an
account that has a local password can still sign in without the provider. Use the local-login switch below to close it.
:::

### Local login switch and break-glass users

Since AWE 5, `awe.security.sso.local-login=false` turns the local login path off when SSO is enabled (default `true`,
which keeps the previous behaviour). Details and examples are in
[Single sign-on](authentication-sso.md#switching-off-local-login).

```properties title="IdP-first with an emergency way in"
awe.security.sso.enabled=true
awe.security.sso.local-login=false
# Optional. Empty by default: nobody can sign in locally.
awe.security.sso.break-glass-users=admin
# Not needed: with local-login=false a role that maps to an AWE profile is required by default.
# awe.security.sso.require-role=true
```

| Aspect | Behaviour |
| --- | --- |
| Scope | Only applies when `awe.security.sso.enabled=true`; ignored otherwise. |
| Local paths | The three login entry points (`POST /action/login`, `POST /api/authenticate` and the standard form login) share one authentication manager that applies the policy. Only break-glass users get through. The shared `AuthenticationManager` bean used by internal flows is not decorated. |
| What a refused user sees | The same response as a wrong password, at about the same cost (a real authentication with a throw-away password runs), plus the same bad-credentials failure event. Which users are break-glass users cannot be found out from the outside. |
| Sign-in screen | Both clients hide the user, password and log-in button; SSO buttons remain. A break-glass user opens `/screen/public/signin-local`. |
| Break-glass users | The user names in `awe.security.sso.break-glass-users`, trimmed and matched exactly (case-sensitive). Off by default (empty list). They authenticate with their normal local credentials and profile; locked or expired accounts are still refused. |
| Audit | Each break-glass login attempt is logged at `WARN` with the `[break-glass]` tag and the sanitized user name; refusals are logged at `WARN` without the tag. Passwords and tokens are never logged. |
| What it does not cover | OIDC login, the REST `oauth2-resource-server` mode and any authentication the application adds outside the AWE authentication manager. |

### Back-channel logout

Since AWE 5, the provider can end AWE sessions (off by default). Details in
[Single sign-on](authentication-sso.md#back-channel-logout).

```properties title="Let the provider end AWE sessions"
awe.security.sso.back-channel-logout.enabled=true
# Only when the public address is not reachable from the server itself
awe.security.sso.back-channel-logout.logout-uri=http://localhost:8080{basePath}/logout/connect/back-channel/{registrationId}
```

### Integration test with Keycloak

`KeycloakIdpFirstTest` (`awe-tests/awe-boot`) runs the login, the profile mapping from the roles claims, the fail-closed
refusal, the refusal of a user disabled in AWE and the back-channel logout against a real Keycloak in a container. The
step-by-step setup with Keycloak and Entra ID is in the [IdP-first guide](idp-first-keycloak-entra.md).

## Residual risks and recommendations

| Risk | Recommendation |
| --- | --- |
| Open AWE sessions survive IdP revocation until expiry. | Enable back-channel logout (`awe.security.sso.back-channel-logout.enabled=true`) and keep a short session timeout as a second line, for providers or events that send no logout. |
| Local login stays active under SSO (default). | Set `awe.security.sso.local-login=false` and treat every break-glass login in the log as a monitored exception. |
| Default role can grant access to users with no role. | Keep `awe.application.default-role` **unprivileged** (never an administrative profile); keep the fail-closed default of IdP-first, or set `awe.security.sso.require-role=true`. |
| Several roles carry the prefix. | The first name in alphabetical order that exists as an AWE profile wins; send a single prefixed role per user when the choice matters. |
| Client secrets in configuration files. | Inject them from environment variables or a secret vault; if you use `ENC(...)` encrypted properties, replace the default `awe.security.master-key`. Never commit secrets. |
| Wrong or spoofed provider. | Set `issuer-uri` explicitly and over HTTPS; Spring Security validates the issuer, audience, expiry and nonce of the ID token. For Entra ID prefer a single-tenant app registration. Keep redirect URIs exact (no wildcards in production). |
| Open tenant resolution. | Declare explicit `tenant-mappings`; keep user names unique across tenants. |
| Second factor not enforced. | AWE TOTP does not run on the SSO path; enforce MFA in the provider. |

## What this mode does not change

- Screen and option authorization, profiles and restrictions: always AWE.
- The REST module can also validate externally issued tokens (`OAUTH2_RESOURCE_SERVER` mode, see
  [REST module](../rest-module.md)); that is configured separately from browser login and is out of scope here.
