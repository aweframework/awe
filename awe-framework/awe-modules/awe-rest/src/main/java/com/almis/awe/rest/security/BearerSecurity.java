package com.almis.awe.rest.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Bearer authentication of the REST api, to be applied by another security filter chain that accepts the same token.
 *
 * <p>It is deliberately not a <code>Customizer&lt;HttpSecurity&gt;</code>: Spring Security applies every bean of that
 * type to all the filter chains of the application.
 */
@FunctionalInterface
public interface BearerSecurity {

  /**
   * Add the bearer authentication to a filter chain
   *
   * @param httpSecurity Http security of the chain
   * @throws Exception Error configuring the chain
   */
  void apply(HttpSecurity httpSecurity) throws Exception;
}
