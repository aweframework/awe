# Exploration: Scheduler datasource routing cleanup (GitLab issue #685)

## Context

The scheduler manual-launch NPE (#685) was root-caused in a prior investigation
(Engram `discovery/root-caused-issue-685-scheduler-npe`, obs #3817). This exploration
does NOT re-derive the root cause; it maps the **blast radius** of the session-based
datasource routing so the maintainer can decide how to clean it up per the target
architecture (scheduler must never consider `site`/`database` session variables;
always use the single configured `spring.datasource`; DB schema unchanged).

All file:line references below were re-verified against the current on-disk source
during this exploration.

## Current State (verified)

### The routing mechanism (three collaborating beans, all in `awe-controller`)

- `AweRoutingDataSource extends AbstractRoutingDataSource`
  (`awe-framework/awe-controller/src/main/java/com/almis/awe/component/AweRoutingDataSource.java`).
  `determineCurrentLookupKey()` (line 71) returns `contextHolder.getCurrentDatabase()`.
- `AweDatabaseContextHolder.getCurrentDatabase()` (line 202) returns
  `(String) sessionService.getSessionParameter(AweConstants.SESSION_DATABASE)`.
  `SESSION_DATABASE = "database"` (`AweConstants.java:162`).
- `SessionService.getSessionParameter()`
  (`awe-controller/.../service/SessionService.java:21`) reads from the `@SessionScope`
  `AweSession` bean; returns `null` when no session is bound (the Quartz thread case).

### CRITICAL: the routing datasource is OPTIONAL, gated behind a disabled-by-default flag

`SQLConfig` (`awe-framework/awe-starters/awe-spring-boot-starter/.../autoconfigure/SQLConfig.java`)
wires the routing datasource ONLY when multi-database is explicitly enabled:

```java
@Bean(name = "dataSource")
@ConditionalOnProperty(name = "awe.database.multidatabase-enable", havingValue = "true")
public DataSource aweRoutingDataSource(AweDatabaseContextHolder databaseContextHolder) { ... }   // line 64-68

@Bean
@ConditionalOnProperty(name = "awe.database.multidatabase-enable", havingValue = "true")
public RoutingDatasourceInitializer routingDatasourceInitializer(DataSource dataSource) { ... }   // line 76-80
```

`DatabaseConfigProperties.multidatabaseEnable` defaults to `false`
(`awe-model/.../config/DatabaseConfigProperties.java:40`), and `parameterName`
defaults to `"_database_"` (line 28). So **in a default single-database deployment,
`AweRoutingDataSource` is never instantiated** — there is only Spring's plain
`spring.datasource` DataSource, and `AweDatabaseContextHolder` still exists (it is
unconditional, see below) but its routing is never consulted for connection selection.

### How the alias flows into routing — `QueryUtil.getParameters(alias, page, max)`

`QueryUtil.getParameters(String alias, String page, String max)` (line 354) →
`forceAliasPageAndMax(...)` (line 430). The alias is only applied when non-null:

```java
if (alias != null) {
  forcedParameters.set(databaseConfigProperties.getParameterName(), JsonNodeFactory.instance.textNode(alias)); // sets "_database_"
}
```

The `_database_` parameter is what selects a datasource alias in multi-database mode.
Because every scheduler call passes `task.getDatabase()` as the alias, and
`task.getDatabase()` is ALWAYS `null` (dead read path, see #3817), the `if (alias != null)`
guard is never taken. Routing then falls back to `getCurrentDatabase()` (session `database`,
also null on the Quartz thread) → Spring default DataSource. Net effect today: scheduler
work already lands on the default datasource — the session routing is dead weight that
never actually routes anything for the scheduler, but the presence of the code plus the
2020 cross-DB join is what surfaces the NPE.

---

## Answers to the 7 investigation questions

### Q1 — Blast radius: are the routing beans scheduler-only or shared web infrastructure?

**They are SHARED web/request infrastructure. They must NOT be deleted.** The scheduler
is only one of many consumers. Full caller map:

**`AweDatabaseContextHolder`** (unconditional `@Bean` in `SQLConfig:51`, no property gate):
- `AweRoutingDataSource` (ctor + `determineCurrentLookupKey`) — the routing DS itself.
- `SQLQueryConnector` (`awe-controller/.../data/connector/query/SQLQueryConnector.java:59,77`)
  — constructor dependency; core query execution path for ALL SQL queries.
- `MaintainService` (`awe-controller/.../service/MaintainService.java:284,295`) —
  `getDatabaseConnection(alias)` / `getDatabaseConnection(dataSource)` for ALL maintains.
- `SQLConfig.sqlQueryConnector(...)` bean wiring (line 192).
- Tests: `AweDatabaseContextHolderTest`, `SQLQueryConnectorTest`, `QueryTest` (integration).

**`AweRoutingDataSource`** (conditional `@Bean`, only when `multidatabase-enable=true`):
- `QueryService` (`awe-controller/.../service/QueryService.java:462-480`) —
  `loadDataSources()` / `reloadDataSources()` on the injected `dataSource` if it is an
  `AweRoutingDataSource`. Guarded by `instanceof`, logs "not found, using default" otherwise.
- `RoutingDatasourceInitializer` (`awe-controller/.../component/RoutingDatasourceInitializer.java:25`).
- `FlywayMigrationConfig` (`awe-spring-boot-starter/.../autoconfigure/FlywayMigrationConfig.java:72`).
- `SQLConfig` bean definitions (lines 4, 66, 67).

**`SESSION_DATABASE` / `"site"` session attributes** (read/written across NON-scheduler code):
- `AweDatabaseContextHolder.getCurrentDatabase()` (read, line 204) — routing key.
- `AweLoggingFilter` (`awe-controller/.../component/AweLoggingFilter.java:45,47,61`) —
  puts/removes `database` in the MDC for request logging.
- `ReportGenerator` (`awe-controller/.../service/report/ReportGenerator.java:260`) —
  reads session `database` (JasperReports datasource selection).
- `MaintainService` (line 334) and `SQLQueryConnector` (line 219) — MDC `database` for logging.

**Conclusion for Q1**: "clean up the beans" for the scheduler means **remove the
scheduler's dependency on session-based routing (stop passing `task.getDatabase()` as an
alias)**, NOT delete `AweRoutingDataSource` / `AweDatabaseContextHolder` /
`SessionService.getSessionParameter`. Those beans remain the legitimate multi-database
web-request feature (`awe.database.multidatabase-enable=true`) for interactive users.
No scheduler bean becomes unused as a result — the routing beans are not scheduler beans
to begin with. (Constraint #3's "including the beans involved if they end up unused"
resolves to: nothing becomes unused; keep them all.)

### Q2 — What "always use spring.datasource" means concretely

For both the scheduler bookkeeping (`TaskDAO`) and the task's own business maintain
(`MaintainJobService`), it means **stop passing a datasource alias override** and let the
call fall through to the single configured datasource. Concretely, replace the
alias-carrying overloads with the alias-free equivalents that preserve paging:

- `queryUtil.getParameters(task.getDatabase(), "1", "0")` → `queryUtil.getParameters("1", "0")`
  (there is already a `getParameters(String page, String max)`-shaped need; note the
  current 3-arg overload is `(alias, page, max)`, so passing `null` alias is behaviorally
  identical — `forceAliasPageAndMax` skips the alias when null). The cleanest form is to
  drop the alias argument entirely rather than pass `null`, so the intent is explicit and
  the dead field can be removed.
- `queryUtil.getParameters(task.getDatabase())` (1-arg alias form, used in report services
  and `startTask`/`endTask`) → `queryUtil.getParameters()`.

Because `multidatabase-enable` defaults to `false` and `task.getDatabase()` is already
null, this is **behavior-preserving in the default deployment** (both resolve to the
default datasource) while removing the session/site coupling entirely. In a
multi-database deployment it changes behavior intentionally: the scheduler stops trying to
honor a per-task datasource alias and always uses the configured default — exactly the
target architecture.

**Call sites in `TaskDAO` that pass an alias derived from a task/database and must change**
(all verified):
- line 146 `getTask(taskId, database)` → `getParameters(database, "1", "0")`
- line 196 `setFileModificationsFromDb` → `getParameters(task.getDatabase(), "1", "0")`
- line 238 `startTask` → `getParameters(task.getDatabase())`
- line 264 `endTask` → `getParameters(task.getDatabase())`
- line 397 (file modifications insert path) → `getParameters(task.getDatabase())`
- line 1065 `getTaskExecution(taskId, database, executionId)` → `getParameters(database, "1", "0")`
- line 1082 `getLastExecutionFromDB` → `getParameters(task.getDatabase(), "1", "0")`
- Also the `database` parameter threaded through `getTask(Integer, String)`,
  `getTaskExecution(Integer, String, Integer)`, `getCalendar(alias, ...)`,
  `serverDAO.findServer(id, database)` — these `String database`/`alias` params become
  dead and can be removed for a clean signature (design decision).

### Q3 — Fate of `Task.database` and the `db`/`site` columns/session bindings (DB model frozen)

- **`Task.database` field** (`Task.java:57`): should be **removed** from the Java bean.
  It is a pure dead read path (never populated — zero `.setDatabase(` calls; the `taskData`
  query's `db`/`site` mapping is commented out at `Queries.xml:439-440`). Its 12 read
  callers all feed the routing alias we are eliminating. Removing it is the honest fix and
  makes the "no session routing" guarantee structural rather than incidental.
  - Blast radius of removal: `getDatabase()` has 12 callers across `TaskDAO`, `FileDAO`,
    `SchedulerEmailReportService`, `SchedulerMaintainReportService` — all of which are the
    same routing call sites being cleaned up in Q2, so removal and cleanup are the same edit.
- **`db`/`site` columns** (`AweSchTsk.db`, `AweSchTsk.site`, plus `HISAweSchTsk` audit
  mirror): **left in place, unchanged** (schema is frozen). They become legacy/unused
  columns. Existing rows keep their values; new rows either keep writing them or stop
  (see below). No DDL.
- **`NewSchedulerTask` maintain target** (`Maintain.xml:108-160`): today it writes
  `<field id="db" variable="database"/>` (line 133) and `<field id="site" variable="site"/>`
  (line 134), sourced from `<variable id="database" ... session="database"/>` (line 158)
  and `<variable id="site" ... session="site"/>` (line 159). Per constraint #3 (scheduler
  must not consider site/database session variables **at all, ever**), these
  `session="database"` / `session="site"` bindings should be **removed**. Two compatible
  options for the columns themselves (design decision, both keep schema intact):
  1. Stop writing `db`/`site` on new tasks (drop the two `<field>` + two `<variable>`
     lines) → columns become NULL for new rows, remain valued for old rows.
  2. Keep writing them but from a non-session source (e.g. a literal/empty), if any
     external tooling still SELECTs them.
  Recommended: option 1 (fully removes the session coupling; the columns are already
  never read back). **Forward/backward compatibility**: because nothing reads `db`/`site`
  at execution time, NULLing them on new rows has no runtime effect; old rows with values
  are equally ignored. Safe either way.

### Q4 — Concrete diff in `MaintainJobService.executeJob()` for "database param = plain param"

Current (`MaintainJobService.java:98-142`):
```java
// line 109
ObjectNode parameters = getQueryUtil().getParameters(task.getDatabase(), "1", "0");
...
// line 121
parameters.put("database", task.getDatabase());
```
Target:
```java
// line 109 — no alias override; use the configured datasource
ObjectNode parameters = getQueryUtil().getParameters("1", "0");   // (drop the alias arg)
...
// line 121 — DELETE this line entirely. Do NOT special-case "database".
```
- The `parameters.put("database", ...)` at line 121 is the "convenience key" the constraint
  #4 calls out; removing it means a task parameter literally named `database` (added by the
  normal `TaskParameter` loop at lines 112-118) is treated like any other plain parameter,
  with no forcing into `_database_` and no routing side effect. The `TaskParameter` loop
  (lines 112-118) stays **untouched**.
- Note: `getQueryUtil().getParameters("1", "0")` — confirm the exact overload. The existing
  API is `getParameters(String alias, String page, String max)`; there is no 2-arg
  `(page, max)` overload today (only `getParameters(String alias)` and the 3-arg form).
  So the minimal edit is `getParameters(null, "1", "0")` (behavior-identical, alias skipped),
  OR add a clean `getParameters(page, max)` helper in `QueryUtil`. Passing `null` avoids
  touching shared `QueryUtil`; adding a helper is cleaner but widens the blast radius to
  `awe-model`. **Design decision flagged.**

### Q5 — Remote scheduler mode (separate service) datasource wiring

When `awe.scheduler.scheduler-instance=true` (`SchedulerConfigProperties.schedulerInstance`,
line 63), `MaintainJobService.executeJob()` routes the task's business maintain via
`launchRemoteMaintainRest(...)` (line 126) — an HTTP POST to the main app's
`/api/maintain/{id}`. In that mode the **standalone scheduler does not execute the business
maintain against any local datasource at all** — the main app does.

However, the scheduler instance STILL runs its own **bookkeeping** (`TaskDAO.startTask`,
`endTask`, `getLastExecutionFromDB`, task/calendar/server loads) against **its own local
datasource** — the Quartz store and `AweSchTsk`/`AweSchExe` live wherever the scheduler
instance's `spring.datasource` points. There is nothing scheduler-specific about datasource
wiring: the scheduler starter
(`awe-scheduler-spring-boot-starter`, autoconfig imports: `SchedulerConfig`,
`SchedulerSecurityConfig`, `SchedulerTaskConfig`, `SchedulerController`) ships **no
datasource/`application.yml` of its own** (grep for `multidatabase-enable` / `spring.datasource`
in the starter returned nothing). It inherits whatever datasource the host app configures.
A standalone scheduler that does not enable `awe.database.multidatabase-enable` therefore
has only a plain `spring.datasource` and NO `AweRoutingDataSource` — reinforcing that the
target architecture ("scheduler always uses the same/configured datasource") is the natural
state; the current code is fighting it via the dead `task.getDatabase()` alias. Per the
maintainer's constraint #2, a separate scheduler service uses the **same datasource
configuration as the main application** — this is a deployment/config concern, not code:
point the scheduler instance's `spring.datasource` at the same DB. No code change needed to
enforce it beyond removing the per-task alias routing.

### Q6 — Existing tests to update vs. add (STRICT TDD)

Tests that **directly mock the `getParameters(alias, ...)` overloads** being changed
(these WILL break on signature/argument change and must be updated):

| Test file | Stubs to update |
|-----------|-----------------|
| `awe-scheduler/.../service/MaintainJobServiceTest.java` | 6× `getParameters(isNull(), any(), any())` (lines 94,123,156,187,216,245) — the whole class exercises `executeJob`; this is the primary NPE-fix test target. |
| `awe-scheduler/.../dao/TaskDAOTest.java` | 9× `getParameters(any(), any(), any())` / `getParameters((String) isNull())` (lines 102,236,258,259,280,301,321,358,359,379,406) — covers `startTask`/`endTask`/`onFinishTask`/execution flows. |
| `awe-scheduler/.../dao/ServerDAOTest.java` | 3× `getParameters(null, "1", "0")` (lines 57,77,96). |
| `awe-scheduler/.../dao/FileDAOTest.java` | 2× `getParameters((String) null)` (lines 51,69). |
| `awe-scheduler/.../dao/CalendarDAOTest.java` | `getParameters(any(),any(),any())` (line 80), `getParameters()` (line 149). |
| `awe-scheduler/.../job/report/SchedulerEmailReportServiceTest.java` | `getParameters((String) eq(null))` (line 92). |

Tests that must be **ADDED** (TDD — write failing first):
- **Regression test reproducing #685**: manual launch on the Quartz thread (no session)
  completing without NPE — the `getLastExecutionFromDB` path returning a row when task +
  execution live in the same (default) datasource. This is the test that "previously failed
  and now passes" per AWE's contribution rules.
- Assert `executeJob` no longer forces a `_database_`/`database` routing key, and that a
  `TaskParameter` named `database` is passed through verbatim (constraint #4).
- Integration coverage under `awe-tests/awe-boot` (`TaskDAO`/scheduler flows) — verify with
  `mvn test -pl awe-framework/awe-modules/awe-scheduler -Dtest=MaintainJobServiceTest` etc.,
  and `AweSchTsk`/`AweSchExe` DDL is at
  `awe-tests/awe-boot/src/main/resources/sql/schema-hsqldb.sql:611-637`.

Note: `AweDatabaseContextHolderTest`, `SQLQueryConnectorTest`, `QueryTest` cover the SHARED
routing beans — they should NOT need changes (we are not touching those beans), but re-run
them to confirm no regression.

### Q7 — Other session/site-database coupling in the scheduler module

Beyond `TaskDAO` and `MaintainJobService`, the same `task.getDatabase()`-as-alias pattern
(and thus the same cleanup) appears in:

- **`SchedulerEmailReportService.execute()`** (`.../service/report/SchedulerEmailReportService.java:56`):
  `queryUtil.getParameters(task.getDatabase())` → `getParameters()`.
- **`SchedulerMaintainReportService.execute()`** (`.../service/report/SchedulerMaintainReportService.java:41`):
  `queryUtil.getParameters(task.getDatabase())` → `getParameters()`.
- **`FileDAO`** (`.../dao/FileDAO.java:69,89,110`): 3× `queryUtil.getParameters(task.getDatabase())`.
- **`ServerDAO.findServer(serverId, database)`** (`.../dao/ServerDAO.java:39,41`): the
  `database` param originates from `task.getDatabase()` (called at `TaskDAO.java:165`).
- **`CalendarDAO.getCalendar(alias, calendarId)`** (`.../dao/CalendarDAO.java:91,93`): the
  `alias` originates from `task.getDatabase()` (called at `TaskDAO.java:159`).

**Dead-but-declared site constants** (no functional coupling, safe to remove for hygiene):
- `TaskConstants.TASK_SITE = "site"` (`.../constant/TaskConstants.java:54`) — **zero usages**.
- `CalendarConstants.CALENDAR_SITE = "site"` (`.../constant/CalendarConstants.java:17`) — **zero usages**.

**Listeners / services checked and CLEAN** (no session/site-database coupling):
- `SchedulerTriggerListener` (calls `taskDAO.getTaskExecution(trigger)` — trigger-key based,
  no datasource alias).
- `SchedulerEventListener` (UI progress/cell updates only).
- `SchedulerJobListener`, `SchedulerService`, `TaskService` (`TaskService.java:51` uses
  `getParameters(null, "1", "0")` — already null alias, no task-database coupling).
- `CommandJobService` (command execution, not maintain/DB routing).

**XML descriptor coupling** (the read-back + write paths):
- `Queries.xml:439-440` — the commented-out `db`/`site` field mapping in `taskData`
  (the dead read path). Leave commented or delete the comment; do NOT uncomment (that would
  re-introduce the coupling by populating `Task.database`).
- `Queries.xml:280-303` (`getLastExecution`) and `306-328` (`getTaskExecution`) — these
  carry the 2020 `cb2c9b23` cross-join `AweSchTsk`(Tsk) ⋈ `AweSchExe`(Exe) with
  `filter Ide(Tsk) = taskId`. This join is the mechanism that returns zero rows → NPE when
  task and execution rows ended up in different datasources. Once routing is unified to the
  single datasource, the join is consistent again; whether to KEEP the join as a safety
  check or SIMPLIFY back to the pre-2020 `AweSchExe`-only query is a **design decision**
  (keeping it is harmless once single-DB; simplifying reduces surface). Same consideration
  for `getMaxDate` (line 337) subquery.
- `Maintain.xml:158-159` — the `session="database"`/`session="site"` variable bindings to
  remove (Q3).

---

## Approaches

### Approach A — Minimal behavior-preserving cleanup (recommended)
Drop the alias argument at every scheduler call site (pass nothing / rely on default DS),
remove `Task.database`, remove the `session="database"`/`session="site"` bindings and the
`parameters.put("database", ...)` line, leave shared beans and DB schema untouched, keep
the 2020 join.
- **Pros**: smallest diff; behavior-identical in default (single-DB) deployments; fully
  satisfies constraints #1-#4; no shared-infra risk; testable as a clean regression.
- **Cons**: leaves legacy `db`/`site` columns and the 2020 join in place (cosmetic debt).
- **Effort**: Low-Medium (touch ~7 files + ~6 test files; add 1-2 regression tests).

### Approach B — Approach A + structural hardening
Also remove the now-dead `String database`/`alias` parameters from `TaskDAO`/`ServerDAO`/
`CalendarDAO`/`FileDAO` signatures, delete the dead `TASK_SITE`/`CALENDAR_SITE` constants
and the commented `taskData` fields, and simplify `getLastExecution`/`getTaskExecution`
back to `AweSchExe`-only.
- **Pros**: removes all latent coupling and dead surface; the "no site/database" guarantee
  becomes structurally impossible to regress.
- **Cons**: larger blast radius (signature changes ripple to more call sites and tests);
  simplifying the join changes SQL behavior and needs its own regression coverage; more
  review load (watch the 400-line PR budget — likely needs splitting).
- **Effort**: Medium-High.

### Approach C — Add a clean `QueryUtil.getParameters(page, max)` helper
Same as A/B but introduce a 2-arg `(page, max)` overload in `QueryUtil` (`awe-model`) so
scheduler code reads `getParameters("1", "0")` with explicit intent instead of
`getParameters(null, "1", "0")`.
- **Pros**: most readable end state; no `null` alias magic.
- **Cons**: modifies shared `awe-model` `QueryUtil` (broader blast radius, more tests);
  not strictly necessary.
- **Effort**: Medium.

## Recommendation

**Approach A as the core change**, optionally folding in the low-risk parts of B (deleting
the two dead `*_SITE` constants and removing the `session=` bindings) since those carry no
behavioral risk. Defer the join-simplification and DAO-signature pruning (the higher-risk
parts of B) to a follow-up unless the maintainer wants a single comprehensive pass. Prefer
`getParameters(null, "1", "0")` over adding a `QueryUtil` overload (C) to keep the change
inside the scheduler module and off shared `awe-model`. This satisfies every hard
constraint with minimal, behavior-preserving edits and a clean #685 regression test.

## Risks

- **Shared-bean misidentification**: deleting `AweRoutingDataSource`/`AweDatabaseContextHolder`/
  `SessionService.getSessionParameter` would break the interactive multi-database feature and
  request logging (MDC). The cleanup MUST stay scoped to the scheduler's *use* of them.
- **2020 join behavior**: if the join is simplified, ensure `getLastExecution`/`getTaskExecution`
  still return the correct single row; add regression coverage before touching SQL.
- **`getParameters` overload ambiguity**: `getParameters(null, "1", "0")` must resolve to
  the `(String alias, String, String)` overload (it does — `null` is `String` here), not the
  `(ObjectNode, ...)` one; keep the cast/intent explicit to avoid a surprising bind.
- **Test churn**: ~21 existing scheduler test stubs reference the changing overloads; all
  must be updated in the same PR to keep the module green (STRICT TDD).
- **Review budget**: Approach B likely exceeds the 400-line PR budget — plan chained PRs if
  the maintainer chooses B.
- **Remote mode**: enforcing "same datasource as main app" for a separate scheduler service
  is a deployment/config responsibility (point `spring.datasource` at the same DB); code
  cannot fully guarantee it. Document it.

## Ready for Proposal

**Yes.** The blast radius is fully mapped and the target end-state is unambiguous. Recommend
proceeding to the proposal phase with Approach A (core) and letting the maintainer decide
how much of Approach B's hardening to include and the delivery/PR-split strategy. Key
decisions to surface in the proposal: (1) A vs A+B-hardening; (2) keep vs simplify the 2020
join; (3) stop-writing vs keep-writing `db`/`site` on new tasks; (4) `null`-alias vs new
`QueryUtil` helper.
