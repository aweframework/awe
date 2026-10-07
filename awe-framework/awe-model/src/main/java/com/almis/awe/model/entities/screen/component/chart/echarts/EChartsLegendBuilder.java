package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.ChartLegend;

import java.util.Locale;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.dropEmptyHints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;

/**
 * Builds the {@code legend} of the ECharts model from the {@code chart-legend} element
 */
final class EChartsLegendBuilder {

  private final HighchartsParameterTranslator translator;

  EChartsLegendBuilder(HighchartsParameterTranslator translator) {
    this.translator = translator;
  }

  /**
   * Build the legend
   *
   * @param legend          Legend element, null when the chart does not define one
   * @param hiddenByDefault Highcharts does not show a legend by default in this chart (pies and stock charts)
   * @return Legend options
   */
  Map<String, Object> build(ChartLegend legend, boolean hiddenByDefault) {
    Map<String, Object> options = map();
    if (legend == null) {
      options.put("show", !hiddenByDefault);
      placement(options, null, null);
      return options;
    }

    options.put("show", legend.isEnabled());
    if (legend.getLayout() != null) {
      options.put("orient", legend.getLayout().toLowerCase(Locale.ROOT));
    }
    placement(options, legend.getAlign(), legend.getVerticalAlign());
    if (legend.getBorderWidth() != null) {
      options.put("borderWidth", legend.getBorderWidth());
    }

    Map<String, Object> hints = hints(options);
    if (legend.isFloating()) {
      hints.put("floating", true);
    }
    if (legend.getLabel() != null) {
      hints.put("title", legend.getLabel());
    }
    translator.apply(legend, ParameterScope.LEGEND, new ParameterTarget(options));
    dropEmptyHints(options);
    return options;
  }

  /**
   * Highcharts legends are centered at the bottom unless stated
   */
  private static void placement(Map<String, Object> options, String align, String verticalAlign) {
    options.put("left", align == null ? "center" : align.toLowerCase(Locale.ROOT));
    options.put("top", verticalAlign == null ? "bottom" : verticalAlign.toLowerCase(Locale.ROOT));
  }
}
