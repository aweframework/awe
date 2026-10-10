---
id: logging
title: Logging
sidebar_label: Logging
---

El framework AWE utiliza [Logback](https://logback.qos.ch/) para todo el registro interno, pero puede configurar su proyecto para usar otras librerías como [JUL](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) o [Log4j2](https://logging.apache.org/log4j/2.x/).  
En cada caso, los loggers vienen preconfigurados para usar la salida por consola, con salida opcional a archivo también disponible.

Por defecto, si utiliza los “Starters” de AWE, se usa Logback para el registro. También se incluye la configuración de Logback adecuada para garantizar que todo funcione correctamente.

## Configuración de Logback {#logback-configuration}
Si necesita aplicar personalizaciones a logback más allá de las que se pueden conseguir con `application.properties`, deberá añadir un archivo de configuración estándar de logback. Puede añadir un archivo `logback.xml` en la raíz de su classpath para que logback lo encuentre. También puede usar `logback-spring.xml` si quiere utilizar las [extensiones de Logback de Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.logging.logback-extensions).

El framework AWE proporciona varias configuraciones de logback que pueden `incluirse` desde su propia configuración. Estos includes están diseñados para permitir volver a aplicar ciertas convenciones comunes de Spring Boot.

El siguiente archivo se proporciona en `com/almis/awe/logging/` dentro de `awe-spring-boot-starter`:

- [awe-log.xml](logging#awe-logxml) - Proporciona reglas de conversión, propiedades de patrón y loggers comunes. También añade un `ConsoleAppender` y `RollingFileAppenders`

El siguiente archivo se proporciona en `com/almis/awe/scheduler/logging/`:

- [scheduler-log.xml](logging#scheduler-logxml) - Añade otro `RollingFileAppender` para registrar las ejecuciones de tareas del módulo AWE Scheduler.

Un archivo `logback-spring.xml` personalizado típico tendría un aspecto similar a este:

```xml title="Logback configuration"
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Includes -->
    <include resource="com/almis/awe/logging/awe-log.xml"/>
    <include resource="com/almis/awe/scheduler/logging/scheduler-log.xml"/> <!-- Optional (Only if you user Scheduler module)>
    <!-- Loggers -->
    <logger name="org.springframework.web" level="DEBUG"/>
</configuration>
```

:::note
Más información sobre el registro en Spring boot [aquí](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.logging)
:::

### awe-log.xml {#awe-logxml}
```xml title="AWE default Logback configuration"
<?xml version="1.0" encoding="UTF-8"?>
<!--
 AWE logback configuration provided for import
-->

<included>
  <!-- Conversion rules -->
  <conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter" />
  <conversionRule conversionWord="wex" converterClass="org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter" />
  <conversionRule conversionWord="wEx" converterClass="org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter" />

  <!-- Properties -->
  <springProperty name="APPLICATION_NAME" scope="context" source="spring.application.name" defaultValue="awe-app"/>
  <springProperty name="APPLICATION_LOG_PATH" scope="context" source="awe.application.log.group" defaultValue="/application"/>
  <property name="LOG_PATH" value="${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}}"/>
  <property name="CONSOLE_LOG_PATTERN" value="${CONSOLE_LOG_PATTERN:-%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-30.30logger{29}){cyan} %clr(:){faint} %clr(%X{execution}%X{user}%X{database}%X{currentScreen}){magenta}%m%n%wEx}"/>
  <property name="CONSOLE_LOG_CHARSET" value="${CONSOLE_LOG_CHARSET:-${file.encoding:-UTF-8}}"/>
  <property name="FILE_LOG_PATTERN" value="${FILE_LOG_PATTERN:-%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %X{execution}%X{user}%X{database}%X{currentScreen}%m%n%wEx}"/>
  <property name="FILE_LOG_CHARSET" value="${FILE_LOG_CHARSET:-${file.encoding:-UTF-8}}"/>

  <!--  Loggers -->
  <logger name="org.apache.catalina.startup.DigesterFactory" level="ERROR"/>
  <logger name="org.apache.catalina.util.LifecycleBase" level="ERROR"/>
  <logger name="org.apache.coyote.http11.Http11NioProtocol" level="WARN"/>
  <logger name="org.apache.sshd.common.util.SecurityUtils" level="WARN"/>
  <logger name="org.apache.tomcat.util.net.NioSelectorPool" level="WARN"/>
  <logger name="org.eclipse.jetty.util.component.AbstractLifeCycle" level="ERROR"/>
  <logger name="org.hibernate.validator.internal.util.Version" level="WARN"/>
  <logger name="org.springframework.boot.actuate.endpoint.jmx" level="WARN"/>

  <!-- CONSOLE Appender -->
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${CONSOLE_LOG_PATTERN}</pattern>
      <charset>${CONSOLE_LOG_CHARSET}</charset>
    </encoder>
  </appender>

  <!-- FILE Appender -->
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}${APPLICATION_LOG_PATH}/${APPLICATION_NAME}.log</file>
    <encoder>
      <pattern>${FILE_LOG_PATTERN}</pattern>
      <charset>${FILE_LOG_CHARSET}</charset>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_PATH}${APPLICATION_LOG_PATH}/${APPLICATION_NAME}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
      <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
      <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
      <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
      <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
    </rollingPolicy>
  </appender>

  <!-- USER Appender (logs by user)-->
  <appender name="USER" class="ch.qos.logback.classic.sift.SiftingAppender">
    <discriminator>
      <key>logUserName</key>
      <defaultValue>anonymous</defaultValue>
    </discriminator>
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
      <evaluator class="ch.qos.logback.classic.boolex.JaninoEventEvaluator">
        <expression>
          mdc.get("logUserName")!=null
        </expression>
      </evaluator>
      <OnMismatch>DENY</OnMismatch>
      <OnMatch>NEUTRAL</OnMatch>
    </filter>
    <sift>
      <appender name="FILE-${logUserName}" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}${APPLICATION_LOG_PATH}/${APPLICATION_NAME}_${logUserName}.log</file>
        <encoder>
          <pattern>${FILE_LOG_PATTERN}</pattern>
          <charset>${FILE_LOG_CHARSET}</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
          <fileNamePattern>
            ${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_PATH}${APPLICATION_LOG_PATH}/${APPLICATION_NAME}_${logUserName}.%d{yyyy-MM-dd}.%i.gz}
          </fileNamePattern>
          <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
          <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
          <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
          <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
        </rollingPolicy>
      </appender>
    </sift>
  </appender>

  <!-- ROOT logger -->
  <root level="info">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
    <appender-ref ref="USER"/>
  </root>
</included>
```

### scheduler-log.xml {#scheduler-logxml}
```xml title="AWE Scheduler Logback configuration"
<?xml version="1.0" encoding="UTF-8"?>
<!--
Scheduler AWE module logback configuration provided for import
-->

<included>
  <springProperty name="EXECUTION_LOG_PATTERN" scope="context" source="awe.scheduler.execution-log-pattern" defaultValue="%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx"/>
  <property name="APPLICATION_LOG_PATH" value="${APPLICATION_LOG_PATH:-/application}"/>

  <!-- SCHEDULER FILE Appender -->
  <appender name="SCHEDULER" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}${APPLICATION_LOG_PATH}/SCHEDULER.log</file>
    <encoder>
      <pattern>${FILE_LOG_PATTERN}</pattern>
      <charset>${FILE_LOG_CHARSET}</charset>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_PATH}${APPLICATION_LOG_PATH}/SCHEDULER.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
      <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
      <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
      <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
      <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
    </rollingPolicy>
  </appender>

  <!-- SCHEDULER EXECUTION Appender (logs by user)-->
  <appender name="SCHEDULER_EXECUTION" class="ch.qos.logback.classic.sift.SiftingAppender">
    <discriminator>
      <key>logByTaskExecution</key>
      <defaultValue>unknown</defaultValue>
    </discriminator>
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
      <evaluator class="ch.qos.logback.classic.boolex.JaninoEventEvaluator">
        <expression>
          mdc.get("logByTaskExecution")!=null
        </expression>
      </evaluator>
      <OnMismatch>DENY</OnMismatch>
      <OnMatch>NEUTRAL</OnMatch>
    </filter>
    <sift>
      <appender name="FILE-${logByTaskExecution}" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/scheduler/execution_${logByTaskExecution}.log</file>
        <encoder>
          <pattern>${EXECUTION_LOG_PATTERN}</pattern>
          <charset>${FILE_LOG_CHARSET}</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
          <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_PATH}/scheduler/execution_${logByTaskExecution}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
          <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
          <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
          <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
          <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
        </rollingPolicy>
      </appender>
    </sift>
  </appender>

  <!-- Scheduler logger -->
  <logger name="com.almis.awe.scheduler" level="INFO">
    <appender-ref ref="SCHEDULER"/>
  </logger>

  <root level="info">
    <appender-ref ref="SCHEDULER_EXECUTION"/>
  </root>

</included>
```

## Log de ejecución del scheduler {#scheduler-execution-log}

Cada ejecución de una tarea del scheduler tiene su propio log, que puede abrir desde la pantalla de gestión de tareas
con el botón **Show execution log**. El lugar donde se almacena ese log lo controla
[awe.scheduler.execution-log-store](properties.md#awe.scheduler.execution-log-store):

- **`file`** (por defecto) — cada ejecución escribe su log en un archivo dentro de
  [awe.scheduler.execution-log-path](properties.md#awe.scheduler.execution-log-path). El visor
  lee ese archivo del sistema de archivos local, por lo que solo funciona cuando el visor y la ejecución se encuentran
  en el **mismo nodo** (o comparten sistema de archivos).
- **`database`** — los logs de ejecución se almacenan en su lugar en la base de datos de la aplicación. El visor
  funciona entonces desde **cualquier nodo**: despliegues con varias réplicas (por ejemplo Kubernetes con varios pods) y
  la topología de [scheduler remoto](scheduler-module.md), donde la tarea se ejecuta en una instancia distinta
  de la que muestra la pantalla.

Para habilitar el almacenamiento en base de datos, establezca:

```properties
awe.scheduler.execution-log-store=database
```

No se requiere ningún otro cambio: las tareas, las pantallas y el visor se comportan igual en ambos modos.

### Qué muestra el visor {#what-the-viewer-shows}

Con el almacenamiento en base de datos, el log que se guarda por ejecución es una **ventana acotada**: las primeras líneas de la
ejecución más las más recientes, hasta un total de
[awe.scheduler.execution-log-max-lines](properties.md#awe.scheduler.execution-log-max-lines) líneas
(por defecto `1000`).

- Si la ejecución produce menos líneas que el límite, verá el log completo.
- Si produce más, verá el principio y el final, con una línea marcadora entre ambos que indica cuántas
  líneas se omitieron y cuántas líneas produjo realmente la ejecución. El log nunca queda
  incompleto de forma silenciosa.
- Una única línea más larga de
  [awe.scheduler.execution-log-max-line-length](properties.md#awe.scheduler.execution-log-max-line-length)
  caracteres se acorta con puntos suspensivos.

Mientras una tarea se está ejecutando, el visor se actualiza automáticamente y sigue las líneas más recientes
(live tail). Aumentar `execution-log-max-lines` conserva más historial por ejecución a costa de más
almacenamiento y de más datos transferidos al visor en cada actualización — dimensiónelo según lo que un operador
necesita ver, no para la retención completa de logs.

### Garantías {#guarantees}

El registro de ejecución nunca interfiere con la propia tarea: las líneas de log se almacenan en segundo plano
y, si la base de datos es lenta o no está disponible, la tarea se completa con normalidad — como mucho se descartan algunas líneas
de log, y una línea marcadora indica cuándo ocurrió.

El visor de logs de ejecución es una ventana operativa acotada, **no** un sustituto de la retención completa de logs.
Las mismas líneas se emiten siempre a través del flujo de registro habitual de la aplicación
(consola / archivos de log), que sigue siendo la fuente adecuada para logs completos y de largo plazo — por ejemplo
a través de un agregador centralizado de logs como Grafana Loki.

## Logs de ejecución con un scheduler remoto {#execution-logs-with-a-remote-scheduler}

Cuando el scheduler se ejecuta como una instancia independiente
([awe.scheduler.remote-enabled](properties.md#awe.scheduler.remote-enabled)), el cuerpo de una
tarea de mantenimiento se ejecuta realmente en la **aplicación**, no en el scheduler. Con el almacenamiento `database`
activo en ambas instancias, las trazas producidas por la tarea en la aplicación se capturan en el
mismo log de ejecución, de modo que el visor muestra un único log ordenado en el tiempo de toda la ejecución — las
líneas del propio scheduler y la salida real de la tarea, en vivo mientras se ejecuta.

Además, cada ejecución termina con una **línea de resumen** que indica cómo finalizó y cuánto
tardó. Para el estado final y definitivo de una ejecución (incluidos los tiempos de espera agotados y las cancelaciones),
la tabla de ejecuciones sigue siendo la referencia.

### Configuración {#configuration}

La captura remota de trazas está **habilitada por defecto** siempre que la aplicación utilice el almacenamiento `database`.
Puede desactivarse con
[awe.scheduler.execution-log-callback-capture](properties.md#awe.scheduler.execution-log-callback-capture)`=false`.

La aplicación identifica las llamadas del scheduler mediante las mismas credenciales de callback que el scheduler
remoto ya utiliza. Para que la captura funcione, la **aplicación** debe declarar el mismo usuario
de callback que el scheduler:

```properties
# On the application (same value the scheduler uses to authenticate its callbacks)
awe.scheduler.remote-callback-user=<callback user>
```

Si la aplicación no establece esta propiedad mientras
[awe.scheduler.remote-callback-secure-enabled](properties.md#awe.scheduler.remote-callback-secure-enabled)
sea `true` (el valor por defecto), las trazas remotas no se capturan y la aplicación registra un `WARN` al
arrancar explicando qué propiedad falta.

**Nota de seguridad.** Con `remote-callback-secure-enabled=false`, los callbacks del scheduler no están
autenticados y la captura de trazas confía entonces en cualquier llamante que alcance el endpoint de callback. Utilice
esa configuración solo cuando el endpoint sea accesible exclusivamente desde el interior de la red de
despliegue (por ejemplo, tráfico entre pods que nunca se expone a través de un ingress).

El scheduler identifica cada ejecución en sus peticiones de callback mediante la
cabecera HTTP `X-AWE-Execution-Key` — si un proxy entre el scheduler y la aplicación elimina
las cabeceras personalizadas, la captura remota de trazas no funcionará.

### Nota de actualización {#upgrade-note}

El almacenamiento en base de datos incluye su tabla dentro de las migraciones de Flyway del módulo `SCHEDULER`. Si un
entorno aplicó una versión preliminar de la migración `SCHEDULER_V1.0.6`, vuelva a aplicarla
(`flyway repair`, o recree el esquema) antes de iniciar la aplicación actualizada.


## Formato del log {#log-format}
La salida de log por defecto de AWE Framework se parece al siguiente ejemplo:
```log title="Log format"
2022-03-17 10:57:32.583  INFO [awe-boot,dc4de32c41616ee9,dc4de32c41616ee9] 26656 --- [nio-8080-exec-8] .s.d.c.q.ServiceQueryConnector : [user: test] [screen: home] [getScreenRestrictions] =>  0 records. Prepare service time: 0.0s - Service time: 0.01s - Datalist time: 0.0s - Total time: 0.01s
2022-03-17 10:57:32.597  INFO [awe-boot,18bb85bd7bf4ef42,18bb85bd7bf4ef42] 26656 --- [nio-8080-exec-3] c.a.awe.service.ScreenService  : [user: test] [screen: home] Screen parameters retrieved - keepCriteria-information - {}
2022-03-17 10:57:32.609  INFO [awe-boot,18bb85bd7bf4ef42,18bb85bd7bf4ef42] 26656 --- [   AweThread-10] .a.a.s.d.c.q.SQLQueryConnector : [user: test] [screen: home] [getScreenConfiguration] [select ScrCnf.IdeAweScrCnf as id, ope.l1_nom as "user", pro.Acr as profile, ScrCnf.Nam as component, ScrCnf.Scr as screen, ScrCnf.Atr as attribute, ScrCnf.Val as "value" from AweScrCnf ScrCnf left join AwePro as pro on ScrCnf.IdePro = pro.IdePro left join ope as ope on ScrCnf.IdeOpe = ope.IdeOpe where (ScrCnf.Scr = 'info' or ScrCnf.Scr is null) and (lower(trim(both from ope.l1_nom)) = lower('test') or ScrCnf.IdeOpe is null) and (trim(both from pro.Acr) = 'ADM' or ScrCnf.IdePro is null) and ScrCnf.Act = 1] => 0 records. Create query time: 0.004s - Sql time: 0.002s - Datalist time: 0.001s - Total time: 0.007s
2022-03-17 10:57:32.803  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [   AweThread-11] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [printTabs] => 2 records. Create enumerated time: 0.0s - Enumerated time: 0.001s - Datalist time: 0.001s - Total time: 0.002s
2022-03-17 10:57:32.804  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [   AweThread-12] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [printTabs] => 2 records. Create enumerated time: 0.003s - Enumerated time: 0.0s - Datalist time: 0.001s - Total time: 0.004s
2022-03-17 10:57:32.815  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [    AweThread-2] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [EnuQueTabSel] => 2 records. Create enumerated time: 0.0s - Enumerated time: 0.0s - Datalist time: 0.0s - Total time: 0.0s
```
Se muestran los siguientes elementos:
- Fecha y hora: Precisión de milisegundos y fácilmente ordenable.
- Nivel de log: `ERROR`, `WARN`, `INFO`, `DEBUG` o `TRACE`.
- Breadcrumbs. Se utilizan para la observabilidad y el rastreo de las peticiones de servicio.
- ID del proceso.
- Un separador `---` para distinguir el inicio de los mensajes de log propiamente dichos.
- Nombre del hilo: Entre corchetes (puede truncarse en la salida por consola).
- Nombre del logger: Normalmente es el nombre de la clase de origen (a menudo abreviado).
- Campos del mensaje de log [user][screen][database].
- El mensaje de log.

Puede personalizar el formato mediante esta propiedad:

| Entorno de Spring         | Propiedad del sistema | Comentarios                                     |
|---------------------------|-----------------------|-------------------------------------------------|
| `logging.pattern.console` | `CONSOLE_LOG_PATTERN` | El patrón de log a usar en la consola (stdout). |
| `logging.pattern.file`    | `FILE_LOG_PATTERN`    | El patrón de log a usar en un archivo.          |

## Salida a archivo de log {#log-file-output}
Por defecto, si utiliza los includes de configuración de logback de AWE, los logs se escriben en archivos de log. El destino de salida por defecto tiene como valor `${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}}` y `LOG_PATH` se rellena con la propiedad de spring boot de registro `logging.file.path`.

| Entorno de Spring   | Propiedad del sistema | Comentarios                                                                         |
|---------------------|-----------------|-------------------------------------------------------------------------------------------|
| `logging.file.path` | `LOG_PATH`       | Si se define, se usa en la configuración de log por defecto para configurar la ruta de destino del log |

## Rotación de archivos de log {#log-file-rotation}
Por defecto, los archivos de log se rotan cuando el archivo alcanza 10 Mb, pero puede ajustar la política de rotación mediante las siguientes propiedades:

| Nombre                                                 | Descripción                                                            | Valor por defecto |
|--------------------------------------------------------|------------------------------------------------------------------------|---------------|
| `logging.logback.rollingpolicy.file-name-pattern`      | El patrón de nombre de archivo usado para crear los archivos de log.   |               |
| `logging.logback.rollingpolicy.clean-history-on-start` | Si debe limpiarse el archivo de logs al arrancar la aplicación.        | `false`       |
| `logging.logback.rollingpolicy.max-file-size`          | El tamaño máximo del archivo de log antes de archivarse.               | `10MB`        |
| `logging.logback.rollingpolicy.total-size-cap`         | El tamaño máximo que pueden ocupar los archivos de log antes de eliminarse. | `0`           |
| `logging.logback.rollingpolicy.max-history`            | El número máximo de archivos de log archivados a conservar.            | `7`           | 
