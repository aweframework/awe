package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.List;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.AREA_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.ITEM_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.MARKER;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.STACKING;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;

/**
 * Last step of a series: resolves what depends on the type of the series, once the options of the series and the
 * defaults of its type have been merged.
 * <p>
 * The options of the {@code plotOptions} defaults and of the {@code series} defaults reach series of any type, so
 * the options that do not apply to the type are removed here. The markers are resolved here too, because a disabled
 * marker hides the symbols of a line and the points of a scatter in different ways.
 * </p>
 */
final class SeriesFinisher {

  private static final String STACK = "stack";
  private static final String FILL_COLOR = "fillColor";
  private static final String LINE_COLOR = "lineColor";
  private static final String LINE_WIDTH = "lineWidth";
  private static final List<String> BAR_OPTIONS = List.of("barGap", "barCategoryGap", "barWidth");

  private SeriesFinisher() {
  }

  /**
   * Resolve the options that depend on the series type
   *
   * @param options Series options, with all the defaults merged
   * @param type    Series type
   */
  static void finish(Map<String, Object> options, EChartsSeriesType type) {
    Object marker = options.remove(MARKER);
    if (!type.isArea()) {
      options.remove(AREA_STYLE);
    }
    resolveStack(options, type, options.remove(STACKING) != null);
    if (!"bar".equals(type.echartsType())) {
      BAR_OPTIONS.forEach(options::remove);
      hints(options).remove("borderRadius");
    }
    if (marker instanceof Map<?, ?> markerOptions) {
      applyMarker(options, type, castMap(markerOptions));
    }
  }

  /**
   * The stack name groups the series only when the stacking is active; the series without a name share the default
   * group. Series that cannot be stacked never stack
   */
  private static void resolveStack(Map<String, Object> options, EChartsSeriesType type, boolean stacking) {
    if (stacking && type.isStackable()) {
      options.putIfAbsent(STACK, STACK);
      return;
    }
    options.remove(STACK);
    hints(options).remove("stackPercent");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Map<?, ?> map) {
    return (Map<String, Object>) map;
  }

  /**
   * A disabled marker hides the symbols of a line and makes the points of a scatter invisible, so that they still
   * place the axis range. Marker styles become a filled circle with its border. The fill goes in a hint, because
   * ECharts draws the line and the area with the color of the symbols: the client gives the fill to the symbols and
   * keeps the color of the series for the line and the area. Other series have no markers
   */
  private static void applyMarker(Map<String, Object> options, EChartsSeriesType type, Map<String, Object> marker) {
    boolean scatter = EChartsKeys.SCATTER.equals(type.echartsType());
    if (!scatter && !"line".equals(type.echartsType())) {
      return;
    }
    if (Boolean.FALSE.equals(marker.get("enabled"))) {
      if (scatter) {
        child(options, ITEM_STYLE).put("opacity", 0);
      } else {
        options.put("showSymbol", false);
      }
      return;
    }
    boolean styled = marker.containsKey("radius") || marker.containsKey(FILL_COLOR)
      || marker.containsKey(LINE_COLOR) || marker.containsKey(LINE_WIDTH);
    if (!scatter && (styled || Boolean.TRUE.equals(marker.get("enabled")))) {
      options.put("showSymbol", true);
    }
    if (styled) {
      styleSymbols(options, marker);
    }
  }

  private static void styleSymbols(Map<String, Object> options, Map<String, Object> marker) {
    // The default ECharts symbol is an empty circle, which is painted with the background
    options.put("symbol", "circle");
    if (marker.get("radius") instanceof Number radius) {
      options.put("symbolSize", diameter(radius));
    }
    if (marker.containsKey(FILL_COLOR)) {
      hints(options).put("markerFill", marker.get(FILL_COLOR));
    }
    if (marker.containsKey(LINE_COLOR)) {
      child(options, ITEM_STYLE).put("borderColor", marker.get(LINE_COLOR));
    }
    if (marker.containsKey(LINE_WIDTH)) {
      child(options, ITEM_STYLE).put("borderWidth", marker.get(LINE_WIDTH));
    }
  }

  private static Number diameter(Number radius) {
    double size = radius.doubleValue() * 2;
    return size == Math.rint(size) ? (Number) (int) size : (Number) size;
  }
}
