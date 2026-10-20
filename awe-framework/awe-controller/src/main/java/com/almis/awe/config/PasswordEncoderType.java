package com.almis.awe.config;

/**
 * Algorithm used to hash new user passwords. Stored hashes of every algorithm keep validating.
 */
public enum PasswordEncoderType {
  /**
   * BCrypt, stored with the {@code {bcrypt}} prefix (default)
   */
  BCRYPT,
  /**
   * Legacy RIPEMD-160 of AWE 4, without salt, stored without prefix. Only for applications whose own code or external
   * systems still compare the stored password hashes
   */
  RIPEMD160
}
