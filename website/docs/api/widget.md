---
id: widget
title: Widgets
---

:::tip See it live

Try the file and PDF viewers with live controls in the [component catalog](../components/file-viewer.mdx): the real AWE widgets, a sample file and the XML that produces them. The [carousel and the help viewer](../components/widgets.mdx) are there too.

:::

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
- **React client (`awe-react-client`)**: the `type` resolves against a client-side widget registry. Applications register their own components with `registerWidget(type, component)` (exported by `awe-react-client`), so widgets are extensible at the application level with no changes to the framework core. See the [Custom widgets (React) guide](../guides/custom-widgets-react).
- If no component matches `type`, the client renders a graceful placeholder (`The widget <type> has not been created yet.`) instead of failing.

To build and register your own widget, see the [Custom widgets guide](../guides/custom-widgets) (Angular) or the [Custom widgets (React) guide](../guides/custom-widgets-react).

## Built-in widgets

The Angular client ships these widget types out of the box:

| `type`         | Description                          |
| -------------- | ------------------------------------ |
| `file-manager` | File manager / browser               |
| `pdf-viewer`   | PDF document viewer                  |
| `log-viewer`   | Streaming log viewer                 |
| `file-viewer`  | Text file viewer (both engines)      |
| `help-viewer`  | Help content viewer                  |
| `carousel`     | Image / content carousel             |
| `month-view`   | Read-only month of the dates of a grid (both engines) |

### React client

The React client (`awe-react-client`) resolves the same `type` against a client-side registry and ships the same built-in widget types. Applications add their own by calling `registerWidget(type, component)` (exported by `awe-react-client`) once at startup; the built-in widgets use that same mechanism. The widget node attributes (`id`, `style`, ...) are passed to the component as props. See the [Custom widgets (React) guide](../guides/custom-widgets-react).

### Log viewer

The `log-viewer` widget (React client) streams a log: a toolbar with a search field, a filter toggle (only the lines
that match), the matches count and, at its right end, the **Follow the end** switch (`LOG_VIEWER_FOLLOW`). The switch is
on while the widget reads the new lines every `autorefresh` seconds and scrolls to the last one; turning it off stops
reading. The search placeholder is `LOG_VIEWER_SEARCH`.

The answer of a read may carry other client actions besides the lines (`log-delta`): they run on the viewer, in both
engines. The server stops the following with an `update-controller` action without target that sets `autorefresh` to
`0` (the scheduler sends it once the execution of the log has ended): the switch turns off and the reads stop.

### File viewer

The `file-viewer` widget shows the text of a file inside a `<pre>`. It works in both the Angular and the React client. On
load it posts the screen values to `server-action`/`target-action` (for example `file` / `text`) and shows the answer
when the server returns status 201 with a content. `autoload` reads the file when the screen starts and `autorefresh`
(seconds) reads it again periodically. The widget parameter `stickBottom` (`boolean`) keeps the view at the end of the
file while the user is at the bottom (and on the first load).

### Month view

The `month-view` widget draws a read-only month with the dates of a grid of the same screen struck through, and the
strip of the twelve months of the year with the number of dates of each one. It works in both the Angular and the React
client. It makes no server call: it reads the current rows of the grid on the client, the rows added or changed and not
saved yet included (the rows deleted and not saved yet are left out), and draws again when they change.

```xml
<grid id="GrdCalDat" multioperation="true" ...>
  <column name="Dat" label="PARAMETER_DATE" component="date" />
  <column name="Nam" label="PARAMETER_NAME" component="text" />
</grid>
<widget type="month-view" id="CalMth">
  <widget-parameter type="string" name="grid" value="GrdCalDat" />
  <widget-parameter type="string" name="dateColumn" value="Dat" />
  <widget-parameter type="string" name="nameColumn" value="Nam" />
</widget>
```

| Parameter    | Required | Description                                                                                 |
| ------------ | -------- | ------------------------------------------------------------------------------------------- |
| `grid`       | Yes      | Id of the grid of the same screen whose rows give the dates.                                |
| `dateColumn` | Yes      | Column of the grid with the date (`dd/mm/yyyy`, or `yyyy-mm-dd`).                           |
| `nameColumn` | No       | Column with the name of each date: the tooltip and part of the accessible name of the day.  |
| `legend`     | No       | Locale key of the legend under the month (`MONTH_VIEW_LEGEND` by default).                  |

- It opens on the month of the next date from today (today included), else on the month of today, and follows the rows
  of the grid until the user moves to another month with the previous and next buttons (`MONTH_VIEW_PREVIOUS`,
  `MONTH_VIEW_NEXT`) or a month of the strip (`MONTH_VIEW_STRIP`, it shows that month of the year shown).
- The week starts on Monday; the names of the months and of the days follow the language of the application.
- A date of the grid is a struck-through day. Its state is told by the line and by its accessible name and tooltip, not
  only by its colour: `6, excluded: Constitution Day`, with `(not saved)` for a row not saved yet (`MONTH_VIEW_EXCLUDED`,
  `MONTH_VIEW_UNSAVED`). Each month of the strip is a toggle button named with its count (`MONTH_VIEW_MONTH_DATES`).

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
