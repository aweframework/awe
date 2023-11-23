package com.almis.awe.config;

import com.almis.awe.test.listener.TestSessionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Custom Spring security configuration
 */
@Configuration
public class SpecificSecurityConfig {

  @Value("${management.endpoints.web.base-path:/actuator}")
  private String actuatorEndpoint;

  /**
   * Custom http security filter chain
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean(name = "customSecurityFilterChain")
  @Order(2)
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    return httpSecurity.securityMatcher(actuatorEndpoint + "/**").anonymous()
            .and().csrf().disable()
            .build();
  }

  /**
   * H2 console http security filter chain
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean
  SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
    return httpSecurity
        .securityMatcher(antMatcher("/h2-console/**"))
        .authorizeHttpRequests( auth -> auth
            .requestMatchers(antMatcher("/h2-console/**")).permitAll())
        .csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher("/h2-console/**")))
        .headers(headers -> headers.frameOptions().disable())
        .build();
  }

  @Bean
  public ServletListenerRegistrationBean<TestSessionListener> sessionListenerWithMetrics() {
    ServletListenerRegistrationBean<TestSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();

    listenerRegBean.setListener(new TestSessionListener());
    return listenerRegBean;
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }
}