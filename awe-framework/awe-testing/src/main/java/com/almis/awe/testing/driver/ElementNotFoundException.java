package com.almis.awe.testing.driver;

/**
 * No element matches a locator. Tool-neutral: the adapter of each automation tool maps its own exception to this one.
 */
public class ElementNotFoundException extends RuntimeException {

  /**
   * Create the exception
   *
   * @param locator Locator that matched nothing
   * @param cause   Exception of the automation tool
   */
  public ElementNotFoundException(Locator locator, Throwable cause) {
    super("No element found for " + locator, cause);
  }
}
