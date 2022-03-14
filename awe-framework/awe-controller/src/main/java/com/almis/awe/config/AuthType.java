package com.almis.awe.config;

/**
 * Authentication type Enum
 */
public enum AuthType {
  /**
   * Ldap authentication
   */
  LDAP,
  /**
   * Database authentication
   */
  BBDD,
  /**
   * Memory authentication
   */
  IN_MEMORY,
  /**
   * Custom authentication
   */
  CUSTOM
}
