---
id: chart-parameters
title: Chart parameters
---

The `chart-parameter` element adds or overwrites Highcharts options of a chart. It is the extension point of the chart
XML: anything that the XML attributes do not cover (colors, data label styles, markers, gradient fills, stack names,
bar padding, tooltip formats...) is set with it.

The same element is used by both engines, but each one reads it in its own way:

* **AngularJS engine (Highcharts).** The options are merged into the Highcharts configuration as they are, so any
  option of the [Highcharts API](https://api.highcharts.com/highcharts) is valid.
* **React engine (Apache ECharts).** The server translates the options that have an ECharts counterpart into the
  `echartsModel` of the chart. Every other option is **ignored** and reported once, with a warning in the server log.
  The options still reach the Highcharts `chartModel` unchanged, so the same screen keeps working in the AngularJS
  engine.

This page lists which options are translated, which ones are approximated and which ones are ignored. The table is
derived from the translation table of the server (`HighchartsParameterTable` and `SeriesParameterRules` in
`awe-model`).

## The element

| Attribute | Use          | Type   | Description                                                                                            |
| --------- | ------------ | ------ | ------------------------------------------------------------------------------------------------------ |
| type      | **Required** | String | `string`, `integer`, `long`, `float`, `double`, `boolean`, `array`, `object` or `null`                 |
| name      | **Required** | String | Name of the Highcharts option. Use an empty name for the items of an `array`                           |
| value     | Optional     | String | Value of the option. `object` and `array` parameters hold nested `chart-parameter` elements instead   |

Nested `object` parameters build the path of an option: the option `dataLabels.style.fontSize` is written as three
nested parameters.

```xml
<chart-parameter type="object" name="dataLabels">
  <chart-parameter type="boolean" name="enabled" value="true"/>
  <chart-parameter type="object" name="style">
    <chart-parameter type="string" name="fontSize" value="11px"/>
  </chart-parameter>
</chart-parameter>
```

## Where to put a parameter

The element that holds the `chart-parameter` decides the scope of the option, which is the root of its path:

| Parent element                    | Scope   | Root of the option path                                  |
| --------------------------------- | ------- | -------------------------------------------------------- |
| `chart`                           | chart   | The Highcharts configuration (`title`, `colors`, `plotOptions`, `chart`...) |
| `x-axis`, `y-axis`                | axis    | The axis (`labels`, `dateTimeLabelFormats`, `gridLineWidth`...) |
| `chart-tooltip`                   | tooltip | The tooltip (`headerFormat`, `useHTML`...)                |
| `chart-legend`                    | legend  | The legend                                                |
| `chart-serie`                     | series  | The series (`color`, `dataLabels`, `marker`, `stack`...) |

The options of a series can be written in the series itself or, as defaults for all the series or for the series of a
type, in `plotOptions.series.*` and `plotOptions.<type>.*` of the chart scope (for example `plotOptions.pie.size`).
The options of the series override those of `plotOptions.<type>`, which override those of `plotOptions.series`.

The raw options of every series are also available to the [format language](chart-format.md) as
`series.userOptions.<name>`, including custom names such as `fullname`.

## Translation table (React engine)

The tables use these marks:

* **Translated**: the option has an ECharts counterpart and is applied.
* **Approximated**: the closest ECharts behavior is used. The server logs a warning once.
* **Ignored**: the option is dropped. The server logs a warning once.

### Chart scope

| Option                                   | Result          | Notes                                                                                         |
| ---------------------------------------- | --------------- | --------------------------------------------------------------------------------------------- |
| `title` (string) and `title.text`        | Translated      | Text of the title                                                                             |
| `title.align`                            | Translated      | Horizontal position of the title (`left`, `center`, `right`)                                  |
| `title.verticalAlign`                    | Translated      | Vertical position of the title (`top`, `middle`, `bottom`)                                    |
| `title.y`                                | Translated      | Vertical offset in pixels. ECharts cannot offset a title that is aligned to the middle, so the client applies it |
| `colors`                                 | Translated      | Palette of the chart. Replaces the default palette                                            |
| `plotOptions.pie.size`                   | Translated      | Outer radius of the pie (percentage or pixels)                                                |
| `plotOptions.pie.innerSize`              | Translated      | Inner radius of a donut. It is relative to `size`, like in Highcharts                         |
| `chart.alignTicks`                       | Translated      | The ticks of the secondary value axes are aligned with the ones of the first axis             |
| `chart.alignThresholds`                  | Approximated    | Approximated by aligned ticks (`alignTicks`)                                                  |
| `tickAmount` (chart root)                | Ignored         | Dropped without a warning: it has no effect in Highcharts either. Set it in the axis          |

### Series scope and `plotOptions`

Each of these options is valid in a `chart-serie` and in `plotOptions.series` and `plotOptions.<type>`.

| Option                                  | Result       | Notes                                                                                       |
| --------------------------------------- | ------------ | ------------------------------------------------------------------------------------------- |
| `dataLabels.enabled`                    | Translated   | Shows the data labels of the series                                                         |
| `dataLabels.format`                     | Translated   | [Format](chart-format.md) of the labels. HTML tags are removed                              |
| `dataLabels.distance`                   | Translated   | In a pie, a negative value puts the label inside the slice, otherwise it is the length of the leader line |
| `dataLabels.style.fontSize`             | Translated   |                                                                                             |
| `dataLabels.style.fontWeight`           | Translated   |                                                                                             |
| `dataLabels.style.color`                | Translated   |                                                                                             |
| `dataLabels.style.textShadow`           | Translated   | CSS form `<x> <y> [<blur>] <color>`. The keywords `contrast` and `none` are dropped         |
| `dataLabels.connectorColor`             | Translated   | Color of the leader line of a pie                                                           |
| `stack`                                 | Translated   | Name of the stack of the series. Series with the same name are stacked together             |
| `stacking`                              | Translated   | `normal` or `percent`. Other values are ignored                                             |
| `groupPadding`                          | Translated   | Gap between groups of columns. Converted to a percentage of the bar width                   |
| `pointPadding`                          | Translated   | Gap between the columns of a group. Converted to a percentage of the bar width              |
| `pointWidth`                            | Translated   | Width of the columns in pixels                                                              |
| `borderRadius`                          | Translated   | Pixels or percentage. The end of the bar that is away from zero is rounded, only in the outermost series of a stack |
| `color`                                 | Translated   | Color of the series                                                                         |
| `borderColor`                           | Translated   |                                                                                             |
| `colorByPoint`                          | Translated   | Each point takes a color of the palette                                                     |
| `allowPointSelect`                      | Translated   | Points can be selected with a click                                                         |
| `lineWidth`                             | Translated   |                                                                                             |
| `dashStyle`                             | Translated   | Highcharts names are reduced to `solid`, `dashed` or `dotted`                               |
| `fillColor`                             | Translated   | Color of the area                                                                           |
| `fillColor.linearGradient.x1`, `y1`, `x2`, `y2` | Translated | Direction of the gradient. Vertical, from top to bottom, by default                  |
| `fillColor.stops`                       | Translated   | Colors of the gradient: array of `[offset, color]` pairs. Each stop carries its own alpha   |
| `threshold`                             | Approximated | `0` and `-Infinity` are translated. Any other threshold fills the area from zero            |
| `marker.enabled`                        | Translated   | `false` hides the symbols of a line and makes the points of a scatter invisible             |
| `marker.radius`                         | Translated   |                                                                                             |
| `marker.fillColor`                      | Translated   |                                                                                             |
| `marker.lineColor`                      | Translated   |                                                                                             |
| `marker.lineWidth`                      | Translated   |                                                                                             |
| `enableMouseTracking`                   | Translated   | `false` removes the series from the tooltip and from the mouse interaction                  |
| `showInLegend`                          | Translated   | `false` leaves the series out of the legend                                                 |
| `tooltip.valueSuffix`                   | Translated   | Suffix of the values of the series in the tooltip. Replaces the one of `chart-tooltip`      |
| `minSize`, `maxSize`                    | Translated   | Range of the bubble sizes, in pixels or as a percentage                                     |

These options are valid only in a `chart-serie`:

| Option          | Result     | Notes                                                                                                  |
| --------------- | ---------- | ------------------------------------------------------------------------------------------------------ |
| `linkedTo`      | Translated | `:previous` or the id of an earlier series. A linked series hides and shows with the series it is linked to, and has no legend entry |
| `legendIndex`   | Translated | Position of the series in the legend                                                                   |
| `keys`          | Translated | Binds the values of a point to `name`, `y` and `drilldown`, for [drilldown](chart.md#drilldown) charts |
| `fullname`      | Translated | Custom key of the screens. It does not change the chart, the formats read it as `series.userOptions.fullname` |

### Axis scope

| Option                       | Result     | Notes                                                                                           |
| ---------------------------- | ---------- | ----------------------------------------------------------------------------------------------- |
| `dateTimeLabelFormats.<unit>` | Translated | Date pattern of the labels of a time axis for one unit: `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond` |
| `labels.format`              | Translated | [Format](chart-format.md) of the labels. The value is `{value}`                                |
| `gridLineWidth`              | Translated | `0` hides the grid lines                                                                        |
| `tickAmount`                 | Translated | Approximate number of ticks of the axis                                                         |

### Tooltip scope

| Option          | Result  | Notes                                                                                               |
| --------------- | ------- | --------------------------------------------------------------------------------------------------- |
| `useHTML`       | Translated | The pieces of the tooltip are html blocks (table rows) joined as they are                        |
| `headerFormat`  | Translated | [Format](chart-format.md) of the header, shown once                                              |
| `pointFormat`   | Translated | Same as the `point-format` attribute                                                              |
| `footerFormat`  | Translated | [Format](chart-format.md) of the footer, shown once                                              |
| `style.fontSize` | Translated | Font size of the tooltip                                                                         |
| `distance`      | Ignored | The ECharts tooltip follows the pointer at a fixed offset. The server logs a warning                |

### Legend scope

The options of a `chart-parameter` inside `chart-legend` are all **ignored**. Use the attributes of
[`chart-legend`](chart.md#legend-element).

### Everything else

Any option that is not in the tables is **ignored**. The first time the server finds it, it writes a warning:

```text
Highcharts chart-parameter 'plotOptions.series.shadow' has no ECharts translation yet (scope: chart); it is ignored by the ECharts model
```

Every option is reported once while the server runs (the server remembers up to 500 different options). Check the log
of the application after you migrate a screen, and either remove the options that are reported or change the
chart to use the attributes of the XML.

### Approximations that the server also reports

| Case                                                                     | Result in the React engine                                    |
| ------------------------------------------------------------------------ | ------------------------------------------------------------- |
| Series type `arearange` and `areasplinerange`                            | Drawn as a filled line (warning)                              |
| A series `type` that is not in the table of chart types                   | Drawn as a line (warning)                                     |
| `crosshairs="yAxis"` in `chart-tooltip`                                  | Drawn as the pair of crosshairs of both axes (no warning)     |

## Examples

**Change the format of the labels of a date axis.**

```xml
<x-axis label="Dates" type="datetime">
  <chart-parameter type="object" name="dateTimeLabelFormats">
    <chart-parameter type="string" name="day" value="%Y-%m-%d"/>
  </chart-parameter>
</x-axis>
```

**Change the position of the title of a chart.**

```xml
<chart-parameter type="object" name="title">
  <chart-parameter type="string" name="align" value="center"/>
  <chart-parameter type="string" name="verticalAlign" value="middle"/>
  <chart-parameter type="integer" name="y" value="50"/>
</chart-parameter>
```

**Change the plot options of the pies of a chart.**

```xml
<chart-parameter type="object" name="plotOptions">
  <chart-parameter type="object" name="pie">
    <chart-parameter type="string" name="size" value="75%"/>
    <chart-parameter type="string" name="innerSize" value="40%"/>
    <chart-parameter type="object" name="dataLabels">
      <chart-parameter type="boolean" name="enabled" value="true"/>
      <chart-parameter type="integer" name="distance" value="-20"/>
      <chart-parameter type="object" name="style">
        <chart-parameter type="string" name="fontWeight" value="bold"/>
        <chart-parameter type="string" name="color" value="white"/>
        <chart-parameter type="string" name="textShadow" value="0px 1px 2px black"/>
      </chart-parameter>
    </chart-parameter>
  </chart-parameter>
</chart-parameter>
```

**Set the palette of a chart.** The items of an array have an empty name.

```xml
<chart-parameter type="array" name="colors">
  <chart-parameter type="string" name="" value="rgba(201,229,134,0.9)"/>
  <chart-parameter type="string" name="" value="rgba(140,172,65,0.9)"/>
  <chart-parameter type="string" name="" value="rgba(47,111,143,0.9)"/>
</chart-parameter>
```
