---
id: screen-access
title: Screen access of targets and queries
sidebar_label: Screen access
---

# Access control of maintain targets and queries by screen access

A client (the web application or a REST client) launches maintain targets and queries by name. AWE can check that the
signed-in user is allowed to open at least one screen that uses the target or the query before it runs.

| You want to | Read |
| --- | --- |
| Know what is checked | [How it works](#how-it-works) |
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
profile and of the screen apply. The options of the public menu are open to everyone.

The check applies to every client entry point:

- `/action/...` requests that launch a maintain or a query: `maintain` (also the silent and asynchronous variants),
  `data`, `subscribe`, `value`, `validate`...
- The file endpoints `/file/stream/maintain` and `/file/download/maintain`.
- The REST endpoints `/api/maintain`, `/api/data` and `/api/public/...`.

`/api/data` and `/api/public/data` go through the same checks as `/action/data`.

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

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `awe.security.screen-access` | `enforce` | `off`, `warn` or `enforce` |

| Value | Behaviour |
| --- | --- |
| `enforce` | A request that the user is not allowed to make is refused with the same *unauthorized* error as a `server-only` target, and the refusal is audited. |
| `warn` | The refusal is only logged and recorded in the [security audit](security-audit.md) (`ACCESS_DENIED`); the request runs. |
| `off` | No check. |

```properties
# off | warn | enforce (default)
awe.security.screen-access=warn
```

## Rollout

An existing application, such as a product migrating from AWE 4, can adopt the check gradually:

1. Set `awe.security.screen-access=warn` and run the application with normal use.
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
- The check follows the screen access rules. It does not replace the restrictions of your data, which must still be
  enforced by the target and query definitions (for example, with `session` variables).

## Audit

A refusal records an `ACCESS_DENIED` [security audit](security-audit.md) event with the `RESTRICTED` cause and the kind
and name of the target or query as detail, and a warning in the log. In `warn` mode the same record is written and the request continues.
