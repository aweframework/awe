package com.almis.awe.autoconfigure;

import com.almis.ade.autoconfigure.AdeAutoConfiguration;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.azure.spring.cloud.autoconfigure.implementation.aad.configuration.AadAutoConfiguration;
import com.azure.spring.cloud.autoconfigure.implementation.aad.configuration.properties.AadAuthenticationProperties;
import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadClientRegistrationRepository;
import com.azure.spring.cloud.autoconfigure.implementation.context.properties.AzureGlobalProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;


class AzureOAuthConfigTest {

  private static AadAuthenticationProperties aadAuthenticationProperties;

  @BeforeAll
  static void setUp() {
    aadAuthenticationProperties = new AadAuthenticationProperties();
    aadAuthenticationProperties.getCredential().setClientId("client-id");
  }

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
              SecurityAutoConfiguration.class,
              ClientRegistration.class,
              SecurityAutoConfiguration.class,
              OAuth2ClientAutoConfiguration.class,
              WebMvcAutoConfiguration.class,
              SecurityConfig.class,
              WebSecurityConfig.class,
              AuthenticationConfig.class,
              SerializerConfig.class,
              SecurityConfig.class,
              AuthenticationConfiguration.class,
              SessionConfig.class,
              WebsocketConfig.class,
              TemplateConfig.class,
              AdeAutoConfiguration.class,
              RestConfig.class,
              AadAutoConfiguration.class,
              RestTemplateAutoConfiguration.class
          )
      )
      .withUserConfiguration(BaseConfigProperties.class,
          SecurityConfigProperties.class,
          AzureGlobalProperties.class,
          OAuth2ClientProperties.class
      )
      .withPropertyValues(
          "spring.cloud.azure.active-directory.enabled=true",
          "awe.application.module-list=awe"
      )
      .withBean(NoOpCacheManager.class)
      .withBean(AadClientRegistrationRepository.class, aadAuthenticationProperties);

  @Test
  void testAzureOauthFilterShouldBeEnabled() {
    this.runner.withConfiguration(AutoConfigurations.of(AzureOAuthConfig.class))
        .withPropertyValues(
            "spring.cloud.azure.active-directory.credential.client-id=testApp")
        .run(context -> Assertions.assertThat(context).hasBean("azureOauth2Filter"));
  }
}
