---
id: scheduler
title: Módulo Scheduler
sidebar_label: Módulo Scheduler
---

El módulo Scheduler añade una potente herramienta de planificación a tu aplicación

Para activar este módulo, sigue estos pasos:

- Añade las **dependencias awe scheduler** al descriptor pom.xml.

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

- Añade las pantallas del planificador en tu archivo `private.xml`:

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

- Configura el valor de la propiedad para añadir `awe-scheduler` a la lista de módulos.

```properties
awe.application.module-list = APP, ..., awe-scheduler, ..., awe
```

- Por último, si estás usando `flyway`, añade las tablas del planificador al módulo de migración:

```properties
awe.database.migration-modules=AWE,...,SCHEDULER,...
```

## Ejecución remota de comandos por SSH {#ssh-remote-command-execution}

Las tareas de comando pueden ejecutarse en un host remoto mediante SSH (consulta la **[guía del planificador](guides/scheduler-guide.md#command-execution-local-and-remote)**). Los clientes SSH usados para la ejecución remota y para la comprobación de archivos por SFTP se configuran con las siguientes propiedades:

| Propiedad | Descripción | Valor por defecto |
|----------|-------------|---------|
| `awe.scheduler.ssh-host-key-policy` | Política de verificación de claves de host. Consulta los valores más abajo. | `ACCEPT_ON_FIRST_USE` |
| `awe.scheduler.ssh-known-hosts-path` | Ruta al archivo `known_hosts` usado para persistir y leer las claves de host de confianza | `${user.home}/.ssh/known_hosts` |
| `awe.scheduler.ssh-connect-timeout` | Tiempo de espera de conexión y autenticación SSH, en segundos | `30s` |
| `awe.scheduler.connection-test-timeout` | Tiempo de espera del botón **Test connection** de las pantallas de servidores (sesión SSH e intercambios de conexión/datos FTP), en segundos | `10s` |

La política de claves de host admite tres valores:

| Valor | Comportamiento |
|-------|-----------|
| `ACCEPT_ON_FIRST_USE` | Confianza en el primer uso: un host que aún no está en `known_hosts` se acepta y se almacena en su primera conexión, y después se valida contra la clave almacenada. |
| `STRICT` | Solo se aceptan los hosts que ya están en `known_hosts`; los hosts desconocidos se rechazan. |
| `ACCEPT_ALL` | Se acepta cualquier clave de host sin verificación. Inseguro: solo para desarrollo y pruebas. |

```properties
awe.scheduler.ssh-host-key-policy=ACCEPT_ON_FIRST_USE
awe.scheduler.ssh-known-hosts-path=/opt/awe/.ssh/known_hosts
awe.scheduler.ssh-connect-timeout=30s
```

> **Nota:** Para uso en producción prefiere `STRICT` con un `known_hosts` preprovisionado, o el valor por defecto `ACCEPT_ON_FIRST_USE`. Evita `ACCEPT_ALL` fuera del desarrollo.
