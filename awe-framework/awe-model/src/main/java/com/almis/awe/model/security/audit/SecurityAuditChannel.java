package com.almis.awe.model.security.audit;

/**
 * Entry point through which the audited operation arrived
 */
public enum SecurityAuditChannel {
  /** Web application (local login and screens) */
  WEB,
  /** Single sign-on through an OAuth2 / OIDC identity provider */
  SSO,
  /** REST API */
  REST,
  /** Raised by the server itself, outside any request (session expiration) */
  SYSTEM
}
