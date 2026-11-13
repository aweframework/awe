package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.AREA_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.ITEM_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.LABEL;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.LINE_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.MARKER;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.CHART;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.SERIES;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.bool;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.fontSize;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.number;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.pixelsOrPercent;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterValues.putIfPresent;

/**
 * Rows of the translation table for the options of a series.
 * <p>
 * Highcharts takes these options from the series itself and from {@code plotOptions}, which are defaults for all the
 * series ({@code series}) or for the series of a type. Each option is registered for both places with the same
 * translation, which writes to the options of the series, or to the defaults that the series builder merges into the
 * series of that type.
 * </p>
 */
final class SeriesParameterRules {

  private static final String PLOT_OPTIONS = "plotOptions.*.";
  private static final String STACK = "stack";
  private static final String COLOR = "color";
  private static final String ORIGIN = "origin";
  private static final Pattern TEXT_SHADOW = Pattern.compile("^(\\S++)\\s++(\\S++)\\s++(.++)$");
  private static final Pattern BLUR_AND_COLOR = Pattern.compile("^(\\S++)\\s++(.++)$");

  /**
   * Writes the value of an option to the options of a series, or to the defaults of the series of a type
   */
  @FunctionalInterface
  private interface SeriesTranslation {
    void apply(Map<String, Object> series, Object value, ParameterTarget target);
  }

  private SeriesParameterRules() {
  }

  /**
   * Translation of an option that is known and has nothing to write in the model: a custom key of the screens, which
   * reaches the formats through {@code userOptions}
   */
  private static void noEffect() {
    // Nothing to write: the option is read by the client formats
  }

  /**
   * Rows for the options of a series
   *
   * @return Translation rules
   */
  static List<ParameterRule> rules() {
    List<ParameterRule> rules = new ArrayList<>();
    dataLabels(rules);
    barGeometry(rules);
    colors(rules);
    linesAndAreas(rules);
    markers(rules);
    interaction(rules);
    // Binding of the point values to the drilldown point (name, y, drilldown)
    rules.add(new ParameterRule(SERIES, "keys", (target, captures, value) -> hints(target.options()).put("keys", value)));
    // Custom key of the screens, which the formats read as series.userOptions.fullname
    rules.add(new ParameterRule(SERIES, "fullname", (target, captures, value) -> noEffect()));
    return List.copyOf(rules);
  }

  /**
   * Register an option of the series and of the {@code plotOptions} defaults
   */
  private static void both(List<ParameterRule> rules, String path, SeriesTranslation translation) {
    rules.add(new ParameterRule(SERIES, path,
      (target, captures, value) -> translation.apply(target.options(), value, target)));
    rules.add(new ParameterRule(CHART, PLOT_OPTIONS + path,
      (target, captures, value) -> translation.apply(target.seriesDefaults(captures.get(0)), value, target)));
  }

  // ------------------------------------------------------------------------------------------------------------
  // Data labels
  // ------------------------------------------------------------------------------------------------------------

  private static void dataLabels(List<ParameterRule> rules) {
    both(rules, "dataLabels.enabled", (series, value, target) -> child(series, LABEL).put("show", bool(value)));
    both(rules, "dataLabels.format", (series, value, target) -> hints(series).put("labelFormat", value));
    both(rules, "dataLabels.distance", (series, value, target) -> labelDistance(series, number(value)));
    both(rules, "dataLabels.style.fontSize",
      (series, value, target) -> putIfPresent(child(series, LABEL), "fontSize", fontSize(value)));
    both(rules, "dataLabels.style.fontWeight", (series, value, target) -> child(series, LABEL).put("fontWeight", value));
    both(rules, "dataLabels.style.color", (series, value, target) -> child(series, LABEL).put(COLOR, value));
    both(rules, "dataLabels.style.textShadow", (series, value, target) -> textShadow(child(series, LABEL), value));
    both(rules, "dataLabels.connectorColor",
      (series, value, target) -> child(child(series, "labelLine"), LINE_STYLE).put(COLOR, value));
  }

  /**
   * A negative Highcharts distance puts the label inside the pie, otherwise it is the length of the leader line
   */
  private static void labelDistance(Map<String, Object> defaults, Number distance) {
    if (distance == null) {
      return;
    }
    Map<String, Object> label = child(defaults, LABEL);
    if (distance.doubleValue() < 0) {
      label.put("position", "inside");
    } else {
      label.put("position", "outside");
      child(defaults, "labelLine").put("length", distance);
    }
  }

  /**
   * Translate a CSS text shadow: {@code <offset-x> <offset-y> [<blur>] <color>}, where the color may be a functional
   * notation with spaces such as {@code rgba(0, 0, 0, 0.5)}. Without blur, the shadow is sharp.
   * <p>
   * The Highcharts keywords {@code contrast} and {@code none}, and the forms that start with the color, have no
   * ECharts equivalent and are dropped.
   * </p>
   */
  private static void textShadow(Map<String, Object> label, Object value) {
    Matcher shadow = TEXT_SHADOW.matcher(String.valueOf(value).trim());
    if (!shadow.matches()) {
      return;
    }
    Number offsetX = length(shadow.group(1));
    Number offsetY = length(shadow.group(2));
    String rest = shadow.group(3);
    Number blur = 0;
    String color = rest;
    Matcher withBlur = BLUR_AND_COLOR.matcher(rest);
    if (withBlur.matches() && length(withBlur.group(1)) != null) {
      blur = length(withBlur.group(1));
      color = withBlur.group(2);
    }
    if (offsetX == null || offsetY == null || length(color) != null) {
      return;
    }
    label.put("textShadowOffsetX", offsetX);
    label.put("textShadowOffsetY", offsetY);
    label.put("textShadowBlur", blur);
    label.put("textShadowColor", color);
  }

  private static Number length(String value) {
    return number(value.endsWith("px") ? value.substring(0, value.length() - 2) : value);
  }

  // ------------------------------------------------------------------------------------------------------------
  // Bars
  // ------------------------------------------------------------------------------------------------------------

  /**
   * Highcharts {@code stack} names the group of a stacked series and {@code stacking} turns the stacking on. ECharts
   * stacks the series that share a {@code stack} name, so {@code stacking} only marks the series and {@link SeriesFinisher}
   * writes the name, which is ignored unless stacking is active (like in Highcharts). The padding options are fractions of
   * the width that the Highcharts column takes, which ECharts expresses as percentages of the bar width (the gap
   * between categories is twice the group padding because it is added on both sides).
   * <p>
   * The corner radius is applied by the client, per point, because Highcharts only rounds the end of the bar that is
   * away from zero, and only in the outermost series of a stack.
   * </p>
   */
  private static void barGeometry(List<ParameterRule> rules) {
    both(rules, STACK, (series, value, target) -> series.put(STACK, String.valueOf(value)));
    both(rules, "stacking", (series, value, target) -> stacking(series, String.valueOf(value)));
    both(rules, "groupPadding",
      (series, value, target) -> putIfPresent(series, "barCategoryGap", percent(number(value), 200)));
    both(rules, "pointPadding", (series, value, target) -> putIfPresent(series, "barGap", percent(number(value), 100)));
    both(rules, "pointWidth", (series, value, target) -> putIfPresent(series, "barWidth", number(value)));
    both(rules, "borderRadius", (series, value, target) -> {
      Number radius = number(value);
      hints(series).put("borderRadius", radius == null ? String.valueOf(value) : radius);
    });
  }

  private static void stacking(Map<String, Object> series, String mode) {
    if (!"normal".equalsIgnoreCase(mode) && !"percent".equalsIgnoreCase(mode)) {
      return;
    }
    series.put(EChartsKeys.STACKING, true);
    if ("percent".equalsIgnoreCase(mode)) {
      hints(series).put("stackPercent", true);
    }
  }

  private static String percent(Number fraction, int factor) {
    return fraction == null ? null : Math.round(fraction.doubleValue() * factor) + "%";
  }

  // ------------------------------------------------------------------------------------------------------------
  // Colors
  // ------------------------------------------------------------------------------------------------------------

  private static void colors(List<ParameterRule> rules) {
    both(rules, COLOR, (series, value, target) -> child(series, ITEM_STYLE).put(COLOR, value));
    both(rules, "borderColor", (series, value, target) -> child(series, ITEM_STYLE).put("borderColor", value));
    both(rules, "colorByPoint",
      (series, value, target) -> series.put("colorBy", bool(value) ? "data" : "series"));
    both(rules, "allowPointSelect", (series, value, target) -> {
      if (bool(value)) {
        series.put("selectedMode", "single");
      }
    });
  }

  // ------------------------------------------------------------------------------------------------------------
  // Lines and areas
  // ------------------------------------------------------------------------------------------------------------

  private static void linesAndAreas(List<ParameterRule> rules) {
    both(rules, "lineWidth", (series, value, target) -> putIfPresent(child(series, LINE_STYLE), "width", number(value)));
    both(rules, "dashStyle", (series, value, target) -> child(series, LINE_STYLE).put("type", lineType(value)));
    both(rules, "fillColor", (series, value, target) -> {
      Map<String, Object> area = child(series, AREA_STYLE);
      area.put(COLOR, value);
      area.put("opacity", 1);
    });
    both(rules, "fillColor.linearGradient.x1",
      (series, value, target) -> putIfPresent(gradient(series), "x", number(value)));
    both(rules, "fillColor.linearGradient.y1",
      (series, value, target) -> putIfPresent(gradient(series), "y", number(value)));
    both(rules, "fillColor.linearGradient.x2",
      (series, value, target) -> putIfPresent(gradient(series), "x2", number(value)));
    both(rules, "fillColor.linearGradient.y2",
      (series, value, target) -> putIfPresent(gradient(series), "y2", number(value)));
    both(rules, "fillColor.stops", (series, value, target) -> gradient(series).put("colorStops", colorStops(value)));
    both(rules, "threshold", SeriesParameterRules::threshold);
  }

  /**
   * Highcharts dash styles are named ({@code Dot}, {@code ShortDash}...), ECharts has solid, dashed and dotted lines
   */
  private static String lineType(Object dashStyle) {
    String name = String.valueOf(dashStyle).toLowerCase(Locale.ROOT);
    if ("solid".equals(name)) {
      return "solid";
    }
    return name.contains("dot") && !name.contains("dash") ? "dotted" : "dashed";
  }

  /**
   * Linear gradient fill of an area, which is plain JSON in ECharts. It is vertical, from the top to the bottom, until
   * the gradient coordinates say otherwise. The fill is not translucent: the stops carry their own alpha
   */
  private static Map<String, Object> gradient(Map<String, Object> series) {
    Map<String, Object> area = child(series, AREA_STYLE);
    area.put("opacity", 1);
    Map<String, Object> fill = child(area, COLOR);
    fill.put("type", "linear");
    fill.putIfAbsent("x", 0);
    fill.putIfAbsent("y", 0);
    fill.putIfAbsent("x2", 0);
    fill.putIfAbsent("y2", 1);
    return fill;
  }

  private static List<Object> colorStops(Object stops) {
    List<Object> result = new ArrayList<>();
    if (stops instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof List<?> pair && pair.size() >= 2 && number(pair.get(0)) != null) {
          Map<String, Object> stop = map();
          stop.put("offset", number(pair.get(0)));
          stop.put(COLOR, pair.get(1));
          result.add(stop);
        }
      }
    }
    return result;
  }

  /**
   * A threshold of minus infinity fills the area from the bottom of the axis. The default one (zero) is what ECharts
   * does. Other thresholds have no ECharts equivalent: the area still fills from zero.
   */
  private static void threshold(Map<String, Object> series, Object value, ParameterTarget target) {
    Number number = number(value);
    if (number != null && number.doubleValue() == 0) {
      return;
    }
    if (isNegativeInfinity(value)) {
      child(series, AREA_STYLE).put(ORIGIN, "start");
    } else {
      target.approximation("threshold:" + value, "Highcharts threshold '" + value
        + "' has no ECharts equivalent; the area is filled from zero in the ECharts model");
    }
  }

  private static boolean isNegativeInfinity(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue() == Double.NEGATIVE_INFINITY;
    }
    return "-infinity".equalsIgnoreCase(String.valueOf(value).trim());
  }

  // ------------------------------------------------------------------------------------------------------------
  // Markers
  // ------------------------------------------------------------------------------------------------------------

  /**
   * The marker options are collected in a {@code marker} object of the series, which the series builder resolves
   * when it knows the type of the series and all the options have been applied (see {@link SeriesFinisher})
   */
  private static void markers(List<ParameterRule> rules) {
    both(rules, "marker.enabled", (series, value, target) -> child(series, MARKER).put("enabled", bool(value)));
    both(rules, "marker.radius", (series, value, target) -> putIfPresent(child(series, MARKER), "radius", number(value)));
    both(rules, "marker.fillColor", (series, value, target) -> child(series, MARKER).put("fillColor", value));
    both(rules, "marker.lineColor", (series, value, target) -> child(series, MARKER).put("lineColor", value));
    both(rules, "marker.lineWidth",
      (series, value, target) -> putIfPresent(child(series, MARKER), "lineWidth", number(value)));
  }

  // ------------------------------------------------------------------------------------------------------------
  // Interaction and legend
  // ------------------------------------------------------------------------------------------------------------

  private static void interaction(List<ParameterRule> rules) {
    both(rules, "enableMouseTracking", (series, value, target) -> {
      if (!bool(value)) {
        series.put("silent", true);
        child(series, "tooltip").put("show", false);
      }
    });
    both(rules, "showInLegend", (series, value, target) -> hints(series).put("showInLegend", bool(value)));
    both(rules, "tooltip.valueSuffix", (series, value, target) -> hints(series).put("valueSuffix", value));
    both(rules, "minSize", (series, value, target) -> putIfPresent(hints(series), "minSize", pixelsOrPercent(value)));
    both(rules, "maxSize", (series, value, target) -> putIfPresent(hints(series), "maxSize", pixelsOrPercent(value)));
    // The legend is built by the client, which knows the names of the series
    rules.add(new ParameterRule(SERIES, "linkedTo",
      (target, captures, value) -> hints(target.options()).put("linkedTo", String.valueOf(value))));
    rules.add(new ParameterRule(SERIES, "legendIndex",
      (target, captures, value) -> putIfPresent(hints(target.options()), "legendIndex", number(value))));
  }
}
