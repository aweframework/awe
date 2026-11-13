package com.almis.awe.security.audit;

import com.almis.awe.config.SecurityAuditConfigProperties;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SecurityAuditServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-16T10:15:30Z");

  private final List<SecurityAuditEvent> recorded = new ArrayList<>();
  private SecurityAuditConfigProperties properties;
  private SecurityAuditService service;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    // Other tests may leave a mocked security context behind
    SecurityContextHolder.clearContext();
    properties = new SecurityAuditConfigProperties();
    service = new SecurityAuditService(properties, List.of(recorded::add), () -> "tenant-a",
      Clock.fixed(NOW, ZoneOffset.UTC));
    request = new MockHttpServletRequest("POST", "/action/login");
    request.setRemoteAddr("192.168.1.27");
    request.addHeader("User-Agent", "Mozilla/5.0 Test");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
    MDC.clear();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("A failure is enriched with the request data and sent to the sinks")
  void failureIsEnrichedFromTheRequest() {
    String sessionId = request.getSession(true).getId();

    service.failure(SecurityAuditEventType.LOGIN, "jdoe", SecurityAuditCause.BAD_CREDENTIALS, "form");

    assertThat(recorded).singleElement().satisfies(event -> {
      assertThat(event.getId()).isNotBlank();
      assertThat(event.getTimestamp()).isEqualTo(NOW);
      assertThat(event.getType()).isEqualTo(SecurityAuditEventType.LOGIN);
      assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.FAILURE);
      assertThat(event.getCause()).isEqualTo(SecurityAuditCause.BAD_CREDENTIALS);
      assertThat(event.getUsername()).isEqualTo("jdoe");
      assertThat(event.getTenant()).isEqualTo("tenant-a");
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.WEB);
      assertThat(event.getIpAddress()).isEqualTo("192.168.1.27");
      assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0 Test");
      assertThat(event.getSessionHash()).isEqualTo(DigestUtils.sha256Hex(sessionId)).isNotEqualTo(sessionId);
      assertThat(event.getCorrelationId()).isNotBlank();
      assertThat(event.getDetail()).isEqualTo("form");
    });
  }

  @Test
  @DisplayName("A success carries no cause and the SUCCESS outcome")
  void successHasSuccessOutcome() {
    service.success(SecurityAuditEventType.TOKEN_ISSUED, "jdoe", null);

    assertThat(recorded).singleElement().satisfies(event -> {
      assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);
      assertThat(event.getCause()).isNull();
    });
  }

  @Test
  @DisplayName("Requests under /api/ are recorded on the REST channel")
  void apiRequestsUseTheRestChannel() {
    request.setRequestURI("/app/api/authenticate");
    request.setContextPath("/app");

    service.success(SecurityAuditEventType.TOKEN_ISSUED, "jdoe", null);

    assertThat(recorded.getFirst().getChannel()).isEqualTo(SecurityAuditChannel.REST);
  }

  @Test
  @DisplayName("Values already set on the event are kept")
  void presetValuesAreKept() {
    service.publish(SecurityAuditEvent.builder()
      .type(SecurityAuditEventType.LOGOUT)
      .outcome(SecurityAuditOutcome.SUCCESS)
      .channel(SecurityAuditChannel.SSO)
      .sessionHash("preset-hash")
      .tenant("other-tenant")
      .build());

    assertThat(recorded).singleElement().satisfies(event -> {
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.SSO);
      assertThat(event.getSessionHash()).isEqualTo("preset-hash");
      assertThat(event.getTenant()).isEqualTo("other-tenant");
    });
  }

  @Test
  @DisplayName("The correlation header is used when valid, and one generated identifier is shared by the request")
  void correlationIdentifier() {
    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);
    service.success(SecurityAuditEventType.TOKEN_ISSUED, "jdoe", null);
    assertThat(recorded.get(0).getCorrelationId()).isEqualTo(recorded.get(1).getCorrelationId());

    MockHttpServletRequest withHeader = new MockHttpServletRequest("POST", "/action/login");
    withHeader.addHeader("X-Correlation-Id", "abc-123");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(withHeader));
    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);
    assertThat(recorded.get(2).getCorrelationId()).isEqualTo("abc-123");

    MockHttpServletRequest withBadHeader = new MockHttpServletRequest("POST", "/action/login");
    withBadHeader.addHeader("X-Correlation-Id", "bad value\n<script>");
    withBadHeader.addHeader("X-Request-Id", "req-9");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(withBadHeader));
    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);
    assertThat(recorded.get(3).getCorrelationId()).isEqualTo("req-9");
  }

  @Test
  @DisplayName("Without a correlation header the tracing identifier of the log context is used")
  void correlationFromTraceId() {
    MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");

    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);

    assertThat(recorded.getFirst().getCorrelationId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
  }

  @Test
  @DisplayName("The client address is anonymized when configured")
  void anonymizesIp() {
    properties.setAnonymizeIp(true);

    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);

    assertThat(recorded.getFirst().getIpAddress()).isEqualTo("192.168.1.0");
  }

  @Test
  @DisplayName("Outside a request the event is recorded on the SYSTEM channel without request data")
  void outsideRequest() {
    RequestContextHolder.resetRequestAttributes();

    service.success(SecurityAuditEventType.LOGOUT, "jdoe", null);

    assertThat(recorded).singleElement().satisfies(event -> {
      assertThat(event.getChannel()).isEqualTo(SecurityAuditChannel.SYSTEM);
      assertThat(event.getIpAddress()).isNull();
      assertThat(event.getUserAgent()).isNull();
      assertThat(event.getSessionHash()).isNull();
    });
  }

  @Test
  @DisplayName("Without a user name the authenticated user is recorded")
  void usernameFromSecurityContext() {
    SecurityContextHolder.setContext(new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("admin", null, AuthorityUtils.NO_AUTHORITIES)));

    service.failure(SecurityAuditEventType.ACCESS_DENIED, null, SecurityAuditCause.RESTRICTED, "screen/private/x");

    assertThat(recorded.getFirst().getUsername()).isEqualTo("admin");
  }

  @Test
  @DisplayName("Long user agents and details are truncated to the column sizes")
  void truncatesLongValues() {
    request.removeHeader("User-Agent");
    request.addHeader("User-Agent", "A".repeat(2000));

    service.success(SecurityAuditEventType.LOGIN, "jdoe", "D".repeat(5000));

    assertThat(recorded.getFirst().getUserAgent()).hasSize(SecurityAuditService.MAX_USER_AGENT);
    assertThat(recorded.getFirst().getDetail()).hasSize(SecurityAuditService.MAX_DETAIL);
  }

  @Test
  @DisplayName("When the audit is disabled no sink receives anything")
  void disabledRecordsNothing() {
    properties.setEnabled(false);

    service.success(SecurityAuditEventType.LOGIN, "jdoe", null);

    assertThat(service.isEnabled()).isFalse();
    assertThat(recorded).isEmpty();
  }

  @Test
  @DisplayName("A failing sink neither breaks the audited operation nor the other sinks")
  void failingSinkIsIsolated() {
    SecurityAuditService withFailingSink = new SecurityAuditService(properties, List.of(event -> {
      throw new IllegalStateException("database down");
    }, recorded::add), () -> {
      throw new IllegalStateException("no session");
    }, Clock.fixed(NOW, ZoneOffset.UTC));

    assertThatCode(() -> withFailingSink.success(SecurityAuditEventType.LOGIN, "jdoe", null)).doesNotThrowAnyException();
    assertThat(recorded).singleElement().satisfies(event -> assertThat(event.getTenant()).isNull());
  }

  @Test
  @DisplayName("Inside a transaction the event is sent after commit and dropped on rollback")
  void transactionalDispatch() {
    TransactionSynchronizationManager.initSynchronization();
    service.success(SecurityAuditEventType.ACCOUNT_UNLOCKED, "jdoe", null);
    assertThat(recorded).isEmpty();
    List<TransactionSynchronization> committed = TransactionSynchronizationManager.getSynchronizations();
    committed.forEach(TransactionSynchronization::afterCommit);
    committed.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    assertThat(recorded).hasSize(1);
    TransactionSynchronizationManager.clearSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    service.success(SecurityAuditEventType.ACCOUNT_LOCKED, "jdoe", null);
    TransactionSynchronizationManager.getSynchronizations()
      .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    assertThat(recorded).hasSize(1);
  }
}
