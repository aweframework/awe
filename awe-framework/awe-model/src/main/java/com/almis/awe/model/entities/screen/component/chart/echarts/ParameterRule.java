package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Translation of one Highcharts option, found in a {@code chart-parameter}, to the ECharts model.
 * <p>
 * The pattern is the dotted path of the Highcharts option. A {@code *} segment matches any name and its value is
 * handed to the translation, in order, as a capture.
 * </p>
 */
final class ParameterRule {

  /**
   * Writes the value of a Highcharts option to the ECharts model
   */
  @FunctionalInterface
  interface Translation {
    /**
     * Apply a value
     *
     * @param target   Target of the scope where the parameter appears
     * @param captures Names matched by the {@code *} segments of the pattern
     * @param value    Value of the Highcharts option
     */
    void apply(ParameterTarget target, List<String> captures, Object value);
  }

  private final ParameterScope scope;
  private final List<String> pattern;
  private final Translation translation;

  ParameterRule(ParameterScope scope, String pattern, Translation translation) {
    this.scope = scope;
    this.pattern = Arrays.asList(pattern.split("\\."));
    this.translation = translation;
  }

  /**
   * Check if the rule translates the option and retrieve what its wildcards matched
   *
   * @param optionScope Scope of the parameter
   * @param path        Segments of the Highcharts option path
   * @return Captured names, empty when the rule does not apply
   */
  Optional<List<String>> match(ParameterScope optionScope, List<String> path) {
    if (scope != optionScope || pattern.size() != path.size()) {
      return Optional.empty();
    }
    List<String> captures = new ArrayList<>();
    for (int index = 0; index < pattern.size(); index++) {
      if ("*".equals(pattern.get(index))) {
        captures.add(path.get(index));
      } else if (!pattern.get(index).equals(path.get(index))) {
        return Optional.empty();
      }
    }
    return Optional.of(captures);
  }

  void apply(ParameterTarget target, List<String> captures, Object value) {
    translation.apply(target, captures, value);
  }
}
