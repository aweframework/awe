package com.almis.awe.rest.service;

import com.almis.awe.rest.dto.JwtTokenInfo;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JWTTokenServiceTest {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer";
  private static final String SECRET = "test-secret";
  private static final String ISSUER = "awe";

  private JWTTokenService service;

  @BeforeEach
  void setUp() {
    service = new JWTTokenService(HEADER, PREFIX, SECRET, ISSUER, Duration.ofMinutes(5));
  }

  @Test
  void generatedTokenIsAddedToResponseAndVerifies() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(new User("test", "secret", Collections.emptyList()));
    HttpServletResponse response = mock(HttpServletResponse.class);

    JwtTokenInfo info = service.generateToken(authentication, response);

    verify(response).addHeader(HEADER, PREFIX + " " + info.getToken());
    assertThat(info.getUsername()).isEqualTo("test");
    assertThat(info.getIssuer()).isEqualTo(ISSUER);
    assertThat(info.getExpiredAt()).isAfter(new Date());

    DecodedJWT decoded = service.verifyToken(info.getToken());
    assertThat(decoded.getSubject()).isEqualTo("test");
    assertThat(decoded.getIssuer()).isEqualTo(ISSUER);
  }

  @Test
  void extractTokenRemovesPrefix() {
    assertThat(service.extractToken(PREFIX + " abc.def.ghi")).isEqualTo("abc.def.ghi");
  }

  @Test
  void tokenSignedWithAnotherSecretIsRejected() {
    String token = sign(Algorithm.HMAC512("other-secret"), ISSUER, inMinutes(5));

    assertThatThrownBy(() -> service.verifyToken(token)).isInstanceOf(JWTVerificationException.class);
  }

  @Test
  void tokenFromAnotherIssuerIsRejected() {
    String token = sign(Algorithm.HMAC512(SECRET), "other-issuer", inMinutes(5));

    assertThatThrownBy(() -> service.verifyToken(token)).isInstanceOf(JWTVerificationException.class);
  }

  @Test
  void expiredTokenIsRejected() {
    String token = sign(Algorithm.HMAC512(SECRET), ISSUER, inMinutes(-5));

    assertThatThrownBy(() -> service.verifyToken(token)).isInstanceOf(JWTVerificationException.class);
  }

  @Test
  void malformedTokenIsRejected() {
    assertThatThrownBy(() -> service.verifyToken("not-a-token")).isInstanceOf(JWTVerificationException.class);
  }

  private static String sign(Algorithm algorithm, String issuer, Date expiresAt) {
    return JWT.create().withSubject("test").withIssuer(issuer).withExpiresAt(expiresAt).sign(algorithm);
  }

  private static Date inMinutes(long minutes) {
    return new Date(System.currentTimeMillis() + Duration.ofMinutes(minutes).toMillis());
  }
}
