---
id: dependency-task
title: Dependencies between tasks
sidebar_label: Dependencies between tasks
---

A task can run after other tasks: its **parents**. When a parent ends, the scheduler launches the tasks that run after
it (its **dependencies**, or children), and those tasks can have dependencies of their own, creating a workflow.

## Dependencies configuration

The dependencies of a task are chosen **in the child**, in the **When** section of the
[task editor](../scheduler-module.md#task-editor) (`scheduler-task-editor`):

1. Open the task in the editor (**Edit** in the [Tasks catalog](../scheduler-module.md#tasks-catalog) or in the
   [task screen](../scheduler-module.md#task-screen), or **Change what it depends on** in its **Dependencies** tab).
2. In **When**, choose **After other tasks** and pick its parents. The list groups the tasks in *Chosen*, *Available*
   and *Cannot be chosen* (the tasks that already run after it, which would close a loop).
3. Save. The task is stored as a `Manual` launch with its parents, and it runs when they end.

| Setting | Where it is set | Meaning |
|---------|-----------------|---------|
| Parents | **When** › **After other tasks** of the child | The tasks it runs after. With several parents it waits for all of them (see [fan-in](#flow-runs-and-fan-in)) |
| Condition | **Behaviour** of the **parent** | When the parent launches its children: always on OK, also on warning, also on error. It is the same for every child of that parent, so the child shows it read only, with a link to the parent |
| Mark the parent as warning | **Behaviour** of the **parent** | The parent ends as warning when one of its dependencies ends with an error |

The **Dependencies** section of the editor and the **Dependencies** tab of the task screen show, read only, the tasks
that run after it. Leaving **After other tasks** with parents chosen asks for a confirmation first, because it clears
them.

## Workflows

<img alt="Workflow example" src={require('@docusaurus/useBaseUrl').default('img/Dependency-tasks.png')} />

This image shows an example on how to create a workflow using the scheduler dependencies concatenation.

## Flow runs and fan-in

A task that is launched by its schedule, by hand, by a file or by a webhook starts a **flow run**. The run groups that execution and every
dependency launched from it, directly or through other dependencies: these tasks are the **scope** of the run.

A task with several parents (**fan-in**) is launched once, not once per parent. The child waits until every parent that is in
the scope of the run has ended, and it is launched only if each one met its condition: it ended successfully, or with a
warning or an error when the parent launches its dependencies on warning or on error. If any parent did not meet its
condition, the child is **skipped**, and the skip propagates: the children of a skipped task are skipped in turn.

The run ends when no task is waiting or running. Its status is the worst of its executions: **error** (error, stopped or
interrupted), then **warning** (warning, or any skipped task), then **ok**.

### Parameters of a dependency

Each parameter of a dependency takes the first value found in this order:

1. the default value of the task;
2. the value propagated by the parent that completes the join (the one that ends last);
3. the run parameter with the same name. Run parameters apply only to `VARIABLE` parameters: the child defines which
   parameters it accepts. The run parameters of a [webhook](scheduler-guide.md#4-webhook) are the values mapped from its data,
   so they reach every step of the flow.

### Circular dependencies

Saving a task whose dependencies would make it launch itself, directly or through other tasks, is rejected with a
**Circular dependency** message. Remove the dependency that closes the loop.

### Flow run page

Every flow run link opens the [flow run page](../scheduler-module.md#flow-run-page) (`scheduler-flow-run`): the flow
run column and the side panel of the [Activity screen](../scheduler-module.md#activity-screen) (also its **Group by
flow** view, which lists the runs newest first), and the history and the last execution of the
[task screen](../scheduler-module.md#task-screen). The page shows the final status of the run and why, who launched it,
its start, duration and parameters, and its tasks as a graph, a timeline or a list, with the skipped ones; a task opens
the detail of its execution.
