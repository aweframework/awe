package com.almis.awe.rest.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.dto.JwtTokenInfo;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * Util class to manage authentication with JWT tokens.
 * @see <a href="https://jwt.io/">https://jwt.io/</a>
 */
@Getter
@Setter
public class JWTTokenService extends ServiceConfig {

  // JWT properties
  private String authorizationHeader;
  private String prefix;
  private String secret;
  private String issuer;
  private Duration expiration;

  // Fields
  private JWTVerifier jwtVerifier;

  /**
   * JWTTokenService constructor
   * @param authorizationHeader HTTP Auth header name
   * @param prefix JWT token prefix
   * @param secret JWT token secret
   * @param issuer JWT token issuer
   * @param expiration JWT token expiration
   */
  public JWTTokenService(String authorizationHeader, String prefix, String secret, String issuer, Duration expiration) {
    this.authorizationHeader = authorizationHeader;
    this.prefix = prefix;
    this.secret = secret;
    this.issuer = issuer;
    this.expiration = expiration;
  }

  /**
   * Get JWT verifier
   */
  public JWTVerifier getJWTVerifier() {
    return Optional.ofNullable(jwtVerifier)
            .orElse(JWT.require(Algorithm.HMAC512(secret)).withIssuer(issuer).build());
  }

  /**
   * Generate JWT Token and add it to http response as header
   * @param authentication Authentication object
   * @param response {@link HttpServletResponse}
   */
  public JwtTokenInfo generateToken(Authentication authentication, HttpServletResponse response) {
    final String username = ((UserDetails) authentication.getPrincipal()).getUsername();
    final Date expiresAt = new Date(System.currentTimeMillis() + expiration.toMillis());

    String token = JWT.create()
            .withSubject(username)
            .withExpiresAt(expiresAt)
            .withIssuer(issuer)
            .sign(Algorithm.HMAC512(secret.getBytes()));

    // Add auth header
    response.addHeader(authorizationHeader, prefix + " " + token);

    return new JwtTokenInfo()
      .setToken(token)
      .setIssuer(issuer)
      .setUsername(username)
      .setExpiredAt(expiresAt);
  }

  /**
   * Verify token. Check if it has JWT format and if not expired
   * @param token JWT token
   * @return Decode JWT token
   */
  public DecodedJWT verifyToken(String token) {
    return this.getJWTVerifier().verify(token);
  }

  /**
   * Extract token from http Authorization header
   * @param authorizationHeader Authorization header value
   * @return JWT Token
   */
  public String extractToken(String authorizationHeader) {
    return authorizationHeader.substring(prefix.length() + 1);
  }
}
