package com.almis.awe.exception;

import com.almis.awe.model.type.AnswerType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/*
 * File Imports
 */

/**
 * Specific AWE Exception class
 * Formatted exception to show a title and a description of the thrown ERROR
 *
 * @author Pablo GARCIA - 24/JUN/2010
 */
@Getter
@Slf4j
public class AWEQueryException extends AWException {

  private static final long serialVersionUID = -764683322805477265L;
  // Exception query
  private final String query;

  /**
   * Constructs an instance of <code>AWException</code> with the specified detail message and cause exception.
   *
   * @param title   Exception title
   * @param message Detail message
   * @param query   Query launched
   * @param cause   Cause exception
   */
  public AWEQueryException(String title, String message, String query, Throwable cause) {
    super(title, message, cause);
    this.query = query;
  }

  @Override
  public AWException log() {
    // Log ERROR
    String errorType = AnswerType.ERROR.equals(this.getType()) ? "ERROR" : "WARNING";

    // Start log
    StringBuilder exceptionBuilder = new StringBuilder();
    exceptionBuilder.append("\n================================================================================")
      .append("\n[")
      .append(errorType)
      .append("] [Message] ")
      .append(getTitle())
      .append(" (")
      .append(getMessage())
      .append(")\n");


    // Log query
    if (query != null) {
      exceptionBuilder.append("[")
        .append(errorType)
        .append("] [Query]: ")
        .append("[")
        .append(query)
        .append("]\n");
    }

    // Log details
    exceptionBuilder.append("[")
      .append(errorType)
      .append("] [StackTrace]");
    if (AnswerType.ERROR.equals(this.getType())){
      log.error(exceptionBuilder.toString(), getCause());
    } else {
      log.warn(exceptionBuilder.toString(), getCause());
    }
    return this;
  }
}
