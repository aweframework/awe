package com.almis.awe.rest.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.autoconfigure.config.JWTProperties;
import com.almis.awe.rest.security.JWTAuthenticationEntryPoint;
import com.almis.awe.rest.security.JWTAuthenticationFilter;
import com.almis.awe.rest.security.JWTAuthorizationFilter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * REST security configuration
 */
@EnableConfigurationProperties({AweRestConfigProperties.class, JWTProperties.class})
@Configuration
public class RestSecurityConfiguration extends ServiceConfig {

  private final PublicQueryMaintainAuthorization publicQueryMaintainAuthorization;

  private static final String[] API_URL_LIST = {
          "/api/**",
          // -- Swagger UI v3 (OpenAPI)
          "/v3/api-docs/**",
          "/swagger-ui/**"
  };

  // Autowire services
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  @Autowired
  public RestSecurityConfiguration(PublicQueryMaintainAuthorization publicQueryMaintainAuthorization, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, ObjectMapper objectMapper) {
    this.publicQueryMaintainAuthorization = publicQueryMaintainAuthorization;
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.objectMapper = objectMapper;
  }

  /**
   * Awe Rest http security filter chain
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean(name = "aweRestSecurityFilterChain")
  @Order(99)
  public SecurityFilterChain restFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.securityMatcher(API_URL_LIST).authorizeHttpRequests(httpRequest -> httpRequest
                // Swagger UI and api authenticate
                .requestMatchers(
										PathPatternRequestMatcher.withDefaults().matcher("/api/authenticate"),
										PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**")
								).permitAll()
                // Filter public queries and maintains
                .requestMatchers(
										PathPatternRequestMatcher.withDefaults().matcher("/api/public/data/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/api/public/maintain/**")
								).access(publicQueryMaintainAuthorization)
                // Any requests needs be authenticated
                .anyRequest().authenticated()
            )
            // Handles unauthorized attempts to access protected URLS
            .exceptionHandling(exceptionHandlingConfig ->
                exceptionHandlingConfig.authenticationEntryPoint(new JWTAuthenticationEntryPoint(objectMapper)))
            // No session cookie for API endpoints
            .sessionManagement(sessionManagementConfig ->
                sessionManagementConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable csrf
            .csrf(AbstractHttpConfigurer::disable);

    // Add JWT (Json web token) filters
    httpSecurity.addFilterBefore(new JWTAuthenticationFilter(authenticationManager, objectMapper, getBean(JWTTokenService.class)), UsernamePasswordAuthenticationFilter.class);
    httpSecurity.addFilterBefore(new JWTAuthorizationFilter(authenticationManager, userDetailsService, getBean(JWTTokenService.class)), UsernamePasswordAuthenticationFilter.class);

    return httpSecurity.build();
  }

  @Bean
  ModelMapper modelMapper() {
    return new ModelMapper();
  }

  /**
   * JWT Token service
   *
   * @return JWTTokenService
   */
  @Bean
  JWTTokenService jwtTokenService(JWTProperties jwtProperties) {
    return new JWTTokenService(jwtProperties.getAuthorizationHeader(),
      jwtProperties.getPrefix(),
      jwtProperties.getSecret(),
      jwtProperties.getIssuer(),
      jwtProperties.getExpirationTime());
  }
}