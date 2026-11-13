package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.RADIUS;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.TITLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.AXIS;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.CHART;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.TOOLTIP;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.bool;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.fontSize;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.number;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.putIfPresent;

/**
 * Translation table of the Highcharts options that the screens set through {@code chart-parameter}.
 * <p>
 * Each row maps the path of one Highcharts option, relative to the element that holds the parameter, to its place
 * in the ECharts model. Supporting another option only needs another row in {@link #rules()}; options without a row
 * are dropped and reported once by {@link UnsupportedOptionReporter}.
 * </p>
 * <p>
 * {@code plotOptions} are defaults of the series: a row writes them to {@link ParameterTarget#seriesDefaults}, under
 * the Highcharts series type (or {@code series} for all of them), and the series builder merges them into each series
 * of that type. Pie sizes are stored as {@code radius}, a two items list [inner, outer] that the series builder
 * completes with the defaults of the chart type.
 * </p>
 * <p>
 * The options of the series and of the {@code plotOptions} are in {@link SeriesParameterRules}. Highcharts options
 * that ECharts does not have are approximated or dropped, and say so in the log: {@code chart.alignThresholds} is
 * approximated by aligned ticks, and the tooltip {@code distance} is dropped. The {@code tickAmount} of the chart
 * root has no effect in Highcharts either, so it is dropped without a warning.
 * </p>
 */
final class HighchartsParameterTable {

  private static final List<ParameterRule> RULES = buildRules();

  private HighchartsParameterTable() {
  }

  /**
   * Rows of the table
   *
   * @return Translation rules
   */
  static List<ParameterRule> rules() {
    return RULES;
  }

  private static List<ParameterRule> buildRules() {
    List<ParameterRule> rules = new ArrayList<>();
    chartRules(rules);
    rules.addAll(SeriesParameterRules.rules());
    axisRules(rules);
    tooltipRules(rules);
    return List.copyOf(rules);
  }

  private static void chartRules(List<ParameterRule> rules) {
    // Chart title: a plain string is the text, the rest places it
    rules.add(new ParameterRule(CHART, TITLE,
      (target, captures, value) -> child(target.options(), TITLE).put("text", String.valueOf(value))));
    rules.add(new ParameterRule(CHART, "title.text",
      (target, captures, value) -> child(target.options(), TITLE).put("text", String.valueOf(value))));
    rules.add(new ParameterRule(CHART, "title.align",
      (target, captures, value) -> child(target.options(), TITLE).put("left", String.valueOf(value))));
    rules.add(new ParameterRule(CHART, "title.verticalAlign",
      (target, captures, value) -> child(target.options(), TITLE).put("top", String.valueOf(value))));
    // ECharts cannot offset a title that is aligned to the middle, so the client applies the offset
    rules.add(new ParameterRule(CHART, "title.y",
      (target, captures, value) -> putIfPresent(hints(child(target.options(), TITLE)), "offsetY", number(value))));

    // Palette of the chart
    rules.add(new ParameterRule(CHART, "colors",
      (target, captures, value) -> target.options().put("color", value)));

    // Pie sizes: Highcharts size is the outer radius, innerSize the inner one relative to the size
    rules.add(new ParameterRule(CHART, "plotOptions.pie.size",
      (target, captures, value) -> radius(target.seriesDefaults("pie"), 1, value)));
    rules.add(new ParameterRule(CHART, "plotOptions.pie.innerSize",
      (target, captures, value) -> radius(target.seriesDefaults("pie"), 0, value)));

    // ECharts aligns the ticks of the secondary value axes with the first one, which is what Highcharts does when
    // the ticks or the thresholds are aligned. The axes are built after the parameters, so this is a model hint
    rules.add(new ParameterRule(CHART, "chart.alignTicks", (target, captures, value) -> alignTicks(target, value)));
    rules.add(new ParameterRule(CHART, "chart.alignThresholds", (target, captures, value) -> {
      target.approximation("align-thresholds", "Highcharts chart.alignThresholds is approximated by aligned ticks "
        + "(alignTicks) in the ECharts model");
      alignTicks(target, value);
    }));
    // No effect in Highcharts either: the ticks are set in the axes
    rules.add(new ParameterRule(CHART, "tickAmount", (target, captures, value) -> noEffect()));
  }

  /**
   * Translation of an option that is dropped on purpose and does not deserve a warning
   */
  private static void noEffect() {
    // Nothing to write: the option has no effect in Highcharts either
  }

  private static void alignTicks(ParameterTarget target, Object value) {
    if (bool(value)) {
      hints(target.options()).put(EChartsMaps.ALIGN_TICKS, true);
    }
  }

  private static void axisRules(List<ParameterRule> rules) {
    // Date formats of the axis labels, interpreted by the client
    rules.add(new ParameterRule(AXIS, "dateTimeLabelFormats.*",
      (target, captures, value) -> child(hints(target.options()), "dateTimeLabelFormats").put(captures.get(0), value)));
    // Label format of the axis, interpreted by the client
    rules.add(new ParameterRule(AXIS, "labels.format",
      (target, captures, value) -> hints(target.options()).put("labelFormat", value)));
    rules.add(new ParameterRule(AXIS, "gridLineWidth",
      (target, captures, value) -> gridLines(target.options(), number(value))));
    rules.add(new ParameterRule(AXIS, "tickAmount",
      (target, captures, value) -> putIfPresent(target.options(), "splitNumber", number(value))));
  }

  /**
   * A grid line width of zero hides the grid lines
   */
  private static void gridLines(Map<String, Object> axis, Number width) {
    if (width == null) {
      return;
    }
    Map<String, Object> splitLine = child(axis, "splitLine");
    splitLine.put("show", width.doubleValue() > 0);
    if (width.doubleValue() > 0) {
      child(splitLine, "lineStyle").put("width", width);
    }
  }

  private static void tooltipRules(List<ParameterRule> rules) {
    rules.add(new ParameterRule(TOOLTIP, "useHTML",
      (target, captures, value) -> hints(target.options()).put("useHTML", bool(value))));
    for (String format : List.of("headerFormat", "pointFormat", "footerFormat")) {
      rules.add(new ParameterRule(TOOLTIP, format,
        (target, captures, value) -> hints(target.options()).put(format, String.valueOf(value))));
    }
    rules.add(new ParameterRule(TOOLTIP, "style.fontSize",
      (target, captures, value) -> putIfPresent(child(target.options(), "textStyle"), "fontSize", fontSize(value))));
    // The ECharts tooltip follows the pointer at a fixed offset
    rules.add(new ParameterRule(TOOLTIP, "distance", (target, captures, value) ->
      target.approximation("tooltip-distance", "Highcharts tooltip.distance has no ECharts equivalent; "
        + "it is ignored by the ECharts model")));
  }

  @SuppressWarnings("unchecked")
  private static void radius(Map<String, Object> defaults, int index, Object value) {
    List<Object> radius = (List<Object>) defaults.computeIfAbsent(RADIUS,
      key -> new ArrayList<>(Arrays.asList(null, null)));
    radius.set(index, value);
  }
}
