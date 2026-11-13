package com.almis.awe.model.util.data.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Formats {@code java.time} values as AWE date and time text. Every method throws {@link NullPointerException} when
 * the value is null.
 */
public final class DateTimeFormatting {

  private DateTimeFormatting() {
  }

  /**
   * @param date Date
   * @return Web date ({@code dd/MM/yyyy})
   */
  public static String formatWebDate(LocalDate date) {
    return DateFormats.WEB_DATE.format(date);
  }

  /**
   * @param dateTime Date time
   * @return Web date and time without milliseconds ({@code dd/MM/yyyy HH:mm:ss})
   */
  public static String formatWebDateTime(LocalDateTime dateTime) {
    return DateFormats.WEB_DATETIME.format(dateTime);
  }

  /**
   * @param dateTime Date time
   * @return Web timestamp, with milliseconds only when they are not zero ({@code dd/MM/yyyy HH:mm:ss[.SSS]})
   */
  public static String formatWebTimestamp(LocalDateTime dateTime) {
    return DateFormats.WEB_TIMESTAMP.format(dateTime);
  }

  /**
   * @param dateTime Date time
   * @return Web timestamp with milliseconds ({@code dd/MM/yyyy HH:mm:ss.SSS})
   */
  public static String formatWebTimestampWithMs(LocalDateTime dateTime) {
    return DateFormats.WEB_TIMESTAMP_MS.format(dateTime);
  }

  /**
   * @param time Time, or a date time
   * @return Web time ({@code HH:mm:ss})
   */
  public static String formatWebTime(TemporalAccessor time) {
    return DateFormats.WEB_TIME.format(time);
  }

  /**
   * @param date Date, or a date time
   * @return Javascript date ({@code MM/dd/yyyy})
   */
  public static String formatJsDate(TemporalAccessor date) {
    return DateFormats.JS_DATE.format(date);
  }

  /**
   * @param dateTime Date time
   * @return Javascript timestamp ({@code MM/dd/yyyy HH:mm:ss})
   */
  public static String formatJsTimestamp(LocalDateTime dateTime) {
    return DateFormats.JS_TIMESTAMP.format(dateTime);
  }

  /**
   * @param date Date
   * @return SQL date ({@code yyyy-MM-dd})
   */
  public static String formatSqlDate(LocalDate date) {
    return DateFormats.SQL_DATE.format(date);
  }

  /**
   * @param dateTime Date time
   * @return SQL timestamp with milliseconds ({@code yyyy-MM-dd HH:mm:ss.SSS})
   */
  public static String formatSqlTimestamp(LocalDateTime dateTime) {
    return DateFormats.SQL_TIMESTAMP_MS.format(dateTime);
  }

  /**
   * @param date Date
   * @return Web service date ({@code yyyy-MM-dd})
   */
  public static String formatWbsDate(LocalDate date) {
    return DateFormats.WBS_DATE.format(date);
  }

  /**
   * @param date Date
   * @return RDB date in upper case ({@code dd-MMM-yyyy}, e.g. {@code 23-OCT-1978})
   */
  public static String formatRdbDate(LocalDate date) {
    return DateFormats.RDB_DATE.format(date).toUpperCase(Locale.ENGLISH);
  }

  /**
   * @param dateTime Zoned date time
   * @return JSON date ({@code yyyy-MM-dd@HH:mm:ss.SSSZ})
   */
  public static String formatJsonDate(ZonedDateTime dateTime) {
    return DateFormats.JSON_DATE.format(dateTime);
  }
}
