package com.almis.awe.observability.instrumentation;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

class AweConnectionMetricsTest {

  private final DefaultSimpUserRegistry userRegistry = new DefaultSimpUserRegistry();
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

  @Test
  @DisplayName("The gauges read the live WebSocket sessions on every scrape: connected users and open connections")
  void gaugesFollowTheSessions() {
    new AweConnectionMetrics(userRegistry).bindTo(registry);

    assertThat(users()).isZero();
    assertThat(connections()).isZero();

    connect("s1", user("jdoe"));
    connect("s2", user("jdoe"));
    connect("s3", user("asmith"));

    assertThat(users()).isEqualTo(2.0);
    assertThat(connections()).isEqualTo(3.0);

    disconnect("s3", user("asmith"));

    assertThat(users()).as("A user whose WebSocket closed is not connected").isEqualTo(1.0);
    assertThat(connections()).isEqualTo(2.0);

    disconnect("s1", user("jdoe"));

    assertThat(users()).isEqualTo(1.0);
    assertThat(connections()).as("A closed tab drops its connection").isEqualTo(1.0);
  }

  @Test
  @DisplayName("The login page (PUBLIC) and anonymous sessions are neither users nor connections")
  void publicAndAnonymousAreNotCounted() {
    new AweConnectionMetrics(userRegistry).bindTo(registry);

    connect("s1", user("PUBLIC"));
    connect("s2", user("anonymousUser"));
    connect("s3", new AnonymousAuthenticationToken("key", "guest", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    connect("s4", null);

    assertThat(users()).isZero();
    assertThat(connections()).isZero();

    connect("s5", user("jdoe"));

    assertThat(users()).isEqualTo(1.0);
    assertThat(connections()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("The gauges carry no user tag")
  void noUserTags() {
    new AweConnectionMetrics(userRegistry).bindTo(registry);
    connect("s1", user("jdoe"));

    assertThat(registry.get(AweConnectionMetrics.USERS_METER).gauge().getId().getTags()).isEmpty();
    assertThat(registry.get(AweConnectionMetrics.CONNECTIONS_METER).gauge().getId().getTags()).isEmpty();
  }

  private void connect(String sessionId, Principal user) {
    userRegistry.onApplicationEvent(new SessionConnectedEvent(this, message(sessionId), user));
  }

  private void disconnect(String sessionId, Principal user) {
    userRegistry.onApplicationEvent(new SessionDisconnectEvent(this, message(sessionId), sessionId,
      CloseStatus.GOING_AWAY, user));
  }

  private static Message<byte[]> message(String sessionId) {
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionId(sessionId);
    return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
  }

  private static Principal user(String name) {
    return () -> name;
  }

  private double users() {
    return registry.get(AweConnectionMetrics.USERS_METER).gauge().value();
  }

  private double connections() {
    return registry.get(AweConnectionMetrics.CONNECTIONS_METER).gauge().value();
  }
}
