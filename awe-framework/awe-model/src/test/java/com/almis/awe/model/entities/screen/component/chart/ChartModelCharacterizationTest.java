package com.almis.awe.model.entities.screen.component.chart;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the Highcharts {@code chartModel} output of the chart test screens.
 * <p>
 * AngularJS and the server-side chart service consume this model, so the ECharts model that sits beside it
 * must never alter it. The golden files under {@code src/test/resources/chart/golden} were captured before
 * the ECharts model existed.
 * </p>
 */
class ChartModelCharacterizationTest {

  private static final ObjectMapper MAPPER = JsonMapper.builder()
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .build();

  @Test
  void chrTstChartModelsAreUnchanged() throws IOException {
    assertGolden("ChrTst", ChartFixtures.loadCharts("/chart/ChrTst.xml"), 10);
  }

  @Test
  void dynamicSeriesChartModelIsUnchanged() throws IOException {
    assertGolden("ChrTstDynamicSeries", ChartFixtures.loadCharts("/chart/ChrTstDynamicSeries.xml"), 1);
  }

  private void assertGolden(String screen, Map<String, Chart> charts, int expectedCount) throws IOException {
    assertThat(charts).hasSize(expectedCount);
    for (Map.Entry<String, Chart> entry : charts.entrySet()) {
      String golden = "/chart/golden/" + screen + "-" + entry.getKey() + ".chartModel.json";
      JsonNode expected;
      try (InputStream stream = getClass().getResourceAsStream(golden)) {
        assertThat(stream).as("golden file " + golden).isNotNull();
        expected = MAPPER.readTree(stream);
      }
      JsonNode actual = MAPPER.valueToTree(entry.getValue().getChartModel());
      assertThat(actual).as("chartModel of " + screen + "/" + entry.getKey()).isEqualTo(expected);
    }
  }
}
