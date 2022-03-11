package com.almis.awe.rest.autoconfigure.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "awe.rest.jwt")
public class JWTProperties {
  /**
   * Authentication header name.
   * Default value "Authorization"
   */
  private String authorizationHeader = "Authorization";
  /**
   * JWT token prefix.
   * Default value "Bearer"
   */
  private String prefix = "Bearer";
  /**
   * JWT secret password for sign token.
   * Default ${awe.security.master.key} value
   */
  @Value("${awe.security.master.key:B1Le3s%25abc75TeBe05}")
  private String secret;
  /**
   * JWT issuer name.
   * The Default value is AWE ISSUER
   */
  private String issuer = "AWE ISSUER";
  /**
   * JWT time valid token to expire.
   * You can use Duration format (1ms, 1s, 1m, 1h, 1d, ...) See {@link Duration}
   * The Default value is 60m
   */
  private Duration expirationTime = Duration.ofMinutes(60);
}
