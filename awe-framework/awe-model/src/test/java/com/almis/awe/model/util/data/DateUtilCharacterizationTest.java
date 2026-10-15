package com.almis.awe.model.util.data;

import com.almis.awe.model.component.AweElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for {@link DateUtil}: they pin the observable behavior (values, runtime classes, null and
 * error handling) of every public method so that the class can be split without changing it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DateUtilCharacterizationTest {

  private static final LocalDateTime DATE_TIME = LocalDateTime.of(1978, 10, 23, 20, 10, 12, 345_000_000);
  private static final LocalDate DATE = DATE_TIME.toLocalDate();
  private static final Timestamp TIMESTAMP = Timestamp.valueOf(DATE_TIME);
  private static final Date UTIL_DATE_TIME = Date.from(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
  private static final Date UTIL_DATE = Date.from(DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

  @Mock
  private AweElements elements;

  @BeforeEach
  void setUp() {
    when(elements.getLanguage()).thenReturn("en-GB");
    doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      String tokens = Arrays.stream(arguments, 2, arguments.length).map(String::valueOf).collect(Collectors.joining(","));
      return arguments[0] + "[" + tokens + "]";
    }).when(elements).getLocaleWithLanguage(anyString(), anyString(), any(Object[].class));
  }

  // Web text to java.sql / java.util

  @Test
  void web2SqlDateReturnsSqlDateAndThrowsOnInvalidInput() {
    assertThat(DateUtil.web2SqlDate("23/10/1978")).isExactlyInstanceOf(java.sql.Date.class).hasToString("1978-10-23");
    assertThatThrownBy(() -> DateUtil.web2SqlDate("1978-10-23")).isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void web2SqlTimeReturnsSqlTime() {
    assertThat(DateUtil.web2SqlTime("20:10:12")).isExactlyInstanceOf(Time.class).hasToString("20:10:12");
    assertThatThrownBy(() -> DateUtil.web2SqlTime("20:10")).isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void webDate2SqlTimestampReturnsStartOfDay() {
    assertThat(DateUtil.webDate2SqlTimestamp("23/10/1978")).isExactlyInstanceOf(Timestamp.class)
      .hasToString("1978-10-23 00:00:00.0");
  }

  @Test
  void web2SqlTimestampAcceptsOptionalMilliseconds() {
    assertThat(DateUtil.web2SqlTimestamp("23/10/1978 20:10:12")).hasToString("1978-10-23 20:10:12.0");
    assertThat(DateUtil.web2SqlTimestamp("23/10/1978 20:10:12.345")).isEqualTo(TIMESTAMP);
  }

  @Test
  void web2DateReturnsExactUtilDateAtStartOfDay() {
    assertThat(DateUtil.web2Date("23/10/1978")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.web2Date("23/10/1978 20:10:12")).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.web2Date(null)).isNull();
    assertThat(DateUtil.web2Date("")).isNull();
    assertThat(DateUtil.web2Date("1978-10-23")).isNull();
  }

  @Test
  void web2TimeReturnsSqlTimeOrNull() {
    assertThat(DateUtil.web2Time("20:10:12")).isExactlyInstanceOf(Time.class).hasToString("20:10:12");
    assertThat(DateUtil.web2Time(null)).isNull();
    assertThat(DateUtil.web2Time("20:10")).isNull();
  }

  @Test
  void web2TimestampReturnsSqlTimestampOrNull() {
    assertThat(DateUtil.web2Timestamp("23/10/1978 20:10:12.345")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.web2Timestamp("23/10/1978 20:10:12")).hasToString("1978-10-23 20:10:12.0");
    assertThat(DateUtil.web2Timestamp(null)).isNull();
    assertThat(DateUtil.web2Timestamp("23/10/1978")).isNull();
  }

  @Test
  void web2TimestampWithMsRequiresMilliseconds() {
    assertThat(DateUtil.web2TimestampWithMs("23/10/1978 20:10:12.345")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.web2TimestampWithMs("23/10/1978 20:10:12")).isNull();
    assertThat(DateUtil.web2TimestampWithMs(null)).isNull();
  }

  // java.util / java.sql to java.util / java.sql

  @Test
  void dat2SqlDateKeepsMilliseconds() {
    java.sql.Date sqlDate = DateUtil.dat2SqlDate(UTIL_DATE_TIME);
    assertThat(sqlDate).isExactlyInstanceOf(java.sql.Date.class);
    assertThat(sqlDate.getTime()).isEqualTo(UTIL_DATE_TIME.getTime());
  }

  @Test
  void sqlTimestamp2DateAndSqlDate2DateReturnExactUtilDate() {
    assertThat(DateUtil.sqlTimestamp2Date(TIMESTAMP)).isExactlyInstanceOf(Date.class)
      .isEqualTo(new Date(TIMESTAMP.getTime()));
    assertThat(DateUtil.sqlTimestamp2Date(null)).isNull();
    java.sql.Date sqlDate = java.sql.Date.valueOf(DATE);
    assertThat(DateUtil.sqlDate2Date(sqlDate)).isExactlyInstanceOf(Date.class)
      .isEqualTo(new Date(sqlDate.getTime()));
    assertThat(DateUtil.sqlDate2Date(null)).isNull();
  }

  @Test
  void dat2SqlTimestampTruncatesToMilliseconds() {
    Timestamp nanos = Timestamp.valueOf(DATE_TIME.plusNanos(999));
    assertThat(DateUtil.dat2SqlTimestamp(nanos)).isExactlyInstanceOf(Timestamp.class).isEqualTo(TIMESTAMP);
    assertThat(DateUtil.dat2SqlTimestamp(UTIL_DATE_TIME)).isEqualTo(TIMESTAMP);
  }

  // Date and java.time to text

  @Test
  void dateToTextFormats() {
    assertThat(DateUtil.dat2SqlDateString(TIMESTAMP)).isEqualTo("1978-10-23");
    assertThat(DateUtil.dat2SqlTimeString(UTIL_DATE_TIME)).isEqualTo("1978-10-23 20:10:12.345");
    assertThat(DateUtil.dat2WebTimestamp(UTIL_DATE_TIME)).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateUtil.dat2WebTimestampMs(UTIL_DATE_TIME)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.dat2WebTime(UTIL_DATE_TIME)).isEqualTo("20:10:12");
    assertThat(DateUtil.dat2WebTime(Time.valueOf("08:09:10"))).isEqualTo("08:09:10");
    assertThat(DateUtil.dat2WebDate(UTIL_DATE_TIME)).isEqualTo("23/10/1978");
    assertThat(DateUtil.dat2WebDate(java.sql.Date.valueOf(DATE))).isEqualTo("23/10/1978");
    assertThat(DateUtil.dat2DateMs(UTIL_DATE_TIME)).isEqualTo(String.valueOf(UTIL_DATE_TIME.getTime()));
    assertThat(DateUtil.dat2JsDate(UTIL_DATE_TIME)).isEqualTo("10/23/1978");
    assertThat(DateUtil.dat2JsTimestamp(UTIL_DATE_TIME)).isEqualTo("10/23/1978 20:10:12");
    assertThat(DateUtil.dat2WbsDate(UTIL_DATE_TIME)).isEqualTo("1978-10-23");
    assertThat(DateUtil.dat2RDBDate(UTIL_DATE_TIME)).isEqualTo("23-OCT-1978");
    assertThat(DateUtil.dat2RDBDate(null)).isNull();
    assertThat(DateUtil.localDatetime2WebTimestamp(DATE_TIME)).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateUtil.localDate2WebDate(DATE)).isEqualTo("23/10/1978");
  }

  @Test
  void dateToTextNullInputThrows() {
    assertThatThrownBy(() -> DateUtil.dat2WebDate(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.dat2WbsDate(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void sqlTypesToText() {
    java.sql.Date sqlDate = java.sql.Date.valueOf(DATE);
    Time sqlTime = Time.valueOf("20:10:12");
    Timestamp noMillis = Timestamp.valueOf(DATE_TIME.withNano(0));
    assertThat(DateUtil.sqlDat2WebDate(sqlDate)).isEqualTo("23/10/1978");
    assertThat(DateUtil.sqlDat2WebTime(sqlTime)).isEqualTo("20:10:12");
    assertThat(DateUtil.sqlDat2WebTimestamp(TIMESTAMP)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.sqlDat2WebTimestamp(noMillis)).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateUtil.sqlDat2JsDate(sqlDate)).isEqualTo("10/23/1978");
    assertThat(DateUtil.sqlDat2JsTimestamp(TIMESTAMP)).isEqualTo("10/23/1978 20:10:12");
    assertThat(DateUtil.sqlDat2WebTimestampWithMs(TIMESTAMP)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.sqlDat2WebTimestampWithMs(noMillis)).isEqualTo("23/10/1978 20:10:12.000");
  }

  @Test
  void sqlTypesToTextNullInputThrows() {
    assertThatThrownBy(() -> DateUtil.sqlDat2WebDate(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.sqlDat2WebTime(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.sqlDat2WebTimestamp(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.sqlDat2JsDate(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.sqlDat2JsTimestamp(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> DateUtil.sqlDat2WebTimestampWithMs(null)).isInstanceOf(NullPointerException.class);
  }

  // Text to java.util

  @Test
  void textToJavaDate() {
    assertThat(DateUtil.sql2JavaDate("1978-10-23 20:10:12.345")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThatThrownBy(() -> DateUtil.sql2JavaDate("1978-10-23")).isInstanceOf(DateTimeParseException.class);
    assertThat(DateUtil.sqlDate2JavaDate("1978-10-23")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.wbs2JavaDate("1978-10-23")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.rdb2Date("23-oct-1978")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThatThrownBy(() -> DateUtil.rdb2Date("23/10/1978")).isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void jsonDateParsesIgnoringTheOffsetAndFormatsWithTheSystemOffset() {
    assertThat(DateUtil.jsonDate("1978-10-23@20:10:12.345+0500")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.jsonDate(UTIL_DATE_TIME)).startsWith("1978-10-23@20:10:12.345").matches(".*[+-]\\d{4}");
    assertThat(DateUtil.jsonDate(DateUtil.jsonDate(UTIL_DATE_TIME))).isEqualTo(TIMESTAMP);
  }

  @Test
  void dateWithTime() {
    assertThat(DateUtil.getDateWithTimeFromCriteria("23/10/1978", "20:10:12")).isExactlyInstanceOf(Timestamp.class)
      .hasToString("1978-10-23 20:10:12.0");
    assertThat(DateUtil.addTimeToDate(UTIL_DATE, "20:10:12")).isExactlyInstanceOf(Timestamp.class)
      .hasToString("1978-10-23 20:10:12.0");
    assertThat(DateUtil.addTimeToDate(UTIL_DATE, "")).isSameAs(UTIL_DATE);
    assertThat(DateUtil.addTimeToDate(UTIL_DATE, null)).isSameAs(UTIL_DATE);
  }

  // Text to text

  @Test
  void sqlTextToOtherText() {
    String sql = "1978-10-23 20:10:12.345";
    assertThat(DateUtil.sql2WebDate(sql)).isEqualTo("23/10/1978");
    assertThat(DateUtil.sql2WebTime(sql)).isEqualTo("20:10:12");
    assertThat(DateUtil.sql2WebTimestamp(sql)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.sql2WebTimestamp("1978-10-23 20:10:12.000")).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateUtil.sql2JsDate(sql)).isEqualTo("10/23/1978");
    assertThat(DateUtil.sql2JsTimestamp(sql)).isEqualTo("10/23/1978 20:10:12");
    assertThat(DateUtil.sql2DateMs(sql)).isEqualTo(String.valueOf(TIMESTAMP.getTime()));
  }

  @Test
  void sqlTextToOtherTextReturnsInputWhenInvalid() {
    assertThat(DateUtil.sql2WebDate("bad")).isEqualTo("bad");
    assertThat(DateUtil.sql2WebTime("bad")).isEqualTo("bad");
    assertThat(DateUtil.sql2WebTimestamp("bad")).isEqualTo("bad");
    assertThat(DateUtil.sql2JsDate("bad")).isEqualTo("bad");
    assertThat(DateUtil.sql2JsTimestamp("bad")).isEqualTo("bad");
    assertThatThrownBy(() -> DateUtil.sql2DateMs("bad")).isInstanceOf(DateTimeParseException.class);
  }

  @Test
  void webWbsAndRdbTextConversions() {
    assertThat(DateUtil.web2WbsDate("23/10/1978")).isEqualTo("1978-10-23");
    assertThat(DateUtil.web2WbsDate("bad")).isEqualTo("bad");
    assertThat(DateUtil.web2RdbDate("23/10/1978")).isEqualTo("23-OCT-1978");
    assertThat(DateUtil.web2RdbDate("bad")).isEqualTo("BAD");
    assertThat(DateUtil.wbs2WebDate("1978-10-23")).isEqualTo("23/10/1978");
    assertThat(DateUtil.wbs2WebDate("bad")).isEqualTo("bad");
    assertThat(DateUtil.wbs2JsDate("1978-10-23")).isEqualTo("10/23/1978");
    assertThat(DateUtil.wbs2JsDate("bad")).isEqualTo("bad");
    assertThat(DateUtil.rdbDate2Web("23-oct-1978")).isEqualTo("23/10/1978");
    assertThat(DateUtil.rdbDate2Web("bad")).isEqualTo("BAD");
  }

  @Test
  void generic2DateConvertsBetweenPatterns() {
    assertThat(DateUtil.generic2Date("23/10/1978", "dd/MM/yyyy", "yyyy-MM-dd")).isEqualTo("1978-10-23");
    assertThat(DateUtil.generic2Date("23/10/1978 20:10:12.345", "dd/MM/yyyy", "dd-MMM-yyyy HH:mm:ss.SSS"))
      .isEqualTo("23-Oct-1978 20:10:12.345");
    assertThat(DateUtil.generic2Date("23-Oct-1978", "dd-MMM-yyyy", "dd/MM/yyyy")).isEqualTo("23/10/1978");
    // parseCaseInsensitive() is appended after the pattern, so it does not apply to the month name
    assertThat(DateUtil.generic2Date("23-OCT-1978", "dd-MMM-yyyy", "dd/MM/yyyy")).isEqualTo("23-OCT-1978");
    assertThat(DateUtil.generic2Date("bad", "dd/MM/yyyy", "yyyy-MM-dd")).isEqualTo("bad");
  }

  // Validators and detection

  @Test
  void validators() {
    assertThat(DateUtil.isWebDate("23/10/1978")).isTrue();
    assertThat(DateUtil.isWebDate("23/10/1978 20:10:12")).isFalse();
    assertThat(DateUtil.isWbsDate("1978-10-23")).isTrue();
    assertThat(DateUtil.isSqlDate("1978-10-23 20:10:12.345")).isTrue();
    assertThat(DateUtil.isSqlDate("1978-10-23 20:10:12")).isFalse();
    assertThat(DateUtil.isWebTimestamp("23/10/1978 20:10:12")).isTrue();
    assertThat(DateUtil.isWebTimestamp("23/10/1978 20:10:12.345")).isTrue();
    assertThat(DateUtil.isWebTimestampWithMs("23/10/1978 20:10:12.345")).isTrue();
    assertThat(DateUtil.isWebTimestampWithMs("23/10/1978 20:10:12")).isFalse();
    assertThat(DateUtil.isJsonDate("1978-10-23@20:10:12.345+0100")).isTrue();
    assertThat(DateUtil.isJsonDate(null)).isFalse();
  }

  @Test
  void autoDetectDateFormatKeepsTheRuntimeClassOfEachParser() {
    assertThat(DateUtil.autoDetectDateFormat("1978-10-23@20:10:12.345+0100")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.autoDetectDateFormat("1978-10-23 20:10:12.345")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.autoDetectDateFormat("23/10/1978")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.autoDetectDateFormat("1978-10-23")).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.autoDetectDateFormat("23/10/1978 20:10:12.345")).isExactlyInstanceOf(Timestamp.class)
      .isEqualTo(TIMESTAMP);
    assertThat(DateUtil.autoDetectDateFormat("bad")).isNull();
  }

  @Test
  void systemAndCalendarDates() {
    assertThat(DateUtil.getSystemDate()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    Calendar calendar = DateUtil.getCalendarDate("23/10/1978");
    assertThat(calendar.getTime()).isEqualTo(UTIL_DATE);
    assertThatThrownBy(() -> DateUtil.getCalendarDate("bad")).isInstanceOf(NullPointerException.class);
  }

  // Bridges

  @Test
  void bridgesToJavaTime() {
    assertThat(DateUtil.asLocalDate(UTIL_DATE_TIME)).isEqualTo(DATE);
    assertThat(DateUtil.asLocalDate(java.sql.Date.valueOf(DATE))).isEqualTo(DATE);
    assertThatThrownBy(() -> DateUtil.asLocalDate(null)).isInstanceOf(NullPointerException.class);
    assertThat(DateUtil.asLocalTime(UTIL_DATE_TIME)).isEqualTo(DATE_TIME.toLocalTime());
    assertThat(DateUtil.asLocalTime(Time.valueOf("08:09:10"))).isEqualTo(LocalTime.of(8, 9, 10));
    assertThat(DateUtil.asLocalTime(null)).isNull();
    assertThat(DateUtil.asLocalDateTime(TIMESTAMP)).isEqualTo(DATE_TIME);
    assertThat(DateUtil.asLocalDateTime(UTIL_DATE_TIME)).isEqualTo(DATE_TIME);
    assertThatThrownBy(() -> DateUtil.asLocalDateTime(null)).isInstanceOf(NullPointerException.class);
    assertThat(DateUtil.asInstant(UTIL_DATE_TIME)).isEqualTo(UTIL_DATE_TIME.toInstant());
    assertThat(DateUtil.asInstant(null)).isNull();
    assertThat(DateUtil.asZonedDateTime(UTIL_DATE_TIME)).isEqualTo(DATE_TIME.atZone(ZoneId.systemDefault()));
    assertThat(DateUtil.asZonedDateTime(UTIL_DATE_TIME, ZoneId.of("UTC")))
      .isEqualTo(UTIL_DATE_TIME.toInstant().atZone(ZoneId.of("UTC")));
    assertThat(DateUtil.asZonedDateTime(null)).isNull();
  }

  @Test
  void bridgesToUtilDate() {
    assertThat(DateUtil.asUtilDate(TIMESTAMP)).isExactlyInstanceOf(Date.class).isEqualTo(new Date(TIMESTAMP.getTime()));
    assertThat(DateUtil.asUtilDate(java.sql.Date.valueOf(DATE))).isExactlyInstanceOf(Date.class).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.asUtilDate(UTIL_DATE)).isSameAs(UTIL_DATE);
    assertThat(DateUtil.asUtilDate(DATE)).isEqualTo(UTIL_DATE);
    assertThat(DateUtil.asUtilDate(DATE_TIME)).isEqualTo(UTIL_DATE_TIME);
    assertThat(DateUtil.asUtilDate(DATE_TIME.atZone(ZoneId.systemDefault()))).isEqualTo(UTIL_DATE_TIME);
    assertThat(DateUtil.asUtilDate(UTIL_DATE_TIME.toInstant())).isEqualTo(UTIL_DATE_TIME);
    assertThat(DateUtil.asUtilDate(null)).isNull();
    assertThatThrownBy(() -> DateUtil.asUtilDate("23/10/1978")).isInstanceOf(UnsupportedOperationException.class);
    assertThat(DateUtil.asUtilDate(DATE, ZoneId.of("UTC")))
      .isEqualTo(Date.from(DATE.atStartOfDay(ZoneId.of("UTC")).toInstant()));
  }

  // Elapsed time

  @Test
  void elapsedTimePicksTheLargestUnit() {
    assertThat(elapsed(Duration.ZERO)).isEqualTo("MILLISECONDS[0]");
    assertThat(elapsed(Duration.ofMillis(500))).isEqualTo("MILLISECONDS[500]");
    assertThat(elapsed(Duration.ofSeconds(5))).isEqualTo("SECONDS[5]");
    assertThat(elapsed(Duration.ofMinutes(3).plusSeconds(4))).isEqualTo("MINUTES[3]");
    assertThat(elapsed(Duration.ofHours(2).plusMinutes(5))).isEqualTo("HOURS[2]");
    assertThat(elapsed(Duration.ofDays(3).plusHours(2))).isEqualTo("DAYS[3]");
    assertThat(elapsed(Duration.ofDays(9))).isEqualTo("WEEK[1]");
    assertThat(elapsed(Duration.ofDays(20))).isEqualTo("WEEKS[2]");
    assertThat(elapsed(Duration.ofDays(40))).isEqualTo("MONTH[1]");
    assertThat(elapsed(Duration.ofDays(70))).isEqualTo("MONTHS[2]");
    assertThat(elapsed(Duration.ofDays(400))).isEqualTo("YEAR[1]");
    assertThat(elapsed(Duration.ofDays(800))).isEqualTo("YEARS[2]");
  }

  @Test
  void elapsedTimeReadsHoursOnATwentyFourHourClock() {
    assertThat(elapsed(Duration.ofHours(12))).isEqualTo("HOURS[12]");
    assertThat(elapsed(Duration.ofHours(13))).isEqualTo("HOURS[13]");
    assertThat(elapsed(Duration.ofHours(23).plusMinutes(59))).isEqualTo("HOURS[23]");
    assertThat(elapsed(Duration.ofHours(24))).isEqualTo("DAYS[1]");
  }

  @Test
  void dateSinceWrapsTheElapsedTime() {
    Date threeDaysAgo = Date.from(Instant.now().minus(Duration.ofDays(3).plusMinutes(30)));
    assertThat(DateUtil.dateSince(threeDaysAgo, elements)).isEqualTo("TIME_AGO[DAYS[3]]");
  }

  // Formatter constants

  @Test
  void formatterConstants() {
    assertThat(DateUtil.DATE_FORMAT_WEB.format(DATE)).isEqualTo("23/10/1978");
    assertThat(DateUtil.DATE_FORMAT_JS.format(DATE)).isEqualTo("10/23/1978");
    assertThat(DateUtil.DATE_FORMAT_SQL.format(DATE)).isEqualTo("1978-10-23");
    assertThat(DateUtil.DATE_FORMAT_WBS.format(DATE)).isEqualTo("1978-10-23");
    assertThat(DateUtil.DATE_FORMAT_RDB.format(DATE)).isEqualTo("23-Oct-1978");
    assertThat(DateUtil.TIMESTAMP_FORMAT_SQL_MS.format(DATE_TIME)).isEqualTo("1978-10-23 20:10:12.345");
    assertThat(DateUtil.TIMESTAMP_FORMAT_WEB.format(DATE_TIME)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.DATETIME_FORMAT_WEB.format(DATE_TIME)).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateUtil.TIMESTAMP_FORMAT_JS.format(DATE_TIME)).isEqualTo("10/23/1978 20:10:12");
    assertThat(DateUtil.TIMESTAMP_FORMAT_WEB_MS.format(DATE_TIME)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateUtil.TIME_FORMAT_WEB.format(DATE_TIME)).isEqualTo("20:10:12");
    assertThat(DateUtil.JSON_DATE.format(ZonedDateTime.of(DATE_TIME, ZoneId.of("UTC"))))
      .isEqualTo("1978-10-23@20:10:12.345+0000");
    assertThat(LocalDateTime.parse("23/10/1978", DateUtil.DATE_FORMAT_WEB_PARSER)).isEqualTo(DATE.atStartOfDay());
  }

  private String elapsed(Duration duration) {
    return DateUtil.elapsedTime(duration.toMillis(), elements);
  }
}
