---
id: design-tokens
title: Design tokens y modo oscuro
sidebar_label: Design tokens
---

:::info Dirección de AWE 5
Esta página documenta los nombres de los tokens que leen los componentes AngularJS. Sus valores vienen del fichero de
tokens de AWE 5; el sistema de diseño completo (todos los tokens, modelo claro/oscuro, temas como primario + superficie)
está en [Sistema de diseño](../design-system/overview.md) y en la [referencia de tokens](../design-system/tokens-reference.md).
:::

El cliente AngularJS expone su aspecto como propiedades personalizadas de CSS, los **design tokens**, con nombre
`--awe-*`. Los componentes, tus propias hojas de estilo, Tailwind (`@theme`) y cualquier cliente nuevo leen los tokens;
el tema y el modo oscuro solo cambian los valores que hay detrás. Los dos motores cargan el mismo fichero de tokens de
AWE 5, `awe-framework/awe-design-tokens/tokens.css`; el motor AngularJS lo incluye en su hoja de estilos
(`less/awe.less`). LESS y Bootstrap 3 siguen ahí: sus variables LESS leen los tokens.

## Cómo un tema se convierte en tokens {#how-a-theme-becomes-tokens}

```
nombre de tema o propiedades   sunset, un tema de base de datos, awe.application.theme-primary...
      │  el servidor lo resuelve en primario + superficie + modo + densidad
      ▼
atributos de <html>            class="dark" | "light", data-awe-primary, data-awe-surface, data-awe-density
      │
      ▼
design token                   --awe-color-primary        solo se declara en tokens.css y sigue a los atributos
      │
      ▼
consumidores                   LESS (@primary-color), componentes, Tailwind @theme, el cliente React
```

- Ya no hay temas clásicos (AWE 5, issue #885). Un nombre de tema (`default`, `sunset`...) y un tema de la base de datos
  son **presets**: el servidor los resuelve, con las propiedades de la aplicación y las elecciones del usuario, en un
  primario, una superficie, un modo y una densidad (ver la [configuración del tema](../design-system/for-developers.md)).
- El motor pinta esa elección como atributos de `<html>`, y el fichero de tokens da a cada token su valor para ellos. Un
  primario o una superficie dados como hex llegan con su paleta generada como propiedades `--awe-primary-*` o
  `--awe-surface-*` de `<html>`.
- Las variables LESS del motor leen los tokens (`@primary-color` es `var(--awe-color-primary)`), así que las reglas de
  Bootstrap 3 y pixel-admin siguen al tema, al modo y a la densidad. Un producto que sobrescribe una variable LESS en su
  propio LESS conserva ese valor.
- Las variables de tema de AWE 4 (`--primary-color`, `--panel-background-color`...), las reglas `.theme-<nombre>` y
  `/css/themeVariables.css` ya no se usan: definirlas no cambia nada. La clase `theme-<nombre>` sigue en el body solo
  para los tests de navegador.

Lee los tokens, no los definas. Para cambiar los colores, elige un primario y una superficie (un nombre de paleta o un
hex).

## Modo del tema: claro, oscuro y el sistema operativo {#theme-mode-light-dark-and-the-operating-system}

El modo es una clase de `<html>`, junto a los demás atributos del tema (`<html class="dark" data-awe-primary="indigo">`):

| Clase de `<html>` | Significado |
|-------------------|-------------|
| `dark` | El modo oscuro: lo eligió el usuario (menú de usuario, guardado en sus preferencias) o lo fija el tema o `awe.application.theme-mode` |
| `light` | El modo claro, elegido de la misma forma |
| ninguna | El modo del sistema: decide el sistema operativo mediante `prefers-color-scheme` |

Los tokens tienen los dos valores con `light-dark()` y el motor fija `color-scheme` en `<html>` (`light dark`, o `light`
/ `dark` con la clase), así que la elección explícita siempre gana al sistema operativo. El menú de usuario del motor
AngularJS sigue añadiendo `dark` o `light` al body; el motor vuelve a leer entonces la apariencia del servidor y
actualiza `<html>`.

### Un producto que no debe seguir al sistema operativo {#a-product-that-must-not-follow-the-operating-system}

Fija el modo de la aplicación a `light` (o `dark`): `awe.application.theme-mode=light`. El usuario puede seguir eligiendo
el otro modo en el menú de usuario. La variable LESS `@awe-follow-os-color-scheme` ya no cambia el motor AngularJS, y
la propiedad `awe.application.theme-follow-os-color-scheme` se eliminó junto con `/css/themeVariables.css`.

## Referencia de tokens {#token-reference}

Los nombres de los tokens son un contrato: no cambian entre versiones de la línea. Se pueden añadir tokens nuevos. Los
valores son los del fichero de tokens de AWE 5, recogidos en la [referencia de tokens](../design-system/tokens-reference.md).

### Color {#color}

| Token | Significado | Variable LESS |
|-------|-------------|---------------|
| `--awe-color-theme` | Color de acento del tema | `@theme-color` |
| `--awe-color-text` | Texto por defecto | `@text-color` |
| `--awe-color-primary` | Acciones principales, selección, enlaces del tema | `@primary-color` |
| `--awe-color-on-primary` | Texto sobre el color primario | `@primary-text-color` |
| `--awe-color-secondary` | Botones por defecto y controles neutros | `@secondary-color` |
| `--awe-color-on-secondary` | Texto sobre el color secundario | `@secondary-text-color` |
| `--awe-color-background` | Fondo de la página | `@primary-background-color` |
| `--awe-color-surface` | Superficie secundaria (pies de panel, zonas de campos) | `@secondary-background-color` |
| `--awe-color-border` | Borde por defecto | `@border-color` |
| `--awe-color-shadow` | Color de las sombras | |
| `--awe-color-menu` | Fondo del menú principal | `@primary-menu-color` |
| `--awe-color-menu-accent` | Hover y resaltado del menú principal | `@secondary-menu-color` |
| `--awe-color-menu-text` | Texto del menú principal | `@menu-text-color` |
| `--awe-color-navbar` | Fondo de la barra de navegación | `@primary-navbar-color` |
| `--awe-color-navbar-accent` | Cabecera de la barra de navegación | `@secondary-navbar-color` |
| `--awe-color-navbar-text` | Texto de la barra de navegación | `@navbar-text-color` |
| `--awe-color-navbar-dropdown` | Menús desplegables | `@navbar-dropdown-color` |
| `--awe-color-panel` | Fondo de paneles y diálogos | `@panel-background-color` |
| `--awe-color-panel-header` | Fondo de la cabecera del panel | `@panel-header-color` |
| `--awe-color-panel-border` | Borde del panel | `@panel-border-color` |
| `--awe-color-panel-text` | Texto sobre la cabecera del panel | `@panel-text-color` |
| `--awe-color-grid-header` | Cabecera de la grid | `@grid-header-color` |
| `--awe-color-grid-border` | Bordes de la grid | `@grid-border-color` |
| `--awe-color-skeleton` | Relleno de los esqueletos de carga de grids, criterios, botones y gráficos | |
| `--awe-color-skeleton-highlight` | Brillo animado de los esqueletos de carga | |
| `--awe-color-disabled` | Fondo de un control deshabilitado | `@disabled-color` |
| `--awe-color-disabled-border` | Borde de un control deshabilitado | `@disabled-border-color` |
| `--awe-color-disabled-text` | Texto de un control deshabilitado | `@disabled-text-color` |
| `--awe-color-required` | Marca de obligatorio (`@required-color`, el tinte de los campos obligatorios, lee `--awe-color-highlight`) | `@required-color` |
| `--awe-color-loading-bar` | Barra de carga | `@loading-bar-color` |
| `--awe-color-danger` | Error | `@danger-color` |
| `--awe-color-warning` | Aviso | `@warning-color` |
| `--awe-color-success` | Éxito | `@success-color` |
| `--awe-color-info` | Información | `@info-color` |

### Tipografía {#typography}

| Token | Significado | Variable LESS |
|-------|-------------|---------------|
| `--awe-font-family-base` | Fuente de la aplicación | `@font-family-base` |
| `--awe-font-family-monospace` | Fuente de código | `@font-family-monospace` |
| `--awe-font-size-base` | Tamaño de fuente por defecto | `@primary-font-size` |
| `--awe-font-size-small` | Texto pequeño | `@secondary-font-size` |
| `--awe-line-height-base` | Altura de línea por defecto | `@line-height-base` |
| `--awe-font-weight-normal` | Peso normal | |
| `--awe-font-weight-bold` | Peso negrita | |

### Espaciado, radio, sombra y tamaño {#spacing-radius-shadow-and-size}

| Token | Valor | Variable LESS |
|-------|-------|---------------|
| `--awe-space-2xs` | `2px` | |
| `--awe-space-xs` | `4px` | |
| `--awe-space-sm` | `8px` | |
| `--awe-space-md` | `12px` | |
| `--awe-space-lg` | `16px` | |
| `--awe-space-xl` | `24px` | |
| `--awe-radius` | Radio por defecto (`6px`) | `@border-radius` |
| `--awe-radius-sm` | Radio pequeño (`4px`) | |
| `--awe-radius-lg` | Radio grande (`8px`) | |
| `--awe-radius-full` | Píldoras y círculos | |
| `--awe-shadow-sm` | Elevación pequeña | |
| `--awe-shadow-md` | Elevación media | |
| `--awe-shadow-lg` | Elevación grande | |
| `--awe-size-header-height` | Altura de la barra de navegación | `@main-navbar-height` |
| `--awe-size-menu-width` | Ancho del menú principal | `@main-menu-width` |

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

Cambian con el tema, el modo y la densidad sin hacer nada más. Un producto que tiene sus propias variables LESS las
conserva: compilan como antes.

## Correspondencia con Tailwind {#mapping-into-tailwind}

Los tokens son lo que lee Tailwind v4 (`@theme`, issue #755). El cliente AngularJS compila Tailwind junto a la compilación
LESS y mapea **todos** los tokens uno a uno en `awe-client-angular/src/main/resources/tailwind/theme.css`, con
`@theme inline` y sin el tema por defecto de Tailwind, de modo que los únicos colores, tamaños, radios y sombras son los de AWE:

```css
@theme inline {
  --color-primary: var(--awe-color-primary);
  --color-panel: var(--awe-color-panel);
  --radius-base: var(--awe-radius);
  --shadow-md: var(--awe-shadow-md);
  --spacing-md: var(--awe-space-md);
}
```

`--awe-color-X` es `--color-X`, `--awe-space-X` es `--spacing-X`, `--awe-radius[-X]` es `--radius-base|X`, `--awe-shadow-X`
es `--shadow-X`, `--awe-font-family-X` es `--font-X`, `--awe-font-size-X` es `--text-X`, `--awe-line-height-X` es
`--leading-X`, `--awe-font-weight-X` es `--font-weight-X` y `--awe-size-header-height` / `--awe-size-menu-width` son
`--spacing-header` / `--spacing-menu`. Una utilidad lee entonces el token, por ejemplo `tw:bg-primary`, `tw:p-md`,
`tw:rounded-base`. `@theme inline` mantiene la referencia `var(--awe-...)`, así que las utilidades siguen el tema y el modo
oscuro sin clases adicionales. Consulta [Tailwind y la capa de compatibilidad con Bootstrap 3](tailwind-compat-guide.md).

## El cliente React {#the-react-client}

El cliente React carga el mismo fichero de tokens y pone los mismos atributos en `<html>`. Sus componentes PrimeReact
leen los tokens a través del [tema AWE de PrimeReact](../design-system/tokens-reference.md#primereact-theme) generado,
así que una página React y una AngularJS obtienen los mismos valores en los dos modos.

## Comprobar un cambio {#checking-a-change}

`awe-client-angular/src/test/jest/styles/designTokens.test.js` compila toda la build LESS y comprueba que los tokens
`--awe-*` solo los declara el fichero de tokens compartido (sin puente), que no queda ninguna regla `.theme-<nombre>` ni
ninguna variable de tema de AWE 4, que el modo de color se fija en `<html>`, que las variables LESS leen los tokens y que
el LESS de un producto sigue compilando. `sharedTokens.test.js` comprueba que el motor incluye el fichero de tokens
completo y que coincide con el prototipo. El test de navegador `ThemeAndModeIT` cambia el tema y el modo oscuro en la
aplicación.
