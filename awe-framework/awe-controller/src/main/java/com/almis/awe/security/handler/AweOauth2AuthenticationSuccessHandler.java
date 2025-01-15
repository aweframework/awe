package com.almis.awe.security.handler;

import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.AccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


import java.io.IOException;

public class AweOauth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AccessService accessService;

  public AweOauth2AuthenticationSuccessHandler(AccessService accessService) {
    this.accessService = accessService;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

    DefaultOAuth2User authUser = (DefaultOAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // Manage awe user details from oauth info
    String targetRedirect;
    try {
      targetRedirect = accessService.onAuthenticationSuccess(authUser);
    } catch (AWException ex) {
      throw new AWERuntimeException(ex);
    }

    //set our response to OK status
    response.setStatus(HttpServletResponse.SC_OK);

    // Redirect to user home
    response.sendRedirect(request.getContextPath() + targetRedirect);
  }
}
