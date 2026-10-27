---
id: icons
title: Iconos
sidebar_label: Iconos
---

AWE 5 dibuja sus iconos con los iconos de línea [Lucide](https://lucide.dev/icons/) (licencia ISC) en los dos
motores. Busque un icono que se ajuste al concepto que quiere mostrar e indique su nombre en el atributo `icon` (o en
el valor, en las columnas `icon` de un grid).

El nombre de un icono tiene la estructura:

```
family:icon_name
```

Donde family es una de las familias descritas a continuación. Sin familia, el nombre es un nombre de Font Awesome 4.

Los dos motores muestran un icono Lucide de la misma forma: un `svg` en línea que usa el sprite compartido
`icons/lucide-sprite.svg` (generado a partir de `lucide-static` 1.61.0, versión fijada), con el tamaño de los
tokens `--awe-size-icon` y el color del texto padre (`currentColor`). Los iconos son decorativos
(`aria-hidden="true"`): dé una etiqueta o un tooltip a un botón que solo tenga icono. El elemento conserva el nombre
del XML en su atributo `data-icon`.

Tras el nombre del icono pueden ir clases adicionales, separadas por espacios. Los modificadores de Font Awesome
`fa-spin`, `fa-pulse` y `fa-lg` pasan a ser `awe-icon-spin` (sin animación con movimiento reducido) y `awe-icon-lg` en
un icono Lucide:

```
icon="refresh fa-spin"
```

## Iconos Font Awesome 4.7.0 (fa) {#font-awesome-470-icons-fa}

La familia de iconos por defecto (heredada de la versión 3 de AWE) usa los nombres de
[Font Awesome 4.7.0](https://fontawesome.com/v4/icons/). Las pantallas actuales no necesitan cambios: cada nombre de
Font Awesome de la [tabla de equivalencias](#equivalence-table) se dibuja con su icono Lucide. Durante la transición a
AWE 5, un nombre sin equivalencia mantiene la fuente de Font Awesome.

Indique el nombre del icono en el atributo `icon` sin familia, con el prefijo `fa-` o con la familia `fa`:

```
icon="check"
```

```
icon="fa:check"
```

## Material Design Icons (mdi) {#material-design-icons-mdi}

La familia `mdi` usa los nombres de las ligaduras de
[Google Material Icons](https://fonts.google.com/icons?icon.style=Filled&icon.set=Material+Icons). Cada nombre de la
[tabla de equivalencias](#equivalence-mdi) se dibuja con su icono Lucide; durante la transición, un nombre sin
equivalencia mantiene la fuente de Material Icons.

Indique el nombre de la familia `mdi` antes del nombre del icono:

```
icon="mdi:home"
```

## Iconos Lucide (lucide) {#lucide-icons-lucide}

La familia `lucide` ofrece el conjunto completo de Lucide de `lucide-static` 1.61.0: use los nombres de la página
de [iconos de Lucide](https://lucide.dev/icons/). También funcionan los nombres antiguos de Lucide que el conjunto
mantiene como alias. Use esta familia en las pantallas nuevas: el icono es exactamente el que elige, sin equivalencia.

```
icon="lucide:calendar-days"
```

Un nombre de Lucide desconocido no muestra ningún icono, y la consola del navegador avisa una vez.

## Tabla de equivalencias {#equivalence-table}

La tabla está junto a los tokens de diseño (`awe-framework/awe-design-tokens/icons/icon-equivalences.json`) y la leen
los dos motores y las plantillas del servidor. Cubre todos los nombres de icono que usan las pantallas de AWE.

### Font Awesome {#equivalence-fa}

| Icono del XML          | Lucide                 |
|------------------------|------------------------|
| `align-justify`        | `text-align-justify`   |
| `align-left`           | `text-align-start`     |
| `align-right`          | `text-align-end`       |
| `angle-double-down`    | `chevrons-down`        |
| `angle-double-up`      | `chevrons-up`          |
| `angle-down`           | `chevron-down`         |
| `angle-up`             | `chevron-up`           |
| `area-chart`           | `chart-area`           |
| `arrow-circle-left`    | `circle-arrow-left`    |
| `arrow-circle-o-left`  | `circle-arrow-left`    |
| `arrow-circle-o-right` | `circle-arrow-right`   |
| `arrow-circle-right`   | `circle-arrow-right`   |
| `arrow-circle-up`      | `circle-arrow-up`      |
| `arrow-down`           | `arrow-down`           |
| `arrow-left`           | `arrow-left`           |
| `arrow-right`          | `arrow-right`          |
| `arrow-up`             | `arrow-up`             |
| `ban`                  | `ban`                  |
| `bar-chart`            | `chart-column`         |
| `bar-chart-o`          | `chart-column`         |
| `bars`                 | `menu`                 |
| `bell`                 | `bell`                 |
| `bell-o`               | `bell`                 |
| `book`                 | `book`                 |
| `calculator`           | `calculator`           |
| `calendar`             | `calendar`             |
| `camera`               | `camera`               |
| `camera-retro`         | `camera`               |
| `caret-square-o-down`  | `square-chevron-down`  |
| `check`                | `check`                |
| `check-circle-o`       | `circle-check`         |
| `chevron-circle-left`  | `circle-chevron-left`  |
| `chevron-circle-right` | `circle-chevron-right` |
| `chevron-down`         | `chevron-down`         |
| `chevron-right`        | `chevron-right`        |
| `chevron-up`           | `chevron-up`           |
| `circle`               | `circle`               |
| `circle-o`             | `circle`               |
| `clipboard`            | `clipboard`            |
| `clock-o`              | `clock`                |
| `close`                | `x`                    |
| `cloud`                | `cloud`                |
| `code-fork`            | `git-fork`             |
| `cog`                  | `settings`             |
| `cogs`                 | `cog`                  |
| `columns`              | `columns-2`            |
| `comment`              | `message-circle`       |
| `compass`              | `compass`              |
| `compress`             | `shrink`               |
| `database`             | `database`             |
| `dollar`               | `dollar-sign`          |
| `dot-circle-o`         | `circle-dot`           |
| `download`             | `download`             |
| `edit`                 | `pencil`               |
| `ellipsis-v`           | `ellipsis-vertical`    |
| `envelope`             | `mail`                 |
| `envelope-o`           | `mail`                 |
| `eraser`               | `eraser`               |
| `exchange`             | `arrow-right-left`     |
| `exclamation-circle`   | `circle-alert`         |
| `exclamation-triangle` | `triangle-alert`       |
| `expand`               | `expand`               |
| `eye`                  | `eye`                  |
| `eye-slash`            | `eye-off`              |
| `eyedropper`           | `pipette`              |
| `file-text`            | `file-text`            |
| `file-text-o`          | `file-text`            |
| `filter`               | `funnel`               |
| `flash`                | `zap`                  |
| `flask`                | `flask-conical`        |
| `floppy-o`             | `save`                 |
| `folder`               | `folder`               |
| `font`                 | `type`                 |
| `gears`                | `cog`                  |
| `globe`                | `globe`                |
| `group`                | `users`                |
| `home`                 | `house`                |
| `hourglass-half`       | `hourglass`            |
| `image`                | `image`                |
| `inbox`                | `inbox`                |
| `info`                 | `info`                 |
| `info-circle`          | `info`                 |
| `key`                  | `key`                  |
| `keyboard-o`           | `keyboard`             |
| `language`             | `languages`            |
| `laptop`               | `laptop`               |
| `level-down`           | `corner-right-down`    |
| `level-up`             | `corner-right-up`      |
| `line-chart`           | `chart-line`           |
| `list`                 | `list`                 |
| `list-alt`             | `layout-list`          |
| `list-ol`              | `list-ordered`         |
| `list-ul`              | `list`                 |
| `lock`                 | `lock`                 |
| `long-arrow-right`     | `move-right`           |
| `magic`                | `wand-sparkles`        |
| `minus`                | `minus`                |
| `minus-circle`         | `circle-minus`         |
| `minus-square`         | `square-minus`         |
| `mobile`               | `smartphone`           |
| `object-group`         | `group`                |
| `paint-brush`          | `paintbrush`           |
| `paper-plane`          | `send`                 |
| `pause`                | `pause`                |
| `pencil`               | `pencil`               |
| `pencil-square-o`      | `square-pen`           |
| `picture-o`            | `image`                |
| `pie-chart`            | `chart-pie`            |
| `play`                 | `play`                 |
| `plug`                 | `plug`                 |
| `plus`                 | `plus`                 |
| `plus-circle`          | `circle-plus`          |
| `plus-square`          | `square-plus`          |
| `print`                | `printer`              |
| `puzzle-piece`         | `puzzle`               |
| `question`             | `circle-question-mark` |
| `question-circle`      | `circle-question-mark` |
| `recycle`              | `recycle`              |
| `refresh`              | `refresh-cw`           |
| `repeat`               | `rotate-cw`            |
| `retweet`              | `repeat-2`             |
| `rotate-left`          | `rotate-ccw`           |
| `rss`                  | `rss`                  |
| `save`                 | `save`                 |
| `search`               | `search`               |
| `server`               | `server`               |
| `shield`               | `shield`               |
| `sign-in`              | `log-in`               |
| `sign-out`             | `log-out`              |
| `sitemap`              | `network`              |
| `sliders`              | `sliders-horizontal`   |
| `sort`                 | `chevrons-up-down`     |
| `sort-alpha-asc`       | `arrow-down-a-z`       |
| `sort-alpha-desc`      | `arrow-down-z-a`       |
| `sort-asc`             | `chevron-up`           |
| `sort-desc`            | `chevron-down`         |
| `spinner`              | `loader-circle`        |
| `star-o`               | `star`                 |
| `step-forward`         | `step-forward`         |
| `stop`                 | `square`               |
| `table`                | `table`                |
| `tasks`                | `list-checks`          |
| `terminal`             | `terminal`             |
| `text-width`           | `letter-spacing`       |
| `th`                   | `grid-3x3`             |
| `th-large`             | `grid-2x2`             |
| `ticket`               | `ticket`               |
| `times`                | `x`                    |
| `tint`                 | `droplet`              |
| `trash`                | `trash`                |
| `undo`                 | `undo-2`               |
| `unlock-alt`           | `lock-open`            |
| `upload`               | `upload`               |
| `user`                 | `user`                 |
| `warning`              | `triangle-alert`       |
| `window-maximize`      | `maximize`             |
| `window-restore`       | `minimize`             |
| `wrench`               | `wrench`               |

### Material Icons (mdi) {#equivalence-mdi}

| Icono del XML         | Lucide                 |
|-----------------------|------------------------|
| `mdi:analytics`       | `chart-column`         |
| `mdi:arrow_back`      | `arrow-left`           |
| `mdi:auto_fix_high`   | `wand-sparkles`        |
| `mdi:autorenew`       | `refresh-cw`           |
| `mdi:backspace`       | `delete`               |
| `mdi:block`           | `ban`                  |
| `mdi:calendar_month`  | `calendar-days`        |
| `mdi:calendar_today`  | `calendar`             |
| `mdi:content_copy`    | `copy`                 |
| `mdi:dark_mode`       | `moon`                 |
| `mdi:developer_board` | `circuit-board`        |
| `mdi:donut_small`     | `chart-pie`            |
| `mdi:help`            | `circle-question-mark` |
| `mdi:home`            | `house`                |
| `mdi:insights`        | `chart-spline`         |
| `mdi:keyboard`        | `keyboard`             |
| `mdi:light_mode`      | `sun`                  |
| `mdi:map`             | `map`                  |
| `mdi:menu`            | `menu`                 |
| `mdi:person_outline`  | `user`                 |
| `mdi:print`           | `printer`              |
| `mdi:receipt_long`    | `receipt-text`         |
| `mdi:route`           | `route`                |
| `mdi:save`            | `save`                 |
| `mdi:science`         | `flask-conical`        |
| `mdi:search`          | `search`               |
| `mdi:storefront`      | `store`                |
| `mdi:table_chart`     | `table-2`              |
| `mdi:table_view`      | `table`                |
| `mdi:toys`            | `toy-brick`            |
| `mdi:vertical_split`  | `columns-2`            |
| `mdi:view_carousel`   | `gallery-horizontal`   |

### Nombres sin equivalencia {#equivalence-fallback}

Estos nombres no tienen equivalencia en Lucide. Durante la transición, los iconos de marca mantienen la fuente de Font
Awesome; los demás no son iconos de Font Awesome 4, así que no muestran nada, igual que antes de AWE 5:

| Icono del XML  | Motivo                                                                                         |
|----------------|------------------------------------------------------------------------------------------------|
| `clock`        | no es un icono de Font Awesome 4 (sí lo es `clock-o`): hoy no muestra nada                     |
| `mail-closed`  | no es un icono de Font Awesome 4: hoy no muestra nada                                          |
| `ok-sign`      | nombre de Glyphicons, no de Font Awesome 4: hoy no muestra nada                                |
| `rebel`        | icono de marca: Lucide no tiene iconos de marca                                                |
| `remove-sign`  | nombre de Glyphicons, no de Font Awesome 4: hoy no muestra nada                                |
| `signin`       | nombre al estilo de Glyphicons, no de Font Awesome 4 (sí lo es `sign-in`): hoy no muestra nada |
| `squarebar`    | no es un icono de Font Awesome 4: hoy no muestra nada                                          |
| `youtube-play` | icono de marca: Lucide no tiene iconos de marca                                                |
