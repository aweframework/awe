---
id: color-and-themes
title: Color and themes
sidebar_label: Color and themes
---

A component reads a **semantic** token (`--awe-color-text`). The semantic token points to a **configurable** palette
(`--awe-surface-700`), which points to a **primitive** palette (`--awe-palette-slate-700`). A theme only chooses the
configurable palettes; the mode chooses the light or the dark value.

```text
component      color: var(--awe-color-text)
     │
semantic       --awe-color-text: light-dark(var(--awe-surface-700), var(--awe-surface-0))
     │
configurable   --awe-surface-700: var(--awe-palette-slate-700)      ← data-awe-surface="slate"
     │
primitive      --awe-palette-slate-700: #334155                     ← fixed value, never changes
```

## The three tiers

| Tier | Name pattern | Who sets it | Who reads it |
|------|--------------|-------------|--------------|
| 1 · Primitive | `--awe-palette-<color>-<50..950>` | AWE (fixed values) | Only tier 1 configurable tokens |
| 1 · Configurable | `--awe-primary-<50..950>`, `--awe-primary-fill*`, `--awe-surface-<0..950>` | The theme (`data-awe-primary`, `data-awe-surface`) | Semantic tokens |
| 2 · Semantic | `--awe-color-*`, `--awe-font-*`, `--awe-space-*`, `--awe-radius-*`, `--awe-shadow-*`, `--awe-motion-*`, `--awe-size-*` | AWE | Components, product CSS |
| 3 · Component | `--awe-<component>-<property>` (`--awe-grid-*`, `--awe-menu-*`, `--awe-header-*`, `--awe-segmented-*`, `--awe-login-*`) | AWE | That component only |

Rules:

- Components and product CSS read tier 2 or tier 3. They **never** read tier 1.
- A component token exists only when the component needs its own knob. Default to a semantic token.
- The existing `--awe-color-*` names stay (see the [mapping](tokens-reference.md#mapping-from-the-current-tokens)).

## Palettes

Twenty-two primitive palettes, each with 11 shades (50 to 950):

| Use | Palettes |
|-----|----------|
| Primary (17) | `emerald` (default), `green`, `lime`, `teal`, `cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`, `red`, `orange`, `amber`, `yellow` |
| Surface (5) | `slate` (default in light), `gray`, `zinc` (default in dark), `neutral`, `stone` |

`--awe-surface-0` is always white. The values are in the [token reference](tokens-reference.md#tier-1-primitive).

### Why the primary has fill tokens

Filling primary buttons with shade 500 and white text is below 4.5:1 for most palettes (emerald 500 with white is
2.54:1). AWE adds four configurable tokens chosen **per palette** so the label of a filled surface always
reaches AA in light mode:

| Palettes | `--awe-primary-fill` | Text on the fill |
|----------|----------------------|------------------|
| `red`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose` | 600 | white |
| `emerald`, `green`, `teal`, `cyan`, `sky`, `orange` | 700 | white |
| `lime`, `amber`, `yellow` | 500 | shade 950 |

`--awe-primary-fill-hover`, `--awe-primary-fill-active` and `--awe-primary-on-fill` follow the same table. In dark mode
the primary is shade 400 with dark text. Consequence: in light mode the default emerald is a darker shade (700).

## Light and dark

The mode is a class on `<html>`, as the AngularJS engine does today on the body:

| `<html>` class | Mode |
|----------------|------|
| `light` | Light, chosen by the user |
| `dark` | Dark, chosen by the user |
| none | The operating system decides (`prefers-color-scheme`) |

Every mode-dependent token is written **once** with `light-dark()`:

```css
:root {
  color-scheme: light dark;
  --awe-color-surface: light-dark(var(--awe-surface-0), var(--awe-surface-900));
}
.light { color-scheme: light; }
.dark { color-scheme: dark; }
```

- `light-dark()` resolves with the `color-scheme` of the element, so the classes also work on any subtree (a preview
  of the other mode inside a page).
- Requirement: browsers that support `light-dark()` (Chrome and Edge 123, Firefox 120, Safari 17.5). AWE 5 targets the
  current versions of these browsers.
- The user's explicit choice always wins over the operating system.

## A theme is primary + surface

```html
<html class="dark" data-awe-primary="indigo" data-awe-surface="zinc" data-awe-density="compact">
```

| Attribute | Values | Default |
|-----------|--------|---------|
| `data-awe-primary` | One of the 17 primary palettes, or a palette generated from a hex | `emerald` |
| `data-awe-surface` | `slate`, `gray`, `zinc`, `neutral`, `stone`, or a palette generated from a hex | `slate` in light, `zinc` in dark |
| `data-awe-density` | `comfortable`, `compact` | `comfortable` |

A product does not set colours one by one any more. The 35 colour, size and radius fields of today's themes are derived
from these two palettes. Each one is given as a **palette name** or as a **hex colour** (`#4F46E5`, `#F4F1EA`).

### Palettes generated from a hex {#palettes-generated-from-a-hex}

A hex is not used as one colour: it generates the whole scale, so every semantic token keeps working.

1. The hex (`#RGB` or `#RRGGBB`) is converted to OKLCH. Its **hue** and **chroma** are kept.
2. Each step gets a **fixed lightness**, the same for every hex. Lightness is what decides contrast, so the pairs of
   the semantic tokens hold for any hue.
3. The chroma is clamped into the sRGB gamut per step (the lightness never moves for that).
4. A few steps are nudged by measured contrast, never by more than needed (see the table below).
5. The fill of the primary (`--awe-primary-fill`, `-hover`, `-active`, `-on-fill`) is the step closest to the
   lightness of the hex whose label, white or the 950 step, reaches 4.5:1.

| | Primary (50 → 950) | Surface (0 → 950) |
|---|---|---|
| Lightness (OKLCH L) | 0.97, 0.935, 0.885, 0.81, 0.72, 0.64, 0.56, 0.49, 0.43, 0.38, 0.28 | 0 is white; then 0.985, 0.968, 0.929, 0.869, 0.711, 0.554, 0.446, 0.372, 0.279, 0.208, 0.129 |
| Chroma | The chroma of the hex, capped at 0.035 (50) and 0.07 (100), then clamped to the gamut | **Capped at 0.03**, lower at the light and dark ends |
| Adjusted by contrast | 700 and 800 on 50, 100 and white; 400 and 300 on the dark surfaces | 500 on white, 50 and 100; 400 on 800 and 900 |

What it guarantees:

- Every text pair of the semantic tokens is at least 4.5:1 in light and dark, as with the named palettes.
- A saturated surface hex (`#FF0000`) only **tints** the neutrals: the backgrounds stay calm and readable. The surface
  generates one scale for both modes; the dark mode uses its dark steps.
- The fill follows the brand: a dark brand (`#575C9C`) gets a filled 700 with white text, a light one (`#F59E0B`) a
  filled light step with dark text.

Tested hexes, in light and dark (2,736 pairs): primaries `#575C9C`, `#E11D48`, `#F59E0B`, `#84CC16`, `#0EA5E9`,
`#111827`, `#FFFFFF`, `#7C3AED`; surfaces `#F4F1EA`, `#0F172A`, `#FF0000`. Every text pair passes; the only failures
are the accepted exceptions of [Contrast rules](#contrast-rules). A near-white or near-grey hex gives a grey primary:
it is valid, but the filled controls stand out less from the page. The prototype's **Theme** panel has a *Custom*
option to try any hex.

A theme reaches the page in one of two ways (implemented on the server and rendered on `<html>`; the screens keep the
classic look until the new look of the engines, see
[For developers](for-developers.md#configure-the-theme-of-a-product)):

- **By name**, as today: a theme name (`sky`, `sunset`…) is a preset of primary, surface and default mode, chosen
  through the user theme, the profile theme, `awe.application.theme` and the theme selector.
- **By properties**: the application properties set the primary, the surface, the mode and the density directly.

### Presets of the classic themes {#presets-of-the-classic-themes}

Each classic theme name maps to the AWE 5 palette nearest to its current brand colour:

1. The brand colour is the more chromatic of the theme's `--theme-color` and `--primary-color`.
2. The 2 palettes nearest to it by OKLCH hue are taken, and of those the nearest in OKLab wins.
3. A grey brand (OKLCH chroma below 0.05) has no nearest hue: it keeps its hex, which generates its palette.
4. The surface is `auto` unless the classic background is tinted: warm gives `stone`, blue gives `slate`.
5. The mode is `system`.

| Theme | Primary | Surface | Mode |
|-------|---------|---------|------|
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

The nearest palette is not always the expected name: `orange` maps to `red` and `grass` to `cyan`, because their
brand colours are nearer to those palettes. `dust` and `amazonia` have a grey brand and keep its hex.

Status: implemented. The server applies the preset when a theme has no `AweThm` row (`amazonia` only exists in LESS)
or its row has no valid value, and the `AWE_V1.5.0` migration writes the same values on the classic rows of `AweThm`.
The engines render it on `<html>`; the screens keep the classic look until the new look of the engines.

## Semantic colours

The most used tokens. Full list in the [token reference](tokens-reference.md#tier-2-semantic).

| Need | Token |
|------|-------|
| Body text, emphasised text, muted text | `--awe-color-text`, `--awe-color-text-emphasis`, `--awe-color-text-muted` |
| Page, card, subtle area, hover | `--awe-color-background`, `--awe-color-surface`, `--awe-color-surface-subtle`, `--awe-color-surface-hover` |
| Borders | `--awe-color-border`, `--awe-color-border-subtle` |
| Filled primary and its text | `--awe-color-primary`, `--awe-color-on-primary` |
| Primary as text or thin line (links, active tab) | `--awe-color-primary-text` |
| Selected option, row, menu item | `--awe-color-highlight`, `--awe-color-on-highlight` |
| Form fields | `--awe-color-field`, `--awe-color-field-border`, `--awe-color-field-border-focus`, `--awe-color-field-border-invalid` |
| Overlays | `--awe-color-overlay`, `--awe-color-overlay-border`, `--awe-color-mask` |
| Status | `--awe-color-<danger\|success\|warning\|info>` (solid), `-subtle` (background), `-text`, `-border`, `on-<status>` |

Pairs to respect: a background token goes with its `on-` or `-text` token. Never put `--awe-color-text-muted` on a
filled primary, never put `--awe-color-primary` text on `--awe-color-highlight` (use `--awe-color-on-highlight`).

## Contrast rules {#contrast-rules}

Measured for 17 primaries × 6 surface settings × 2 modes with the prototype tokens.

| Rule | Target | Status |
|------|--------|--------|
| Text and icons that carry meaning | 4.5:1 (WCAG 1.4.3) | Met by every pair in the table above. Tightest: 4.51:1. |
| Focus ring against its background | 3:1 (WCAG 1.4.11) | Met (2px ring in `--awe-color-primary-text`). |
| Disabled text | Exempt (WCAG 1.4.3) | 3.76:1 light, 4.04:1 dark. Accepted. |
| Filled primary against the page (`lime`, `amber`, `yellow`) | 3:1 (WCAG 1.4.11) | 1.9:1 to 2.2:1. Accepted: the label on the fill is 7:1 and the control has text. |
| Form field border | 3:1 (WCAG 1.4.11) | 1.47:1 light, 2.53:1 dark. Accepted: the subtle border is kept; the label, the focus ring and the invalid state identify the field. |

## Data visualisation

| Need | Tokens |
|------|--------|
| Colour scale cells (heatmap), 5 steps from the primary | `--awe-color-scale-1..5`, text `--awe-color-on-scale-1..5` |
| Data bars in cells | `--awe-color-data-bar`, `--awe-color-data-bar-negative` |
| Sparklines and small charts | `--awe-color-chart-line`, `--awe-color-chart-area`, `--awe-color-chart-negative`, `--awe-color-chart-axis` |
| Map illustration and map controls | `--awe-color-map-*` |

Negative values always use the danger colour **and** a second cue (sign, position), never colour alone.
