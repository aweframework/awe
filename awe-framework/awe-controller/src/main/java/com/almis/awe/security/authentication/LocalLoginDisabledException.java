package com.almis.awe.security.authentication;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Thrown when a local (username and password) login is attempted while it is switched off
 * ({@code awe.security.sso.local-login=false}) and the user is not a break-glass user.
 *
 * <p>It is a {@link BadCredentialsException} with the standard message on purpose: the caller must get exactly the
 * same response as with a wrong password, so the failure cannot be used to find out which users are break-glass
 * users. The real reason is only logged on the server.</p>
 */
public class LocalLoginDisabledException extends BadCredentialsException {

  /**
   * Message of a bad credentials failure. Shared by every response that reports one, so a refused user and a
   * wrong password cannot be told apart
   */
  public static final String BAD_CREDENTIALS_MESSAGE = "Bad credentials";

  /**
   * Create the exception
   */
  public LocalLoginDisabledException() {
    super(BAD_CREDENTIALS_MESSAGE);
  }
}
