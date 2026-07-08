---
id: widget
title: Widgets
---

A **widget** embeds a custom JavaScript component into an AWE screen. It is the extension point for enriching the UI that AWE provides out of the box with your own framework components (and their styles), without having to enable them one by one in the framework.

Widgets are declared with the `<widget>` element inside any screen container (a `tag`, `window`, etc.):

```xml
<tag source="center">
  <widget type="file-manager" id="file-manager" style="expand"/>
</tag>
```

## How resolution works

The `type` attribute is the **key that resolves the client component**. The backend renders the widget as a custom element named after `type`:

```
<widget type="event-calendar" id="myCalendar"/>
  →  <awe-event-calendar event-calendar-id="myCalendar"></awe-event-calendar>
```

- **Angular client (`awe-client-angular`)**: the element resolves against the AngularJS directive registry. A directive named `aweEventCalendar` (element `awe-event-calendar`) renders the widget. Applications register their own directives in their bundle, so widgets are extensible at the application level with no changes to the framework core.
- If no component matches `type`, the client renders a graceful placeholder (`The widget <type> has not been created yet.`) instead of failing.

To build and register your own widget, see the [Custom widgets guide](../guides/custom-widgets).

## Built-in widgets

The Angular client ships these widget types out of the box:

| `type`         | Description                          |
| -------------- | ------------------------------------ |
| `file-manager` | File manager / browser               |
| `pdf-viewer`   | PDF document viewer                  |
| `log-viewer`   | Streaming log viewer                 |
| `help-viewer`  | Help content viewer                  |
| `carousel`     | Image / content carousel             |

## Attributes

| Attribute       | Required | Description                                                                                 |
| --------------- | -------- | ------------------------------------------------------------------------------------------- |
| `id`            | Yes      | Unique widget identifier (used as the component id on the client).                          |
| `type`          | No       | Component key that selects the client component (see [resolution](#how-resolution-works)).  |
| `component`     | No       | Javascript component name (advanced/legacy).                                                |
| `style`         | No       | CSS classes applied to the widget container (e.g. `expand` to fill the available space).    |
| `visible`       | No       | Whether the widget is visible (`true` / `false`).                                           |
| `help`          | No       | Locale key for the widget help text.                                                        |
| `help-image`    | No       | Help image path.                                                                            |
| `server-action` | No       | Server action to retrieve the widget data.                                                  |
| `target-action` | No       | Target action / query executed to feed the widget.                                          |
| `initial-load`  | No       | Initial load action executed on screen startup.                                             |

## Widget parameters

Use `<widget-parameter>` to pass configuration values to the client component. Parameters reach the component through its controller (`component.controller.parameters` in the Angular client):

```xml
<widget type="event-calendar" id="myCalendar" style="expand">
  <widget-parameter type="string" name="initialView" value="dayGridMonth"/>
  <widget-parameter type="boolean" name="editable" value="true"/>
</widget>
```

| Attribute | Required | Description                                                                    |
| --------- | -------- | ------------------------------------------------------------------------------ |
| `type`    | Yes      | Parameter type: `string`, `label`, `boolean`, `integer`, `long`, `float`, `double`, `array`, `object`, `null`. |
| `value`   | Yes      | Parameter value (parsed according to `type`).                                  |
| `name`    | No       | Parameter name (key) as received by the component.                             |

Parameters of type `array` and `object` can nest further `<widget-parameter>` elements.
