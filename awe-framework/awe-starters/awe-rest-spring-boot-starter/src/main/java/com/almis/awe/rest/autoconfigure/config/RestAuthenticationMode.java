package com.almis.awe.rest.autoconfigure.config;

/**
 * Authentication mode for awe-rest api endpoints.
 */
public enum RestAuthenticationMode {
  /**
   * Existing local AWE JWT authentication mode.
   */
  LOCAL_JWT,
  /**
   * OAuth2 Resource Server mode for externally issued access tokens.
   */
  OAUTH2_RESOURCE_SERVER
}
