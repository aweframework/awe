package com.almis.awe.security.authentication.entrypoint;

import com.almis.awe.session.AweSessionDetails;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Action entry point.
 * Handle exceptions in actions request (/action/**).
 */
@Log4j2
public class ActionAuthenticationEntryPoint implements AuthenticationEntryPoint {

  // Constants
  private static final String SESSION_EXPIRED = "ERROR_MESSAGE_SESSION_EXPIRED";

  // Autowired services
  private final AweSessionDetails sessionDetails;

  /**
   * ActionAuthenticationEntryPoint constructor
   *
   * @param sessionDetails Access service
   */
  public ActionAuthenticationEntryPoint(AweSessionDetails sessionDetails) {
    this.sessionDetails = sessionDetails;
  }

  @Override
  public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException exception) throws IOException, ServletException {
    log.info("Action entryPoint. Invalid session expired [{}]", httpServletRequest.getSession().getId());

    // Clear websocket connections
    sessionDetails.onLogoutSuccess();

    // Send 401 Error
    httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, SESSION_EXPIRED);
  }
}
