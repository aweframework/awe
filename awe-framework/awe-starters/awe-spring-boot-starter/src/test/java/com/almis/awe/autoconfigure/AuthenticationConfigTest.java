package com.almis.awe.autoconfigure;

import com.almis.ade.autoconfigure.AdeAutoConfiguration;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AuthenticationConfigTest {

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(
            AuthenticationConfig.class,
            SerializerConfig.class,
            SecurityConfig.class,
            AuthenticationConfiguration.class,
            SessionConfig.class,
            WebsocketConfig.class,
            TotpAutoConfiguration.class,
            TemplateConfig.class,
            AdeAutoConfiguration.class,
            RestConfig.class)
    )
    .withUserConfiguration(BaseConfigProperties.class, SecurityConfigProperties.class)
    .withPropertyValues("awe.application.module-list=awe",
      "spring.cache.type=NONE",
      "awe.database.enabled=false")
    .withBean(NoOpCacheManager.class)
    .withBean(DefaultConversionService.class);

  @Test
  void testLdapProviderShouldBeEnabled() {
    runner.withPropertyValues("awe.security.auth-mode=ldap")
      .run(context -> Assertions.assertThat(context).hasBean("ldapAuthenticationProvider"));
  }
}