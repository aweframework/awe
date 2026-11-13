# AWE 5 visual reference prototype

Static, no-build prototype of the AWE 5 redesign (issue #883, task T4b): the token architecture (decision D5), the new
application shell and the AWE components with an Aura-like look, in light and dark mode. It is a reference to validate
the look before the design guide and the implementation in the AngularJS and React engines; it is not shipped.

## Open it

```bash
python3 -m http.server 8899 -d website/static/design-system/prototype
```

Then browse <http://localhost:8899/> (sample application), <http://localhost:8899/components.html> (the new AWE 5
elements of the roadmap, redesigned) and <http://localhost:8899/tokens.html> (token reference).
It is also published with the website at `/design-system/prototype/` (Docusaurus static files) and linked from
the Design system section of the documentation. A local HTTP server is needed because the icon sprite is referenced by URL; everything works offline.

## Files

| File | Content |
| --- | --- |
| `tokens.css` | The three token tiers. The only file that declares colours. |
| `prototype.css` | Shell and components. Reads `var(--awe-*)` only. |
| `shell.js` | Application shell shared by the pages: brand slot, three-level side menu (search, rail flyout), header. |
| `prototype.js` | Demo behaviour, the Theme panel and the listbox used by every select, multi-select and autocomplete. |
| `components.js` | Behaviour of the `components.html` demos. |
| `index.html` | Sample screen: Customer list of the fictitious customer application “Operations Hub” (Veloria Logistics). |
| `login.html` | Sign-in page: form, AWE brand image panel (configurable image slot), states, password change (#849). |
| `login.js` | Behaviour of the login page and its prototype state switcher. |
| `components.html` | New AWE 5 elements grouped by area, each with its issue number, a live example and its behaviour. |
| `tokens.html` | Palettes, semantic colours (light and dark side by side), type, spacing, radius, elevation, all tokens. |
| `assets/` | Inter (latin subset) and the Lucide icon sprite, with their licences. See `THIRD-PARTY.md`. |

## Theme

The floating **Theme** button only changes `<html>`:

- mode: class `light` or `dark`; with neither, the operating system decides (`prefers-color-scheme`);
- `data-awe-primary="emerald|green|lime|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose|red|orange|amber|yellow"`;
- `data-awe-surface="slate|gray|zinc|neutral|stone"`; without it, slate in light and zinc in dark;
- `data-awe-density="comfortable|compact"`;
- *Custom* primary or surface (hex field, colour picker, quick examples): `data-awe-primary="custom"` /
  `data-awe-surface="custom"` plus the scale generated from the hex (OKLCH, `prototype.js`) as inline
  `--awe-primary-*` / `--awe-surface-*` properties on `<html>`;
- `data-proto-brand="wide|mark|none"` (prototype only): wide customer logo, square customer mark with the product name,
  or no customer logo (AWE logo). The collapsed rail always shows the square mark;
- `data-proto-login-image="brand|gradient|none"` (prototype only, login page): AWE brand panel, plain brand gradient, or no
  image panel. A product image goes in `--awe-login-image` (a `url()`) or the `<img class="login__img">` slot.

The choice is kept in `localStorage` (`awe-prototype-theme`) so both pages share it.

Browser baseline: current Chrome, Edge, Firefox and Safari (`light-dark()`, `color-mix()`, native
`<dialog>` and `popover`).
