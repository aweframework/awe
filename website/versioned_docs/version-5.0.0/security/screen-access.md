---
id: screen-access
title: Screen access of targets and queries
sidebar_label: Screen access
---

# Access control of maintain targets and queries by screen access

The web application launches maintain targets and queries by name. AWE can check that the signed-in user is allowed to
open at least one screen that uses the target or the query before it runs. The [REST API](#rest-api) is not checked by
screen access: it is authorized by the [permissions](permissions.md) only.

| You want to | Read |
| --- | --- |
| Know what is checked | [How it works](#how-it-works) |
| Know how the REST API is authorized | [REST API](#rest-api) |
| Set the mode or roll it out in an existing application | [Configuration](#configuration), [Rollout](#rollout) |
| Know what is not covered | [Limits](#limits) |
| Find the refusals | [Audit](#audit) |

## How it works

At startup, and every time the descriptors are reloaded, AWE builds an index of the targets and queries that each menu
option uses:

1. It walks every option of the private and the public menu that has a `screen`.
2. It resolves the `<include>` elements of the screen and follows the frames that load another screen.
3. It collects the `target-action` of the components and buttons, the targets and queries of the `initial-load`
   attributes and the `check-target` values.

When a client launches a target or a query that is in the index, it runs only if the session user can open **at least
one** of the menu options whose screen uses it. The rule is the same as opening the screen: the restrictions of the
profiles of the user and of the screen apply (for a user with
[additional profiles](additional-profiles.md#window-access), the union of every profile). The options of the public
menu are open to everyone.

The check applies to the entry points of the web application:

- `/action/...` requests that launch a maintain or a query: `maintain` (also the silent and asynchronous variants),
  `data`, `subscribe`, `value`, `validate`...
- The file endpoints `/file/stream/maintain` and `/file/download/maintain`.

Targets and queries marked `public` are never checked by screen access: they can already be launched without a
session.

A target or a query that no indexed screen uses keeps its previous behaviour, which depends on its attributes:
`public` (no session needed), a signed-in session, or [`server-only`](../api/maintain-definition.md#server-only-targets)
(never from a client).

Launches made by the server are not affected: Java services that call `launchPrivateMaintain` or `launchPrivateQuery`,
and the targets that a maintain includes with `<include target="..."/>`.

:::info
`server-only` and screen access complement each other. `server-only` refuses a target to **every** client request.
Screen access lets through only the users who can open a screen that uses the target.
:::

The [permissions](permissions.md) are a second check after the access by screen: a target or a query that declares a
`permission` also needs the user to have it.

## REST API {#rest-api}

The REST API launches queries and maintain targets without screens, so the requests under `/api/` (`/api/maintain`,
`/api/data` and `/api/public/...`) are not checked by screen access. The rest of the checks still apply:

- The [permissions](permissions.md): a target or a query that declares a `permission` runs only when the user of the
  token has it.
- `public` and [`server-only`](../api/maintain-definition.md#server-only-targets) targets keep their rules.
- A refused request answers HTTP `403` with the generic refusal message, which never names the target, and records an
  `ACCESS_DENIED` [security audit](security-audit.md) event on the `REST` channel.

:::caution
A private target or query without `permission` can be launched through the REST API by any authenticated user. When the
REST API is exposed, set [`awe.security.permissions.require-undeclared=true`](permissions.md#require-undeclared) so
that those targets require `default.read` or `default.write`.
:::

## Configuration

The mode is shared by every authorization of the launches requested by a client: the access by screen
and the [permissions](permissions.md#mode).

| Property | Default | Description |
| --- | --- | --- |
| `awe.security.authorization.mode` | `enforce` | `off`, `warn` or `enforce` |

| Value | Behaviour |
| --- | --- |
| `enforce` | A request that the user is not allowed to make is refused with the same *unauthorized* error as a `server-only` target, and the refusal is audited. |
| `warn` | The refusal is only logged and recorded in the [security audit](security-audit.md) (`ACCESS_DENIED`); the request runs. |
| `off` | No check. |

```properties
# off | warn | enforce (default)
awe.security.authorization.mode=warn
```

## Rollout

An existing application, such as a product migrating from AWE 4, can adopt the check gradually:

1. Set `awe.security.authorization.mode=warn` and run the application with normal use.
2. Read the log and the `ACCESS_DENIED` events of the [security audit](security-audit.md). Each one carries the user and, in
   the detail, the kind and the name of the target or query that would be refused (`RESTRICTED` cause).
3. Review the refusals. Fix the screens or the restrictions that do not match the intended access.
4. Remove the property (or set `enforce`).

`off` keeps the behaviour of AWE 4 while the migration is under way.

## Limits

- The options with a `dynamic-screen-service` and the `menu-screen` options are not indexed, because their screens are
  generated at runtime. The targets and queries that only these screens use are not checked.
- A target or query used both by one of those screens and by a restricted static screen is refused to the users who
  cannot open the static one. Run with `warn` first to find these cases.
- The REST API is not checked by screen access (see [REST API](#rest-api)).
- The check follows the screen access rules. It does not replace the restrictions of your data, which must still be
  enforced by the target and query definitions (for example, with `session` variables).

## Audit

A refusal records an `ACCESS_DENIED` [security audit](security-audit.md) event with the `RESTRICTED` cause and the kind
and name of the target or query as detail (`query: <id>` or `maintain: <id>`), and a warning in the log. In `warn` mode
the same record is written with the detail ending in `(allowed: warn)`, and the request continues.
