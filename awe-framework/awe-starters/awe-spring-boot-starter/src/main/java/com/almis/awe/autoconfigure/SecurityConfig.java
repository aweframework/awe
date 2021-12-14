package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweHttpServletRequestWrapper;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.security.accessbean.LoginAccessControl;
import com.almis.awe.security.authentication.encoder.Ripemd160PasswordEncoder;
import com.almis.awe.security.authentication.entrypoint.ActionAuthenticationEntryPoint;
import com.almis.awe.security.authentication.filter.JsonAuthenticationFilter;
import com.almis.awe.security.authentication.filter.PublicQueryMaintainFilter;
import com.almis.awe.security.handler.AweAccessDeniedHandler;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.service.user.LdapAweUserDetailsMapper;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
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
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig extends ServiceConfig {

  // White list urls
  private static final String[] AUTH_LIST = {
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
    "/screen/public/**",
    // File and upload controllers
    "/file/text",
    "/file/stream",
    "/file/download",
    "/file/upload",
    "/file/delete",
    // React engine
    "/screen-data",
    "/locales/**"
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
  private final AweSessionDetails aweSessionDetails;
  private final LogUtil logger;
  private final AweElements elements;
  private final ObjectMapper objectMapper;
  @Value("${screen.parameter.username:cod_usr}")
  private String usernameParameter;
  @Value("${screen.parameter.password:pwd_usr}")
  private String passwordParameter;
  @Value("${language.default}:en")
  private String defaultLocale;
  @Value("${security.auth.mode:bbdd}")
  private String authenticationProviderSource;
  @Value("${security.role.prefix:ROLE_}")
  private String rolePrefix;
  // Custom authentication
  @Value("#{'${security.auth.custom.providers:}'.split(',')}")
  private List<String> authenticationProviders;
  // LDAP authentication
  @Value("#{'${security.auth.ldap.url:}'.split(',')}")
  private List<String> ldapUrl;
  @Value("${security.auth.ldap.user:}")
  private String ldapUserFilter;
  @Value("${security.auth.ldap.password.bind:}")
  private String ldapPassword;
  @Value("${security.auth.ldap.user.bind:}")
  private String ldapUserDN;
  @Value("${security.auth.ldap.basedn:}")
  private String ldapBaseDN;
  @Value("${security.auth.ldap.timeout:}")
  private String ldapConnectTimeout;
  @Value("${security.headers.frameOptions.sameOrigin:true}")
  private boolean sameOrigin;
  @Value("${session.cookie.name:JSESSIONID}")
  private String cookieName;

  /**
   * Autowired constructor
   *
   * @param sessionDetails AWE session details
   * @param logger         Log utility
   * @param elements       Awe elements
   * @param objectMapper   Object mapper
   */
  @Autowired
  public SecurityConfig(AweSessionDetails sessionDetails, LogUtil logger, AweElements elements, ObjectMapper objectMapper) {
    this.aweSessionDetails = sessionDetails;
    this.logger = logger;
    this.elements = elements;
    this.objectMapper = objectMapper;
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
        .and().formLogin().loginPage("/").permitAll()
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

      if (sameOrigin) {
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

      AUTHENTICATION_MODE mode = AUTHENTICATION_MODE.fromValue(authenticationProviderSource);
      mode = mode == null ? AUTHENTICATION_MODE.BBDD : mode;
      logger.log(getClass(), Level.INFO, "Using authentication mode: " + mode);

      switch (mode) {
        case CUSTOM:
          // Custom authentication bean
          for (String provider : authenticationProviders) {
            try {
              Object beanObj = getBean(provider);
              if (beanObj instanceof AuthenticationProvider) {
                auth.authenticationProvider((AuthenticationProvider) beanObj);
              }
            } catch (Exception exc) {
              logger.log(this.getClass(), Level.ERROR, "Couldn't load authentication provider bean with name [{0}]", exc, provider);
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
      authenticationFilter.setUsernameParameter(usernameParameter);
      authenticationFilter.setPasswordParameter(passwordParameter);
      authenticationFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
        // Initialize parameters
        initRequest(request);
        aweSessionDetails.onLoginSuccess();
        request.getRequestDispatcher("/action/loginRedirect").forward(request, response);
      });
      authenticationFilter.setAuthenticationFailureHandler((request, response, authenticationException) -> {
        // Initialize parameters
        initRequest(request);
        aweSessionDetails.onLoginFailure(authenticationException);
        request.getRequestDispatcher("/action/loginRedirect").forward(request, response);
      });
      return authenticationFilter;
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
     * @return LdapAuthenticationProvider
     */
    @Bean
    @ConditionalOnProperty(name = "security.auth.mode", havingValue = "ldap")
    public AuthenticationProvider ldapAuthenticationProvider(UserDAO userDAO) {

      // Bind authenticator with search filter
      final BindAuthenticator bindAuthenticator = new BindAuthenticator(getBean(LdapContextSource.class));
      bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch("", "(" + ldapUserFilter + ")", getBean(LdapContextSource.class)));


      // Ldap provider
      final LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
      ldapAuthenticationProvider.setHideUserNotFoundExceptions(false);
      ldapAuthenticationProvider.setAuthoritiesMapper(new SimpleAuthorityMapper());
      ldapAuthenticationProvider.setUserDetailsContextMapper(ldapAweUserDetailsMapper(userDAO));

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
     * @param userDAO user dao
     * @return Ldap user details context mapper
     */
    @Bean
    public UserDetailsContextMapper ldapAweUserDetailsMapper(UserDAO userDAO) {
      return new LdapAweUserDetailsMapper(userDAO);
    }

    /**
     * Configure DAO authentication provider
     *
     * @param userDetailsService User detail service
     * @return DaoAuthenticationProvider
     */
    @Bean
    @ConditionalOnProperty(name = "security.auth.mode", havingValue = "bbdd")
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
     * @param queryService Query service
     * @return UserDetailService
     */
    @Bean
    public UserDAO userDAO(QueryService queryService) {
      return new UserDAOImpl(queryService);
    }

    /**
     * Spring context source for ldap connection
     *
     * @return Ldap context
     */
    @Bean
    @ConditionalOnMissingBean
    public LdapContextSource contextSource() {
      // Environment properties
      Map<String, Object> environmentProperties = Collections.synchronizedMap(new HashMap<>());
      environmentProperties.put("com.sun.jndi.ldap.connect.timeout", ldapConnectTimeout);

      LdapContextSource ldapContextSource = new LdapContextSource();
      ldapContextSource.setBaseEnvironmentProperties(environmentProperties);
      ldapContextSource.setUrls(ldapUrl.toArray(new String[0]));
      ldapContextSource.setBase(ldapBaseDN);
      ldapContextSource.setUserDn(ldapUserDN);
      ldapContextSource.setPassword(ldapPassword);
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
     * @param menuService Menu service
     * @return Access service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessService accessService(MenuService menuService) {
      return new AccessService(menuService);
    }
  }
}