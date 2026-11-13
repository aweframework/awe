package com.almis.awe.service.data.builder.sql;

import com.almis.awe.model.util.data.date.DateTimeFormatting;
import com.almis.awe.model.util.data.date.DateTimeParser;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalTime;

/**
 * Converts date and time variable values into the values {@code SQLBuilder} binds. Used by {@code SQLBuilder}; it is
 * not an extension point.
 */
@Slf4j
public final class SqlDateValues {

  private SqlDateValues() {
  }

  /**
   * Retrieve the current time as the value of a {@code SYSTEM_TIME} variable
   *
   * @param clock Clock
   * @return Current time as web time ({@code HH:mm:ss})
   */
  public static String currentTime(Clock clock) {
    return DateTimeFormatting.formatWebTime(LocalTime.now(clock));
  }

  /**
   * Parse a web timestamp into the SQL timestamp bound for a {@code TIMESTAMP} variable
   *
   * @param timestamp Web timestamp ({@code dd/MM/yyyy HH:mm:ss})
   * @return SQL timestamp, or null when the value is null or is not a web timestamp
   */
  public static Timestamp timestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }
    try {
      return Timestamp.valueOf(DateTimeParser.parseWebTimestamp(timestamp));
    } catch (Exception exc) {
      log.error("Error parsing WEB TIMESTAMP to date -{}-", timestamp, exc);
      return null;
    }
  }
}
