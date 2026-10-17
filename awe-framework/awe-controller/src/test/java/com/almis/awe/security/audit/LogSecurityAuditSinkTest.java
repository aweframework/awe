package com.almis.awe.security.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LogSecurityAuditSinkTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(LogSecurityAuditSink.LOGGER_NAME);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void setUp() {
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(null);
  }

  @Test
  void writesOneJsonLinePerEvent() {
    new LogSecurityAuditSink().record(SecurityAuditEvent.builder()
      .id("e-1")
      .timestamp(Instant.parse("2026-10-16T10:15:30Z"))
      .type(SecurityAuditEventType.LOGIN)
      .outcome(SecurityAuditOutcome.FAILURE)
      .cause(SecurityAuditCause.LOCKED)
      .username("jdoe")
      .channel(SecurityAuditChannel.WEB)
      .ipAddress("10.0.0.1")
      .userAgent("agent \"quoted\"\nnext")
      .correlationId("c-1")
      .build());

    assertThat(appender.list).singleElement().satisfies(entry -> {
      assertThat(entry.getLevel()).isEqualTo(Level.INFO);
      String line = entry.getFormattedMessage();
      assertThat(line).doesNotContain("\n");
      JsonNode json = new JsonMapper().readTree(line);
      assertThat(json.path("id").asString()).isEqualTo("e-1");
      assertThat(json.path("timestamp").asString()).isEqualTo("2026-10-16T10:15:30Z");
      assertThat(json.path("type").asString()).isEqualTo("LOGIN");
      assertThat(json.path("outcome").asString()).isEqualTo("FAILURE");
      assertThat(json.path("cause").asString()).isEqualTo("LOCKED");
      assertThat(json.path("username").asString()).isEqualTo("jdoe");
      assertThat(json.path("channel").asString()).isEqualTo("WEB");
      assertThat(json.path("ipAddress").asString()).isEqualTo("10.0.0.1");
      assertThat(json.path("userAgent").asString()).isEqualTo("agent \"quoted\"\nnext");
      assertThat(json.path("correlationId").asString()).isEqualTo("c-1");
      assertThat(json.has("detail")).as("null values are left out").isFalse();
      assertThat(json.has("tenant")).isFalse();
    });
  }
}
