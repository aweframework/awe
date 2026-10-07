package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.Locale;
import java.util.Optional;

/**
 * Series types of the AWE chart element, which are Highcharts series type names, and their ECharts counterpart
 */
enum EChartsSeriesType {
  LINE("line", "line", false, false, false),
  SPLINE("spline", "line", true, false, false),
  COLUMN("column", "bar", false, false, false),
  BAR("bar", "bar", false, false, false),
  AREA("area", "line", false, true, false),
  AREASPLINE("areaspline", "line", true, true, false),
  AREARANGE("arearange", "line", false, true, true),
  AREASPLINERANGE("areasplinerange", "line", true, true, true),
  PIE("pie", "pie", false, false, false),
  SCATTER("scatter", EChartsKeys.SCATTER, false, false, false),
  BUBBLE("bubble", EChartsKeys.SCATTER, false, false, false);

  private final String highchartsName;
  private final String echartsType;
  private final boolean smooth;
  private final boolean area;
  private final boolean approximated;

  EChartsSeriesType(String highchartsName, String echartsType, boolean smooth, boolean area, boolean approximated) {
    this.highchartsName = highchartsName;
    this.echartsType = echartsType;
    this.smooth = smooth;
    this.area = area;
    this.approximated = approximated;
  }

  /**
   * Name of the series type in Highcharts, as written in the XML
   */
  String highchartsName() {
    return highchartsName;
  }

  /**
   * Series type in ECharts
   */
  String echartsType() {
    return echartsType;
  }

  /**
   * The line is smoothed
   */
  boolean isSmooth() {
    return smooth;
  }

  /**
   * The line is filled down to the axis
   */
  boolean isArea() {
    return area;
  }

  /**
   * ECharts has no equivalent, so a close one is used
   */
  boolean isApproximated() {
    return approximated;
  }

  boolean isPie() {
    return this == PIE;
  }

  /**
   * Highcharts bar series are horizontal, which inverts the whole chart
   */
  boolean isHorizontalBar() {
    return this == BAR;
  }

  /**
   * Series that accumulate over each other when stacking is enabled
   */
  boolean isStackable() {
    return !isPie() && this != SCATTER && this != BUBBLE && !approximated;
  }

  /**
   * Look up a series type by its Highcharts name
   *
   * @param name Highcharts series type
   * @return Series type, empty when it is not known
   */
  static Optional<EChartsSeriesType> fromName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    String normalized = name.trim().toLowerCase(Locale.ROOT);
    for (EChartsSeriesType type : values()) {
      if (type.highchartsName.equals(normalized)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  /**
   * Series type that the series of a chart of the given AWE chart type take when they do not define their own.
   * The 3D types are rendered as their flat counterpart
   *
   * @param chartType AWE chart type
   * @return Default series type
   */
  static EChartsSeriesType forChartType(String chartType) {
    if (chartType == null) {
      return LINE;
    }
    String normalized = chartType.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("donut") || normalized.startsWith("pie") || normalized.equals("semicircle")) {
      return PIE;
    }
    return fromName(normalized.replace("_3d", "")).orElse(LINE);
  }
}
