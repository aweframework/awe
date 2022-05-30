package com.almis.awe.model.type;

public enum SecondFactorStatusType {
  /**
   * 2FA is not enabled.
   */
  DISABLED,
  /**
   * 2FA is enabled for users if they want to configure it.
   */
  OPTIONAL,
  /**
   * 2FA is required for all users.
   */
  FORCE
}
