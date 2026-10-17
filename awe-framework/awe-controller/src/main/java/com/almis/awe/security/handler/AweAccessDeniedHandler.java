package com.almis.awe.security.handler;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.SecurityAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Access denied handler
 */
@Slf4j
public class AweAccessDeniedHandler implements AccessDeniedHandler {

  // Constants
  private static final String FORBIDDEN = "ERROR_MESSAGE_FORBIDDEN_ACCESS";
  private SecurityAuditService securityAuditService = SecurityAuditService.disabled();

  /**
   * Record the denied accesses in the security audit
   *
   * @param securityAuditService Security audit service
   */
  public void setSecurityAuditService(SecurityAuditService securityAuditService) {
    this.securityAuditService = securityAuditService;
  }

  @Override
  public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException exception) throws IOException, ServletException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      log.error("Forbidden. User: [{}] attempted to access the protected URL: [{}]", authentication.getName(), httpServletRequest.getRequestURI());
    }
    securityAuditService.failure(SecurityAuditEventType.ACCESS_DENIED, authentication == null ? null : authentication.getName(),
      SecurityAuditCause.FORBIDDEN, "url: " + httpServletRequest.getRequestURI());
    // Send 403 Error
    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
  }
}
