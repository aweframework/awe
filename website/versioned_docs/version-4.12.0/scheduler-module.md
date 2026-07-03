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

- Finally, if you are using `flyway`, add the scheduler tables into the migration module:

```properties
awe.database.migration-modules=AWE,...,SCHEDULER,...
```

## SSH remote command execution

Command tasks can run on a remote host over SSH (see the **[Scheduler guide](guides/scheduler-guide.md#command-execution-local-and-remote)**). The SSH client used for remote execution is configured with the following properties:

| Property | Description | Default |
|----------|-------------|---------|
| `awe.scheduler.ssh-host-key-policy` | Host-key verification policy. See the values below. | `ACCEPT_ON_FIRST_USE` |
| `awe.scheduler.ssh-known-hosts-path` | Path to the `known_hosts` file used to persist and read trusted host keys | `${user.home}/.ssh/known_hosts` |
| `awe.scheduler.ssh-connect-timeout` | SSH connect and authentication timeout, in seconds | `30s` |

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