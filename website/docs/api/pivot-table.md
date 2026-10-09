---
id: pivot-table
title: Pivot Table
---

The pivot table is an analytic component. It loads the rows of a query and lets the user build a report from them: the
user drags the fields to the rows and to the columns, picks an aggregation (count, sum, average...) and a renderer
(table, heat map...), and the table is calculated in the browser.

<img alt="PivotTable" src={require('@docusaurus/useBaseUrl').default('img/PivotTable.png')} />

## When to use it

Use a pivot table when the user must **explore** a set of data (group it, total it, cross it) and you do not know in
advance which groups they will need. If the report is fixed, a [grid](grids.md) with the right query is simpler and
supports paging, editing and selection.

The pivot table works on **all the rows that the query returns**: the grouping and the totals are calculated by the
client, not by the server. Because of that:

* Set `max="0"` to load every row. When `max` is not set, the initial load asks the server for a page of rows, like a
  grid does (30 rows by default, see `awe.application.component.grid-rows-per-page` in [properties](../properties.md)),
  and the pivot would only use those rows.
* Keep the query to the fields the user needs. Every field of the query becomes a field of the pivot table.

## XML skeleton

```xml
<pivot-table id="[pivot-id]" initial-load="query" target-action="[query-id]" max="0"
             rows="[fields]" cols="[fields]" aggregator="[aggregator]" aggregation-field="[field]"
             style="expand"/>
```

A pivot table can contain these child elements: `context-button`, `context-separator` (see
[context menu](context-menu.md)) and `dependency` (see [dependencies](dependencies.md)).

## Attributes

The attributes are the ones of the `screen.xsd` schema (see [XSD tooling](../guides/xsd-tooling.md)).

### Identification and data loading

| Attribute     | Use          | Type    | Description                                                         | Values / default                                                                  |
| ------------- | ------------ | ------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| id            | **Required** | String  | Identifier of the pivot table. It must be unique in the screen      |                                                                                   |
| style         | Optional     | String  | CSS classes of the pivot table                                      | Use `expand` to fill the available space (see [layout](layout.md))                |
| initial-load  | Optional     | String  | Where the data comes from on the initial load                       | `query` (the usual one), `enum`, `value`                                          |
| server-action | Optional     | String  | Server action that reloads the data                                 | See the [server action list](actions.md#server-actions)                           |
| target-action | Optional     | String  | [Query](query-definition.md) that returns the data                  | Query identifier                                                                  |
| max           | Optional     | Integer | Maximum number of rows to load                                      | `0` loads every row. Default: the grid page size (30)                             |
| autoload      | Optional     | Boolean | Launches the `target-action` when the screen is initialized         | AngularJS engine only                                                             |
| autorefresh   | Optional     | Integer | Launches the `target-action` every X seconds                        | AngularJS engine only                                                             |

### Initial layout of the table

The user can change all of these in the browser. The attributes only set what the user sees when the table is first
drawn.

| Attribute              | Use      | Type    | Description                                     | Values / default                                                              |
| ---------------------- | -------- | ------- | ----------------------------------------------- | ----------------------------------------------------------------------------- |
| rows                   | Optional | String  | Fields that start in the rows                   | Field names separated by commas                                               |
| cols                   | Optional | String  | Fields that start in the columns                | Field names separated by commas                                               |
| renderer               | Optional | String  | Renderer selected at the start                  | `Table` (default), `Table Barchart`, `Heatmap`, `Row Heatmap`, `Col Heatmap`  |
| aggregator             | Optional | String  | Aggregator selected at the start                | See [aggregators](#aggregators). Default: `Count`                             |
| aggregation-field      | Optional | String  | Field that the aggregator works on              | A field of the query. Not needed for `Count`                                  |
| sort-method            | Optional | String  | How the values of a field are sorted            | `natural` (default), `absolute` (by absolute numeric value)                   |
| total-row-placement    | Optional | String  | Where the row with the totals is drawn          | `top`, `bottom` (default)                                                     |
| total-column-placement | Optional | String  | Where the column with the totals is drawn       | `left`, `right` (default)                                                     |

### Number format

| Attribute          | Use      | Type    | Description                        | Values / default                                                             |
| ------------------ | -------- | ------- | ---------------------------------- | ---------------------------------------------------------------------------- |
| decimal-numbers    | Optional | Integer | Number of decimals of the results  | Default: the decimals of the numeric options of the application, or `0`      |
| thousand-separator | Optional | String  | Thousands separator                | Default: the one of the numeric options of the application                   |
| decimal-separator  | Optional | String  | Decimal separator                  | Default: the one of the numeric options of the application                   |

The three attributes are only applied by the **custom aggregators** (`Custom Sum`, `Custom Average`...) of the
AngularJS engine. The standard aggregators format the numbers with the default format of the library, and the
custom aggregators exist only when the `aggregator` attribute is set.

## Aggregators

The `aggregator` attribute accepts these values:

| Group                  | Values                                                                                                                         |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Count                  | `Count`, `Count Unique Values`, `List Unique Values`                                                                           |
| Sum and statistics     | `Sum`, `Integer Sum`, `Average`, `Minimum`, `Maximum`, `Sum over Sum`, `80% Upper Bound`, `80% Lower Bound`                    |
| Fractions              | `Sum as Fraction of Total`, `Sum as Fraction of Rows`, `Sum as Fraction of Columns`, `Count as Fraction of Total`, `Count as Fraction of Rows`, `Count as Fraction of Columns` |
| Custom (number format) | `Custom Sum`, `Custom Average`, `Custom Minimum`, `Custom Maximum`, `Custom Sum over Sum`, `Custom 80% Upper Bound`, `Custom 80% Lower Bound` |

The custom aggregators are the same as the standard ones but they use the format defined by `decimal-numbers`,
`thousand-separator` and `decimal-separator`.

> **Note:** `Custom 80% Upper Bound` and `Custom 80% Lower Bound` do not apply the custom number format, and both
> calculate the upper bound. Use `80% Upper Bound` and `80% Lower Bound` instead.

## Example

The pivot table of the test screen `pivot-test` groups the rows of a query by the `Als` field and sums the `Prg1`
field. The user can change the groups, the aggregator and the renderer on the screen:

```xml
<window label="SCREEN_TEXT_DATA" style="expand" expandible="vertical">
  <tag type="div" style="panel-body expand scrollable-both" expandible="vertical">
    <pivot-table id="listaDatos" initial-load="query" target-action="QryUniTstId" max="0"
                 cols="Als" aggregation-field="Prg1" style="expand"/>
  </tag>
</window>
```

To reload the data (for example from a search button, as the test screen does) use the `filter` action on the pivot
table:

```xml
<button button-type="submit" label="BUTTON_SEARCH" icon="search" id="ButSch">
  <button-action type="filter" target="listaDatos"/>
</button>
```

This example is the pivot table of the test screen `pivot-test` (`awe-tests/awe-boot` and `awe-tests/awe-boot-react`),
with `max="0"` added.

### Changing the groups from the server

Three [client actions](actions.md#client-actions) change the table from a service or a dependency:

| Action                 | Parameter | Description                                                                           |
| ---------------------- | --------- | ------------------------------------------------------------------------------------- |
| `set-pivot-group-rows` | `rows`    | Sets the fields of the rows, as a comma-separated list                                |
| `set-pivot-group-cols` | `cols`    | Sets the fields of the columns, as a comma-separated list                             |
| `set-pivot-sorters`    | `sorters` | Sets the order of the values of each field                                            |

## AngularJS and React engines

The pivot table is drawn with a different library in each engine, and the XML attributes are not equivalent:

| Feature                                                              | AngularJS engine                      | React engine                                                  |
| -------------------------------------------------------------------- | ------------------------------------- | ------------------------------------------------------------- |
| Library                                                              | PivotTable.js (jQuery), included in AWE      | [react-pivottable](https://github.com/plotly/react-pivottable) 0.11 |
| `rows`, `cols`                                                       | Applied                               | Applied                                                       |
| `renderer`, `aggregator`, `aggregation-field`                        | Applied                               | **Not applied**: the table starts with the library defaults   |
| `sort-method`, `total-row-placement`, `total-column-placement`       | Applied                               | **Not applied**                                               |
| `decimal-numbers`, `thousand-separator`, `decimal-separator`, custom aggregators | Applied                   | **Not applied**: the `Custom ...` aggregators do not exist    |
| Actions `set-pivot-group-rows`, `set-pivot-group-cols`, `set-pivot-sorters` | Applied                        | Applied                                                       |
| Context menu (`context-button`)                                      | Available                             | Not available                                                 |
| `autoload`, `autorefresh`                                            | Applied                               | Ignored                                                       |
| Texts of the table                                                   | Translated (English, Spanish, Basque, French) | Always in English                                     |
| Maximum number of values listed in the filter of a field             | `awe.application.component.pivot-num-group` (5000) | 500 (the library default)                        |

In the React engine the user can still choose the renderer and the aggregator in the table, and the choices are
kept while the screen is open. If a screen must start with a given renderer or aggregator in both engines, it needs the
AngularJS engine today.

> **Note:** The differences of the React engine come from how the component passes the XML attributes to the library
> (`AwePivotTable.jsx` forwards them as they are, and the library expects other names). They are listed here so you can
> plan ahead; they do not break the screen.

## Related pages

* [Grids](grids.md): when the report is fixed or the rows must be edited or paged.
* [Query definition](query-definition.md): the query that feeds the table.
* [Layout](layout.md): how `style="expand"` makes the table fill its container.
* [Actions](actions.md): the `filter` action and the pivot actions.
* [Dependencies](dependencies.md) and [context menu](context-menu.md): the child elements.

_Reviewed for AWE 5._
