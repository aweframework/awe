package com.almis.awe.testing.driver;

/**
 * Opaque reference to one element found at a moment, for the code that has to tell whether it is still the same element
 * (for instance, whether a message that was closed has left the page). Two references are equal when they point to the
 * same element. Internal preview of the driver port: no compatibility promise yet.
 */
public interface ElementRef {

  /**
   * Check whether the element is still part of the page (it was not replaced or removed)
   *
   * @return true if the element is still attached
   */
  boolean isAttached();
}
