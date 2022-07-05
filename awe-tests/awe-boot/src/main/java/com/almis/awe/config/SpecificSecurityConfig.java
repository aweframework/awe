package com.almis.awe.config;

import com.almis.awe.test.listener.TestSessionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
    httpSecurity.antMatcher(actuatorEndpoint + "/**").anonymous()
      .and().csrf().disable();
    return httpSecurity.build();
  }

  @Bean
  public ServletListenerRegistrationBean<TestSessionListener> sessionListenerWithMetrics() {
    ServletListenerRegistrationBean<TestSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();

    listenerRegBean.setListener(new TestSessionListener());
    return listenerRegBean;
  }
}