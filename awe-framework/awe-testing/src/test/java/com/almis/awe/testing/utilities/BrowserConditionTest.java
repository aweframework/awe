package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserConditionTest {

  private static final Locator ITEM = Locator.css("#item");

  private final BrowserDriver browser = mock(BrowserDriver.class);

  @Test
  void shouldBePresentWhenSomethingMatches() {
    when(browser.exists(ITEM)).thenReturn(false, true);

    assertThat(BrowserCondition.present(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.present(ITEM).isMet(browser)).isTrue();
  }

  @Test
  void shouldBeVisibleAsTheBrowserReportsIt() {
    when(browser.isVisible(ITEM)).thenReturn(true, false);

    assertThat(BrowserCondition.visible(ITEM).isMet(browser)).isTrue();
    assertThat(BrowserCondition.visible(ITEM).isMet(browser)).isFalse();
  }

  /** Clock that only moves when the test says so */
  private static final class StoppedClock extends Clock {
    private Instant now = Instant.parse("2026-10-06T10:00:00Z");

    void advance(long millis) {
      now = now.plusMillis(millis);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }

    long nanos() {
      return java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(now.toEpochMilli());
    }
  }

  private final StoppedClock clock = new StoppedClock();

  @Test
  void shouldBeInvisibleAtOnceWhenTheElementIsNoLongerRendered() {
    when(browser.isRendered(ITEM)).thenReturn(true, false);
    when(browser.isVisible(ITEM)).thenReturn(true);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    assertThat(invisible.isMet(browser)).isTrue();
  }

  @Test
  void shouldNeverBeInvisibleWhileTheElementIsShown() {
    when(browser.isRendered(ITEM)).thenReturn(true);
    when(browser.isVisible(ITEM)).thenReturn(true);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(10_000);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(10_000);
    assertThat(invisible.isMet(browser)).isFalse();
  }

  @Test
  void shouldBeInvisibleWhenTheElementHasBeenTransparentLongEnough() {
    // An element that faded out to opacity 0 stays in the layout: it is gone once it has been transparent for a while,
    // whatever the interval of the poll
    when(browser.isRendered(ITEM)).thenReturn(true);
    when(browser.isVisible(ITEM)).thenReturn(false);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(299);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(1);
    assertThat(invisible.isMet(browser)).isTrue();
  }

  @Test
  void shouldNotDependOnTheNumberOfChecksNorOnTheIntervalBetweenThem() {
    when(browser.isRendered(ITEM)).thenReturn(true);
    when(browser.isVisible(ITEM)).thenReturn(false);
    BrowserCondition fastPoll = BrowserCondition.invisible(ITEM, clock::nanos);
    BrowserCondition slowPoll = BrowserCondition.invisible(ITEM, clock::nanos);

    // Many checks in a short time are not enough, two checks far apart are
    for (int check = 0; check < 20; check++) {
      assertThat(fastPoll.isMet(browser)).isFalse();
      clock.advance(10);
    }
    assertThat(slowPoll.isMet(browser)).isFalse();
    clock.advance(5_000);
    assertThat(slowPoll.isMet(browser)).isTrue();
  }

  @Test
  void shouldNotTakeALoaderThatIsFadingInAsInvisible() {
    // Transparent in its first frame, then shown: the time starts again every time that it is shown
    when(browser.isRendered(ITEM)).thenReturn(true);
    when(browser.isVisible(ITEM)).thenReturn(false, true, false, true, false, false);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(200);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(200);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(200);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(200);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(200);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(300);
    assertThat(invisible.isMet(browser)).isTrue();
  }

  @Test
  void shouldStartCountingAgainWhenTheTransparentElementIsRemovedAndComesBack() {
    when(browser.isRendered(ITEM)).thenReturn(true, false, true, true);
    when(browser.isVisible(ITEM)).thenReturn(false);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(1_000);
    assertThat(invisible.isMet(browser)).isTrue();
    clock.advance(1_000);
    // It is back and transparent: it has not been transparent for a while yet
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(300);
    assertThat(invisible.isMet(browser)).isTrue();
  }

  @Test
  void shouldBeCorrectWhenTheSameConditionIsUsedForAnotherWait() {
    when(browser.isRendered(ITEM)).thenReturn(true);
    when(browser.isVisible(ITEM)).thenReturn(false);
    BrowserCondition invisible = BrowserCondition.invisible(ITEM, clock::nanos);

    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(500);
    assertThat(invisible.isMet(browser)).isTrue();

    // A later wait on the same instance starts from zero
    clock.advance(60_000);
    assertThat(invisible.isMet(browser)).isFalse();
    clock.advance(500);
    assertThat(invisible.isMet(browser)).isTrue();
  }

  @Test
  void shouldBeClickableOnlyWhenItIsVisibleAndEnabled() {
    when(browser.isVisible(ITEM)).thenReturn(false, true, true);
    when(browser.isEnabled(ITEM)).thenReturn(false, true);

    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isFalse();
    assertThat(BrowserCondition.clickable(ITEM).isMet(browser)).isTrue();
  }

  @Test
  void shouldMatchTheTextOfTheFirstMatch() {
    when(browser.text(ITEM)).thenReturn("Hello world");

    assertThat(BrowserCondition.textContains(ITEM, "world").isMet(browser)).isTrue();
    assertThat(BrowserCondition.textContains(ITEM, "moon").isMet(browser)).isFalse();
  }

  @Test
  void shouldMatchTheWholeTextIgnoringTheCaseAndTheTextOfAnyMatch() {
    when(browser.text(ITEM)).thenReturn("Hello World", "Hello World!");
    when(browser.texts(ITEM)).thenReturn(List.of("one", "two"), List.of(), List.of("one"));

    assertThat(BrowserCondition.textEquals(ITEM, "hello world").isMet(browser)).isTrue();
    assertThat(BrowserCondition.textEquals(ITEM, "hello world").isMet(browser)).isFalse();
    assertThat(BrowserCondition.anyTextContains(ITEM, "tw").isMet(browser)).isTrue();
    assertThat(BrowserCondition.anyTextContains(ITEM, "tw").isMet(browser)).isFalse();
    assertThat(BrowserCondition.anyTextContains(ITEM, "tw").isMet(browser)).isFalse();
  }

  @Test
  void shouldTakeAReplacedElementAsNotMatchingYetButNotAMissingOne() {
    when(browser.text(ITEM)).thenThrow(new ElementReplacedException(ITEM, new RuntimeException("stale")));
    when(browser.attribute(ITEM, "value")).thenThrow(new ElementNotFoundException(ITEM, new RuntimeException()));

    assertThat(BrowserCondition.textContains(ITEM, "x").isMet(browser)).isFalse();
    assertThatThrownBy(() -> BrowserCondition.valueContains(ITEM, "x").isMet(browser))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldMatchTheValueOfTheFirstMatchAndNeverAMissingOne() {
    when(browser.attribute(ITEM, "value")).thenReturn("typed text", null);

    assertThat(BrowserCondition.valueContains(ITEM, "typed").isMet(browser)).isTrue();
    assertThat(BrowserCondition.valueContains(ITEM, "typed").isMet(browser)).isFalse();
  }

  @Test
  void shouldRequireAllTheConditionsAndStopAtTheFirstOneThatFails() {
    BrowserCondition failing = BrowserCondition.of("failing", driver -> false);
    BrowserCondition second = BrowserCondition.of("second", driver -> {
      throw new IllegalStateException("must not be checked");
    });

    assertThat(BrowserCondition.allOf(failing, second).isMet(browser)).isFalse();
    assertThat(BrowserCondition.allOf(BrowserCondition.of("a", driver -> true), BrowserCondition.of("b", driver -> true))
      .isMet(browser)).isTrue();
  }

  @Test
  void shouldBeMetWhenAnyOfTheConditionsIsAndStopAtTheFirstOneThatIs() {
    BrowserCondition failing = BrowserCondition.of("failing", driver -> false);
    BrowserCondition met = BrowserCondition.of("met", driver -> true);
    BrowserCondition unreached = BrowserCondition.of("unreached", driver -> {
      throw new IllegalStateException("must not be checked");
    });

    assertThat(BrowserCondition.anyOf(failing, met, unreached).isMet(browser)).isTrue();
    assertThat(BrowserCondition.anyOf(failing, failing).isMet(browser)).isFalse();
  }

  @Test
  void shouldKeepCheckingAfterAConditionThatFailsAndThrowItOnlyWhenNoneIsMet() {
    BrowserCondition missing = BrowserCondition.of("missing", driver -> {
      throw new ElementNotFoundException(ITEM, new RuntimeException());
    });
    BrowserCondition broken = BrowserCondition.of("broken", driver -> {
      throw new IllegalStateException("broken");
    });
    BrowserCondition met = BrowserCondition.of("met", driver -> true);
    BrowserCondition failing = BrowserCondition.of("failing", driver -> false);

    assertThat(BrowserCondition.anyOf(missing, broken, met).isMet(browser)).isTrue();
    assertThatThrownBy(() -> BrowserCondition.anyOf(missing, failing).isMet(browser))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldNegateTheConditionButLetAMissingElementThrough() {
    when(browser.attribute(ITEM, "value")).thenReturn("typed");

    assertThat(BrowserCondition.not(BrowserCondition.valueContains(ITEM, "typed")).isMet(browser)).isFalse();
    assertThat(BrowserCondition.not(BrowserCondition.valueContains(ITEM, "other")).isMet(browser)).isTrue();

    when(browser.attribute(ITEM, "value")).thenThrow(new ElementNotFoundException(ITEM, new RuntimeException()));
    assertThatThrownBy(() -> BrowserCondition.not(BrowserCondition.valueContains(ITEM, "typed")).isMet(browser))
      .isInstanceOf(ElementNotFoundException.class);
  }

  @Test
  void shouldDescribeItselfForTheFailureMessage() {
    assertThat(BrowserCondition.present(ITEM)).hasToString("presence of element located by css=#item");
    assertThat(BrowserCondition.visible(ITEM)).hasToString("visibility of element located by css=#item");
    assertThat(BrowserCondition.invisible(ITEM)).hasToString("element located by css=#item to be invisible");
    assertThat(BrowserCondition.clickable(ITEM)).hasToString("element to be clickable: css=#item");
    assertThat(BrowserCondition.textContains(ITEM, "a")).hasToString("text 'a' to be present in element located by css=#item");
    assertThat(BrowserCondition.valueContains(ITEM, "a")).hasToString("text 'a' to be present in the value of element located by css=#item");
    assertThat(BrowserCondition.allOf(BrowserCondition.present(ITEM), BrowserCondition.visible(ITEM)))
      .hasToString("all conditions to be valid: presence of element located by css=#item and visibility of element located by css=#item");
    assertThat(BrowserCondition.anyOf(BrowserCondition.present(ITEM), BrowserCondition.visible(ITEM)))
      .hasToString("at least one condition to be valid: presence of element located by css=#item or visibility of element located by css=#item");
    assertThat(BrowserCondition.not(BrowserCondition.present(ITEM)))
      .hasToString("condition to not be valid: presence of element located by css=#item");
  }
}
