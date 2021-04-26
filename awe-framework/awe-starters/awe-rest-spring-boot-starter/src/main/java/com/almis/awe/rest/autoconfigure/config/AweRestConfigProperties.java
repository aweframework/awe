package com.almis.awe.rest.autoconfigure.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AWE Rest module configuration properties
 */
@ConfigurationProperties(prefix = "awe.rest.jwt")
@Data
public class AweRestConfigProperties {
  /**
   * Authentication header name.
   * Default value "Authorization"
   */
  private String authorizationHeader = "Authorization";
  /**
   * JWT token prefix.
   * Default value ""
   */
  private String jwtPrefix = "";
  /**
   * JWT secret password for sign token.
   * Default ${security.master.key} value
   */
  @Value("${security.master.key}")
  private String jwtSecret;
  /**
   * JWT issuer name.
   * The Default value is AWE ISSUER
   */
  private String jwtIssuer = "AWE ISSUER";
  /**
   * JWT time valid token to expire.
   * You can use Duration format (1ms, 1s, 1m, 1h, 1d, ...) See {@link Duration}
   * The Default value is 60m
   */
  private Duration jwtExpirationTime = Duration.ofMinutes(60);
}
