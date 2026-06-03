# Change Proposal: Safe async request snapshot fallback in `AweMDCTaskDecorator`

## Summary
When `AweMDCTaskDecorator` decorates async tasks outside an active Spring request scope, request parameter snapshot extraction should degrade to an empty `ObjectNode` instead of throwing and breaking `@Async` execution.

## Problem
`AweMDCTaskDecorator.decorate(...)` currently attempts to resolve `scopedTarget.aweRequest` from `RequestContextHolder` at decoration time. In async email/report flows, decoration may occur when request scope is absent or already inactive, causing exceptions that break task submission/execution.

## Goals
- Keep MDC propagation behavior unchanged.
- Make request-parameter snapshot extraction resilient to missing/inactive request scope.
- Ensure async task decoration always produces a valid `RequestDataHolder` payload (empty object when unavailable).

## Non-goals
- No redesign of async executor topology.
- No behavioral changes to downstream services beyond fallback payload shape.
- No unrelated refactors in task configuration classes.

## Impacted area
- `awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweMDCTaskDecorator.java`

## Acceptance intent
- Decorating tasks without request scope does not throw.
- Decorated runnable executes and clears MDC/prototype holder as before.
- Request snapshot defaults to empty JSON object when scope is unavailable.
