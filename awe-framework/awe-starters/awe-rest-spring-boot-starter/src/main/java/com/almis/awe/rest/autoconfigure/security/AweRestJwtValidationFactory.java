package com.almis.awe.rest.autoconfigure.security;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Factory methods for JWT validation composition.
 */
public final class AweRestJwtValidationFactory {

  private AweRestJwtValidationFactory() {
  }

  /**
   * Compose validators into a delegating validator.
   *
   * @param validators validators to compose
   * @return composed validator
   */
  @SafeVarargs
  public static OAuth2TokenValidator<Jwt> composeValidators(OAuth2TokenValidator<Jwt>... validators) {
    return new DelegatingOAuth2TokenValidator<>(validators);
  }
}
