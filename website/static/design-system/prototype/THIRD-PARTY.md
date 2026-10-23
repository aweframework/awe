# Third-party material

The AWE 5 visual reference prototype contains or derives from the following third-party material.

| Material | Version | Licence | Used as | Where |
| --- | --- | --- | --- | --- |
| Aura preset of `@primeuix/themes` (PrimeTek Informatics) | 2.0.3 | MIT | Reference **values** only: primitive palettes, border radius scale, semantic light/dark values, form field, list, navigation, overlay and component sizes. The CSS is our own. | `tokens.css` (tier 1 palettes and the values noted as Aura) |
| Inter, `@fontsource-variable/inter` (The Inter Project Authors) | 5.3.0 | SIL Open Font License 1.1 | Latin variable font (weights 100-900), unmodified | `assets/fonts/inter-latin-wght-normal.woff2`, licence in `assets/fonts/LICENSE-inter-OFL.txt` |
| Lucide icons, `lucide-static` (Lucide Icons and Contributors) | 1.61.0 | ISC (icons derived from Feather: MIT) | Subset of 134 icons combined into one SVG sprite; presentation attributes removed so CSS controls the stroke | `assets/icons/sprite.svg`, licence in `assets/icons/LICENSE-lucide.txt` |
| AWE logos (Almis Web Engine project) | repository files | Apache-2.0 (AWE project assets) | Unmodified copies: `awe-client-react/src/static/images/logos/logo-awe-nuevo.svg` (mark), `awe-logo2.svg` (wordmark, dark text) and `awe-client-angular/.../images/logo/logo-awe-nuevo2.svg` (wordmark, white text) | `assets/logos/awe-mark.svg`, `awe-logo-dark-text.svg`, `awe-logo-light-text.svg` |

## Notes

- The customer logo (“Veloria Logistics”) and the map illustration are original artwork made for this prototype; they
  do not represent a real brand or map data.
- `@primeuix/themes` 2.0.3 is published under the MIT licence: Copyright (c) 2025 PrimeTek (verbatim licence below). No PrimeUI-licensed package
  (`@primeuix/themes` >= 3, `@primeuix/styled` >= 1, `@primereact/*`, `primereact` 11) is used or referenced.
- Deviations from the Aura values are deliberate and documented in `tokens.css`: the light-mode primary fill shade is
  chosen per palette to reach WCAG AA for the label on it, the keyboard focus ring is 2px, form fields show a soft
  focus halo, and the invalid field border uses red-500 in the light mode.

### MIT licence of `@primeuix/themes` 2.0.3

```text
MIT License

Copyright (c) 2025 PrimeTek

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
