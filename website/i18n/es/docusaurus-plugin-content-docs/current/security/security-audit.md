---
id: security-audit
title: Auditoría de seguridad
sidebar_label: Auditoría de seguridad
---

# Auditoría de seguridad {#security-audit}

AWE registra los eventos de seguridad de una aplicación: inicios y cierres de sesión, bloqueos de cuentas, cambios de
contraseña y de segundo factor, aprovisionamiento SSO, accesos denegados y tokens REST. Cada evento lleva el usuario,
el tenant, la dirección del cliente, el agente de usuario, un hash de la sesión y un identificador de correlación, y se
envía a uno o varios **destinos** (sinks): una tabla de base de datos, una línea de log JSON para un SIEM y un contador
de Micrometer.

La auditoría está **activada por defecto**. El único cambio visible para una aplicación existente es la tabla
`AweSecAudEvt`, que crea la migración de Flyway `AWE_V1.3.0`, y la pantalla **Auditoría de seguridad** del menú de
seguridad.

| Si quiere | Consulte |
| --- | --- |
| Desactivar toda la funcionalidad | [Activarla y desactivarla](#turn-it-on-and-off) |
| Saber qué se registra | [Modelo de evento](#event-model), [Catálogo de eventos](#event-catalogue) |
| Enviar los eventos a un SIEM o no guardarlos en la base de datos | [Destinos](#sinks) |
| Bloquear cuentas tras inicios de sesión fallidos | [Bloqueo automático](#automatic-lockout) |
| Cumplir la normativa de protección de datos | [Privacidad](#privacy) |
| Consultar los eventos | [Pantalla de auditoría de seguridad](#security-audit-screen) |

## Activarla y desactivarla {#turn-it-on-and-off}

`awe.security.audit.enabled` es el interruptor general. Con `false` ningún destino registra eventos y no se purga
nada; el servicio de auditoría sigue disponible sin hacer nada, de modo que el código que publica eventos no necesita
comprobaciones.

```properties
# Desactivar la auditoría de seguridad
awe.security.audit.enabled=false
```

Cada destino tiene además su propio interruptor (consulte [Propiedades](#properties)). Los productos que ya auditan
por su cuenta pueden, por ejemplo, mantener el log JSON y prescindir de la tabla:

```properties
awe.security.audit.database.enabled=false
```

## Modelo de evento {#event-model}

Un evento es un `com.almis.awe.model.security.audit.SecurityAuditEvent`. El framework completa los datos de la
petición al publicar el evento; los valores ya informados en el evento se conservan.

| Campo | Contenido |
| --- | --- |
| `id` | Identificador único (UUID) |
| `timestamp` | Momento del evento |
| `type` | Tipo de evento, consulte el [catálogo](#event-catalogue) |
| `outcome` | `SUCCESS` o `FAILURE` |
| `cause` | Motivo, sobre todo de los fallos (puede estar vacío) |
| `username` | Usuario implicado, tal como se escribió en el inicio de sesión o el usuario autenticado (hasta 255 caracteres) |
| `tenant` | Tenant SSO de la petición o, si no lo hay, el alias de base de datos de la sesión (hasta 100 caracteres) |
| `channel` | `WEB` (aplicación web), `SSO` (proveedor de identidad), `REST` (peticiones bajo `/api/`) o `SYSTEM` (generado por el servidor fuera de cualquier petición, como la caducidad de una sesión) |
| `ipAddress` | Dirección del cliente, [anonimizada](#privacy) si así se configura |
| `userAgent` | Cabecera `User-Agent` del cliente (hasta 512 caracteres) |
| `sessionHash` | SHA-256 del identificador de sesión. El identificador de sesión nunca se guarda |
| `correlationId` | Identificador común a los eventos de una petición: la cabecera `X-Correlation-Id`, si no `X-Request-Id`, si no el `traceId` del MDC (trazas) y, en último caso, uno generado por petición. Se ignoran los valores de cabecera que no tengan de 1 a 64 caracteres entre letras, dígitos, `.`, `_`, `:` o `-` |
| `detail` | Detalle libre: opción, URL, perfil, administrador, código de error del proveedor... (hasta 1000 caracteres) |

## Catálogo de eventos {#event-catalogue}

Los valores de tipo y causa son los enumerados `SecurityAuditEventType` y `SecurityAuditCause`. El "canal" es el de la
petición salvo que se indique un valor.

| Tipo | Se genera cuando | Resultado y causa | Canal |
| --- | --- | --- | --- |
| `LOGIN` | Termina un inicio de sesión local (formulario, `/action/login`, `POST /api/authenticate`) | `SUCCESS`; `FAILURE` con `BAD_CREDENTIALS` (contraseña incorrecta o usuario desconocido), `LOCKED`, `DISABLED`, `ACCOUNT_EXPIRED`, `CREDENTIALS_EXPIRED`, `LOCAL_LOGIN_DISABLED` o `ERROR` | `WEB`, `REST` |
| `LOGIN` | Termina un inicio de sesión SSO | `SUCCESS`; `FAILURE` con `LOCKED`, `DISABLED`... (cuenta rechazada por AWE), `ROLE_NOT_MAPPED`, `PROVIDER_ERROR` (detalle: código de error OAuth2) o `ERROR` | `SSO` |
| `LOGOUT` | El usuario cierra la sesión | `SUCCESS`, `EXPLICIT` | Canal de la petición, `SSO` en sesiones SSO |
| `LOGOUT` | El proveedor de identidad termina la sesión (OIDC back-channel logout) | `SUCCESS`, `PROVIDER` | `SSO` |
| `LOGOUT` | Caduca una sesión iniciada | `SUCCESS`, `EXPIRED` | `SYSTEM` |
| `ACCOUNT_LOCKED` | El [bloqueo automático](#automatic-lockout) bloquea la cuenta | `SUCCESS`, `MAX_ATTEMPTS` | `WEB`, `REST` |
| `ACCOUNT_LOCKED` | Un administrador bloquea al usuario en la pantalla de usuarios | `SUCCESS`, `ADMINISTRATOR` (detalle: `administrator: <usuario>`) | `WEB` |
| `ACCOUNT_UNLOCKED` | Un administrador desbloquea al usuario, o restablece la contraseña de un usuario bloqueado, en la pantalla de usuarios | `SUCCESS`, `ADMINISTRATOR` | `WEB` |
| `PASSWORD_CHANGE` | El usuario cambia la contraseña en las pantallas de cambio de contraseña | `SUCCESS`; `FAILURE`, `BAD_CREDENTIALS` (contraseña actual incorrecta) | `WEB` |
| `PASSWORD_CHANGE` | El usuario sustituye una contraseña caducada desde el inicio de sesión | `SUCCESS`, `CREDENTIALS_EXPIRED` | `WEB` |
| `PASSWORD_RESET` | Un administrador establece una contraseña nueva en la pantalla de usuarios | `SUCCESS`, `ADMINISTRATOR` | `WEB` |
| `TOTP_ENROLLMENT` | Se genera y guarda un secreto de segundo factor | `SUCCESS`; `FAILURE` con `FORBIDDEN` (una sesión pendiente de su código TOTP intenta rotar el secreto) o `ERROR` (no se pudo guardar el secreto) | `WEB` |
| `TOTP_VERIFICATION` | El usuario introduce el código TOTP tras iniciar sesión | `SUCCESS`; `FAILURE`, `INVALID_CODE` | `WEB` |
| `TOTP_RESET` | El usuario desactiva el segundo factor, o un administrador lo restablece | `SUCCESS` (sin causa, o `ADMINISTRATOR`) | `WEB` |
| `SSO_PROVISIONING` | Se crea un usuario a partir de los datos del proveedor de identidad (`awe.security.sso.auto-provision-user`) | `SUCCESS` (detalle: perfil) | `SSO` |
| `SSO_PROFILE_SYNC` | El perfil de un usuario se actualiza a partir de los roles del proveedor | `SUCCESS` (detalle: `<anterior> -> <nuevo>`) | `SSO` |
| `ACCESS_DENIED` | Una opción que existe en el menú está restringida para el perfil del usuario | `FAILURE`, `RESTRICTED` (detalle: `option: <nombre>`) | `WEB` |
| `ACCESS_DENIED` | Spring Security deniega una URL a un usuario autenticado | `FAILURE`, `FORBIDDEN` (detalle: `url: <ruta>`) | Canal de la petición |
| `TOKEN_ISSUED` | `POST /api/authenticate` emite un JWT | `SUCCESS` | `REST` |
| `TOKEN_REJECTED` | Una petición REST lleva un JWT mal formado, mal firmado o caducado | `FAILURE`, `INVALID_TOKEN` o `TOKEN_EXPIRED` | `REST` |

## Destinos {#sinks}

El servicio de auditoría envía cada evento a todos los beans `SecurityAuditSink`. El registro se hace en la medida de
lo posible: un destino que falla se registra en el log y no detiene a los demás destinos ni a la operación auditada.
Los eventos publicados dentro de una transacción (por ejemplo, un bloqueo hecho por un maintain) se envían **después
del commit** y se descartan si la transacción hace rollback, de modo que solo describen cambios que se han producido
de verdad.

### Base de datos {#database}

`DatabaseSecurityAuditSink` guarda los eventos en la tabla de solo inserción `AweSecAudEvt`, que crea la migración
`AWE_V1.3.0__Security_audit_events.sql` en todas las bases de datos soportadas. El script se puede volver a aplicar: la
tabla y sus índices solo se crean si no existen. Cada evento se escribe en su propia
conexión del `DataSource` de la aplicación y se confirma al momento, fuera de la transacción de la operación auditada.
Las filas solo se borran con la purga.

| Columna | Campo | Columna | Campo |
| --- | --- | --- | --- |
| `IdeSecAud` | `id` | `Chn` | `channel` |
| `EvtDat` | `timestamp` | `IpAdr` | `ipAddress` |
| `EvtTyp` | `type` | `UsrAgt` | `userAgent` |
| `EvtRes` | `outcome` | `SesHsh` | `sessionHash` |
| `EvtCau` | `cause` | `CorIde` | `correlationId` |
| `UsrNam` | `username` | `Det` | `detail` |
| `Ten` | `tenant` | `EvtDay` | día de `timestamp` |

`EvtDay` es el día del evento en la zona horaria del servidor: los filtros de fecha y la gráfica diaria de la pantalla
comparan fechas sin hora sobre esa columna, así que se comportan igual en todas las bases de datos y usan su índice.

Índices: `EvtDat`, `(UsrNam, EvtDat)`, `(EvtTyp, EvtDat)` y `EvtDay`.

**Retención.** La purga borra los eventos con más de `awe.security.audit.database.retention-days` días (365 por
defecto; `0` los conserva para siempre) según la planificación `awe.security.audit.database.purge-cron` (por defecto,
todos los días a las 03:00, en la zona horaria del servidor). Se ejecuta en su propio hilo, así que la aplicación no
necesita `@EnableScheduling`. Con varios nodos, cada nodo ejecuta el mismo borrado, que es idempotente.

### Log JSON {#json-log}

`LogSecurityAuditSink` escribe cada evento como una línea JSON, con nivel `INFO`, en el logger `awe.security.audit`.
Los campos vacíos se omiten:

```json
{"timestamp":"2026-10-16T08:15:30.123Z","id":"0b5f7c1e-6c1d-4f7e-9a4b-3f2d1c0e9a87","type":"LOGIN","outcome":"FAILURE","cause":"BAD_CREDENTIALS","username":"jdoe","tenant":"awemysql","channel":"WEB","ipAddress":"10.0.12.0","userAgent":"Mozilla/5.0 (X11; Linux x86_64)","sessionHash":"9f2c...e41a","correlationId":"4d1f6a0c-2b7e-4a51-8c0f-7d5e2a9b1c33"}
```

Para enviar los eventos a un SIEM, dirija ese logger a su propio fichero en su `logback-spring.xml`:

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

Si el nivel del logger es superior a `INFO`, no se escribe nada.

### Contador de Micrometer {#micrometer-counter}

Con el [starter de observabilidad](../guides/observability-guide.md#security-audit-events),
`MicrometerSecurityAuditSink` cuenta los eventos en `awe.security.audit.events`, con las etiquetas `type`, `outcome`,
`channel` y `cause` (`none` si están vacías). El usuario, la dirección y los demás valores libres nunca son etiquetas.

### Destinos propios {#custom-sinks}

Declare un bean que implemente `com.almis.awe.security.audit.SecurityAuditSink`. Recibe cada evento ya completado con
los datos de la petición:

```java
@Bean
public SecurityAuditSink kafkaSecurityAuditSink(KafkaTemplate<String, String> kafka) {
  return event -> kafka.send("security-audit", event.getType().name(), event.getId());
}
```

Los destinos incluidos son `@ConditionalOnMissingBean`: un bean de la misma clase los sustituye. Para publicar eventos
desde su propio código (por ejemplo, un flujo de autenticación propio), inyecte `SecurityAuditService` y llame a
`success`, `failure` o `publish`.

## Bloqueo automático {#automatic-lockout}

El bloqueo automático está **desactivado por defecto**. Asigne a `awe.security.lockout.max-attempts` el número de
contraseñas incorrectas consecutivas tras el que se bloquea una cuenta local:

```properties
awe.security.lockout.max-attempts=5
```

| Paso | Qué ocurre |
| --- | --- |
| Contraseña incorrecta de un usuario existente | El contador `ope.NumLog` aumenta en uno. Los demás fallos (bloqueado, deshabilitado, caducado...) y los usuarios desconocidos no cuentan |
| El contador llega a `max-attempts` | La cuenta se bloquea (`ope.PwdLck = 1`) y se registra un evento `ACCOUNT_LOCKED` con causa `MAX_ATTEMPTS`. Los siguientes inicios de sesión fallan con causa `LOCKED` |
| Inicio de sesión correcto | El contador vuelve a 0 |
| Un administrador desbloquea al usuario (o restablece la contraseña) en la pantalla de usuarios | La cuenta se desbloquea, el contador se pone a cero y se registra un evento `ACCOUNT_UNLOCKED` |

El bloqueo solo se aplica a los inicios de sesión locales (formulario, `/action/login` y `/api/authenticate`); con SSO,
el bloqueo corresponde al proveedor de identidad. Funciona aunque la auditoría esté desactivada. Un fallo al contar
nunca cambia el resultado del inicio de sesión.

## Privacidad {#privacy}

- **Anonimización de la IP.** `awe.security.audit.anonymize-ip=true` guarda la dirección del cliente con el último
  octeto de las direcciones IPv4 a cero (`10.0.12.34` → `10.0.12.0`) y las direcciones IPv6 cortadas tras los primeros
  48 bits (`2001:db8:85a3:8d3:1319:8a2e:370:7348` → `2001:db8:85a3::`). Las direcciones que no se pueden interpretar
  se descartan, nunca se guardan en claro.
- **Sesiones.** Solo se guarda el hash SHA-256 del identificador de sesión, de modo que los eventos se pueden agrupar
  por sesión pero el identificador no se puede reutilizar.
- **Detrás de un proxy o balanceador** la dirección es la del proxy salvo que se indique a Spring Boot que confíe en
  las cabeceras reenviadas: establezca `server.forward-headers-strategy=native` o `framework` (consulte
  [Logging, base de datos y web](../guides/v5-migration.md#logging-database-and-web) para los despliegues war).
- **Retención.** Ajuste `awe.security.audit.database.retention-days` al periodo que permita su política de protección
  de datos.

## Pantalla de auditoría de seguridad {#security-audit-screen}

**Configuración > Seguridad > Auditoría de seguridad** (opción `security-audit`, pantalla `SecAud`) consulta los
eventos que guarda el destino de base de datos. Es una pantalla XML genérica que muestran tanto el cliente Angular
como el cliente React.

| Parte | Contenido |
| --- | --- |
| Criterios | Rango de fechas (por defecto, los últimos 7 días), tipos de evento, resultado, canal, usuario e IP (coincidencia parcial) |
| Actividad por día | Gráfico de columnas apiladas con los eventos correctos y fallidos |
| Fallos por causa | Gráfico circular de los fallos |
| Eventos | Tabla paginada, de más reciente a más antiguo: fecha, evento, resultado, usuario, causa, canal, IP y detalle |
| Ver | Detalle del evento seleccionado (pantalla `SecAudViw`, opción `view-security-audit-event`), con tenant, identificador, identificador de correlación, hash de sesión y agente de usuario |

Las aplicaciones que definen su propio `menu/private.xml` (los arquetipos lo hacen) deben añadir la opción a su grupo
de seguridad para mostrar la pantalla:

```xml
<option name="security-audit" label="MENU_TOOLS_SECURITY_AUDIT" screen="SecAud" icon="shield">
  <option name="view-security-audit-event" screen="SecAudViw" invisible="true"/>
</option>
```

La pantalla es una opción de menú normal: restrínjala a los perfiles que puedan verla con las restricciones de perfil
y de usuario habituales (consulte [Mecanismos de seguridad](security-management.md)). Sin el destino de base de datos
la pantalla no muestra eventos.

## Propiedades {#properties}

| Propiedad | Valor por defecto | Descripción |
| --- | --- | --- |
| `awe.security.audit.enabled` | `true` | Registra los eventos de seguridad. Con `false` ningún destino registra nada y no se purga nada |
| `awe.security.audit.anonymize-ip` | `false` | Anonimiza la dirección del cliente de cada evento (último octeto en IPv4, a partir de los primeros 48 bits en IPv6) |
| `awe.security.audit.database.enabled` | `true` | Guarda los eventos en la tabla `AweSecAudEvt` |
| `awe.security.audit.database.retention-days` | `365` | Días que se conserva un evento antes de que la purga lo borre. `0` conserva los eventos para siempre |
| `awe.security.audit.database.purge-cron` | `0 0 3 * * *` | Expresión cron de Spring (zona horaria del servidor) de la purga |
| `awe.security.audit.log.enabled` | `true` | Escribe cada evento como una línea JSON en el logger `awe.security.audit` |
| `awe.security.audit.metrics.enabled` | `true` | Cuenta los eventos en el contador `awe.security.audit.events` (starter de observabilidad) |
| `awe.security.lockout.max-attempts` | `0` | Contraseñas incorrectas consecutivas tras las que se bloquea una cuenta local. `0` desactiva el bloqueo |

También aparecen en la [referencia de propiedades](../properties#awe-security-properties).

## Limitaciones {#limitations}

- **Sin control de sesiones concurrentes.** AWE no limita las sesiones de un usuario; la auditoría registra los
  cierres de sesión explícitos, del proveedor y por caducidad.
- **Los flujos de autenticación propios** (filtros, controladores u otros puntos de entrada propios que autentican
  usuarios sin pasar por el inicio de sesión de AWE) no se registran salvo que llamen a `SecurityAuditService`.
- **La purga se ejecuta en cada nodo.** El borrado es idempotente, así que varios nodos solo repiten el mismo trabajo.
- **Varias bases de datos.** Con `awe.database.multidatabase-enable=true` el `DataSource` de la aplicación enruta según
  la base de datos de la sesión: los eventos generados en una petición se guardan en esa base de datos, y la purga, que
  se ejecuta fuera de cualquier petición, limpia la base de datos por defecto.
