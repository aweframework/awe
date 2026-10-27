---
id: tokens-reference
title: Token reference
sidebar_label: Token reference
---

Every token of the AWE 5 proposal, generated from the prototype's
[`tokens.css`](pathname:///design-system/prototype/tokens.css), and the mapping from today's tokens. To see them
rendered in both modes, open the [tokens page](pathname:///design-system/prototype/tokens.html) of the prototype.

| Tier | Tokens |
|------|--------|
| 1 · Primitive | 242 |
| 1 · Configurable | 27 |
| 2 · Semantic | 257 |
| 3 · Component | 58 |
| Prototype only | 5 |
| **Total** | **589** |

How to read the tables:

- **Light / Dark**: the two arguments of `light-dark()`. "= light" means the token does not depend on the mode.
- **Comfortable / Compact**: the value per density. An empty Compact cell means the same value as Comfortable.
- `var(--awe-primary-fill)` and the other configurable tokens depend on `data-awe-primary` and `data-awe-surface`.

## Tokens {#tokens}

### Tier 1: primitive palettes {#tier-1-primitive}

Name: `--awe-palette-<palette>-<shade>`. 22 palettes × 11 shades = 242 tokens.

| Palette | 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 950 |
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

### Tier 1: configurable palettes {#tier-1-configurable}

The values below are those of the named palettes. When a theme gives a hex, the same tokens hold the generated values
(see [Palettes generated from a hex](color-and-themes.md#palettes-generated-from-a-hex)).

| Token | Default value (emerald) |
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

The primary is chosen with `data-awe-primary` on `<html>`. Fill shades per palette (light mode):

| Palette | Fill | Hover | Active | Text on fill |
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

Surface: `data-awe-surface` on `<html>` (`slate`, `gray`, `zinc`, `neutral`, `stone`). Without the attribute:

| Token | Light | Dark |
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

### Tier 2: semantic {#tier-2-semantic}

**Colour · Brand**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-primary` | `var(--awe-primary-fill)` | `var(--awe-primary-400)` |
| `--awe-color-primary-hover` | `var(--awe-primary-fill-hover)` | `var(--awe-primary-300)` |
| `--awe-color-primary-active` | `var(--awe-primary-fill-active)` | `var(--awe-primary-200)` |
| `--awe-color-on-primary` | `var(--awe-primary-on-fill)` | `var(--awe-surface-900)` |
| `--awe-color-primary-text` | `var(--awe-primary-700)` | `var(--awe-primary-400)` |
| `--awe-color-primary-subtle` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-500), transparent 84%)` |
| `--awe-color-on-primary-subtle` | `var(--awe-primary-700)` | `var(--awe-primary-300)` |
| `--awe-color-theme` | `var(--awe-color-primary)` | = light |
| `--awe-color-secondary` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-secondary-hover` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-on-secondary` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Colour · Text**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-text` | `var(--awe-surface-700)` | `var(--awe-surface-0)` |
| `--awe-color-text-emphasis` | `var(--awe-surface-900)` | `var(--awe-surface-0)` |
| `--awe-color-text-muted` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-text-hover` | `var(--awe-surface-800)` | `var(--awe-surface-0)` |
| `--awe-color-text-muted-hover` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Colour · Page and content**

| Token | Light | Dark |
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
| `--awe-color-content` | `var(--awe-color-surface)` | = light |
| `--awe-color-content-hover` | `var(--awe-color-surface-hover)` | = light |
| `--awe-color-content-border` | `var(--awe-color-border)` | = light |
| `--awe-color-mask` | `rgb(0 0 0 / 0.4)` | `rgb(0 0 0 / 0.6)` |

**Colour · Highlight (selected option, selected row, active menu option)**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-highlight` | `var(--awe-primary-50)` | `color-mix(in srgb, var(--awe-primary-400), transparent 84%)` |
| `--awe-color-highlight-focus` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400), transparent 76%)` |
| `--awe-color-on-highlight` | `var(--awe-primary-700)` | `rgb(255 255 255 / 0.87)` |
| `--awe-color-on-highlight-focus` | `var(--awe-primary-800)` | `rgb(255 255 255 / 0.87)` |

**Colour · Main menu, navbar, panels and grids (existing AWE vocabulary)**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-menu` | `var(--awe-color-surface)` | = light |
| `--awe-color-menu-accent` | `var(--awe-color-highlight)` | = light |
| `--awe-color-menu-text` | `var(--awe-color-text)` | = light |
| `--awe-color-navbar` | `var(--awe-color-surface)` | = light |
| `--awe-color-navbar-accent` | `var(--awe-color-surface-hover)` | = light |
| `--awe-color-navbar-text` | `var(--awe-color-text)` | = light |
| `--awe-color-navbar-dropdown` | `var(--awe-color-overlay)` | = light |
| `--awe-color-panel` | `var(--awe-color-surface)` | = light |
| `--awe-color-panel-header` | `var(--awe-color-surface)` | = light |
| `--awe-color-panel-border` | `var(--awe-color-border)` | = light |
| `--awe-color-panel-text` | `var(--awe-color-text)` | = light |
| `--awe-color-grid-header` | `var(--awe-color-surface-subtle)` | = light |
| `--awe-color-grid-border` | `var(--awe-color-border)` | = light |
| `--awe-color-skeleton` | `var(--awe-surface-200)` | `var(--awe-surface-800)` |
| `--awe-color-skeleton-highlight` | `var(--awe-surface-100)` | `var(--awe-surface-700)` |

**Colour · Overlays (select panels, popovers, menus, dialogs)**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-overlay` | `var(--awe-surface-0)` | `var(--awe-surface-900)` |
| `--awe-color-overlay-border` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-overlay-text` | `var(--awe-color-text)` | = light |
| `--awe-color-tooltip` | `var(--awe-surface-800)` | `var(--awe-surface-700)` |
| `--awe-color-on-tooltip` | `var(--awe-surface-0)` | = light |

**Colour · Form fields**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-field` | `var(--awe-surface-0)` | `var(--awe-surface-950)` |
| `--awe-color-field-filled` | `var(--awe-surface-50)` | `var(--awe-surface-800)` |
| `--awe-color-field-border` | `var(--awe-surface-300)` | `var(--awe-surface-600)` |
| `--awe-color-field-border-hover` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-field-border-focus` | `var(--awe-color-primary)` | = light |
| `--awe-color-field-border-invalid` | `var(--awe-palette-red-500)` | `var(--awe-palette-red-300)` |
| `--awe-color-field-text` | `var(--awe-surface-700)` | `var(--awe-surface-0)` |
| `--awe-color-field-placeholder` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-field-placeholder-invalid` | `var(--awe-palette-red-600)` | `var(--awe-palette-red-400)` |
| `--awe-color-field-icon` | `var(--awe-surface-400)` | = light |
| `--awe-color-field-label` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-color-field-help` | `var(--awe-color-text-muted)` | = light |
| `--awe-color-field-focus-halo` | `color-mix(in srgb, var(--awe-color-primary), transparent 80%)` | = light |
| `--awe-color-field-invalid-halo` | `color-mix(in srgb, var(--awe-color-danger), transparent 82%)` | = light |

**Colour · List options (select, autocomplete, menus)**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-list-option-focus` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-list-option-selected` | `var(--awe-color-highlight)` | = light |
| `--awe-color-list-option-selected-focus` | `var(--awe-color-highlight-focus)` | = light |
| `--awe-color-list-option-text` | `var(--awe-color-text)` | = light |
| `--awe-color-list-option-text-focus` | `var(--awe-color-text-hover)` | = light |
| `--awe-color-list-option-text-selected` | `var(--awe-color-on-highlight)` | = light |
| `--awe-color-list-option-icon` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-list-option-group` | `var(--awe-color-text-muted)` | = light |

**Colour · Navigation items (side menu, breadcrumb, tabs, user menu)**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-nav-item-focus` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-nav-item-active` | `var(--awe-surface-100)` | `var(--awe-surface-800)` |
| `--awe-color-nav-item-text` | `var(--awe-color-text)` | = light |
| `--awe-color-nav-item-text-active` | `var(--awe-color-text-hover)` | = light |
| `--awe-color-nav-item-icon` | `var(--awe-surface-400)` | `var(--awe-surface-500)` |
| `--awe-color-nav-item-icon-active` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-nav-submenu-label` | `var(--awe-color-text-muted)` | = light |

**Colour · States**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-disabled` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-color-disabled-border` | `var(--awe-surface-300)` | `var(--awe-surface-600)` |
| `--awe-color-disabled-text` | `var(--awe-surface-500)` | `var(--awe-surface-400)` |
| `--awe-color-required` | `var(--awe-palette-red-600)` | `var(--awe-palette-red-400)` |
| `--awe-color-loading-bar` | `var(--awe-color-primary)` | = light |
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
| `--awe-color-neutral-subtle` | `var(--awe-color-secondary)` | = light |
| `--awe-color-neutral-text` | `var(--awe-color-on-secondary)` | = light |
| `--awe-color-contrast` | `var(--awe-surface-950)` | `var(--awe-surface-0)` |
| `--awe-color-on-contrast` | `var(--awe-surface-0)` | `var(--awe-surface-950)` |

**Data visualisation**

| Token | Light | Dark |
|---|---|---|
| `--awe-color-scale-1` | `var(--awe-primary-50)` | `color-mix(in srgb, var(--awe-primary-400) 12%, var(--awe-surface-900))` |
| `--awe-color-scale-2` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400) 22%, var(--awe-surface-900))` |
| `--awe-color-scale-3` | `var(--awe-primary-200)` | `color-mix(in srgb, var(--awe-primary-400) 34%, var(--awe-surface-900))` |
| `--awe-color-scale-4` | `var(--awe-primary-300)` | `color-mix(in srgb, var(--awe-primary-400) 48%, var(--awe-surface-900))` |
| `--awe-color-scale-5` | `var(--awe-color-primary)` | = light |
| `--awe-color-on-scale-1` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-color-on-scale-2` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-color-on-scale-3` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-color-on-scale-4` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-color-on-scale-5` | `var(--awe-color-on-primary)` | = light |
| `--awe-color-data-bar` | `var(--awe-primary-100)` | `color-mix(in srgb, var(--awe-primary-400) 35%, transparent)` |
| `--awe-color-data-bar-negative` | `var(--awe-palette-red-100)` | `color-mix(in srgb, var(--awe-palette-red-400) 35%, transparent)` |
| `--awe-color-chart-line` | `var(--awe-color-primary-text)` | = light |
| `--awe-color-chart-area` | `color-mix(in srgb, var(--awe-color-primary-text) 18%, transparent)` | = light |
| `--awe-color-chart-negative` | `var(--awe-color-danger)` | = light |
| `--awe-color-chart-axis` | `var(--awe-color-border)` | = light |
| `--awe-color-map-water` | `var(--awe-palette-sky-100)` | `color-mix(in srgb, var(--awe-palette-sky-500) 16%, var(--awe-surface-950))` |
| `--awe-color-map-land` | `var(--awe-surface-50)` | `var(--awe-surface-900)` |
| `--awe-color-map-park` | `var(--awe-palette-green-100)` | `color-mix(in srgb, var(--awe-palette-green-500) 14%, var(--awe-surface-900))` |
| `--awe-color-map-road` | `var(--awe-surface-0)` | `var(--awe-surface-700)` |
| `--awe-color-map-road-casing` | `var(--awe-surface-300)` | `var(--awe-surface-800)` |
| `--awe-color-map-border` | `var(--awe-surface-400)` | `var(--awe-surface-600)` |
| `--awe-color-map-label` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |

**Typography**

| Token | Comfortable | Compact |
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

**Spacing, radius, elevation, focus, motion, layers**

| Token | Value | Reduced motion |
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

**Density**

| Token | Comfortable | Compact |
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

### Tier 3: component {#tier-3-component}

**Grid, side menu, header, overlays, segmented control**

| Token | Light | Dark |
|---|---|---|
| `--awe-grid-header-background` | `var(--awe-color-grid-header)` | = light |
| `--awe-grid-header-text` | `var(--awe-color-text-muted)` | = light |
| `--awe-grid-header-font-size` | `var(--awe-font-size-sm)` | = light |
| `--awe-grid-header-font-weight` | `var(--awe-font-weight-semibold)` | = light |
| `--awe-grid-border` | `var(--awe-color-grid-border)` | = light |
| `--awe-grid-row-background` | `var(--awe-color-surface)` | = light |
| `--awe-grid-row-hover-background` | `var(--awe-color-surface-subtle)` | = light |
| `--awe-grid-row-selected-background` | `var(--awe-color-highlight)` | = light |
| `--awe-grid-row-selected-text` | `var(--awe-color-text)` | = light |
| `--awe-grid-loading-overlay` | `color-mix(in srgb, var(--awe-color-surface) 50%, transparent)` | = light |
| `--awe-grid-cell-padding-y` | `11px` | = light |
| `--awe-grid-cell-padding-x` | `14px` | = light |
| `--awe-grid-row-marker-width` | `3px` | = light |
| `--awe-grid-min-body-height` | `12rem` | = light |
| `--awe-menu-width` | `var(--awe-size-menu-width)` | = light |
| `--awe-menu-width-collapsed` | `68px` | = light |
| `--awe-menu-background` | `var(--awe-color-menu)` | = light |
| `--awe-menu-border` | `var(--awe-color-border)` | = light |
| `--awe-menu-item-text` | `var(--awe-color-nav-item-text)` | = light |
| `--awe-menu-item-icon` | `var(--awe-color-nav-item-icon)` | = light |
| `--awe-menu-item-hover-background` | `var(--awe-color-nav-item-focus)` | = light |
| `--awe-menu-item-active-background` | `var(--awe-color-menu-accent)` | = light |
| `--awe-menu-item-active-text` | `var(--awe-color-on-highlight)` | = light |
| `--awe-menu-item-active-icon` | `var(--awe-color-primary-text)` | = light |
| `--awe-menu-item-radius` | `var(--awe-radius-md)` | = light |
| `--awe-menu-section-label` | `var(--awe-color-nav-submenu-label)` | = light |
| `--awe-menu-item-height` | `36px` | = light |
| `--awe-menu-item-sub-text` | `var(--awe-color-text-muted)` | = light |
| `--awe-menu-indent` | `30px` | = light |
| `--awe-menu-indent-l3` | `22px` | = light |
| `--awe-menu-guide-l2` | `20px` | = light |
| `--awe-menu-guide-l3` | `14px` | = light |
| `--awe-menu-guide-l3-flyout` | `16px` | = light |
| `--awe-menu-guide` | `var(--awe-color-border)` | = light |
| `--awe-menu-guide-active` | `var(--awe-color-primary-text)` | = light |
| `--awe-menu-flyout-width` | `264px` | = light |
| `--awe-header-height` | `var(--awe-size-header-height)` | = light |
| `--awe-header-background` | `color-mix(in srgb, var(--awe-color-navbar), transparent 15%)` | = light |
| `--awe-header-border` | `var(--awe-color-border)` | = light |
| `--awe-header-backdrop-filter` | `saturate(1.4) blur(10px)` | = light |
| `--awe-header-search-width` | `380px` | = light |
| `--awe-header-logo-height` | `34px` | = light |
| `--awe-mask-backdrop-filter` | `blur(2px)` | = light |
| `--awe-toast-backdrop-filter` | `blur(8px)` | = light |
| `--awe-segmented-track` | `var(--awe-surface-100)` | `var(--awe-surface-950)` |
| `--awe-segmented-border` | `var(--awe-surface-200)` | `var(--awe-surface-700)` |
| `--awe-segmented-text` | `var(--awe-surface-600)` | `var(--awe-surface-300)` |
| `--awe-segmented-hover-text` | `var(--awe-color-text-emphasis)` | = light |
| `--awe-segmented-selected` | `var(--awe-surface-0)` | `var(--awe-surface-700)` |
| `--awe-segmented-selected-border` | `var(--awe-surface-500)` | = light |
| `--awe-segmented-selected-text` | `var(--awe-surface-900)` | `var(--awe-surface-0)` |
| `--awe-checkbox-icon-size` | `12px` | = light |
| `--awe-radio-dot-size` | `8px` | = light |
| `--awe-switch-width` | `36px` | = light |
| `--awe-switch-height` | `20px` | = light |
| `--awe-switch-handle-size` | `16px` | = light |

**Login page**

| Token | Comfortable | Compact |
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

### Prototype only {#prototype-only}

**Customer logo stand-in**

| Token | Light | Dark |
|---|---|---|
| `--awe-customer-logo-primary` | `var(--awe-palette-blue-700)` | `var(--awe-palette-sky-400)` |
| `--awe-customer-logo-on-primary` | `#ffffff` | `var(--awe-palette-slate-950)` |
| `--awe-customer-logo-accent` | `var(--awe-palette-orange-500)` | `var(--awe-palette-orange-400)` |
| `--awe-customer-logo-ink` | `var(--awe-palette-slate-900)` | `var(--awe-palette-slate-50)` |
| `--awe-customer-logo-muted` | `var(--awe-palette-slate-500)` | `var(--awe-palette-slate-400)` |


## Token file of the engines {#token-file-of-the-engines}

Both engines load one file, `awe-framework/awe-design-tokens/tokens.css`: the AngularJS engine includes it in its
stylesheet (`less/awe.less`) and the React engine imports it from `src/main.css`. It holds every token of the
prototype except the prototype-only ones, with the same values; the token tests of both engines check it. It leaves
out the Inter font face and the colour-mode rules (`color-scheme`). The font face is its own shared file,
`awe-framework/awe-design-tokens/fonts/inter.css`, next to the font and its SIL Open Font License; the React engine
imports it from `src/main.css` and publishes the licence with the font (`fonts/` of the package). Each engine sets
the mode (`color-scheme: light dark`, or `light` / `dark` with the class of `<html>`): the React engine in
`src/main.css`, the AngularJS engine in `less/awe/tokens.less`. The AngularJS engine keeps its current font until it
loads Inter.

Neither engine declares tokens of its own. The AngularJS engine has no classic themes, no `/css/themeVariables.css`
and no bridge of names: its LESS variables (`@primary-color`...) read the tokens, so its Bootstrap 3 and pixel-admin
rules follow the `<html>` attributes. The React engine's PrimeReact components read the tokens through the AWE theme of
PrimeReact.

The server error page (`templates/error.html` of `awe-generic-screens`), shared by both engines, links a copy of the
same file that `awe-generic-screens` publishes as `css/awe-design-tokens/tokens.css`. The page has no attributes on
`<html>`: it sets `color-scheme: light dark`, so it follows the colour mode of the system with the default palettes.

### AWE theme of PrimeReact {#primereact-theme}

The React engine loads one theme for PrimeReact 10, generated from the tokens on each build; it replaces `saga-blue`
and the 18 compiled themes, and the theme name only selects the preset of the `<html>` attributes.
`awe-framework/awe-design-tokens/primereact/primereact-theme.cjs` reads the Lara light theme of the pinned `primereact`
(10.9.9, MIT) for its selectors only and replaces every colour, font size and weight, spacing, size, radius, shadow,
focus ring and transition with an `--awe-*` token, through the equivalence table `primereact-theme.json` next to it.
The values follow the Aura preset of `@primeuix/themes` 2.0.3 (MIT, reference values only), as the prototype does. A
value of Lara that the table does not map, or a token the token file does not declare, fails the build, so a
PrimeReact upgrade cannot bring its own look. `primeReactThemeTest.js` checks the result: no literal colour or
geometry, only tokens of the token file, and the rules of every PrimeReact component the engine uses.

## Mapping from the current tokens {#mapping-from-the-current-tokens}

What happens to the tokens and theme variables of AWE 4 and the current AWE 5 branch. The current tokens are
documented in [Design tokens](../guides/design-tokens-guide.md).

### Current tokens of the AngularJS engine {#mapping-angular}

The 56 tokens that `tokens.less` declared until the AngularJS engine adopted the AWE 5 values. **None is removed or
renamed**; some change value or meaning.

| Token | AWE 5 | Note |
|---|---|---|
| `--awe-color-theme` | Kept, new meaning | Alias of `--awe-color-primary`. |
| `--awe-color-text` | Kept |  |
| `--awe-color-primary` | Kept |  |
| `--awe-color-on-primary` | Kept |  |
| `--awe-color-secondary` | Kept |  |
| `--awe-color-on-secondary` | Kept |  |
| `--awe-color-background` | Kept |  |
| `--awe-color-surface` | Kept, new meaning | Card and content surface. The old meaning (panel footers, input areas) is `--awe-color-surface-subtle`. |
| `--awe-color-border` | Kept |  |
| `--awe-color-shadow` | Kept |  |
| `--awe-color-menu` | Kept, new value | The menu is a light surface (`--awe-color-surface`). |
| `--awe-color-menu-accent` | Kept, new value | Highlight of the active option. |
| `--awe-color-menu-text` | Kept |  |
| `--awe-color-navbar` | Kept, new value | The header is a light surface. |
| `--awe-color-navbar-accent` | Kept, new value | Hover surface of the header. |
| `--awe-color-navbar-text` | Kept |  |
| `--awe-color-navbar-dropdown` | Kept, new value | Overlay surface (`--awe-color-overlay`). |
| `--awe-color-panel` | Kept |  |
| `--awe-color-panel-header` | Kept, new value | Card headers are not coloured: card surface. |
| `--awe-color-panel-border` | Kept |  |
| `--awe-color-panel-text` | Kept |  |
| `--awe-color-grid-header` | Kept, new value | Subtle surface (`--awe-color-surface-subtle`). |
| `--awe-color-grid-border` | Kept |  |
| `--awe-color-skeleton` | Kept |  |
| `--awe-color-skeleton-highlight` | Kept |  |
| `--awe-color-disabled` | Kept |  |
| `--awe-color-disabled-border` | Kept |  |
| `--awe-color-disabled-text` | Kept |  |
| `--awe-color-required` | Kept, new meaning | Colour of the required marker (`*`), no longer a field background. |
| `--awe-color-loading-bar` | Kept, new value | The primary. |
| `--awe-color-danger` | Kept |  |
| `--awe-color-warning` | Kept |  |
| `--awe-color-success` | Kept |  |
| `--awe-color-info` | Kept |  |
| `--awe-font-family-base` | Kept, new value | Inter. |
| `--awe-font-family-monospace` | Kept |  |
| `--awe-font-size-base` | Kept, new value | 14px, 13px in compact. |
| `--awe-font-size-small` | Kept, new value | Alias of `--awe-font-size-sm` (13px, 12px in compact). |
| `--awe-line-height-base` | Kept |  |
| `--awe-font-weight-normal` | Kept |  |
| `--awe-font-weight-bold` | Kept |  |
| `--awe-space-2xs` | Kept |  |
| `--awe-space-xs` | Kept, new value | 4px (was 5px). |
| `--awe-space-sm` | Kept, new value | 8px (was 10px). |
| `--awe-space-md` | Kept, new value | 12px (was 15px). |
| `--awe-space-lg` | Kept, new value | 16px (was 20px). |
| `--awe-space-xl` | Kept, new value | 24px (was 30px). |
| `--awe-radius` | Kept, new value | 6px, alias of `--awe-radius-md`. |
| `--awe-radius-sm` | Kept, new value | 4px fixed (was half of the radius). |
| `--awe-radius-lg` | Kept, new value | 8px fixed (was twice the radius). |
| `--awe-radius-full` | Kept |  |
| `--awe-shadow-sm` | Kept, new value | New shadow scale. |
| `--awe-shadow-md` | Kept, new value | New shadow scale. |
| `--awe-shadow-lg` | Kept, new value | New shadow scale. |
| `--awe-size-header-height` | Kept, new value | 60px, 52px in compact. |
| `--awe-size-menu-width` | Kept, new value | 264px, 240px in compact. |

### Current tokens of the React engine {#mapping-react}

The 15 tokens `awe-client-react/src/main.css` declared read PrimeReact variables. They are now declared only in the shared token file and the [AWE theme of PrimeReact](#primereact-theme) reads the tokens (the other way round).

| Token | Read before | AWE 5 |
|---|---|---|
| `--awe-color-primary` | `--primary-color` | Kept |
| `--awe-color-on-primary` | `--primary-color-text` | Kept |
| `--awe-color-text` | `--text-color` | Kept |
| `--awe-color-background` | `--surface-ground` | Kept |
| `--awe-color-surface` | `--surface-card` | Kept |
| `--awe-color-panel` | `--surface-card` | Kept |
| `--awe-color-border` | `--surface-border` | Kept |
| `--awe-color-danger` | `--danger-color` | Kept |
| `--awe-color-warning` | `--warning-color` | Kept |
| `--awe-color-success` | `--success-color` | Kept |
| `--awe-color-info` | `--info-color` | Kept |
| `--awe-font-family-base` | `--font-family` | Kept |
| `--awe-radius` | `--border-radius` | Kept |
| `--awe-color-skeleton` | `--surface-c` | Kept |
| `--awe-color-skeleton-highlight` | `--surface-b` | Kept |

### Theme variables and database themes {#mapping-themes}

The theme variables of AngularJS (LESS themes) and the colour fields of the database themes (table `AweThmCol`, served by AWE 4 as `/css/themeVariables.css`, removed in AWE 5) share their names. In AWE 5 **none is configured**: a theme is primary + surface and the rest is derived.

| `AweThmCol` field | Theme variable | AWE 5 | Status |
|---|---|---|---|
| `themeColor` | `--theme-color` | `--awe-color-theme` → `--awe-color-primary` | Removed: derived |
| `textColor` | `--text-color` | `--awe-color-text` | Removed: derived |
| `primaryColor` | `--primary-color` | `data-awe-primary` | Kept as a palette choice |
| `secondaryColor` | `--secondary-color` | `--awe-color-secondary` | Removed: derived |
| `primaryTextColor` | `--primary-text-color` | `--awe-color-on-primary` (`--awe-primary-on-fill`) | Removed: derived |
| `secondaryTextColor` | `--secondary-text-color` | `--awe-color-on-secondary` | Removed: derived |
| `primaryBackgroundColor` | `--primary-background-color` | `--awe-color-background` | Removed: derived |
| `secondaryBackgroundColor` | `--secondary-background-color` | `--awe-color-surface-subtle` | Removed: derived |
| `primaryMenuColor` | `--primary-menu-color` | `--awe-color-menu` | Removed: derived |
| `secondaryMenuColor` | `--secondary-menu-color` | `--awe-color-menu-accent` | Removed: derived |
| `menuTextColor` | `--menu-text-color` | `--awe-color-menu-text` | Removed: derived |
| `primaryNavbarColor` | `--primary-navbar-color` | `--awe-color-navbar` | Removed: derived |
| `secondaryNavbarColor` | `--secondary-navbar-color` | `--awe-color-navbar-accent` | Removed: derived |
| `navbarDropdownColor` | `--navbar-dropdown-color` | `--awe-color-navbar-dropdown` | Removed: derived |
| `navbarTextColor` | `--navbar-text-color` | `--awe-color-navbar-text` | Removed: derived |
| `primaryFontSize` | `--primary-font-size` | `--awe-font-size-base` (density) | Removed: derived |
| `secondaryFontSize` | `--secondary-font-size` | `--awe-font-size-sm` (density) | Removed: derived |
| `disabledColor` | `--disabled-color` | `--awe-color-disabled` | Removed: derived |
| `disabledBorderColor` | `--disabled-border-color` | `--awe-color-disabled-border` | Removed: derived |
| `disabledTextColor` | `--disabled-text-color` | `--awe-color-disabled-text` | Removed: derived |
| `headerHeight` | `--header-height` | `--awe-size-header-height` (density) | Removed: derived |
| `panelHeaderColor` | `--panel-header-color` | `--awe-color-panel-header` | Removed: derived |
| `panelBorderColor` | `--panel-border-color` | `--awe-color-panel-border` | Removed: derived |
| `panelBackgroundColor` | `--panel-background-color` | `--awe-color-panel` | Removed: derived |
| `panelTextColor` | `--panel-text-color` | `--awe-color-panel-text` | Removed: derived |
| `gridHeaderColor` | `--grid-header-color` | `--awe-color-grid-header` | Removed: derived |
| `gridBorderColor` | `--grid-border-color` | `--awe-color-grid-border` | Removed: derived |
| `dangerColor` | `--danger-color` | `--awe-color-danger*` | Removed: derived |
| `warningColor` | `--warning-color` | `--awe-color-warning*` | Removed: derived |
| `successColor` | `--success-color` | `--awe-color-success*` | Removed: derived |
| `infoColor` | `--info-color` | `--awe-color-info*` | Removed: derived |
| `borderColor` | `--border-color` | `--awe-color-border` | Removed: derived |
| `borderRadius` | `--border-radius` | `--awe-radius-*` (fixed scale) | Removed: derived |
| `loadingBarColor` | `--loading-bar-color` | `--awe-color-loading-bar` | Removed: derived |
| `requiredColor` | `--required-color` | `--awe-color-required` | Removed: derived |
| `dark` |  | `light-dark()` in every token | Replaced by the mode |

New in the AWE 5 theme: surface palette (`data-awe-surface`) and, optionally, a default mode and density.

The `AWE_V1.5.0` Flyway migration (all 6 databases) adds the columns `Pri` (primary), `Srf` (surface) and `Mde` (mode) to `AweThm` and `HISAweThm`, and `density` to `AweUsrSet`. Then it fills the themes:

1. The classic theme names take their [preset](color-and-themes.md#presets-of-the-classic-themes).
2. A theme with a light colour set in `AweThmCol` takes its `primaryColor` when it is a hex (`#RGB` or `#RRGGBB`).
3. Every value still empty takes the default: `emerald`, `auto` and `system`.

Nothing is deleted: `AweThmCol` is kept as it is, unused, and is dropped in a later version. The user density is empty (the application density) until the user chooses one.

The React client no longer loads the compiled PrimeReact `theme.css` files (`saga-blue` and the 18 classic themes): it bundles **one** [AWE theme of PrimeReact](#primereact-theme) that reads the tokens.
