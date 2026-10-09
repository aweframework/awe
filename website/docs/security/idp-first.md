---
id: idp-first
title: IdP-first security mode
sidebar_label: IdP-first mode
---

# IdP-first security mode

This page is the decision document for running an AWE application with an external identity provider (IdP) as the
only way to sign in. It is written for application teams that configure AWE and for maintainers that evolve it.
It explains what the IdP decides, what AWE keeps, who can block a user, and which parts exist today and which are
planned.

:::info Reading guide
Everything not marked **Planned** describes the behaviour of the current code. Items marked **Planned in AWE 5**
are decisions already taken but not available yet; they carry no delivery date and their property names may still
change. To configure SSO itself, start with [Single sign-on](authentication-sso.md).
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
| Role claim sent to the application | IdP | `AweWebSecurityConfig.realmRolesAuthoritiesConverter` reads `realm_access.roles` |
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
| IdP | The user cannot authenticate. | Existing AWE sessions are not touched until they end (see below). |
| AWE (`enabled` / locked flags in the user record) | The user is rejected after a successful IdP login. | Current behaviour since AWE 5.0.0: the SSO login checks both flags and shows an error page; no session is created. AWE 4.x and earlier did not check them on the SSO path, so there an AWE-disabled or AWE-locked user could still sign in through the provider — block such users in the provider too. |

Block a user in the provider when the user must lose access everywhere; block in AWE when the user must lose
access to this application only.

### Session lifetime and IdP revocation

After login, the AWE session is a regular HTTP session (see [Session config](../session.md)). The user's profile,
restrictions, language, theme and initial screen are loaded into it at login (`AweSessionDetails`). AWE does not
call the provider again while the session lives, so:

- Disabling or deleting the user in the IdP does **not** end an open AWE session; it ends when it expires or when
  the user logs out.
- A role or profile change only takes effect at the next login.
- Keep the session timeout of the application short enough to match your revocation requirements.

**Planned in AWE 5:** OIDC back-channel logout, so the provider can end AWE sessions when a user is revoked or
signs out elsewhere.

### Logout

Logout is **RP-initiated** today: `/action/logout` clears the AWE session and, when SSO is enabled, redirects the
browser to the provider's end-session endpoint through `OidcClientInitiatedLogoutSuccessHandler`, which ends the
provider session too. This requires the browser to complete the redirect; nothing happens in AWE if the user is
logged out at the provider by another channel. Back-channel logout is the planned complement.

### Role mapping

- Authorities from the provider are filtered by `awe.security.sso.filter-authority-prefix`: only authorities that
  start with the prefix are considered and the prefix is stripped to obtain the AWE profile name. With an empty
  prefix, the whole authority is used.
- The **first** matching authority wins. The converted authorities are collected into a set, so when a user
  carries several matching roles the choice is not guaranteed to be stable. Send exactly one role carrying the
  prefix per user until deterministic selection is available (planned, see below).
- A role that does not exist as an AWE profile is ignored for existing users (their profile is kept) and replaced
  by the default role for newly provisioned users.
- The default converter only understands the Keycloak `realm_access.roles` claim. Entra ID sends `roles` (app
  roles) or `groups`, which are not read by default; today they must be mapped by the provider (Keycloak
  brokering, see [Single sign-on](authentication-sso.md#mappers)) or by overriding the
  `realmRolesAuthoritiesConverter` bean in the application.

### Provisioning and the default role

Property names below are the ones in `SecurityConfigProperties.Sso` and `BaseConfigProperties`.

| Situation | Behaviour today |
| --- | --- |
| Unknown user, `awe.security.sso.auto-provision-user=true` (default) | A user record is created with the mapped role, or with `awe.application.default-role` (default `operator`) when no role maps. |
| Unknown user, `auto-provision-user=false` | No record is created, but the login still proceeds with a session built from the token claims and the same role fallback. Turning this flag off does not by itself deny unknown users. |
| Existing user, provider sends no usable role | The stored profile is kept, unless `awe.security.sso.overwrite-profile-with-default-role=true`. |
| New user, provider sends no usable role | Falls back to `awe.application.default-role`. |

New users take their login name from the `preferred_username` claim, while existing users are looked up with
`awe.security.sso.user-name-attribute` (default `preferred_username`). Keep both aligned.

**Planned in AWE 5:** in IdP-first mode a login with no mappable role is **denied** (fail closed). The default-role
fallback becomes an explicit opt-in, so it can never grant access by accident.

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
`awe.security.sso.auto-launch=true` only skips the native login screen. The local login endpoint
(`POST /action/login`, `JsonAuthenticationFilter`) and the configured authentication provider
(`awe.security.auth-mode`: database, LDAP, in-memory or custom) stay registered and active while SSO is enabled.
Today, an account that has a local password can still sign in without the provider.
:::

Until the planned switch exists, reduce the local path by not provisioning local passwords for SSO users and by
restricting network access to `/action/login` where your infrastructure allows it.

### Planned in AWE 5

| Item | Intent |
| --- | --- |
| Local-login switch | A property (proposed name `awe.security.sso.local-login`, default `true` to preserve current behaviour) that disables the local login path when SSO is active. |
| Break-glass admin | An optional local administrator that can still sign in when the local path is disabled; off by default. |
| Back-channel logout | OIDC back-channel logout plus a session registry, so provider-side revocation ends AWE sessions. |
| Configurable roles claim | Reading the roles from a configurable claim so Entra ID works directly, in addition to Keycloak brokering. Includes deterministic profile selection when several roles match. |
| Fail-closed role mapping | In IdP-first, deny the login when no role maps; the default-role fallback becomes opt-in. |
| Keycloak integration test | An automated end-to-end test against a real Keycloak (login, profile mapping, back-channel logout, disabled user). |

## Residual risks and recommendations

| Risk | Recommendation |
| --- | --- |
| Open AWE sessions survive IdP revocation until expiry. | Use a short session timeout; adopt back-channel logout when available. |
| Local login stays active under SSO. | Do not keep local passwords for SSO users; adopt the local-login switch when available and treat the break-glass admin as a monitored exception. |
| Default role can grant access to users with no role. | Keep `awe.application.default-role` **unprivileged** (never an administrative profile); prefer fail-closed mapping when available. |
| Ambiguous mapping when several roles carry the prefix. | Send a single prefixed role per user; do not rely on the order. |
| Client secrets in configuration files. | Inject them from environment variables or a secret vault; if you use `ENC(...)` encrypted properties, replace the default `awe.security.master-key`. Never commit secrets. |
| Wrong or spoofed provider. | Set `issuer-uri` explicitly and over HTTPS; Spring Security validates the issuer, audience, expiry and nonce of the ID token. For Entra ID prefer a single-tenant app registration. Keep redirect URIs exact (no wildcards in production). |
| Open tenant resolution. | Declare explicit `tenant-mappings`; keep user names unique across tenants. |
| Second factor not enforced. | AWE TOTP does not run on the SSO path; enforce MFA in the provider. |

## What this mode does not change

- Screen and option authorization, profiles and restrictions: always AWE.
- The REST module can also validate externally issued tokens (`OAUTH2_RESOURCE_SERVER` mode, see
  [REST module](../rest-module.md)); that is configured separately from browser login and is out of scope here.
