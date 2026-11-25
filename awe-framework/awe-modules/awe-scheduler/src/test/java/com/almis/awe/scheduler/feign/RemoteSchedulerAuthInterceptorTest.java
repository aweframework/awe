package com.almis.awe.scheduler.feign;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import feign.RequestTemplate;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteSchedulerAuthInterceptorTest {

  private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

  private final AtomicReference<Instant> now = new AtomicReference<>(NOW);
  private final Clock clock = new Clock() {
    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  };
  private MockWebServer server;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  private RemoteSchedulerAuthInterceptor interceptor(String user, String password) {
    return new RemoteSchedulerAuthInterceptor(server.url("/").toString() + "scheduler/api/v1", user, password, clock);
  }

  private MockResponse login(String token, String expiresAt) {
    String expires = expiresAt == null ? "" : ",\"expiresAt\":\"" + expiresAt + "\"";
    return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
      .body("{\"username\":\"sched\",\"token\":\"" + token + "\"" + expires + "}").build();
  }

  private String call(RemoteSchedulerAuthInterceptor interceptor) {
    RequestTemplate template = new RequestTemplate();
    interceptor.apply(template);
    return template.headers().containsKey("Authorization")
      ? template.headers().get("Authorization").iterator().next() : null;
  }

  @Test
  void addsTheBearerTokenObtainedFromTheLoginOfTheSchedulerInstance() throws Exception {
    server.enqueue(login("t1", "2030-01-01T01:00:00.000+00:00"));

    String header = call(interceptor("sched", "s3cret"));

    assertThat(header).isEqualTo("Bearer t1");
    RecordedRequest recorded = server.takeRequest();
    assertThat(recorded.getMethod()).isEqualTo("POST");
    assertThat(recorded.getTarget()).isEqualTo("/api/authenticate");
    assertThat(recorded.getBody().utf8()).contains("\"username\":\"sched\"").contains("\"password\":\"s3cret\"");
  }

  @Test
  void logsInOnlyOnceWhileTheTokenIsValid() {
    server.enqueue(login("t1", "2030-01-01T01:00:00.000+00:00"));
    RemoteSchedulerAuthInterceptor interceptor = interceptor("sched", "s3cret");

    call(interceptor);
    now.set(NOW.plusSeconds(1800));

    assertThat(call(interceptor)).isEqualTo("Bearer t1");
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  void logsInAgainWhenTheTokenIsAboutToExpire() {
    server.enqueue(login("t1", "2030-01-01T01:00:00.000+00:00"));
    server.enqueue(login("t2", "2030-01-01T03:00:00.000+00:00"));
    RemoteSchedulerAuthInterceptor interceptor = interceptor("sched", "s3cret");

    call(interceptor);
    now.set(NOW.plusSeconds(3600 - 10));

    assertThat(call(interceptor)).isEqualTo("Bearer t2");
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test
  void cachesTheTokenUntilTheExpirationOfItsJwtWhenTheLoginGivesNone() {
    String jwt = JWT.create().withExpiresAt(NOW.plusSeconds(3600)).sign(Algorithm.HMAC256("key"));
    server.enqueue(login(jwt, null));
    RemoteSchedulerAuthInterceptor interceptor = interceptor("sched", "s3cret");

    call(interceptor);
    now.set(NOW.plusSeconds(1800));

    assertThat(call(interceptor)).isEqualTo("Bearer " + jwt);
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  void logsInOnEveryCallWhenTheLoginGivesNoExpiration() {
    server.enqueue(login("t1", null));
    server.enqueue(login("t2", null));
    RemoteSchedulerAuthInterceptor interceptor = interceptor("sched", "s3cret");

    call(interceptor);

    assertThat(call(interceptor)).isEqualTo("Bearer t2");
  }

  @Test
  void sendsNoHeaderAndDoesNotLoginWithoutUser() {
    RemoteSchedulerAuthInterceptor interceptor = interceptor("", "s3cret");

    assertThat(call(interceptor)).isNull();
    assertThat(server.getRequestCount()).isZero();
  }

  @Test
  void aFailedLoginDoesNotLeakThePassword() {
    server.enqueue(new MockResponse.Builder().code(401).body("{\"error\":\"bad credentials\"}").build());
    RemoteSchedulerAuthInterceptor interceptor = interceptor("sched", "s3cret-pass");

    assertThatThrownBy(() -> call(interceptor))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("sched")
      .hasMessageNotContaining("s3cret-pass");
  }

  @Test
  void aLoginThatDoesNotAnswerFailsWithinTheReadTimeoutWithoutLeakingThePassword() {
    server.enqueue(new MockResponse.Builder().code(200).bodyDelay(5, java.util.concurrent.TimeUnit.SECONDS)
      .body("{}").build());
    RemoteSchedulerAuthInterceptor interceptor = new RemoteSchedulerAuthInterceptor(
      server.url("/").toString() + "scheduler/api/v1", "sched", "s3cret-pass", clock,
      Duration.ofMillis(500), Duration.ofMillis(300));

    long start = System.nanoTime();
    assertThatThrownBy(() -> call(interceptor))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Login of user 'sched'")
      .hasMessageNotContaining("s3cret-pass");

    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
  }
}
