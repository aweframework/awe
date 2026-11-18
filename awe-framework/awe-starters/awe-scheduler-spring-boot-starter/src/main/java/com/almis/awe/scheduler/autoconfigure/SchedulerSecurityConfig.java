package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.rest.security.BearerSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Scheduler REST api security configuration
 */
@Slf4j
@Configuration
public class SchedulerSecurityConfig extends ServiceConfig {

  /**
   * Security filter chain of the webhooks that launch tasks.
   *
   * <p>It matches only the webhook path and lets every request through: the signature of the message is the
   * authentication, checked by the controller over the raw body. No other path of the scheduler api is opened.
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean(name = "aweSchedulerWebhookSecurityFilterChain")
  @Order(97)
  public SecurityFilterChain webhookFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.securityMatcher("/scheduler/api/v1/webhooks/**")
        .authorizeHttpRequests(httpRequest -> httpRequest.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 stateless, the signature authenticates
        .sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return httpSecurity.build();
  }

  /**
   * Awe Scheduler http security filter chain.
   *
   * <p>Every request needs the same bearer token as the REST api, provided by the <code>restBearerSecurity</code>
   * bean of the awe-rest starter. Without that bean no request can authenticate and all of them are answered with
   * 401.
   *
   * @param httpSecurity   Http security
   * @param bearerSecurity Bearer authentication of the REST api, when the awe-rest starter is present
   * @return security filter chain
   * @throws Exception exception
   */
  @Bean(name = "aweSchedulerSecurityFilterChain")
  @Order(98)
  public SecurityFilterChain schedulerFilterChain(HttpSecurity httpSecurity,
      ObjectProvider<BearerSecurity> bearerSecurity) throws Exception {
    httpSecurity.securityMatcher("/scheduler/api/**")
        .authorizeHttpRequests(httpRequest -> httpRequest.anyRequest().authenticated())
        // Disable csrf
        .csrf(AbstractHttpConfigurer::disable)
        // No session cookie for API endpoints
        .sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    BearerSecurity bearer = bearerSecurity.getIfAvailable();
    if (bearer != null) {
      bearer.apply(httpSecurity);
    } else {
      log.warn("The awe-rest starter is not present: the scheduler api answers 401 to all requests");
      httpSecurity.exceptionHandling(handling ->
          handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    }
    return httpSecurity.build();
  }
}
