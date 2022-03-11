package com.almis.awe.rest.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * AWE Rest module configuration properties
 */
@ConfigurationProperties(prefix = "awe.rest.api")
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
  private String docTitle = "AWE Rest API";
  /**
   * Open API Info description.
   */
  private String docDescription = "Rest API to access AWE services";
  /**
   * Open API Info version.
   * Default value "v1.0.0"
   */
  private String docVersion = "v1.0.0";
  /**
   * Open API Contact name.
   */
  private String docContactName;
  /**
   * Open API Contact url.
   */
  private String docContactUrl;
  /**
   * Open API Contact email.
   */
  private String docContactEmail;
  /**
   * Open API License name.
   */
  private String docLicenseName = "Apache 2.0";
  /**
   * Open API License url.
   */
  private String docLicenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.html";
  /**
   * Open API Terms of service url.
   */
  private String docTermsOfServiceUrl;
  /**
   * Open API External doc description.
   */
  private String docExternalDescription;
  /**
   * Open API External doc url.
   */
  private String docExternalUrl;

  @NestedConfigurationProperty
  private JWTProperties jwt;
}
