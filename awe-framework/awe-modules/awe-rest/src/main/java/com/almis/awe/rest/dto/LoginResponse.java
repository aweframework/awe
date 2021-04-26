package com.almis.awe.rest.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * JWT Token info dto. Fill by "/api/login" rest service
 */
@Data
public class LoginResponse {
  @ApiModelProperty(value = "User name")
  private String username;
  @ApiModelProperty(value = "JWT Token")
  private String token;
  @ApiModelProperty(value = "Issuer of authentication token")
  private String issuer;
  @ApiModelProperty(value = "JWT expiration token")
  private Date expiresAt;
}
