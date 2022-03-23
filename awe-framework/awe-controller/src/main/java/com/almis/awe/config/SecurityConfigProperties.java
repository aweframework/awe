package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ConfigurationProperties(prefix = "awe.security")
@Validated
@Data
public class SecurityConfigProperties {
  /**
   * Master key used in encryption process.
   * Default value B1Le3s%25abc75TeBe05
   */
  private String masterKey = "B1Le3s%25abc75TeBe05";
  /**
   * Enable json encryption in between client-server
   */
  private boolean jsonEncryptEnable = false;
  /**
   * Encrypted parameter list name.
   * Default value p
   */
  private String jsonParameter = "p";

  /**
   * Default restriction set (default should be the most restricted).
   * Default value general
   */
  private String defaultRestriction = "general";

  /**
   * Role prefix to build user grants in spring security
   * Default value ROLE_
   */
  private String rolePrefix = "ROLE_";

  /**
   * Allows paths for xstream serializer.
   * Default value "java.*", "com.almis.awe.model.entities.**"
   */
  private String[] xstreamAllowPaths = {"java.*", "com.almis.awe.model.entities.**"};

  /**
   * Password regex pattern. Used in criteria validation field
   * Default value .*
   */
  private String passwordPattern = ".*";

  /**
   * Password minimum length. Used in criteria validation field
   * Default value .*
   */
  private int passwordMinLength = 4;

  /**
   * Authentication mode
   * Default value BBDD (Database)
   */
  private AuthType authMode = AuthType.BBDD;

  /**
   * Authentication custom class providers. Used to implement your custom authentication provider
   */
  private List<String> authCustomProviders;

  /**
   * Enable same origin in http headers. Avoid cross domain frame requests.
   * Default value true;
   */
  private boolean sameOriginEnable = true;
  /**
   * List with allowed origin paths separate by commas.
   * Default value *;
   */
  private String[] allowedOriginPatterns = {"*"};

  @NestedConfigurationProperty
  @Valid
  private Ldap ldap = new Ldap();

  /**
   * Ldap authentication configuration properties
   */
  @Data
  public static class Ldap {
    /**
     * Ldap user login property name.
     * Examples of filters:
     *
     * In WINDOWS environment sAMAccountName={0}
     * In UNIX environment uid={0}
     */
    private String userFilter = "sAMAccountName={0}";
    /**
     * Ldap server url. Set server values separate by commas when you need config more than one.
     * Default value ldap://localhost:389
     */
    private String[] url = {"ldap://localhost:389"};
    /**
     * Ldap base domain for search. Default value empty (Root Dn)
     */
    private String baseDn = "";
    /**
     * Ldap user bind search pattern.
     */
    private String userBind;
    /**
     * Ldap user bind password.
     */
    private String passwordBind;
    /**
     * Ldap connection timeout in millis. Set com.sun.jndi.ldap.connect.timeout environment property
     * Default value 5s
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectTimeout = Duration.ofMillis(5000);

    /**
     * Ldap read timeout in millis. Set com.sun.jndi.ldap.read.timeout environment property
     * Default value 5s
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration readTimeout = Duration.ofMillis(5000);
  }
}