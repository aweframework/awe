package com.almis.awe.model.util.data.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Parses AWE date and time text into {@code java.time} values. Every method throws
 * {@link java.time.format.DateTimeParseException} when the text does not match the format and
 * {@link NullPointerException} when it is null.
 */
public final class DateTimeParser {

  private DateTimeParser() {
  }

  /**
   * Parses a web date ({@code dd/MM/yyyy})
   *
   * @param text Web date
   * @return Local date
   */
  public static LocalDate parseWebDate(String text) {
    return LocalDate.parse(text, DateFormats.WEB_DATE);
  }

  /**
   * Parses a web date with an optional time ({@code dd/MM/yyyy[ HH:mm:ss]}), keeping only the date
   *
   * @param text Web date, optionally followed by a time
   * @return Local date
   */
  public static LocalDate parseWebDateIgnoringTime(String text) {
    return LocalDate.parse(text, DateFormats.WEB_DATE_PARSER);
  }

  /**
   * Parses a web time ({@code HH:mm:ss})
   *
   * @param text Web time
   * @return Local time
   */
  public static LocalTime parseWebTime(String text) {
    return LocalTime.parse(text, DateFormats.WEB_TIME);
  }

  /**
   * Parses a web timestamp with optional milliseconds ({@code dd/MM/yyyy HH:mm:ss[.SSS]})
   *
   * @param text Web timestamp
   * @return Local date time
   */
  public static LocalDateTime parseWebTimestamp(String text) {
    return LocalDateTime.parse(text, DateFormats.WEB_TIMESTAMP);
  }

  /**
   * Parses a web timestamp with mandatory milliseconds ({@code dd/MM/yyyy HH:mm:ss.SSS})
   *
   * @param text Web timestamp with milliseconds
   * @return Local date time
   */
  public static LocalDateTime parseWebTimestampWithMs(String text) {
    return LocalDateTime.parse(text, DateFormats.WEB_TIMESTAMP_MS);
  }

  /**
   * Parses a web date and a web time into a single value
   *
   * @param date Web date ({@code dd/MM/yyyy})
   * @param time Web time ({@code HH:mm:ss[.SSS]})
   * @return Local date time
   */
  public static LocalDateTime parseWebDateAndTime(String date, String time) {
    return parseWebTimestamp(date + " " + time);
  }

  /**
   * Parses a SQL timestamp with milliseconds ({@code yyyy-MM-dd HH:mm:ss.SSS})
   *
   * @param text SQL timestamp
   * @return Local date time
   */
  public static LocalDateTime parseSqlTimestamp(String text) {
    return LocalDateTime.parse(text, DateFormats.SQL_TIMESTAMP_MS);
  }

  /**
   * Parses a SQL date ({@code yyyy-MM-dd})
   *
   * @param text SQL date
   * @return Local date
   */
  public static LocalDate parseSqlDate(String text) {
    return LocalDate.parse(text, DateFormats.SQL_DATE);
  }

  /**
   * Parses a web service date ({@code yyyy-MM-dd})
   *
   * @param text Web service date
   * @return Local date
   */
  public static LocalDate parseWbsDate(String text) {
    return LocalDate.parse(text, DateFormats.WBS_DATE);
  }

  /**
   * Parses an RDB date ({@code dd-MMM-yyyy}, case-insensitive)
   *
   * @param text RDB date
   * @return Local date
   */
  public static LocalDate parseRdbDate(String text) {
    return LocalDate.parse(text, DateFormats.RDB_DATE);
  }

  /**
   * Parses a JSON date ({@code yyyy-MM-dd@HH:mm:ss.SSSZ}). The offset is validated but not applied: the result keeps
   * the local date and time written in the text.
   *
   * @param text JSON date
   * @return Local date time
   */
  public static LocalDateTime parseJsonDate(String text) {
    return LocalDateTime.parse(text, DateFormats.JSON_DATE);
  }

  /**
   * Detects the format of a date text and parses it
   *
   * @param text Date text in any {@link DetectedDateFormat}
   * @return Local date time (midnight for date-only formats), or empty when no format matches
   */
  public static Optional<LocalDateTime> parseAnyFormat(String text) {
    return DateFormatDetector.detect(text).map(format -> switch (format) {
      case JSON_DATE -> parseJsonDate(text);
      case SQL_TIMESTAMP -> parseSqlTimestamp(text);
      case WEB_DATE -> parseWebDateIgnoringTime(text).atStartOfDay();
      case WBS_DATE -> parseWbsDate(text).atStartOfDay();
      case WEB_TIMESTAMP -> parseWebTimestamp(text);
    });
  }
}
