package com.almis.awe.model.util.data;

/*
 * File Imports
 */

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.date.DateFormatDetector;
import com.almis.awe.model.util.data.date.DateFormats;
import com.almis.awe.model.util.data.date.DateTextConverter;
import com.almis.awe.model.util.data.date.DateTimeFormatting;
import com.almis.awe.model.util.data.date.DateTimeParser;
import com.almis.awe.model.util.data.date.ElapsedTimeFormatter;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * DateUtil Class
 * Date Utilities for AWE.
 * <p>
 * Facade over the {@code com.almis.awe.model.util.data.date} package, which holds the implementation split by
 * responsibility and works with {@code java.time}:
 * <ul>
 * <li>{@link DateFormats}: date and time formats</li>
 * <li>{@link DateTimeParser}: text to {@code java.time}</li>
 * <li>{@link DateTimeFormatting}: {@code java.time} to text</li>
 * <li>{@link DateTextConverter}: text from one format to another</li>
 * <li>{@link DateFormatDetector}: format detection</li>
 * <li>{@link ElapsedTimeFormatter}: human-readable elapsed time</li>
 * <li>{@link LegacyDateConverter}: {@code java.util.Date} and {@code java.sql} types to and from {@code java.time}</li>
 * </ul>
 * Methods whose signature uses {@code java.util.Date} or {@link Calendar} are deprecated in favour of the
 * {@code java.time} methods of those classes. Methods that use {@code java.sql} types are kept for the JDBC boundary.
 *
 * @author Pablo GARCIA and Pablo Vidal - 13/JUL/2014
 */
@Slf4j
public final class DateUtil {

  /**
   * Private constructor to enclose the default one
   */
  private DateUtil() {
  }

  // Util name
  private static final String UTILITY_NAME = "DATE UTILITY";

  // Web Date time formatter
  public static final DateTimeFormatter DATE_FORMAT_WEB_PARSER = DateFormats.WEB_DATE_PARSER;

  // Date in Web Format
  public static final DateTimeFormatter DATE_FORMAT_WEB = DateFormats.WEB_DATE;

  // Date in Js Format
  public static final DateTimeFormatter DATE_FORMAT_JS = DateFormats.JS_DATE;

  // Date in SQL Format
  public static final DateTimeFormatter DATE_FORMAT_SQL = DateFormats.SQL_DATE;

  // Date in Web Service Format
  public static final DateTimeFormatter DATE_FORMAT_WBS = DateFormats.WBS_DATE;

  // Date in RDB Format
  public static final DateTimeFormatter DATE_FORMAT_RDB = DateFormats.RDB_DATE;

  // Timestamp in SQL Format
  public static final DateTimeFormatter TIMESTAMP_FORMAT_SQL_MS = DateFormats.SQL_TIMESTAMP_MS;

  // Timestamp in Web Format with Optional milliseconds
  public static final DateTimeFormatter TIMESTAMP_FORMAT_WEB = DateFormats.WEB_TIMESTAMP;

  // Timestamp in Web Format
  public static final DateTimeFormatter DATETIME_FORMAT_WEB = DateFormats.WEB_DATETIME;

  // Timestamp in Js Format
  public static final DateTimeFormatter TIMESTAMP_FORMAT_JS = DateFormats.JS_TIMESTAMP;

  // Timestamp in Web Format with milliseconds
  public static final DateTimeFormatter TIMESTAMP_FORMAT_WEB_MS = DateFormats.WEB_TIMESTAMP_MS;

  // Time in Web Format
  public static final DateTimeFormatter TIME_FORMAT_WEB = DateFormats.WEB_TIME;

  // Timestamp in Web Format with milliseconds
  public static final DateTimeFormatter JSON_DATE = DateFormats.JSON_DATE;

  /**
   * Transforms a web date into a SQL Date
   *
   * @param date Web Date
   * @return SQL Date
   */
  public static java.sql.Date web2SqlDate(String date) {
    return java.sql.Date.valueOf(DateTimeParser.parseWebDate(date));
  }

  /**
   * Transforms a web timestamp into a SQL Timestamp
   *
   * @param time Web Timestamp
   * @return SQL Date
   */
  public static java.sql.Time web2SqlTime(String time) {
    return Time.valueOf(DateTimeParser.parseWebTime(time));
  }

  /**
   * Transforms a web date into a SQL Timestamp
   *
   * @param date Web Date
   * @return SQL Timestamp
   */
  public static java.sql.Timestamp webDate2SqlTimestamp(String date) {
    return Timestamp.valueOf(DateTimeParser.parseWebDate(date).atStartOfDay());
  }

  /**
   * Transforms a web timestamp into a SQL Timestamp
   *
   * @param timestamp Web Timestamp
   * @return SQL Timestamp
   */
  public static java.sql.Timestamp web2SqlTimestamp(String timestamp) {
    return Timestamp.valueOf(DateTimeParser.parseWebTimestamp(timestamp));
  }

  /**
   * Transforms a web date  (dd/MM/yyyy[ HH:mm:ss]) into a Date
   * Return null if  date is not valid
   *
   * @param date string Web date
   * @return Date
   * @deprecated use {@link DateTimeParser#parseWebDateIgnoringTime(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date web2Date(String date) {
    if (date == null || date.isEmpty()) {
      return null;
    }
    try {
      return LegacyDateConverter.asUtilDate(DateTimeParser.parseWebDateIgnoringTime(date));
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB date to date -{}-", UTILITY_NAME, date, exc);
      return null;
    }
  }

  /**
   * Transforms a web time into a Date
   * Return null if time is not valid
   *
   * @param time Web Time
   * @return SQL Time
   * @deprecated use {@link DateTimeParser#parseWebTime(String)} or {@link #web2SqlTime(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date web2Time(String time) {
    try {
      return Time.valueOf(DateTimeParser.parseWebTime(time));
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB TIME to date -{}-", UTILITY_NAME, time, exc);
      return null;
    }
  }

  /**
   * Transforms a web timestamp into a Date
   * Return null if timestamp is not valid
   *
   * @param timestamp Web Timestamp date
   * @return SQL Timestamp
   * @deprecated use {@link DateTimeParser#parseWebTimestamp(String)} or {@link #web2SqlTimestamp(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date web2Timestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }
    try {
      return Timestamp.valueOf(DateTimeParser.parseWebTimestamp(timestamp));
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB TIMESTAMP to date -{}-", UTILITY_NAME, timestamp, exc);
      return null;
    }
  }

  /**
   * Transforms a web timestamp with ms into a Date
   * Return null if timestamp is not valid
   *
   * @param date Web Date
   * @return SQL Timestamp
   * @deprecated use {@link DateTimeParser#parseWebTimestampWithMs(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date web2TimestampWithMs(String date) {
    try {
      return Timestamp.valueOf(DateTimeParser.parseWebTimestampWithMs(date));
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB TIMESTAMP WITH MS to date -{}-", UTILITY_NAME, date, exc);
      return null;
    }
  }

  /**
   * Transforms a date into a SQL Date
   *
   * @param date Date
   * @return SQL Date
   * @deprecated use {@code java.sql.Date.valueOf(LocalDate)}
   */
  @Deprecated(since = "5.0.0")
  public static java.sql.Date dat2SqlDate(java.util.Date date) {
    return new java.sql.Date(date.getTime());
  }

  /**
   * Transforms a date into a SQL Date
   *
   * @param date Date
   * @return SQL Date
   * @deprecated use {@link DateTimeFormatting#formatSqlDate(LocalDate)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2SqlDateString(java.util.Date date) {
    return DateTimeFormatting.formatSqlDate(LegacyDateConverter.asLocalDate(date));
  }

  /**
   * Transforms a sql timestamp into a java Date
   *
   * @param timestamp Date
   * @return SQL Date
   * @deprecated use {@link Timestamp#toLocalDateTime()} or {@link Timestamp#toInstant()}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date sqlTimestamp2Date(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : LegacyDateConverter.asUtilDate(timestamp);
  }

  /**
   * Transforms a sql date into a java Date
   *
   * @param date Date
   * @return SQL Date
   * @deprecated use {@link java.sql.Date#toLocalDate()}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date sqlDate2Date(java.sql.Date date) {
    return date == null ? null : LegacyDateConverter.asUtilDate(date);
  }

  /**
   * Transforms a date into a SQL Time
   *
   * @param date Date
   * @return SQL Timestamp
   * @deprecated use {@code Timestamp.valueOf(LocalDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static java.sql.Timestamp dat2SqlTimestamp(java.util.Date date) {
    return Timestamp.valueOf(LegacyDateConverter.asLocalDateTime(date).truncatedTo(ChronoUnit.MILLIS));
  }

  /**
   * Transforms a LocalDatetime into a Web Timestamp
   *
   * @param localDateTime Date
   * @return Web Timestamp
   */
  public static String localDatetime2WebTimestamp(LocalDateTime localDateTime) {
    return DateTimeFormatting.formatWebDateTime(localDateTime);
  }

  /**
   * Transforms a LocalDate into a Web date
   *
   * @param localDate Date
   * @return Web Timestamp
   */
  public static String localDate2WebDate(LocalDate localDate) {
    return DateTimeFormatting.formatWebDate(localDate);
  }

  /**
   * Transforms a timestamp date into a SQL Timestamp string
   *
   * @param date Date
   * @return SQL Timestamp
   * @deprecated use {@link DateTimeFormatting#formatSqlTimestamp(LocalDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2SqlTimeString(java.util.Date date) {
    return DateTimeFormatting.formatSqlTimestamp(LegacyDateConverter.asLocalDateTime(date));
  }

  /**
   * Transforms a date time into a Web Timestamp
   *
   * @param dateTime Date
   * @return WEB Timestamp
   * @deprecated use {@link DateTimeFormatting#formatWebDateTime(LocalDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2WebTimestamp(java.util.Date dateTime) {
    return DateTimeFormatting.formatWebDateTime(LegacyDateConverter.asLocalDateTime(dateTime));
  }

  /**
   * Transforms a date into a Web Timestamp with milliseconds
   *
   * @param date dateTime Date
   * @return WEB Timestamp
   * @deprecated use {@link DateTimeFormatting#formatWebTimestampWithMs(LocalDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2WebTimestampMs(java.util.Date date) {
    return DateTimeFormatting.formatWebTimestampWithMs(LegacyDateConverter.asLocalDateTime(date));
  }

  /**
   * Transforms a time as Date into a Web Time
   *
   * @param date Date
   * @return WEB Time
   * @deprecated use {@link DateTimeFormatting#formatWebTime(java.time.temporal.TemporalAccessor)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2WebTime(java.util.Date date) {
    return DateTimeFormatting.formatWebTime(LegacyDateConverter.asLocalTime(date));
  }

  /**
   * Transforms a date into a Web Date
   *
   * @param date Date
   * @return String WEB date
   * @deprecated use {@link DateTimeFormatting#formatWebDate(LocalDate)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2WebDate(java.util.Date date) {
    return DateTimeFormatting.formatWebDate(LegacyDateConverter.asLocalDate(date));
  }

  /**
   * Transforms a java Date into a date in milliseconds (Used in charts)
   *
   * @param date Date
   * @return string date in milliseconds
   * @deprecated use {@link Instant#toEpochMilli()}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2DateMs(java.util.Date date) {
    return String.valueOf(date.getTime());
  }

  /**
   * Transforms a SQL String date into a web date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sql2WebDate(String date) {
    return DateTextConverter.sqlToWebDate(date);
  }

  /**
   * Transforms a SQL String date into a java date
   *
   * @param timestamp SQL String Timestamp
   * @return SQL Timestamp
   * @deprecated use {@link DateTimeParser#parseSqlTimestamp(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date sql2JavaDate(String timestamp) {
    return Timestamp.valueOf(DateTimeParser.parseSqlTimestamp(timestamp));
  }

  /**
   * Transforms a SQL String date (without time) into a java date
   *
   * @param date SQL String Date
   * @return Java date
   * @deprecated use {@link DateTimeParser#parseSqlDate(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date sqlDate2JavaDate(String date) {
    return LegacyDateConverter.asUtilDate(DateTimeParser.parseSqlDate(date));
  }

  /**
   * Transforms a WBS String date into a java date
   *
   * @param date SQL String Date
   * @return Java date
   * @deprecated use {@link DateTimeParser#parseWbsDate(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date wbs2JavaDate(String date) {
    return LegacyDateConverter.asUtilDate(DateTimeParser.parseWbsDate(date));
  }

  /**
   * Transforms a RDB String date into a java date
   *
   * @param date SQL String Date
   * @return Java date
   * @deprecated use {@link DateTimeParser#parseRdbDate(String)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date rdb2Date(String date) {
    return LegacyDateConverter.asUtilDate(DateTimeParser.parseRdbDate(date));
  }

  /**
   * Transforms a SQL String date into a web time
   *
   * @param timestamp SQL String Date
   * @return Web time formatted
   */
  public static String sql2WebTime(String timestamp) {
    return DateTextConverter.sqlToWebTime(timestamp);
  }

  /**
   * Transforms a SQL String date into a web timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sql2WebTimestamp(String date) {
    return DateTextConverter.sqlToWebTimestamp(date);
  }

  /**
   * Transforms a SQL String date into a js date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sql2JsDate(String date) {
    return DateTextConverter.sqlToJsDate(date);
  }

  /**
   * Parse sql date to java date in milliseconds
   *
   * @param val Sql date
   * @return Java date in milliseconds
   */
  public static String sql2DateMs(String val) {
    return String.valueOf(Timestamp.valueOf(DateTimeParser.parseSqlTimestamp(val)).getTime());
  }

  /**
   * Transforms a SQL String date into a js timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sql2JsTimestamp(String date) {
    return DateTextConverter.sqlToJsTimestamp(date);
  }

  /**
   * Transforms a SQL date into a web date
   *
   * @param date SQL Date
   * @return Web date formatted
   */
  public static String sqlDat2WebDate(java.sql.Date date) {
    return DateTimeFormatting.formatWebDate(date.toLocalDate());
  }

  /**
   * Transforms a SQL time into a web time
   *
   * @param date SQL Time
   * @return Web time formatted
   */
  public static String sqlDat2WebTime(java.sql.Time date) {
    return DateTimeFormatting.formatWebTime(date.toLocalTime());
  }

  /**
   * Transforms a SQL timestamp into a web timestamp
   *
   * @param date SQL Timestamp
   * @return Web timestamp formatted
   */
  public static String sqlDat2WebTimestamp(java.sql.Timestamp date) {
    return DateTimeFormatting.formatWebTimestamp(date.toLocalDateTime());
  }

  /**
   * Transforms a SQL date into a js date
   *
   * @param date SQL Date
   * @return Js date formatted
   */
  public static String sqlDat2JsDate(java.sql.Date date) {
    return DateTimeFormatting.formatJsDate(date.toLocalDate());
  }

  /**
   * Transforms a SQL timestamp into a js timestamp
   *
   * @param date SQL Timestamp
   * @return Js timestamp formatted
   */
  public static String sqlDat2JsTimestamp(java.sql.Timestamp date) {
    return DateTimeFormatting.formatJsTimestamp(date.toLocalDateTime());
  }

  /**
   * Transforms a Date into a js date
   *
   * @param date Date
   * @return Js date formatted
   * @deprecated use {@link DateTimeFormatting#formatJsDate(java.time.temporal.TemporalAccessor)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2JsDate(java.util.Date date) {
    return DateTimeFormatting.formatJsDate(LegacyDateConverter.asLocalDate(date));
  }

  /**
   * Transforms a Date into a js timestamp
   *
   * @param date Date
   * @return Js timestamp formatted
   * @deprecated use {@link DateTimeFormatting#formatJsTimestamp(LocalDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2JsTimestamp(java.util.Date date) {
    return DateTimeFormatting.formatJsTimestamp(LegacyDateConverter.asLocalDateTime(date));
  }

  /**
   * Transforms a SQL timestamp into a web timestamp with Milliseconds
   *
   * @param date SQL Timestamp
   * @return Web timestamp formatted
   */
  public static String sqlDat2WebTimestampWithMs(java.sql.Timestamp date) {
    return DateTimeFormatting.formatWebTimestampWithMs(date.toLocalDateTime());
  }

  /**
   * Transforms a web date into a web service date
   *
   * @param date (Web formatted)
   * @return Web Service date formatted
   */
  public static String web2WbsDate(String date) {
    return DateTextConverter.webToWbsDate(date);
  }

  /**
   * Transforms a date into a web service date
   *
   * @param date Date
   * @return Web Service date formatted
   * @deprecated use {@link DateTimeFormatting#formatWbsDate(LocalDate)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2WbsDate(java.util.Date date) {
    return DateTimeFormatting.formatWbsDate(LegacyDateConverter.asLocalDate(date));
  }

  /**
   * Transforms a web date into an RDB date (23-OCT-1978)
   *
   * @param date (Web formatted)
   * @return RDB date formatted
   */
  public static String web2RdbDate(String date) {
    return DateTextConverter.webToRdbDate(date);
  }

  /**
   * Transforms a web service date into a web date
   *
   * @param date (Web service formatted)
   * @return Web date formatted
   */
  public static String wbs2WebDate(String date) {
    return DateTextConverter.wbsToWebDate(date);
  }

  /**
   * Transforms a web service date into a js date
   *
   * @param date (Web service formatted)
   * @return Web date formatted
   */
  public static String wbs2JsDate(String date) {
    return DateTextConverter.wbsToJsDate(date);
  }

  /**
   * Returns system date in SQL timestamp format (yyyy-MM-dd HH:mm:ss.SSS)
   *
   * @return String SQL system date
   */
  public static String getSystemDate() {
    return DateTimeFormatting.formatSqlTimestamp(LocalDateTime.now());
  }

  /**
   * Returns Calendar object for a web date (dd/MM/yyyy)
   *
   * @param strSqlDate Web Date
   * @return Calendar object
   * @deprecated use {@link DateTimeParser#parseWebDateIgnoringTime(String)}
   */
  @Deprecated(since = "5.0.0")
  public static Calendar getCalendarDate(String strSqlDate) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(web2Date(strSqlDate));
    return cal;
  }

  /**
   * Returns true if date is an WEB date
   *
   * @param date (Web service formatted)
   * @return Is an WEB date
   */
  public static boolean isWebDate(String date) {
    return DateFormatDetector.isWebDate(date);
  }

  /**
   * Returns true if date is an WBS date
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWbsDate(String date) {
    return DateFormatDetector.isWbsDate(date);
  }

  /**
   * Returns true if date is an SQL date
   *
   * @param date (Web service formatted)
   * @return Is an SQL date
   */
  public static boolean isSqlDate(String date) {
    return DateFormatDetector.isSqlTimestamp(date);
  }

  /**
   * Returns true if date is a Web timestamp
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWebTimestamp(String date) {
    return DateFormatDetector.isWebTimestamp(date);
  }

  /**
   * Returns true if date is a Web date with milliseconds
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWebTimestampWithMs(String date) {
    return DateFormatDetector.isWebTimestampWithMs(date);
  }

  /**
   * Returns true if date is json date
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isJsonDate(String date) {
    return DateFormatDetector.isJsonDate(date);
  }

  /**
   * Transforms a JSON Date to a Date object
   *
   * @param date JSON date
   * @return SQL Timestamp
   * @deprecated use {@link DateTimeParser#parseJsonDate(String)}
   */
  @Deprecated(since = "5.0.0")
  public static Date jsonDate(String date) {
    return Timestamp.valueOf(DateTimeParser.parseJsonDate(date));
  }

  /**
   * Transforms a JSON Date from a Date object
   *
   * @param date JSON date
   * @return Date OBJECT
   * @deprecated use {@link DateTimeFormatting#formatJsonDate(ZonedDateTime)}
   */
  @Deprecated(since = "5.0.0")
  public static String jsonDate(Date date) {
    return DateTimeFormatting.formatJsonDate(LegacyDateConverter.asZonedDateTime(date));
  }

  /**
   * Build java Date object from date criteria with Time criteria
   *
   * @param date Date criteria [dd/MM/yyyy]
   * @param time Time criteria [HH:mm:ss]
   * @return SQL Timestamp with format [dd/MM/yyyy HH:mm:ss]
   * @deprecated use {@link DateTimeParser#parseWebDateAndTime(String, String)}
   */
  @Deprecated(since = "5.0.0")
  public static Date getDateWithTimeFromCriteria(String date, String time) {
    return Timestamp.valueOf(DateTimeParser.parseWebDateAndTime(date, time));
  }

  /**
   * Build java Date object from date criteria with Time criteria
   *
   * @param date Date criteria [dd/MM/yyyy]
   * @param time Time criteria [HH:mm:ss]
   * @return SQL Timestamp with the date and the time, or the same date when the time is empty
   * @deprecated use {@link LocalDate#atTime(LocalTime)}
   */
  @Deprecated(since = "5.0.0")
  public static Date addTimeToDate(Date date, String time) {
    if (time == null || time.isEmpty()) {
      return date;
    }
    String webDate = DateTimeFormatting.formatWebDate(LegacyDateConverter.asLocalDate(date));
    return Timestamp.valueOf(DateTimeParser.parseWebDateAndTime(webDate, time));
  }

  /**
   * TransformColumn a date from a format to another date format
   *
   * @param dateIn     Input date
   * @param formatFrom Initial date format
   * @param formatTo   Final date format
   * @return Date formatted
   */
  public static String generic2Date(String dateIn, String formatFrom, String formatTo) {
    return DateTextConverter.convert(dateIn, formatFrom, formatTo);
  }

  /**
   * Transforms an RDB date into a web date (23-OCT-1978)
   *
   * @param date (Web formatted)
   * @return RDB date formatted
   */
  public static String rdbDate2Web(String date) {
    return DateTextConverter.rdbToWebDate(date);
  }

  /**
   * Transforms Date into an RDB String  (23-OCT-1978)
   *
   * @param date Date
   * @return RDB date formatted, or null when the date is not valid
   * @deprecated use {@link DateTimeFormatting#formatRdbDate(LocalDate)}
   */
  @Deprecated(since = "5.0.0")
  public static String dat2RDBDate(java.util.Date date) {
    try {
      return DateTimeFormatting.formatRdbDate(LegacyDateConverter.asLocalDate(date));
    } catch (Exception exc) {
      log.error("[{}] Error parsing date to RDB date -{}-", UTILITY_NAME, date, exc);
      return null;
    }
  }

  /**
   * Retrieve how much time has elapsed from milliseconds
   *
   * @param milliseconds Difference in milliseconds
   * @param elements     AWE Elements
   * @return Date since
   */
  public static String elapsedTime(Long milliseconds, AweElements elements) {
    return ElapsedTimeFormatter.format(Duration.ofMillis(milliseconds), elements);
  }

  /**
   * Retrieve how much time has elapsed since a date
   *
   * @param date     Date to check
   * @param elements AWE Elements
   * @return Date since
   * @deprecated use {@link ElapsedTimeFormatter#since(Instant, AweElements)}
   */
  @Deprecated(since = "5.0.0")
  public static String dateSince(java.util.Date date, AweElements elements) {
    return ElapsedTimeFormatter.since(LegacyDateConverter.asInstant(date), elements);
  }

  /**
   * Autodetect date format and applies a format into java date
   *
   * @param dateString String date
   * @return SQL Timestamp for formats with time, Date for date-only formats, null when no format matches
   * @deprecated use {@link DateTimeParser#parseAnyFormat(String)}
   */
  @Deprecated(since = "5.0.0")
  public static Date autoDetectDateFormat(String dateString) {
    return DateFormatDetector.detect(dateString).map(format -> {
      LocalDateTime dateTime = DateTimeParser.parseAnyFormat(dateString).orElseThrow();
      return switch (format) {
        case JSON_DATE, SQL_TIMESTAMP, WEB_TIMESTAMP -> Timestamp.valueOf(dateTime);
        case WEB_DATE, WBS_DATE -> LegacyDateConverter.asUtilDate(dateTime.toLocalDate());
      };
    }).orElse(null);
  }

  /**
   * Calls {@link #asLocalDate(Date, ZoneId)} with the system default time zone.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalDate(Date)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalDate asLocalDate(java.util.Date date) {
    return LegacyDateConverter.asLocalDate(date);
  }

  /**
   * Creates {@link LocalDate} from {@code java.util.Date} or it's subclasses.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalDate(Date, ZoneId)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalDate asLocalDate(java.util.Date date, ZoneId zone) {
    return LegacyDateConverter.asLocalDate(date, zone);
  }

  /**
   * Calls {@link #asLocalTime(Date, ZoneId)} with the system default time zone.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalTime(Date)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalTime asLocalTime(java.util.Date date) {
    return LegacyDateConverter.asLocalTime(date);
  }

  /**
   * Creates {@link LocalTime} from {@code java.util.Date} or it's subclasses.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalTime(Date, ZoneId)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalTime asLocalTime(java.util.Date date, ZoneId zone) {
    return LegacyDateConverter.asLocalTime(date, zone);
  }

  /**
   * Calls {@link #asLocalDateTime(Date, ZoneId)} with the system default time zone.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalDateTime(Date)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalDateTime asLocalDateTime(java.util.Date date) {
    return LegacyDateConverter.asLocalDateTime(date);
  }

  /**
   * Creates {@link LocalDateTime} from {@code java.util.Date} or it's subclasses.
   *
   * @deprecated use {@link LegacyDateConverter#asLocalDateTime(Date, ZoneId)}
   */
  @Deprecated(since = "5.0.0")
  public static LocalDateTime asLocalDateTime(java.util.Date date, ZoneId zone) {
    return LegacyDateConverter.asLocalDateTime(date, zone);
  }

  /**
   * Calls {@link #asUtilDate(Object, ZoneId)} with the system default time zone.
   *
   * @deprecated use {@link LegacyDateConverter#asUtilDate(Object)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date asUtilDate(Object date) {
    return LegacyDateConverter.asUtilDate(date);
  }

  /**
   * Creates a {@link java.util.Date} from various date objects. Is null-safe.
   *
   * @param zone Time zone, used only if the input object is LocalDate or LocalDateTime.
   * @return {@link java.util.Date} (exactly this class, not a subclass, such as java.sql.Date)
   * @deprecated use {@link LegacyDateConverter#asUtilDate(Object, ZoneId)}
   */
  @Deprecated(since = "5.0.0")
  public static java.util.Date asUtilDate(Object date, ZoneId zone) {
    return LegacyDateConverter.asUtilDate(date, zone);
  }

  /**
   * Creates an {@link Instant} from {@code java.util.Date} or it's subclasses. Null-safe.
   *
   * @deprecated use {@link LegacyDateConverter#asInstant(Date)}
   */
  @Deprecated(since = "5.0.0")
  public static Instant asInstant(Date date) {
    return LegacyDateConverter.asInstant(date);
  }

  /**
   * Calls {@link #asZonedDateTime(Date, ZoneId)} with the system default time zone.
   *
   * @deprecated use {@link LegacyDateConverter#asZonedDateTime(Date)}
   */
  @Deprecated(since = "5.0.0")
  public static ZonedDateTime asZonedDateTime(Date date) {
    return LegacyDateConverter.asZonedDateTime(date);
  }

  /**
   * Creates {@link ZonedDateTime} from {@code java.util.Date} or it's subclasses. Null-safe.
   *
   * @deprecated use {@link LegacyDateConverter#asZonedDateTime(Date, ZoneId)}
   */
  @Deprecated(since = "5.0.0")
  public static ZonedDateTime asZonedDateTime(Date date, ZoneId zone) {
    return LegacyDateConverter.asZonedDateTime(date, zone);
  }
}
