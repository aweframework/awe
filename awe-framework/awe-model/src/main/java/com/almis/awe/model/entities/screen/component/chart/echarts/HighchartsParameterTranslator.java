package com.almis.awe.model.entities.screen.component.chart.echarts;

import com.almis.awe.model.entities.screen.component.chart.AbstractChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies the {@code chart-parameter} elements of a chart element to its ECharts model.
 * <p>
 * The parameters are evaluated into the same raw Highcharts option tree that the Highcharts model uses, which is then
 * walked option by option. Each option is looked up in the translation table; options without a row are not applied
 * and are reported once.
 * </p>
 */
final class HighchartsParameterTranslator {

  private final List<ParameterRule> rules;
  private final UnsupportedOptionReporter reporter;

  HighchartsParameterTranslator(UnsupportedOptionReporter reporter) {
    this(HighchartsParameterTable.rules(), reporter);
  }

  HighchartsParameterTranslator(List<ParameterRule> rules, UnsupportedOptionReporter reporter) {
    this.rules = rules;
    this.reporter = reporter;
  }

  /**
   * Translate the parameters of an element
   *
   * @param source Element that holds the {@code chart-parameter} children
   * @param scope  Kind of element
   * @param target Where to write the translation
   */
  void apply(AbstractChart source, ParameterScope scope, ParameterTarget target) {
    if (source.getParameterList() == null || source.getParameterList().isEmpty()) {
      return;
    }
    Map<String, Object> raw = EChartsMaps.map();
    source.addParameters(raw);
    walk(raw, new ArrayList<>(), scope, target);
  }

  @SuppressWarnings("unchecked")
  private void walk(Map<String, Object> options, List<String> path, ParameterScope scope, ParameterTarget target) {
    for (Map.Entry<String, Object> entry : options.entrySet()) {
      path.add(entry.getKey());
      if (entry.getValue() instanceof Map<?, ?> nested) {
        walk((Map<String, Object>) nested, path, scope, target);
      } else {
        translate(path, entry.getValue(), scope, target);
      }
      path.remove(path.size() - 1);
    }
  }

  private void translate(List<String> path, Object value, ParameterScope scope, ParameterTarget target) {
    for (ParameterRule rule : rules) {
      Optional<List<String>> captures = rule.match(scope, path);
      if (captures.isPresent()) {
        if (value != null) {
          rule.apply(target, captures.get(), value);
        }
        return;
      }
    }
    reporter.parameter(scope, String.join(".", path));
  }
}
