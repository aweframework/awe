package com.almis.awe.config;

/**
 * Authentication type Enum
 */
public enum AuthType {
  /**
   * Ldap authentication
   */
  LDAP("ldap"),
  /**
   * Database authentication
   */
  BBDD("bbdd"),
  /**
   * Memory authentication
   */
  IN_MEMORY("in_memory"),
  /**
   * Custom authentication
   */
  CUSTOM("custom");

  private final String mode;

  AuthType(String mode) {
    this.mode = mode;
  }

  public static AuthType fromValue(String value) {
    if (value.equalsIgnoreCase(LDAP.getValue())) {
      return LDAP;
    } else if (value.equalsIgnoreCase(BBDD.getValue())) {
      return BBDD;
    } else if (value.equalsIgnoreCase(IN_MEMORY.getValue())) {
      return IN_MEMORY;
    } else if (value.equalsIgnoreCase(CUSTOM.getValue())) {
      return CUSTOM;
    }
    return null;
  }

  public String getValue() {
    return mode;
  }
}
