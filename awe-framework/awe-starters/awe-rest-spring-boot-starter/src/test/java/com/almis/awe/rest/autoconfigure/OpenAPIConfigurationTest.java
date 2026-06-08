package com.almis.awe.rest.autoconfigure;

import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.autoconfigure.config.RestAuthenticationMode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAPIConfigurationTest {

  private final OpenAPIConfiguration configuration = new OpenAPIConfiguration();

  @Test
  void shouldExposeHttpBearerSchemeInLocalJwtMode() {
    AweRestConfigProperties properties = new AweRestConfigProperties();
    properties.getAuth().setMode(RestAuthenticationMode.LOCAL_JWT);

    OpenAPI openAPI = configuration.aweRestOpenAPI(properties);

    SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
    assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
    assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
  }

  @Test
  void shouldExposeOauth2AuthorizationCodeSchemeWhenMetadataConfigured() {
    AweRestConfigProperties properties = new AweRestConfigProperties();
    properties.getAuth().setMode(RestAuthenticationMode.OAUTH2_RESOURCE_SERVER);
    properties.getOpenapi().getOauth2().setAuthorizationUrl("https://issuer.example.com/authorize");
    properties.getOpenapi().getOauth2().setTokenUrl("https://issuer.example.com/token");
    properties.getOpenapi().getOauth2().getScopes().put("api://awe-rest/access", "Access AWE REST API");

    OpenAPI openAPI = configuration.aweRestOpenAPI(properties);

    SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
    assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);

    OAuthFlow authorizationCode = bearerAuth.getFlows().getAuthorizationCode();
    assertThat(authorizationCode.getAuthorizationUrl()).isEqualTo("https://issuer.example.com/authorize");
    assertThat(authorizationCode.getTokenUrl()).isEqualTo("https://issuer.example.com/token");
    assertThat(authorizationCode.getScopes()).containsEntry("api://awe-rest/access", "Access AWE REST API");
  }

  @Test
  void shouldFallbackToHttpBearerSchemeInOauth2ModeWithoutMetadata() {
    AweRestConfigProperties properties = new AweRestConfigProperties();
    properties.getAuth().setMode(RestAuthenticationMode.OAUTH2_RESOURCE_SERVER);

    OpenAPI openAPI = configuration.aweRestOpenAPI(properties);

    SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
    assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
    assertThat(bearerAuth.getDescription()).containsIgnoringCase("external");
  }

  @Test
  void shouldConfigureSwaggerUiOAuthClientIdAndPkceWhenConfigured() {
    AweRestConfigProperties properties = new AweRestConfigProperties();
    properties.getAuth().setMode(RestAuthenticationMode.OAUTH2_RESOURCE_SERVER);
    properties.getOpenapi().getOauth2().setClientId("awe-swagger-ui");
    properties.getOpenapi().getOauth2().setUsePkce(true);

    SwaggerUiOAuthProperties swaggerUiOAuthProperties = new SwaggerUiOAuthProperties();

    configuration.configureSwaggerUiOAuth(properties, swaggerUiOAuthProperties);

    assertThat(swaggerUiOAuthProperties.getClientId()).isEqualTo("awe-swagger-ui");
    assertThat(swaggerUiOAuthProperties.getUsePkceWithAuthorizationCodeGrant()).isTrue();
    assertThat(swaggerUiOAuthProperties.getClientSecret()).isNull();
  }
}
