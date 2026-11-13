package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Automatic account lockout properties
 */
@Data
@ConfigurationProperties(prefix = "awe.security.lockout")
public class LockoutConfigProperties {
  /**
   * Consecutive failed local logins (wrong password) after which the account is locked. A successful login resets the
   * count, and an administrator unlocks the account from the user screen. 0 switches the automatic lockout off.
   * Default value 0
   */
  private int maxAttempts = 0;
}
