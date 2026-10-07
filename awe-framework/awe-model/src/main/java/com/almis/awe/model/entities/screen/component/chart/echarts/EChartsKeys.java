package com.almis.awe.model.entities.screen.component.chart.echarts;

/**
 * Names of the ECharts options used by more than one class of the model
 */
final class EChartsKeys {

  static final String TITLE = "title";
  static final String LABEL = "label";
  static final String RADIUS = "radius";
  static final String SCATTER = "scatter";
  static final String ITEM_STYLE = "itemStyle";
  static final String LINE_STYLE = "lineStyle";
  static final String AREA_STYLE = "areaStyle";
  /**
   * Marker options of a series, collected by the translation until the series type is known
   */
  static final String MARKER = "marker";
  /**
   * Marks a series whose stacking is active, until the series type is known. Highcharts ignores the stack name of a
   * series unless stacking is enabled
   */
  static final String STACKING = "stacking";

  private EChartsKeys() {
  }
}
