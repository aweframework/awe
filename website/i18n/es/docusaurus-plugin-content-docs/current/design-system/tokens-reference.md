---
id: tokens-reference
title: Referencia de tokens
sidebar_label: Referencia de tokens
---

Todos los tokens de la propuesta de AWE 5, generados a partir del
[`tokens.css`](pathname:///design-system/prototype/tokens.css) del prototipo, y la correspondencia con los tokens
actuales. Para verlos aplicados en los dos modos, abra la [página de tokens](pathname:///design-system/prototype/tokens.html)
del prototipo.

| Nivel | Tokens |
|-------|--------|
| 1 · Primitivo | 242 |
| 1 · Configurable | 27 |
| 2 · Semántico | 257 |
| 3 · Componente | 58 |
| Solo del prototipo | 5 |
| **Total** | **589** |

Cómo leer las tablas:

- **Claro / Oscuro**: los dos argumentos de `light-dark()`. "= claro" indica que el token no depende del modo.
- **Cómoda / Compacta**: el valor por densidad. Una celda Compacta vacía indica el mismo valor que en Cómoda.
- `var(--awe-primary-fill)` y el resto de tokens configurables dependen de `data-awe-primary` y `data-awe-surface`.

## Tokens {#tokens}

### Nivel 1: paletas primitivas {#tier-1-primitive}

Nombre: `--awe-palette-<paleta>-<tono>`. 22 paletas × 11 tonos = 242 tokens.

| Paleta | 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 950 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `emerald` | `#ecfdf5` | `#d1fae5` | `#a7f3d0` | `#6ee7b7` | `#34d399` | `#10b981` | `#059669` | `#047857` | `#065f46` | `#064e3b` | `#022c22` |
| `green` | `#f0fdf4` | `#dcfce7` | `#bbf7d0` | `#86efac` | `#4ade80` | `#22c55e` | `#16a34a` | `#15803d` | `#166534` | `#14532d` | `#052e16` |
| `lime` | `#f7fee7` | `#ecfccb` | `#d9f99d` | `#bef264` | `#a3e635` | `#84cc16` | `#65a30d` | `#4d7c0f` | `#3f6212` | `#365314` | `#1a2e05` |
| `red` | `#fef2f2` | `#fee2e2` | `#fecaca` | `#fca5a5` | `#f87171` | `#ef4444` | `#dc2626` | `#b91c1c` | `#991b1b` | `#7f1d1d` | `#450a0a` |
| `orange` | `#fff7ed` | `#ffedd5` | `#fed7aa` | `#fdba74` | `#fb923c` | `#f97316` | `#ea580c` | `#c2410c` | `#9a3412` | `#7c2d12` | `#431407` |
| `amber` | `#fffbeb` | `#fef3c7` | `#fde68a` | `#fcd34d` | `#fbbf24` | `#f59e0b` | `#d97706` | `#b45309` | `#92400e` | `#78350f` | `#451a03` |
| `yellow` | `#fefce8` | `#fef9c3` | `#fef08a` | `#fde047` | `#facc15` | `#eab308` | `#ca8a04` | `#a16207` | `#854d0e` | `#713f12` | `#422006` |
| `teal` | `#f0fdfa` | `#ccfbf1` | `#99f6e4` | `#5eead4` | `#2dd4bf` | `#14b8a6` | `#0d9488` | `#0f766e` | `#115e59` | `#134e4a` | `#042f2e` |
| `cyan` | `#ecfeff` | `#cffafe` | `#a5f3fc` | `#67e8f9` | `#22d3ee` | `#06b6d4` | `#0891b2` | `#0e7490` | `#155e75` | `#164e63` | `#083344` |
| `sky` | `#f0f9ff` | `#e0f2fe` | `#bae6fd` | `#7dd3fc` | `#38bdf8` | `#0ea5e9` | `#0284c7` | `#0369a1` | `#075985` | `#0c4a6e` | `#082f49` |
| `blue` | `#eff6ff` | `#dbeafe` | `#bfdbfe` | `#93c5fd` | `#60a5fa` | `#3b82f6` | `#2563eb` | `#1d4ed8` | `#1e40af` | `#1e3a8a` | `#172554` |
| `indigo` | `#eef2ff` | `#e0e7ff` | `#c7d2fe` | `#a5b4fc` | `#818cf8` | `#6366f1` | `#4f46e5` | `#4338ca` | `#3730a3` | `#312e81` | `#1e1b4b` |
| `violet` | `#f5f3ff` | `#ede9fe` | `#ddd6fe` | `#c4b5fd` | `#a78bfa` | `#8b5cf6` | `#7c3aed` | `#6d28d9` | `#5b21b6` | `#4c1d95` | `#2e1065` |
| `purple` | `#faf5ff` | `#f3e8ff` | `#e9d5ff` | `#d8b4fe` | `#c084fc` | `#a855f7` | `#9333ea` | `#7e22ce` | `#6b21a8` | `#581c87` | `#3b0764` |
| `fuchsia` | `#fdf4ff` | `#fae8ff` | `#f5d0fe` | `#f0abfc` | `#e879f9` | `#d946ef` | `#c026d3` | `#a21caf` | `#86198f` | `#701a75` | `#4a044e` |
| `pink` | `#fdf2f8` | `#fce7f3` | `#fbcfe8` | `#f9a8d4` | `#f472b6` | `#ec4899` | `#db2777` | `#be185d` | `#9d174d` | `#831843` | `#500724` |
| `rose` | `#fff1f2` | `#ffe4e6` | `#fecdd3` | `#fda4af` | `#fb7185` | `#f43f5e` | `#e11d48` | `#be123c` | `#9f1239` | `#881337` | `#4c0519` |
| `slate` | `#f8fafc` | `#f1f5f9` | `#e2e8f0` | `#cbd5e1` | `#94a3b8` | `#64748b` | `#475569` | `#334155` | `#1e293b` | `#0f172a` | `#020617` |
| `gray` | `#f9fafb` | `#f3f4f6` | `#e5e7eb` | `#d1d5db` | `#9ca3af` | `#6b7280` | `#4b5563` | `#374151` | `#1f2937` | `#111827` | `#030712` |
| `zinc` | `#fafafa` | `#f4f4f5` | `#e4e4e7` | `#d4d4d8` | `#a1a1aa` | `#71717a` | `#52525b` | `#3f3f46` | `#27272a` | `#18181b` | `#09090b` |
| `neutral` | `#fafafa` | `#f5f5f5` | `#e5e5e5` | `#d4d4d4` | `#a3a3a3` | `#737373` | `#525252` | `#404040` | `#262626` | `#171717` | `#0a0a0a` |
| `stone` | `#fafaf9` | `#f5f5f4` | `#e7e5e4` | `#d6d3d1` | `#a8a29e` | `#78716c` | `#57534e` | `#44403c` | `#292524` | `#1c1917` | `#0c0a09` |

### Nivel 1: paletas configurables {#tier-1-configurable}

Los valores siguientes son los de las paletas con nombre. Cuando un tema indica un hex, los mismos tokens contienen los
valores generados (ver [Paletas generadas a partir de un hex](color-and-themes.md#palettes-generated-from-a-hex)).

| Token | Valor por defecto (emerald) |
|---|---|
| `--awe-primary-50` | `var(--awe-palette-emerald-50)` |
| `--awe-primary-100` | `var(--awe-palette-emerald-100)` |
| `--awe-primary-200` | `var(--awe-palette-emerald-200)` |
| `--awe-primary-300` | `var(--awe-palette-emerald-300)` |
| `--awe-primary-400` | `var(--awe-palette-emerald-400)` |
| `--awe-primary-500` | `var(--awe-palette-emerald-500)` |
| `--awe-primary-600` | `var(--awe-palette-emerald-600)` |
| `--awe-primary-700` | `var(--awe-palette-emerald-700)` |
| `--awe-primary-800` | `var(--awe-palette-emerald-800)` |
| `--awe-primary-900` | `var(--awe-palette-emerald-900)` |
| `--awe-primary-950` | `var(--awe-palette-emerald-950)` |
| `--awe-primary-fill` | `var(--awe-primary-700)` |
| `--awe-primary-fill-hover` | `var(--awe-primary-800)` |
| `--awe-primary-fill-active` | `var(--awe-primary-900)` |
| `--awe-primary-on-fill` | `#ffffff` |

El primario se elige con `data-awe-primary` en `<html>`. Tonos de relleno por paleta (modo claro):

| Paleta | Relleno | Hover | Activo | Texto sobre el relleno |
|---|---|---|---|---|
| `emerald` | 700 | 800 | 900 | `#ffffff` |
| `green` | 700 | 800 | 900 | `#ffffff` |
| `lime` | 500 | 400 | 600 | `950` |
| `red` | 600 | 700 | 800 | `#ffffff` |
| `orange` | 700 | 800 | 900 | `#ffffff` |
| `amber` | 500 | 400 | 600 | `950` |
| `yellow` | 500 | 400 | 600 | `950` |
| `teal` | 700 | 800 | 900 | `#ffffff` |
| `cyan` | 700 | 800 | 900 | `#ffffff` |
| `sky` | 700 | 800 | 900 | `#ffffff` |
| `blue` | 600 | 700 | 800 | `#ffffff` |
| `indigo` | 600 | 700 | 800 | `#ffffff` |
| `violet` | 600 | 700 | 800 | `#ffffff` |
| `purple` | 600 | 700 | 800 | `#ffffff` |
| `fuchsia` | 600 | 700 | 800 | `#ffffff` |
| `pink` | 600 | 700 | 800 | `#ffffff` |
| `rose` | 600 | 700 | 800 | `#ffffff` |

Superficie: `data-awe-surface` en `<html>` (`slate`, `gray`, `zinc`, `neutral`, `stone`). Sin el atributo:

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-surface-0` | `#ffffff` | `#ffffff` |
| `--awe-surface-50` | `var(--awe-palette-slate-50)` | `var(--awe-palette-zinc-50)` |
| `--awe-surface-100` | `var(--awe-palette-slate-100)` | `var(--awe-palette-zinc-100)` |
| `--awe-surface-200` | `var(--awe-palette-slate-200)` | `var(--awe-palette-zinc-200)` |
| `--awe-surface-300` | `var(--awe-palette-slate-300)` | `var(--awe-palette-zinc-300)` |
| `--awe-surface-400` | `var(--awe-palette-slate-400)` | `var(--awe-palette-zinc-400)` |
| `--awe-surface-500` | `var(--awe-palette-slate-500)` | `var(--awe-palette-zinc-500)` |
| `--awe-surface-600` | `var(--awe-palette-slate-600)` | `var(--awe-palette-zinc-600)` |
| `--awe-surface-700` | `var(--awe-palette-slate-700)` | `var(--awe-palette-zinc-700)` |
| `--awe-surface-800` | `var(--awe-palette-slate-800)` | `var(--awe-palette-zinc-800)` |
| `--awe-surface-900` | `var(--awe-palette-slate-900)` | `var(--awe-palette-zinc-900)` |
| `--awe-surface-950` | `var(--awe-palette-slate-950)` | `var(--awe-palette-zinc-950)` |

### Nivel 2: semánticos {#tier-2-semantic}

**Color · Marca**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-primary` | `var(--awe-primary-fill)` | `var(--awe-primary-400)` |
| `--awe-color-primary-hover` | `var(--awe-primary-fill-hover)` | `var(--awe-primary-300)` |
| `--awe-color-primary-active` | `var(--awe-primary-fill-active)` | `var(--awe-primary-200)` |
| `--awe-color-on-primary` | `var(--awe-primary-on-fill)` | `var(--awe-surface-900)` |
| `--awe-color-primary-text` | `var(--awe-primary-700)` | `var(--awe-primary-400)` |
| `--awe-color-primary-subtle` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-500), transparent 84%)` |
| `--awe-color-on-primary-subtle` | `var(--awe-primary-700)` | `var(--awe-primary-300)` |
| `--awe-color-theme` | `var(--awe-color-primary)` | = claro |
| `--awe-color-secondary` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-secondary-hover` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-on-secondary` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Color · Texto**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-text` | `var(--awe-surface-700)` | `var(--awe-surface-0)` |
| `--awe-color-text-emphasis` | `var(--awe-surface-900)` | `var(--awe-surface-0)` |
| `--awe-color-text-muted` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-text-hover` | `var(--awe-surface-800)` | `var(--awe-surface-0)` |
| `--awe-color-text-muted-hover` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Color · Página y contenido**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-background` | `var(--awe-surface-50)` | `var(--awe-surface-950)` |
| `--awe-color-surface` | `var(--awe-surface-0)` | `var(--awe-surface-900)` |
| `--awe-color-surface-hover` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-surface-subtle` | `var(--awe-surface-50)` | `color-mix(in srgb, var(--awe-surface-800), transparent 50%)` |
| `--awe-color-surface-raised` | `var(--awe-surface-0)` | `var(--awe-surface-600)` |
| `--awe-color-border` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-border-subtle` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-shadow` | `rgb(0 0 0 / 0.1)` | `rgb(0 0 0 / 0.45)` |
| `--awe-color-shadow-soft` | `rgb(18 18 23 / 0.05)` | `rgb(0 0 0 / 0.3)` |
| `--awe-color-content` | `var(--awe-color-surface)` | = claro |
| `--awe-color-content-hover` | `var(--awe-color-surface-hover)` | = claro |
| `--awe-color-content-border` | `var(--awe-color-border)` | = claro |
| `--awe-color-mask` | `rgb(0 0 0 / 0.4)` | `rgb(0 0 0 / 0.6)` |

**Color · Resaltado (opción, fila u opción de menú seleccionada)**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-highlight` | `var(--awe-primary-50)` | `color-mix(in srgb, var(--awe-primary-400), transparent 84%)` |
| `--awe-color-highlight-focus` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400), transparent 76%)` |
| `--awe-color-on-highlight` | `var(--awe-primary-700)` | `rgb(255 255 255 / 0.87)` |
| `--awe-color-on-highlight-focus` | `var(--awe-primary-800)` | `rgb(255 255 255 / 0.87)` |

**Color · Menú, cabecera, paneles y grids (vocabulario actual de AWE)**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-menu` | `var(--awe-color-surface)` | = claro |
| `--awe-color-menu-accent` | `var(--awe-color-highlight)` | = claro |
| `--awe-color-menu-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-navbar` | `var(--awe-color-surface)` | = claro |
| `--awe-color-navbar-accent` | `var(--awe-color-surface-hover)` | = claro |
| `--awe-color-navbar-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-navbar-dropdown` | `var(--awe-color-overlay)` | = claro |
| `--awe-color-panel` | `var(--awe-color-surface)` | = claro |
| `--awe-color-panel-header` | `var(--awe-color-surface)` | = claro |
| `--awe-color-panel-border` | `var(--awe-color-border)` | = claro |
| `--awe-color-panel-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-grid-header` | `var(--awe-color-surface-subtle)` | = claro |
| `--awe-color-grid-border` | `var(--awe-color-border)` | = claro |
| `--awe-color-skeleton` | `var(--awe-surface-200)` | `var(--awe-surface-800)` |
| `--awe-color-skeleton-highlight` | `var(--awe-surface-100)` | `var(--awe-surface-700)` |

**Color · Capas superpuestas (listas, popovers, menús, diálogos)**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-overlay` | `var(--awe-surface-0)` | `var(--awe-surface-900)` |
| `--awe-color-overlay-border` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-overlay-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-tooltip` | `var(--awe-surface-800)` | `var(--awe-surface-700)` |
| `--awe-color-on-tooltip` | `var(--awe-surface-0)` | = claro |

**Color · Campos de formulario**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-field` | `var(--awe-surface-0)` | `var(--awe-surface-950)` |
| `--awe-color-field-filled` | `var(--awe-surface-50)` | `var(--awe-surface-800)` |
| `--awe-color-field-border` | `var(--awe-surface-300)` | `var(--awe-surface-600)` |
| `--awe-color-field-border-hover` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-field-border-focus` | `var(--awe-color-primary)` | = claro |
| `--awe-color-field-border-invalid` | `var(--awe-palette-red-500)` | `var(--awe-palette-red-300)` |
| `--awe-color-field-text` | `var(--awe-surface-700)` | `var(--awe-surface-0)` |
| `--awe-color-field-placeholder` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-field-placeholder-invalid` | `var(--awe-palette-red-600)` | `var(--awe-palette-red-400)` |
| `--awe-color-field-icon` | `var(--awe-surface-400)` | = claro |
| `--awe-color-field-label` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-color-field-help` | `var(--awe-color-text-muted)` | = claro |
| `--awe-color-field-focus-halo` | `color-mix(in srgb, var(--awe-color-primary), transparent 80%)` | = claro |
| `--awe-color-field-invalid-halo` | `color-mix(in srgb, var(--awe-color-danger), transparent 82%)` | = claro |

**Color · Opciones de lista (select, autocompletar, menús)**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-list-option-focus` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-list-option-selected` | `var(--awe-color-highlight)` | = claro |
| `--awe-color-list-option-selected-focus` | `var(--awe-color-highlight-focus)` | = claro |
| `--awe-color-list-option-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-list-option-text-focus` | `var(--awe-color-text-hover)` | = claro |
| `--awe-color-list-option-text-selected` | `var(--awe-color-on-highlight)` | = claro |
| `--awe-color-list-option-icon` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-list-option-group` | `var(--awe-color-text-muted)` | = claro |

**Color · Elementos de navegación (menú lateral, breadcrumb, pestañas, menú de usuario)**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-nav-item-focus` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-nav-item-active` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-nav-item-text` | `var(--awe-color-text)` | = claro |
| `--awe-color-nav-item-text-active` | `var(--awe-color-text-hover)` | = claro |
| `--awe-color-nav-item-icon` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-nav-item-icon-active` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-nav-submenu-label` | `var(--awe-color-text-muted)` | = claro |

**Color · Estados**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-disabled` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-disabled-border` | `var(--awe-surface-300)` | `var(--awe-surface-600)` |
| `--awe-color-disabled-text` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-required` | `var(--awe-palette-red-600)` | `var(--awe-palette-red-400)` |
| `--awe-color-loading-bar` | `var(--awe-color-primary)` | = claro |
| `--awe-color-danger` | `var(--awe-palette-red-600)` | `var(--awe-palette-red-400)` |
| `--awe-color-danger-hover` | `var(--awe-palette-red-700)` | `var(--awe-palette-red-300)` |
| `--awe-color-on-danger` | `#ffffff` | `var(--awe-palette-red-950)` |
| `--awe-color-danger-subtle` | `var(--awe-palette-red-100)` | `color-mix(in srgb, var(--awe-palette-red-500), transparent 84%)` |
| `--awe-color-danger-text` | `var(--awe-palette-red-700)` | `var(--awe-palette-red-300)` |
| `--awe-color-danger-border` | `var(--awe-palette-red-200)` | `color-mix(in srgb, var(--awe-palette-red-700), transparent 64%)` |
| `--awe-color-success` | `var(--awe-palette-green-700)` | `var(--awe-palette-green-400)` |
| `--awe-color-success-hover` | `var(--awe-palette-green-800)` | `var(--awe-palette-green-300)` |
| `--awe-color-on-success` | `#ffffff` | `var(--awe-palette-green-950)` |
| `--awe-color-success-subtle` | `var(--awe-palette-green-100)` | `color-mix(in srgb, var(--awe-palette-green-500), transparent 84%)` |
| `--awe-color-success-text` | `var(--awe-palette-green-700)` | `var(--awe-palette-green-300)` |
| `--awe-color-success-border` | `var(--awe-palette-green-200)` | `color-mix(in srgb, var(--awe-palette-green-700), transparent 64%)` |
| `--awe-color-warning` | `var(--awe-palette-orange-700)` | `var(--awe-palette-orange-400)` |
| `--awe-color-warning-hover` | `var(--awe-palette-orange-800)` | `var(--awe-palette-orange-300)` |
| `--awe-color-on-warning` | `#ffffff` | `var(--awe-palette-orange-950)` |
| `--awe-color-warning-subtle` | `var(--awe-palette-orange-100)` | `color-mix(in srgb, var(--awe-palette-orange-500), transparent 84%)` |
| `--awe-color-warning-text` | `var(--awe-palette-orange-700)` | `var(--awe-palette-orange-300)` |
| `--awe-color-warning-border` | `var(--awe-palette-orange-200)` | `color-mix(in srgb, var(--awe-palette-orange-700), transparent 64%)` |
| `--awe-color-info` | `var(--awe-palette-sky-700)` | `var(--awe-palette-sky-400)` |
| `--awe-color-info-hover` | `var(--awe-palette-sky-800)` | `var(--awe-palette-sky-300)` |
| `--awe-color-on-info` | `#ffffff` | `var(--awe-palette-sky-950)` |
| `--awe-color-info-subtle` | `var(--awe-palette-sky-100)` | `color-mix(in srgb, var(--awe-palette-sky-500), transparent 84%)` |
| `--awe-color-info-text` | `var(--awe-palette-sky-700)` | `var(--awe-palette-sky-300)` |
| `--awe-color-info-border` | `var(--awe-palette-sky-200)` | `color-mix(in srgb, var(--awe-palette-sky-700), transparent 64%)` |
| `--awe-color-neutral-subtle` | `var(--awe-color-secondary)` | = claro |
| `--awe-color-neutral-text` | `var(--awe-color-on-secondary)` | = claro |
| `--awe-color-contrast` | `var(--awe-surface-950)` | `var(--awe-surface-0)` |
| `--awe-color-on-contrast` | `var(--awe-surface-0)` | `var(--awe-surface-950)` |

**Visualización de datos**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-color-scale-1` | `var(--awe-primary-50)` | `color-mix(in srgb, var(--awe-primary-400) 12%, var(--awe-surface-900))` |
| `--awe-color-scale-2` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400) 22%, var(--awe-surface-900))` |
| `--awe-color-scale-3` | `var(--awe-primary-200)` | `color-mix(in srgb, var(--awe-primary-400) 34%, var(--awe-surface-900))` |
| `--awe-color-scale-4` | `var(--awe-primary-300)` | `color-mix(in srgb, var(--awe-primary-400) 48%, var(--awe-surface-900))` |
| `--awe-color-scale-5` | `var(--awe-color-primary)` | = claro |
| `--awe-color-on-scale-1` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-color-on-scale-2` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-color-on-scale-3` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-color-on-scale-4` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-color-on-scale-5` | `var(--awe-color-on-primary)` | = claro |
| `--awe-color-data-bar` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400) 35%, transparent)` |
| `--awe-color-data-bar-negative` | `var(--awe-palette-red-100)` | `color-mix(in srgb, var(--awe-palette-red-400) 35%, transparent)` |
| `--awe-color-chart-line` | `var(--awe-color-primary-text)` | = claro |
| `--awe-color-chart-area` | `color-mix(in srgb, var(--awe-color-primary-text) 18%, transparent)` | = claro |
| `--awe-color-chart-negative` | `var(--awe-color-danger)` | = claro |
| `--awe-color-chart-axis` | `var(--awe-color-border)` | = claro |
| `--awe-color-map-water` | `var(--awe-palette-sky-100)` | `color-mix(in srgb, var(--awe-palette-sky-500) 16%, var(--awe-surface-950))` |
| `--awe-color-map-land` | `var(--awe-surface-50)` | `var(--awe-surface-900)` |
| `--awe-color-map-park` | `var(--awe-palette-green-100)` | `color-mix(in srgb, var(--awe-palette-green-500) 14%, var(--awe-surface-900))` |
| `--awe-color-map-road` | `var(--awe-surface-0)` | `var(--awe-surface-700)` |
| `--awe-color-map-road-casing` | `var(--awe-surface-300)` | `var(--awe-surface-800)` |
| `--awe-color-map-border` | `var(--awe-surface-400)` | `var(--awe-surface-600)` |
| `--awe-color-map-label` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Tipografía**

| Token | Cómoda | Compacta |
|---|---|---|
| `--awe-font-family-base` | `'Inter Variable', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif` |  |
| `--awe-font-family-monospace` | `ui-monospace, 'SF Mono', 'Cascadia Code', Menlo, Consolas, monospace` |  |
| `--awe-font-size-xs` | `12px` | `11px` |
| `--awe-font-size-sm` | `13px` | `12px` |
| `--awe-font-size-base` | `14px` | `13px` |
| `--awe-font-size-md` | `16px` | `15px` |
| `--awe-font-size-lg` | `18px` |  |
| `--awe-font-size-xl` | `20px` |  |
| `--awe-font-size-2xl` | `24px` |  |
| `--awe-font-size-3xl` | `30px` |  |
| `--awe-font-size-small` | `var(--awe-font-size-sm)` |  |
| `--awe-font-size-page-title` | `var(--awe-font-size-2xl)` | `var(--awe-font-size-xl)` |
| `--awe-font-size-section-title` | `var(--awe-font-size-md)` |  |
| `--awe-font-size-dialog-title` | `var(--awe-font-size-lg)` |  |
| `--awe-font-size-display` | `var(--awe-font-size-3xl)` | `var(--awe-font-size-2xl)` |
| `--awe-font-size-label` | `var(--awe-font-size-sm)` |  |
| `--awe-font-size-caption` | `var(--awe-font-size-xs)` |  |
| `--awe-font-weight-normal` | `400` |  |
| `--awe-font-weight-medium` | `500` |  |
| `--awe-font-weight-semibold` | `600` |  |
| `--awe-font-weight-bold` | `700` |  |
| `--awe-line-height-tight` | `1.25` |  |
| `--awe-line-height-snug` | `1.375` |  |
| `--awe-line-height-base` | `1.5` |  |
| `--awe-letter-spacing-tight` | `-0.011em` |  |
| `--awe-letter-spacing-display` | `-0.022em` |  |
| `--awe-letter-spacing-caps` | `0.06em` |  |
| `--awe-font-feature-base` | `'cv05', 'calt'` |  |
| `--awe-font-variant-numeric` | `tabular-nums` |  |

**Espaciado, radios, elevación, foco, movimiento, capas**

| Token | Valor | Movimiento reducido |
|---|---|---|
| `--awe-space-0` | `0` |  |
| `--awe-space-0-5` | `2px` |  |
| `--awe-space-1` | `4px` |  |
| `--awe-space-1-5` | `6px` |  |
| `--awe-space-2` | `8px` |  |
| `--awe-space-3` | `12px` |  |
| `--awe-space-4` | `16px` |  |
| `--awe-space-5` | `20px` |  |
| `--awe-space-6` | `24px` |  |
| `--awe-space-8` | `32px` |  |
| `--awe-space-10` | `40px` |  |
| `--awe-space-12` | `48px` |  |
| `--awe-space-16` | `64px` |  |
| `--awe-space-2xs` | `var(--awe-space-0-5)` |  |
| `--awe-space-xs` | `var(--awe-space-1)` |  |
| `--awe-space-sm` | `var(--awe-space-2)` |  |
| `--awe-space-md` | `var(--awe-space-3)` |  |
| `--awe-space-lg` | `var(--awe-space-4)` |  |
| `--awe-space-xl` | `var(--awe-space-6)` |  |
| `--awe-space-2xl` | `var(--awe-space-8)` |  |
| `--awe-radius-none` | `0` |  |
| `--awe-radius-xs` | `2px` |  |
| `--awe-radius-sm` | `4px` |  |
| `--awe-radius-md` | `6px` |  |
| `--awe-radius-lg` | `8px` |  |
| `--awe-radius-xl` | `12px` |  |
| `--awe-radius-full` | `9999px` |  |
| `--awe-radius` | `var(--awe-radius-md)` |  |
| `--awe-radius-field` | `var(--awe-radius-md)` |  |
| `--awe-radius-content` | `var(--awe-radius-md)` |  |
| `--awe-radius-card` | `var(--awe-radius-xl)` |  |
| `--awe-radius-overlay` | `var(--awe-radius-md)` |  |
| `--awe-radius-modal` | `var(--awe-radius-xl)` |  |
| `--awe-radius-list-option` | `var(--awe-radius-sm)` |  |
| `--awe-shadow-xs` | `0 1px 2px 0 var(--awe-color-shadow-soft)` |  |
| `--awe-shadow-sm` | `0 1px 3px 0 var(--awe-color-shadow), 0 1px 2px -1px var(--awe-color-shadow)` |  |
| `--awe-shadow-md` | `0 4px 6px -1px var(--awe-color-shadow), 0 2px 4px -2px var(--awe-color-shadow)` |  |
| `--awe-shadow-lg` | `0 20px 25px -5px var(--awe-color-shadow), 0 8px 10px -6px var(--awe-color-shadow)` |  |
| `--awe-shadow-field` | `var(--awe-shadow-xs)` |  |
| `--awe-shadow-card` | `var(--awe-shadow-xs)` |  |
| `--awe-shadow-overlay` | `var(--awe-shadow-md)` |  |
| `--awe-shadow-modal` | `var(--awe-shadow-lg)` |  |
| `--awe-color-focus-ring` | `var(--awe-color-primary-text)` |  |
| `--awe-focus-ring-width` | `2px` |  |
| `--awe-focus-ring-style` | `solid` |  |
| `--awe-focus-ring-offset` | `2px` |  |
| `--awe-focus-ring` | `var(--awe-focus-ring-width) var(--awe-focus-ring-style) var(--awe-color-focus-ring)` |  |
| `--awe-shadow-field-focus` | `0 0 0 3px var(--awe-color-field-focus-halo)` |  |
| `--awe-shadow-field-invalid` | `0 0 0 3px var(--awe-color-field-invalid-halo)` |  |
| `--awe-motion-duration-fast` | `120ms` | `0ms` |
| `--awe-motion-duration-base` | `200ms` | `0ms` |
| `--awe-motion-duration-slow` | `300ms` | `0ms` |
| `--awe-motion-easing-standard` | `cubic-bezier(0.2, 0, 0, 1)` |  |
| `--awe-motion-easing-enter` | `cubic-bezier(0, 0, 0.2, 1)` |  |
| `--awe-motion-easing-exit` | `cubic-bezier(0.4, 0, 1, 1)` |  |
| `--awe-motion-duration-expand` | `240ms` | `0ms` |
| `--awe-motion-easing-expand` | `cubic-bezier(0.22, 1, 0.36, 1)` |  |
| `--awe-z-header` | `100` |  |
| `--awe-z-menu` | `110` |  |
| `--awe-z-dropdown` | `1000` |  |
| `--awe-z-modal` | `1100` |  |
| `--awe-z-toast` | `1200` |  |
| `--awe-z-tooltip` | `1300` |  |
| `--awe-opacity-disabled` | `0.6` |  |

**Densidad**

| Token | Cómoda | Compacta |
|---|---|---|
| `--awe-size-control` | `36px` | `30px` |
| `--awe-size-control-sm` | `30px` | `26px` |
| `--awe-size-control-lg` | `44px` | `36px` |
| `--awe-size-icon` | `16px` | `14px` |
| `--awe-size-icon-sm` | `14px` | `12px` |
| `--awe-size-icon-lg` | `20px` | `18px` |
| `--awe-size-checkbox` | `18px` | `16px` |
| `--awe-size-header-height` | `60px` | `52px` |
| `--awe-size-menu-width` | `264px` | `240px` |
| `--awe-field-padding-x` | `12px` | `9px` |
| `--awe-field-padding-y` | `8px` | `5px` |
| `--awe-button-padding-x` | `14px` | `10px` |
| `--awe-gap-label` | `6px` | `4px` |
| `--awe-gap-inline` | `8px` | `6px` |
| `--awe-gap-form-row` | `18px` | `12px` |
| `--awe-gap-form-column` | `20px` | `14px` |
| `--awe-content-padding` | `20px` | `14px` |
| `--awe-layout-gap` | `20px` | `12px` |
| `--awe-page-padding` | `28px` | `16px` |
| `--awe-list-padding` | `4px` |  |
| `--awe-list-gap` | `2px` |  |
| `--awe-list-option-padding` | `8px 12px` | `6px 10px` |
| `--awe-nav-item-padding` | `8px 12px` | `6px 10px` |
| `--awe-nav-item-gap` | `10px` | `8px` |
| `--awe-overlay-padding` | `12px` | `10px` |
| `--awe-modal-padding` | `20px` | `16px` |

### Nivel 3: componentes {#tier-3-component}

**Grid, menú lateral, cabecera, capas superpuestas, control segmentado**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-grid-header-background` | `var(--awe-color-grid-header)` | = claro |
| `--awe-grid-header-text` | `var(--awe-color-text-muted)` | = claro |
| `--awe-grid-header-font-size` | `var(--awe-font-size-sm)` | = claro |
| `--awe-grid-header-font-weight` | `var(--awe-font-weight-semibold)` | = claro |
| `--awe-grid-border` | `var(--awe-color-grid-border)` | = claro |
| `--awe-grid-row-background` | `var(--awe-color-surface)` | = claro |
| `--awe-grid-row-hover-background` | `var(--awe-color-surface-subtle)` | = claro |
| `--awe-grid-row-selected-background` | `var(--awe-color-highlight)` | = claro |
| `--awe-grid-row-selected-text` | `var(--awe-color-text)` | = claro |
| `--awe-grid-loading-overlay` | `color-mix(in srgb, var(--awe-color-surface) 50%, transparent)` | = claro |
| `--awe-grid-cell-padding-y` | `11px` | = claro |
| `--awe-grid-cell-padding-x` | `14px` | = claro |
| `--awe-grid-row-marker-width` | `3px` | = claro |
| `--awe-grid-drop-indicator-width` | `var(--awe-focus-ring-width)` | = claro |
| `--awe-grid-min-body-height` | `12rem` | = claro |
| `--awe-menu-width` | `var(--awe-size-menu-width)` | = claro |
| `--awe-menu-width-collapsed` | `68px` | = claro |
| `--awe-menu-background` | `var(--awe-color-menu)` | = claro |
| `--awe-menu-border` | `var(--awe-color-border)` | = claro |
| `--awe-menu-item-text` | `var(--awe-color-nav-item-text)` | = claro |
| `--awe-menu-item-icon` | `var(--awe-color-nav-item-icon)` | = claro |
| `--awe-menu-item-hover-background` | `var(--awe-color-nav-item-focus)` | = claro |
| `--awe-menu-item-active-background` | `var(--awe-color-menu-accent)` | = claro |
| `--awe-menu-item-active-text` | `var(--awe-color-on-highlight)` | = claro |
| `--awe-menu-item-active-icon` | `var(--awe-color-primary-text)` | = claro |
| `--awe-menu-item-radius` | `var(--awe-radius-md)` | = claro |
| `--awe-menu-section-label` | `var(--awe-color-nav-submenu-label)` | = claro |
| `--awe-menu-item-height` | `36px` | = claro |
| `--awe-menu-item-sub-text` | `var(--awe-color-text-muted)` | = claro |
| `--awe-menu-indent` | `30px` | = claro |
| `--awe-menu-indent-l3` | `22px` | = claro |
| `--awe-menu-guide-l2` | `20px` | = claro |
| `--awe-menu-guide-l3` | `14px` | = claro |
| `--awe-menu-guide-l3-flyout` | `16px` | = claro |
| `--awe-menu-guide` | `var(--awe-color-border)` | = claro |
| `--awe-menu-guide-active` | `var(--awe-color-primary-text)` | = claro |
| `--awe-menu-flyout-width` | `264px` | = claro |
| `--awe-header-height` | `var(--awe-size-header-height)` | = claro |
| `--awe-header-background` | `color-mix(in srgb, var(--awe-color-navbar), transparent 15%)` | = claro |
| `--awe-header-border` | `var(--awe-color-border)` | = claro |
| `--awe-header-backdrop-filter` | `saturate(1.4) blur(10px)` | = claro |
| `--awe-header-search-width` | `380px` | = claro |
| `--awe-header-logo-height` | `34px` | = claro |
| `--awe-mask-backdrop-filter` | `blur(2px)` | = claro |
| `--awe-toast-backdrop-filter` | `blur(8px)` | = claro |
| `--awe-segmented-track` | `var(--awe-surface-100)` | `var(--awe-surface-950)` |
| `--awe-segmented-border` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-segmented-text` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |
| `--awe-segmented-hover-text` | `var(--awe-color-text-emphasis)` | = claro |
| `--awe-segmented-selected` | `var(--awe-surface-0)` | `var(--awe-surface-700)` |
| `--awe-segmented-selected-border` | `var(--awe-surface-500)` | = claro |
| `--awe-segmented-selected-text` | `var(--awe-surface-900)` | `var(--awe-surface-0)` |
| `--awe-checkbox-icon-size` | `12px` | = claro |
| `--awe-radio-dot-size` | `8px` | = claro |
| `--awe-switch-width` | `36px` | = claro |
| `--awe-switch-height` | `20px` | = claro |
| `--awe-switch-handle-size` | `16px` | = claro |

**Página de login**

| Token | Cómoda | Compacta |
|---|---|---|
| `--awe-login-main-min` | `440px` |  |
| `--awe-login-form-width` | `420px` | `380px` |
| `--awe-login-padding-y` | `var(--awe-space-10)` | `var(--awe-space-8)` |
| `--awe-login-padding-x` | `var(--awe-space-12)` | `var(--awe-space-8)` |
| `--awe-login-logo-height` | `40px` | `34px` |
| `--awe-login-background` | `var(--awe-color-surface)` |  |
| `--awe-login-image` | `none` |  |
| `--awe-login-scrim` | `linear-gradient(to top, color-mix(in srgb, var(--awe-palette-slate-950) 86%, transparent) 0%, color-mix(in srgb, var(--awe-palette-slate-950) 62%, transparent) 22%, color-mix(in srgb, var(--awe-palette-slate-950) 0%, transparent) 50%)` |  |
| `--awe-login-caption-text` | `#ffffff` |  |
| `--awe-login-caption-muted` | `color-mix(in srgb, #ffffff 88%, transparent)` |  |
| `--awe-login-gradient` | `radial-gradient(110% 70% at 88% 8%, light-dark(var(--awe-primary-300), var(--awe-primary-600)) 0%, transparent 58%), radial-gradient(90% 60% at 8% 92%, light-dark(var(--awe-primary-800), var(--awe-primary-950)) 0%, transparent 62%), linear-gradient(160deg, light-dark(var(--awe-primary-700), var(--awe-primary-900)) 0%, light-dark(var(--awe-primary-500), var(--awe-primary-700)) 100%)` |  |
| `--awe-login-brand-background` | `radial-gradient(75% 55% at 88% 6%, color-mix(in srgb, var(--awe-primary-500) 42%, transparent) 0%, transparent 70%), radial-gradient(70% 50% at 0% 100%, color-mix(in srgb, var(--awe-primary-700) 45%, transparent) 0%, transparent 72%), linear-gradient(160deg, var(--awe-surface-900) 0%, var(--awe-surface-950) 100%)` |  |
| `--awe-login-brand-logo-width` | `340px` | `280px` |
| `--awe-login-card-width` | `480px` | `420px` |
| `--awe-login-card-padding` | `var(--awe-space-10)` | `var(--awe-space-8)` |
| `--awe-login-card-background` | `var(--awe-color-surface)` |  |
| `--awe-login-card-radius` | `var(--awe-radius-modal)` |  |
| `--awe-login-card-shadow` | `var(--awe-shadow-modal)` |  |
| `--awe-login-backdrop-veil` | `color-mix(in srgb, var(--awe-palette-slate-950) 35%, transparent)` |  |
| `--awe-login-microsoft-background` | `var(--awe-color-surface)` |  |
| `--awe-login-microsoft-border` | `var(--awe-color-border)` |  |
| `--awe-login-microsoft-text` | `var(--awe-color-text-emphasis)` |  |
| `--awe-login-microsoft-font-family` | `var(--awe-font-family-base)` |  |
| `--awe-login-microsoft-font-size` | `var(--awe-font-size-md)` |  |
| `--awe-login-microsoft-height` | `var(--awe-size-control-lg)` |  |
| `--awe-login-microsoft-padding` | `var(--awe-space-3)` |  |
| `--awe-login-microsoft-radius` | `var(--awe-radius-field)` |  |

### Solo del prototipo {#prototype-only}

**Logo de cliente de ejemplo**

| Token | Claro | Oscuro |
|---|---|---|
| `--awe-customer-logo-primary` | `var(--awe-palette-blue-700)` | `var(--awe-palette-sky-400)` |
| `--awe-customer-logo-on-primary` | `#ffffff` | `var(--awe-palette-slate-950)` |
| `--awe-customer-logo-accent` | `var(--awe-palette-orange-500)` | `var(--awe-palette-orange-400)` |
| `--awe-customer-logo-ink` | `var(--awe-palette-slate-900)` | `var(--awe-palette-slate-50)` |
| `--awe-customer-logo-muted` | `var(--awe-palette-slate-500)` | `var(--awe-palette-slate-400)` |


## Fichero de tokens de los motores {#token-file-of-the-engines}

Los dos motores cargan un único fichero, `awe-framework/awe-design-tokens/tokens.css`: el motor AngularJS lo incluye
en su hoja de estilos (`less/awe.less`) y el motor React lo importa desde `src/main.css`. Contiene todos los tokens
del prototipo salvo los exclusivos del prototipo, con los mismos valores; las pruebas de tokens de los dos motores lo
comprueban. Deja fuera la fuente Inter y las reglas del modo de color (`color-scheme`). La fuente tiene su propio fichero
compartido, `awe-framework/awe-design-tokens/fonts/inter.css`, junto a la fuente y su licencia SIL Open Font License;
el motor React lo importa desde `src/main.css` y publica la licencia con la fuente (`fonts/` del paquete). Cada motor
fija el modo (`color-scheme: light dark`, o `light` / `dark` con la clase de `<html>`): el motor React en
`src/main.css` y el motor AngularJS en `less/awe/tokens.less`. El motor AngularJS mantiene su fuente actual hasta que
cargue Inter.

Ningún motor declara tokens propios. El motor AngularJS no tiene temas clásicos, ni `/css/themeVariables.css`, ni
puente de nombres: sus variables LESS (`@primary-color`...) leen los tokens, así que sus reglas de Bootstrap 3 y
pixel-admin siguen a los atributos de `<html>`. Los componentes de PrimeReact del motor React leen los tokens a través
del tema AWE de PrimeReact.

La página de error del servidor (`templates/error.html` de `awe-generic-screens`), común a los dos motores, enlaza una
copia del mismo fichero que `awe-generic-screens` publica como `css/awe-design-tokens/tokens.css`. La página no tiene
atributos en `<html>`: fija `color-scheme: light dark`, así que sigue el modo de color del sistema con las paletas por
defecto.

### Tema AWE de PrimeReact {#primereact-theme}

El motor React carga un único tema para PrimeReact 10, generado a partir de los tokens en cada compilación; sustituye a
`saga-blue` y a los 18 temas compilados, y el nombre del tema solo elige el preset de los atributos de `<html>`.
`awe-framework/awe-design-tokens/primereact/primereact-theme.cjs` lee el tema Lara claro del `primereact` fijado
(10.9.9, MIT) solo por sus selectores y sustituye cada color, tamaño y peso de letra, espaciado, tamaño, radio, sombra,
anillo de foco y transición por un token `--awe-*`, mediante la tabla de equivalencias `primereact-theme.json` que está
a su lado. Los valores siguen el preset Aura de `@primeuix/themes` 2.0.3 (MIT, solo como valores de referencia), como
hace el prototipo. Un valor de Lara que la tabla no traduce, o un token que el fichero de tokens no declara, hace fallar
la compilación, de modo que una actualización de PrimeReact no puede traer su propio aspecto. `primeReactThemeTest.js`
comprueba el resultado: ningún color ni medida literal, solo tokens del fichero de tokens y las reglas de cada
componente de PrimeReact que usa el motor.

## Correspondencia con los tokens actuales {#mapping-from-the-current-tokens}

Qué ocurre con los tokens y las variables de tema de AWE 4 y de la rama actual de AWE 5. Los tokens actuales están
documentados en [Design tokens](../guides/design-tokens-guide.md).

### Tokens actuales del motor AngularJS {#mapping-angular}

Los 56 tokens que `tokens.less` declaraba hasta que el motor AngularJS adoptó los valores de AWE 5. **Ninguno se
elimina ni cambia de nombre**; algunos cambian de valor o de significado.

| Token | AWE 5 | Nota |
|---|---|---|
| `--awe-color-theme` | Se mantiene, significado nuevo | Alias de `--awe-color-primary`. |
| `--awe-color-text` | Se mantiene |  |
| `--awe-color-primary` | Se mantiene |  |
| `--awe-color-on-primary` | Se mantiene |  |
| `--awe-color-secondary` | Se mantiene |  |
| `--awe-color-on-secondary` | Se mantiene |  |
| `--awe-color-background` | Se mantiene |  |
| `--awe-color-surface` | Se mantiene, significado nuevo | Superficie de tarjetas y contenido. El significado anterior (pies de panel, zonas de campos) es `--awe-color-surface-subtle`. |
| `--awe-color-border` | Se mantiene |  |
| `--awe-color-shadow` | Se mantiene |  |
| `--awe-color-menu` | Se mantiene, valor nuevo | El menú es una superficie clara (`--awe-color-surface`). |
| `--awe-color-menu-accent` | Se mantiene, valor nuevo | Resaltado de la opción activa. |
| `--awe-color-menu-text` | Se mantiene |  |
| `--awe-color-navbar` | Se mantiene, valor nuevo | La cabecera es una superficie clara. |
| `--awe-color-navbar-accent` | Se mantiene, valor nuevo | Superficie de hover de la cabecera. |
| `--awe-color-navbar-text` | Se mantiene |  |
| `--awe-color-navbar-dropdown` | Se mantiene, valor nuevo | Superficie de capa (`--awe-color-overlay`). |
| `--awe-color-panel` | Se mantiene |  |
| `--awe-color-panel-header` | Se mantiene, valor nuevo | Las cabeceras de tarjeta no tienen color: superficie de la tarjeta. |
| `--awe-color-panel-border` | Se mantiene |  |
| `--awe-color-panel-text` | Se mantiene |  |
| `--awe-color-grid-header` | Se mantiene, valor nuevo | Superficie sutil (`--awe-color-surface-subtle`). |
| `--awe-color-grid-border` | Se mantiene |  |
| `--awe-color-skeleton` | Se mantiene |  |
| `--awe-color-skeleton-highlight` | Se mantiene |  |
| `--awe-color-disabled` | Se mantiene |  |
| `--awe-color-disabled-border` | Se mantiene |  |
| `--awe-color-disabled-text` | Se mantiene |  |
| `--awe-color-required` | Se mantiene, significado nuevo | Color de la marca de obligatorio (`*`), ya no es un fondo de campo. |
| `--awe-color-loading-bar` | Se mantiene, valor nuevo | El primario. |
| `--awe-color-danger` | Se mantiene |  |
| `--awe-color-warning` | Se mantiene |  |
| `--awe-color-success` | Se mantiene |  |
| `--awe-color-info` | Se mantiene |  |
| `--awe-font-family-base` | Se mantiene, valor nuevo | Inter. |
| `--awe-font-family-monospace` | Se mantiene |  |
| `--awe-font-size-base` | Se mantiene, valor nuevo | 14px; 13px en compacta. |
| `--awe-font-size-small` | Se mantiene, valor nuevo | Alias de `--awe-font-size-sm` (13px; 12px en compacta). |
| `--awe-line-height-base` | Se mantiene |  |
| `--awe-font-weight-normal` | Se mantiene |  |
| `--awe-font-weight-bold` | Se mantiene |  |
| `--awe-space-2xs` | Se mantiene |  |
| `--awe-space-xs` | Se mantiene, valor nuevo | 4px (antes 5px). |
| `--awe-space-sm` | Se mantiene, valor nuevo | 8px (antes 10px). |
| `--awe-space-md` | Se mantiene, valor nuevo | 12px (antes 15px). |
| `--awe-space-lg` | Se mantiene, valor nuevo | 16px (antes 20px). |
| `--awe-space-xl` | Se mantiene, valor nuevo | 24px (antes 30px). |
| `--awe-radius` | Se mantiene, valor nuevo | 6px, alias de `--awe-radius-md`. |
| `--awe-radius-sm` | Se mantiene, valor nuevo | 4px fijo (antes la mitad del radio). |
| `--awe-radius-lg` | Se mantiene, valor nuevo | 8px fijo (antes el doble del radio). |
| `--awe-radius-full` | Se mantiene |  |
| `--awe-shadow-sm` | Se mantiene, valor nuevo | Nueva escala de sombras. |
| `--awe-shadow-md` | Se mantiene, valor nuevo | Nueva escala de sombras. |
| `--awe-shadow-lg` | Se mantiene, valor nuevo | Nueva escala de sombras. |
| `--awe-size-header-height` | Se mantiene, valor nuevo | 60px; 52px en compacta. |
| `--awe-size-menu-width` | Se mantiene, valor nuevo | 264px; 240px en compacta. |

### Tokens actuales del motor React {#mapping-react}

Los 15 tokens que declaraba `awe-client-react/src/main.css` leían variables de PrimeReact. Ahora solo los declara el fichero de tokens común y el [tema AWE de PrimeReact](#primereact-theme) lee los tokens (al revés que antes).

| Token | Leía | AWE 5 |
|---|---|---|
| `--awe-color-primary` | `--primary-color` | Se mantiene |
| `--awe-color-on-primary` | `--primary-color-text` | Se mantiene |
| `--awe-color-text` | `--text-color` | Se mantiene |
| `--awe-color-background` | `--surface-ground` | Se mantiene |
| `--awe-color-surface` | `--surface-card` | Se mantiene |
| `--awe-color-panel` | `--surface-card` | Se mantiene |
| `--awe-color-border` | `--surface-border` | Se mantiene |
| `--awe-color-danger` | `--danger-color` | Se mantiene |
| `--awe-color-warning` | `--warning-color` | Se mantiene |
| `--awe-color-success` | `--success-color` | Se mantiene |
| `--awe-color-info` | `--info-color` | Se mantiene |
| `--awe-font-family-base` | `--font-family` | Se mantiene |
| `--awe-radius` | `--border-radius` | Se mantiene |
| `--awe-color-skeleton` | `--surface-c` | Se mantiene |
| `--awe-color-skeleton-highlight` | `--surface-b` | Se mantiene |

### Variables de tema y temas de base de datos {#mapping-themes}

Las variables de tema de AngularJS (temas LESS) y los campos de color de los temas de base de datos (tabla `AweThmCol`, servidos por AWE 4 en `/css/themeVariables.css`, eliminado en AWE 5) tienen los mismos nombres. En AWE 5 **ninguno se configura**: un tema es primario + superficie y el resto se deriva.

| Campo de `AweThmCol` | Variable de tema | AWE 5 | Estado |
|---|---|---|---|
| `themeColor` | `--theme-color` | `--awe-color-theme` → `--awe-color-primary` | Eliminado: derivado |
| `textColor` | `--text-color` | `--awe-color-text` | Eliminado: derivado |
| `primaryColor` | `--primary-color` | `data-awe-primary` | Se mantiene como elección de paleta |
| `secondaryColor` | `--secondary-color` | `--awe-color-secondary` | Eliminado: derivado |
| `primaryTextColor` | `--primary-text-color` | `--awe-color-on-primary` (`--awe-primary-on-fill`) | Eliminado: derivado |
| `secondaryTextColor` | `--secondary-text-color` | `--awe-color-on-secondary` | Eliminado: derivado |
| `primaryBackgroundColor` | `--primary-background-color` | `--awe-color-background` | Eliminado: derivado |
| `secondaryBackgroundColor` | `--secondary-background-color` | `--awe-color-surface-subtle` | Eliminado: derivado |
| `primaryMenuColor` | `--primary-menu-color` | `--awe-color-menu` | Eliminado: derivado |
| `secondaryMenuColor` | `--secondary-menu-color` | `--awe-color-menu-accent` | Eliminado: derivado |
| `menuTextColor` | `--menu-text-color` | `--awe-color-menu-text` | Eliminado: derivado |
| `primaryNavbarColor` | `--primary-navbar-color` | `--awe-color-navbar` | Eliminado: derivado |
| `secondaryNavbarColor` | `--secondary-navbar-color` | `--awe-color-navbar-accent` | Eliminado: derivado |
| `navbarDropdownColor` | `--navbar-dropdown-color` | `--awe-color-navbar-dropdown` | Eliminado: derivado |
| `navbarTextColor` | `--navbar-text-color` | `--awe-color-navbar-text` | Eliminado: derivado |
| `primaryFontSize` | `--primary-font-size` | `--awe-font-size-base` (densidad) | Eliminado: derivado |
| `secondaryFontSize` | `--secondary-font-size` | `--awe-font-size-sm` (densidad) | Eliminado: derivado |
| `disabledColor` | `--disabled-color` | `--awe-color-disabled` | Eliminado: derivado |
| `disabledBorderColor` | `--disabled-border-color` | `--awe-color-disabled-border` | Eliminado: derivado |
| `disabledTextColor` | `--disabled-text-color` | `--awe-color-disabled-text` | Eliminado: derivado |
| `headerHeight` | `--header-height` | `--awe-size-header-height` (densidad) | Eliminado: derivado |
| `panelHeaderColor` | `--panel-header-color` | `--awe-color-panel-header` | Eliminado: derivado |
| `panelBorderColor` | `--panel-border-color` | `--awe-color-panel-border` | Eliminado: derivado |
| `panelBackgroundColor` | `--panel-background-color` | `--awe-color-panel` | Eliminado: derivado |
| `panelTextColor` | `--panel-text-color` | `--awe-color-panel-text` | Eliminado: derivado |
| `gridHeaderColor` | `--grid-header-color` | `--awe-color-grid-header` | Eliminado: derivado |
| `gridBorderColor` | `--grid-border-color` | `--awe-color-grid-border` | Eliminado: derivado |
| `dangerColor` | `--danger-color` | `--awe-color-danger*` | Eliminado: derivado |
| `warningColor` | `--warning-color` | `--awe-color-warning*` | Eliminado: derivado |
| `successColor` | `--success-color` | `--awe-color-success*` | Eliminado: derivado |
| `infoColor` | `--info-color` | `--awe-color-info*` | Eliminado: derivado |
| `borderColor` | `--border-color` | `--awe-color-border` | Eliminado: derivado |
| `borderRadius` | `--border-radius` | `--awe-radius-*` (escala fija) | Eliminado: derivado |
| `loadingBarColor` | `--loading-bar-color` | `--awe-color-loading-bar` | Eliminado: derivado |
| `requiredColor` | `--required-color` | `--awe-color-required` | Eliminado: derivado |
| `dark` |  | `light-dark()` en cada token | Sustituido por el modo |

Nuevo en el tema de AWE 5: paleta de superficie (`data-awe-surface`) y, opcionalmente, modo y densidad por defecto.

La migración de Flyway `AWE_V1.5.0` (en las 6 bases de datos) añade las columnas `Pri` (primario), `Srf` (superficie) y `Mde` (modo) a `AweThm` y `HISAweThm`, y `density` a `AweUsrSet`. Después rellena los temas:

1. Los nombres de tema clásicos toman su [preset](color-and-themes.md#presets-of-the-classic-themes).
2. Un tema con un juego de colores claro en `AweThmCol` toma su `primaryColor` cuando es un hex (`#RGB` o `#RRGGBB`).
3. Cada valor que sigue vacío toma el valor por defecto: `emerald`, `auto` y `system`.

No se borra nada: `AweThmCol` se conserva tal cual, sin uso, y se elimina en una versión posterior. La densidad del usuario queda vacía (la densidad de la aplicación) hasta que el usuario elige una.

El cliente React ya no carga las hojas `theme.css` compiladas de PrimeReact (`saga-blue` y los 18 temas clásicos): incluye **un** [tema AWE de PrimeReact](#primereact-theme) que lee los tokens.
