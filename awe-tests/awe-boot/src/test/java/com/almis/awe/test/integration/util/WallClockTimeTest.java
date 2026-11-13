package com.almis.awe.test.integration.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wall-clock values returned by the server, read back as instants across daylight saving time transitions
 */
@Tag("integration")
class WallClockTimeTest {

  private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
  private static final DateTimeFormatter WEB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter WEB_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter WEB_TIMESTAMP_MS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

  @Test
  void parsesTheAfternoonAndNoonAsTwentyFourHourTimes() {
    assertThat(WallClockTime.parse("25/10/2026", "12:30:00")).isEqualTo(LocalDateTime.of(2026, 10, 25, 12, 30));
    assertThat(WallClockTime.parse("25/10/2026", "14:05:09")).isEqualTo(LocalDateTime.of(2026, 10, 25, 14, 5, 9));
  }

  @Test
  void parsesATimestampWithMilliseconds() {
    assertThat(WallClockTime.parseTimestamp("25/10/2026 14:05:09.123"))
      .isEqualTo(LocalDateTime.of(2026, 10, 25, 14, 5, 9, 123_000_000));
  }

  @Test
  void readsTheRepeatedHourAsItsSummerTimePass() {
    // 02:12 happens twice on 2026-10-25 in Madrid: first at 00:12Z (CEST), then at 01:12Z (CET)
    Instant earliest = WallClockTime.earliestInstant(LocalDateTime.of(2026, 10, 25, 2, 12), MADRID);

    assertThat(earliest).isEqualTo(Instant.parse("2026-10-25T00:12:00Z"));
  }

  @Test
  void readsTheSkippedHourAsItsEarliestInterpretation() {
    // 02:30 never happens on 2026-03-29 in Madrid; as CEST it would be 00:30Z, as CET 01:30Z
    Instant earliest = WallClockTime.earliestInstant(LocalDateTime.of(2026, 3, 29, 2, 30), MADRID);

    assertThat(earliest).isEqualTo(Instant.parse("2026-03-29T00:30:00Z"));
  }

  @Test
  void readsAnUnambiguousTimeWithItsOnlyOffset() {
    Instant earliest = WallClockTime.earliestInstant(LocalDateTime.of(2026, 7, 1, 12, 0), MADRID);

    assertThat(earliest).isEqualTo(Instant.parse("2026-07-01T10:00:00Z"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-03-29T01:00:00Z", "2026-10-25T01:00:00Z"})
  void neverReadsTheServerTimeAsLaterThanNowAroundATransition(String transition) {
    Instant start = Instant.parse(transition).minus(Duration.ofHours(3));
    for (Instant now = start; now.isBefore(start.plus(Duration.ofHours(6))); now = now.plusSeconds(37)) {
      LocalDateTime wallClock = LocalDateTime.ofInstant(now, MADRID);
      String date = WEB_DATE.format(wallClock);
      String time = WEB_TIME.format(wallClock);
      String timestamp = WEB_TIMESTAMP_MS.format(wallClock);

      Instant parsedDate = WallClockTime.earliestInstant(WallClockTime.parse(date, time), MADRID);
      Instant parsedTimestamp = WallClockTime.earliestInstant(WallClockTime.parseTimestamp(timestamp), MADRID);

      assertThat(parsedDate).as("date and time at %s", now)
        .isBetween(now.truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(1)), now);
      assertThat(parsedTimestamp).as("timestamp at %s", now)
        .isBetween(now.truncatedTo(ChronoUnit.MILLIS).minus(Duration.ofHours(1)), now);
    }
  }
}
