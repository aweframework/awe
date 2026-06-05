package com.almis.awe.rest.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AweRestConfigPropertiesTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
    .withUserConfiguration(TestConfig.class);

  @Test
  void shouldKeepLocalJwtAsDefaultAuthModeAndBindLegacyJwtProperties() {
    contextRunner
      .withPropertyValues(
        "awe.security.master.key=test-master-key",
        "awe.rest.api.jwt.issuer=TEST_ISSUER",
        "awe.rest.api.jwt.authorization-header=X-Authorization"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();

        AweRestConfigProperties properties = context.getBean(AweRestConfigProperties.class);
        JWTProperties jwtProperties = context.getBean(JWTProperties.class);

        assertThat(properties.getAuth().getMode()).isEqualTo(RestAuthenticationMode.LOCAL_JWT);
        assertThat(jwtProperties.getAuthorizationHeader()).isEqualTo("X-Authorization");
        assertThat(jwtProperties.getIssuer()).isEqualTo("TEST_ISSUER");
      });
  }

  @Test
  void shouldBindOauth2ResourceServerAndOpenApiProperties() {
    contextRunner
      .withPropertyValues(
        "awe.rest.api.auth.mode=oauth2-resource-server",
        "awe.rest.api.oauth2-resource-server.jwt.issuer-uri=https://issuer.example.com",
        "awe.rest.api.oauth2-resource-server.jwt.jwk-set-uri=https://issuer.example.com/jwks",
        "awe.rest.api.oauth2-resource-server.jwt.audiences[0]=api://awe-rest",
        "awe.rest.api.oauth2-resource-server.jwt.audiences[1]=api://awe-rest-admin",
        "awe.rest.api.oauth2-resource-server.principal.profile-claim=profile",
        "awe.rest.api.oauth2-resource-server.principal.default-profile=operator",
        "awe.rest.api.oauth2-resource-server.principal.delegated-username-claims[0]=preferred_username",
        "awe.rest.api.oauth2-resource-server.principal.client-id-claims[0]=azp",
        "awe.rest.api.openapi.oauth2.authorization-url=https://issuer.example.com/authorize",
        "awe.rest.api.openapi.oauth2.token-url=https://issuer.example.com/token",
        "awe.rest.api.openapi.oauth2.client-id=awe-swagger",
        "awe.rest.api.openapi.oauth2.use-pkce=true",
        "awe.rest.api.openapi.oauth2.scopes.rest.access=Access api"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();

        AweRestConfigProperties properties = context.getBean(AweRestConfigProperties.class);

        assertThat(properties.getAuth().getMode()).isEqualTo(RestAuthenticationMode.OAUTH2_RESOURCE_SERVER);
        assertThat(properties.getOauth2ResourceServer().getJwt().getIssuerUri()).isEqualTo("https://issuer.example.com");
        assertThat(properties.getOauth2ResourceServer().getJwt().getJwkSetUri()).isEqualTo("https://issuer.example.com/jwks");
        assertThat(properties.getOauth2ResourceServer().getJwt().getAudiences())
          .containsExactly("api://awe-rest", "api://awe-rest-admin");
        assertThat(properties.getOauth2ResourceServer().getPrincipal().getProfileClaim()).isEqualTo("profile");
        assertThat(properties.getOauth2ResourceServer().getPrincipal().getDefaultProfile()).isEqualTo("operator");
        assertThat(properties.getOpenapi().getOauth2().getAuthorizationUrl()).isEqualTo("https://issuer.example.com/authorize");
        assertThat(properties.getOpenapi().getOauth2().getTokenUrl()).isEqualTo("https://issuer.example.com/token");
        assertThat(properties.getOpenapi().getOauth2().getClientId()).isEqualTo("awe-swagger");
        assertThat(properties.getOpenapi().getOauth2().isUsePkce()).isTrue();
        assertThat(properties.getOpenapi().getOauth2().getScopes())
          .containsEntry("rest.access", "Access api");
      });
  }

  @Test
  void shouldAllowIssuerAndJwkCombinedConfiguration() {
    contextRunner
      .withPropertyValues(
        "awe.rest.api.auth.mode=oauth2-resource-server",
        "awe.rest.api.oauth2-resource-server.jwt.issuer-uri=https://issuer.example.com",
        "awe.rest.api.oauth2-resource-server.jwt.jwk-set-uri=https://issuer.example.com/jwks",
        "awe.rest.api.oauth2-resource-server.jwt.audiences[0]=api://awe-rest"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();
        AweRestConfigProperties properties = context.getBean(AweRestConfigProperties.class);
        assertThat(properties.getOauth2ResourceServer().getJwt().getIssuerUri()).isEqualTo("https://issuer.example.com");
        assertThat(properties.getOauth2ResourceServer().getJwt().getJwkSetUri()).isEqualTo("https://issuer.example.com/jwks");
      });
  }

  @Test
  void shouldAllowIssuerWithoutOpenApiOauthSettings() {
    contextRunner
      .withPropertyValues(
        "awe.rest.api.auth.mode=oauth2-resource-server",
        "awe.rest.api.oauth2-resource-server.jwt.issuer-uri=https://issuer.example.com",
        "awe.rest.api.oauth2-resource-server.jwt.audiences[0]=api://awe-rest"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();
        AweRestConfigProperties properties = context.getBean(AweRestConfigProperties.class);
        assertThat(properties.getOpenapi().getOauth2().getAuthorizationUrl()).isNull();
        assertThat(properties.getOpenapi().getOauth2().getTokenUrl()).isNull();
      });
  }

  @Test
  void shouldFailWhenOauth2ModeHasNoAudiencesConfigured() {
    contextRunner
      .withPropertyValues(
        "awe.rest.api.auth.mode=oauth2-resource-server",
        "awe.rest.api.oauth2-resource-server.jwt.issuer-uri=https://issuer.example.com"
      )
      .run(context -> {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure()).hasStackTraceContaining("audiences must not be empty");
      });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({AweRestConfigProperties.class, JWTProperties.class})
  static class TestConfig {
  }
}
