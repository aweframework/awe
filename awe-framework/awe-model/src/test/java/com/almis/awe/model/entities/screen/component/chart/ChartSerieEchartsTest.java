package com.almis.awe.model.entities.screen.component.chart;

import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A series sent by a chart action carries its ECharts translation beside the Highcharts fields that AngularJS reads
 */
class ChartSerieEchartsTest {

  private static JsonNode json(ChartSerie serie) {
    return DataListUtil.getMapper().valueToTree(serie);
  }

  @Test
  void theSerializedSerieKeepsItsHighchartsFields() {
    JsonNode json = json(ChartSerie.builder().id("serie1").label("Serie 1").type("column").xValue("dates")
      .yValue("amount").color("#A8E0A6").build());

    assertThat(json.at("/id").asString()).isEqualTo("serie1");
    assertThat(json.at("/label").asString()).isEqualTo("Serie 1");
    assertThat(json.at("/type").asString()).isEqualTo("column");
    assertThat(json.at("/xValue").asString()).isEqualTo("dates");
    assertThat(json.at("/yValue").asString()).isEqualTo("amount");
    assertThat(json.at("/color").asString()).isEqualTo("#A8E0A6");
  }

  @Test
  void theSerializedSerieCarriesTheEChartsSeries() {
    JsonNode json = json(ChartSerie.builder().id("serie1").label("Serie 1").type("column").xValue("dates")
      .yValue("amount").color("#A8E0A6").yAxis("1").build());

    assertThat(json.at("/echarts/id").asString()).isEqualTo("serie1");
    assertThat(json.at("/echarts/name").asString()).isEqualTo("Serie 1");
    assertThat(json.at("/echarts/type").asString()).isEqualTo("bar");
    assertThat(json.at("/echarts/yAxisIndex").asInt()).isEqualTo(1);
    assertThat(json.at("/echarts/itemStyle/color").asString()).isEqualTo("#A8E0A6");
    assertThat(json.at("/echarts/awe/type").asString()).isEqualTo("column");
    assertThat(json.at("/echarts/awe/xValue").asString()).isEqualTo("dates");
    assertThat(json.at("/echarts/awe/yValue").asString()).isEqualTo("amount");
  }

  @Test
  void aSerieWithoutTypeIsAChartLine() {
    JsonNode json = json(ChartSerie.builder().id("serie1").xValue("x").yValue("y").build());

    assertThat(json.at("/echarts/type").asString()).isEqualTo("line");
  }

  @Test
  void aSplineSerieIsASmoothLine() {
    JsonNode json = json(ChartSerie.builder().id("serie1").type("spline").xValue("x").yValue("y").build());

    assertThat(json.at("/echarts/type").asString()).isEqualTo("line");
    assertThat(json.at("/echarts/smooth").asBoolean()).isTrue();
  }

  @Test
  void aPieSerieIsAPie() {
    JsonNode json = json(ChartSerie.builder().id("serie1").type("pie").xValue("name").yValue("y").build());

    assertThat(json.at("/echarts/type").asString()).isEqualTo("pie");
    assertThat(json.at("/echarts/awe/type").asString()).isEqualTo("pie");
  }

  @Test
  void aSerieThatCannotBeTranslatedIsSerializedWithoutEcharts() {
    ChartSerie broken = new ChartSerie() {
      @Override
      public String getXAxis() {
        boolean fromECharts = Arrays.stream(new Throwable().getStackTrace())
          .anyMatch(frame -> frame.getClassName().contains(".chart.echarts."));
        if (fromECharts) {
          throw new IllegalStateException("simulated ECharts failure");
        }
        return super.getXAxis();
      }
    };
    broken.setId("broken");

    JsonNode json = json(broken);

    assertThat(json.has("echarts")).isFalse();
    assertThat(json.at("/id").asString()).isEqualTo("broken");
  }
}
