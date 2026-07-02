# Tasks: Scheduler datasource routing cleanup

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500-700 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: alias-free cleanup + dead field/signature removal; PR 2: SQL simplification + regression hardening |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Remove scheduler alias forcing and dead datasource plumbing | PR 1 | Base: develop; include unit tests for alias-free calls and `database` parameter passthrough |
| 2 | Simplify execution-history SQL and prove #685 regression | PR 2 | Base: PR 1; include SQL regression tests and execution-name preservation checks |

## Phase 1: Foundation / Routing cleanup

- [x] 1.1 Update `awe-framework/awe-modules/awe-scheduler/src/main/java/com/almis/awe/scheduler/dao/TaskDAO.java`, `FileDAO.java`, `ServerDAO.java`, `CalendarDAO.java`, and `service/report/*ReportService.java` to stop forcing datasource aliases (`getParameters()` or `getParameters(null, "1", "0")` per design).
- [x] 1.2 Remove `Task.database` from `awe-framework/awe-modules/awe-scheduler/src/main/java/com/almis/awe/scheduler/bean/task/Task.java` and drop dead `TASK_SITE` / `CALENDAR_SITE` constants.
- [x] 1.3 Remove alias forcing and `parameters.put("database", ...)` from `service/MaintainJobService.java`; keep the `TaskParameter` loop untouched.

## Phase 2: XML wiring and DAO signatures

- [x] 2.1 Edit `awe-framework/awe-modules/awe-scheduler/src/main/resources/application/awe-scheduler/global/Maintain.xml` to remove `session="database"` / `session="site"` bindings and their `db` / `site` field writes in `NewSchedulerTask`.
- [x] 2.2 Simplify `global/Queries.xml` `getLastExecution` to `AweSchExe`-only and replace `getTaskExecution` with the design-approved `AweSchExe` + correlated `getTaskName` lookup.
- [x] 2.3 Delete dead `SchedulerService.insertSchedulerCalendar(String, ...)` overload only if no callers remain after `CalendarDAO` signature cleanup.

## Phase 3: Testing / Verification

- [x] 3.1 Add/adjust RED-first unit tests in `TaskDAOTest`, `FileDAOTest`, `CalendarDAOTest`, `ServerDAOTest`, `MaintainJobServiceTest`, `SchedulerEmailReportServiceTest`, and `SchedulerMaintainReportServiceTest` for the new alias-free signatures.
- [x] 3.2 Add regression tests for GitLab #685: manual Quartz launch with no session completes without NPE, and a task parameter named `database` reaches maintain unchanged.
- [x] 3.3 Add SQL-facing tests proving `getLastExecution` and `getTaskExecution` still return exactly one row and preserve `TaskExecution.name`.

## Phase 4: Cleanup / Risk checks

- [x] 4.1 Verify the correlated scalar-subquery syntax for `getTaskName` matches AWE XML rules before implementation; do not introduce a `QueryUtil` helper.
- [x] 4.2 If `Task` serialization compatibility is impacted, add an explicit `serialVersionUID` and note the clustered Quartz JobStore rollout requirement.
