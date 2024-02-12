package com.almis.awe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Profile({"gitlab-ci"})
@Configuration
public class SecurityConfig {

  /**
   * Custom http security filter chain for test
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception Http security config error
   */
  @Bean(name = "testSecurityFilterChain")
  @Order(1)
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.securityMatcher("/alu-microservice/**",
                    "/alu-service-bis/**",
                    "/testapi/**")
            .authorizeHttpRequests(request -> request.anyRequest().permitAll())
            // Disable CSRF for microservices tests
            .csrf(AbstractHttpConfigurer::disable);

      return  httpSecurity.build();
  }
}
