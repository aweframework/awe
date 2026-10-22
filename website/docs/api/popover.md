---
id: popover
title: Popover
---

A popover is a [dialog](dialog.md) anchored to a control of the screen: it opens next to that control (below it by
default, or above it, at its left or at its right), with a header, a body and an optional footer. Use it for short
content linked to a control (a quick edit, a few options, notes) without covering the screen. It is opened and closed
exactly like a dialog: with a button-action of type `dialog` (see [button actions](button.md#button-actions)), and with
the `close` and `close-cancel` actions. Both web engines (AngularJS and React) support it.

<img alt="Popover" src={require('@docusaurus/useBaseUrl').default('img/popover.png')} />

---

## XML skeleton

```xml
<popover id="[popover-identifier]" label="[popover-label]" icon="[popover-icon]" style="[popover-style]"
         anchor="[control-identifier]" position="[bottom|top|left|right]" max-width="[css-length]"
         on-close="[on-close]" deferred="[deferred]">
  [define the content of the popover]
  <tag type="div" style="modal-footer">
    [footer buttons]
  </tag>
</popover>
```

## Popover structure

| Element                        | Use          | Multiples instances | Description                                              |
|--------------------------------|--------------|---------------------|----------------------------------------------------------|
| [popover](#popover-attributes) | **Required** | No                  | Global node of the popover. Defines the popover attributes |

## Popover attributes

| Attribute | Use          | Type    | Description                                                                                                                    | Values                                                                                           |
|-----------|--------------|---------|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| id        | Optional     | String  | Popover identifier. The `dialog`, `close` and `close-cancel` actions target it. It is unique among the dialogs, side panels and popovers of the screen |                                     |
| anchor    | **Required** | String  | Identifier of the control of the screen the popover is anchored to (a button, a criterion, a grid...)                          |                                                                                                  |
| position  | Optional     | String  | Side of the anchor where the popover opens                                                                                     | `bottom` (default), `top`, `left`, `right`                                                       |
| max-width | Optional     | String  | Maximum width of the popover, which takes the width of its content up to it                                                    | A CSS length, for instance `30rem`, `320px` or `30vw`. Default `20rem`                           |
| label     | Optional     | String  | Title of the header                                                                                                            | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                       |
| icon      | Optional     | String  | Icon of the header (AngularJS engine)                                                                                          | **Note:** You can check all iconsets at [icons](icons.md) screen                                 |
| style     | Optional     | String  | Style of the popover (css classes)                                                                                             | CSS classes separated by space (`' '`)                                                           |
| on-close  | Optional     | String  | Behaviour of the stack after closing the popover with its close button, Esc or a click outside                                 | `accept` (default), `reject`- Reject cancels the stack, accept continues executing stack actions |
| deferred  | Optional     | Boolean | Load the data of the components of the popover the first time it is opened, not with the screen. See [deferred loading](dialog.md#deferred-loading) | `true`, `false` (default)                                                   |

## Opening and closing

The popover opens with a `dialog` action whose target is the popover, as a dialog does. Usually the control that opens
it is its anchor:

```xml
<button id="ButPopBottom" label="BUTTON_POPOVER_BOTTOM" icon="arrow-down">
  <button-action type="dialog" target="PopBottom"/>
</button>
```

It opens on the side of its anchor set in `position` and follows it when the page scrolls or the window is resized. When
the anchor is not on the screen, it opens in the middle of the window.

It closes:

- with a `close` action (the actions after the `dialog` action go on) or a `close-cancel` action (they are cancelled),
  usually in the buttons of its footer;
- with the close button of its header, with Esc or with a click outside it. A click on its anchor closes it too, and
  does not open it again. These follow the `on-close` attribute: `accept` (default) behaves as `close`, `reject` as
  `close-cancel`.

A click on the lists and calendars opened from the controls of the popover is not a click outside it.

## Footer

Put the buttons of the popover in a child tag with the `modal-footer` style.

## Content

The popover takes the width of its content, up to `max-width`. A grid inside it needs a width and a height: give it the
`col-xs-12` style and a height style (`height-md`, `height-lg`...), as in the [example](#example).

## Focus

While the popover is open, the keyboard focus stays inside it: Tab from its last control goes back to the first one and
Shift+Tab from the first one goes to the last one. When the popover closes, the focus goes back to the control that
opened it.

## Example

A deferred popover below its button with two criteria, a grid and a footer with Cancel and Accept (the select and the
grid load their data the first time it opens), and a deferred popover at the right of its button, up to 30rem wide,
which rejects the stack when it is dismissed:

```xml
<tag source="center">
  <window style="expand" label="SCREEN_TITLE_POPOVER" expandible="vertical">
    <tag type="div" style="panel-body">
      <button id="ButPopBottom" label="BUTTON_POPOVER_BOTTOM" icon="arrow-down">
        <button-action type="dialog" target="PopBottom"/>
      </button>
      <button id="ButPopRight" label="BUTTON_POPOVER_RIGHT" icon="arrow-right">
        <button-action type="dialog" target="PopRight"/>
      </button>
    </tag>
    <tag type="div" style="panel-body">
      <criteria id="CrtPopResult" label="PARAMETER_POPOVER_RESULT" component="text" readonly="true"
                style="col-xs-12 col-sm-6"/>
    </tag>
  </window>

  <popover id="PopBottom" label="POPOVER_TITLE_BOTTOM" anchor="ButPopBottom" max-width="40rem" deferred="true">
    <tag type="div" style="panel-body">
      <criteria id="CrtPopName" label="PARAMETER_NAME" component="text" style="col-xs-12 col-sm-6"/>
      <criteria id="CrtPopUser" label="PARAMETER_USER" component="select" initial-load="query"
                target-action="TestComponentInitialLoadQuery" optional="true" style="col-xs-12 col-sm-6"/>
      <grid id="GrdPopThemes" style="col-xs-12 height-lg" initial-load="query" server-action="data"
            target-action="ThemeStyleTst" max="5">
        <column label="PARAMETER_THEME" name="name" align="left" charlength="20"/>
        <column label="PARAMETER_POPOVER_THEME_ID" name="id" align="right" charlength="10"/>
      </grid>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPopBottomCancel" label="BUTTON_CANCEL" icon="times">
        <button-action type="close-cancel" target="PopBottom"/>
      </button>
      <button id="ButPopBottomAccept" label="BUTTON_ACCEPT" icon="check">
        <button-action type="close" target="PopBottom"/>
        <button-action type="value" target="CrtPopResult" value="accepted"/>
      </button>
    </tag>
  </popover>

  <popover id="PopRight" label="POPOVER_TITLE_RIGHT" anchor="ButPopRight" position="right" max-width="30rem"
           on-close="reject" deferred="true">
    <tag type="div" style="panel-body">
      <criteria id="CrtPopNotes" label="PARAMETER_DESCRIPTION" component="textarea" style="col-xs-12"/>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPopRightClose" label="BUTTON_CLOSE" icon="times">
        <button-action type="close" target="PopRight"/>
      </button>
    </tag>
  </popover>
</tag>
```

With the [builder module](../builder-module.md), `PopoverBuilder` builds a `<popover>`:

```java
new PopoverBuilder()
  .setAnchor("ButPopRight")
  .setPosition(PopoverPosition.RIGHT)
  .setMaxWidth("30rem")
  .setOnClose(OnClose.REJECT)
  .setDeferred(true)
  .setId("PopRight")
  .setLabel("POPOVER_TITLE_RIGHT");
```
