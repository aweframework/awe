---
id: additional-profiles
title: Additional profiles of a user
sidebar_label: Additional profiles
---

# Additional profiles of a user

A user has one **main profile** and can have **additional profiles**. The main profile decides how the application
looks for the user; the access to the windows, the modules and the sites is the union of every profile of the user.

| You want to | Read |
| --- | --- |
| Know what each profile decides | [Main and additional profiles](#main-and-additional-profiles) |
| Know which windows a user can open | [Window access](#window-access) |
| Check a profile in Java or in a query | [Spring Security roles](#spring-security-roles), [Session values](#session-values) |
| Assign additional profiles | [Administration](#administration), [Single sign-on](#single-sign-on) |
| Upgrade the database or a custom `UserDAO` | [Database](#database), [Custom user DAO](#custom-user-dao) |

## Main and additional profiles

- The **main profile** is the profile of the user (`ope.IdePro`), as before. It alone decides the settings that admit a
  single value: the initial screen, the theme and the `profile` value of the session. The screen configuration is still
  read for the main profile.
- The **additional profiles** are the profiles the user has besides the main one. They are stored in the `AweOpeRol`
  table, one row per user and profile, and never repeat the main profile.
- An **inactive** additional profile (`AwePro.Act` other than `1`) does not count: it gives no window access, no
  modules or sites and no role.

A user without additional profiles behaves exactly as before. Changes to the additional profiles of a user apply to the
session, the roles and the window access at the next login; the [modules and sites](#modules-and-sites) can show them
before.

## Window access

The access to a menu option is computed for each profile of the user, as for a user that only had that profile: the
restriction file of the profile (`profile/*.xml`, set in the *restriction* of the profile, or the default restriction
when it has none), the global restrictions and the `AweScrRes` rows of the profile. Then the results are joined:

- An option is open when **any** profile opens it. Between profiles, allow wins over restrict: if one profile
  restricts an option and another allows it, the user can open it.
- The restriction file of the **user** (the *restriction* of the user), when the user has one, has the last word over
  every profile: it is applied after the union.
- The `AweScrRes` restrictions of the **user** are applied last. An administrator can still close an option to one
  user whatever the profiles and the restriction file of the user allow.
- A restriction file that does not exist opens nothing, as for a user with a single profile.

For example, a user with the main profile `operator`, which restricts the *Reports* option, and the additional profile
`auditor`, which allows it, sees *Reports* in the menu. If an `AweScrRes` row restricts *Reports* to that user, the
option is closed.

The same union applies to the [access by screen](screen-access.md) of the maintain targets and queries that a client
launches (`awe.security.authorization.mode`): a user can launch them when any of the profiles opens a screen that uses
them.

### Modules and sites

The modules and the sites available to the user include those of the additional profiles. They read the additional
profiles of the user on each query, so a change can show there before the next login.

## Spring Security roles

In the web session, the user receives one authority `ROLE_<profile name>` for each profile, the main one and the
additional ones. `hasRole` and the other role checks of Spring Security work with any profile of the user:

```java
@PreAuthorize("hasRole('auditor')")
public void exportAudit() {
  // Open to every user that has the auditor profile, as main or as additional profile
}
```

In the [REST API](../rest-module.md):

- The `local-jwt` mode gives the user of the token the same authorities as the web session: one `ROLE_<profile name>`
  for each profile, the main one and the additional ones.
- In the `oauth2-resource-server` mode, the profiles come from the profile claim of the token
  (`awe.rest.api.oauth2-resource-server.principal.profile-claim`), mapped as the single sign-on login maps its roles:
  the authority prefix is stripped and only the roles that are profile names (compared without case) are kept. The
  first one found in `awe.security.sso.profile-priority`, or else the first in alphabetical order, is the main profile
  and the rest are additional profiles, each one with a `ROLE_<profile name>` authority. When no role maps, the
  default profile applies.

## Session values

The session keeps the values of the main profile (`profile`, `restriction`) and adds these values:

| Session value | Content |
| --- | --- |
| `profiles` | Acronyms of every profile of the user, main profile first, separated by commas. |
| `restrictions` | Restriction files of those profiles, separated by commas. |
| `userRestriction` | Restriction file of the user itself. Only present when the user has one. |

Read them in a query with a `STRING_TO_LIST` variable to filter by every profile of the user:

```xml
<query id="UserProfileNames">
  <table id="AwePro"/>
  <field id="Nam" alias="label"/>
  <where>
    <filter left-field="Acr" condition="in" right-variable="profiles"/>
  </where>
  <variable id="profiles" type="STRING_TO_LIST" session="profiles"/>
</query>
```

A `STRING_TO_LIST` variable is always a list, also when its value comes from the session or holds a single value, so
`condition="in"` works with session values. An empty value is an empty list. See the
[variable types](../api/query-definition.md#variable-types) of the queries.

## Administration

The side panel of the [user administration screen](../guides/default-screens.md#users) (`user-admin`) has an
*Additional profiles* criterion, a multiple selection of profiles:

- Saving the user replaces its additional profiles with the selected ones.
- The main profile is never stored as an additional profile: if the selection contains it, or a user receives as main
  profile one of its additional profiles, that row is removed.
- Deleting a user deletes its additional profiles, and deleting a profile removes it from the users that had it as an
  additional profile.

The grid of the screen shows the additional profiles of each user in the *Additional profiles* column, as tags: the
first two and a *+N* tag whose tooltip lists the others. *View permissions* shows the
[effective permissions](permissions.md#effective-permissions) of the user, those of all its profiles.

## Single sign-on

With [single sign-on](authentication-sso.md) (OAuth2 / OpenID Connect), the main profile is chosen by the
[rule of several roles](authentication-sso.md#several-roles-which-profile-wins): the first profile of
`awe.security.sso.profile-priority` that the provider sends and that exists, else the first one in alphabetical order
that exists. Then:

- When the main profile comes from a provider role that exists as a profile, the other provider roles that exist as
  profiles become the additional profiles of the user. They replace the additional profiles stored for that user (the
  change is audited in `HISAweOpeRol`), for existing users and for users provisioned at that login: the provider is
  the source of truth for the additional profiles of an SSO user.
- Roles without an existing profile are ignored.
- When no role maps to an existing profile (no role, an unknown role, or the default role applied with
  `overwrite-profile-with-default-role`), the additional profiles are not touched and the login follows the
  [previous flow](authentication-sso.md#synchronization-with-the-stored-profile).

On the first login after the provider changes the main role of a user, the session keeps the previous main profile, as
before: the stored main profile is updated and applies from the next login.

## Database

The Flyway migration `AWE_V1.6.0__authorization.sql` creates the `AweOpeRol` table (unique by user and profile),
its audit table `HISAweOpeRol` and the `OpeRolKey` key, for H2, HSQLDB, MySQL, Oracle, PostgreSQL and SQL Server. The
scripts can be applied again. Applications that do not use the Flyway migrations of AWE create these tables themselves
(see the script of their database in `db/migration`).

## Custom user DAO

`UserDAO` has three new default methods:

| Method | Default | Used to |
| --- | --- | --- |
| `supportsAdditionalProfiles()` | Returns `false`. | Let the SSO login write the additional profiles of the user. |
| `findAdditionalProfiles(String userName)` | Returns no profiles. | Load the additional profiles of the user at login. |
| `findProfileId(String profileName)` | Returns an empty `Optional`. | Find the profiles of the provider roles at the SSO login. |

An application with its own `UserDAO` implementation keeps working with the main profile only. Override the three
methods to support additional profiles. While `supportsAdditionalProfiles()` returns `false`, the SSO login leaves the
`AweOpeRol` rows of the user untouched: it neither writes nor deletes them.
