package com.almis.awe.autoconfigure;

import com.almis.ade.autoconfigure.AdeAutoConfiguration;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.security.authentication.encoder.AwePasswordEncoder;
import com.almis.awe.service.TotpService;
import com.almis.awe.service.user.CurrentPasswordChangeService;
import com.almis.awe.service.user.PasswordChangeService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Clock;

@ExtendWith(SpringExtension.class)
class AuthenticationConfigTest {

  private static final String QR_GENERATOR_BEAN = "zxingQrPngGenerator";
  private static final String TOTP_OPERATIONS_BEAN = "aweTotpOperations";
  private static final String TOTP_OPERATIONS_CLASS = "com.almis.awe.service.totp.OtpJavaTotpOperations";
  private static final String QR_GENERATOR_CLASS = "com.almis.awe.service.totp.ZxingQrPngGenerator";

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(
            AuthenticationConfig.class,
            SerializerConfig.class,
            SecurityConfig.class,
            AuthenticationConfiguration.class,
            TaskConfig.class,
            SessionConfig.class,
            WebsocketConfig.class,
            TemplateConfig.class,
            AdeAutoConfiguration.class,
            RestConfig.class)
    )
    .withUserConfiguration(BaseConfigProperties.class, SecurityConfigProperties.class, MultiTenantOAuth2Config.class)
    .withPropertyValues("awe.application.module-list=awe",
      "awe.security.master-key=test-master-key",
      "spring.cache.type=NONE",
      "awe.database.enabled=false")
    .withBean(NoOpCacheManager.class)
    .withBean(SpringTemplateEngine.class)
    .withBean(DefaultConversionService.class);

  @Test
  void testLdapProviderShouldBeEnabled() {
    runner.withPropertyValues("awe.security.auth-mode=ldap")
      .run(context -> Assertions.assertThat(context).hasBean("ldapAuthenticationProvider"));
  }

  @Test
  void securityConfigPublishesAweOwnedTotpBeans() {
    runner.withBean("applicationClock", Clock.class, Clock::systemDefaultZone).run(context -> {
      Assertions.assertThat(context).hasBean("applicationClock");
      Assertions.assertThat(context).hasBean(SecurityConfig.TOTP_CLOCK_BEAN);
      Assertions.assertThat(context).hasBean(QR_GENERATOR_BEAN);
      Assertions.assertThat(context).hasBean(TOTP_OPERATIONS_BEAN);
      Assertions.assertThat(context).hasSingleBean(TotpService.class);

      Clock applicationClock = context.getBean("applicationClock", Clock.class);
      Clock clock = context.getBean(SecurityConfig.TOTP_CLOCK_BEAN, Clock.class);
      Object qrPngGenerator = context.getBean(QR_GENERATOR_BEAN);
      Object operations = context.getBean(TOTP_OPERATIONS_BEAN);

      Assertions.assertThat(operations.getClass().getName()).isEqualTo(TOTP_OPERATIONS_CLASS);
      Assertions.assertThat(qrPngGenerator.getClass().getName()).isEqualTo(QR_GENERATOR_CLASS);
      Assertions.assertThat(clock).isNotSameAs(applicationClock);
      Assertions.assertThat(ReflectionTestUtils.getField(operations, "clock")).isSameAs(clock);
      Assertions.assertThat(ReflectionTestUtils.getField(operations, "qrPngGenerator")).isSameAs(qrPngGenerator);
    });
  }

  @Test
  void passwordEncoderHashesNewPasswordsWithBcryptByDefault() {
    runner.run(context -> {
      Assertions.assertThat(context).hasSingleBean(PasswordEncoder.class);
      PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
      Assertions.assertThat(encoder).isInstanceOf(AwePasswordEncoder.class);
      Assertions.assertThat(encoder.encode("secret")).startsWith("{bcrypt}");
    });
  }

  @Test
  void passwordEncoderHashesNewPasswordsWithRipemd160WhenConfigured() {
    runner.withPropertyValues("awe.security.password-encoder=ripemd160").run(context -> {
      PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
      Assertions.assertThat(encoder.encode("secret")).doesNotStartWith("{").hasSize(40);
    });
  }

  @Test
  void authenticationAndPasswordChangeUseThePasswordEncoderBean() {
    runner.run(context -> {
      PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
      Assertions.assertThat(ReflectionTestUtils.getField(context.getBean(PasswordChangeService.class), "passwordEncoder"))
        .isSameAs(encoder);
      Assertions.assertThat((Object) ReflectionTestUtils.invokeMethod(context.getBean(DaoAuthenticationProvider.class), "getPasswordEncoder"))
        .isSameAs(encoder);
      Assertions.assertThat(ReflectionTestUtils.getField(context.getBean(CurrentPasswordChangeService.class), "passwordEncoder"))
        .isSameAs(encoder);
      Assertions.assertThat(ReflectionTestUtils.getField(context.getBean(CurrentPasswordChangeService.class), "userDAO"))
        .isSameAs(context.getBean(UserDAO.class));
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  void applicationPasswordEncoderReplacesAweOne() {
    runner.withBean(PasswordEncoder.class, NoOpPasswordEncoder::getInstance).run(context -> {
      Assertions.assertThat(context).hasSingleBean(PasswordEncoder.class);
      Assertions.assertThat(context.getBean(PasswordEncoder.class)).isSameAs(NoOpPasswordEncoder.getInstance());
    });
  }
}
