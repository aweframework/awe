package com.almis.awe.rest.autoconfigure.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AWE Rest module configuration properties
 */
@ConfigurationProperties(prefix = "awe.rest.api")
@Validated
@Data
public class AweRestConfigProperties {
  private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
  private static final String EMAIL_CLAIM = "email";

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
  private Auth auth = new Auth();

  @NestedConfigurationProperty
  private Oauth2ResourceServer oauth2ResourceServer = new Oauth2ResourceServer();

  @NestedConfigurationProperty
  private Openapi openapi = new Openapi();

  @NestedConfigurationProperty
  private JWTProperties jwt = new JWTProperties();

  /**
   * Ensure oauth2 mode has audiences configured.
   *
   * @return True when valid
   */
  @AssertTrue(message = "awe.rest.api.oauth2-resource-server.jwt.audiences must not be empty when auth.mode=oauth2-resource-server")
  public boolean isOauth2AudiencesConfigured() {
    if (auth.getMode() != RestAuthenticationMode.OAUTH2_RESOURCE_SERVER) {
      return true;
    }
    return oauth2ResourceServer != null
      && oauth2ResourceServer.getJwt() != null
      && !oauth2ResourceServer.getJwt().getAudiences().isEmpty();
  }

  /**
   * Ensure oauth2 mode has issuer or jwk set uri configured.
   *
   * @return True when valid
   */
  @AssertTrue(message = "awe.rest.api.oauth2-resource-server.jwt.issuer-uri or awe.rest.api.oauth2-resource-server.jwt.jwk-set-uri must be configured when auth.mode=oauth2-resource-server")
  public boolean isOauth2IssuerOrJwkConfigured() {
    if (auth.getMode() != RestAuthenticationMode.OAUTH2_RESOURCE_SERVER) {
      return true;
    }
    Jwt jwtProperties = oauth2ResourceServer.getJwt();
    return jwtProperties != null &&
      (StringUtils.hasText(jwtProperties.getIssuerUri()) || StringUtils.hasText(jwtProperties.getJwkSetUri()));
  }

  /**
   * Authentication mode settings.
   */
  @Data
  public static class Auth {
    private RestAuthenticationMode mode = RestAuthenticationMode.LOCAL_JWT;
  }

  /**
   * OAuth2 Resource Server settings.
   */
  @Data
  public static class Oauth2ResourceServer {
    private Jwt jwt = new Jwt();
    private Principal principal = new Principal();
  }

  /**
   * OAuth2 Resource Server JWT settings.
   */
  @Data
  public static class Jwt {
    private String issuerUri;
    private String jwkSetUri;
    private List<String> audiences = new ArrayList<>();
  }

  /**
   * OAuth2 principal mapping settings.
   */
  @Data
  public static class Principal {
    private List<String> delegatedUsernameClaims = new ArrayList<>(List.of(PREFERRED_USERNAME_CLAIM, "upn", EMAIL_CLAIM, "oid"));
    private List<String> delegatedDisplayNameClaims = new ArrayList<>(List.of("name", PREFERRED_USERNAME_CLAIM, EMAIL_CLAIM));
    private List<String> delegatedEmailClaims = new ArrayList<>(List.of(EMAIL_CLAIM, "upn", PREFERRED_USERNAME_CLAIM));
    private List<String> clientIdClaims = new ArrayList<>(List.of("azp", "appid", "client_id", "clientId", "sub"));
    private List<String> clientDisplayNameClaims = new ArrayList<>(List.of("app_displayname", "azp", "appid", "client_id", "clientId"));
    private String clientPrincipalPrefix = "client:";
    private String profileClaim;
    private String defaultProfile;
  }

  /**
   * OpenAPI specific settings.
   */
  @Data
  public static class Openapi {
    private Oauth2 oauth2 = new Oauth2();
  }

  /**
   * OpenAPI OAuth2 settings.
   */
  @Data
  public static class Oauth2 {
    private String authorizationUrl;
    private String tokenUrl;
    private String clientId;
    private boolean usePkce = true;
    private Map<String, String> scopes = new LinkedHashMap<>();
  }
}
