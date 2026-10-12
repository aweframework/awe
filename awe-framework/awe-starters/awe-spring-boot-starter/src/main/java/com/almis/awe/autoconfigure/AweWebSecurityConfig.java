package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.constants.SecurityEndpoints;
import com.almis.awe.component.AweHttpServletRequestWrapper;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.security.authentication.ClaimRolesAuthoritiesConverter;
import com.almis.awe.security.authentication.entrypoint.ActionAuthenticationEntryPoint;
import com.almis.awe.security.authentication.filter.JsonAuthenticationFilter;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.handler.AweAccessDeniedHandler;
import com.almis.awe.security.handler.AweLogoutHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationFailureHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.security.multitenant.MultiTenantOAuth2AuthenticationEntryPoint;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ActionService;
import com.almis.awe.service.ErrorPageService;
import com.almis.awe.service.user.PasswordChangeService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.oidc.session.InMemoryOidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Unified web security configuration class.
 * Handles both traditional authentication and SSO OAuth2 authentication.
 */
@Configuration
@EnableWebSecurity
@Import({AweAutoConfiguration.class, SessionConfig.class, LocalLoginConfig.class})
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(value = {
		BaseConfigProperties.class,
		SecurityConfigProperties.class}
)
@Slf4j
public class AweWebSecurityConfig {

	// Autowired services
	private final ApplicationContext context;
	private final BaseConfigProperties baseConfigProperties;
	private final SecurityConfigProperties securityConfigProperties;
	private final AweSessionDetails sessionDetails;
	private final AweElements elements;
	private final ActionService actionService;
	private final ObjectMapper objectMapper;
	private final AccessService accessService;
	private final ErrorPageService errorPageService;
	private final MultiTenantOAuth2Config multiTenantConfig;
	private final MultiTenantFilter multiTenantFilter;

	// Constants
	public static final SecurityEndpoints SECURITY_ENDPOINTS = new SecurityEndpoints();

	@Value("${session.cookie.name:JSESSIONID}")
	private String cookieName;

	/**
	 * Unified security config constructor.
	 */
	@Autowired
	public AweWebSecurityConfig(
			ApplicationContext context,
			BaseConfigProperties baseConfigProperties,
			SecurityConfigProperties securityConfigProperties,
			AweSessionDetails sessionDetails,
			AweElements elements,
			ActionService actionService,
			ObjectMapper objectMapper,
			AccessService accessService,
			ErrorPageService errorPageService,
			MultiTenantOAuth2Config multiTenantConfig,
			MultiTenantFilter multiTenantFilter) {
		this.context = context;
		this.baseConfigProperties = baseConfigProperties;
		this.securityConfigProperties = securityConfigProperties;
		this.sessionDetails = sessionDetails;
		this.elements = elements;
		this.actionService = actionService;
		this.objectMapper = objectMapper;
		this.accessService = accessService;
		this.errorPageService = errorPageService;
		this.multiTenantConfig = multiTenantConfig;
		this.multiTenantFilter = multiTenantFilter;
	}

	/**
	 * The main security filter chain-handles both SSO and traditional authentication
	 *
	 * @param httpSecurity Http security
	 * @return security filter chain
	 * @throws Exception Spring http security error
	 */
	@Bean(name = "aweSecurityFilterChain")
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity, @Qualifier("localLoginAuthenticationManager") AuthenticationManager localLoginAuthenticationManager) throws Exception {
		boolean ssoEnabled = securityConfigProperties.getSso() != null && securityConfigProperties.getSso().isEnabled();
		boolean autoLaunch = ssoEnabled && securityConfigProperties.getSso().isAutoLaunch();

		// Configure XSS protection
		httpSecurity.headers(headers ->
				headers.xssProtection(
						xssConfig -> xssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED)
				));

		// Add multi-tenant filter if enabled
		if (ssoEnabled && multiTenantConfig != null && multiTenantConfig.isEnabled() && multiTenantFilter != null) {
			log.info("Multi-tenant support is enabled. Adding multi-tenant filter");
			httpSecurity.addFilterBefore(multiTenantFilter, BasicAuthenticationFilter.class);
		}

		// Local login policy wiring. The decorated manager is used by exactly three entry points and never by the shared
		// authentication manager bean, so internal flows such as re-authentication are not affected. The entry points are
		// the JSON login filter of the login action, the standard form login and the REST authentication endpoint of the
		// REST module. For the standard form login the decorated manager is the parent of the HttpSecurity builder manager,
		// which asks its own providers first and the parent only afterwards: no provider registered in that builder may
		// support username and password tokens, or it would answer first and skip the policy. The OAuth2 providers do not
		// support them, and a test guards this.
		httpSecurity.getSharedObject(AuthenticationManagerBuilder.class).parentAuthenticationManager(localLoginAuthenticationManager);

		// Configure authorization rules
		configureAuthorization(httpSecurity, ssoEnabled, autoLaunch);

		// Configure an authentication mechanism
		configureTraditionalAuthentication(httpSecurity, localLoginAuthenticationManager);

		// Configure form login only if NOT using auto-launch SSO
		if (!autoLaunch) {
			httpSecurity.formLogin(formLoginConfigurer -> formLoginConfigurer.loginPage("/").permitAll());
		}

		// Add SSO authentication
		if (ssoEnabled) {
			configureSSOAuthentication(httpSecurity, autoLaunch);
		}

		// Configure logout
		configureLogout(httpSecurity);

		// Configure CSRF
		configureCsrf(httpSecurity, ssoEnabled);

		// Configure headers
		if (securityConfigProperties.isSameOriginEnable()) {
			httpSecurity.headers(headers ->
					headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		}

		// Configure exception handling
		configureExceptionHandling(httpSecurity, ssoEnabled, autoLaunch);

		return httpSecurity.build();
	}

	/**
	 * Configure authorization rules
	 */
	private void configureAuthorization(HttpSecurity http, boolean ssoEnabled, boolean autoLaunch) throws Exception {

		http.authorizeHttpRequests(requests -> {
				// Skip the native app login screen for SSO only if auto-launch is disabled
				if (ssoEnabled && !autoLaunch) {
					requests.requestMatchers("/").permitAll();
				}
				requests
					// Web resources
					.requestMatchers(SECURITY_ENDPOINTS.getWebResourcesRequestMatchers()).permitAll()
					// Public actions
					.requestMatchers(SECURITY_ENDPOINTS.getPublicActionsRequestMatchers()).permitAll()
					// File and upload controllers
					.requestMatchers(SECURITY_ENDPOINTS.getFileRequestMatchers()).permitAll()
					// Public queries and maintains
					.requestMatchers(SECURITY_ENDPOINTS.getAuthenticatedRequestMatchers()).access(publicQueryMaintainAuthorization(elements))
					// Any other request
					.anyRequest().authenticated();
		});
	}

	/**
	 * Configure SSO OAuth2 authentication
	 */
	private void configureSSOAuthentication(HttpSecurity http, boolean autoLaunch) throws Exception {
		http.oauth2Login(oauth2 -> {
			if (!autoLaunch) {
				oauth2.loginPage("/");
			}
			// Configure sso handlers
			// (resolved from the context so handler beans provided by the application are honoured)
			oauth2.successHandler(context.getBean(AweOauth2AuthenticationSuccessHandler.class));
			oauth2.failureHandler(context.getBean(AweOauth2AuthenticationFailureHandler.class));
		});

		// OIDC back-channel logout: the provider posts a logout token and the matching sessions are ended. Each session is
		// ended by an internal request that carries its session cookie, so it runs the same logout handlers as a user logout
		// (AWE session state, websocket connections, security context and HTTP session)
		if (isBackChannelLogoutEnabled()) {
			log.info("OIDC back-channel logout is enabled");
			AweOidcBackChannelLogoutHandler logoutHandler = context.getBean(AweOidcBackChannelLogoutHandler.class);
			http.oidcLogout(logout -> logout.backChannel(backChannel -> backChannel.logoutHandler(logoutHandler)));
		}
	}

	private boolean isBackChannelLogoutEnabled() {
		return securityConfigProperties.getSso().getBackChannelLogout().isEnabled();
	}

	/**
	 * Configure traditional form-based authentication
	 */
	private void configureTraditionalAuthentication(HttpSecurity http, AuthenticationManager localLoginAuthenticationManager) throws Exception {
		// Add a filter to parse login parameters
		JsonAuthenticationFilter jsonFilter = jsonAuthenticationFilter(baseConfigProperties, elements, actionService, objectMapper);
		jsonFilter.setAuthenticationManager(localLoginAuthenticationManager);
		http.addFilterAt(jsonFilter, UsernamePasswordAuthenticationFilter.class);

		// Security context repository (to adapt for spring security 6)
		http.securityContext(httpSecuritySecurityContextConfigurer ->
				httpSecuritySecurityContextConfigurer.securityContextRepository(securityContextRepository()));
	}

	/**
	 * Configure logout
	 */
	private void configureLogout(HttpSecurity http) throws Exception {
		boolean ssoEnabled = securityConfigProperties.getSso() != null && securityConfigProperties.getSso().isEnabled();
		http.logout(logoutConfig -> {
			logoutConfig.logoutUrl("/action/logout")
				.deleteCookies(cookieName)
				.clearAuthentication(true)
				.invalidateHttpSession(true)
				.addLogoutHandler(logoutHandler(sessionDetails));
			if (ssoEnabled) {
				logoutConfig.logoutSuccessHandler(oidcLogoutSuccessHandler());
			}
		});
	}

	private LogoutSuccessHandler oidcLogoutSuccessHandler() {
		OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(context.getBean(ClientRegistrationRepository.class));
		// Sets the location that the End-User's User Agent will be redirected to after the logout has been performed at the Provider
		String postLogoutRedirectUri = "{baseUrl}";
		if (securityConfigProperties.getSso().isAutoLaunch()) {
			postLogoutRedirectUri += "/screen/public/logout-sso";
		}
		oidcLogoutSuccessHandler.setPostLogoutRedirectUri(postLogoutRedirectUri);
		return oidcLogoutSuccessHandler;
	}

	/**
	 * Configure CSRF protection
	 */
	private void configureCsrf(HttpSecurity http, boolean ssoEnabled) throws Exception {
		http.csrf(csrf -> csrf
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
				// Ignore logout action to allow CSRF protection in SPA applications
				.ignoringRequestMatchers("/action/logout")
		);

		if (ssoEnabled) {
			http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
		} else {
			http.addFilterAfter(new CsrfCookieFilter(), JsonAuthenticationFilter.class);
		}
	}
	/**
	 * Configure exception handling
	 */
	private void configureExceptionHandling(HttpSecurity http, boolean ssoEnabled, boolean autoLaunch) throws Exception {
		if (ssoEnabled && multiTenantConfig != null && multiTenantConfig.isEnabled() && autoLaunch) {
			log.debug("Configuring OAuth2 entrypoint with multi-tenant for auto-launch");
			http.exceptionHandling(exceptions ->
					exceptions.authenticationEntryPoint(
							multiTenantOAuth2AuthenticationEntryPoint()
					)
			);
		} else if (!ssoEnabled) {
			http.exceptionHandling(exceptionHandlingConfigurer ->
					exceptionHandlingConfigurer
							.accessDeniedHandler(accessDeniedHandler())
							.defaultAuthenticationEntryPointFor(
									actionAuthenticationEntryPoint(sessionDetails),
									PathPatternRequestMatcher.withDefaults()
											.matcher("/action/**")
							)
			);
		}
	}

	// ============== BEANS ==============

	@Bean
	@ConditionalOnMissingBean
	public PublicQueryMaintainAuthorization publicQueryMaintainAuthorization(AweElements elements) {
		return new PublicQueryMaintainAuthorization(elements);
	}

	@Bean
	@Primary
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return new AweAccessDeniedHandler();
	}

	@Bean
	public AuthenticationEntryPoint actionAuthenticationEntryPoint(AweSessionDetails sessionDetails) {
		return new ActionAuthenticationEntryPoint(sessionDetails);
	}

	@Bean
	public AweLogoutHandler logoutHandler(AweSessionDetails sessionDetails) {
		return new AweLogoutHandler(sessionDetails);
	}

	@Bean
	public HttpSessionSecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	// ============== SSO BEANS ==============

	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean
	public MultiTenantOAuth2AuthenticationEntryPoint multiTenantOAuth2AuthenticationEntryPoint() {
		return new MultiTenantOAuth2AuthenticationEntryPoint(
				securityConfigProperties,
				multiTenantConfig,
				context.getBean(ClientRegistrationRepository.class)
		);
	}

	/**
	 * Registry of the OIDC sessions that the provider can end through back-channel logout. It is kept in memory, so it
	 * only knows the sessions created in this node: applications running several nodes provide a shared registry bean.
	 *
	 * @return OIDC session registry
	 */
	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = {"enabled", "back-channel-logout.enabled"}, havingValue = "true")
	@ConditionalOnMissingBean
	public OidcSessionRegistry oidcSessionRegistry() {
		return new InMemoryOidcSessionRegistry();
	}

	/**
	 * Back-channel logout handler: ends each session named by the logout token through an internal logout request that
	 * carries the AWE session cookie
	 *
	 * @param oidcSessionRegistry OIDC session registry
	 * @return back-channel logout handler
	 */
	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = {"enabled", "back-channel-logout.enabled"}, havingValue = "true")
	@ConditionalOnMissingBean
	public AweOidcBackChannelLogoutHandler oidcBackChannelLogoutHandler(OidcSessionRegistry oidcSessionRegistry) {
		SecurityConfigProperties.BackChannelLogout backChannelLogout = securityConfigProperties.getSso().getBackChannelLogout();
		String logoutUri = StringUtils.hasText(backChannelLogout.getLogoutUri())
			? backChannelLogout.getLogoutUri() : AweOidcBackChannelLogoutHandler.DEFAULT_LOGOUT_URI;
		return new AweOidcBackChannelLogoutHandler(oidcSessionRegistry,
			AweOidcBackChannelLogoutHandler.restOperations(backChannelLogout.getConnectTimeout(), backChannelLogout.getReadTimeout()),
			cookieName, logoutUri);
	}

	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(name = "realmRolesAuthoritiesConverter")
	AuthoritiesConverter realmRolesAuthoritiesConverter() {
		// Roles are read from the configured claims (awe.security.sso.roles-claims, Keycloak realm roles by default)
		return new ClaimRolesAuthoritiesConverter(securityConfigProperties.getSso().getRolesClaims())::convert;
	}

	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
	GrantedAuthoritiesMapper authenticationConverter(Converter<Map<String, Object>, Collection<GrantedAuthority>> authoritiesConverter) {
		return authorities -> authorities.stream()
				.filter(OidcUserAuthority.class::isInstance)
				.map(OidcUserAuthority.class::cast)
				.map(OidcUserAuthority::getIdToken)
				.map(OidcIdToken::getClaims)
				.map(authoritiesConverter::convert).filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean
	public AweOauth2AuthenticationSuccessHandler authSuccessHandler(AweOauth2AuthenticationFailureHandler failureHandler) {
		return new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler);
	}

	@Bean
	@ConditionalOnProperty(prefix = "awe.security.sso", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean
	public AweOauth2AuthenticationFailureHandler authFailureHandler() {
		return new AweOauth2AuthenticationFailureHandler(errorPageService);
	}

	// ============== TRADITIONAL AUTH BEANS ==============

	@Bean
	public JsonAuthenticationFilter jsonAuthenticationFilter(BaseConfigProperties baseConfigProperties, AweElements elements, ActionService actionService, ObjectMapper objectMapper) {
		JsonAuthenticationFilter authenticationFilter = new JsonAuthenticationFilter(elements, objectMapper);
		authenticationFilter.setRequiresAuthenticationRequestMatcher(PathPatternRequestMatcher.withDefaults()
				.matcher(HttpMethod.POST,"/action/login"));
		authenticationFilter.setUsernameParameter(baseConfigProperties.getParameter().getUsername());
		authenticationFilter.setPasswordParameter(baseConfigProperties.getParameter().getPassword());
		authenticationFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
			// A pending expired password change never survives into a login
			clearPasswordChange(request);
			initRequest(request, objectMapper);
			response.getWriter().write(objectMapper.writeValueAsString(actionService.launchAction("afterLogin")));
		});
		authenticationFilter.setAuthenticationFailureHandler((request, response, authenticationException) -> {
			initRequest(request, objectMapper);
			String username = context.getBean(AweRequest.class).getParameterAsString(baseConfigProperties.getParameter().getUsername());
			response.setHeader("Content-Type", "application/json; charset=UTF-8");
			List<ClientAction> actions = actionService.launchError("afterLogin", getCredentialsException(authenticationException, username));
			// Any failed attempt discards the pending change; an expired password stores a new one
			clearPasswordChange(request);
			if (authenticationException instanceof CredentialsExpiredException) {
				actions = addPasswordChangeStep(actions, request, username);
			}
			response.getWriter().write(objectMapper.writeValueAsString(actions));
		});
		authenticationFilter.setSecurityContextRepository(securityContextRepository());

		return authenticationFilter;
	}

	// ============== HELPER METHODS ==============

	private void initRequest(HttpServletRequest request, ObjectMapper objectMapper) {
		try {
			String body = ((AweHttpServletRequestWrapper) request).getBody();
			context.getBean(AweRequest.class).setParameterList((ObjectNode) objectMapper.readTree(body));
		} catch (IOException exc) {
			log.error("Error reading request body in initialization process", exc);
		}
	}

	/**
	 * The password has been verified and is expired: the user can set a new one, but gets no authenticated session.
	 * The change is allowed through a short-lived request of the HTTP session bound to the user
	 */
	private List<ClientAction> addPasswordChangeStep(List<ClientAction> errorActions, HttpServletRequest request, String username) {
		PasswordChangeService passwordChangeService = context.getBeanProvider(PasswordChangeService.class).getIfAvailable();
		if (passwordChangeService == null || !passwordChangeService.allowPasswordChange(request, username)) {
			return errorActions;
		}
		// The screen action replaces the cancel of the error answer, which would discard it
		List<ClientAction> actions = new ArrayList<>(errorActions);
		actions.removeIf(action -> "cancel".equals(action.getType()));
		actions.add(passwordChangeService.getPasswordChangeScreenAction());
		return actions;
	}

	private void clearPasswordChange(HttpServletRequest request) {
		PasswordChangeService passwordChangeService = context.getBeanProvider(PasswordChangeService.class).getIfAvailable();
		if (passwordChangeService != null) {
			passwordChangeService.clearPasswordChange(request.getSession(false));
		}
	}

	private AWException getCredentialsException(AuthenticationException authenticationException, String username) {
		AWException exc;
		if (authenticationException instanceof UsernameNotFoundException) {
			exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_USER"), elements.getLocale("ERROR_MESSAGE_INVALID_USER", username), authenticationException);
		} else if (authenticationException instanceof CredentialsExpiredException) {
			exc = new AWException(elements.getLocale("ERROR_TITLE_EXPIRED_PASSWORD"), elements.getLocale("ERROR_MESSAGE_EXPIRED_PASSWORD"), authenticationException);
		} else if (authenticationException instanceof BadCredentialsException) {
			exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), elements.getLocale("ERROR_MESSAGE_INVALID_CREDENTIALS", username), authenticationException);
		} else {
			exc = new AWException(elements.getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), authenticationException.getMessage(), authenticationException);
		}
		exc.setType(AnswerType.WARNING);
		return exc;
	}

	interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
	}
}

final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
	private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
		this.delegate.handle(request, response, csrfToken);
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
			return super.resolveCsrfTokenValue(request, csrfToken);
		}
		return this.delegate.resolveCsrfTokenValue(request, csrfToken);
	}
}

final class CsrfCookieFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
		csrfToken.getToken();
		filterChain.doFilter(request, response);
	}
}
