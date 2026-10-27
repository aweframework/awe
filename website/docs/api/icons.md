---
id: icons
title: Icons
sidebar_label: Icons
---

AWE 5 draws its icons with [Lucide](https://lucide.dev/icons/) line icons (ISC licence) in both engines. Look for an
icon that suits the concept you want to show and put its name in the `icon` attribute (or in the value, for the
`icon` columns of a grid).

An icon name has the structure:

```
family:icon_name
```

Where family is one of the families described below. Without family the name is a Font Awesome 4 name.

Both engines render a Lucide icon the same way: an inline `svg` that uses the shared sprite
`icons/lucide-sprite.svg` (generated from the pinned `lucide-static` 1.61.0), with the size of the
`--awe-size-icon` tokens and the colour of the parent text (`currentColor`). The icons are decorative
(`aria-hidden="true"`): give an icon-only button a label or a tooltip. The element keeps the XML name in its
`data-icon` attribute.

Extra classes can follow the icon name, separated by spaces. The Font Awesome modifiers `fa-spin`, `fa-pulse` and
`fa-lg` become `awe-icon-spin` (no animation with reduced motion) and `awe-icon-lg` on a Lucide icon:

```
icon="refresh fa-spin"
```

## Font Awesome 4.7.0 icons (fa) {#font-awesome-470-icons-fa}

The default icon family (coming from AWE 3 version) uses the names of
[Font Awesome 4.7.0](https://fontawesome.com/v4/icons/). The existing screens need no change: every Font Awesome name
of the [equivalence table](#equivalence-table) is drawn with its Lucide icon. During the transition to AWE 5, a name
without equivalence keeps its Font Awesome font.

Put the name of the icon in the `icon` attribute without family, with the `fa-` prefix or with the `fa` family:

```
icon="check"
```

```
icon="fa:check"
```

## Material Design Icons (mdi) {#material-design-icons-mdi}

The `mdi` family uses the names of the
[Google Material Icons](https://fonts.google.com/icons?icon.style=Filled&icon.set=Material+Icons) ligatures. Every name
of the [equivalence table](#equivalence-mdi) is drawn with its Lucide icon; during the transition, a name without
equivalence keeps the Material Icons font.

Put the family name `mdi` before the icon name:

```
icon="mdi:home"
```

## Lucide icons (lucide) {#lucide-icons-lucide}

The `lucide` family gives the whole Lucide set of `lucide-static` 1.61.0: use the names of the
[Lucide icons](https://lucide.dev/icons/) page. Former Lucide names that the set keeps as aliases also work. Use this
family for new screens: the icon is exactly the one you choose, without equivalence.

```
icon="lucide:calendar-days"
```

An unknown Lucide name shows no icon, and the browser console warns once about it.

## Equivalence table {#equivalence-table}

The table lives with the design tokens (`awe-framework/awe-design-tokens/icons/icon-equivalences.json`) and both
engines and the server templates read it. It covers every icon name used in the AWE screens.

### Font Awesome {#equivalence-fa}

| XML icon               | Lucide                 |
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

| XML icon              | Lucide                 |
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

### Names without equivalence {#equivalence-fallback}

These names have no Lucide equivalence. During the transition the brand icons keep their Font Awesome font; the
other names are not Font Awesome 4 icons, so they show nothing, as they did before AWE 5:

| XML icon       | Reason                                                                         |
|----------------|--------------------------------------------------------------------------------|
| `clock`        | not a Font Awesome 4 icon (`clock-o` is): renders nothing today                |
| `mail-closed`  | not a Font Awesome 4 icon: renders nothing today                               |
| `ok-sign`      | Glyphicons name, not Font Awesome 4: renders nothing today                     |
| `rebel`        | brand icon: Lucide has no brand icons                                          |
| `remove-sign`  | Glyphicons name, not Font Awesome 4: renders nothing today                     |
| `signin`       | Glyphicons-like name, not Font Awesome 4 (`sign-in` is): renders nothing today |
| `squarebar`    | not a Font Awesome 4 icon: renders nothing today                               |
| `youtube-play` | brand icon: Lucide has no brand icons                                          |
