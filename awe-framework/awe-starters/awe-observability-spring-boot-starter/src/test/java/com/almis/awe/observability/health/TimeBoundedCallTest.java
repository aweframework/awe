package com.almis.awe.observability.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeBoundedCallTest {

  @Test
  @DisplayName("A call cancelled before it started does not leave its key stuck as still running")
  void cancelledBeforeItStartedReleasesTheKey() throws Exception {
    ExecutorService single = Executors.newSingleThreadExecutor();
    CountDownLatch blockerStarted = new CountDownLatch(1);
    CountDownLatch releaseBlocker = new CountDownLatch(1);
    try {
      // The only pool thread is busy, so the check cannot start before its timeout
      single.submit(() -> {
        blockerStarted.countDown();
        releaseBlocker.await();
        return null;
      });
      assertThat(blockerStarted.await(5, TimeUnit.SECONDS)).isTrue();
      TimeBoundedCall bounded = new TimeBoundedCall(single);

      assertThatThrownBy(() -> bounded.run("db", () -> "never runs", Duration.ofMillis(50))).isInstanceOf(TimeoutException.class);
      releaseBlocker.countDown();

      assertThat(bounded.run("db", () -> "runs again", Duration.ofSeconds(5))).isEqualTo("runs again");
    } finally {
      releaseBlocker.countDown();
      single.shutdownNow();
    }
  }

  @Test
  @DisplayName("A call that is cancelled but ignores the interruption keeps its key busy until it ends")
  void hungCallKeepsTheKeyBusy() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService pool = Executors.newCachedThreadPool();
    try {
      TimeBoundedCall bounded = new TimeBoundedCall(pool);
      assertThatThrownBy(() -> bounded.run("db", () -> {
        while (release.getCount() > 0) {
          try {
            release.await();
          } catch (InterruptedException exc) {
            // keep hanging
          }
        }
        return "late";
      }, Duration.ofMillis(50))).isInstanceOf(TimeoutException.class);

      assertThatThrownBy(() -> bounded.run("db", () -> "second", Duration.ofMillis(50)))
        .isInstanceOf(TimeBoundedCall.StillRunningException.class);

      release.countDown();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      String result = null;
      while (result == null && System.nanoTime() < deadline) {
        try {
          result = bounded.run("db", () -> "free", Duration.ofSeconds(1));
        } catch (TimeBoundedCall.StillRunningException exc) {
          Thread.sleep(10);
        }
      }
      assertThat(result).isEqualTo("free");
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("Different keys do not block each other")
  void keysAreIndependent() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService pool = Executors.newCachedThreadPool();
    try {
      TimeBoundedCall bounded = new TimeBoundedCall(pool);
      assertThatThrownBy(() -> bounded.run("hung", () -> {
        while (release.getCount() > 0) {
          try {
            release.await();
          } catch (InterruptedException exc) {
            // keep hanging
          }
        }
        return null;
      }, Duration.ofMillis(50))).isInstanceOf(TimeoutException.class);

      assertThat(bounded.run("other", () -> "fine", Duration.ofSeconds(5))).isEqualTo("fine");
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }
}
