# AWE 5 design system

Captured from the project sources, not invented: every value below exists in the file named after it.

| Source | What it gives |
|--------|---------------|
| `awe-framework/awe-design-tokens/tokens.css` | The single AWE 5 token file of both engines: 661 unique `--awe-*` tokens (1013 declarations, density and palette overrides included) |
| `awe-framework/awe-client-react/src/main.css` | Imports `tokens.css` and sets the colour mode on `<html>` (`light`, `dark`, or the system one); the system mode is declared on `:root:not(.light):not(.dark)` so the PrimeReact theme, loaded later, cannot force light |
| `awe-framework/awe-client-react/src/static/css/splash.css`, `awe-framework/awe-client-angular/src/main/resources/css/splash.css` | Splash (#964): standalone, loads before the tokens, so it keeps its own literals (`--awe-splash-background`, `--awe-splash-empty`, light and dark); the logo is a mask (`--awe-splash-logo`, `--awe-splash-liquid`) that fills up while the page loads, animating only transform and opacity |
| `awe-framework/awe-client-angular/src/main/resources/tailwind/theme.css` | Tailwind theme: one to one aliases of the `--awe-*` tokens, so utilities follow the theme and the mode |
| `website/docs/design-system/foundations.md` | Token tiers, colour, typography, spacing rules |
| `website/docs/design-system/color-and-themes.md` | Primary and surface palettes, light and dark |
| `website/docs/design-system/layout-and-shell.md` | Shell: side menu, header, page, cards |
| `website/docs/design-system/components.md` | Rules and states per component |
| `website/docs/design-system/tokens-reference.md` | Token reference |

Hard rules (from `skills/awe-ui/SKILL.md`): `--awe-*` tokens only, no colour literals, no bare `px` outside the token
file, never a `--awe-palette-*` primitive in a component, no mode or density selectors in component CSS, Inter and
Lucide only.

## Colour

Three tiers: primitives (`--awe-palette-*`, never used by a component), semantic (`--awe-color-*`, typography,
spacing, radius, shadow, focus, motion) and component tokens (`--awe-<component>-*`).

**Modes.** Every mode-dependent colour is one `light-dark(<light>, <dark>)` declaration resolved by the `color-scheme`
of the root (`tokens.css`). The engines set `class="light"` / `class="dark"` on `<html>` or follow the system.

**Primary palettes (17).** `emerald` (default), `green`, `lime`, `red`, `orange`, `amber`, `yellow`, `teal`, `cyan`,
`sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`, chosen with
`<html data-awe-primary="<color>">`. Each one fills `--awe-primary-50..950` plus `--awe-primary-fill`,
`--awe-primary-fill-hover`, `--awe-primary-fill-active` and `--awe-primary-on-fill` (the filled shades are chosen per
palette so the label on them reaches WCAG AA).

**Surface palettes (5).** `slate` (default light), `gray`, `zinc` (default dark), `neutral`, `stone`, chosen with
`<html data-awe-surface="<neutral>">`, filling `--awe-surface-0..950`.

**Semantic groups** (`tokens.css`, Tier 2):

| Group | Tokens |
|-------|--------|
| Primary | `--awe-color-primary`, `-hover`, `-active`, `-text`, `-subtle`, `--awe-color-on-primary`, `--awe-color-on-primary-subtle`, `--awe-color-theme` |
| Text | `--awe-color-text`, `-emphasis`, `-muted`, `-hover`, `-muted-hover` |
| Surfaces | `--awe-color-background`, `-surface`, `-surface-hover`, `-surface-subtle`, `-surface-raised`, `-border`, `-border-subtle`, `-shadow`, `-shadow-soft`, `-mask` |
| Highlight (selection) | `--awe-color-highlight`, `-highlight-focus`, `-on-highlight`, `-on-highlight-focus` |
| Fields | `--awe-color-field`, `-field-filled`, `-field-border`, `-field-border-hover`, `-field-border-focus`, `-field-border-invalid`, `-field-text`, `-field-placeholder`, `-field-label`, `-field-help`, `-field-focus-halo`, `-field-invalid-halo` |
| Lists and options | `--awe-color-list-option-focus`, `-selected`, `-selected-focus`, `-text`, `-icon`, `-group` |
| Navigation | `--awe-color-nav-item-focus`, `-active`, `-text`, `-icon`, `--awe-color-nav-submenu-label` |
| Overlays | `--awe-color-overlay`, `-border`, `-text`, `--awe-color-tooltip`, `--awe-color-on-tooltip` |
| Grid | `--awe-color-grid-header`, `--awe-color-grid-border`, `--awe-color-skeleton`, `-skeleton-highlight` |
| States | `--awe-color-disabled`, `-border`, `-text`, `--awe-color-required`, `--awe-color-loading-bar` |
| Status | per tone (`danger`, `success`, `warning`, `info`): solid, `-hover`, `-subtle`, `-text`, `-border`, `--awe-color-on-<tone>`; `--awe-color-neutral-subtle`, `-neutral-text`, `--awe-color-contrast`, `-on-contrast` |
| Data visualisation | `--awe-color-scale-1..5` + `-on-scale-1..5`, `--awe-color-data-bar`, `-negative`, `--awe-color-chart-line`, `-area`, `-negative`, `-axis`, `--awe-color-map-*` |

`danger` is `red-600` light / `red-400` dark, `success` `green-700` / `green-400`, `warning` `orange-700` /
`orange-400`, `info` `sky-700` / `sky-400` (`tokens.css`).

## Typography

`--awe-font-family-base: 'Inter Variable', Inter, system-ui, ...`; monospace
`--awe-font-family-monospace: ui-monospace, 'SF Mono', ...`.

| Scale (comfortable / compact) | Token |
|-------------------------------|-------|
| 12 / 11px | `--awe-font-size-xs` |
| 13 / 12px | `--awe-font-size-sm` (`--awe-font-size-small`) |
| 14 / 13px | `--awe-font-size-base` |
| 16 / 15px | `--awe-font-size-md` |
| 18px | `--awe-font-size-lg` |
| 20px | `--awe-font-size-xl` |
| 24 / 20px | `--awe-font-size-2xl` |
| 30 / 24px | `--awe-font-size-3xl` |

Roles: `--awe-font-size-page-title` (2xl), `--awe-font-size-section-title` (md), `--awe-font-size-dialog-title` (lg),
`--awe-font-size-display` (3xl, standalone pages), `--awe-font-size-label` (sm), `--awe-font-size-caption` (xs).
Weights `400/500/600/700`; line heights `--awe-line-height-tight/-snug/-base` (`1.25/1.375/1.5`); letter spacing
`--awe-letter-spacing-tight` (`-0.011em`), `-display` (`-0.022em`), `-caps` (`0.06em`). Font features `'cv05', 'calt'`, numeric `tabular-nums`.

## Spacing, radius, elevation, focus, motion

- Spacing on a 4px grid: `--awe-space-0..16` (`0, 2, 4, 6, 8, 12, 16, 20, 24, 32, 40, 48, 64px`), plus the t-shirt
  aliases `2xs…2xl` pointing at the numeric scale.
- Radius: `none 0`, `xs 2px`, `sm 4px`, `md 6px`, `lg 8px`, `xl 12px`, `full 9999px`; roles `--awe-radius-field`
  (md), `--awe-radius-card` (xl), `--awe-radius-overlay` (md), `--awe-radius-modal` (xl), `--awe-radius-list-option` (sm).
- Elevation: `--awe-shadow-xs/sm/md/lg` plus roles `--awe-shadow-field` (xs), `--awe-shadow-card` (xs),
  `--awe-shadow-overlay` (md), `--awe-shadow-modal` (lg).
- Focus: `--awe-focus-ring: 2px solid var(--awe-color-focus-ring)` with `--awe-focus-ring-offset: 2px`; fields add
  `--awe-shadow-field-focus` / `--awe-shadow-field-invalid` (3px halo).
- Motion: `120ms / 200ms / 300ms` (`fast/base/slow`) and `--awe-motion-duration-expand: 240ms`; easings
  `standard cubic-bezier(0.2,0,0,1)`, `enter`, `exit`, `expand cubic-bezier(0.22,1,0.36,1)`. Every duration becomes
  `0ms` under `prefers-reduced-motion: reduce`.
- Layers: header 100, menu 110, dropdown 1000, modal 1100, toast 1200, tooltip 1300.
- Opacity: `--awe-opacity-disabled: 0.6`.
- Borders: `--awe-border-width: 1px`, `--awe-border-width-strong: 2px`.

## Density

Comfortable by default; `<html data-awe-density="compact">` changes sizes, not structure.

| Token | Comfortable | Compact |
|-------|-------------|---------|
| `--awe-size-control` | 36px | 30px |
| `--awe-size-control-sm` | 30px | 26px |
| `--awe-size-control-lg` | 44px | 36px |
| `--awe-size-icon` | 16px | 14px |
| `--awe-size-checkbox` | 18px | 16px |
| `--awe-size-header-height` | 60px | 52px |
| `--awe-size-menu-width` | 264px | 240px |
| `--awe-field-padding-x` / `-y` | 12px / 8px | 9px / 5px |
| `--awe-gap-form-row` / `-column` | 18px / 20px | 12px / 14px |
| `--awe-content-padding` / `--awe-layout-gap` / `--awe-page-padding` | 20 / 20 / 28px | 14 / 12 / 16px |
| `--awe-grid-cell-padding-y` / `-x` | 11px / 14px | 6px / 10px |
| `--awe-menu-item-height` | 36px | 30px |

## Component tokens

| Component | Tokens (`tokens.css`, Tier 3) |
|-----------|-------------------------------|
| Buttons | `--awe-button-padding-x: 14px`, `--awe-button-font-size/-sm/-lg` (base/sm/md), `--awe-button-gap`, `--awe-button-default-background/-border-color/-color/-font-weight` with `-hover` and `-active` variants |
| Labels | `--awe-label-font-weight` (medium), `--awe-label-text-transform: none`, `--awe-label-letter-spacing: normal` |
| Card | `--awe-card-title-font-size` (base), `-title-text-transform: uppercase`, `-title-letter-spacing` (caps), `--awe-card-padding` (content padding), `--awe-card-gap` (space-3), `--awe-card-icon-size` (control), `--awe-card-grid-inset`, `--awe-card-grid-gap-after`; `.awe-card-compact` lowers padding to space-4, gap to space-2 and the icon to control-sm |
| Readout | `--awe-readout-label-color/-font-size/-font-weight` (muted, xs, normal), `--awe-readout-value-color/-font-size`, `--awe-readout-block-background/-border/-radius/-padding` (style `awe-readout`, `awe-readout-block`) |
| Datepicker | `--awe-datepicker-day-size` (space-8), `--awe-datepicker-cell-gap` (space-0-5) |
| Side panel | `--awe-panel-avatar-icon-size` (icon) |
| Grid | `--awe-grid-search-width: 32ch` (least width of the toolbar search), `--awe-grid-avatar-icon-size` (icon-sm), `--awe-grid-header-background/-text/-font-size/-font-weight`, `--awe-grid-border`, `--awe-grid-row-background/-hover-background/-selected-background/-selected-text`, `--awe-grid-loading-overlay`, `--awe-grid-cell-padding-x/-y`, `--awe-grid-row-marker-width: 3px`, `--awe-grid-drop-indicator-width`, `--awe-grid-min-body-height: 12rem`, `--awe-grid-columns-panel-width: 432px` |
| Side menu | `--awe-menu-width`, `-width-collapsed: 68px`, `-background`, `-border`, `-item-text/-icon/-hover-background/-active-background/-active-text/-active-icon`, `-item-radius`, `-item-height`, `-section-label`, `-item-sub-text`, `-indent` (30px), `-indent-l3` (22px), `-guide-l2/-l3`, `-guide`, `-guide-active`, `-flyout-width` |
| Header | `--awe-header-height`, `-background` (85% navbar), `-border`, `-backdrop-filter: saturate(1.4) blur(10px)`, `-search-width: 380px`, `-logo-height: 34px` |
| Overlays | `--awe-mask-backdrop-filter: blur(2px)`, `--awe-toast-backdrop-filter: blur(8px)` |
| Segmented | `--awe-segmented-track/-border/-text/-hover-text/-selected/-selected-border/-selected-text` |
| Choice controls | `--awe-checkbox-icon-size: 12px`, `--awe-radio-dot-size: 8px`, `--awe-switch-width: 36px`, `--awe-switch-height: 20px`, `--awe-switch-handle-size: 16px` |
| Layout | `--awe-content-padding`, `--awe-layout-gap`, `--awe-page-padding`, `--awe-grid-container-sm/md/lg` (742/962/1162px), `--awe-list-option-padding`, `--awe-nav-item-padding`, `--awe-overlay-padding`, `--awe-modal-padding` |
| Login | `--awe-login-*` (34 tokens: form width, padding, image, scrim, gradient, brand panel, card layout) |

## Component rules

Summarised from `website/docs/design-system/components.md`; the AWE element and its React/AngularJS
implementation are the source of truth.

- **Buttons**: primary (one per area), secondary, outlined, text, ghost icon (always `aria-label` + tooltip), danger
  (filled only in the confirmation step); sizes `--awe-size-control-sm/default/lg`; label weight 500, icon left.
- **Criteria**: label above (`--awe-font-size-label`, weight 500, red `*` when required); field height
  `--awe-size-control`, `--awe-radius-field`; focus = primary border + halo; invalid = red border, message and
  `aria-invalid`; auto-fill grid of 232px columns; numbers right-aligned, tabular.
- **Select**: never the native popup; combobox with listbox overlay (`--awe-shadow-overlay`), 8×12 padding options,
  `--awe-radius-list-option`, check on the selected one, filter when more than 10 options.
- **Readout**: a read-only criterion shows a muted small label over the value; `awe-readout-block` boxes long values
  (detail, hash, user agent).
- **Grid**: lives in a card; toolbar with search, filters, columns, export; sticky header
  (`--awe-grid-header-*`); no zebra; selection uses `--awe-color-highlight` plus a selection bar; status as tags with a
  dot; row actions as ghost icon buttons; pager "Showing 1–10 of 1,284"; skeleton rows while loading; empty state.
- **Overlays**: dialog (`<dialog>`, `--awe-radius-modal`, mask `--awe-color-mask`), side panel (#843, slides from the
  right, own footer), popover (#843), dropdown menu, toast (`--awe-z-toast`), tooltip.
- **Layout and shell**: side menu (sections, three levels, rail at 68px, drawer below 1024px), light sticky header,
  breadcrumb, page title with one primary action, every block a card (`--awe-radius-card`, `--awe-shadow-card`).
- **Accessibility**: contrast ≥ 4.5:1 in light and dark with any primary, visible `:focus-visible` ring everywhere,
  motion off with `prefers-reduced-motion`, never colour alone for a state.

## Gaps found while capturing

- No `--awe-*` token for row detail or for a split (two column) page layout: those still use the Bootstrap-compatible
  `col-*` classes of both engines (`theme.css` breakpoints `sm 768px`, `md 992px`, `lg 1200px`).
- The token file is the only place that declares a value; the engine files only alias it, so a design that needs a new
  value has to propose a token here and implement it in both engines.
