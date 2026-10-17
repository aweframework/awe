package com.almis.awe.security.audit;

import com.almis.awe.config.SecurityAuditConfigProperties;
import com.almis.awe.model.security.audit.SecurityAuditEvent;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Security audit service whose events are kept in memory, for tests
 */
public class RecordingSecurityAudit {

  private final List<SecurityAuditEvent> events = new ArrayList<>();
  private final SecurityAuditService service = new SecurityAuditService(new SecurityAuditConfigProperties(),
    List.of(events::add), () -> null, Clock.systemUTC());

  public SecurityAuditService service() {
    return service;
  }

  public List<SecurityAuditEvent> events() {
    return events;
  }
}
