package com.almis.awe.observability.instrumentation;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MicrometerSecurityAuditSinkTest {

  @Test
  void countsEventsByTypeOutcomeChannelAndCauseWithoutTheUser() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MicrometerSecurityAuditSink sink = new MicrometerSecurityAuditSink(() -> registry);

    sink.record(event(SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS));
    sink.record(event(SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS));
    sink.record(event(SecurityAuditOutcome.SUCCESS, null));

    Counter failures = registry.get(MicrometerSecurityAuditSink.METER)
      .tags("type", "LOGIN", "outcome", "FAILURE", "channel", "WEB", "cause", "BAD_CREDENTIALS").counter();
    Counter successes = registry.get(MicrometerSecurityAuditSink.METER)
      .tags("type", "LOGIN", "outcome", "SUCCESS", "channel", "WEB", "cause", "none").counter();
    assertThat(failures.count()).isEqualTo(2.0);
    assertThat(successes.count()).isEqualTo(1.0);
    assertThat(failures.getId().getTags()).noneMatch(tag -> tag.getValue().contains("jdoe"));
  }

  @Test
  void doesNothingWithoutRegistry() {
    MicrometerSecurityAuditSink sink = new MicrometerSecurityAuditSink(() -> null);

    assertThatCode(() -> sink.record(event(SecurityAuditOutcome.SUCCESS, null))).doesNotThrowAnyException();
  }

  private static SecurityAuditEvent event(SecurityAuditOutcome outcome, SecurityAuditCause cause) {
    return SecurityAuditEvent.builder().type(SecurityAuditEventType.LOGIN).outcome(outcome).cause(cause)
      .channel(SecurityAuditChannel.WEB).username("jdoe").build();
  }
}
