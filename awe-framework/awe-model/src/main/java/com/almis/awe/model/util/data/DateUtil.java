package com.almis.awe.model.util.data;

/*
 * File Imports
 */

import com.almis.awe.model.component.AweElements;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DateUtil Class
 * Date Utilities for AWE
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
  public static final DateTimeFormatter DATE_FORMAT_WEB_PARSER = new DateTimeFormatterBuilder()
          .appendPattern("dd/MM/yyyy[ HH:mm:ss]")
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();

  // Date in Web Format
  public static final DateTimeFormatter DATE_FORMAT_WEB = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  // Date in Js Format
  public static final DateTimeFormatter DATE_FORMAT_JS = DateTimeFormatter.ofPattern("MM/dd/yyyy");

  // Date in SQL Format
  public static final DateTimeFormatter DATE_FORMAT_SQL = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // Date in Web Service Format
  public static final DateTimeFormatter DATE_FORMAT_WBS = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // Date in RDB Format
  public static final DateTimeFormatter DATE_FORMAT_RDB = new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("dd-MMM-yyyy")
          .toFormatter(Locale.ENGLISH);

  // Timestamp in SQL Format
  public static final DateTimeFormatter TIMESTAMP_FORMAT_SQL_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  // Timestamp in Web Format with Optional milliseconds
  public static final DateTimeFormatter TIMESTAMP_FORMAT_WEB = new DateTimeFormatterBuilder().appendPattern("dd/MM/yyyy HH:mm:ss")
                  .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();

  // Timestamp in Web Format
  public static final DateTimeFormatter DATETIME_FORMAT_WEB = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  // Timestamp in Js Format
  public static final DateTimeFormatter TIMESTAMP_FORMAT_JS = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

  // Timestamp in Web Format with milliseconds
  public static final DateTimeFormatter TIMESTAMP_FORMAT_WEB_MS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

  // Time in Web Format
  public static final DateTimeFormatter TIME_FORMAT_WEB = DateTimeFormatter.ofPattern("HH:mm:ss");

  // Timestamp in Web Format with milliseconds
  public static final DateTimeFormatter JSON_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss.SSSZ");

  /**
   * Transforms a web date into a SQL Date
   *
   * @param date Web Date
   * @return SQL Date
   */
  public static java.sql.Date web2SqlDate(String date) {
    // Convert to sql Date
    return java.sql.Date.valueOf(LocalDate.parse(date, DATE_FORMAT_WEB));
  }

  /**
   * Transforms a web timestamp into a SQL Timestamp
   *
   * @param time Web Timestamp
   * @return SQL Date
   */
  public static java.sql.Time web2SqlTime(String time) {
    LocalTime localTime = LocalTime.parse(time, TIME_FORMAT_WEB);
    return Time.valueOf(localTime);
  }

  /**
   * Transforms a web date into a SQL Timestamp
   *
   * @param date Web Date
   * @return SQL Timestamp
   */
  public static java.sql.Timestamp webDate2SqlTimestamp(String date) {
    // Parse date and format as Timestamp
    return Timestamp.valueOf(LocalDate.parse(date, DATE_FORMAT_WEB).atStartOfDay());
  }

  /**
   * Transforms a web timestamp into a SQL Timestamp
   *
   * @param timestamp Web Timestamp
   * @return SQL Timestamp
   */
  public static java.sql.Timestamp web2SqlTimestamp(String timestamp) {

    LocalDateTime localDateTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT_WEB);
    return Timestamp.valueOf(localDateTime);
  }

  /**
   * Transforms a web date  (dd/MM/yyyy[ HH:mm:ss]) into a Date
   * Return null if  date is not valid
   *
   * @param date string Web date
   * @return Date
   */
  public static java.util.Date web2Date(String date) {

    // Variable definition
    java.util.Date webDat = null;

    try {
      // Parse initial date
      if (date != null && !"".equalsIgnoreCase(date)) {
        LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WEB_PARSER);
        webDat = asUtilDate(localDate);
      }
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB date to date -{}-", UTILITY_NAME, date, exc);
    }

    /* Return sql Date */
    return webDat;
  }

  /**
   * Transforms a web time into a Date
   * Return null if time is not valid
   *
   * @param time Web Time
   * @return SQL Date
   */
  public static java.util.Date web2Time(String time) {

    // Variable definition
    java.util.Date webDat;

    try {
      // Parse time
      LocalTime localTime = LocalTime.parse(time, TIME_FORMAT_WEB);
      webDat = Time.valueOf(localTime);
    } catch (Exception exc) {
      webDat = null;
      log.error("[{}] Error parsing WEB TIME to date -{}-", UTILITY_NAME, time, exc);
    }
    // Return sql Date
    return webDat;
  }

  /**
   * Transforms a web timestamp into a Date
   *
   * @param timestamp Web Timestamp date
   * @return timestamp Date
   */
  public static java.util.Date web2Timestamp(String timestamp) {
    // Variable definition
    java.util.Date webDat = null;

    try {
      if (timestamp != null) {
        // Parse initial date
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT_WEB);
        webDat = Timestamp.valueOf(localDateTime);
      }
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB TIMESTAMP to date -{}-", UTILITY_NAME, timestamp, exc);
    }
    // Return sql Date
    return webDat;
  }

  /**
   * Transforms a web timestamp with ms into a Date
   *
   * @param date Web Date
   * @return SQL Date
   */
  public static java.util.Date web2TimestampWithMs(String date) {

    // Variable definition
    java.util.Date webDat = null;
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(date, TIMESTAMP_FORMAT_WEB_MS);
      webDat = Timestamp.valueOf(localDateTime);
    } catch (Exception exc) {
      log.error("[{}] Error parsing WEB TIMESTAMP WITH MS to date -{}-", UTILITY_NAME, date, exc);
    }
    return webDat;
  }

  /**
   * Transforms a date into a SQL Date
   *
   * @param date Date
   * @return SQL Date
   */
  public static java.sql.Date dat2SqlDate(java.util.Date date) {
    // Return sql Date
    return new java.sql.Date(date.getTime());
  }

  /**
   * Transforms a date into a SQL Date
   *
   * @param date Date
   * @return SQL Date
   */
  public static String dat2SqlDateString(java.util.Date date) {
    // Get localDate and format
    return asLocalDate(date).format(DATE_FORMAT_SQL);
  }

  /**
   * Transforms a sql timestamp into a java Date
   *
   * @param timestamp Date
   * @return SQL Date
   */
  public static java.util.Date sqlTimestamp2Date(java.sql.Timestamp timestamp) {

    Date dateValue = null;

    if (timestamp != null) {
      dateValue = new java.util.Date(timestamp.getTime());
    }
    return dateValue;
  }

  /**
   * Transforms a sql date into a java Date
   *
   * @param date Date
   * @return SQL Date
   */
  public static java.util.Date sqlDate2Date(java.sql.Date date) {

    Date dateValue = null;

    if (date != null) {
      dateValue = new java.util.Date(date.getTime());
    }
    return dateValue;
  }

  /**
   * Transforms a date into a SQL Time
   *
   * @param date Date
   * @return SQL Timestamp
   */
  public static java.sql.Timestamp dat2SqlTimestamp(java.util.Date date) {
    // Get localDate and format
    LocalDateTime localDateTime = asLocalDateTime(date);
    // Return sql timestamp
    return java.sql.Timestamp.valueOf(localDateTime.format(TIMESTAMP_FORMAT_SQL_MS));
  }

  /**
   * Transforms a LocalDatetime into a Web Timestamp
   *
   * @param localDateTime Date
   * @return Web Timestamp
   */
  public static String localDatetime2WebTimestamp(LocalDateTime localDateTime) {
    // Format date
    return localDateTime.format(DATETIME_FORMAT_WEB);
  }

  /**
   * Transforms a LocalDate into a Web date
   *
   * @param localDate Date
   * @return Web Timestamp
   */
  public static String localDate2WebDate(LocalDate localDate) {
    // Format date
    return localDate.format(DATE_FORMAT_WEB);
  }

  /**
   * Transforms a timestamp date into a SQL Timestamp string
   *
   * @param date Date
   * @return SQL Timestamp
   */
  public static String dat2SqlTimeString(java.util.Date date) {
    // Get localDate and format
    LocalDateTime localDateTime = asLocalDateTime(date);
    // Return sql timestamp
    return localDateTime.format(TIMESTAMP_FORMAT_SQL_MS);
  }

  /**
   * Transforms a date time into a Web Timestamp
   *
   * @param dateTime Date
   * @return WEB Timestamp
   */
  public static String dat2WebTimestamp(java.util.Date dateTime) {
    // Get localDate and format
    LocalDateTime localDateTime = asLocalDateTime(dateTime);
    return DATETIME_FORMAT_WEB.format(localDateTime);
  }


  /**
   * Transforms a date into a Web Timestamp with milliseconds
   *
   * @param date dateTime Date
   * @return WEB Timestamp
   */
  public static String dat2WebTimestampMs(java.util.Date date) {
    // Get localDate and format
    LocalDateTime localDateTime = asLocalDateTime(date);
    return TIMESTAMP_FORMAT_WEB_MS.format(localDateTime);
  }

  /**
   * Transforms a time as Date into a Web Time
   *
   * @param date Date
   * @return WEB Time
   */
  public static String dat2WebTime(java.util.Date date) {
    // Get localDate and format
    LocalTime localTime = asLocalTime(date);
    return TIME_FORMAT_WEB.format(localTime);
  }

  /**
   * Transforms a date into a Web Date
   *
   * @param date Date
   * @return String WEB date
   */
  public static String dat2WebDate(java.util.Date date) {
    // Get localDate and format
    LocalDate localDate = asLocalDate(date);
    return DATE_FORMAT_WEB.format(localDate);
  }

  /**
   * Transforms a java Date into a date in milliseconds (Used in charts)
   *
   * @param date Date
   * @return string date in milliseconds
   */
  public static String dat2DateMs(java.util.Date date) {
    // Convert to date milliseconds
    return String.valueOf(date.getTime());
  }

  /**
   * Transforms a SQL String date into a web date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sql2WebDate(String date) {
    // Variable definition
    String outDat;
    try {
      // Parse initial date
      LocalDateTime localDateTime = LocalDateTime.parse(date, TIMESTAMP_FORMAT_SQL_MS);
      outDat = localDateTime.format(DATE_FORMAT_WEB);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing SQL date to WEB date -{}-", UTILITY_NAME, date, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a java date
   *
   * @param timestamp SQL String Timestamp
   * @return Web date formatted
   */
  public static java.util.Date sql2JavaDate(String timestamp) {
    LocalDateTime localDateTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT_SQL_MS);
    return java.sql.Timestamp.valueOf(localDateTime);
  }

  /**
   * Transforms a SQL String date (without time) into a java date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static java.util.Date sqlDate2JavaDate(String date) {
    LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_SQL);
    return asUtilDate(localDate);
  }

  /**
   * Transforms a WBS String date into a java date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static java.util.Date wbs2JavaDate(String date) {
    LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WBS);
    return asUtilDate(localDate);
  }

  /**
   * Transforms a RDB String date into a java date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static java.util.Date rdb2Date(String date) {
    /* Return web date string */
    return asUtilDate(LocalDate.parse(date, DATE_FORMAT_RDB));
  }

  /**
   * Transforms a SQL String date into a web time
   *
   * @param timestamp SQL String Date
   * @return Web time formatted
   */
  public static String sql2WebTime(String timestamp) {

    // Variable definition
    String outDat;
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT_SQL_MS);
      outDat = TIME_FORMAT_WEB.format(localDateTime);
    } catch (Exception exc) {
      outDat = timestamp;
      log.error("[{}] Error parsing SQL date to WEB TIME -{}-", UTILITY_NAME, timestamp, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a web timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sql2WebTimestamp(String date) {

    // Variable definition
    String outDat;

    try {
      // Parse initial date
      LocalDateTime localDateTime = LocalDateTime.parse(date, TIMESTAMP_FORMAT_SQL_MS);
      outDat = TIMESTAMP_FORMAT_WEB.format(localDateTime);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing SQL date to WEB TIMESTAMP -{}-", UTILITY_NAME, date, exc);
    }

    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a js date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sql2JsDate(String date) {

    // Variable definition
    String outDat;

    try {
      // Parse initial date
      LocalDateTime localDateTime = LocalDateTime.parse(date, TIMESTAMP_FORMAT_SQL_MS);
      outDat = DATE_FORMAT_JS.format(localDateTime);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing SQL date to JS date -{}-", UTILITY_NAME, date, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Parse sql date to java date in milliseconds
   *
   * @param val Sql date
   * @return Java date in milliseconds
   */
  public static String sql2DateMs(String val) {
    String dateMs;
    Date date = sql2JavaDate(val);
    dateMs = dat2DateMs(date);
    return dateMs;
  }

  /**
   * Transforms a SQL String date into a js timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sql2JsTimestamp(String date) {

    // Variable definition
    String outDat;
    try {
      // Parse initial date
      LocalDateTime localDateTime = LocalDateTime.parse(date, TIMESTAMP_FORMAT_SQL_MS);
      outDat = TIMESTAMP_FORMAT_JS.format(localDateTime);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing SQL date to JS TIMESTAMP -{}-", UTILITY_NAME, date, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a web date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sqlDat2WebDate(java.sql.Date date) {
    String outDat = null;
    try {
      // Parse initial date
      outDat = DATE_FORMAT_WEB.format(date.toLocalDate());
    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to WEB date -{}-", UTILITY_NAME, date.toString(), exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a web time
   *
   * @param date SQL String Date
   * @return Web time formatted
   */
  public static String sqlDat2WebTime(java.sql.Time date) {

    // Variable definition
    String outDat = null;

    try {
      /* Parse initial date */
      outDat = TIME_FORMAT_WEB.format(date.toLocalTime());
    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to WEB TIME -{}-", UTILITY_NAME, date.toString(), exc);
    }

    /* Return web date string */
    return outDat;
  }

  /**
   * Transforms a SQL String date into a web timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sqlDat2WebTimestamp(java.sql.Timestamp date) {

    // Variable definition
    String outDat = null;
    try {
      /* Parse initial date */
      outDat = TIMESTAMP_FORMAT_WEB.format(date.toLocalDateTime());
    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to WEB TIMESTAMP -{}-", UTILITY_NAME, date.toString(), exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a web date
   *
   * @param date SQL String Date
   * @return Web date formatted
   */
  public static String sqlDat2JsDate(java.sql.Date date) {

    // Variable definition
    String outDat = null;

    try {
      // Parse and format date
      outDat = DATE_FORMAT_JS.format(date.toLocalDate());
    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to JS date -{}-", UTILITY_NAME, date.toString(), exc);
    }

    // Return web date string
    return outDat;
  }

  /**
   * Transforms a SQL String date into a js timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sqlDat2JsTimestamp(java.sql.Timestamp date) {

    // Variable definition
    String outDat = null;

    try {
      // Parse and format date
      outDat = TIMESTAMP_FORMAT_JS.format(date.toLocalDateTime());
    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to JS TIMESTAMP -{}-", UTILITY_NAME, date.toString(), exc);
    }

    // Return web date string
    return outDat;
  }

  /**
   * Transforms a Date into a js date
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String dat2JsDate(java.util.Date date) {
    // Get local date and format
    LocalDate localDate = asLocalDate(date);
    return DATE_FORMAT_JS.format(localDate);
  }

  /**
   * Transforms a Date into a js timestamp
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String dat2JsTimestamp(java.util.Date date) {
    // Get local date and format
    LocalDateTime localDateTime = asLocalDateTime(date);
    return TIMESTAMP_FORMAT_JS.format(localDateTime);
  }

  /**
   * Transforms a SQL String date into a web timestamp with Milliseconds
   *
   * @param date SQL String Date
   * @return Web timestamp formatted
   */
  public static String sqlDat2WebTimestampWithMs(java.sql.Timestamp date) {

    // Variable definition
    String outDat = null;

    try {
      // Parse initial date
      outDat = TIMESTAMP_FORMAT_WEB_MS.format(date.toLocalDateTime());

    } catch (Exception exc) {
      log.error("[{}] Error parsing SQL date to WEB TIMESTAMP WITH MS -{}-", UTILITY_NAME, date.toString(), exc);
    }

    /* Return web date string */
    return outDat;
  }

  /**
   * Transforms a web date into a web service date
   *
   * @param date (Web formatted)
   * @return Web Service date formatted
   */
  public static String web2WbsDate(String date) {

    // Variable definition
    String outDat;

    try {
      /* Parse initial date */
      LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WEB);
      outDat = DATE_FORMAT_WBS.format(localDate);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing WEB date to WBS date -{}-", UTILITY_NAME, date, exc);
    }

    // Return web date string
    return outDat;
  }

  /**
   * Transforms a date into a web service date
   *
   * @param date (Web formatted)
   * @return Web Service date formatted
   */
  public static String dat2WbsDate(java.util.Date date) {

    // Variable definition
    String outDat;

    try {
      // Get local date and format
      LocalDate localDate = asLocalDate(date);
      outDat = DATE_FORMAT_WBS.format(localDate);
    } catch (Exception exc) {
      outDat = date.toString();
      log.error("[{}] Error parsing date to WBS date -{}-", UTILITY_NAME, date, exc);
    }

    // Return web date string
    return outDat;
  }

  /**
   * Transforms a web date into an RDB date (23-OCT-1978)
   *
   * @param date (Web formatted)
   * @return RDB date formatted
   */
  public static String web2RdbDate(String date) {

    // Variable definition
    String outDat;

    try {
      // Parse initial date and format
      LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WEB);
      outDat = DATE_FORMAT_RDB.format(localDate);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing WEB date to RDB date -{}-", UTILITY_NAME, date, exc);
    }

    // Return rdb date string in UPPERCASE
    return outDat.toUpperCase();
  }

  /**
   * Transforms a web service date into a web date
   *
   * @param date (Web service formatted)
   * @return Web date formatted
   */
  public static String wbs2WebDate(String date) {

    // Variable definition
    String outDat;
    try {
      // Parse initial date and format
      LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WBS);
      outDat = DATE_FORMAT_WEB.format(localDate);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing WBS date to WEB date -{}-", UTILITY_NAME, date, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Transforms a web service date into a js date
   *
   * @param date (Web service formatted)
   * @return Web date formatted
   */
  public static String wbs2JsDate(String date) {

    // Variable definition
    String outDat;
    try {
      // Parse initial date and format
      LocalDate localDate = LocalDate.parse(date, DATE_FORMAT_WBS);
      outDat = DATE_FORMAT_JS.format(localDate);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing WBS date to JS date -{}-", UTILITY_NAME, date, exc);
    }
    // Return web date string
    return outDat;
  }

  /**
   * Returns system date in SQL timestamp format (yyyy-MM-dd HH:mm:ss.SSS)
   *
   * @return String SQL system date
   */
  public static String getSystemDate() {
    // Return sql Date
    return TIMESTAMP_FORMAT_SQL_MS.format(LocalDateTime.now());
  }

  /**
   * Returns Calendar object for string Date with SQL format
   *
   * @param strSqlDate SQL Date
   * @return Calendar object
   */
  public static Calendar getCalendarDate(String strSqlDate) {

    // Variable definition
    Calendar cal = Calendar.getInstance();

    // Set date
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
    return isXXX(date, DATE_FORMAT_WEB);
  }

  /**
   * Returns true if date is an WBS date
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWbsDate(String date) {
    return isXXX(date, DATE_FORMAT_WBS);
  }

  /**
   * Returns true if date is an SQL date
   *
   * @param date (Web service formatted)
   * @return Is an SQL date
   */
  public static boolean isSqlDate(String date) {
    return isXXX(date, TIMESTAMP_FORMAT_SQL_MS);
  }

  /**
   * Returns true if date is a Web timestamp
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWebTimestamp(String date) {
    return isXXX(date, TIMESTAMP_FORMAT_WEB);
  }

  /**
   * Returns true if date is a Web date with milliseconds
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isWebTimestampWithMs(String date) {
    return isXXX(date, TIMESTAMP_FORMAT_WEB_MS);
  }

  /**
   * Returns true if date is json date
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  public static boolean isJsonDate(String date) {
    return isXXX(date, JSON_DATE);
  }

  /**
   * Returns true if date is an WBS date
   *
   * @param date (Web service formatted)
   * @return Is an WBS date
   */
  private static boolean isXXX(String date, DateTimeFormatter format) {
    try {
      format.parse(date);
      return true;
    } catch (Exception exc) {
      return false;
    }
  }

  /**
   * Transforms a JSON Date to a Date object
   *
   * @param date JSON date
   * @return Date OBJECT
   */
  public static Date jsonDate(String date) {
    // Return web date string
    return java.sql.Timestamp.valueOf(LocalDateTime.parse(date, JSON_DATE));
  }

  /**
   * Transforms a JSON Date from a Date object
   *
   * @param date JSON date
   * @return Date OBJECT
   */
  public static String jsonDate(Date date) {
    // Get local date and format
    ZonedDateTime zonedDateTime = asZonedDateTime(date);
    // Return web date string
    return JSON_DATE.format(zonedDateTime);
  }

  /**
   * Build java Date object from date criteria with Time criteria
   *
   * @param date Date criteria [dd/MM/yyyy]
   * @param time Time criteria [HH:mm:ss]
   * @return Object date from criterion's or null value if any criterion are null
   * with format [dd/MM/yyyy HH:mm:ss]
   */
  public static Date getDateWithTimeFromCriteria(String date, String time) {
    LocalDateTime localDateTime = LocalDateTime.parse(date + " " + time, TIMESTAMP_FORMAT_WEB);
    return java.sql.Timestamp.valueOf(localDateTime);
  }

  /**
   * Build java Date object from date criteria with Time criteria
   *
   * @param date Date criteria [dd/MM/yyyy]
   * @param time Time criteria [HH:mm:ss]
   * @return Object date from criteria or null value if any criterion is null
   * with format [dd/MM/yyyy HH:mm:ss]
   */
  public static Date addTimeToDate(Date date, String time) {
    Date fullDate = date;
    if (time != null && !time.isEmpty()) {
      fullDate = java.sql.Timestamp.valueOf(LocalDateTime.parse(dat2WebDate(date) + " " + time, TIMESTAMP_FORMAT_WEB));
    }
    return fullDate;
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

    // Variable definition
    String outDat;

    try {
      // Create format from and format to
      DateTimeFormatter fromDateTimeFormatter =
              new DateTimeFormatterBuilder().appendPattern(formatFrom + "[ HH:mm:ss.SSS]")
                      .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                      .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                      .parseCaseInsensitive()
                      .toFormatter(Locale.ENGLISH);
      DateTimeFormatter toDateTimeFormatter = DateTimeFormatter.ofPattern(formatTo).withLocale(Locale.ENGLISH);

      // Parse dates
      LocalDateTime auxLocalDateTime = LocalDateTime.parse(dateIn, fromDateTimeFormatter);
      outDat = toDateTimeFormatter.format(auxLocalDateTime);

    } catch (Exception ex) {
      outDat = dateIn;
      log.error("[{}] Error parsing generic date from {} to {} -{}-", UTILITY_NAME, dateIn, formatFrom, formatTo, ex);
    }
    return outDat;
  }

  /**
   * Transforms an RDB date into a web date (23-OCT-1978)
   *
   * @param date (Web formatted)
   * @return RDB date formatted
   */
  public static String rdbDate2Web(String date) {

    /* Variable definition */
    String outDat;

    try {
      // Parse initial date and format
      LocalDate parse = LocalDate.parse(date, DATE_FORMAT_RDB);
      outDat = DATE_FORMAT_WEB.format(parse);
    } catch (Exception exc) {
      outDat = date;
      log.error("[{}] Error parsing RDB date to WEB date -{}-", UTILITY_NAME, date, exc);
    }

    /* Return rdb date string in UPPERCASE */
    return outDat.toUpperCase();
  }

  /**
   * Transforms Date into an RDB String  (23-OCT-1978)
   *
   * @param date (Web formatted)
   * @return RDB date formatted
   */
  public static String dat2RDBDate(java.util.Date date) {

    // Variable definition
    String outDat = null;

    try {
      // Generate rdb date string
      outDat = asLocalDate(date).format(DATE_FORMAT_RDB).toUpperCase();
    } catch (Exception exc) {
      log.error("[{}] Error parsing date to RDB date -{}-", UTILITY_NAME, date, exc);
    }

    // Return rdb date string in UPPERCASE
    return outDat;
  }

  /**
   * Retrieve how much time has elapsed from milliseconds
   *
   * @param milliseconds Difference in milliseconds
   * @param elements     AWE Elements
   * @return Date since
   */
  public static String elapsedTime(Long milliseconds, AweElements elements) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis(milliseconds);
    int mYear = calendar.get(Calendar.YEAR) - 1970;
    int mMonth = calendar.get(Calendar.MONTH);
    int mDay = calendar.get(Calendar.DAY_OF_MONTH) - 1;
    int mHour = calendar.get(Calendar.HOUR);

    if (mYear > 1) {
      return elements.getLocaleWithLanguage("YEARS", elements.getLanguage(), mYear);
    } else if (mYear > 0) {
      return elements.getLocaleWithLanguage("YEAR", elements.getLanguage(), 1);
    } else if (mMonth > 1) {
      return elements.getLocaleWithLanguage("MONTHS", elements.getLanguage(), mMonth);
    } else if (mMonth > 0) {
      return elements.getLocaleWithLanguage("MONTH", elements.getLanguage(), 1);
    } else if (mDay > 14) {
      return elements.getLocaleWithLanguage("WEEKS", elements.getLanguage(), Math.floorDiv(mDay, 7));
    } else if (mDay > 7) {
      return elements.getLocaleWithLanguage("WEEK", elements.getLanguage(), 1);
    } else if (mDay > 0) {
      return elements.getLocaleWithLanguage("DAYS", elements.getLanguage(), mDay);
    } else if (mHour > 0) {
      return elements.getLocaleWithLanguage("HOURS", elements.getLanguage(), mHour);
    } else if (calendar.get(Calendar.MINUTE) > 0) {
      return elements.getLocaleWithLanguage("MINUTES", elements.getLanguage(), calendar.get(Calendar.MINUTE));
    } else if (calendar.get(Calendar.SECOND) > 0) {
      return elements.getLocaleWithLanguage("SECONDS", elements.getLanguage(), calendar.get(Calendar.SECOND));
    } else {
      return elements.getLocaleWithLanguage("MILLISECONDS", elements.getLanguage(), milliseconds);
    }
  }

  /**
   * Retrieve how much time has elapsed since a date
   *
   * @param date     Date to check
   * @param elements AWE Elements
   * @return Date since
   */
  public static String dateSince(java.util.Date date, AweElements elements) {
    return elements.getLocaleWithLanguage("TIME_AGO", elements.getLanguage(), elapsedTime(new Date().getTime() - date.getTime(), elements));
  }

  /**
   * Autodetect date format and applies a format into java date
   *
   * @param dateString String date
   * @return Date formatted
   */
  public static Date autoDetectDateFormat(String dateString) {
    if (isJsonDate(dateString)) {
      return jsonDate(dateString);
    } else if (isSqlDate(dateString)) {
      return sql2JavaDate(dateString);
    } else if (isWebDate(dateString)) {
      return web2Date(dateString);
    } else if (isWbsDate(dateString)) {
      return wbs2JavaDate(dateString);
    } else if (isWebTimestamp(dateString)) {
      return web2Timestamp(dateString);
    }
    return null;
  }

  /**
   * Calls {@link #asLocalDate(Date, ZoneId)} with the system default time zone.
   */
  public static LocalDate asLocalDate(java.util.Date date) {
    return asLocalDate(date, ZoneId.systemDefault());
  }

  /**
   * Creates {@link LocalDate} from {@code java.util.Date} or it's subclasses.
   */
  public static LocalDate asLocalDate(java.util.Date date, ZoneId zone) {

    if (date instanceof java.sql.Date)
      return ((java.sql.Date) date).toLocalDate();
    else
      return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
  }

  /**
   * Calls {@link #asLocalTime(Date, ZoneId)} with the system default time zone.
   */
  public static LocalTime asLocalTime(java.util.Date date) {
    return asLocalTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates {@link LocalTime} from {@code java.util.Date} or it's subclasses.
   */
  public static LocalTime asLocalTime(java.util.Date date, ZoneId zone) {

    if (date instanceof java.sql.Time)
      return ((java.sql.Time) date).toLocalTime();
    else
      return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalTime();
  }

  /**
   * Calls {@link #asLocalDateTime(Date, ZoneId)} with the system default time zone.
   */
  public static LocalDateTime asLocalDateTime(java.util.Date date) {
    return asLocalDateTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates {@link LocalDateTime} from {@code java.util.Date} or it's subclasses.
   */
  public static LocalDateTime asLocalDateTime(java.util.Date date, ZoneId zone) {

    if (date instanceof java.sql.Timestamp)
      return ((java.sql.Timestamp) date).toLocalDateTime();
    else
      return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDateTime();
  }

  /**
   * Calls {@link #asUtilDate(Object, ZoneId)} with the system default time zone.
   */
  public static java.util.Date asUtilDate(Object date) {
    return asUtilDate(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link java.util.Date} from various date objects. Is null-safe. Currently, supports:<ul>
   * <li>{@link java.util.Date}
   * <li>{@link java.sql.Date}
   * <li>{@link java.sql.Timestamp}
   * <li>{@link java.time.LocalDate}
   * <li>{@link java.time.LocalDateTime}
   * <li>{@link java.time.ZonedDateTime}
   * <li>{@link java.time.Instant}
   * </ul>
   *
   * @param zone Time zone, used only if the input object is LocalDate or LocalDateTime.
   *
   * @return {@link java.util.Date} (exactly this class, not a subclass, such as java.sql.Date)
   */
  public static java.util.Date asUtilDate(Object date, ZoneId zone) {
    if (date == null)
      return null;

    if (date instanceof java.sql.Date || date instanceof java.sql.Timestamp)
      return new java.util.Date(((java.util.Date) date).getTime());
    if (date instanceof java.util.Date)
      return (java.util.Date) date;
    if (date instanceof LocalDate)
      return java.util.Date.from(((LocalDate) date).atStartOfDay(zone).toInstant());
    if (date instanceof LocalDateTime)
      return java.util.Date.from(((LocalDateTime) date).atZone(zone).toInstant());
    if (date instanceof ZonedDateTime)
      return java.util.Date.from(((ZonedDateTime) date).toInstant());
    if (date instanceof Instant)
      return java.util.Date.from((Instant) date);

    throw new UnsupportedOperationException("Don't know hot to convert " + date.getClass().getName() + " to java.util.Date");
  }

  /**
   * Creates an {@link Instant} from {@code java.util.Date} or it's subclasses. Null-safe.
   */
  public static Instant asInstant(Date date) {
    if (date == null)
      return null;
    else
      return Instant.ofEpochMilli(date.getTime());
  }

  /**
   * Calls {@link #asZonedDateTime(Date, ZoneId)} with the system default time zone.
   */
  public static ZonedDateTime asZonedDateTime(Date date) {
    return asZonedDateTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates {@link ZonedDateTime} from {@code java.util.Date} or it's subclasses. Null-safe.
   */
  public static ZonedDateTime asZonedDateTime(Date date, ZoneId zone) {
    if (date == null)
      return null;
    else
      return asInstant(date).atZone(zone);
  }
}
