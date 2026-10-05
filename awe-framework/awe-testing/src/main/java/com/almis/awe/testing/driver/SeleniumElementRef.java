package com.almis.awe.testing.driver;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

/**
 * Reference to a Selenium element: equal when the elements are the same
 */
record SeleniumElementRef(WebElement element) implements ElementRef {

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
}
