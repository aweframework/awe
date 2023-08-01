---
id: logging
title: Logging
sidebar_label: Logging
---

AWE framework uses [Logback](https://logback.qos.ch/) for all internal logging but you can configure your project to use other libraries like [JUL](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) or [Log4j2](https://logging.apache.org/log4j/2.x/).  
In each case, loggers are pre-configured to use console output with optional file output also available.

By default, if you use the AWE “Starters”, Logback is used for logging. Appropriate Logback configuration is also included to ensure all work correctly.

## Logback Configuration
If you need to apply customizations to logback beyond those that can be achieved with `application.properties`, you’ll need to add a standard logback configuration file. You can add a `logback.xml` file to the root of your classpath for logback to find. You can also use `logback-spring.xml` if you want to use the [Spring Boot Logback extensions](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.logging.logback-extensions).

AWE framework provides a number of logback configurations that be `included` from your own configuration. These includes are designed to allow certain common Spring Boot conventions to be re-applied.

The following file is provided under `com/almis/awe/logging/` in `awe-spring-boot-starter`:

- [awe-log.xml](logging#awe-logxml) - Provides conversion rules, pattern properties and common loggers. Also adds a `ConsoleAppender` and `RollingFileAppenders`

The following file is provided under `com/almis/awe/scheduler/logging/`:

- [scheduler-log.xml](logging#scheduler-logxml) - Adds other `RollingFileAppender` to logging executions task of AWE Scheduler module.

A typical custom `logback-spring.xml` file would look something like this:

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
More info about logging in Spring boot [here](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.logging)
:::

### awe-log.xml
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

### scheduler-log.xml
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

## Log Format
The default log output from AWE Framework resembles the following example:
```log title="Log format"
2022-03-17 10:57:32.583  INFO [awe-boot,dc4de32c41616ee9,dc4de32c41616ee9] 26656 --- [nio-8080-exec-8] .s.d.c.q.ServiceQueryConnector : [user: test] [screen: home] [getScreenRestrictions] =>  0 records. Prepare service time: 0.0s - Service time: 0.01s - Datalist time: 0.0s - Total time: 0.01s
2022-03-17 10:57:32.597  INFO [awe-boot,18bb85bd7bf4ef42,18bb85bd7bf4ef42] 26656 --- [nio-8080-exec-3] c.a.awe.service.ScreenService  : [user: test] [screen: home] Screen parameters retrieved - keepCriteria-information - {}
2022-03-17 10:57:32.609  INFO [awe-boot,18bb85bd7bf4ef42,18bb85bd7bf4ef42] 26656 --- [   AweThread-10] .a.a.s.d.c.q.SQLQueryConnector : [user: test] [screen: home] [getScreenConfiguration] [select ScrCnf.IdeAweScrCnf as id, ope.l1_nom as "user", pro.Acr as profile, ScrCnf.Nam as component, ScrCnf.Scr as screen, ScrCnf.Atr as attribute, ScrCnf.Val as "value" from AweScrCnf ScrCnf left join AwePro as pro on ScrCnf.IdePro = pro.IdePro left join ope as ope on ScrCnf.IdeOpe = ope.IdeOpe where (ScrCnf.Scr = 'info' or ScrCnf.Scr is null) and (lower(trim(both from ope.l1_nom)) = lower('test') or ScrCnf.IdeOpe is null) and (trim(both from pro.Acr) = 'ADM' or ScrCnf.IdePro is null) and ScrCnf.Act = 1] => 0 records. Create query time: 0.004s - Sql time: 0.002s - Datalist time: 0.001s - Total time: 0.007s
2022-03-17 10:57:32.803  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [   AweThread-11] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [printTabs] => 2 records. Create enumerated time: 0.0s - Enumerated time: 0.001s - Datalist time: 0.001s - Total time: 0.002s
2022-03-17 10:57:32.804  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [   AweThread-12] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [printTabs] => 2 records. Create enumerated time: 0.003s - Enumerated time: 0.0s - Datalist time: 0.001s - Total time: 0.004s
2022-03-17 10:57:32.815  INFO [awe-boot,22f6ab10d2e34892,22f6ab10d2e34892] 26656 --- [    AweThread-2] a.a.s.d.c.q.EnumQueryConnector : [user: test] [screen: information] [EnuQueTabSel] => 2 records. Create enumerated time: 0.0s - Enumerated time: 0.0s - Datalist time: 0.0s - Total time: 0.0s
```
The following items are output:
- Date and Time: Millisecond precision and easily sortable.
- Log Level: `ERROR`, `WARN`, `INFO`, `DEBUG` or `TRACE`.
- Breadcrumbs. Used to observability and trace service request.
- Process ID.
- A `---` separator o distinguish the start of actual log messages.
- Thread name: Enclosed in square brackets (maybe truncated for console output).
- Logger name: This is usually the source class name (often abbreviated).
- Log message fields [user][screen][database].
- The log message.

You can customize the format using this property:

| Spring Environment        | System Property       | Comments                                        |
|---------------------------|-----------------------|-------------------------------------------------|
| `logging.pattern.console` | `CONSOLE_LOG_PATTERN` | The log pattern to use on the console (stdout). |
| `logging.pattern.file`    | `FILE_LOG_PATTERN`    | The log pattern to use in a file.               |

## Log File Output
By default, if you use AWE logback configuration includes, the logs are writen in log files. The default output destination has as value `${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}}}` and `LOG_PATH` is populated by logging spring boot property `logging.file.path`.

| Spring Environment  | System Property | Comments                                                                                  |
|---------------------|-----------------|-------------------------------------------------------------------------------------------|
| `logging.file.path` | `LOG_PATH`       | If defined, it is used in the default log configuration to configure destination log path |

## Log File Rotation
By default, the log files are rotated when the file is 10 Mb, but you can tune the rotation policy using the next properties:

| Name                                                   | Description                                                            | Default value |
|--------------------------------------------------------|------------------------------------------------------------------------|---------------|
| `logging.logback.rollingpolicy.file-name-pattern`      | The filename pattern used to create log archives.                      |               |
| `logging.logback.rollingpolicy.clean-history-on-start` | If log archive cleanup should occur when the application starts.       | `false`       |
| `logging.logback.rollingpolicy.max-file-size`          | The maximum size of log file before it is archived.                    | `10MB`        |
| `logging.logback.rollingpolicy.total-size-cap`         | The maximum amount of size log archives can take before being deleted. | `0`           |
| `logging.logback.rollingpolicy.max-history`            | The maximum number of archive log files to keep.                       | `7`           | 
