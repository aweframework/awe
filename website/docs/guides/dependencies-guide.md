---
id: dependency-task
title: Dependencies between tasks
sidebar_label: Dependencies between tasks
---

Another powerful feature of the Scheduler module is the possibility of adding dependencies to a task. The dependencies are tasks that will be launched just after the parent task finishes. These dependencies can also contain other dependencies, creating a workflow.

## Types
There are two main types of dependencies.

### Synchronous dependencies

The synchronous dependencies are, as their name suggest, executed in a synchronous way, in a configurable order.

The synchronous dependencies are created by setting the Blocking=Yes option.

Setting blocking to `yes` means that, in case that any of the dependencies ends with an error, it will cancel the entire synchronous execution stack.

To manage the stack, there is the Order criterion, which is used to set the order in which the synchronous dependencies are going to be executed. 

### Asynchronous dependencies

The asynchronous dependencies are executed in bulk and their execution order can't be configured.

As the dependencies are executed in bulk, they are not going to block each other

## Dependencies configuration

The dependency configuration screen allows to create two type of dependencies, synchronous and asynchronous.

The difference between them is just that one is blocking whereas the other is not.

**In order to add a task as a dependency, its launch type must be set to `Manual`.**

| Element       | Definition    | Use   |
| ------------- |:-------------:| -----:|
| Task          | The task the dependency is going to execute    | **Required** |
| Blocking      | Used to define if the dependency is going to be synchronous or asynchronous, and if it can cancel the synchronous dependencies execution stack | **Required** |
| Order         | Synchronous dependency execution order, only needed if the `Blocking` option is set to `Yes`, otherwise it will be disabled |  **Required** |

## Workflows

<img alt="Workflow example" src={require('@docusaurus/useBaseUrl').default('img/Dependency-tasks.png')} />

This image shows an example on how to create a workflow using the scheduler dependencies concatenation.

## Flow runs and fan-in

A task that is launched by its schedule, by hand or by a file starts a **flow run**. The run groups that execution and every
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
   parameters it accepts.

### Circular dependencies

Saving a task whose dependencies would make it launch itself, directly or through other tasks, is rejected with a
**Circular dependency** message. Remove the dependency that closes the loop.

### Flow runs screen

The **Flow runs** option of the scheduler menu lists the runs, newest first, with their status, root task, root execution,
who launched it, and start and end times. Selecting a run shows its executions and the tasks that were skipped. In the task
manager, select an execution and press **Open run** to go straight to its run.
