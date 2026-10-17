package com.almis.awe.security.handler;

import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.audit.SecurityAuditCauses;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ErrorPageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Handler for managing OAuth2 authentication failures.
 * Implements the {@link AuthenticationFailureHandler} interface to define custom behavior
 * when OAuth2 authentication fails during the authentication process.
 * <p>
 * Responsibilities:
 * - Logs the reason for authentication failure.
 * - Generates and serves a custom error page using {@link ErrorPageService}.
 * - Sets HTTP response status as unauthorized and displays the generated error page.
 */
@Slf4j
public class AweOauth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final ErrorPageService errorPageService;
  private SecurityAuditService securityAuditService = SecurityAuditService.disabled();

  public AweOauth2AuthenticationFailureHandler(ErrorPageService errorPageService) {
		this.errorPageService = errorPageService;
  }

  /**
   * Record the refused SSO logins in the security audit
   *
   * @param securityAuditService Security audit service
   */
  public void setSecurityAuditService(SecurityAuditService securityAuditService) {
    this.securityAuditService = securityAuditService;
  }

	@Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
    String message = exception.getMessage();
    log.error("OAuth2 Authentication failed: {}", message);
    Object username = request.getAttribute(AccessService.SSO_REJECTED_USERNAME_ATTRIBUTE);
    securityAuditService.publish(SecurityAuditEvent.builder()
      .type(SecurityAuditEventType.LOGIN)
      .outcome(SecurityAuditOutcome.FAILURE)
      .cause(SecurityAuditCauses.of(exception))
      .channel(SecurityAuditChannel.SSO)
      .username(username == null ? null : username.toString())
      .detail(exception instanceof OAuth2AuthenticationException oauth2 ? oauth2.getError().getErrorCode() : null)
      .build());

    // Show error page
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("text/html; charset=UTF-8");
    String htmlErrorPage = errorPageService.generateErrorPageFromTemplate(null, message);
    response.getWriter().write(htmlErrorPage);
  }
}
