---
id: security-audit
title: Security audit
sidebar_label: Security audit
---

# Security audit

AWE records the security events of an application: logins, logouts, account locks, password and second factor
changes, SSO provisioning, denied accesses and REST tokens. Every event carries the user, tenant, client address,
user agent, a hash of the session and a correlation identifier, and is sent to one or more **sinks**: a database
table, a JSON log line for a SIEM and a Micrometer counter.

The audit is **on by default**. The only visible change for an existing application is the `AweSecAudEvt` table,
created by the Flyway migration `AWE_V1.3.0`, and the **Security audit** screen in the security menu.

| You want to | Read |
| --- | --- |
| Switch the whole feature off | [Turn it on and off](#turn-it-on-and-off) |
| Know what is recorded | [Event model](#event-model), [Event catalogue](#event-catalogue) |
| Send the events to a SIEM or keep them out of the database | [Sinks](#sinks) |
| Lock accounts after failed logins | [Automatic lockout](#automatic-lockout) |
| Comply with data protection rules | [Privacy](#privacy) |
| Browse the events | [Security audit screen](#security-audit-screen) |

## Turn it on and off

`awe.security.audit.enabled` is the global switch. With `false` no event is recorded by any sink and nothing is
purged; the audit service stays available as a no-op, so code that publishes events needs no checks.

```properties
# Switch the security audit off
awe.security.audit.enabled=false
```

Each sink has its own switch as well (see [Properties](#properties)). Products that already audit on their own can,
for example, keep the JSON log and drop the table:

```properties
awe.security.audit.database.enabled=false
```

## Event model

An event is a `com.almis.awe.model.security.audit.SecurityAuditEvent`. The framework fills in the request data when the
event is published; values already set on the event are kept.

| Field | Content |
| --- | --- |
| `id` | Unique identifier (UUID) |
| `timestamp` | Moment of the event |
| `type` | Event kind, see the [catalogue](#event-catalogue) |
| `outcome` | `SUCCESS` or `FAILURE` |
| `cause` | Reason, mainly for failures (may be empty) |
| `username` | User involved, as typed at the login or the authenticated user (up to 255 characters) |
| `tenant` | SSO tenant of the request or, without one, the database alias of the session (up to 100 characters) |
| `channel` | `WEB` (web application), `SSO` (identity provider), `REST` (requests under `/api/`) or `SYSTEM` (raised by the server outside any request, such as a session timeout) |
| `ipAddress` | Client address, [anonymized](#privacy) when configured |
| `userAgent` | `User-Agent` header of the client (up to 512 characters) |
| `sessionHash` | SHA-256 of the session identifier. The session identifier itself is never stored |
| `correlationId` | Identifier shared by the events of one request: the `X-Correlation-Id` header, else `X-Request-Id`, else the MDC `traceId` (tracing), else one generated per request. Header values that are not 1 to 64 characters of letters, digits, `.`, `_`, `:` or `-` are ignored |
| `detail` | Free detail: option, URL, profile, administrator, provider error code... (up to 1000 characters) |

## Event catalogue

The type and cause values are the enums `SecurityAuditEventType` and `SecurityAuditCause`. "Channel" is the
channel of the request unless a value is given.

| Type | Raised when | Outcome and cause | Channel |
| --- | --- | --- | --- |
| `LOGIN` | A local login (login form, `/action/login`, `POST /api/authenticate`) ends | `SUCCESS`; `FAILURE` with `BAD_CREDENTIALS` (wrong password or unknown user), `LOCKED`, `DISABLED`, `ACCOUNT_EXPIRED`, `CREDENTIALS_EXPIRED`, `LOCAL_LOGIN_DISABLED` or `ERROR` | `WEB`, `REST` |
| `LOGIN` | An SSO login ends | `SUCCESS`; `FAILURE` with `LOCKED`, `DISABLED`... (account refused by AWE), `ROLE_NOT_MAPPED`, `PROVIDER_ERROR` (detail: OAuth2 error code) or `ERROR` | `SSO` |
| `LOGOUT` | The user logs out | `SUCCESS`, `EXPLICIT` | Request channel, `SSO` for SSO sessions |
| `LOGOUT` | The identity provider ends the session (OIDC back-channel logout) | `SUCCESS`, `PROVIDER` | `SSO` |
| `LOGOUT` | A signed in session times out | `SUCCESS`, `EXPIRED` | `SYSTEM` |
| `ACCOUNT_LOCKED` | The [automatic lockout](#automatic-lockout) locks the account | `SUCCESS`, `MAX_ATTEMPTS` | `WEB`, `REST` |
| `ACCOUNT_LOCKED` | An administrator locks the user in the users screen | `SUCCESS`, `ADMINISTRATOR` (detail: `administrator: <user>`) | `WEB` |
| `ACCOUNT_UNLOCKED` | An administrator unlocks the user, or resets the password of a locked user, in the users screen | `SUCCESS`, `ADMINISTRATOR` | `WEB` |
| `PASSWORD_CHANGE` | The user changes the password in the change password screens | `SUCCESS`; `FAILURE`, `BAD_CREDENTIALS` (wrong current password) | `WEB` |
| `PASSWORD_CHANGE` | The user replaces an expired password from the login | `SUCCESS`, `CREDENTIALS_EXPIRED` | `WEB` |
| `PASSWORD_RESET` | An administrator sets a new password in the users screen | `SUCCESS`, `ADMINISTRATOR` | `WEB` |
| `TOTP_ENROLLMENT` | A second factor secret is generated and stored | `SUCCESS`; `FAILURE` with `FORBIDDEN` (a session waiting for its TOTP code tries to rotate the secret) or `ERROR` (the secret could not be stored) | `WEB` |
| `TOTP_VERIFICATION` | The user types the TOTP code after the login | `SUCCESS`; `FAILURE`, `INVALID_CODE` | `WEB` |
| `TOTP_RESET` | The user switches the second factor off, or an administrator resets it | `SUCCESS` (no cause, or `ADMINISTRATOR`) | `WEB` |
| `SSO_PROVISIONING` | A user is created from the identity provider data (`awe.security.sso.auto-provision-user`) | `SUCCESS` (detail: profile) | `SSO` |
| `SSO_PROFILE_SYNC` | The profile of a user is updated from the provider roles | `SUCCESS` (detail: `<old> -> <new>`) | `SSO` |
| `ACCESS_DENIED` | An option that exists in the menu is restricted for the user profile | `FAILURE`, `RESTRICTED` (detail: `option: <name>`) | `WEB` |
| `ACCESS_DENIED` | Spring Security refuses a URL to an authenticated user | `FAILURE`, `FORBIDDEN` (detail: `url: <path>`) | Request channel |
| `TOKEN_ISSUED` | `POST /api/authenticate` issues a JWT | `SUCCESS` | `REST` |
| `TOKEN_REJECTED` | A REST request carries a malformed, wrongly signed or expired JWT | `FAILURE`, `INVALID_TOKEN` or `TOKEN_EXPIRED` | `REST` |

## Sinks

The audit service sends every event to every `SecurityAuditSink` bean. Recording is best effort: a sink that fails
is logged and stops neither the other sinks nor the audited operation. Events published inside a transaction (for
example a lock made by a maintain) are sent **after the commit** and dropped if the transaction rolls back, so they
only describe changes that really happened.

### Database

`DatabaseSecurityAuditSink` stores the events in the append-only table `AweSecAudEvt`, created by the migration
`AWE_V1.3.0__Security_audit_events.sql` for every supported database. The script can be applied again: the table and
its indexes are only created when they do not exist. Each event is written on its own connection of
the application `DataSource` and committed at once, outside the transaction of the audited operation. Rows are only
deleted by the purge.

| Column | Field | Column | Field |
| --- | --- | --- | --- |
| `IdeSecAud` | `id` | `Chn` | `channel` |
| `EvtDat` | `timestamp` | `IpAdr` | `ipAddress` |
| `EvtTyp` | `type` | `UsrAgt` | `userAgent` |
| `EvtRes` | `outcome` | `SesHsh` | `sessionHash` |
| `EvtCau` | `cause` | `CorIde` | `correlationId` |
| `UsrNam` | `username` | `Det` | `detail` |
| `Ten` | `tenant` | `EvtDay` | day of `timestamp` |

`EvtDay` is the day of the event in the server time zone: the date filters and the daily chart of the screen compare
plain dates on it, so they behave the same on every database and use its index.

Indexes: `EvtDat`, `(UsrNam, EvtDat)`, `(EvtTyp, EvtDat)` and `EvtDay`.

**Retention.** The purge deletes the events older than `awe.security.audit.database.retention-days` (365 by default;
`0` keeps them forever) on the `awe.security.audit.database.purge-cron` schedule (every day at 03:00, server time
zone, by default). It runs on its own thread, so the application does not need `@EnableScheduling`. With several
nodes every node runs the same deletion, which is idempotent.

### JSON log

`LogSecurityAuditSink` writes every event as one JSON line, at `INFO` level, on the logger `awe.security.audit`.
Empty fields are left out:

```json
{"timestamp":"2026-10-16T08:15:30.123Z","id":"0b5f7c1e-6c1d-4f7e-9a4b-3f2d1c0e9a87","type":"LOGIN","outcome":"FAILURE","cause":"BAD_CREDENTIALS","username":"jdoe","tenant":"awemysql","channel":"WEB","ipAddress":"10.0.12.0","userAgent":"Mozilla/5.0 (X11; Linux x86_64)","sessionHash":"9f2c...e41a","correlationId":"4d1f6a0c-2b7e-4a51-8c0f-7d5e2a9b1c33"}
```

To ship the events to a SIEM, route that logger to its own file in your `logback-spring.xml`:

```xml title="logback-spring.xml"
<appender name="SECURITY_AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>${LOG_PATH:-logs}/security-audit.log</file>
  <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH:-logs}/security-audit.%d{yyyy-MM-dd}.log</fileNamePattern>
    <maxHistory>90</maxHistory>
  </rollingPolicy>
  <encoder>
    <pattern>%msg%n</pattern>
  </encoder>
</appender>

<logger name="awe.security.audit" level="INFO" additivity="false">
  <appender-ref ref="SECURITY_AUDIT"/>
</logger>
```

With the logger above `INFO` nothing is written.

### Micrometer counter

With the [observability starter](../guides/observability-guide.md#security-audit-events), `MicrometerSecurityAuditSink`
counts the events on `awe.security.audit.events`, tagged by `type`, `outcome`, `channel` and `cause` (`none` when
empty). The user, address and other free values are never tags.

### Custom sinks

Declare a bean that implements `com.almis.awe.security.audit.SecurityAuditSink`. It receives every event already
completed with the request data:

```java
@Bean
public SecurityAuditSink kafkaSecurityAuditSink(KafkaTemplate<String, String> kafka) {
  return event -> kafka.send("security-audit", event.getType().name(), event.getId());
}
```

The built-in sinks are `@ConditionalOnMissingBean`: a bean of the same class replaces them. To publish events from
your own code (for example a custom authentication flow), inject `SecurityAuditService` and call `success`,
`failure` or `publish`.

## Automatic lockout

The automatic lockout is **off by default**. Set `awe.security.lockout.max-attempts` to the number of consecutive
wrong passwords after which a local account is locked:

```properties
awe.security.lockout.max-attempts=5
```

| Step | What happens |
| --- | --- |
| Wrong password of an existing user | The counter `ope.NumLog` grows by one. Other failures (locked, disabled, expired...) and unknown users are not counted |
| Counter reaches `max-attempts` | The account is locked (`ope.PwdLck = 1`) and an `ACCOUNT_LOCKED` event with cause `MAX_ATTEMPTS` is recorded. Next logins fail with cause `LOCKED` |
| Successful login | The counter goes back to 0 |
| Administrator unlocks the user (or resets the password) in the users screen | The account is unlocked, the counter is cleared and an `ACCOUNT_UNLOCKED` event is recorded |

The lockout only applies to local logins (form, `/action/login` and `/api/authenticate`); with SSO, lockout is the
identity provider's job. It works even when the audit is switched off. A failure while counting never changes the
result of the login.

## Privacy

- **IP anonymization.** `awe.security.audit.anonymize-ip=true` stores the client address with the last octet of IPv4
  addresses set to zero (`10.0.12.34` → `10.0.12.0`) and IPv6 addresses cut after the first 48 bits
  (`2001:db8:85a3:8d3:1319:8a2e:370:7348` → `2001:db8:85a3::`). Addresses that cannot be parsed are dropped, never
  stored in clear.
- **Sessions.** Only the SHA-256 hash of the session identifier is stored, so the events can be grouped by session
  but the identifier cannot be replayed.
- **Behind a proxy or load balancer** the address is the one of the proxy unless Spring Boot is told to trust the
  forwarded headers: set `server.forward-headers-strategy=native` or `framework` (see
  [Logging, database and web](../guides/v5-migration.md#logging-database-and-web) for war deployments).
- **Retention.** Adjust `awe.security.audit.database.retention-days` to the period your data protection policy
  allows.

## Security audit screen

**Settings > Security access > Security audit** (option `security-audit`, screen `SecAud`) browses the events stored by the
database sink. It is a generic XML screen, rendered by both the Angular and the React clients.

| Part | Content |
| --- | --- |
| Criteria | Date range (the last 7 days by default), event types, outcome, channel, user and IP (partial match) |
| Activity per day | Stacked column chart of successful and failed events |
| Failures by cause | Pie chart of the failures |
| Events | Paginated grid, newest first: date, event, outcome, user, cause, channel, IP and detail |
| View | Detail of the selected event (screen `SecAudViw`, option `view-security-audit-event`), with tenant, identifier, correlation identifier, session hash and user agent |

Applications that define their own `menu/private.xml` (the archetypes do) must add the option to their security
group to show the screen:

```xml
<option name="security-audit" label="MENU_TOOLS_SECURITY_AUDIT" screen="SecAud" icon="shield">
  <option name="view-security-audit-event" screen="SecAudViw" invisible="true"/>
</option>
```

The screen is a regular menu option: restrict it to the profiles that may see it with the usual profile and user
restrictions (see [Security mechanisms](security-management.md)). Without the database sink the screen shows no
events.

## Properties

| Property | Default | Description |
| --- | --- | --- |
| `awe.security.audit.enabled` | `true` | Record security events. With `false` no sink records anything and nothing is purged |
| `awe.security.audit.anonymize-ip` | `false` | Anonymize the client address of every event (IPv4 last octet, IPv6 after the first 48 bits) |
| `awe.security.audit.database.enabled` | `true` | Store the events in the `AweSecAudEvt` table |
| `awe.security.audit.database.retention-days` | `365` | Days an event is kept before the purge deletes it. `0` keeps the events forever |
| `awe.security.audit.database.purge-cron` | `0 0 3 * * *` | Spring cron expression (server time zone) of the purge |
| `awe.security.audit.log.enabled` | `true` | Write every event as one JSON line on the `awe.security.audit` logger |
| `awe.security.audit.metrics.enabled` | `true` | Count the events on the `awe.security.audit.events` counter (observability starter) |
| `awe.security.lockout.max-attempts` | `0` | Consecutive wrong passwords after which a local account is locked. `0` switches the lockout off |

They are also listed in the [properties reference](../properties#awe-security-properties).

## Limitations

- **No concurrent session control.** AWE does not limit the sessions of a user; the audit records the explicit,
  provider and expiration logouts.
- **Custom authentication flows** (filters, controllers or other entry points of your own that authenticate users
  without the AWE login) are not recorded unless they call `SecurityAuditService`.
- **The purge runs on every node.** The deletion is idempotent, so several nodes only repeat the same work.
- **Multiple databases.** With `awe.database.multidatabase-enable=true` the application `DataSource` routes by the
  database of the session: events raised in a request are stored in that database, and the purge, which runs outside
  any request, cleans the default database.
