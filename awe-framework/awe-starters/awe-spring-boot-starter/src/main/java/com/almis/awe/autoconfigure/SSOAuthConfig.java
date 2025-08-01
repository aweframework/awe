package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.constants.SecurityEndpoints;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationFailureHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler;
import com.almis.awe.security.multitenant.MultiTenantClientRegistrationRepository;
import com.almis.awe.security.multitenant.MultiTenantOAuth2AuthenticationEntryPoint;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ErrorPageService;
import com.almis.awe.session.AweSessionDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SSO OAuth configuration class
 */
@Configuration
@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SSOAuthConfig {

  // Autowired services
  private final AccessService accessService;
  private final AweSessionDetails sessionDetails;
  private final SecurityConfigProperties securityConfigProperties;
  private final PublicQueryMaintainAuthorization publicQueryMaintainAuthorization;
  private final ErrorPageService errorPageService;

  // Optional Autowired dependencies
  private final MultiTenantOAuth2Config multiTenantConfig;
  private final MultiTenantFilter multiTenantFilter;

  // Constants
  public static final SecurityEndpoints SECURITY_ENDPOINTS = new SecurityEndpoints();

  /**
   * Constructor for SSOAuthConfig.
   *
   * @param accessService Service to access specific application-level actions or data.
   * @param sessionDetails Session details containing context and session-specific data.
   * @param securityConfigProperties Security configuration properties for the application.
   * @param publicQueryMaintainAuthorization Authorization handler for public query maintenance.
   * @param multiTenantConfig Optional multi-tenant OAuth2 configuration.
   * @param errorPageService Error page generate service.
   * @param multiTenantFilter Optional multi-tenant filter for tenant-level request handling.
   */
  public SSOAuthConfig(AccessService accessService, AweSessionDetails sessionDetails, SecurityConfigProperties securityConfigProperties, PublicQueryMaintainAuthorization publicQueryMaintainAuthorization, ErrorPageService errorPageService,
											 Optional<MultiTenantOAuth2Config> multiTenantConfig,
											 Optional<MultiTenantFilter> multiTenantFilter) {
    this.accessService = accessService;
    this.sessionDetails = sessionDetails;
    this.securityConfigProperties = securityConfigProperties;
    this.publicQueryMaintainAuthorization = publicQueryMaintainAuthorization;
		this.errorPageService = errorPageService;
		this.multiTenantConfig = multiTenantConfig.orElse(null);
    this.multiTenantFilter = multiTenantFilter.orElse(null);
  }

  /**
   * Configures the security filter chain for handling HTTP requests, OAuth2 login, and logout.
   *
   * @param http HttpSecurity object to define web-based security at the HTTP level
   * @return SecurityFilterChain for filtering and securing HTTP requests
   * @throws Exception in case of an error during configuration
   */
  @Bean
  public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {

    // Add multi-tenant filter if enabled
    if (multiTenantConfig != null && multiTenantConfig.isEnabled()) {
      log.info("Multi-tenant support is enabled. Adding multi-tenant filter");
      http.addFilterBefore(multiTenantFilter, BasicAuthenticationFilter.class);
    }

    // Skip the native app login screen
    if (!securityConfigProperties.getSso().isAutoLaunch()) {
      http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/").permitAll());
    }
    // Configures authorization rules for different endpoints
    http.authorizeHttpRequests(authorize -> authorize
            // Web resources
            .requestMatchers(SECURITY_ENDPOINTS.getWebResourcesRequestMatchers()).permitAll()
            // Public actions
            .requestMatchers(SECURITY_ENDPOINTS.getPublicActionsRequestMatchers()).permitAll()
            // File and upload controllers
            .requestMatchers(SECURITY_ENDPOINTS.getFileRequestMatchers()).permitAll()
            // Public queries and maintains
            .requestMatchers(SECURITY_ENDPOINTS.getAuthenticatedRequestMatchers()).access(publicQueryMaintainAuthorization)
            // Requires authentication for any other request
            .anyRequest().authenticated()
        )
        // Configures OAuth2 login settings
        .oauth2Login(oauth2 -> {
              if (!securityConfigProperties.getSso().isAutoLaunch()) {
                oauth2.loginPage("/");
              }
              // Handle after successful login
              oauth2.successHandler(authSuccessHandler());
              // Configure multi-tenant client registration repository if enabled
              if (multiTenantConfig != null && multiTenantConfig.isEnabled()) {
                log.debug("Configuring OAuth2 login with multi-tenant client registration repository");
                oauth2.clientRegistrationRepository(clientRegistrationRepository());
              }
              oauth2.failureHandler(authFailureHandler());
            }
        )
        // Csrf SPA customize
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
        )
        // Add csrf cookie filter
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
        // Configures logout settings
        .logout(logout -> logout
            .logoutUrl("/action/logout")
            .addLogoutHandler(new AweLogoutHandler(sessionDetails))
        );

    // CSRF config
    if (securityConfigProperties.isSameOriginEnable()) {
      http.headers(headers ->
          headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
    }

    // Configure exception handling
    if (multiTenantConfig != null && multiTenantConfig.isEnabled() && securityConfigProperties.getSso().isAutoLaunch()) {
      log.debug("Configuring OAuth2 entrypoint with multi-tenant to handle auto-launch authentication flow");
      http.exceptionHandling(exceptions ->
          exceptions.authenticationEntryPoint(
              new MultiTenantOAuth2AuthenticationEntryPoint(securityConfigProperties
          ))
      );
    }


    return http.build();
  }

  interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
  }

  /**
   * Multi-tenant client registration repository
   *
   * @return ClientRegistrationRepository for multi-tenant support
   */
  @Bean
  @ConditionalOnProperty(prefix = "awe.security.sso.multitenant", name = "enabled", havingValue = "true")
  public ClientRegistrationRepository clientRegistrationRepository() {
    log.info("Configuring multi-tenant OAuth2 client registration repository");
    return new MultiTenantClientRegistrationRepository(multiTenantConfig);
  }

  /**
   * Keycloak authorities converter
   *
   * @return Authority's converter
   */
  @Bean
  AuthoritiesConverter realmRolesAuthoritiesConverter() {

    return claims -> {
      @SuppressWarnings("unchecked") var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
      @SuppressWarnings("unchecked") var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
      return roles.stream().flatMap(Collection::stream)
          .map(SimpleGrantedAuthority::new)
          .map(grantedAuthority -> {
            log.debug("Granted authority {} converted", (grantedAuthority));
            return (GrantedAuthority) grantedAuthority;
          })
          .toList();
    };
  }

  /**
   * Granted authorities' mapper. Used in mapping user roles
   *
   * @param authoritiesConverter authority's converter
   * @return GrantedAuthoritiesMapper of user
   */
  @Bean
  GrantedAuthoritiesMapper authenticationConverter(Converter<Map<String, Object>, Collection<GrantedAuthority>> authoritiesConverter) {
    return authorities -> authorities.stream()
        .filter(OidcUserAuthority.class::isInstance)
        .map(OidcUserAuthority.class::cast)
        .map(OidcUserAuthority::getIdToken)
        .map(OidcIdToken::getClaims)
        .map(authoritiesConverter::convert).filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Configures and provides a bean for handling successful OAuth2 authentication events.
   * This handler is responsible for processing authentication success scenarios
   * and managing user-specific details based on the OAuth2 token.
   *
   * @return AweOauth2AuthenticationSuccessHandler instance for managing successful authentication flows
   */
  @Bean
  @ConditionalOnMissingBean
  public AweOauth2AuthenticationSuccessHandler authSuccessHandler() {
    return new AweOauth2AuthenticationSuccessHandler(accessService);
  }

  /**
   * Creates an instance of AweOauth2AuthenticationFailureHandler, which is responsible
   * for handling authentication failures during an OAuth2 login process.
   *
   * @return an instance of AweOauth2AuthenticationFailureHandler to manage OAuth2 authentication failures
   */
  @Bean
  @ConditionalOnMissingBean
  public AweOauth2AuthenticationFailureHandler authFailureHandler() {
    return new AweOauth2AuthenticationFailureHandler(errorPageService);
  }
}
