package com.almis.awe.rest.security;

import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.service.JWTTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A local JWT authenticates the user of the token with the roles of every profile of the user
 */
class JWTAuthorizationFilterTest {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer";

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void localTokenGivesOneRolePerProfileOfTheUser() throws Exception {
    JWTTokenService tokenService = new JWTTokenService(HEADER, PREFIX, "test-secret", "awe", Duration.ofMinutes(5));
    Authentication login = mock(Authentication.class);
    when(login.getPrincipal()).thenReturn(new User("multi", "secret", Collections.emptyList()));
    String token = tokenService.generateToken(login, mock(HttpServletResponse.class)).getToken();
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    AweUserDetails user = new AweUserDetails().setUsername("multi")
      .setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_administrator"),
        new SimpleGrantedAuthority("ROLE_operator")));
    when(userDetailsService.loadUserByUsername("multi")).thenReturn(user);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HEADER)).thenReturn(PREFIX + " " + token);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    new JWTAuthorizationFilter(mock(AuthenticationManager.class), userDetailsService, tokenService)
      .doFilterInternal(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("ROLE_administrator", "ROLE_operator");
    verify(chain).doFilter(request, response);
  }
}
