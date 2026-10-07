package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small helpers to assemble the nested option maps of the ECharts model
 */
final class EChartsMaps {

  /**
   * Key of the object that carries the AWE client hints
   */
  static final String AWE = "awe";

  private EChartsMaps() {
  }

  /**
   * Parse an integer attribute of the XML, which may be absent or malformed
   *
   * @param value Attribute value
   * @return Number, or null when the value is blank or not an integer
   */
  static Integer integer(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  /**
   * Create an empty ordered map
   *
   * @return New map
   */
  static Map<String, Object> map() {
    return new LinkedHashMap<>();
  }

  /**
   * Retrieve a nested map, creating it when it does not exist
   *
   * @param parent Parent map
   * @param key    Key of the nested map
   * @return Nested map
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> child(Map<String, Object> parent, String key) {
    Object value = parent.get(key);
    if (value instanceof Map<?, ?> existing) {
      return (Map<String, Object>) existing;
    }
    Map<String, Object> created = map();
    parent.put(key, created);
    return created;
  }

  /**
   * Retrieve the AWE client hints of an option object, creating them when needed
   *
   * @param options Option object
   * @return Hints map stored under the {@code awe} key
   */
  static Map<String, Object> hints(Map<String, Object> options) {
    return child(options, AWE);
  }

  /**
   * Remove the hints object of an option object when no hint was set
   *
   * @param options Option object
   */
  static void dropEmptyHints(Map<String, Object> options) {
    if (options.get(AWE) instanceof Map<?, ?> hints && hints.isEmpty()) {
      options.remove(AWE);
    }
  }

  /**
   * Copy the defaults into the target. Values already present in the target always win; nested maps are merged
   *
   * @param target   Map to complete
   * @param defaults Default values
   */
  @SuppressWarnings("unchecked")
  static void mergeDefaults(Map<String, Object> target, Map<String, Object> defaults) {
    for (Map.Entry<String, Object> entry : defaults.entrySet()) {
      Object current = target.get(entry.getKey());
      if (current == null) {
        target.put(entry.getKey(), copy(entry.getValue()));
      } else if (current instanceof Map<?, ?> currentMap && entry.getValue() instanceof Map<?, ?> defaultMap) {
        mergeDefaults((Map<String, Object>) currentMap, (Map<String, Object>) defaultMap);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Object copy(Object value) {
    if (value instanceof Map<?, ?> source) {
      Map<String, Object> copy = map();
      ((Map<String, Object>) source).forEach((key, item) -> copy.put(key, copy(item)));
      return copy;
    }
    if (value instanceof List<?> source) {
      return new ArrayList<>(source);
    }
    return value;
  }
}
