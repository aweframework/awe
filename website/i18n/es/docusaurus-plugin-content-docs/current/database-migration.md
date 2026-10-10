---
id: database-migration
title: Migración de base de datos
sidebar_label: Migración de base de datos
---

AWE utiliza [Flyway](https://flywaydb.org/) como herramienta por defecto para el control de versiones de los scripts de base de datos.

<img alt="flyway-logo" src={require('@docusaurus/useBaseUrl').default('img/flyway-logo.png')} />

Además de usarla internamente para gestionar el `AWE database model`, puede gestionar sus propios scripts o utilizar cualquier otra herramienta externa.

:::info ¿Busca las bases de datos soportadas?
Consulte el resumen visual con fragmentos para copiar y pegar en la página de [Bases de datos soportadas](supported-databases.md).
:::

# **Primeros pasos** {#get-started}

Para ejecutar automáticamente las migraciones de base de datos de Flyway al arrancar AWE, añada la propiedad `spring.flyway.enabled=true` en su configuración. Normalmente, las migraciones provienen de scripts con el formato `<MODULE>_V<VERSION>__<NAME>.sql` (siendo `<VERSION>` una versión separada por guiones bajos, como ‘1.1’ o ‘2_1’).

```properties
spring.flyway.enabled=true
```

Por defecto, se encuentran en la carpeta `classpath:db/migration/{vendor}`, pero puede modificar esa ubicación mediante `spring.flyway.locations`. Se trata de una lista separada por comas de una o más ubicaciones classpath: o filesystem:. Por ejemplo, la siguiente configuración buscaría scripts tanto en la ubicación por defecto del classpath como en el directorio `/opt/migration`:

```properties
spring.flyway.locations=classpath:db/migration/{vendor},filesystem:/opt/migration
```

En lugar de usar `db/migration`, la configuración anterior establece la carpeta a utilizar según el tipo de base de datos (como db/migration/mysql para MySQL). Consulte la lista de bases de datos soportadas y sus claves de proveedor en la página de [Bases de datos soportadas](supported-databases.md).

El framework AWE personaliza el *proceso de migración de Flyway* para gestionar varios módulos. Por defecto, el módulo inicial se llama *AWE*, pero puede añadir los módulos que necesite para ejecutar scripts. Por ejemplo, si necesita usar el `AWE scheduler module` y tiene esos scripts SQL en sus aplicaciones, debe configurarlo así:

```properties
# List of modules to migrate. 
awe.database.migration-modules=AWE,SCHEDULER,APP
```

> **Nota:** recuerde nombrar sus scripts SQL con el mismo nombre que el módulo configurado. Ej.: `APP_V1.0__Init_schema.sql`