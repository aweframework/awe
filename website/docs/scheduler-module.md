---
id: scheduler
title: Scheduler Module
sidebar_label: Scheduler Module
---

Scheduler module adds a powerful scheduling tool to your application

To activate this module, follow this steps:

- Add **awe scheduler dependencies** to pom.xml descriptor.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-scheduler-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Add the scheduler screens into your `private.xml` file:

```xml
<option name="scheduler" label="MENU_SCHEDULER" icon="clock-o">
  <option name="scheduler-activity" label="MENU_SCHEDULER_ACTIVITY" screen="scheduler-activity" icon="lucide:activity">
    <option name="scheduler-flow-run" screen="scheduler-flow-run" invisible="true" />
  </option>
  <option name="scheduler-task-catalog" label="MENU_SCHEDULER_TASK_CATALOG" screen="scheduler-task-catalog" icon="lucide:list-checks">
    <option name="scheduler-task" screen="scheduler-task" invisible="true" />
    <option name="scheduler-task-editor" screen="scheduler-task-editor" invisible="true" />
  </option>
  <option name="scheduler-configuration" label="MENU_SCHEDULER_CONFIGURATION" screen="scheduler-configuration" icon="lucide:settings"/>
</option>
```

| Menu option | Screen | What it is for |
|-------------|--------|----------------|
| **Activity** | [`scheduler-activity`](#activity-screen) | What runs, waits or failed in every task, and the engine menu |
| **Tasks** | [`scheduler-task-catalog`](#tasks-catalog) | Every task, with **New task** |
| **Configuration** | [`scheduler-configuration`](#configuration-screen) | Servers, calendars and the engine, one tab each |
| (invisible) | [`scheduler-task`](#task-screen), [`scheduler-task-editor`](#task-editor), [`scheduler-flow-run`](#flow-run-page) | The task page, the task editor and the flow run page, opened from the screens above |

The invisible options give their screens a route and a breadcrumb: the task page and the editor go back to **Tasks**,
the flow run page to **Activity**. An application that upgrades from the old scheduler screens follows the
[Scheduler redesign](guides/v5-migration.md#scheduler-redesign) entry of the migration guide.

- Configure property value to add `awe-scheduler` to module list.

```properties
awe.application.module-list = APP, ..., awe-scheduler, ..., awe
```

- Give the profiles that use the scheduler screens the `scheduler.read`, `scheduler.update` and `scheduler.execute` permissions (only the administrator profile has them). See [Permissions](security/permissions.md#catalog-resources).

- Finally, if you are using `flyway`, add the scheduler tables into the migration module:

```properties
awe.database.migration-modules=AWE,...,SCHEDULER,...
```

## Flow runs

Every task launched by its schedule, by hand, by a file or by a webhook starts a flow run that groups the executions of its dependencies, with a final status and optional run parameters. A task with several parents is launched once, when all its parents in the run have ended.

The run stores the parameters of its launch in `AweSchRunPar`: first the values the operator gave to a manual launch, as they are, which every dependency of the run receives; then the other `VALUE` and `VARIABLE` parameters of the root task with their resolved value and the reserved prefix `=` in their name (`=secondsToWait`). The prefixed rows are only shown (the **Activity** side panel): the dependencies do not receive them, so a flow receives the same values as before they were stored. A `PROPERTY` parameter is stored only as the key a manual launch sends, which cannot change (its resolved value is configuration and is never written), and a parameter whose name has 40 characters (no room for the prefix) is not stored as resolved. That is why the name of a task parameter cannot start with `=`: the task screen rejects it.

The [flow run page](#flow-run-page) (`scheduler-flow-run`, permission `scheduler.read`) shows one run with its tasks, its executions and its skipped tasks; the [Activity screen](#activity-screen) lists the runs in its **Group by flow** view. See **[Dependencies between tasks](guides/dependencies-guide.md#flow-runs-and-fan-in)**.

The `flow-graph` widget draws the dependencies of the tasks (the rows of `getFlowGraph`) as a graph from left to right. Each node shows the task, its launch type and
whether it is active; each edge shows when the parent launches its children (always on OK, also on warning or on error).
In a run, each node shows its state: ended with its status, running, waiting, skipped or not reached. Clicking a node
opens the task. An inactive task is muted, with an **Inactive** tag. Both engines draw the graph. With the widget
parameter `mode` = `task` the same widget draws the one-level graph of the [task screen](#task-screen): the tasks the
task runs after, the task and the tasks it launches, in three titled columns. Each edge shows its condition as icons
(✓ OK, ⚠ warning, ✕ error: the endings of the parent that launch the child), with its whole text as tooltip and
accessible name, and a legend under the graph explains them. Each task shows the status of its last execution with
when it started (`OK · today 08:00`) and a scheduled one its next execution (`Next: tomorrow 03:00`); a child that also
waits for other tasks lists them, muted, at its right (**Also waits for**). With the `target` parameter the tasks it
runs after and the tasks it launches are buttons (`Open <task>`, Enter or Space) that open the `target` screen, and the
task itself is not: the task of the node goes to the selected value of the criterion named by the `criterion`
parameter (the task screen reads `IdeTsk.selected`) and the screen is opened again. Without a `target` its nodes are
not buttons. With the `side` parameter (`parents` or `children`, the [task editor](#task-editor)) only that side is
drawn, in two columns: the tasks it runs after and the task, or the task and the tasks it launches; without it the three
columns.

With the widget parameter `mode` = `run` the same widget draws one flow run from the rows of `getFlowRunGraph` (one row
per task of the run and parent), in both engines:

```xml
<widget type="flow-graph" id="FlwRunGph" server-action="data" target-action="getFlowRunGraph" initial-load="query">
  <widget-parameter type="string" name="mode" value="run" />
</widget>
```

The actions of a selected node are the dependencies of the page on its click event
(`<dependency-element id="FlwRunGph" event="click" />`).

- Each node shows the state of the task in the run with its icon and its text (the box is dotted while it is
  **Pending**, dashed when it was **Skipped** or **Waiting for confirmation**), its execution, its duration with its
  times (up to **Now** while it runs), the due date of a deferred one (`ExeDueLbl`) and, on a join, the parent that
  closed it (**Joined by …**, `joinedByText`) with its wait (`joinWaitText`). A task never reached in an ended run is
  skipped.
- Each edge shows the condition of its parent (`conditionLabel`: «OK», «OK o Aviso», «OK o Error», «OK, Aviso o
  Error») and its state in the run: met, not met (dashed, error tone), cut (dashed, muted) or pending (dotted); the
  edge that closed a join is thicker. A legend under the graph explains the states and the edges.
- A node with an execution is a toggle button (`aria-pressed`): a click, Enter or Space selects it, raises the `click`
  event of the widget (the dependencies on it launch their actions, as the Activity side panel does) and the widget
  sends `<id>.selected` (the task) and every column of the row of the node as `<column>.selected`, like the selected
  row of a grid (`IdeTsk.selected`, `ExeTsk.selected`). With a `target` the screen opens too. A pending or skipped task
  has no execution: it stays focusable but opens nothing. The selected node is kept in view and announced to screen
  readers; Escape clears the selection.
- A small toolbar zooms out, in (50 % to 200 %, a quarter each step) and fits the graph to its width.

The widget parameter `view` draws the same run in another view (a static parameter: place one widget per view, or
switch it in the screen): `graph` (the default, also for any other value), `timeline` or `list`.

```xml
<widget type="flow-graph" id="FlwRunTim" server-action="data" target-action="getFlowRunGraph" initial-load="query">
  <widget-parameter type="string" name="mode" value="run" />
  <widget-parameter type="string" name="view" value="timeline" />
</widget>
```

- **Timeline** (`timeline`): a table with one row per task, in the order of the list: its name, its state (icon and
  text), its execution and a note with its due date or the join it closed; at the right its bars on a time axis with
  its ticks. Each bar has the tone of its state and its duration; a deferred execution runs up to the moment it started
  to wait and then a dashed bar stays open up to **Now** (a line across the rows while a bar is open), with its due date
  marked. The wait in a join is a hatched bar before the task. Every idle gap longer than a quarter of the run is
  compressed (hatched, `≈` on the axis) so minutes of work and hours of waiting fit one row.
- **List** (`list`): a nested ordered list from the root task, following the dependencies: each task with its state,
  execution, duration and times, its due date, the join it closed with its wait, why it was skipped and the condition
  of its edge (`After <parent> if it ends OK…`). A join appears once, under the parent that closed it; under its other
  parents an `Also feeds <task>` entry points to it.
- The bars of the timeline and the tasks of the list select their task exactly like a node of the graph: toggle buttons
  (`aria-pressed`, Enter or Space), the same `click` event, `<id>.selected` and `<column>.selected` values; a task
  without execution opens nothing, Escape clears the selection and the selected task is kept in view and announced.
  Both views keep the legend of the states (the timeline adds the join wait); the zoom toolbar is only in the graph.

If you use `flyway`, the tables of the flow runs (`AweSchRun`, `AweSchRunPar`, `AweSchRunTsk` and the `IdeRun` column of `AweSchExe`) come with the `SCHEDULER` migration module, version `1.0.9`.

## Activity screen

The **Activity** screen (`scheduler-activity`, permission `scheduler.read`; its actions need `scheduler.execute`) shows what is running, what is waiting for a confirmation and what has failed, in every task. Its header shows the status of the engine (running, paused or stopped) and the **Engine** menu (pause, resume, restart and clear and stop, the last two with a confirmation). The counters (running, waiting for confirmation, queued, errors in the last 24 hours and upcoming in the next 24 hours) filter the list, except the upcoming one, which only informs. The list of executions is filtered by task, status, origin and start date; the task and the flow run of a row open their screens, and the detail button of a row opens a side panel with the links to the task and to the flow run, the progress of the execution (its elapsed time, or its execution time once ended, against the average of its task), the execution data (a dash for a value it does not have yet), the due time of a deferred execution, the parameters stored with its flow run, the live log and the stop and relaunch actions. It reads the [activity queries](#activity-queries).

## Activity queries

The activity data of all tasks is read with these queries (permission `scheduler.read`, read only). With `awe.scheduler.remote-enabled=true` they are answered by the scheduler instance (`POST /scheduler/api/v1/executions`, `POST /scheduler/api/v1/executions/runs` and `GET /scheduler/api/v1/executions/counters`):

| Query | Returns |
|-------|---------|
| `getActivityExecutionList` | A page of the executions of all tasks, newest first unless the grid sorts it, sorted and paged in the database (with the `sort`, `page` and `max` the grid sends). Optional filters: `CrtExeTsk` (text of the task name), `CrtExeTskIde` (one task, the history of the [task screen](#task-screen)), `CrtExeSta` (one or more statuses), `CrtExeOri` (one or more origins of the `ExeOri` enumerated: `scheduled`, `manual`, `file`, `dependency`, `webhook`), `CrtExeDfr` (`1` running executions waiting for the confirmation of a deferred answer, `0` running executions not waiting), `CrtExeWin.from` / `CrtExeWin.to` (first and last start day, included, `dd/MM/yyyy`: the custom range of the date range quick filter `CrtExeWin`) and `CrtExeWin` (started in the last hours). A filter value that is not valid shows the error *Invalid filter* instead of a wider list, as does a page size (`max`) of `0` or above `500` (without it, `50` rows) or a `sort` column or direction that is not accepted (see the [Scheduler guide](guides/scheduler-guide.md#activity-of-all-tasks) for the valid values and the API). Each row has the columns of the task execution list (status, progress with the average time of its own task, time, launched by, log locator) plus the task (`IdeTsk`, `TskNam`), the flow run (`ExeRun`, and its label alone in `ExeRunLbl`, which the side panel shows), the origin code (`ExeOri`) and, while it waits for a deferred confirmation, `ExeDfr` = `1` and its due date (`ExeDue`, `ExeDueTim` as `HH:mm`). The database sorts every execution before it takes the page; the sortable columns, and the field each one sends as its sort id (declare it as `sort-field`, so the engine sorting the page again keeps the order of the database), are the start `ExeLstTim`, the task `TskNam`, the status `Sta`, the duration `ExeEla` (elapsed seconds, until now while the execution has not ended), the launcher `ExeLchBy` and the origin `TypLch` (the column names `ExeStaTxt`, `ExePrg`, `ExeTim` and `ExeOriTxt` are accepted too); declare any other column `sortable="false"` |
| `getActivityRunList` | The executions of `getActivityExecutionList` grouped by flow run, for a tree grid (`tree-id="id"`, `tree-parent="RunPar"`, `tree-leaf="RunLeaf"`). The top-level rows are the flow runs and the executions without run (launched before the runs were stored), newest first; the page, the sort and the filters (the ones of `getActivityExecutionList`, read from the toolbar of the runs view: `CrtRunTsk`, `CrtRunQck`, `CrtRunOri`, `CrtRunWin`, `CrtRunWin.from` and `CrtRunWin.to`) apply to them: **a run is listed when at least one of its executions matches the filters**, and then it brings every execution of the run, not only the matching ones. Without `max`, 50 top-level rows. The sortable fields are the ones of `getActivityExecutionList`: a run sorts by its stored start, status, launcher and elapsed time, by the name of its root task and by the origin of its root execution. A run row has `id` = the run id, `RunPar` empty, `RunLeaf` = `0`, its root as `TskNam` (`<root task> #<root execution>`) and as `ExeRun`/`ExeRunLbl`, its aggregated status in `Sta`, `ExeStaTxt`, `ExeStaIcn` and `ExeStaTon` (running when an execution runs, else queued when one is queued, else error when one ended with error, stopped or interrupted, else warning when one ended with warning or the run skipped a task, else OK), its start (`ExeLstTim`, the first start of its executions), its duration (`ExeTim`, `ExeEla`: until the last end, or until now while an execution has not ended), the origin and the launcher of its root execution and its number of executions (`RunExeCnt`, and `RunExeTxt`, «5 executions · 2 skipped», also in `ExeDueLbl`, the line under its status), and a hidden log button (`ExecutionLogPath` with the `hidden` style). Its children follow it, first started first: each execution with the fields of `getActivityExecutionList` (`id` = `taskId-executionId`, `RunPar` = the run id, `RunLeaf` = `1`), then each task the run skipped (`id` = `runId/taskId`, the **Skipped** status, no execution nor log). An execution without run is a top-level leaf with the fields of `getActivityExecutionList` |
| `getActivityCounters` | One row with `running`, `waiting` (waiting for the confirmation of a deferred answer), `queued`, `errorsLast24h` and `upcomingNext24h` (fire times of the visible triggers that can fire (not paused, in error or complete) in the next 24 hours, with their calendars, at most 1440 counted per trigger and at most 8640 skipped because its calendar excludes them; empty when the Quartz part fails, the other counters are still returned). Each execution counter matches the rows of `getActivityExecutionList` with the same filter |
| `getActivityExecutionProgress` | Progress of the selected execution (`IdeTsk.selected`, `ExeTsk.selected`) as the fields of a [card](api/card.md): `title`, `value` (the elapsed time while it runs, else its execution time), `unit` (`s`, `min` or `h`, as the longest of the time and the average needs), `target` (the average time of its task, none without successful executions), `numberDecimals` and `description`. No row without execution |
| `getActivityExecutionParameters` | Launch parameters of the flow run of the selected execution (`ExeRun.selected`), from the `getActivityExecutionParameters` service: `ParNam` and `ParVal` (a dash without value), sorted by name. They are the operator values of a manual launch and the resolved `VALUE` and `VARIABLE` parameters of the root task, without their `=` prefix (the operator value wins when both have the same name; see [Flow runs](#flow-runs)). A dependency shows the parameters of the launch of its run; an execution launched before they were stored shows none. The values are not masked (the parameters of a task have no secret flag; `PROPERTY` values are never stored) |

`getSchedulerMetadata` also returns `schedulerStatusCode` (`started`, `standby` or `stopped`), the language independent code of `schedulerStatus`.

## Tasks catalog

The **Tasks** screen (`scheduler-task-catalog`, permission `scheduler.read`; running a task needs `scheduler.execute`, creating, editing, activating, deactivating and deleting it `scheduler.update`) lists every task with what it runs, when it is launched and how its last executions went. The **New task** button opens the task editor. The toolbar of the grid has a search (the name, the description or what the task runs), the launch filter (manual, scheduled, file, dependency or webhook), the active filter with the number of tasks of each option and the last status filter (OK, warning, error, running, waiting for confirmation, queued or no executions). Its columns are:

- **Last status**: the status pill of the [Activity screen](#activity-screen) of the last execution, with its start under it (today, yesterday, tomorrow or the date).
- **Task**: a link to the [task screen](#task-screen), with its description as its title.
- **When**: a summary in plain words of how the task is launched, derived from its launch type, its repetition, its calendar, the file server, folder and pattern it checks and the tasks it runs after (no field of its own). For example *Weekdays at 22:30* with *calendar Holidays* under it, *When a file arrives in sftp-banks:/in* with *\*.n43 · checks every 5 min*, or *After Import statements* with *if it ends OK or with a warning*. A manual task with parents is a **dependency**: it runs after them, and with several parents when all of them have ended.
- **History**: a [status strip](api/grids.md) of the last 20 saved executions (the ones the task keeps), from the oldest to the newest; the tooltip of each bar tells the execution, its start, its duration and its flow run.
- **Next execution**: the next fire time of a scheduled or file task, or what launches the task (its parents, a webhook, a user), or that it is inactive. It is not sortable.
- **Average** time of its OK executions and **Active** state (a pill).

The row menu (right click, or the «⋯» *More actions* button at the end of each row, also in the executions grids of Activity and in the history of the task screen) opens the task screen (*View details*, as the link of the task), runs the task (*Run…* opens the [run dialog](#run-dialog) when it has parameters, *Run now* launches a task without parameters at once), opens the editor (*Edit* and *Duplicate*), activates or deactivates it and deletes it after a confirmation (the maintains `LchTsk`, `ActSchTsk`, `DeaSchTsk` and `DeleteSchedulerTask`). The engine has no editable switch cell bound to a maintain, so the active state is a pill and the row menu changes it.

The catalog is read with these queries (permission `scheduler.read`, read only). With `awe.scheduler.remote-enabled=true` they are answered by the scheduler instance (`POST /scheduler/api/v1/tasks/catalog` and `POST /scheduler/api/v1/tasks/catalog/active-counts`, with the bearer token; their body is the `TaskCatalogCriteria` with the fields `search`, `launchType`, `active`, `lastStatus`, `page`, `max` and `sort`, and an invalid value answers `400` with `{"type": "invalid-request"}`):

| Query | Returns |
|-------|---------|
| `getTaskCatalog` | A page of the tasks, by name unless the grid sorts it. The service reads every task once, filters, sorts and pages them, and reads the next fire times, the history and the parameters of the tasks of the page only (each with one query for the whole page; the database keeps the last 20 executions of each task, so a long history never brings more rows than the strip shows). Optional filters: `CrtTskSch` (text of the name, the description or what the task runs, case insensitive), `CrtTskLch` (`manual`, `scheduled`, `file`, `dependency` or `webhook`), `CrtTskAct` (`1` active, `0` inactive) and `CrtTskSta` (`ok`, `warning`, `error` — also stopped and interrupted —, `running`, `waiting`, `queued` or `none`). A value that is not valid shows the error *Invalid filter*, as does a page size (`max`) of `0` or above `500` (without it, `50` rows) or a `sort` column or direction that is not accepted. Each row has the task (`IdeTsk`, `Nam`, `Des`, `CmdExe`, `Act`, `TypLch`, `Sta` = status of its last execution), the link `TskNam` (`value`, `label` and `title` = its description), the last status pill (`TskStaTxt`, `TskStaIcn`, `TskStaTon`, `TskStaSub`), the «When» summary (`TskWhnTxt`, `TskWhnIcn`, `TskWhnSub`), the history (`TskHis`, a JSON array of `{"tone", "label", "title"}`), the next execution (`TskNxtTxt`, `TskNxtSub`), the average (`TskAvg`), the active pill (`TskActTxt`, `TskActIcn`, `TskActTon`) and `TskVar` (`1` when the task has parameters, so its launch asks for their values). The sortable fields (declare them as `sort-field`) are `Nam`, `TskLstDat` (start of the last execution), `TskKnd` (kind of launch), `TskAvgMs` (average time) and `Act`; the column names `TskNam`, `TskStaTxt`, `TskWhnTxt`, `TskAvg` and `TskActTxt` are accepted too. The next execution is not sortable: it mixes fire times with what launches the task («When X ends», a file), and sorting it would ask the scheduler for the fire time of every task |
| `getTaskCatalogActiveCounts` | The options of the active filter (every task, active, inactive) with `value`, `label`, `count` and `tone`, counted with the search, launch and last status filters |
| `getTaskCatalogLaunchTypes`, `getTaskCatalogLastStatuses` | The options of the launch and last status filters (`TskCatLch` and `TskCatSta` enumerated) |

## Task screen

The task screen (`scheduler-task`, permission `scheduler.read`; running the task needs `scheduler.execute`, editing,
activating, deactivating and deleting it `scheduler.update`) opens from the task of a row of the
[Tasks catalog](#tasks-catalog) and from every task link of the [Activity screen](#activity-screen), with the task as
`IdeTsk.selected`. Declare it as an invisible option of the catalog (see the menu above), so its breadcrumb goes back to
**Tasks**, like the **Back to Tasks** button, the first action of its header. The browser tab keeps the task (`keep-on-reload`, see [criteria](api/criteria.md)), so a reload of the
browser opens the same task. Its header shows the name, the status of the last execution, the description and the facts
of the task as pills: its id, what it runs (the name of the maintain or the command in the monospace font), when it is
launched (the «When» summary of the catalog), the executions it keeps, its maximum time and its report. **Edit** opens
the task editor; **Run…** opens the [run dialog](#run-dialog) when the task has parameters, and **Run now** launches a
task without parameters at once (there is nothing to ask); the three dots button at
the right (**More actions**) activates, deactivates or deletes the task (after a confirmation, then back to the
catalog). Its tabs are:

- **Summary**, over the saved executions of the task: the success rate (the percent of OK executions among the ones that
  ended, as a stacked bar by status with the count of each status under it), the average duration of its OK executions
  with the maximum time allowed and the durations of its last executions as an area chart, the next execution (its date when the scheduler fires the task, else what launches
  it), the status strip of its last 20 executions as wide as its card, with the count of each status and **View
  history**, and its last execution (status, result,
  execution, flow, origin, launcher, start and duration) with **View log**, which opens the detail of the newest
  execution of the history. The actions of the last executions and of the last execution sit on the same line.
- **History**: the executions of the task as the Activity screen lists them (`getActivityExecutionList` with
  `CrtExeTskIde`), with its state, origin and date filters and the same side panel (progress, parameters, live log and
  stop).
- **Dependencies**: the position of the task in the execution flow, as a one-level flow graph (read only: the conditions
  of each dependency are set in the task that is launched after, which **Change what it depends on** edits) in three
  columns, left to right **Runs after**, **This task** and **Launches after** (an empty one says
  **None**), with the whole name of each task in up to two lines, the status of its last execution with when it started
  and, for a scheduled task, its next execution; the condition of each edge as icons with a legend; at the right of a
  child, the other tasks it also waits for. A task it runs after or launches opens its own task screen (the browser
  tab keeps it, so a reload opens it again). **Change what it depends on** opens the task editor.
- **Configuration**: a read-only summary of what the task runs, where, what it keeps, its maximum time, when it is
  launched, after which tasks, what it launches after it, its parameters and its report, with **Edit**.

It reads these queries (permission `scheduler.read`, read only):

| Query | Returns |
|-------|---------|
| `getTaskPage` | One row for the task of `IdeTsk.selected` (none when it does not exist), the target of the screen: `TskAct`, `TskVar` (`1` when its launch asks for the values of its parameters), the header (`TskPagTit`, `TskPagSub`, `TskPagChp`, `TskPagChpTon`, `TskPagIcn` = the icon of its launch, `TskPagFct` = its facts as a JSON array of pills `{"label", "icon", "code"}`), the summary (`TskOkVal`, `TskOkUni`, `TskOkDes`, `TskAvgVal`, `TskAvgDes`, `TskAvgSpk`, `TskNxtVal`, `TskNxtDes`, `TskHis`, `TskHisTxt`), the last execution (`TskLstHas`, `TskLstSta`, `TskLstTon`, `TskLstIcn`, `TskLstDes`, `TskLstExe`, `TskLstRun`, `TskLstOri`, `TskLstBy`, `TskLstIni`, `TskLstDur`) and the configuration (`TskCfgRun`, `TskCfgWhr`, `TskCfgWhn`, `TskCfgPrn`, `TskCfgChl`, `TskCfgSto`, `TskCfgTim`, `TskCfgRpt`). A value the task does not have is a dash |
| `getTaskPageHeader`, `getTaskPageSuccess`, `getTaskPageAverage`, `getTaskPageNext`, `getTaskPageHistoryCount`, `getTaskPageLast` | The fields of `getTaskPage` each [card](api/card.md) of the screen shows, with the names of the card attributes (`title`, `subtitle`, `chip`, `chipTone`, `pills`, `value`, `unit`, `description`, `stack`, `sparkline`, `strip`) |
| `getFlowGraph` | The flow graph rows, now with `lastStatus` (the status of the last execution of each task) |
| `getTaskFlowGraph` | The `getFlowGraph` rows of the task (the **Dependencies** tab), where the task, the tasks it runs after and the tasks it launches also have `lastWhen` (when their last execution started, in the relative words of the catalog: `today 08:00`, `23/03/2019 08:00`) and `nextRun` (`Next: tomorrow 03:00` for an active scheduled task the scheduler fires); empty for the other rows |

### Run dialog

**Run…** (the task screen and the row menu of the catalog) opens the run dialog
(`scheduler-launch-variables`): a manual launch where the operator may change the value of the **Value** and
**Variable** parameters of the task, only for this execution (the task does not change). Its grid
(`taskVariableParameters`, permission `scheduler.read`) lists each parameter with its type under its name, its origin
as a pill (**Value**, **Variable** or **Property**) and its stored value, editable except for a **Property**. **Reset values** reloads the stored values, **Cancel** closes it and
**Run** validates the values (`VldTskPar`) and launches the task (`LchTskVar`), both with the permission
`scheduler.execute` (`scheduler.update` does not allow it). A note under the grid (`getTaskLaunchNote`) tells that the
launch is recorded with origin **Manual**, launched by the user of the session.

- A **Property** parameter shows the key of the property, read-only: it is resolved at execution time and a key that
  does not resolve rejects the launch. A launch cannot change the key: a different key is rejected with *The key of a
  property cannot be changed on launch* and nothing runs (also through the API and a remote scheduler). The resolved
  value is configuration and never reaches the browser.
- A value that changes the stored one must be a value of the parameter type, without the blanks around it (which are
  removed, also from the value that runs): a whole number (`INTEGER`, `LONG`), a decimal number with a dot (`FLOAT`,
  `DOUBLE`: `3`, `0.5` or `.5`), a date `dd/MM/yyyy` or `yyyy-MM-dd` (`DATE`) or a time `HH:mm` or `HH:mm:ss` (`TIME`);
  only the text types (`STRING`, `OBJECT`) accept a blank value and keep their blanks. Otherwise the launch is rejected
  with *Parameter value not valid* and nothing runs. The launch itself checks it too, so the
  scheduler API (`POST /scheduler/api/v1/task/{taskId}/execute`, see [Scheduler API security](#scheduler-api-security))
  and a remote scheduler reject the same values. An unchanged stored value is not
  checked again.
- The values reach the execution and are stored as the parameters of its run (`AweSchRunPar`, see
  [Flow runs](#flow-runs)), so the **Activity** side panel shows them. Other launches (a schedule, a file, a webhook,
  a dependency) only change the **Variable** parameters.

## Task editor

The task editor (`scheduler-task-editor`, permission `scheduler.update` to read the task and to save it) is one screen
for a new task, its edition and its copy; declare it as an invisible option of the catalog, as the task screen. The
catalog opens it with **New task** and with **Edit** and **Duplicate** of its row menu; the task screen with **Edit**,
**Duplicate** (its **More actions** menu), **Edit** of its Configuration tab and **Change what it depends on**, which
opens it on **When**. The caller sends the mode (`TskEdtMod`: `new`, `edit` or `copy`) and the task (`IdeTsk.selected`),
and the browser tab keeps both, so a reload opens the same task in the same mode. Its header tells the mode (**New
task**, **Edit task #N**, **Duplicate task**) and whether the task is active; its sections, one card each, are **What it
runs** (name, description and the execution type as [choice cards](api/criteria.md#choice-cards)), **Parameters**,
**When** (choice cards: manual, scheduled, when a file arrives, after other tasks or webhook), **Dependencies** (the tasks
that run after it, read only, each a link to its task screen), **Behaviour** (maximum time, kept executions, active and
the three switches of the tasks that run after it) and **Notifications**; a navigation on the left scrolls to each
section and a summary on the right follows the changes. **After other tasks** chooses the parents in a list grouped in
*Chosen*, *Available* and *Cannot be chosen* (the tasks that already run after it, with the reason) and draws them
joining into the task with the condition of each one (read only: the switches of the parent; a parent opens its task
screen). Leaving it with parents chosen asks first: changing clears them, keeping goes back to it. The navigation
marks the section in sight while the form scrolls. The footer stays visible under the form with **Cancel** (back where
the user came from) and **Save**, whose validation takes the focus to the first field in error and which opens the task
screen of the saved task (a new one for a new task or a copy) with a success message.

A scheduled task chooses its **Repetition** among presets, each one with only its fields: *Every N minutes* and *Every N
hours* (N), *Daily* and *Workdays* (time), *Weekly* (days of the week and time), *Monthly* (day of the month and time),
*Once* (date and time) and *Custom*, which shows the stored fields of the old screens grouped in *Which days* (years,
months, weeks, days of the week, days) and *At what time* (hours, minutes, seconds) with what they mean. A preset is
only a way of filling those stored fields (no new data): every N minutes or hours is the repetition of that unit, the
others the custom repetition of the lists at that time (workdays are Monday to Friday), and once the date and time of a
launch. A change of preset selects its stored fields in *Custom* (`SyncTaskEditorSchedule`), and the save stores the
fields of the preset again on the server. A task opens on the preset its stored fields match, else on *Custom* (a time
with seconds too: the presets take hours and minutes). The presets are chips; the days of *Daily*, *Workdays* and
*Weekly* are toggles («M T W T F S S»), and changing the days of *Daily* or *Workdays* turns the preset into *Weekly*.
*Custom* offers to go back to the preset its fields match. Under them, the calendar of holidays and the range (from and
until, each one a date and its time; both optional, and each time can only be filled once its date is, and is emptied
with it), and a preview card computed on the server from the same fields as they change: the repetition in a sentence
with its key parts bold, its next five runs as tiles (the day and the date, the time, and *today*, *tomorrow* or the days
or the holiday it skips), the first holiday of its calendar it skips (or, when its *until* has passed, that it will not
run again), and how it is saved (the stored fields, folded). Dates, times and numbers take a column sized to their value,
so they flow one after the other. A file task shows how often it checks its
folder, where it looks, the calendar and the range; a webhook task its hook.

It uses these targets:

- `getTaskEditor` (permission `scheduler.update`), the target of the screen, with `TskEdtMod` and `IdeTsk.selected`: the
  fields of `taskData` with the parents (`TskPrn`, edit), the copy (copy) or none (new), with `TskEdtMod` (`new` when the
  task does not exist) and `TskWhn` (the launch type, or `3` for a manual task with parents). `getTaskEditorHeader` gives
  its header card (`title`, `subtitle`, `chip`, `chipTone`).
- `getTaskEditorParentOptions` (permission `scheduler.read`): the rows of `getTaskEditorParents` for the task of the
  mode with the fields of the select (`group`, `disabled`, `description`, see [criteria](api/criteria.md)).
- `getTaskEditorParameters` (permission `scheduler.update`): the parameters of the task (none for a new task, without
  identifiers for a copy).
- `getTaskEditorParentGraph` and `getTaskEditorChildGraph` (permission `scheduler.read`): the rows of the flow graph in
  task mode for the parents chosen (`TskPrn`, not saved yet) and for the tasks that run after the task as saved.
- `getTaskEditorWhenOptions` and `getTaskEditorExecutionOptions` (permission `scheduler.read`): the options of the
  choice cards of **When** and of the execution type (`value`, `label`, `icon`, `description`).
- `getTaskEditorSchedule` (permission `scheduler.read`): the preview of a scheduled task from the fields of the screen
  (the preset `SchPre` and its fields `SchEvy`, `SchTim`, `SchWkd`, `SchMdy`, and the stored fields): the repetition in a
  sentence whose key parts are bold (`title`, segments), its next five runs (`tiles`, `tilesLabel`), the first holiday
  it skips or why there are no runs (`description`) and how it is saved (`details`, `detailsLabel`); a preset with a
  field to fill asks for it. `getTaskEditor` brings the preset of the task and its fields (with `SchWkdPre`, the preset
  its days were filled for).
- `getTaskEditorWeekDayOptions` (permission `scheduler.read`): the day toggles (`value`, `label` its initial, `title`
  its full name).
- `SyncTaskEditorSchedule` (permission `scheduler.read`, saves nothing): the stored fields of the preset as select
  actions of their criteria; it also fills the days of *Daily* and *Workdays*, and selects *Weekly* when they change.
- `getTaskEditorScheduleMatch` and `BackToTaskEditorSchedulePreset` (permission `scheduler.read`, save nothing): the
  preset the custom repetition matches, and going back to it (the preset and its fields as select actions).
- Both saves end with `getTaskEditorSaved`, which answers with the saved task as `IdeTsk.selected`.

- `NewSchedulerTaskEditor` and `UpdateSchedulerTaskEditor` (permission `scheduler.update`) save the task (its configuration, launch, parameters, report and webhook hook; a scheduled task with a preset other than *Custom* stores the fields of the preset in its launch row, `applyTaskEditorSchedule`), but the dependencies are edited **from the child**: `TskPrn` is the list of the tasks it runs after, and its own rows of `AweSchTskDpn` (`IdeTsk` = the task) are replaced by them. The rows of the other tasks are kept, and `DpnOrd` is left empty (it orders the children of a parent in its flows). *After other tasks* is saved as a manual launch (`TypLch` = `0`) with parents. A parent that is the task itself or already runs after it, directly or through other tasks, is refused with the *Circular dependency* message before anything changes. After the save the task takes its new launch in the scheduler without purging its executions, their logs nor the files it has seen (a change of launch type adds or removes its trigger; `POST /scheduler/api/v1/task/{taskId}/refresh` with `awe.scheduler.remote-enabled=true`). The new trigger is built and checked before the old one is removed, so a task whose calendar is missing keeps its previous trigger and the save shows the error; only the scheduled and file groups of the task are touched. A trigger is never live when it should not fire: an inactive task, and a task whose trigger does not fire again after now, are left without trigger (none is added paused), and activating the task (`ActSchTsk`) adds its trigger from the stored task; the catalog and the task screen show it as inactive, without next execution. If the new trigger cannot be added the previous one comes back, rebuilt to start at its previous next fire (or at its first fire after now when that one has passed), so no past fire is launched as a misfire; a previous trigger that was paused does not come back, and the task stays without trigger until it is activated. Its parents are not touched in the scheduler: a task that ends reads the tasks that run after it from the database.
- `getTaskEditorParents` (permission `scheduler.read`): the tasks it can run after, every task but itself (`taskId`; none for a new task, and `copyTaskId` gives the parents of a duplicated task). Each row has `value`, `label`, `Act`, the condition of the parent `PrnCnd` (*Launches on OK*, *OK or warning*, *OK or error*, *OK, warning or error*: the flags of the parent, the same for all the tasks that run after it), its «When» summary (`PrnWhnTxt`, `PrnWhnIcn`, `PrnWhnSub`), its group `PrnGrp` (`chosen` first, `available`, then `blocked`, each group by name), `PrnSel`, and `PrnDis` with the reason `PrnDisTxt` (*Circular dependency: it already runs after this task*) for the tasks that run after it. A chosen task that already runs after it (a cycle saved before the editor) stays chosen with `PrnDis` = `0` and the reason, so it can be removed but not added again. The rows carry nothing restricted to the editors.
- `getTaskEditorCopy` (permission `scheduler.update`, as copying a task is editing it: the copy carries where it runs, its files and who gets its report): the fields of `taskData` of a task (`taskId`) to duplicate it, named *name (copy)* in the language of the user (the name is cut so that the whole fits in the 40 characters of a task name), inactive, without identifier nor webhook hook (its key cannot repeat), and with its parents (`TskPrn`). The tasks that run after it are not copied.

## Configuration screen

The **Configuration** screen (`scheduler-configuration`, permission `scheduler.read`; changing a server or a calendar
needs `scheduler.update`, operating the engine `scheduler.execute`) has three tabs: **Servers**, **Calendars** and
**Engine**. Each tab title tells how many servers or calendars there are, or the status of the engine. The tab is part
of the route: another screen opens one tab by sending `CfgTab` (`CfgSrv`, `CfgCal` or `CfgEng`); a reload of the
browser opens the first one.

| Tab | What it shows | Actions |
|-----|---------------|---------|
| **Servers** | The external servers of the file and SSH tasks: name, host, port, connection type, active state, **Used by** (a link to the Tasks catalog filtered by the server) and **Last test** (the last connection test of the session, never stored). Search by name or host and the active filter with its counts | **New server** and **Test all** (every active server); the row menu edits, tests, activates, deactivates (with a confirmation) and deletes. A server in use cannot be deleted, also on the server side. The side panel edits the connection and the authentication: saved secrets are never sent back (**Change**, **Replace**, **Remove**), and **Test connection** tests the values of the form without saving them |
| **Calendars** | The holiday calendars: name, description, dates (how many and the next one), active state and **Used by**. Search and active filter | **New calendar**; the side panel edits the name, the description, the active state and the dates on which the tasks do not run (**Add date**, **Import file** with an `.ics` or `.csv` file), with a read-only month view. Saving reloads the calendar in the engine. Deleting a calendar in use lists the tasks and dates it affects and removes it from them |
| **Engine** | The engine card (status, instance, version, job store, threads in use, running since, jobs executed and the technical details for support) and the schedules of the engine (task, trigger, calendar, previous and next execution, trigger state), filtered by all, paused or with error | **Pause…**, **Resume**, **Restart…** and **Stop and clear…**, each with a confirmation; stop and clear asks the operator to acknowledge that no task runs until the engine is resumed. When a deferred execution waits, a note links to **Activity** |

It reads the `getConfiguration*` queries (permission `scheduler.read`) and saves with the server targets
(`NewSchedulerServer`, `UpdSchSrv`, `ActSchSrv`, `DeaSchSrv`, `DeleteConfigurationServer`, `TstSchSrvCnx`,
`TstSchSrvCnxSel`, `TstAllSchSrvCnx`) and the calendar targets (`NewConfigurationCalendar`, `UpdConfigurationCalendar`,
`ActConfigurationCalendar`, `DeaConfigurationCalendar`, `DeleteConfigurationCalendar`,
`ImportConfigurationCalendarDates`), all with the permission `scheduler.update`. The engine actions use
`stopScheduler`, `startScheduler`, `restartScheduler` and `clearAndStop` (permission `scheduler.execute`).

## Flow run page

The flow run page (`scheduler-flow-run`, permission `scheduler.read`; stopping an execution needs `scheduler.execute`)
shows one [flow run](#flow-runs): what happened, why and what is left. Every flow run link opens it: the flow run column
and the side panel of the [Activity screen](#activity-screen), and the history, the last execution and the side panel
of the [task screen](#task-screen).
The link sends the run as `ExeRun.selected`, and the browser tab keeps it, so a reload opens the same run. Declare it as an
invisible option of **Activity** (see the menu above), so its breadcrumb goes back to **Activity**.

- **Header**: the run (its root task and execution) as the title, its final status, who launched it, its start, end and
  duration, how many of its tasks are done, the task the final status points to and the parameters of its launch. The
  info button next to the status explains its rule: **Error** when a task ended in error, stopped or interrupted, else
  **Warning** when one ended in warning or was skipped, else **OK**.
- **Views**: **Graph**, **Timeline** and **List**, one `flow-graph` widget in `run` mode each (see
  [Flow runs](#flow-runs)), chosen with the segmented control of the card.
- **Execution panel**: a task with an execution (a node, a bar or an entry of the list) opens a side panel with its
  status, its task (a link to its task screen), its times, who launched it, its live log and the stop action.

It reads `getFlowRunPageHeader`, `getFlowRunPageRule` and `getFlowRunSummary` (the run, from `IdeRun`),
`getFlowRunGraph` (its tasks) and `getFlowRunExecutionStatus` (the status of the selected execution).

## Webhook launch

A task with the **Webhook** launch type is launched when a signed `POST` reaches its hook URL (see the **[Scheduler guide](guides/scheduler-guide.md#4-webhook)**). The webhooks are received, stored and answered at once, and processed outside the request. They are configured with the following properties:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.webhooks.max-body-size` | Maximum size of the body of a webhook. A bigger one is answered with `413` | `256KB` |
| `awe.scheduler.webhooks.tolerance` | Maximum difference between the `webhook-timestamp` of a webhook and the current time. A webhook outside it is answered with `401` | `5m` |
| `awe.scheduler.webhooks.inbox-retention` | Time the processed, ignored and definitively failed webhooks stay in the inbox before they are purged | `30d` |
| `awe.scheduler.webhooks.process-interval` | Interval between the checks of the inbox for webhooks to launch | `10s` |

A duration without a unit is read in the unit of its default (minutes for `tolerance`, days for `inbox-retention`, seconds for `process-interval`).

```properties
awe.scheduler.webhooks.max-body-size=256KB
awe.scheduler.webhooks.tolerance=5m
awe.scheduler.webhooks.inbox-retention=30d
awe.scheduler.webhooks.process-interval=10s
```

### Security

The webhook path (`/scheduler/api/v1/webhooks/**`) needs no bearer token: it has its own security chain, and **the signature is the authentication**. A request is accepted only when its [Standard Webhooks](https://www.standardwebhooks.com/) signature is valid for the secret of the hook and its timestamp is within the tolerance. The rest of the scheduler API keeps requiring the bearer token (see [Scheduler API security](#scheduler-api-security)).

The secret is never stored in the database or shown on a screen: the hook holds the **name of an application property**, and the property holds the secret. It may hold several secrets separated by spaces or commas, and a webhook signed with any of them is accepted, so a secret can be rotated without downtime: add the new one, change the sender, then remove the old one.

```properties
fit.webhook.secret=whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=
# During a rotation both secrets are accepted
fit.webhook.secret=whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8= whsec_BAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=
```

### Migration

If you use `flyway`, the tables of the webhook launch (`AweSchTskHok`, `AweSchHokMsg` and the `WhkPtr` column of `AweSchTskPar`) come with the `SCHEDULER` migration module, version `1.0.10`.

## SSH remote command execution

Command tasks can run on a remote host over SSH (see the **[Scheduler guide](guides/scheduler-guide.md#command-execution-local-and-remote)**). The SSH clients used for remote execution and for SFTP file checking are configured with the following properties:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.ssh-host-key-policy` | Host-key verification policy. See the values below. | `ACCEPT_ON_FIRST_USE` |
| `awe.scheduler.ssh-known-hosts-path` | Path to the `known_hosts` file used to persist and read trusted host keys | `${user.home}/.ssh/known_hosts` |
| `awe.scheduler.ssh-connect-timeout` | SSH connect and authentication timeout, in seconds | `30s` |
| `awe.scheduler.connection-test-timeout` | Timeout for **Test connection** and **Test all** in the **Servers** tab of the [Configuration screen](#configuration-screen) (SSH session and FTP connect/data exchanges), in seconds | `10s` |

The host-key policy accepts three values:

| Value | Behaviour |
|-------|-----------|
| `ACCEPT_ON_FIRST_USE` | Trust-on-first-use: a host not yet present in `known_hosts` is accepted and stored on its first connection, and validated against the stored key afterwards. |
| `STRICT` | Only hosts already present in `known_hosts` are accepted; unknown hosts are rejected. |
| `ACCEPT_ALL` | Every host key is accepted without verification. Insecure — for development and testing only. |

```properties
awe.scheduler.ssh-host-key-policy=ACCEPT_ON_FIRST_USE
awe.scheduler.ssh-known-hosts-path=/opt/awe/.ssh/known_hosts
awe.scheduler.ssh-connect-timeout=30s
```

> **Note:** For production use prefer `STRICT` with a pre-provisioned `known_hosts`, or the default `ACCEPT_ON_FIRST_USE`. Avoid `ACCEPT_ALL` outside development.

## Deferred executions

A maintain task can answer that it accepted the work and finishes later (see the **[Scheduler guide](guides/scheduler-guide.md#deferred-executions)**). The deadline of those executions is configured with the following properties:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.deferred.default-timeout` | Time a deferred execution may stay running when its task has no timeout. After it the execution is interrupted | `24h` |
| `awe.scheduler.deferred.timeout-check-interval` | Interval between the checks for deferred executions past their deadline | `60s` |

A duration without a unit is read in the unit of its default (hours for the first property, seconds for the second).

```properties
awe.scheduler.deferred.default-timeout=12h
awe.scheduler.deferred.timeout-check-interval=30s
```

## Scheduler API security

The scheduler API (`/scheduler/api/**`) requires the AWE bearer token, validated as the [REST API](rest-module.md) validates it (`local-jwt` or `oauth2-resource-server`, according to `awe.rest.api.auth.mode`). Every route of the API needs it, including the one that completes a deferred execution.

The validation is done by the [AWE REST starter](rest-module.md). The scheduler does not depend on it, so the application that hosts the scheduler API has to include it. Without it no request can authenticate: every request is answered with `401` and a warning is written in the log at startup.

:::warning Breaking change for applications that use an external scheduler
Before, `/scheduler/api/**` was open. An application with the embedded scheduler is not affected, because it does not call that API. An application with `awe.scheduler.remote-enabled=true` now has to authenticate against the scheduler instance, and the scheduler instance has to include the AWE REST starter. Configure a user of the scheduler instance in the main application, or its calls to the scheduler will be answered with `401`:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.remote-scheduler-user` | User of the scheduler instance. The main application logs in with it at `/api/authenticate` of the scheduler instance (the `awe.scheduler.remote-scheduler-url` without its `/scheduler/api/v1` ending) and sends the token it gets. The token is reused until 30 seconds before it expires. When empty, the calls are sent without credentials and a warning is written in the log | empty |
| `awe.scheduler.remote-scheduler-password` | Password of that user | empty |

```properties
awe.scheduler.remote-enabled=true
awe.scheduler.remote-scheduler-url=https://scheduler.example.com/scheduler/api/v1
awe.scheduler.remote-scheduler-user=scheduler-client
awe.scheduler.remote-scheduler-password=ENC(yourEncryptedPassword)
```
:::

:::note Limitation: the OAuth2 resource-server mode of the scheduler instance
When the scheduler instance uses `awe.rest.api.auth.mode=oauth2-resource-server`, its `/api/authenticate` endpoint is disabled, so the login of the main application cannot get a token and its calls are refused. The scheduler API accepts the tokens of the identity provider, but the remote scheduler client does not obtain them: use the `local-jwt` mode in the scheduler instance when it is called by another AWE application.
:::

## Upgrade notes

- **Scheduler screens**: the old scheduler screens and their menu options were removed. Replace them in your menus,
  profiles and restrictions with **Activity**, **Tasks** and **Configuration**; the table of old and new options is in
  the [Scheduler redesign](guides/v5-migration.md#scheduler-redesign) entry of the migration guide.
- **Days of the week**: weekly tasks now fire on the configured day (they fired one day earlier). The task editor stores Monday as `1` to Saturday as `6` and Sunday as `0`, and the cron now converts them to the Quartz days (Sunday `1` to Saturday `7`); the stored tasks are not changed. The «When» summary of the [Tasks catalog](#tasks-catalog) reads the stored days the same way. With `awe.scheduler.remote-enabled=true`, upgrade the application and the scheduler instance together: the instance builds the cron and the application shows the «When» summary, so with only one of them upgraded they differ by one day.
