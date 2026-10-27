---
id: layout-and-shell
title: Estructura de la aplicación
sidebar_label: Estructura de la aplicación
---

El marco de la aplicación en AWE 5: menú lateral plegable, cabecera clara, breadcrumb y contenido en tarjetas. Las
opciones de menú y el XML de las pantallas no cambian; solo cambia la presentación. Puede verlo en la
[pantalla de ejemplo](pathname:///design-system/prototype/).

```text
┌──────────────┬──────────────────────────────────────────────────┐
│ Zona de marca│ Cabecera: menú · búsqueda global · ayuda ·       │
│              │           notificaciones · menú de usuario       │
├──────────────┼──────────────────────────────────────────────────┤
│ Buscar opción│ Breadcrumb                                       │
│ Sección      │ Título de página + acciones                      │
│  Opción      │ ┌ Tarjeta ────────────┐ ┌ Tarjeta ────────────┐  │
│   Subopción  │ │                     │ │                     │  │
│    Sub-sub   │ └─────────────────────┘ └─────────────────────┘  │
│ Powered by   │                                                  │
└──────────────┴──────────────────────────────────────────────────┘
```

## Zona de marca {#brand-slot}

Arriba a la izquierda del menú lateral. Contiene el logo **del cliente**, como hacen hoy los productos de AWE.

| Variante | Contenido | Cuándo |
|----------|-----------|--------|
| Logo horizontal | El logo horizontal del cliente | Por defecto |
| Marca + nombre | Marca cuadrada más el nombre del producto y del cliente | El logo es cuadrado o no se lee en pequeño |
| Logo de AWE | El logo de AWE | No hay logo de cliente configurado |
| Rail | Solo la marca cuadrada | El menú está plegado |

- El logo tiene una variante clara y otra oscura, o una que funcione sobre los dos fondos.
- La atribución a AWE es pequeña y secundaria: "Powered by AWE 5" con la marca de AWE, en el pie del menú.
- Altura del logo: `--awe-header-logo-height` (34px en cómoda, 30px en compacta).

## Menú lateral {#side-menu}

Hasta **tres niveles** por debajo de una sección: sección › opción › subopción › sub-subopción.

| Elemento | Regla |
|----------|-------|
| Etiqueta de sección | Texto pequeño en mayúsculas y atenuado. No es clicable. |
| Opción de nivel 1 | Icono + etiqueta (+ badge). Las opciones con hijos muestran un chevron. |
| Niveles 2 y 3 | Sin icono. Con sangría y una línea guía vertical por nivel. Más pequeños y atenuados. |
| Rastro activo | Cada antecesor de la página actual queda marcado (peso, barra de acento en la guía); la propia página tiene el fondo de resaltado. |
| Expandir / plegar | Por nivel y animado (`--awe-motion-duration-expand`). Los niveles abiertos se restauran sin animación. |
| Búsqueda | Filtra a cualquier profundidad, abre la ruta de cada coincidencia y resalta el texto encontrado. |
| Badges | Contadores (neutros o de peligro) y "New". |

**Rail.** El menú se pliega a una barra de iconos (`--awe-menu-width-collapsed`, 68px) y el cambio de ancho se anima.
Al pasar el ratón, enfocar o pulsar una opción de nivel 1 se abre un **flyout** con sus niveles 2 y 3. Teclado: Enter o
→ lo abre y enfoca el primer enlace, ↑/↓ se mueven, Esc o ← lo cierran y devuelven el foco.

**Pie.** Bajo una línea, "Powered by **AWE** 5" con la marca de AWE y un botón con `chevrons-left` que pliega el
menú; en el rail solo queda el botón (girado para desplegar) y se oculta la búsqueda de opciones. Los dos motores le dan estilo a partir del
marcado de la pantalla home, después del menú:

```xml
<tag type="div" style="sidebar-footer">
  <tag type="span" style="sidebar-powered">
    <tag type="span" label="SIDEBAR_POWERED_BY"/>
    <tag type="strong" label="SIDEBAR_POWERED_BRAND"/>
  </tag>
  <!-- React: info-button con un toggle-class de layout-static-inactive sobre .layout-container -->
  <!-- AngularJS: button con style="sidebar-collapse no-class" y una acción toggle-menu -->
  <info-button id="collapse-menu" title="BUTTON_TOGGLE_MENU" icon="lucide:chevrons-left" style="sidebar-collapse">
    <button-action type="toggle-class" target-action="layout-static-inactive" target=".layout-container"/>
  </info-button>
</tag>
```

**Tableta y menor (≤1024px).** El menú pasa a ser un cajón sobre el contenido con máscara. El rail solo existe en
escritorio.

## Cabecera {#header}

- Clara, ligeramente translúcida, fija al desplazar, de altura `--awe-header-height`.
- Izquierda: botón de menú (icono `panel-left`) y búsqueda global (`Ctrl K` la enfoca).
- Derecha: ayuda, notificaciones (punto cuando hay sin leer; lista en un popover) y menú de usuario (perfil,
  preferencias, atajos, cambiar de empresa, cerrar sesión).

## Página {#page}

| Elemento | Regla |
|----------|-------|
| Breadcrumb | Icono de inicio y cada nivel del menú hasta la página. La página actual no es un enlace. |
| Título de página | `--awe-font-size-page-title` y una línea opcional de descripción en texto atenuado. |
| Acciones de página | A la derecha del título. Como mucho una acción primaria; el resto secundarias, con borde, de texto o solo icono. |
| Tarjetas | Cada bloque (criterios, grid, pestañas) es una tarjeta: superficie, borde, `--awe-radius-card`, `--awe-shadow-card`. |
| Cabecera de tarjeta | Título (+ subtítulo) y acciones de la tarjeta. Un separador cuando el cuerpo es un formulario o un grid. |
| Rejilla de tarjetas | `--awe-layout-gap` entre tarjetas. Las dos columnas pasan a una por debajo de 1280px. |
| Maximizar | Una tarjeta puede ocupar el área de contenido; el menú y la cabecera siguen visibles. |

## Página de login {#login-page}

Puede verla en el [prototipo del login](pathname:///design-system/prototype/login.html).

| Zona | Contenido |
|------|-----------|
| Columna izquierda | Logo del cliente, formulario centrado verticalmente (máximo 420px) y pie con copyright, "Powered by AWE 5", selector de idioma y de modo |
| Columna derecha | Panel de imagen con un pie (título y una línea) sobre una capa oscura |
| Por debajo de 960px | El panel de imagen se oculta; el formulario queda centrado, máximo 400px |

Formulario: título "Sign in to the platform" en `--awe-font-size-display`, usuario, contraseña con mostrar/ocultar,
"Forgot password?", "Sign in" primario a todo el ancho, separador "or continue with" y un botón con borde a todo el ancho
para el inicio de sesión único.

**Hueco de imagen.** Un producto fija su propia imagen con `--awe-login-image` (un `url()`) o con el hueco
`<img class="login__img">` (`object-fit: cover`). Por defecto: el logo de AWE sobre un degradado oscuro de marca
(`--awe-login-brand-background`). Otras opciones: un degradado simple del primario (`--awe-login-gradient`) o sin
imagen (formulario centrado y solo). La capa del pie mantiene su texto por encima de 10:1 con cualquier primario;
compruébelo con su propia imagen.

**Diseños.** Los dos motores construyen la página de inicio de sesión a partir de las propiedades
`awe.application.login.*`, sin cambiar el XML de inicio de sesión. `awe.application.login.layout` elige el diseño, que
se indica como `data-awe-login-layout` en la raíz de la página; cualquier otro valor equivale a `split`.

| Diseño | Qué muestra |
|--------|-------------|
| `split` (por defecto) | La columna del formulario junto al panel de imagen con su pie; el pie de página bajo el formulario. Por debajo de 960px el panel se oculta. |
| `form-only` | El formulario solo, centrado, con su pie de página. Sin panel de imagen. |
| `card` | La imagen de fondo (o el panel de marca de AWE) ocupa toda la pantalla bajo `--awe-login-backdrop-veil`, el formulario queda en una tarjeta centrada y el pie de página bajo la tarjeta, sobre la superficie de la tarjeta. El pie de la imagen se mantiene sobre el fondo y se oculta por debajo de 960px; la tarjeta conserva el fondo. |

**Partes configurables.** Los colores vienen del tema (`awe.application.theme-primary`, `theme-surface`); el login no
añade ninguna propiedad de color. Un texto vacío oculta su elemento.

| Parte | Propiedad | Por defecto |
|-------|-----------|-------------|
| Logo del formulario | `awe.application.login.logo` | El logo de AWE |
| Logo en modo oscuro | `awe.application.login.logo-dark` | El logo |
| Imagen del panel (`split`) o del fondo (`card`) | `awe.application.login.background` | El panel de marca de AWE |
| Efecto canvas del panel de imagen, por el nombre que registra la aplicación | `awe.application.login.background-effect` | Ninguno (la imagen) |
| Título del formulario y subtítulo bajo él | `awe.application.login.title`, `awe.application.login.subtitle` | `SCREEN_TEXT_LOGIN_TITLE` ("Bienvenido a AWE"), `SCREEN_TEXT_LOGIN_SUBTITLE` ("Introduce tus credenciales para continuar.") |
| Pie sobre la imagen (título y una línea) | `awe.application.login.panel-title`, `awe.application.login.panel-subtitle` | `SCREEN_TEXT_LOGIN_PANEL_TITLE` ("AWE Framework"), `SCREEN_TEXT_LOGIN_PANEL_SUBTITLE` ("Aplicaciones web funcionales completas con low code") |
| Texto del pie de página (a la izquierda), por ejemplo el copyright | `awe.application.login.footer-text` | Ninguno |
| "Powered by AWE 5" (bajo el texto del pie de página) | `awe.application.login.powered-by` | `true` |
| Selector de idioma (a la derecha del pie), con los idiomas de `awe.application.language-list` | `awe.application.login.language-selector` | `true` |
| Selector de modo claro, oscuro y sistema (a la derecha del pie) | `awe.application.login.mode-selector` | `true` |

El campo de contraseña tiene un botón para mostrarla u ocultarla y avisa mientras Bloq Mayús está activado. Antes de
iniciar sesión, el idioma elegido se aplica a la página y el modo elegido se guarda en el navegador (`localStorage`);
tras iniciar sesión se aplican el idioma y el modo del usuario.

- **Textos**: son una clave de idioma o un literal. Un valor que es una clave de idioma completa muestra su traducción
  en el idioma del usuario; cualquier otro valor se muestra tal cual.
- **Imágenes**: admiten una URL relativa a la raíz de la aplicación (`images/acme/logo.svg`), una URL absoluta
  (`https://cdn.acme.com/bg.jpg`) o una URI `data:`. Un logo configurado sustituye al logo y al nombre de AWE del
  formulario.
- **Modo oscuro**: mientras la página está en modo oscuro (el modo del usuario o el del sistema), el formulario muestra
  `logo-dark` si está definido y, si no, `logo`; cambia cuando cambia el modo.
- **Powered by AWE 5**: ponga `powered-by` a `false` para ocultarlo; sin él y sin texto de pie no hay pie de página.
- **Alternativa obsoleta**: cuando `logo` o `background` no están definidas, un valor de
  `awe.application.paths.image-startup-logo` o `awe.application.paths.image-startup-background` distinto del valor por
  defecto sigue personalizando la página de inicio de sesión. Páselo a la propiedad de `login`. En AngularJS esas dos
  propiedades siguen dando estilo a las pantallas de cambio de contraseña y de 2FA, a las que no llega
  `awe.application.login.*`.

Personalización de un cliente en `application.properties`:

```properties
awe.application.theme-primary=#0f766e
awe.application.login.layout=card
awe.application.login.logo=images/acme/logo.svg
awe.application.login.logo-dark=images/acme/logo-white.svg
awe.application.login.background=images/acme/sign-in.jpg
awe.application.login.subtitle=Portal de clientes
awe.application.login.panel-title=ACME Logistics
awe.application.login.panel-subtitle=Todos sus envíos en un solo lugar
awe.application.login.footer-text=\u00A9 2026 ACME Logistics
```

Spring Boot lee los ficheros `.properties` en ISO-8859-1: escriba los caracteres que no son ASCII como escapes Unicode (`\u00A9` para ©) o use `application.yml`, que se lee en UTF-8.

**Tokens de la tarjeta.** El diseño `card` lee `--awe-login-card-width` (480px, 420px en densidad compacta),
`--awe-login-card-padding`, `--awe-login-card-background` (la superficie), `--awe-login-card-radius` y
`--awe-login-card-shadow` (los de un modal), y `--awe-login-backdrop-veil`, el velo sobre la imagen de fondo. Consulte
la [referencia de tokens](tokens-reference.md#tier-3-component).

**Efecto de fondo propio.** `awe.application.login.background-effect` recibe el nombre de un efecto canvas que
registra la aplicación: AWE no incluye ningún efecto, cada producto escribe el suyo. El nombre se recorta; un valor
vacío, el de por defecto, mantiene la imagen de fondo. El efecto ocupa el lugar del panel de imagen del diseño `split` y
del fondo del diseño `card`, bajo el velo y el degradado oscuro, con el pie de la imagen encima; el diseño `form-only`
no tiene fondo, así que no dibuja nada, y en `split` se oculta con el panel por debajo de 960px. El framework no da
ningún color al canvas: el efecto pinta su propio fondo (opaco, o transparente para que se vea el fondo de la página).

El framework añade `<div class="signin-effect" aria-hidden="true"><canvas></canvas></div>` donde va la imagen de fondo,
llama al efecto registrado con ese canvas y lo detiene cuando la página desaparece. La raíz de la página lleva
`data-awe-login-effect="<nombre>"` y, mientras el efecto se está dibujando, `data-awe-login-effect-running`.

| Caso | Qué ocurre |
|------|------------|
| Nombre desconocido | No se dibuja nada y se mantiene la imagen de fondo (ningún contenedor tapa el panel) |
| Efecto registrado después de dibujar la página | Se inicia cuando se registra, así que un bundle que carga más tarde también lo dibuja |
| Efecto que lanza un error | Se informa con `console.error` (en AngularJS, `$log.error`) y la página y su imagen quedan como estaban |
| La página desaparece | Se llama a la función que devolvió el efecto (en AngularJS, al recibir `$destroy`) |

El contrato es una función, `effect(canvas) => stop | void`: dibuja en el canvas y devuelve la función que lo detiene
(cancelar los fotogramas, los observadores y los escuchadores), o nada cuando no tiene nada que detener. El registro es
común a los dos motores mediante `globalThis.aweLoginEffects`, así que el efecto se escribe una sola vez.

```js
// Bundle de la aplicación, antes o después de dibujar la página (React)
import {registerLoginEffect} from "awe-react-client";

registerLoginEffect("brand-wave", canvas => {
  const context = canvas.getContext("2d");
  let frame = requestAnimationFrame(function draw() {
    // Dibuje el fondo y la animación del producto en el canvas
    frame = requestAnimationFrame(draw);
  });
  return () => cancelAnimationFrame(frame);
});
```

```js
// Bundle de la aplicación (AngularJS): el mismo registro global
aweLoginEffects.register("brand-wave", function (canvas) {
  var context = canvas.getContext("2d");
  var frame = requestAnimationFrame(function draw() {
    frame = requestAnimationFrame(draw);
  });
  return function () {
    cancelAnimationFrame(frame);
  };
});
```

```properties
awe.application.login.background-effect=brand-wave
```

El canvas ocupa el panel (el 100% de su ancho y su alto): ajuste su búfer de dibujo con `getBoundingClientRect()` y
`devicePixelRatio`. Un canvas no resuelve propiedades personalizadas de CSS, así que lea los tokens del producto con
`getComputedStyle(canvas)`. El movimiento, el movimiento reducido (`prefers-reduced-motion`), la animación y la
limpieza son responsabilidad del efecto.

**Inicio de sesión único.** El botón es genérico ("Continue with your organization", icono de llave). No use logos ni
marcas de terceros.

| Estado | Comportamiento |
|--------|----------------|
| Errores de validación | Campos obligatorios marcados, mensaje bajo cada uno, foco en el primero |
| Credenciales incorrectas | Mensaje de peligro arriba del formulario, contraseña vaciada y enfocada |
| Bloq Mayús activado | Aviso bajo la contraseña mientras se escribe |
| Cargando | Spinner en el botón, "Signing in…", campos deshabilitados, `aria-busy` |
| Contraseña caducada | Paso "Change your password": campos nueva y confirmación, lista de requisitos en vivo (#849) |
| SSO en curso | Vista de progreso con Cancelar |

Los campos usan `autocomplete="username"`, `current-password` y `new-password`. Enter envía el formulario.
