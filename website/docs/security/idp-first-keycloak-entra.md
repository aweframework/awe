---
id: idp-first-keycloak-entra
title: IdP-first with Keycloak and Microsoft Entra ID
sidebar_label: Keycloak and Entra ID guide
---

# IdP-first with Keycloak and Microsoft Entra ID

This guide shows how to run an AWE application with Keycloak, or with Microsoft Entra ID, as the only way to sign in.
It is the practical companion of the decision document [IdP-first security mode](idp-first.md), which explains what the
identity provider decides and what AWE keeps. Screen and option authorization stays in AWE: the provider only sends a
role name, and AWE maps it to a profile.

:::info What you need
- AWE 5 or later (the roles claim, the local-login switch and back-channel logout are not in AWE 4.x).
- An AWE **profile** for every role you will send. The profile names are the ones stored in AWE (for example
  `administrator`); the provider roles carry a prefix, such as `role_administrator`.
- A public HTTPS address for the application. The examples use `https://my-app.example.com`.
:::

## Common configuration {#common-configuration}

These properties make the provider the only way in, whichever provider you use. The provider-specific properties are in
the sections below.

```properties title="IdP-first baseline"
awe.security.sso.enabled=true
# Turn local login off. A role that maps to an AWE profile is then required (fail closed)
awe.security.sso.local-login=false
# Roles of the provider that start with this prefix are AWE profiles: role_administrator -> administrator
awe.security.sso.filter-authority-prefix=role_
# Let the provider end AWE sessions
awe.security.sso.back-channel-logout.enabled=true
# No local way in at all: nobody can sign in with a local password (see the break-glass section to opt in)
awe.security.sso.break-glass-users=
```

| Property | What it does here |
| --- | --- |
| `awe.security.sso.local-login=false` | Hides the local form and refuses `POST /action/login`, `POST /api/authenticate` and the form login for everybody who is not a break-glass user. |
| `awe.security.sso.filter-authority-prefix` | Selects the roles that mean an AWE profile and strips the prefix. |
| `awe.security.sso.roles-claims` | Claims of the ID token that carry the roles (see [the roles claim](#the-roles-claim)). |
| `awe.security.sso.auto-provision-user` | `true` (default) creates the AWE user at the first login, with the profile of the role. |
| `awe.security.sso.break-glass-users` | User names that can still use the local form (see [local login and the break-glass administrator](#local-login-and-the-break-glass-administrator)). |

## The roles claim {#the-roles-claim}

AWE reads the roles from the **ID token**, from the claims listed in `awe.security.sso.roles-claims` (default
`realm_access.roles`). Several claims can be listed; their roles are joined. When more than one role maps, the first
name in alphabetical order that exists as an AWE profile wins, so send one prefixed role per user when the choice matters.

| Provider | Roles | `awe.security.sso.roles-claims` |
| --- | --- | --- |
| Keycloak | Realm roles | `realm_access.roles` (default) |
| Keycloak | Client roles | `resource_access.<client-id>.roles` |
| Keycloak | Both | `realm_access.roles,resource_access.<client-id>.roles` |
| Microsoft Entra ID | App roles | `roles` |

If no role maps to an AWE profile the login is **refused** with an error page, for new and existing users alike. Neither
the profile stored in AWE nor `awe.application.default-role` is used. Opting back in to the default role
(`awe.security.sso.require-role=false`) is possible but defeats the purpose of IdP-first.

## Keycloak {#keycloak}

### Create the client {#keycloak-client}

In the realm of your users, create a client:

1. **Client ID**: for example `awe`. **Client authentication**: on (a confidential client). Keep **Standard flow** on and
   the other flows off.
2. **Valid redirect URIs**: `https://my-app.example.com/login/oauth2/code/keycloak`. The last segment is the Spring
   registration id used below (`keycloak`). Do not use wildcards in production.
3. **Valid post logout redirect URIs**: `https://my-app.example.com/*`, so that logging out of AWE returns to the application.
4. Copy the **Client secret** of the **Credentials** tab. Inject it from an environment variable or a secret vault, never
   from a file in the repository.

### Send the roles in the ID token {#keycloak-roles}

Create the realm roles with the prefix (`role_administrator`, `role_general`...) and assign them to the users or to
groups. Then, in the dedicated scope of the client (**Client scopes** > `awe-dedicated` > **Add mapper** > **By
configuration**):

- **User Realm Role** mapper: **Token Claim Name** `realm_access.roles`, **Multivalued** on, **Add to ID token** on.
- For client roles, a **User Client Role** mapper with **Token Claim Name** `resource_access.awe.roles` and **Add to ID
  token** on.

Roles that are not in the **ID token** are invisible to AWE, even when they are in the access token.

Every user needs an e-mail, a first name and a last name, or Keycloak interrupts the login with an "update your account"
step that an AWE login cannot complete.

### Back-channel logout {#keycloak-back-channel-logout}

In the client settings, **Logout settings**:

- **Backchannel logout URL**: `https://my-app.example.com/logout/connect/back-channel/keycloak`. The last segment is the
  registration id.
- **Backchannel logout session required**: on, so that the logout token carries the session id (`sid`).

Keycloak must be able to open that address from where it runs. AWE ends the session through an internal call to itself:
when the public address is not reachable from the AWE server (a proxy, a load balancer), set
`awe.security.sso.back-channel-logout.logout-uri` as described in [Single sign-on](authentication-sso.md#back-channel-logout).

### AWE configuration {#keycloak-configuration}

```properties title="Keycloak"
# Common baseline: see above
awe.security.sso.roles-claims=realm_access.roles

spring.security.oauth2.client.provider.keycloak.issuer-uri=https://keycloak.example.com/realms/my-realm
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.client-id=awe
spring.security.oauth2.client.registration.keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

Use `https` for `issuer-uri`: AWE validates the issuer, audience, expiry and nonce of the ID token against it.

## Microsoft Entra ID {#microsoft-entra-id}

Entra ID is configured as a plain OpenID Connect provider. This is different from the
`spring-cloud-azure-starter-active-directory` option described in [Single sign-on](authentication-sso.md): in IdP-first
use the OIDC client registration below, so that the roles claim and the other properties work the same as with Keycloak.

### Register the application {#entra-registration}

In the Entra admin center, under **App registrations**:

1. **New registration**. **Supported account types**: single tenant, unless you really serve several organizations.
2. **Redirect URI** (platform **Web**): `https://my-app.example.com/login/oauth2/code/entra`.
3. Under **Certificates & secrets**, create a client secret and note its expiry date: an expired secret stops every login.
   Inject it from an environment variable or a secret vault.
4. Note the **Application (client) ID** and the **Directory (tenant) ID**.

### Send the roles {#entra-roles}

Use **app roles**, not groups: the `groups` claim carries group object ids, not names.

1. In **App roles**, create a role per AWE profile. The **Value** is the role name with the prefix, for example
   `role_administrator`. Allowed member types: **Users/Groups**.
2. In **Enterprise applications**, open the application, then **Users and groups** and assign users or groups to the roles.
   Turn on **Assignment required** in the **Properties** of the application so that unassigned users cannot sign in at all.

Entra ID sends the roles assigned to the user in the `roles` claim of the ID token.

### AWE configuration {#entra-configuration}

```properties title="Microsoft Entra ID"
# Common baseline: see above
awe.security.sso.roles-claims=roles

spring.security.oauth2.client.provider.entra.issuer-uri=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0
spring.security.oauth2.client.provider.entra.user-name-attribute=preferred_username
spring.security.oauth2.client.registration.entra.provider=entra
spring.security.oauth2.client.registration.entra.client-id=${ENTRA_CLIENT_ID}
spring.security.oauth2.client.registration.entra.client-secret=${ENTRA_CLIENT_SECRET}
spring.security.oauth2.client.registration.entra.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.entra.scope=openid,profile,email
spring.security.oauth2.client.registration.entra.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

The AWE user name is the `preferred_username` claim, which in Entra ID is the user principal name (usually the e-mail
address). Create the AWE users with that name, or let auto-provisioning create them.

### Logout {#entra-logout}

Entra ID does not support OpenID Connect **back-channel** logout, only front-channel logout. With Entra ID alone,
a session ended by an administrator in Entra ID stays open in AWE until it expires or the user logs out of AWE. Either
keep a short session timeout (see [Session config](../session.md)) or put Keycloak in front of Entra ID as an identity
broker: AWE then talks to Keycloak and gets back-channel logout, with Entra ID as the identity source.

## Local login and the break-glass administrator {#local-login-and-the-break-glass-administrator}

With `awe.security.sso.local-login=false` the sign-in screen only shows the provider buttons. If the provider is down
you still need a way in: list one or two AWE users in `awe.security.sso.break-glass-users`. They sign in with their normal
local credentials on `/screen/public/signin-local`; everybody else is refused with the same answer as a wrong password.

- Give the break-glass user a long, unique password kept in a vault, and a profile with only the access needed to repair
  the connection to the provider.
- Every attempt is logged at `WARN`; alert on the `[break-glass]` tag.
- Leave the list empty (the baseline above) if you do not want any local way in.

To opt in, add this to the baseline, with the name of an AWE user that you keep only for emergencies:

```properties title="Break-glass opt-in (not part of the baseline)"
awe.security.sso.break-glass-users=admin
```

Details are in [Single sign-on](authentication-sso.md#switching-off-local-login).

## Check that it works {#check-that-it-works}

1. Open the application: you are sent to the provider and, after signing in, back to the initial screen.
2. Sign in with a user whose role maps to a profile: the menu is the one of that profile.
3. Sign in with a user without a mapped role: you get the "no role allowed in this application" error page and no session.
4. Disable the user in AWE (not in the provider) and sign in again: you get the "disabled in the application" page.
5. With Keycloak, sign the user out of all sessions in the admin console: the next request in AWE goes to the sign-in flow.
6. Open `/screen/public/signin-local` as a normal user: the sign-in is refused like a wrong password.

The repository runs the same checks against a real Keycloak in a container, see
[how this is tested](#how-this-is-tested).

## Residual risks {#residual-risks}

| Risk | What to do |
| --- | --- |
| An open AWE session survives the revocation of the user in the provider until it expires. | Back-channel logout (Keycloak) and a short session timeout. Entra ID has no back-channel logout: rely on the timeout. |
| A role change in the provider takes effect at the next login. | Keep the session timeout short if role changes must apply quickly. |
| The ID token is the only source of roles. | Check in the provider that the roles are in the ID token, and review who can assign them. |
| Two-factor authentication of AWE does not run on the SSO path. | Enforce MFA in the provider (Keycloak authentication flows, Entra ID Conditional Access). |
| The break-glass user is a local password account. | Keep the list short, use a vault, alert on `[break-glass]`. |
| The client secret is a credential in configuration. | Inject it from the environment or a vault, rotate it before it expires, and never commit it. |
| Back-channel logout reaches only the node that holds the session. | In a cluster, declare a shared `OidcSessionRegistry` and make sure the internal call reaches the right node, see [Single sign-on](authentication-sso.md#back-channel-logout). |
| Screen and option authorization is not delegated. | Nothing to do: it stays in AWE. Review the AWE profiles and restrictions as usual. |

The full trust matrix is in [IdP-first security mode](idp-first.md).

## How this is tested {#how-this-is-tested}

`KeycloakIdpFirstTest` in `awe-tests/awe-boot` starts a Keycloak container with a test realm and AWE on a random port,
signs users in through the real authorization code flow and checks the login, the profile taken from the roles claims, the
refusal when no role maps, the refusal of a user disabled in AWE and the back-channel logout. It needs Docker and is
skipped without it:

```bash
mvn -pl awe-framework/awe-testing,awe-tests/awe-boot -Pkeycloak -Dskip.frontend=true \
  -Dtest=KeycloakIdpFirstTest -Dsurefire.failIfNoSpecifiedTests=false test
```
