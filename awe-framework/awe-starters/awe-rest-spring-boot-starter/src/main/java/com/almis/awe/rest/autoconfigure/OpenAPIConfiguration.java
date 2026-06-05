package com.almis.awe.rest.autoconfigure;

import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.autoconfigure.config.RestAuthenticationMode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Open API configuration
 */
@Configuration
public class OpenAPIConfiguration {

  private static final String BEARER_AUTH = "bearerAuth";

  /**
   * Configures AWE API Rest documentation with Open API.
   *
   * @return Grouped OpenApi
   */
  @Bean
  public GroupedOpenApi aweApi() {
    return GroupedOpenApi.builder()
      .group("awe-api")
      .pathsToMatch("/api/**")
      .build();
  }

  /**
   * Configures AWE API Rest documentation with Open API.
   *
   * @return Awe Rest OpenAPI
   */
  @Bean
  public OpenAPI aweRestOpenAPI(AweRestConfigProperties restConfigProperties) {
    return new OpenAPI()
      .components(new Components().addSecuritySchemes(BEARER_AUTH, buildSecurityScheme(restConfigProperties)))
      .info(new Info()
        .title(restConfigProperties.getDocTitle())
        .description(restConfigProperties.getDocDescription())
        .version(restConfigProperties.getDocVersion())
        .contact(new Contact()
          .name(restConfigProperties.getDocContactName())
          .email(restConfigProperties.getDocContactEmail())
          .url(restConfigProperties.getDocContactUrl()))
        .license(new License()
          .name(restConfigProperties.getDocLicenseName())
          .url(restConfigProperties.getDocLicenseUrl()))
        .termsOfService(restConfigProperties.getDocTermsOfServiceUrl()))
      .externalDocs(new ExternalDocumentation()
        .description(restConfigProperties.getDocExternalDescription())
        .url(restConfigProperties.getDocExternalUrl()));
  }

  /**
   * Configure Swagger UI OAuth properties when available.
   */
  @Bean
  public InitializingBean swaggerUiOAuthCustomizer(AweRestConfigProperties restConfigProperties,
                                                   ObjectProvider<SwaggerUiOAuthProperties> swaggerUiOAuthPropertiesProvider) {
    return () -> swaggerUiOAuthPropertiesProvider.ifAvailable(
      properties -> configureSwaggerUiOAuth(restConfigProperties, properties)
    );
  }

  void configureSwaggerUiOAuth(AweRestConfigProperties restConfigProperties,
                               SwaggerUiOAuthProperties swaggerUiOAuthProperties) {
    if (restConfigProperties.getAuth().getMode() != RestAuthenticationMode.OAUTH2_RESOURCE_SERVER) {
      return;
    }

    AweRestConfigProperties.Oauth2 openApiOauth2 = restConfigProperties.getOpenapi().getOauth2();
    if (StringUtils.hasText(openApiOauth2.getClientId())) {
      swaggerUiOAuthProperties.setClientId(openApiOauth2.getClientId());
      swaggerUiOAuthProperties.setUsePkceWithAuthorizationCodeGrant(openApiOauth2.isUsePkce());
    }
  }

  private SecurityScheme buildSecurityScheme(AweRestConfigProperties restConfigProperties) {
    if (restConfigProperties.getAuth().getMode() == RestAuthenticationMode.OAUTH2_RESOURCE_SERVER) {
      if (hasOAuth2Metadata(restConfigProperties)) {
        return buildOAuth2AuthorizationCodeScheme(restConfigProperties);
      }
      return buildExternalBearerScheme();
    }
    return buildLocalJwtScheme();
  }

  private SecurityScheme buildLocalJwtScheme() {
    return new SecurityScheme()
      .name(BEARER_AUTH)
      .type(SecurityScheme.Type.HTTP)
      .scheme("bearer")
      .bearerFormat("JWT")
      .description("AWE local JWT token obtained from /api/authenticate.");
  }

  private SecurityScheme buildExternalBearerScheme() {
    return new SecurityScheme()
      .name(BEARER_AUTH)
      .type(SecurityScheme.Type.HTTP)
      .scheme("bearer")
      .bearerFormat("JWT")
      .description("External provider access token. Obtain it outside Swagger UI and paste as Bearer token.");
  }

  private SecurityScheme buildOAuth2AuthorizationCodeScheme(AweRestConfigProperties restConfigProperties) {
    AweRestConfigProperties.Oauth2 oauth2 = restConfigProperties.getOpenapi().getOauth2();

    Scopes scopes = new Scopes();
    oauth2.getScopes().forEach(scopes::addString);

    OAuthFlow authorizationCodeFlow = new OAuthFlow()
      .authorizationUrl(oauth2.getAuthorizationUrl())
      .tokenUrl(oauth2.getTokenUrl())
      .scopes(scopes);

    return new SecurityScheme()
      .name(BEARER_AUTH)
      .type(SecurityScheme.Type.OAUTH2)
      .description("OAuth2 authorization code flow for delegated user testing.")
      .flows(new OAuthFlows().authorizationCode(authorizationCodeFlow));
  }

  private boolean hasOAuth2Metadata(AweRestConfigProperties restConfigProperties) {
    AweRestConfigProperties.Oauth2 oauth2 = restConfigProperties.getOpenapi().getOauth2();
    return StringUtils.hasText(oauth2.getAuthorizationUrl()) && StringUtils.hasText(oauth2.getTokenUrl());
  }
}
