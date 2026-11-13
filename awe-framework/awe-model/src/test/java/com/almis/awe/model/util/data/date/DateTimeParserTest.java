package com.almis.awe.model.util.data.date;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeParserTest {

  private static final LocalDate DATE = LocalDate.of(1978, 10, 23);
  private static final LocalDateTime DATE_TIME = LocalDateTime.of(1978, 10, 23, 20, 10, 12, 345_000_000);

  @Test
  void parsesDates() {
    assertThat(DateTimeParser.parseWebDate("23/10/1978")).isEqualTo(DATE);
    assertThat(DateTimeParser.parseWebDateIgnoringTime("23/10/1978 20:10:12")).isEqualTo(DATE);
    assertThat(DateTimeParser.parseSqlDate("1978-10-23")).isEqualTo(DATE);
    assertThat(DateTimeParser.parseWbsDate("1978-10-23")).isEqualTo(DATE);
    assertThat(DateTimeParser.parseRdbDate("23-OCT-1978")).isEqualTo(DATE);
    assertThat(DateTimeParser.parseRdbDate("23-oct-1978")).isEqualTo(DATE);
  }

  @Test
  void parsesTimesAndTimestamps() {
    assertThat(DateTimeParser.parseWebTime("20:10:12")).isEqualTo(LocalTime.of(20, 10, 12));
    assertThat(DateTimeParser.parseWebTimestamp("23/10/1978 20:10:12.345")).isEqualTo(DATE_TIME);
    assertThat(DateTimeParser.parseWebTimestamp("23/10/1978 20:10:12")).isEqualTo(DATE_TIME.withNano(0));
    assertThat(DateTimeParser.parseWebTimestampWithMs("23/10/1978 20:10:12.345")).isEqualTo(DATE_TIME);
    assertThat(DateTimeParser.parseWebDateAndTime("23/10/1978", "20:10:12.345")).isEqualTo(DATE_TIME);
    assertThat(DateTimeParser.parseSqlTimestamp("1978-10-23 20:10:12.345")).isEqualTo(DATE_TIME);
    assertThat(DateTimeParser.parseJsonDate("1978-10-23@20:10:12.345-0300")).isEqualTo(DATE_TIME);
  }

  @Test
  void rejectsTextInAnotherFormat() {
    assertThatThrownBy(() -> DateTimeParser.parseWebDate("1978-10-23")).isInstanceOf(DateTimeParseException.class);
    assertThatThrownBy(() -> DateTimeParser.parseWebTimestampWithMs("23/10/1978 20:10:12"))
      .isInstanceOf(DateTimeParseException.class);
    assertThatThrownBy(() -> DateTimeParser.parseWebTime(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void parseAnyFormatDetectsTheFormat() {
    assertThat(DateTimeParser.parseAnyFormat("1978-10-23@20:10:12.345+0100")).contains(DATE_TIME);
    assertThat(DateTimeParser.parseAnyFormat("1978-10-23 20:10:12.345")).contains(DATE_TIME);
    assertThat(DateTimeParser.parseAnyFormat("23/10/1978")).contains(DATE.atStartOfDay());
    assertThat(DateTimeParser.parseAnyFormat("1978-10-23")).contains(DATE.atStartOfDay());
    assertThat(DateTimeParser.parseAnyFormat("23/10/1978 20:10:12.345")).contains(DATE_TIME);
    assertThat(DateTimeParser.parseAnyFormat("bad")).isEmpty();
  }
}
