package com.almis.awe.model.util.data.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Boundary between the legacy {@code java.util.Date} / {@code java.sql} types and {@code java.time}. This is the only
 * place where AWE date code handles {@code java.util.Date}; everything else works with {@code java.time}.
 */
public final class LegacyDateConverter {

  private LegacyDateConverter() {
  }

  /**
   * Calls {@link #asLocalDate(Date, ZoneId)} with the system default time zone
   *
   * @param date Date (not null)
   * @return Local date
   */
  public static LocalDate asLocalDate(Date date) {
    return asLocalDate(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link LocalDate} from a {@code java.util.Date} or one of its subclasses
   *
   * @param date Date (not null)
   * @param zone Time zone, ignored for {@code java.sql.Date}
   * @return Local date
   */
  public static LocalDate asLocalDate(Date date, ZoneId zone) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
  }

  /**
   * Calls {@link #asLocalTime(Date, ZoneId)} with the system default time zone
   *
   * @param date Date
   * @return Local time, or null when the date is null
   */
  public static LocalTime asLocalTime(Date date) {
    return asLocalTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link LocalTime} from a {@code java.util.Date} or one of its subclasses
   *
   * @param date Date
   * @param zone Time zone, ignored for {@code java.sql.Time}
   * @return Local time, or null when the date is null
   */
  public static LocalTime asLocalTime(Date date, ZoneId zone) {
    if (date == null) {
      return null;
    } else if (date instanceof java.sql.Time time) {
      return time.toLocalTime();
    }
    return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalTime();
  }

  /**
   * Calls {@link #asLocalDateTime(Date, ZoneId)} with the system default time zone
   *
   * @param date Date (not null)
   * @return Local date time
   */
  public static LocalDateTime asLocalDateTime(Date date) {
    return asLocalDateTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link LocalDateTime} from a {@code java.util.Date} or one of its subclasses
   *
   * @param date Date (not null)
   * @param zone Time zone, ignored for {@code java.sql.Timestamp}
   * @return Local date time
   */
  public static LocalDateTime asLocalDateTime(Date date, ZoneId zone) {
    if (date instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime();
    }
    return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDateTime();
  }

  /**
   * Calls {@link #asUtilDate(Object, ZoneId)} with the system default time zone
   *
   * @param date Date object
   * @return {@code java.util.Date}
   */
  public static Date asUtilDate(Object date) {
    return asUtilDate(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link Date} from various date objects. Is null-safe. Supports {@code java.util.Date},
   * {@code java.sql.Date}, {@code java.sql.Timestamp}, {@link LocalDate}, {@link LocalDateTime},
   * {@link ZonedDateTime} and {@link Instant}.
   *
   * @param date Date object
   * @param zone Time zone, used only for {@link LocalDate} and {@link LocalDateTime}
   * @return {@code java.util.Date} (exactly this class, not a subclass such as {@code java.sql.Date})
   */
  public static Date asUtilDate(Object date, ZoneId zone) {
    if (date == null) {
      return null;
    }
    if (date instanceof java.sql.Date || date instanceof java.sql.Timestamp) {
      return new Date(((Date) date).getTime());
    }
    if (date instanceof Date utilDate) {
      return utilDate;
    }
    if (date instanceof LocalDate localDate) {
      return Date.from(localDate.atStartOfDay(zone).toInstant());
    }
    if (date instanceof LocalDateTime localDateTime) {
      return Date.from(localDateTime.atZone(zone).toInstant());
    }
    if (date instanceof ZonedDateTime zonedDateTime) {
      return Date.from(zonedDateTime.toInstant());
    }
    if (date instanceof Instant instant) {
      return Date.from(instant);
    }
    throw new UnsupportedOperationException("Don't know hot to convert " + date.getClass().getName() + " to java.util.Date");
  }

  /**
   * Creates an {@link Instant} from a {@code java.util.Date} or one of its subclasses. Null-safe.
   *
   * @param date Date
   * @return Instant, or null when the date is null
   */
  public static Instant asInstant(Date date) {
    return date == null ? null : Instant.ofEpochMilli(date.getTime());
  }

  /**
   * Calls {@link #asZonedDateTime(Date, ZoneId)} with the system default time zone
   *
   * @param date Date
   * @return Zoned date time, or null when the date is null
   */
  public static ZonedDateTime asZonedDateTime(Date date) {
    return asZonedDateTime(date, ZoneId.systemDefault());
  }

  /**
   * Creates a {@link ZonedDateTime} from a {@code java.util.Date} or one of its subclasses. Null-safe.
   *
   * @param date Date
   * @param zone Time zone
   * @return Zoned date time, or null when the date is null
   */
  public static ZonedDateTime asZonedDateTime(Date date, ZoneId zone) {
    return date == null ? null : asInstant(date).atZone(zone);
  }
}
