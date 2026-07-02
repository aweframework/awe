# Design: Scheduler datasource routing cleanup (GitLab issue #685)

## Context and current state

The scheduler forces a per-task datasource alias from `task.getDatabase()` into `QueryUtil.getParameters(alias,…)`, setting the `_database_` routing key. `Task.database` is dead (never written — `setDatabase` has zero callers), so the alias is always `null`; on the Quartz thread the session `database`/`site` keys are `null` too. Routing therefore already resolves to the configured `spring.datasource` everywhere — the coupling is dead weight. The 2020 defensive join in `getLastExecution`/`getTaskExecution` (`AweSchTsk ⋈ AweSchExe`) returns zero rows when bookkeeping split across datasources, and the downstream read NPEs (#685).

Verified (all in `awe-framework/awe-modules/awe-scheduler`): 14 production `getDatabase()` sites (`TaskDAO.java` 165,196,238,264,397,1051,1082=7; `FileDAO.java` 69,89,110=3; `SchedulerEmailReportService.java:56`=1; `SchedulerMaintainReportService.java:41`=1; `MaintainJobService.java` 109,121=2) — proposal.md's "12" undercounted this; 14 is the authoritative count for `sdd-tasks`. Shared routing beans (`AweRoutingDataSource`, `AweDatabaseContextHolder`, `SessionService`) live in `awe-controller`, gated on `awe.database.multidatabase-enable=true` (default `false`) with many non-scheduler consumers — out of scope.

## Design decisions

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| D1 | Alias-free `getParameters` form | 3-arg call sites (alias+page+max) pass `null` alias via the existing `(String,String,String)` overload; 1-arg alias-only call sites use the existing 0-arg `getParameters()` overload instead — NOT `getParameters(null)`. Do NOT add a `QueryUtil` helper. | `getParameters(null,"1","0")` is arity-unambiguous (proof: `TaskDAO.getAverageTime:1113` already does this). But a bare 1-arg `getParameters(null)` is genuinely ambiguous: `QueryUtil` has three competing 1-arg overloads (`getParameters(ObjectNode)`, generic `getParameters(T)`, `getParameters(String)`) and the compiler cannot resolve a null literal among them — existing tests already work around this with explicit `(String) null` casts (`TaskDAOTest.java:102,259,359`, `FileDAOTest.java:51,69`). `getParameters()` (0-arg) is unambiguous and behaviorally identical for alias-only sites. Keeps the change off shared `awe-model` (rejects Approach C). |
| D2 | `Task.database` field | Remove (`Task.java:57`). | Pure dead read; removal makes the "no session routing" guarantee structural, and its 14 readers are the same alias sites being cleaned. |
| D3 | Dead DAO signature params | Drop `String database`/`alias` from `TaskDAO.getTask/getTaskExecution`, `ServerDAO.findServer`, `CalendarDAO.getCalendar`. | Removes latent coupling. Note `getCalendar`'s alias ripples to ~5 internal `CalendarDAO` callers (134, 230, 388) — not a one-line edit — plus one external hop: `CalendarDAO.insertSchedulerCalendar(String,...)` (132/134) forwards `alias` into `getCalendar`; its only caller, `SchedulerService.insertSchedulerCalendar(String,...)` (425-426), has zero callers repo-wide — delete that dead overload in this same cleanup instead of leaving a silent no-op param. |
| D4 | Dead `*_SITE` constants | Delete `TaskConstants.TASK_SITE`, `CalendarConstants.CALENDAR_SITE`. | Declarations only, zero usages (verified). |
| D5 | `NewSchedulerTask` `db`/`site` writes | Remove the two `<field>` + two `session=`-bound `<variable>` (Maintain.xml 133-134, 158-159). Leave `taskData` commented `db`/`site` (Queries.xml 439-440) commented. | Only the INSERT writes them; `UpdateSchedulerTask` never did. New rows go NULL; nothing reads them back. |
| D6 | `getLastExecution` simplify | Rewrite to `AweSchExe`-only (drop the `Tsk` join + `getMaxDate` subquery filter can stay). | Its caller (`startTask`→`getLastExecutionFromDB`) does not read `name` downstream. |
| D7 | `getTaskExecution` simplify | Simplify to `AweSchExe`-only, but preserve `name` via a scalar correlated subquery on `AweSchTsk.Nam` — **not** a `Tsk` join (maintainer-decided). | **`AweSchExe` has NO `Nam` column**; `name` comes only from `Tsk`, consumed at `TaskDAO.refreshExecutionScreen:926` (drives the `task-id` execution-screen selector). A naive `AweSchExe`-only rewrite would null it and break the screen. A `Tsk` join is rejected: it silently drops the whole `AweSchExe` row when no matching `AweSchTsk` row exists (orphaned/legacy data, direct DB edits, or a future maintain change that fails to cascade) — reproducing a narrower variant of the exact #685 defect class this change exists to eliminate. The scalar subquery is null-tolerant: the row still surfaces, only `name` goes null. XML sketch: a scalar `<field query="getTaskName">` correlated lookup — a new subquery selecting `Nam` from `AweSchTsk` filtered by `Ide` = the outer `AweSchExe.IdeTsk` — added to `getTaskExecution`'s field list. **New pattern**: existing precedent only uses `<filter query="...">` (WHERE, e.g. `getMaxDate`) and `<table query="...">` (FROM/JOIN, e.g. `getAverageTime`) for subqueries; no read `<query>` uses `<field query="...">` in its SELECT list today (proven only in Maintain.xml for INSERT/UPDATE value sourcing). `SQLBuilder.java:96-98`'s `field.getQuery()` confirms the mechanism exists, but `sdd-tasks`/`sdd-apply` must verify the exact syntax for a correlated scalar-subquery SELECT field before implementing. |
| D8 | `getMaxDate` | No change. | Already `AweSchExe`-only (Queries.xml 337-348). |

## Data flow (after change)

    startTask/endTask ──→ QueryUtil.getParameters(); executeJob ──→ QueryUtil.getParameters(null,"1","0")  [no _database_ key]
                                            │
                                            ▼
                              configured spring.datasource  (local: direct; remote scheduler-instance=true: bookkeeping local, business maintain via REST /api/maintain)

## File changes

| File | Action | Description |
|------|--------|-------------|
| `service/scheduled/MaintainJobService.java` | Modify | Line 109 → `getParameters(null,"1","0")`; delete line 121 `parameters.put("database", …)`. Keep the `TaskParameter` loop (112-118). |
| `dao/TaskDAO.java` | Modify | Null-alias (`null,"1","0"`) at 146,196,1065,1082; zero-arg `getParameters()` at 238,264,397 (`startTask`/`endTask`/`changeStatus` — 1-arg alias-only sites, see D1); drop `database` params from `getTask`, `getTaskExecution`, and the `findServer`/`getCalendar` calls (159,165). |
| `service/report/SchedulerEmailReportService.java` | Modify | Line 56 → `getParameters()`. |
| `service/report/SchedulerMaintainReportService.java` | Modify | Line 41 → `getParameters()`. |
| `dao/FileDAO.java` | Modify | Zero-arg `getParameters()` at 69,89,110 — 1-arg alias-only sites, NOT null-alias (see D1). |
| `dao/ServerDAO.java` | Modify | Drop `database` param (39,41 → `getParameters(null,"1","0")`). |
| `dao/CalendarDAO.java` | Modify | Drop `alias` param from `getCalendar` (91,93) + update ~5 internal callers. |
| `service/SchedulerService.java` | Modify | Delete dead `insertSchedulerCalendar(String alias, Integer, boolean, boolean)` overload (425-426) — zero callers repo-wide (D3 ripple via `CalendarDAO.insertSchedulerCalendar`). |
| `bean/task/Task.java` | Modify | Remove `database` field (57); add explicit `serialVersionUID` (mitigates the implicit-UID break this removal causes — see risk note below). |
| `constant/TaskConstants.java`, `constant/CalendarConstants.java` | Modify | Delete `TASK_SITE`, `CALENDAR_SITE`. |
| `global/Maintain.xml` | Modify | `NewSchedulerTask`: remove `db`/`site` fields + `session=` variables. |
| `global/Queries.xml` | Modify | `getLastExecution` → `AweSchExe`-only; `getTaskExecution` → `AweSchExe` + `Nam` scalar subquery (D7). |

**Cross-module / DB-profile consequences**: no `awe-model`/`awe-controller`/shared-bean edits; `awe-database.multidatabase-enable=false` default behavior is identical (alias already null). Multi-database deployments intentionally stop per-task routing (target architecture). Schema (`AweSchTsk.db`/`.site`, `HISAweSchTsk`) untouched across all `schema-*.sql` dialects.

**Serialization risk (Task field removal)**: `Task` is `Serializable` with no explicit `serialVersionUID`; removing `database` (D2) changes the JVM-computed implicit UID. `SchedulerDAO.java:420-422` confirms AWE surfaces JDBC-backed/clustered Quartz `JobStore` metadata (a real, monitored deployment mode) — any such deployment with already-scheduled jobs whose `JobDataMap` holds a serialized `Task` will fail to deserialize (`InvalidClassException`) after this ships. Mitigation: (a) add an explicit `serialVersionUID` to `Task` in this change (cheap; doesn't fully solve this one-time migration but prevents the whole class of future implicit-UID breaks); (b) rollout note — JDBC-backed JobStore deployments must clear/reschedule persisted jobs as part of this upgrade.

## Testing strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit (RED-first) | #685 manual launch (Quartz, no session) completes without NPE | New test on `getLastExecutionFromDB` path returning a row |
| Unit | `database`-named `TaskParameter` passthrough; no `_database_` forced | Assert on `executeJob` params |
| Unit | `getTaskExecution` still returns `name` (D7 regression) | Assert `TaskExecution.name` non-null after the SQL change |
| Unit (churn) | ~21 existing stubs mocking `getParameters(alias,…)` | Update to null-alias (3-arg) or zero-arg `getParameters()` per call site (see D1), same slice (module goes red otherwise) |
| Unit (new) | `SchedulerMaintainReportService.execute()` has zero existing coverage | New test asserting the alias-free `getParameters()` call, mirroring `SchedulerEmailReportServiceTest` |
| Integration | `awe-tests/awe-boot` scheduler flows | `AweSchTsk`/`AweSchExe` DDL at `schema-hsqldb.sql:611-637`,`580-591` |
| Regression | Shared beans unaffected | Re-run `AweDatabaseContextHolderTest`, `SQLQueryConnectorTest`, `QueryTest` (no edits) |

Command: `mvn test -pl awe-framework/awe-modules/awe-scheduler`.

**RED-first repro for the #685 regression test**: seed the test's in-memory DB with an `AweSchExe` execution row for a task while deliberately omitting (or deleting) the matching `AweSchTsk` row. Against the pre-fix `getLastExecution` join, this must legitimately return zero rows — reproducing the exact join failure behind #685 — before the post-fix `AweSchExe`-only query (D6) is shown to return the row instead. This is achievable as a plain unit test: the defect is triggered by row *absence*, not by wiring a real second datasource — the missing `AweSchTsk` row simulates the cross-database bookkeeping split without needing actual multi-datasource routing.

## Review workload and PR slices

**Budget risk is HIGH.** Scope A+B touches ~9 Java files + 2 XML + ~21 test stubs + new tests — **likely over the 800-line single-PR budget.** Recommended 2 slices (SQL split is independently revertible per the rollback plan):

1. **Slice 1 — Alias-free cleanup + field removal**: D1–D5 (all `getParameters` null-alias edits, `Task.database` removal, DAO signature pruning, `*_SITE` deletion, `Maintain.xml`), plus the #685 regression and `database`-passthrough tests and their ~21 stub updates.
2. **Slice 2 — Join simplification + regression**: D6–D7 (`Queries.xml` `getLastExecution`/`getTaskExecution`) plus the `name`-preservation (D7) regression test.

**Flag to orchestrator**: if the combined forecast from `sdd-tasks` confirms >800 lines, raise the split-vs-exception decision with the maintainer before apply, since delivery is `single-pr-default`.

## Decisions summary

- `null`-alias (3-arg) / zero-arg (1-arg) `getParameters` forms per call site (no `awe-model` change); remove dead `Task.database`, DAO alias params, `*_SITE` constants.
- Stop writing `db`/`site` in `NewSchedulerTask`; schema and shared routing beans untouched.
- `getLastExecution` → pure `AweSchExe`; `getTaskExecution` → `AweSchExe` **but preserve `name`** (the one non-obvious correctness constraint: `AweSchExe` lacks `Nam` and the execution screen reads it).
- Deliver as 2 review slices; escalate to the maintainer if `sdd-tasks` confirms >800 lines.
