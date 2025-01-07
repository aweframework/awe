package com.almis.awe.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * Filter every request to enrich log information
 */
@Slf4j
public class AweLoggingFilter implements Filter {

  private final AweSession aweSession;

  private final BaseConfigProperties baseConfigProperties;

  public AweLoggingFilter(AweSession aweSession, BaseConfigProperties baseConfigProperties) {
    this.aweSession = aweSession;
    this.baseConfigProperties = baseConfigProperties;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing.
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    // Add user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (baseConfigProperties.isLogUserEnable() && authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
      // Add username
      String username = aweSession.getUser();
      MDC.put(AweConstants.SESSION_USER, username);
      MDC.put(AweConstants.LOG_BY_USER, username);
      // Add Database name
      String databaseValue = aweSession.getParameter(String.class, AweConstants.SESSION_DATABASE);
      if (databaseValue != null) {
        MDC.put(AweConstants.SESSION_DATABASE, databaseValue);
      }
      // Add Screen name
      String screenValue = aweSession.getParameter(String.class, AweConstants.SESSION_CURRENT_SCREEN);
      if (screenValue != null) {
        MDC.put(AweConstants.SESSION_CURRENT_SCREEN, screenValue);
      }
    }
    // Continue filters
    filterChain.doFilter(servletRequest, servletResponse);

    // Clear info
    MDC.remove(AweConstants.LOG_BY_USER);
    MDC.remove(AweConstants.SESSION_USER);
    MDC.remove(AweConstants.SESSION_DATABASE);
    MDC.remove(AweConstants.SESSION_CURRENT_SCREEN);
  }

  @Override
  public void destroy() {
    // do nothing.
  }
}
