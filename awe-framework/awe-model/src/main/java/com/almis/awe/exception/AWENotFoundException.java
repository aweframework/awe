package com.almis.awe.exception;

/**
 * Specific AWE not found Exception class
 *
 * @author Pablo GARCIA - 24/JUN/2010
 */
public class AWENotFoundException extends AWException {

  /**
   * Constructs an instance of <code>AWException</code> with the specified detail message.
   *
   * @param title   Exception title
   * @param message Detail message
   */
  public AWENotFoundException(String title, String message) {
    super(title, message);
  }
}
