---
id: supported-databases
title: Bases de datos soportadas
sidebar_label: Bases de datos soportadas
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Esta página enumera los motores de bases de datos SQL soportados oficialmente por AWE Framework y muestra, de un vistazo, cómo configurar cada uno para JDBC y Flyway.

AWE utiliza Spring Boot + JDBC y Flyway. Para cada motor necesita:
- Un driver JDBC en el classpath.
- (Si usa Flyway) el módulo de base de datos de Flyway correspondiente en el classpath.
- Una URL JDBC y credenciales.
- Opcionalmente, scripts de migración específicos del proveedor en `classpath:db/migration/{vendor}`, donde `{vendor}` se resuelve a partir de la URL JDBC.

> Consejo: AWE establece las ubicaciones de Flyway en `classpath:db/migration/{vendor}` para que pueda mantener un conjunto de migraciones por proveedor de base de datos. Los módulos indicados en `awe.database.migration-modules` se aplican al mismo datasource.

# Comparación rápida {#quick-comparison}

| Motor | Clave de proveedor | Driver JDBC | Módulo de Flyway | URL de ejemplo |
|---|---|---|---|---|
| 🧪 HSQLDB | `hsqldb` | `org.hsqldb:hsqldb` | `org.flywaydb:flyway-database-hsqldb` | `jdbc:hsqldb:mem:awetestdb` |
| ⚡ H2 | `h2` | `com.h2database:h2` | incluido en el núcleo de Flyway (sin módulo adicional en la mayoría de versiones) | `jdbc:h2:mem:awetestdb;MODE=MySQL` |
| 🐬 MySQL/MariaDB | `mysql` | `com.mysql:mysql-connector-j` | `org.flywaydb:flyway-mysql` | `jdbc:mysql://localhost/awetestdb` |
| 🐘 PostgreSQL | `postgresql` | `org.postgresql:postgresql` | `org.flywaydb:flyway-database-postgresql` | `jdbc:postgresql://localhost/awetestdb` |
| 🪟 SQL Server | `sqlserver` | `com.microsoft.sqlserver:mssql-jdbc` | `org.flywaydb:flyway-sqlserver` | `jdbc:sqlserver://localhost:1433;databaseName=awetestdb` |
| 🏛️ Oracle | `oracle` | `com.oracle.database.jdbc:ojdbc11` | `org.flywaydb:flyway-database-oracle` (Teams en algunas versiones) | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |

# Fragmentos para copiar y pegar {#copypaste-snippets}

Utilice las pestañas para elegir su base de datos y copiar los ejemplos mínimos de Maven y application.properties.

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

# Disposición de los scripts de Flyway {#flyway-script-layout}

Coloque los scripts específicos de cada proveedor en:

```
classpath:db/migration/{vendor}
```

Los propios módulos de AWE siguen esa convención. Por ejemplo:
- Núcleo de AWE: `awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/mysql` and `.../postgresql`.
- Scheduler: `awe-starters/awe-scheduler-spring-boot-starter/src/main/resources/db/migration/{vendor}`.
- Notifier: `awe-starters/awe-notifier-spring-boot-starter/src/main/resources/db/migration/{vendor}`.

Los scripts de cada módulo se nombran con el prefijo del módulo y la versión, por ejemplo:

```
AWE_V1.0.0__Init_awe_schema.sql
SCHEDULER_V1.0.0__Init_scheduler.sql
NOTIFIER_V1.0.0__Init_notifier.sql
```

# Configuración global mínima {#minimal-global-configuration}

Habilite Flyway e indique los módulos cuyas migraciones quiere aplicar:

```properties
spring.flyway.enabled=true
# Default location already uses {vendor}
# spring.flyway.locations=classpath:db/migration/{vendor}

# AWE will migrate all listed modules on the configured datasource
# (Using the customized FlywayMigrationConfig)
awe.database.migration-modules=AWE,SCHEDULER,NOTIFIER
```