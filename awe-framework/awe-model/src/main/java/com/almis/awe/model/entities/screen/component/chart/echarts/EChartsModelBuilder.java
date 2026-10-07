package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsKeys.TITLE;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;

/**
 * Translates an AWE {@link Chart} into the option object of Apache ECharts.
 * <p>
 * The same {@code chart}, {@code chart-serie}, {@code x-axis}, {@code y-axis}, {@code chart-legend},
 * {@code chart-tooltip} and {@code chart-parameter} elements that produce the Highcharts {@code chartModel} produce
 * this {@code echartsModel}, which is sent next to it and never alters it.
 * </p>
 * <h2>Contract with the client</h2>
 * <ul>
 *   <li>The model carries no data. The client builds the points from the component values, guided by the
 *   {@code xValue}, {@code yValue} and {@code zValue} hints of each series.</li>
 *   <li>The model carries no functions. Everything that the client has to evaluate or interpret travels as data in
 *   an {@code awe} key of the object it belongs to: Highcharts format strings, formatter names, data binding
 *   fields, drilldown, theme and stock flags.</li>
 *   <li><strong>Any object of the model may carry an {@code awe} key with AWE client hints. The client removes
 *   every {@code awe} key before it gives the option to ECharts</strong>, so the hints never reach the
 *   library.</li>
 * </ul>
 * <h2>Top level</h2>
 * <ul>
 *   <li>{@code title}, {@code legend}, {@code tooltip}, {@code xAxis}, {@code yAxis}, {@code dataZoom} and
 *   {@code series}: ECharts options. {@code xAxis} and {@code yAxis} are arrays, and are missing in pie charts. In
 *   an inverted chart they are swapped: {@code xAxis} holds the horizontal axes, which are the AWE y axes.</li>
 *   <li>{@code awe.chartType}: AWE chart type, such as {@code column_3d}; {@code awe.theme}; {@code awe.inverted};
 *   {@code awe.stock}; {@code awe.stacking} ({@code normal} or {@code percent});
 *   {@code awe.drilldown.series}: the series that open when a point of another series is drilled, which are not in
 *   {@code series}.</li>
 * </ul>
 * <h2>Hints by object</h2>
 * <ul>
 *   <li>Series: {@code type} (Highcharts series type, such as {@code bubble} or {@code spline}), {@code xValue},
 *   {@code yValue}, {@code zValue}, {@code drilldown} (id of the series that opens), {@code keys} (point binding of
 *   the drilldown points), {@code labelFormat} (Highcharts format of the data labels), {@code stackPercent},
 *   {@code borderRadius} (pixels, or a percentage text, of the bars; the client rounds the end of the bar away from
 *   zero in the outermost series of a stack), {@code showInLegend}, {@code linkedTo} ({@code :previous} or a series
 *   id) and {@code legendIndex} (the client builds the legend with them), {@code valueSuffix} (tooltip),
 *   {@code minSize} and {@code maxSize} (range of the bubble sizes in pixels) and {@code userOptions} (the raw
 *   {@code chart-parameter} options of the series, which the formats read as {@code series.userOptions.<key>}).</li>
 *   <li>Axes: {@code axis} ({@code x} or {@code y}, the AWE role), {@code labelFormat}, {@code formatter} (name of a
 *   client formatter, such as {@code formatCurrencyMagnitude}), {@code dateTimeLabelFormats}.</li>
 *   <li>Tooltip: {@code crosshairs}, {@code numberDecimals}, {@code prefix}, {@code suffix}, {@code pointFormat},
 *   {@code headerFormat}, {@code footerFormat}, {@code useHTML}, {@code dateFormat}.</li>
 *   <li>Legend: {@code title}, {@code floating}.</li>
 *   <li>Title: {@code offsetY} (pixels).</li>
 * </ul>
 * <p>
 * 3D chart types are rendered as flat 2D charts. {@code chart-parameter} elements that have no row in
 * {@link HighchartsParameterTable} are not applied to this model and are reported once with a warning; they still
 * reach the Highcharts model unchanged.
 * </p>
 */
public final class EChartsModelBuilder {

  private final UnsupportedOptionReporter reporter;
  private final HighchartsParameterTranslator translator;

  /**
   * Create a builder that reports unsupported options to the log
   */
  public EChartsModelBuilder() {
    this(UnsupportedOptionReporter.shared());
  }

  /**
   * Create a builder
   *
   * @param reporter Receives the options that cannot be translated
   */
  public EChartsModelBuilder(UnsupportedOptionReporter reporter) {
    this.reporter = reporter;
    this.translator = new HighchartsParameterTranslator(reporter);
  }

  /**
   * Build the ECharts model of a chart
   *
   * @param chart Chart element
   * @return ECharts option, without data
   */
  public Map<String, Object> build(Chart chart) {
    ParameterTarget root = new ParameterTarget(map());
    Map<String, Object> model = root.options();

    putTitle(model, chart);
    translator.apply(chart, ParameterScope.CHART, root);
    if (model.get(TITLE) instanceof Map<?, ?> title && !title.containsKey("left")) {
      child(model, TITLE).put("left", "center");
    }

    List<ChartSerie> series = Optional.ofNullable(chart.getSerieList()).orElse(Collections.emptyList());
    SeriesTypeResolver typeResolver = new SeriesTypeResolver(chart, reporter);
    boolean inverted = chart.isInverted() || isHorizontalBar(series, typeResolver);
    boolean cartesian = isCartesian(series, typeResolver);
    EChartsSeriesBuilder seriesBuilder =
      new EChartsSeriesBuilder(chart, inverted, root, translator, typeResolver, reporter);

    model.put("legend", new EChartsLegendBuilder(translator).build(chart.getChartLegend(),
      !cartesian || chart.isStockChart()));
    model.put("tooltip", new EChartsTooltipBuilder(translator).build(chart.getChartTooltip(), !cartesian));

    if (cartesian) {
      EChartsAxisBuilder axisBuilder = new EChartsAxisBuilder(translator);
      List<Object> xAxes = axisBuilder.build(chart.getXAxisList(), true, inverted, true);
      List<Object> yAxes = axisBuilder.build(chart.getYAxisList(), false, inverted, true);
      if (hints(model).remove(EChartsMaps.ALIGN_TICKS) != null) {
        alignTicks(xAxes);
        alignTicks(yAxes);
      }
      model.put("xAxis", inverted ? yAxes : xAxes);
      model.put("yAxis", inverted ? xAxes : yAxes);
      List<Object> zoom = dataZoom(chart, inverted);
      if (!zoom.isEmpty()) {
        model.put("dataZoom", zoom);
      }
    }

    hints(model).remove(EChartsMaps.ALIGN_TICKS);
    model.put("series", buildSeries(series, seriesBuilder, false));
    putHints(model, chart, inverted, buildSeries(series, seriesBuilder, true));
    return model;
  }

  /**
   * Ask ECharts to align the ticks of the value axes with the ones of the first axis
   */
  @SuppressWarnings("unchecked")
  private static void alignTicks(List<Object> axes) {
    axes.stream().map(axis -> (Map<String, Object>) axis).filter(axis -> "value".equals(axis.get("type")))
      .forEach(axis -> axis.put("alignTicks", true));
  }

  /**
   * Build the model of a single series, for the chart actions that add or replace series at runtime.
   * <p>
   * The series is translated on its own, as in a chart without inversion, stacking or data labels, because the
   * action does not carry the chart. The client applies the chart level options it already has to it
   * ({@code awe.inverted} swaps the axis indexes).
   * </p>
   *
   * @param serie Series element
   * @return Series options, without data
   */
  public Map<String, Object> buildSeries(ChartSerie serie) {
    Chart chart = Chart.builder().type(StringUtils.defaultIfBlank(serie.getType(), "line")).build();
    SeriesTypeResolver typeResolver = new SeriesTypeResolver(chart, reporter);
    return new EChartsSeriesBuilder(chart, false, new ParameterTarget(map()), translator, typeResolver, reporter)
      .build(serie);
  }

  private void putTitle(Map<String, Object> model, Chart chart) {
    if (chart.getLabel() == null && chart.getSubTitle() == null) {
      return;
    }
    Map<String, Object> title = child(model, TITLE);
    if (chart.getLabel() != null) {
      title.put("text", chart.getLabel());
    }
    if (chart.getSubTitle() != null) {
      title.put("subtext", chart.getSubTitle());
    }
    // Highcharts titles are centered
    title.put("left", "center");
  }

  /**
   * A Highcharts {@code bar} series is horizontal, which turns the whole chart sideways
   */
  private static boolean isHorizontalBar(List<ChartSerie> series, SeriesTypeResolver typeResolver) {
    return series.stream().anyMatch(serie -> !isDrilldown(serie) && typeResolver.typeOf(serie).isHorizontalBar());
  }

  /**
   * The chart is drawn on axes unless all its series are pies
   */
  private static boolean isCartesian(List<ChartSerie> series, SeriesTypeResolver typeResolver) {
    List<ChartSerie> mainSeries = series.stream().filter(serie -> !isDrilldown(serie)).toList();
    if (mainSeries.isEmpty()) {
      return !typeResolver.defaultType().isPie();
    }
    return mainSeries.stream().anyMatch(serie -> !typeResolver.typeOf(serie).isPie());
  }

  private static boolean isDrilldown(ChartSerie serie) {
    return Boolean.TRUE.equals(serie.getDrillDown());
  }

  private static List<Object> buildSeries(List<ChartSerie> series, EChartsSeriesBuilder builder, boolean drilldown) {
    return series.stream()
      .filter(serie -> isDrilldown(serie) == drilldown)
      .<Object>map(builder::build)
      .toList();
  }

  /**
   * Zoom of the x axis (inside, plus a slider in stock charts) and, when asked, of the y axis
   */
  private static List<Object> dataZoom(Chart chart, boolean inverted) {
    String zoomType = chart.getZoomType() == null ? "" : chart.getZoomType().toLowerCase(Locale.ROOT);
    boolean zoomX = chart.isStockChart() || List.of("x", "xaxis", "xy", "all").contains(zoomType);
    boolean zoomY = List.of("y", "yaxis", "xy", "all").contains(zoomType);
    // The AWE x axis is the vertical one in an inverted chart
    String awexAxisIndex = inverted ? "yAxisIndex" : "xAxisIndex";
    String aweYAxisIndex = inverted ? "xAxisIndex" : "yAxisIndex";

    List<Object> zoom = new ArrayList<>();
    if (zoomX) {
      zoom.add(zoomOf("inside", awexAxisIndex));
    }
    if (chart.isStockChart()) {
      zoom.add(zoomOf("slider", awexAxisIndex));
    }
    if (zoomY) {
      zoom.add(zoomOf("inside", aweYAxisIndex));
    }
    return zoom;
  }

  private static Map<String, Object> zoomOf(String type, String axisIndexKey) {
    Map<String, Object> zoom = map();
    zoom.put("type", type);
    zoom.put(axisIndexKey, 0);
    return zoom;
  }

  private static void putHints(Map<String, Object> model, Chart chart, boolean inverted, List<Object> drilldown) {
    Map<String, Object> hints = hints(model);
    if (chart.getType() != null) {
      hints.put("chartType", chart.getType().toLowerCase(Locale.ROOT));
    }
    if (StringUtils.isNotBlank(chart.getTheme())) {
      hints.put("theme", chart.getTheme());
    }
    if (inverted) {
      hints.put("inverted", true);
    }
    if (chart.isStockChart()) {
      hints.put("stock", true);
    }
    if (chart.isStacking()) {
      hints.put("stacking", chart.getStacking().toLowerCase(Locale.ROOT));
    }
    if (!drilldown.isEmpty()) {
      child(hints, "drilldown").put("series", drilldown);
    }
  }
}
