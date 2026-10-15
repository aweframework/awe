package com.almis.awe.model.util.data.date;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeFormattingTest {

  private static final LocalDateTime DATE_TIME = LocalDateTime.of(1978, 10, 23, 20, 10, 12, 345_000_000);
  private static final LocalDate DATE = DATE_TIME.toLocalDate();

  @Test
  void formatsDates() {
    assertThat(DateTimeFormatting.formatWebDate(DATE)).isEqualTo("23/10/1978");
    assertThat(DateTimeFormatting.formatJsDate(DATE)).isEqualTo("10/23/1978");
    assertThat(DateTimeFormatting.formatJsDate(DATE_TIME)).isEqualTo("10/23/1978");
    assertThat(DateTimeFormatting.formatSqlDate(DATE)).isEqualTo("1978-10-23");
    assertThat(DateTimeFormatting.formatWbsDate(DATE)).isEqualTo("1978-10-23");
    assertThat(DateTimeFormatting.formatRdbDate(DATE)).isEqualTo("23-OCT-1978");
  }

  @Test
  void formatsTimesAndTimestamps() {
    assertThat(DateTimeFormatting.formatWebTime(LocalTime.of(8, 9, 10))).isEqualTo("08:09:10");
    assertThat(DateTimeFormatting.formatWebTime(DATE_TIME)).isEqualTo("20:10:12");
    assertThat(DateTimeFormatting.formatWebDateTime(DATE_TIME)).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateTimeFormatting.formatWebTimestamp(DATE_TIME)).isEqualTo("23/10/1978 20:10:12.345");
    assertThat(DateTimeFormatting.formatWebTimestamp(DATE_TIME.withNano(0))).isEqualTo("23/10/1978 20:10:12");
    assertThat(DateTimeFormatting.formatWebTimestampWithMs(DATE_TIME.withNano(0))).isEqualTo("23/10/1978 20:10:12.000");
    assertThat(DateTimeFormatting.formatJsTimestamp(DATE_TIME)).isEqualTo("10/23/1978 20:10:12");
    assertThat(DateTimeFormatting.formatSqlTimestamp(DATE_TIME)).isEqualTo("1978-10-23 20:10:12.345");
    assertThat(DateTimeFormatting.formatJsonDate(DATE_TIME.atZone(ZoneOffset.ofHours(-3))))
      .isEqualTo("1978-10-23@20:10:12.345-0300");
  }

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> DateTimeFormatting.formatWebDate(null)).isInstanceOf(NullPointerException.class);
  }
}
