package com.almis.awe.rest.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * AWE Rest module configuration properties
 */
@ConfigurationProperties(prefix = "awe.rest")
@Data
public class AweRestConfigProperties {
  /**
   * Enable awe rest module.
   *
   * Default value "false"
   */
  private boolean enabled;
  /**
   * Open API Info title
   * Default value "AWE Rest API"
   */
  private String title = "AWE Rest API";
  /**
   * Open API Info description.
   */
  private String description = "Rest API to access AWE services";
  /**
   * Open API Info version.
   * Default value "v1.0.0"
   */
  private String version = "v1.0.0";
  /**
   * Open API Contact name.
   */
  private String contactName;
  /**
   * Open API Contact url.
   */
  private String contactUrl;
  /**
   * Open API Contact email.
   */
  private String contactEmail;
  /**
   * Open API License name.
   */
  private String licenseName = "Apache 2.0";
  /**
   * Open API License url.
   */
  private String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.html";
  /**
   * Open API Terms of service url.
   */
  private String termsOfServiceUrl;
  /**
   * Open API External doc description.
   */
  private String externalDocDescription;
  /**
   * Open API External doc url.
   */
  private String externalDocUrl;


  @NestedConfigurationProperty
  private JWTProperties jwt;
}
