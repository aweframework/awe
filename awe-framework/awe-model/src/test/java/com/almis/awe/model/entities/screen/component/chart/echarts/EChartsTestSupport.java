package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers shared by the ECharts model tests
 */
final class EChartsTestSupport {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private EChartsTestSupport() {
  }

  /**
   * Retrieve the ECharts model of a chart as the JSON the clients receive
   *
   * @param chart Chart
   * @return JSON tree of the model
   */
  static JsonNode echarts(Chart chart) {
    return MAPPER.valueToTree(chart.getEchartsModel());
  }

  /**
   * Retrieve the ECharts model built with the given reporter
   *
   * @param chart    Chart
   * @param reporter Receives the unsupported options
   * @return JSON tree of the model
   */
  static JsonNode echarts(Chart chart, UnsupportedOptionReporter reporter) {
    return MAPPER.valueToTree(new EChartsModelBuilder(reporter).build(chart));
  }

  /**
   * Collect the paths of the keys with the given name, wherever they appear in the tree
   *
   * @param node Tree to inspect
   * @param name Key name
   * @return Pointer paths of the matching keys
   */
  static List<String> pathsOf(JsonNode node, String name) {
    List<String> paths = new ArrayList<>();
    collect(node, "", name, paths);
    return paths;
  }

  private static void collect(JsonNode node, String path, String name, List<String> paths) {
    if (node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        String child = path + "/" + entry.getKey();
        if (entry.getKey().equals(name)) {
          paths.add(child);
        }
        collect(entry.getValue(), child, name, paths);
      });
    } else if (node.isArray()) {
      for (int index = 0; index < node.size(); index++) {
        collect(node.get(index), path + "/" + index, name, paths);
      }
    }
  }
}
