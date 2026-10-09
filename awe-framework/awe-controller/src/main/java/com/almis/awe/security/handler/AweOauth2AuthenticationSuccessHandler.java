package com.almis.awe.security.handler;

import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.AccessService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.io.IOException;

/**
 * Handles a successful OAuth2 authentication: creates the AWE session and redirects to the initial screen.
 * <p>
 * Users that the identity provider accepts but AWE refuses because of their account status (disabled or locked)
 * are rejected: the authentication and the HTTP session are discarded and the failure is delegated to the
 * failure handler. Account expiry is not checked because AWE has no account-expiry column, and password expiry
 * does not apply to SSO because the password is not used.
 */
public class AweOauth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AccessService accessService;
  private final AuthenticationFailureHandler failureHandler;

  /**
   * Constructor with the handler that reports a rejected login
   *
   * @param accessService  Access service
   * @param failureHandler Failure handler used when the user is rejected by AWE
   */
  public AweOauth2AuthenticationSuccessHandler(AccessService accessService, AuthenticationFailureHandler failureHandler) {
    this.accessService = accessService;
    this.failureHandler = failureHandler;
  }

  /**
   * Constructor kept for backward compatibility. A rejected user gets an HTTP 401 with the rejection message,
   * after the session is discarded. A redirect to the login page is avoided on purpose: with SSO auto launch the
   * login page starts the identity provider flow again, which would loop for a user AWE keeps refusing.
   *
   * @param accessService Access service
   * @deprecated use {@link #AweOauth2AuthenticationSuccessHandler(AccessService, AuthenticationFailureHandler)}
   */
  @Deprecated(since = "5.0.0", forRemoval = true)
  public AweOauth2AuthenticationSuccessHandler(AccessService accessService) {
    this(accessService, (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage()));
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

    // Manage awe user details from oauth info
    String targetRedirect;
    try {
      targetRedirect = accessService.onAuthenticationSuccess(oauth2Token);
    } catch (AccountStatusException ex) {
      // The identity provider accepted the user but AWE refuses its account status (disabled, locked...)
      rejectLogin(request, response, ex);
      return;
    } catch (AWException ex) {
      throw new AWERuntimeException(ex);
    }

    //set our response to OK status
    response.setStatus(HttpServletResponse.SC_OK);

    // Redirect to user home
    response.sendRedirect(request.getContextPath() + targetRedirect);
  }

  /**
   * Drops the already stored authentication (security context and session) and reports the failure,
   * so a rejected user is never left half logged in
   */
  private void rejectLogin(HttpServletRequest request, HttpServletResponse response, AccountStatusException exception) throws IOException, ServletException {
    new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    failureHandler.onAuthenticationFailure(request, response, exception);
  }
}
