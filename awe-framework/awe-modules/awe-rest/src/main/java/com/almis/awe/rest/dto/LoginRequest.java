package com.almis.awe.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Login request info. Used by "/api/authenticate" rest service to get a session token
 */
@Data
@Accessors(chain = true)
public class LoginRequest {
  @NotBlank(message = "The username is required.")
  @Schema(title = "User name", example = "foo" )
  private String username;
  @NotBlank(message = "The password is required.")
  @Schema(title = "User password", example = "too" )
  private String password;
}
