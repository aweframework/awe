package com.almis.awe.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * JWT Token info dto. Fill by "/api/authenticate" rest service
 */
@Data
public class LoginResponse {
  @Schema(title = "User name", example = "foo" )
  private String username;
  @Schema(title = "JWT Token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0IiwiaXNzIjoiQVdFIElTU1VFUiIsImV4cCI6MT")
  private String token;
  @Schema(title = "Issuer of authentication token", example = "AWE issuer")
  private String issuer;
  @Schema(title = "JWT expiration token", example = "2021-04-26T16:16:18.000+00:00")
  private Date expiresAt;
}
