package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Something to wait for in the browser, checked instantly against the {@link BrowserDriver} port (the waiting itself is
 * done by {@link BrowserPoll}). It is the tool-neutral counterpart of the Selenium expected conditions that the test
 * utilities have always waited for, with their same semantics: an element that is not there yet or that was replaced
 * while it was read is "not yet", never an error, so the poll keeps waiting. {@link #toString()} describes what is waited
 * for, as the message of the failure.
 */
@FunctionalInterface
interface BrowserCondition {

  /** Time that a rendered element has to be transparent to be taken as gone */
  Duration TRANSPARENT_TO_BE_GONE = Duration.ofMillis(300);

  /**
   * Check the condition now
   *
   * @param browser Browser
   * @return true if it is met
   */
  boolean isMet(BrowserDriver browser);

  /**
   * Create a condition from a check
   *
   * @param description What is waited for, as the failure message says it
   * @param check       Check
   * @return Condition
   */
  static BrowserCondition of(String description, Predicate<BrowserDriver> check) {
    return new BrowserCondition() {
      @Override
      public boolean isMet(BrowserDriver browser) {
        return check.test(browser);
      }

      @Override
      public String toString() {
        return description;
      }
    };
  }

  /**
   * Something matches the locator
   *
   * @param locator Locator
   * @return Condition
   */
  static BrowserCondition present(Locator locator) {
    return of("presence of element located by " + locator, browser -> browser.exists(locator));
  }

  /**
   * The first match of the locator is displayed
   *
   * @param locator Locator
   * @return Condition
   */
  static BrowserCondition visible(Locator locator) {
    return of("visibility of element located by " + locator, browser -> browser.isVisible(locator));
  }

  /**
   * The first match of the locator is gone: there is none, it is not rendered ({@code display:none},
   * {@code visibility:hidden}, no box), or it has been transparent for at least {@link #TRANSPARENT_TO_BE_GONE}.
   *
   * <p>A transparent element that is still rendered is not gone at its first look: a loader that fades in is at
   * {@code opacity:0} in its first frame and covers what it loads from the next one. An element that is still transparent
   * 300 milliseconds after it was first seen transparent has faded out to stay (it is gone, as it is for Selenium, whose
   * {@code isDisplayed} counts opacity) while a fade-in of some hundred milliseconds is over by then and restarts the
   * time. It depends on the time, not on the number of checks, so the interval of the poll does not matter (the poll of
   * the utilities looks every 500 ms, so a transparent element is gone at its second look).</p>
   *
   * <p>The condition remembers when the element was first seen transparent, so <strong>create one per wait</strong> (as the
   * utilities do). If one is reused it is still correct: the time restarts when the element is shown or not rendered, and
   * when the condition is met. With Selenium a transparent element is not rendered ({@link BrowserDriver#isRendered} is
   * its {@link BrowserDriver#isVisible}), so it is gone at once, as it always was.</p>
   *
   * @param locator Locator
   * @return Condition
   */
  static BrowserCondition invisible(Locator locator) {
    return invisible(locator, System::nanoTime);
  }

  /**
   * The first match of the locator is gone, with the time that a monotonic ticker tells (nanoseconds, as
   * {@link System#nanoTime()}: a change of the system clock does not change how long the element has been transparent)
   *
   * @param locator Locator
   * @param nanos   Ticker that measures the time that the element is transparent, in nanoseconds
   * @return Condition
   * @see #invisible(Locator)
   */
  static BrowserCondition invisible(Locator locator, LongSupplier nanos) {
    return new BrowserCondition() {
      private Long transparentSince;

      @Override
      public boolean isMet(BrowserDriver browser) {
        if (!browser.isRendered(locator)) {
          transparentSince = null;
          return true;
        }
        if (browser.isVisible(locator)) {
          transparentSince = null;
          return false;
        }
        long now = nanos.getAsLong();
        if (transparentSince == null) {
          transparentSince = now;
        }
        boolean gone = now - transparentSince >= TRANSPARENT_TO_BE_GONE.toNanos();
        if (gone) {
          transparentSince = null;
        }
        return gone;
      }

      @Override
      public String toString() {
        return "element located by " + locator + " to be invisible";
      }
    };
  }

  /**
   * The first match of the locator is displayed and enabled
   *
   * @param locator Locator
   * @return Condition
   */
  static BrowserCondition clickable(Locator locator) {
    return of("element to be clickable: " + locator, browser -> browser.isVisible(locator) && browser.isEnabled(locator));
  }

  /**
   * The text of the first match contains a text. An element that is not there throws
   * {@link com.almis.awe.testing.driver.ElementNotFoundException}, which the poll takes as "not yet" and a negation
   * lets through, as the Selenium condition did; a replaced one is not a match
   *
   * @param locator Locator
   * @param text    Text
   * @return Condition
   */
  static BrowserCondition textContains(Locator locator, String text) {
    return of("text '" + text + "' to be present in element located by " + locator, browser -> {
      try {
        return browser.text(locator).contains(text);
      } catch (ElementReplacedException exc) {
        return false;
      }
    });
  }

  /**
   * The text of the first match is a text, ignoring the case. See {@link #textContains} for a missing or replaced element
   *
   * @param locator Locator
   * @param text    Text
   * @return Condition
   */
  static BrowserCondition textEquals(Locator locator, String text) {
    return of("text '" + text + "' to be the text of the element located by " + locator, browser -> {
      try {
        return browser.text(locator).equalsIgnoreCase(text);
      } catch (ElementReplacedException exc) {
        return false;
      }
    });
  }

  /**
   * The text of any of the matches contains a text. Nothing matching is "not yet", not an error
   *
   * @param locator Locator
   * @param text    Text
   * @return Condition
   */
  static BrowserCondition anyTextContains(Locator locator, String text) {
    return of("text '" + text + "' to be present in any of the elements located by " + locator, browser -> {
      try {
        return browser.texts(locator).stream().anyMatch(candidate -> candidate.contains(text));
      } catch (ElementReplacedException exc) {
        return false;
      }
    });
  }

  /**
   * The value of the first match contains a text. See {@link #textContains} for a missing or replaced element
   *
   * @param locator Locator
   * @param text    Text
   * @return Condition
   */
  static BrowserCondition valueContains(Locator locator, String text) {
    return of("text '" + text + "' to be present in the value of element located by " + locator, browser -> {
      try {
        String value = browser.attribute(locator, "value");
        return value != null && value.contains(text);
      } catch (ElementReplacedException exc) {
        return false;
      }
    });
  }

  /**
   * Every condition is met, checked in order until one is not
   *
   * @param conditions Conditions
   * @return Condition
   */
  static BrowserCondition allOf(BrowserCondition... conditions) {
    StringBuilder description = new StringBuilder("all conditions to be valid:");
    String separator = " ";
    for (BrowserCondition condition : conditions) {
      description.append(separator).append(condition);
      separator = " and ";
    }
    BrowserCondition[] all = Arrays.copyOf(conditions, conditions.length);
    return of(description.toString(), browser -> Arrays.stream(all).allMatch(condition -> condition.isMet(browser)));
  }

  /**
   * At least one condition is met, checked in order until one is. A condition that fails (an element that is not there)
   * does not stop the check: the failure is thrown only if no other condition is met, as the Selenium {@code or} did
   *
   * @param conditions Conditions
   * @return Condition
   */
  static BrowserCondition anyOf(BrowserCondition... conditions) {
    StringBuilder description = new StringBuilder("at least one condition to be valid:");
    String separator = " ";
    for (BrowserCondition condition : conditions) {
      description.append(separator).append(condition);
      separator = " or ";
    }
    BrowserCondition[] any = Arrays.copyOf(conditions, conditions.length);
    return of(description.toString(), browser -> {
      RuntimeException lastFailure = null;
      for (BrowserCondition condition : any) {
        try {
          if (condition.isMet(browser)) {
            return true;
          }
        } catch (RuntimeException exc) {
          lastFailure = exc;
        }
      }
      if (lastFailure != null) {
        throw lastFailure;
      }
      return false;
    });
  }

  /**
   * The condition is not met
   *
   * @param condition Condition
   * @return Condition
   */
  static BrowserCondition not(BrowserCondition condition) {
    return of("condition to not be valid: " + condition, browser -> !condition.isMet(browser));
  }
}
