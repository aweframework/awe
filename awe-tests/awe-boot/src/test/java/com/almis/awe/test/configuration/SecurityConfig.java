package com.almis.awe.test.configuration;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Profile({"gitlab-ci"})
@Order(1)
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Disable CSRF for microservices tests
    http.requestMatchers().antMatchers("/alu-microservice/**",
            "/alu-service-bis/**",
            "/testapi/**")
            .and().csrf().disable();
  }
}
