package com.almis.awe.model.security.audit;

/**
 * Reason attached to a security event, mainly to failures
 */
public enum SecurityAuditCause {
  /** Wrong user name or password */
  BAD_CREDENTIALS,
  /** The account is locked */
  LOCKED,
  /** The account is disabled */
  DISABLED,
  /** The account has expired */
  ACCOUNT_EXPIRED,
  /** The password has expired */
  CREDENTIALS_EXPIRED,
  /** Local login (user name and password) is switched off */
  LOCAL_LOGIN_DISABLED,
  /** No role sent by the identity provider maps to an AWE profile */
  ROLE_NOT_MAPPED,
  /** The identity provider refused the login or answered with an error */
  PROVIDER_ERROR,
  /** Too many consecutive failed login attempts */
  MAX_ATTEMPTS,
  /** Done by an administrator */
  ADMINISTRATOR,
  /** Requested by the user */
  EXPLICIT,
  /** Requested by the identity provider (OIDC back-channel logout) */
  PROVIDER,
  /** Session timeout */
  EXPIRED,
  /** Wrong second factor code */
  INVALID_CODE,
  /** Malformed or wrongly signed token */
  INVALID_TOKEN,
  /** Token past its expiration date */
  TOKEN_EXPIRED,
  /** The resource is restricted for the user profile */
  RESTRICTED,
  /** The resource is forbidden for the authenticated user */
  FORBIDDEN,
  /** Any other error */
  ERROR
}
