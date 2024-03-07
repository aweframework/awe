package com.almis.awe.model.util.data;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

import static com.almis.awe.model.util.data.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Date Util tests
 *
 * @author pgarcia & pvidal
 */
class DateUtilTest {

  /**
   * Test of check public addresses
   */
  @Test
  void testRdb2JavaDate() {

    Date expect = java.sql.Date.valueOf(LocalDate.parse("23/10/1978", DATE_FORMAT_WEB));
    Date expect2 = java.sql.Date.valueOf(LocalDate.parse("02/01/2018", DATE_FORMAT_WEB));
    Date expect3 = java.sql.Date.valueOf(LocalDate.parse("05/08/2011", DATE_FORMAT_WEB));
    Date expect4 = java.sql.Date.valueOf(LocalDate.parse("31/03/2004", DATE_FORMAT_WEB));

    // Run
    Date date = DateUtil.rdb2Date("23-OCT-1978");
    Date date2 = DateUtil.rdb2Date("02-JAN-2018");
    Date date3 = DateUtil.rdb2Date("05-AUG-2011");
    Date date4 = DateUtil.rdb2Date("31-MAR-2004");

    // Assert
    assertAll("Should assert all java date",
            () -> assertEquals(expect, date),
            () -> assertEquals(expect2, date2),
            () -> assertEquals(expect3, date3),
            () -> assertEquals(expect4, date4)
    );
  }

  /**
   * Test of check public addresses
   */
  @Test
  void testDateAndTime() {
    // Prepare
    Date expect = java.sql.Date.valueOf(LocalDate.parse("23/10/1978", DATE_FORMAT_WEB));
    Date expect2 = java.sql.Date.valueOf(LocalDate.parse("02/01/2018", DATE_FORMAT_WEB));
    Date expect5 = java.sql.Timestamp.valueOf(LocalDateTime.parse("23/10/1978 20:11:23", TIMESTAMP_FORMAT_WEB));

    // Run
    Date date5 = DateUtil.getDateWithTimeFromCriteria("23/10/1978", "20:11:23");
    Date date6 = DateUtil.addTimeToDate(expect, "20:11:23");
    Date date7 = DateUtil.addTimeToDate(expect2, "");
    Date date8 = DateUtil.addTimeToDate(expect2, null);

    // Assert
    assertAll("Should assert all date times",
            () -> assertEquals(expect5, date5),
            () -> assertEquals(expect5, date6),
            () -> assertEquals(expect2, date7),
            () -> assertEquals(expect2, date8)
    );
  }

  /**
   * Test of check date type
   */
  @Test
  void testIsDateFormat() {
    // Assert
    assertTrue(DateUtil.isWebDate("23/10/1978"));
    assertFalse(DateUtil.isWebDate("10231978"));

    assertTrue(DateUtil.isWbsDate("1978-10-23"));
    assertFalse(DateUtil.isWbsDate("23/10/1978"));

    assertTrue(DateUtil.isSqlDate("1978-10-23 21:55:31.012"));
    assertFalse(DateUtil.isSqlDate("23/10/1978 21:55:31.012"));

    assertTrue(DateUtil.isWebTimestamp("23/10/1978 10:00:02"));
    assertFalse(DateUtil.isWebTimestamp("1978-10-23 10:00:02"));

    assertTrue(DateUtil.isWebTimestampWithMs("23/10/1978 10:00:02.202"));
    assertFalse(DateUtil.isWebTimestampWithMs("1978-10-23 10:00:02.121"));

    assertTrue(DateUtil.isJsonDate("1978-10-23@12:23:33.221+0100"));
    assertFalse(DateUtil.isJsonDate("1978-10-23"));

    String textDate = DateUtil.dat2WebDate(new Date());
    assertEquals(DateUtil.web2Date(textDate), DateUtil.autoDetectDateFormat(textDate));
  }

  @Test
  void web2SqlDate() {
    String webDate = "10/02/1985";
    LocalDate localDate = LocalDate.parse(webDate, DATE_FORMAT_WEB);
    assertEquals(java.sql.Date.valueOf(localDate), DateUtil.web2SqlDate(webDate));
  }

  @Test
  void web2SqlTime() {
    String time = "14:30:25";
    assertEquals(Time.valueOf(time), DateUtil.web2SqlTime(time));
  }

  @Test
  void webDate2SqlTimestamp() {
    String webDate = "10/02/1985";
    LocalDateTime localDateTime = LocalDate.parse(webDate, DATE_FORMAT_WEB).atStartOfDay();
    assertEquals(Timestamp.valueOf(localDateTime), DateUtil.webDate2SqlTimestamp(webDate));
  }

  @Test
  void web2SqlTimestamp() {
    Timestamp expect = java.sql.Timestamp.valueOf(LocalDateTime.parse("23/10/1978 20:10:12", TIMESTAMP_FORMAT_WEB));
    assertEquals(expect, DateUtil.web2SqlTimestamp("23/10/1978 20:10:12"));
  }

  @Test
  void web2Time() {
    LocalTime expectedTime = LocalTime.parse("20:10:12");
    assertEquals(Time.valueOf(expectedTime), DateUtil.web2Time("20:10:12"));
  }

  @Test
  void web2TimeErrorParse_returnNullValue() {
    assertNull(DateUtil.web2Time("23/10/1978 20:10:12"));
  }

  @Test
  void web2Timestamp() {
    Date expectedTimeStamp = DateUtil.asUtilDate(LocalDateTime.parse("23/10/1978 20:10:12", TIMESTAMP_FORMAT_WEB));
    assertEquals(expectedTimeStamp, DateUtil.web2Timestamp("23/10/1978 20:10:12"));
    assertNull(DateUtil.web2Timestamp("10/23/1978"));
    assertNull(DateUtil.web2Timestamp(null));
  }

  @Test
  void web2TimestampWithMs() {
    Date expectedTimeStamp = DateUtil.asUtilDate(LocalDateTime.parse("23/10/1978 20:10:12.123", TIMESTAMP_FORMAT_WEB_MS));
    assertEquals(expectedTimeStamp, DateUtil.web2TimestampWithMs("23/10/1978 20:10:12.123"));
    assertNull(DateUtil.web2TimestampWithMs("10/23/1978 20:10:12.123"));
  }

  @Test
  void web2Date() {
    Date expectedDate1 = DateUtil.asUtilDate(LocalDate.parse("10/02/1985", DATE_FORMAT_WEB_PARSER));
    Date expectedDate2 = DateUtil.asUtilDate(LocalDateTime.parse("10/02/1985 00:00:00", DATE_FORMAT_WEB_PARSER));
    assertAll("Parse all string web Dates",
            () -> assertEquals(expectedDate1, DateUtil.web2Date("10/02/1985")),
            () -> assertEquals(expectedDate2, DateUtil.web2Date("10/02/1985 00:00:00"))
    );
  }

  @Test
  void dat2SqlDate() {
    Date date = DateUtil.web2Date("10/02/1985");
    java.sql.Date expectedDate = DateUtil.web2SqlDate("10/02/1985");
    assertEquals(expectedDate, DateUtil.dat2SqlDate(date));
  }

  @Test
  void sqlDat2JsTimestamp() {
    String expectedJsTimestamp = "02/10/1985 20:10:12";
    assertEquals(expectedJsTimestamp, DateUtil.sqlDat2JsTimestamp(java.sql.Timestamp.valueOf(LocalDateTime.parse("10/02/1985 20:10:12.123", TIMESTAMP_FORMAT_WEB))));
  }

  @Test
  void sqlDat2WebTimestampMs() {
    String expectedWebTimestampMs = "10/02/1985 20:10:12.123";
    assertEquals(expectedWebTimestampMs, DateUtil.sqlDat2WebTimestampWithMs(java.sql.Timestamp.valueOf(LocalDateTime.parse("10/02/1985 20:10:12.123", TIMESTAMP_FORMAT_WEB))));
  }

  @Test
  void sqlDat2JsDate() {
    String expectedJsDate = "10/02/1985";
    assertEquals(expectedJsDate, DateUtil.sqlDat2JsDate(java.sql.Date.valueOf(LocalDate.parse("02/10/1985", DATE_FORMAT_WEB))));
  }

  @Test
  void sqlTimestamp2Date() {
    Date expectedDate = DateUtil.web2Timestamp("10/02/1085 23:23:00.212");
    java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(LocalDateTime.parse("10/02/1085 23:23:00.212", TIMESTAMP_FORMAT_WEB_MS));
    assertEquals(expectedDate.getTime(), DateUtil.sqlTimestamp2Date(timestamp).getTime());
  }

  @Test
  void sqlDate2Date() {
    java.sql.Date expectedDate = DateUtil.web2SqlDate("10/02/1985");
    Date checkDate = DateUtil.web2Date("10/02/1985");
    assertEquals(checkDate.getTime(), DateUtil.sqlDate2Date(expectedDate).getTime());
  }

  @Test
  void dat2SqlTimestamp() {
    Date timestampDate = DateUtil.web2Timestamp("10/02/1085 23:23:00.212");
    Timestamp expectedTimestamp = Timestamp.valueOf(LocalDateTime.parse("10/02/1085 23:23:00.212", TIMESTAMP_FORMAT_WEB_MS));
    assertEquals(expectedTimestamp, DateUtil.dat2SqlTimestamp(timestampDate));
  }

  @Test
  void dat2DateMs() {
    Date date = DateUtil.asUtilDate(Instant.now());
    String expectedMilliseconds = String.valueOf(date.getTime());
    assertEquals(expectedMilliseconds, DateUtil.dat2DateMs(date));
  }

  @Test
  void sql2WebDateTest() {
    String expectedDate = "10/02/1985";
    assertEquals(expectedDate, DateUtil.sql2WebDate("1985-02-10 00:00:00.000"));
  }

  @Test
  void sql2WebDateError_returnSqlDate() {
    assertEquals("1985-02-10", DateUtil.sql2WebDate("1985-02-10"));
  }

  @Test
  void sql2JavaDate() {
    LocalDateTime localDateTime = LocalDateTime.parse("10/02/1985 14:00:00.123", TIMESTAMP_FORMAT_WEB_MS);
    Date expectedDate = DateUtil.asUtilDate(localDateTime);
    assertEquals(expectedDate, DateUtil.sql2JavaDate("1985-02-10 14:00:00.123"));
  }

  @Test
  void sqlDate2JavaDate() {
    LocalDate localDate = LocalDate.parse("10/02/1985", DATE_FORMAT_WEB);
    Date expectedDate = DateUtil.asUtilDate(localDate);
    assertEquals(expectedDate, DateUtil.sqlDate2JavaDate("1985-02-10"));
  }

  @Test
  void wbs2JavaDate() {
    LocalDate localDate = LocalDate.parse("10/02/1985", DATE_FORMAT_WEB);
    Date expectedDate = DateUtil.asUtilDate(localDate);
    assertEquals(expectedDate, DateUtil.wbs2JavaDate("1985-02-10"));
  }

  @Test
  void sql2WebTime() {
    String expectedTime = "14:00:00";
    assertEquals(expectedTime, DateUtil.sql2WebTime("1985-02-10 14:00:00.123"));
  }

  @Test
  void sql2WebTime_Error_returnSqlTimestamp() {
    assertEquals("10/02/1985 14:00:00.123", DateUtil.sql2WebTime("10/02/1985 14:00:00.123"));
  }

  @Test
  void sql2WebTimestamp() {
    String expectedTimeStamp = "10/02/1985 14:00:00.123";
    assertEquals(expectedTimeStamp, DateUtil.sql2WebTimestamp("1985-02-10 14:00:00.123"));
  }

  @Test
  void sql2WebTimestamp_Error_returnSqlTimestamp() {
    String expectedTimeStamp = "10/02/1985 14:00:00.123";
    assertEquals(expectedTimeStamp, DateUtil.sql2WebTimestamp("10/02/1985 14:00:00.123"));
  }

  @Test
  void sql2JsDate() {
    String expectedTimeStamp = "02/10/1985";
    assertEquals(expectedTimeStamp, DateUtil.sql2JsDate("1985-02-10 14:00:00.123"));
  }

  @Test
  void sql2JsDate_Error_returnSql() {
    String expectedTimeStamp = "10/02/1985 14:00:00.123";
    assertEquals(expectedTimeStamp, DateUtil.sql2JsDate("10/02/1985 14:00:00.123"));
  }

  @Test
  void sql2DateMs() {
    String expectedMilliseconds = String.valueOf(DateUtil.asUtilDate(LocalDateTime.parse("1985-02-10 14:00:00.123", TIMESTAMP_FORMAT_SQL_MS)).getTime());
    assertEquals(expectedMilliseconds, DateUtil.sql2DateMs("1985-02-10 14:00:00.123"));
  }

  @Test
  void sql2JsTimestamp() {
    String expectedTimeStamp = "02/10/1985 14:00:00";
    assertEquals(expectedTimeStamp, DateUtil.sql2JsTimestamp("1985-02-10 14:00:00.123"));
  }

  @Test
  void sql2JsTimestamp_Error_returnSql() {
    String expectedTimeStamp = "02/10/1985 14:00:00";
    assertEquals(expectedTimeStamp, DateUtil.sql2JsTimestamp("02/10/1985 14:00:00"));
  }

  @Test
  void sqlDat2WebDate() {
    String expectedTimeStamp = "02/10/1985";
    assertEquals(expectedTimeStamp, DateUtil.sqlDat2WebDate(java.sql.Date.valueOf(LocalDate.parse("02/10/1985", DATE_FORMAT_WEB))));
  }

  @Test
  void sqlDat2WebTime() {
    String expectedTime = "10:10:35";
    assertEquals(expectedTime, DateUtil.sqlDat2WebTime(java.sql.Time.valueOf(LocalTime.parse("10:10:35", TIME_FORMAT_WEB))));
  }

  @Test
  void sqlDat2WebTimestamp() {
    String expectedTimeStamp = "02/10/1985 14:00:00";
    assertEquals(expectedTimeStamp, DateUtil.sqlDat2WebTimestamp(Timestamp.valueOf(LocalDateTime.parse("02/10/1985 14:00:00", TIMESTAMP_FORMAT_WEB))));
  }

  @Test
  void dat2JsDate() {
    String expectedDate = "10/27/1985";
    LocalDate localDate = LocalDate.parse("27/10/1985", DATE_FORMAT_WEB);
    assertEquals(expectedDate, DateUtil.dat2JsDate(DateUtil.asUtilDate(localDate)));
  }

  @Test
  void dat2JsTimestamp() {
    String expectedDate = "10/27/1985 14:00:00";
    LocalDateTime localDateTime = LocalDateTime.parse("27/10/1985 14:00:00", TIMESTAMP_FORMAT_WEB);
    assertEquals(expectedDate, DateUtil.dat2JsTimestamp(DateUtil.asUtilDate(localDateTime)));
  }

  @Test
  void sqlDat2WebTimestampWithMs() {
    String expectedDate = "10/27/1985 14:00:00";
    LocalDateTime localDateTime = LocalDateTime.parse("27/10/1985 14:00:00", TIMESTAMP_FORMAT_WEB);
    assertEquals(expectedDate, DateUtil.dat2JsTimestamp(DateUtil.asUtilDate(localDateTime)));
  }

  @Test
  void web2WbsDate() {
    String expectedDate = "1985-10-27";
    assertEquals(expectedDate, DateUtil.web2WbsDate("27/10/1985"));
  }

  @Test
  void dat2WbsDate() {
    String expectedDate = "1985-10-27";
    LocalDate localDate = LocalDate.parse("27/10/1985", DATE_FORMAT_WEB);
    assertEquals(expectedDate, DateUtil.dat2WbsDate(DateUtil.asUtilDate(localDate)));
  }

  @Test
  void web2RdbDate() {
    String expectedDate = "10-FEB-1985";
    assertEquals(expectedDate, DateUtil.web2RdbDate("10/02/1985"));
  }

  @Test
  void wbs2WebDate() {
    String expectedDate = "10/02/1985";
    assertEquals(expectedDate, DateUtil.wbs2WebDate("1985-02-10"));
  }

  @Test
  void wbs2JsDate() {
    String expectedDate = "02/10/1985";
    assertEquals(expectedDate, DateUtil.wbs2JsDate("1985-02-10"));
  }

  @Test
  void getSystemDate() {
    assertNotNull(DateUtil.getSystemDate());
  }

  @Test
  void getCalendarDate() {
    Calendar expectedCalendar = Calendar.getInstance();
    expectedCalendar.setTime(DateUtil.asUtilDate(LocalDate.parse("10/02/1985", DATE_FORMAT_WEB)));
    assertEquals(expectedCalendar, DateUtil.getCalendarDate("10/02/1985"));
  }

  @Test
  void generic2DateTest() {
    String expectedDate1 = "10-Aug-1985";
    String expectedDate2 = "10/02/2020";
    String expectedDate3 = "03-28-2020";
    String expectedDate4 = "10/02/2020 23:23:00.123";
    String expectedDate5 = "10/02/2020";

    // Assert
    assertAll("Should assert all date times",
            () -> assertEquals(expectedDate1, DateUtil.generic2Date("10/08/1985", "dd/MM/yyyy", "dd-MMM-yyyy")),
            () -> assertEquals(expectedDate2, DateUtil.generic2Date("10-02-2020", "dd-MM-yyyy", "dd/MM/yyyy")),
            () -> assertEquals(expectedDate3, DateUtil.generic2Date("28-03-2020", "dd-MM-yyyy", "MM-dd-yyyy")),
            () -> assertEquals(expectedDate4, DateUtil.generic2Date("10-02-2020 23:23:00.123", "dd-MM-yyyy HH:mm:ss.SSS", "dd/MM/yyyy HH:mm:ss.SSS")),
            () -> assertEquals(expectedDate5, DateUtil.generic2Date("10/02/2020", "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy"))
    );
  }

  @Test
  void rdbDate2Web() {
    // Assert
    assertAll("Should assert all dates",
            () -> assertEquals("10/02/1985", DateUtil.rdbDate2Web("10-Feb-1985")),
            () -> assertEquals("10/02/1985", DateUtil.rdbDate2Web("10-FEB-1985")));
  }

  @Test
  void rdbDate2String() {
    LocalDate localDate = LocalDate.parse("27/10/1985", DATE_FORMAT_WEB);
    // Assert
    assertAll("Should assert all RDB Dates",
            () -> assertEquals("27-OCT-1985", DateUtil.dat2RDBDate(DateUtil.asUtilDate(localDate))),
            () -> assertNull(DateUtil.dat2RDBDate(null))
    );
  }

  @Test
  void autoDetectDateFormat() {
    Date expectedSqlTimestampDate = DateUtil.asUtilDate(LocalDateTime.parse("2022-02-14 14:22:27.123", TIMESTAMP_FORMAT_SQL_MS));
    Date expectedWbsDate = DateUtil.asUtilDate(LocalDate.parse("2022-02-14", DATE_FORMAT_WBS));
    Date expectedWebTimestampMs = DateUtil.asUtilDate(LocalDateTime.parse("14/02/2022 14:22:27.123", TIMESTAMP_FORMAT_WEB_MS));


    // Assert
    assertAll("Should assert all dates",
            () -> assertEquals(expectedSqlTimestampDate, DateUtil.autoDetectDateFormat("2022-02-14 14:22:27.123")),
            () -> assertEquals(expectedWbsDate, DateUtil.autoDetectDateFormat("2022-02-14")),
            () -> assertEquals(expectedWebTimestampMs, DateUtil.autoDetectDateFormat("14/02/2022 14:22:27.123")));
  }

  @Test
  void testAsLocalTime() {
    LocalTime expectedLocalTime = LocalTime.parse("17:12:34", TIME_FORMAT_WEB);
    Date date = Time.valueOf(LocalTime.parse("17:12:34", TIME_FORMAT_WEB));
    assertEquals(expectedLocalTime, DateUtil.asLocalTime(date));
  }

  @Test
  void testAsLocalDateTime() {
    LocalDateTime expectedLocalDateTime = LocalDateTime.parse("14/02/2022 17:12:34", TIMESTAMP_FORMAT_WEB);
    Date date = Timestamp.valueOf(LocalDateTime.parse("14/02/2022 17:12:34", TIMESTAMP_FORMAT_WEB));
    assertEquals(expectedLocalDateTime, DateUtil.asLocalDateTime(date));
  }

  @Test
  void testAsInstant() {
    Date date = Timestamp.valueOf(LocalDateTime.parse("14/02/2022 17:12:34", TIMESTAMP_FORMAT_WEB));
    Instant expectedInstant = date.toInstant();
    // Assert
    assertAll("Should assert all instants",
            () -> assertNull(DateUtil.asInstant(null)),
            () -> assertEquals(expectedInstant, DateUtil.asInstant(date))
    );
  }

  @Test
  void testAsZonedDateTime() {
    Date date = Timestamp.valueOf(LocalDateTime.parse("14/02/2022 17:12:34", TIMESTAMP_FORMAT_WEB));
    ZonedDateTime expectedZonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
    // Assert
    assertAll("Should assert all Zoned DateTimes",
            () -> assertNull(DateUtil.asZonedDateTime(null)),
            () -> assertEquals(expectedZonedDateTime, DateUtil.asZonedDateTime(date))
    );
  }

  @Test
  void asUtilDate() {
    Date expectedDate = new Date();
    LocalDate expectedLocalDate = LocalDate.now();
    LocalDateTime expectedLocalDateTime = Instant.ofEpochMilli(expectedDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    ZonedDateTime zonedDateTime = expectedLocalDateTime.atZone(ZoneId.systemDefault());
    Instant instant = expectedDate.toInstant();
    java.sql.Date sqlDate = new java.sql.Date(expectedDate.getTime());
    java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(expectedDate.getTime());

    // Assert
    assertAll("Should assert all dates",
            () -> assertNull(DateUtil.asUtilDate(null)),
            () -> assertEquals(java.sql.Date.valueOf(expectedLocalDate), DateUtil.asUtilDate(expectedLocalDate)),
            () -> assertEquals(expectedDate, DateUtil.asUtilDate(sqlDate)),
            () -> assertEquals(expectedDate, DateUtil.asUtilDate(sqlTimestamp)),
            () -> assertEquals(expectedDate, DateUtil.asUtilDate(expectedDate)),
            () -> assertEquals(expectedDate, DateUtil.asUtilDate(zonedDateTime)),
            () -> assertEquals(expectedDate, DateUtil.asUtilDate(instant)),
            () -> assertThrows(UnsupportedOperationException.class, () -> DateUtil.asUtilDate("10/02/1985"))
    );

  }
}