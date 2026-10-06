---
id: supported-databases
title: Supported databases
sidebar_label: Supported databases
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

This page lists the SQL database engines officially supported by AWE Framework and shows, at a glance, how to configure each one for JDBC and Flyway.

AWE uses Spring Boot + JDBC and Flyway. For each engine you need:
- A JDBC driver on the classpath.
- (When using Flyway) the corresponding Flyway database module on the classpath.
- A JDBC URL and credentials.
- Optionally, vendor-specific migration scripts under `classpath:db/migration/{vendor}` where `{vendor}` is resolved from the JDBC URL.

> Tip: AWE sets the Flyway locations to `classpath:db/migration/{vendor}` so you can keep one set of migrations per database vendor. Modules listed in `awe.database.migration-modules` are applied to the same datasource.

# Quick comparison

| Engine | Vendor key | JDBC driver | Flyway module | Example URL |
|---|---|---|---|---|
| 🧪 HSQLDB | `hsqldb` | `org.hsqldb:hsqldb` | `org.flywaydb:flyway-database-hsqldb` | `jdbc:hsqldb:mem:awetestdb` |
| ⚡ H2 | `h2` | `com.h2database:h2` | bundled in Flyway core (no extra module in most versions) | `jdbc:h2:mem:awetestdb;MODE=MySQL` |
| 🐬 MySQL/MariaDB | `mysql` | `com.mysql:mysql-connector-j` | `org.flywaydb:flyway-mysql` | `jdbc:mysql://localhost/awetestdb` |
| 🐘 PostgreSQL | `postgresql` | `org.postgresql:postgresql` | `org.flywaydb:flyway-database-postgresql` | `jdbc:postgresql://localhost/awetestdb` |
| 🪟 SQL Server | `sqlserver` | `com.microsoft.sqlserver:mssql-jdbc` | `org.flywaydb:flyway-sqlserver` | `jdbc:sqlserver://localhost:1433;databaseName=awetestdb` |
| 🏛️ Oracle | `oracle` | `com.oracle.database.jdbc:ojdbc11` | `org.flywaydb:flyway-database-oracle` (Teams in some versions) | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |

# Copy‑paste snippets

Use the tabs to pick your database and copy the minimal Maven and application.properties examples.

<Tabs>
  <TabItem value="hsqldb" label="HSQLDB 🧪">

```xml title="pom.xml — add JDBC + Flyway module"
<dependency>
  <groupId>org.hsqldb</groupId>
  <artifactId>hsqldb</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-hsqldb</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:hsqldb:mem:awetestdb
spring.datasource.driver-class-name=org.hsqldb.jdbcDriver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
  <TabItem value="h2" label="H2 ⚡">

```xml title="pom.xml"
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:h2:mem:awetestdb;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
  <TabItem value="mysql" label="MySQL 🐬">

```xml title="pom.xml"
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:mysql://localhost/awetestdb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
  <TabItem value="postgresql" label="PostgreSQL 🐘">

```xml title="pom.xml"
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:postgresql://localhost/awetestdb
spring.datasource.username=postgres
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
  <TabItem value="sqlserver" label="SQL Server 🪟">

```xml title="pom.xml"
<dependency>
  <groupId>com.microsoft.sqlserver</groupId>
  <artifactId>mssql-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-sqlserver</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=awetestdb
spring.datasource.username=sa
spring.datasource.password=Secret!123
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
  <TabItem value="oracle" label="Oracle 🏛️">

```xml title="pom.xml"
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc11</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-oracle</artifactId>
</dependency>
```

```properties title="application.properties"
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=system
spring.datasource.password=secret
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration/{vendor}
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```

  </TabItem>
</Tabs>

# Flyway script layout

Place vendor-specific scripts under:

```
classpath:db/migration/{vendor}
```

AWE’s own modules follow that convention. For example:
- AWE core: `awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/mysql` and `.../postgresql`.
- Scheduler: `awe-starters/awe-scheduler-spring-boot-starter/src/main/resources/db/migration/{vendor}`.
- Notifier: `awe-starters/awe-notifier-spring-boot-starter/src/main/resources/db/migration/{vendor}`.

Scripts per module are named with the module prefix and version, for example:

```
AWE_V1.0.0__Init_awe_schema.sql
SCHEDULER_V1.0.0__Init_scheduler.sql
NOTIFIER_V1.0.0__Init_notifier.sql
```

# Minimal global configuration

Enable Flyway and list the modules whose migrations you want to apply:

```properties
spring.flyway.enabled=true
# Default location already uses {vendor}
# spring.flyway.locations=classpath:db/migration/{vendor}

# AWE will migrate all listed modules on the configured datasource
# (Using the customized FlywayMigrationConfig)
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```