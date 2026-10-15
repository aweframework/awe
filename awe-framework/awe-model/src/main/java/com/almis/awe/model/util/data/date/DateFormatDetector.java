package com.almis.awe.model.util.data.date;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Checks which AWE format a date text is written in
 */
public final class DateFormatDetector {

  private DateFormatDetector() {
  }

  /**
   * @param text Date text
   * @return Whether the text is a web date ({@code dd/MM/yyyy})
   */
  public static boolean isWebDate(String text) {
    return matches(text, DateFormats.WEB_DATE);
  }

  /**
   * @param text Date text
   * @return Whether the text is a web service date ({@code yyyy-MM-dd})
   */
  public static boolean isWbsDate(String text) {
    return matches(text, DateFormats.WBS_DATE);
  }

  /**
   * @param text Date text
   * @return Whether the text is a SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   */
  public static boolean isSqlTimestamp(String text) {
    return matches(text, DateFormats.SQL_TIMESTAMP_MS);
  }

  /**
   * @param text Date text
   * @return Whether the text is a web timestamp with optional milliseconds ({@code dd/MM/yyyy HH:mm:ss[.SSS]})
   */
  public static boolean isWebTimestamp(String text) {
    return matches(text, DateFormats.WEB_TIMESTAMP);
  }

  /**
   * @param text Date text
   * @return Whether the text is a web timestamp with milliseconds ({@code dd/MM/yyyy HH:mm:ss.SSS})
   */
  public static boolean isWebTimestampWithMs(String text) {
    return matches(text, DateFormats.WEB_TIMESTAMP_MS);
  }

  /**
   * @param text Date text
   * @return Whether the text is a JSON date ({@code yyyy-MM-dd@HH:mm:ss.SSSZ})
   */
  public static boolean isJsonDate(String text) {
    return matches(text, DateFormats.JSON_DATE);
  }

  /**
   * Detects the format of a date text, trying the formats in {@link DetectedDateFormat} order
   *
   * @param text Date text
   * @return Detected format, or empty when none matches
   */
  public static Optional<DetectedDateFormat> detect(String text) {
    if (isJsonDate(text)) {
      return Optional.of(DetectedDateFormat.JSON_DATE);
    } else if (isSqlTimestamp(text)) {
      return Optional.of(DetectedDateFormat.SQL_TIMESTAMP);
    } else if (isWebDate(text)) {
      return Optional.of(DetectedDateFormat.WEB_DATE);
    } else if (isWbsDate(text)) {
      return Optional.of(DetectedDateFormat.WBS_DATE);
    } else if (isWebTimestamp(text)) {
      return Optional.of(DetectedDateFormat.WEB_TIMESTAMP);
    }
    return Optional.empty();
  }

  private static boolean matches(String text, DateTimeFormatter format) {
    try {
      format.parse(text);
      return true;
    } catch (RuntimeException exc) {
      return false;
    }
  }
}
