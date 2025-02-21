package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.constants.SecurityEndpoints;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler;
import com.almis.awe.service.AccessService;
import com.almis.awe.session.AweSessionDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  // Constants
  public static final SecurityEndpoints SECURITY_ENDPOINTS = new SecurityEndpoints();

  @Value("${awe.security.sso.auto-launch}")
  private boolean autoLaunch;

  /**
   * SSOAuthConfig constructor
   *
   * @param accessService            Access service
   * @param sessionDetails           AWE session details
   * @param securityConfigProperties AWE security configuration
   */
  public SSOAuthConfig(AccessService accessService, AweSessionDetails sessionDetails, SecurityConfigProperties securityConfigProperties, PublicQueryMaintainAuthorization publicQueryMaintainAuthorization) {
    this.accessService = accessService;
    this.sessionDetails = sessionDetails;
    this.securityConfigProperties = securityConfigProperties;
    this.publicQueryMaintainAuthorization = publicQueryMaintainAuthorization;
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
    // Skip native app login screen
    if (!autoLaunch) {
      http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/").permitAll()
      );
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
        .oauth2Login(oauth2 -> oauth2.successHandler(authSuccessHandler()) // Handle after successful login
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

    return http.build();
  }

  interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
  }

  /**
   * Keycloak authorities converter
   *
   * @return Authorities converter
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
   * @param authoritiesConverter authorities converter
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

  @Bean
  @ConditionalOnMissingBean
  public AweOauth2AuthenticationSuccessHandler authSuccessHandler() {
    return new AweOauth2AuthenticationSuccessHandler(accessService);
  }
}