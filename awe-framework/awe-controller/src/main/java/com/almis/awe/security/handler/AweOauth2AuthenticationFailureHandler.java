package com.almis.awe.security.handler;

import com.almis.awe.service.ErrorPageService;
import com.almis.awe.model.type.ErrorTypology;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
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

  @Autowired
  public AweOauth2AuthenticationFailureHandler(ErrorPageService errorPageService) {
		this.errorPageService = errorPageService;
  }

	@Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
    String message = exception.getMessage();
    log.error("OAuth2 Authentication failed: {}", message);

    // Show error page
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("text/html; charset=UTF-8");
    String htmlErrorPage = errorPageService.generateErrorPageFromTemplate(ErrorTypology.AUTHENTICATION, null, message);
    response.getWriter().write(htmlErrorPage);
  }
}