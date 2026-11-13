---
id: parameter-handling
title: Parameter handling
sidebar_label: Parameter handling
---

AWE services can read parameters from two places:

1. the **live request**, while the HTTP request is still active;
2. a **propagated parameter snapshot**, when code runs later in an async thread.

Use the API that matches what you need to do. Do not use `getRequest()` as the default parameter API.

## API quick reference

| Need | Use | Meaning |
| --- | --- | --- |
| Access the live request object | `getRequest()` | "I need the current request bean." |
| Read parameters safely from the current context | `getRequestParameters()` / `getRequestParameter(...)` | "I want to read parameters safely." |
| Build a parameter object that I will change | `getMutableRequestParameters()` | "I want a snapshot that I am going to modify." |
| Add or replace values in an `ObjectNode` | `putRequestParameter(parameters, name, value)` | "I am preparing parameters for another call." |
| Make new values visible to descendant async tasks | `mergePropagatedRequestParameters(...)` or `putPropagatedRequestParameter(...)` | "Future async hops must inherit these values." |
| Work outside `ServiceConfig` | `QueryUtil` | Lower-level API used by the `ServiceConfig` helpers. |

## The simple rule

:::tip Recommended default
In services, prefer the parameter helpers exposed by `ServiceConfig`:

- `getRequestParameters()` for reading;
- `getMutableRequestParameters()` when you will modify a snapshot;
- `putRequestParameter(...)` to add values to that snapshot.
:::

Use `getRequest()` only when you really need the live request bean itself.

## `getRequest()`

Use `getRequest()` when the code is tied to an active HTTP request and needs request-specific behavior.

Typical use cases:

- read request-only metadata;
- access request token, target action, or servlet-related data;
- update the live request in a synchronous request flow.

```java
String screenName = getRequest().getParameterAsString("screen");
```

:::warning
Do not use `getRequest()` just because you need a parameter value. In async code the live request may no longer exist.
:::

## `getRequestParameters()`

`getRequestParameters()` means:

> "I want to read parameters safely."

Expected use:

- read values;
- inspect parameters from the current execution context;
- avoid depending on a live request in code that may run async;
- do not emphasize modification.

```java
ObjectNode parameters = getRequestParameters();
String userName = getRequestParameterAsString("user", parameters);
JsonNode report = getRequestParameter("report", parameters);
```

How it resolves parameters — two-source merge at read time:

| Source | Description | Priority |
|--------|-------------|----------|
| Live request (`AweRequest`) | HTTP parameters from the current request bean | Base — lowest |
| Propagated snapshot | Pre-merged worker snapshot installed by `AweMDCTaskDecorator` on async threads | Overlay — wins on conflict |

The propagated snapshot is the snapshot that `AweMDCTaskDecorator` assembled at task submission
time from the live request, any ancestor snapshot, and any pending overlay written via
`putPropagatedRequestParameter`.  By the time `getRequestParameters()` runs on an async thread,
all three sources are already collapsed into one snapshot — `getRequestParameters()` itself only
merges two things: the live request (if any) and that pre-assembled snapshot.

When no live request is active (async threads, scheduler jobs), only the propagated snapshot
is used and the result is never `null`.  If neither source is present the method returns an
empty object.

Use this API for safe reads in services, especially when the method can be called from `@Async`, scheduler jobs, child threads, or reusable service flows.

## `getMutableRequestParameters()`

`getMutableRequestParameters()` means:

> "I want a snapshot that I am going to modify."

Expected use:

- build parameters for downstream calls;
- add values to an `ObjectNode`;
- prepare data for maintain, mail, report, print, or service calls;
- keep changes explicit instead of mutating request-scoped state.

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "PdfNam", pdfPath);
putRequestParameter(parameters, "ScrTitFil", fileName);
putRequestParameter(parameters, "report", reportNode);
```

Then pass the object to the downstream operation:

```java
maintainService.launchMaintain("SndRep", parameters);
```

This is the preferred pattern when threaded code prepares data for later maintain, mail, report, or print work.

## Async parameter propagation

AWE can propagate a snapshot of request parameters to child threads, but only when the async task uses an AWE context-aware executor.

The common Spring annotation is:

```java
@Async("threadPoolTaskExecutor")
```

`threadPoolTaskExecutor` is configured with the AWE task decoration needed to copy context. If a project uses another executor, that executor must provide the same context propagation behavior.

:::important
Async propagation gives you a parameter snapshot, not a live `AweRequest`.
:::

**Sibling tasks**: multiple async tasks submitted within the same request all receive the same
propagated overlay — no rewriting is needed between submissions.

**Lifecycle**: the propagated overlay is request-scoped.  It is written on the request thread,
inherited by every `decorate()` call as an immutable snapshot, and cleared exactly once at
request end by `AwePropagationCleanupFilter`.  It never leaks onto a subsequent unrelated request.

## Updating the propagated snapshot

Most code should pass an explicit `ObjectNode` to the next operation. That is usually enough.

Update the propagated snapshot only when new values must be inherited by descendant async hops.

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "PdfNam", reportPath);

mergePropagatedRequestParameters(parameters);
```

For a single value, you can write directly to the propagated snapshot:

```java
putPropagatedRequestParameter("PdfNam", reportPath);
```

The overlay is request-scoped: once written it remains available to every async task decorated
within the same request (sibling tasks included), without any re-write between submissions.
It is cleared automatically at request end — no manual cleanup is required.

## Scenario guide

| Scenario | Read with | Write / prepare with |
| --- | --- | --- |
| Synchronous code that needs the request object | `getRequest()` | `getRequest()` if live request mutation is intended |
| Synchronous service that only needs parameters | `getRequestParameters()` / `getRequestParameter(...)` | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Async service using an AWE executor | `getRequestParameters()` / `getRequestParameter(...)` | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Multiple sibling async tasks in the same request | `getRequestParameters()` on each task | write overlay once on request thread with `putPropagatedRequestParameter(...)` — all siblings inherit it |
| Async flow that calls another async flow | `getRequestParameters()` / `getRequestParameter(...)` | explicit `ObjectNode`; optionally update propagated snapshot |
| Maintain, mail, report, or print after thread work | explicit `ObjectNode` | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Class that does not extend `ServiceConfig` | `QueryUtil` | `QueryUtil` / explicit `ObjectNode` |

## Practical examples

### Safe read

```java
ObjectNode parameters = getRequestParameters();
String userName = getRequestParameterAsString("user", parameters);
```

### Prepare report parameters

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "ScrTit", screenTitle);
putRequestParameter(parameters, "ScrTitFil", fileName);
putRequestParameter(parameters, "PdfNam", pdfPath);

maintainService.launchMaintain("SndRep", parameters);
```

### Prepare mail parameters

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "subject", subject);
putRequestParameter(parameters, "body", body);
putRequestParameter(parameters, "recipients", recipients);

mailService.sendEmail(parameters);
```

## Checklist

Before choosing an API, ask:

- Do I need the live request object, or only parameter values?
- Can this code run in `@Async`, a scheduler, or a child thread?
- Am I only reading parameters?
- Am I building parameters for maintain, mail, report, print, or another service?
- Must descendant async tasks inherit the new values?

## Summary

- Use `getRequest()` only for live request-specific work.
- Use `getRequestParameters()` when the intent is safe parameter reading — it merges the live request with the pre-assembled propagated snapshot (built by `AweMDCTaskDecorator` at submission time) and works on both sync and async threads.
- Use `getMutableRequestParameters()` when the intent is to modify a parameter snapshot.
- Use `putRequestParameter(...)` to add values to that `ObjectNode`.
- Use `mergePropagatedRequestParameters(...)` or `putPropagatedRequestParameter(...)` only when descendant async hops must inherit new values; the overlay is shared by all sibling tasks and is cleaned up at request end.
- Use `QueryUtil` directly only outside `ServiceConfig` or for lower-level infrastructure code.

## Next step

If you are preparing report or print parameters, also review the [Print engine](./print-guide.md) guide.
