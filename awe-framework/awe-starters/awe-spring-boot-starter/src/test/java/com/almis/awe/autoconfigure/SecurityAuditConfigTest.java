package com.almis.awe.autoconfigure;

import com.almis.awe.security.audit.DatabaseSecurityAuditSink;
import com.almis.awe.security.audit.LogSecurityAuditSink;
import com.almis.awe.security.audit.LoginAttemptService;
import com.almis.awe.security.audit.LoginAuditor;
import com.almis.awe.security.audit.SecurityAuditPurgeScheduler;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.security.audit.SecurityAuditSessionListener;
import com.almis.awe.security.audit.SecurityAuditSink;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Security audit wiring: enabled by default, each sink switchable and a global switch
 */
class SecurityAuditConfigTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withBean(DataSource.class, () -> mock(DataSource.class))
    .withBean(QueryService.class, () -> mock(QueryService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withUserConfiguration(SecurityAuditConfig.class);

  @Test
  void enabledByDefaultWithDatabaseAndLogSinks() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).hasSingleBean(DatabaseSecurityAuditSink.class);
      assertThat(context).hasSingleBean(LogSecurityAuditSink.class);
      assertThat(context).hasSingleBean(SecurityAuditPurgeScheduler.class);
      assertThat(context.getBean(SecurityAuditService.class).isEnabled()).isTrue();
      assertThat(context).hasSingleBean(LoginAuditor.class);
      assertThat(context).hasSingleBean(SecurityAuditSessionListener.class);
      assertThat(context.getBean(LoginAttemptService.class).isEnabled()).as("automatic lockout is off by default").isFalse();
    });
  }

  @Test
  void automaticLockoutIsSwitchedOnWithMaxAttempts() {
    runner.withPropertyValues("awe.security.lockout.max-attempts=5")
      .run(context -> assertThat(context.getBean(LoginAttemptService.class).isEnabled()).isTrue());
  }

  @Test
  void databaseSinkCanBeSwitchedOffKeepingTheLog() {
    runner.withPropertyValues("awe.security.audit.database.enabled=false").run(context -> {
      assertThat(context).doesNotHaveBean(DatabaseSecurityAuditSink.class);
      assertThat(context).doesNotHaveBean(SecurityAuditPurgeScheduler.class);
      assertThat(context).hasSingleBean(LogSecurityAuditSink.class);
      assertThat(context.getBean(SecurityAuditService.class).isEnabled()).isTrue();
    });
  }

  @Test
  void logSinkCanBeSwitchedOff() {
    runner.withPropertyValues("awe.security.audit.log.enabled=false").run(context -> {
      assertThat(context).doesNotHaveBean(LogSecurityAuditSink.class);
      assertThat(context).hasSingleBean(DatabaseSecurityAuditSink.class);
    });
  }

  @Test
  void globalSwitchTurnsEverythingOff() {
    runner.withPropertyValues("awe.security.audit.enabled=false").run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(SecurityAuditSink.class);
      assertThat(context).doesNotHaveBean(SecurityAuditPurgeScheduler.class);
      assertThat(context.getBean(SecurityAuditService.class).isEnabled()).isFalse();
    });
  }
}
