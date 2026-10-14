---
id: observability
title: Observabilidad
sidebar_label: Observabilidad
---

El **awe-observability-spring-boot-starter** da a una aplicación AWE comprobaciones de salud, métricas y trazas con una sola dependencia y sin más configuración, además de un dashboard de Grafana de referencia. Es opcional: una aplicación que no añade la dependencia no cambia en absoluto.

| Qué obtienes | Cómo |
| ------------ | ---- |
| Comprobaciones de salud de los conectores de AWE | `aweDatabases` y `aweQueue` en `/actuator/health` |
| Métricas por consulta, mantenimiento y acción | Temporizadores `awe.query`, `awe.maintain` y `awe.action` en `/actuator/metrics` y `/actuator/prometheus` |
| Trazas | Un span por consulta, mantenimiento y acción, como hijo de la traza actual (Micrometer Tracing) |
| Un dashboard de Grafana | `awe-observability-dashboard.json`, incluido en el starter |

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

### Percentiles {#percentiles}

Los histogramas no están activados, porque multiplican el número de series por el número de ids. Para obtener percentiles de las consultas, actívalos en el proyecto:

```properties
management.metrics.distribution.percentiles-histogram.awe.query=true
```

## Trazas {#traces}

Cada consulta, mantenimiento y acción es un span llamado `awe.query`, `awe.maintain` o `awe.action`, con las mismas etiquetas `id`, `connector` y `outcome` (el id es una etiqueta y no forma parte del nombre, porque Micrometer pasa a minúsculas los nombres de los spans). El span es hijo del span que está activo cuando empieza la ejecución, por ejemplo el que Spring Boot abre para la petición HTTP, de modo que la traza se mantiene desde la petición hasta las consultas que lanza. Una ejecución fallida marca su span con el error.

Spring Boot pone `traceId` y `spanId` en el MDC del log mientras hay un span activo. Imprímelos con `%X{traceId}` y `%X{spanId}` en el patrón de log, o usa el codificador JSON (la aplicación de pruebas `awe-boot` lo hace), para saltar de una línea de log a su traza.

El starter crea los spans, pero no decide adónde enviarlos. Por defecto Spring Boot muestrea el 10 % de las peticiones (`management.tracing.sampling.probability=0.1`). Para exportarlos añade un exportador, por ejemplo OTLP, y apúntalo a tu colector o a Tempo:

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```properties
management.otlp.tracing.endpoint=http://tempo:4318/v1/traces
management.tracing.sampling.probability=1.0
```

## Dashboard de Grafana {#grafana-dashboard}

El starter incluye un dashboard de referencia, `awe-observability/grafana/awe-observability-dashboard.json`, dentro de su jar y en el árbol de fuentes en `awe-framework/awe-starters/awe-observability-spring-boot-starter/src/main/resources/awe-observability/grafana/`. Impórtalo en Grafana (_Dashboards > Import > Upload JSON file_) y elige tu fuente de datos de Prometheus.

Tiene un selector de fuente de datos y filtra por aplicación y por id, y muestra, para consultas, mantenimientos y acciones: ejecuciones por segundo según el resultado, la duración media y la más larga de los diez ids más lentos, los ids más ejecutados, los ids que han fallado y las ejecuciones en curso, además de la tasa global y el porcentaje de errores.

La aplicación de pruebas `awe-boot` tiene un entorno local listo con Prometheus, Grafana, Tempo y Loki en `awe-tests/awe-boot/observability/docker-compose.yaml`. Monta este dashboard en Grafana, así que tras `docker compose up` aparece junto a los demás. Arranca la aplicación con `management.prometheus.metrics.export.enabled=true` para que Prometheus pueda leerla.

## Configuración {#configuration}

| Propiedad | Valor por defecto | Descripción |
| --------- | ----------------- | ----------- |
| `awe.observability.enabled` | `true` | Desactiva comprobaciones de salud, métricas y trazas sin quitar la dependencia |
| `awe.observability.health-timeout` | `2s` | Tiempo máximo que una comprobación de base de datos o cola espera una conexión |
| `management.health.awe-databases.enabled` | `true` | La comprobación de salud `aweDatabases` |
| `management.health.awe-queue.enabled` | `true` | La comprobación de salud `aweQueue` |

## Cómo funciona {#how-it-works}

AWE llama a un `ExecutionObserver` alrededor de los tres puntos en los que termina cualquier forma de lanzar una consulta, un mantenimiento o una acción (`QueryLauncher.launchQuery`, `MaintainService.launchMaintain` con un target y `ActionService.launchAction` con una acción). Sin un bean observador la ejecución se hace tal cual. El starter aporta uno que envuelve cada ejecución en una observación de Micrometer, que Spring Boot convierte en el temporizador y el span, de modo que métricas y trazas siempre coinciden. Un proyecto puede definir su propio bean `ExecutionObserver` para sustituirlo, por ejemplo para auditar las ejecuciones.
