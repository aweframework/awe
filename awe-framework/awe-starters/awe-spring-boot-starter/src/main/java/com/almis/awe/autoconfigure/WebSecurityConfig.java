package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweHttpServletRequestWrapper;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.security.authentication.entrypoint.ActionAuthenticationEntryPoint;
import com.almis.awe.security.authentication.filter.JsonAuthenticationFilter;
import com.almis.awe.security.authentication.filter.PublicQueryMaintainFilter;
import com.almis.awe.security.handler.AweAccessDeniedHandler;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.service.ActionService;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Web security configuration class.
 *
 * Used to configure security for web application.
 */
@Configuration
@EnableWebSecurity
@Import({AweAutoConfiguration.class, SessionConfig.class})
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableConfigurationProperties(value = {
  BaseConfigProperties.class,
  SecurityConfigProperties.class})
@Slf4j
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

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

  @Value("${session.cookie.name:JSESSIONID}")
  private String cookieName;

  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final AweSessionDetails sessionDetails;
  private final AweElements elements;
  private final ActionService actionService;
  private final ObjectMapper objectMapper;


  /**
   * Web security config constructor.
   *
   * @param baseConfigProperties     Base config properties
   * @param securityConfigProperties Security config properties
   * @param sessionDetails           Session details
   * @param elements                 Awe elements
   * @param actionService            Action service
   * @param objectMapper             Object mapper
   */
  @Autowired
  public WebSecurityConfig(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, AweSessionDetails sessionDetails, AweElements elements, ActionService actionService, ObjectMapper objectMapper) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.sessionDetails = sessionDetails;
    this.elements = elements;
    this.actionService = actionService;
    this.objectMapper = objectMapper;
  }

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
      .and().exceptionHandling().defaultAuthenticationEntryPointFor(actionAuthenticationEntryPoint(sessionDetails), new AntPathRequestMatcher("/action/**"))
      // Add a filter to parse login parameters
      .and().addFilterAt(jsonAuthenticationFilter(baseConfigProperties, elements, actionService, objectMapper), UsernamePasswordAuthenticationFilter.class)
      // Add logout handler
      .logout().logoutUrl("/action/logout")
      .deleteCookies(cookieName).clearAuthentication(true).invalidateHttpSession(true)
      .addLogoutHandler(logoutHandler(sessionDetails))
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
   * Required by Spring Boot 2
   *
   * @return Authentication manager
   * @throws Exception exception
   */
  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
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
   * Username and password authentication filter
   *
   * @return Json Authentication filter
   */
  @Bean
  public JsonAuthenticationFilter jsonAuthenticationFilter(BaseConfigProperties baseConfigProperties, AweElements elements, ActionService actionService, ObjectMapper objectMapper) {
    JsonAuthenticationFilter authenticationFilter = new JsonAuthenticationFilter(elements);
    authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/action/login", "POST"));
    authenticationFilter.setUsernameParameter(baseConfigProperties.getParameter().getUsername());
    authenticationFilter.setPasswordParameter(baseConfigProperties.getParameter().getPassword());
    authenticationFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
      initRequest(request, objectMapper);
      response.getWriter().write(objectMapper.writeValueAsString(actionService.launchAction("afterLogin")));
    });
    authenticationFilter.setAuthenticationFailureHandler((request, response, authenticationException) -> {
      initRequest(request, objectMapper);
      String username = this.getApplicationContext().getBean(AweRequest.class).getParameterAsString(baseConfigProperties.getParameter().getUsername());
      response.getWriter().write(objectMapper.writeValueAsString(actionService.launchError("afterLogin", getCredentialsException(authenticationException, username))));
    });
    return authenticationFilter;
  }

  /**
   * Initialize request
   *
   * @param request      Request
   * @param objectMapper Object mapper
   */
  private void initRequest(HttpServletRequest request, ObjectMapper objectMapper) {
    try {
      // Get body and read the parameters
      String body = ((AweHttpServletRequestWrapper) request).getBody();
      this.getApplicationContext().getBean(AweRequest.class).setParameterList((ObjectNode) objectMapper.readTree(body));
    } catch (IOException exc) {
      log.error("Error reading request body in initialization process", exc);
    }
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
      exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_USER"), elements.getLocale("ERROR_MESSAGE_INVALID_USER", username), authenticationException);
    } else if (authenticationException instanceof BadCredentialsException) {
      exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), elements.getLocale("ERROR_MESSAGE_INVALID_CREDENTIALS", username), authenticationException);
    } else {
      exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), authenticationException.getMessage(), authenticationException);
    }
    exc.setType(AnswerType.WARNING);
    return exc;
  }

}
