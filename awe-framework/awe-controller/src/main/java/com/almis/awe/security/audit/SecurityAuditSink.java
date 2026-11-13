package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditEvent;

/**
 * Destination of security audit events. Every sink bean receives every published event; a sink that fails does not
 * stop the others nor the audited operation.
 */
@FunctionalInterface
public interface SecurityAuditSink {

  /**
   * Record an event
   *
   * @param event Event, already enriched with the request data
   */
  void record(SecurityAuditEvent event);
}
