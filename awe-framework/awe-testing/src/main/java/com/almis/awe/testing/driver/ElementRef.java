package com.almis.awe.testing.driver;

/**
 * Opaque reference to one element found at a moment, for the code that has to tell whether it is still the same element
 * (for instance, whether a message that was closed has left the page) or to ask each of several matches about its state
 * (is any of them displayed, are the displayed ones enabled). Two references are equal when they point to the
 * same element. Internal preview of the driver port: no compatibility promise yet.
 */
public interface ElementRef {

  /**
   * Check whether the element is still part of the page (it was not replaced or removed)
   *
   * @return true if the element is still attached
   */
  boolean isAttached();

  /**
   * Check whether the element is displayed
   *
   * @return true if it is displayed
   * @throws ElementReplacedException If the element was replaced since it was found
   */
  boolean isVisible();

  /**
   * Check whether the element is enabled
   *
   * @return true if it is enabled
   * @throws ElementReplacedException If the element was replaced since it was found
   */
  boolean isEnabled();
}
