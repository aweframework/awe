package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Tool-neutral wait: it checks a {@link BrowserCondition} over the instant queries of the {@link BrowserDriver} port at
 * a fixed interval until it is met or the time is over. It keeps the behavior of the Selenium wait that the test
 * utilities used (a {@code WebDriverWait}): the condition is checked at least once and once more after the last
 * interval, the interval is 500 milliseconds, and an element that is not there ({@link ElementNotFoundException}) is
 * "not yet", while any other failure of the condition ends the wait.
 */
final class BrowserPoll {

  /** Time between two checks */
  static final Duration DEFAULT_INTERVAL = Duration.ofMillis(500);

  private BrowserPoll() {
  }

  /**
   * Pause of the poll, replaceable so it can be tested without waiting
   */
  @FunctionalInterface
  interface Sleeper {
    /**
     * Sleep
     *
     * @param duration Time to sleep
     * @throws InterruptedException If the thread is interrupted
     */
    void sleep(Duration duration) throws InterruptedException;
  }

  /**
   * The condition was not met in time
   */
  static final class PollTimeoutException extends RuntimeException {
    PollTimeoutException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Wait until a condition is met
   *
   * @param browser   Browser
   * @param condition Condition
   * @param timeout   Maximum time to wait
   * @throws PollTimeoutException If the condition is not met in time (its cause is the last element that was missing)
   */
  static void until(BrowserDriver browser, BrowserCondition condition, Duration timeout) {
    until(browser, condition, timeout, DEFAULT_INTERVAL, Clock.systemUTC(), duration -> Thread.sleep(duration.toMillis()));
  }

  /**
   * Wait until a condition is met
   *
   * @param browser   Browser
   * @param condition Condition
   * @param timeout   Maximum time to wait
   * @param interval  Time between two checks
   * @param clock     Clock that measures the time
   * @param sleeper   Pause between two checks
   * @throws PollTimeoutException  If the condition is not met in time (its cause is the last element that was missing)
   * @throws IllegalStateException If the thread is interrupted while it waits
   */
  static void until(BrowserDriver browser, BrowserCondition condition, Duration timeout, Duration interval, Clock clock,
                    Sleeper sleeper) {
    Instant end = clock.instant().plus(timeout);
    ElementNotFoundException lastMissing = null;
    while (true) {
      try {
        if (condition.isMet(browser)) {
          return;
        }
      } catch (ElementNotFoundException exc) {
        lastMissing = exc;
      }
      if (end.isBefore(clock.instant())) {
        throw new PollTimeoutException("Timed out after " + describe(timeout) + " waiting for: " + condition
          + " (polling every " + interval.toMillis() + " milliseconds)", lastMissing);
      }
      try {
        sleeper.sleep(interval);
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for: " + condition, exc);
      }
    }
  }

  private static String describe(Duration timeout) {
    return timeout.toMillis() % 1000 == 0 ? timeout.toSeconds() + " second(s)" : timeout.toMillis() + " millisecond(s)";
  }
}
