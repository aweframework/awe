---
id: color-and-themes
title: Color y temas
sidebar_label: Color y temas
---

Un componente lee un token **semántico** (`--awe-color-text`). El token semántico apunta a una paleta **configurable**
(`--awe-surface-700`), que apunta a una paleta **primitiva** (`--awe-palette-slate-700`). Un tema solo elige las
paletas configurables; el modo elige el valor claro o el oscuro.

```text
componente     color: var(--awe-color-text)
     │
semántico      --awe-color-text: light-dark(var(--awe-surface-700), var(--awe-surface-0))
     │
configurable   --awe-surface-700: var(--awe-palette-slate-700)      ← data-awe-surface="slate"
     │
primitivo      --awe-palette-slate-700: #334155                     ← valor fijo, no cambia nunca
```

## Los tres niveles {#the-three-tiers}

| Nivel | Patrón de nombre | Quién lo fija | Quién lo lee |
|-------|------------------|---------------|--------------|
| 1 · Primitivo | `--awe-palette-<color>-<50..950>` | AWE (valores fijos) | Solo los tokens configurables del nivel 1 |
| 1 · Configurable | `--awe-primary-<50..950>`, `--awe-primary-fill*`, `--awe-surface-<0..950>` | El tema (`data-awe-primary`, `data-awe-surface`) | Los tokens semánticos |
| 2 · Semántico | `--awe-color-*`, `--awe-font-*`, `--awe-space-*`, `--awe-radius-*`, `--awe-shadow-*`, `--awe-motion-*`, `--awe-size-*` | AWE | Componentes y CSS de producto |
| 3 · Componente | `--awe-<componente>-<propiedad>` (`--awe-grid-*`, `--awe-menu-*`, `--awe-header-*`, `--awe-segmented-*`, `--awe-login-*`) | AWE | Solo ese componente |

Reglas:

- Los componentes y el CSS de producto leen el nivel 2 o el 3. **Nunca** leen el nivel 1.
- Un token de componente solo existe cuando el componente necesita su propio ajuste. Por defecto, use un token
  semántico.
- Los nombres `--awe-color-*` actuales se mantienen (ver la
  [correspondencia](tokens-reference.md#mapping-from-the-current-tokens)).

## Paletas {#palettes}

Veintidós paletas primitivas, cada una con 11 tonos (de 50 a 950):

| Uso | Paletas |
|-----|---------|
| Primario (17) | `emerald` (por defecto), `green`, `lime`, `teal`, `cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`, `red`, `orange`, `amber`, `yellow` |
| Superficie (5) | `slate` (por defecto en claro), `gray`, `zinc` (por defecto en oscuro), `neutral`, `stone` |

`--awe-surface-0` siempre es blanco. Los valores están en la
[referencia de tokens](tokens-reference.md#tier-1-primitive).

### Por qué el primario tiene tokens de relleno {#why-the-primary-has-fill-tokens}

Rellenar los botones primarios con el tono 500 y texto blanco queda por debajo de 4,5:1 en la mayoría de paletas
(emerald 500 con blanco da 2,54:1). AWE añade cuatro tokens configurables elegidos **por paleta** para que el
texto sobre una superficie rellena alcance siempre AA en modo claro:

| Paletas | `--awe-primary-fill` | Texto sobre el relleno |
|---------|----------------------|------------------------|
| `red`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose` | 600 | blanco |
| `emerald`, `green`, `teal`, `cyan`, `sky`, `orange` | 700 | blanco |
| `lime`, `amber`, `yellow` | 500 | tono 950 |

`--awe-primary-fill-hover`, `--awe-primary-fill-active` y `--awe-primary-on-fill` siguen la misma tabla. En modo oscuro
el primario es el tono 400 con texto oscuro. Consecuencia: en modo claro el emerald por defecto es un tono más oscuro
(700).

## Claro y oscuro {#light-and-dark}

El modo es una clase de `<html>`, como hace hoy el motor AngularJS en el body:

| Clase de `<html>` | Modo |
|-------------------|------|
| `light` | Claro, elegido por el usuario |
| `dark` | Oscuro, elegido por el usuario |
| ninguna | Decide el sistema operativo (`prefers-color-scheme`) |

Cada token que depende del modo se escribe **una sola vez** con `light-dark()`:

```css
:root {
  color-scheme: light dark;
  --awe-color-surface: light-dark(var(--awe-surface-0), var(--awe-surface-900));
}
.light { color-scheme: light; }
.dark { color-scheme: dark; }
```

- `light-dark()` se resuelve con el `color-scheme` del elemento, así que las clases funcionan también en cualquier
  subárbol (una vista previa del otro modo dentro de una página).
- Requisito: navegadores con soporte de `light-dark()` (Chrome y Edge 123, Firefox 120, Safari 17.5). AWE 5 se dirige
  a las versiones actuales de estos navegadores.
- La elección explícita del usuario siempre prevalece sobre el sistema operativo.

## Un tema es primario + superficie {#a-theme-is-primary--surface}

```html
<html class="dark" data-awe-primary="indigo" data-awe-surface="zinc" data-awe-density="compact">
```

| Atributo | Valores | Por defecto |
|----------|---------|-------------|
| `data-awe-primary` | Una de las 17 paletas primarias o una paleta generada a partir de un hex | `emerald` |
| `data-awe-surface` | `slate`, `gray`, `zinc`, `neutral`, `stone` o una paleta generada a partir de un hex | `slate` en claro, `zinc` en oscuro |
| `data-awe-density` | `comfortable`, `compact` | `comfortable` |

Un producto ya no fija los colores uno a uno. Los 35 campos de color, tamaño y radio de los temas actuales se derivan
de estas dos paletas. Cada una se indica con un **nombre de paleta** o con un **color hexadecimal** (`#4F46E5`,
`#F4F1EA`).

### Paletas generadas a partir de un hex {#palettes-generated-from-a-hex}

Un hex no se usa como un único color: genera la escala completa, de modo que todos los tokens semánticos siguen
funcionando.

1. El hex (`#RGB` o `#RRGGBB`) se convierte a OKLCH. Se conservan su **tono** y su **croma**.
2. Cada tono de la escala recibe una **luminosidad fija**, la misma para cualquier hex. La luminosidad es lo que decide
   el contraste, así que las parejas de los tokens semánticos se cumplen con cualquier tono.
3. El croma se ajusta a la gama sRGB en cada tono (la luminosidad no se mueve por ello).
4. Algunos tonos se corrigen según el contraste medido, nunca más de lo necesario (ver la tabla siguiente).
5. El relleno del primario (`--awe-primary-fill`, `-hover`, `-active`, `-on-fill`) es el tono más cercano a la
   luminosidad del hex cuyo texto, blanco o el tono 950, alcanza 4,5:1.

| | Primario (50 → 950) | Superficie (0 → 950) |
|---|---|---|
| Luminosidad (L de OKLCH) | 0,97; 0,935; 0,885; 0,81; 0,72; 0,64; 0,56; 0,49; 0,43; 0,38; 0,28 | 0 es blanco; después 0,985; 0,968; 0,929; 0,869; 0,711; 0,554; 0,446; 0,372; 0,279; 0,208; 0,129 |
| Croma | El croma del hex, limitado a 0,035 (50) y 0,07 (100) y ajustado a la gama | **Limitado a 0,03**, menor en los extremos claro y oscuro |
| Corregido por contraste | 700 y 800 sobre 50, 100 y blanco; 400 y 300 sobre las superficies oscuras | 500 sobre blanco, 50 y 100; 400 sobre 800 y 900 |

Qué garantiza:

- Todas las parejas de texto de los tokens semánticos llegan al menos a 4,5:1 en claro y en oscuro, igual que con las
  paletas con nombre.
- Un hex de superficie saturado (`#FF0000`) solo **tiñe** los neutros: los fondos siguen siendo tranquilos y legibles.
  La superficie genera una única escala para los dos modos; el modo oscuro usa sus tonos oscuros.
- El relleno sigue a la marca: una marca oscura (`#575C9C`) obtiene un relleno 700 con texto blanco y una clara
  (`#F59E0B`), un relleno de tono claro con texto oscuro.

Hex probados, en claro y en oscuro (2.736 parejas): primarios `#575C9C`, `#E11D48`, `#F59E0B`, `#84CC16`, `#0EA5E9`,
`#111827`, `#FFFFFF`, `#7C3AED`; superficies `#F4F1EA`, `#0F172A`, `#FF0000`. Todas las parejas de texto pasan; los
únicos fallos son las excepciones aceptadas de [Reglas de contraste](#contrast-rules). Un hex casi blanco o casi gris
da un primario gris: es válido, pero los controles rellenos destacan menos sobre la página. El panel **Theme** del
prototipo tiene una opción *Custom* para probar cualquier hex.

Un tema llega a la página de una de estas dos formas (implementado en el servidor y pintado en `<html>`; las pantallas
mantienen el aspecto clásico hasta el nuevo aspecto de los motores, ver [Para desarrolladores](for-developers.md#configure-the-theme-of-a-product)):

- **Por nombre**, como hoy: un nombre de tema (`sky`, `sunset`…) es un preset de primario, superficie y modo por
  defecto, y se elige con el tema del usuario, el del perfil, `awe.application.theme` y el selector de temas.
- **Por propiedades**: las propiedades de la aplicación fijan directamente el primario, la superficie, el modo y la
  densidad.

### Presets de los temas clásicos {#presets-of-the-classic-themes}

Cada nombre de tema clásico se corresponde con la paleta de AWE 5 más cercana a su color de marca actual:

1. El color de marca es el más cromático de `--theme-color` y `--primary-color` del tema.
2. Se toman las 2 paletas más cercanas por tono OKLCH y, de ellas, gana la más cercana en OKLab.
3. Una marca gris (croma OKLCH menor que 0,05) no tiene tono más cercano: conserva su hex, que genera su paleta.
4. La superficie es `auto` salvo que el fondo clásico esté teñido: cálido da `stone` y azul da `slate`.
5. El modo es `system`.

| Tema | Primario | Superficie | Modo |
|------|----------|------------|------|
| `default` | `sky` | `auto` | `system` |
| `asphalt` | `teal` | `auto` | `system` |
| `purple-hills` | `violet` | `auto` | `system` |
| `dust` | `#948787` | `stone` | `system` |
| `frost` | `cyan` | `slate` | `system` |
| `fresh` | `lime` | `auto` | `system` |
| `silver` | `cyan` | `auto` | `system` |
| `clean` | `emerald` | `auto` | `system` |
| `white` | `cyan` | `auto` | `system` |
| `adminflare` | `blue` | `auto` | `system` |
| `orange` | `red` | `stone` | `system` |
| `sunset` | `red` | `auto` | `system` |
| `grass` | `cyan` | `auto` | `system` |
| `sky` | `blue` | `auto` | `system` |
| `eclipse` | `sky` | `auto` | `system` |
| `sunny` | `yellow` | `auto` | `system` |
| `sea` | `blue` | `slate` | `system` |
| `ferrari` | `red` | `auto` | `system` |
| `amazonia` | `#948787` | `stone` | `system` |

La paleta más cercana no siempre es el nombre esperado: `orange` da `red` y `grass` da `cyan`, porque sus colores de
marca están más cerca de esas paletas. `dust` y `amazonia` tienen una marca gris y conservan su hex.

Estado: implementado. El servidor aplica el preset cuando un tema no tiene fila en `AweThm` (`amazonia` solo existe en
LESS) o su fila no tiene un valor válido, y la migración `AWE_V1.5.0` escribe los mismos valores en las filas clásicas
de `AweThm`. Los motores lo pintan en `<html>`; las pantallas mantienen el aspecto clásico hasta el nuevo aspecto de los motores.

## Colores semánticos {#semantic-colours}

Los tokens más usados. La lista completa está en la [referencia de tokens](tokens-reference.md#tier-2-semantic).

| Necesidad | Token |
|-----------|-------|
| Texto normal, destacado y atenuado | `--awe-color-text`, `--awe-color-text-emphasis`, `--awe-color-text-muted` |
| Página, tarjeta, zona sutil, hover | `--awe-color-background`, `--awe-color-surface`, `--awe-color-surface-subtle`, `--awe-color-surface-hover` |
| Bordes | `--awe-color-border`, `--awe-color-border-subtle` |
| Primario relleno y su texto | `--awe-color-primary`, `--awe-color-on-primary` |
| Primario como texto o línea fina (enlaces, pestaña activa) | `--awe-color-primary-text` |
| Opción, fila o elemento de menú seleccionado | `--awe-color-highlight`, `--awe-color-on-highlight` |
| Campos de formulario | `--awe-color-field`, `--awe-color-field-border`, `--awe-color-field-border-focus`, `--awe-color-field-border-invalid` |
| Capas superpuestas | `--awe-color-overlay`, `--awe-color-overlay-border`, `--awe-color-mask` |
| Estados | `--awe-color-<danger\|success\|warning\|info>` (sólido), `-subtle` (fondo), `-text`, `-border`, `on-<estado>` |

Parejas que hay que respetar: un token de fondo va con su token `on-` o `-text`. Nunca ponga `--awe-color-text-muted`
sobre un primario relleno ni texto en `--awe-color-primary` sobre `--awe-color-highlight` (use
`--awe-color-on-highlight`).

## Reglas de contraste {#contrast-rules}

Medido para 17 primarios × 6 ajustes de superficie × 2 modos con los tokens del prototipo.

| Regla | Objetivo | Estado |
|-------|----------|--------|
| Texto e iconos con significado | 4,5:1 (WCAG 1.4.3) | Lo cumplen todas las parejas de la tabla anterior. La más ajustada: 4,51:1. |
| Anillo de foco contra su fondo | 3:1 (WCAG 1.4.11) | Se cumple (anillo de 2px en `--awe-color-primary-text`). |
| Texto deshabilitado | Exento (WCAG 1.4.3) | 3,76:1 en claro, 4,04:1 en oscuro. Aceptado. |
| Primario relleno contra la página (`lime`, `amber`, `yellow`) | 3:1 (WCAG 1.4.11) | De 1,9:1 a 2,2:1. Aceptado: el texto sobre el relleno da 7:1 y el control tiene texto. |
| Borde de los campos | 3:1 (WCAG 1.4.11) | 1,47:1 en claro y 2,53:1 en oscuro. Aceptado: se mantiene el borde suave; la etiqueta, el anillo de foco y el estado inválido identifican el campo. |

## Visualización de datos {#data-visualisation}

| Necesidad | Tokens |
|-----------|--------|
| Celdas con escala de color (mapa de calor), 5 pasos desde el primario | `--awe-color-scale-1..5`, texto `--awe-color-on-scale-1..5` |
| Barras de datos en celdas | `--awe-color-data-bar`, `--awe-color-data-bar-negative` |
| Sparklines y gráficos pequeños | `--awe-color-chart-line`, `--awe-color-chart-area`, `--awe-color-chart-negative`, `--awe-color-chart-axis` |
| Ilustración y controles del mapa | `--awe-color-map-*` |

Los valores negativos usan siempre el color de peligro **y** una segunda señal (signo, posición), nunca solo el color.
