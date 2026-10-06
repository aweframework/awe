package com.almis.awe.testing.utilities;

import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;

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
   * The first match of the locator is not displayed, or there is none
   *
   * @param locator Locator
   * @return Condition
   */
  static BrowserCondition invisible(Locator locator) {
    return of("element located by " + locator + " to be invisible", browser -> !browser.isVisible(locator));
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
