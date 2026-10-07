package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves the type of the series of a chart: their own type, or the one of the chart
 */
final class SeriesTypeResolver {

  private final EChartsSeriesType defaultType;
  private final UnsupportedOptionReporter reporter;

  SeriesTypeResolver(Chart chart, UnsupportedOptionReporter reporter) {
    this.defaultType = EChartsSeriesType.forChartType(chart.getType());
    this.reporter = reporter;
  }

  /**
   * Type that the series of the chart take when they do not define their own
   */
  EChartsSeriesType defaultType() {
    return defaultType;
  }

  /**
   * Resolve the type of a series. A type without translation is rendered as a line and reported once
   *
   * @param serie Series element
   * @return Series type
   */
  EChartsSeriesType typeOf(ChartSerie serie) {
    if (StringUtils.isBlank(serie.getType())) {
      return defaultType;
    }
    return EChartsSeriesType.fromName(serie.getType()).orElseGet(() -> {
      reporter.approximation("series-type:" + serie.getType(), "Highcharts series type '" + serie.getType()
        + "' has no ECharts translation yet; it is rendered as a line in the ECharts model");
      return EChartsSeriesType.LINE;
    });
  }
}
