package com.almis.awe.security.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Access denied handler
 */
@Log4j2
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
