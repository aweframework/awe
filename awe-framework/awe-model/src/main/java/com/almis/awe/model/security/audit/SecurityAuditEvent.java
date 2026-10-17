package com.almis.awe.model.security.audit;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Security audit event: an authentication, session, account or access fact recorded by the framework.
 * <p>
 * Request data (address, agent, session, correlation identifier and tenant) is filled in by the audit service when the
 * event is published; values already set on the event are kept.
 */
@Value
@Builder(toBuilder = true)
public class SecurityAuditEvent {
  /** Unique identifier */
  String id;
  /** Moment of the event */
  Instant timestamp;
  /** Event kind */
  SecurityAuditEventType type;
  /** Result */
  SecurityAuditOutcome outcome;
  /** Reason, mainly for failures (may be null) */
  SecurityAuditCause cause;
  /** User name involved, as typed or resolved (may be null) */
  String username;
  /** Tenant: SSO tenant or session database alias (may be null) */
  String tenant;
  /** Entry point */
  SecurityAuditChannel channel;
  /** Client address, anonymized when configured (may be null) */
  String ipAddress;
  /** Client user agent (may be null) */
  String userAgent;
  /** SHA-256 of the session identifier, never the identifier itself (may be null) */
  String sessionHash;
  /** Identifier correlating the events of one request (may be null) */
  String correlationId;
  /** Free detail: screen, option, URL, profile... (may be null) */
  String detail;
}
