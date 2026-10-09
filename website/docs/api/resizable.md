---
id: resizable
title: Resizable
---

A resizable is a container whose size the user can change by dragging one of its sides. Use it to let the user decide how
much room a part of the screen takes, for example a list of items on the left of the detail that it controls.

## XML skeleton

```xml
<resizable directions="[directions]" style="[style]">
  ...
</resizable>
```

A resizable can contain any screen element: [tags](tags.md), [windows](window.md), [grids](grids.md), [charts](chart.md),
[criteria](criteria.md), other resizables...

## Attributes

| Attribute  | Use          | Type   | Description                                              | Values / default                                                                                  |
| ---------- | ------------ | ------ | -------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| directions | Optional (set it) | String | Sides of the container that can be dragged        | `right`, `left`, `top`, `bottom`. In the AngularJS engine you can set several, separated by spaces |
| style      | Optional     | String | CSS classes of the container                             | See [size and responsive styles](#size-and-responsive-styles)                                     |
| id         | Optional     | String | Identifier of the resizable. For reference purposes      |                                                                                                   |

The schema does not force `directions`, but without it the AngularJS engine draws no grabber, so the resizable cannot be
resized. Set it always.

The `screen.xsd` schema also declares `icon`, `label` and `expandible` for the resizable, but neither client uses
them.

> **Note:** A resizable needs an **initial width or height**, defined with a CSS class in `style` (for example the
> `col-xs-12 col-sm-4 col-lg-2` grid classes). The resizable changes that size when the user drags the side.

## Examples

### Resizable with a grabber to the right

The resizable of the test screen `MatTst`: a grid on the left that the user can widen or narrow, next to another grid
that takes the rest of the space.

```xml
<tag type="div" style="expand expandible-horizontal">
  <resizable directions="right" style="col-xs-12 col-sm-4 col-lg-2 no-padding no-resizable-xs">
    <grid id="MatSel" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
      <column label="COLUMN_SIT" sort-field="Als" name="Als5" charlength="25" />
    </grid>
  </resizable>
  <grid id="MatPrn" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
    <column label="COLUMN_SIT" sort-field="Als" name="Als6" charlength="25" />
  </grid>
</tag>
```

<img alt="resizable" src={require('@docusaurus/useBaseUrl').default('img/resizable.png')} />

The parent `tag` has `expandible-horizontal` so the resizable and the second grid share the width (see
[layout](layout.md)).

### Resizable with a grabber at the bottom

The `height-lg` style gives the resizable its initial height (see the note above).

```xml
<tag type="div" style="expand expandible-vertical">
  <resizable directions="bottom" style="no-resizable-xs height-lg">
    <grid id="MatSel" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
      <column label="COLUMN_SIT" sort-field="Als" name="Als5" charlength="25" />
    </grid>
  </resizable>
  <grid id="MatCol" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
    <column label="COLUMN_SIT" sort-field="Als" name="Als17" charlength="25" />
  </grid>
</tag>
```

Both examples are adapted from the screen `MatTst` of the test applications (`awe-tests/awe-boot` and
`awe-tests/awe-boot-react`): the first one keeps its attributes, the second one only changes the contents of the
resizable.

## Size and responsive styles

In the AngularJS engine the resizable has a minimum size of 160 px and it cannot grow over 99% of its parent. These
styles turn the grabber off when the screen is narrow, so on a phone the container uses its normal size:

| Style             | The resizable stops being resizable when the screen is      |
| ----------------- | ----------------------------------------------------------- |
| `no-resizable-xs` | narrower than 768 px                                        |
| `no-resizable-sm` | narrower than 992 px                                        |
| `no-resizable-md` | narrower than 1200 px                                       |
| `no-resizable-lg` | 1200 px wide or wider                                       |

When the user releases the grabber the client launches a resize, so grids and charts inside the screen adapt to their
new size.

## AngularJS and React engines

The two engines do not draw the resizable in the same way:

| Feature                      | AngularJS engine                                                                | React engine                                                                                     |
| ---------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| What is dragged              | The sides listed in `directions` of the container itself                        | The border **between** the children: each direct child of the resizable becomes a panel          |
| Several directions           | Allowed (`directions="right bottom"`)                                           | Not used: `top` and `bottom` split the children vertically, any other value splits them horizontally |
| One child                    | The container is resizable                                                      | There is nothing to drag: the border only exists between two or more children                     |
| `no-resizable-*` styles      | Applied                                                                         | Ignored                                                                                          |
| Library                      | Own directive                                                                   | [PrimeReact Splitter](https://primereact.org/splitter/)                                          |

If a screen must resize in both engines, put in the resizable **two or more children** that share the space: the React
engine splits them, and in the AngularJS engine they are stacked inside the container that the grabber resizes. Check
the result in both engines before you rely on it.

## Related pages

* [Layout](layout.md): how the container and its siblings share the space.
* [Tags](tags.md) and [window](window.md): the usual contents of a resizable.
* [Screens](screens.md): the list of screen elements.

_Reviewed for AWE 5._
