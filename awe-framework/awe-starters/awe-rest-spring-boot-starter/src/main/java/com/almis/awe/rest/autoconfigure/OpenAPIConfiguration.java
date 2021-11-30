package com.almis.awe.rest.autoconfigure;

import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Open API configuration
 */
@Configuration
@ConditionalOnProperty(name = "awe.rest.enabled", havingValue = "true")
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
                    .title(restConfigProperties.getTitle())
                    .description(restConfigProperties.getDescription())
                    .version(restConfigProperties.getVersion())
                    .contact(new Contact()
                            .name(restConfigProperties.getContactName())
                            .email(restConfigProperties.getContactEmail())
                            .url(restConfigProperties.getContactUrl()))
                    .license(new License()
                            .name(restConfigProperties.getLicenseName())
                            .url(restConfigProperties.getLicenseUrl()))
                    .termsOfService(restConfigProperties.getTermsOfServiceUrl()))
            .externalDocs(new ExternalDocumentation()
                    .description(restConfigProperties.getExternalDocDescription())
                    .url(restConfigProperties.getExternalDocUrl()));
  }
}
