package com.almis.awe.rest.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.security.JWTAuthenticationEntryPoint;
import com.almis.awe.rest.security.JWTAuthenticationFilter;
import com.almis.awe.rest.security.JWTAuthorizationFilter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.service.user.AweUserDetailService;
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

/**
 * REST security configuration
 */
@DependsOn("aweUserDetailsService")
@Configuration
@EnableConfigurationProperties(AweRestConfigProperties.class)
public class RestSecurityConfiguration extends ServiceConfig {

  // Autowire services
  private final AuthenticationManager authenticationManager;
  private final AweUserDetailService aweUserDetailService;
  private final ObjectMapper objectMapper;

  // White list urls
  private static final String[] AUTH_LIST = {
    // Rest API
    "/api/authenticate",
    "/api/public/**"
  };

  @Autowired
  public RestSecurityConfiguration(AuthenticationManager authenticationManager, AweUserDetailService aweUserDetailService, ObjectMapper objectMapper) {
    this.authenticationManager = authenticationManager;
    this.aweUserDetailService = aweUserDetailService;
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
              .and().antMatcher("/api/**").sessionManagement()
              // no session cookie for API endpoints
              .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
              .and().authorizeRequests().anyRequest().authenticated()
              // Handles unauthorized attempts to access protected URLS
              .and().exceptionHandling().authenticationEntryPoint(new JWTAuthenticationEntryPoint(objectMapper))
              // Add JWT (Json web token) filters
              .and().addFilter(new JWTAuthenticationFilter(authenticationManager, objectMapper, getBean(JWTTokenService.class)))
              .addFilter(new JWTAuthorizationFilter(authenticationManager, aweUserDetailService, getBean(JWTTokenService.class)));
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers(
              // -- swagger ui
              "/v2/api-docs",
              "/swagger-resources/**",
              "/swagger-ui/",
              "/swagger-ui/**",
              "/swagger-ui.html",
              "/webjars/**");
    }

    @Bean
    ModelMapper modelMapper() {
      return new ModelMapper();
    }

    /**
     * JWT Token service
     *
     * @param restConfigProperties Rest config properties
     * @return JWTTokenService
     */
    @Bean
    JWTTokenService jwtTokenService(AweRestConfigProperties restConfigProperties) {
      return new JWTTokenService(restConfigProperties.getAuthorizationHeader(),
              restConfigProperties.getJwtPrefix(),
              restConfigProperties.getJwtSecret(),
              restConfigProperties.getJwtIssuer(),
              restConfigProperties.getJwtExpirationTime());
    }
  }
}