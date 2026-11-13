package com.almis.awe.security.handler;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Logout access control bean
 * Created by pgarcia on 13/06/2019.
 */
@Slf4j
public class AweLogoutHandler extends SecurityContextLogoutHandler {

  /**
   * Header set by the OIDC back-channel logout on its internal logout request: the logout is recorded as started by
   * the identity provider. It only changes the recorded cause.
   */
  public static final String LOGOUT_ORIGIN_HEADER = "X-AWE-Logout-Origin";
  /** Value of {@link #LOGOUT_ORIGIN_HEADER} for logouts started by the identity provider */
  public static final String PROVIDER_ORIGIN = "provider";
  /** Session attribute marking a session ended by a logout (not by expiration) */
  public static final String LOGOUT_SESSION_ATTRIBUTE = AweLogoutHandler.class.getName() + ".logout";

  private final AweSessionDetails sessionDetails;
  private final SecurityAuditService securityAuditService;

  /**
   * Constructor
   *
   * @param sessionDetails session details
   */
  public AweLogoutHandler(AweSessionDetails sessionDetails) {
    this(sessionDetails, SecurityAuditService.disabled());
  }

  /**
   * Constructor recording the logouts in the security audit
   *
   * @param sessionDetails       session details
   * @param securityAuditService security audit service
   */
  public AweLogoutHandler(AweSessionDetails sessionDetails, SecurityAuditService securityAuditService) {
    this.sessionDetails = sessionDetails;
    this.securityAuditService = securityAuditService;
  }

  @Override
  public void logout(HttpServletRequest httpServletRequest, HttpServletResponse response, Authentication authentication) {
    audit(httpServletRequest, authentication);
    if (!response.isCommitted()) {
      sessionDetails.onLogoutSuccess();
    }
  }

  private void audit(HttpServletRequest request, Authentication authentication) {
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      return;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      // The session is about to be invalidated: it must not be recorded again as expired
      session.setAttribute(LOGOUT_SESSION_ATTRIBUTE, Boolean.TRUE);
    }
    boolean provider = PROVIDER_ORIGIN.equalsIgnoreCase(request.getHeader(LOGOUT_ORIGIN_HEADER));
    boolean sso = authentication instanceof OAuth2AuthenticationToken;
    securityAuditService.publish(SecurityAuditEvent.builder()
      .type(SecurityAuditEventType.LOGOUT)
      .outcome(SecurityAuditOutcome.SUCCESS)
      .cause(provider ? SecurityAuditCause.PROVIDER : SecurityAuditCause.EXPLICIT)
      .channel(sso ? SecurityAuditChannel.SSO : null)
      .username(authentication.getName())
      .build());
  }
}
