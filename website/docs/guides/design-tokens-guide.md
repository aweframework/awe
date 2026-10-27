---
id: design-tokens
title: Design tokens and dark mode
sidebar_label: Design tokens
---

:::info AWE 5 direction
This page documents the names of the tokens that the AngularJS components read. Their values come from the AWE 5 token
file; the whole design system (every token, light/dark model, themes as primary + surface) is in
[Design system](../design-system/overview.md) and the [token reference](../design-system/tokens-reference.md).
:::

The AngularJS client exposes its look as CSS custom properties, the **design tokens**, named `--awe-*`. Components,
your own stylesheets, Tailwind (`@theme`) and any new client read the tokens; the theme and the dark mode only change
the values behind them. Both engines load the same AWE 5 token file, `awe-framework/awe-design-tokens/tokens.css`; the
AngularJS engine includes it in its stylesheet (`less/awe.less`). LESS and Bootstrap 3 are still there: their LESS
variables read the tokens.

## How a theme becomes tokens

```
theme name or properties   sunset, a database theme, awe.application.theme-primary...
      │  resolved by the server into primary + surface + mode + density
      ▼
<html> attributes          class="dark" | "light", data-awe-primary, data-awe-surface, data-awe-density
      │
      ▼
design token               --awe-color-primary        declared only in tokens.css, following the attributes
      │
      ▼
consumers                  LESS (@primary-color), components, Tailwind @theme, the React client
```

- There are no classic themes any more (AWE 5, issue #885). A theme name (`default`, `sunset`...) and a theme of the
  database are **presets**: the server resolves them, with the application properties and the choices of the user, into
  a primary, a surface, a mode and a density (see [theme configuration](../design-system/for-developers.md)).
- The engine renders that choice as attributes of `<html>`, and the token file gives every token its value for them. A
  primary or a surface given as a hex comes with its generated palette as `--awe-primary-*` or `--awe-surface-*`
  properties of `<html>`.
- The LESS variables of the engine read the tokens (`@primary-color` is `var(--awe-color-primary)`), so the Bootstrap 3
  and pixel-admin rules follow the theme, the mode and the density. A product that overrides a LESS variable in its own
  LESS keeps that value.
- The AWE 4 theme variables (`--primary-color`, `--panel-background-color`...), the `.theme-<name>` rules and
  `/css/themeVariables.css` are no longer used: setting them changes nothing. The `theme-<name>` class stays on the body
  only for the browser tests.

Read tokens, do not set them. To change the colours, choose a primary and a surface (a palette name or a hex).

## Theme mode: light, dark and the operating system

The mode is a class of `<html>`, next to the other theme attributes (`<html class="dark" data-awe-primary="indigo">`):

| `<html>` class | Meaning |
|----------------|---------|
| `dark` | The dark mode: chosen by the user (user menu, saved in the user settings) or set by the theme or `awe.application.theme-mode` |
| `light` | The light mode, chosen in the same way |
| none | The system mode: the operating system decides through `prefers-color-scheme` |

The tokens hold both values with `light-dark()`, and the engine sets `color-scheme` on `<html>` (`light dark`, or
`light` / `dark` with the class), so the explicit choice always wins over the operating system. The user menu of the
AngularJS engine still adds `dark` or `light` to the body; the engine then reads the appearance again from the server
and updates `<html>`.

### A product that must not follow the operating system

Set the mode of the application to `light` (or `dark`): `awe.application.theme-mode=light`. The user can still choose
the other mode in the user menu. The LESS variable `@awe-follow-os-color-scheme` no longer changes the AngularJS
engine, and the property `awe.application.theme-follow-os-color-scheme` was removed with `/css/themeVariables.css`.

## Token reference

The token names are a contract: they do not change between versions of the line. New tokens can be added. The values
are the ones of the AWE 5 token file, listed in the [token reference](../design-system/tokens-reference.md).

### Color

| Token | Meaning | LESS variable |
|-------|---------|---------------|
| `--awe-color-theme` | Accent color of the theme | `@theme-color` |
| `--awe-color-text` | Default text | `@text-color` |
| `--awe-color-primary` | Primary actions, selection, links of the theme | `@primary-color` |
| `--awe-color-on-primary` | Text over the primary color | `@primary-text-color` |
| `--awe-color-secondary` | Default buttons and neutral controls | `@secondary-color` |
| `--awe-color-on-secondary` | Text over the secondary color | `@secondary-text-color` |
| `--awe-color-background` | Page background | `@primary-background-color` |
| `--awe-color-surface` | Secondary surface (panel footers, inputs areas) | `@secondary-background-color` |
| `--awe-color-border` | Default border | `@border-color` |
| `--awe-color-shadow` | Color of the shadows | |
| `--awe-color-menu` | Main menu background | `@primary-menu-color` |
| `--awe-color-menu-accent` | Main menu hover and highlight | `@secondary-menu-color` |
| `--awe-color-menu-text` | Main menu text | `@menu-text-color` |
| `--awe-color-navbar` | Navbar background | `@primary-navbar-color` |
| `--awe-color-navbar-accent` | Navbar header | `@secondary-navbar-color` |
| `--awe-color-navbar-text` | Navbar text | `@navbar-text-color` |
| `--awe-color-navbar-dropdown` | Dropdown menus | `@navbar-dropdown-color` |
| `--awe-color-panel` | Panel and dialog background | `@panel-background-color` |
| `--awe-color-panel-header` | Panel header background | `@panel-header-color` |
| `--awe-color-panel-border` | Panel border | `@panel-border-color` |
| `--awe-color-panel-text` | Text over the panel header | `@panel-text-color` |
| `--awe-color-grid-header` | Grid header | `@grid-header-color` |
| `--awe-color-grid-border` | Grid borders | `@grid-border-color` |
| `--awe-color-skeleton` | Loading skeleton placeholders of grids, criteria, buttons and charts | |
| `--awe-color-skeleton-highlight` | Shimmer highlight of the loading skeletons | |
| `--awe-color-disabled` | Disabled control background | `@disabled-color` |
| `--awe-color-disabled-border` | Disabled control border | `@disabled-border-color` |
| `--awe-color-disabled-text` | Disabled control text | `@disabled-text-color` |
| `--awe-color-required` | Required mark (`@required-color`, the tint of the required fields, reads `--awe-color-highlight`) | `@required-color` |
| `--awe-color-loading-bar` | Loading bar | `@loading-bar-color` |
| `--awe-color-danger` | Error | `@danger-color` |
| `--awe-color-warning` | Warning | `@warning-color` |
| `--awe-color-success` | Success | `@success-color` |
| `--awe-color-info` | Information | `@info-color` |

### Typography

| Token | Meaning | LESS variable |
|-------|---------|---------------|
| `--awe-font-family-base` | Font of the application | `@font-family-base` |
| `--awe-font-family-monospace` | Code font | `@font-family-monospace` |
| `--awe-font-size-base` | Default font size | `@primary-font-size` |
| `--awe-font-size-small` | Small text | `@secondary-font-size` |
| `--awe-line-height-base` | Default line height | `@line-height-base` |
| `--awe-font-weight-normal` | Regular weight | |
| `--awe-font-weight-bold` | Bold weight | |

### Spacing, radius, shadow and size

| Token | Value | LESS variable |
|-------|-------|---------------|
| `--awe-space-2xs` | `2px` | |
| `--awe-space-xs` | `4px` | |
| `--awe-space-sm` | `8px` | |
| `--awe-space-md` | `12px` | |
| `--awe-space-lg` | `16px` | |
| `--awe-space-xl` | `24px` | |
| `--awe-radius` | Default radius (`6px`) | `@border-radius` |
| `--awe-radius-sm` | Small radius (`4px`) | |
| `--awe-radius-lg` | Large radius (`8px`) | |
| `--awe-radius-full` | Pills and circles | |
| `--awe-shadow-sm` | Small elevation | |
| `--awe-shadow-md` | Medium elevation | |
| `--awe-shadow-lg` | Large elevation | |
| `--awe-size-header-height` | Navbar height | `@main-navbar-height` |
| `--awe-size-menu-width` | Main menu width | `@main-menu-width` |

## Using the tokens

In your own CSS or LESS:

```css
.my-card {
  background: var(--awe-color-panel);
  color: var(--awe-color-text);
  border: 1px solid var(--awe-color-border);
  border-radius: var(--awe-radius);
  box-shadow: var(--awe-shadow-sm);
  padding: var(--awe-space-md);
}
```

They change with the theme, the mode and the density with nothing else to do. A product that has its own LESS variables
keeps them: they compile as before.

## Mapping into Tailwind

The tokens are what Tailwind v4 adopts (`@theme`, issue #755). Each token maps one to one, without renaming it:

```css
@theme inline {
  --color-primary: var(--awe-color-primary);
  --color-on-primary: var(--awe-color-on-primary);
  --color-surface: var(--awe-color-panel);
  --color-border: var(--awe-color-border);
  --radius-awe: var(--awe-radius);
  --shadow-awe-md: var(--awe-shadow-md);
  --spacing-awe-md: var(--awe-space-md);
}
```

Use `@theme inline` so the utilities keep the `var(--awe-...)` reference and the dark mode applies with no extra
utility classes.

## The React client

The React client loads the same token file and sets the same `<html>` attributes. Its PrimeReact components read the
tokens through the generated [AWE theme of PrimeReact](../design-system/tokens-reference.md#primereact-theme), so a
React page and an AngularJS page get the same values in both modes.

## Checking a change

`awe-client-angular/src/test/jest/styles/designTokens.test.js` compiles the whole LESS build and checks that the
`--awe-*` tokens are declared only by the shared token file (no bridge), that no `.theme-<name>` rule and no AWE 4 theme
variable is left, that the colour mode is set on `<html>`, that the LESS variables read the tokens and that a product
LESS keeps compiling. `sharedTokens.test.js` checks that the engine ships the token file whole and that it matches the
prototype. The browser test `ThemeAndModeIT` switches the theme and the dark mode in the application.
