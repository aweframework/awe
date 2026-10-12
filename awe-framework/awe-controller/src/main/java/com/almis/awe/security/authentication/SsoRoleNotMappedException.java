package com.almis.awe.security.authentication;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Thrown when an SSO login is refused because a role is required ({@code awe.security.sso.require-role}) and no role
 * sent by the identity provider maps to an AWE profile.
 *
 * <p>It is an account status failure, so the SSO success handler rejects the login the same way as a disabled or
 * locked AWE user: no AWE session is created and the user sees the error page with the localized message.</p>
 */
public class SsoRoleNotMappedException extends AccountStatusException {

  /**
   * Create the exception
   *
   * @param message localized message shown to the user
   */
  public SsoRoleNotMappedException(String message) {
    super(message);
  }
}
