package com.almis.awe.config;

/**
 * How the authorization of a client launch of a maintain target or a query is applied
 * ({@code awe.security.authorization.mode})
 */
public enum AuthorizationMode {
  /**
   * The launch is not checked
   */
  OFF,
  /**
   * A launch that is not authorized is logged and audited, but allowed
   */
  WARN,
  /**
   * A launch that is not authorized is refused
   */
  ENFORCE
}
