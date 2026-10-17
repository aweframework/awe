package com.almis.awe.security.audit;

import com.almis.awe.config.SecurityAuditConfigProperties;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Publishes security audit events.
 * <p>
 * Every event is completed with the data of the current request (client address, user agent, SHA-256 of the session
 * identifier, correlation identifier, channel and tenant) and sent to every {@link SecurityAuditSink}. Recording is
 * best effort: a failure is logged and never reaches the audited operation. Events published inside a transaction
 * are sent once it commits and dropped if it rolls back, so they only describe changes that really happened.
 */
@Slf4j
public class SecurityAuditService {

  /** Column sizes of the AweSecAudEvt table */
  public static final int MAX_USERNAME = 255;
  public static final int MAX_TENANT = 100;
  public static final int MAX_USER_AGENT = 512;
  public static final int MAX_DETAIL = 1000;

  /** Correlation headers, by priority */
  public static final List<String> CORRELATION_HEADERS = List.of("X-Correlation-Id", "X-Request-Id");
  private static final String TRACE_ID_KEY = "traceId";
  private static final String CORRELATION_ATTRIBUTE = SecurityAuditService.class.getName() + ".correlationId";
  private static final Pattern CORRELATION_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
  private static final String API_PATH = "/api/";

  private final SecurityAuditConfigProperties properties;
  private final List<SecurityAuditSink> sinks;
  private final SecurityAuditTenantResolver tenantResolver;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param properties     Security audit properties
   * @param sinks          Event destinations
   * @param tenantResolver Tenant resolver
   * @param clock          Clock of the event timestamps
   */
  public SecurityAuditService(SecurityAuditConfigProperties properties, List<SecurityAuditSink> sinks,
                              SecurityAuditTenantResolver tenantResolver, Clock clock) {
    this.properties = properties;
    this.sinks = List.copyOf(sinks);
    this.tenantResolver = tenantResolver;
    this.clock = clock;
  }

  /**
   * Service that records nothing, used where no security audit service is available
   *
   * @return Disabled service
   */
  public static SecurityAuditService disabled() {
    SecurityAuditConfigProperties disabled = new SecurityAuditConfigProperties();
    disabled.setEnabled(false);
    return new SecurityAuditService(disabled, List.of(), null, Clock.systemUTC());
  }

  /**
   * Whether events are recorded
   *
   * @return Security audit enabled
   */
  public boolean isEnabled() {
    return properties.isEnabled() && !sinks.isEmpty();
  }

  /**
   * Publish a successful operation
   *
   * @param type     Event type
   * @param username User name (the authenticated user when null)
   * @param detail   Detail (may be null)
   */
  public void success(SecurityAuditEventType type, String username, String detail) {
    publish(SecurityAuditEvent.builder().type(type).outcome(SecurityAuditOutcome.SUCCESS)
      .username(username).detail(detail).build());
  }

  /**
   * Publish a failed or refused operation
   *
   * @param type     Event type
   * @param username User name (the authenticated user when null)
   * @param cause    Failure cause
   * @param detail   Detail (may be null)
   */
  public void failure(SecurityAuditEventType type, String username, SecurityAuditCause cause, String detail) {
    publish(SecurityAuditEvent.builder().type(type).outcome(SecurityAuditOutcome.FAILURE).cause(cause)
      .username(username).detail(detail).build());
  }

  /**
   * Publish an event. Values already set on the event are kept; the missing ones are taken from the current request.
   *
   * @param event Event
   */
  public void publish(SecurityAuditEvent event) {
    if (!isEnabled() || event == null) {
      return;
    }
    SecurityAuditEvent enriched;
    try {
      enriched = enrich(event);
    } catch (RuntimeException exc) {
      log.warn("Security audit event {} could not be prepared and is not recorded", event.getType(), exc);
      return;
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          dispatch(enriched);
        }
      });
    } else {
      dispatch(enriched);
    }
  }

  /**
   * SHA-256 of a session identifier, as stored on the events
   *
   * @param sessionId Session identifier
   * @return Hash in hexadecimal, null when there is no session
   */
  public static String hashSessionId(String sessionId) {
    return sessionId == null ? null : DigestUtils.sha256Hex(sessionId);
  }

  private void dispatch(SecurityAuditEvent event) {
    for (SecurityAuditSink sink : sinks) {
      try {
        sink.record(event);
      } catch (RuntimeException exc) {
        log.warn("Security audit sink {} failed to record a {} event", sink.getClass().getSimpleName(), event.getType(), exc);
      }
    }
  }

  private SecurityAuditEvent enrich(SecurityAuditEvent event) {
    HttpServletRequest request = currentRequest();
    String ipAddress = orElse(event.getIpAddress(), () -> request == null ? null : request.getRemoteAddr());
    return event.toBuilder()
      .id(orElse(event.getId(), () -> UUID.randomUUID().toString()))
      .timestamp(orElse(event.getTimestamp(), clock::instant))
      .outcome(orElse(event.getOutcome(), () -> SecurityAuditOutcome.SUCCESS))
      .username(truncate(orElse(event.getUsername(), SecurityAuditService::authenticatedUser), MAX_USERNAME))
      .tenant(truncate(orElse(event.getTenant(), this::resolveTenant), MAX_TENANT))
      .detail(truncate(event.getDetail(), MAX_DETAIL))
      .channel(orElse(event.getChannel(), () -> channel(request)))
      .correlationId(orElse(event.getCorrelationId(), () -> correlationId(request)))
      .userAgent(truncate(orElse(event.getUserAgent(), () -> request == null ? null : request.getHeader("User-Agent")), MAX_USER_AGENT))
      .sessionHash(orElse(event.getSessionHash(), () -> sessionHash(request)))
      .ipAddress(properties.isAnonymizeIp() ? IpAnonymizer.anonymize(ipAddress) : ipAddress)
      .build();
  }

  /**
   * Value set on the event, or else the computed one
   */
  private static <T> T orElse(T value, Supplier<T> computed) {
    return value != null ? value : computed.get();
  }

  private static String sessionHash(HttpServletRequest request) {
    HttpSession session = request == null ? null : request.getSession(false);
    return session == null ? null : hashSessionId(session.getId());
  }

  private static HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes servletAttributes ? servletAttributes.getRequest() : null;
  }

  private static String authenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    return authentication.getName();
  }

  private String resolveTenant() {
    try {
      return tenantResolver == null ? null : tenantResolver.resolveTenant();
    } catch (RuntimeException exc) {
      log.debug("Security audit tenant not available", exc);
      return null;
    }
  }

  private static SecurityAuditChannel channel(HttpServletRequest request) {
    if (request == null) {
      return SecurityAuditChannel.SYSTEM;
    }
    String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
    String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
    String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
    return path.startsWith(API_PATH) ? SecurityAuditChannel.REST : SecurityAuditChannel.WEB;
  }

  private static String correlationId(HttpServletRequest request) {
    if (request != null) {
      for (String header : CORRELATION_HEADERS) {
        String value = request.getHeader(header);
        if (isValidCorrelationId(value)) {
          return value;
        }
      }
    }
    String traceId = MDC.get(TRACE_ID_KEY);
    if (isValidCorrelationId(traceId)) {
      return traceId;
    }
    if (request == null) {
      return null;
    }
    // One generated identifier shared by every event of the request
    Object generated = request.getAttribute(CORRELATION_ATTRIBUTE);
    if (generated == null) {
      generated = UUID.randomUUID().toString();
      request.setAttribute(CORRELATION_ATTRIBUTE, generated);
    }
    return generated.toString();
  }

  private static boolean isValidCorrelationId(String value) {
    return value != null && CORRELATION_PATTERN.matcher(value).matches();
  }

  private static String truncate(String value, int length) {
    return value == null || value.length() <= length ? value : value.substring(0, length);
  }
}
