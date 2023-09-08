package com.almis.awe.security.handler;

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

  @Override
  public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException exception) throws IOException, ServletException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      log.error("Forbidden. User: [{}] attempted to access the protected URL: [{}]", authentication.getName(), httpServletRequest.getRequestURI());
    }
    // Send 403 Error
    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
  }
}
