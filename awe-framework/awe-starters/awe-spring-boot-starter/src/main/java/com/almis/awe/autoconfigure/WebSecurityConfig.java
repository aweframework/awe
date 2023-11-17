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
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.handler.AweAccessDeniedHandler;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.service.ActionService;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Web security configuration class.
 * Used to configure security for web application.
 */
@Configuration
@EnableWebSecurity
@Import({AweAutoConfiguration.class, SessionConfig.class})
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(value = {
  BaseConfigProperties.class,
  SecurityConfigProperties.class})
@Slf4j
public class WebSecurityConfig {

  // White list urls
  private static final String[] ALLOW_LIST = {
    // Web resources
    "/css/**",
    "/js/**",
    "/fonts/**",
    "/images/**",
    "/error**",
    "/websocket/**",
    "/template/**",
    "/settings",
    "/locals-*/**",
    // public actions
    "/action/login",
    "/action/get-locals",
    "/action/screen-data",
    "/action/encrypt",
    "/action/get-file",
    "/action/file-info",
    "/action/delete-file",
    "/screen/public/**",
    "/screen-data/**",
    // File and upload controllers
    "/file/text",
    "/file/stream",
    "/file/download",
    "/file/upload",
    "/file/delete"
  };

  // query and maintain action  required
  private static final String[] PUBLIC_QUERY_MAINTAIN_LIST = {
          "/action/data*/**",
          "/action/update*/**",
          "/action/control*/**",
          "/action/unique*/**",
          "/action/value*/**",
          "/action/validate*/**",
          "/action/subscribe*/**",
          "/action/tree-branch*/**",
          "/action/maintain*/**",
          "/action/get-file-maintain/**",
          "/file/stream/maintain/**",
          "/file/download/maintain/**"
  };

  @Value("${session.cookie.name:JSESSIONID}")
  private String cookieName;

  private final ApplicationContext context;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final AweSessionDetails sessionDetails;
  private final AweElements elements;
  private final ActionService actionService;
  private final ObjectMapper objectMapper;


  /**
   * Web security config constructor.
   *
   * @param context                          Application context
   * @param baseConfigProperties             Base config properties
   * @param securityConfigProperties         Security config properties
   * @param sessionDetails                   Session details
   * @param elements                         Awe elements
   * @param actionService                    Action service
   * @param objectMapper                     Object mapper
   */
  @Autowired
  public WebSecurityConfig(ApplicationContext context, BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, AweSessionDetails sessionDetails, AweElements elements, ActionService actionService, ObjectMapper objectMapper) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.sessionDetails = sessionDetails;
    this.elements = elements;
    this.actionService = actionService;
    this.objectMapper = objectMapper;
    this.context = context;
  }

  /**
   * Awe Rest http security filter chain
   *
   * @param httpSecurity Http security
   * @return security filter chain
   * @throws Exception Spring http security error
   */
  @Bean(name = "aweSecurityFilterChain")
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
            .headers().xssProtection().headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED).and()
            .and().authorizeHttpRequests(requests -> requests
                    // Web
                    .requestMatchers(ALLOW_LIST).permitAll()
                    // Public queries and maintains
                    .requestMatchers(PUBLIC_QUERY_MAINTAIN_LIST).access(publicQueryMaintainAuthorization(elements))
                    // 2FA endpoint
                    .requestMatchers("/access/**").authenticated()
                    // Any other request
                    .anyRequest().authenticated()
            )
            // Add a filter to parse login parameters
            .addFilterAt(jsonAuthenticationFilter(baseConfigProperties, elements, actionService, objectMapper), UsernamePasswordAuthenticationFilter.class)
            // Add logout handler
            .logout().logoutUrl("/action/logout")
            .deleteCookies(cookieName).clearAuthentication(true).invalidateHttpSession(true)
            .addLogoutHandler(logoutHandler(sessionDetails))
            // Security context repository (to adapt for spring security 6)
            .and().securityContext().securityContextRepository(securityContextRepository())
            // Login redirect
            .and().formLogin().loginPage("/").permitAll()
            // Exceptions handling
            .and().exceptionHandling().accessDeniedHandler(accessDeniedHandler())
            .and().exceptionHandling().defaultAuthenticationEntryPointFor(actionAuthenticationEntryPoint(sessionDetails), new AntPathRequestMatcher("/action/**"))
            // Csrf SPA customize
            .and().csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
			)
			.addFilterAfter(new CsrfCookieFilter(), JsonAuthenticationFilter.class);

    if (securityConfigProperties.isSameOriginEnable()) {
      httpSecurity.headers().frameOptions().sameOrigin();
    }

    return httpSecurity.build();
  }

  /**
   * Query and Maintain public filter.
   * Filter /action/maintain or /action/data to verify if target is public
   *
   * @return PublicQueryMaintainFilter
   */
  @Bean
  @ConditionalOnMissingBean
  public PublicQueryMaintainAuthorization publicQueryMaintainAuthorization(AweElements elements) {
    return new PublicQueryMaintainAuthorization(elements);
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
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

  @Bean
  public HttpSessionSecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
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
      String username = context.getBean(AweRequest.class).getParameterAsString(baseConfigProperties.getParameter().getUsername());
      response.getWriter().write(objectMapper.writeValueAsString(actionService.launchError("afterLogin", getCredentialsException(authenticationException, username))));
    });
    authenticationFilter.setSecurityContextRepository(securityContextRepository());

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
      context.getBean(AweRequest.class).setParameterList((ObjectNode) objectMapper.readTree(body));
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

final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
  private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
    /*
     * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
     * the CsrfToken when it is rendered in the response body.
     */
    this.delegate.handle(request, response, csrfToken);
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    /*
     * If the request contains a request header, use CsrfTokenRequestAttributeHandler
     * to resolve the CsrfToken. This applies when a single-page application includes
     * the header value automatically, which was obtained via a cookie containing the
     * raw CsrfToken.
     */
    if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
      return super.resolveCsrfTokenValue(request, csrfToken);
    }
    /*
     * In all other cases (e.g. if the request contains a request parameter), use
     * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
     * when a server-side rendered form includes the _csrf request parameter as a
     * hidden input.
     */
    return this.delegate.resolveCsrfTokenValue(request, csrfToken);
  }
}

final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
    // Render the token value to a cookie by causing the deferred token to be loaded
    csrfToken.getToken();

    filterChain.doFilter(request, response);
  }
}
