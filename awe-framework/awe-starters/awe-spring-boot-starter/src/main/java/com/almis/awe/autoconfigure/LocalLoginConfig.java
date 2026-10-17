package com.almis.awe.autoconfigure;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.audit.LoginAuditor;
import com.almis.awe.security.authentication.LocalLoginAuthenticationManager;
import com.almis.awe.security.authentication.LocalLoginPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Local login switch beans (see {@code awe.security.sso.local-login}).
 *
 * <p>The decorated {@code localLoginAuthenticationManager} must only be used by the login entry points: the standard
 * form login (as parent manager of the HttpSecurity builder), {@code /action/login} and {@code /api/authenticate}.
 * The shared {@code authenticationManager} bean, used by internal flows such as re-authentication, stays undecorated.</p>
 */
@Configuration(proxyBeanMethods = false)
public class LocalLoginConfig {

  /**
   * Policy that decides if a local login is allowed. Declare your own bean to replace it
   *
   * @param securityConfigProperties Security properties
   * @return Local login policy
   */
  @Bean
  @ConditionalOnMissingBean
  public LocalLoginPolicy localLoginPolicy(SecurityConfigProperties securityConfigProperties) {
    return new LocalLoginPolicy(securityConfigProperties);
  }

  /**
   * Authentication manager of the login entry points: applies the local login policy before the real authentication
   *
   * @param authenticationManager Shared (undecorated) authentication manager
   * @param localLoginPolicy      Local login policy
   * @param loginAuditor          Security audit of the logins (optional)
   * @return Authentication manager of the login entry points
   */
  @Bean
  public AuthenticationManager localLoginAuthenticationManager(@Qualifier("authenticationManager") AuthenticationManager authenticationManager,
                                                               LocalLoginPolicy localLoginPolicy,
                                                               ObjectProvider<LoginAuditor> loginAuditor) {
    LocalLoginAuthenticationManager manager = new LocalLoginAuthenticationManager(authenticationManager, localLoginPolicy);
    // Security audit of the logins and automatic lockout
    manager.setLoginAuditor(loginAuditor.getIfAvailable());
    return manager;
  }
}
