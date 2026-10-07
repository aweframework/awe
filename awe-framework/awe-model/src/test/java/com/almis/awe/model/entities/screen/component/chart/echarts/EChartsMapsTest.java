package com.almis.awe.model.entities.screen.component.chart.echarts;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EChartsMapsTest {

  @Test
  void mergedDefaultsAreCopiesAtAnyDepth() {
    Map<String, Object> stop = EChartsMaps.map();
    stop.put("color", "red");
    Map<String, Object> defaults = EChartsMaps.map();
    EChartsMaps.child(defaults, "areaStyle").put("stops", new ArrayList<>(List.of(stop)));

    Map<String, Object> first = EChartsMaps.map();
    Map<String, Object> second = EChartsMaps.map();
    EChartsMaps.mergeDefaults(first, defaults);
    EChartsMaps.mergeDefaults(second, defaults);
    EChartsMaps.child(first, "areaStyle").put("opacity", 1);
    stopsOf(first).get(0).put("color", "blue");

    assertThat(EChartsMaps.child(second, "areaStyle")).doesNotContainKey("opacity");
    assertThat(stopsOf(second).get(0)).containsEntry("color", "red");
    assertThat(stop).containsEntry("color", "red");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> stopsOf(Map<String, Object> options) {
    return (List<Map<String, Object>>) EChartsMaps.child(options, "areaStyle").get("stops");
  }
}
