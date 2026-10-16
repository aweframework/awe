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
| Java (destino de compilación y ejecución) | 17 | **21** | `java.version` en `awe-dependencies` y en los arquetipos. Se necesita un JDK 21 para compilar y para ejecutar. Consulte [Java 21 y Spring Boot 4.1](#java-21-and-spring-boot-41). |
| Spring Boot | 3.5.16 | **4.1.1** | Spring Framework 7.0, Spring Security 7.0, JUnit 6, Flyway 11. |
| Spring Cloud | 2025.0.3 | **2025.1.3** | |
| Jackson | 2.x (`com.fasterxml.jackson`) | **3.2.1** (`tools.jackson`) | Las anotaciones siguen en `com.fasterxml.jackson.annotation`. Consulte [Jackson 3](#jackson-3). |
| java-jwt / springdoc-openapi | 3.18.2 / 2.8.13 | 4.5.0 / 3.0.3 | |
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
| Caducidad de contraseña (`PwdExp`) | La aplicación define el parámetro `PwdExp`. Tras la actualización, los usuarios cuyo último cambio de contraseña es anterior a `PwdExp` días se les indica que la contraseña ha caducado (antes entraban) y tienen que establecer una nueva, y los que la cambiaron recientemente pueden volver a iniciar sesión (antes eran rechazados). | Nada si los datos son correctos: la comprobación ya no está invertida y el inicio de sesión guía al usuario para cambiar la contraseña (consulte la nota bajo la tabla). Revise los usuarios que nunca cambiaron la contraseña y el valor de `PwdExp`. | [Caducidad de contraseña](../security/authentication.md#password-expiration-local-login), incidencia #846 |
| JSON del menú | La carga del menú es más pequeña. | Nada, salvo que un cliente personalizado lea `elementList` de una `Option` del menú: lea `options` en su lugar. Consulte la nota bajo la tabla. | MR !792 |
| Inicio de sesión SSO de usuarios deshabilitados o bloqueados | Un usuario deshabilitado o bloqueado en AWE podía iniciar sesión a través del proveedor de identidad; ahora el inicio de sesión SSO se rechaza con una página de error. | Nada, salvo que dependiera de ello: habilite el usuario en AWE en su lugar. | [Usuarios deshabilitados y bloqueados](../security/authentication-sso.md#disabled-and-locked-users) |
| Beans del manejador SSO | Su propio código llama a `AweWebSecurityConfig.authSuccessHandler()` (ahora recibe como parámetro el bean `AweOauth2AuthenticationFailureHandler`), o construye `AweOauth2AuthenticationSuccessHandler` con un argumento (obsoleto y pendiente de eliminación). | Pase el manejador de fallos: `new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler)`. Los beans de manejador propios ahora se respetan en el inicio de sesión SSO. | [Usuarios deshabilitados y bloqueados](../security/authentication-sso.md#disabled-and-locked-users) |
| Base de datos del planificador | Flyway falla con un error de checksum en `SCHEDULER_V1.0.5`, o no se puede construir una base de datos nueva desde cero. | Ejecute el paso de migración descrito más abajo. | MR !843 |
| Carpetas por defecto (módulo de desarrollador, logs del planificador) | La ruta del módulo de desarrollador y los logs de ejecución del planificador están ahora en una carpeta sin la `}` sobrante con la que terminaba el valor por defecto (`<user.home>/awe-developer` en lugar de `<user.home>/awe-developer}`, `<logging.file.path or java.io.tmpdir>/scheduler` en lugar de `.../scheduler}`). Solo aplica si no definió `awe.developer.path` ni `awe.scheduler.execution-log-path`. | Mueva sus archivos existentes de la carpeta antigua a la nueva si los necesita, o defina la propiedad con la ruta antigua. | MR !889 |
| JDK y Spring Boot (#783) | La aplicación no arranca ni compila con JDK 17; Maven no reconoce starters como `spring-boot-starter-web` o `spring-boot-starter-aop`; `@MockBean` no compila. | Compile y ejecute con JDK 21, establezca `java.version` a 21 y pase a los starters y anotaciones de prueba de Spring Boot 4.1. | [Java 21 y Spring Boot 4.1](#java-21-and-spring-boot-41) |
| Jackson 3 (#783) | Su código no compila porque un método de AWE recibe o devuelve ahora `tools.jackson.databind.node.ObjectNode` (o `JsonNode`, `ArrayNode`, `ObjectMapper`); `asText()` o `asInt()` lanza `JsonNodeException`. | Cambie los imports a `tools.jackson.*`, use las sobrecargas con valor por defecto (`asString("")`, `asInt(0)`...) y sustituya `JsonProcessingException` por `JacksonException`. | [Jackson 3](#jackson-3), [API pública con Jackson 3](#jackson-3-public-api) |
| Peticiones JSON con contenido sobrante (#783) | Un cliente recibe HTTP 400 para un cuerpo como `{...},` o `{...}}` que AWE 4 aceptaba. | Corrija el cliente: AWE 5 rechaza el contenido que sigue al valor JSON raíz. El resto del formato JSON es el de AWE 4. | [Contrato JSON](#json-contract) |
| Clientes de AWE con Jackson 3 (#783) | Un cliente construido con los valores por defecto de Spring 7 / Jackson 3 falla con `Cannot deserialize value of type AnswerType from String "OK"`. | Configure el cliente con el mapper de AWE: `MessageConverterUtil.withJsonMapper(restTemplate, DataListUtil.getMapper())`. | [Contrato JSON](#json-contract) |
| Configuración de Logback (#783) | La aplicación falla al arrancar con `Logback configuration error detected` tras la actualización. | Elimine la regla de conversión `applicationName` de su propio `logback-spring.xml`; use `%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}`. | [Logging, base de datos y web](#logging-database-and-web) |
| Redirecciones del inicio de sesión (#783) | Una petición de navegador sin autenticar se redirige a `Location: /` en lugar de a una URL absoluta. | Nada, salvo que algo analice la URL absoluta: acepte una relativa. Las implementaciones propias de `AuthorizationManager` deben recibir `Supplier<? extends Authentication>`. | [Spring Security 7](#spring-security-7) |
| Plugins de build (#783) | Su `pom.xml` declara `lombok-maven-plugin` a través de la gestión de plugins de AWE y el build no resuelve su versión; `selenium-server` no tiene versión gestionada. | Declare `maven-antrun-plugin` para delombok (la ejecución `delombok` de AWE) o elimínelo; declare usted mismo la versión de `selenium-server`. | [Java 21 y Spring Boot 4.1](#java-21-and-spring-boot-41) |
| Transacciones de maintain (#781) | Un maintain que falla ya no deja nada a medias: los maintains lanzados desde servicios Java y `retrieve-data` se unen al maintain exterior, un paso que devuelve `ERROR` deshace el maintain, y un maintain lanzado dentro de un `@Transactional` de la aplicación se une a esa transacción. Un test que lee con su propia conexión JDBC dentro de un test `@Transactional` no ve las filas (o espera a un bloqueo). | Nada para maintains correctos. Si la aplicación depende de confirmaciones parciales, defina `awe.maintain.transaction.enabled=false` o mueva ese trabajo a su propio maintain. En los tests, lea a través de la transacción (`JdbcTemplate`, `TransactionAwareDataSourceProxy`). | [Transacciones de maintain](#maintain-transactions), [Transacciones](../api/maintain-definition.md#transactions) |
| Utilidades de fecha (#782) | El compilador avisa de que los métodos de `DateUtil` que reciben o devuelven `java.util.Date` o `Calendar` están deprecados. | Nada obligatorio: mantienen su comportamiento y no se eliminan antes de la 6.0. Pase a los métodos `java.time` de `com.almis.awe.model.util.data.date`. | [Utilidades de fecha](#date-utilities) |
| MenuService (#782) | Solo para proyectos que extienden `MenuService`: un método que sobrescriba ya no lo llaman los demás métodos de `MenuService` (por ejemplo, sobrescribir `getOptionByName` ya no cambia `getOptionScreen`). El compilador avisa de que el constructor de siete argumentos de `MenuService` está deprecado. | Nada si solo llama a `MenuService`. Si lo extiende, lleve la personalización a su propio bean del colaborador que contiene el método. | [MenuService](#menuservice) |
| Spring Session con Redis (#861) | Con varias instancias, los usuarios pierden la sesión al azar y las pantallas fallan tras el login (por ejemplo `Index 0 out of bounds for length 0`): la cookie de sesión es `JSESSIONID` en lugar de `SESSION`, porque las sesiones se quedan en cada servidor. | Sustituya `spring-session-data-redis` por `spring-boot-starter-session-data-redis`, renombre `spring.session.redis.*` a `spring.session.data.redis.*` y actualice las clases de Redis en `spring.autoconfigure.exclude`. | [Java 21 y Spring Boot 4.1](#java-21-and-spring-boot-41) |
| `TemplateService.getTagList(Screen, String)` (#861) | El código que llama directamente a este método no compila: ahora declara `throws AWENotFoundException`. Un tag list que no está en la pantalla lanza este error en lugar de un `IndexOutOfBoundsException`. | Capture o propague `AWENotFoundException` (o `AWException`). | issue #861 |
| SQLBuilder (#782) | Solo para proyectos que extienden `SQLQueryBuilder` o `SQLMaintainBuilder` e invocan por reflexión métodos privados eliminados de `SQLBuilder`. Los métodos protegidos mantienen sus firmas y se siguen llamando entre sí en el mismo orden. | Nada si solo sobrescribe métodos protegidos. Sustituya las llamadas por reflexión a los métodos privados eliminados por los métodos protegidos. | [SQLBuilder](#sqlbuilder) |
| Hilos virtuales (#783, nuevo) | No cambia nada salvo que defina `spring.threads.virtual.enabled=true`. | Opcional: defina la propiedad para ejecutar Spring Boot y los ejecutores de tareas de AWE sobre hilos virtuales. | [Hilos virtuales](../deployment.md#virtual-threads) |
| Dependencias de los clientes (`moment`, `lodash`, herramientas de build) | El cliente AngularJS ya no incluye `moment` (usa `day.js`) y el cliente React ya no lista `@redux-devtools/extension`, `redux-thunk`, `react-transition-group`, `deep-freeze`, `autoprefixer`, `postcss-loader` ni `@types/*`. Su propio código importa uno de ellos y el build falla con `Cannot find module`. | Declare en su propio `package.json` el paquete que importa: AWE solo los instalaba como detalle de implementación y no los promete a las aplicaciones. `lodash` se mantiene, pero trátelo igual. Consulte la nota bajo la tabla. | [Dependencias de los clientes](#client-dependencies), incidencia #769 |
| Modo oscuro y temas (cliente AngularJS) | Los usuarios que nunca eligieron el modo claro u oscuro obtienen ahora el modo oscuro cuando su sistema operativo lo prefiere. Los temas declaran ahora sus variables oscuras mediante un mixin y existen los design tokens `--awe-*`. | Nada para temas y LESS correctos. Para seguir ignorando el sistema operativo, defina `@awe-follow-os-color-scheme: false` después de importar AWE. Los usuarios pueden seguir eligiendo el modo claro u oscuro en el menú de usuario. | [Design tokens y modo oscuro](design-tokens-guide.md) (issue #770) |
| Indicadores de carga (ambos clientes) | Un grid o un grid en árbol sin `icon-loading` muestra filas de relleno con la forma de sus columnas mientras cargan sus filas, en lugar del spinner. Una pantalla muestra una estructura de relleno mientras se abre. Los valores `data-testid` de los indicadores no cambian, así que las esperas de los tests de navegador siguen funcionando. | Nada. Para mantener el spinner en un grid, defina `icon-loading="spinner"` en él; para mantenerlo en todos los grids, defina `awe.application.component.grid-icon-loading=spinner`. | [Atributos del grid](../api/grids.md#general-attributes), [Propiedades](../properties#awe.application.component.grid-icon-loading) (issue #774) |

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
  mientras `PwdExp` esté definido (véase más abajo). Los usuarios no necesitan a un administrador: el inicio de sesión les
  guía para establecer una nueva contraseña (véase más abajo).
- **Volver a AWE 4** después de que los usuarios hayan cambiado sus contraseñas restituye la comprobación invertida: los
  usuarios con un cambio reciente vuelven a ser rechazados. Desactive `PwdExp` antes de volver atrás.
- **Los usuarios que nunca cambiaron la contraseña** (fecha vacía) siguen estando caducados cuando
  `PwdExp` está definido, como antes. Esto incluye a los usuarios cuya contraseña fue establecida por un administrador
  en la pantalla de usuarios: tienen que cambiarla en el primer inicio de sesión (decisión documentada, incidencia #849).
- **Las contraseñas caducadas se pueden cambiar desde el inicio de sesión** (incidencia #849). El usuario recibe un mensaje
  localizado (antes era el texto en inglés de Spring bajo el título de credenciales no válidas) y es llevado a la pantalla
  pública `change-expired-password` en ambos motores web, y después inicia sesión con la nueva contraseña. El usuario nunca
  obtiene una sesión con la contraseña caducada. `POST /api/authenticate` responde `401` con un mensaje localizado y el
  código `PASSWORD_EXPIRED`; los clientes REST no pueden cambiar la contraseña y tienen que usar la aplicación web. Consulte
  [Caducidad de contraseña](../security/authentication.md#password-expiration-local-login).
- **El maintain target `ChdPwd` ya no es público.** Podía invocarse sin sesión para cambiar la contraseña de cualquier
  usuario. Ahora necesita sesión y cambia solo la contraseña del usuario conectado. Una pantalla propia que dependiera de
  la llamada anónima tiene que usar `ChdPwdExpired` (contraseñas caducadas) o iniciar sesión primero. Una aplicación que
  sustituya todo el menú `public` tiene que añadir la opción `change-expired-password`.
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

### Dependencias de los clientes {#client-dependencies}

La incidencia #769 limpió las dependencias npm de ambos clientes:

- **`moment` se sustituye por `day.js`** en el cliente AngularJS. Las fechas mantienen el formato `dd/mm/yyyy` que lee el
  servidor y las reglas de validación se comportan como antes (las fechas imposibles, como `31/02/2020`, se siguen
  rechazando). `moment` estaba empaquetado dentro del bundle de AWE y nunca fue una variable global, por lo que solo
  afecta al código que lo importa desde el paquete `awe-client-angular`. **Obsolescencia:** no confíe en que `moment` o
  `lodash` vengan con AWE; añádalos a su propio `package.json` si los usa. AWE puede cambiarlos o retirarlos de nuevo
  sin una versión mayor.
- **Se eliminaron paquetes sin uso** de ambos clientes (consulte la fila de la tabla). El cliente React sigue instalando
  `react-transition-group` a través de `primereact`, pero impórtelo solo si lo declara usted mismo.
- **Los restos de Karma han desaparecido** del cliente AngularJS (specs de Jasmine que nadie ejecutaba, los entornos
  Babel de Istanbul y los paquetes `babel-plugin-istanbul`, `exports-loader` y `@babel/types`). La cobertura la
  proporciona Jest.
- Las actualizaciones mayores que requieren su propia migración no forman parte de esta limpieza: AngularJS 1.5,
  `angular-ui-router` 0.4, `angular-ui-grid` (la 4.12.7 cambia el punto de entrada del paquete), Highcharts y
  jQuery/Bootstrap (#755, #772), Redux Toolkit 2 (#807) y React 19.

### Java 21 y Spring Boot 4.1 {#java-21-and-spring-boot-41}

La incidencia #783 lleva AWE 5 a Java 21, Spring Boot 4.1.1 (Spring Framework 7.0, Spring Security 7.0) y Jackson 3.

- **Se necesita JDK 21** para compilar y ejecutar. Establezca `java.version` a 21 en su `pom.xml` (los arquetipos de
  AWE 5 ya lo hacen). El build también debe ejecutarse con un JDK 21: con un JDK anterior la compilación falla.
- **Spring Boot 4.1 elimina todo lo que la 4.0 marcó como obsoleto**: los nombres antiguos de starters
  (`spring-boot-starter-web`, `-aop`, `-oauth2-*`), `@MockBean`/`@SpyBean` y `spring-boot-jackson2`. Use los starters
  modulares (`spring-boot-starter-webmvc`, `-aspectj`, `-security-oauth2-*`) y los starters de prueba explícitos. Las
  recetas `UpgradeSpringBoot_4_0` y `SpringBootProperties_4_1` de [OpenRewrite](https://docs.openrewrite.org) hacen
  la mayor parte del trabajo; revise su resultado.
- Clases que Spring Boot 4 movió y que las aplicaciones pueden usar directamente: `ErrorController`
  (`org.springframework.boot.webmvc.error`), la autoconfiguración de JMS (`org.springframework.boot.jms.autoconfigure`),
  la autoconfiguración del servidor de recursos OAuth2 (dividida en `OAuth2ResourceServerAutoConfiguration` y
  `OAuth2ResourceServerWebSecurityAutoConfiguration`) y `TestRestTemplate` (`org.springframework.boot.resttestclient`).
- `awe-spring-boot-starter` aporta ahora `spring-boot-starter-cache` y `spring-boot-jms`, y `awe-controller` aporta
  `spring-boot-starter-webmvc`, de modo que las aplicaciones que dependían de ellos de forma transitiva siguen
  funcionando.
- **Pruebas.** `@SpringBootTest` ya no proporciona MockMvc ni `TestRestTemplate`: añada `@AutoConfigureMockMvc` o
  `@AutoConfigureTestRestTemplate`. Los campos `@Mock` de Mockito en pruebas de Spring ya no se inicializan (Spring Boot 4
  eliminó `MockitoTestExecutionListener`) y quedan a `null`: añada `@ExtendWith(MockitoExtension.class)`, con
  `@MockitoSettings(strictness = Strictness.LENIENT)` para mantener los stubs permisivos. Con JUnit 6, una prueba que
  importa con comodín `org.junit.jupiter.api.*` y `org.springframework.http.*` ya no compila
  (`reference to MediaType is ambiguous`): importe `org.springframework.http.MediaType` de forma explícita.
- **Selenium** sigue el BOM de Spring Boot (4.43.0 con Spring Boot 4.1.1) en lugar de la 4.18.1 que fijaba AWE, y
  `awe-dependencies` ya no gestiona `selenium-server`.
- **`lombok-maven-plugin` desaparece.** AWE ejecutaba delombok (las fuentes del Javadoc) con este plugin abandonado.
  Ahora usa la ejecución `delombok` de `maven-antrun-plugin`, gestionada en `awe-dependencies`, con la versión de
  Lombok del BOM de Spring Boot. La propiedad `skip.delombok` sigue funcionando. Si su `pom.xml` declaraba
  `lombok-maven-plugin`, declare `maven-antrun-plugin` en su lugar (hereda la ejecución `delombok`) o elimínelo.
- **Spring Session con Redis necesita su starter** (#861). Spring Boot 4 movió la autoconfiguración de Spring Session
  a su propio módulo: solo con `spring-session-data-redis` la aplicación arranca sin ningún error, pero las sesiones se
  quedan en cada servidor y se pierden cuando una petición llega a otra instancia. Dependa en su lugar de
  `spring-boot-starter-session-data-redis` y renombre las propiedades `spring.session.redis.*` a
  `spring.session.data.redis.*`. Las clases de autoconfiguración de Redis también cambiaron: excluir
  `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` ya no excluye nada. Excluya
  `org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration` y
  `org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration` donde no haya Redis.
  Con Spring Session activo, la cookie de sesión es `SESSION`.
- Los constructores de `TaskConfig` y `SchedulerTaskConfig` reciben ahora también el `Environment`. Solo importa si su
  código crea estas clases de configuración por su cuenta.

### Jackson 3 {#jackson-3}

- **Cambio de paquete.** `com.fasterxml.jackson.core` y `com.fasterxml.jackson.databind` pasan a ser `tools.jackson.core`
  y `tools.jackson.databind`. Las anotaciones siguen en `com.fasterxml.jackson.annotation`. Todo método de AWE que recibe
  o devuelve `ObjectNode`, `JsonNode`, `ArrayNode` u `ObjectMapper` usa ahora el tipo de Jackson 3, así que el código que
  lo llama debe cambiar sus imports. La lista completa está en [API pública con Jackson 3](#jackson-3-public-api).
- `JsonProcessingException` pasa a ser `JacksonException`, que no es comprobada (*unchecked*).
- **Jackson 3 lanza una excepción donde Jackson 2 devolvía un valor por defecto** (medido con jackson-databind 3.2.1):

  | Llamada | Jackson 2 | Jackson 3 |
  |---|---|---|
  | `asText()` / `asString()` sobre un nodo array, objeto o POJO | `""` | lanza `JsonNodeException` |
  | `asText()` / `asString()` sobre `NullNode` | `"null"` | `""` |
  | `asInt()` / `asLong()` / `asDouble()` sobre un array o un texto no numérico | `0` | lanza excepción |
  | `asBoolean()` sobre un array o un texto no booleano | `false` | lanza excepción |
  | `intValue()` sobre un nodo no numérico | `0` | lanza excepción |

  AWE llama a las sobrecargas con valor por defecto (`asString("")`, `asInt(0)`, `asLong(0L)`, `asDouble(0.0)`,
  `asBoolean(false)`), que dan los resultados de Jackson 2 salvo el texto de `NullNode` (`""` en lugar de `"null"`).
  Haga lo mismo en su propio código o gestione la excepción.
- `ObjectMapper.writer(PrettyPrinter)` se eliminó: use `writer().with(prettyPrinter)`. `MappingJackson2HttpMessageConverter`
  pasa a ser `JacksonJsonHttpMessageConverter`. El soporte de java.time forma parte de Jackson 3, así que ya no se
  registra `JavaTimeModule`.
- Spring Boot 4 autoconfigura un `JsonMapper`. Para sustituirlo, defina un bean `JsonMapper` (un bean `ObjectMapper` ya
  no basta). Spring Boot 4 registra además todos los módulos de Jackson del classpath; defina
  `spring.jackson.find-and-add-modules=false` para evitarlo.
- java-jwt 4 sigue dependiendo de Jackson 2, de modo que Jackson 2 permanece en el classpath. No pase tipos de Jackson 2
  a las API de AWE.

### Contrato JSON {#json-contract}

AWE 5 mantiene el **formato JSON de AWE 4** sobre Jackson 3 y pasa a ser estricto con el contenido sobrante. AWE aplica
su contrato al `JsonMapper` de Spring Boot mediante un `JsonMapperBuilderCustomizer` (`DataListUtil.applyAweJsonContract`,
que también usa el mapper de `DataListUtil`), así que la capa HTTP y el mapper inyectado lo usan.

| Comportamiento | Por defecto en Jackson 3 | AWE 5 |
|---|---|---|
| Enumerados | se escriben y leen con `toString()` (`MaintainType.INSERT` -> `"I"`) | **por nombre** (`"INSERT"`), como en AWE 4 |
| Orden de las propiedades | alfabético | **orden de declaración**, como en AWE 4 |
| `null` para un campo primitivo | error | **se acepta** (valor por defecto), como en AWE 4 |
| Fechas, duraciones, propiedades desconocidas, inclusión de vistas | valores por defecto de Jackson 3 | comportamiento de Spring Boot 3 mediante `spring.jackson.use-jackson2-defaults=true` (definido en el `base.properties` de AWE) |
| Contenido tras el valor JSON raíz (`{...},`) | se rechaza | **se rechaza con HTTP 400** (AWE 4 lo ignoraba) |

- **Los clientes que envían contenido sobrante** tras el cuerpo JSON reciben ahora HTTP 400 y deben corregirse.
- **Clientes de AWE con Jackson 3.** El formato no cambió, así que los clientes con Jackson 2 siguen funcionando. Un
  cliente con los valores por defecto de Jackson 3 (por ejemplo un `new RestTemplate()` sin configurar con Spring 7) lee
  los enumerados con `toString()` y falla con `Cannot deserialize value of type AnswerType from String "OK"`.
  Configúrelo con el mapper de AWE: `MessageConverterUtil.withJsonMapper(restTemplate, DataListUtil.getMapper())`, o
  desactive `EnumFeature.READ_ENUMS_USING_TO_STRING`. `spring.jackson.use-jackson2-defaults` no ayuda aquí, porque un
  `RestTemplate` sin configurar no usa el mapper de Spring Boot. Los conectores REST y de microservicios de AWE ya lo
  hacen.
- Para cambiar el contrato, añada su propio `JsonMapperBuilderCustomizer` o propiedades `spring.jackson.*`; un
  customizer que se ejecuta después del de AWE lo sobrescribe.
- El documento OpenAPI de la API REST de AWE no cambia (AWE mantiene el esquema de `JsonNode` como `{}` para springdoc).

### Spring Security 7 {#spring-security-7}

- **Las redirecciones del inicio de sesión son relativas.** Una petición de navegador sin autenticar recibe
  `Location: /` en lugar de `Location: http://host/` (valor por defecto de Spring Security 7). Los proxies inversos que
  reescriben cabeceras `Location` absolutas no se ven afectados; lo que analizara la URL absoluta debe aceptar una
  relativa.
- `AuthorizationManager.authorize` recibe un `Supplier<? extends Authentication>`; actualice sus propias
  implementaciones.
- `DaoAuthenticationProvider` exige el `UserDetailsService` en su constructor.
- Spring Boot 4 movió la seguridad web de servlets a `ServletWebSecurityAutoConfiguration`; las pruebas que enumeran
  explícitamente las autoconfiguraciones de seguridad (por ejemplo con `ApplicationContextRunner`) deben añadirla.

### Logging, base de datos y web {#logging-database-and-web}

- **Logback.** Spring Boot 4 eliminó `org.springframework.boot.logging.logback.ApplicationNameConverter`. Una
  configuración de Logback que todavía declara `<conversionRule conversionWord="applicationName" .../>` detiene la
  aplicación al arrancar. `awe-log.xml` y `scheduler-log.xml` de AWE usan `%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}`
  (misma salida); un `logback-spring.xml` copiado de AWE 4 o de Spring Boot 3 debe hacer lo mismo.
- **Límites de distribución de las métricas.** Un `management.metrics.distribution.maximum-expected-value.http.server.requests`
  inferior a 2 minutos, definido sin `minimum-expected-value`, hace ahora que **todas las peticiones HTTP fallen con 500**
  (`InvalidConfigurationException: maximumExpectedValue ... must be equal to or greater than minimumExpectedValue`): el
  prefijo también alcanza al long task timer `http.server.requests.active`, cuyo mínimo por defecto es de 2 minutos.
  Defina ambos límites, por ejemplo `minimum-expected-value.http.server.requests=1ms`.
- **H2 2.4 rechaza el tipo `DATETIME`** (Spring Boot 4.1 gestiona H2 2.4). El script H2 de AWE
  `AWE_V1.0.0__Init_awe_schema.sql` declara ahora `HISAweAppPar.HISdat` como `TIMESTAMP`, por lo que cambia su checksum de
  Flyway: vuelva a crear su base de datos H2 local (o ejecute `flyway repair`). H2 solo está pensada para desarrollo local y
  pruebas. Sus propios scripts H2 que usen `DATETIME` deben usar `TIMESTAMP`.
- **Flyway 11 eliminó `cleanOnValidationError`**: `spring.flyway.clean-on-validation-error` no tiene efecto.
- La subida de ficheros usa `commons-fileupload2-jakarta-servlet6` (paquete
  `org.apache.commons.fileupload2.jakarta.servlet6`).
- En despliegues war en un contenedor externo, `server.forward-headers-strategy` no tiene efecto (Spring Boot 4); defina un
  bean `ForwardedHeaderFilter` si lo necesita.
- **jasypt-spring-boot 3.0.5** sigue descifrando las propiedades `ENC(...)`, pero todavía no declara soporte de Spring
  Boot 4: compruébelo cuando lo actualice.

### Transacciones de maintain {#maintain-transactions}

La incidencia #781 ejecuta cada maintain dentro de una transacción de su datasource (`awe.maintain.transaction.enabled`,
`true` por defecto). Respecto a AWE 4:

- Una sentencia que falla, o un paso que devuelve un resultado `ERROR` sin lanzar excepción, deshace el maintain
  **completo**. En AWE 4 un resultado `ERROR` sin excepción se confirmaba.
- Los maintains lanzados desde un servicio Java (`serve`) y `retrieve-data` sobre el mismo datasource **se unen** al
  maintain exterior. En AWE 4 usaban otras conexiones: un maintain anidado seguía confirmado cuando el exterior
  fallaba, y `retrieve-data` no veía las filas que el maintain acababa de escribir.
- Un maintain lanzado dentro de una transacción de Spring de la aplicación (`@Transactional`, `TransactionTemplate`)
  sobre el mismo datasource **se une a ella**. En AWE 4 siempre confirmaba en su propia conexión.
- Un servicio Java que captura el fallo de un maintain anidado y devuelve `OK` hace ahora que el maintain exterior
  falle con el error genérico de maintain, y se deshace todo.
- `<commit/>` sigue confirmando a mitad del maintain.
- **Tests.** En un test `@Transactional` los maintains se unen ahora a la transacción del test y se deshacen con ella.
  Un test que lee o limpia con su propia conexión (`dataSource.getConnection()`) no ve sus filas, y en HSQLDB espera
  indefinidamente al bloqueo. Lea a través de la transacción: `JdbcTemplate`, o
  `new TransactionAwareDataSourceProxy(dataSource).getConnection()`. Un test que deba comprobar lo que confirma un
  maintain no debe ser `@Transactional`.

Defina `awe.maintain.transaction.enabled=false` para recuperar el comportamiento de AWE 4. Consulte
[Transacciones](../api/maintain-definition.md#transactions).

### Utilidades de fecha {#date-utilities}

La incidencia #782 divide `DateUtil` por responsabilidades en el paquete `com.almis.awe.model.util.data.date`, que
trabaja con `java.time`. `DateUtil` se mantiene como fachada con los mismos métodos, resultados y clases en tiempo de
ejecución que en AWE 4.

| Clase | Responsabilidad |
|---|---|
| `DateFormats` | Los formatos de fecha y hora (las constantes de `DateUtil` apuntan a ellos). |
| `DateTimeParser` | Texto a `LocalDate`, `LocalTime` y `LocalDateTime`; `parseAnyFormat` detecta el formato. |
| `DateTimeFormatting` | Valores `java.time` a texto. |
| `DateTextConverter` | Texto de un formato a otro (tolerante: una entrada no válida se devuelve sin cambios). |
| `DateFormatDetector` | En qué formato está escrito un texto. |
| `ElapsedTimeFormatter` | Tiempo transcurrido legible ("hace 3 días"). |
| `LegacyDateConverter` | Tipos `java.util.Date` y `java.sql` a y desde `java.time`. |

Los métodos de `DateUtil` cuya firma usa `java.util.Date` o `Calendar` están deprecados; su Javadoc indica el
reemplazo. Los métodos que usan tipos `java.sql` (`web2SqlDate`, `sqlDat2WebDate`...) se mantienen para el código
JDBC. Algunos métodos deprecados devuelven un `java.sql.Timestamp` o `java.sql.Time` declarado como `Date`
(`web2Timestamp`, `web2Time`, `sql2JavaDate`, `jsonDate`, `getDateWithTimeFromCriteria`, `addTimeToDate`); lo siguen
haciendo.

Se corrige un comportamiento: el tiempo transcurrido (`DateUtil.elapsedTime`, `dateSince` y las transformaciones
`ELAPSED_TIME` y `DATE_SINCE`) cuenta ahora las horas en formato de 24 horas. AWE 4 mostraba 13 horas como "1 hora" y 12 horas
en milisegundos.

En el planificador, `TaskDAO` mantiene sus métodos y delega en las clases de `com.almis.awe.scheduler.dao.task`.
`TaskDAO.getProgress(Date, long)` y `TaskDAO.getElapsedTime(Date)` están deprecados en favor de sus sobrecargas con
`Instant`; una llamada con un `null` literal necesita ahora un cast.

### MenuService {#menuservice}

La incidencia #782 divide `MenuService` por responsabilidades en el paquete `com.almis.awe.service.menu`.
`MenuService` mantiene sus 33 métodos públicos, con las mismas firmas y resultados, y delega cada uno en un
colaborador. Los métodos que `Services.xml` invoca por nombre (`getNameOptionList`, `getModuleList`, `refreshMenu`,
los servicios del árbol de restricciones...) siguen en `MenuService`.

| Colaborador | Responsabilidad | Métodos de `MenuService` que delegan en él |
|---|---|---|
| `MenuProvider` | Cargar los menús y aplicarles restricciones y favoritos. | `getMenu`, `getMenuWithRestrictions`, `getMenuWithAllRestrictions`, `refreshMenu` |
| `MenuOptionLookup` | Buscar opciones y comprobar si están disponibles. | `getOptionByName`, `getAvailableOptionByName`, `checkOptionAddress`, `isAvailableOption` |
| `MenuScreenResolver` | Resolver la pantalla de un menú u opción y lanzar sus cargas iniciales. | `getDefaultScreen`, `getOptionScreen`, `getAvailableOptionScreen`, `getScreen` |
| `MenuSuggestionLists` | Listas de sugerencias de pantallas, opciones y módulos. | `getAvailableScreenList`, `getAllScreenList`, `getNameOptionList`, `getModuleList` |
| `MenuRestrictionTreeService` | Restricciones de perfil y árbol de restricciones por usuario o perfil. | `getScreenRestrictions`, `getMenuOptionTree`, `getMenuOptionTreeByModule`, `updateMenuRestrictionTree`, `allowMenuOption`, `restrictMenuOption`, `removeRestriction` |

Notas para los proyectos que extienden `MenuService`:

- **Un override solo afecta a quien llama directamente.** En AWE 4 los métodos de `MenuService` se llamaban entre
  sí, de modo que sobrescribir `getOptionByName` cambiaba también `getOptionScreen`, y sobrescribir `getScreen`
  cambiaba las listas de sugerencias. Ahora los colaboradores se llaman entre sí, no a `MenuService`: su override lo
  usa el código que llama directamente a `MenuService` (controladores, `Services.xml`, sus propios servicios), pero
  no los demás métodos de `MenuService`.
- **Personalice el colaborador.** Cada colaborador es un bean declarado con `@ConditionalOnMissingBean`. Declare su
  propio bean del mismo tipo (normalmente una subclase) y AWE lo usa en todas partes, también en `MenuService`:

  ```java
  @Bean
  public MenuOptionLookup menuOptionLookup(MenuProvider menuProvider, BaseConfigProperties baseConfigProperties) {
    return new MyMenuOptionLookup(menuProvider, baseConfigProperties);
  }
  ```

- **Constructores.** El constructor de siete argumentos (`QueryService`, `ScreenComponentGenerator`...) está
  deprecado pero sigue funcionando: construye sus propios colaboradores por defecto, que comparten el contexto de
  aplicación de su `MenuService`, por lo que no ven los beans de colaborador que usted declare. Pase las subclases de
  `MenuService` al constructor que recibe los cinco colaboradores. No se elimina nada antes de la 6.0.

### SQLBuilder {#sqlbuilder}

La incidencia #782 traslada la lógica pura de `SQLBuilder` al paquete `com.almis.awe.service.data.builder.sql`. A
diferencia de `MenuService`, `SQLBuilder` se divide sin cambiar cómo se llaman sus métodos entre sí, porque los
proyectos sustituyen los beans `SQLQueryBuilder` y `SQLMaintainBuilder` por sus propias subclases:

- Todos los métodos públicos y protegidos de `SQLBuilder` mantienen su firma, y los métodos protegidos se siguen
  llamando entre sí en el mismo orden. Un override de `getOperandExpression`, `getVariableAsExpression`,
  `getStringExpression`, `buildPath` o cualquier otro método protegido se sigue aplicando donde se aplicaba en AWE 4.
- Solo se ha movido lógica que no llama a ningún método sobrescribible: los métodos privados de los filtros y el
  cuerpo de algunos métodos protegidos. Los métodos protegidos siguen ahí y delegan en las clases auxiliares.
- El SQL generado y las clases de los valores enlazados no cambian: una variable `DATE` se sigue enlazando como
  `java.sql.Date` y una variable `TIMESTAMP` como `java.sql.Timestamp`. Las operaciones de fecha (`ADD_DAYS`,
  `SUB_MONTHS`...) y la función `TRUNCDATE` siguen tipadas como `java.util.Date`, que determina cómo las lee el driver
  JDBC.

| Clase auxiliar | Responsabilidad | Procedencia en `SQLBuilder` |
|---|---|---|
| `SqlFilterConditions` | La condición de un filtro (`eq`, `in`, `like`, `exists`...) y la unión de un grupo de filtros. | Métodos privados `applyFilterCondition`, `getFilterEq`, `getFilterIn`, `getFilterLike`, `getFilterExists`, `getBooleanExpressionResults` |
| `SqlOperations` | La expresión de una operación (`CONCAT`, `ADD`, `ADD_DAYS`, `SUBSTRING`...). | Cuerpo del método protegido `generateOperationExpression` |
| `SqlFieldFunctions` | Las funciones y los casts de un campo. | Cuerpos de los métodos protegidos `getExpressionFunction` y `getExpressionCast`; el error de valor no soportado sigue en `SQLBuilder` |
| `SubstringValidator` | El número de operandos y el tipo de los índices de `SUBSTRING`. | Métodos privados `validateIntegerCompatibleOperand`, `validateConstantIntegerOperand`, `validateFieldIntegerOperand`; `validateSubstringOperation` se mantiene y lanza el error localizado |
| `SqlListValues` | Qué valores de una variable de lista enlaza un filtro, y la unión de `LIST_TO_STRING`. | Parte de los métodos privados `convertVariableListToExpression` y `getListAsStringExpression`, método privado `addStringToListIfValid` |
| `SqlPaths` | La ruta de un único nodo. | Cuerpo del método protegido `buildPath(String)` |
| `SqlDateValues` | El valor de `SYSTEM_TIME`, leído de un `Clock`, y el valor de `TIMESTAMP`. | Código de `getVariableAsExpression` y `getVariableAsExpressionOrNull` |

Notas para los proyectos que extienden `SQLQueryBuilder` o `SQLMaintainBuilder`:

- **Siga sobrescribiendo los métodos protegidos.** Las clases auxiliares son clases `final` con métodos estáticos: no
  son puntos de extensión. Para cambiar un operador, una función o un cast, sobrescriba `generateOperationExpression`,
  `getExpressionFunction` o `getExpressionCast` como antes; una llamada a `super` devuelve la misma expresión que en
  AWE 4.
- **Revise las llamadas por reflexión.** Los métodos privados de la tabla ya no existen en `SQLBuilder`. Una subclase
  que los invoque por reflexión falla con `NoSuchMethodException`; llame en su lugar al método protegido que los usa
  (`getFilterGroups` para los filtros, `getOperationExpression` para `SUBSTRING`).
- **Nada más cambia.** Los constructores, la declaración de los beans (`@ConditionalOnMissingBean`, ámbito
  prototype) y los métodos de join que `SQLQueryBuilder` invoca por reflexión no cambian.

### API pública con Jackson 3 {#jackson-3-public-api}

Los miembros públicos y protegidos de los módulos del framework de AWE 5 cuya firma usa un tipo de Jackson 3
(`tools.jackson.core` o `tools.jackson.databind`). El código que llama a uno de ellos, lo sobrescribe o extiende su clase
con tipos de Jackson 2 debe pasar a los de Jackson 3. Los tipos se muestran por su nombre simple; la lista se obtuvo de
las clases compiladas, por lo que incluye los getters y setters que genera Lombok.

<details>
<summary>Mostrar la lista (204 miembros)</summary>

**`awe-controller`**

```java
// com.almis.awe.controller.ActionController
  List<ClientAction> launchAction(String, ObjectNode)
  List<ClientAction> launchAction(String, String, ObjectNode)
// com.almis.awe.controller.FileController
  ResponseEntity<String> getFileAsText(ObjectNode)
  ResponseEntity<FileSystemResource> getFileAsStream(ObjectNode)
  ResponseEntity<FileSystemResource> getFileAsStreamPOST(ObjectNode, String)
  ResponseEntity<byte[]> downloadFile(ObjectNode)
  ResponseEntity<byte[]> downloadFileMaintain(ObjectNode, String)
  ServiceData deleteFile(ObjectNode)
// com.almis.awe.controller.ScreenDataController
  ScreenData getDefaultScreenData(ObjectNode)
  ScreenData getOptionScreenData(String, ObjectNode)
// com.almis.awe.controller.UploadController
  UploadController(FileService, BroadcastService, ObjectMapper, BaseConfigProperties)
// com.almis.awe.security.authentication.filter.JsonAuthenticationFilter
  JsonAuthenticationFilter(AweElements, ObjectMapper)
// com.almis.awe.service.ChartService
  ChartService(ObjectMapper, BaseConfigProperties)
// com.almis.awe.service.EmailService
  Future<ServiceData> sendEmail(String, ObjectNode)
// com.almis.awe.service.MaintainService
  ServiceData launchMaintain(String, ObjectNode)
  ServiceData launchMaintain(String, ObjectNode, DatabaseConnection, boolean)
  ServiceData launchPrivateMaintain(String, ObjectNode)
  ServiceData launchPrivateMaintain(String, ObjectNode, DatabaseConnection, boolean)
  DatabaseConnection getDatabaseConnection(ObjectNode)
  ServiceData launchMaintain(Target, ObjectNode, DatabaseConnection, boolean)
// com.almis.awe.service.QueryService
  ServiceData launchQuery(String, ObjectNode)
  ServiceData launchQuery(String, ObjectNode, boolean)
  ServiceData launchPrivateQuery(String, ObjectNode)
// com.almis.awe.service.connector.AbstractRestConnector
  protected AbstractRestConnector(ClientHttpRequestFactory, ObjectMapper)
// com.almis.awe.service.connector.JavaConnector
  JavaConnector(ObjectMapper)
// com.almis.awe.service.connector.MicroserviceConnector
  MicroserviceConnector(ClientHttpRequestFactory, QueryUtil, ObjectMapper, RestConfigProperties)
// com.almis.awe.service.connector.RestConnector
  RestConnector(ClientHttpRequestFactory, ObjectMapper, RestConfigProperties)
// com.almis.awe.service.data.builder.AbstractQueryBuilder
  protected ObjectNode parameters
  protected void addComponentSort(ArrayNode)
  protected String getVariableAsString(JsonNode)
  ObjectNode getParameters()
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.EnumBuilder
  ArrayNode getEnumeratedAsJson()
  ArrayNode getOptionListAsJson(EnumeratedGroup)
  ObjectNode getJsonRow(Global)
// com.almis.awe.service.data.builder.QueueBuilder
  QueueBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.SQLMaintainBuilder
  SQLMaintainBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.SQLQueryBuilder
  SQLQueryBuilder setComponentSort(ArrayNode)
  SQLQueryBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.XMLEmailBuilder
  ObjectNode getParameters()
  XMLEmailBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.connector.maintain.EmailMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.MaintainConnector
  abstract <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.MaintainLauncher
  ServiceData launchMaintain(MaintainQuery, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.QueueMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.SQLMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.ServiceMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.query.AbstractQueryConnector
  protected AbstractQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.service.data.connector.query.EnumQueryConnector
  EnumQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData launchEnum(String, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueryConnector
  abstract ServiceData launch(Query, ObjectNode)
  abstract ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueryLauncher
  ServiceData launchQuery(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueueQueryConnector
  QueueQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.SQLQueryConnector
  SQLQueryConnector(AweDatabaseContextHolder, QueryUtil, javax.sql.DataSource, BaseConfigProperties, AweElements, NumericService, EncodeService, DatabaseConfigProperties, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.ServiceQueryConnector
  ServiceQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.processor.CompoundColumnProcessor
  CompoundColumnProcessor(AweElements, BaseConfigProperties, Compound, Map<String, QueryParameter>, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.service.report.ReportDesigner
  ReportDesigner(QueryService, ObjectMapper, BaseConfigProperties)
  com.almis.ade.api.bean.input.PrintBean getPrintDesign(List<Element>, ObjectNode)
  com.almis.ade.api.bean.input.PrintBean getPrintDesign(List<Element>, ObjectNode, boolean)
  boolean hasSpreadsheetOnlyColumns(List<Element>, ObjectNode)
// com.almis.awe.service.report.ReportDesigner$1
  type: extends TypeReference<List<PrintColumnData>>
// com.almis.awe.service.report.ReportDesigner$2
  type: extends TypeReference<Map<String, Object>>
// com.almis.awe.service.report.ReportGenerator
  FileData generateReportFormat(com.almis.ade.api.fluid.engine.generic.TemplateExporterBuilder, String, String, String, ObjectNode)
// com.almis.awe.service.report.ReportGenerator$GeneratedScreenReportContext
  ReportGenerator$GeneratedScreenReportContext(List<FileData>, ObjectNode)
  ObjectNode parameters()
// com.almis.awe.service.screen.ScreenComponentGenerator
  Map<String, ScreenComponent> generateComponentMap(Screen, ScreenData, Menu, ObjectNode)
```

**`awe-model`**

```java
// com.almis.awe.config.ServiceConfig
  ObjectNode getRequestParameters()
  ObjectNode getMutableRequestParameters()
  JsonNode getRequestParameter(String)
  JsonNode getRequestParameter(String, ObjectNode)
  String getRequestParameterAsString(String, ObjectNode)
  void putRequestParameter(ObjectNode, String, JsonNode)
  void putRequestParameter(ObjectNode, String, String)
  void putPropagatedRequestParameter(String, JsonNode)
  void mergePropagatedRequestParameters(ObjectNode)
// com.almis.awe.model.component.AweRequest
  AweRequest(HttpServletRequest, HttpServletResponse, ObjectMapper)
  void setParameterList(ObjectNode)
  JsonNode getParameter(String)
  ObjectNode getParameterList()
  ObjectNode getParametersSafe()
  List<CellData> getParameterAsCellDataList(JsonNode)
  CellData getParameterAsCellData(JsonNode)
  ObjectNode getParameters()
  ObjectMapper getMapper()
  AweRequest setParameters(ObjectNode)
// com.almis.awe.model.component.PrototypeRequestBeanHolder
  ObjectNode getRequestDataSnapshot()
  void mergeRequestData(ObjectNode)
  ObjectNode getPendingPropagationSnapshot()
// com.almis.awe.model.component.RequestDataHolder
  ObjectNode getRequestData()
  void setRequestData(ObjectNode)
// com.almis.awe.model.dto.FilterColumn
  FilterColumn(ObjectNode)
// com.almis.awe.model.dto.QueryData
  ObjectNode getParameters()
  QueryData setParameters(ObjectNode)
// com.almis.awe.model.dto.QueryParameter
  QueryParameter(JsonNode)
  QueryParameter(JsonNode, boolean, ParameterType)
  JsonNode getValue()
  void setValue(JsonNode)
// com.almis.awe.model.dto.SortColumn
  SortColumn(ObjectNode)
// com.almis.awe.model.entities.Element
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.actions.ComponentAddress
  static ComponentAddress fromJson(JsonNode)
// com.almis.awe.model.entities.queries.Query
  String getQueryKeys(ObjectNode)
// com.almis.awe.model.entities.screen.Screen
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.Dialog
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.Window
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.chart.Chart
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.chart.ChartSeriePoint
  List<JsonNode> getPositionPoint()
  void setPositionPoint(List<JsonNode>)
// com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.grid.AbstractGrid
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.panelable.Panelable
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.pivottable.PivotSorter
  void addSorter(String, JsonNode)
  Map<String, JsonNode> getSorterMap()
  void setSorterMap(Map<String, JsonNode>)
  PivotSorter(Map<String, JsonNode>)
// com.almis.awe.model.entities.screen.data.AweThreadInitialization
  ObjectNode getParameters()
  AweThreadInitialization setParameters(ObjectNode)
// com.almis.awe.model.util.data.DataListUtil
  static JsonMapper$Builder applyAweJsonContract(JsonMapper$Builder)
  static ObjectMapper getMapper()
  static ArrayNode getColumnAsArrayNode(DataList, String)
// com.almis.awe.model.util.data.QueryUtil
  QueryUtil(BaseConfigProperties, DatabaseConfigProperties, ObjectMapper, PrototypeRequestBeanHolder)
  List<SortColumn> getSortList(ArrayNode)
  Map<String, QueryParameter> getDefaultVariableMap(ObjectNode)
  Map<String, QueryParameter> getVariableMap(List<Variable>, ObjectNode, Integer)
  Map<String, QueryParameter> getVariableMap(Query, ObjectNode)
  void addToVariableMap(Map<String, QueryParameter>, Query, ObjectNode)
  void addDataListToRequestParameters(DataList, ObjectNode)
  Map<String, QueryParameter> getVariableMap(MaintainQuery, ObjectNode)
  Map<String, QueryParameter> getVariableMap(MaintainQuery, ObjectNode, List<ServiceInputParameter>)
  ObjectNode getParameters()
  ObjectNode getParameters(ObjectNode)
  <T> ObjectNode getParameters(T)
  ObjectNode getParameters(String, String, String)
  ObjectNode getParameters(String)
  ObjectNode getParameters(ObjectNode, String, String, String)
  JsonNode getParameter(Variable, ObjectNode)
  JsonNode getParameter(String, JsonNode, ParameterType, String)
  JsonNode getRequestParameter(String, ObjectNode)
  JsonNode getRequestParameter(String)
  boolean variableIsList(Variable, ObjectNode)
  boolean isEmptyVariable(JsonNode)
  boolean isNullVariable(JsonNode)
// com.almis.awe.model.util.data.StringUtil
  static ArrayNode toArrayNode(String, String)
  static List<String> asList(JsonNode)
// com.almis.awe.model.util.web.MessageConverterUtil
  static RestTemplate withJsonMapper(RestTemplate, ObjectMapper)
```

**`awe-builder`**

```java
// com.almis.awe.builder.client.grid.AddRowActionBuilder
  AddRowActionBuilder(RowPosition, String, ObjectNode)
// com.almis.awe.builder.client.grid.RowsActionBuilder
  RowsActionBuilder(String, RowPosition, String, ObjectNode)
// com.almis.awe.builder.client.grid.UpdateCellActionBuilder
  UpdateCellActionBuilder(ComponentAddress, JsonNode)
// com.almis.awe.builder.client.grid.UpdateRowActionBuilder
  UpdateRowActionBuilder(String, ObjectNode)
  UpdateRowActionBuilder(String, String, ObjectNode)
```

**`awe-notifier`**

```java
// com.almis.awe.notifier.service.NotifierService
  NotifierService(QueryService, MaintainService, BroadcastService, ObjectMapper)
```

**`awe-rest`**

```java
// com.almis.awe.rest.controller.AweRestController
  AweRestController(QueryService, MaintainService, AweRequest, JWTTokenService, org.modelmapper.ModelMapper, ObjectMapper)
// com.almis.awe.rest.security.JWTAuthenticationEntryPoint
  JWTAuthenticationEntryPoint(ObjectMapper)
// com.almis.awe.rest.security.JWTAuthenticationFilter
  JWTAuthenticationFilter(AuthenticationManager, ObjectMapper, JWTTokenService)
  JWTAuthenticationFilter(AuthenticationManager, ObjectMapper, JWTTokenService, AweElements)
```

**`awe-scheduler`**

```java
// com.almis.awe.scheduler.dao.CalendarDAO
  CalendarDAO(org.quartz.Scheduler, QueryService, QueryUtil, ObjectMapper)
// com.almis.awe.scheduler.dao.TaskDAO
  ObjectNode getProgressNode(long)
  ObjectNode getStatusIcon(TaskExecution)
  ServiceData loadExecutionScreen(String, ObjectNode)
// com.almis.awe.scheduler.log.DatabaseExecutionLogStore
  ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.log.ExecutionLogStore
  abstract ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.log.FileExecutionLogStore
  ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.service.RemoteSchedulerService
  RemoteSchedulerService(SchedulerService, RemoteScheduler, ObjectMapper, boolean)
// com.almis.awe.scheduler.service.RemoteSchedulerService$1
  type: extends TypeReference<List<String>>
// com.almis.awe.scheduler.service.SchedulerService
  ServiceData loadExecutionScreen(String, JsonNode)
// com.almis.awe.scheduler.service.report.SchedulerEmailReportService
  SchedulerEmailReportService(QueryUtil, MaintainService, QueryService, ObjectMapper)
// com.almis.awe.scheduler.service.scheduled.MaintainJobService
  MaintainJobService(ExecutionService, MaintainService, QueryUtil, TaskDAO, ApplicationEventPublisher, ObjectMapper, ExecutionLogStore, java.time.Duration, boolean, java.net.URI, boolean, String, String, RestTemplate)
// com.almis.awe.scheduler.service.scheduled.MaintainJobService$1
  type: extends TypeReference<Map<String, Object>>
```

**`awe-tools`**

```java
// com.almis.awe.tools.controller.FileManagerController
  FileManagerController(FileManagerService, ServletContext, ObjectMapper)
// com.almis.awe.tools.service.FileManagerService
  JsonNode uploadFile(String, List<MultipartFile>)
  JsonNode fileOperation(ObjectNode)
  JsonNode error(Exception)
```

**`awe-notifier-spring-boot-starter`**

```java
// com.almis.awe.notifier.autoconfigure.NotifierConfig
  NotifierService notifierService(QueryService, MaintainService, BroadcastService, ObjectMapper)
```

**`awe-rest-spring-boot-starter`**

```java
// com.almis.awe.rest.autoconfigure.RestSecurityConfiguration
  RestSecurityConfiguration(PublicQueryMaintainAuthorization, AuthenticationManager, AuthenticationManager, UserDetailsService, ObjectMapper, AweRestConfigProperties, BaseConfigProperties, SecurityConfigProperties)
```

**`awe-scheduler-spring-boot-starter`**

```java
// com.almis.awe.scheduler.autoconfigure.SchedulerConfig
  RemoteSchedulerService remoteSchedulerService(SchedulerService, RemoteScheduler, ObjectMapper)
  MaintainJobService maintainJobService(ExecutionService, MaintainService, QueryUtil, TaskDAO, ApplicationEventPublisher, ObjectMapper, ExecutionLogStore, RestTemplate)
  SchedulerEmailReportService emailReportService(QueryUtil, MaintainService, QueryService, ObjectMapper)
  CalendarDAO calendarDAO(org.quartz.Scheduler, QueryService, QueryUtil, ObjectMapper)
```

**`awe-spring-boot-starter`**

```java
// com.almis.awe.autoconfigure.AweAutoConfiguration
  AweRequest aweRequest(HttpServletRequest, HttpServletResponse, ObjectMapper)
  ObjectMapper objectMapper()
  QueryUtil queryUtil(BaseConfigProperties, DatabaseConfigProperties, ObjectMapper, PrototypeRequestBeanHolder)
  ChartService chartService(ObjectMapper, BaseConfigProperties)
  ReportDesigner reportDesigner(QueryService, ObjectMapper, BaseConfigProperties)
  JavaConnector javaConnector(ObjectMapper)
  MicroserviceConnector microserviceConnector(ClientHttpRequestFactory, QueryUtil, ObjectMapper, RestConfigProperties)
  RestConnector restConnector(ClientHttpRequestFactory, ObjectMapper, RestConfigProperties)
  EnumQueryConnector enumQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceQueryConnector serviceQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.autoconfigure.AweWebSecurityConfig
  AweWebSecurityConfig(ApplicationContext, BaseConfigProperties, SecurityConfigProperties, AweSessionDetails, AweElements, ActionService, ObjectMapper, AccessService, ErrorPageService, MultiTenantOAuth2Config, MultiTenantFilter)
  JsonAuthenticationFilter jsonAuthenticationFilter(BaseConfigProperties, AweElements, ActionService, ObjectMapper)
// com.almis.awe.autoconfigure.JmsConfig
  QueueQueryConnector queueQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.autoconfigure.SQLConfig
  SQLQueryConnector sqlQueryConnector(AweDatabaseContextHolder, QueryUtil, javax.sql.DataSource, BaseConfigProperties, AweElements, NumericService, EncodeService, DatabaseConfigProperties, ObjectMapper)
```

</details>

## Próximamente en AWE 5 {#coming-in-awe-5}

Estas iniciativas están abiertas, etiquetadas para el hito 5.0.0 y se espera que requieran acciones por parte de las
aplicaciones. Se listan para que pueda planificar; cada una añadirá su propia entrada más arriba cuando se integre.

| Incidencia | Título |
|---|---|
| [#755](https://gitlab.com/aweframework/awe/-/issues/755) | Sustituir el Bootstrap 3 incluido por Tailwind y una capa de compatibilidad de AWE |
| [#775](https://gitlab.com/aweframework/awe/-/issues/775) | Migrar los gráficos de Highcharts a Apache ECharts con renderizado en el servidor |
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Sustituir angular-ui-grid por un componente de *grid* moderno |
| [#778](https://gitlab.com/aweframework/awe/-/issues/778) | Imponer un hash de contraseñas moderno y una clave maestra obligatoria |
| [#823](https://gitlab.com/aweframework/awe/-/issues/823) | Alinear los nombres de acciones del constructor y los valores muertos del XSD con el contrato del cliente |
| [#829](https://gitlab.com/aweframework/awe/-/issues/829) | Sanitizar el HTML de las celdas de grid en el cliente React (se descartará un `style` en línea en una celda; use una clase CSS) |

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
- [ ] The application builds and runs on JDK 21 with `java.version` 21
- [ ] Spring Boot 4.1 starters and test annotations replace the removed ones (`spring-boot-starter-web`, `@MockBean`...)
- [ ] Spring Session with Redis uses `spring-boot-starter-session-data-redis` and `spring.session.data.redis.*` properties
- [ ] Jackson imports moved to `tools.jackson.*`; `asText()`/`asInt()`/`asBoolean()` calls use the default-value overloads
- [ ] Clients of the AWE API send no content after the JSON body; Jackson 3 clients use the AWE mapper
- [ ] Own `logback-spring.xml` no longer declares the `applicationName` conversion rule
- [ ] Maintains that relied on partial commits were reviewed, or `awe.maintain.transaction.enabled=false` is set
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
