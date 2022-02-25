package com.almis.awe.rest.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.autoconfigure.config.JWTProperties;
import com.almis.awe.rest.security.JWTAuthenticationEntryPoint;
import com.almis.awe.rest.security.JWTAuthenticationFilter;
import com.almis.awe.rest.security.JWTAuthorizationFilter;
import com.almis.awe.rest.service.JWTTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * REST security configuration
 */
@DependsOn("aweUserDetailsService")
@EnableConfigurationProperties({AweRestConfigProperties.class, JWTProperties.class})
@Configuration
public class RestSecurityConfiguration extends ServiceConfig {

  // White list urls
  private static final String[] AUTH_LIST = {
    // Rest API
    "/api/authenticate"
  };
  // Autowire services
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  @Autowired
  public RestSecurityConfiguration(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, ObjectMapper objectMapper) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.objectMapper = objectMapper;
  }

  /**
   * Rest security configuration adapter
   */
  @Configuration
  @Order(99)
  public class RestSecurityConfigurationImpl extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable().authorizeRequests()
        .antMatchers(AUTH_LIST).permitAll()
        // Filter public queries and maintains
        .antMatchers("/api/data/**").access("isAuthenticated() or @publicQueryMaintainFilter.isPublicQuery(request)")
        .antMatchers("/api/maintain/**").access("isAuthenticated() or @publicQueryMaintainFilter.isPublicMaintain(request)")
        .antMatchers("/api/public/data/**").access("@publicQueryMaintainFilter.isPublicQuery(request)")
        .antMatchers("/api/public/maintain/**").access("@publicQueryMaintainFilter.isPublicMaintain(request)")
        .and().antMatcher("/api/**").sessionManagement()
        // no session cookie for API endpoints
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and().authorizeRequests().anyRequest().authenticated()
        // Handles unauthorized attempts to access protected URLS
        .and().exceptionHandling().authenticationEntryPoint(new JWTAuthenticationEntryPoint(objectMapper))
        // Add JWT (Json web token) filters
        .and().addFilter(new JWTAuthenticationFilter(authenticationManager, objectMapper, getBean(JWTTokenService.class)))
        .addFilter(new JWTAuthorizationFilter(authenticationManager, userDetailsService, getBean(JWTTokenService.class)));
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers(
        // -- Swagger 2
        "/v2/api-docs",
        // -- Open API
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html");
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
}