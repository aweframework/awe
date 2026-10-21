---
id: observability
title: Observabilidad
sidebar_label: Observabilidad
---

El **awe-observability-spring-boot-starter** da a una aplicación AWE comprobaciones de salud, métricas, métricas de uso y trazas con una sola dependencia y sin más configuración, además de un dashboard de Grafana para importar. Es opcional: una aplicación que no añade la dependencia no cambia en absoluto.

| Qué obtienes | Cómo |
| ------------ | ---- |
| Comprobaciones de salud de los conectores de AWE | `aweDatabases` y `aweQueue` en `/actuator/health` |
| Métricas por consulta, mantenimiento y acción | Temporizadores `awe.query`, `awe.maintain` y `awe.action` en `/actuator/metrics` y `/actuator/prometheus` |
| Métricas de uso | Pantallas abiertas, exportaciones, ficheros y usuarios conectados: `awe.screen`, `awe.export`, `awe.files`, `awe.users.connected` y `awe.connections.active` |
| Trazas | Un span por consulta, mantenimiento, acción, pantalla y exportación, como hijo de la traza actual (Micrometer Tracing) |
| Un dashboard de Grafana | `awe-observability-dashboard.json` (operación y uso), incluido en el starter (consulta [Dashboard de Grafana](#grafana-dashboard)) |

## Añadir el starter {#add-the-starter}

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-observability-spring-boot-starter</artifactId>
</dependency>
```

La versión viene del BOM de AWE (`awe-dependencies`), igual que en los demás starters. El starter trae Spring Boot Actuator, el registro de Prometheus y el puente de Micrometer Tracing para OpenTelemetry, y publica estos valores por defecto (cualquier valor que establezcas tú prevalece sobre ellos):

| Propiedad | Valor por defecto | Motivo |
| --------- | ----------------- | ------ |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | Los endpoints que necesita una plataforma de monitorización |
| `management.metrics.tags.application` | `${spring.application.name}` (`awe-application` si no hay ninguno) | El dashboard filtra por aplicación |

:::caution Protege los endpoints
Los endpoints de actuator son endpoints web, así que la seguridad de AWE se aplica a ellos como a cualquier otra ruta. Prometheus tiene que poder llegar a `/actuator/prometheus`: permítelo en tu configuración de seguridad (consulta `SpecificSecurityConfig` en la aplicación de pruebas `awe-boot`, que permite el acceso anónimo a `/actuator/**` para el desarrollo local) o dale credenciales a Prometheus. No los expongas sin protección fuera de una red de confianza.
:::

## Comprobaciones de salud {#health-checks}

`/actuator/health` lista un componente por cada conector que usa AWE:

| Componente | Comprueba | Cuándo se registra |
| ---------- | --------- | ------------------ |
| `db` | El datasource por defecto | Siempre (Spring Boot) |
| `aweDatabases` | Cada alias de base de datos al que se conecta AWE, con un detalle por alias; `DOWN` si alguno lo está | `awe.database.multidatabase-enable=true` |
| `aweQueue` | Que se puede abrir una conexión JMS; informa del proveedor | `awe.application.jms.enabled=true` y un `ConnectionFactory` de JMS |
| `mail`, `ldap`, `redis` | Servidor de correo, LDAP y Redis | Los indicadores de Spring Boot, cuando la aplicación los usa |

`aweDatabases` está en `UNKNOWN` hasta que se han cargado los alias de base de datos. Cada comprobación espera como máximo `awe.observability.health-timeout` (por defecto `2s`) a que se valide una conexión. Para desactivar una usa la propiedad estándar, por ejemplo `management.health.awe-queue.enabled=false`.

Por defecto Spring Boot solo muestra el estado global. Establece `management.endpoint.health.show-details=when-authorized` (o `always` en un entorno de confianza) para ver los componentes.

## Métricas {#metrics}

Cada ejecución es un temporizador de Micrometer. Su contador es el número de ejecuciones, así que no hay un contador aparte:

| Métrica | Qué mide |
| ------- | -------- |
| `awe.query` | Ejecuciones de consultas, por el conector que las ejecuta. Una consulta servida desde la caché no es una ejecución y no se cuenta |
| `awe.maintain` | Ejecuciones de mantenimientos |
| `awe.action` | Ejecuciones de acciones |

Cada temporizador tiene estas etiquetas:

| Etiqueta | Valores |
| -------- | ------- |
| `id` | El identificador de la consulta, el mantenimiento o la acción, tal como se define en el XML de la aplicación |
| `outcome` | `error` cuando la ejecución lanzó un fallo o terminó con una respuesta de tipo error (una consulta o mantenimiento cuyos datos son un error, una acción cuyo servicio respondió un error, de modo que se ejecutó su respuesta de error); `success` en caso contrario, incluidos los avisos e informaciones que una aplicación lanza como mensajes |
| `error` | El nombre de la clase de la excepción cuando se lanzó un fallo; `none` en caso contrario, también para una respuesta de error que no lanzó nada |
| `connector` | Solo en consultas: `sql`, `service`, `enum` o `queue` |
| `application` | El nombre de la aplicación |

En Prometheus el temporizador `awe.query` se expone como `awe_query_seconds_count`, `awe_query_seconds_sum` y `awe_query_seconds_max`, y las ejecuciones en curso como `awe_query_active_seconds_count`; lo mismo para `awe_maintain` y `awe_action`. Por ejemplo, la duración media de cada consulta en cinco minutos:

```text
sum by (id) (rate(awe_query_seconds_sum[5m])) / sum by (id) (rate(awe_query_seconds_count[5m]))
```

### Cardinalidad {#cardinality}

La etiqueta `id` está acotada por las definiciones XML de la aplicación. Los usuarios, las sesiones y los valores de los parámetros nunca son etiquetas.

Como red de seguridad, el starter limita las series de cada métrica cuyo nombre empieza por `awe.`: cada una de sus etiquetas `id` y `screen` admite como máximo `awe.observability.max-tag-values` valores distintos (por defecto `1000`). Cuando una métrica alcanza el límite, cada valor nuevo se informa como `other`, mientras que los valores admitidos antes conservan sus propias series, así que una etiqueta muestra como mucho el límite más `other`. La cuenta se mantiene en memoria por instancia desde que arrancó la aplicación. El temporizador `<nombre>.active` de las ejecuciones en curso comparte los valores de su temporizador, así que ambos muestran los mismos ids.

### Percentiles {#percentiles}

Los histogramas no están activados, porque multiplican el número de series por el número de ids. Para obtener percentiles de las consultas, actívalos en el proyecto:

```properties
management.metrics.distribution.percentiles-histogram.awe.query=true
```

### Eventos de la auditoría de seguridad {#security-audit-events}

El starter también cuenta los eventos de la [auditoría de seguridad](../security/security-audit.md) en el contador `awe.security.audit.events`, con las etiquetas `type` (`LOGIN`, `LOGOUT`, `ACCOUNT_LOCKED`...), `outcome` (`SUCCESS` o `FAILURE`), `channel` (`WEB`, `SSO`, `REST` o `SYSTEM`) y `cause` (`BAD_CREDENTIALS`, `EXPIRED`... o `none`). El usuario y la dirección del cliente nunca son etiquetas. En Prometheus es `awe_security_audit_events_total`; por ejemplo, los inicios de sesión fallidos por causa en cinco minutos:

```text
sum by (cause) (rate(awe_security_audit_events_total{type="LOGIN",outcome="FAILURE"}[5m]))
```

Establezca `awe.security.audit.metrics.enabled=false` para dejar de contarlos; `awe.observability.enabled=false` y `awe.security.audit.enabled=false` también lo detienen.

## Métricas de uso {#usage-metrics}

Las métricas de uso responden a qué pantallas abren los usuarios, qué exportan y cuántos están conectados. Están activadas por defecto, y cada familia se puede desactivar con su propia propiedad (consulta [Configuración](#configuration)).

| Métrica | Tipo | Qué mide | Etiquetas |
| ------- | ---- | -------- | --------- |
| `awe.screen` | Temporizador | Pantallas abiertas: su contador es el número de pantallas construidas y su tiempo el que tarda el servidor en construirlas (no el pintado en el navegador) | `id`, `screen`, `outcome`, `error` |
| `awe.export` | Temporizador | Impresiones y exportaciones de las pantallas, una por fichero generado: una impresión en PDF y XLSX cuenta dos | `id`, `format`, `outcome`, `error` |
| `awe.files` | Resumen de distribución (bytes) | Ficheros subidos y descargados: su contador es el número de ficheros y su total los bytes | `direction` |
| `awe.users.connected` | Gauge | Usuarios autenticados con al menos una sesión WebSocket activa en esta instancia | ninguna |
| `awe.connections.active` | Gauge | Sesiones WebSocket activas de los usuarios autenticados en esta instancia, una por pestaña del navegador | ninguna |

Las etiquetas de las métricas de uso:

| Etiqueta | Valores |
| -------- | ------- |
| `id` | En `awe.screen`, la opción de menú abierta, o el id de la pantalla para la pantalla por defecto (la que se muestra sin opción); en `awe.export`, la pantalla exportada |
| `screen` | La pantalla construida para la opción |
| `format` | `pdf`, `xlsx`, `csv`, `docx`, `text` u `other` |
| `direction` | `upload` o `download` |
| `outcome`, `error`, `application` | Igual que en las [métricas de ejecución](#metrics) |

Solo se miden las opciones que existen en el menú: AWE observa la construcción una vez encontrada la opción en el menú, así que una opción desconocida o falsificada nunca se convierte en etiqueta. Los usuarios nunca son etiquetas; los gauges se leen en cada lectura de Prometheus del `SimpUserRegistry` de Spring, que contiene las sesiones STOMP activas del broker WebSocket y elimina una sesión en cuanto se cierra su WebSocket (al cerrar una pestaña o recargar). No se cuentan la página de inicio de sesión ni las sesiones anónimas (usuario `PUBLIC` o `anonymousUser`). Son por instancia: con varias instancias, sumarlas cuenta dos veces a un usuario conectado a dos de ellas.

Los inicios de sesión, los cierres de sesión y las sesiones caducadas no son una métrica nueva: son los [eventos de la auditoría de seguridad](#security-audit-events) (`type="LOGIN"`, `type="LOGOUT"` y `cause="EXPIRED"` para las sesiones que caducaron).

En Prometheus se exponen como `awe_screen_seconds_count`, `awe_screen_seconds_sum` y `awe_screen_seconds_max` (y `awe_screen_active_seconds_count` para las pantallas que se están construyendo), lo mismo para `awe_export`, `awe_files_bytes_count`, `awe_files_bytes_sum` y `awe_files_bytes_max`, y `awe_users_connected` y `awe_connections_active`.

### Consultas de ejemplo {#example-queries}

Las diez opciones más abiertas en el último día:

```text
topk(10, sum by (id) (increase(awe_screen_seconds_count[1d])))
```

Las diez pantallas más lentas de construir, por tiempo medio en cinco minutos:

```text
topk(10, sum by (id) (rate(awe_screen_seconds_sum[5m])) / sum by (id) (rate(awe_screen_seconds_count[5m])))
```

Los usuarios conectados ahora, sumando las instancias:

```text
sum(awe_users_connected)
```

Las exportaciones por formato en el último día:

```text
sum by (format) (increase(awe_export_seconds_count[1d]))
```

Los bytes descargados en el último día:

```text
sum(increase(awe_files_bytes_sum{direction="download"}[1d]))
```

Los diez mantenimientos más usados en el último día (de las [métricas de ejecución](#metrics)):

```text
topk(10, sum by (id) (increase(awe_maintain_seconds_count[1d])))
```

Las sesiones que caducaron en el último día:

```text
sum(increase(awe_security_audit_events_total{type="LOGOUT",cause="EXPIRED"}[1d]))
```

### Coste {#cost}

Cada familia se registra en memoria y Prometheus la lee cuando consulta `/actuator/prometheus`: una petición no hace ninguna E/S, acceso a base de datos ni llamada de red adicional para medirse. El número de series de cada familia está acotado, y `awe.observability.max-tag-values` limita de todos modos las etiquetas `id` y `screen`:

| Familia | Series | Trabajo por petición |
| ------- | ------ | -------------------- |
| `awe.screen` | Opciones y pantallas por defecto abiertas × `outcome` (× clase de `error` en los fallos), cada una con su contador, suma y máximo, más las pantallas en construcción | Una actualización del temporizador por pantalla construida |
| `awe.export` | Pantallas exportadas × formato (seis como máximo) × `outcome` | Una actualización del temporizador por fichero generado |
| `awe.files` | Dos, `upload` y `download`, cada una con su contador, suma y máximo | Una actualización del resumen por fichero |
| `awe.users.connected`, `awe.connections.active` | Una de cada por instancia | Ninguno: se calculan en cada lectura a partir de las sesiones WebSocket que Spring ya registra |

Los spans de las pantallas y las exportaciones siguen el muestreo de las [trazas](#traces), como los demás spans.

## Trazas {#traces}

Cada consulta, mantenimiento, acción, construcción de pantalla y exportación es un span llamado `awe.query`, `awe.maintain`, `awe.action`, `awe.screen` o `awe.export`, con las mismas etiquetas `id`, `connector` y `outcome` (el id es una etiqueta y no forma parte del nombre, porque Micrometer pasa a minúsculas los nombres de los spans). El span es hijo del span que está activo cuando empieza la ejecución, por ejemplo el que Spring Boot abre para la petición HTTP, de modo que la traza se mantiene desde la petición hasta las consultas que lanza. Una ejecución fallida marca su span con el error.

Spring Boot pone `traceId` y `spanId` en el MDC del log mientras hay un span activo. Imprímelos con `%X{traceId}` y `%X{spanId}` en el patrón de log, o usa el codificador JSON (la aplicación de pruebas `awe-boot` lo hace), para saltar de una línea de log a su traza.

El starter crea los spans, pero no decide adónde enviarlos. Por defecto Spring Boot muestrea el 10 % de las peticiones (`management.tracing.sampling.probability=0.1`). Para exportarlos añade un exportador, por ejemplo OTLP, y apúntalo a tu colector o a Tempo:

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```properties
management.opentelemetry.tracing.export.otlp.endpoint=http://tempo:4318/v1/traces
management.tracing.sampling.probability=1.0
```

## Dashboard de Grafana {#grafana-dashboard}

El starter incluye un dashboard, **AWE Observability** (`awe-observability-dashboard.json`). Muestra cómo se comporta el servidor (ejecuciones, duraciones y errores de las consultas, mantenimientos y acciones) y cómo se usa la aplicación (pantallas abiertas, usuarios conectados, inicios de sesión, exportaciones y ficheros).

### Usar el dashboard en tu entorno {#use-the-dashboard-in-your-environment}

Necesitas un Prometheus que lea las métricas de la aplicación y un Grafana que tenga ese Prometheus como fuente de datos.

1. **Añade el starter** a la aplicación, como en [Añadir el starter](#add-the-starter).
2. **Haz que `/actuator/prometheus` esté disponible.** El starter trae el registro de Prometheus y expone el endpoint. Comprueba que tu configuración no lo deshace:
   - `management.prometheus.metrics.export.enabled` no debe ser `false`. Spring Boot lo activa por defecto, pero una aplicación puede desactivarlo (la aplicación de pruebas `awe-boot` lo hace).
   - Si estableces tú `management.endpoints.web.exposure.include`, mantén `prometheus` en la lista.
   - Prometheus tiene que pasar la seguridad de AWE: permite la ruta o dale credenciales a Prometheus, como se explica en [Añadir el starter](#add-the-starter) (_Protege los endpoints_).

   Compruébalo con `curl http://<host>:<puerto>/actuator/prometheus | grep awe_`: en cuanto la aplicación ha abierto una pantalla o ejecutado una consulta, aparecen las métricas `awe_`.
3. **Haz que Prometheus la lea.** Un `prometheus.yml` mínimo:

   ```yaml
   scrape_configs:
     - job_name: awe
       metrics_path: /actuator/prometheus
       scrape_interval: 15s
       static_configs:
         - targets: ['my-awe-host:8080']
   ```

   Si la aplicación tiene un context path (`server.servlet.context-path`), añádelo delante de `metrics_path`. No añadas a la target una etiqueta llamada `application`: el dashboard filtra por la etiqueta `application` que publica la aplicación, y Prometheus la renombraría a `exported_application`.
4. **Obtén el JSON del dashboard.** Está dentro del jar del starter, en `awe-observability/grafana/awe-observability-dashboard.json`, y en el árbol de fuentes, en `awe-framework/awe-starters/awe-observability-spring-boot-starter/src/main/resources/awe-observability/grafana/`. Para extraerlo del jar (por ejemplo de tu repositorio local de Maven, `~/.m2/repository/com/almis/awe/awe-observability-spring-boot-starter/<versión>/`):

   ```bash
   unzip -p awe-observability-spring-boot-starter-<versión>.jar awe-observability/grafana/awe-observability-dashboard.json > awe-observability-dashboard.json
   ```

5. **Impórtalo en Grafana.** Abre _Dashboards > Import > Upload JSON file_, elige el fichero y pulsa _Import_. Después elige tu fuente de datos de Prometheus en el selector _Data source_ de la parte superior del dashboard. Su uid es `awe-observability`, así que importar una versión más reciente sustituye a la anterior (Grafana pregunta antes de sobrescribirla).
6. **Si los paneles están vacíos**, comprueba:
   - El selector _Application_: lista los valores de la etiqueta `application`, que es `spring.application.name` (`awe-application` si no hay ninguno). Si está vacío, Prometheus aún no tiene métricas de AWE.
   - Que la target en Prometheus (_Status > Targets_) está `UP`. Un `401`, un `403` o una redirección a la página de login indican que la seguridad de AWE bloquea a Prometheus; un `404` indica un `metrics_path` incorrecto o un endpoint que no está expuesto.
   - Las familias desactivadas en la [configuración](#configuration) dejan sus paneles vacíos, igual que lo que no ha ocurrido en el intervalo de tiempo elegido (por ejemplo los paneles de ficheros hasta que se sube o se descarga un fichero).

### Variables y filas {#variables-and-rows}

| Variable | Filtra |
| -------- | ------ |
| `datasource` | La fuente de datos de Prometheus |
| `application` | La aplicación, por su etiqueta `application` |
| `id` | Los ids de consultas, mantenimientos y acciones de las filas Queries, Maintains y Actions (todos por defecto) |
| `option` | Las opciones de menú de la fila Screens y de las pantallas abiertas (todas por defecto) |

| Fila | Paneles |
| ---- | ------- |
| Overview | Consultas, mantenimientos y acciones por segundo y el porcentaje de errores |
| Usage | Usuarios conectados y conexiones abiertas ahora; pantallas abiertas, inicios de sesión, inicios de sesión fallidos y exportaciones en el intervalo de tiempo elegido |
| Screens | Las opciones más abiertas, las pantallas abiertas por minuto, las pantallas con errores, y el tiempo medio y el más largo en construirlas |
| Users and sessions | Usuarios conectados y conexiones abiertas a lo largo del tiempo, inicios de sesión por canal y cierres de sesión por causa |
| Exports and files | Exportaciones por formato, las pantallas más exportadas, la duración de las exportaciones por formato, y los ficheros y bytes subidos y descargados |
| Queries | Ejecuciones por segundo según el resultado, la duración media y la más larga de los diez ids más lentos, los ids de consulta más ejecutados, los ids con errores y las ejecuciones en curso |
| Maintains | Lo mismo para los mantenimientos |
| Actions | Lo mismo para las acciones |

Los ids de consulta más ejecutados cuentan todas las ejecuciones de consultas, las lance quien las lance: la target action de las acciones data, las cargas iniciales, los mantenimientos y servicios que ejecutan consultas y las consultas del propio framework. Una consulta servida desde la caché no se cuenta.

Los recuentos del intervalo de tiempo elegido son números enteros e incluyen los ids que aparecen por primera vez en el intervalo; si la aplicación se reinició dentro del intervalo, el recuento de las series que reinició es la estimación de Prometheus (`increase`).

### Entorno local {#local-setup}

La aplicación de pruebas `awe-boot` tiene un entorno local listo con Prometheus, Grafana, Tempo y Loki en `awe-tests/awe-boot/observability/docker-compose.yaml`. Monta el dashboard en Grafana, así que tras `docker compose up` aparece junto a los demás. Arranca la aplicación con `management.prometheus.metrics.export.enabled=true` para que Prometheus pueda leerla.

## Configuración {#configuration}

| Propiedad | Valor por defecto | Descripción |
| --------- | ----------------- | ----------- |
| `awe.observability.enabled` | `true` | Desactiva comprobaciones de salud, métricas y trazas sin quitar la dependencia |
| `awe.observability.health-timeout` | `2s` | Tiempo máximo que una comprobación de base de datos o cola espera una conexión |
| `awe.observability.max-tag-values` | `1000` | Número máximo de valores distintos de las etiquetas `id` y `screen` de cada métrica de AWE; los valores que lo superan se informan como `other` |
| `awe.observability.usage.screens.enabled` | `true` | Cuenta y mide las pantallas construidas para una opción de menú (`awe.screen`) |
| `awe.observability.usage.exports.enabled` | `true` | Cuenta y mide las impresiones y exportaciones de las pantallas (`awe.export`) |
| `awe.observability.usage.files.enabled` | `true` | Cuenta los ficheros subidos y descargados y suma sus bytes (`awe.files`) |
| `awe.observability.usage.users.enabled` | `true` | Publica los usuarios conectados y las conexiones abiertas en este momento, por instancia (`awe.users.connected` y `awe.connections.active`) |
| `management.health.awe-databases.enabled` | `true` | La comprobación de salud `aweDatabases` |
| `management.health.awe-queue.enabled` | `true` | La comprobación de salud `aweQueue` |

## Cómo funciona {#how-it-works}

AWE llama a un `ExecutionObserver` alrededor de los tres puntos en los que termina cualquier forma de lanzar una consulta, un mantenimiento o una acción (`QueryLauncher.launchQuery`, `MaintainService.launchMaintain` con un target y `ActionService.launchAction` con una acción). Sin un bean observador la ejecución se hace tal cual. El starter aporta uno que envuelve cada ejecución en una observación de Micrometer, que Spring Boot convierte en el temporizador y el span, de modo que métricas y trazas siempre coinciden. Un proyecto puede definir su propio bean `ExecutionObserver` para sustituirlo, por ejemplo para auditar las ejecuciones.

El mismo observador recibe dos tipos de ejecución más: `SCREEN`, desde `ScreenService.generateScreenData` una vez encontrada la opción en el menú, y `EXPORT`, desde `ReportGenerator.generateScreenReportContext` una vez por formato generado. Cuando la familia de un tipo está desactivada, el starter lo ejecuta sin observarlo.

Los ficheros pasan por un segundo punto de extensión, `FileTransferObserver`: `FileService` lo llama una vez por fichero subido o descargado, después de guardarlo o enviarlo, con la dirección y el tamaño en bytes. El starter aporta uno que registra `awe.files`; un proyecto puede definir su propio bean `FileTransferObserver` para sustituirlo.
