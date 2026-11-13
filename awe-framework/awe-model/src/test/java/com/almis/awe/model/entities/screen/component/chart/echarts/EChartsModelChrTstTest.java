package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsTestSupport.echarts;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ECharts model of every chart of the AWE chart test screen (ChrTst) and of the dynamic series screen
 */
class EChartsModelChrTstTest {

  private static Map<String, Chart> charts;
  private static Map<String, Chart> dynamicCharts;

  @BeforeAll
  static void loadScreens() {
    charts = ChartFixtures.loadCharts("/chart/ChrTst.xml");
    dynamicCharts = ChartFixtures.loadCharts("/chart/ChrTstDynamicSeries.xml");
  }

  @Test
  void mixedChartHasTitleAndTwoYAxesWithTheOppositeOneOnTheRight() {
    JsonNode model = echarts(charts.get("ChrLinTst"));

    assertThat(model.at("/title/text").asString()).isEqualTo("SCREEN_TEXT_CHART_TITLE_1");
    assertThat(model.at("/title/subtext").asString()).isEqualTo("Subtitulo grafico 1");
    assertThat(model.at("/awe/chartType").asString()).isEqualTo("mixed");

    assertThat(model.at("/xAxis")).hasSize(1);
    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("time");
    assertThat(model.at("/xAxis/0/name").asString()).isEqualTo("Fechassss");

    assertThat(model.at("/yAxis")).hasSize(2);
    assertThat(model.at("/yAxis/0/type").asString()).isEqualTo("value");
    assertThat(model.at("/yAxis/0/awe/formatter").asString()).isEqualTo("formatCurrencyMagnitude");
    assertThat(model.at("/yAxis/0/position").isMissingNode()).isTrue();
    assertThat(model.at("/yAxis/1/position").asString()).isEqualTo("right");
    assertThat(model.at("/yAxis/1/name").asString()).isEqualTo("Lluvias (mm)");
  }

  @Test
  void mixedChartKeepsEachSeriesTypeAndAxis() {
    JsonNode model = echarts(charts.get("ChrLinTst"));

    assertThat(model.at("/series")).hasSize(2);
    assertThat(model.at("/series/0/id").asString()).isEqualTo("serie-1");
    assertThat(model.at("/series/0/name").asString()).isEqualTo("Serie 1");
    assertThat(model.at("/series/0/type").asString()).isEqualTo("bar");
    assertThat(model.at("/series/0/yAxisIndex").asInt()).isZero();
    assertThat(model.at("/series/0/itemStyle/color").asString()).isEqualTo("#A8E0A6");
    assertThat(model.at("/series/0/awe/xValue").asString()).isEqualTo("dates");
    assertThat(model.at("/series/0/awe/yValue").asString()).isEqualTo("serie1");
    assertThat(model.at("/series/1/type").asString()).isEqualTo("line");
    assertThat(model.at("/series/1/smooth").asBoolean()).isTrue();
    assertThat(model.at("/series/1/yAxisIndex").asInt()).isEqualTo(1);
    assertThat(model.at("/series/1/awe/type").asString()).isEqualTo("spline");
  }

  @Test
  void mixedChartHasHiddenLegendSharedTooltipAndXZoom() {
    JsonNode model = echarts(charts.get("ChrLinTst"));

    assertThat(model.at("/legend/show").asBoolean()).isFalse();
    assertThat(model.at("/legend/awe/title").asString()).isEqualTo("Leyenda");
    assertThat(model.at("/tooltip/trigger").asString()).isEqualTo("axis");
    assertThat(model.at("/tooltip/axisPointer/type").asString()).isEqualTo("line");
    assertThat(model.at("/tooltip/awe/suffix").asString()).isEqualTo(" ºC");
    assertThat(model.at("/tooltip/awe/numberDecimals").asInt()).isEqualTo(3);
    assertThat(model.at("/dataZoom")).hasSize(1);
    assertThat(model.at("/dataZoom/0/type").asString()).isEqualTo("inside");
    assertThat(model.at("/dataZoom/0/xAxisIndex").asInt()).isZero();
  }

  @Test
  void stackedColumn3dRendersFlatAndSharesOneStack() {
    JsonNode model = echarts(charts.get("ChrBarTst"));

    assertThat(model.at("/awe/chartType").asString()).isEqualTo("column_3d");
    assertThat(model.at("/awe/stacking").asString()).isEqualTo("normal");
    assertThat(model.at("/series")).hasSize(3);
    for (JsonNode serie : model.at("/series")) {
      assertThat(serie.at("/type").asString()).isEqualTo("bar");
      assertThat(serie.at("/stack").asString()).isEqualTo("stack");
    }
    assertThat(model.at("/legend/orient").asString()).isEqualTo("vertical");
    assertThat(model.at("/legend/left").asString()).isEqualTo("right");
    assertThat(model.at("/legend/top").asString()).isEqualTo("middle");
    assertThat(EChartsTestSupport.pathsOf(model, "options3d")).isEmpty();
    assertThat(EChartsTestSupport.pathsOf(model, "depth")).isEmpty();
  }

  @Test
  void invertedColumnSwapsAxesAndKeepsDataBindingRoles() {
    JsonNode model = echarts(charts.get("ChrBarHorTst"));

    assertThat(model.at("/awe/inverted").asBoolean()).isTrue();
    // The horizontal axis is the AWE y axis
    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("value");
    assertThat(model.at("/xAxis/0/name").asString()).isEqualTo("Temperaturas (ºC)");
    assertThat(model.at("/xAxis/0/interval").asInt()).isEqualTo(1);
    assertThat(model.at("/xAxis/0/awe/labelFormat").asString()).isEqualTo("{value} ºC");
    assertThat(model.at("/xAxis/0/awe/axis").asString()).isEqualTo("y");
    // The vertical axis is the AWE x axis
    assertThat(model.at("/yAxis/0/type").asString()).isEqualTo("time");
    assertThat(model.at("/yAxis/0/interval").asInt()).isEqualTo(259200);
    assertThat(model.at("/yAxis/0/inverse").asBoolean()).isTrue();
    assertThat(model.at("/yAxis/0/awe/axis").asString()).isEqualTo("x");

    assertThat(model.at("/series/0/type").asString()).isEqualTo("bar");
    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/awe/labelFormat").asString()).isEqualTo("{y:.3f}");
    assertThat(model.at("/series/0/awe/xValue").asString()).isEqualTo("dates");
    assertThat(model.at("/legend/borderWidth").asInt()).isEqualTo(1);
    assertThat(model.at("/tooltip/awe/dateFormat").asString()).isEqualTo("%Y-%m-%d");
  }

  @Test
  void stockChartAddsInsideAndSliderZoomAndThemeHint() {
    JsonNode model = echarts(charts.get("ChrStockTst"));

    assertThat(model.at("/awe/stock").asBoolean()).isTrue();
    assertThat(model.at("/dataZoom")).hasSize(2);
    assertThat(model.at("/dataZoom/0/type").asString()).isEqualTo("inside");
    assertThat(model.at("/dataZoom/0/xAxisIndex").asInt()).isZero();
    assertThat(model.at("/dataZoom/1/type").asString()).isEqualTo("slider");
    assertThat(model.at("/dataZoom/1/xAxisIndex").asInt()).isZero();
    assertThat(model.at("/series/0/type").asString()).isEqualTo("line");
    assertThat(model.at("/series/0/smooth").isMissingNode()).isTrue();
    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("time");
    assertThat(model.at("/yAxis/0/awe/labelFormat").asString()).isEqualTo("{value} ºC");
    assertThat(model.at("/tooltip/awe/numberDecimals").asInt()).isEqualTo(2);
  }

  @Test
  void areasplineIsASmoothAreaLine() {
    JsonNode model = echarts(charts.get("ChrAreTst"));

    assertThat(model.at("/series")).hasSize(2);
    assertThat(model.at("/series/0/type").asString()).isEqualTo("line");
    assertThat(model.at("/series/0/smooth").asBoolean()).isTrue();
    assertThat(model.at("/series/0/areaStyle").isObject()).isTrue();
    assertThat(model.at("/xAxis/0/awe/dateTimeLabelFormats/day").asString()).isEqualTo("%Y-%m-%d");
    assertThat(model.at("/legend/left").asString()).isEqualTo("right");
    assertThat(model.at("/legend/top").asString()).isEqualTo("top");
    assertThat(model.at("/legend/awe/floating").asBoolean()).isTrue();
  }

  @Test
  void pieWithDrilldownMovesTheChildSeriesToTheHints() {
    JsonNode model = echarts(charts.get("ChrPieTst"));

    assertThat(model.at("/xAxis").isMissingNode()).isTrue();
    assertThat(model.at("/yAxis").isMissingNode()).isTrue();
    assertThat(model.at("/series")).hasSize(1);
    assertThat(model.at("/series/0/id").asString()).isEqualTo("serie1");
    assertThat(model.at("/series/0/type").asString()).isEqualTo("pie");
    assertThat(model.at("/series/0/radius").asString()).isEqualTo("75%");
    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/awe/labelFormat").asString()).isEqualTo("<b>{point.name}</b>: {point.percentage:.1f} %");
    assertThat(model.at("/series/0/awe/drilldown").asString()).isEqualTo("serie1_1");
    assertThat(model.at("/series/0/awe/keys")).extracting(JsonNode::asString).containsExactly("name", "y", "drilldown");

    assertThat(model.at("/awe/drilldown/series")).hasSize(1);
    assertThat(model.at("/awe/drilldown/series/0/id").asString()).isEqualTo("serie1_1");
    assertThat(model.at("/awe/drilldown/series/0/name").asString()).isEqualTo("SubThemes");
    assertThat(model.at("/awe/drilldown/series/0/type").asString()).isEqualTo("pie");
    assertThat(model.at("/awe/drilldown/series/0/awe/yValue").asString()).isEqualTo("subserie1");

    assertThat(model.at("/legend/show").asBoolean()).isTrue();
    assertThat(model.at("/tooltip/trigger").asString()).isEqualTo("item");
  }

  @Test
  void donut3dRendersAsFlatDonutWithParameterRadius() {
    JsonNode model = echarts(charts.get("ChrDonutTst"));

    assertThat(model.at("/awe/chartType").asString()).isEqualTo("donut_3d");
    assertThat(model.at("/series/0/type").asString()).isEqualTo("pie");
    // innerSize is relative to the pie size in Highcharts: 40% of 75%
    assertThat(model.at("/series/0/radius")).extracting(JsonNode::asString).containsExactly("30%", "75%");
    assertThat(EChartsTestSupport.pathsOf(model, "options3d")).isEmpty();
  }

  @Test
  void semicircleOpensUpwardWithInsideLabelsAndCenteredTitle() {
    JsonNode model = echarts(charts.get("ChrSemiCircleTst"));

    assertThat(model.at("/series/0/type").asString()).isEqualTo("pie");
    assertThat(model.at("/series/0/startAngle").asInt()).isEqualTo(180);
    assertThat(model.at("/series/0/endAngle").asInt()).isEqualTo(360);
    assertThat(model.at("/series/0/center")).extracting(JsonNode::asString).containsExactly("50%", "75%");
    assertThat(model.at("/series/0/radius")).extracting(JsonNode::asString).containsExactly("30%", "75%");
    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/label/position").asString()).isEqualTo("inside");
    assertThat(model.at("/series/0/label/fontWeight").asString()).isEqualTo("bold");
    assertThat(model.at("/series/0/label/color").asString()).isEqualTo("white");
    assertThat(model.at("/series/0/label/textShadowColor").asString()).isEqualTo("black");
    assertThat(model.at("/series/0/label/textShadowOffsetY").asInt()).isEqualTo(1);
    assertThat(model.at("/series/0/label/textShadowBlur").asInt()).isEqualTo(2);
    assertThat(model.at("/title/left").asString()).isEqualTo("center");
    assertThat(model.at("/title/top").asString()).isEqualTo("middle");
    assertThat(model.at("/title/awe/offsetY").asInt()).isEqualTo(50);
  }

  @Test
  void bubbleIsAScatterWithZValueHint() {
    JsonNode model = echarts(charts.get("ChrBubTst"));

    assertThat(model.at("/series/0/type").asString()).isEqualTo("scatter");
    assertThat(model.at("/series/0/awe/type").asString()).isEqualTo("bubble");
    assertThat(model.at("/series/0/awe/xValue").asString()).isEqualTo("Ord");
    assertThat(model.at("/series/0/awe/yValue").asString()).isEqualTo("serie1_1");
    assertThat(model.at("/series/0/awe/zValue").asString()).isEqualTo("serie1_2");
    assertThat(model.at("/series/0/label/show").asBoolean()).isTrue();
    assertThat(model.at("/series/0/awe/labelFormat").asString()).isEqualTo("{point.y:.2f}");
    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("time");
    assertThat(model.at("/awe/chartType").asString()).isEqualTo("bubble");
  }

  @Test
  void scatterKeepsItsOwnTypeAndDataBinding() {
    JsonNode model = echarts(charts.get("ChrScaTst"));

    assertThat(model.at("/series/0/type").asString()).isEqualTo("scatter");
    assertThat(model.at("/series/0/awe/type").asString()).isEqualTo("scatter");
    assertThat(model.at("/series/0/awe/zValue").asString()).isEqualTo("serie1_2");
    assertThat(model.at("/series/0/label").isMissingNode()).isTrue();
    assertThat(model.at("/title/subtext").asString()).isEqualTo("Subtitulo grafico 10");
  }

  @Test
  void dynamicSeriesLineHasNoSeriesAndACategoryAxis() {
    JsonNode model = echarts(dynamicCharts.get("ChrLinTst"));

    assertThat(model.at("/series").isArray()).isTrue();
    assertThat(model.at("/series")).isEmpty();
    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("category");
    assertThat(model.at("/xAxis/0/name").asString()).isEqualTo("SCREEN_TEXT_MONTHS");
    assertThat(model.at("/yAxis/0/type").asString()).isEqualTo("value");
    assertThat(model.at("/awe/chartType").asString()).isEqualTo("line");
    assertThat(model.at("/legend/orient").asString()).isEqualTo("vertical");
    assertThat(model.at("/legend/awe/title").asString()).isEqualTo("SCREEN_TEXT_LEGEND");
  }
}
