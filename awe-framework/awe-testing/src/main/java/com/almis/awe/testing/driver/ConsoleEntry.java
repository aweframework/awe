package com.almis.awe.testing.driver;

import java.time.Instant;

/**
 * Entry of the browser console
 *
 * @param timestamp Moment of the entry, in milliseconds since the epoch
 * @param level     Level name (SEVERE, WARNING, INFO...)
 * @param message   Message
 */
public record ConsoleEntry(long timestamp, String level, String message) {

  private static final String SEVERE = "SEVERE";

  /**
   * Check whether it is an error
   *
   * @return true if the level is SEVERE
   */
  public boolean isSevere() {
    return SEVERE.equals(level);
  }

  @Override
  public String toString() {
    return String.format("[%s] [%s] %s", Instant.ofEpochMilli(timestamp), level, message);
  }
}
