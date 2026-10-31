---
id: window
title: Window
---

A window is a container with a title bar. It also can be maximized or restored, and it's very useful to sort groups of components in the screen.

<img alt="Window" src={require('@docusaurus/useBaseUrl').default('img/Window.png')} />

## XML skeleton

```xml 
<window id="[window-identifier]" label="[window-label]" style="[window-style]"
        icon="[window-icon]" expandible="[expand-orientation]" maximize="[maximize-window]"
        advanced-mode="[advanced-mode]">
  <tag type="div" style="panel-body">
    ...
    <advanced>...</advanced>
  </tag>
  <tag type="div" style="panel-footer">...</tag>
  ...
  <grid>...</grid>
  ...
  <chart>...</chart>
  ...
</window>
```

## Window structure

| Element                      | Use          | Multiples instances   | Description                                                                                       |
|------------------------------|--------------|-----------------------|---------------------------------------------------------------------------------------------------|
| [window](#window-attributes) | **Required** | No                    | Global node of window. Describes the window attributes                                            |
| [tag](tags.md)               | Optional     | Yes                   | A [tag](tags.md) list inside the window, usually with styles like `panel-body` and `panel-footer` |
| [grid](grids.md)             | Optional     | No                    | A [grid](grids.md) inside the window                                                              |
| [chart](chart.md)            | Optional     | No                    | A [chart](chart.md) inside the window                                                             |
| [advanced](#advanced-search) | Optional     | No                    | The advanced block of a search window: its advanced criteria, shown on demand. See [advanced search](#advanced-search) |

> **Note** There are three special styles you can use as tag styles on windows:
> * `panel-body`: A special style to define the content of a window. It adds margins to the content.
> * `panel-footer`: A special style to define the bottom of a window. It is recommended to put buttons inside.
> * `expand-maximize`: A special style usually combined with `panel-body` which expands the content of the window when it is maximized.

## Window attributes

| Attribute    | Use        | Type     | Description                                    | Values                                                                     |
|--------------|------------|----------|------------------------------------------------|----------------------------------------------------------------------------|
| id           | Optional   | String   | Window identifier. For reference purposes      |                                                                            |
| label        | Optional   | String   | Window title                                   | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| style        | Optional   | String   | Window CSS classes                             | **Note:** Here you can use `expand` class to set the window as expandible  |
| icon         | Optional   | String   | Icon identifier                                | **Note:** You can check all iconsets at [icons](icons.md) screen           |
| expandible   | Optional   | String   | How to [expand](layout.md) the window children | `vertical` (default), `horizontal`                                         |
| maximize     | Optional   | Boolean  | Whether to show the maximize icon or not       | **Note:** Only a window with a `label` can be maximized                    |
| advanced-mode | Optional  | String   | How the window shows its [advanced block](#advanced-search) | `inline` (default), `side-panel`. **Note:** Only used when the window holds an `advanced` block |
| help         | Optional   | String   | Help text for the criterion                    | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| help-image   | Optional   | String   | Help image for the criterion                   | This **must** be a image path                                              |

## Examples

### Expandible window with grid (maximizable)

```xml 
<window style="expand" maximize="true" label="SCREEN_TEXT_DATA" icon="list">
  <grid ...>...</grid>
</window>
```

<img alt="expandible window with grid" src={require('@docusaurus/useBaseUrl').default('img/expandible window with grid.png')} />

### Static window with content and buttons zone

```xml 
<window label="SCREEN_TEXT_CRITERIA" icon="filter">
  <tag type="div" style="panel-body">...</tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">...</tag>
  </tag>
</window>
```
<img alt="Static_window_with_buttons_and_panel" src={require('@docusaurus/useBaseUrl').default('img/Static_window_with_buttons_and_panel.png')} />

## Advanced search

A search window can show only the criteria users need every day and keep the others at hand. Put the essential criteria
first and, right after them, an `<advanced>` block with the less frequent ones (and any layout tags they need). Choose
with `advanced-mode` how the window shows the block:

* `inline` (default): an **Advanced filters** button shows the block below it, inside the window, and **Hide advanced
  filters** closes it. The button does not move when the block opens. The window remembers, per user and screen,
  whether the block was left open, and shows it that way the next time the screen opens.

<img alt="Advanced filters collapsed, with a hidden filter in effect" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-inline-collapsed.png')} />

<img alt="Advanced filters expanded below the button" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-inline-expanded.png')} />

* `side-panel`: the same **Advanced filters** button opens the block in a side panel titled with the window label. **Reset to
  defaults** returns the advanced criteria to their default values, **Cancel** (or Escape) undoes the changes made in
  the panel and **Show results** closes it and launches the search.

<img alt="Advanced filters in a side panel" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-side-panel.png')} />

The button is the first element of a row that the window draws where the `<advanced>` block is, so the essential
criteria stay above it and the advanced ones below it (or in the panel). The button shows how many advanced criteria
have a value different from their default. After a divider, while there are filters in effect, the row lists as
**Filtered by** chips every criterion of the window whose value differs from its default, also the advanced ones that
are hidden (marked with an icon). Each chip clears its criterion back to its default value, and **Clear all**, at the
end of the row, clears them all. If a hidden advanced criterion fails a validation, the window shows it.

**Show results** and Enter in any criterion of the window launch the submit button of the window (`button-type="submit"`).
The title bar and the footer of the window stay as you write them, so the search and reset buttons are ordinary
buttons with their own identifiers.

The advanced criteria are always part of the screen, so dependencies, validation, default values, initial loads,
session and variable values, printing and export work in the same way when they are hidden. Declare at most one
`<advanced>` block per window; it only has effect inside a window. A window without it does not change.

```xml
<window id="WinUsrCrt" label="SCREEN_TEXT_CRITERIA" icon="filter" advanced-mode="side-panel">
  <tag type="div" style="panel-body">
    <criteria id="CrtUsr" component="suggest" label="PARAMETER_USER" .../>
    <criteria id="CrtPro" component="suggest" label="PARAMETER_PROFILE" .../>
    <advanced>
      <criteria id="CrtThm" component="suggest" label="PARAMETER_THEME" .../>
      <criteria id="CrtBlk" component="select" label="PARAMETER_BLOCKED" .../>
    </advanced>
  </tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">
      <button id="ButRst" button-type="reset" label="BUTTON_RESET" icon="eraser"/>
      <button id="ButSch" button-type="submit" label="BUTTON_SEARCH" icon="search">
        <button-action type="filter" target="GrdUsrLst"/>
      </button>
    </tag>
  </tag>
</window>
```

## AngularJS and React engines

Both engines handle `expandible` and `maximize` in the same way:

* `expandible` sets the direction in which the window shares the space among its children (`vertical` by default).
* `maximize="true"` shows a button in the title bar that maximizes the window over the content of the screen and
  restores it. The window fills the content area, keeping the menu and the top bar visible, and its sibling windows are
  hidden while it is maximized. The Restore button brings it back; there is no Escape key shortcut. The content with the
  `expand-maximize` style takes the free space while the window is maximized.

The differences are only visual: the maximize button shows the Lucide `expand` and `shrink` icons in the AngularJS
engine and `maximize` and `minimize` in the React engine, and the React engine shows the `icon` of the window at the right of the title bar.
