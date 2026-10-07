package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.LABEL;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.RADIUS;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.TITLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.AXIS;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.CHART;
import static com.almis.awe.model.entities.screen.component.chart.echarts.ParameterScope.SERIES;

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
 */
final class HighchartsParameterTable {

  private static final List<ParameterRule> RULES = buildRules();
  private static final Pattern TEXT_SHADOW = Pattern.compile("^(\\S++)\\s++(\\S++)\\s++(.++)$");
  private static final Pattern BLUR_AND_COLOR = Pattern.compile("^(\\S++)\\s++(.++)$");

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

    // Chart title placement
    rules.add(new ParameterRule(CHART, "title.align",
      (target, captures, value) -> child(target.options(), TITLE).put("left", String.valueOf(value))));
    rules.add(new ParameterRule(CHART, "title.verticalAlign",
      (target, captures, value) -> child(target.options(), TITLE).put("top", String.valueOf(value))));
    // ECharts cannot offset a title that is aligned to the middle, so the client applies the offset
    rules.add(new ParameterRule(CHART, "title.y",
      (target, captures, value) -> putIfPresent(hints(child(target.options(), TITLE)), "offsetY", number(value))));

    // Pie sizes: Highcharts size is the outer radius, innerSize the inner one relative to the size
    rules.add(new ParameterRule(CHART, "plotOptions.pie.size",
      (target, captures, value) -> radius(target.seriesDefaults("pie"), 1, value)));
    rules.add(new ParameterRule(CHART, "plotOptions.pie.innerSize",
      (target, captures, value) -> radius(target.seriesDefaults("pie"), 0, value)));

    // Data labels of any series type, or of all of them with plotOptions.series
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.enabled",
      (target, captures, value) -> child(target.seriesDefaults(captures.get(0)), LABEL).put("show", bool(value))));
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.format",
      (target, captures, value) -> hints(target.seriesDefaults(captures.get(0))).put("labelFormat", value)));
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.distance",
      (target, captures, value) -> labelDistance(target.seriesDefaults(captures.get(0)), number(value))));
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.style.fontWeight",
      (target, captures, value) -> child(target.seriesDefaults(captures.get(0)), LABEL).put("fontWeight", value)));
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.style.color",
      (target, captures, value) -> child(target.seriesDefaults(captures.get(0)), LABEL).put("color", value)));
    rules.add(new ParameterRule(CHART, "plotOptions.*.dataLabels.style.textShadow",
      (target, captures, value) -> textShadow(child(target.seriesDefaults(captures.get(0)), LABEL), value)));

    // Binding of the point values to the drilldown point (name, y, drilldown)
    rules.add(new ParameterRule(SERIES, "keys",
      (target, captures, value) -> hints(target.options()).put("keys", value)));

    // Date formats of the axis labels, interpreted by the client
    rules.add(new ParameterRule(AXIS, "dateTimeLabelFormats.*",
      (target, captures, value) -> child(hints(target.options()), "dateTimeLabelFormats").put(captures.get(0), value)));

    return List.copyOf(rules);
  }

  @SuppressWarnings("unchecked")
  private static void radius(Map<String, Object> defaults, int index, Object value) {
    List<Object> radius = (List<Object>) defaults.computeIfAbsent(RADIUS,
      key -> new ArrayList<>(Arrays.asList(null, null)));
    radius.set(index, value);
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

  private static void putIfPresent(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static Boolean bool(Object value) {
    return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value).toLowerCase(Locale.ROOT));
  }

  private static Number number(Object value) {
    if (value instanceof Number number) {
      return number;
    }
    try {
      double parsed = Double.parseDouble(String.valueOf(value).trim());
      return parsed == Math.rint(parsed) ? (Number) Integer.valueOf((int) parsed) : (Number) Double.valueOf(parsed);
    } catch (NumberFormatException exc) {
      return null;
    }
  }
}
