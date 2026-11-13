---
id: include
title: Include
---

An `include` picks a piece of another screen and places it inside the current one. Use it to reuse a part of a screen
(a dialog, a navigation bar, a group of buttons) instead of copying it in every screen that needs it.

## XML skeleton

```xml
<include target-screen="[screen-to-include]" target-source="[source-to-pick]" />
```

An `include` has no child elements. It can be placed wherever the schema accepts a screen element: inside a
[`tag`](tags.md), a [window](window.md), a [button](button.md), an [info](info.md) element...

## Attributes

| Attribute     | Use                  | Type   | Description                                                                          |
| ------------- | -------------------- | ------ | ------------------------------------------------------------------------------------ |
| target-screen | **Required**         | String | Identifier of the [screen](screen.md) to take the code from (the name of its XML file, without `.xml`) |
| target-source | **Required** (see below) | String | `source` of the [tag](tags.md) of that screen that holds the code to pick up     |

The schema marks `target-source` as optional, but the server rejects an `include` without it. The `screen.xsd` schema
also declares a `name` attribute for the `include`, but it has no effect.

## How it works

The server resolves the includes when it reads the screen, so **it works the same in the AngularJS and in the React
engines**: the client receives the screen with the included elements already in place.

* The server looks in `target-screen` for the **direct child** `tag` whose `source` matches `target-source` (the
  comparison ignores the case).
* It places the **children** of that tag where the `include` is. The tag itself, and the template of the included
  screen, are not copied.
* The code is copied as it is, so the identifiers of the included elements are the same in every screen that includes
  them. They must not clash with the identifiers of the screen that includes them.
* An included screen can include other screens, but a chain that comes back to the same `target-screen` and
  `target-source` is rejected with the error `ERROR_TITLE_NESTED_INCLUDE`.
* If the source does not exist in the screen, the screen fails to load with the error *Bad 'include' definition*, that
  names the screen, the `target-screen` and the `target-source`. If the `target-screen` does not exist, the error is the
  one of a screen that is not defined.

> **Note:** To make a piece reusable, put it in a `tag` with a `source` in a screen of its own. Screens like
> `info-buttons` only hold pieces to include. Remember that every source of a screen must be unique (see
> [template](template.md)).

## Examples

### Insert the print options dialog and the screen help dialog

The `modal` source of the screen `ScrCnf` includes two dialogs defined in other screens:

```xml
<tag source="modal">
  <include target-screen="PrnOpt" target-source="center" />
  <include target-screen="screen-help" target-source="center"/>
</tag>
```

`PrnOpt` defines `<tag source="center">` with the print options `dialog`, and `screen-help` the help `dialog`. After the
server resolves the includes, `ScrCnf` has both dialogs in its `modal` source.

### Insert pieces of a navigation bar

The screen `home_navbar` picks several sources of the screen `info-buttons` and places them inside a `tag`:

```xml
<tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
  <include target-screen="info-buttons" target-source="favourites"/>
  <include target-screen="info-buttons" target-source="siteModuleDatabase"/>
  <include target-screen="info-buttons" target-source="logout"/>
</tag>
```

The first example is the end of the screen `ScrCnf` and the second one a trimmed part of `home_navbar`, both in the
generic screens of AWE (`awe-framework/awe-generic-screens`). The React generic screens use the same pattern
(`home-sidebar`, `home-topbar`).

## Related pages

* [Template](template.md) and [tags](tags.md): the `source` attribute that makes a piece reusable.
* [Screen](screen.md): the element that holds the sources.
* [Dialog](dialog.md): the most common piece to include.

_Reviewed for AWE 5._
