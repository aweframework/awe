package com.almis.awe.model.security.audit;

/**
 * Kind of security event recorded by the security audit
 */
public enum SecurityAuditEventType {
  /** Login attempt (local, REST or SSO) */
  LOGIN,
  /** Session end: explicit, started by the identity provider or expired */
  LOGOUT,
  /** Account locked, automatically after too many failed attempts or by an administrator */
  ACCOUNT_LOCKED,
  /** Account unlocked by an administrator */
  ACCOUNT_UNLOCKED,
  /** Password changed by its owner */
  PASSWORD_CHANGE,
  /** Password reset by an administrator */
  PASSWORD_RESET,
  /** Second factor (TOTP) secret enrolled */
  TOTP_ENROLLMENT,
  /** Second factor (TOTP) code checked */
  TOTP_VERIFICATION,
  /** Second factor (TOTP) switched off by its owner or reset by an administrator */
  TOTP_RESET,
  /** User created from the identity provider data */
  SSO_PROVISIONING,
  /** User profile synchronized from the identity provider roles */
  SSO_PROFILE_SYNC,
  /** Access to a screen, option or URL refused */
  ACCESS_DENIED,
  /** REST token issued */
  TOKEN_ISSUED,
  /** REST token refused */
  TOKEN_REJECTED
}
