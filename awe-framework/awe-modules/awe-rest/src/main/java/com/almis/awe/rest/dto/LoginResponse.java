package com.almis.awe.rest.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * JWT Token info dto. Fill by "/api/authenticate" rest service
 */
@Data
public class LoginResponse {
  @ApiModelProperty(value = "User name", example = "foo" )
  private String username;
  @ApiModelProperty(value = "JWT Token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0IiwiaXNzIjoiQVdFIElTU1VFUiIsImV4cCI6MT")
  private String token;
  @ApiModelProperty(value = "Issuer of authentication token", example = "AWE issuer")
  private String issuer;
  @ApiModelProperty(value = "JWT expiration token", example = "2021-04-26T16:16:18.000+00:00")
  private Date expiresAt;
}
