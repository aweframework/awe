package com.almis.awe.model.entities.screen.component.chart.echarts;

import java.util.Locale;
import java.util.Map;

/**
 * Conversions of the values of the Highcharts options, which the XML gives as strings or typed numbers
 */
final class ParameterValues {

  /**
   * Font size that a percentage font size is relative to, in pixels
   */
  static final double BASE_FONT_SIZE = 12;

  private static final double MAX_EXACT = 9_007_199_254_740_992d;

  private ParameterValues() {
  }

  /**
   * Convert a value to a boolean
   *
   * @param value Boolean or its text
   * @return Boolean value
   */
  static boolean bool(Object value) {
    return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value).toLowerCase(Locale.ROOT));
  }

  /**
   * Convert a value to a number, keeping integers as integers
   *
   * @param value Number or its text
   * @return Number, or null when the value is not a finite number
   */
  static Number number(Object value) {
    if (value instanceof Number number) {
      return number;
    }
    try {
      double parsed = Double.parseDouble(String.valueOf(value).trim());
      if (!Double.isFinite(parsed)) {
        return null;
      }
      return integral(parsed);
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  /**
   * Keep an integral value as an integer when it fits, as a long when it fits one that a double holds exactly (a
   * timestamp in milliseconds), and as a double otherwise
   */
  private static Number integral(double value) {
    if (value != Math.rint(value) || Math.abs(value) > MAX_EXACT) {
      return value;
    }
    return Math.abs(value) <= Integer.MAX_VALUE ? (Number) (int) value : (Number) (long) value;
  }

  /**
   * Convert a font size to pixels: {@code 11px}, {@code 11} or a percentage of {@link #BASE_FONT_SIZE}
   *
   * @param value Highcharts font size
   * @return Pixels, or null when the value is not a size
   */
  static Number fontSize(Object value) {
    String text = String.valueOf(value).trim();
    if (text.endsWith("%")) {
      Number percent = number(text.substring(0, text.length() - 1));
      return percent == null ? null : (Number) (int) Math.round(BASE_FONT_SIZE * percent.doubleValue() / 100);
    }
    return number(text.endsWith("px") ? text.substring(0, text.length() - 2) : text);
  }

  /**
   * Convert a size that is in pixels or a percentage of the plot ({@code 20%})
   *
   * @param value Number, or its text
   * @return Number, the text of a percentage, or null when the value is neither
   */
  static Object pixelsOrPercent(Object value) {
    Number number = number(value);
    if (number != null) {
      return number;
    }
    String text = String.valueOf(value).trim();
    boolean percentage = text.endsWith("%") && number(text.substring(0, text.length() - 1)) != null;
    return percentage ? text : null;
  }

  /**
   * Store a value unless it is null
   *
   * @param map   Target
   * @param key   Key
   * @param value Value
   */
  static void putIfPresent(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
