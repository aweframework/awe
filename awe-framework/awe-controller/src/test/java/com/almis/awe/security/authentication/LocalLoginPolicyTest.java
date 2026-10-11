package com.almis.awe.security.authentication;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.SecurityConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Local login policy")
class LocalLoginPolicyTest {

  private final SecurityConfigProperties properties = new SecurityConfigProperties();
  private final LocalLoginPolicy policy = new LocalLoginPolicy(properties);
  private final Logger logger = (Logger) LoggerFactory.getLogger(LocalLoginPolicy.class);
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

  private void switchOff(String... breakGlassUsers) {
    properties.getSso().setEnabled(true);
    properties.getSso().setLocalLogin(false);
    properties.getSso().setBreakGlassUsers(List.of(breakGlassUsers));
  }

  @Test
  @DisplayName("Defaults keep local login open")
  void defaultsAllowLocalLogin() {
    assertThat(properties.getSso().isLocalLogin()).isTrue();
    assertThat(properties.getSso().getBreakGlassUsers()).isEmpty();
    assertThat(policy.check("john")).isEqualTo("john");
  }

  @Test
  @DisplayName("The switch is ignored when SSO is disabled, so nobody gets locked out")
  void switchIgnoredWithoutSso() {
    properties.getSso().setEnabled(false);
    properties.getSso().setLocalLogin(false);
    assertThat(policy.check("john")).isEqualTo("john");
  }

  @Test
  @DisplayName("The user name is trimmed once and null stays null")
  void normalisesTheUserName() {
    assertThat(policy.check("  john ")).isEqualTo("john");
    assertThat(policy.check(null)).isNull();
  }

  @Test
  @DisplayName("A refusal looks exactly like bad credentials, so users cannot be enumerated")
  void refusalIsABadCredentialsFailure() {
    switchOff();
    assertThatThrownBy(() -> policy.check("john"))
      .isInstanceOf(LocalLoginDisabledException.class)
      .isInstanceOf(BadCredentialsException.class)
      .hasMessage(new BadCredentialsException("Bad credentials").getMessage());
    assertThatThrownBy(() -> policy.check(null)).isInstanceOf(BadCredentialsException.class);
    assertThatThrownBy(() -> policy.check("   ")).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  @DisplayName("A break-glass user can still log in locally and the use is audited")
  void breakGlassUserAllowedAndAudited() {
    switchOff("admin");

    assertThat(policy.check(" admin ")).isEqualTo("admin");

    assertThat(appender.list).anySatisfy(event -> {
      assertThat(event.getLevel()).hasToString("WARN");
      assertThat(event.getFormattedMessage()).contains("[break-glass]").contains("admin");
    });
  }

  @Test
  @DisplayName("Configured break-glass entries are trimmed and blank ones ignored; matching is exact (case-sensitive)")
  void breakGlassEntriesAreNormalised() {
    switchOff("  admin  ", " ", "");

    assertThat(policy.check("admin")).isEqualTo("admin");
    assertThatThrownBy(() -> policy.check("")).isInstanceOf(LocalLoginDisabledException.class);
    assertThatThrownBy(() -> policy.check("ADMIN")).isInstanceOf(LocalLoginDisabledException.class);
  }

  @Test
  @DisplayName("Refusals are logged server-side with the real reason")
  void refusalsAreLogged() {
    switchOff("admin");

    assertThatThrownBy(() -> policy.check("john")).isInstanceOf(LocalLoginDisabledException.class);

    assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
      .contains("refused").contains("local-login").contains("john"));
  }

  @Test
  @DisplayName("A user name cannot forge log lines or break-glass entries")
  void logInjectionIsNeutralised() {
    switchOff("admin");
    String forged = "john\r\n2026-01-01 WARN [break-glass] Local login attempt by break-glass user admin\u0007" + "x".repeat(500);

    assertThatThrownBy(() -> policy.check(forged)).isInstanceOf(LocalLoginDisabledException.class);

    assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
      String message = event.getFormattedMessage();
      assertThat(message).doesNotContain("\n").doesNotContain("\r").doesNotContain("\u0007");
      assertThat(message).startsWith("Local login refused");
      assertThat(message.length()).isLessThan(300);
    });
  }
}
