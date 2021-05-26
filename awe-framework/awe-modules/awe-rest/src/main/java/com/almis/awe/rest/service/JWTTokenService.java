package com.almis.awe.rest.service;

import com.almis.awe.config.ServiceConfig;
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
  private String jwtPrefix;
  private String jwtSecret;
  private String jwtIssuer;
  private Duration jwtExpiration;

  // Fields
  private JWTVerifier jwtVerifier;

  /**
   * JWTTokenService constructor
   * @param authorizationHeader HTTP Auth header name
   * @param jwtPrefix JWT token prefix
   * @param jwtSecret JWT token secret
   * @param jwtIssuer JWT token issuer
   * @param jwtExpirationTime JWT token expiration
   */
  public JWTTokenService(String authorizationHeader, String jwtPrefix, String jwtSecret, String jwtIssuer, Duration jwtExpirationTime) {
    this.authorizationHeader = authorizationHeader;
    this.jwtPrefix = jwtPrefix;
    this.jwtSecret = jwtSecret;
    this.jwtIssuer = jwtIssuer;
    this.jwtExpiration = jwtExpirationTime;
  }

  /**
   * Get JWT verifier
   */
  public JWTVerifier getJWTVerifier() {
    return Optional.ofNullable(jwtVerifier)
            .orElse(JWT.require(Algorithm.HMAC512(jwtSecret)).withIssuer(jwtIssuer).build());
  }

  /**
   * Generate JWT Token and add it to http response as header
   * @param authentication Authentication object
   * @param response {@link HttpServletResponse}
   */
  public void generateToken(Authentication authentication, HttpServletResponse response) {
    String token = JWT.create()
            .withSubject(((UserDetails) authentication.getPrincipal()).getUsername())
            .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration.toMillis()))
            .withIssuer(jwtIssuer)
            .sign(Algorithm.HMAC512(jwtSecret.getBytes()));

    // Add auth header
    response.addHeader(authorizationHeader, jwtPrefix + token);
  }

  /**
   * Verify token. Check if has JWT format and if not expired
   * @param token JWT token
   * @return Decode JWT token
   */
  public DecodedJWT verifyToken(String token) {
    return this.getJWTVerifier().verify(token);
  }
}
