package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
    return httpSecurity.securityMatcher("/scheduler/api/**")
        .authorizeHttpRequests(httpRequest -> httpRequest
            // Filter /scheduler/api urls
            .requestMatchers(
								PathPatternRequestMatcher.withDefaults().matcher("/scheduler/api/**")
						).anonymous())
        // Disable csrf
        .csrf(AbstractHttpConfigurer::disable)
        // No session cookie for API endpoints
        .sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }
}