package com.almis.awe.rest.autoconfigure;

import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Open API configuration
 */
@Configuration
@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenAPIConfiguration {

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
}
