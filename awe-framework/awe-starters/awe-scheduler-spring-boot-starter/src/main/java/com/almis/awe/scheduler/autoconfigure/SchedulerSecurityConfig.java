package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * REST security configuration
 */
@Configuration
public class SchedulerSecurityConfig extends ServiceConfig {

  /**
   * Awe Scheduler http security filter chain
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean(name = "aweSchedulerSecurityFilterChain")
  @Order(98)
  public SecurityFilterChain schedulerFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
      // Filter /api urls
      .antMatcher("/scheduler/api/**").anonymous()
      // Disable csrf
      .and().csrf().disable()
      // No session cookie for API endpoints
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().authorizeRequests().anyRequest().permitAll();

    return httpSecurity.build();
  }
}