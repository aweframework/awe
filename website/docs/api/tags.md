---
id: tags
title: Tag
---

A `tag` is the basic building block of a screen. It does two jobs:

* **Structure.** With a `type` it generates an HTML element (`div`, `span`, `h3`...) that groups other elements, applies
  CSS classes and, with `expandible`, [lays out](layout.md) its children.
* **Source point.** With a `source` it fills one of the places of the [template](template.md) of the screen (`center`,
  `buttons`, `modal`, `hidden`), or it marks a piece that other screens can pick with an [`include`](include.md).

## XML skeleton

```xml
<tag type="[type]" label="[label]" style="[style]" id="[id]" source="[source]" expandible="[expand-direction]">
  ...
</tag>
```

A tag can contain any screen element: other tags, [windows](window.md), [grids](grids.md), [criteria](criteria.md),
[buttons](button.md), [includes](include.md)...

There is a special form that only contains text:

```xml
<tag type="p"><text>[text]</text></tag>
```

## Tag attributes

| Attribute  | Use      | Type   | Description                                              | Values                                                                      |
| ---------- | -------- | ------ | -------------------------------------------------------- | --------------------------------------------------------------------------- |
| source     | Optional | String | Source point of the template that the tag fills          | `center`, `buttons`, `modal`, `hidden`... see [template](template.md)       |
| type       | Optional | String | HTML element to generate                                 | `div`, `span`, `p`, `ul`, `h3`... any HTML element name                     |
| label      | Optional | String | Text of the tag                                          | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)  |
| style      | Optional | String | CSS class or classes to apply to the element             |                                                                             |
| id         | Optional | String | Tag identifier. Useful for external references           |                                                                             |
| expandible | Optional | String | How to [expand](layout.md) the children of the tag       | `vertical`, `horizontal`                                                    |

A `source` can be used **once** per screen, and the tags that have a `source` are direct children of the `screen`.

## What the tag generates

* **With `type`** the tag generates `<type id="..." class="...">...</type>`. The classes are the `style` plus
  `expandible-vertical` or `expandible-horizontal` when `expandible` is set.
* **Without `type`** the tag does not need to generate any element of its own. This is how you define a source point
  (`<tag source="center">...</tag>`) or group elements that must not be wrapped (see the differences below).
* **The text** of the tag is its `label` (translated with the locales of the application) or, when there is no `label`,
  the `<text>` element.
* **The children** are the elements inside the tag.

## AngularJS and React engines

| Case                                           | AngularJS engine                                                              | React engine                                                                    |
| ---------------------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| Tag without `type` (and without `source`)      | No HTML element: the children are placed in the parent                        | A `div` is generated                                                            |
| Tag with `label` **and** children              | Only the label is drawn (inside a `span`); the children are ignored           | The label, the `<text>` and the children are drawn, in this order               |
| `type` and `expandible` of a tag with `source` | Applied: the source tag generates its own element inside the template         | Not applied: the template uses the `style` and the children of the source tag   |

Because of the first two rows, write tags that behave the same in both engines:

* Give a `type` to every tag that has a `style` or an `expandible`, so it is a real element in both engines.
* Use a tag with `label` for text only. If you need text next to other elements, put each one in its own tag.
* Use `<tag source="...">` only to fill a source point. Put the `div` with `expandible` **inside** it.

## Examples

### Structure of a window

The `panel-body` and `panel-footer` styles give a [window](window.md) its content and its footer. This is the pivot table
screen of the test applications:

```xml
<window label="SCREEN_TEXT_CRITERIA">
  <tag type="div" style="panel-body">
    <criteria component="date" id="fecha" label="PARAMETER_DATE" style="col-xs-6 col-sm-3 col-md-2"/>
  </tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">
      <button button-type="submit" label="BUTTON_SEARCH" icon="search" id="ButSch">
        <button-action type="filter" target="listaDatos" />
      </button>
    </tag>
  </tag>
</window>
```

### Expandible tags

A tag with `expandible` shares the space among its children: the ones with the `expand` style take the free space, the
others keep their size (see [layout](layout.md)):

```xml
<tag type="div" style="panel-body expand" expandible="horizontal">
  <tag type="div" style="p-2 expand">
    <criteria label="SCREEN_TEXT_USER" component="text" id="gegtr" validation="required" style="col-xs-12" />
    <criteria label="SCREEN_TEXT_PASS" component="password" id="ertge" validation="required" style="col-xs-12" />
  </tag>
  <tag type="div" style="p-2 staticWidthSample">
    <criteria label="SCREEN_TEXT_USER" icon="user" component="text" id="etertg" validation="required" style="col-xs-12" />
  </tag>
</tag>
```

### Text tags

A tag with a `label` writes translated text, and the `<text>` form writes the text as it is:

```xml
<tag type="h3" label="SCREEN_TEXT_USER_DATA"/>
<tag type="p">
  <text>Lorem ipsum dolor sit amet.</text>
</tag>
```

### Source points

```xml
<screen template="window" label="SCREEN_TITLE_QUE">
  <tag source="hidden"></tag>
  <tag source="buttons">
    <button label="BUTTON_SEND" icon="exchange" id="ButSndSyn">
      <button-action type="server" server-action="maintain" target-action="TstQueSndSyn" />
    </button>
  </tag>
  <tag source="center">
    ...
  </tag>
</screen>
```

The examples are copies of parts of the test screens `pivot-test` (structure of a window), `layout` (expandible tags),
`settings` (the `h3` tag with a label), `accordion` (the `<text>` form, there without `type`) and `QueTst` (source
points) of `awe-tests/awe-boot-react`. `pivot-test` and `layout` are also in `awe-tests/awe-boot`.

## Related pages

* [Template](template.md): the source points of each template.
* [Layout](layout.md): the `expandible` attribute and the `expand` styles.
* [Include](include.md): pick a source of another screen.
* [Window](window.md): the usual container of tags with `panel-body` and `panel-footer`.
* [Screen](screen.md): the element that holds the source tags.

_Reviewed for AWE 5._
