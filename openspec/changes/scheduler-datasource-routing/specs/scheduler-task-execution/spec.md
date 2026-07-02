# Scheduler Task Execution Specification

## Purpose

Define how AWE scheduler tasks resolve their JDBC datasource so behavior is
identical between an embedded (local) scheduler and a separate REST-connected
instance, independent of HTTP session state.

## ADDED Requirements

### Requirement: Datasource resolution always uses the configured `spring.datasource`

The scheduler MUST resolve the JDBC connection for task bookkeeping
(start/end, execution lookup) and the task's business maintain via the
configured `spring.datasource`, in both local (embedded) and remote/REST
scheduler modes. The scheduler MUST NOT force a per-task datasource alias.

#### Scenario: Local mode task execution uses the configured datasource

- GIVEN a scheduler task with no datasource-alias configuration
- WHEN the task executes in local (embedded) mode
- THEN bookkeeping and the business maintain run against the configured `spring.datasource`

#### Scenario: Remote/REST mode bookkeeping uses the configured datasource

- GIVEN a scheduler instance running as a separate service (`scheduler-instance=true`)
- WHEN it records task start/end and looks up its own execution history
- THEN it uses that instance's configured `spring.datasource`, not a session-derived alias

### Requirement: No dependency on `site`/`database` session state

Scheduler code MUST NOT read or depend on `site`/`database` HTTP session
attributes: bookkeeping queries, the task's business maintain, or report
jobs (email/maintain report services).

#### Scenario: Manual task launch with no bound session completes without NPE (regression for #685)

- GIVEN a task is launched manually with no bound HTTP session (e.g. Quartz thread)
- WHEN the scheduler looks up the task's last execution and runs its business maintain
- THEN execution completes successfully with no `NullPointerException`

#### Scenario: Report job execution does not depend on session state

- GIVEN a scheduled email or maintain report job runs with no bound session
- WHEN the report job executes
- THEN it resolves datasource and parameters without reading session `site`/`database`

### Requirement: A `database`-named task parameter is an ordinary parameter

A `TaskParameter` literally named `database` MUST be treated as an ordinary
task parameter and MUST NOT trigger datasource forcing or routing.

#### Scenario: `database`-named parameter passes through verbatim

- GIVEN a task configured with a `TaskParameter` named `database`
- WHEN the task executes its business maintain
- THEN the parameter reaches the maintain call unchanged, with no routing key forced

### Requirement: Default single-database deployments are behavior-unchanged

Removing `Task.database` and its alias-forcing MUST NOT change observable
behavior for deployments where `awe.database.multidatabase-enable` is
`false` (the default).

#### Scenario: Default deployment behaves identically before and after

- GIVEN a default deployment with `multidatabase-enable=false`
- WHEN a scheduler task executes, in local or remote mode
- THEN task execution and bookkeeping results are identical to prior behavior

### Requirement: Execution history queries return the correct single row

`getLastExecution` and `getTaskExecution` MUST return exactly the correct,
most-recent execution row for a task once the cross-table join is removed.

#### Scenario: Last execution lookup returns the single most-recent row

- GIVEN a task with multiple recorded executions
- WHEN `getLastExecution` is queried for that task
- THEN exactly one row is returned, matching the most recent execution

#### Scenario: Specific execution lookup returns the matching row

- GIVEN a task with a known execution ID
- WHEN `getTaskExecution` is queried with that task and execution ID
- THEN exactly one row is returned, matching that specific execution

### Requirement: Shared multi-database routing and schema remain unaffected

This change MUST NOT alter `AweRoutingDataSource`, `AweDatabaseContextHolder`,
`SessionService`, or the interactive multi-database feature they back. It
MUST NOT alter the `AweSchTsk.db`/`.site` columns or historical row values;
new rows MAY leave them NULL.

#### Scenario: Interactive multi-database feature is unaffected

- GIVEN `awe.database.multidatabase-enable=true` for the interactive web feature
- WHEN a non-scheduler request uses session-based datasource routing
- THEN routing behavior is unchanged by this change

#### Scenario: Historical execution rows keep their stored `db`/`site` values

- GIVEN existing `AweSchTsk` rows with historical `db`/`site` values
- WHEN this change is deployed
- THEN historical rows and the column definitions remain unchanged
