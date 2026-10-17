package com.almis.awe.security.audit;

import com.almis.awe.config.SecurityAuditConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityAuditPurgeSchedulerTest {

  private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

  private final DatabaseSecurityAuditSink sink = mock(DatabaseSecurityAuditSink.class);
  private final SecurityAuditConfigProperties properties = new SecurityAuditConfigProperties();
  private SecurityAuditPurgeScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.stop();
    }
  }

  @Test
  void purgesEventsOlderThanTheRetention() {
    properties.getDatabase().setRetentionDays(30);
    scheduler = new SecurityAuditPurgeScheduler(sink, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    when(sink.purge(any())).thenReturn(4);

    assertThat(scheduler.purgeExpiredEvents()).isEqualTo(4);
    verify(sink).purge(NOW.minus(30, ChronoUnit.DAYS));
  }

  @Test
  void startsOnlyWithARetention() {
    properties.getDatabase().setRetentionDays(30);
    scheduler = new SecurityAuditPurgeScheduler(sink, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    scheduler.start();
    assertThat(scheduler.isRunning()).isTrue();
    scheduler.stop();
    assertThat(scheduler.isRunning()).isFalse();

    properties.getDatabase().setRetentionDays(0);
    scheduler.start();
    assertThat(scheduler.isRunning()).isFalse();
    assertThat(scheduler.purgeExpiredEvents()).isZero();
    verify(sink, never()).purge(any());
  }

  @Test
  void purgeErrorsDoNotEscape() {
    properties.getDatabase().setRetentionDays(30);
    scheduler = new SecurityAuditPurgeScheduler(sink, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    when(sink.purge(any())).thenThrow(new IllegalStateException("database down"));

    assertThat(scheduler.purgeExpiredEvents()).isZero();
  }
}
