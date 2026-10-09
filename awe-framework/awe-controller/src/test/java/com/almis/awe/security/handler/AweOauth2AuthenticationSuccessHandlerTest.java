package com.almis.awe.security.handler;

import com.almis.awe.service.AccessService;
import com.almis.awe.service.ErrorPageService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AweOauth2AuthenticationSuccessHandlerTest {

  @Mock
  private AccessService accessService;

  @Mock
  private AweOauth2AuthenticationFailureHandler failureHandler;

  private AweOauth2AuthenticationSuccessHandler handler;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockHttpSession session;
  private OAuth2AuthenticationToken token;

  @BeforeEach
  void setUp() {
    handler = new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler);
    session = new MockHttpSession();
    request = new MockHttpServletRequest();
    request.setSession(session);
    response = new MockHttpServletResponse();
    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    token = new OAuth2AuthenticationToken(new DefaultOAuth2User(authorities, Map.of("sub", "foo"), "sub"), authorities, "clientRegId");
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(token);
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void givenAllowedUser_onAuthenticationSuccess_redirectsToInitialUrl() throws Exception {
    when(accessService.onAuthenticationSuccess(token)).thenReturn("/app/home");

    handler.onAuthenticationSuccess(request, response, token);

    assertEquals("/app/home", response.getRedirectedUrl());
    assertFalse(session.isInvalid());
    assertSame(token, SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(failureHandler);
  }

  @Test
  void givenDisabledUser_onAuthenticationSuccess_dropsAuthenticationAndDelegatesToFailureHandler() throws Exception {
    DisabledException exception = new DisabledException("disabled message");
    when(accessService.onAuthenticationSuccess(token)).thenThrow(exception);

    handler.onAuthenticationSuccess(request, response, token);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(session.isInvalid());
    assertNull(response.getRedirectedUrl());
    verify(failureHandler).onAuthenticationFailure(request, response, exception);
  }

  @Test
  void givenLockedUser_onAuthenticationSuccess_dropsAuthenticationAndDelegatesToFailureHandler() throws Exception {
    LockedException exception = new LockedException("locked message");
    when(accessService.onAuthenticationSuccess(token)).thenThrow(exception);

    handler.onAuthenticationSuccess(request, response, token);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(session.isInvalid());
    verify(failureHandler).onAuthenticationFailure(request, response, exception);
  }

  @Test
  void givenDisabledUser_withRealFailureHandler_errorPageShowsMessageAfterSessionIsInvalidated() throws Exception {
    ErrorPageService errorPageService = mock(ErrorPageService.class);
    when(errorPageService.generateErrorPageFromTemplate(null, "disabled message")).thenReturn("<html>disabled message</html>");
    handler = new AweOauth2AuthenticationSuccessHandler(accessService, new AweOauth2AuthenticationFailureHandler(errorPageService));
    when(accessService.onAuthenticationSuccess(token)).thenThrow(new DisabledException("disabled message"));

    handler.onAuthenticationSuccess(request, response, token);

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertEquals("<html>disabled message</html>", response.getContentAsString());
  }

  @Test
  @SuppressWarnings("removal")
  void givenDisabledUser_withDeprecatedConstructor_dropsAuthenticationAndSendsUnauthorized() throws Exception {
    handler = new AweOauth2AuthenticationSuccessHandler(accessService);
    when(accessService.onAuthenticationSuccess(token)).thenThrow(new DisabledException("disabled message"));

    handler.onAuthenticationSuccess(request, response, token);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(session.isInvalid());
    assertNull(response.getRedirectedUrl());
    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertEquals("disabled message", response.getErrorMessage());
  }

  @Test
  @SuppressWarnings("removal")
  void givenAllowedUser_withDeprecatedConstructor_redirectsToInitialUrl() throws Exception {
    handler = new AweOauth2AuthenticationSuccessHandler(accessService);
    when(accessService.onAuthenticationSuccess(token)).thenReturn("/app/home");

    handler.onAuthenticationSuccess(request, response, token);

    assertEquals("/app/home", response.getRedirectedUrl());
    assertFalse(session.isInvalid());
  }
}
