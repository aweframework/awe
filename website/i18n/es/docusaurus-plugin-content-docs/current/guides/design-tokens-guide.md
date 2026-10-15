---
id: design-tokens
title: Design tokens y modo oscuro
sidebar_label: Design tokens
---

El cliente AngularJS expone su aspecto como un conjunto pequeño de propiedades personalizadas de CSS, los **design
tokens**, con nombre `--awe-*`. Los componentes, tus propias hojas de estilo, Tailwind (`@theme`) y cualquier cliente
nuevo leen los tokens; los temas y el modo oscuro solo cambian los valores que hay detrás. Los tokens se emiten desde un
único fichero LESS, `awe-client-angular/src/main/resources/less/awe/tokens.less`, sobre la compilación LESS actual: LESS
y Bootstrap 3 siguen ahí y nada de lo que hace hoy un producto deja de funcionar.

## Cómo un tema se convierte en tokens {#how-a-theme-becomes-tokens}

```
variable de tema  --primary-color          la define .theme-<nombre>, .theme-<nombre>.dark (temas LESS, temas de base de datos)
      │
      ▼
design token      --awe-color-primary      se declara en el body (.base-theme) y en :root, nunca la define un tema
      │
      ▼
consumidores      LESS (@primary-color), componentes, Tailwind @theme, el cliente React
```

- Una **variable de tema** es lo que sobrescribe un tema. Son las variables que AWE ya tenía (`--primary-color`,
  `--panel-background-color`...) y las que la pantalla de mantenimiento de *Temas* escribe en la base de datos, de modo
  que todo tema existente, tema de base de datos y tema de producto sigue funcionando sin cambios.
- Un **token** es el nombre público. Se declara en `body.base-theme` (y en `:root` para lo que queda fuera del body), así
  que se resuelve con las variables de tema del tema **y** del modo activos. Cambiar de tema o de modo cambia las
  variables de tema y los tokens las siguen.
- Cada token lee la variable LESS del mismo significado (`@primary-color` es `var(--primary-color, <valor por
  defecto>)`), por lo que un token siempre tiene valor por defecto, y un producto que sobrescribe `@primary-color` en su
  LESS sigue funcionando.

Lee los tokens, no los definas. Para cambiar el aspecto, sobrescribe en tu tema la variable de tema de las tablas
siguientes.

## Modo del tema: claro, oscuro y el sistema operativo {#theme-mode-light-dark-and-the-operating-system}

El modo es una clase del body junto al tema (`<body class="base-theme theme-default dark">`):

| Clase del body | Significado |
|----------------|-------------|
| `dark` | El usuario eligió el modo oscuro (botón de información, guardado en las preferencias del usuario) |
| `light` | El usuario eligió el modo claro |
| ninguna | El usuario no eligió: decide el sistema operativo mediante `prefers-color-scheme` |

Cada tema declara sus variables oscuras una sola vez. Se usan para `.theme-<nombre>.dark` y, para un usuario que no
eligió, dentro de `@media (prefers-color-scheme: dark)` para `.theme-<nombre>:not(.light):not(.dark)`. La elección
explícita del usuario siempre gana al sistema operativo.

Un fichero de tema declara ambos con un mixin:

```less
.theme-sunset {
  --primary-color: #7a6f6f;
  // ...las variables claras
}

// Dark mode: chosen by the user (.dark) or, when not chosen, by the OS (prefers-color-scheme)
@theme-sunset-dark: {
  --primary-color: #7a6f6f;
  // ...las variables oscuras
};
.awe-theme-dark(sunset; @theme-sunset-dark);
```

Los temas guardados en la base de datos obtienen el mismo par de reglas desde `/css/themeVariables.css`.

### Un producto que no debe seguir al sistema operativo {#a-product-that-must-not-follow-the-operating-system}

Define la variable LESS después de importar AWE (el usuario puede seguir eligiendo el modo oscuro, solo se ignora el
sistema operativo):

```less
@awe-follow-os-color-scheme: false;
```

Los temas guardados en la base de datos usan el mismo interruptor en el servidor: defina
`awe.application.theme-follow-os-color-scheme=false` (por defecto `true`) para que `/css/themeVariables.css` no emita la regla
`prefers-color-scheme`. Defina ambos para desactivar el sistema operativo.

## Referencia de tokens {#token-reference}

Los nombres de los tokens son un contrato: no cambian entre versiones de la línea. Se pueden añadir tokens nuevos.

### Color {#color}

| Token | Significado | Variable de tema a sobrescribir | Variable LESS |
|-------|-------------|---------------------------------|---------------|
| `--awe-color-theme` | Color de acento del tema | `--theme-color` | `@theme-color` |
| `--awe-color-text` | Texto por defecto | `--text-color` | `@text-color` |
| `--awe-color-primary` | Acciones principales, selección, enlaces del tema | `--primary-color` | `@primary-color` |
| `--awe-color-on-primary` | Texto sobre el color primario | `--primary-text-color` | `@primary-text-color` |
| `--awe-color-secondary` | Botones por defecto y controles neutros | `--secondary-color` | `@secondary-color` |
| `--awe-color-on-secondary` | Texto sobre el color secundario | `--secondary-text-color` | `@secondary-text-color` |
| `--awe-color-background` | Fondo de la página | `--primary-background-color` | `@primary-background-color` |
| `--awe-color-surface` | Superficie secundaria (pies de panel, zonas de campos) | `--secondary-background-color` | `@secondary-background-color` |
| `--awe-color-border` | Borde por defecto | `--border-color` | `@border-color` |
| `--awe-color-shadow` | Color de las sombras | `--shadow-color` | |
| `--awe-color-menu` | Fondo del menú principal | `--primary-menu-color` | `@primary-menu-color` |
| `--awe-color-menu-accent` | Hover y resaltado del menú principal | `--secondary-menu-color` | `@secondary-menu-color` |
| `--awe-color-menu-text` | Texto del menú principal | `--menu-text-color` | `@menu-text-color` |
| `--awe-color-navbar` | Fondo de la barra de navegación | `--primary-navbar-color` | `@primary-navbar-color` |
| `--awe-color-navbar-accent` | Cabecera de la barra de navegación | `--secondary-navbar-color` | `@secondary-navbar-color` |
| `--awe-color-navbar-text` | Texto de la barra de navegación | `--navbar-text-color` | `@navbar-text-color` |
| `--awe-color-navbar-dropdown` | Menús desplegables | `--navbar-dropdown-color` | `@navbar-dropdown-color` |
| `--awe-color-panel` | Fondo de paneles y diálogos | `--panel-background-color` | `@panel-background-color` |
| `--awe-color-panel-header` | Fondo de la cabecera del panel | `--panel-header-color` | `@panel-header-color` |
| `--awe-color-panel-border` | Borde del panel | `--panel-border-color` | `@panel-border-color` |
| `--awe-color-panel-text` | Texto sobre la cabecera del panel | `--panel-text-color` | `@panel-text-color` |
| `--awe-color-grid-header` | Cabecera de la grid | `--grid-header-color` | `@grid-header-color` |
| `--awe-color-grid-border` | Bordes de la grid | `--grid-border-color` | `@grid-border-color` |
| `--awe-color-skeleton` | Relleno de los esqueletos de carga (derivado del borde de la grid) | | |
| `--awe-color-skeleton-highlight` | Brillo animado de los esqueletos de carga | | |
| `--awe-color-disabled` | Fondo de un control deshabilitado | `--disabled-color` | `@disabled-color` |
| `--awe-color-disabled-border` | Borde de un control deshabilitado | `--disabled-border-color` | `@disabled-border-color` |
| `--awe-color-disabled-text` | Texto de un control deshabilitado | `--disabled-text-color` | `@disabled-text-color` |
| `--awe-color-required` | Fondo de un campo obligatorio | `--required-color` | `@required-color` |
| `--awe-color-loading-bar` | Barra de carga | `--loading-bar-color` | `@loading-bar-color` |
| `--awe-color-danger` | Error | `--danger-color` | `@danger-color` |
| `--awe-color-warning` | Aviso | `--warning-color` | `@warning-color` |
| `--awe-color-success` | Éxito | `--success-color` | `@success-color` |
| `--awe-color-info` | Información | `--info-color` | `@info-color` |

### Tipografía {#typography}

| Token | Significado | Variable de tema a sobrescribir | Variable LESS |
|-------|-------------|---------------------------------|---------------|
| `--awe-font-family-base` | Fuente de la aplicación | | `@font-family-base` |
| `--awe-font-family-monospace` | Fuente de código | | `@font-family-monospace` |
| `--awe-font-size-base` | Tamaño de fuente por defecto | `--primary-font-size` | `@primary-font-size` |
| `--awe-font-size-small` | Texto pequeño | `--secondary-font-size` | `@secondary-font-size` |
| `--awe-line-height-base` | Altura de línea por defecto | | `@line-height-base` |
| `--awe-font-weight-normal` | Peso normal | | |
| `--awe-font-weight-bold` | Peso negrita | | |

### Espaciado, radio, sombra y tamaño {#spacing-radius-shadow-and-size}

| Token | Valor | Variable de tema a sobrescribir | Variable LESS |
|-------|-------|---------------------------------|---------------|
| `--awe-space-2xs` | `2px` | | |
| `--awe-space-xs` | `5px` | | |
| `--awe-space-sm` | `10px` (margen de panel pequeño) | | `@panel-margin-sm` |
| `--awe-space-md` | `15px` (margen de panel mediano) | | `@panel-margin-md` |
| `--awe-space-lg` | `20px` (margen de panel grande) | | `@panel-margin-lg` |
| `--awe-space-xl` | `30px` | | |
| `--awe-radius` | Radio por defecto | `--border-radius` | `@border-radius` |
| `--awe-radius-sm` | La mitad del radio por defecto | | |
| `--awe-radius-lg` | El doble del radio por defecto | | |
| `--awe-radius-full` | Píldoras y círculos | | |
| `--awe-shadow-sm` | Elevación pequeña | `--shadow-color` | |
| `--awe-shadow-md` | Elevación media | `--shadow-color` | |
| `--awe-shadow-lg` | Elevación grande | `--shadow-color` | |
| `--awe-size-header-height` | Altura de la barra de navegación | `--header-height` | `@main-navbar-height` |
| `--awe-size-menu-width` | Ancho del menú principal | | `@main-menu-width` |

## Usar los tokens {#using-the-tokens}

En tu CSS o LESS:

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

Cambian con el tema y con el modo sin hacer nada más. Un producto que tiene sus propias variables LESS las conserva:
compilan como antes y los tokens que las leen las siguen.

## Correspondencia con Tailwind {#mapping-into-tailwind}

Los tokens son lo que adopta Tailwind v4 (`@theme`, issue #755). Cada token se corresponde uno a uno, sin renombrarlo:

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

Usa `@theme inline` para que las utilidades mantengan la referencia `var(--awe-...)` y el modo oscuro se aplique sin
clases de utilidad adicionales.

## El cliente React {#the-react-client}

El cliente React no usa los temas LESS: usa los temas de PrimeReact. Para que sus páginas puedan leer los mismos
nombres, declara en `awe-client-react/src/main.css` los tokens que tienen un equivalente directo en PrimeReact:

| Token | Variable de PrimeReact |
|-------|------------------------|
| `--awe-color-primary` | `--primary-color` |
| `--awe-color-on-primary` | `--primary-color-text` |
| `--awe-color-text` | `--text-color` |
| `--awe-color-background` | `--surface-ground` |
| `--awe-color-surface`, `--awe-color-panel` | `--surface-card` |
| `--awe-color-border` | `--surface-border` |
| `--awe-color-skeleton`, `--awe-color-skeleton-highlight` | Fondo y brillo de `.p-skeleton` (temas sky y sunset) |
| `--awe-color-danger`, `--awe-color-warning`, `--awe-color-success`, `--awe-color-info` | `--danger-color`, `--warning-color`, `--success-color`, `--info-color` |
| `--awe-font-family-base` | `--font-family` |
| `--awe-radius` | `--border-radius` |

El cliente React todavía no tiene modo oscuro: una página React lee los mismos nombres y obtiene los valores claros.

## Comprobar un cambio {#checking-a-change}

`awe-client-angular/src/test/jest/styles/designTokens.test.js` compila toda la build LESS y comprueba que cada tema
conserva sus variables (claro y oscuro, contra `themes.baseline.json`, tomado antes de los tokens), que las reglas
oscuras del sistema operativo coinciden con las reglas oscuras explícitas, que cada token `--awe-*` se emite y está
escrito en esta guía, y que el LESS de un producto sigue compilando. El test de navegador `ThemeAndModeIT` cambia el
tema y el modo oscuro en la aplicación.
