---
id: observability
title: Observability
sidebar_label: Observability
---

The **awe-observability-spring-boot-starter** gives an AWE application health checks, metrics, usage metrics and traces with one dependency and no further configuration, plus a Grafana dashboard to import. It is opt-in: an application that does not add the dependency does not change at all.

| You get | How |
| ------- | --- |
| Health checks of the AWE connectors | `aweDatabases` and `aweQueue` in `/actuator/health` |
| Metrics per query, maintain and action | `awe.query`, `awe.maintain` and `awe.action` timers in `/actuator/metrics` and `/actuator/prometheus` |
| Usage metrics | Screens opened, exports, files and users connected: `awe.screen`, `awe.export`, `awe.files`, `awe.users.connected` and `awe.connections.active` |
| Traces | One span per query, maintain, action, screen and export, as a child of the current trace (Micrometer Tracing) |
| A Grafana dashboard | `awe-observability-dashboard.json` (operations and usage), shipped in the starter (see [Grafana dashboard](#grafana-dashboard)) |

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

As a safety net, the starter caps the series of every meter named `awe.*`: each of its `id` and `screen` tags admits at most `awe.observability.max-tag-values` distinct values (default `1000`). Once a meter reaches the cap, every new value is reported as `other`, while the values admitted before keep their own series, so a tag shows at most the cap plus `other`. The count is kept in memory per instance since the application started. The `<name>.active` timer of the running executions shares the values of its timer, so both show the same ids.

### Percentiles

Histograms are not enabled, because they multiply the number of series by the number of ids. To get percentiles for the queries, enable them in the project:

```properties
management.metrics.distribution.percentiles-histogram.awe.query=true
```

### Security audit events

The starter also counts the [security audit](../security/security-audit.md) events on the `awe.security.audit.events` counter, tagged by `type` (`LOGIN`, `LOGOUT`, `ACCOUNT_LOCKED`...), `outcome` (`SUCCESS` or `FAILURE`), `channel` (`WEB`, `SSO`, `REST` or `SYSTEM`) and `cause` (`BAD_CREDENTIALS`, `EXPIRED`... or `none`). The user and the client address are never tags. In Prometheus it is `awe_security_audit_events_total`; for example, the failed logins per cause over five minutes:

```text
sum by (cause) (rate(awe_security_audit_events_total{type="LOGIN",outcome="FAILURE"}[5m]))
```

Set `awe.security.audit.metrics.enabled=false` to stop counting them; `awe.observability.enabled=false` and `awe.security.audit.enabled=false` stop it too.

## Usage metrics

The usage metrics answer which screens the users open, what they export and how many are connected. They are on by default, and each family can be switched off with its own property (see [Configuration](#configuration)).

| Metric | Type | What it measures | Tags |
| ------ | ---- | ---------------- | ---- |
| `awe.screen` | Timer | Screens opened: its count is the number of screens built and its time the time the server takes to build them (not the rendering in the browser) | `id`, `screen`, `outcome`, `error` |
| `awe.export` | Timer | Prints and exports of the screens, one per file generated: a print in PDF and XLSX counts two | `id`, `format`, `outcome`, `error` |
| `awe.files` | Distribution summary (bytes) | Files uploaded and downloaded: its count is the number of files and its total the bytes | `direction` |
| `awe.users.connected` | Gauge | Logged users with at least one live WebSocket session on this instance | none |
| `awe.connections.active` | Gauge | Live WebSocket sessions of the logged users on this instance, one per browser tab | none |

The tags of the usage metrics:

| Tag | Values |
| --- | ------ |
| `id` | In `awe.screen`, the menu option opened, or the screen id for the default screen (the one shown without an option); in `awe.export`, the screen exported |
| `screen` | The screen built for the option |
| `format` | `pdf`, `xlsx`, `csv`, `docx`, `text` or `other` |
| `direction` | `upload` or `download` |
| `outcome`, `error`, `application` | The same as in the [execution metrics](#metrics) |

Only the options that exist in the menu are measured: AWE observes the build once the option has been found in the menu, so an unknown or forged option never becomes a tag. The users are never tags; the gauges are read on every scrape from Spring's `SimpUserRegistry`, which holds the live STOMP sessions of the WebSocket broker and drops a session as soon as its WebSocket closes (a closed tab or a reload). The login page and anonymous sessions (user `PUBLIC` or `anonymousUser`) are not counted. They are per instance: with several instances, summing them counts twice a user connected to two of them.

Logins, logouts and expired sessions are not a new metric: they are the [security audit events](#security-audit-events) (`type="LOGIN"`, `type="LOGOUT"`, and `cause="EXPIRED"` for the sessions that timed out).

In Prometheus they are exposed as `awe_screen_seconds_count`, `awe_screen_seconds_sum` and `awe_screen_seconds_max` (and `awe_screen_active_seconds_count` for the screens being built now), the same for `awe_export`, `awe_files_bytes_count`, `awe_files_bytes_sum` and `awe_files_bytes_max`, and `awe_users_connected` and `awe_connections_active`.

### Example queries

The ten most opened options over the last day:

```text
topk(10, sum by (id) (increase(awe_screen_seconds_count[1d])))
```

The ten slowest screens to build, by mean time over five minutes:

```text
topk(10, sum by (id) (rate(awe_screen_seconds_sum[5m])) / sum by (id) (rate(awe_screen_seconds_count[5m])))
```

The users connected right now, adding up the instances:

```text
sum(awe_users_connected)
```

The exports per format over the last day:

```text
sum by (format) (increase(awe_export_seconds_count[1d]))
```

The bytes downloaded over the last day:

```text
sum(increase(awe_files_bytes_sum{direction="download"}[1d]))
```

The ten most used maintains over the last day (from the [execution metrics](#metrics)):

```text
topk(10, sum by (id) (increase(awe_maintain_seconds_count[1d])))
```

The sessions that expired over the last day:

```text
sum(increase(awe_security_audit_events_total{type="LOGOUT",cause="EXPIRED"}[1d]))
```

### Cost

Every family is recorded in memory and Prometheus reads it when it scrapes `/actuator/prometheus`: a request does no extra I/O, database access or network call to be measured. The number of series of each family is bounded, and `awe.observability.max-tag-values` caps the `id` and `screen` tags anyway:

| Family | Series | Work per request |
| ------ | ------ | ---------------- |
| `awe.screen` | Options and default screens opened × `outcome` (× `error` class for the failures), each with its count, sum and max, plus the screens being built | One timer update per screen built |
| `awe.export` | Screens exported × format (at most six) × `outcome` | One timer update per file generated |
| `awe.files` | Two, `upload` and `download`, each with its count, sum and max | One summary update per file |
| `awe.users.connected`, `awe.connections.active` | One each per instance | None: they are computed on every scrape from the WebSocket sessions Spring already registers |

The spans of the screens and exports follow the sampling of the [traces](#traces), like the other spans.

## Traces

Each query, maintain, action, screen build and export is a span named `awe.query`, `awe.maintain`, `awe.action`, `awe.screen` or `awe.export` with the same `id`, `connector` and `outcome` as tags (the id is a tag and not part of the name, because Micrometer lower-cases span names). The span is a child of the span that is current when the execution starts, for example the one Spring Boot opens for the HTTP request, so the trace is kept from the request down to the queries it runs. A failed execution marks its span with the error.

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

The starter ships one dashboard, **AWE Observability** (`awe-observability-dashboard.json`). It shows how the server behaves (executions, durations and errors of the queries, maintains and actions) and how the application is used (screens opened, users connected, logins, exports and files).

### Use the dashboard in your environment

You need a Prometheus that scrapes the application and a Grafana that has that Prometheus as a data source.

1. **Add the starter** to the application, as in [Add the starter](#add-the-starter).
2. **Make `/actuator/prometheus` available.** The starter brings the Prometheus registry and exposes the endpoint. Check that your configuration does not undo it:
   - `management.prometheus.metrics.export.enabled` must not be `false`. Spring Boot enables it by default, but an application can switch it off (the `awe-boot` test application does).
   - If you set `management.endpoints.web.exposure.include` yourself, keep `prometheus` in it.
   - Prometheus has to get through AWE security: allow the path or give Prometheus credentials, as explained in [Add the starter](#add-the-starter) (_Secure the endpoints_).

   Check it with `curl http://<host>:<port>/actuator/prometheus | grep awe_`: once the application has opened a screen or run a query, the `awe_` metrics are listed.
3. **Scrape it from Prometheus.** A minimal `prometheus.yml`:

   ```yaml
   scrape_configs:
     - job_name: awe
       metrics_path: /actuator/prometheus
       scrape_interval: 15s
       static_configs:
         - targets: ['my-awe-host:8080']
   ```

   If the application has a context path (`server.servlet.context-path`), prefix it to `metrics_path`. Do not add a target label named `application`: the dashboard filters by the `application` label that the application publishes, and Prometheus would rename it to `exported_application`.
4. **Get the dashboard JSON.** It is inside the starter jar, at `awe-observability/grafana/awe-observability-dashboard.json`, and in the source tree, in `awe-framework/awe-starters/awe-observability-spring-boot-starter/src/main/resources/awe-observability/grafana/`. To extract it from the jar (for example from your local Maven repository, `~/.m2/repository/com/almis/awe/awe-observability-spring-boot-starter/<version>/`):

   ```bash
   unzip -p awe-observability-spring-boot-starter-<version>.jar awe-observability/grafana/awe-observability-dashboard.json > awe-observability-dashboard.json
   ```

5. **Import it in Grafana.** Open _Dashboards > Import > Upload JSON file_, choose the file and click _Import_. Then pick your Prometheus data source in the _Data source_ selector at the top of the dashboard. Its uid is `awe-observability`, so importing a newer version replaces the previous one (Grafana asks before overwriting it).
6. **If the panels are empty**, check:
   - The _Application_ selector: it lists the values of the `application` label, which is `spring.application.name` (`awe-application` if there is none). If it is empty, Prometheus has no AWE metrics yet.
   - The target in Prometheus (_Status > Targets_) is `UP`. A `401`, a `403` or a redirection to the login page means that AWE security blocks Prometheus; a `404` means a wrong `metrics_path` or an endpoint that is not exposed.
   - The families switched off in the [configuration](#configuration) leave their panels empty, and so do the things that did not happen in the selected time range (for example the files panels until a file is uploaded or downloaded).

### Variables and rows

| Variable | Filters |
| -------- | ------- |
| `datasource` | The Prometheus data source |
| `application` | The application, by its `application` label |
| `id` | The query, maintain and action ids of the Queries, Maintains and Actions rows (all by default) |
| `option` | The menu options of the Screens row and of the screens opened (all by default) |

| Row | Panels |
| --- | ------ |
| Overview | Queries, maintains and actions per second and the error ratio |
| Usage | Users connected and open connections now; screens opened, logins, failed logins and exports in the selected time range |
| Screens | The most opened options, screens opened per minute, the screens with errors, and the mean and the longest time to build them |
| Users and sessions | Users connected and open connections over time, logins by channel and logouts by cause |
| Exports and files | Exports by format, the most exported screens, the export duration by format, and the files and bytes uploaded and downloaded |
| Queries | Executions per second by outcome, the mean and the longest duration of the ten slowest ids, the most executed query ids, the ids with errors and the executions running now |
| Maintains | The same for the maintain targets |
| Actions | The same for the actions |

The most executed query ids count every query execution, whatever launched it: the target action of the data actions, the initial loads, the maintains and services that run queries, and the queries of the framework itself. A query served from the cache is not counted.

The counts in the selected time range are whole numbers and include the ids that first appear in the range; if the application restarted inside the range, the count of the series it reset is the Prometheus estimate (`increase`).

### Local setup

The `awe-boot` test application has a ready-made local setup with Prometheus, Grafana, Tempo and Loki in `awe-tests/awe-boot/observability/docker-compose.yaml`. It mounts the dashboard in Grafana, so after `docker compose up` it is listed with the others. Run the application with `management.prometheus.metrics.export.enabled=true` so Prometheus can scrape it.

## Configuration

| Property | Default | Description |
| -------- | ------- | ----------- |
| `awe.observability.enabled` | `true` | Switches off health checks, metrics and traces without removing the dependency |
| `awe.observability.health-timeout` | `2s` | Maximum time a database or queue health check waits for a connection |
| `awe.observability.max-tag-values` | `1000` | Maximum number of distinct values of the `id` and `screen` tags of each AWE meter; values beyond it are reported as `other` |
| `awe.observability.usage.screens.enabled` | `true` | Count and time the screens built for a menu option (`awe.screen`) |
| `awe.observability.usage.exports.enabled` | `true` | Count and time the prints and exports of the screens (`awe.export`) |
| `awe.observability.usage.files.enabled` | `true` | Count the files uploaded and downloaded and sum their bytes (`awe.files`) |
| `awe.observability.usage.users.enabled` | `true` | Publish the users connected and the connections open right now, per instance (`awe.users.connected` and `awe.connections.active`) |
| `management.health.awe-databases.enabled` | `true` | The `aweDatabases` health check |
| `management.health.awe-queue.enabled` | `true` | The `aweQueue` health check |

## How it works

AWE calls an `ExecutionObserver` around the three points where every way of launching a query, a maintain or an action ends up (`QueryLauncher.launchQuery`, `MaintainService.launchMaintain` with a target and `ActionService.launchAction` with an action). Without an observer bean the execution runs untouched. The starter provides one that wraps each execution in a Micrometer observation, which Spring Boot turns into the timer and the span, so metrics and traces always agree. A project can define its own `ExecutionObserver` bean to replace it, for example to audit executions.

The same observer receives two more kinds of execution: `SCREEN`, from `ScreenService.generateScreenData` once the option has been found in the menu, and `EXPORT`, from `ReportGenerator.generateScreenReportContext` once per format generated. When the family of a kind is switched off, the starter runs it without observing it.

The files go through a second extension point, `FileTransferObserver`: `FileService` calls it once per file uploaded or downloaded, after the file has been stored or sent, with the direction and the size in bytes. The starter provides one that records `awe.files`; a project can define its own `FileTransferObserver` bean to replace it.
