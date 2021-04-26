package com.almis.awe.rest.autoconfigure;

import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Swagger configuration
 */
@Configuration
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
@EnableSwagger2
public class SwaggerConfiguration {

  // Autowired
  private final AweRestConfigProperties restConfigProperties;

  public SwaggerConfiguration(AweRestConfigProperties restConfigProperties) {
    this.restConfigProperties = restConfigProperties;
  }

  /**
   * Configures how the API Rest documentation is generated, it uses the Swagger framework for this.
   *
   * @return Docket configuration object
   */
  @Bean
  public Docket apiDocumentGeneration() {
    return new Docket(DocumentationType.SWAGGER_2)
            .produces(Collections.singleton("application/json"))
            .consumes(Collections.singleton("application/json"))
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.almis.awe.rest"))
            .paths(PathSelectors.any())
            .build()
            .useDefaultResponseMessages(false)
            .pathMapping("/")
            .securitySchemes(securitySchemes())
            .apiInfo(apiInfo());
  }

  /**
   * sorts API endpoints alphabetically by method
   *
   * @return UiConfiguration bean
   */
  @Bean
  UiConfiguration uiConfig() {
    return UiConfigurationBuilder
            .builder()
            .operationsSorter(OperationsSorter.METHOD)
            .build();
  }

  /**
   * Configure security for SWAGGER
   *
   * @return SecurityConfiguration
   */
  @Bean
  public SecurityConfiguration security() {
    return SecurityConfigurationBuilder.builder()
            .useBasicAuthenticationWithAccessCodeGrant(false)
            .build();
  }

  private List<SecurityScheme> securitySchemes() {
    List<SecurityScheme> securitySchemeList = new ArrayList<>();
    // JWT Auth
    securitySchemeList.add(apiKey());

    return securitySchemeList;
  }

  private ApiKey apiKey() {
    return new ApiKey("JWTToken", restConfigProperties.getAuthorizationHeader(), "header");
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
            .title("AWE Rest API")
            .version("1.1")
            .description("Rest API to access AWE services")
            .contact(new Contact("AWE", "https://docs.aweframework.com/docs/rest/", "awe@almis.com"))
            .termsOfServiceUrl("https://gitlab.com/aweframework/awe/-/blob/master/LICENSE.md")
            .license("Apache 2.0")
            .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html")
            .build();
  }
}
