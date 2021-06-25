package com.almis.awe.config;

import com.almis.awe.test.listener.TestSessionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Custom Spring security configuration
 */
@Order(2)
@Configuration
public class SpecificSecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${management.endpoints.web.base-path:/actuator}")
  private String actuatorEndpoint;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.antMatcher(actuatorEndpoint + "/**").anonymous()
    .and().csrf().disable();
  }

  @Bean
  public ServletListenerRegistrationBean<TestSessionListener> sessionListenerWithMetrics() {
    ServletListenerRegistrationBean<TestSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();

    listenerRegBean.setListener(new TestSessionListener());
    return listenerRegBean;
  }
}