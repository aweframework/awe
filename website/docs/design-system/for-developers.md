---
id: for-developers
title: For developers
sidebar_label: For developers
---

How to write CSS and components for AWE 5 in either engine, and how a product will configure its theme.

## Rules

| Rule | Instead of | Write |
|------|------------|-------|
| Tokens only | `color: #334155` | `color: var(--awe-color-text)` |
| No colour literals outside the token file | `rgba(0, 0, 0, 0.1)` | `var(--awe-color-shadow)` or a shadow token |
| No sizes outside the scales | `padding: 10px 15px` | `padding: var(--awe-space-2) var(--awe-space-3)` |
| Read tier 2 or 3, never tier 1 | `var(--awe-palette-emerald-600)` | `var(--awe-color-primary)` |
| No mode-specific rules | `.dark .my-card { … }` | A token with `light-dark()` |
| No density-specific rules | `[data-awe-density] .my-field { … }` | Size and padding tokens |
| Use components, not ad-hoc markup | A hand-made dropdown | The listbox, dialog, popover components |
| Motion through tokens | `transition: 0.3s` | `var(--awe-motion-duration-base) var(--awe-motion-easing-standard)` |

A card in product CSS:

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

It follows the theme, the mode and the density with nothing else to do.

## Add a component

1. Build it with semantic tokens. Check every state: hover, focus-visible, active, disabled, invalid, loading, empty.
2. Add a component token (`--awe-<component>-<property>`) only if the component needs a knob that a semantic token
   does not give, for example a value that changes with the density. Define it in the token file, pointing to a
   semantic token, and give it a compact value if needed.
3. Add a light and a dark value only through `light-dark()` in the token file, never in the component.
4. Document the component in [Components](components.md) and its tokens in the [token reference](tokens-reference.md).
5. Implement it in **both** engines with the same tokens and the same states.

## Configure the theme of a product {#configure-the-theme-of-a-product}

:::note Planned
The configuration below is the planned design. It is not implemented yet.
:::

Two ways to give a product its theme, which can be combined:

| Way | How | Example |
|-----|-----|---------|
| Theme name (classic) | Each theme name is a preset of primary, surface and default mode. It is chosen as today: user theme, then profile theme, then `awe.application.theme`, and the theme selector. | `awe.application.theme=sky` |
| Properties | The application properties set the theme values directly, without a name. | `awe.application.theme-primary=indigo` |

Primary and surface take a **palette name or a hex**. A hex generates the whole scale (see
[Palettes generated from a hex](color-and-themes.md#palettes-generated-from-a-hex)). A theme name can also carry hex
values, so a customer theme is not limited to the 17 primaries.

Proposed properties (names to confirm in the implementation), next to the existing
`awe.application.theme` and `awe.application.theme-follow-os-color-scheme`:

| Property | Values | Default |
|----------|--------|---------|
| `awe.application.theme-primary` | Palette name (one of the 17 primaries) or hex (`#RGB`, `#RRGGBB`) | from the theme name, else `emerald` |
| `awe.application.theme-surface` | Palette name (`slate`, `gray`, `zinc`, `neutral`, `stone`, `auto`) or hex | from the theme name, else `auto` (slate light, zinc dark) |
| `awe.application.theme-mode` | `light`, `dark`, `system` | `system` |
| `awe.application.density` | `comfortable`, `compact` | `comfortable` |

Examples:

```properties
# A named palette
awe.application.theme-primary=indigo
# The brand colour of the customer: the scale and the fill are generated
awe.application.theme-primary=#4F46E5
# A warm neutral background
awe.application.theme-surface=#F4F1EA
```

| Where | What | Status |
|-------|------|--------|
| Theme names | Today's 19 theme names map to a primary + surface preset; the database themes (`AweThm`) keep their name and are reduced to primary, surface and mode, each primary and surface a palette name or a hex. Existing rows are migrated. | Planned |
| Theme selector and theme screen | Choose a theme by name, as today; the theme screen edits primary and surface with a live preview | Planned |
| User settings | Mode (as today) and density per user | Planned |

The engines render the choice as attributes of `<html>`:
`class="dark"`, `data-awe-primary`, `data-awe-surface`, `data-awe-density`. A palette generated from a hex is rendered
as the `--awe-primary-*` or `--awe-surface-*` custom properties of `<html>`; nothing else changes. A product that needs
another brand colour sets its hex; it does not override semantic tokens locally.

## Accessibility checklist

- [ ] Text against its background is at least 4.5:1 in light **and** dark, with any primary.
- [ ] Every interactive element shows a visible focus ring with `:focus-visible`.
- [ ] Everything works with the keyboard: Tab order follows the layout; Esc closes overlays and returns the focus.
- [ ] Icon-only buttons have `aria-label`; decorative icons are `aria-hidden="true"`.
- [ ] Errors are linked to their field (`aria-invalid`, `aria-describedby`) and never shown by colour alone.
- [ ] Popups use the right roles (`combobox`/`listbox`, `menu`, `dialog`) and states (`aria-expanded`, `aria-selected`).
- [ ] Loading regions set `aria-busy`; status updates use `role="status"` or `role="alert"`.
- [ ] Nothing moves with `prefers-reduced-motion: reduce`.
- [ ] Layout works at 200% zoom and down to tablet width.

## Check a change

| Check | How |
|-------|-----|
| Look | Compare with the [prototype](pathname:///design-system/prototype/) in light and dark, comfortable and compact, and with two primaries (one dark like `indigo`, one light like `amber`). |
| Literals | Search the changed CSS for `#`, `rgb(`, `hsl(` and bare `px` values outside the token file. |
| Contrast | Measure text pairs with a contrast checker in both modes. |
| Keyboard | Go through the change with Tab, Enter, Space, arrows and Esc only. |
| Tests | Jest and browser tests of the engine you changed; the token tests once the token file is in the engines. |

Today's tokens of the AngularJS engine are still checked by `designTokens.test.js` against the
[current guide](../guides/design-tokens-guide.md).
