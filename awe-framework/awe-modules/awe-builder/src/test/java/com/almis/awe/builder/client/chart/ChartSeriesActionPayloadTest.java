package com.almis.awe.builder.client.chart;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The series of the chart actions are sent with their ECharts translation, beside the fields of the AngularJS client
 */
class ChartSeriesActionPayloadTest {

  private final ChartSerie serie = ChartSerie.builder().id("serie1").label("Serie 1").type("column")
    .xValue("dates").yValue("amount").build();

  private JsonNode payload(ClientAction action) {
    return DataListUtil.getMapper().valueToTree(action).at("/parameters/series/0");
  }

  @Test
  void addChartSeriesCarriesTheEChartsSeries() {
    JsonNode json = payload(new AddChartSeriesActionBuilder("chart", serie).build());

    assertEquals("serie1", json.at("/id").asText());
    assertEquals("column", json.at("/type").asText());
    assertEquals("bar", json.at("/echarts/type").asText());
    assertEquals("amount", json.at("/echarts/awe/yValue").asText());
  }

  @Test
  void replaceChartSeriesCarriesTheEChartsSeries() {
    JsonNode json = payload(new ReplaceChartSeriesActionBuilder("chart", serie).build());

    assertEquals("bar", json.at("/echarts/type").asText());
    assertEquals("dates", json.at("/echarts/awe/xValue").asText());
  }

  @Test
  void removeChartSeriesCarriesTheSerieIdentifier() {
    JsonNode json = payload(new RemoveChartSeriesActionBuilder("chart", serie).build());

    assertEquals("serie1", json.at("/id").asText());
    assertEquals("serie1", json.at("/echarts/id").asText());
  }

  @Test
  void addPointsStillSendsTheDataList() {
    ClientAction action = new AddPointsActionBuilder("chart", new DataList()).build();

    assertTrue(action.getParameters().containsKey("data"));
  }
}
