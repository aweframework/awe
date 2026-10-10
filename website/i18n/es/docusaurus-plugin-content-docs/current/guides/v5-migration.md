---
id: v5-migration
title: Guía de migración a AWE 5
sidebar_label: Guía de migración a AWE 5
---

Esta guía enumera lo que una aplicación debe revisar cuando pasa de AWE 4 a AWE 5. Es una página viva: cada cambio de
la rama `develop` que pueda afectar a una aplicación existente añade aquí una entrada (consulte
[Mantenimiento de esta guía](#maintaining-this-guide)), por lo que irá creciendo hasta la versión final 5.0.

Las entradas describen únicamente cambios que ya están integrados. Los cambios previstos se listan aparte, en
[Próximamente en AWE 5](#coming-in-awe-5), y no se describen en detalle hasta que se integren.

## A quién va dirigida esta guía {#who-this-guide-is-for}

A los desarrolladores de aplicaciones construidas sobre AWE 4 (el cliente AngularJS o el cliente React) y de productos
que extienden AWE, por ejemplo widgets personalizados o pruebas de navegador construidas sobre `awe-testing`.

**Requisitos previos**

1. **Estar en la última versión 4.x.** Actualice primero a la última versión `4.x` y corrija sus avisos de
   obsolescencia: una obsolescencia de la línea 4.x que se elimina en la 5 es más barata de resolver en una línea que
   todavía recibe correcciones.
2. Saber en qué línea está. AWE 4 se mantiene en la rama `support/4.x` (solo correcciones de seguridad y críticas)
   mientras `develop` construye AWE 5. Las ramas, etiquetas, etiquetas de Docker y el periodo de soporte se explican en
   [Líneas de versiones y soporte](release-lines-and-support.md#release-lines).
3. Si su aplicación usaba el cliente React en su línea `2.x`, lea también la
   [guía de actualización del cliente React](react-client-upgrade.md): contiene las instrucciones paso a paso que esta
   página solo resume.

## Matriz de compatibilidad {#compatibility-matrix}

Solo se lista lo que el repositorio declara hoy. Los elementos marcados como *previsto* todavía no están en `develop`.

| Componente | AWE 4 (`support/4.x`) | AWE 5 (`develop`) | Notas |
|---|---|---|---|
| Java (destino de compilación) | 17 | 17 | La integración continua compila con JDK 21 pero genera código para 17 (`java.version` en `awe-dependencies`). Java 21 como base está *previsto* ([#783](https://gitlab.com/aweframework/awe/-/issues/783)). |
| Spring Boot | 3.5.16 | 3.5.16 | Spring Boot 4 está *previsto* ([#783](https://gitlab.com/aweframework/awe/-/issues/783)). |
| Node.js usado por la compilación del frontend con Maven | v24.14.0 | v24.14.0 | `node.version` en `awe-dependencies`. Las aplicaciones que compilan el cliente React con su propio Node deberían usar la misma versión mayor. |
| Cliente React (`awe-react-client`) | `2.x`, línea de versiones propia | Misma versión que el framework (`5.y.z`) | Consulte [Actualización del cliente React](react-client-upgrade.md). React 18.3.1. |
| Gráficos, motor React | Highcharts | Apache ECharts 6.1.0 | Consulte [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Gráficos, motor AngularJS | Highcharts | Highcharts | ECharts para el motor AngularJS está *previsto* ([#775](https://gitlab.com/aweframework/awe/-/issues/775)). |
| Pruebas de navegador (`awe-testing`) | Selenium | Selenium por defecto, Playwright como piloto (`awe.test.tool`) | Consulte la [guía de pruebas con Selenium](selenium-test-guide.md#automation-tool-awetesttool). |
| Navegadores usados para probar el framework | Chrome, Firefox (Selenium) | Chromium y Firefox (Playwright), Chrome y Firefox (Selenium, programado) | Todavía no se declara en el repositorio una lista de navegadores de usuario final soportados. |

## Qué cambia {#what-changes}

Cada fila indica qué notará, qué debe hacer y dónde leer más. «MR» es una merge request del
[proyecto AWE](https://gitlab.com/aweframework/awe/-/merge_requests).

| Área | Síntoma | Qué hacer | Más información |
|---|---|---|---|
| Paquete del cliente React | La versión de `awe-react-client` en su `package.json` es `2.x`, o el `npm ci` de un proyecto 2.x falla tras la actualización. | Establezca `awe-react-client` en la versión exacta de su versión de AWE (por ejemplo `5.0.0`), ejecute `npm install` una vez y confirme el nuevo archivo de bloqueo. No mezcle un cliente 5.x con un servidor AWE 4. | [Actualización del cliente React](react-client-upgrade.md), [paquete npm](release-lines-and-support.md#npm-package) |
| Imágenes de Docker | Necesita una imagen de la aplicación de pruebas React. | `awe-boot-react` se construye y publica en `develop`, `master` y `support/*` con las mismas etiquetas que `awe-boot`. No existe la etiqueta `latest`: fije una versión. | [Imagen de la aplicación de pruebas React](release-lines-and-support.md#react-test-application-image) |
| Gráficos (motor React) | Los gráficos aparecen vacíos con un servidor AWE 4, una serie tiene un color distinto, los gráficos 3D son planos, las reglas CSS `.highcharts-*` no hacen nada, o un componente personalizado que importa Highcharts no compila. | Revise en el log del servidor los avisos `Highcharts chart-parameter`, defina colores y fuentes en el XML y declare Highcharts usted mismo si su propio código lo importa. | [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5), [actualización del cliente React](react-client-upgrade.md#charts-use-apache-echarts) (MR !854, !855, !856) |
| Pruebas de navegador | El compilador avisa de que las sobrecargas de `By` y `getDriver()` están obsoletas; las pruebas dependen del orden de las clases. | Mueva los pasos personalizados a las sobrecargas de `Locator` y a `getBrowser()`; prepare la sesión en cada prueba con `ensureLoggedIn`/`ensureModule`. Para probar Playwright, establezca `awe.test.tool=playwright`. No se elimina nada antes de la 6.0. | [Compatibilidad de API de awe-testing](selenium-test-guide.md#api-compatibility-of-awe-testing), [clases de prueba independientes](selenium-test-guide.md#independent-test-classes), [pasos personalizados sin tipos de Selenium](selenium-test-guide.md#custom-steps-without-selenium-types) (MR !840, !841) |
| Dependencia `jsoup` | `org.jsoup:jsoup` ya no está en el classpath de `awe-model` y ya no lo gestiona el BOM `awe-dependencies`. | Si su propio código usa jsoup, declare la dependencia y su versión en su `pom.xml`. En caso contrario no hay que hacer nada. Consulte la nota bajo la tabla. | MR !849 |
| Caducidad de contraseña (`PwdExp`) | La aplicación define el parámetro `PwdExp`. Tras la actualización, los usuarios cuyo último cambio de contraseña es anterior a `PwdExp` días son rechazados por caducidad (antes entraban), y los que la cambiaron recientemente pueden volver a iniciar sesión (antes eran rechazados). | Nada si los datos son correctos: la comprobación ya no está invertida (consulte la nota bajo la tabla). Revise los usuarios que nunca cambiaron la contraseña y el valor de `PwdExp`. | [Caducidad de contraseña](../security/authentication.md#password-expiration-local-login), incidencia #846 |
| JSON del menú | La carga del menú es más pequeña. | Nada, salvo que un cliente personalizado lea `elementList` de una `Option` del menú: lea `options` en su lugar. Consulte la nota bajo la tabla. | MR !792 |
| Inicio de sesión SSO de usuarios deshabilitados o bloqueados | Un usuario deshabilitado o bloqueado en AWE podía iniciar sesión a través del proveedor de identidad; ahora el inicio de sesión SSO se rechaza con una página de error. | Nada, salvo que dependiera de ello: habilite el usuario en AWE en su lugar. | [Usuarios deshabilitados y bloqueados](../security/authentication-sso.md#disabled-and-locked-users) |
| Beans del manejador SSO | Su propio código llama a `AweWebSecurityConfig.authSuccessHandler()` (ahora recibe como parámetro el bean `AweOauth2AuthenticationFailureHandler`), o construye `AweOauth2AuthenticationSuccessHandler` con un argumento (obsoleto y pendiente de eliminación). | Pase el manejador de fallos: `new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler)`. Los beans de manejador propios ahora se respetan en el inicio de sesión SSO. | [Usuarios deshabilitados y bloqueados](../security/authentication-sso.md#disabled-and-locked-users) |
| Base de datos del planificador | Flyway falla con un error de checksum en `SCHEDULER_V1.0.5`, o no se puede construir una base de datos nueva desde cero. | Ejecute el paso de migración descrito más abajo. | MR !843 |
| Carpetas por defecto (módulo de desarrollador, logs del planificador) | La ruta del módulo de desarrollador y los logs de ejecución del planificador están ahora en una carpeta sin la `}` sobrante con la que terminaba el valor por defecto (`<user.home>/awe-developer` en lugar de `<user.home>/awe-developer}`, `<logging.file.path or java.io.tmpdir>/scheduler` en lugar de `.../scheduler}`). Solo aplica si no definió `awe.developer.path` ni `awe.scheduler.execution-log-path`. | Mueva sus archivos existentes de la carpeta antigua a la nueva si los necesita, o defina la propiedad con la ruta antigua. | MR !889 |

### jsoup en el sanitizador de parámetros de entrada {#jsoup-in-the-input-parameter-sanitizer}

`StringUtil.sanitizeInputParameter`, que `ScreenDataController` aplica a la variable de ruta `optionId`, solía pasar
el valor escapado por `Jsoup.clean(..., Safelist.basic())`. Ahora devuelve el valor escapado por `escapeJson`,
`escapeJava` y `escapeHtml4` sin ese último paso (MR !849). El escapado ya convierte `<` y `>` en entidades, por lo que
la diferencia se limita a los caracteres que jsoup normalizaba después del escapado: una comilla doble sigue escapada
como `&quot;` en lugar de volver a convertirse en `"`, y los espacios repetidos o iniciales ya no se colapsan ni se
recortan. El código que comparaba el valor sanitizado con una cadena fija necesita el nuevo valor; las pruebas
unitarias de `StringUtilTest` muestran el resultado exacto.

### JSON de `Option` del menú {#menu-option-json}

Una `Option` del menú solía serializar sus hijos dos veces, bajo `elementList` y bajo `options`, lo que duplicaba el
tamaño de la carga en cada nivel del árbol del menú. `elementList` ya no forma parte del JSON de una `Option` (MR !792).
Los clientes de este repositorio leen `options`, por lo que para ellos no cambia nada. El `elementList` del árbol de
pantalla (componentes de una pantalla) no se ve afectado.

### Caducidad de contraseña (`PwdExp`) {#password-expiration-pwdexp}

Cuando el parámetro `PwdExp` está definido, el inicio de sesión local evaluaba la caducidad al revés: una contraseña
cambiada en los últimos `PwdExp` días se rechazaba por caducada y una más antigua se aceptaba. Ahora se evalúa
correctamente (incidencia #846): la contraseña se acepta hasta `PwdExp` días después del último cambio.

Qué hacer:

- **Aplicaciones que no definen `PwdExp`:** nada, las contraseñas nunca caducan.
- **Aplicaciones que definen `PwdExp`:** los usuarios cuyo último cambio es anterior a `PwdExp` días ya no pueden
  iniciar sesión hasta que cambien la contraseña, y los usuarios con un cambio reciente sí pueden. Compruebe el valor de
  `PwdExp` antes de la actualización. Para evitar bloquear a muchos usuarios a la vez, revise las fechas de cambio de
  contraseña antes de la actualización y aumente `PwdExp` (o desactívelo) durante un periodo de transición, hasta que
  los usuarios hayan cambiado sus contraseñas mediante la opción de cambio de contraseña. Una contraseña establecida por
  un administrador en la pantalla de usuarios no ayuda aquí: deja la fecha de cambio vacía, lo que cuenta como caducada
  mientras `PwdExp` esté definido (véase más abajo). La pantalla de inicio de sesión todavía no tiene un cambio de
  contraseña guiado.
- **Volver a AWE 4** después de que los usuarios hayan cambiado sus contraseñas restituye la comprobación invertida: los
  usuarios con un cambio reciente vuelven a ser rechazados. Desactive `PwdExp` antes de volver atrás.
- **Los usuarios que nunca cambiaron la contraseña** (fecha vacía) siguen siendo rechazados por caducidad cuando
  `PwdExp` está definido, como antes. Esto incluye a los usuarios cuya contraseña fue establecida por un administrador
  en la pantalla de usuarios.
- **Datos de prueba:** una prueba de integración que inicia sesión con un usuario cuya fecha de contraseña es antigua
  necesita un valor de `PwdExp` mayor que la antigüedad de esa contraseña (las aplicaciones de prueba de AWE usan 36500
  días).

### Migración del planificador `SCHEDULER_V1.0.5` {#scheduler-migration-scheduler_v105}

`SCHEDULER_V1.0.5__Unify_ftp_credentials_into_server.sql` traslada las credenciales FTP de cada lanzador al servidor al
que apuntan. Ya no elimina las columnas `SrvUsr` y `SrvPwd` de `AweSchTskLch` y `HISAweSchTskLch`: permanecen como
columnas obsoletas y anulables que el planificador ya no lee ni escribe (MR !843). El script se modificó porque las
aplicaciones cuyos propios scripts todavía insertan lanzadores con esas columnas no podían construir una base de datos
vacía.

Qué hacer:

- **Base de datos vacía o una base de datos que todavía no ha aplicado V1.0.5:** nada, los scripts se ejecutan en orden.
- **Base de datos que ya aplicó la versión anterior del script** (por ejemplo una construida con 4.12.9 o 4.12.10): su
  checksum cambió, por lo que Flyway se niega a arrancar. Ejecute `flyway repair` una vez, o recree la base de datos.
  Las columnas que eliminaba el script antiguo no se restauran: si sus propios scripts las insertan, vuelva a añadirlas
  en su propia migración.
- **Aplicaciones con scripts propios:** no dependa de las columnas de credenciales de las tablas de lanzadores, están
  obsoletas. Almacene las credenciales en el servidor (`AweSchSrv`).

## Próximamente en AWE 5 {#coming-in-awe-5}

Estas iniciativas están abiertas, etiquetadas para el hito 5.0.0 y se espera que requieran acciones por parte de las
aplicaciones. Se listan para que pueda planificar; cada una añadirá su propia entrada más arriba cuando se integre.

| Incidencia | Título |
|---|---|
| [#755](https://gitlab.com/aweframework/awe/-/issues/755) | Sustituir el Bootstrap 3 incluido por Tailwind y una capa de compatibilidad de AWE |
| [#775](https://gitlab.com/aweframework/awe/-/issues/775) | Migrar los gráficos de Highcharts a Apache ECharts con renderizado en el servidor |
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Sustituir angular-ui-grid por un componente de rejilla moderno |
| [#778](https://gitlab.com/aweframework/awe/-/issues/778) | Imponer un hash de contraseñas moderno y una clave maestra obligatoria |
| [#783](https://gitlab.com/aweframework/awe/-/issues/783) | Actualizar la plataforma base a Java 21 y Spring Boot 4 |
| [#823](https://gitlab.com/aweframework/awe/-/issues/823) | Alinear los nombres de acciones del constructor y los valores muertos del XSD con el contrato del cliente |
| [#829](https://gitlab.com/aweframework/awe/-/issues/829) | Sanitizar el HTML de las celdas de rejilla en el cliente React (se descartará un `style` en línea en una celda; use una clase CSS) |

## Lista de comprobación de la actualización {#upgrade-checklist}

Copie esta lista a la incidencia de su actualización y márquela a medida que avance.

```markdown
- [ ] The application runs on the latest 4.x release and builds without deprecation warnings
- [ ] `awe.version` (or the `awe-starter-parent` version) is set to the AWE 5 version
- [ ] `awe-react-client` is set to the exact same version, `npm install` was run and the lock file is committed
- [ ] Every screen with a chart was opened and the server log has no `Highcharts chart-parameter` warnings
- [ ] Custom code that imports Highcharts or reads `.highcharts-*` CSS was reviewed
- [ ] Custom code that uses jsoup declares its own dependency
- [ ] If the application sets `PwdExp`, the value and the users with an old or empty password change date were reviewed
- [ ] Custom clients do not read `elementList` from the menu JSON
- [ ] Flyway was repaired or the database recreated if `SCHEDULER_V1.0.5` had been applied before
- [ ] Browser tests compile; the `By` overloads and `getDriver()` were moved to `Locator` and `getBrowser()`
- [ ] The open items of "Coming in AWE 5" were reviewed against the application
```

## Mantenimiento de esta guía {#maintaining-this-guide}

Esta guía solo es útil si se mantiene actualizada, por lo que forma parte de la definición de terminado de un cambio:

- **Quién:** el autor de una merge request que pueda afectar a una aplicación existente: una API, XML, propiedad,
  script de base de datos o campo JSON eliminado o renombrado, un valor por defecto modificado o una dependencia que las
  aplicaciones reciben de AWE.
- **Cuándo:** en la misma merge request, marque la casilla «If this MR has impacts on existing applications, I added an
  entry to the AWE 5 migration guide». La etiqueta `has impacts` marca estas merge requests e incidencias.
- **Cómo:** añada una fila a la tabla [Qué cambia](#what-changes) con el síntoma, qué hacer y un enlace a la página
  detallada, y una subsección debajo cuando la fila no sea suficiente. Describa solo lo que está integrado y se
  comprobó en el código; saque la incidencia de [Próximamente en AWE 5](#coming-in-awe-5) cuando se integre.
- **Dónde:** edite `website/docs/guides/v5-migration.md` en `develop`. Los cambios que solo existen en la línea 4.x
  pertenecen a la documentación de `support/4.x` (consulte
  [Documentación por línea](release-lines-and-support.md#documentation-per-line)).
