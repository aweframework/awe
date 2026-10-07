package com.almis.awe.model.entities.screen.component.chart.echarts;

/**
 * Element of the chart where a {@code chart-parameter} appears
 */
enum ParameterScope {
  CHART("chart"),
  SERIES("series"),
  AXIS("axis"),
  TOOLTIP("tooltip"),
  LEGEND("legend");

  private final String label;

  ParameterScope(String label) {
    this.label = label;
  }

  String label() {
    return label;
  }
}
