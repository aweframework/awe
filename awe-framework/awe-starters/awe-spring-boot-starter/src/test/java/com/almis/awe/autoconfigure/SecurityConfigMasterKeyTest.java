package com.almis.awe.autoconfigure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.TotpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Master key configuration tests")
class SecurityConfigMasterKeyTest {

  private static final String LEGACY_KEY = "B1Le3s%25abc75TeBe05";

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(SecurityConfig.class))
    .withBean(AuthenticationManagerBuilder.class, () -> mock(AuthenticationManagerBuilder.class))
    .withBean(AccessService.class, () -> mock(AccessService.class))
    .withBean(TotpService.class, () -> mock(TotpService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
    .withUserConfiguration(BaseConfigProperties.class)
    .withPropertyValues("awe.application.module-list=awe", "awe.security.auth-mode=bbdd");

  private final Logger logger = (Logger) LoggerFactory.getLogger(SecurityConfig.class);
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  private List<ILoggingEvent> warnings() {
    return appender.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
  }

  private static String failureMessages(Throwable failure) {
    StringBuilder messages = new StringBuilder();
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getMessage()).append('\n');
    }
    return messages.toString();
  }

  @Test
  @DisplayName("Startup fails with a clear error when no master key is configured")
  void missingMasterKey_failsWithClearError() {
    runner.run(context -> {
      assertThat(context).hasFailed();
      String messages = failureMessages(context.getStartupFailure());
      assertThat(messages)
        .contains("awe.security.master-key")
        .contains("required")
        .contains("encrypts stored data")
        .contains("AWE 4")
        .contains("migration guide")
        .doesNotContain(LEGACY_KEY);
    });
  }

  @Test
  @DisplayName("Startup fails when the master key is blank")
  void blankMasterKey_fails() {
    runner.withPropertyValues("awe.security.master-key=  ").run(context -> {
      assertThat(context).hasFailed();
      assertThat(failureMessages(context.getStartupFailure())).contains("awe.security.master-key");
    });
  }

  @Test
  @DisplayName("A configured master key is bound and logs no warning")
  void configuredMasterKey_isBoundWithoutWarning() {
    runner.withPropertyValues("awe.security.master-key=my-own-master-key").run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(SecurityConfigProperties.class).getMasterKey()).isEqualTo("my-own-master-key");
      assertThat(warnings()).isEmpty();
    });
  }

  @Test
  @DisplayName("The legacy AWE 4 master key is accepted with a single warning that never contains the key")
  void legacyMasterKey_isAcceptedWithWarning() {
    runner.withPropertyValues("awe.security.master-key=" + LEGACY_KEY).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(SecurityConfigProperties.class).getMasterKey()).isEqualTo(LEGACY_KEY);
      List<ILoggingEvent> warnings = warnings();
      assertThat(warnings).hasSize(1);
      assertThat(warnings.get(0).getFormattedMessage())
        .contains("awe.security.master-key")
        .contains("rotate")
        .doesNotContain(LEGACY_KEY);
    });
  }
}
