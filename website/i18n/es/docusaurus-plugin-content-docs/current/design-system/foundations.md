---
id: foundations
title: Fundamentos
sidebar_label: Fundamentos
---

Tipografía, espaciado, densidad, forma, elevación, movimiento e iconos. Cada valor de esta página es un token: use el
token, no el número. Los valores exactos de las dos densidades están en la
[referencia de tokens](tokens-reference.md#tier-2-semantic).

## Tipografía {#typography}

La fuente es **Inter** (variable, OFL-1.1), servida por AWE, con una alternativa del sistema:
`--awe-font-family-base: 'Inter Variable', Inter, system-ui, ...`. El código y los identificadores usan
`--awe-font-family-monospace`.

| Token | Cómoda | Compacta | Uso |
|-------|--------|----------|-----|
| `--awe-font-size-xs` | 12px | 11px | Pies, badges, cabeceras de grid en espacios estrechos |
| `--awe-font-size-sm` | 13px | 12px | Etiquetas, ayudas, líneas secundarias, cabeceras de grid |
| `--awe-font-size-base` | 14px | 13px | Texto, campos, botones, celdas |
| `--awe-font-size-md` | 16px | 15px | Títulos de tarjetas y secciones |
| `--awe-font-size-lg` | 18px | 18px | Títulos de diálogos |
| `--awe-font-size-xl` | 20px | 20px | Título de página en compacta |
| `--awe-font-size-2xl` | 24px | 24px | Título de página, valores de KPI |
| `--awe-font-size-3xl` | 30px | 30px | Display (login y páginas independientes) |

Los tokens de rol apuntan a la escala y son preferibles: `--awe-font-size-page-title`, `--awe-font-size-section-title`,
`--awe-font-size-dialog-title`, `--awe-font-size-display`, `--awe-font-size-label`, `--awe-font-size-caption`.

| Peso | Token | Uso |
|------|-------|-----|
| 400 | `--awe-font-weight-normal` | Texto, valores |
| 500 | `--awe-font-weight-medium` | Etiquetas, botones, opciones de menú, pestañas |
| 600 | `--awe-font-weight-semibold` | Títulos, elementos seleccionados, totales |
| 700 | `--awe-font-weight-bold` | Poco frecuente: badges con texto muy corto |

- Interlineados: `--awe-line-height-tight` (1,25, títulos), `--awe-line-height-snug` (1,375, etiquetas, listas) y
  `--awe-line-height-base` (1,5, texto).
- Los títulos usan un espaciado de letras ligeramente negativo (`--awe-letter-spacing-display`,
  `--awe-letter-spacing-tight`). Las etiquetas pequeñas en mayúsculas usan `--awe-letter-spacing-caps`.
- **Los números de grids, totales y KPI usan cifras tabulares:**
  `font-variant-numeric: var(--awe-font-variant-numeric)`, alineados a la derecha, para que los dígitos cuadren.

## Espaciado {#spacing}

El espaciado sigue una **rejilla de 4px**. Los tokens numéricos son la escala; los nombres de talla actuales se
mantienen como alias.

| Token | Valor | Alias |
|-------|-------|-------|
| `--awe-space-0-5` | 2px | `--awe-space-2xs` |
| `--awe-space-1` | 4px | `--awe-space-xs` |
| `--awe-space-1-5` | 6px | |
| `--awe-space-2` | 8px | `--awe-space-sm` |
| `--awe-space-3` | 12px | `--awe-space-md` |
| `--awe-space-4` | 16px | `--awe-space-lg` |
| `--awe-space-5` | 20px | |
| `--awe-space-6` | 24px | `--awe-space-xl` |
| `--awe-space-8` | 32px | `--awe-space-2xl` (nuevo) |
| `--awe-space-10`, `-12`, `-16` | 40px, 48px, 64px | |

:::warning Cambio de valor
Los nombres de talla se mantienen, pero **no sus valores**: hoy `xs` vale 5px, `sm` 10px, `md` 15px, `lg` 20px y `xl`
30px. En AWE 5 valen 4, 8, 12, 16 y 24px. Hay que revisar el CSS de producto que dependa de los valores antiguos.
:::

El espaciado de la disposición tiene sus propios tokens, que cambian con la densidad: `--awe-content-padding` (dentro de
las tarjetas), `--awe-layout-gap` (entre tarjetas), `--awe-page-padding` (bordes de la página),
`--awe-gap-form-row` y `--awe-gap-form-column` (rejilla de criterios).

## Densidad {#density}

| | Cómoda (por defecto) | Compacta |
|---|---|---|
| Fuente base | 14px | 13px |
| Altura de control (`--awe-size-control`) | 36px | 30px |
| Control pequeño / grande | 30px / 44px | 26px / 36px |
| Relleno de campo | 8px × 12px | 5px × 9px |
| Relleno de celda del grid | 11px × 14px | 6px × 10px |
| Altura de la cabecera | 60px | 52px |
| Ancho del menú | 264px | 240px |

La densidad es `data-awe-density="compact"` en `<html>`. Se configura por aplicación y por usuario. Los componentes la
obtienen sin más si usan los tokens de tamaño, relleno y separación. Nunca escriba una regla de componente para una
sola densidad.

## Forma {#shape}

Escala de radios (Aura):

| Token | Valor | Uso |
|-------|-------|-----|
| `--awe-radius-xs` | 2px | Resaltados de texto, marcas pequeñas |
| `--awe-radius-sm` | 4px | Casillas, chips, etiquetas, opciones de lista |
| `--awe-radius-md` | 6px | Campos, botones, capas (`--awe-radius-field`, `--awe-radius-overlay`) |
| `--awe-radius-lg` | 8px | Paneles interiores, tarjetas de lanzadera |
| `--awe-radius-xl` | 12px | Tarjetas y diálogos (`--awe-radius-card`, `--awe-radius-modal`) |
| `--awe-radius-full` | 9999px | Avatares, píldoras, interruptores |

`--awe-radius` se mantiene como alias de `md`. Hoy `--awe-radius-sm` y `-lg` se derivan (la mitad y el doble); en
AWE 5 son valores fijos de la escala.

## Elevación {#elevation}

| Token | Uso |
|-------|-----|
| `--awe-shadow-xs` (`--awe-shadow-card`, `--awe-shadow-field`) | Tarjetas, campos, botones con borde: apenas elevados |
| `--awe-shadow-sm` | Segmento seleccionado, tirador del interruptor |
| `--awe-shadow-md` (`--awe-shadow-overlay`) | Desplegables, listas, popovers, toasts, tooltips |
| `--awe-shadow-lg` (`--awe-shadow-modal`) | Diálogos, panel lateral, ventana maximizada |

Las tarjetas se apoyan en un borde más `xs`, no en sombras fuertes. Las sombras son más intensas en modo oscuro
(`--awe-color-shadow`). Las capas tienen tokens de z-index fijos: `--awe-z-header`, `--awe-z-menu`, `--awe-z-dropdown`,
`--awe-z-modal`, `--awe-z-toast` y `--awe-z-tooltip`. Las capas que no deben quedar recortadas (listas, flyouts, toasts)
usan la top layer (`popover`, `<dialog>`).

## Foco {#focus}

El foco de teclado es un contorno de 2px en `--awe-color-focus-ring` con 2px de separación (`--awe-focus-ring`,
`--awe-focus-ring-offset`), mostrado con `:focus-visible`. Los campos muestran el borde de foco más un halo suave
(`--awe-shadow-field-focus`; `--awe-shadow-field-invalid` si son inválidos). Nunca quite un contorno sin sustituirlo.

## Movimiento {#motion}

| Token | Valor | Uso |
|-------|-------|-----|
| `--awe-motion-duration-fast` | 120ms | Hover, cambios de color, salida de elementos pequeños |
| `--awe-motion-duration-base` | 200ms | Conmutadores, diálogos, tooltips |
| `--awe-motion-duration-slow` | 300ms | Toasts, panel lateral, máscara |
| `--awe-motion-duration-expand` | 240ms | Expandir y plegar (niveles del menú, acordeones, rail) |
| `--awe-motion-easing-standard` | `cubic-bezier(0.2, 0, 0, 1)` | La mayoría de transiciones |
| `--awe-motion-easing-enter` / `-exit` | deceleración / aceleración | Elementos que aparecen / se van |
| `--awe-motion-easing-expand` | `cubic-bezier(0.22, 1, 0.36, 1)` | Contenido desplegable |

- Con `prefers-reduced-motion: reduce` todas las duraciones valen 0. Si usa los tokens, lo obtiene sin más.
- Sin animación en el primer pintado: un estado restaurado (un nivel del menú abierto) aparece ya en su sitio.
- Anime el tamaño real: los desplegables animan `grid-template-rows` de `0fr` a `1fr`, no un `max-height` fijo.

## Iconografía {#iconography}

AWE 5 usa los iconos de línea **Lucide** (ISC) en los dos motores: rejilla de 24px, trazo de 1,75px fijado por CSS,
`currentColor`. Tamaños: `--awe-size-icon-sm`, `--awe-size-icon` y `--awe-size-icon-lg` (14, 16 y 20px en cómoda).

El XML conserva sus nombres de icono. Una **tabla de equivalencias** asocia cada nombre `fa-*` a un icono Lucide, así
que las pantallas actuales no cambian. Ejemplo:

| Icono del XML | Lucide |
|---------------|--------|
| `fa-search` | `search` |
| `fa-plus` | `plus` |
| `fa-trash` | `trash-2` |
| `fa-pencil`, `fa-edit` | `pencil` |
| `fa-floppy-o` | `save` |
| `fa-filter` | `filter` |
| `fa-download` | `download` |
| `fa-print` | `printer` |
| `fa-times` | `x` |
| `fa-cog` | `settings` |

La tabla completa, la alternativa para nombres sin equivalencia y el tratamiento de los iconos `mdi-*` son trabajo de
implementación.

Reglas:

- Un botón solo con icono necesita `aria-label` y un tooltip.
- Los iconos decorativos llevan `aria-hidden="true"`.
- El color viene del texto padre; no coloree los iconos por separado salvo para indicar estado.
