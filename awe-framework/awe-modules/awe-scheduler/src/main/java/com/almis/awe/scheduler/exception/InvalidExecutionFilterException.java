package com.almis.awe.scheduler.exception;

import com.almis.awe.exception.AWException;

/**
 * A filter of the execution list of all tasks has a value that is not valid: the list is refused instead of being
 * read without that filter
 */
public class InvalidExecutionFilterException extends AWException {

  /**
   * Constructor
   *
   * @param title   Translated title
   * @param message Translated message
   */
  public InvalidExecutionFilterException(String title, String message) {
    super(title, message);
  }
}
