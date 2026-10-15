---
id: design-tokens
title: Design tokens and dark mode
sidebar_label: Design tokens
---

The AngularJS client exposes its look as a small set of CSS custom properties, the **design tokens**, named `--awe-*`.
Components, your own stylesheets, Tailwind (`@theme`) and any new client read the tokens; the themes and the dark mode
only change the values behind them. The tokens are emitted from one LESS file,
`awe-client-angular/src/main/resources/less/awe/tokens.less`, on top of the current LESS build: LESS and Bootstrap 3 are
still there and nothing a product does today stops working.

## How a theme becomes tokens

```
theme variable  --primary-color            set by .theme-<name>, .theme-<name>.dark (LESS themes, database themes)
      │
      ▼
design token    --awe-color-primary        declared on the body (.base-theme) and on :root, never set by a theme
      │
      ▼
consumers       LESS (@primary-color), components, Tailwind @theme, the React client
```

- A **theme variable** is what a theme overrides. They are the variables AWE already had (`--primary-color`,
  `--panel-background-color`...) and the ones the *Themes* maintenance screen writes to the database, so every existing
  theme, database theme and product theme keeps working with no change.
- A **token** is the public name. It is declared on `body.base-theme` (and on `:root` for what is outside the body), so it
  is resolved with the theme variables of the active theme **and** mode. Switching theme or mode changes the theme
  variables and the tokens follow.
- Each token reads the LESS variable of the same meaning (`@primary-color` is `var(--primary-color, <fallback>)`), so a
  token always has a fallback, and a product that overrides `@primary-color` in its own LESS keeps working.

Read tokens, do not set them. To restyle, override the theme variable of the table below in your theme.

## Theme mode: light, dark and the operating system

The mode is a class of the body next to the theme (`<body class="base-theme theme-default dark">`):

| Body class | Meaning |
|------------|---------|
| `dark` | The user chose the dark mode (info button, saved in the user settings) |
| `light` | The user chose the light mode |
| none | The user did not choose: the operating system decides through `prefers-color-scheme` |

Every theme declares its dark variables once. They are used for `.theme-<name>.dark` and, for a user that did not choose,
inside `@media (prefers-color-scheme: dark)` for `.theme-<name>:not(.light):not(.dark)`. The explicit choice of the user
always wins over the operating system.

A theme file declares both through one mixin:

```less
.theme-sunset {
  --primary-color: #7a6f6f;
  // ...the light variables
}

// Dark mode: chosen by the user (.dark) or, when not chosen, by the OS (prefers-color-scheme)
@theme-sunset-dark: {
  --primary-color: #7a6f6f;
  // ...the dark variables
};
.awe-theme-dark(sunset; @theme-sunset-dark);
```

The themes stored in the database get the same pair of rules from `/css/themeVariables.css`.

### A product that must not follow the operating system

Set the LESS variable after importing AWE (the user can still choose the dark mode, the OS is only ignored):

```less
@awe-follow-os-color-scheme: false;
```

The themes stored in the database follow the same switch on the server: set `awe.application.theme-follow-os-color-scheme=false`
(default `true`) so `/css/themeVariables.css` does not emit the `prefers-color-scheme` rule. Set both to turn the operating system off.

## Token reference

The token names are a contract: they do not change between versions of the line. New tokens can be added.

### Color

| Token | Meaning | Theme variable to override | LESS variable |
|-------|---------|----------------------------|---------------|
| `--awe-color-theme` | Accent color of the theme | `--theme-color` | `@theme-color` |
| `--awe-color-text` | Default text | `--text-color` | `@text-color` |
| `--awe-color-primary` | Primary actions, selection, links of the theme | `--primary-color` | `@primary-color` |
| `--awe-color-on-primary` | Text over the primary color | `--primary-text-color` | `@primary-text-color` |
| `--awe-color-secondary` | Default buttons and neutral controls | `--secondary-color` | `@secondary-color` |
| `--awe-color-on-secondary` | Text over the secondary color | `--secondary-text-color` | `@secondary-text-color` |
| `--awe-color-background` | Page background | `--primary-background-color` | `@primary-background-color` |
| `--awe-color-surface` | Secondary surface (panel footers, inputs areas) | `--secondary-background-color` | `@secondary-background-color` |
| `--awe-color-border` | Default border | `--border-color` | `@border-color` |
| `--awe-color-shadow` | Color of the shadows | `--shadow-color` | |
| `--awe-color-menu` | Main menu background | `--primary-menu-color` | `@primary-menu-color` |
| `--awe-color-menu-accent` | Main menu hover and highlight | `--secondary-menu-color` | `@secondary-menu-color` |
| `--awe-color-menu-text` | Main menu text | `--menu-text-color` | `@menu-text-color` |
| `--awe-color-navbar` | Navbar background | `--primary-navbar-color` | `@primary-navbar-color` |
| `--awe-color-navbar-accent` | Navbar header | `--secondary-navbar-color` | `@secondary-navbar-color` |
| `--awe-color-navbar-text` | Navbar text | `--navbar-text-color` | `@navbar-text-color` |
| `--awe-color-navbar-dropdown` | Dropdown menus | `--navbar-dropdown-color` | `@navbar-dropdown-color` |
| `--awe-color-panel` | Panel and dialog background | `--panel-background-color` | `@panel-background-color` |
| `--awe-color-panel-header` | Panel header background | `--panel-header-color` | `@panel-header-color` |
| `--awe-color-panel-border` | Panel border | `--panel-border-color` | `@panel-border-color` |
| `--awe-color-panel-text` | Text over the panel header | `--panel-text-color` | `@panel-text-color` |
| `--awe-color-grid-header` | Grid header | `--grid-header-color` | `@grid-header-color` |
| `--awe-color-grid-border` | Grid borders | `--grid-border-color` | `@grid-border-color` |
| `--awe-color-skeleton` | Loading skeleton placeholders (derived from the grid border) | | |
| `--awe-color-skeleton-highlight` | Shimmer highlight of the loading skeletons | | |
| `--awe-color-disabled` | Disabled control background | `--disabled-color` | `@disabled-color` |
| `--awe-color-disabled-border` | Disabled control border | `--disabled-border-color` | `@disabled-border-color` |
| `--awe-color-disabled-text` | Disabled control text | `--disabled-text-color` | `@disabled-text-color` |
| `--awe-color-required` | Required field background | `--required-color` | `@required-color` |
| `--awe-color-loading-bar` | Loading bar | `--loading-bar-color` | `@loading-bar-color` |
| `--awe-color-danger` | Error | `--danger-color` | `@danger-color` |
| `--awe-color-warning` | Warning | `--warning-color` | `@warning-color` |
| `--awe-color-success` | Success | `--success-color` | `@success-color` |
| `--awe-color-info` | Information | `--info-color` | `@info-color` |

### Typography

| Token | Meaning | Theme variable to override | LESS variable |
|-------|---------|----------------------------|---------------|
| `--awe-font-family-base` | Font of the application | | `@font-family-base` |
| `--awe-font-family-monospace` | Code font | | `@font-family-monospace` |
| `--awe-font-size-base` | Default font size | `--primary-font-size` | `@primary-font-size` |
| `--awe-font-size-small` | Small text | `--secondary-font-size` | `@secondary-font-size` |
| `--awe-line-height-base` | Default line height | | `@line-height-base` |
| `--awe-font-weight-normal` | Regular weight | | |
| `--awe-font-weight-bold` | Bold weight | | |

### Spacing, radius, shadow and size

| Token | Value | Theme variable to override | LESS variable |
|-------|-------|----------------------------|---------------|
| `--awe-space-2xs` | `2px` | | |
| `--awe-space-xs` | `5px` | | |
| `--awe-space-sm` | `10px` (panel margin small) | | `@panel-margin-sm` |
| `--awe-space-md` | `15px` (panel margin medium) | | `@panel-margin-md` |
| `--awe-space-lg` | `20px` (panel margin large) | | `@panel-margin-lg` |
| `--awe-space-xl` | `30px` | | |
| `--awe-radius` | Default radius | `--border-radius` | `@border-radius` |
| `--awe-radius-sm` | Half of the default radius | | |
| `--awe-radius-lg` | Twice the default radius | | |
| `--awe-radius-full` | Pills and circles | | |
| `--awe-shadow-sm` | Small elevation | `--shadow-color` | |
| `--awe-shadow-md` | Medium elevation | `--shadow-color` | |
| `--awe-shadow-lg` | Large elevation | `--shadow-color` | |
| `--awe-size-header-height` | Navbar height | `--header-height` | `@main-navbar-height` |
| `--awe-size-menu-width` | Main menu width | | `@main-menu-width` |

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

They change with the theme and with the mode with nothing else to do. A product that has its own LESS variables keeps
them: they compile as before and the tokens that read them follow.

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

The React client does not use the LESS themes: it uses the PrimeReact themes. So its pages can read the same names, it
declares the tokens that have a direct PrimeReact equivalent in `awe-client-react/src/main.css`:

| Token | PrimeReact variable |
|-------|---------------------|
| `--awe-color-primary` | `--primary-color` |
| `--awe-color-on-primary` | `--primary-color-text` |
| `--awe-color-text` | `--text-color` |
| `--awe-color-background` | `--surface-ground` |
| `--awe-color-surface`, `--awe-color-panel` | `--surface-card` |
| `--awe-color-border` | `--surface-border` |
| `--awe-color-skeleton`, `--awe-color-skeleton-highlight` | `.p-skeleton` background and shimmer (themes sky and sunset) |
| `--awe-color-danger`, `--awe-color-warning`, `--awe-color-success`, `--awe-color-info` | `--danger-color`, `--warning-color`, `--success-color`, `--info-color` |
| `--awe-font-family-base` | `--font-family` |
| `--awe-radius` | `--border-radius` |

The React client has no dark mode yet: a React page reads the same names and gets the light values.

## Checking a change

`awe-client-angular/src/test/jest/styles/designTokens.test.js` compiles the whole LESS build and checks that every theme
keeps its variables (light and dark, against `themes.baseline.json`, taken before the tokens), that the dark rules of
the operating system match the explicit dark rules, that every `--awe-*` token is emitted and written in this guide, and
that a product LESS keeps compiling. The browser test `ThemeAndModeIT` switches the theme and the dark mode in the
application.
