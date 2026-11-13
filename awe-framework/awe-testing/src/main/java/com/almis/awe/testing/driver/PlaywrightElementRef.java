package com.almis.awe.testing.driver;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.PlaywrightException;

import java.util.Objects;

/**
 * Reference to a Playwright element: equal when the elements are the same. Playwright hands out a new handle on every
 * query, so the identity of the element is a token that the adapter stamps on it in the page
 *
 * @param handle  Playwright handle of the element
 * @param locator Locator it was found with, to tell which one was replaced
 * @param id      Identity of the element in the page
 */
record PlaywrightElementRef(ElementHandle handle, Locator locator, String id) implements ElementRef {

  @Override
  public boolean isAttached() {
    try {
      return Boolean.TRUE.equals(handle.evaluate(PlaywrightBrowserDriver.IS_CONNECTED));
    } catch (PlaywrightException exc) {
      // The page navigated or closed: the element is not part of it any longer
      return false;
    }
  }

  @Override
  public boolean isVisible() {
    try {
      // An element that left the page is reported as replaced, as Selenium (and the port) do
      requireAttached();
      return Boolean.TRUE.equals(handle.evaluate(PlaywrightBrowserDriver.IS_SHOWN));
    } catch (PlaywrightException exc) {
      throw replacedOrSame(exc);
    }
  }

  @Override
  public boolean isEnabled() {
    try {
      requireAttached();
      return handle.isEnabled();
    } catch (PlaywrightException exc) {
      throw replacedOrSame(exc);
    }
  }

  private void requireAttached() {
    if (!isAttached()) {
      throw new ElementReplacedException(locator, new PlaywrightException("Element is not attached to the DOM"));
    }
  }

  private RuntimeException replacedOrSame(PlaywrightException exc) {
    return PlaywrightBrowserDriver.isReplaced(exc) ? new ElementReplacedException(locator, exc) : exc;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof PlaywrightElementRef ref && id.equals(ref.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
