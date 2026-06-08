# Async Request Snapshot Specification

## Purpose

Define the contract for resolving and propagating request-parameter snapshots across asynchronous execution hops so async tasks remain deterministic when request scope is missing or inactive.

## Requirements

### Requirement: Resolve request-parameter snapshot with ordered fallback

When an async task context is created, the system MUST resolve request parameters using this fallback order:
1. Capture from live request scope when an active request-scoped source is available.
2. Reuse an already propagated ancestor snapshot when live request scope is unavailable.
3. Use an empty snapshot when neither live request scope nor propagated ancestor snapshot is available.

The system MUST NOT throw due to missing or inactive request scope during snapshot resolution.

#### Scenario: Active request scope available

- GIVEN a task is being decorated while request scope is active and exposes request parameters
- WHEN the async context snapshot is resolved
- THEN the resolved snapshot MUST equal the current request-scope parameters
- AND the task decoration MUST complete without exception

#### Scenario: No request scope and no prior propagated snapshot

- GIVEN a task is being decorated with no active request scope
- AND no propagated ancestor snapshot exists for the current async context
- WHEN the async context snapshot is resolved
- THEN the resolved snapshot MUST be an empty parameter object
- AND the task decoration MUST complete without exception

#### Scenario: Request scope inactive but prior snapshot exists

- GIVEN a task is being decorated with no active request scope
- AND a propagated ancestor snapshot exists for the current async context
- WHEN the async context snapshot is resolved
- THEN the resolved snapshot MUST reuse the propagated ancestor snapshot
- AND the resolved snapshot MUST NOT be replaced by an empty parameter object

### Requirement: Propagate resolved snapshot across nested async hops

A child async task MUST propagate its resolved request-parameter snapshot to any descendant async tasks it spawns.

When descendants resolve snapshots after the original request scope has ended, they MUST receive the propagated snapshot from the nearest ancestor async context that already resolved one.

#### Scenario: Parent to child to grandchild propagation

- GIVEN a parent async hop resolves a non-empty request-parameter snapshot
- AND the parent spawns a child async hop, and the child spawns a grandchild async hop
- AND the grandchild resolves its snapshot after original request scope is inactive
- WHEN each hop initializes its async context
- THEN the child snapshot MUST equal the parent snapshot
- AND the grandchild snapshot MUST equal the child snapshot
- AND neither child nor grandchild MUST degrade to an empty snapshot while an ancestor snapshot exists
