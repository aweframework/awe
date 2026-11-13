---
id: theme-preview
title: Theme preview
---

A theme preview shows how an AWE 5 theme looks **while it is being edited**, before it is saved: a page with a raised
card, a title, text with a link, a tag, a focused field and the primary and secondary buttons, drawn with the primary
colour, the surface and the mode chosen in the criteria it names. It updates as those criteria change. The
[theme screen](../guides/default-screens.md#themes) uses it in its side panel.

Both web engines (React and AngularJS) draw it, with the same sample and the same behaviour.

---

## XML skeleton

```xml
<theme-preview id="[preview-identifier]" label="[accessible-name]" style="[preview-style]"
               primary="[primary-palette-criterion]" primary-hex="[primary-hex-criterion]"
               surface="[surface-palette-criterion]" surface-hex="[surface-hex-criterion]"
               mode="[mode-criterion]" />
```

## Theme preview attributes

| Attribute   | Use      | Type   | Description                                                                         | Values                                                                     |
|-------------|----------|--------|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| id          | Optional | String | Preview identifier. The server generates one when it is missing                     |                                                                            |
| label       | Optional | String | Accessible name of the sample                                                       | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| style       | Optional | String | CSS classes of the preview                                                          | `col-xs-12`                                                                |
| primary     | Optional | String | Criterion with the primary palette                                                  | A palette name or `custom`                                                 |
| primary-hex | Optional | String | Criterion with the primary hex, read when the primary palette is `custom`           | `#RGB` or `#RRGGBB`                                                        |
| surface     | Optional | String | Criterion with the surface palette                                                  | A palette name, `auto` or `custom`                                         |
| surface-hex | Optional | String | Criterion with the surface hex, read when the surface palette is `custom`           | `#RGB` or `#RRGGBB`                                                        |
| mode        | Optional | String | Criterion with the mode                                                             | `light`, `dark` or `system`                                                |

## Behaviour

- **Isolated.** The sample is drawn in a frame of its own whose root gets the theme, as the application does on its own
  `<html>`. The theme of the application never changes, and closing or cancelling the panel leaves no trace.
- **Palettes.** A named palette is shown at once. A custom hex needs its generated palette: the preview asks the
  `ThmPrvPal` query (service `themePreviewPalette`, `ThemeService.previewPalette`) for it a moment after the last
  change, and shows it when it arrives.
- **Invalid colours.** An empty or invalid hex keeps the last valid preview and makes no request.
- **Mode.** `light` and `dark` show that mode. `system` follows the device, and a switch above the sample shows it in
  the light or the dark mode, to check both.
- **Density.** The sample takes the density of the application.
- The sample is a picture: nothing in it can be focused or used.

## Example

The side panel of the theme screen (`theme-admin`) shows the preview after the colours and the mode:

```xml
<tag type="h3" style="col-xs-12 awe-form-section-title" label="SCREEN_TEXT_THEME_PREVIEW" icon="lucide:eye" />
<theme-preview id="ThmPrv" label="SCREEN_TEXT_THEME_PREVIEW_SAMPLE" style="col-xs-12" primary="PriPal"
               primary-hex="PriHex" surface="SrfPal" surface-hex="SrfHex" mode="Mde" />
```
