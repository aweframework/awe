package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsTestSupport.echarts;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ECharts model of the charts of the advanced options screen: charts of business applications with their exact
 * attributes and raw Highcharts options
 */
class EChartsModelAdvancedOptionsTest {

  private static Map<String, Chart> charts;

  @BeforeAll
  static void loadScreen() {
    charts = ChartFixtures.loadCharts("/chart/AdvancedCharts.xml");
  }

  private static JsonNode model(String id) {
    return echarts(charts.get(id));
  }

  @Test
  void everyChartOfTheScreenIsLoaded() {
    assertThat(charts.keySet()).containsExactly("AdvPieLabels", "AdvStock", "AdvBubble", "AdvStacked", "AdvPyramid",
      "AdvArea", "AdvColumns", "AdvPie3d");
  }

  @Test
  void noAdvancedOptionIsReportedAsUntranslated() {
    List<String> messages = new ArrayList<>();
    UnsupportedOptionReporter reporter = new UnsupportedOptionReporter(messages::add);

    charts.values().forEach(chart -> echarts(chart, reporter));

    assertThat(messages).isEmpty();
  }

  @Test
  void pyramidHasStackedRoundedBarsWithHiddenDummies() {
    JsonNode model = model("AdvPyramid");

    assertThat(model.at("/awe/inverted").asBoolean()).isTrue();
    assertThat(model.at("/xAxis")).hasSize(1);
    assertThat(model.at("/yAxis")).hasSize(2);
    assertThat(model.at("/series")).hasSize(4);
    // The pyramid has no stacking: Highcharts ignores the stack of its series and so does the ECharts model
    assertThat(model.at("/series/0").has("stack")).isFalse();
    assertThat(model.at("/series/0/awe/userOptions/stack").asString()).isEqualTo("salary");
    assertThat(model.at("/series/0/awe/borderRadius").asInt()).isEqualTo(20);
    assertThat(model.at("/series/0/awe/userOptions/fullname").asString()).isEqualTo("Salario bruto medio hombres");
    assertThat(model.at("/series/0/awe/labelFormat").asString()).contains("{#if (gt y 0)}");
    assertThat(model.at("/series/2/silent").asBoolean()).isTrue();
    assertThat(model.at("/series/2/itemStyle/opacity").asInt(-1)).isZero();
    assertThat(model.at("/series/3/awe/linkedTo").asString()).isEqualTo(":previous");
    assertThat(model.at("/series/3/awe/showInLegend").asBoolean(true)).isFalse();
    assertThat(model.at("/tooltip/awe/useHTML").asBoolean()).isTrue();
    assertThat(model.at("/tooltip/awe/footerFormat").asString()).isEqualTo("</table>");
    assertThat(model.at("/xAxis/0/awe/labelFormat").asString()).contains("{multiply value 0.001}k");
    assertThat(model.at("/xAxis/0/splitLine/show").asBoolean(true)).isFalse();
  }

  @Test
  void gradientAreaHasGradientMarkersAndAThreshold() {
    JsonNode serie = model("AdvArea").at("/series/0");

    assertThat(serie.at("/areaStyle/color/type").asString()).isEqualTo("linear");
    assertThat(serie.at("/areaStyle/color/colorStops")).hasSize(2);
    assertThat(serie.at("/areaStyle/origin").asString()).isEqualTo("start");
    assertThat(serie.at("/lineStyle/width").asInt()).isEqualTo(2);
    assertThat(serie.at("/itemStyle/color").asString()).isEqualTo("#8cac41");
    assertThat(serie.at("/awe/markerFill").asString()).isEqualTo("#FFFFFF");
    assertThat(serie.at("/symbol").asString()).isEqualTo("circle");
    assertThat(serie.at("/symbolSize").asInt()).isEqualTo(8);
    assertThat(model("AdvArea").at("/xAxis/0/splitLine/lineStyle/width").asDouble()).isEqualTo(0.5);
    assertThat(model("AdvArea").at("/xAxis/0/awe/dateTimeLabelFormats/month").asString()).isEqualTo("%B %Y");
  }

  @Test
  void roundedColumnsKeepTheLegendOrderAndTheRadius() {
    JsonNode model = model("AdvColumns");

    assertThat(model.at("/series/0/awe/borderRadius").asString()).isEqualTo("30%");
    assertThat(model.at("/series/0/awe/legendIndex").asInt()).isEqualTo(2);
    assertThat(model.at("/series/1/awe/legendIndex").asInt()).isEqualTo(1);
    assertThat(model.at("/series/0/barGap").asString()).isEqualTo("5%");
    assertThat(model.at("/series/0/barCategoryGap").asString()).isEqualTo("10%");
  }

  @Test
  void pieWithColorsSelectsAndColorsByPoint() {
    JsonNode model = model("AdvPie3d");

    assertThat(model.at("/color")).hasSize(5);
    assertThat(model.at("/color/0").asString()).isEqualTo("rgba(201,229,134,0.9)");
    assertThat(model.at("/series/0/colorBy").asString()).isEqualTo("data");
    assertThat(model.at("/series/0/selectedMode").asString()).isEqualTo("single");
    assertThat(model.at("/series/0/labelLine/lineStyle/color").asString()).isEqualTo("rgba(128,128,128,0.7)");
  }

  @Test
  void fitChartsKeepTheirPieFontAndBubbleSizes() {
    assertThat(model("AdvPieLabels").at("/series/0/label/fontSize").asInt()).isEqualTo(8);
    assertThat(model("AdvBubble").at("/series/0/awe/minSize").asInt()).isEqualTo(10);
    assertThat(model("AdvBubble").at("/series/0/awe/maxSize").asInt()).isEqualTo(40);
    assertThat(model("AdvBubble").at("/series/0/label/show").asBoolean()).isTrue();
  }
}
