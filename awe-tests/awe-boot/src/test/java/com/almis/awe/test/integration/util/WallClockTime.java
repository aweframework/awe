package com.almis.awe.test.integration.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;

/**
 * Reads back the wall-clock date and time values the server returns ({@code dd/MM/yyyy}, {@code HH:mm:ss} and
 * {@code dd/MM/yyyy HH:mm:ss.SSS}). A wall-clock value carries no offset, so around a daylight saving time transition
 * it can stand for two instants; tests that check the server time is not in the future must read it as the earliest one.
 */
public final class WallClockTime {

  private static final DateTimeFormatter WEB_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter WEB_TIMESTAMP_MS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

  private WallClockTime() {
  }

  /**
   * Parse a web date and a web time
   *
   * @param date Web date ({@code dd/MM/yyyy})
   * @param time Web time ({@code HH:mm:ss})
   * @return Wall-clock date and time
   */
  public static LocalDateTime parse(String date, String time) {
    return LocalDateTime.parse(date + " " + time, WEB_DATE_TIME);
  }

  /**
   * Parse a web timestamp with milliseconds
   *
   * @param timestamp Web timestamp ({@code dd/MM/yyyy HH:mm:ss.SSS})
   * @return Wall-clock date and time
   */
  public static LocalDateTime parseTimestamp(String timestamp) {
    return LocalDateTime.parse(timestamp, WEB_TIMESTAMP_MS);
  }

  /**
   * Earliest instant a wall-clock date and time can stand for in a zone: in the repeated hour of a transition the
   * summer time pass, and in the skipped hour the reading with the larger offset
   *
   * @param wallClock Wall-clock date and time
   * @param zone      Zone the wall clock was read in
   * @return Earliest instant
   */
  public static Instant earliestInstant(LocalDateTime wallClock, ZoneId zone) {
    ZoneRules rules = zone.getRules();
    ZoneOffsetTransition transition = rules.getTransition(wallClock);
    ZoneOffset offset = transition == null ? rules.getOffset(wallClock) : largest(transition);
    return wallClock.toInstant(offset);
  }

  private static ZoneOffset largest(ZoneOffsetTransition transition) {
    ZoneOffset before = transition.getOffsetBefore();
    ZoneOffset after = transition.getOffsetAfter();
    return before.getTotalSeconds() > after.getTotalSeconds() ? before : after;
  }
}
