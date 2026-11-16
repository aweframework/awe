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
  <option name="scheduler-management" label="MENU_SCHEDULER_MANAGEMENT" screen="scheduler-management" icon="cogs"/>
  <option name="scheduler-tasks" label="MENU_SCHEDULER_TASKS" screen="scheduler-tasks" icon="tasks">
    <option name="new-scheduler-task" screen="new-scheduler-task" invisible="true" />
    <option name="update-scheduler-task" screen="update-scheduler-task" invisible="true" />
  </option>
  <option name="scheduler-servers" label="MENU_SCHEDULER_SERVERS" screen="scheduler-server" icon="server">
    <option name="new-scheduler-server" screen="new-scheduler-server" invisible="true" />
    <option name="update-scheduler-server" screen="update-scheduler-server" invisible="true" />
  </option>
  <option name="scheduler-calendars" label="MENU_SCHEDULER_CALENDARS" screen="scheduler-calendars" icon="calendar">
    <option name="new-scheduler-calendar" screen="new-scheduler-calendar" invisible="true" />
    <option name="update-scheduler-calendar" screen="update-scheduler-calendar" invisible="true" />
  </option>
</option>
```

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

Every task launched by its schedule, by hand or by a file starts a flow run that groups the executions of its dependencies, with a final status and optional run parameters. A task with several parents is launched once, when all its parents in the run have ended. The **Flow runs** screen (`scheduler-runs`, permission `scheduler.read`) lists the runs with their executions and skipped tasks. See **[Dependencies between tasks](guides/dependencies-guide.md#flow-runs-and-fan-in)**.

If you use `flyway`, the tables of the flow runs (`AweSchRun`, `AweSchRunPar`, `AweSchRunTsk` and the `IdeRun` column of `AweSchExe`) come with the `SCHEDULER` migration module, version `1.0.9`.

## SSH remote command execution

Command tasks can run on a remote host over SSH (see the **[Scheduler guide](guides/scheduler-guide.md#command-execution-local-and-remote)**). The SSH clients used for remote execution and for SFTP file checking are configured with the following properties:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.ssh-host-key-policy` | Host-key verification policy. See the values below. | `ACCEPT_ON_FIRST_USE` |
| `awe.scheduler.ssh-known-hosts-path` | Path to the `known_hosts` file used to persist and read trusted host keys | `${user.home}/.ssh/known_hosts` |
| `awe.scheduler.ssh-connect-timeout` | SSH connect and authentication timeout, in seconds | `30s` |
| `awe.scheduler.connection-test-timeout` | Timeout for the **Test connection** button on the server screens (SSH session and FTP connect/data exchanges), in seconds | `10s` |

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
