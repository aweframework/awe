package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsTestSupport.echarts;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Radar, heatmap, gauge and funnel chart types of the ECharts model, and the range hint
 */
class EChartsModelRadarHeatmapGaugeFunnelTest {

  private static Chart.ChartBuilder<?, ?> chart(String type) {
    return Chart.builder().id("chart").type(type);
  }

  private static ChartSerie.ChartSerieBuilder<?, ?> serie(String id) {
    return ChartSerie.builder().id(id).xValue("x").yValue("y");
  }

  private static JsonNode model(String type) {
    return echarts(chart(type).serieList(List.of(serie("s").zValue("z").build())).build());
  }

  @Test
  void newChartTypesMapToTheirEChartsSeriesType() {
    for (String type : List.of("radar", "heatmap", "gauge", "funnel")) {
      JsonNode serie = model(type).at("/series/0");

      assertThat(serie.at("/type").asString()).isEqualTo(type);
      assertThat(serie.at("/smooth").isMissingNode()).isTrue();
      assertThat(serie.at("/areaStyle").isMissingNode()).isTrue();
      assertThat(serie.at("/awe/type").asString()).isEqualTo(type);
    }
  }

  @Test
  void seriesTypesOfAMixedChartResolveTheNewTypes() {
    JsonNode model = echarts(chart("mixed").serieList(List.of(serie("a").type("funnel").build())).build());

    assertThat(model.at("/series/0/type").asString()).isEqualTo("funnel");
  }

  @Test
  void radarGaugeAndFunnelHaveNoAxesNorZoom() {
    for (String type : List.of("radar", "gauge", "funnel")) {
      JsonNode model = echarts(chart(type).zoomType("xy").serieList(List.of(
        serie("s").xAxis("0").yAxis("0").build())).build());

      assertThat(model.has("xAxis")).as(type).isFalse();
      assertThat(model.has("yAxis")).as(type).isFalse();
      assertThat(model.has("dataZoom")).as(type).isFalse();
      assertThat(model.at("/series/0/xAxisIndex").isMissingNode()).as(type).isTrue();
      assertThat(model.at("/series/0/yAxisIndex").isMissingNode()).as(type).isTrue();
    }
  }

  @Test
  void axisFreeChartsUseItemTooltipAndHiddenLegendByDefault() {
    JsonNode model = echarts(chart("funnel").chartTooltip(
      com.almis.awe.model.entities.screen.component.chart.ChartTooltip.builder().shared(true).build())
      .serieList(List.of(serie("s").build())).build());

    assertThat(model.at("/tooltip/trigger").asString()).isEqualTo("item");
    assertThat(model.at("/legend/show").asBoolean()).isFalse();
  }

  @Test
  void aChartWithAxisSeriesAndARadarKeepsItsAxes() {
    JsonNode model = echarts(chart("mixed").serieList(List.of(
      serie("a").type("radar").build(), serie("b").type("line").build())).build());

    assertThat(model.at("/xAxis").isArray()).isTrue();
  }

  @Test
  void radarAddsTheRadarComponentForTheClientToFill() {
    JsonNode model = model("radar");

    assertThat(model.at("/radar").isObject()).isTrue();
    assertThat(model.at("/radar").size()).isZero();
    assertThat(model("gauge").has("radar")).isFalse();
  }

  @Test
  void heatmapHasCategoryAxesAndNoZoomByDefault() {
    JsonNode model = model("heatmap");

    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("category");
    assertThat(model.at("/yAxis/0/type").asString()).isEqualTo("category");
    assertThat(model.at("/xAxis/0/minInterval").isMissingNode()).isTrue();
    assertThat(model.has("dataZoom")).isFalse();
  }

  @Test
  void heatmapAxesDeclaredInTheXmlAreCategoryToo() {
    JsonNode model = echarts(chart("heatmap")
      .xAxisList(List.of(com.almis.awe.model.entities.screen.component.chart.ChartAxis.builder().type("linear").build()))
      .serieList(List.of(serie("s").build())).build());

    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("category");
  }

  @Test
  void mixedChartWithAHeatmapKeepsTheAxisTypesOfItsOtherSeries() {
    JsonNode model = echarts(chart("mixed")
      .xAxisList(List.of(com.almis.awe.model.entities.screen.component.chart.ChartAxis.builder().type("linear").build()))
      .serieList(List.of(serie("cells").type("heatmap").zValue("z").build(), serie("trend").type("line").build()))
      .build());

    assertThat(model.at("/xAxis/0/type").asString()).isEqualTo("value");
  }

  @Test
  void heatmapZoomIsOnlyAddedWhenTheXmlAsksForIt() {
    JsonNode model = echarts(chart("heatmap").zoomType("x").serieList(List.of(serie("s").build())).build());

    assertThat(model.at("/dataZoom/0/type").asString()).isEqualTo("inside");
  }

  @Test
  void heatmapCarriesAContinuousVisualMapForTheClientToFill() {
    JsonNode visualMap = model("heatmap").at("/visualMap");

    assertThat(visualMap.at("/type").asString()).isEqualTo("continuous");
    assertThat(visualMap.at("/calculable").asBoolean()).isTrue();
    assertThat(visualMap.at("/orient").asString()).isEqualTo("horizontal");
    assertThat(visualMap.at("/left").asString()).isEqualTo("center");
    assertThat(visualMap.at("/bottom").asInt()).isZero();
    assertThat(model("line").has("visualMap")).isFalse();
  }

  @Test
  void heatmapIsNotStackedNorInverted() {
    JsonNode model = echarts(chart("heatmap").stacking("normal").serieList(List.of(serie("s").build())).build());

    assertThat(model.at("/series/0/stack").isMissingNode()).isTrue();
    assertThat(model.at("/awe/inverted").isMissingNode()).isTrue();
  }

  @Test
  void seriesKeepTheirValueHintsForTheClientBinding() {
    JsonNode gauge = model("gauge").at("/series/0/awe");
    JsonNode funnel = model("funnel").at("/series/0/awe");

    assertThat(gauge.at("/yValue").asString()).isEqualTo("y");
    assertThat(gauge.at("/zValue").asString()).isEqualTo("z");
    assertThat(funnel.at("/xValue").asString()).isEqualTo("x");
    assertThat(funnel.at("/yValue").asString()).isEqualTo("y");
  }

  @Test
  void rangeSeriesCarryTheRangeHint() {
    assertThat(model("arearange").at("/series/0/awe/range").asBoolean()).isTrue();
    assertThat(model("areasplinerange").at("/series/0/awe/range").asBoolean()).isTrue();
    assertThat(model("area").at("/series/0/awe/range").isMissingNode()).isTrue();
  }

  @Test
  void legacyHighchartsModelAcceptsTheNewTypes() {
    for (String type : List.of("radar", "heatmap", "gauge", "funnel")) {
      Chart chart = chart(type).serieList(List.of(serie("s").build())).build();

      assertThat(chart.getChartModel()).isNotEmpty();
    }
  }
}
