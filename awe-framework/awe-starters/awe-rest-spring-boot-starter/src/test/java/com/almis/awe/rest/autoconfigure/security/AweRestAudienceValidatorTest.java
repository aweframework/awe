package com.almis.awe.rest.autoconfigure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AweRestAudienceValidatorTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

  @Test
  void shouldAcceptMatchingAudience() {
    AweRestAudienceValidator validator = new AweRestAudienceValidator(List.of("api://awe-rest"));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("api://awe-rest")));

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void shouldRejectWhenAudienceDoesNotMatch() {
    AweRestAudienceValidator validator = new AweRestAudienceValidator(List.of("api://awe-rest"));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("api://other")));

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).anyMatch(error -> "invalid_token".equals(error.getErrorCode()));
  }

  @Test
  void shouldRejectWhenAudienceMissing() {
    AweRestAudienceValidator validator = new AweRestAudienceValidator(List.of("api://awe-rest"));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithoutAudience());

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).anyMatch(error -> error.getDescription().contains("audience"));
  }

  @Test
  void shouldAcceptAnyConfiguredAudienceMatch() {
    AweRestAudienceValidator validator = new AweRestAudienceValidator(List.of("api://awe-rest", "api://admin"));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("api://admin", "api://x")));

    assertThat(result.hasErrors()).isFalse();
  }

  private Jwt jwtWithAudience(List<String> audience) {
    return Jwt.withTokenValue("token")
      .header("alg", "none")
      .claim("sub", "subject")
      .claim("aud", audience)
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();
  }

  private Jwt jwtWithoutAudience() {
    return Jwt.withTokenValue("token")
      .header("alg", "none")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();
  }
}
