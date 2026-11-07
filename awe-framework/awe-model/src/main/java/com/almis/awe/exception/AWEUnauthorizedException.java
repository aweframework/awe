package com.almis.awe.exception;

/**
 * Specific AWE exception of a client launch of a query or a maintain target refused by the authorization, so the
 * callers can tell a refusal from any other error
 */
public class AWEUnauthorizedException extends AWException {

  /**
   * Locale key of the title of every refusal answered to a client
   */
  public static final String TITLE_KEY = "ERROR_TITLE_ACCESS_DENIED";

  /**
   * Locale key of the message of every refusal answered to a client. It has no parameters: the refused target is a
   * technical detail kept in the log and the security audit, never shown to the user
   */
  public static final String MESSAGE_KEY = "ERROR_MESSAGE_ACTION_NOT_PERMITTED";

  /**
   * Constructs an instance of <code>AWEUnauthorizedException</code> with the specified detail message.
   *
   * @param title   Exception title
   * @param message Detail message
   */
  public AWEUnauthorizedException(String title, String message) {
    super(title, message);
  }
}
