---
name: awe-ui
description: "Trigger: AWE UI, UX design, screen design, styles, CSS, LESS, theme, dark mode, tokens, new component look. Apply the AWE 5 design system in both engines."
license: Apache-2.0
metadata:
  author: awe-team
  version: "1.0"
---

## Activation Contract

Use when designing, styling or reviewing any visual part of AWE: a screen, a component, a theme, CSS/LESS in
`awe-client-angular` or `awe-client-react`, a mock-up or a prototype. The AWE 5 design system (Aura-inspired, light
and dark, primary + surface themes, comfortable and compact density) is the source of truth for both engines.

## Hard Rules

- Style only with `--awe-*` tokens. No colour literals (`#`, `rgb(`, `hsl(`) and no bare `px` sizes outside the
  token file.
- Read semantic (`--awe-color-*`, `--awe-space-*`, `--awe-radius-*`, `--awe-shadow-*`, `--awe-font-*`,
  `--awe-motion-*`) or component tokens (`--awe-<component>-<property>`); never primitives (`--awe-palette-*`).
- No mode or density selectors in component CSS (`.dark …`, `[data-awe-density] …`): light/dark values live in the
  token file with `light-dark()`, density values in size tokens.
- A new token is added to the token file (pointing to a semantic token), documented in the token reference and
  implemented in **both** engines; never define tokens inside a component.
- Text contrast is at least 4.5:1 in light and dark with any primary; every interactive element has a visible
  `:focus-visible` ring and full keyboard support; motion stops with `prefers-reduced-motion`.
- Use the existing components (listbox, dialog, popover, side panel, toast, segmented…) instead of ad-hoc markup;
  never a native `<select>` popup.
- Typography is Inter on the type scale; icons are Lucide (XML `fa-*` names map to Lucide equivalents).
- Do not add PrimeUI-licensed packages (`primereact` 11, `@primereact/*`, `@primeuix/themes` >= 3); the React
  engine stays on PrimeReact 10.

## Decision Gates

| Situation | Action |
|-----------|--------|
| Code in an engine before the AWE 5 token file has landed in it | Use only the tokens that exist today (`guides/design-tokens`); do not invent AWE 5 names in engine code |
| Design, mock-up or prototype work | Use the AWE 5 tokens of the prototype `tokens.css` |
| A colour, size or motion value has no token | Propose a component or semantic token; do not hardcode |
| A product needs another brand colour | A palette name or a hex for primary/surface (the hex generates the scale, AA kept); never override semantic tokens |
| New element listed in the roadmap | Follow its entry in the components page (issue number) |

## Execution Steps

1. Read the relevant page in `references/design-system.md` (foundations, layout, components) before designing.
2. Pick the component and tokens; list every state (hover, focus-visible, active, disabled, invalid, loading, empty).
3. Implement with tokens only, the same way in both engines.
4. Check it against the prototype in light and dark, comfortable and compact, with a dark (`indigo`) and a light
   (`amber`) primary.
5. Run the accessibility checklist and search the diff for literals.

## Output Contract

Return: the tokens and components used, any new token proposed (name, tier, value, why), the states covered, the
modes/densities/primaries checked, the accessibility checklist result and any deviation from the guide with its
reason.

## References

- `references/design-system.md` — map of the design system pages, the prototype and the token files.
