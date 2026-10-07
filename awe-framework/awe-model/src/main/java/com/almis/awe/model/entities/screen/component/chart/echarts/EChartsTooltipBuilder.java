package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.ChartTooltip;

import java.util.Locale;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.dropEmptyHints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;

/**
 * Builds the {@code tooltip} of the ECharts model from the {@code chart-tooltip} element
 */
final class EChartsTooltipBuilder {

  private final HighchartsParameterTranslator translator;

  EChartsTooltipBuilder(HighchartsParameterTranslator translator) {
    this.translator = translator;
  }

  /**
   * Build the tooltip
   *
   * @param tooltip  Tooltip element, null when the chart does not define one
   * @param itemOnly The chart has no axes to share a tooltip along (pies)
   * @return Tooltip options
   */
  Map<String, Object> build(ChartTooltip tooltip, boolean itemOnly) {
    Map<String, Object> options = map();
    if (tooltip == null) {
      // Highcharts shows a tooltip for the hovered point unless stated
      options.put("show", true);
      options.put("trigger", "item");
      return options;
    }

    options.put("show", tooltip.isEnabled());
    options.put("trigger", tooltip.isShared() && !itemOnly ? "axis" : "item");
    Map<String, Object> hints = hints(options);
    if (tooltip.getCrosshairs() != null) {
      options.put("axisPointer", axisPointer(tooltip.getCrosshairs()));
      hints.put("crosshairs", tooltip.getCrosshairs());
    }
    putIfPresent(hints, "numberDecimals", tooltip.getNumberDecimals());
    putIfPresent(hints, "prefix", tooltip.getPrefix());
    putIfPresent(hints, "suffix", tooltip.getSuffix());
    putIfPresent(hints, "pointFormat", tooltip.getPointFormat());
    putIfPresent(hints, "dateFormat", tooltip.getDateFormat());

    translator.apply(tooltip, ParameterScope.TOOLTIP, new ParameterTarget(options));
    dropEmptyHints(options);
    return options;
  }

  /**
   * Highcharts crosshairs are lines on the x axis, the y axis or both. ECharts has the pointer of the trigger axis
   * ({@code line}) and the pair of both axes ({@code cross}), so a y axis crosshair is approximated by the pair
   */
  private static Map<String, Object> axisPointer(String crosshairs) {
    Map<String, Object> pointer = map();
    pointer.put("type", "xaxis".equals(crosshairs.toLowerCase(Locale.ROOT)) ? "line" : "cross");
    return pointer;
  }

  private static void putIfPresent(Map<String, Object> hints, String key, Object value) {
    if (value != null) {
      hints.put(key, value);
    }
  }
}
