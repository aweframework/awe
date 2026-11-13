package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.AbstractPlaywrightBrowserTest;
import com.almis.awe.testing.driver.Locator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wait for an element to be gone ({@link BrowserCondition#invisible}) polled over a real headless browser, with the
 * loaders that the client shows: one that fades in and is removed, and one that fades out and stays in the layout
 */
class BrowserConditionPlaywrightTest extends AbstractPlaywrightBrowserTest {

  private static final Locator LOADER = Locator.css("#loader");
  private static final String STYLE = "<style>#loader { position: absolute; top: 0; left: 0; width: 100%; height: 100%;"
    + " opacity: 0; transition: opacity 150ms linear; } #loader.in { opacity: 1; }</style><div id='grid'>rows</div>";
  private static final String ADD_LOADER = "const loader = document.createElement('div'); loader.id = 'loader';"
    + "document.body.appendChild(loader); loader.getBoundingClientRect();";

  /**
   * Wait for the loader to be gone, counting how many times the poll looked
   */
  private int waitUntilGone(Duration timeout) {
    AtomicInteger looks = new AtomicInteger();
    BrowserCondition invisible = BrowserCondition.invisible(LOADER);
    BrowserPoll.until(browser, BrowserCondition.of("counting " + invisible, driver -> {
      looks.incrementAndGet();
      return invisible.isMet(driver);
    }), timeout);
    return looks.get();
  }

  @Test
  void shouldNotTakeALoaderThatFadesInAsGoneUntilItIsRemoved() {
    show(STYLE);
    page.evaluate("(() => { " + ADD_LOADER + "requestAnimationFrame(() => loader.classList.add('in')); })()");
    AtomicInteger looks = new AtomicInteger();
    BrowserCondition invisible = BrowserCondition.invisible(LOADER);

    // The loader is removed after the first look that does not take it as gone, so a later look finds it gone, however
    // long the looks take and however slowly the page animates
    BrowserPoll.until(browser, BrowserCondition.of("counting " + invisible, driver -> {
      boolean gone = invisible.isMet(driver);
      if (!gone && looks.incrementAndGet() == 1) {
        page.evaluate("document.getElementById('loader').remove()");
      }
      return gone;
    }), Duration.ofSeconds(10));

    // It was not taken as gone on its first, transparent frame: only once it was really removed
    assertThat(looks.get()).isGreaterThanOrEqualTo(1);
    assertThat(browser.exists(LOADER)).isFalse();
  }

  @Test
  void shouldTakeALoaderThatFadesOutAndStaysInTheLayoutAsGone() {
    show(STYLE);
    page.evaluate("(() => { " + ADD_LOADER + "loader.classList.add('in'); loader.getBoundingClientRect();"
      + "setTimeout(() => loader.classList.remove('in'), 100); })()");

    int looks = waitUntilGone(Duration.ofSeconds(10));

    assertThat(looks).isGreaterThanOrEqualTo(2);
    assertThat(browser.exists(LOADER)).isTrue();
    assertThat(browser.isRendered(LOADER)).isTrue();
    assertThat(browser.isVisible(LOADER)).isFalse();
  }

  @Test
  void shouldTakeARemovedLoaderAsGoneAtOnce() {
    show(STYLE);

    assertThat(waitUntilGone(Duration.ofSeconds(10))).isEqualTo(1);
  }

  @Test
  void shouldTakeALoaderThatIsNotDisplayedAsGoneAtOnce() {
    show(STYLE);
    page.evaluate("(() => { " + ADD_LOADER + "loader.classList.add('in'); loader.style.display = 'none'; })()");

    assertThat(waitUntilGone(Duration.ofSeconds(10))).isEqualTo(1);
  }

  @Test
  void shouldKeepWaitingWhileTheLoaderIsShown() {
    show(STYLE);
    page.evaluate("(() => { " + ADD_LOADER + "loader.classList.add('in'); })()");

    assertThatThrownBy(() -> waitUntilGone(Duration.ofMillis(1200))).isInstanceOf(BrowserPoll.PollTimeoutException.class);
  }
}
