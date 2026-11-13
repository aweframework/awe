package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.handler.AweLogoutHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditSessionListenerTest {

  private final RecordingSecurityAudit audit = new RecordingSecurityAudit();

  @Test
  void anAuthenticatedSessionThatTimedOutIsRecordedAsExpired() {
    MockHttpSession session = session(authenticated("jdoe"));

    listener(session, 31 * 60 * 1000L).onSessionDestroyed(new HttpSessionDestroyedEvent(session));

    assertThat(audit.events()).singleElement().satisfies(event -> {
      assertThat(event.getType()).isEqualTo(SecurityAuditEventType.LOGOUT);
      assertThat(event.getCause()).isEqualTo(SecurityAuditCause.EXPIRED);
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.SYSTEM);
      assertThat(event.getUsername()).isEqualTo("jdoe");
      assertThat(event.getSessionHash()).isEqualTo(SecurityAuditService.hashSessionId(session.getId()));
    });
  }

  @Test
  void aSessionEndedByALogoutIsNotRecordedAgain() {
    MockHttpSession session = session(authenticated("jdoe"));
    session.setAttribute(AweLogoutHandler.LOGOUT_SESSION_ATTRIBUTE, Boolean.TRUE);

    listener(session, 31 * 60 * 1000L).onSessionDestroyed(new HttpSessionDestroyedEvent(session));

    assertThat(audit.events()).isEmpty();
  }

  @Test
  void aSessionInvalidatedBeforeItsTimeoutIsNotRecorded() {
    MockHttpSession session = session(authenticated("jdoe"));

    listener(session, 60 * 1000L).onSessionDestroyed(new HttpSessionDestroyedEvent(session));

    assertThat(audit.events()).isEmpty();
  }

  @Test
  void anonymousSessionsAreNotRecorded() {
    MockHttpSession anonymous = session(new AnonymousAuthenticationToken("key", "anonymousUser",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    MockHttpSession empty = new MockHttpSession();
    empty.setMaxInactiveInterval(1800);

    listener(anonymous, 31 * 60 * 1000L).onSessionDestroyed(new HttpSessionDestroyedEvent(anonymous));
    listener(empty, 31 * 60 * 1000L).onSessionDestroyed(new HttpSessionDestroyedEvent(empty));

    assertThat(audit.events()).isEmpty();
  }

  private SecurityAuditSessionListener listener(MockHttpSession session, long idleMillis) {
    Instant now = Instant.ofEpochMilli(session.getLastAccessedTime() + idleMillis);
    return new SecurityAuditSessionListener(audit.service(), Clock.fixed(now, ZoneOffset.UTC));
  }

  private static MockHttpSession session(Authentication authentication) {
    MockHttpSession session = new MockHttpSession();
    session.setMaxInactiveInterval(1800);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(authentication));
    return session;
  }

  private static Authentication authenticated(String name) {
    return UsernamePasswordAuthenticationToken.authenticated(name, null, AuthorityUtils.NO_AUTHORITIES);
  }
}
