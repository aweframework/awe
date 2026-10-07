package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.AREA_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.ITEM_STYLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.LABEL;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.RADIUS;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.dropEmptyHints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;

/**
 * Builds the series of the ECharts model from the {@code chart-serie} elements of a chart.
 * <p>
 * The series carry no data. The client builds the points from the component values, guided by the
 * {@code xValue}, {@code yValue} and {@code zValue} hints.
 * </p>
 */
final class EChartsSeriesBuilder {

  private static final String SERIES_DEFAULTS = "series";
  private static final String DEFAULT_PIE_RADIUS = "75%";
  private static final String DEFAULT_RING_INNER_RADIUS = "50%";

  private final Chart chart;
  private final boolean inverted;
  private final ParameterTarget chartParameters;
  private final HighchartsParameterTranslator translator;
  private final UnsupportedOptionReporter reporter;
  private final SeriesTypeResolver typeResolver;
  private final String chartType;

  EChartsSeriesBuilder(Chart chart, boolean inverted, ParameterTarget chartParameters,
                       HighchartsParameterTranslator translator, SeriesTypeResolver typeResolver,
                       UnsupportedOptionReporter reporter) {
    this.chart = chart;
    this.inverted = inverted;
    this.chartParameters = chartParameters;
    this.translator = translator;
    this.typeResolver = typeResolver;
    this.reporter = reporter;
    this.chartType = chart.getType() == null ? "" : chart.getType().trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Build the model of a series
   *
   * @param serie Series element
   * @return Series options
   */
  Map<String, Object> build(ChartSerie serie) {
    EChartsSeriesType type = typeResolver.typeOf(serie);
    Map<String, Object> options = map();

    if (StringUtils.isNotBlank(serie.getId())) {
      options.put("id", serie.getId());
    }
    if (StringUtils.isNotBlank(serie.getLabel())) {
      options.put("name", serie.getLabel());
    }
    options.put("type", type.echartsType());
    if (type.isSmooth()) {
      options.put("smooth", true);
    }
    if (type.isArea()) {
      options.put(AREA_STYLE, map());
    }
    if (type.isApproximated()) {
      reporter.approximation("series-type:" + type.highchartsName(), "Highcharts series type '"
        + type.highchartsName() + "' is approximated by a filled line in the ECharts model");
    }
    if (!type.isPie()) {
      putAxisIndexes(options, serie);
    }
    putStack(options, type);
    if (StringUtils.isNotBlank(serie.getColor())) {
      child(options, ITEM_STYLE).put("color", serie.getColor());
    }
    if (chart.isEnableDataLabels()) {
      child(options, LABEL).put("show", true);
    }
    putHints(options, serie, type);

    translator.apply(serie, ParameterScope.SERIES, new ParameterTarget(options));
    // plotOptions of the series type take precedence over the ones of all series
    EChartsMaps.mergeDefaults(options, chartParameters.existingSeriesDefaults(type.highchartsName()));
    EChartsMaps.mergeDefaults(options, chartParameters.existingSeriesDefaults(SERIES_DEFAULTS));

    SeriesFinisher.finish(options, type);
    if (type.isPie()) {
      putPieLayout(options);
    } else {
      options.remove(RADIUS);
    }
    dropEmptyHints(options);
    return options;
  }

  private void putAxisIndexes(Map<String, Object> options, ChartSerie serie) {
    Integer xIndex = EChartsMaps.integer(serie.getXAxis());
    Integer yIndex = EChartsMaps.integer(serie.getYAxis());
    // The ECharts horizontal axis is the AWE y axis in an inverted chart
    Integer horizontal = inverted ? yIndex : xIndex;
    Integer vertical = inverted ? xIndex : yIndex;
    if (horizontal != null) {
      options.put("xAxisIndex", horizontal);
    }
    if (vertical != null) {
      options.put("yAxisIndex", vertical);
    }
  }

  private void putStack(Map<String, Object> options, EChartsSeriesType type) {
    if (chart.isStacking() && type.isStackable()) {
      options.put(EChartsKeys.STACKING, true);
      if ("percent".equalsIgnoreCase(chart.getStacking())) {
        hints(options).put("stackPercent", true);
      }
    }
  }

  private void putHints(Map<String, Object> options, ChartSerie serie, EChartsSeriesType type) {
    Map<String, Object> hints = hints(options);
    hints.put("type", type.highchartsName());
    putIfNotBlank(hints, "xValue", serie.getXValue());
    putIfNotBlank(hints, "yValue", serie.getYValue());
    putIfNotBlank(hints, "zValue", serie.getZValue());
    putIfNotBlank(hints, "drilldown", serie.getDrillDownSerie());
    if (chart.isEnableDataLabels()) {
      putIfNotBlank(hints, "labelFormat", chart.getFormatDataLabels());
    }
  }

  private static void putIfNotBlank(Map<String, Object> target, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      target.put(key, value);
    }
  }

  // ------------------------------------------------------------------------------------------------------------
  // Pie layout
  // ------------------------------------------------------------------------------------------------------------

  /**
   * Donuts and semicircles are pies with an inner radius. A semicircle only draws the upper half, like the Highcharts
   * one: from 9 o'clock to 3 o'clock, clockwise, with its center lowered to leave room for the half circle
   */
  private void putPieLayout(Map<String, Object> options) {
    boolean ring = chartType.startsWith("donut") || "semicircle".equals(chartType);
    Object radius = pieRadius(options.remove(RADIUS), ring);
    if (radius != null) {
      options.put(RADIUS, radius);
    }
    if ("semicircle".equals(chartType)) {
      options.put("startAngle", 180);
      options.put("endAngle", 360);
      options.put("center", List.of("50%", "75%"));
    }
  }

  /**
   * Combine the Highcharts pie sizes with the defaults of the chart type. Highcharts defines {@code innerSize}
   * relative to {@code size}, ECharts defines both relative to the plot, so a percentage inner size is converted
   *
   * @param parameterRadius [innerSize, size] set by parameters, with null for the missing ones
   * @param ring            The chart type is a donut or a semicircle
   * @return Radius to send: a single outer radius, an [inner, outer] pair, or null for the ECharts default
   */
  private static Object pieRadius(Object parameterRadius, boolean ring) {
    Object inner = null;
    Object outer = null;
    if (parameterRadius instanceof List<?> sizes) {
      inner = sizes.get(0);
      outer = sizes.get(1);
    }
    Object resolvedOuter = outer;
    if (resolvedOuter == null && ring) {
      resolvedOuter = DEFAULT_PIE_RADIUS;
    }
    Object resolvedInner = ring ? DEFAULT_RING_INNER_RADIUS : null;
    if (inner != null) {
      resolvedInner = relativeToOuter(inner, resolvedOuter != null ? resolvedOuter : DEFAULT_PIE_RADIUS);
    }
    if (resolvedInner == null) {
      return resolvedOuter;
    }
    return List.of(resolvedInner, resolvedOuter != null ? resolvedOuter : DEFAULT_PIE_RADIUS);
  }

  private static Object relativeToOuter(Object inner, Object outer) {
    Double innerPercent = percent(inner);
    Double outerPercent = percent(outer);
    if (innerPercent == null || outerPercent == null) {
      return inner;
    }
    return BigDecimal.valueOf(innerPercent * outerPercent / 100).stripTrailingZeros().toPlainString() + "%";
  }

  private static Double percent(Object value) {
    if (value instanceof String text && text.trim().endsWith("%")) {
      try {
        double parsed = Double.parseDouble(text.trim().substring(0, text.trim().length() - 1));
        return Double.isFinite(parsed) ? parsed : null;
      } catch (NumberFormatException exc) {
        return null;
      }
    }
    return null;
  }
}
