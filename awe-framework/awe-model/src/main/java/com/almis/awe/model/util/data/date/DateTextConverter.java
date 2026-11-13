package com.almis.awe.model.util.data.date;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Converts date text from one AWE format to another. Conversions are lenient: when the input does not match the
 * source format the error is logged and the input is returned unchanged.
 */
@Slf4j
public final class DateTextConverter {

  private DateTextConverter() {
  }

  /**
   * @param text SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   * @return Web date ({@code dd/MM/yyyy})
   */
  public static String sqlToWebDate(String text) {
    return convert(text, "SQL date to WEB date",
      value -> DateFormats.WEB_DATE.format(DateTimeParser.parseSqlTimestamp(value)));
  }

  /**
   * @param text SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   * @return Web time ({@code HH:mm:ss})
   */
  public static String sqlToWebTime(String text) {
    return convert(text, "SQL date to WEB TIME",
      value -> DateTimeFormatting.formatWebTime(DateTimeParser.parseSqlTimestamp(value)));
  }

  /**
   * @param text SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   * @return Web timestamp, with milliseconds only when they are not zero
   */
  public static String sqlToWebTimestamp(String text) {
    return convert(text, "SQL date to WEB TIMESTAMP",
      value -> DateTimeFormatting.formatWebTimestamp(DateTimeParser.parseSqlTimestamp(value)));
  }

  /**
   * @param text SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   * @return Javascript date ({@code MM/dd/yyyy})
   */
  public static String sqlToJsDate(String text) {
    return convert(text, "SQL date to JS date",
      value -> DateTimeFormatting.formatJsDate(DateTimeParser.parseSqlTimestamp(value)));
  }

  /**
   * @param text SQL timestamp ({@code yyyy-MM-dd HH:mm:ss.SSS})
   * @return Javascript timestamp ({@code MM/dd/yyyy HH:mm:ss})
   */
  public static String sqlToJsTimestamp(String text) {
    return convert(text, "SQL date to JS TIMESTAMP",
      value -> DateTimeFormatting.formatJsTimestamp(DateTimeParser.parseSqlTimestamp(value)));
  }

  /**
   * @param text Web date ({@code dd/MM/yyyy})
   * @return Web service date ({@code yyyy-MM-dd})
   */
  public static String webToWbsDate(String text) {
    return convert(text, "WEB date to WBS date",
      value -> DateTimeFormatting.formatWbsDate(DateTimeParser.parseWebDate(value)));
  }

  /**
   * @param text Web date ({@code dd/MM/yyyy})
   * @return RDB date in upper case (e.g. {@code 23-OCT-1978}); invalid input is returned in upper case
   */
  public static String webToRdbDate(String text) {
    return convert(text, "WEB date to RDB date",
      value -> DateFormats.RDB_DATE.format(DateTimeParser.parseWebDate(value))).toUpperCase();
  }

  /**
   * @param text Web service date ({@code yyyy-MM-dd})
   * @return Web date ({@code dd/MM/yyyy})
   */
  public static String wbsToWebDate(String text) {
    return convert(text, "WBS date to WEB date",
      value -> DateTimeFormatting.formatWebDate(DateTimeParser.parseWbsDate(value)));
  }

  /**
   * @param text Web service date ({@code yyyy-MM-dd})
   * @return Javascript date ({@code MM/dd/yyyy})
   */
  public static String wbsToJsDate(String text) {
    return convert(text, "WBS date to JS date",
      value -> DateTimeFormatting.formatJsDate(DateTimeParser.parseWbsDate(value)));
  }

  /**
   * @param text RDB date ({@code dd-MMM-yyyy}, case-insensitive)
   * @return Web date ({@code dd/MM/yyyy}); invalid input is returned in upper case
   */
  public static String rdbToWebDate(String text) {
    return convert(text, "RDB date to WEB date",
      value -> DateTimeFormatting.formatWebDate(DateTimeParser.parseRdbDate(value))).toUpperCase();
  }

  /**
   * Converts a date between two patterns. The source pattern accepts an optional {@code HH:mm:ss.SSS} time; month
   * names are parsed in English and case-sensitively.
   *
   * @param text        Date text
   * @param patternFrom Source pattern
   * @param patternTo   Target pattern
   * @return Date text in the target pattern
   */
  public static String convert(String text, String patternFrom, String patternTo) {
    try {
      DateTimeFormatter fromFormatter = new DateTimeFormatterBuilder()
        .appendPattern(patternFrom + "[ HH:mm:ss.SSS]")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
        .parseCaseInsensitive()
        .toFormatter(Locale.ENGLISH);
      DateTimeFormatter toFormatter = DateTimeFormatter.ofPattern(patternTo).withLocale(Locale.ENGLISH);
      return toFormatter.format(LocalDateTime.parse(text, fromFormatter));
    } catch (Exception exc) {
      log.error("[DATE UTILITY] Error parsing generic date from {} to {} -{}-", text, patternFrom, patternTo, exc);
      return text;
    }
  }

  private static String convert(String text, String description, UnaryOperator<String> conversion) {
    try {
      return conversion.apply(text);
    } catch (Exception exc) {
      log.error("[DATE UTILITY] Error parsing {} -{}-", description, text, exc);
      return text;
    }
  }
}
