package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.ChartAxis;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.child;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.dropEmptyHints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.hints;
import static com.almis.awe.model.entities.screen.component.chart.echarts.EChartsMaps.map;

/**
 * Builds the {@code xAxis} and {@code yAxis} arrays of the ECharts model from the {@code x-axis} and {@code y-axis}
 * elements
 */
final class EChartsAxisBuilder {

  private static final String CATEGORY = "category";
  private static final String VALUE = "value";

  private final HighchartsParameterTranslator translator;

  EChartsAxisBuilder(HighchartsParameterTranslator translator) {
    this.translator = translator;
  }

  /**
   * Build the axes of one AWE axis role
   *
   * @param axes     Axis elements, may be null
   * @param xRole    The elements are x axes (otherwise they are y axes)
   * @param inverted The chart is inverted, so the x axes are drawn vertically
   * @param needed   The chart is cartesian, so at least one axis is required
   * @return Axis options, empty when no axis is needed
   */
  List<Object> build(List<ChartAxis> axes, boolean xRole, boolean inverted, boolean needed) {
    List<Object> result = new ArrayList<>();
    if (axes != null) {
      for (ChartAxis axis : axes) {
        result.add(buildAxis(axis, xRole, inverted));
      }
    }
    if (result.isEmpty() && needed) {
      Map<String, Object> defaultAxis = map();
      defaultAxis.put("type", VALUE);
      hints(defaultAxis).put("axis", xRole ? "x" : "y");
      result.add(defaultAxis);
    }
    return result;
  }

  private Map<String, Object> buildAxis(ChartAxis axis, boolean xRole, boolean inverted) {
    Map<String, Object> options = map();
    boolean horizontal = xRole != inverted;
    String type = echartsType(axis.getType());

    if (axis.getLabel() != null) {
      options.put("name", axis.getLabel());
      options.put("nameLocation", "middle");
      options.put("nameGap", 30);
    }
    options.put("type", type);
    if (axis.isOpposite()) {
      options.put("position", horizontal ? "top" : "right");
    }
    if (xRole && inverted) {
      // Highcharts draws the first category and the oldest date of an inverted chart at the top
      options.put("inverse", true);
    }
    putTickInterval(options, axis.getTickInterval(), type);
    if (VALUE.equals(type) && !axis.isAllowDecimal()) {
      options.put("minInterval", 1);
    }
    if (axis.getLabelRotation() != null) {
      // Highcharts rotates clockwise, ECharts counterclockwise
      child(options, "axisLabel").put("rotate", -axis.getLabelRotation());
    }

    Map<String, Object> hints = hints(options);
    hints.put("axis", xRole ? "x" : "y");
    if (StringUtils.isNotBlank(axis.getLabelFormat())) {
      hints.put("labelFormat", axis.getLabelFormat());
    }
    if (StringUtils.isNotBlank(axis.getFormatterFunction())) {
      hints.put("formatter", axis.getFormatterFunction());
    }

    translator.apply(axis, ParameterScope.AXIS, new ParameterTarget(options));
    dropEmptyHints(options);
    return options;
  }

  private static void putTickInterval(Map<String, Object> options, String tickInterval, String type) {
    Integer interval = EChartsMaps.integer(tickInterval);
    if (interval == null) {
      return;
    }
    if (CATEGORY.equals(type)) {
      // A Highcharts tick interval of n shows one label every n categories, ECharts counts the skipped ones
      child(options, "axisLabel").put("interval", Math.max(interval - 1, 0));
    } else {
      options.put("interval", interval);
    }
  }

  /**
   * Translate the type of a Highcharts axis. Highcharts axes are linear unless stated
   */
  private static String echartsType(String type) {
    if (type == null) {
      return VALUE;
    }
    return switch (type.trim().toLowerCase(Locale.ROOT)) {
      case "datetime" -> "time";
      case CATEGORY -> CATEGORY;
      case "logarithmic" -> "log";
      default -> VALUE;
    };
  }
}
