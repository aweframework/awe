package com.almis.awe.rest.autoconfigure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Validates token audience for awe-rest OAuth2 Resource Server mode.
 */
public class AweRestAudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final Set<String> acceptedAudiences;

  /**
   * Audience validator constructor.
   *
   * @param acceptedAudiences Accepted audiences
   */
  public AweRestAudienceValidator(Collection<String> acceptedAudiences) {
    this.acceptedAudiences = Set.copyOf(acceptedAudiences);
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    List<String> tokenAudiences = token.getAudience();
    if (tokenAudiences == null || tokenAudiences.isEmpty()) {
      return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Token audience is missing", null)
      );
    }

    boolean anyMatch = tokenAudiences.stream().anyMatch(acceptedAudiences::contains);
    if (!anyMatch) {
      return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Token audience is not accepted", null)
      );
    }

    return OAuth2TokenValidatorResult.success();
  }
}
