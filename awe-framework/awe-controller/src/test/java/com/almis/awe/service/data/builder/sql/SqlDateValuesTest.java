package com.almis.awe.service.data.builder.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlDateValues}
 */
class SqlDateValuesTest {

  @Test
  @DisplayName("The current time is read from the clock in its zone")
  void currentTimeIsReadFromClock() {
    Clock clock = Clock.fixed(Instant.parse("2026-10-15T08:05:09Z"), ZoneId.of("Europe/Madrid"));

    assertThat(SqlDateValues.currentTime(clock)).isEqualTo("10:05:09");
  }

  @Test
  @DisplayName("A web timestamp is a java.sql.Timestamp")
  void webTimestampIsSqlTimestamp() {
    Timestamp timestamp = SqlDateValues.timestamp("15/10/2026 10:20:30");

    assertThat(timestamp).isEqualTo(Timestamp.valueOf("2026-10-15 10:20:30"));
    assertThat(timestamp.getClass()).isEqualTo(Timestamp.class);
  }

  @Test
  @DisplayName("A null or invalid timestamp is null")
  void nullOrInvalidTimestampIsNull() {
    assertThat(SqlDateValues.timestamp(null)).isNull();
    assertThat(SqlDateValues.timestamp("2026-10-15")).isNull();
  }
}
