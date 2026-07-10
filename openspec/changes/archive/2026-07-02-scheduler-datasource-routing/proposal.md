# Change Proposal: Scheduler datasource routing cleanup (GitLab issue #685)

## Summary
Make the AWE scheduler stop depending on `site`/`database` session state for datasource selection so it always uses the single configured `spring.datasource` — fixing the manual-launch `NullPointerException` in GitLab issue #685 — and harden the fix by removing the dead `Task.database` read path and simplifying the 2020 defensive cross-datasource join back to a plain single-table query. The shared multi-database routing beans and the database schema are deliberately left untouched.

## Intent
Guarantee that scheduler bookkeeping and task execution behave identically whether the scheduler runs embedded in the main app or as a separate REST-connected instance: always against the one configured datasource, never influenced by `site`/`database` session variables (which are `null` on the Quartz worker thread). Once routing is unconditionally consistent by construction, the 2020 consistency safety-net join that surfaces the NPE is no longer needed and is removed rather than retained.

## Problem statement
Issue #685: launching a scheduler task manually throws an NPE. Root cause (verified — Engram `discovery/root-caused-issue-685-scheduler-npe`): the scheduler forces a per-task datasource alias from `task.getDatabase()`, but that field is a dead read path (never populated since 2019) so it is always `null`. On the Quartz thread there is also no bound session, so the session `database` key is `null` too. The 2020 defensive join in `getLastExecution`/`getTaskExecution` (`AweSchTsk` ⋈ `AweSchExe`) then returns zero rows when task and execution bookkeeping ended up on different datasources, and the downstream read of that empty result NPEs. The scheduler's whole session/site-database coupling never actually routes anything useful for the scheduler — it is dead weight whose presence causes the failure.

## Goals
- Remove the scheduler's dependency on session-based datasource routing: no scheduler code path considers `site`/`database` session variables.
- Replace every `queryUtil.getParameters(<task-database-alias>, …)` scheduler call with its alias-free form so calls fall through to the configured default datasource.
- Remove the dead `Task.database` Java field and the 12 read callers that exist only to feed the alias forcing being removed.
- In `MaintainJobService.executeJob()`, remove the alias forcing and the `parameters.put("database", task.getDatabase())` convenience key, while leaving the `TaskParameter` loop untouched so a task parameter literally named `database` flows through like any other parameter (no special casing).
- Stop the `NewSchedulerTask` maintain target from writing the `session="database"`/`session="site"` variable bindings, so new tasks no longer populate those columns from session state.
- Simplify the 2020 defensive join in `getLastExecution`/`getTaskExecution` back to a plain query on `AweSchExe` alone.
- Add a regression test reproducing #685 (manual launch on the Quartz thread, no session, completing without NPE) that previously failed and now passes.
- Keep behavior identical in the default single-database deployment; change behavior intentionally only in multi-database deployments (scheduler stops honoring a per-task alias, always uses the configured datasource — the target architecture).

## Non-goals
- **Do NOT delete or modify the shared session-based routing beans** — `AweRoutingDataSource`, `AweDatabaseContextHolder`, and `SessionService`. They back a real, separate, currently-disabled-by-default (`awe.database.multidatabase-enable=false`) interactive multi-database feature used by `SQLQueryConnector`, `MaintainService`, and reporting. This change removes only the *scheduler's dependency* on that mechanism, never the mechanism itself.
- **Do NOT touch the database schema or model.** `AweSchTsk.db` and `AweSchTsk.site` (and the `HISAweSchTsk` audit mirror) columns stay in place, permanently legacy/unused going forward. No migration to drop or rename them. Existing historical rows are untouched.
- No changes to the shared `QueryUtil` API in `awe-model` (prefer the alias-free/`null`-alias form to keep the change inside the scheduler module — see open questions).
- No new datasource configuration shipped by the scheduler starter; remote-mode "same datasource as the main app" is a deployment/config concern, not code.

> **Note on a common point of confusion (from the investigation history):** the shared routing beans look scheduler-related but are shared web-request infrastructure with many non-scheduler consumers, and the `db`/`site` columns look removable but the schema is frozen. Both are intentionally out of scope. Only the scheduler's *use* of session routing is removed.

## Scope
In scope for later spec/design/apply phases:
- Scheduler Java call sites (`TaskDAO`, `MaintainJobService`, report services, `FileDAO`, `ServerDAO`/`CalendarDAO` routing) switched to alias-free datasource resolution.
- Removal of `Task.database` and its now-dead read callers.
- `Maintain.xml` `NewSchedulerTask` target: remove `session="database"`/`session="site"` bindings and the two `<field>` writes they feed.
- `Queries.xml` `getLastExecution`/`getTaskExecution`: simplify to `AweSchExe`-only.
- Update the ~21 existing scheduler test stubs that mock the changing `getParameters(alias, …)` overloads, plus add the #685 regression test and a test asserting a `TaskParameter` named `database` passes through verbatim.

Out of scope:
- Database schema/DDL changes.
- Shared routing beans (`AweRoutingDataSource`, `AweDatabaseContextHolder`, `SessionService`) and shared `QueryUtil`.
- Request-logging MDC `database` handling and the interactive multi-database feature.

## Proposed high-level approach
1. **Alias-free datasource resolution.** At every scheduler call site, drop the datasource-alias argument to `queryUtil.getParameters(...)` (alias-free form / `null` alias) so paging is preserved but no `_database_` routing key is forced. In the default deployment this is behavior-identical; in multi-database deployments it intentionally always targets the configured datasource.
2. **Remove the dead field.** Delete `Task.database` (and its getter usages), which makes the "no session routing" guarantee structural rather than incidental. All 12 read callers are the same routing sites being cleaned up in step 1, so removal and cleanup are one edit.
3. **`MaintainJobService.executeJob()`.** Remove the alias forcing on the parameters build and delete `parameters.put("database", task.getDatabase())`. Leave the `TaskParameter` loop untouched — no special casing for a parameter named `database`.
4. **Stop writing session-derived columns.** In `Maintain.xml`'s `NewSchedulerTask`, remove the `session="database"`/`session="site"` variable bindings and the `db`/`site` `<field>` writes they source. Columns become NULL for new rows; nothing reads them back at execution time, so there is no runtime effect. Do NOT uncomment the dead `db`/`site` mapping in `taskData` (`Queries.xml`).
5. **Simplify the 2020 join.** Rewrite `getLastExecution`/`getTaskExecution` back to plain `AweSchExe` queries (pre-2020 shape), since unified routing makes the cross-table consistency check unnecessary. Add regression coverage for the single-row result before/with the SQL change.
6. **Remote mode is configuration.** A separate scheduler instance points its `spring.datasource` at the same DB as the main app; no additional code beyond the above.

## Compatibility and backward-compatible behavior
- **Default single-database deployments: no behavior change.** `task.getDatabase()` is already `null` and `multidatabase-enable` defaults to `false`, so alias-free calls resolve to the same default datasource they already hit.
- **Multi-database deployments: intentional behavior change.** The scheduler stops attempting per-task datasource routing and always uses the configured datasource — the documented target architecture, not a regression.
- **Schema/data compatibility:** `db`/`site` columns and existing rows are untouched; new rows simply leave them NULL. No forward/backward data-migration concerns because those columns are never read at execution time.
- **Shared feature compatibility:** the interactive multi-database feature and request-logging MDC are unaffected because the shared beans are not modified.
- **SQL simplification compatibility:** the simplified `AweSchExe`-only queries must return the same single row the callers expect; covered by regression tests before the SQL change lands.

## Affected areas
Java (`awe-framework/awe-modules/awe-scheduler`):
- `.../dao/TaskDAO.java` — alias-free `getParameters` at lines ~146, 196, 238, 264, 397, 1065, 1082; dead `String database`/alias params through `getTask`, `getTaskExecution`, `getCalendar`, `serverDAO.findServer`.
- `.../service/MaintainJobService.java` — remove alias forcing (line ~109) and the `parameters.put("database", …)` convenience key (line ~121); keep the `TaskParameter` loop.
- `.../service/report/SchedulerEmailReportService.java` — alias-free `getParameters` (line ~56).
- `.../service/report/SchedulerMaintainReportService.java` — alias-free `getParameters` (line ~41).
- `.../dao/FileDAO.java` — alias-free `getParameters` (lines ~69, 89, 110).
- `.../dao/ServerDAO.java` — drop the `database` param from `findServer` (lines ~39, 41).
- `.../dao/CalendarDAO.java` — drop the alias param from `getCalendar` (lines ~91, 93).
- `.../model/Task.java` — remove the `database` field (line ~57).
- (Hygiene, zero-usage) `.../constant/TaskConstants.java` `TASK_SITE` and `.../constant/CalendarConstants.java` `CALENDAR_SITE`.

XML descriptors (`.../resources/application/awe-scheduler/global`):
- `Maintain.xml` — `NewSchedulerTask`: remove `session="database"`/`session="site"` variables and the `db`/`site` `<field>` writes (lines ~133–134, 158–159).
- `Queries.xml` — simplify `getLastExecution` (lines ~280–303) and `getTaskExecution` (lines ~306–328) to `AweSchExe`-only; leave the commented `taskData` `db`/`site` mapping (lines ~439–440) commented; review `getMaxDate` subquery (line ~337).

Tests (`awe-scheduler` module, ~21 existing stubs + new):
- `MaintainJobServiceTest`, `TaskDAOTest`, `ServerDAOTest`, `FileDAOTest`, `CalendarDAOTest`, `SchedulerEmailReportServiceTest` — update `getParameters(...)` stubs to the alias-free signatures.
- New: #685 manual-launch-without-NPE regression; `database`-named `TaskParameter` passthrough; integration coverage under `awe-tests/awe-boot` (schema at `awe-tests/awe-boot/src/main/resources/sql/schema-hsqldb.sql:611-637`).
- Re-run (no changes expected) `AweDatabaseContextHolderTest`, `SQLQueryConnectorTest`, `QueryTest` to confirm shared beans are unaffected.

## Risks
| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Shared-bean misidentification (deleting/altering `AweRoutingDataSource`/`AweDatabaseContextHolder`/`SessionService`) breaks the interactive multi-DB feature and MDC logging. | Low | Hard out-of-scope constraint; scope stays on the scheduler's *use* only. Re-run shared-bean tests. |
| Simplifying the 2020 join changes SQL behavior and returns a wrong/missing row. | Medium | Add regression coverage asserting the single expected row before/with the SQL change. |
| `getParameters(null, "1", "0")` binds to the wrong overload (`ObjectNode` vs `String`). | Low | Keep the `String`-alias intent explicit; verify overload resolution in tests. |
| Test churn: ~21 existing stubs reference the changing overloads; module goes red if any are missed. | Medium | Update all stubs in the same change (STRICT TDD); run `mvn test -pl awe-framework/awe-modules/awe-scheduler`. |
| **Review budget:** this is scope A+B (minimal + hardening) touching ~6+ Java files, 2+ XML descriptors, and ~21 existing test stubs — likely **near or over the maintainer's 800-line review budget for this session.** | High | Flag explicitly so `sdd-tasks` produces an accurate Review Workload Forecast and recommends chained/stacked PR slices (e.g. alias-free cleanup + field removal as slice 1; join simplification + regression as slice 2). |
| Remote-mode "same datasource" cannot be enforced by code. | Low | Document it as a deployment/config responsibility (point the scheduler instance's `spring.datasource` at the same DB). |

## Open questions for spec/design
- Exact alias-free overload to use: `getParameters(null, "1", "0")` (keeps the change off shared `awe-model`, recommended) vs. adding a clean `getParameters(page, max)` helper in `QueryUtil` (more readable, wider blast radius). Proposal assumes the former.
- Whether to fully delete the dead `TaskConstants.TASK_SITE` / `CalendarConstants.CALENDAR_SITE` constants and the commented `taskData` `db`/`site` mapping, or leave them as inert legacy.
- Delivery/PR-split strategy given the 800-line budget risk (single pass vs chained slices).

## Rollback plan
The change is git-revertible with no data or schema migration to undo (the schema was never touched and `db`/`site` columns/rows are unchanged). To roll back: revert the scheduler-module Java and `Maintain.xml`/`Queries.xml` edits. Because the default-deployment behavior is unchanged by this work, reverting returns multi-database deployments to the prior (dead) per-task alias routing without any data cleanup. The shared routing beans are never modified, so nothing outside the scheduler needs reverting. If only the join simplification proves problematic, that SQL edit can be reverted independently of the alias-free/`Task.database` cleanup when delivered as separate slices.

## Success criteria
- [ ] Manual scheduler task launch (Quartz thread, no session) completes without NPE — the #685 regression test previously failed and now passes.
- [ ] No scheduler code path reads `site`/`database` session variables or forces a `_database_` routing key; `Task.database` no longer exists.
- [ ] A `TaskParameter` named `database` flows through `executeJob` verbatim, with no routing side effect.
- [ ] `NewSchedulerTask` no longer writes `db`/`site` from session state; new rows leave those columns NULL.
- [ ] `getLastExecution`/`getTaskExecution` are `AweSchExe`-only and return the correct single row.
- [ ] Default single-database behavior is unchanged; the full scheduler test module is green (`mvn test -pl awe-framework/awe-modules/awe-scheduler`) and shared-bean tests pass unchanged.
- [ ] Database schema and shared routing beans are demonstrably untouched.
