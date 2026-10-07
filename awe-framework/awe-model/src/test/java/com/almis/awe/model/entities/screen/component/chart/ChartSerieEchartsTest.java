package com.almis.awe.model.entities.screen.component.chart;

import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

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

    assertThat(json.at("/id").asText()).isEqualTo("serie1");
    assertThat(json.at("/label").asText()).isEqualTo("Serie 1");
    assertThat(json.at("/type").asText()).isEqualTo("column");
    assertThat(json.at("/xValue").asText()).isEqualTo("dates");
    assertThat(json.at("/yValue").asText()).isEqualTo("amount");
    assertThat(json.at("/color").asText()).isEqualTo("#A8E0A6");
  }

  @Test
  void theSerializedSerieCarriesTheEChartsSeries() {
    JsonNode json = json(ChartSerie.builder().id("serie1").label("Serie 1").type("column").xValue("dates")
      .yValue("amount").color("#A8E0A6").yAxis("1").build());

    assertThat(json.at("/echarts/id").asText()).isEqualTo("serie1");
    assertThat(json.at("/echarts/name").asText()).isEqualTo("Serie 1");
    assertThat(json.at("/echarts/type").asText()).isEqualTo("bar");
    assertThat(json.at("/echarts/yAxisIndex").asInt()).isEqualTo(1);
    assertThat(json.at("/echarts/itemStyle/color").asText()).isEqualTo("#A8E0A6");
    assertThat(json.at("/echarts/awe/type").asText()).isEqualTo("column");
    assertThat(json.at("/echarts/awe/xValue").asText()).isEqualTo("dates");
    assertThat(json.at("/echarts/awe/yValue").asText()).isEqualTo("amount");
  }

  @Test
  void aSerieWithoutTypeIsAChartLine() {
    JsonNode json = json(ChartSerie.builder().id("serie1").xValue("x").yValue("y").build());

    assertThat(json.at("/echarts/type").asText()).isEqualTo("line");
  }

  @Test
  void aSplineSerieIsASmoothLine() {
    JsonNode json = json(ChartSerie.builder().id("serie1").type("spline").xValue("x").yValue("y").build());

    assertThat(json.at("/echarts/type").asText()).isEqualTo("line");
    assertThat(json.at("/echarts/smooth").asBoolean()).isTrue();
  }

  @Test
  void aPieSerieIsAPie() {
    JsonNode json = json(ChartSerie.builder().id("serie1").type("pie").xValue("name").yValue("y").build());

    assertThat(json.at("/echarts/type").asText()).isEqualTo("pie");
    assertThat(json.at("/echarts/awe/type").asText()).isEqualTo("pie");
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
    assertThat(json.at("/id").asText()).isEqualTo("broken");
  }
}
