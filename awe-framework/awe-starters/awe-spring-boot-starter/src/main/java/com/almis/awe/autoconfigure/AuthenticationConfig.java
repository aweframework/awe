package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authentication.encoder.Ripemd160PasswordEncoder;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.service.user.LdapAweUserDetailsMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication configuration class. Used to configure authentication providers.
 */
@Configuration
@Import(AweAutoConfiguration.class)
@EnableConfigurationProperties({SecurityConfigProperties.class, MultiTenantOAuth2Config.class})
public class AuthenticationConfig {

  // Timeout for Ldap socket connect
  private static final String LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";

  // Timeout for Ldap reading responses
  private static final String LDAP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";

  /**
   * Configure User detail service
   *
   * @param queryUtil       Query utilities
   * @param queryService    Query service
   * @return UserDetailService
   */
  @Bean
  public UserDAO userDAO(QueryUtil queryUtil, QueryService queryService) {
    return new UserDAOImpl(queryUtil, queryService);
  }

  /**
   * Configure User detail service
   *
   * @param baseConfigProperties Base config properties
   * @param securityConfigProperties Security config properties
   * @param userDAO User DAO
   * @return User detail service
   */
  @Bean
  public AweUserDetailService aweUserDetailsService(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, UserDAO userDAO) {
    return new AweUserDetailService(baseConfigProperties, securityConfigProperties, userDAO);
  }

  /**
   * Configure ldap user details mapper
   *
   * @param userDetailService user detail service
   * @return Ldap user details context mapper
   */
  @Bean
  public UserDetailsContextMapper userDetailsContextMapper(UserDetailsService userDetailService) {
    return new LdapAweUserDetailsMapper(userDetailService);
  }

  /**
   * Spring context source for ldap connection
   *
   * @return Ldap context
   */
  @Bean
  @ConditionalOnMissingBean
  public LdapContextSource ldapContextSource(SecurityConfigProperties securityConfigProperties) {
    // Environment properties
    Map<String, Object> environmentProperties = Collections.synchronizedMap(new HashMap<>());
    environmentProperties.put(LDAP_CONNECT_TIMEOUT, String.valueOf(securityConfigProperties.getLdap().getConnectTimeout().toMillis()));
    environmentProperties.put(LDAP_READ_TIMEOUT, String.valueOf(securityConfigProperties.getLdap().getReadTimeout().toMillis()));

    LdapContextSource ldapContextSource = new LdapContextSource();
    ldapContextSource.setBaseEnvironmentProperties(environmentProperties);
    ldapContextSource.setUrls(securityConfigProperties.getLdap().getUrl());
    ldapContextSource.setBase(securityConfigProperties.getLdap().getBaseDn());
    ldapContextSource.setUserDn(securityConfigProperties.getLdap().getUserBind());
    ldapContextSource.setPassword(securityConfigProperties.getLdap().getPasswordBind());
    ldapContextSource.setPooled(true);
    return ldapContextSource;
  }

  /**
   * Configure Ldap provider for bind auth
   *
   * @param ldapContextSource Ldap context
   * @return LDAP authentication provider
   */
  @Bean
  @ConditionalOnProperty(prefix = "awe.security", name = "auth-mode", havingValue = "ldap")
  public AuthenticationProvider ldapAuthenticationProvider(LdapContextSource ldapContextSource, UserDetailsContextMapper userDetailsContextMapper, SecurityConfigProperties securityConfigProperties) {

    // Bind authenticator with search filter
    final BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
    bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch("", "(" + securityConfigProperties.getLdap().getUserFilter() + ")", ldapContextSource));

    // Ldap provider
    final LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
    ldapAuthenticationProvider.setHideUserNotFoundExceptions(false);
    ldapAuthenticationProvider.setAuthoritiesMapper(new SimpleAuthorityMapper());
    ldapAuthenticationProvider.setUserDetailsContextMapper(userDetailsContextMapper);

    return ldapAuthenticationProvider;
  }

  /**
   * Configure DAO authentication provider
   *
   * @return DaoAuthenticationProvider
   */
  @Bean
  @ConditionalOnProperty(prefix = "awe.security", name = "auth-mode", havingValue = "bbdd", matchIfMissing = true)
  public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService) {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setPasswordEncoder(new Ripemd160PasswordEncoder());
    daoAuthenticationProvider.setUserDetailsService(userDetailsService);
    daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
    return daoAuthenticationProvider;
  }
}
