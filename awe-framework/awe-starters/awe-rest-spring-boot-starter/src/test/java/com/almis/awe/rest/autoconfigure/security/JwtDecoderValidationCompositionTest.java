package com.almis.awe.rest.autoconfigure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtDecoderValidationCompositionTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

  @Test
  void shouldPassWhenAllValidatorsPass() {
    OAuth2TokenValidator<Jwt> composed = AweRestJwtValidationFactory.composeValidators(
      token -> OAuth2TokenValidatorResult.success(),
      token -> OAuth2TokenValidatorResult.success()
    );

    OAuth2TokenValidatorResult result = composed.validate(sampleJwt());

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void shouldFailWhenAnyValidatorFails() {
    OAuth2TokenValidator<Jwt> composed = AweRestJwtValidationFactory.composeValidators(
      token -> OAuth2TokenValidatorResult.success(),
      token -> OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "invalid audience", null))
    );

    OAuth2TokenValidatorResult result = composed.validate(sampleJwt());

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).anyMatch(error -> "invalid_token".equals(error.getErrorCode()));
  }

  private Jwt sampleJwt() {
    return Jwt.withTokenValue("token")
      .header("alg", "none")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();
  }
}
