---
id: parameter-handling
title: Parameter handling
sidebar_label: Parameter handling
---

AWE supports two different parameter access models:

1. **Live request scope** — used while code is running inside an active web request.
2. **Async snapshot propagation** — used when code runs in child threads after the original request may already be gone.

This guide explains when to use each model, how parameters are propagated, and which APIs are safe in each case.

:::danger Default rule
Do **not** treat `getRequest()` as the default way to access parameters everywhere.

In modern AWE code, especially in services that may be executed from threads, **prefer the async-safe parameter helpers exposed by `ServiceConfig`**.

At a lower level, those helpers delegate to `QueryUtil`.
:::

## Quick path

1. If your code runs with an active HTTP request and really needs the live request bean, use `getRequest()`.
2. If your code can run inside `@Async`, scheduler jobs, or child threads, read parameters through `QueryUtil`.
3. For `@Async` propagation to work, the client application must use an AWE context-aware executor such as `@Async("threadPoolTaskExecutor")`.
4. If async code needs to add or change parameters, use an explicit `ObjectNode` and pass it forward.
5. If those new parameters must also be visible to descendant async hops, merge them into the propagated snapshot holder.

## The two execution contexts

| Context | What exists | Recommended API |
| --- | --- | --- |
| Active request | A live `AweRequest` request-scoped bean | `getRequest()` only if you really need the live request bean |
| Async thread with propagated parent snapshot | No guarantee of live request scope, but propagated parameters may exist | `getRequestParameters()` / `getRequestParameter(...)` |
| Async thread with no request and no prior snapshot | No live request and no propagated parameters | `getRequestParameters()` returns an empty object |

:::tip Preferred mental model
Think of AWE parameter access like this:

- **`getRequest()`** = live request bean
- **`ServiceConfig` helpers** = preferred parameter access API for services
- **`QueryUtil`** = lower-level parameter access abstraction used under the hood
- **propagated snapshot** = what survives across async hops
:::

## How AWE propagates parameters to child threads

When AWE launches **context-aware** async tasks, it captures a **snapshot** of request parameters.

:::important `@Async` must use an AWE executor
If a client application wants parameter propagation in methods annotated with `@Async`, it must use one of the AWE executors.

The most common case is:

```java
@Async("threadPoolTaskExecutor")
```

This is important because **`threadPoolTaskExecutor` is the AWE executor that has the `TaskDecorator` configured**.

If the application uses another executor without that AWE context propagation behavior, the async method may run without the parent parameter snapshot.
:::

That snapshot is propagated in this order:

1. **Live request scope**
2. **Propagated ancestor snapshot**
3. **Empty parameter object**

This means:

- a child thread can still read the parent parameters after the original request has ended
- a grandchild thread can reuse the same snapshot captured by its parent async hop
- async code must not assume that a live `AweRequest` still exists

## Use `getRequest()` only when a live request is required

`getRequest()` is still correct when code is tied to a real web request.

Typical examples:

- reading request-only metadata
- setting request values in controller/request-driven flows
- working with `HttpServletRequest`, request token, or target action during normal synchronous execution

:::warning Do not overuse `getRequest()`
Many developers use `getRequest()` for all parameter access because it is convenient.

That is exactly what should be avoided in async-sensitive services.

If the real need is **"read parameters safely"**, the preferred API is usually `QueryUtil`, not `getRequest()`.
:::

### Example

```java
String screenName = getRequest().getParameterAsString("screen");
```

This is fine when the code runs during a normal request lifecycle.

## Use `ServiceConfig` helpers in async code

If your service extends `ServiceConfig` and may run in a thread, use the async-safe helpers exposed by `ServiceConfig`.

### Async precondition

Before applying the examples below, make sure the async method is using an AWE executor.

```java
@Async("threadPoolTaskExecutor")
public Future<ServiceData> launchSomethingAsync() {
  ...
}
```

If a client project defines its own executor name, it must still be configured with the same AWE context propagation behavior, especially the same `TaskDecorator` strategy. Otherwise, `QueryUtil` will only see explicit parameters or an empty snapshot.

:::important Preferred API for `ServiceConfig`
If your service extends `ServiceConfig`, you do **not** need to inject `QueryUtil` just to read propagated parameters.

Use the helper methods already available on `ServiceConfig`.
:::

### Recommended APIs

```java
ObjectNode parameters = getRequestParameters();
ObjectNode mutableParameters = getMutableRequestParameters();
JsonNode user = getRequestParameter("user");
JsonNode value = getRequestParameter("myParameter", parameters);
String userName = getRequestParameterAsString("user");
String valueAsString = getRequestParameterAsString("myParameter", parameters);
putRequestParameter(mutableParameters, "PdfNam", pdfPath);
putRequestParameter(mutableParameters, "report", reportNode);
putPropagatedRequestParameter("PdfNam", pdfPath);
putPropagatedRequestParameter("report", reportNode);
```

Why this works:

- if a live request exists, the helper reads from it through `QueryUtil`
- if not, it falls back to the propagated async snapshot
- if no snapshot exists, it returns an empty parameter object or `null`

### Example: async-safe read

```java
String userName = getRequestParameterAsString("user", parameters);
```

### Lower-level option

If you are writing infrastructure code or a class that does **not** extend `ServiceConfig`, you can still inject and use `QueryUtil` directly.

## Do not use `getRequest()` as the primary parameter source in async code

In async code, this pattern is fragile:

```java
getRequest().getParameterAsString("user");
```

Why:

- the original request scope may already be inactive
- the request-scoped bean may not exist on the current thread
- async propagation guarantees the **parameter snapshot**, not a live `AweRequest`

:::danger Wrong default in async code
If the question is **"how do I recover parameters here?"**, the default answer should be:

- first think of **`ServiceConfig` parameter helpers**
- then, if needed, of `QueryUtil`
- not `getRequest()`
:::

## How to write parameters in async code

When async code needs to add or change parameters, prefer an explicit mutable `ObjectNode`.

:::tip Good async pattern
In threaded code, work with explicit parameter objects and pass them forward.

This is easier to reason about than mutating request-scoped state.
:::

### Recommended pattern

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "PdfNam", reportPath);
putRequestParameter(parameters, "ScrTitFil", fileName);
```

Then pass that object explicitly to downstream services:

```java
maintainService.launchMaintain("SndRep", parameters);
```

This is the preferred approach for report, maintain, and email flows executed in threads.

## When should async code update the propagated snapshot too?

Sometimes the new parameters created by async code must also be visible to **descendant async hops**.

In that case, after updating the explicit `ObjectNode`, merge it into the current propagated snapshot holder.

### Example

```java
mergePropagatedRequestParameters(parameters);
```

If you only need to propagate one value, you can also write it directly to the propagated snapshot:

```java
putPropagatedRequestParameter("PdfNam", reportPath);
```

This is useful when:

- a parent async step computes new parameters
- child or grandchild async steps must reuse those values later

## Recommended patterns by scenario

| Scenario | Read parameters with | Write parameters with |
| --- | --- | --- |
| Synchronous request flow | `getRequest()` or `getRequestParameters()` | `getRequest()` if request-scoped mutation is intended |
| Async service method using AWE executor | `getRequestParameters()` / `getRequestParameter(...)` | explicit `ObjectNode` + `putRequestParameter(...)` |
| Async flow that calls another async flow | `getRequestParameters()` / `getRequestParameter(...)` | explicit `ObjectNode` + `putRequestParameter(...)` + optional `mergePropagatedRequestParameters(...)` or `putPropagatedRequestParameter(...)` |
| Mail/report maintain executed after thread work | explicit `ObjectNode` passed to service/maintain | explicit `ObjectNode` + `putRequestParameter(...)` |

## Practical examples

### EmailService pattern

For async email sending, prefer recovering `SESSION_USER` from propagated parameters:

```java
String userName = Optional.ofNullable(getRequestParameterAsString(SESSION_USER, parameters))
    .filter(parameter -> !parameter.isEmpty())
    .orElse(Optional.ofNullable(getSession()).map(AweSession::getUser).orElse(null));
```

### ReportGenerator pattern

For report generation, build report parameters explicitly and pass them forward:

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "ScrTit", screenTitle);
putRequestParameter(parameters, "ScrTitFil", fileName);
putRequestParameter(parameters, "PdfNam", pdfPath);
```

Then call the downstream maintain with those parameters:

```java
maintainService.launchMaintain("SndRep", parameters);
```

:::info Summary rule of thumb
- **Need live request object?** use `getRequest()`
- **Need parameters safely, especially in async?** use `ServiceConfig` parameter helpers
- **Need lower-level control outside `ServiceConfig`?** use `QueryUtil`
- **Need to create/update async parameters?** mutate an explicit `ObjectNode` with `putRequestParameter(...)`
- **Need descendants to inherit new async values?** call `mergePropagatedRequestParameters(...)` or `putPropagatedRequestParameter(...)`
:::

## Decision checklist

Use this checklist before writing code:

- [ ] Does this code always run with an active request?
- [ ] Could this code be called from `@Async`, scheduler, or a child thread?
- [ ] Am I reading only propagated parameters, or do I really need the live request bean?
- [ ] If I write parameters here, do I only need them locally, or must descendants also see them?

## Summary

- Use **`getRequest()`** for live request-driven code.
- Prefer **`ServiceConfig` parameter helpers** for async-safe parameter access in services.
- Use **`QueryUtil`** directly only when you are outside `ServiceConfig` or need lower-level control.
- For `@Async`, use an AWE context-aware executor such as **`@Async("threadPoolTaskExecutor")`**.
- Use an explicit **`ObjectNode`** plus **`putRequestParameter(...)`** for async parameter writes.
- Call **`mergePropagatedRequestParameters(...)`** only when descendant async hops must inherit a full parameter snapshot.
- Use **`putPropagatedRequestParameter(...)`** when only one propagated value must be written explicitly.

## Next step

If you are working on printed reports or mail flows, also review the [Print engine](./print-guide.md) guide.
