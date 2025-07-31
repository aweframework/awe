package com.almis.awe.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * AWE OAuth2 Authentication Failure Handler
 * Handles authentication failures during the OAuth2 login process
 */
@Slf4j
public class AweOauth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
    String message = exception.getMessage();
    log.error("OAuth2 Authentication failed: {}", message);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\": \"" + message + "\"}");
  }
}