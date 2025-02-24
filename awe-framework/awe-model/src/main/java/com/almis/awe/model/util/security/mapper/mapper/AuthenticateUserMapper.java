package com.almis.awe.model.util.security.mapper.mapper;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;


import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

/**
 * Mapper class to retrieve user info
 */
public class AuthenticateUserMapper {

  private AuthenticateUserMapper() {
    // private constructor
  }

  /**
   * Retrieve username from authentication info
   * @param authenticationUserInfo authentication info
   * @return username
   */
  public static String getUserFromAuthentication(Authentication authenticationUserInfo) {
    String username;
    if (authenticationUserInfo instanceof OAuth2AuthenticationToken oauth2Token) {
      username = oauth2Token.getPrincipal().getAttribute(PREFERRED_USERNAME);
    } else if (authenticationUserInfo instanceof UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
      username = usernamePasswordAuthenticationToken.getName();
    } else {
      username = "PUBLIC";
    }
    return username;
  }
}
