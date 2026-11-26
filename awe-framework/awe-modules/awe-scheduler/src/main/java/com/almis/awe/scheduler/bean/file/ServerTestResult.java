package com.almis.awe.scheduler.bean.file;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Result of a connection test of a server, kept in the session of the user (never in the database): whether it
 * connected, how long it took, the reason of a failure and when it ended. The reason is the code of its locales (for
 * instance CONNECTION_ERROR: SCHEDULER_SERVER_TEST_CONNECTION_ERROR and SCREEN_TEXT_SCHEDULER_SERVER_TEST_REASON_...),
 * never the text of an exception
 *
 * @param ok       The connection and the authentication succeeded
 * @param millis   Duration of the test in milliseconds
 * @param reason   Code of the reason of a failure, null when it succeeded
 * @param testedAt When the test ended
 */
public record ServerTestResult(boolean ok, long millis, String reason, LocalDateTime testedAt) implements Serializable {

  /**
   * Successful test
   *
   * @param millis   Duration in milliseconds
   * @param testedAt When the test ended
   * @return Result
   */
  public static ServerTestResult success(long millis, LocalDateTime testedAt) {
    return new ServerTestResult(true, millis, null, testedAt);
  }

  /**
   * Failed test
   *
   * @param reason   Code of the reason of the failure
   * @param millis   Duration in milliseconds
   * @param testedAt When the test ended
   * @return Result
   */
  public static ServerTestResult failure(String reason, long millis, LocalDateTime testedAt) {
    return new ServerTestResult(false, millis, reason, testedAt);
  }

  /**
   * Duration in seconds as the parameters of the locale SCREEN_TEXT_SCHEDULER_SERVER_TEST_SECONDS: the whole seconds
   * and the hundredths with two digits (420 ms: 0 and 42), so the locale puts the decimal separator of its language
   *
   * @return Whole seconds and hundredths
   */
  public String[] seconds() {
    long hundredths = Math.round(millis / 10.0);
    return new String[]{String.valueOf(hundredths / 100), String.format("%02d", hundredths % 100)};
  }
}
