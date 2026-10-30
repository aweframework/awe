---
id: tailwind-compat
title: Tailwind y la capa de compatibilidad con Bootstrap 3
sidebar_label: Tailwind y compatibilidad
---

El cliente AngularJS está pasando del LESS vendorizado de Bootstrap 3 y pixel-admin (unos 1,7 MB de `styles.css`) a Tailwind
v4 construido sobre los [design tokens](design-tokens-guide.md) (issue #755). El cambio se hace por fases, y cada una sin
cambio visual. Esta página describe lo que existe hoy.

:::info Qué ha cambiado para un producto
Las primeras familias las sirve solo la capa de compatibilidad: el grid, los paneles, las utilidades y las utilidades responsive
vendorizados de Bootstrap ya no están en la compilación LESS (fase 2d). Tus pantallas XML y plantillas se pintan igual (ver el
[diff de píxeles](#pixel-diff-of-the-removal)). Un LESS de producto que usara esos conjuntos de reglas de Bootstrap como mixins o
los extendiera tiene que cambiar: ver la [guía de migración a AWE 5](v5-migration.md).
:::

## Qué se construye {#what-is-built}

:::note Los tokens AWE 5 (#755)
Los ficheros de compatibilidad leen solo tokens semánticos o de componente `--awe-*` (sin literales de color, sin tamaños `px` sueltos y sin primitivas `--awe-palette-*`; lo comprueba `compatTokens.test.js`). Cada valor de Bootstrap se asignó por significado, así que las clases siguen el tema, el modo y la densidad y **pueden verse distintas de los valores compilados de Bootstrap 3**. Los tokens nuevos son `--awe-border-width`, `--awe-border-width-strong` y `--awe-grid-container-sm|md|lg`.
:::

`awe-client-angular` ejecuta Tailwind junto a la compilación LESS (`postcss-loader` + `@tailwindcss/postcss`, configurado en
`postcss.config.js`). Ambos acaban en el mismo `css/styles.css`. Las fuentes de Tailwind están en
`awe-client-angular/src/main/resources/tailwind/`:

| Fichero | Qué contiene |
|---------|--------------|
| `awe-tailwind.css` | Punto de entrada: orden de capas, prefijo, sin preflight, sin detección automática de fuentes |
| `theme.css` | El `@theme` que mapea todos los tokens `--awe-*` (ver [Design tokens](design-tokens-guide.md#mapping-into-tailwind)) |
| `sources.css` | Las carpetas que Tailwind escanea: plantillas AngularJS, directivas y las pantallas XML del framework, los módulos y las apps de prueba |
| `compat/` | Los nombres de clase de Bootstrap 3 escritos sobre Tailwind con `@apply`, un fichero por familia |

### Cómo convive con Bootstrap 3 {#how-it-coexists-with-bootstrap-3}

- **Capas de cascada.** Tailwind escribe en `@layer theme, base, components, utilities`. El LESS no está en una capa, y una
  regla sin capa siempre gana a una con capa, así que Tailwind nunca puede cambiar lo que pinta una regla vendorizada de
  Bootstrap. Cuando una fase elimina una familia vendorizada de Bootstrap, su regla de compatibilidad (ya en la capa
  `components`) toma el relevo sin más cambios, y desde ese momento pierde frente a cualquier regla sin capa de igual o mayor
  especificidad, sea cual sea el orden de los ficheros.
- **Prefijo `tw:`.** Las utilidades de Tailwind solo se escriben con el prefijo (`tw:flex`, `tw:p-3`, `tw:bg-primary`). Tailwind
  y Bootstrap 3 comparten nombres (`hidden`, `table`, `collapse`, `container`...), así que un nombre sin prefijo siempre
  significa Bootstrap. Las variantes responsive usan los breakpoints de Bootstrap 3: `tw:sm:` desde 768px, `tw:md:` desde
  992px y `tw:lg:` desde 1200px.
- **Sin preflight.** El reset de Tailwind no se importa: los estilos base de Bootstrap los sirve la capa de compatibilidad (slice 8: normalize, scaffolding, type, code y print), y las reglas que una regla sin capa vencería se quedan en los restos del LESS.
- **Sin detección automática.** Solo se escanean las carpetas de `sources.css`, así que una clase que solo está escrita en un
  test o en la documentación nunca se emite.

Ya puedes escribir `style="tw:p-md tw:bg-panel"` en una pantalla XML del framework y verla emitida, con una limitación: hasta que se
elimine el LESS vendorizado de Bootstrap, pixel-admin y AWE (fase 6), una utilidad `tw:` solo tiene efecto donde ninguna de esas
reglas fija la misma propiedad en ese elemento, porque el LESS sin capa gana al Tailwind con capa. No confíes aún en las utilidades
`tw:` para sobrescribir un estilo existente. Un producto tiene pantallas XML que esta compilación no puede escanear: para ellas usa
los nombres de Bootstrap 3 de abajo, que la capa de compatibilidad siempre emite, o añade tu propia compilación opcional de Tailwind.

## La capa de compatibilidad {#the-compatibility-layer}

Un nombre de clase de Bootstrap 3 se reimplementa en `tailwind/compat/<familia>.css`. Una familia solo está completa cuando un
test de Jest demuestra que equivale a la regla de Bootstrap vendorizada (mismas declaraciones, mismas media queries) y el test
guardián muestra que todas las clases de las pantallas XML siguen servidas.

| Familia | Clases | Estado |
|---------|--------|--------|
| Grid | `container`, `container-fluid`, `row`, `col-{xs,sm,md,lg}-{1..12}`, `-offset-`, `-push-`, `-pull-`, `-offset-right-` (AWE) | **Hecha, regla vendorizada eliminada** (fase 2d) |
| Paneles | `panel`, `panel-{heading,title,title-icon,body,footer}`, `panel-transparent`, `panel-{default,info,body-colorful}`, `panel-group`, `panel-collapse`, `panel-padding{,-h,-r}`, `panel-wide`, `tab-content-padding`, `form-group-margin`, `no-grid-gutter-h`, `rounded` | **Hecha, regla vendorizada eliminada** (fase 2d); las reglas de paneles de pixel-admin y lo listado más abajo siguen en LESS |
| Utilidades | `pull-{left,right}`, `text-{left,right,center,justify,nowrap,lowercase,uppercase,capitalize}`, `hide`, `show`, `hidden`, `invisible`, `hidden-*`, `visible-*`, `clearfix`, `center-block`, `text-hide` | **Hecha, regla vendorizada eliminada** (fase 2d), salvo `text-*`, que `type.less` sigue definiendo |
| Utilidades de espaciado | `no-padding*`, `padding-{sm,xs}*`, `no-margin*`, `no-border*`, `no-border-radius`, `no-bg`, `valign-*`, `height-{sm,md,lg,200}`, `text-{xxs,xs,sm,bg,lg,xlg,slg}`, `text-{slim,normal,semibold,bold,underline,default,light-gray}` | Hecha (fase 2b), la regla vendorizada sigue activa |
| Botones | `btn`, `btn-{default,primary,success,info,warning,danger,link}`, `btn-{lg,sm,xs,block}`, con los estados `active`, `disabled` y `focus` | Hecho, regla vendorizada eliminada (slice 3d); las reglas hover, focus y active de `btn` y de los colores, `btn-flat\|outline\|rounded\|labeled` y los selectores de tema siguen en LESS |
| Grupos de botones | `btn-group`, `btn-group-{lg,sm,xs,vertical,justified}`, `btn-toolbar`, `[data-toggle="buttons"]` y `dropdown-toggle` (la regla global `:focus` y las de dentro de grupos) | Hecho, regla vendorizada eliminada (slice 3d); los estados del desplegable (`.open`, `.dropup`), `.btn-outline` dentro de un grupo y la copia `#ui-grid-twbs` siguen en LESS |
| Formularios | `form-group`, `form-control` (foco, placeholder, `readonly`/`disabled`), `input-{sm,lg}`, `form-group-{sm,lg}`, `form-control-static`, `control-label`, `help-block`, `form-horizontal`, `form-inline`, `has-feedback`, `form-control-feedback` (slice 3b), `checkbox`, `radio`, `*-inline` y `has-{success,warning,error}` (slice 3b estados) | Hecho, regla vendorizada eliminada (slice 3d); `form-actions` (solo pantallas de acceso), los selectores `.has-* .form-control:focus`, `.has-*.dark\|.simple` y las reglas de criterio, de tema y de `px`/`lbl` siguen en LESS |
| Grupos de entrada | `input-group` (y `col-*` dentro), `input-group-{addon,btn,lg,sm}` con sus variantes `> .form-control` y select/textarea, reglas de primer/último hijo, uniones de botones, y los selectores `.has-*` del addon, `.form-inline` y `.btn-toolbar` que los mencionan | Hecho, regla vendorizada eliminada (slice 3d); las copias de navbar-form, `.btn-outline\|flat` en un grupo, los bordes del botón primario por tema (`.theme-*`), los addons `.dark` y `.no-background` y las reglas de criterio, cabecera de panel, select2 y selectores de fecha/color siguen en LESS |
| Modal, dropdown, popover, collapse (comportamiento) | El comportamiento de `modal`, `dropdown-toggle`, el `popover` de los mensajes sobre un destino y el `collapse` del acordeón | **Hecho, nativo** (slice 4): ya no se publica el JavaScript de Bootstrap |
| Modales | `modal`, `modal-{open,dialog,content,backdrop,header,title,body,footer,scrollbar-measure,lg,sm}`, `modal-alert` con sus variantes `modal-{info,danger,warning,success}`, `close` | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (modals.less 12 de 26, close.less 0 de 4 reglas compiladas); el resto se queda literal en los restos: las reglas del panel lateral y del editor de columnas del grid, los diálogos de plugins (bootbox, summernote, los selectores), el botón de cerrar de las pantallas de acceso y los selectores de tema siguen en LESS |
| Desplegables | `caret`, `dropup`, `dropdown`, `dropdown-menu` (`pull-right`, `-right`, `-left`, los enlaces y sus estados, `divider`, `dropdown-header`, `dropdown-{xs,sm,md,lg,xl}`, `dropdown-submenu`), `dropdown-backdrop`, `open > dropdown-menu` | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (2 de 31 reglas compiladas); el resto se queda literal en los restos: `.open .btn.dropdown-toggle` (un fondo `!important` que los botones de color sobrescriben), los estados abiertos de los botones de color y de tema, los desplegables de la barra de navegación y de los plugins y la pestaña de desbordamiento `tabdrop` siguen en LESS |
| Navs | `nav`, `nav-{tabs,pills,stacked,justified,tabs-justified,divider,icon}`, `tab-content`, `tab-pane` y el desplegable dentro de una pestaña | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (6 de 63 reglas compiladas); el resto se queda literal en los restos: `.nav-tabs.nav-justified > li > a` (un borde `!important` que podría coincidir con la paginación), las insignias y etiquetas dentro de una pestaña, el contenedor de pestañas y la entrada de avatar de AWE y los navs de la barra principal siguen en LESS |
| Barra de navegación | `navbar`, `navbar-{header,collapse,fixed-top,static-top,fixed-bottom,brand,toggle,nav,form,btn,text,left,right,default,inverse,link}` con el formulario, los botones de icono y las etiquetas e insignias de la barra | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (23 de 180 reglas compiladas); el resto se queda literal en los restos: `#main-navbar` y su menú (pixel-admin), `.navbar-header h1` (el logo es un `url()` relativo a su fichero LESS) y los selectores de tema siguen en LESS |
| Popovers | `popover`, `popover-{title,content}` y la `arrow` de cada colocación | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (3 de 19 reglas compiladas); el resto se queda literal en los restos: las variantes de color y oscuras de pixel-admin, el popover de ayuda y los popovers de plugins siguen en LESS |
| Tooltip | `tooltip`, `tooltip-{inner,arrow}` y las colocaciones | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (2 de 16 reglas compiladas); el resto se queda literal en los restos: las variantes de color de pixel-admin, el tooltip del slider y `.grid-cell-tooltip` siguen en LESS |
| Animaciones de componentes | `fade`, `collapse`, `collapsing` | **Hecho** (slice 5): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (3 de 7 reglas compiladas); el resto se queda literal en los restos: los estados `.in` siguen en LESS |
| Tablas | `table`, `table-{condensed,bordered,striped,hover,responsive}` y las filas y celdas contextuales (`active`, `success`, `info`, `warning`, `danger`) | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (10 de 150 reglas compiladas); el resto se queda literal en los restos: las reglas de elementos (`table`, `caption`, `th`, las celdas de columna), las celdas de `.table` (pierden frente a las reglas de `td` y `th` de normalize, de los selectores de fecha y de los plugins), las filas contextuales y de hover, y las tablas con borde y con rayas. El borde de impresión de la tabla con borde, las tablas dentro de un panel y las reglas del grid de AWE sobre la tabla siguen en LESS |
| Etiquetas | `label`, `label-{default,primary,success,info,warning,danger}` con sus estados de enlace, y la etiqueta dentro de un botón o de una píldora | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (8 de 23 reglas compiladas); el resto se queda literal en los restos: la etiqueta base y sus estados de enlace. Las etiquetas de la barra de navegación las sirve la familia de la barra; la etiqueta tipo tag y las etiquetas en las pestañas, el menú principal y un grupo de lista siguen en LESS |
| Insignias | `badge`, `badge-{success,warning,danger,info}`, y la insignia dentro de un botón, una píldora o un elemento de grupo de lista | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y las diez reglas de Bootstrap se quedan literales en los restos (cada una pierde frente a una regla que se queda: la guarda de cascada no encontró ninguna que pudiera salir); las insignias de la barra de navegación las sirve la familia de la barra, y las de las pestañas, el menú principal, la cabecera de un panel de color y el menú de ui-grid siguen en LESS |
| Pozos (wells) | `well`, `well-{lg,sm}` | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y las cuatro reglas de Bootstrap se quedan literales en los restos (los tamaños pierden frente al `.well` de pixel-admin); el editor de fecha de x-editable sigue en LESS |
| Alertas | `alert`, `alert-{link,dismissable,dismissible,success,info,warning,danger}` con el botón de cierre de una alerta descartable | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (8 de 22 reglas compiladas); el resto se queda literal en los restos. El toast de mensajes de AWE 5 (`alert alert-dark alert-awe`, `less/awe/controllers/message.less`) y su animación de entrada y salida, las alertas oscuras y de página de pixel-admin y el tema demo siguen en LESS, y ganan a las reglas de alerta |
| Migas de pan | `breadcrumb`, sus elementos, separador y elemento activo, `breadcrumb-page` y `breadcrumb-label` | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (2 de 4 reglas compiladas); el resto se queda literal en los restos. La barra de migas del shell de AWE 5 (`less/awe/breadcrumbs.less`) rediseña los niveles, el separador y la etiqueta, se escribe una sola vez en LESS y gana a las reglas de compatibilidad; las migas oscuras y sin relleno de pixel-admin siguen en LESS |
| Barras de progreso | `progress`, `progress-bar`, `progress-bar-{success,info,warning,danger}`, `progress-striped`, `progress-bar-striped`, `active` | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y las catorce reglas compiladas salen de la compilación (no queda nada; `progress-bars.less` no tiene restos). Los keyframes `progress-bar-stripes` están en `progress-bars.css`, y las rayas y la transición del ancho toman su duración de `--awe-motion-duration-slow`, que el fichero de tokens pone a `0ms` bajo `prefers-reduced-motion`: la barra sigue mostrando su progreso y deja de moverse. La columna de progreso de AWE y el cargador de ficheros (una pista plana y redondeada), la vista previa de dropzone y la barra de progreso en la cabecera de un panel siguen en LESS |
| Paginación | `pagination`, `pagination-{lg,sm,xs}` con los enlaces, el primer y el último elemento y los estados hover, activo y deshabilitado | **Hecho** (slice 6): las reglas de compatibilidad son iguales a las vendorizadas, y las 36 reglas de Bootstrap se quedan literales en los restos (cada una pierde frente a una regla que se queda: la guarda de cascada no encontró ninguna que pudiera salir). El paginador del grid de AWE (`pagination pagination-size`, construido en `grid.js` y `grid.html` con `uib-pagination`) está escrito en `less/awe/directives/grid.less` y gana a estas reglas; el calendario del selector de fecha, el paginador de DataTables y la paginación de la cabecera de un panel siguen en LESS |
| Pager, objeto media, jumbotron, carrusel y fuente de iconos | `pager`, `media*`, `jumbotron`, `carousel*` y `glyphicon*` (la fuente Glyphicons Halflings) | **Eliminado** (slice 7): nada del framework escribe estas clases (`unusedBootstrapFamilies.test.js` lee las plantillas, las directivas, las pantallas XML, el gestor de ficheros de awe-tools y el cliente React), así que sus reglas vendorizadas se eliminan sin CSS de compatibilidad. Un producto que las escriba necesita su propio CSS |
| Listas de grupo | `list-group`, `list-group-item`, sus enlaces y los estados activo, deshabilitado y contextuales (`list-group-item-{success,info,warning,danger}`), `list-group-item-heading` y `list-group-item-text` | **Hecho** (slice 7): las reglas de compatibilidad son iguales a las vendorizadas, y la regla vendorizada solo se elimina donde ninguna regla sin capa compite (2 de 83 reglas compiladas de las tres familias siguientes); el resto se queda literal en los restos. Las insignias y etiquetas de un elemento, las listas dentro de un panel y el icono de pixel-admin se quedan en LESS |
| Miniaturas | `thumbnail`, sus imágenes, la miniatura enlazada y `caption` | **Hecho** (slice 7): las reglas de compatibilidad son iguales a las vendorizadas; la miniatura base, sus imágenes y el borde del enlace se quedan literales en los restos. El gestor de ficheros de awe-tools escribe el marcado de terceros `thumbnail` y mantiene sus propias sobrescrituras |
| Embeds responsive | `embed-responsive`, `embed-responsive-{item,16by9,4by3}` | **Hecho** (slice 7): las reglas de compatibilidad son iguales a las vendorizadas, y las ocho reglas de Bootstrap se quedan literales en los restos (cada una pierde frente a una regla que se queda) |
| Estilos base | Las reglas de elemento y clases de Bootstrap de `normalize`, `print`, `scaffolding` (body, enlaces, imágenes, `img-*`, `sr-only`), `type` (títulos y `h1`..`h6`, `lead`, `small`, `mark`, `text-*`, `bg-*`, `page-header`, listas, `dl-horizontal`, citas) y `code` (`code`, `kbd`, `pre`, `samp`) | **Hecho** (slice 8): las reglas de compatibilidad equivalen a las vendorizadas (los tests de familia listan los selectores exactos de cada fichero vendorizado); 226 de las 300 reglas compiladas salieron de la compilación y 74 se quedan literales en los restos: `html` (su tamaño de 10px es la base de toda regla en rem), las reglas de impresión que imprimen en negro sobre blanco y las reglas que una regla de pixel-admin o de AWE vencería |

**JavaScript de Bootstrap (slice 4).** El cliente ya no importa el JavaScript de Bootstrap 3 (`import "bootstrap"` y el paquete npm `bootstrap`
han desaparecido; el CSS no cambia). Los cuatro plugins que usaba AWE son nativos y mantienen el mismo marcado, las mismas clases y los mismos tiempos:

| Comportamiento | Sustituto nativo | Marcado y eventos que se mantienen |
|---|---|---|
| Modal (diálogos, paneles laterales, confirmación, el panel lateral de Columnas del grid) | Servicio `AweModal` (`services/modal.js`) | `.modal` recibe `display: block` y `.in`; se añade al body un `.modal-backdrop.in` por modal abierto; `body.modal-open` y el relleno de la barra de desplazamiento; lo cierran Esc, un clic en el fondo y `data-dismiss="modal"`, y se respetan `data-keyboard="false"` y `data-backdrop="static"`; `show.bs.modal`, `shown.bs.modal`, `hide.bs.modal` y `hidden.bs.modal` son eventos jQuery del elemento, y `hidden.bs.modal` se lanza después de quitar el fondo |
| Popover de los mensajes sobre un destino | `createMessagePopover` (`services/messagePopover.js`) | `.popover.fade.in.{top,bottom,left,right}` con su `.arrow`, añadido al body; `auto bottom` pasa a estar encima del destino si no hay sitio; el popover se queda dentro de la ventana y la flecha sigue apuntando al destino; el título y el contenido se muestran como texto y la plantilla se sanea con la lista de permitidos de Bootstrap más los ganchos de test (`data-testid`, `data-testid-owner`, `data-type`) |
| Dropdown (menús de usuario e información, menú de desbordamiento de las pestañas) | `services/dropdown.js` | `data-toggle="dropdown"` alterna `.open` en el padre (o en el `data-target`) y fija `aria-expanded`; lo cierran un clic fuera, en un elemento o Esc; abrir uno cierra los demás; funcionan las flechas y la barra espaciadora; `show.bs.dropdown`, `shown.bs.dropdown`, `hide.bs.dropdown` y `hidden.bs.dropdown` se lanzan en el dropdown |
| Collapse (acordeón) | `services/collapse.js`, dirigido por el elemento del acordeón | El elemento es el único que lo gobierna: `.collapse` y `.collapse.in` en reposo, `.collapsing` con la transición de altura de 0,35 s mientras cambia, `.collapsed` en el enlace mientras el panel está cerrado. Los atributos `data-toggle="collapse"` y `data-parent` han desaparecido de `accordionItem.html`: la regla de uno abierto es el `autocollapse` del acordeón |

`modalChange` y la directiva `ui-modal` funcionan como antes. Los tests basados en `jsdom` no necesitan nada: los servicios no esperan una
transición CSS que el elemento no tiene. `bootstrapJsRemoval.spec.js` falla si el bundle vuelve a importar Bootstrap, si un fuente llama a un
plugin de Bootstrap (`.modal(`, `.popover(`, `.dropdown(`, `.collapse(`, `.tab(`, `.tooltip(`) o si una plantilla usa un atributo `data-*` sin
manejador nativo. Los plugins JavaScript de Bootstrap que AWE no usaba (`tab`, `tooltip`, `button`, `alert`, `carousel`, `scrollspy`, `affix`) ya
estaban sustituidos por código nativo o nunca se llamaban. Las plantillas de producto que dependían del data-api de Bootstrap tienen que cambiar:
consulte [El JavaScript de Bootstrap ya no se publica](v5-migration.md#bootstrap-javascript-is-no-longer-shipped).

**Qué sigue vendorizado tras la fase 2d:** el resto de ficheros de Bootstrap (navs, tablas, modales...), todo el
LESS de pixel-admin (incluidos su espaciado, `utils.less` y sus reglas de paneles), las clases de alineación y transformación
`text-*` de `type.less` y el LESS de AWE. `bootstrap.less` ya no importa `grid.less`, `panels.less`, `utilities.less` ni
`responsive-utilities.less`, y los ficheros se han borrado. Lo que la capa de compatibilidad no sirve se queda, tal cual, en
`less/legacy/panels-leftovers.less` y en `grid-columns.less`.

**Botones, grupos de botones, formularios y grupos de entrada (slice 3d).** `bootstrap.less` ya no importa `buttons.less`, `button-groups.less`,
`forms.less` ni `input-groups.less` (los ficheros están borrados; `mixins/*.less` se queda). Importa `buttons-leftovers.less`,
`button-groups-leftovers.less`, `forms-leftovers.less` e `input-groups-leftovers.less` en los mismos sitios. Cada fichero de restos contiene,
literal tal como compiló y en el mismo orden, las reglas que deben seguir sin capa: las que ganaban a una regla que se queda en el LESS (los
colores de botón deshabilitado frente a los estados de pixel-admin y AWE, `.btn:active` y `.btn.active`, los tamaños de grupo, las alturas
de select y textarea de los grupos de entrada...), las que aquellas ganarían, y las reglas que la capa de compatibilidad no sirve (las reglas
globales de elementos de formulario: `fieldset`, `legend`, `label`, `input[type=...]`; `.btn:focus`; el caret de un botón grande). Las reglas
de compatibilidad siguen ahí y son iguales; solo cambia quién gana donde existe una regla sin capa, y `cascadeGuard.test.js` demuestra que no
queda ninguna (con los pares que ningún elemento puede cumplir a la vez, como `select` y `textarea` o dos colores de botón, excluidos por
`canMeet`). Solo algunas de las reglas compiladas de los cuatro ficheros pudieron salir (los números exactos están en la entrada del paso 2 de `support/vendored-removed.baseline.json`: las reglas congeladas y la lista `kept` de las que se quedan): el guard de cascada encontró que el resto debe quedarse, porque para esta eliminación cuenta también las clases que las fuentes escriben juntas (`form-control input-sm`), las reglas de elemento sobre una etiqueta que lleva la clase (`label` y `btn`) y un shorthand frente a su longhand (`border-radius` y `border-top-right-radius`), cosas que el pixel diff demostró que importan. Los ficheros de restos también conservan lo que Less descartaría en silencio: `.btn-lg()` y `.btn-group-justified()` (los llama
`awe/mobile.less`), `.form-inline()` (lo llama `navbar.less`) y los tres `&:extend(.btn-xs|sm|lg)` de los grupos de botones, que imprimen los
tamaños de grupo en las reglas de pixel-admin. Las reglas congeladas de la eliminación están en `vendored-removed.baseline.json` (paso 2, con
la lista de las que se quedan); `vendoredRemoval.test.js` comprueba que los ficheros no existen, que ninguna regla congelada está en el build,
que cada resto sigue estándolo y que ninguna llamada o extend de un conjunto eliminado queda sin su mixin. El slice 6 elimina los restos junto
con las reglas de pixel-admin con las que compiten.

**Desplegables, navs, barra de navegación, modales, popovers, tooltip y animaciones de componentes (slice 5).** `bootstrap.less` ya no importa
`dropdowns.less`, `navs.less`, `navbar.less`, `modals.less`, `close.less`, `popovers.less`, `tooltip.less` y `component-animations.less`
(los ficheros se eliminan). Importa `component-animations-leftovers.less`, `dropdowns-leftovers.less`, `navs-leftovers.less`,
`navbar-leftovers.less`, `close-leftovers.less`, `modals-leftovers.less`, `tooltip-leftovers.less` y `popovers-leftovers.less` en los mismos
sitios. Las reglas de compatibilidad son iguales a las compiladas vendorizadas (copias de Bootstrap, pixel-admin y AWE de cada selector, con las
declaraciones efectivas en cada media query: `compatDropdowns.test.js` y sus seis hermanos, uno por familia, comparten `support/compatFamily.js`).
De las 346 reglas compiladas de los ocho ficheros, solo 51 salieron del build y 295 se quedan literales en los restos (las listas exactas están en la
entrada del paso 3 de `support/vendored-removed.baseline.json`), porque pixel-admin, los temas y los componentes de AWE reestilizan los mismos
elementos con reglas sin capa a las que una regla de compatibilidad en capa perdería (el menú desplegable y sus enlaces, las pestañas, los enlaces
de la barra, las flechas de los popovers...). El pixel diff de una primera versión encontró tres maneras en que una regla puede perder frente a una sin
capa que la guarda de los slices anteriores no veía, y la guarda ya las cubre para esta eliminación: una regla dentro de una media query que gana a una
regla base que se queda (`.nav-justified > li { display: table-cell }` sobre `.nav > li`), una regla sobre todos los elementos (`body *`) y las variantes
de un mismo componente escritas en un elemento (`navbar` con `navbar-default` y `navbar-right`, o una clase que AWE nunca escribe pero un producto sí,
como `<a class="navbar-brand">` frente a las reglas de `a`). Esta etapa añade además tres guardas:

- **`!important`.** Un `!important` en capa gana a cualquier `!important` sin capa, sea cual sea su especificidad. `compatFamily.js` busca las
  declaraciones `!important` servidas con las que una regla que se queda en LESS (mismo media, una propiedad solapada, otro valor) podría coincidir
  en un elemento, y falla salvo que el selector se excluya con un motivo: `.open .btn.dropdown-toggle` y `.nav-tabs.nav-justified > li > a`. También
  modela una declaración que otra normal de la misma regla repite (`float: left !important; float: left` en `.navbar-left`): el `!important` se
  mantiene, y se congela con la regla (`shadowed`).
- **`url()`.** Un fichero de compatibilidad no escribe ningún `url()`: una ruta relativa se resuelve desde la carpeta del fichero que la escribe,
  así que `.navbar-header h1` (el logo) sigue en LESS.
- **Orden.** `compatSlice5Order.test.js` fija el orden de los imports de `compat/index.css` al que tenía `bootstrap.less`.

**Variables LESS.** Los restos (y los ficheros de compatibilidad) guardan los valores tal como compilaron, así que un valor que venía de una variable
LESS de Bootstrap queda congelado: el tamaño de fuente de `.close` (`19.5px`), los colores de los enlaces activos de desplegables y navs (`#337ab7`),
etc. Un producto que sobrescriba esas variables con `modifyVars` o con un LESS propio ya no las cambia. Los valores escritos como propiedades CSS
personalizadas (`var(--navbar-text-color, #9D4444)` y las demás `--*` de los temas) siguen el tema en ejecución; para cambiar un valor congelado,
escriba su propia regla sin capa.

La guarda de cascada (`cascadeGuard.test.js`) cubre ahora la tercera eliminación con la misma coincidencia amplia que la segunda (clases escritas
juntas, reglas de elemento, un atajo frente a su propiedad individual) más los tres casos anteriores, y no encontró ningún par pendiente. Su pixel diff de las pantallas de AWE
(ajustes y el módulo Test, Chromium y Firefox, 1440 y 375 píxeles de ancho, tema claro y oscuro, con los desplegables de avatar e info abiertos, el
menú de desbordamiento de pestañas, el diálogo de confirmación, los diálogos, los paneles laterales, el acordeón, el popover de los mensajes y la
barra móvil) es idéntico al anterior a la eliminación. `vendoredRemoval.test.js` comprueba que los ficheros no están, que ninguna regla congelada
está en el build, que cada resto sigue estándolo, que el build es menor y que ninguna llamada ni extend de un conjunto de reglas eliminado se queda
sin su mixin. El slice 6 elimina los restos junto con las reglas de pixel-admin con las que compiten.

**Tablas, etiquetas, insignias, wells, alertas, migas de pan, barras de progreso y paginación (slice 6).** `bootstrap.less` ya no importa `tables.less`,
`labels.less`, `badges.less`, `wells.less`, `alerts.less`, `breadcrumbs.less`, `progress-bars.less` y `pagination.less` (los ficheros se han borrado). Importa
`tables-leftovers.less`, `breadcrumbs-leftovers.less`, `pagination-leftovers.less`, `labels-leftovers.less`, `badges-leftovers.less`,
`alerts-leftovers.less` y `wells-leftovers.less` en los mismos sitios; las barras de progreso no dejan nada, así que su import ha desaparecido. Las reglas de
compatibilidad son iguales a las vendorizadas compiladas (copias de Bootstrap, de pixel-admin y de AWE de cada selector: `compatTables.test.js` y sus siete
hermanos, uno por familia, comparten `support/compatFamily.js`, con los selectores de AWE 5 del toast de mensajes y de la barra de migas declarados como `owned` del LESS
de AWE, y los `!important` del borde de impresión de la tabla con borde y del enlace deshabilitado de la paginación en hover excluidos como riesgos). De las 263
reglas compiladas de los ocho ficheros, solo 42 salieron de la compilación y 221 se quedan literales en los restos (las listas exactas están en la entrada del
paso 4 de `support/vendored-removed.baseline.json`), por la misma razón que en las eliminaciones anteriores: las reglas de elementos (`th`, `td`, `a`, `h2 span`), los plugins
(los selectores de fecha, DataTables, las barras de desplazamiento) y los componentes de pixel-admin y de AWE rediseñan los mismos elementos con reglas sin capa que una
regla de compatibilidad en capa perdería. Dos detalles son propios de esta eliminación:

- **Movimiento reducido.** La guarda de cascada encontró la animación de rayas de una barra de progreso activa (`.progress.active .progress-bar`, `.progress-bar.active`) en
  conflicto con las animaciones de los cargadores, de los marcadores del esqueleto y de la raíz de vista ocupada (`.loader *`, `.screen-skeleton > *`,
  `[ui-view][aria-busy="true"]`). Es la única exclusión revisada del paso (`cascadeGuard.test.js` explica por qué ningún elemento es ambas cosas: ninguna plantilla escribe una barra de
  progreso dentro de ellos), así que la animación sale del LESS y la regla de compatibilidad la sirve con `--awe-motion-duration-slow`, que es `0ms` bajo `prefers-reduced-motion`.
  `compatProgressBars.test.js` fija que el fichero de compatibilidad no tiene un tiempo de animación fijo.
- **Una regla que imprimen dos ficheros.** `.list-group-item > .badge + .badge` la escriben Bootstrap y pixel-admin con las mismas declaraciones. La línea base cuenta las
  reglas por identidad, así que la copia de Bootstrap se queda en `badges-leftovers.less` (la compilación es la vendorizada menos las reglas congeladas, y un test lo fija).

Los valores de los restos están congelados tal como compilaron (véase *Variables LESS* arriba): un producto que sobrescribe `@table-border-color`, `@label-default-bg` o similares con
`modifyVars` ya no cambia los restos ni las reglas de compatibilidad. `vendoredRemoval.test.js` comprueba que los ficheros no están, que ninguna regla congelada está en la compilación, que cada
resto sigue estando, que la compilación es menor y que no queda ninguna llamada ni extend de un conjunto de reglas eliminado sin su mixin. Los mixins de `less/legacy/mixins` que
los ficheros llamaban (`.label-variant()`, `.alert-variant()`, `.progress-bar-variant()`, `.table-row-variant()`, `.pagination-size()`) se quedan, y los mixins `.tables-theme`, `.labels-theme`,
`.badges-theme`, `.progress-bars-theme` y `.pagination-theme` de pixel-admin que `less/awe/base.less` llama dentro de un comentario siguen compilando.

**Pager, objeto media, jumbotron, carrusel y fuente de iconos (slice 7).** `bootstrap.less` ya no importa `pager.less`, `media.less`, `jumbotron.less`,
`carousel.less` ni `glyphicons.less`, y los ficheros se eliminan, junto con los ficheros de la fuente `glyphicons-halflings-regular` del cliente
(`libs/bootstrap-3.3.2/fonts`, una carpeta que ya no existe, véase el slice 9) y la copia sin referencias de `awe-tools` (`src/main/resources/fonts`; el bundle del gestor de ficheros compila su propio Bootstrap
desde npm). También desaparecen el `theme.less` huérfano (el tema opcional de Bootstrap, que nadie importa) y el helper `.pre-scrollable` de `code.less`. **No hay CSS
de compatibilidad para ninguno**: ninguna plantilla, directiva, pantalla XML, plantilla `.stg`, gestor de ficheros ni fichero React escribe sus clases
(`unusedBootstrapFamilies.test.js`; el cliente React tiene sus propios estilos y su clase `carousel` es el widget de PrimeReact con los estilos de `AweCarousel.less`).
Un producto que escriba `media`, `jumbotron`, `carousel`, `pager`, `glyphicon` o `pre-scrollable` en sus pantallas o plantillas debe aportar ese CSS él mismo, y su LESS que
llame o extienda uno de sus conjuntos de reglas pierde esas declaraciones sin avisar. Las 367 reglas compiladas quedan congeladas en el paso 5 de
`support/vendored-removed.baseline.json`, marcado `unserved` (los tests de compat las saltan: ningún fichero de compat las sirve a propósito). Las reglas de elemento de
`code.less` (`code`, `kbd`, `pre`, `samp`) son estilos base: la capa de compatibilidad los sirve desde el slice 8 (ver abajo), y solo el bloque de código (`pre`) se queda, en `pre-leftovers.less`. `list-group.less`, `thumbnails.less` y `responsive-embed.less` los sirve la capa de compatibilidad en lugar de eliminarse, porque algo aún los escribe: la pantalla de ajustes
del notificador escribe `list-group` y `list-group-item`, y el gestor de ficheros de `awe-tools` escribe `embed-responsive` y el `thumbnail` de su marcado de terceros
(`unusedBootstrapFamilies.test.js` fija cada uso y su fichero de compatibilidad, así que el test falla el día que el uso desaparezca y el fichero podrá seguirlo). Están en
el paso 6 de la línea base (servido, a diferencia del paso 5), y `bootstrap.less` importa `thumbnails-leftovers.less`, `list-group-leftovers.less` y
`responsive-embed-leftovers.less` donde estaban los ficheros. Solo 2 de sus 83 reglas compiladas salieron de la compilación (`.list-group-item:first-child`,
`.thumbnail .caption`); 81 se quedan literales (entre ellas `.list-group`: el margen de `ul, ol` de la tipografía, que sigue en el LESS, ganaría a una regla en capa sobre un `ul.list-group`), por la razón de las eliminaciones anteriores: las reglas de elemento, pixel-admin y los plugins dan estilo a los mismos elementos con
reglas sin capa que una regla de compatibilidad en capa perdería. Los restos de miniaturas mantienen el `&:extend(.img-responsive)` del código de Bootstrap, así que la regla que
`.img-responsive` imprime para las imágenes de miniatura se queda donde estaba. El mixin `.list-groups-theme` de pixel-admin que `less/awe/base.less` llama dentro de un comentario,
las reglas `.panel > .list-group` y las reglas `.list-group-item > .badge` de los restos de paneles e insignias siguen compilando. El gestor de ficheros de awe-tools compila su propio
Bootstrap desde npm y mantiene sus sobrescrituras de `.iconset .thumbnail`.

**Estilos base (slice 8).** `normalize.less`, `print.less`, `scaffolding.less`, `type.less` y las reglas de elemento de `code-leftovers.less` las sirve la capa de compatibilidad
(`normalize.css`, `print.css`, `scaffolding.css`, `type.css` y `code.css`, importados los primeros, donde los compilaba la compilación vendorizada). Sus reglas son sobre todo reglas de elemento
(`body`, `a`, `h1`, `p`, `ul`, `code`...), que ninguna clase encuentra, así que cada test de familia lista los selectores exactos en que compiló su fichero vendorizado
(`support/baseStyles.selectors.json`), y dos selectores de elemento que ningún elemento puede cumplir a la vez (`code` y `fieldset`) no cuentan como competidores en la comprobación del orden de la
cascada. De las 300 reglas compiladas, 226 salieron de la compilación (paso 7 de `support/vendored-removed.baseline.json`: normalize 64, print 12, scaffolding 15, type 127, code 8) y 74 se quedan literales
en `normalize-leftovers.less`, `print-leftovers.less`, `scaffolding-leftovers.less`, `type-leftovers.less` y `pre-leftovers.less` (`code-leftovers.less` desaparece); la compilación LESS pasó de 815487 a 808896 bytes.
Lo que se queda, además de las reglas que la guarda de cascada encontró que una regla sin capa de pixel-admin o de AWE vencería: `html` (su tamaño de 10px es la base de la que se calcula toda regla en rem del
LESS de AWE y de los plugins, y ningún token lo contiene), las reglas de impresión que imprimen en negro sobre blanco (un color o fondo con `!important` y bordes grises o negros fijos: ningún token es el mismo en el
modo oscuro, y un `!important` en capa ganaría a los sin capa de los estilos de impresión de AWE), `.img-responsive` (las miniaturas lo extienden), `.text-success`, `.text-info`, `.text-warning` y
`.text-danger` (`icon.less` los llama como conjuntos de reglas, y Less descarta sin aviso una llamada cuyo conjunto ya no existe), `.text-left`, `.text-right`, `.text-center` y `.sr-only-focusable` (la compilación
los imprime más de una vez y la línea base necesita cada copia). Los valores que salieron siguen a los tokens, así que difieren un poco de los compilados de Bootstrap: los títulos leen la escala tipográfica (33, 27,
23, 17, 13 y 12px pasan a `--awe-font-size-3xl`, `2xl`, `xl`, `lg`, `base` y `xs`; el peso 500 y el interlineado 1.1 pasan al peso medio y al interlineado ajustado), el cuerpo lee el tamaño e interlineado base (1.428
pasa a 1.5), los enlaces y las clases de texto y fondo contextuales leen los tokens de texto y suaves de primario, éxito, info, aviso y peligro, los márgenes de 9 y 18px pasan a `--awe-space-2` y `--awe-space-5` (8 y 20px),
la barra de la cita (5px) es el grosor de borde fuerte (2px), `code`, `kbd` y `pre` leen los tokens de peligro, contraste y superficie suave, y `.lead` pesa 400 en lugar de 300. Los desplazamientos de `dl-horizontal`
(160 y 180px) se escriben a partir de los tokens de espacio y siguen siendo exactos. No hizo falta ningún token nuevo.

**LESS heredado de Bootstrap 3 (slice 9).** Lo que queda de la compilación vendorizada de Bootstrap 3 ya no vive en `libs/bootstrap-3.3.2`: es LESS propio de AWE en `less/legacy`, que `awe.less`
importa mediante `legacy/legacy.less` (el antiguo `bootstrap.less`, con los mismos imports en el mismo orden). La carpeta contiene `variables.less`, `mixins.less` y `mixins/`, `grid-columns.less` y los
28 ficheros `*-leftovers.less`, movidos con `git mv` y sin cambios: el `styles.css` compilado es idéntico byte a byte al anterior al movimiento. El aviso de licencia MIT de Bootstrap es la cabecera de
`legacy.less` y de `legacy/LICENSE-bootstrap.txt`. Donde esta guía dice `bootstrap.less`, léase `legacy/legacy.less`; la línea base congelada guarda solo nombres de fichero, así que sus digests no cambiaron.

**pixel-admin ya no es una librería vendorizada (último paso de #755).** El LESS de pixel-admin que importa la compilación (67 ficheros: los ficheros principales, los plugins y el widget de notificaciones)
es LESS propio de AWE en `less/legacy/pixel-admin`, que `awe.less` importa en el mismo orden mediante `legacy/pixel-admin/pixel-admin.less`; `libs/pixel-admin-less` ya no existe. Se eliminaron el demo,
las páginas, las reglas rtl, los demás widgets y las imágenes a las que no apunta ninguna regla compilada; las dos imágenes que una regla aún alcanza quedan en `legacy/pixel-admin/images`, y el sprite de
minicolors pasó a `legacy/images/plugins`, de modo que el `styles.css` compilado es idéntico byte a byte al anterior al cambio. Donde esta guía dice `libs/pixel-admin-less`, léase `legacy/pixel-admin`.
`NOTICE-pixel-admin.txt` conserva la procedencia de los ficheros.

**La regla de la migración: mientras se entregue el LESS de pixel-admin y de AWE, una regla de compatibilidad solo toma el relevo
donde no compite ninguna regla sin capa.** Una regla de compatibilidad pierde frente a cualquier regla sin capa, sea cual sea su
especificidad, así que una regla vendorizada de Bootstrap que ganaba a una regla que se queda en el LESS (por especificidad o por
orden) tiene que quedarse también en el LESS, hasta que la fase 6 elimine aquello con lo que competía. `cascadeGuard.test.js` lo
impone: lista cada regla eliminada que ganaba a una regla aún vendorizada en la misma propiedad con otro valor y una clase en común, y
falla salvo que el par esté revisado (los colores contextuales de los paneles, que nunca coinciden en un elemento). Dos casos que
encontró el diff de píxeles: el acordeón, cuyas reglas `.panel-group` (sin margen entre paneles, sin borde bajo la cabecera) ganaban a
las reglas `.panel` y `.panel-heading` de pixel-admin y ahora se quedan enteras en `panels-leftovers.less`, y la regla base de las
columnas: Bootstrap daba a cada columna `min-height: 1px` (para que una columna vacía usada como separador mantenga su sitio) y
AWE tiene `body * { min-height: 0 }`; como regla en una capa el 1px perdía, así que `grid-columns.less` mantiene esa regla base
(`position`, `min-height`, paddings laterales) en LESS. Otras reglas de elemento (`td`, `th`, `fieldset`, `legend` con padding o
width, `colorpicker-saturation`) ganan ahora también a un `col-*` escrito en el mismo elemento; ninguna pantalla de AWE lo hace,
pero un producto podría.

Las utilidades siguen el CSS vendorizado compilado (`.affix`, la clase del plugin affix de Bootstrap, no se reimplementa: nada la
usa, y desaparece con la eliminación). Bootstrap escribe `pull-*`,
`hide`, `show`, `hidden`, `hidden-*` y `visible-*` con `!important`, y con `!important` el orden de las capas se invierte: un
`!important` en capa gana a uno sin capa. Por eso estas reglas de compatibilidad **no** son inertes junto al LESS vendorizado: en
un elemento que además lleve una clase con un `!important` sin capa de otro valor (`.stat-cell`, `.wizard-steps`, un `ng-hide`
de Angular...), ganaría la regla de compatibilidad. Nada en el framework las mezcla. `compatUtilities.test.js` falla si una
lista de clases escrita en un atributo de una plantilla, directiva o pantalla empieza a hacerlo, pero no ve las clases que añade
`ng-class` o el JavaScript, ni la regla `ng-hide` que Angular inyecta: un producto que escriba `show` o `visible-*` en un elemento con
`ng-show` no debería depender aún de `ng-show`.

Las clases de espaciado y tipografía conservan los valores compilados y su `!important` (espaciado, bordes, alineación, alturas y
grosores de fuente; no los tamaños, `text-underline` ni los colores). Quedan fuera `text-success|danger|warning|info`
(fase de feedback). Dos elementos ya mezclan una clase con un `!important` sin capa de otro valor
(`panel-padding no-padding-t`, `no-border panel-transparent`); ambos resuelven al mismo valor calculado y `compatSpacing.test.js`
los enumera. Los navegadores
sin `color-mix()` reciben el color base en `text-default a:hover` y `text-light-gray a:hover` (la compilación añade un respaldo); la
regla vendorizada no les daba ninguno.

Los paneles conservan los valores compilados; los temáticos quedan como `var(--panel-background-color, #fff)` con el respaldo que
compiló LESS, de modo que un tema sigue fijando la variable (no se escriben como tokens `--awe-*`: un token se resuelve en el
elemento del tema y una variable redefinida más abajo en el árbol ya no llegaría a la regla). `compat/index.css` importa
`panels.css` **antes** de `spacing.css`: `panel-padding no-padding-t` aparece en pantallas, ambos son `!important` con la misma
especificidad y `no-padding-t` debe seguir ganando; `compatPanels.test.js` fija el orden. De momento quedan en LESS (los de Bootstrap están en `panels-leftovers.less`) los colores
contextuales que nadie usa (`panel-primary|success|warning|danger`, `panel-dark`), los acordeones de color
(`panel-group-info|success|...`), las tablas y listas dentro de un panel, los selectores que dependen de una clase de tema del
body (`.theme-* .panel`) y las directivas AWE de panel, ventana y asistente (`panel-awe`, `panel-sm|md|lg`, `maximized`,
`wizard-panel`). `compatPanels.test.js` nombra cada uno.

Los botones y los grupos de botones (`buttons.css` y `button-groups.css`, importados en ese orden) son declaraciones directas en el orden del build vendorizado (muchas reglas compiten con la misma especificidad: los colores de
pixel-admin sobre los de Bootstrap, el botón por defecto de AWE sobre ambos). Los valores de tema siguen siendo `var(--secondary-color, #e8e6e6)` con el valor
alternativo compilado. Los selectores `:hover`, `:focus`, `:active` y `.active` de `.btn` y de `.btn-{default,info,success,warning,danger}` siguen en LESS: pixel-admin les
da `background: ... !important` y todo botón de AWE (`btn btn-awe btn-*`, `services/button.js`) tiene un fondo `!important` de tema con más especificidad. Un `!important` en capa
gana a uno sin capa sea cual sea su especificidad, así que servirlos repintaría todos los botones de AWE. `compatButtons.test.js` nombra lo que queda y fija el motivo. El `!important` que sí se mueve, `.btn { outline: none }` y el `display: none` de impresión, queda en capa, así que vence a cualquier `!important` sin capa de un producto sobre un botón.

Los formularios son dos ficheros, en el orden en que los compila el build vendorizado: `forms.css` (`forms.less` de Bootstrap) se importa antes de las utilidades, los paneles, el espaciado y los botones, y
`forms-pixel-admin.css` (los overrides de pixel-admin sobre los mismos selectores) después de los botones y los grupos de botones. Los valores son los compilados, los valores de tema conservan `var(--text-color, #000000)`
con su fallback compilado y ninguno de los dos ficheros contiene `!important`. Los pseudoelementos de placeholder con prefijo de fabricante son reglas separadas, porque el navegador descarta una regla entera si un selector
le es desconocido. Siguen en LESS: los selectores de elemento globales de `forms.less` (`fieldset`, `legend`, `label`, `output`, `input[type=...]`), los formularios de navbar, las de
`.has-*.dark|.simple` y los controles personalizados `.lbl`, los selectores `.has-* .form-control:focus` y `.form-control.focus` (pixel-admin escribe `border-color: ... !important` y las reglas de tema `.has-*.dark` también lo llevan), `form-actions` (solo se estiliza
dentro de las pantallas de acceso, cambio de contraseña, TOTP y cierre de sesión SSO) y todo lo acotado a un criterio, un tema, un id o un plugin. `compatForms.test.js` nombra cada uno de ellos.

Los grupos de entrada son `input-groups.css` (`input-groups.less` de Bootstrap, importado después de los botones y los grupos de botones y antes de `forms-pixel-admin.css`, como lo ordena el build vendorizado) e `input-groups-pixel-admin.css` (los overrides de pixel-admin, importado después de `forms-pixel-admin.css`). Los valores son los compilados, los valores de tema conservan `var(--x, fallback)` y ninguno de los dos ficheros contiene `!important`. Los addons `.has-*.dark` y `.no-background` llevan un fondo `!important`, y las reglas de criterio, select2 y selectores de fecha/color están acotadas a su componente, así que siguen en LESS; `compatInputGroups.test.js` da cuenta de cada selector vendorizado que menciona un grupo de entrada con selectores de ejemplo. La clase `add-on` que escriben las directivas de fecha no tiene regla en Bootstrap 3: solo la estilan los plugins de selector de color y x-editable.

Las esquinas de la cabecera y del pie se escriben como `0` a propósito: el `calc(var(--border-radius) - 1)` de Bootstrap (una longitud
menos un número) no es válido y hoy se calcula como 0, así que no cambia nada. `.panel-body` tiene un padding de 20px (pixel-admin sobre
Bootstrap) y los tamaños `panel-sm|md|lg` de AWE siguen siendo más específicos que la regla del compat.

El grid de AWE mantiene sus propios números, no los de Bootstrap por defecto: un **gutter de 22px** (11px por lado) y
contenedores de **742, 962 y 1162px**. El grid es una lista fija, porque un framework no puede escanear el XML de cada
producto. Se genera:

```bash
npm run compat:grid --prefix awe-framework/awe-client-angular
```

## Comprobar un cambio {#checking-a-change}

| Test | Qué demuestra |
|------|---------------|
| `tailwindBuild.test.js` | Orden de capas, sin preflight, solo utilidades `tw:`, sin tema por defecto, todos los tokens mapeados, las carpetas escaneadas existen y el escaneo llega a XML, plantillas y directivas |
| `compatGrid.test.js` | El fichero del grid generado está al día y cada clase del grid equivale a Bootstrap vendorizado (4 breakpoints x 12 columnas, offsets, push, pull, container, row) |
| `compatUtilities.test.js` | Cada clase de utilidad equivale a la regla vendorizada compilada por media query, las utilidades de las pantallas XML están servidas y ningún elemento del framework mezcla una utilidad con una clase cuyo `!important` sin capa perdería ahora |
| `compatSpacing.test.js` | Cada clase de espaciado, borde, alineación, altura y tipografía equivale a la regla vendorizada compilada (selectores, `!important`, media), las clases XML de la familia están servidas, no falta ninguna de la familia y las mezclas de `!important` revisadas son las únicas |
| `compatPanels.test.js` | Cada clase de panel y sus selectores descendientes equivalen a la regla vendorizada compilada, cada clase de panel de las plantillas, directivas y pantallas está servida o excluida con motivo, `panels.css` se importa antes que `spacing.css` y las mezclas de `!important` revisadas son las únicas |
| `vendoredRemoval.test.js` | Los cuatro ficheros vendorizados no existen y ninguna de sus reglas compiladas está en la compilación vendorizada, cada selector eliminado lo sirve la capa de compatibilidad (salvo `.affix`), cada clase de ellos que escribe una plantilla, directiva o pantalla está servida, ningún LESS extiende ni llama a un conjunto de reglas eliminado (Less los descarta sin avisar) y la compilación LESS es más pequeña |
| `compatInputGroups.test.js` | Cada selector de grupo de entrada (y los de formulario y grupo de botones que mencionan uno) equivale a la regla vendorizada compilada (selectores, media), los valores de tema conservan su `var()`, no se sirve ningún `!important`, cada selector que se deja fuera tiene su motivo con selectores de ejemplo, las reglas que compiten con la misma especificidad conservan el orden vendorizado y ambos ficheros se importan donde los compila el build vendorizado |
| `compatForms.test.js` | Cada selector de control, maquetación, checkbox, radio y estado de validación de formulario equivale a la regla vendorizada compilada (selectores, media), los valores de tema conservan su `var()` con el fallback, no se sirve ningún `!important`, las clases de formulario de las plantillas, directivas y pantallas se sirven o se dejan fuera por un motivo declarado, las reglas que compiten con la misma especificidad conservan el orden vendorizado y ambos ficheros se importan donde los compila el build vendorizado |
| `compatButtons.test.js` | Cada selector de botón y de grupo de botones equivale a la regla vendorizada compilada (selectores, `!important`, media), los valores de tema conservan su `var()` con valor alternativo, las clases de botón de plantillas, directivas y pantallas están servidas o excluidas con motivo, las reglas que compiten con la misma especificidad mantienen el orden vendorizado y las mezclas de `!important` revisadas son las únicas |
| `cascadeGuard.test.js` | Ninguna regla que sirve una regla de compatibilidad ganaba a una regla que se queda en el LESS (sin capa) con otro valor en la misma propiedad, salvo los pares revisados |
| `xmlClasses.test.js` | Cada clase escrita en el `style` de una pantalla XML la sirve la capa de compatibilidad o el CSS vendorizado, o es un marcador conocido sin regla CSS |

### Las reglas vendorizadas congeladas {#the-frozen-vendored-rules}

Los tests de equivalencia comparan la capa de compatibilidad con la regla vendorizada de Bootstrap compilada. Cuando una familia
sale del LESS, la regla con la que se comparaba desaparece, así que las reglas eliminadas se **congelan** en
`src/test/jest/styles/support/vendored-removed.baseline.json` (generado desde el árbol anterior a la eliminación, con su posición
en la compilación) y los tests las restauran junto a la compilación actual (`support/vendoredBeforeRemoval.js`). Un cambio normal
no toca ese fichero. Para generarlo de nuevo, desde el árbol anterior a la eliminación:

```bash
git archive <commit anterior a la eliminación> awe-framework/awe-client-angular/src/main/resources/less | tar -x -C /tmp/before
npm run compat:freeze-vendored --prefix awe-framework/awe-client-angular -- /tmp/before/awe-framework/awe-client-angular/src/main/resources/less
```

El script falla si la compilación posterior a la eliminación tiene alguna regla que la anterior no tenía.

## Diff de píxeles de la eliminación {#pixel-diff-of-the-removal}

La fase 2d eliminó las cuatro familias sin cambio visual. La prueba es un diff de píxeles con umbral 0 de la aplicación
(`awe-tests/awe-boot`, una sola instancia, mismos datos): las pantallas se capturan con el `styles.css` del árbol anterior a la
eliminación (intercambiado) y con el posterior, en Chromium y Firefox, a 1440x900 y 375x812, con el tema claro y el oscuro.
`awe-framework/awe-client-angular/tools/pixel-diff/` contiene el script (Playwright y su propia comparación de PNG); su README
explica cómo ejecutarlo. Resultado de la eliminación: 95 capturas (15 pantallas en el viewport de escritorio, 9 en el de móvil, cuyos menús están tras la hamburguesa), **95 idénticas, 0 píxeles distintos**. La primera ejecución encontró una diferencia real, una columna vacía sin su altura de 1px (ver arriba), corregida antes de la ejecución final.

`styles.css` (build de producción de webpack) pasó de 1.368.418 a 1.348.917 bytes (-19.501, -1,4 %): la eliminación solo quita las
reglas vendorizadas, las reglas de compatibilidad ya estaban en el bundle. El resto del presupuesto llega con las siguientes fases.

## Navegadores {#browsers}

Las capas de cascada necesitan Chrome 99, Firefox 97 o Safari 15.4. Desde la fase 2d importan: el grid, las clases de panel y las
utilidades de esta página las sirven solo reglas dentro de una capa, así que un navegador sin capas las ignora y las pantallas
pierden su maquetación. La fase que elimine el resto del Bootstrap vendorizado las convierte en la base del cliente AngularJS
para todo lo demás; el cambio se lista en la [guía de migración a AWE 5](v5-migration.md).
