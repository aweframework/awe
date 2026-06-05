package com.almis.awe.rest.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.autoconfigure.config.JWTProperties;
import com.almis.awe.rest.autoconfigure.config.RestAuthenticationMode;
import com.almis.awe.rest.autoconfigure.security.AweRestAudienceValidator;
import com.almis.awe.rest.autoconfigure.security.AweRestJwtAuthenticationConverter;
import com.almis.awe.rest.autoconfigure.security.AweRestJwtValidationFactory;
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
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;

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
  private final AweRestConfigProperties restConfigProperties;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;

  @Autowired
  public RestSecurityConfiguration(PublicQueryMaintainAuthorization publicQueryMaintainAuthorization,
                                   AuthenticationManager authenticationManager,
                                   UserDetailsService userDetailsService,
                                   ObjectMapper objectMapper,
                                   AweRestConfigProperties restConfigProperties,
                                   BaseConfigProperties baseConfigProperties,
                                   SecurityConfigProperties securityConfigProperties) {
    this.publicQueryMaintainAuthorization = publicQueryMaintainAuthorization;
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.objectMapper = objectMapper;
    this.restConfigProperties = restConfigProperties;
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
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
    configureCommonChain(httpSecurity);

    if (isOauth2ResourceServerMode()) {
      configureOauth2Mode(httpSecurity);
    } else {
      configureLocalJwtMode(httpSecurity);
    }

    return httpSecurity.build();
  }

  private void configureCommonChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.securityMatcher(API_URL_LIST)
      .authorizeHttpRequests(httpRequest -> {
        if (isOauth2ResourceServerMode()) {
          httpRequest.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/authenticate")).denyAll();
        } else {
          httpRequest.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/authenticate")).permitAll();
        }

        httpRequest
          .requestMatchers(
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**")
          ).permitAll()
          .requestMatchers(
            PathPatternRequestMatcher.withDefaults().matcher("/api/public/data/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/public/maintain/**")
          ).access(publicQueryMaintainAuthorization)
          .anyRequest().authenticated();
      })
      .exceptionHandling(exceptionHandlingConfig ->
        exceptionHandlingConfig.authenticationEntryPoint(new JWTAuthenticationEntryPoint(objectMapper)))
      .sessionManagement(sessionManagementConfig ->
        sessionManagementConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .csrf(AbstractHttpConfigurer::disable);
  }

  private void configureLocalJwtMode(HttpSecurity httpSecurity) {
    httpSecurity.addFilterBefore(new JWTAuthenticationFilter(authenticationManager, objectMapper, getBean(JWTTokenService.class)), UsernamePasswordAuthenticationFilter.class);
    httpSecurity.addFilterBefore(new JWTAuthorizationFilter(authenticationManager, userDetailsService, getBean(JWTTokenService.class)), UsernamePasswordAuthenticationFilter.class);
  }

  private void configureOauth2Mode(HttpSecurity httpSecurity) throws Exception {
    JwtDecoder jwtDecoder = buildJwtDecoder(restConfigProperties.getOauth2ResourceServer().getJwt());
    AweRestJwtAuthenticationConverter authenticationConverter = new AweRestJwtAuthenticationConverter(
      baseConfigProperties,
      securityConfigProperties,
      restConfigProperties
    );

    httpSecurity.oauth2ResourceServer(oauth2ResourceServer ->
      oauth2ResourceServer.jwt(jwtConfigurer -> jwtConfigurer
        .decoder(jwtDecoder)
        .jwtAuthenticationConverter(authenticationConverter)
      )
    );
  }

  private JwtDecoder buildJwtDecoder(AweRestConfigProperties.Jwt jwtProperties) {
    JwtDecoder jwtDecoder;

    if (StringUtils.hasText(jwtProperties.getJwkSetUri())) {
      jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwtProperties.getJwkSetUri()).build();
    } else {
      jwtDecoder = JwtDecoders.fromIssuerLocation(jwtProperties.getIssuerUri());
    }

    OAuth2TokenValidator<Jwt> baseValidator = StringUtils.hasText(jwtProperties.getIssuerUri())
      ? JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuerUri())
      : JwtValidators.createDefault();

    OAuth2TokenValidator<Jwt> audienceValidator = new AweRestAudienceValidator(jwtProperties.getAudiences());
    OAuth2TokenValidator<Jwt> composedValidator = AweRestJwtValidationFactory.composeValidators(baseValidator, audienceValidator);

    if (jwtDecoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
      nimbusJwtDecoder.setJwtValidator(composedValidator);
    }

    return jwtDecoder;
  }

  private boolean isOauth2ResourceServerMode() {
    return restConfigProperties.getAuth().getMode() == RestAuthenticationMode.OAUTH2_RESOURCE_SERVER;
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