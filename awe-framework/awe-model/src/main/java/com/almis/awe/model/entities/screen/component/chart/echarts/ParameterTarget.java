package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where the translation of the {@code chart-parameter} elements of one scope is written
 */
final class ParameterTarget {

  private final Map<String, Object> options;
  private final Map<String, Map<String, Object>> seriesDefaults = new LinkedHashMap<>();
  private UnsupportedOptionReporter reporter;

  ParameterTarget(Map<String, Object> options) {
    this.options = options;
  }

  /**
   * Option object of the element that owns the parameters
   */
  Map<String, Object> options() {
    return options;
  }

  /**
   * Options that apply by default to the series of a Highcharts type. The {@code series} key applies to all of them.
   * Only the chart scope uses them, as {@code plotOptions} are defined there.
   *
   * @param type Highcharts series type, or {@code series}
   * @return Default options of the series
   */
  Map<String, Object> seriesDefaults(String type) {
    return seriesDefaults.computeIfAbsent(type, key -> EChartsMaps.map());
  }

  /**
   * Retrieve the default options without creating them
   *
   * @param type Highcharts series type, or {@code series}
   * @return Default options of the series, empty when there are none
   */
  Map<String, Object> existingSeriesDefaults(String type) {
    return seriesDefaults.getOrDefault(type, Map.of());
  }

  /**
   * Set the reporter of the options that are approximated or dropped. The translator sets it before it applies a rule
   *
   * @param reporter Reporter
   */
  void reporter(UnsupportedOptionReporter reporter) {
    this.reporter = reporter;
  }

  /**
   * Report an option that is approximated or dropped by the translation
   *
   * @param key     Identity of the report, used to report it once
   * @param message Message to log
   */
  void approximation(String key, String message) {
    if (reporter != null) {
      reporter.approximation(key, message);
    }
  }
}
