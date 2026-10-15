package com.almis.awe.model.util.data.date;

import com.almis.awe.model.component.AweElements;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

/**
 * Describes an elapsed time in the largest relevant unit ("3 days", "2 months"), using the AWE locales
 * {@code YEAR(S)}, {@code MONTH(S)}, {@code WEEK(S)}, {@code DAYS}, {@code HOURS}, {@code MINUTES}, {@code SECONDS},
 * {@code MILLISECONDS} and {@code TIME_AGO}.
 */
public final class ElapsedTimeFormatter {

  private ElapsedTimeFormatter() {
  }

  /**
   * Describes an elapsed time
   *
   * @param elapsed  Elapsed time
   * @param elements AWE elements (locales and language)
   * @return Localized description
   */
  public static String format(Duration elapsed, AweElements elements) {
    long milliseconds = elapsed.toMillis();
    // Fields of the elapsed time read as a date after the epoch (UTC)
    LocalDateTime fields = LocalDateTime.ofEpochSecond(Math.floorDiv(milliseconds, 1000L),
      (int) Math.floorMod(milliseconds, 1000L) * 1_000_000, ZoneOffset.UTC);
    int years = fields.getYear() - 1970;
    int months = fields.getMonthValue() - 1;
    int days = fields.getDayOfMonth() - 1;
    int hours = fields.get(ChronoField.HOUR_OF_AMPM);

    if (years > 1) {
      return locale(elements, "YEARS", years);
    } else if (years > 0) {
      return locale(elements, "YEAR", 1);
    } else if (months > 1) {
      return locale(elements, "MONTHS", months);
    } else if (months > 0) {
      return locale(elements, "MONTH", 1);
    } else if (days > 14) {
      return locale(elements, "WEEKS", Math.floorDiv(days, 7));
    } else if (days > 7) {
      return locale(elements, "WEEK", 1);
    } else if (days > 0) {
      return locale(elements, "DAYS", days);
    } else if (hours > 0) {
      return locale(elements, "HOURS", hours);
    } else if (fields.getMinute() > 0) {
      return locale(elements, "MINUTES", fields.getMinute());
    } else if (fields.getSecond() > 0) {
      return locale(elements, "SECONDS", fields.getSecond());
    }
    return locale(elements, "MILLISECONDS", milliseconds);
  }

  /**
   * Describes the time elapsed since an instant ("3 days ago")
   *
   * @param instant  Past instant
   * @param elements AWE elements (locales and language)
   * @return Localized description
   */
  public static String since(Instant instant, AweElements elements) {
    return since(instant, Clock.systemUTC(), elements);
  }

  /**
   * Describes the time elapsed since an instant ("3 days ago"), measured with the given clock
   *
   * @param instant  Past instant
   * @param clock    Clock that provides the current instant
   * @param elements AWE elements (locales and language)
   * @return Localized description
   */
  public static String since(Instant instant, Clock clock, AweElements elements) {
    return locale(elements, "TIME_AGO", format(Duration.between(instant, clock.instant()), elements));
  }

  private static String locale(AweElements elements, String identifier, Object value) {
    return elements.getLocaleWithLanguage(identifier, elements.getLanguage(), value);
  }
}
