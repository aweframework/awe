package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartAxis;
import com.almis.awe.model.entities.screen.component.chart.ChartLegend;
import com.almis.awe.model.entities.screen.component.chart.ChartParameter;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import com.almis.awe.model.entities.screen.component.chart.ChartTooltip;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsTestSupport.echarts;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsTestSupport.pathsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chart type, axis, series, legend, tooltip and parameter mappings of the ECharts model
 */
class EChartsModelBuilderTest {

  private static Chart.ChartBuilder<?, ?> chart(String type) {
    return Chart.builder().id("chart").type(type);
  }

  private static ChartSerie.ChartSerieBuilder<?, ?> serie(String id) {
    return ChartSerie.builder().id(id).xValue("x").yValue("y");
  }

  private static ChartParameter parameter(String type, String name, String value, ChartParameter... children) {
    return ChartParameter.builder().type(type).name(name).value(value).parameterList(List.of(children)).build();
  }

  // ------------------------------------------------------------------------------------------------------------
  // Chart and series types
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void simpleChartTypesMapToTheEChartsSeriesTypes() {
    assertThat(seriesOf("line")).containsExactly("line", null, null);
    assertThat(seriesOf("spline")).containsExactly("line", "true", null);
    assertThat(seriesOf("column")).containsExactly("bar", null, null);
    assertThat(seriesOf("column_3d")).containsExactly("bar", null, null);
    assertThat(seriesOf("area")).containsExactly("line", null, "area");
    assertThat(seriesOf("areaspline")).containsExactly("line", "true", "area");
    assertThat(seriesOf("scatter")).containsExactly("scatter", null, null);
    assertThat(seriesOf("bubble")).containsExactly("scatter", null, null);
  }

  /**
   * Resolve [type, smooth, area] of the only series of a chart of the given type
   */
  private static List<String> seriesOf(String chartType) {
    JsonNode serie = echarts(chart(chartType).serieList(List.of(serie("s").build())).build()).at("/series/0");
    List<String> result = new ArrayList<>();
    result.add(serie.at("/type").asText());
    result.add(serie.at("/smooth").isMissingNode() ? null : serie.at("/smooth").asText());
    result.add(serie.at("/areaStyle").isMissingNode() ? null : "area");
    return result;
  }

  @Test
  void rangeAreasAreApproximatedAndReportedOnce() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);
    Chart chart = chart("arearange").serieList(List.of(serie("s").build())).build();

    JsonNode first = echarts(chart, reporter);
    echarts(chart, reporter);

    assertThat(first.at("/series/0/type").asText()).isEqualTo("line");
    assertThat(first.at("/series/0/areaStyle").isObject()).isTrue();
    assertThat(first.at("/series/0/awe/type").asText()).isEqualTo("arearange");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0)).contains("arearange");
  }

  @Test
  void donutAndSemicircleDefaultsComeFromTheirType() {
    JsonNode donut = echarts(chart("donut").serieList(List.of(serie("s").build())).build());
    JsonNode semicircle = echarts(chart("semicircle").serieList(List.of(serie("s").build())).build());
    JsonNode pie = echarts(chart("pie_3d").serieList(List.of(serie("s").build())).build());

    assertThat(donut.at("/series/0/type").asText()).isEqualTo("pie");
    assertThat(donut.at("/series/0/radius")).extracting(JsonNode::asText).containsExactly("50%", "75%");
    assertThat(donut.at("/series/0/startAngle").isMissingNode()).isTrue();
    assertThat(semicircle.at("/series/0/startAngle").asInt()).isEqualTo(180);
    assertThat(semicircle.at("/series/0/endAngle").asInt()).isEqualTo(360);
    assertThat(semicircle.at("/series/0/center")).extracting(JsonNode::asText).containsExactly("50%", "75%");
    assertThat(pie.at("/series/0/type").asText()).isEqualTo("pie");
    assertThat(pie.at("/series/0/radius").isMissingNode()).isTrue();
  }

  @Test
  void mixedChartDefaultsSeriesWithoutTypeToLine() {
    JsonNode model = echarts(chart("mixed").serieList(List.of(
      serie("a").type("column").build(), serie("b").build(), serie("c").type("scatter").build())).build());

    assertThat(model.at("/series")).extracting(serie -> serie.at("/type").asText()).containsExactly("bar", "line", "scatter");
  }

  @Test
  void barSeriesInvertTheWholeChart() {
    JsonNode model = echarts(chart("mixed")
      .xAxisList(List.of(ChartAxis.builder().label("X").type("category").build()))
      .yAxisList(List.of(ChartAxis.builder().label("Y").build()))
      .serieList(List.of(serie("a").type("bar").xAxis("0").yAxis("0").build()))
      .build());

    assertThat(model.at("/awe/inverted").asBoolean()).isTrue();
    assertThat(model.at("/series/0/type").asText()).isEqualTo("bar");
    assertThat(model.at("/xAxis/0/name").asText()).isEqualTo("Y");
    assertThat(model.at("/yAxis/0/name").asText()).isEqualTo("X");
    assertThat(model.at("/yAxis/0/type").asText()).isEqualTo("category");
    assertThat(model.at("/series/0/xAxisIndex").asInt()).isZero();
    assertThat(model.at("/series/0/yAxisIndex").asInt()).isZero();
  }

  @Test
  void unknownSeriesTypeFallsBackToLineAndIsReportedOnce() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);
    Chart chart = chart("mixed").serieList(List.of(serie("a").type("gauge").build())).build();

    JsonNode model = echarts(chart, reporter);
    echarts(chart, reporter);

    assertThat(model.at("/series/0/type").asText()).isEqualTo("line");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0)).contains("gauge");
  }

  // ------------------------------------------------------------------------------------------------------------
  // Stacking, zoom, data labels
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void stackingNormalSharesTheStackOnlyAmongStackableSeries() {
    JsonNode model = echarts(chart("mixed").stacking("normal").serieList(List.of(
      serie("a").type("column").build(), serie("b").type("area").build(), serie("c").type("scatter").build())).build());

    assertThat(model.at("/series/0/stack").asText()).isEqualTo("stack");
    assertThat(model.at("/series/1/stack").asText()).isEqualTo("stack");
    assertThat(model.at("/series/2/stack").isMissingNode()).isTrue();
    assertThat(model.at("/series/0/awe/stackPercent").isMissingNode()).isTrue();
    assertThat(model.at("/awe/stacking").asText()).isEqualTo("normal");
  }

  @Test
  void stackingPercentAddsTheHint() {
    JsonNode model = echarts(chart("column").stacking("percent").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/series/0/stack").asText()).isEqualTo("stack");
    assertThat(model.at("/series/0/awe/stackPercent").asBoolean()).isTrue();
  }

  @Test
  void zoomTypeSelectsTheZoomedAxes() {
    assertThat(zoomOf("x")).containsExactly("inside:x");
    assertThat(zoomOf("xAxis")).containsExactly("inside:x");
    assertThat(zoomOf("y")).containsExactly("inside:y");
    assertThat(zoomOf("yAxis")).containsExactly("inside:y");
    assertThat(zoomOf("xy")).containsExactly("inside:x", "inside:y");
    assertThat(zoomOf("all")).containsExactly("inside:x", "inside:y");
    assertThat(zoomOf(null)).isEmpty();
  }

  private static List<String> zoomOf(String zoomType) {
    JsonNode zoom = echarts(chart("line").zoomType(zoomType).serieList(List.of(serie("a").build())).build())
      .at("/dataZoom");
    List<String> result = new ArrayList<>();
    for (JsonNode item : zoom) {
      result.add(item.at("/type").asText() + ":" + (item.has("xAxisIndex") ? "x" : "y"));
    }
    return result;
  }

  @Test
  void zoomOnAnInvertedChartTargetsTheSwappedAxis() {
    JsonNode model = echarts(chart("column").inverted(true).zoomType("xAxis").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/dataZoom/0/yAxisIndex").asInt()).isZero();
    assertThat(model.at("/dataZoom/0/xAxisIndex").isMissingNode()).isTrue();
  }

  @Test
  void pieChartsDoNotZoom() {
    assertThat(echarts(chart("pie").zoomType("xAxis").stockChart(true).serieList(List.of(serie("a").build())).build())
      .at("/dataZoom").isMissingNode()).isTrue();
  }

  @Test
  void dataLabelsAreEnabledPerSeriesWithTheFormatAsHint() {
    JsonNode model = echarts(chart("column").enableDataLabels(true).formatDataLabels("{y:.1f}").serieList(List.of(
      serie("a").build(), serie("b").build())).build());

    for (JsonNode serie : model.at("/series")) {
      assertThat(serie.at("/label/show").asBoolean()).isTrue();
      assertThat(serie.at("/awe/labelFormat").asText()).isEqualTo("{y:.1f}");
    }
  }

  @Test
  void seriesLevelValuesWinOverChartParameterDefaults() {
    ChartParameter defaults = parameter("object", "plotOptions", null,
      parameter("object", "series", null,
        parameter("object", "dataLabels", null, parameter("boolean", "enabled", "true"),
          parameter("string", "format", "{point.y:.2f}"))));
    JsonNode model = echarts(chart("line").enableDataLabels(true).formatDataLabels("{y}").parameterList(List.of(defaults))
      .serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/awe/labelFormat").asText()).isEqualTo("{y}");
  }

  // ------------------------------------------------------------------------------------------------------------
  // Axes
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void axisTypesFollowHighchartsTypes() {
    assertThat(axisType("datetime")).isEqualTo("time");
    assertThat(axisType("category")).isEqualTo("category");
    assertThat(axisType("linear")).isEqualTo("value");
    assertThat(axisType("logarithmic")).isEqualTo("log");
    assertThat(axisType(null)).isEqualTo("value");
  }

  private static String axisType(String type) {
    Chart chart = chart("line").xAxisList(List.of(ChartAxis.builder().type(type).build()))
      .serieList(List.of(serie("a").build())).build();
    return echarts(chart).at("/xAxis/0/type").asText();
  }

  @Test
  void axisAttributesMapToTheirEChartsOptions() {
    ChartAxis x = ChartAxis.builder().label("X").type("datetime").opposite(true).labelRotation(45f).tickInterval("5")
      .labelFormat("{value:%Y}").build();
    ChartAxis y = ChartAxis.builder().label("Y").allowDecimal(true).opposite(true).build();
    JsonNode model = echarts(chart("line").xAxisList(List.of(x)).yAxisList(List.of(y))
      .serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/xAxis/0/name").asText()).isEqualTo("X");
    assertThat(model.at("/xAxis/0/position").asText()).isEqualTo("top");
    assertThat(model.at("/xAxis/0/axisLabel/rotate").asInt()).isEqualTo(-45);
    assertThat(model.at("/xAxis/0/interval").asInt()).isEqualTo(5);
    assertThat(model.at("/xAxis/0/awe/labelFormat").asText()).isEqualTo("{value:%Y}");
    assertThat(model.at("/xAxis/0/minInterval").isMissingNode()).isTrue();
    assertThat(model.at("/yAxis/0/position").asText()).isEqualTo("right");
    assertThat(model.at("/yAxis/0/minInterval").isMissingNode()).isTrue();
  }

  @Test
  void integerTicksApplyToValueAxesOnly() {
    Chart chart = chart("line")
      .xAxisList(List.of(ChartAxis.builder().type("category").build()))
      .yAxisList(List.of(ChartAxis.builder().build(), ChartAxis.builder().allowDecimal(true).build()))
      .serieList(List.of(serie("a").build())).build();
    JsonNode model = echarts(chart);

    assertThat(model.at("/xAxis/0/minInterval").isMissingNode()).isTrue();
    assertThat(model.at("/yAxis/0/minInterval").asInt()).isEqualTo(1);
    assertThat(model.at("/yAxis/1/minInterval").isMissingNode()).isTrue();
  }

  @Test
  void categoryTickIntervalSkipsLabels() {
    Chart chart = chart("line").xAxisList(List.of(ChartAxis.builder().type("category").tickInterval("3").build()))
      .serieList(List.of(serie("a").build())).build();

    assertThat(echarts(chart).at("/xAxis/0/axisLabel/interval").asInt()).isEqualTo(2);
  }

  @Test
  void cartesianChartsWithoutAxesGetDefaultValueAxes() {
    JsonNode model = echarts(chart("line").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/xAxis")).hasSize(1);
    assertThat(model.at("/xAxis/0/type").asText()).isEqualTo("value");
    assertThat(model.at("/yAxis")).hasSize(1);
    assertThat(echarts(chart("pie").serieList(List.of(serie("a").build())).build()).has("xAxis")).isFalse();
  }

  @Test
  void seriesAxisIndexesFollowTheirAxes() {
    JsonNode model = echarts(chart("line").serieList(List.of(serie("a").xAxis("1").yAxis("2").build())).build());

    assertThat(model.at("/series/0/xAxisIndex").asInt()).isEqualTo(1);
    assertThat(model.at("/series/0/yAxisIndex").asInt()).isEqualTo(2);
  }

  // ------------------------------------------------------------------------------------------------------------
  // Legend, tooltip, title
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void legendMapsLayoutAndAlignment() {
    ChartLegend legend = ChartLegend.builder().enabled(true).layout("vertical").align("left").verticalAlign("top")
      .floating(true).borderWidth(2).label("Title").build();
    JsonNode model = echarts(chart("line").chartLegend(legend).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/legend/show").asBoolean()).isTrue();
    assertThat(model.at("/legend/orient").asText()).isEqualTo("vertical");
    assertThat(model.at("/legend/left").asText()).isEqualTo("left");
    assertThat(model.at("/legend/top").asText()).isEqualTo("top");
    assertThat(model.at("/legend/borderWidth").asInt()).isEqualTo(2);
    assertThat(model.at("/legend/awe/floating").asBoolean()).isTrue();
    assertThat(model.at("/legend/awe/title").asText()).isEqualTo("Title");
  }

  @Test
  void legendDefaultsMatchHighchartsWhenTheElementIsMissing() {
    JsonNode line = echarts(chart("line").serieList(List.of(serie("a").build())).build());
    JsonNode stock = echarts(chart("line").stockChart(true).serieList(List.of(serie("a").build())).build());
    JsonNode pie = echarts(chart("pie").serieList(List.of(serie("a").build())).build());

    assertThat(line.at("/legend/show").asBoolean()).isTrue();
    assertThat(line.at("/legend/left").asText()).isEqualTo("center");
    assertThat(line.at("/legend/top").asText()).isEqualTo("bottom");
    assertThat(stock.at("/legend/show").asBoolean()).isFalse();
    assertThat(pie.at("/legend/show").asBoolean()).isFalse();
  }

  @Test
  void tooltipMapsTriggerPointerAndHints() {
    ChartTooltip tooltip = ChartTooltip.builder().shared(true).crosshairs("all").numberDecimals(2).prefix("$")
      .suffix("€").pointFormat("{point.y}").dateFormat("%Y").build();
    JsonNode model = echarts(chart("line").chartTooltip(tooltip).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/tooltip/show").asBoolean()).isTrue();
    assertThat(model.at("/tooltip/trigger").asText()).isEqualTo("axis");
    assertThat(model.at("/tooltip/axisPointer/type").asText()).isEqualTo("cross");
    assertThat(model.at("/tooltip/awe/crosshairs").asText()).isEqualTo("all");
    assertThat(model.at("/tooltip/awe/numberDecimals").asInt()).isEqualTo(2);
    assertThat(model.at("/tooltip/awe/prefix").asText()).isEqualTo("$");
    assertThat(model.at("/tooltip/awe/suffix").asText()).isEqualTo("€");
    assertThat(model.at("/tooltip/awe/pointFormat").asText()).isEqualTo("{point.y}");
    assertThat(model.at("/tooltip/awe/dateFormat").asText()).isEqualTo("%Y");
  }

  @Test
  void disabledTooltipIsHiddenAndMissingTooltipIsItemTriggered() {
    JsonNode disabled = echarts(chart("line").chartTooltip(ChartTooltip.builder().enabled(false).build())
      .serieList(List.of(serie("a").build())).build());
    JsonNode missing = echarts(chart("line").serieList(List.of(serie("a").build())).build());
    JsonNode pieShared = echarts(chart("pie").chartTooltip(ChartTooltip.builder().shared(true).build())
      .serieList(List.of(serie("a").build())).build());

    assertThat(disabled.at("/tooltip/show").asBoolean()).isFalse();
    assertThat(missing.at("/tooltip/show").asBoolean()).isTrue();
    assertThat(missing.at("/tooltip/trigger").asText()).isEqualTo("item");
    assertThat(pieShared.at("/tooltip/trigger").asText()).isEqualTo("item");
  }

  @Test
  void titleIsCenteredLikeHighcharts() {
    JsonNode model = echarts(chart("line").label("Title").subTitle("Sub").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/title/text").asText()).isEqualTo("Title");
    assertThat(model.at("/title/subtext").asText()).isEqualTo("Sub");
    assertThat(model.at("/title/left").asText()).isEqualTo("center");
    assertThat(echarts(chart("line").build()).at("/title").isMissingNode()).isTrue();
  }

  // ------------------------------------------------------------------------------------------------------------
  // chart-parameter
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void unknownParametersAreIgnoredAndReportedOncePerPath() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);
    ChartParameter unknown = parameter("object", "plotOptions", null,
      parameter("object", "area", null, parameter("integer", "threshold", "5")));
    ChartParameter known = parameter("object", "plotOptions", null,
      parameter("object", "pie", null, parameter("string", "size", "60%")));
    Chart chart = chart("pie").parameterList(List.of(unknown, known)).serieList(List.of(serie("a").build())).build();

    JsonNode first = echarts(chart, reporter);
    JsonNode second = echarts(chart, reporter);

    assertThat(first).isEqualTo(second);
    assertThat(first.at("/series/0/radius").asText()).isEqualTo("60%");
    assertThat(pathsOf(first, "threshold")).isEmpty();
    assertThat(messages).containsExactly("Highcharts chart-parameter 'plotOptions.area.threshold' has no ECharts "
      + "translation yet (scope: chart); it is ignored by the ECharts model");
  }

  @Test
  void unknownParametersOfEachScopeAreReportedWithTheirScope() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);
    Chart chart = chart("line")
      .xAxisList(List.of(ChartAxis.builder().parameterList(List.of(parameter("integer", "gridLineWidth", "0"))).build()))
      .chartTooltip(ChartTooltip.builder().parameterList(List.of(parameter("string", "headerFormat", "x"))).build())
      .chartLegend(ChartLegend.builder().parameterList(List.of(parameter("string", "itemStyle", "x"))).build())
      .serieList(List.of(serie("a").parameterList(List.of(parameter("integer", "lineWidth", "3"))).build()))
      .build();

    echarts(chart, reporter);

    assertThat(messages).hasSize(4)
      .anyMatch(message -> message.contains("'gridLineWidth'") && message.contains("scope: axis"))
      .anyMatch(message -> message.contains("'headerFormat'") && message.contains("scope: tooltip"))
      .anyMatch(message -> message.contains("'itemStyle'") && message.contains("scope: legend"))
      .anyMatch(message -> message.contains("'lineWidth'") && message.contains("scope: series"));
  }

  @Test
  void axisDateTimeLabelFormatsBecomeAHint() {
    ChartParameter formats = parameter("object", "dateTimeLabelFormats", null,
      parameter("string", "day", "%d/%m"), parameter("string", "month", "%m/%Y"));
    JsonNode model = echarts(chart("line").xAxisList(List.of(ChartAxis.builder().type("datetime")
      .parameterList(List.of(formats)).build())).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/xAxis/0/awe/dateTimeLabelFormats/day").asText()).isEqualTo("%d/%m");
    assertThat(model.at("/xAxis/0/awe/dateTimeLabelFormats/month").asText()).isEqualTo("%m/%Y");
  }

  @Test
  void titleParametersMapToTitlePlacement() {
    ChartParameter title = parameter("object", "title", null, parameter("string", "align", "right"),
      parameter("string", "verticalAlign", "bottom"), parameter("integer", "y", "12"));
    JsonNode model = echarts(chart("line").label("T").parameterList(List.of(title)).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/title/text").asText()).isEqualTo("T");
    assertThat(model.at("/title/left").asText()).isEqualTo("right");
    assertThat(model.at("/title/top").asText()).isEqualTo("bottom");
    assertThat(model.at("/title/awe/offsetY").asInt()).isEqualTo(12);
  }

  @Test
  void pieSizeParametersAreCombinedIntoTheRadius() {
    assertThat(radiusWith("size", "80%", null, null).asText()).isEqualTo("80%");
    assertThat(radiusWith(null, null, "donut", null)).extracting(JsonNode::asText).containsExactly("50%", "75%");
    assertThat(radiusWith("innerSize", "20%", "donut", null)).extracting(JsonNode::asText).containsExactly("15%", "75%");
    assertThat(radiusWith("innerSize", "60", "donut", null)).extracting(JsonNode::asText).containsExactly("60", "75%");
    assertThat(radiusWith("size", "50%", "donut", "innerSize")).extracting(JsonNode::asText)
      .containsExactly("25%", "50%");
  }

  /**
   * Radius of a pie built with a size parameter (name, value) and optionally a second innerSize of 50%
   */
  private static JsonNode radiusWith(String name, String value, String chartType, String second) {
    List<ChartParameter> pieParameters = new ArrayList<>();
    if (name != null) {
      pieParameters.add(parameter("string", name, value));
    }
    if (second != null) {
      pieParameters.add(parameter("string", second, "50%"));
    }
    ChartParameter plotOptions = parameter("object", "plotOptions", null, parameter("object", "pie", null,
      pieParameters.toArray(new ChartParameter[0])));
    JsonNode model = echarts(chart(chartType == null ? "pie" : chartType).parameterList(List.of(plotOptions))
      .serieList(List.of(serie("a").build())).build());
    return model.at("/series/0/radius");
  }

  @Test
  void nonFinitePieSizesDoNotProduceAComputedRadius() {
    JsonNode infiniteOuter = radiusOf("donut", parameter("string", "size", "Infinity%"),
      parameter("string", "innerSize", "40%"));
    JsonNode nanInner = radiusOf("donut", parameter("string", "innerSize", "NaN%"));

    // The inner size cannot be made relative to the size, so it is sent as written
    assertThat(infiniteOuter).extracting(JsonNode::asText).containsExactly("40%", "Infinity%");
    assertThat(nanInner).extracting(JsonNode::asText).containsExactly("NaN%", "75%");
  }

  private static JsonNode radiusOf(String chartType, ChartParameter... pieParameters) {
    ChartParameter plotOptions = parameter("object", "plotOptions", null,
      parameter("object", "pie", null, pieParameters));
    return echarts(chart(chartType).parameterList(List.of(plotOptions))
      .serieList(List.of(serie("a").build())).build()).at("/series/0/radius");
  }

  @Test
  void textShadowKeepsFunctionalColorsWithSpaces() {
    JsonNode label = labelWithTextShadow("0px 1px 2px rgba(0, 0, 0, 0.5)");

    assertThat(label.at("/textShadowOffsetX").asInt()).isZero();
    assertThat(label.at("/textShadowOffsetY").asInt()).isEqualTo(1);
    assertThat(label.at("/textShadowBlur").asInt()).isEqualTo(2);
    assertThat(label.at("/textShadowColor").asText()).isEqualTo("rgba(0, 0, 0, 0.5)");
  }

  @Test
  void textShadowWithoutBlurHasNoBlur() {
    JsonNode label = labelWithTextShadow("1px 2px rgba(0, 0, 0, 0.5)");

    assertThat(label.at("/textShadowOffsetX").asInt()).isEqualTo(1);
    assertThat(label.at("/textShadowOffsetY").asInt()).isEqualTo(2);
    assertThat(label.at("/textShadowBlur").asInt()).isZero();
    assertThat(label.at("/textShadowColor").asText()).isEqualTo("rgba(0, 0, 0, 0.5)");
  }

  @Test
  void textShadowKeywordsAndIncompleteShadowsAreDropped() {
    for (String shadow : List.of("contrast", "none", "1px 2px", "1px 2px 3px", "black 1px 2px 3px")) {
      JsonNode label = labelWithTextShadow(shadow);

      assertThat(pathsOf(label, "textShadowColor")).as(shadow).isEmpty();
      assertThat(pathsOf(label, "textShadowBlur")).as(shadow).isEmpty();
    }
  }

  private static JsonNode labelWithTextShadow(String shadow) {
    ChartParameter dataLabels = parameter("object", "dataLabels", null, parameter("boolean", "enabled", "true"),
      parameter("object", "style", null, parameter("string", "textShadow", shadow)));
    ChartParameter plotOptions = parameter("object", "plotOptions", null, parameter("object", "pie", null, dataLabels));
    return echarts(chart("pie").parameterList(List.of(plotOptions)).serieList(List.of(serie("a").build())).build())
      .at("/series/0/label");
  }

  @Test
  void seriesKeysBecomeAHint() {
    ChartParameter keys = parameter("array", "keys", null, parameter("string", "", "name"), parameter("string", "", "y"));
    JsonNode model = echarts(chart("pie").serieList(List.of(serie("a").parameterList(List.of(keys)).build())).build());

    assertThat(model.at("/series/0/awe/keys")).extracting(JsonNode::asText).containsExactly("name", "y");
  }

  @Test
  void dataLabelParametersMapToTheSeriesLabel() {
    ChartParameter dataLabels = parameter("object", "dataLabels", null, parameter("boolean", "enabled", "true"),
      parameter("integer", "distance", "20"), parameter("string", "format", "{point.name}"));
    ChartParameter plotOptions = parameter("object", "plotOptions", null, parameter("object", "pie", null, dataLabels));
    JsonNode model = echarts(chart("pie").parameterList(List.of(plotOptions)).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/label/position").asText()).isEqualTo("outside");
    assertThat(model.at("/series/0/labelLine/length").asInt()).isEqualTo(20);
    assertThat(model.at("/series/0/awe/labelFormat").asText()).isEqualTo("{point.name}");
  }

  // ------------------------------------------------------------------------------------------------------------
  // Series drilldown and hints
  // ------------------------------------------------------------------------------------------------------------

  @Test
  void drilldownSeriesLeaveTheSeriesListAndCarryTheirOwnHints() {
    JsonNode model = echarts(chart("pie").serieList(List.of(
      serie("parent").drillDownSerie("child").build(),
      serie("child").drillDown(true).zValue("z").build())).build());

    assertThat(model.at("/series")).hasSize(1);
    assertThat(model.at("/series/0/awe/drilldown").asText()).isEqualTo("child");
    assertThat(model.at("/awe/drilldown/series")).hasSize(1);
    assertThat(model.at("/awe/drilldown/series/0/id").asText()).isEqualTo("child");
    assertThat(model.at("/awe/drilldown/series/0/awe/zValue").asText()).isEqualTo("z");
  }

  @Test
  void chartWithoutDrilldownHasNoDrilldownHint() {
    JsonNode model = echarts(chart("line").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/awe/drilldown").isMissingNode()).isTrue();
  }

  @Test
  void everythingClientEvaluatedTravelsInsideAweHints() {
    ChartAxis axis = ChartAxis.builder().labelFormat("{value} ºC").formatterFunction("formatCurrencyMagnitude").build();
    ChartTooltip tooltip = ChartTooltip.builder().pointFormat("{point.y}").dateFormat("%Y").build();
    Chart chart = chart("column").enableDataLabels(true).formatDataLabels("{y:.1f}").stockChart(true).theme("blue")
      .xAxisList(List.of(axis)).chartTooltip(tooltip).serieList(List.of(serie("a").build())).build();
    JsonNode model = echarts(chart);

    for (String key : List.of("labelFormat", "formatter", "pointFormat", "dateFormat", "xValue", "yValue", "theme", "stock")) {
      assertThat(pathsOf(model, key)).as(key).isNotEmpty().allMatch(path -> path.contains("/awe/"));
    }
    // Hints are always objects' children, never array items or values
    assertThat(pathsOf(model, "awe")).isNotEmpty().noneMatch(path -> path.matches(".*/\\d+$"));
    // The option can be serialized as plain JSON (no functions)
    JsonNode serialized = new ObjectMapper().valueToTree(chart.getEchartsModel());
    assertThat(serialized.isObject()).isTrue();
  }

  @Test
  void emptyHintsAreNotSent() {
    JsonNode model = echarts(chart("line").chartLegend(ChartLegend.builder().enabled(true).build())
      .chartTooltip(ChartTooltip.builder().build()).serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/legend").has("awe")).isFalse();
    assertThat(model.at("/tooltip").has("awe")).isFalse();
    assertThat(model.at("/series/0/awe/type").asText()).isEqualTo("line");
    assertThat(model.at("/series/0/awe/zValue").isMissingNode()).isTrue();
    assertThat(model.at("/series/0/awe/drilldown").isMissingNode()).isTrue();
  }

  @Test
  void chartWithoutTypeStillBuildsAModel() {
    JsonNode model = echarts(Chart.builder().id("chart").label("T").serieList(List.of(serie("a").build())).build());

    assertThat(model.at("/series/0/type").asText()).isEqualTo("line");
    assertThat(model.at("/title/text").asText()).isEqualTo("T");
  }

  @Test
  void echartsModelIsSerializedBesideTheChartModel() {
    Chart chart = chart("line").serieList(List.of(serie("a").build())).build();
    JsonNode json = new ObjectMapper().valueToTree(chart.getEchartsModel());
    JsonNode highcharts = new ObjectMapper().valueToTree(chart.getChartModel());

    assertThat(json.at("/series")).hasSize(1);
    assertThat(highcharts.has("echartsModel")).isFalse();
    assertThat(highcharts.at("/series/0/id").asText()).isEqualTo("a");
  }
}
