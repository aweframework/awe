---
id: observability
title: Observability
sidebar_label: Observability
---

The **awe-observability-spring-boot-starter** gives an AWE application health checks, metrics and traces with one dependency and no further configuration, plus a reference Grafana dashboard. It is opt-in: an application that does not add the dependency does not change at all.

| You get | How |
| ------- | --- |
| Health checks of the AWE connectors | `aweDatabases` and `aweQueue` in `/actuator/health` |
| Metrics per query, maintain and action | `awe.query`, `awe.maintain` and `awe.action` timers in `/actuator/metrics` and `/actuator/prometheus` |
| Traces | One span per query, maintain and action, as a child of the current trace (Micrometer Tracing) |
| A Grafana dashboard | `awe-observability-dashboard.json`, shipped in the starter |

## Add the starter

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-observability-spring-boot-starter</artifactId>
</dependency>
```

The version comes from the AWE BOM (`awe-dependencies`), as with the other starters. The starter brings Spring Boot Actuator, the Prometheus registry and the Micrometer Tracing bridge for OpenTelemetry, and publishes these defaults (any value you set wins over them):

| Property | Default | Reason |
| -------- | ------- | ------ |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | The endpoints a monitoring stack needs |
| `management.metrics.tags.application` | `${spring.application.name}` (`awe-application` if there is none) | The dashboard filters by application |

:::caution Secure the endpoints
The actuator endpoints are web endpoints, so AWE security applies to them like to any other path. Prometheus has to be able to reach `/actuator/prometheus`: allow it in your security configuration (see `SpecificSecurityConfig` in the `awe-boot` test application, which permits anonymous access to `/actuator/**` for local development) or give Prometheus credentials. Do not expose them without protection outside a trusted network.
:::

## Health checks

`/actuator/health` lists a component for each connector AWE uses:

| Component | Checks | When it is registered |
| --------- | ------ | --------------------- |
| `db` | The default datasource | Always (Spring Boot) |
| `aweDatabases` | Every database alias AWE connects to, one detail per alias; `DOWN` if any is | `awe.database.multidatabase-enable=true` |
| `aweQueue` | A JMS connection can be opened; reports the provider | `awe.application.jms.enabled=true` and a JMS `ConnectionFactory` |
| `mail`, `ldap`, `redis` | Mail server, LDAP and Redis | The Spring Boot indicators, when the application uses them |

`aweDatabases` is `UNKNOWN` until the database aliases have been loaded. Each check waits at most `awe.observability.health-timeout` (default `2s`) for a connection to be validated. To switch one off use the standard property, for example `management.health.awe-queue.enabled=false`.

By default Spring Boot shows only the overall status. Set `management.endpoint.health.show-details=when-authorized` (or `always` in a trusted environment) to see the components.

## Metrics

Every execution is a Micrometer timer. Its count is the number of executions, so there is no separate counter:

| Metric | What it measures |
| ------ | ---------------- |
| `awe.query` | Query executions, by the connector that runs them. A query served from the cache is not an execution and is not counted |
| `awe.maintain` | Maintain target executions |
| `awe.action` | Action executions |

Each timer has these tags:

| Tag | Values |
| --- | ------ |
| `id` | The identifier of the query, maintain target or action, as defined in the XML of the application |
| `outcome` | `error` when the execution threw a failure or ended with an answer of error type (a query or maintain whose data is an error, an action whose service answered an error, so its error answer ran); `success` otherwise, including the warnings and infos an application throws as messages |
| `error` | The exception class name when a failure was thrown; `none` otherwise, also for an error answer that threw nothing |
| `connector` | Queries only: `sql`, `service`, `enum` or `queue` |
| `application` | The application name |

In Prometheus the `awe.query` timer is exposed as `awe_query_seconds_count`, `awe_query_seconds_sum` and `awe_query_seconds_max`, and the executions running right now as `awe_query_active_seconds_count`; the same for `awe_maintain` and `awe_action`. For example, the mean duration of each query over five minutes:

```text
sum by (id) (rate(awe_query_seconds_sum[5m])) / sum by (id) (rate(awe_query_seconds_count[5m]))
```

### Cardinality

The `id` tag is bounded by the XML definitions of the application. Users, sessions and parameter values are never tags.

### Percentiles

Histograms are not enabled, because they multiply the number of series by the number of ids. To get percentiles for the queries, enable them in the project:

```properties
management.metrics.distribution.percentiles-histogram.awe.query=true
```

## Traces

Each query, maintain and action is a span named `awe.query`, `awe.maintain` or `awe.action` with the same `id`, `connector` and `outcome` as tags (the id is a tag and not part of the name, because Micrometer lower-cases span names). The span is a child of the span that is current when the execution starts, for example the one Spring Boot opens for the HTTP request, so the trace is kept from the request down to the queries it runs. A failed execution marks its span with the error.

Spring Boot puts `traceId` and `spanId` in the logging MDC while a span is current. Print them with `%X{traceId}` and `%X{spanId}` in your log pattern, or use the JSON encoder (the `awe-boot` test application does it), to jump from a log line to its trace.

The starter creates the spans, but it does not decide where to send them. By default Spring Boot samples 10% of the requests (`management.tracing.sampling.probability=0.1`). To export them add an exporter, for example OTLP, and point it to your collector or Tempo:

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

## Grafana dashboard

The starter ships a reference dashboard, `awe-observability/grafana/awe-observability-dashboard.json`, inside its jar and in the source tree at `awe-framework/awe-starters/awe-observability-spring-boot-starter/src/main/resources/awe-observability/grafana/`. Import it in Grafana (_Dashboards > Import > Upload JSON file_) and choose your Prometheus data source.

It has a data source selector and filters by application and by id, and shows, for queries, maintains and actions: executions per second by outcome, the mean and the longest duration of the ten slowest ids, the most executed ids, the ids that failed and the executions running now, plus the overall rate and error ratio.

The `awe-boot` test application has a ready-made local setup with Prometheus, Grafana, Tempo and Loki in `awe-tests/awe-boot/observability/docker-compose.yaml`. It mounts this dashboard in Grafana, so after `docker compose up` it is listed with the others. Run the application with `management.prometheus.metrics.export.enabled=true` so Prometheus can scrape it.

## Configuration

| Property | Default | Description |
| -------- | ------- | ----------- |
| `awe.observability.enabled` | `true` | Switches off health checks, metrics and traces without removing the dependency |
| `awe.observability.health-timeout` | `2s` | Maximum time a database or queue health check waits for a connection |
| `management.health.awe-databases.enabled` | `true` | The `aweDatabases` health check |
| `management.health.awe-queue.enabled` | `true` | The `aweQueue` health check |

## How it works

AWE calls an `ExecutionObserver` around the three points where every way of launching a query, a maintain or an action ends up (`QueryLauncher.launchQuery`, `MaintainService.launchMaintain` with a target and `ActionService.launchAction` with an action). Without an observer bean the execution runs untouched. The starter provides one that wraps each execution in a Micrometer observation, which Spring Boot turns into the timer and the span, so metrics and traces always agree. A project can define its own `ExecutionObserver` bean to replace it, for example to audit executions.
