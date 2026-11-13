package com.almis.awe.security.handler;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.RecordingSecurityAudit;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AweLogoutHandlerTest {

  private final RecordingSecurityAudit audit = new RecordingSecurityAudit();
  private final AweSessionDetails sessionDetails = mock(AweSessionDetails.class);
  private final AweLogoutHandler handler = new AweLogoutHandler(sessionDetails, audit.service());

  @Test
  void anExplicitLogoutIsRecordedAndMarksTheSession() {
    MockHttpServletRequest request = request();

    handler.logout(request, new MockHttpServletResponse(), UsernamePasswordAuthenticationToken.authenticated("jdoe", null, AuthorityUtils.NO_AUTHORITIES));

    assertThat(audit.events()).singleElement().satisfies(event -> {
      assertThat(event.getType()).isEqualTo(SecurityAuditEventType.LOGOUT);
      assertThat(event.getCause()).isEqualTo(SecurityAuditCause.EXPLICIT);
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.WEB);
      assertThat(event.getUsername()).isEqualTo("jdoe");
    });
    assertThat(request.getSession().getAttribute(AweLogoutHandler.LOGOUT_SESSION_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
    verify(sessionDetails).onLogoutSuccess();
  }

  @Test
  void aLogoutStartedByTheIdentityProviderIsRecordedAsSuch() {
    MockHttpServletRequest request = request();
    request.addHeader(AweLogoutHandler.LOGOUT_ORIGIN_HEADER, AweLogoutHandler.PROVIDER_ORIGIN);
    OAuth2AuthenticationToken sso = new OAuth2AuthenticationToken(
      new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, Map.of("sub", "jdoe"), "sub"), AuthorityUtils.NO_AUTHORITIES, "keycloak");

    handler.logout(request, new MockHttpServletResponse(), sso);

    assertThat(audit.events()).singleElement().satisfies(event -> {
      assertThat(event.getCause()).isEqualTo(SecurityAuditCause.PROVIDER);
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.SSO);
      assertThat(event.getUsername()).isEqualTo("jdoe");
    });
  }

  @Test
  void anonymousLogoutsAreNotRecorded() {
    MockHttpServletRequest request = request();

    handler.logout(request, new MockHttpServletResponse(), null);
    handler.logout(request, new MockHttpServletResponse(), new AnonymousAuthenticationToken("key", "anonymousUser",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(audit.events()).isEmpty();
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/action/logout");
    request.setSession(new MockHttpSession());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    return request;
  }
}
