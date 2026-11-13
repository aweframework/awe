package com.almis.awe.testing.driver;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.Objects;

/**
 * Reference to a Selenium element: equal when the elements are the same
 *
 * @param element Selenium element
 * @param locator Locator it was found with, to tell which one was replaced
 */
record SeleniumElementRef(WebElement element, Locator locator) implements ElementRef {

  @Override
  public boolean isAttached() {
    try {
      // Any call on a detached element fails as stale
      element.isEnabled();
      return true;
    } catch (StaleElementReferenceException exc) {
      return false;
    }
  }

  @Override
  public boolean isVisible() {
    try {
      return element.isDisplayed();
    } catch (StaleElementReferenceException exc) {
      throw new ElementReplacedException(locator, exc);
    }
  }

  @Override
  public boolean isEnabled() {
    try {
      return element.isEnabled();
    } catch (StaleElementReferenceException exc) {
      throw new ElementReplacedException(locator, exc);
    }
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof SeleniumElementRef ref && element.equals(ref.element);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(element);
  }
}
