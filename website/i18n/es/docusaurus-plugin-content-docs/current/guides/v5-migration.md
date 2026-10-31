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
| Cliente React (`awe-react-client`) | `2.x`, línea de versiones propia | Misma versión que el framework (`5.y.z`) | Consulte [Actualización del cliente React](react-client-upgrade.md). React 19.3.1 (AWE 4: React 18). |
| Gráficos, motor React | Highcharts | Apache ECharts 6.1.0 | Consulte [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Gráficos, motor AngularJS | Highcharts | Apache ECharts 6.1.0 | El mismo constructor de opciones que el motor React. Consulte [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Renderizado de gráficos en el servidor (`ChartService`) | Servidor de exportación de Highcharts | Servidor de gráficos de AWE (Apache ECharts, Node) | Véase [Servidor de gráficos](../api/chart.md#chart-server). |
| Pruebas de navegador (`awe-testing`) | Selenium | Selenium por defecto, Playwright como piloto (`awe.test.tool`) | Consulte la [guía de pruebas con Selenium](selenium-test-guide.md#automation-tool-awetesttool). |
| Navegadores usados para probar el framework | Chrome, Firefox (Selenium) | Chromium y Firefox (Playwright), Chrome y Firefox (Selenium, programado) | Todavía no se declara en el repositorio una lista de navegadores de usuario final soportados. |

## Qué cambia {#what-changes}

Cada fila indica qué notará, qué debe hacer y dónde leer más. «MR» es una merge request del
[proyecto AWE](https://gitlab.com/aweframework/awe/-/merge_requests).

| Área | Síntoma | Qué hacer | Más información |
|---|---|---|---|
| Paquete del cliente React | La versión de `awe-react-client` en su `package.json` es `2.x`, o el `npm ci` de un proyecto 2.x falla tras la actualización. | Establezca `awe-react-client` en la versión exacta de su versión de AWE (por ejemplo `5.0.0`), ejecute `npm install` una vez y confirme el nuevo archivo de bloqueo. No mezcle un cliente 5.x con un servidor AWE 4. | [Actualización del cliente React](react-client-upgrade.md), [paquete npm](release-lines-and-support.md#npm-package) |
| React 19 | Un widget o componente React propio de su aplicación avisa o falla con React 19 (por ejemplo usa `defaultProps` en un componente de función, `findDOMNode`, refs de texto o `react-dom/test-utils`), o importaba `react-helmet`, que ya no llega con el cliente. | Actualice su código React y sus librerías a versiones compatibles con React 19, y declare `react-helmet-async` (o fije el título en un efecto) si su código usaba `react-helmet`. Mantenga `react` y `react-dom` en la versión que declara el cliente. | [Guía de actualización a React 19](https://react.dev/blog/2024/04/25/react-19-upgrade-guide) (#883) |
| Imágenes de Docker | Necesita una imagen de la aplicación de pruebas React. | `awe-boot-react` se construye y publica en `develop`, `master` y `support/*` con las mismas etiquetas que `awe-boot`. No existe la etiqueta `latest`: fije una versión. | [Imagen de la aplicación de pruebas React](release-lines-and-support.md#react-test-application-image) |
| Gráficos (motor React) | Los gráficos aparecen vacíos con un servidor AWE 4, una serie tiene un color distinto, los gráficos 3D son planos, las reglas CSS `.highcharts-*` no hacen nada, o un componente personalizado que importa Highcharts no compila. | Revise en el log del servidor los avisos `Highcharts chart-parameter`, defina colores y fuentes en el XML y declare Highcharts usted mismo si su propio código lo importa. | [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5), [actualización del cliente React](react-client-upgrade.md#charts-use-apache-echarts) (MR !854, !855, !856) |
| Gráficos (motor AngularJS) | Los gráficos 3D son planos, una serie tiene un color distinto, un gráfico bursátil tiene un deslizador en lugar del selector de rango y la barra de desplazamiento de Highstock, no hay botón Reset zoom, las opciones de Highcharts de `chart-parameter` o `chart-options` no hacen nada, las reglas CSS `.highcharts-*` no hacen nada, los scripts de Highcharts añadidos en `scripts.xml` (temas, plugins) fallan, o los tests de navegador que buscan elementos `.highcharts-*` fallan. | Revise en el log del servidor los avisos `Highcharts chart-parameter`, defina colores y fuentes en el XML, elimine los scripts de Highcharts y las reglas `.highcharts-*`, y espere a los gráficos con las instrucciones `getChart`/`getChartZoomed` de `awe-testing`. | [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5), [Cambios respecto a AWE 4](../api/chart.md#changes-from-awe-4-highcharts) (issue #795) |
| Atributo `theme` de los gráficos | Un gráfico con `theme="..."` se carga, pero los colores de su tema de Highcharts desaparecen; el código que llama a `setTheme` de `ChartBuilder` o de `Chart` no compila. | Elimine el atributo y las llamadas a `setTheme`. Los gráficos siguen el tema claro u oscuro de la aplicación; defina los colores con el `color` de las series o el parámetro `colors`, y las fuentes con `chart-parameter`. | [Estilo de un gráfico](../api/chart.md#styling-a-chart) (issue #795) |
| Renderizado de gráficos en el servidor (`ChartService`) | `renderChart` falla: el gráfico ya no se envía a un servidor de exportación de Highcharts y la URL por defecto (`https://export.highcharts.com`) desaparece. | Arranque el servidor de gráficos de AWE (imagen `registry.gitlab.com/aweframework/docker-tools/awe-chart-server:latest`) y configure `awe.application.chart-server-url`. La propiedad `awe.application.highcharts-server-url` se elimina y ya no se lee: renómbrela a `awe.application.chart-server-url`. | [Servidor de gráficos](../api/chart.md#chart-server) (issue #795) |
| Pruebas de navegador | El compilador avisa de que las sobrecargas de `By` y `getDriver()` están obsoletas; las pruebas dependen del orden de las clases. | Mueva los pasos personalizados a las sobrecargas de `Locator` y a `getBrowser()`; prepare la sesión en cada prueba con `ensureLoggedIn`/`ensureModule`. Para probar Playwright, establezca `awe.test.tool=playwright`. No se elimina nada antes de la 6.0. | [Compatibilidad de API de awe-testing](selenium-test-guide.md#api-compatibility-of-awe-testing), [clases de prueba independientes](selenium-test-guide.md#independent-test-classes), [pasos personalizados sin tipos de Selenium](selenium-test-guide.md#custom-steps-without-selenium-types) (MR !840, !841) |
| Dependencia `jsoup` | `org.jsoup:jsoup` ya no está en el classpath de `awe-model` y ya no lo gestiona el BOM `awe-dependencies`. | Si su propio código usa jsoup, declare la dependencia y su versión en su `pom.xml`. En caso contrario no hay que hacer nada. Consulte la nota bajo la tabla. | MR !849 |
| Caducidad de contraseña (`PwdExp`) | La aplicación define el parámetro `PwdExp`. Tras la actualización, los usuarios cuyo último cambio de contraseña es anterior a `PwdExp` días se les indica que la contraseña ha caducado (antes entraban) y tienen que establecer una nueva, y los que la cambiaron recientemente pueden volver a iniciar sesión (antes eran rechazados). | Nada si los datos son correctos: la comprobación ya no está invertida y el inicio de sesión guía al usuario para cambiar la contraseña (consulte la nota bajo la tabla). Revise los usuarios que nunca cambiaron la contraseña y el valor de `PwdExp`. | [Caducidad de contraseña](../security/authentication.md#password-expiration-local-login), incidencia #846 |
| JSON del menú | La carga del menú es más pequeña. | Nada, salvo que un cliente personalizado lea `elementList` de una `Option` del menú: lea `options` en su lugar. Consulte la nota bajo la tabla. | MR !792 |
| JSON de los componentes de pantalla | La respuesta de `screen-data` de las pantallas con contenedores anidados es mucho más pequeña. | Nada, salvo que un cliente personalizado lea `elementList` del `controller` de una entrada de `components`: lea los hijos de `structure` en su lugar. Consulte la nota bajo la tabla. | Issue #907 |
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
| Targets de maintain solo de servidor (#886, nuevo) | Una petición de cliente (`/action/maintain`, los endpoints de maintain de ficheros, `/api/maintain`) que lanza uno de los targets del framework que solo lanza Java (por ejemplo `addToFavourites`, `updateUserMode`, `store2faSecret`, `UpdPwdLck`, `UpdCntLog`, `insert-user-subscription`) falla ahora con el error de *lanzamiento de maintain sin autorización*, incluso con sesión. | Lance el target de cliente que lo envuelve (`clickFavourite`, `selectDarkMode`, `toggle-web-subscription`...) o llame al servicio Java. Marque los targets propios que solo lanza Java con `server-only="true"`. | [Targets de maintain solo de servidor](#server-only-maintain-targets), [Targets solo de servidor](../api/maintain-definition.md#server-only-targets) |
| Utilidades de fecha (#782) | El compilador avisa de que los métodos de `DateUtil` que reciben o devuelven `java.util.Date` o `Calendar` están deprecados. | Nada obligatorio: mantienen su comportamiento y no se eliminan antes de la 6.0. Pase a los métodos `java.time` de `com.almis.awe.model.util.data.date`. | [Utilidades de fecha](#date-utilities) |
| MenuService (#782) | Solo para proyectos que extienden `MenuService`: un método que sobrescriba ya no lo llaman los demás métodos de `MenuService` (por ejemplo, sobrescribir `getOptionByName` ya no cambia `getOptionScreen`). El compilador avisa de que el constructor de siete argumentos de `MenuService` está deprecado. | Nada si solo llama a `MenuService`. Si lo extiende, lleve la personalización a su propio bean del colaborador que contiene el método. | [MenuService](#menuservice) |
| Spring Session con Redis (#861) | Con varias instancias, los usuarios pierden la sesión al azar y las pantallas fallan tras el login (por ejemplo `Index 0 out of bounds for length 0`): la cookie de sesión es `JSESSIONID` en lugar de `SESSION`, porque las sesiones se quedan en cada servidor. | Sustituya `spring-session-data-redis` por `spring-boot-starter-session-data-redis`, renombre `spring.session.redis.*` a `spring.session.data.redis.*` y actualice las clases de Redis en `spring.autoconfigure.exclude`. | [Java 21 y Spring Boot 4.1](#java-21-and-spring-boot-41) |
| `TemplateService.getTagList(Screen, String)` (#861) | El código que llama directamente a este método no compila: ahora declara `throws AWENotFoundException`. Un tag list que no está en la pantalla lanza este error en lugar de un `IndexOutOfBoundsException`. | Capture o propague `AWENotFoundException` (o `AWException`). | issue #861 |
| SQLBuilder (#782) | Solo para proyectos que extienden `SQLQueryBuilder` o `SQLMaintainBuilder` e invocan por reflexión métodos privados eliminados de `SQLBuilder`. Los métodos protegidos mantienen sus firmas y se siguen llamando entre sí en el mismo orden. | Nada si solo sobrescribe métodos protegidos. Sustituya las llamadas por reflexión a los métodos privados eliminados por los métodos protegidos. | [SQLBuilder](#sqlbuilder) |
| Acciones del contrato de pantalla (#823) | El código Java que usa el enum `Action` de `awe-builder` envía ahora `check-records-generated`, `select-first-row` y `select-last-row` (antes enviaba nombres que ningún motor trataba, así que la acción no hacía nada). El motor AngularJS ejecuta ahora `toggle-class`, y el motor React muestra `target-message` como un popover sobre el componente destino, igual que AngularJS. El compilador avisa de que `Action.FORWARD`, `GET_LOCALS`, `CHECK_LEAF_SELECTED` y `CHECK_PARENT_SELECTED` están deprecados, y el IDE marca algunos valores del XML como deprecados. | Revise las pantallas en las que estas acciones se ejecutan ahora. Quite los valores deprecados: no hacen nada y se eliminan en la 6.0. | [Valores del contrato de pantalla](#screen-contract-values) |
| Hilos virtuales (#783, nuevo) | No cambia nada salvo que defina `spring.threads.virtual.enabled=true`. | Opcional: defina la propiedad para ejecutar Spring Boot y los ejecutores de tareas de AWE sobre hilos virtuales. | [Hilos virtuales](../deployment.md#virtual-threads) |
| Dependencias de los clientes (`moment`, `lodash`, herramientas de build) | El cliente AngularJS ya no incluye `moment` (usa `day.js`) y el cliente React ya no lista `@redux-devtools/extension`, `redux-thunk`, `react-transition-group`, `deep-freeze`, `autoprefixer`, `postcss-loader` ni `@types/*`. Su propio código importa uno de ellos y el build falla con `Cannot find module`. | Declare en su propio `package.json` el paquete que importa: AWE solo los instalaba como detalle de implementación y no los promete a las aplicaciones. `lodash` se mantiene, pero trátelo igual. Consulte la nota bajo la tabla. | [Dependencias de los clientes](#client-dependencies), incidencia #769 |
| Temas y modo oscuro (cliente AngularJS) | Las pantallas toman el aspecto AWE 5 del tema: un nombre de tema (`default`, `sunset`...) o un tema de base de datos es un preset de primario, superficie y modo, pintado en `<html>` (clase `dark` / `light`, `data-awe-primary`, `data-awe-surface`, `data-awe-density`). Los temas LESS clásicos, las reglas `.theme-<nombre>` y `/css/themeVariables.css` desaparecen (el servidor ya no lo sirve: la petición responde 404): el CSS de la aplicación que define las variables de tema de AWE 4 (`--primary-color`, `--panel-background-color`...) o da estilo a `.theme-<nombre>` no cambia nada, y la pantalla *Personalizar el tema* desaparece (el botón `Personalizar` de la pantalla *Temas*, la opción de menú `customize-theme`, el maintain `customizeTheme`, las queries `themeConfiguration` y `nextIdForThemeColors` y las claves de idioma que solo usaba esa pantalla, como `SCREEN_TITLE_THM_CUSTOM`): los colores que guardaba (tabla `AweThmCol`, conservada sin uso) no se aplican. Los usuarios que nunca eligieron un modo obtienen el de su sistema operativo. | Lea en su CSS los tokens `--awe-*` en lugar de las variables de tema y, si lo necesita, dé estilo al modo con `html.dark`. Dé a un tema sus colores con su primario, su superficie (paleta o hex) y su modo en las pantallas de alta y modificación de *Temas*, o con las propiedades `awe.application.theme-*`. Quite la opción `customize-theme` (pantalla `customize-themes`) del menú `private.xml` de su aplicación. Para ignorar el sistema operativo defina `awe.application.theme-mode=light` (o `dark`); `@awe-follow-os-color-scheme` ya no hace nada y la propiedad `awe.application.theme-follow-os-color-scheme` se elimina: quítela de su configuración. | [Design tokens y modo oscuro](design-tokens-guide.md), [Configuración del tema](../design-system/for-developers.md) (issues #770, #885) |
| Grid, paneles y utilidades de Bootstrap (cliente AngularJS, #755) | No cambia nada en una pantalla que escribe estas clases en el `style` de una pantalla XML o en una plantilla: el grid (`col-*`, `container`, `row`), los paneles (`panel*`) y las utilidades (`pull-*`, `hide`, `show`, `hidden*`, `visible-*`, `clearfix`, `center-block`, `text-hide`) las sirve ahora la capa de compatibilidad. Pero un LESS de producto que llamara a sus conjuntos de reglas de Bootstrap (`.pull-right;`, `.hidden;`, `.panel;`, `.row;`...) o los extendiera (`&:extend(.clearfix all)`, `&:extend(.pull-right)`) pierde esas declaraciones sin aviso (Less las descarta sin error), `.affix` ha desaparecido, y las clases necesitan un navegador con capas de cascada (Chrome 99, Firefox 97, Safari 15.4). Una regla propia gana ahora a estas clases sea cual sea su especificidad o el orden de los ficheros (por ejemplo `td { padding: 0 }` frente a `col-*` en una celda). Por eso las reglas del acordeón (`.panel-group`) y la regla base de las columnas siguen en LESS mientras se entregue el LESS de pixel-admin y de AWE. | Sustituye la llamada o la extensión por las declaraciones que necesites, o por el mixin (`.clearfix()` sigue existiendo), y escribe `.affix` tú mismo si lo usabas. El diff de píxeles de las pantallas de AWE antes y después de la eliminación es idéntico. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (issue #755) |
| Botones, grupos de botones, formularios y grupos de entrada de Bootstrap (cliente AngularJS, #755) | No cambia nada en una pantalla que escribe clases `btn*`, `form-*`, `input-group*`, `checkbox`, `radio` o `has-*`: las sirve la capa de compatibilidad y sus reglas vendorizadas de Bootstrap se eliminan (`buttons.less`, `button-groups.less`, `forms.less`, `input-groups.less`). Un LESS de producto que llamaba a sus conjuntos de reglas (`.btn-lg;`, `.btn-group-justified;`, `.form-inline();`) o los extendía (`&:extend(.btn-xs)`) pierde en silencio esas declaraciones (Less las descarta sin error): AWE conserva `.btn-lg()`, `.btn-group-justified()`, `.form-inline()` y los extends de tamaño de grupo en sus `*-leftovers.less`, pero sus llamadas a otros conjuntos eliminados se descartan. Las clases de compatibilidad están en una capa de cascada, así que cualquier regla suya sin capa les gana sea cual sea su especificidad; por la misma razón las reglas que ganaban a una de pixel-admin o AWE (colores de botón deshabilitado, `.btn:active`, tamaños de grupo) siguen sin capa en `libs/bootstrap-3.3.2/*-leftovers.less` mientras esos LESS se publiquen. | Sustituya la llamada o el extend por las declaraciones que necesite. No edite los ficheros de restos; se van con las reglas de pixel-admin en el slice 6. El pixel diff de las pantallas de AWE antes y después de la eliminación es idéntico. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (issue #755) |
| LESS de pixel-admin (cliente AngularJS, #755) | pixel-admin ya no es una librería vendorizada; sus reglas que usa AWE viven en `less/legacy/pixel-admin` (entrada `legacy/pixel-admin/pixel-admin.less`) y los ficheros sin uso se eliminan. El CSS compilado es idéntico, así que no cambia nada en una pantalla. | Un LESS de producto que importa `libs/pixel-admin-less/...` (variables o mixins) cambia la ruta a `legacy/pixel-admin/...`; un producto que solo compila sobre `awe.less` no necesita nada. | [Capa de compatibilidad con Tailwind](./tailwind-compat-guide.md) |
| Desplegables, navs, barra de navegación, modales, tooltip, popovers y animaciones de componentes de Bootstrap (cliente AngularJS, #755) | No cambia nada en una pantalla que escribe clases `dropdown*`, `caret`, `nav*`, `tab-content`, `tab-pane`, `navbar*`, `modal*`, `close`, `popover*`, `tooltip*`, `fade`, `collapse` o `collapsing`: las sirve la capa de compatibilidad y sus reglas vendorizadas de Bootstrap se eliminan (`dropdowns.less`, `navs.less`, `navbar.less`, `modals.less`, `close.less`, `popovers.less`, `tooltip.less`, `component-animations.less`). Un LESS de producto que llamaba a uno de los conjuntos de reglas que definían o lo extendía (`&:extend(.navbar-nav)`, `.fade;`, `.caret;`, `.modal-content;`, `.tooltip-inner;`...) pierde en silencio esas declaraciones (Less las descarta sin error). Las clases de compatibilidad están en una capa de cascada, así que una regla suya sin capa para la misma clase les gana sea cual sea su especificidad, y las reglas que ganaban a una de pixel-admin, de un tema o de AWE (el menú desplegable y sus enlaces, `.nav-tabs`, los enlaces de la barra, las flechas de los popovers...) siguen sin capa en `libs/bootstrap-3.3.2/*-leftovers.less` mientras esos LESS se publiquen. Los valores de las reglas eliminadas quedan congelados tal como compilaron: una variable LESS de Bootstrap que un producto sobrescribe con `modifyVars` (el tamaño de fuente de `.close`, `19.5px`, los colores activos de desplegables y navs, `#337ab7`) ya no los cambia, mientras que las propiedades CSS personalizadas (`var(--x, fallback)`) siguen el tema en ejecución. | Sustituya la llamada o el extend por las declaraciones que necesite. No edite los ficheros de restos; se van con las reglas de pixel-admin en el slice 6. Si una regla suya con `!important` debe ganar a una clase de compatibilidad, déjela sin capa (la capa de compatibilidad nunca pone `!important` en una clase que otra regla marca con `!important`). El pixel diff de las pantallas de AWE antes y después de la eliminación es idéntico. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (issue #755) |
| Tablas, etiquetas, insignias, wells, alertas, migas de pan, barras de progreso y paginación de Bootstrap (cliente AngularJS, #755) | No cambia nada en una pantalla que escribe clases `table*`, `label*`, `badge*`, `well*`, `alert*`, `breadcrumb*`, `progress*` o `pagination*`: la capa de compatibilidad las sirve y sus reglas vendorizadas de Bootstrap se eliminan (`tables.less`, `labels.less`, `badges.less`, `wells.less`, `alerts.less`, `breadcrumbs.less`, `progress-bars.less`, `pagination.less`). Un LESS de producto que llamaba o extendía uno de los conjuntos de reglas que definían (`&:extend(.label)`, `.well;`, `.alert;`, `&:extend(.table all)`...) pierde esas declaraciones sin avisar (Less las descarta sin error); los mixins (`.label-variant()`, `.alert-variant()`, `.progress-bar-variant()`...) siguen existiendo. Las clases de compatibilidad están en una capa de cascada, así que una regla propia sin capa para la misma clase gana a las suyas sea cual sea su especificidad, y solo 42 de las 263 reglas compiladas salieron de la compilación: el resto se queda sin capa en `libs/bootstrap-3.3.2/*-leftovers.less` mientras se entregue el LESS de pixel-admin y de AWE (las celdas y filas de `.table`, la paginación, las reglas base de etiqueta, insignia, alerta y well). La barra de progreso con rayas y su transición de ancho leen `--awe-motion-duration-slow`, así que dejan de moverse cuando el usuario pide movimiento reducido. El toast de mensajes de AWE 5, la barra de migas y el paginador del grid mantienen sus reglas de AWE. Los valores de las reglas eliminadas están congelados tal como compilaron: una variable LESS de Bootstrap que un producto sobrescribe con `modifyVars` (`@table-border-color`, `@label-default-bg`, `@alert-success-bg`...) ya no los cambia. | Sustituye la llamada o el extend por las declaraciones que necesites. No edites los ficheros de restos; se van con las reglas de pixel-admin. Revisa las pantallas que usan estas clases en claro y oscuro: leen los tokens `--awe-*` y pueden verse distintas de AWE 4. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (incidencia #755) |
| Pager, objeto media, jumbotron, carrusel y fuente de iconos de Bootstrap (cliente AngularJS, #755) | Se eliminan las reglas vendorizadas de Bootstrap de `pager`, `media*`, `jumbotron`, `carousel*` y la fuente Glyphicons Halflings (`glyphicon*`) (`pager.less`, `media.less`, `jumbotron.less`, `carousel.less`, `glyphicons.less`, los ficheros de la fuente, el `theme.less` opcional y el helper `.pre-scrollable`), y **ningún CSS de compatibilidad las sustituye**: nada del framework escribe estas clases. Una pantalla o plantilla de producto que escriba una de ellas pierde su aspecto, y su LESS que llamaba o extendía uno de sus conjuntos de reglas pierde esas declaraciones sin avisar (Less las descarta sin error). `list-group*`, `thumbnail` y `embed-responsive*` mantienen su aspecto: algo del framework aún las escribe (la pantalla de ajustes del notificador y el gestor de ficheros), así que la capa de compatibilidad las sirve y solo 2 de sus 83 reglas compiladas salieron de la compilación. | Aporta tu propio CSS para cada una de estas clases que escribas (o sustitúyelas por utilidades de Tailwind, un icono del conjunto Lucide o tu propia fuente). Nada que hacer si nunca las escribiste. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (incidencia #755) |
| Estilos base de Bootstrap (cliente AngularJS, #755) | `normalize.less`, `print.less`, `scaffolding.less`, `type.less` y las reglas de elemento de `code` vendorizados de Bootstrap los sirve la capa de compatibilidad (`normalize.css`, `print.css`, `scaffolding.css`, `type.css`, `code.css`), así que los títulos, los párrafos, las listas, los enlaces, las clases de texto y fondo contextuales, las citas y el código leen los tokens de AWE 5. 226 de sus 300 reglas compiladas salieron de la compilación y 74 se quedan en el LESS (`normalize-leftovers.less`, `print-leftovers.less`, `scaffolding-leftovers.less`, `type-leftovers.less`, `pre-leftovers.less`): `html`, las reglas de impresión que imprimen en negro sobre blanco y las reglas que una regla de pixel-admin o de AWE vencería. Los valores que salieron siguen a los tokens: los tamaños de los títulos siguen la escala tipográfica (33, 27, 23, 17, 13 y 12px pasan a 30, 24, 20, 18, 14 y 12px, un px menos en la densidad compacta), los márgenes de párrafos y listas son de 8px en lugar de 9px y los mayores de 20px en lugar de 18px, la barra de la cita es de 2px en lugar de 5px, y `.lead` pesa 400 en lugar de 300. Lo que una regla de pixel-admin o de AWE fija sobre el mismo elemento sigue ganando, como antes. | Nada que hacer salvo que tu LESS o tus pantallas dependieran de los tamaños, márgenes o colores exactos de los títulos de Bootstrap. Un LESS tuyo que llame o extienda un conjunto de reglas de `type.less` o `scaffolding.less` (salvo `.text-success`, `.text-info`, `.text-warning`, `.text-danger` y `.img-responsive`, que se quedan) pierde esas declaraciones sin error. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (incidencia #755) |
| Restos de LESS de Bootstrap 3 (cliente AngularJS, #755) | Las variables, mixins, columnas de rejilla y restos sin capa de Bootstrap 3 pasan de `libs/bootstrap-3.3.2` a `less/legacy` (entrada `legacy/legacy.less`, el antiguo `bootstrap.less`). El CSS compilado es idéntico, así que no cambia nada en una pantalla. | Un LESS de producto que importe `libs/bootstrap-3.3.2/...` (variables o mixins) cambia la ruta a `legacy/...`; un producto que solo compila sobre `awe.less` no necesita nada. | [Capa de compatibilidad con Tailwind](./tailwind-compat-guide.md) |
| Las clases de Bootstrap siguen los tokens AWE 5 (cliente AngularJS, #755) | Las clases de compatibilidad que sustituyen las reglas vendorizadas de Bootstrap 3 (grid, paneles, utilidades, botones, formularios, grupos de entrada, desplegables, navs, barra de navegación, modales, tooltip, popovers, tablas, etiquetas, insignias, wells, alertas, migas de pan, barras de progreso, paginación) leen los tokens `--awe-*` en lugar de los colores y tamaños en píxeles de Bootstrap, así que una pantalla que escribe esas clases sigue el tema, el modo y la densidad y **puede verse distinta** de AWE 4 (colores, espaciado, radios, sombras). Tokens nuevos: `--awe-border-width`, `--awe-border-width-strong`, `--awe-grid-container-sm`, `--awe-grid-container-md`, `--awe-grid-container-lg`. | Revise en claro y oscuro las pantallas que usan estas clases; defina el aspecto con el tema (`awe.application.theme-*`), no con CSS de la aplicación sobre estas clases. | [Tailwind y compatibilidad](tailwind-compat-guide.md) (issue #755) |
| JavaScript de Bootstrap (cliente AngularJS, #755) | El cliente ya no publica el JavaScript de Bootstrap 3: los diálogos, los paneles laterales, el diálogo de confirmación, el popover de los mensajes sobre un destino, los dropdown de usuario e información, el menú de desbordamiento de las pestañas y el acordeón funcionan con código nativo con el mismo marcado, las mismas clases y los mismos tiempos. No cambia nada en una pantalla de AWE. Una plantilla o un script de producto que dependiera del data-api de Bootstrap o de sus plugins jQuery (`data-toggle="modal"`, `"collapse"`, `"tab"`, `"tooltip"`, `"popover"`, `"buttons"`, `data-target`, `data-parent`, `data-dismiss="alert"`, `$(el).modal(...)`, `.popover(...)`, `.tooltip(...)`, `.collapse(...)`, `.dropdown(...)`, `.tab(...)`, `.button(...)`) deja de funcionar: esa función no existe y el atributo no hace nada. | Use el equivalente de AWE (consulte la sección siguiente): los componentes `awe-dialog`, `awe-side-panel` y `awe-confirm`, `AweModal`, `data-toggle="dropdown"`, `data-dismiss="modal"`, `awe-popover`, las pestañas y el acordeón de AWE. Elimine `bootstrap` de su propio `package.json` si solo estaba para AWE. | [El JavaScript de Bootstrap ya no se publica](#bootstrap-javascript-is-no-longer-shipped), [Tailwind y compatibilidad](tailwind-compat-guide.md) (issue #755) |
| Indicadores de carga (ambos clientes) | Un grid o un grid en árbol sin `icon-loading` muestra filas de relleno con la forma de sus columnas mientras cargan sus filas, en lugar del spinner. Una pantalla muestra una estructura de relleno mientras se abre; la primera pantalla de la página (al abrir la aplicación o recargarla) carga bajo la animación de inicio en su lugar. Los criterios muestran un relleno con su forma (etiqueta y campo, filas del textarea, checkbox o radio, opciones del picklist), los botones uno tan ancho como su etiqueta y los gráficos su área con una pista del tipo de gráfico. Los esqueletos solo aparecen tras 150 ms, así que las cargas rápidas no muestran nada, y el brillo se detiene con `prefers-reduced-motion: reduce`. Una recarga de grid con filas en pantalla también muestra las filas de relleno; con `icon-loading="skeleton-dim"` mantiene las filas atenuadas en lugar de sustituirlas. Los criterios, botones y gráficos con un `icon-loading` propio conservan su icono. Los valores `data-testid` de los indicadores no cambian, así que las esperas de los tests de navegador siguen funcionando. | Nada. Para mantener el spinner en un grid, defina `icon-loading="spinner"` en él; para mantenerlo en todos los grids, defina `awe.application.component.grid-icon-loading=spinner`. Para atenuar las filas en una recarga en lugar de mostrar el relleno, use `skeleton-dim` en el atributo o en la propiedad. | [Atributos del grid](../api/grids.md#general-attributes), [Propiedades](../properties#awe.application.component.grid-icon-loading) (issues #774, #856, #864) |
| Auditoría de seguridad (#779, nuevo) | Flyway aplica `AWE_V1.3.0__Security_audit_events.sql` y crea la tabla `AweSecAudEvt`; los eventos de seguridad se registran en ella y en el logger `awe.security.audit`; aparece la opción **Auditoría de seguridad** en el menú de seguridad. | Nada. Para desactivar la auditoría, establezca `awe.security.audit.enabled=false`; restrinja la nueva opción a los perfiles que puedan verla. Consulte la nota bajo la tabla. | [Auditoría de seguridad](#security-audit), [Página de auditoría de seguridad](../security/security-audit.md) |
| Hash de contraseñas (#778) | Las contraseñas nuevas se guardan como hashes BCrypt (`{bcrypt}$2a$...`, 68 caracteres) en lugar de hashes RIPEMD-160 (40 caracteres), y cada usuario se vuelve a hashear en su siguiente inicio de sesión correcto. Flyway aplica `AWE_V1.4.0__Password_hash_length.sql`. El XML propio que compara `l1_pas` con `STRING_HASH_RIPEMD160` deja de encontrar a los usuarios ya rehasheados, y los sistemas externos que leen `l1_pas` encuentran un formato que no conocen. | Amplíe `l1_pas` antes de la actualización si gestiona su propio esquema, escriba las contraseñas con `STRING_HASH_PASSWORD` y compruébelas en Java, y mantenga `awe.security.password-encoder=ripemd160` mientras un sistema externo siga leyendo `l1_pas`. Haga una copia de seguridad de `ope`: los usuarios rehasheados ya no pueden iniciar sesión en AWE 4. | [Hash de contraseñas y clave maestra](#password-hashing-and-master-key) |
| Clave maestra (#778) | La aplicación no arranca: `awe.security.master-key is required: it encrypts stored data ...`. | Defina `awe.security.master-key`. Una aplicación que nunca la definió debe definir el antiguo valor por defecto de AWE 4 para seguir descifrando sus datos almacenados y, después, planificar una rotación. | [Clave maestra](#master-key) |
| Barra de herramientas del grid | Todos los grids muestran una barra de herramientas con un botón de *Columnas* y un botón de *Maximizar*, y la migración Flyway `AWE_V1.3.1` crea una tabla nueva `AweUsrGrdCnf` (guarda la disposición de columnas de cada usuario). Las columnas se abren en un popover anclado al botón (`columns-panel="popover"`, el valor por defecto), con *Restaurar predeterminado*, *Cancelar* y *Guardar*; ya no hay *Mostrar todas*. *Guardar* y *Restaurar predeterminado* no muestran mensaje cuando funcionan, y *Guardar* cierra las columnas. En React, las cabeceras de grupo de un grid se calculan ahora a partir de las columnas visibles de cada grupo. | Nada para los descriptores, que siguen funcionando. Use `show-toolbar="false"` en los grids que no deben mostrar la barra de herramientas. Para mantener el diálogo modal, use `columns-panel="dialog"` en el grid o `awe.application.component.grid-columns-panel=dialog`. Tests de navegador: sustituya `clickGridColumnsShowAll` (obsoleto) por `showGridColumn` para cada columna oculta, y quite las comprobaciones del mensaje de éxito tras `clickGridColumnsSave` / `clickGridColumnsReset` (los pasos esperan a que termine el guardado). Las aplicaciones que no usan las migraciones Flyway de AWE deben crear `AweUsrGrdCnf` por su cuenta (vea el script de su base de datos en `db/migration`). | [Barra de herramientas](../api/grids.md#toolbar), issue #813 |
| Estado de la búsqueda avanzada (#790, nuevo) | Flyway aplica `AWE_V1.5.1__window_advanced_state.sql` y crea la tabla `AweUsrWinCnf` (y la clave `WinCnfKey`). Solo guarda, por usuario y pantalla, si el bloque avanzado en línea de una ventana se dejó abierto: las ventanas sin bloque `<advanced>` no la usan. El script se puede aplicar de nuevo. | Nada. Las aplicaciones que no usan las migraciones Flyway de AWE tienen que crear `AweUsrWinCnf` por su cuenta (ver el script de su base de datos en `db/migration`). | [Búsqueda avanzada](../api/window.md#advanced-search), issue #790 |
| Búsquedas guardadas (#790, nuevo) | Flyway aplica `AWE_V1.5.2__saved_searches.sql` y crea la tabla `AweSavSch` (y la clave `SavSchKey`). Guarda las búsquedas que los usuarios guardan en las ventanas con `saved-searches="true"`: búsquedas propias y búsquedas publicadas para un perfil. Las ventanas sin el atributo no la usan. El script se puede aplicar de nuevo. | Nada. Las aplicaciones que no usan las migraciones Flyway de AWE tienen que crear `AweSavSch` por su cuenta (ver el script de su base de datos en `db/migration`). Quién publica para un perfil lo decide el acceso a la opción de menú de `awe.saved-search.publish-option` (por defecto `profiles`). | [Búsquedas guardadas](../api/window.md#saved-searches), issue #790 |
| Iconos (ambos clientes) | Los iconos cambian de aspecto: cada nombre de Font Awesome (`icon="nombre"`) y cada nombre `mdi:` de la tabla de equivalencias se dibuja como un icono de línea de Lucide, un `svg` en línea que usa el sprite `icons/lucide-sprite.svg`, en lugar del elemento `fa fa-<nombre>` o de Material Icons (y, en React, en lugar de los PrimeIcons de los componentes de AWE). Los nombres sin equivalencia (iconos de marca como `rebel`) conservan su fuente. El CSS de la aplicación dirigido al glifo de la fuente (`.fa-<nombre>:before`, `content` o `font-size` sobre `.fa`, `.pi-*`) deja de aplicarse a los iconos con equivalencia. El elemento conserva el nombre del XML en `data-icon`, por lo que las pruebas de navegador que localizan iconos siguen funcionando. | Nada para los descriptores. Traslade el CSS que daba estilo al glifo al `svg.awe-icon` (toma el color del texto, `currentColor`, y los tokens `--awe-size-icon`). En pantallas nuevas use `icon="lucide:<nombre>"` con cualquier nombre de Lucide. | [Iconos](../api/icons.md), [Tabla de equivalencias](../api/icons.md#equivalence-table) (issue #885) |
| Sprite de iconos y configuración de seguridad | La página de inicio de sesión (y cualquier pantalla pública) no muestra iconos: `GET /icons/lucide-sprite.svg` responde `401` porque la aplicación construye su propia configuración de seguridad con su propia lista de recursos web públicos. | Añada `/icons/**` a los recursos web públicos de su configuración de seguridad, junto a `/css/**`, `/js/**` o `/fonts/**`. La configuración de seguridad de AWE ya lo permite. | issue #885 |
| Mensajes (cliente AngularJS) | Las alertas de la zona de mensajes son toasts en la esquina superior derecha, bajo la cabecera, teñidos con el color de su tipo, como en el cliente React. | Nada. Revise el CSS de la aplicación que posiciona o da estilo a las alertas de la zona de mensajes. | [Capas superpuestas](../design-system/components.md#overlays) (issue #885) |
| Diálogos y confirmaciones (cliente AngularJS) | Al abrirse una confirmación o un diálogo de pantalla, el foco del teclado pasa a él (a su primer control) y vuelve al control que lo abrió al cerrarse. Escape cierra ahora una confirmación (como su botón de cancelar: la acción se rechaza) y un diálogo con título (como su botón de cerrar); un diálogo sin título sigue sin cerrarse con Escape. Una prueba de navegador que pulsaba Escape esperando que no ocurriera nada, o que escribe en la página con un diálogo abierto, se comporta de otra forma. | Revise las pruebas de navegador que pulsan Escape con una confirmación o un diálogo abierto, o que dependen de que el foco siga en la página. | issue #885 |
| Altura de fila del grid (cliente AngularJS) | Las filas de los grids miden 43 px en la densidad cómoda y 32 px en la compacta (medían 27 px) y cambian cuando el usuario cambia la densidad. Un `row-height` definido en el XML se mantiene, y ahora se respeta también en los grids de árbol (lo ignoraban y usaban 27 px). | Nada. Defina `row-height` en un grid que deba mantener una altura fija y revise los grids de árbol que lo definen. | [Atributos del grid](../api/grids.md#general-attributes) (issue #885) |
| Pantallas de inicio de sesión, cambio de contraseña y 2FA (ambos clientes) | El inicio de sesión, el cambio de contraseña y de contraseña caducada y las pantallas de 2FA adoptan el diseño de login de AWE 5: una columna de formulario junto a un panel de marca (oculto en pantallas estrechas), controles grandes y botones de ancho completo. En AngularJS ya no se importa `pages/signin.less` de pixel-admin, por lo que el CSS de la aplicación que sobrescribe sus reglas puede dejar de aplicarse. En AngularJS `awe.application.paths.image-startup-background` y `awe.application.paths.image-startup-logo` siguen dando estilo a las pantallas de cambio de contraseña y de 2FA; para la página de inicio de sesión, consulte la fila siguiente. | Revise el CSS de la aplicación que da estilo a estas pantallas, en modo claro y oscuro. | [Página de login](../design-system/layout-and-shell.md#login-page) (issue #885) |
| Página de inicio de sesión configurable (ambos clientes, nuevo) | La página de inicio de sesión se personaliza con las propiedades `awe.application.login.*`: diseño (`split`, `form-only` o `card`), logo y logo en modo oscuro, imagen de fondo, título y subtítulo del formulario, pie sobre la imagen, texto del pie de página y "Powered by AWE 5". `awe.application.paths.image-startup-logo` y `awe.application.paths.image-startup-background` quedan obsoletas para la página de inicio de sesión: un valor distinto del valor por defecto se sigue usando cuando `awe.application.login.logo` o `awe.application.login.background` no están definidas. Las pantallas de cambio de contraseña y de 2FA de AngularJS las siguen usando. | Pase un `image-startup-logo` y un `image-startup-background` personalizados a `awe.application.login.logo` y `awe.application.login.background`, con una URL relativa a la raíz de la aplicación (sin el `../` inicial). Opcional: defina el resto de las propiedades `awe.application.login.*`. | [Página de login](../design-system/layout-and-shell.md#login-page), [Propiedades base de AWE](../properties.md#awe-base-properties) |
| Redimensionable (ambos clientes) | Arrastrar el tirador de un `<resizable>` funciona como un divisor: sigue al puntero y solo el elemento de al lado del tirador cede o toma el tamaño, así que los demás paneles del padre ya no encogen ni crecen con él (antes se repartían el cambio). Tras un arrastre ese elemento conserva su tamaño en el eje, y los demás elementos que se expanden absorben los cambios del tamaño de la ventana. En React, un redimensionable ya no divide sus hijos en paneles (#847): los hijos que dependían de un divisor entre ellos se apilan. | Nada en las pantallas con la estructura de AngularJS (un redimensionable alrededor de un panel, con su vecino al lado). En las pantallas React, ponga el segundo panel junto al redimensionable en lugar de dentro. | [Redimensionable](../api/resizable.md#how-a-drag-shares-the-space) (issue #892) |
| Modo oscuro (cliente React) | Las clases `layout-dark` y `layout-light` ya no se ponen en `<body>`: el modo es la clase `dark` o `light` de `<html>`, junto a `data-awe-primary`, `data-awe-surface` y `data-awe-density`. El CSS de la aplicación sobre `.layout-dark` deja de aplicarse. Desaparecen el `<link>` de la hoja de estilos del tema y los 18 temas compilados de PrimeReact: PrimeReact lee los tokens de AWE 5. | Dé estilo al modo con `html.dark` o, mejor, con los tokens `--awe-*`, que ya cambian con el modo. Quite las dependencias `add-class`/`remove-class` sobre `layout-dark` de su propia copia de `info-buttons.xml`. | [Design tokens y modo oscuro](design-tokens-guide.md), [Configuración del tema](../design-system/for-developers.md) (issue #885) |
| Pantallas de usuarios (ambos clientes) | La opción *Usuarios* del menú por defecto (y de los arquetipos) abre la nueva pantalla `user-admin`: un grid con búsqueda, filtros rápidos y acciones por fila y un panel lateral para crear y editar usuarios. Las pantallas anteriores `Usr`, `UsrNew`, `UsrUpd` y `UsrViw` están deprecadas y se eliminarán en la 5.1.0. Las columnas de grid con `component="button"` ya no aparecen en las Columnas de la barra del grid ni se imprimen o exportan, y el motor React respeta ahora `printable="false"`. | Apunte sus propias opciones de menú de `Usr` a `user-admin` antes de la 5.1.0. Si sobrescribe las queries o los maintains de usuarios (`UsrLst`, `UsrNew`, `UsrUpd`, `UsrDel`, `reset2fa`), la nueva pantalla los usa; compruebe que conservan los campos que lee. Los tests de navegador que abren la opción *Usuarios* esperan la nueva pantalla. | [Usuarios](default-screens.md#users) (issue #793) |
| Shell de AWE 5 (ambos clientes) | Las pantallas home verticales muestran un pie bajo el menú lateral ("Powered by AWE 5" y un botón que pliega el menú) y el botón de menú de la cabecera tiene el icono `panel-left`. El menú plegado oculta la búsqueda de opciones y abre las opciones de una opción de primer nivel como un flyout al pasar el puntero. En React, una opción separadora sin etiqueta se dibuja como una línea en el menú vertical (antes se ocultaba), el menú de usuario muestra el `name` del valor de `connectedUser` en lugar de su etiqueta ("Manager" en lugar de "Manager (test)") con el perfil debajo, los botones outlined son neutros (usaban el color primario) y los criterios de selección tienen la misma altura que los de texto. La consulta `connectedUser` devuelve un campo nuevo `description` con el nombre del perfil. | Nada en los descriptores. Los tests de navegador de aplicaciones React que comprueban el usuario conectado con `checkLogin`/`ensureLoggedIn` deben esperar el nombre sin el código de usuario. Las aplicaciones con su propia copia de `home-sidebar.xml` o `home_vertical.xml` pueden añadir el pie como describe la guía de layout. | [Menú lateral](../design-system/layout-and-shell.md#side-menu), [Avatar](../api/info.md#avatar-attributes) (issue #900) |

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

### JSON de los componentes de pantalla {#screen-components-json}

Cada entrada de `components` en la respuesta de `screen-data` llevaba un `controller` con todo el subárbol
`elementList` del componente, por lo que una definición se enviaba una vez en `structure` y otra más por cada
componente que la contenía. En pantallas grandes la mayor parte de la respuesta eran esas copias. `elementList` ya no
forma parte del JSON del `controller` de un componente (issue #907); `structure` lo conserva, y los campos calculados a
partir de los hijos (`columnModel`, `buttonModel`, `headerModel` de las grids, `children` de avatares y desplegables de
información) se siguen enviando. Los clientes de este repositorio construyen el árbol de componentes a partir de
`structure`, por lo que para ellos no cambia nada.

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

### El JavaScript de Bootstrap ya no se publica {#bootstrap-javascript-is-no-longer-shipped}

La incidencia #755 (slice 4) eliminó `import "bootstrap"` del cliente AngularJS y el paquete `bootstrap` de su `package.json`. El CSS de
Bootstrap no forma parte de este cambio. AWE solo usaba cuatro de sus plugins, y cada uno es ahora código nativo que mantiene el marcado, las
clases, los tiempos y los eventos, así que no cambia ninguna pantalla de AWE. Lo que un producto debe revisar es el código **propio** que usaba
el JavaScript de Bootstrap:

| Usaba | Ahora hace | Use en su lugar |
|---|---|---|
| `$(el).modal('show')`, `.modal('hide')`, `data-toggle="modal"` y `data-target="#id"` | Nada: no existe `$.fn.modal` y el data-api ha desaparecido | Los componentes `awe-dialog` o `awe-side-panel` (abiertos por una acción XML), el componente `awe-confirm`, o el servicio `AweModal`: `AweModal.show(el)`, `hide(el)`, `isShown(el)` y `dispose(el)`. `ui-modal` (con `on-open` y `on-close`) sigue funcionando. |
| `data-dismiss="modal"` dentro de un `.modal` | Funciona como antes (cierra el modal) | Nada que cambiar |
| `data-keyboard="false"` y `data-backdrop="static"` en un `.modal` | Funcionan como antes | Nada que cambiar |
| Los eventos `show.bs.modal`, `shown.bs.modal`, `hide.bs.modal` y `hidden.bs.modal` | Se lanzan como antes, en el elemento, como eventos jQuery | Nada que cambiar (`hidden.bs.modal` sigue llegando después de quitar el fondo) |
| `data-toggle="dropdown"` en un enlace dentro de un `li.dropdown` (o `.btn-group`) | Funciona como antes (la clase `.open`, `aria-expanded`, lo cierran un clic fuera o Esc) | Nada que cambiar |
| `$(el).dropdown('toggle')` | Nada | Lance un `click` sobre el elemento `[data-toggle="dropdown"]` |
| `$(el).popover({...})`, `data-toggle="popover"`, `data-toggle="tooltip"` y `$(el).tooltip(...)` | Nada | El componente `awe-popover` para un popover anclado a un control; el atributo `title` (o su propio CSS) para un tooltip. Los popover de mensaje de `target-message` son nativos. |
| `data-toggle="collapse"`, `data-parent` y `$(el).collapse(...)` | Nada | El acordeón de AWE (`autocollapse` mantiene un elemento abierto) o su propio estado AngularJS con `ng-class` |
| `data-toggle="tab"` y `$(el).tab('show')` | Nada (las pestañas de AWE nunca lo usaron) | Los componentes `tab` y `tab-container` de AWE |
| `data-toggle="buttons"`, `$(el).button(...)`, `data-dismiss="alert"` y `$(el).alert('close')` | Nada | Los componentes botón, checkbox y radio de AWE, y las acciones de mensaje de AWE |
| `$.support.transition` y `$(el).emulateTransitionEnd(ms)` | Nada | Escuche `transitionend`, con un temporizador |

Los tests de sus propios módulos tampoco necesitan Bootstrap: los servicios de AWE no esperan una transición CSS que el elemento no tiene,
así que un `.modal.fade` se muestra y se oculta de inmediato en `jsdom`.

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

### Targets de maintain solo de servidor {#server-only-maintain-targets}

La incidencia #886 añade el atributo `server-only` a `<target>`. Un target solo de servidor se rechaza en cualquier
petición de cliente, aunque venga de un usuario con sesión, y se sigue ejecutando cuando lo lanza el servidor con
`MaintainService.launchPrivateMaintain` (servicios Java, el planificador). En AWE 4 cualquier usuario con sesión podía
lanzar cualquier target no público con los parámetros que quisiera, así que un usuario podía cambiar los datos de otro
usuario a través de los targets que el framework solo lanza desde Java.

El framework marca 39 de sus targets como solo de servidor: los targets de usuario, contraseña, segundo factor, bloqueo
y SSO lanzados desde Java, los de ajustes de usuario, favoritos, avatar y diseño de grid, los de restricciones de menú y
los internos del notificador y del planificador. La lista completa está en la
[auditoría de los targets del framework](../api/maintain-definition.md#maintain-targets-audit).

- Una pantalla, código de cliente o cliente REST que lanzaba uno de ellos directamente recibe ahora un error. Lance el
  target de cliente que lo envuelve, o el servicio Java.
- Marque con `server-only="true"` los targets de la aplicación que solo lanza Java, sobre todo los que toman un usuario
  de los parámetros.
- Un target que un cliente lanza sobre el usuario con sesión debe leer el usuario de la sesión
  (`<variable ... session="user"/>`), nunca de la petición.

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

### Valores del contrato de pantalla {#screen-contract-values}

La incidencia #823 alinea los valores que emiten los constructores y que aceptan los XSD con lo que tratan los motores
AngularJS y React.

Corregidos, así que la acción se ejecuta ahora:

- `Action.CHECK_RECORDS_GENERATED`, `Action.SELECT_FIRST_ROW` y `Action.SELECT_LAST_ROW` envían `check-records-generated`,
  `select-first-row` y `select-last-row` (enviaban `check-records-selected`, `select-first-rows` y `select-last-rows`).
- `toggle-class` se ejecuta en el motor AngularJS, como ya hacía en React.
- `target-message` se ejecuta en el motor React. Como en AngularJS, muestra un popover sobre el componente destino (o
  sobre la celda del grid, cuando el destino tiene fila y columna) con el tipo, el texto y el tiempo del mensaje, y uno
  nuevo sustituye al anterior. Cuando el destino no está en pantalla, React muestra el mensaje como una notificación.
- `close-window`, que tratan los dos motores, es ahora una acción de botón válida en `general.xsd`.

Deprecados: siguen siendo válidos, no tienen comportamiento en ningún motor y se eliminan en la 6.0:

| Valor | Dónde | En su lugar |
|---|---|---|
| `forward`, `check-leaf-selected`, `check-parent-expanded` | Tipo de acción de botón (`general.xsd`), `Action` del constructor | Quítelo. |
| `get-locals` | Tipo de acción de botón, `Action.GET_LOCALS` del constructor | Quítelo. El cliente sigue cargando los textos con la acción de servidor `get-locals`. |
| `check-parent-selected` | `Action.CHECK_PARENT_SELECTED` del constructor (no está en el XSD) | Quítelo. |
| `<frame>`, `<javascript>` | Elementos de pantalla (`screen.xsd`) | Quítelos: ningún motor los pinta. |
| `component="progress"` en un criterio | `criteriaComponentType` (`screen.xsd`) | Quítelo. La columna `progress` de la rejilla no se ve afectada. |

### Auditoría de seguridad {#security-audit}

AWE 5 registra los eventos de seguridad de la aplicación (issue #779): inicios y cierres de sesión, bloqueos, cambios
de contraseña y de segundo factor, aprovisionamiento SSO, accesos denegados y tokens REST. Consulte
[Auditoría de seguridad](../security/security-audit.md).

- **Tabla nueva.** La migración `AWE_V1.3.0__Security_audit_events.sql` crea `AweSecAudEvt` en todas las bases de
  datos que migra Flyway. Las aplicaciones que construyen su esquema con scripts propios deben añadir la tabla.
- **Activada por defecto.** Los eventos van a la tabla, al log JSON (logger `awe.security.audit`) y, con el starter de
  observabilidad, al contador `awe.security.audit.events`. `awe.security.audit.enabled=false` desactiva toda la
  funcionalidad; `awe.security.audit.database.enabled=false` mantiene solo el log. Los eventos con más de 365 días se
  purgan todos los días a las 03:00 (`awe.security.audit.database.retention-days`,
  `awe.security.audit.database.purge-cron`).
- **Bloqueo automático desactivado por defecto.** `awe.security.lockout.max-attempts` sigue en `0`. Cuando una
  aplicación establece un valor, `ope.NumLog` se usa como contador de inicios de sesión fallidos consecutivos
  (contraseña incorrecta): un inicio de sesión correcto lo pone a cero y la cuenta se bloquea (`ope.PwdLck`) al llegar
  al límite. Si su propio código escribe `NumLog` con otros fines, interferirá con el bloqueo.
- **Opción de menú nueva.** `security-audit` (pantalla `SecAud`) se añade en Configuración > Seguridad del menú
  `private`. Las aplicaciones que sustituyen el menú completo no la tienen salvo que la añadan.

### Hash de contraseñas y clave maestra {#password-hashing-and-master-key}

AWE 5 calcula el hash de las contraseñas de usuario con BCrypt y exige una clave maestra en la configuración
(issue #778). Los productos actualizados desde AWE 4 siguen funcionando con sus hashes RIPEMD-160 y sus datos cifrados, pero
algunos pasos deben hacerse **antes** de la actualización. Revise las dos listas.

#### Hash de contraseñas {#password-hashing}

| Tema | AWE 4 | AWE 5 |
|---|---|---|
| Algoritmo de las contraseñas nuevas | RIPEMD-160, sin sal (40 caracteres) | BCrypt, con sal y el prefijo `{bcrypt}` (68 caracteres). `awe.security.password-encoder=ripemd160` mantiene el formato de AWE 4 |
| Hashes RIPEMD-160 almacenados | Se validan | Se siguen validando y se vuelven a hashear con BCrypt en el siguiente inicio de sesión correcto |
| `ope.l1_pas`, `HISope.l1_pas` | `char(40)` | `varchar(255)` (Flyway `AWE_V1.4.0__Password_hash_length.sql`) |
| Comprobación de la contraseña actual (pantallas de ajustes y de cambio de contraseña) | Comparación de hashes en SQL | Java, con `PasswordEncoder.matches` |
| `OpePas` | Lo escriben las pantallas estándar (copia SHA-256) | Ya no se escribe; la columna se mantiene |
| Consulta `getUserData` | Devuelve el hash almacenado (`oldPassword`) | Ya no lo devuelve |

Lista de comprobación:

- [ ] **Elija el codificador.** `awe.security.password-encoder=bcrypt` (por defecto) o `ripemd160`. Con `bcrypt`, un
  usuario cuyo hash es RIPEMD-160 se vuelve a hashear con BCrypt en su siguiente inicio de sesión correcto con la
  autenticación de base de datos. Un rehash fallido (por ejemplo, con una columna que sigue siendo `char(40)`) no
  impide el inicio de sesión: el usuario entra con el hash actual y el log muestra el WARN `The password of user
  <user> could not be rehashed with the configured algorithm. It keeps the current hash`. Con `ripemd160`, las
  contraseñas nuevas mantienen el formato de AWE 4 y no se rehashea nada; los hashes BCrypt ya almacenados se siguen
  validando.
- [ ] **Amplíe la columna antes de actualizar.** Flyway aplica `AWE_V1.4.0__Password_hash_length.sql` en las seis
  bases de datos soportadas: `ope.l1_pas` y `HISope.l1_pas` pasan a `varchar(255)`. Los productos que gestionan su
  propio esquema deben aplicar el cambio equivalente **antes** de actualizar (consulte el script de su base de datos
  en `db/migration`). El script se puede volver a aplicar y nunca reduce una columna que ya sea más ancha de 255
  caracteres.
- [ ] **Revise su XML propio que compara o escribe `l1_pas`.** Un hash BCrypt tiene su propia sal, por lo que el hash
  de la misma contraseña es distinto cada vez: un filtro SQL que compara `l1_pas` con una variable
  `STRING_HASH_RIPEMD160` deja de coincidir en cuanto el usuario se rehashea. Escriba las contraseñas con
  `STRING_HASH_PASSWORD` (calcula el hash con el codificador configurado) y compruébelas en Java con
  `PasswordEncoder.matches`, como hace `CurrentPasswordChangeService`. `STRING_HASH_RIPEMD160` sigue funcionando y
  queda obsoleto. Consulte [Tipos de variable](../api/query-definition.md#variable-types).
- [ ] **Revise los targets estándar redefinidos.** Si su propio `Maintain.xml` o `Queries.xml` redefine los targets de
  contraseña, compárelos con AWE 5: `UsrUpdPwd` y `ChdPwdPub` llaman ahora a un servicio Java que comprueba la
  contraseña actual y después lanza `UsrUpdPwdUpdate` y `ChdPwdPubUpdate`; la consulta `UsrChkPwd` se elimina.
- [ ] **Revise los sistemas externos que leen `l1_pas`.** Un sistema que compara o copia el hash (otra aplicación, un
  informe, un puente de inicio de sesión único) no entiende BCrypt. Mantenga
  `awe.security.password-encoder=ripemd160` hasta que se adapte.
- [ ] **Haga una copia de seguridad de `ope` antes de la actualización.** No hay vuelta atrás a AWE 4 para los usuarios
  ya rehasheados: AWE 4 no puede validar un hash BCrypt. Para volver atrás, restaure la copia (los usuarios que
  cambiaron la contraseña después de la actualización pierden el cambio).
- [ ] **Adapte lo que lea `OpePas`.** Las pantallas estándar ya no lo escriben (contenía una copia SHA-256 sin sal de
  la contraseña). La columna se mantiene; sus valores ya no se actualizan.
- [ ] **Adapte las pantallas propias que leen `oldPassword`.** `getUserData` ya no envía el hash almacenado al
  navegador, y las pantallas de ajustes ya no validan la contraseña actual en el navegador (`validation` con
  `criterion:'oldPassword'`): la comprueba el servidor. Una contraseña actual incorrecta sigue respondiendo OK y no
  actualiza nada.
- [ ] **Valores enmascarados en el log de SQL.** El SQL que se escribe en el log (y el que llevan los errores de
  consulta) muestra `'********'` en lugar de los valores de las variables `STRING_HASH_*` y `STRING_ENCRYPT` y de los
  valores escritos o comparados con las columnas de `awe.database.log-masked-fields` (por defecto `l1_pas`); la
  sentencia que se ejecuta no cambia. Añada a la propiedad sus propias columnas sensibles si también quiere
  enmascararlas.
- [ ] **Scripts de inserción de usuarios.** La pantalla [Utilidad de cifrado](default-screens.md#encrypt-util) muestra
  ahora el **Hash de contraseña** del texto, listo para insertar en `ope.l1_pas`, con el algoritmo configurado o, si
  lo elige en **Algoritmo de contraseña**, con el hash RIPEMD-160 de AWE 4.

#### Clave maestra {#master-key}

`awe.security.master-key` cifra los datos almacenados: las variables `STRING_ENCRYPT`, las propiedades `ENC(...)` y
los valores de la pantalla Utilidad de cifrado. AWE 4 usaba un valor por defecto publicado cuando la propiedad no estaba
definida; AWE 5 no tiene valor por defecto.

Lista de comprobación:

- [ ] **Defina la clave maestra.** Sin ella la aplicación no arranca, y Spring Boot indica este motivo para la
  propiedad `awe.security.master-key`:

  ```text
  awe.security.master-key is required: it encrypts stored data (encrypted parameters and properties). Applications
  upgraded from AWE 4 that never configured it must set the former default value (see the AWE 5 migration guide) to
  keep decrypting their stored data, then rotate it.
  ```

- [ ] **Las aplicaciones que nunca la definieron** cifraron sus datos con el valor por defecto de AWE 4. Defina ese
  valor, copiado literalmente (incluido `%25`), para seguir descifrando los datos `STRING_ENCRYPT` y las propiedades
  `ENC(...)`:

  ```properties
  awe.security.master-key=B1Le3s%25abc75TeBe05
  ```

  El valor se acepta y cada arranque registra el WARN `awe.security.master-key is set to the published AWE 4 default
  value. Keep it only while stored data encrypted with it is needed, and rotate it (see the AWE 5 migration guide)`.
- [ ] **Planifique la rotación.** El valor por defecto de AWE 4 es público. Elija una clave secreta nueva por entorno,
  genere los valores nuevos a partir de su texto en claro en la pantalla Utilidad de cifrado con la clave nueva en el
  campo `Phrase key` (`Encrypted` para las columnas de base de datos, `Encrypted property` para `ENC(...)`) y despliegue
  los valores nuevos junto con la nueva `awe.security.master-key`.
- [ ] **Jasypt y el secreto JWT siguen a la clave maestra.** `jasypt.encryptor.password` (las propiedades `ENC(...)`) y
  el valor por defecto de `awe.rest.api.jwt.secret` ya no ocultan una copia del valor de AWE 4: toman la clave maestra.
  Salvo que se defina `awe.rest.api.jwt.secret`, rotar la clave maestra invalida los tokens JWT locales ya emitidos.
- [ ] **Use solo `awe.security.master-key`.** El nombre antiguo `awe.security.master.key` sigue teniendo prioridad
  para jasypt y el secreto JWT cuando está definido, pero no cumple la obligación de definir `awe.security.master-key`,
  y `STRING_ENCRYPT` usa siempre `awe.security.master-key`. Pase el valor al nombre nuevo y elimine el antiguo, para que
  no puedan ser distintos.

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
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Sustituir angular-ui-grid por un componente de *grid* moderno |
| [#829](https://gitlab.com/aweframework/awe/-/issues/829) | Sanitizar el HTML de las celdas de grid en el cliente React (se descartará un `style` en línea en una celda; use una clase CSS) |

## Lista de comprobación de la actualización {#upgrade-checklist}

Copie esta lista a la incidencia de su actualización y márquela a medida que avance.

```markdown
- [ ] The application runs on the latest 4.x release and builds without deprecation warnings
- [ ] `awe.version` (or the `awe-starter-parent` version) is set to the AWE 5 version
- [ ] `awe-react-client` is set to the exact same version, `npm install` was run and the lock file is committed
- [ ] Every screen with a chart was opened and the server log has no `Highcharts chart-parameter` warnings
- [ ] Custom code that imports Highcharts or reads `.highcharts-*` CSS was reviewed
- [ ] Highcharts scripts (themes, plugins) were removed from `scripts.xml` of AngularJS applications
- [ ] The `theme` attribute was removed from the charts, and `setTheme` calls from the builders
- [ ] Custom code that uses jsoup declares its own dependency
- [ ] If the application sets `PwdExp`, the value and the users with an old or empty password change date were reviewed
- [ ] Custom clients do not read `elementList` from the menu JSON
- [ ] Los clientes personalizados no leen `elementList` del `controller` de los componentes de `screen-data`
- [ ] Flyway was repaired or the database recreated if `SCHEDULER_V1.0.5` had been applied before
- [ ] Se tomó la decisión sobre el modo oscuro que sigue al sistema operativo (`awe.application.theme-mode`)
- [ ] El CSS de la aplicación ya no define las variables de tema de AWE 4 (`--primary-color`...) ni da estilo a `.theme-<nombre>`
- [ ] Browser tests compile; the `By` overloads and `getDriver()` were moved to `Locator` and `getBrowser()`
- [ ] The application builds and runs on JDK 21 with `java.version` 21
- [ ] Spring Boot 4.1 starters and test annotations replace the removed ones (`spring-boot-starter-web`, `@MockBean`...)
- [ ] Spring Session with Redis uses `spring-boot-starter-session-data-redis` and `spring.session.data.redis.*` properties
- [ ] Jackson imports moved to `tools.jackson.*`; `asText()`/`asInt()`/`asBoolean()` calls use the default-value overloads
- [ ] Clients of the AWE API send no content after the JSON body; Jackson 3 clients use the AWE mapper
- [ ] Own `logback-spring.xml` no longer declares the `applicationName` conversion rule
- [ ] Maintains that relied on partial commits were reviewed, or `awe.maintain.transaction.enabled=false` is set
- [ ] No client code, screen or REST client of the application launches a framework target that is now `server-only`; own Java-only targets are marked `server-only="true"`
- [ ] The security audit was reviewed: `AweSecAudEvt` exists, the `security-audit` option is restricted, `awe.security.lockout.max-attempts` decided
- [ ] Screens and builders use no deprecated contract value (`forward`, `get-locals`, `<frame>`, `<javascript>`...)
- [ ] `ope` was backed up, `l1_pas` is `varchar(255)` and `awe.security.password-encoder` was decided (own XML and external readers of `l1_pas` reviewed)
- [ ] `awe.security.master-key` is set (the AWE 4 default value if the application never set it) and its rotation is planned
- [ ] Own templates and scripts use no Bootstrap JavaScript (`data-toggle`, `data-target`, `data-parent`, `$(el).modal(...)`, `.popover(...)`...): the AWE components, `AweModal` or native code replace them
- [ ] Se revisó el CSS de la aplicación que daba estilo a los glifos de las fuentes de iconos (`.fa-*:before`, `.pi-*`) o a la clase `.layout-dark`
- [ ] Una configuración de seguridad propia permite `/icons/**` (el inicio de sesión muestra sus iconos)
- [ ] Se revisaron en modo claro y oscuro las sobrescrituras de CSS del inicio de sesión, el cambio de contraseña y 2FA
- [ ] Se revisaron las pruebas de navegador de AngularJS que pulsan Escape en una confirmación o un diálogo, o que dependen del foco
- [ ] Las pruebas de navegador de React esperan el nombre del usuario sin su código en `checkLogin`/`ensureLoggedIn`
- [ ] Las opciones de menú propias que abren las pantallas deprecadas `Usr` apuntan a `user-admin`
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
