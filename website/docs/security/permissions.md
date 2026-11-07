---
id: permissions
title: Permissions of queries, maintain targets and services
sidebar_label: Permissions
---

# Permissions

A **permission** names one thing a user can do, such as reading the users or deleting a profile. The code declares the
permissions in a catalog, a query or a maintain target declares the permissions it requires, and the administrator
assigns permissions to **profiles**. A client launches a query, a target or a service that declares a permission only
when the session user has it.

| You want to | Read |
| --- | --- |
| Declare the permissions of a module | [Permission catalog](#permission-catalog) |
| Require a permission to launch a query or a target | [The `permission` attribute](#the-permission-attribute) |
| Require a permission to call a service | [Services](#services) |
| Hide the buttons a user cannot use, or open a panel read-only | [Screen elements](#screen-elements) |
| Give permissions to a profile | [The Profiles screen](#profiles-screen), [Assignment to profiles](#assignment-to-profiles) |
| Ship permissions with a profile, or use the Reader and Contributor profiles | [Profile files](#profile-files), [Predefined profiles](#predefined-profiles) |
| Require a permission for the targets that declare none | [Targets without permission](#require-undeclared) |
| See what a user can do and why | [Effective permissions](#effective-permissions) |
| Check a permission in Java | [Spring Security authorities](#spring-security-authorities) |
| Roll it out or find the refusals | [Mode](#mode), [Audit](#audit) |
| Know what AWE and its modules protect | [Catalog resources](#catalog-resources), [Permissions of the AWE screens](#permissions-of-the-awe-screens) |

## Permission identifiers

A permission identifier is `<resource>.<verb>` or `<resource>.<action>`:

- A **resource** is what the permission protects: `users`, `profiles`, `invoices`...
- A **verb** is a generic operation shared by the resources: `read`, `create`, `update`, `delete`, `export`.
- An **action** is an operation of one resource only: `users.unlock`, `users.reset-2fa`.

The identifiers of verbs, resources and actions use letters, digits, `_` and `-`.

## Permission catalog

Each module of `awe.application.module-list` may provide a catalog in `global/Permissions.xml`. AWE combines the
catalogs of every module, as it combines `Queries.xml` and `Maintain.xml`. This is the catalog of AWE:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <verb id="read" label="PERMISSION_VERB_READ" />
  <verb id="create" label="PERMISSION_VERB_CREATE" />
  <verb id="update" label="PERMISSION_VERB_UPDATE" />
  <verb id="delete" label="PERMISSION_VERB_DELETE" />
  <verb id="export" label="PERMISSION_VERB_EXPORT" />

  <!-- verbs="crud" means read, create, update and delete -->
  <resource id="users" label="PERMISSION_RESOURCE_USERS" verbs="crud">
    <action id="unlock" label="PERMISSION_USERS_UNLOCK" />
    <action id="reset-2fa" label="PERMISSION_USERS_RESET_2FA" />
  </resource>
  <resource id="profiles" label="PERMISSION_RESOURCE_PROFILES" verbs="crud" />
  <resource id="permissions" label="PERMISSION_RESOURCE_PERMISSIONS" verbs="read,update" />
  <resource id="system" label="PERMISSION_RESOURCE_SYSTEM" verbs="read,update" />
  <resource id="access" label="PERMISSION_RESOURCE_ACCESS" verbs="read,update" />
  <resource id="audit" label="PERMISSION_RESOURCE_AUDIT" verbs="read" />
  <resource id="themes" label="PERMISSION_RESOURCE_THEMES" verbs="crud" />

  <!-- Granted to every authenticated user -->
  <resource id="self" label="PERMISSION_RESOURCE_SELF" verbs="read,update" granted="always" />

  <resource id="default" label="PERMISSION_RESOURCE_DEFAULT" verbs="read,write" />
</permissions>
```

| Element | Attributes | Description |
| --- | --- | --- |
| `verb` | `id`, `label` | A verb. The verbs of every module are available to the resources of every module. |
| `resource` | `id`, `label`, `verbs`, `granted` | A resource. It has one permission for each verb of `verbs` (separated by commas, `crud` means `read,create,update,delete`) and one for each `action`. |
| `action` | `id`, `label` | An action of the resource that contains it. |

- The labels are locale keys.
- `granted="always"` gives the permissions of the resource to every authenticated user. The `self` resource of AWE
  uses it, so no profile needs `self.read` or `self.update`. A permission granted always needs no authority: it is
  held by every authenticated user, also with an authentication that the AWE login did not build.
- The `write` verb is declared by AWE and exists only for the `default` resource of AWE, the permission of the
  targets that declare none (see [Targets without permission](#require-undeclared)).
- The schema is `permissions.xsd` (see [XSD tooling](../guides/xsd-tooling.md#published-schemas)).

An application declares its own permissions in the `global/Permissions.xml` of its module:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <verb id="approve" label="PERMISSION_VERB_APPROVE" />
  <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="crud,export,approve" />
</permissions>
```

### Extend or override the AWE permissions {#catalog-override}

The modules are combined in the order of `awe.application.module-list`, and the application module comes first in it.
An application redeclares a resource of AWE to extend it or to change its labels:

```xml title="global/Permissions.xml"
<permissions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/permissions.xsd">
  <!-- Themes keeps read, create, update and delete of AWE and adds export -->
  <resource id="themes" label="PERMISSION_RESOURCE_APP_THEMES" verbs="export" />
  <!-- users.unlock is shown with the label of the application -->
  <resource id="users" label="PERMISSION_RESOURCE_USERS">
    <action id="unlock" label="PERMISSION_USERS_UNLOCK_ACCOUNT" />
  </resource>
</permissions>
```

- **Labels**: the first module that declares a verb, a resource or a permission gives its label. The label of the
  application wins over the one of AWE.
- **Verbs and actions**: the permissions of a resource are the ones of every module that declares it. An application
  adds verbs and actions to a resource of AWE, but it cannot remove them: list only the verbs and actions it adds.
- **The same permission** declared by several modules (`themes.read` listed by the application and by AWE, or the
  `users.unlock` action above) keeps the first declaration, and a warning naming the permission is logged at startup.

### Load errors {#catalog-load-errors}

These declarations are load errors: an identifier with other characters, a resource that lists a verb no module
declares, and a resource other than `default` that lists the `write` verb. A load error fails the startup. On a
[hot reload](../dev-tools.md#xml-hot-reload-backend-definitions) the new catalog is rejected and AWE keeps the previous
one; it also keeps the previous catalog when the new one misses a permission that a query or a target requires.

## The `permission` attribute {#the-permission-attribute}

A `<query>` or a maintain `<target>` declares the permissions it requires in its `permission` attribute:

```xml
<target name="UsrDel" permission="users.delete">
  ...
</target>

<query id="InvoicesToApprove" permission="invoices.read invoices.approve">
  ...
</query>
```

- Several permissions are separated by spaces, and **all of them** are required.
- The permissions are explicit: wildcards are not allowed in the attribute.
- A permission that is not in the catalog, or a `permission` on a `public` query or target, is a **load error**: the
  startup fails and names the query or target and the permission, so a typo never leaves a target unprotected. On a
  hot reload, the changed file is rejected and AWE keeps the previous definitions.
- A query or a target without `permission` is not checked by permission.

The permission is checked on the launches requested by a client: the `/action/...` requests, the file maintain
endpoints and the [REST API](#rest-api) endpoints. The launches made by the server are never checked: Java services
that call `launchPrivateMaintain` or `launchPrivateQuery`, and the targets that a maintain includes with
`<include target="..."/>`. The initial loads of the components of a screen are client launches too: a component whose
query is refused opens without data, and the screen shows one warning however many components were refused.

The permission works together with the [access by screen](screen-access.md): a launch of the web application of a
target that declares a permission runs only when the access by screen allows it **and** the user has every permission.
The permission makes the check stricter; it never opens what the access by screen closes.

### REST API {#rest-api}

The REST API (`/api/maintain`, `/api/data` and `/api/public/...`) is authorized by the permissions only: the
[access by screen](screen-access.md#rest-api) does not apply, so a user that cannot open any screen that uses a target
launches it through the REST API when the user has its permissions.

- A refused request answers HTTP `403` with an `AweRestResponse` of type `ERROR` and the generic refusal message, which
  never names the target. The other errors keep their status: `400` for an unknown query or target, `401` without a
  valid token.
- The refusal is audited as `ACCESS_DENIED` with the `PERMISSION` cause on the `REST` channel.
- A private target or query without `permission` is open to any authenticated user of the REST API. When the REST API
  is exposed, set [`require-undeclared`](#require-undeclared) so that those targets require `default.read` or
  `default.write`.

## Services {#services}

A `<service>` declares the permissions it requires in the same `permission` attribute, with the same rules: several
permissions separated by spaces are all required, and a permission that is not in the catalog is a load error.

```xml
<service id="ChgLogLvl" permission="system.update">
  ...
</service>
```

The permission of a service is checked when a client action calls it (the `/action/...` requests), with the same
[mode](#mode), refusal and [audit](#audit) as a query or a target. These calls are never checked: the server calls of
Java code, the services of a query or a maintain of type `service` (the query or the target is checked instead) and
the services of a `launch-phase`.

Some services only launch the query or the maintain target that the client asks for: `launchQuery`, `launchMaintain`,
`checkUnique`, `checkEmpty`, `launchColumnValues`, `subscribe` and `updateModel`. They are marked
`authorization="target"`: the launched query or target is checked, not the service.

```xml
<service id="launchQuery" authorization="target">
  ...
</service>
```

A service with `authorization="target"` cannot declare a `permission`: that is a load error. See
[Services definition](../api/service-definition.md#service-element).

## Screen elements {#screen-elements}

With `enforce`, AWE removes from a screen the buttons, context buttons and button columns (`component="button"`) that
launch a query, a maintain target or a service the user cannot launch. The server decides it when it sends the screen,
for each user, so both engines get the screen without those elements. With `warn` and `off` the screen is not changed.

### How AWE hides screen elements {#how-awe-hides-screen-elements}

| Question | Answer |
| --- | --- |
| When are elements hidden? | Only with `enforce`. With `warn` the user sees every element and a launch without permission runs: it is only logged and [audited](#audit). With `off` nothing is checked. |
| Which elements? | Buttons, context buttons and button columns of the grids. |
| When is a button removed? | When one of its actions launches a query, a maintain target or a service whose permission the user does not have. |
| And a target that declares no permission? | It requires nothing, so it never removes a button, unless [`require-undeclared`](#require-undeclared) is on: then a query requires `default.read` and a maintain target or a service `default.write`. Public and server-only targets never require anything. |
| What if the button launches nothing? | A button that only opens a panel, such as *New* or *Duplicate*, has no target to derive from: it declares its own [`permission`](#the-permission-attribute-of-an-element), which replaces the derived decision. |
| And the panels? | A panel opens read-only (**view mode**) when the action that opens it declares an [`edit-permission`](#view-mode-of-a-panel-edit-permission) the user does not have, or when all its save buttons were removed. |
| Is this the security? | No. Hiding is presentation only: the server checks every launch, so a hidden button that someone calls by hand is still refused. |

An example: an invoices screen and a user who only has `invoices.read`.

```xml
<!-- Opens the panel to create: nothing to derive, so it declares its permission -->
<button id="ButNew" label="BUTTON_NEW" permission="invoices.create">
  <button-action type="dialog" target="PnlInv" />
</button>
<!-- InvDel declares permission="invoices.delete" -->
<button id="ButDel" label="BUTTON_DELETE">
  <button-action type="server" server-action="maintain" target-action="InvDel" />
</button>
<!-- InvSnd declares no permission -->
<button id="ButSnd" label="BUTTON_SEND">
  <button-action type="server" server-action="maintain" target-action="InvSnd" />
</button>
<!-- Opens the same panel to edit -->
<column name="EdtBtn" component="button" icon="lucide:pencil">
  <button-action type="dialog" target="PnlInv" edit-permission="invoices.update" />
</column>
```

| Element | What the user sees | Why |
| --- | --- | --- |
| *New* | Removed | Its `permission` attribute asks for `invoices.create`. |
| *Delete* | Removed | It launches `InvDel`, which requires `invoices.delete`. |
| *Send* | Shown | `InvSnd` declares no permission. With `require-undeclared` it requires `default.write` and is removed. |
| Pencil | Shown, opens the invoice read-only | The opening asks for `invoices.update` in `edit-permission`. |

The rules in detail:

- An element is removed when one of its `button-action` launches a target whose permissions the user does not have:
  the service of its `server-action`, or its `target-action` when that service is marked `authorization="target"`.
- A target without `permission`, or with a permission granted always (`self`), never removes anything. With
  [`require-undeclared`](#require-undeclared), a private target without `permission` requires the permission of the
  `default` resource, and the element follows it.
- When the removed context buttons leave a `context-separator` at the start or the end of the menu, or next to another
  separator, the separator is removed too.
- Only the permissions decide: the [access by screen](screen-access.md) of a target does not remove its button.

### The `permission` attribute of an element

A `button`, a `context-button` or a button `column` may declare `permission`, with the same rules as on a query (several
permissions separated by spaces are all required, and a permission that is not in the catalog is a load error). It
replaces what is derived from the actions: the element is kept when the user has every permission, and removed
otherwise. Use it for an element that launches no target, such as a button that only opens a panel, or to protect an
element with other permissions than its targets.

Without it, a *New* button that only opens a panel stays visible to a user who cannot create, and the panel opens empty
in view mode. The *New* and *Duplicate* buttons of the AWE screens declare the permission of the maintain target that
creates the record:

| Screens | `permission` |
| --- | --- |
| Users | `users.create` |
| Profiles (*New* and *Duplicate*) | `profiles.create` |
| Themes | `themes.create` |
| Sites, modules, databases, email servers, queues, sequences and application parameters (*New*, and *Duplicate* where it exists) | `system.update` |
| Scheduler tasks, dashboard, calendars and servers (`awe-scheduler`) | `scheduler.update` |
| Subscriptions (`awe-notifier`) | `subscriptions.create` |
| Literals manager (`awe-developer`) | `developer.update` |

A descriptor test of AWE (`CreateButtonPermissionTest`) checks that every button that opens a panel to create declares
it, so a new screen cannot forget it. A test of the same kind keeps the screens of your application in line.

### View mode of a panel: `edit-permission`

A dialog or a side panel whose save buttons are all removed opens in **view mode** from every action that opens it,
without declaring anything. A save button is a `button` or a `context-button` of the panel with a `button-action` that
launches a maintain target, or a service that is not marked `authorization="target"`. A panel that has no save button
is not affected, and neither is a panel where the user keeps at least one save button: this is the case of a panel
shared to create and to edit, for a user who can create but not edit, so it still needs `edit-permission`.

A side panel or a dialog that serves both to create and to edit only knows its mode through the action that opens it.
A `button-action` of type `dialog` may declare `edit-permission`: when the user does not have every permission it
lists, that opening shows the panel in **view mode**. Its criteria are read-only and no dependency of the screen
(`set-readonly` included) can make them editable while it is open; the next opening without view mode is editable
again. The buttons of the panel follow the general rule, so a save button whose target the user cannot launch is
removed.

```xml
<button-action type="dialog" target="PnlUsr" edit-permission="users.update" />
```

### Example: user administration

The `user-admin` screen of AWE opens the same side panel `PnlUsr` to create and to edit a user:

```xml
<!-- It only opens the panel, so it declares the permission of the creation -->
<button id="ButNew" label="BUTTON_NEW_USER" permission="users.create">
  ...
  <button-action type="dialog" target="PnlUsr" />
</button>

<!-- The pencil of each row and the Edit context button open the panel to edit -->
<column name="EdtBtn" component="button" icon="lucide:pencil" title="BUTTON_EDIT">
  ...
  <button-action type="dialog" target="PnlUsr" edit-permission="users.update" />
</column>
```

A user with `users.read` only gets the list without the **New user** button and without the delete, unlock and reset
2FA buttons, and the pencil opens the user read-only, without the save button. A user with `users.*` gets the screen
as before.

:::caution Limitation
A target launched under a name built at run time (custom JavaScript or React code) cannot be derived: its button stays
visible and the server still refuses the launch.
:::

## Assignment to profiles

An administrator assigns permissions in [the Profiles screen](#profiles-screen). The table `AweProPer` stores them, one
row per profile and permission:

| Column | Content |
| --- | --- |
| `IdeAweProPer` | Identifier of the row (key `ProPerKey` of `AweKey`) |
| `IdePro` | Profile (`AwePro.IdePro`) |
| `Per` | Permission identifier or wildcard. Unique by profile. |

`Per` holds a permission of the catalog or a wildcard:

| Value | Grants |
| --- | --- |
| `users.delete` | That permission |
| `*` | Every permission of the catalog |
| `users.*` | Every permission of a resource |
| `*.read` | One verb or action on every resource |

The wildcards are expanded against the catalog, so the effective permissions of a user are always a list of real
permissions. There are **no negative permissions**: a profile cannot remove a permission that another assignment gives.

The table stores the identifier as text, so the catalog stays in the code. An assignment that matches no permission of
the catalog is logged at startup and ignored; it is never deleted, so it applies again if the permission comes back.

The AWE migration `AWE_V1.6.0__authorization.sql` creates `AweProPer` (with its audit table `HISAweProPer`) and gives
the administrator profile (`ADM`) the assignment `*`. To give a profile the user and profile administration:

```sql
-- Profile with the acronym MGR: every permission of the users and of the profiles
INSERT INTO AweProPer (IdeAweProPer, IdePro, Per)
SELECT k.KeyVal + 1, p.IdePro, 'users.*' FROM AwePro p, AweKey k WHERE p.Acr = 'MGR' AND k.KeyNam = 'ProPerKey';
INSERT INTO AweProPer (IdeAweProPer, IdePro, Per)
SELECT k.KeyVal + 2, p.IdePro, 'profiles.*' FROM AwePro p, AweKey k WHERE p.Acr = 'MGR' AND k.KeyNam = 'ProPerKey';
UPDATE AweKey SET KeyVal = KeyVal + 2 WHERE KeyNam = 'ProPerKey';
```

### The Profiles screen {#profiles-screen}

The *Permissions* field of the side panel of the [Profiles](../guides/default-screens.md#profiles) screen edits the
assignments of a profile. It is a multiple selection of the catalog of the application (AWE, its modules and the
application itself), in the language of the user and grouped by resource:

| Group | Offers |
| --- | --- |
| *All resources* | *All* (`*`, every permission) and each verb used by several resources, on every resource: *Read* is `*.read`. |
| One group per resource | *All* (`users.*`, every permission of the resource), then each verb and each own action of the resource. An own action, such as *Unlock* of the users, or a verb of only one resource, such as *Write* of *Default*, is only offered here. |

- The permissions granted always (`self`) are not offered: every signed-in user has them.
- Saving the profile stores the permissions of the field. The assignments that the field does not offer, such as a
  permission no longer in the catalog, are kept.
- The changes apply at the **next sign-in** of each user of the profile.
- A user without `permissions.read` does not see the field, and without `permissions.update` sees it read-only; the
  rest of the profile can still be saved.
- Deleting a profile deletes its assignments. Every change is audited in `HISAweProPer`.
- The field shows the assignments only. The permissions of the [profile file](#profile-files) are not in it: the
  *Permissions* tab of [Access control](../guides/default-screens.md#permissions) shows every permission of a profile
  with its origin.

## Profile files {#profile-files}

A profile can also get permissions from its **profile file**: the file `profile/<name>.xml` named in the *restriction*
of the profile (`AwePro.Res`), the same file that holds its screen restrictions. A profile without restriction uses the
default file (`awe.security.default-restriction`, `general` by default). The profile file is code: it is versioned and
travels with the application, while the assignments of `AweProPer` are data of each environment.

```xml title="profile/contributor.xml"
<profile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/profile.xsd">
  <restriction option="users" />
  <restriction option="profiles" />
  <permission id="*" />
  <exclude id="users.*" />
  <exclude id="profiles.*" />
  <exclude id="permissions.*" />
</profile>
```

| Element | Attribute | Description |
| --- | --- | --- |
| `permission` | `id` | A permission of the catalog or a wildcard (`*`, `users.*`, `*.read`), as in `AweProPer`. |
| `exclude` | `id` | A permission or a wildcard subtracted from the permissions of this file. |

- The permissions of a profile are the permissions of its file **minus** its exclusions, **plus** its assignments.
- An exclusion only subtracts inside its own file: it is no denial. An assignment of the profile, or another profile of
  the user, can still grant the permission. Exclusions exist only in profile files.
- An identifier that matches no permission of the catalog is a load error: the startup fails and names the profile and
  the entry. On a hot reload AWE keeps the previous profiles.
- Only the profile files give permissions: the restriction file of a user does not.
- `administrator.xml` grants nothing. The administrator profile (`ADM`) gets `*` as an assignment from the migration,
  so a profile of the application that reuses that file does not get every permission.

## Predefined profiles {#predefined-profiles}

AWE ships two profiles whose permissions are in their profile files:

| Profile | Acronym | Profile file | Permissions |
| --- | --- | --- | --- |
| Reader | `RDR` | `reader.xml` | `*.read`: reads every resource and changes nothing. |
| Contributor | `CTB` | `contributor.xml` | `*` except `users.*`, `profiles.*` and `permissions.*`. Its file also restricts the *Users* and *Profiles* options. |

- AWE creates them at startup when no profile with their acronym exists. When one exists, it is kept and a warning is
  logged. `awe.security.permissions.predefined-profiles=false` switches the creation off.
- Their wildcards cover the permissions of the application too: Reader reads the resources of your catalog, and
  Contributor gets them all.
- They are read-only: the *Profiles* screen shows them without *Save changes*, and the update and delete of a
  predefined profile are refused (*The predefined profile ... cannot be changed or deleted. Duplicate it and change the
  copy.*).
- To adapt one, **duplicate** it: *Duplicate profile*, in the panel or in the menu of a row, turns the panel into a
  copy with an empty acronym and name, and *Create copy* creates a normal profile whose assignments are the effective
  permissions of the original (file minus exclusions, plus assignments). Change the copy as any other profile. It does
  not use the file of the predefined profile; choose its restriction if it needs one. *Duplicate profile* works for any
  profile and needs `profiles.create`.
- A profile is predefined when it has both the acronym and the profile file of one of them: a profile of your
  application with the acronym `RDR` and another file is a normal profile.

## Targets without permission: `require-undeclared` {#require-undeclared}

By default, a query, a maintain target or a service without `permission` is not checked by permission. With
`awe.security.permissions.require-undeclared=true`, it requires the permission of the `default` resource when a client
launches it:

| Target without `permission` | Requires |
| --- | --- |
| Private query | `default.read` |
| Private maintain target | `default.write` |
| Service called by an action | `default.write` |

- Not affected: `public` and [`server-only`](../api/maintain-definition.md#server-only-targets) targets, the services
  marked `authorization="target"` (their launched target is checked) and the launches made by the server.
- The `default` resource is in the catalog of AWE (*Default · Read* and *Default · Write* in the Profiles screen).
  `*` includes `default.write` and `*.read` includes `default.read`, so the administrator, Contributor and Reader keep
  working.
- The [screen elements](#screen-elements) follow it: a button that launches such a target is removed for the users
  without its `default` permission.
- In `warn` mode the startup inventory still lists these targets and says that they require `default.read` or
  `default.write`.
- Switch it on when the [REST API](#rest-api) is exposed: the REST API is not checked by screen access, so without it a
  private target without `permission` can be launched by any authenticated REST user.

:::caution Before you switch it on
- Every existing profile needs `*` or `default.*` (or at least `default.read`), or its users lose every query and
  target that declares no permission.
- Keep the `default` resource of AWE in the catalog of the application: without it nobody can hold `default.read` or
  `default.write`, and no target without permission can be launched.
- Run with `awe.security.authorization.mode=warn` first and read the refusals.
:::

## Effective permissions

The effective permissions of a user are the union of the permissions of the main profile and of the active
[additional profiles](additional-profiles.md), plus the permissions granted always. Each profile gives its
[profile file](#profile-files) minus its exclusions, plus its assignments. They are loaded at login, so a change of the
assignments, of the profile files, of the profiles of a user or of the catalog applies at the next login.

The *Permissions* tab of the [Access control](../guides/default-screens.md#permissions) screen shows them for a user or
a profile, with the origin of each one.

## Spring Security authorities

At login each effective permission becomes a Spring Security authority `PERM_<id>`, next to the `ROLE_` authorities of
the profiles. Application services can check them:

```java
@PreAuthorize("hasAuthority('PERM_users.delete')")
public void deleteUser(Integer userId) {
  // Only for the users with the users.delete permission
}
```

The user gets the same `PERM_` authorities in the web session, with the local login and with the
[single sign-on](authentication-sso.md) web login, and with a `local-jwt` token of the [REST API](../rest-module.md).

In the `oauth2-resource-server` mode of the REST API the user is built from the token, without a user in the database:
the roles of its profile claim are mapped to AWE profiles as the [single sign-on](authentication-sso.md) login maps
them (profile names, without case), and those profiles get the `PERM_` authorities the local login gives them, read on
every request. A role that is not an AWE profile name grants no permission.

## Mode

The check follows `awe.security.authorization.mode`, the same property as the
[access by screen](screen-access.md#configuration):

| Value | Behaviour |
| --- | --- |
| `enforce` (default) | A launch without the permissions is refused with the *unauthorized* error, logged and audited. |
| `warn` | The launch is logged and audited (the audit detail ends with `(allowed: warn)`), and it runs. |
| `off` | No check. |

A refused launch always answers with the title *Access denied* and the message *You do not have permission to perform
this action. Contact your administrator if you need it.*, never with the error answer of the action that launched it,
so a refused `control-unique-cancel` does not read as an *already exists* message. The message never names the refused
query, target or service: that identifier is a technical detail kept in the log and the [audit](#audit).

Run an existing application with `warn` first and read the refusals before you use `enforce`, as in the
[rollout of the access by screen](screen-access.md#rollout).

In `warn` mode AWE also logs at startup the inventory of what is left to declare: the queries, maintain targets and
services that the screens of the private menu launch and that declare no `permission`. Public and server-only targets,
targets of the screens of the public menu and services with `authorization="target"` are not listed. The log has one
warning with the count and one line per kind, and nothing is logged when every target is declared:

```text
3 targets used by private screens declare no permission (authorization mode is warn):
  query: CusLst, OrdLst
  service: exportOrders
```

With [`require-undeclared`](#require-undeclared) the first line ends with `(authorization mode is warn, they require
default.read or default.write)`.

## Audit

A launch without the permissions records an `ACCESS_DENIED` [security audit](security-audit.md) event with the
`PERMISSION` cause and the detail `query: <id>`, `maintain: <id>` or `service: <id>`, and a warning in the log. In
`warn` mode the same record is written, its detail ends with `(allowed: warn)` (for example
`maintain: InvDel (allowed: warn)`) and the request continues, so the audit tells a launch that `warn` let through
from a refused one. The permissions of the user are never logged nor audited.

## Properties {#properties}

| Property | Default | Description |
| --- | --- | --- |
| `awe.security.authorization.mode` | `enforce` | `off`, `warn` or `enforce`. See [Mode](#mode). |
| `awe.security.permissions.predefined-profiles` | `true` | Create the [predefined profiles](#predefined-profiles) Reader and Contributor at startup. |
| `awe.security.permissions.require-undeclared` | `false` | Require `default.read` or `default.write` for the private targets that declare no permission. See [Targets without permission](#require-undeclared). |

They are also listed in the [properties reference](../properties#awe-security-properties).

## Catalog resources {#catalog-resources}

AWE and its modules declare one resource per administration area. Only the administrator profile (`ADM`) gets them
from the migration, and the [predefined profiles](#predefined-profiles) from their files: give the other profiles the
permissions of the areas they use.

| Resource | Permissions | Module | Covers |
| --- | --- | --- | --- |
| `users` | `crud`, `unlock`, `reset-2fa` | AWE | User administration |
| `profiles` | `crud` | AWE | Profile administration |
| `permissions` | `read`, `update` | AWE | Permission assignments of the profiles |
| `system` | `read`, `update` | AWE | Sites, modules, databases, email servers, queues, sequences, application parameters, report configuration, log, encryption tools and messages to users |
| `access` | `read`, `update` | AWE | Access by screen and screen configuration of profiles and users |
| `audit` | `read` | AWE | Security audit |
| `themes` | `crud` | AWE | Theme administration |
| `self` | `read`, `update` (granted always) | AWE | Own settings of the user: user data, password, 2FA, language, theme, grid layouts, saved searches, favourites, notifications |
| `scheduler` | `read`, `update`, `execute` | `awe-scheduler` | Scheduler tasks, servers and calendars (`read`, `update`); launch of tasks and start and stop of the scheduler (`execute`) |
| `developer` | `read`, `update` | `awe-developer` | Literals manager and path of the developer tools |
| `sql-extractor` | `read`, `update` | `awe-tools` | SQL extractor and its saved files |
| `subscriptions` | `crud` | `awe-notifier` | Subscriptions administration (the notifications of a user are `self`) |

Some selectors and suggests of the administration screens declare the permission of their area: `DbsSel`, `DbsSug`,
`SitSel`, `SitSug`, `ModSelBD`, `ModSug`, `ThmSel`, `UsrActSel`, `UsrActSug`, `CatParTyp` and `FntLst` require
`system.read`, and `GetPrnLst` and `SelEmlSrvNom` require `users.read`. A screen of the application that reuses them
needs that permission too, or a query of its own.

## Permissions of the AWE screens

The user administration and profile administration screens of AWE declare these permissions:

| Permission | Queries | Maintain targets |
| --- | --- | --- |
| `users.read` | `UsrLst`, `UsrChkUni`, `UsrStaCnt`, `UsrAddProSel`, `UsrAddProIni` | |
| `users.create` | | `UsrNew` |
| `users.update` | | `UsrUpd` |
| `users.delete` | | `UsrDel` |
| `users.unlock` | | `UsrUnl` |
| `users.reset-2fa` | | `reset2fa` |
| `profiles.read` | `ProLst`, `ProStaCnt`, `check-profile-acronym`, `ProPerAcc`, `ProPdfSel` | |
| `profiles.create` | | `ProNew`, `ProDup` |
| `profiles.update` | | `ProUpd` |
| `profiles.delete` | | `ProDel` |
| `permissions.read` | `profilePermissions`, `allProfilePermissions`, `ProPerCat`, `ProPerSel` | |
| `permissions.update` | | `ProPerUpd` (server only) |
| `users.read` and `permissions.read` | `access-permissions`, `access-permission-counts`, `access-permission-summary`, `access-permission-why`, `access-permission-why-result` | |

`profilePermissions` and `allProfilePermissions` read the assignments of `AweProPer`. AWE launches them from the
server, at login and at startup, so they are never checked; the permission only protects them from a client.

`ProNew` and `ProUpd` store the permissions of the profile through `ProPerUpd` only when they change, and then the user
also needs `permissions.update`. `ProDel` deletes the assignments of the profile, and every change of `AweProPer` is
audited in `HISAweProPer`.
