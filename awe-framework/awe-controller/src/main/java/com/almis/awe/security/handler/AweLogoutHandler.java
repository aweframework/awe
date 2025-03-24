package com.almis.awe.security.handler;

import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Logout access control bean
 * Created by pgarcia on 13/06/2019.
 */
@Slf4j
public class AweLogoutHandler extends SecurityContextLogoutHandler {

  private final AweSessionDetails sessionDetails;

  /**
   * Constructor
   *
   * @param sessionDetails session details
   */
  public AweLogoutHandler(AweSessionDetails sessionDetails) {
    this.sessionDetails = sessionDetails;
  }

  @Override
  public void logout(HttpServletRequest httpServletRequest, HttpServletResponse response, Authentication authentication) {
    if (!response.isCommitted()) {
      setClearAuthentication(true);
      setInvalidateHttpSession(true);
      sessionDetails.onLogoutSuccess();
      try {
        httpServletRequest.getRequestDispatcher("/action/logout-redirect").forward(httpServletRequest, response);
      } catch (Exception exc) {
        log.error("Error redirecting logout handler", exc);
      }
    }
  }
}
