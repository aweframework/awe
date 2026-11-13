package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.handler.AweLogoutHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import java.time.Clock;
import java.util.Objects;

/**
 * Records the authenticated sessions that end because of inactivity (logout {@code EXPIRED}). Sessions ended by a
 * logout are recorded by the logout handler, and sessions invalidated for any other reason before their timeout are
 * not recorded.
 */
public class SecurityAuditSessionListener {

  private static final long TOLERANCE_MILLIS = 1000L;

  private final SecurityAuditService securityAuditService;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param securityAuditService Security audit service
   * @param clock                Clock
   */
  public SecurityAuditSessionListener(SecurityAuditService securityAuditService, Clock clock) {
    this.securityAuditService = securityAuditService;
    this.clock = clock;
  }

  /**
   * A session has been destroyed
   *
   * @param event Session destroyed event
   */
  @EventListener
  public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
    HttpSession session = event.getSession();
    if (!securityAuditService.isEnabled() || session.getAttribute(AweLogoutHandler.LOGOUT_SESSION_ATTRIBUTE) != null
      || !hasTimedOut(session)) {
      return;
    }
    event.getSecurityContexts().stream()
      .map(SecurityContext::getAuthentication)
      .filter(Objects::nonNull)
      .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
      .map(Authentication::getName)
      .findFirst()
      .ifPresent(username -> securityAuditService.publish(SecurityAuditEvent.builder()
        .type(SecurityAuditEventType.LOGOUT)
        .outcome(SecurityAuditOutcome.SUCCESS)
        .cause(SecurityAuditCause.EXPIRED)
        .channel(SecurityAuditChannel.SYSTEM)
        .username(username)
        .sessionHash(SecurityAuditService.hashSessionId(event.getId()))
        .build()));
  }

  private boolean hasTimedOut(HttpSession session) {
    int maxInactive = session.getMaxInactiveInterval();
    if (maxInactive <= 0) {
      return false;
    }
    long idle = clock.millis() - session.getLastAccessedTime();
    return idle + TOLERANCE_MILLIS >= maxInactive * 1000L;
  }
}
