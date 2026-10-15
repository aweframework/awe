package com.almis.awe.model.util.data.date;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Date and time formats exchanged by AWE with the browser (web, js), the database (sql), web services (wbs) and RDB
 * systems. All formatters are immutable and thread-safe.
 */
public final class DateFormats {

  /**
   * Web date with an optional time ({@code dd/MM/yyyy[ HH:mm:ss]}); missing time fields default to midnight
   */
  public static final DateTimeFormatter WEB_DATE_PARSER = new DateTimeFormatterBuilder()
    .appendPattern("dd/MM/yyyy[ HH:mm:ss]")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter();

  /**
   * Web date ({@code dd/MM/yyyy})
   */
  public static final DateTimeFormatter WEB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Javascript date ({@code MM/dd/yyyy})
   */
  public static final DateTimeFormatter JS_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

  /**
   * SQL date ({@code yyyy-MM-dd})
   */
  public static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Web service date ({@code yyyy-MM-dd})
   */
  public static final DateTimeFormatter WBS_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * RDB date ({@code dd-MMM-yyyy}, English month names, case-insensitive parsing)
   */
  public static final DateTimeFormatter RDB_DATE = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("dd-MMM-yyyy")
    .toFormatter(Locale.ENGLISH);

  /**
   * SQL timestamp with milliseconds ({@code yyyy-MM-dd HH:mm:ss.SSS})
   */
  public static final DateTimeFormatter SQL_TIMESTAMP_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Web timestamp with optional milliseconds ({@code dd/MM/yyyy HH:mm:ss[.SSS]})
   */
  public static final DateTimeFormatter WEB_TIMESTAMP = new DateTimeFormatterBuilder()
    .appendPattern("dd/MM/yyyy HH:mm:ss")
    .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
    .toFormatter();

  /**
   * Web date and time without milliseconds ({@code dd/MM/yyyy HH:mm:ss})
   */
  public static final DateTimeFormatter WEB_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  /**
   * Javascript timestamp ({@code MM/dd/yyyy HH:mm:ss})
   */
  public static final DateTimeFormatter JS_TIMESTAMP = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

  /**
   * Web timestamp with milliseconds ({@code dd/MM/yyyy HH:mm:ss.SSS})
   */
  public static final DateTimeFormatter WEB_TIMESTAMP_MS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

  /**
   * Web time ({@code HH:mm:ss})
   */
  public static final DateTimeFormatter WEB_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

  /**
   * JSON date ({@code yyyy-MM-dd@HH:mm:ss.SSSZ})
   */
  public static final DateTimeFormatter JSON_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss.SSSZ");

  private DateFormats() {
  }
}
