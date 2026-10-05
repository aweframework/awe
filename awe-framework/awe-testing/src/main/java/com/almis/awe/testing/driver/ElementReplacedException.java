package com.almis.awe.testing.driver;

/**
 * The element was replaced or moved while it was being used (a list that is filtered while the text is typed): looking
 * it up again may succeed, so callers retry. Tool-neutral: the adapter of each automation tool maps its own exceptions
 * (Selenium: stale element, move target out of bounds) to this one.
 */
public class ElementReplacedException extends RuntimeException {

  /**
   * Create the exception
   *
   * @param locator Locator of the element that was replaced
   * @param cause   Exception of the automation tool
   */
  public ElementReplacedException(Locator locator, Throwable cause) {
    super("The element " + locator + " was replaced: " + cause.getMessage(), cause);
  }
}
