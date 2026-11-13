package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BrowserPollTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static final Duration INTERVAL = Duration.ofMillis(500);

  private final BrowserDriver browser = mock(BrowserDriver.class);
  private final FakeTime time = new FakeTime();

  @BeforeEach
  void clearInterrupt() {
    Thread.interrupted();
  }

  @AfterEach
  void restoreInterrupt() {
    Thread.interrupted();
  }

  @Test
  void shouldReturnWithoutSleepingWhenTheConditionIsMetAtOnce() {
    BrowserPoll.until(browser, condition("now", () -> true), TIMEOUT, INTERVAL, time, time);

    assertThat(time.sleeps).isEmpty();
  }

  @Test
  void shouldCheckAgainAfterEachIntervalUntilTheConditionIsMet() {
    AtomicInteger checks = new AtomicInteger();

    BrowserPoll.until(browser, condition("third time", () -> checks.incrementAndGet() == 3), TIMEOUT, INTERVAL, time, time);

    assertThat(checks).hasValue(3);
    assertThat(time.sleeps).containsExactly(INTERVAL, INTERVAL);
  }

  @Test
  void shouldFailWithTheDescriptionOfTheConditionWhenItIsNeverMet() {
    AtomicInteger checks = new AtomicInteger();

    assertThatThrownBy(() -> BrowserPoll.until(browser, condition("a lazy panel", () -> {
      checks.incrementAndGet();
      return false;
    }), TIMEOUT, INTERVAL, time, time))
      .isInstanceOf(BrowserPoll.PollTimeoutException.class)
      .hasMessageContaining("a lazy panel")
      .hasMessageContaining("1 second");

    // It checks once more when the time is over, as the Selenium wait always did: at 0, 500, 1000 and 1500 ms
    assertThat(checks).hasValue(4);
    assertThat(time.sleeps).hasSize(3);
  }

  @Test
  void shouldKeepWaitingWhenTheElementIsNotThereYetAndReportItWhenTimeIsOver() {
    ElementNotFoundException missing = new ElementNotFoundException(Locator.css("#late"), new RuntimeException());
    AtomicInteger checks = new AtomicInteger();

    BrowserPoll.until(browser, condition("late element", () -> {
      if (checks.incrementAndGet() < 3) {
        throw missing;
      }
      return true;
    }), TIMEOUT, INTERVAL, time, time);
    assertThat(checks).hasValue(3);

    assertThatThrownBy(() -> BrowserPoll.until(browser, condition("never there", () -> {
      throw missing;
    }), TIMEOUT, INTERVAL, time, time))
      .isInstanceOf(BrowserPoll.PollTimeoutException.class)
      .hasCause(missing);
  }

  @Test
  void shouldNotHideAnElementThatWasReplacedWhileItWasChecked() {
    ElementReplacedException replaced = new ElementReplacedException(Locator.css("#moved"), new RuntimeException("stale"));

    assertThatThrownBy(() -> BrowserPoll.until(browser, condition("moved", () -> {
      throw replaced;
    }), TIMEOUT, INTERVAL, time, time)).isSameAs(replaced);
    assertThat(time.sleeps).isEmpty();
  }

  @Test
  void shouldNotHideAnyOtherFailureOfTheCondition() {
    IllegalStateException failure = new IllegalStateException("broken");

    assertThatThrownBy(() -> BrowserPoll.until(browser, condition("broken", () -> {
      throw failure;
    }), TIMEOUT, INTERVAL, time, time)).isSameAs(failure);
  }

  @Test
  void shouldCheckOnceEvenWhenThereIsNoTime() {
    AtomicInteger checks = new AtomicInteger();

    assertThatCode(() -> BrowserPoll.until(browser, condition("once", () -> checks.incrementAndGet() > 0),
      Duration.ZERO, INTERVAL, time, time)).doesNotThrowAnyException();
    assertThat(checks).hasValue(1);
  }

  @Test
  void shouldStopWaitingWhenTheThreadIsInterrupted() {
    BrowserPoll.Sleeper interrupted = duration -> {
      throw new InterruptedException("stop");
    };

    assertThatThrownBy(() -> BrowserPoll.until(browser, condition("never", () -> false), TIMEOUT, INTERVAL, time, interrupted))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("never");
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  @Test
  void shouldPollAgainstTheBrowserItWasGiven() {
    List<BrowserDriver> seen = new ArrayList<>();

    BrowserPoll.until(browser, new BrowserCondition() {
      @Override
      public boolean isMet(BrowserDriver driver) {
        seen.add(driver);
        return true;
      }

      @Override
      public String toString() {
        return "browser check";
      }
    }, TIMEOUT, INTERVAL, time, time);

    assertThat(seen).containsExactly(browser);
  }

  private static BrowserCondition condition(String description, java.util.function.BooleanSupplier check) {
    return BrowserCondition.of(description, driver -> check.getAsBoolean());
  }

  /**
   * A clock that only moves when somebody sleeps, so the poll is checked without waiting
   */
  private static final class FakeTime extends Clock implements BrowserPoll.Sleeper {
    private Instant now = Instant.EPOCH;
    private final List<Duration> sleeps = new ArrayList<>();

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public void sleep(Duration duration) {
      sleeps.add(duration);
      now = now.plus(duration);
    }
  }
}
