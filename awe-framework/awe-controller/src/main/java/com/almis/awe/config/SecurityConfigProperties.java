package com.almis.awe.config;

import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
  @Valid
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

  @NestedConfigurationProperty
  @Valid
  private Sso sso = new Sso();

  /**
   * SSO authentication configuration properties
   */
  @Data
  public static class Sso {
    /**
     * Enable SSO flow login
     */
    private boolean enabled = false;
    /**
     * Flag to enable auto provisioning users in oauth2 authentication process. Create new record in ope table with user
     */
    private boolean autoProvisionUser = true;
    /**
     * When Auto Launch is enabled, the login page will automatically
     * redirect the user to the OAuth authorization url, to log in with OAuth
     */
    private boolean autoLaunch = false;
    /**
     * Allow local (username and password) login when SSO is enabled. Set to {@code false} to make the identity
     * provider the only way to sign in: the login screen hides the local form and the local login endpoints
     * ({@code /action/login} and {@code /api/authenticate}) refuse every user except the ones listed in
     * {@code awe.security.sso.break-glass-users}. It has no effect when SSO is disabled.
     */
    private boolean localLogin = true;
    /**
     * Emergency (break-glass) users that can still sign in with username and password while
     * {@code awe.security.sso.local-login} is {@code false}, for example when the identity provider is down.
     * Empty by default (nobody). Every use is logged at WARN level. Keep the list short and use strong passwords.
     */
    private List<String> breakGlassUsers = new ArrayList<>();
    /**
     * Used to filtering granted authorities in post authentication process
     */
    private String filterAuthorityPrefix;
    /**
     * Claims of the ID token that carry the user's roles, read in order and joined. A dotted name walks nested
     * objects unless the token has a claim with that exact name. Examples: {@code realm_access.roles} (Keycloak realm
     * roles, the default), {@code resource_access.my-client.roles} (Keycloak client roles), {@code roles} (Microsoft
     * Entra ID app roles). When several roles carry the prefix, the first name in alphabetical order that exists as
     * an AWE profile is used. Ignored when the application declares its own {@code realmRolesAuthoritiesConverter} bean.
     */
    private List<String> rolesClaims = new ArrayList<>(List.of("realm_access.roles"));
    /**
     * Require a role from the identity provider that maps to an existing AWE profile (fail closed). When required and
     * no role maps, the SSO login is refused with an error page, for new and existing users alike, and the stored
     * profile and {@code awe.application.default-role} are not used. Not set by default, which means required when
     * {@code awe.security.sso.local-login} is {@code false} (IdP-first) and not required otherwise. Set it to
     * {@code false} to opt in to the default role fallback in IdP-first, or to {@code true} to require a role in any
     * SSO setup.
     */
    private Boolean requireRole;
    /**
     * When {@code false} (default), an existing user's profile is kept as-is and not overwritten
     * with the application default role ({@code awe.application.default-role}) when the SSO
     * provider claim carries no role (or none of the granted authorities match
     * {@code awe.security.sso.filter-authority-prefix}). Set to {@code true} to restore the
     * previous behaviour, where the default role always overwrites the stored profile in that case.
     */
    private boolean overwriteProfileWithDefaultRole = false;
		/**
		 * The name of the attribute used to extract the username during the SSO authentication process.
		 * This attribute is typically sourced from the claims provided by the OAuth2 or OIDC identity provider.
		 */
		private String userNameAttribute = "preferred_username";

		/**
		 * Enable the Azure SSO button. Used in Signin screen
		 */
		private boolean enableAzureButton = false;
		/**
		 * Enable Google SSO button. Used in signin screen
		 */
		private boolean enableGoogleButton = false;
		/**
		 * Enable the Generic SSO button. Used in signin screen
		 */
		private boolean enableGenericSsoButton = false;

    /**
     * OIDC back-channel logout: lets the identity provider end AWE sessions
     */
    @NestedConfigurationProperty
    @Valid
    private BackChannelLogout backChannelLogout = new BackChannelLogout();

		/**
		 * Whether an SSO login needs a provider role that maps to an AWE profile: the value of
		 * {@code awe.security.sso.require-role} when set, otherwise {@code true} only when local login is off (IdP-first)
		 *
		 * @return true if the login is refused when no provider role maps to an AWE profile
		 */
		public boolean isRoleRequired() {
			return requireRole != null ? requireRole : !localLogin;
		}
	}

	/**
	 * OIDC back-channel logout configuration properties
	 */
	@Data
	public static class BackChannelLogout {
		/**
		 * Enable OIDC back-channel logout when SSO is enabled. The identity provider calls
		 * {@code /logout/connect/back-channel/{registrationId}} with a signed logout token and AWE ends the matching
		 * sessions (web session and the AWE state kept for it, such as websocket connections). Off by default.
		 */
		private boolean enabled = false;
		/**
		 * Address AWE calls on itself to end each session named by a logout token, sending the session cookie. Placeholders
		 * {@code {baseUrl}}, {@code {baseScheme}}, {@code {baseHost}}, {@code {basePort}}, {@code {basePath}} and
		 * {@code {registrationId}} are resolved from the provider request. Empty by default, which uses
		 * {@code {baseUrl}/logout/connect/back-channel/{registrationId}}. Set it when the public address is not reachable
		 * from the server itself, for example
		 * {@code http://localhost:8080{basePath}/logout/connect/back-channel/{registrationId}}.
		 */
		private String logoutUri;
		/**
		 * Connection timeout of the internal logout call made for each session named by a logout token. Keeps the
		 * provider request from hanging when the logout address cannot be reached. Default 2 seconds.
		 */
		private Duration connectTimeout = Duration.ofSeconds(2);
		/**
		 * Read timeout of the internal logout call made for each session named by a logout token. A session whose call
		 * times out is reported to the provider as not ended. Default 5 seconds.
		 */
		private Duration readTimeout = Duration.ofSeconds(5);
	}

  /**
   * Ldap authentication configuration properties
   */
  @Data
  public static class Ldap {
    /**
     * Ldap user login property name.
     * Examples of filters:
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
