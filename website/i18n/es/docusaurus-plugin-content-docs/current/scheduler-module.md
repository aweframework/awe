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

- Da a los perfiles que usan las pantallas del planificador los permisos `scheduler.read`, `scheduler.update` y `scheduler.execute` (solo los tiene el perfil de administrador). Consulta [Permisos](security/permissions.md#catalog-resources).

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


## Ejecuciones diferidas {#deferred-executions}

Una tarea maintain puede responder que ha aceptado el trabajo y que finaliza más tarde (consulta la **[guía del planificador](guides/scheduler-guide.md#deferred-executions)**). El plazo de esas ejecuciones se configura con las siguientes propiedades:

| Propiedad | Descripción | Por defecto |
|-----------|-------------|-------------|
| `awe.scheduler.deferred.default-timeout` | Tiempo que una ejecución diferida puede seguir en marcha cuando su tarea no tiene timeout. Pasado ese tiempo la ejecución se interrumpe | `24h` |
| `awe.scheduler.deferred.timeout-check-interval` | Intervalo entre las comprobaciones de las ejecuciones diferidas que han superado su plazo | `60s` |

Una duración sin unidad se lee en la unidad de su valor por defecto (horas para la primera propiedad, segundos para la segunda).

```properties
awe.scheduler.deferred.default-timeout=12h
awe.scheduler.deferred.timeout-check-interval=30s
```

## Seguridad de la API del planificador {#scheduler-api-security}

La API del planificador (`/scheduler/api/**`) exige el token bearer de AWE, validado como lo valida la [API REST](rest-module.md) (`local-jwt` u `oauth2-resource-server`, según `awe.rest.api.auth.mode`). Todas las rutas de la API lo necesitan, incluida la que completa una ejecución diferida.

La validación la hace el [starter de AWE REST](rest-module.md). El planificador no depende de él, así que la aplicación que aloja la API del planificador debe incluirlo. Sin él ninguna petición puede autenticarse: todas reciben `401` y al arrancar se escribe una advertencia en el log.

:::warning Cambio incompatible para las aplicaciones que usan un planificador externo
Antes, `/scheduler/api/**` estaba abierta. Una aplicación con el planificador embebido no se ve afectada, porque no llama a esa API. Una aplicación con `awe.scheduler.remote-enabled=true` ahora debe autenticarse contra la instancia del planificador, y la instancia del planificador debe incluir el starter de AWE REST. Configura en la aplicación principal un usuario de la instancia del planificador, o sus llamadas al planificador recibirán `401`:

| Propiedad | Descripción | Por defecto |
|-----------|-------------|-------------|
| `awe.scheduler.remote-scheduler-user` | Usuario de la instancia del planificador. La aplicación principal inicia sesión con él en `/api/authenticate` de la instancia del planificador (la `awe.scheduler.remote-scheduler-url` sin su terminación `/scheduler/api/v1`) y envía el token que obtiene. El token se reutiliza hasta 30 segundos antes de que caduque. Si está vacío, las llamadas se envían sin credenciales y se escribe una advertencia en el log | vacío |
| `awe.scheduler.remote-scheduler-password` | Contraseña de ese usuario | vacío |

```properties
awe.scheduler.remote-enabled=true
awe.scheduler.remote-scheduler-url=https://scheduler.example.com/scheduler/api/v1
awe.scheduler.remote-scheduler-user=scheduler-client
awe.scheduler.remote-scheduler-password=ENC(yourEncryptedPassword)
```
:::

:::note Limitación: el modo OAuth2 resource server de la instancia del planificador
Cuando la instancia del planificador usa `awe.rest.api.auth.mode=oauth2-resource-server`, su endpoint `/api/authenticate` está deshabilitado, así que el inicio de sesión de la aplicación principal no puede obtener un token y sus llamadas se rechazan. La API del planificador acepta los tokens del proveedor de identidad, pero el cliente del planificador remoto no los obtiene: usa el modo `local-jwt` en la instancia del planificador cuando la llama otra aplicación AWE.
:::
