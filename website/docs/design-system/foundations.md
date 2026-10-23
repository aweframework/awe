---
id: foundations
title: Foundations
sidebar_label: Foundations
---

Type, spacing, density, shape, elevation, motion and icons. Every value below is a token: use the token, not the
number. Exact values for both densities are in the [token reference](tokens-reference.md#tier-2-semantic).

## Typography

The font is **Inter** (variable, OFL-1.1), served by AWE, with a system fallback:
`--awe-font-family-base: 'Inter Variable', Inter, system-ui, ...`. Code and identifiers use
`--awe-font-family-monospace`.

| Token | Comfortable | Compact | Use |
|-------|-------------|---------|-----|
| `--awe-font-size-xs` | 12px | 11px | Captions, badges, grid header labels in tight spaces |
| `--awe-font-size-sm` | 13px | 12px | Labels, help text, secondary lines, grid headers |
| `--awe-font-size-base` | 14px | 13px | Body, fields, buttons, cells |
| `--awe-font-size-md` | 16px | 15px | Card and section titles |
| `--awe-font-size-lg` | 18px | 18px | Dialog titles |
| `--awe-font-size-xl` | 20px | 20px | Page title in compact |
| `--awe-font-size-2xl` | 24px | 24px | Page title, KPI values |
| `--awe-font-size-3xl` | 30px | 30px | Display (login and standalone pages) |

Role tokens point to the scale; prefer them: `--awe-font-size-page-title`, `--awe-font-size-section-title`,
`--awe-font-size-dialog-title`, `--awe-font-size-display`, `--awe-font-size-label`, `--awe-font-size-caption`.

| Weight | Token | Use |
|--------|-------|-----|
| 400 | `--awe-font-weight-normal` | Body, values |
| 500 | `--awe-font-weight-medium` | Labels, buttons, menu items, tabs |
| 600 | `--awe-font-weight-semibold` | Titles, selected items, totals |
| 700 | `--awe-font-weight-bold` | Rare: badges with very short text |

- Line heights: `--awe-line-height-tight` (1.25, titles), `--awe-line-height-snug` (1.375, labels, lists),
  `--awe-line-height-base` (1.5, body).
- Titles use a small negative letter spacing (`--awe-letter-spacing-display`, `--awe-letter-spacing-tight`). Small
  uppercase labels use `--awe-letter-spacing-caps`.
- **Numbers in grids, totals and KPIs use tabular figures:** `font-variant-numeric: var(--awe-font-variant-numeric)`,
  right aligned, so the digits line up.

## Spacing

Spacing is on a **4px grid**. The numeric tokens are the scale; the existing t-shirt names stay as aliases.

| Token | Value | Alias |
|-------|-------|-------|
| `--awe-space-0-5` | 2px | `--awe-space-2xs` |
| `--awe-space-1` | 4px | `--awe-space-xs` |
| `--awe-space-1-5` | 6px | |
| `--awe-space-2` | 8px | `--awe-space-sm` |
| `--awe-space-3` | 12px | `--awe-space-md` |
| `--awe-space-4` | 16px | `--awe-space-lg` |
| `--awe-space-5` | 20px | |
| `--awe-space-6` | 24px | `--awe-space-xl` |
| `--awe-space-8` | 32px | `--awe-space-2xl` (new) |
| `--awe-space-10`, `-12`, `-16` | 40px, 48px, 64px | |

:::warning Value change
The t-shirt names keep their names but **not their values**: today `xs` is 5px, `sm` 10px, `md` 15px, `lg` 20px and
`xl` 30px. In AWE 5 they are 4, 8, 12, 16 and 24px. Product CSS that relies on the old values must be reviewed.
:::

Layout spacing has its own tokens, which change with the density: `--awe-content-padding` (inside cards),
`--awe-layout-gap` (between cards), `--awe-page-padding` (page edges), `--awe-gap-form-row` and
`--awe-gap-form-column` (criteria grid).

## Density

| | Comfortable (default) | Compact |
|---|---|---|
| Base font | 14px | 13px |
| Control height (`--awe-size-control`) | 36px | 30px |
| Small / large control | 30px / 44px | 26px / 36px |
| Field padding | 8px × 12px | 5px × 9px |
| Grid cell padding | 11px × 14px | 6px × 10px |
| Header height | 60px | 52px |
| Menu width | 264px | 240px |

Density is `data-awe-density="compact"` on `<html>`. It is configurable per application and per user. Components get
it for free if they use the size, padding and gap tokens. Never write a component rule for one density only.

## Shape

Radius scale (Aura):

| Token | Value | Use |
|-------|-------|-----|
| `--awe-radius-xs` | 2px | Text highlights, small marks |
| `--awe-radius-sm` | 4px | Checkboxes, chips, tags, list options |
| `--awe-radius-md` | 6px | Fields, buttons, overlays (`--awe-radius-field`, `--awe-radius-overlay`) |
| `--awe-radius-lg` | 8px | Inner panels, launcher cards |
| `--awe-radius-xl` | 12px | Cards and dialogs (`--awe-radius-card`, `--awe-radius-modal`) |
| `--awe-radius-full` | 9999px | Avatars, pills, switches |

`--awe-radius` stays as an alias of `md`. Today `--awe-radius-sm` and `-lg` are derived (half and double); in AWE 5
they are fixed values of the scale.

## Elevation

| Token | Use |
|-------|-----|
| `--awe-shadow-xs` (`--awe-shadow-card`, `--awe-shadow-field`) | Cards, fields, outlined buttons: barely lifted |
| `--awe-shadow-sm` | Selected segment, switch handle |
| `--awe-shadow-md` (`--awe-shadow-overlay`) | Dropdowns, listboxes, popovers, toasts, tooltips |
| `--awe-shadow-lg` (`--awe-shadow-modal`) | Dialogs, side panel, maximized window |

Cards rely on a border plus `xs`, not on heavy shadows. Shadows are stronger in dark mode (`--awe-color-shadow`).
Layers have fixed z-index tokens: `--awe-z-header`, `--awe-z-menu`, `--awe-z-dropdown`, `--awe-z-modal`,
`--awe-z-toast`, `--awe-z-tooltip`. Overlays that must escape clipping (listboxes, flyouts, toasts) use the top layer
(`popover`, `<dialog>`).

## Focus

Keyboard focus is a 2px outline in `--awe-color-focus-ring` with a 2px offset (`--awe-focus-ring`,
`--awe-focus-ring-offset`), shown with `:focus-visible`. Form fields show the focus border plus a soft halo
(`--awe-shadow-field-focus`; `--awe-shadow-field-invalid` when invalid). Never remove an outline without a replacement.

## Motion

| Token | Value | Use |
|-------|-------|-----|
| `--awe-motion-duration-fast` | 120ms | Hover, colour changes, exit of small items |
| `--awe-motion-duration-base` | 200ms | Toggles, dialogs, tooltips |
| `--awe-motion-duration-slow` | 300ms | Toasts, side panel, mask |
| `--awe-motion-duration-expand` | 240ms | Expand and collapse (menu levels, accordions, rail) |
| `--awe-motion-easing-standard` | `cubic-bezier(0.2, 0, 0, 1)` | Most transitions |
| `--awe-motion-easing-enter` / `-exit` | decelerate / accelerate | Elements that appear / leave |
| `--awe-motion-easing-expand` | `cubic-bezier(0.22, 1, 0.36, 1)` | Disclosure content |

- With `prefers-reduced-motion: reduce` every duration token is 0. Use the tokens and you get this for free.
- No animation on first paint: a restored state (an open menu level) appears in place.
- Animate the real size: disclosures animate `grid-template-rows` from `0fr` to `1fr`, not a fixed `max-height`.

## Iconography

AWE 5 uses **Lucide** line icons (ISC) in both engines: 24px grid, 1.75px stroke set by CSS, `currentColor`. Sizes:
`--awe-size-icon-sm`, `--awe-size-icon`, `--awe-size-icon-lg` (14, 16, 20px comfortable).

The XML keeps its icon names. An **equivalence table** maps each `fa-*` name to a Lucide icon, so existing screens
need no change. Example:

| XML icon | Lucide |
|----------|--------|
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

The full table, the fallback for unmapped names and the handling of `mdi-*` icons are implementation work.

Rules:

- An icon-only button needs `aria-label` and a tooltip.
- Decorative icons are `aria-hidden="true"`.
- Colour comes from the parent text; do not colour icons on their own except for status.
