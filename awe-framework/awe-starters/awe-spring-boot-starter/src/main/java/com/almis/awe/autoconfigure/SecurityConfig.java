package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweHttpServletRequestWrapper;
import com.almis.awe.config.AuthType;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.accessbean.LoginAccessControl;
import com.almis.awe.security.authentication.encoder.Ripemd160PasswordEncoder;
import com.almis.awe.security.authentication.entrypoint.ActionAuthenticationEntryPoint;
import com.almis.awe.security.authentication.filter.JsonAuthenticationFilter;
import com.almis.awe.security.authentication.filter.PublicQueryMaintainFilter;
import com.almis.awe.security.handler.AweAccessDeniedHandler;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.service.*;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.service.user.LdapAweUserDetailsMapper;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableConfigurationProperties(value = {BaseConfigProperties.class, SecurityConfigProperties.class, TotpConfigProperties.class})
@Slf4j
public class SecurityConfig extends ServiceConfig {

  // Timeout for Ldap socket connect
  private static final String LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";

  // Timeout for Ldap reading responses
  private static final String LDAP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";

  // White list urls
  private static final String[] AUTH_LIST = {
          "/error**",
          "/websocket/**",
          "/template/**",
          "/settings",
          "/css/**",
          "/action/get-locals",
          "/action/screen-data",
          "/action/encrypt",
          "/action/get-file",
          "/action/file-info",
          "/action/delete-file",
          "/action/view-pdf-file",
          "/screen/**",
          // File and upload controllers
          "/file/text",
          "/file/stream",
          "/file/download",
          "/file/upload",
          "/file/delete",
          // React engine
          "/screen-data",
          "/locales/**",
          // Access controllers
          "/access/**"
  };

  // Data list urls
  private static final String[] DATA_LIST = {
    "/action/data*/**",
    "/action/update*/**",
    "/action/control*/**",
    "/action/unique*/**",
    "/action/value*/**",
    "/action/validate*/**",
    "/action/subscribe*/**",
    "/action/tree-branch*/**"
  };

  // Maintain list urls
  private static final String[] MAINTAIN_LIST = {
    "/action/maintain*/**",
    "/action/get-file-maintain/**",
    "/file/stream/maintain/**",
    "/file/download/maintain/**"
  };

  // Autowired services
  private final AweElements elements;
  private final ActionService actionService;
  private final ObjectMapper objectMapper;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;

  @Value("${session.cookie.name:JSESSIONID}")
  private String cookieName;

  /**
   * Autowired constructor
   *
   * @param elements                 Awe elements
   * @param objectMapper             Object mapper
   * @param actionService            Action service
   * @param baseConfigProperties     Base configuration properties
   * @param securityConfigProperties Security configuration properties
   */
  @Autowired
  public SecurityConfig(AweElements elements, ObjectMapper objectMapper,  ActionService actionService, BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties) {
    this.elements = elements;
    this.objectMapper = objectMapper;
    this.actionService = actionService;
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.actionService = actionService;
  }

  private enum AUTHENTICATION_MODE {
    LDAP("ldap"),
    BBDD("bbdd"),
    IN_MEMORY("in_memory"),
    CUSTOM("custom");

    private final String mode;

    AUTHENTICATION_MODE(String mode) {
      this.mode = mode;
    }

    public static AUTHENTICATION_MODE fromValue(String value) {
      if (value.equalsIgnoreCase(LDAP.getValue())) {
        return LDAP;
      } else if (value.equalsIgnoreCase(BBDD.getValue())) {
        return BBDD;
      } else if (value.equalsIgnoreCase(IN_MEMORY.getValue())) {
        return IN_MEMORY;
      } else if (value.equalsIgnoreCase(CUSTOM.getValue())) {
        return CUSTOM;
      }
      return null;
    }

    public String getValue() {
      return mode;
    }
  }

  /**
   * Configuration class for spring security
   */
  @Configuration
  public class AWEScreenSecurityAdapter extends WebSecurityConfigurerAdapter {

    /**
     * Spring security configuration
     *
     * @param http Http security object
     * @throws Exception Configure error
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.antMatcher("/**")
        .headers().xssProtection().block(false).and()
        .and().authorizeRequests()
        // Web
        .antMatchers(AUTH_LIST).permitAll()
        // Filter public queries and maintains
        .antMatchers(DATA_LIST).access("isAuthenticated() or @publicQueryMaintainFilter.isPublicQuery(request)")
        .antMatchers(MAINTAIN_LIST).access("isAuthenticated() or @publicQueryMaintainFilter.isPublicMaintain(request)")
        .anyRequest().authenticated()
        // Login redirect
        .and().formLogin()
        .loginPage("/")
        .permitAll()
        // Exceptions handling
        .and().exceptionHandling().accessDeniedHandler(accessDeniedHandler())
        .and().exceptionHandling().defaultAuthenticationEntryPointFor(actionAuthenticationEntryPoint(getBean(AweSessionDetails.class)), new AntPathRequestMatcher("/action/**"))
        // Add a filter to parse login parameters
        .and().addFilterAt(getBean(JsonAuthenticationFilter.class), UsernamePasswordAuthenticationFilter.class)
        // Add logout handler
        .logout().logoutUrl("/action/logout")
        .deleteCookies(cookieName).clearAuthentication(true).invalidateHttpSession(true)
        .addLogoutHandler(getBean(AweLogoutHandler.class))
        // CSRF
        .and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        // ignore our stomp endpoints since they are protected using Stomp headers
        .ignoringAntMatchers("/websocket/**");

      if (securityConfigProperties.isSameOriginEnable()) {
        http.headers().frameOptions().sameOrigin();
      }
    }

    /**
     * Allows access to static resources, bypassing Spring security.
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers(
        // Web resources (except css)
        "/js/**",
        "/images/**",
        "/fonts/**",
        "/*.ico",
        "/*.html",
        "/*.map");
    }

    /**
     * Configure current users datasource
     *
     * @param auth Authentication manager
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {

      AuthType mode = securityConfigProperties.getAuthMode();
      log.info("Using authentication mode: " + mode);

      switch (mode) {
        case CUSTOM:
          // Custom authentication bean
          for (String provider : securityConfigProperties.getAuthCustomProviders()) {
            try {
              Object beanObj = getBean(provider);
              if (beanObj instanceof AuthenticationProvider) {
                auth.authenticationProvider((AuthenticationProvider) beanObj);
              }
            } catch (Exception exc) {
              log.error("Couldn't load authentication provider bean with name [{}]", provider, exc);
            }
          }
          break;

        case LDAP:
        case BBDD:
        default:
          auth.authenticationProvider(getBean(AuthenticationProvider.class));
          break;
      }
    }

    /**
     * Initialize request
     *
     * @param request Request
     */
    private void initRequest(HttpServletRequest request) {
      String body = "{}";
      if (request instanceof AweHttpServletRequestWrapper) {
        body = ((AweHttpServletRequestWrapper) request).getBody();
      }
      try {
        // Read the parameters
        getRequest().setParameterList((ObjectNode) objectMapper.readTree(body));
      } catch (IOException exc) {
        // Do nothing
      }
    }

    /**
     * Required by Spring Boot 2
     *
     * @return Authentication manager
     * @throws Exception Error retrieving authentication manager
     */
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
    }

    /**
     * Username and password authentication filter
     *
     * @return Json Authentication filter
     */
    @Bean
    public JsonAuthenticationFilter authenticationFilter() {
      JsonAuthenticationFilter authenticationFilter = new JsonAuthenticationFilter(elements);
      authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/action/login", "POST"));
      authenticationFilter.setUsernameParameter(baseConfigProperties.getParameter().getUsername());
      authenticationFilter.setPasswordParameter(baseConfigProperties.getParameter().getPassword());
      authenticationFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
        initRequest(request);
        response.getWriter().write(new ObjectMapper().writeValueAsString(actionService.launchAction("afterLogin")));
      });
      authenticationFilter.setAuthenticationFailureHandler((request, response, authenticationException) -> {
        initRequest(request);
        String username = getRequest().getParameterAsString(usernameParameter);
        response.getWriter().write(new ObjectMapper().writeValueAsString(actionService.launchError("afterLogin", getCredentialsException(authenticationException, username))));
      });
      return authenticationFilter;
    }

    /**
     * Retrieve credentials exception
     *
     * @param authenticationException Authentication exception
     * @param username                User name
     * @return Credentials exception
     */
    private AWException getCredentialsException(AuthenticationException authenticationException, String username) {
      AWException exc;
      if (authenticationException instanceof UsernameNotFoundException) {
        exc = new AWException(getLocale("ERROR_TITLE_INVALID_USER"), getLocale("ERROR_MESSAGE_INVALID_USER", username), authenticationException);
      } else if (authenticationException instanceof BadCredentialsException) {
        exc = new AWException(getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), getLocale("ERROR_MESSAGE_INVALID_CREDENTIALS", username), authenticationException);
      } else {
        exc = new AWException(getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), authenticationException.getMessage(), authenticationException);
      }
      exc.setType(AnswerType.WARNING);
      return exc;
    }

    /**
     * Query and Maintain public filter.
     * Filter /action/maintain or /action/data to verify if target is public
     *
     * @return PublicQueryMaintainFilter
     */
    @Bean
    public PublicQueryMaintainFilter publicQueryMaintainFilter() {
      return new PublicQueryMaintainFilter(elements);
    }

    /**
     * Configure Ldap provider for bind auth
     *
     * @param userDAO           User DAO
     * @param ldapContextSource Ldap context
     * @return LDAP authentication provider
     */
    @Bean
    @ConditionalOnProperty(prefix = "awe.security", name = "auth-mode", havingValue = "ldap")
    public AuthenticationProvider ldapAuthenticationProvider(UserDAO userDAO, LdapContextSource ldapContextSource) {

      // Bind authenticator with search filter
      final BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
      bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch("", "(" + securityConfigProperties.getLdap().getUserFilter() + ")", ldapContextSource));

      // Ldap provider
      final LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
      ldapAuthenticationProvider.setHideUserNotFoundExceptions(false);
      ldapAuthenticationProvider.setAuthoritiesMapper(new SimpleAuthorityMapper());
      ldapAuthenticationProvider.setUserDetailsContextMapper(getBean(LdapAweUserDetailsMapper.class));

      return ldapAuthenticationProvider;
    }

    /**
     * Authentication entry point.
     * Handle exceptions for awe actions
     *
     * @param sessionDetails AWE session details
     * @return AuthenticationEntryPoint
     */
    @Bean
    public AuthenticationEntryPoint actionAuthenticationEntryPoint(AweSessionDetails sessionDetails) {
      return new ActionAuthenticationEntryPoint(sessionDetails);
    }

    /**
     * Access denied handler.
     * Handle forbidden access (403)
     *
     * @return Access denied handler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
      return new AweAccessDeniedHandler();
    }

    /**
     * Logout handler
     *
     * @param sessionDetails AWE session details
     * @return AweLogoutHandler
     */
    @Bean
    public AweLogoutHandler logoutHandler(AweSessionDetails sessionDetails) {
      return new AweLogoutHandler(sessionDetails);
    }

    /**
     * Configure ldap user details mapper
     *
     * @param userDetailService user detail service
     * @return Ldap user details context mapper
     */
    @Bean
    public UserDetailsContextMapper ldapAweUserDetailsMapper(UserDetailsService userDetailService) {
      return new LdapAweUserDetailsMapper(userDetailService);
    }

    /**
     * Configure DAO authentication provider
     *
     * @param userDetailsService User detail service
     * @return DaoAuthenticationProvider
     */
    @Bean
    @ConditionalOnProperty(prefix = "awe.security", name = "auth-mode", havingValue = "bbdd", matchIfMissing = true)
    public AuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService) {
      DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
      daoAuthenticationProvider.setPasswordEncoder(new Ripemd160PasswordEncoder());
      daoAuthenticationProvider.setUserDetailsService(userDetailsService);
      daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
      return daoAuthenticationProvider;
    }

    /**
     * Configure User detail service
     *
     * @param userDAO User DAO
     * @return User detail service
     */
    @Bean
    public UserDetailsService aweUserDetailsService(UserDAO userDAO) {
      return new AweUserDetailService(userDAO);
    }

    /**
     * Configure User detail service
     *
     * @param queryUtil Query utilities
     * @param queryService Query service
     * @param dataListService DataList service
     * @return UserDetailService
     */
    @Bean
    public UserDAO userDAO(QueryUtil queryUtil, QueryService queryService, DataListService dataListService) {
      return new UserDAOImpl(queryUtil, queryService, dataListService);
    }

    /**
     * Spring context source for ldap connection
     *
     * @return Ldap context
     */
    @Bean
    @ConditionalOnMissingBean
    public LdapContextSource ldapContextSource() {
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
     * Get access control bean to use in the configuration method
     *
     * @return Login access control
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginAccessControl loginAccessControl() {
      return new LoginAccessControl();
    }

    /////////////////////////////////////////////
    // SERVICES
    /////////////////////////////////////////////

    /**
     * Access service
     *
     * @param menuService              Menu service
     * @param baseConfigProperties     Base properties
     * @param securityConfigProperties Security properties
     * @param encodeService            Encode service
     * @return Access service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessService accessService(MenuService menuService, AweSessionDetails aweSessionDetails, BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, EncodeService encodeService,
                                       TotpConfigProperties totpConfigProperties, TotpService totpService) {
      return new AccessService(baseConfigProperties, securityConfigProperties, encodeService, menuService, aweSessionDetails, totpConfigProperties, totpService);
    }

    /**
     * Totp service
     *
     * @param secretGenerator Secret generator
     * @param qrDataFactory QR data factory
     * @param qrGenerator QR Generator
     * @param codeVerifier TOTP Code verifier
     * @return TOTP Service
     */
    @Bean
    @ConditionalOnMissingBean
    public TotpService totpService(SecretGenerator secretGenerator, QrDataFactory qrDataFactory, QrGenerator qrGenerator, CodeVerifier codeVerifier) {
      return new TotpService(secretGenerator, qrDataFactory, qrGenerator, codeVerifier);
    }
  }
}