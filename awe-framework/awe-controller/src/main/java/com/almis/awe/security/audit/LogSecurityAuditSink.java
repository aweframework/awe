package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes every security audit event as one JSON line on the {@value #LOGGER_NAME} logger (INFO level), so it can be
 * routed to a dedicated appender and shipped to a SIEM. Null values are left out.
 */
public class LogSecurityAuditSink implements SecurityAuditSink {

  /** Logger receiving the events */
  public static final String LOGGER_NAME = "awe.security.audit";

  private static final Logger AUDIT_LOG = LoggerFactory.getLogger(LOGGER_NAME);
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Override
  public void record(SecurityAuditEvent event) {
    if (AUDIT_LOG.isInfoEnabled()) {
      AUDIT_LOG.info(mapper.writeValueAsString(toMap(event)));
    }
  }

  private static Map<String, Object> toMap(SecurityAuditEvent event) {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "timestamp", event.getTimestamp());
    put(values, "id", event.getId());
    put(values, "type", event.getType());
    put(values, "outcome", event.getOutcome());
    put(values, "cause", event.getCause());
    put(values, "username", event.getUsername());
    put(values, "tenant", event.getTenant());
    put(values, "channel", event.getChannel());
    put(values, "ipAddress", event.getIpAddress());
    put(values, "userAgent", event.getUserAgent());
    put(values, "sessionHash", event.getSessionHash());
    put(values, "correlationId", event.getCorrelationId());
    put(values, "detail", event.getDetail());
    return values;
  }

  private static void put(Map<String, Object> values, String key, Object value) {
    if (value != null) {
      values.put(key, value.toString());
    }
  }
}
