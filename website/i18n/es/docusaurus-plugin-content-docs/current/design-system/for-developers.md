---
id: for-developers
title: Para desarrolladores
sidebar_label: Para desarrolladores
---

Cómo escribir CSS y componentes para AWE 5 en cualquiera de los dos motores, y cómo configurará un producto su tema.

## Reglas {#rules}

| Regla | En lugar de | Escriba |
|-------|-------------|---------|
| Solo tokens | `color: #334155` | `color: var(--awe-color-text)` |
| Sin colores literales fuera del fichero de tokens | `rgba(0, 0, 0, 0.1)` | `var(--awe-color-shadow)` o un token de sombra |
| Sin tamaños fuera de las escalas | `padding: 10px 15px` | `padding: var(--awe-space-2) var(--awe-space-3)` |
| Leer el nivel 2 o 3, nunca el 1 | `var(--awe-palette-emerald-600)` | `var(--awe-color-primary)` |
| Sin reglas por modo | `.dark .my-card { … }` | Un token con `light-dark()` |
| Sin reglas por densidad | `[data-awe-density] .my-field { … }` | Tokens de tamaño y relleno |
| Usar componentes, no marcado improvisado | Un desplegable hecho a mano | Los componentes de lista, diálogo y popover |
| Movimiento mediante tokens | `transition: 0.3s` | `var(--awe-motion-duration-base) var(--awe-motion-easing-standard)` |

Una tarjeta en el CSS de un producto:

```css
.shipment-card {
  padding: var(--awe-content-padding);
  color: var(--awe-color-text);
  background: var(--awe-color-surface);
  border: 1px solid var(--awe-color-border);
  border-radius: var(--awe-radius-card);
  box-shadow: var(--awe-shadow-card);
}
```

Sigue el tema, el modo y la densidad sin hacer nada más.

## Añadir un componente {#add-a-component}

1. Constrúyalo con tokens semánticos. Revise cada estado: hover, focus-visible, activo, deshabilitado, inválido,
   cargando y vacío.
2. Añada un token de componente (`--awe-<componente>-<propiedad>`) solo si el componente necesita un ajuste que no da
   un token semántico, por ejemplo un valor que cambia con la densidad. Defínalo en el fichero de tokens apuntando a un
   token semántico y dele un valor compacto si hace falta.
3. Añada valores claro y oscuro solo con `light-dark()` en el fichero de tokens, nunca en el componente.
4. Documente el componente en [Componentes](components.md) y sus tokens en la
   [referencia de tokens](tokens-reference.md).
5. Impleméntelo en **los dos** motores con los mismos tokens y los mismos estados.

## Configurar el tema de un producto {#configure-the-theme-of-a-product}

:::note En curso
El servidor resuelve el tema y los dos motores lo pintan como atributos de `<html>`. El motor React pinta sus
componentes de PrimeReact con el [tema AWE de PrimeReact](tokens-reference.md#primereact-theme), en claro y oscuro; el
resto de su aspecto y el motor AngularJS llegan más adelante. Ver la tabla de estado más abajo.
:::

Hay dos formas de dar a un producto su tema, y se pueden combinar:

| Forma | Cómo | Ejemplo |
|-------|------|---------|
| Nombre de tema (clásico) | Cada nombre de tema es un preset de primario, superficie y modo por defecto (ver [Presets de los temas clásicos](color-and-themes.md#presets-of-the-classic-themes)). Se elige como hoy: tema del usuario, luego el del perfil, luego `awe.application.theme`, y el selector de temas. | `awe.application.theme=sky` |
| Propiedades | Las propiedades de la aplicación fijan directamente los valores del tema, sin nombre. Sustituyen a los valores del nombre de tema. | `awe.application.theme-primary=indigo` |

El primario y la superficie admiten un **nombre de paleta o un hex**. Un hex genera la escala completa (ver
[Paletas generadas a partir de un hex](color-and-themes.md#palettes-generated-from-a-hex)). Un tema de base de datos
también puede llevar valores hex, de modo que el tema de un cliente no queda limitado a los 17 primarios.

Propiedades, junto a la existente `awe.application.theme` (ver la
[referencia de propiedades](../properties.md#awe.application.theme-primary)):

| Propiedad | Valores | Por defecto |
|-----------|---------|-------------|
| `awe.application.theme-primary` | Nombre de paleta (uno de los 17 primarios) o hex (`#RGB`, `#RRGGBB`) | el del nombre de tema; si no, `emerald` |
| `awe.application.theme-surface` | Nombre de paleta (`slate`, `gray`, `zinc`, `neutral`, `stone`), `auto` o hex | la del nombre de tema; si no, `auto` (slate en claro, zinc en oscuro) |
| `awe.application.theme-mode` | `light`, `dark`, `system` | el del nombre de tema; si no, `system` |
| `awe.application.density` | `comfortable`, `compact` | `comfortable` |

Cada valor sale de la primera fuente que tenga uno válido:

1. La elección del usuario: modo y densidad (ajustes del usuario).
2. Las propiedades de la aplicación anteriores.
3. El tema de base de datos (`AweThm`) del nombre de tema: columnas `Pri`, `Srf` y `Mde`.
4. El preset del nombre de tema clásico.
5. `emerald`, `auto`, `system` y `comfortable`.

Un valor no válido (una paleta desconocida, un hex mal formado) se descarta con un aviso en el log y se usa la fuente
siguiente.

Ejemplos:

```properties
# Una paleta con nombre
awe.application.theme-primary=indigo
# El color de marca del cliente: se generan la escala y el relleno
awe.application.theme-primary=#4F46E5
# Un fondo neutro cálido
awe.application.theme-surface=#F4F1EA
```

| Dónde | Qué | Estado |
|-------|-----|--------|
| Nombres de tema | Los 19 nombres de tema clásicos se corresponden con un preset de primario + superficie. | Implementado |
| Temas de base de datos | `AweThm` gana las columnas `Pri` (primario), `Srf` (superficie) y `Mde` (modo), cada primario y superficie como nombre de paleta o hex. La migración `AWE_V1.5.0` las rellena en las filas existentes (ver [Variables de tema y temas de base de datos](tokens-reference.md#mapping-themes)). `AweThmCol` se conserva, sin uso, y se elimina en una versión posterior. | Implementado |
| Propiedades | `awe.application.theme-primary`, `-surface`, `-mode` y `awe.application.density`, con el orden anterior. | Implementado |
| Pantalla de temas | Edita el primario, la superficie y el modo de un tema de base de datos: el primario y la superficie son una paleta de una lista o, con la opción personalizada, un hex elegido con un selector de color. La vista previa en vivo llega con el nuevo aspecto. | Campos implementados; vista previa pendiente |
| Ajustes de usuario | Modo (como hoy) y densidad por usuario (`AweUsrSet`). El servidor guarda y aplica la densidad; el selector de la interfaz llega con el nuevo shell. | Servidor implementado; selector pendiente |
| Atributos de `<html>` | Los dos motores pintan el tema resuelto en `<html>` y lo actualizan cuando cambian el tema o el modo. | Implementado |
| Nuevo aspecto | Las pantallas de cada motor leen los tokens de AWE 5 en lugar de los temas clásicos. React: los componentes de PrimeReact leen los tokens a través del [tema AWE de PrimeReact](tokens-reference.md#primereact-theme) generado (sin `saga-blue`, sin temas compilados y sin puente de nombres) y el marco de las pantallas de inicio (hueco de marca, menú lateral con su rastro activo, carril plegado y cajón, cabecera y miga de pan) lee los tokens del marco, y los criterios (etiqueta sobre el campo, altura de control de la densidad, mensaje de error con su icono, campos de solo lectura rellenos, botones radio y checkbox como control segmentado) leen los tokens de campo, y los grids (cabecera y columna ordenada, filas con el relleno de celda de la densidad, selección, cifras tabulares, fila en edición, detalle de fila, barra de herramientas, paginador, esqueleto, barra de recarga, estado vacío, diálogo de columnas y grid en árbol) leen los tokens del grid; los demás componentes de AWE vienen después. AngularJS: un único tema sobre los tokens de AWE 5 (sin temas LESS clásicos, sin `/css/themeVariables.css`, sin reglas `.theme-<nombre>` y sin puente de nombres); sus variables LESS leen los tokens, así que las pantallas de Bootstrap 3 y pixel-admin siguen a los atributos de `<html>`; la fuente, el marco y los componentes se reestilizan después. | React en curso; AngularJS en curso |

Los motores pintan la elección como atributos de `<html>`: `class="dark"` (o `light`), `data-awe-primary`,
`data-awe-surface` y `data-awe-density`. Una paleta generada a partir de un hex se pinta como las propiedades
personalizadas `--awe-primary-*` o `--awe-surface-*` de `<html>`; no cambia nada más. Un producto que necesite otro
color de marca indica su hex; no sobrescribe tokens semánticos localmente.

## Lista de comprobación de accesibilidad {#accessibility-checklist}

- [ ] El texto contra su fondo llega al menos a 4,5:1 en claro **y** en oscuro, con cualquier primario.
- [ ] Cada elemento interactivo muestra un anillo de foco visible con `:focus-visible`.
- [ ] Todo funciona con teclado: el orden de Tab sigue la disposición; Esc cierra las capas y devuelve el foco.
- [ ] Los botones solo con icono tienen `aria-label`; los iconos decorativos llevan `aria-hidden="true"`.
- [ ] Los errores están enlazados a su campo (`aria-invalid`, `aria-describedby`) y nunca se indican solo con color.
- [ ] Las capas usan los roles correctos (`combobox`/`listbox`, `menu`, `dialog`) y los estados (`aria-expanded`,
      `aria-selected`).
- [ ] Las zonas que cargan fijan `aria-busy`; los avisos de estado usan `role="status"` o `role="alert"`.
- [ ] Nada se mueve con `prefers-reduced-motion: reduce`.
- [ ] La disposición funciona con zoom al 200% y hasta el ancho de una tableta.

## Comprobar un cambio {#check-a-change}

| Comprobación | Cómo |
|--------------|------|
| Aspecto | Compárelo con el [prototipo](pathname:///design-system/prototype/) en claro y oscuro, cómoda y compacta, y con dos primarios (uno oscuro como `indigo` y uno claro como `amber`). |
| Literales | Busque en el CSS modificado `#`, `rgb(`, `hsl(` y valores `px` sueltos fuera del fichero de tokens. |
| Contraste | Mida las parejas de texto con un comprobador de contraste en los dos modos. |
| Teclado | Recorra el cambio solo con Tab, Enter, Espacio, flechas y Esc. |
| Pruebas | Pruebas Jest y de navegador del motor modificado, incluidas las pruebas de tokens. |

El [fichero de tokens de los motores](tokens-reference.md#token-file-of-the-engines) lo comprueban
`sharedTokens.test.js` (AngularJS) y `designTokensTest.js` (React), y el tema AWE de PrimeReact `primeReactThemeTest.js`
(React); los tokens actuales del motor AngularJS los sigue
comprobando `designTokens.test.js` contra la [guía actual](../guides/design-tokens-guide.md).
