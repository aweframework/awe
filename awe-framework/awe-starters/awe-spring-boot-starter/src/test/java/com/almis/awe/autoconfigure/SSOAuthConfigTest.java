package com.almis.awe.autoconfigure;

import com.almis.ade.autoconfigure.AdeAutoConfiguration;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.multitenant.MultiTenantClientRegistrationRepository;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ErrorPageService;
import com.almis.awe.session.AweSessionDetails;
import com.azure.spring.cloud.autoconfigure.implementation.aad.configuration.AadAutoConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SSOAuthConfigTest {

	@Mock
	private SecurityConfigProperties securityConfigProperties;

	@Mock
	private MultiTenantOAuth2Config multiTenantConfig;

	@Mock
	private AccessService accessService;

	@Mock
	private AweSessionDetails sessionDetails;

	@Mock
	private ErrorPageService errorPageService;

	@Mock
	private PublicQueryMaintainAuthorization publicQueryMaintainAuthorization;

	private SSOAuthConfig ssoAuthConfig;

	@BeforeEach
	void setUp() {
		ssoAuthConfig = new SSOAuthConfig(
				accessService,
				sessionDetails,
				securityConfigProperties,
				publicQueryMaintainAuthorization,
				errorPageService,
				multiTenantConfig
		);
	}

	// Define web application context runner
	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					SecurityAutoConfiguration.class,
					SSOAuthConfig.class,
					ClientRegistration.class,
					OAuth2ClientAutoConfiguration.class,
					WebMvcAutoConfiguration.class,
					SecurityConfig.class,
					WebSecurityConfig.class,
					AuthenticationConfig.class,
					SerializerConfig.class,
					SecurityConfig.class,
					AuthenticationConfiguration.class,
					SessionConfig.class,
					WebsocketConfig.class,
					TemplateConfig.class,
					AdeAutoConfiguration.class,
					RestConfig.class,
					AadAutoConfiguration.class,
					RestTemplateAutoConfiguration.class,
					TaskConfig.class))
			.withUserConfiguration(
					BaseConfigProperties.class,
					SecurityConfigProperties.class
			)
			.withPropertyValues(
					"awe.application.module-list=awe"
			)
			.withBean(NoOpCacheManager.class)
			.withBean(SpringTemplateEngine.class);

	@Test
	void testClientRegistrationRepositoryBeanCreation() {
		// When
		ClientRegistrationRepository repository = ssoAuthConfig.clientRegistrationRepository(multiTenantConfig);

		// Then
		assertNotNull(repository);
		assertInstanceOf(MultiTenantClientRegistrationRepository.class, repository);
	}

	@Test
	void testClientRegistrationRepositoryWithNullConfig() {
		// When
		ClientRegistrationRepository repository = ssoAuthConfig.clientRegistrationRepository(null);

		// Then
		assertNotNull(repository);
		assertInstanceOf(MultiTenantClientRegistrationRepository.class, repository);
	}

	@Test
	void testRealmRolesAuthoritiesConverter() {
		// When
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		// Then
		assertNotNull(converter);
	}

	@Test
	void testRealmRolesAuthoritiesConverterWithValidClaims() {
		// Given
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		java.util.Map<String, Object> claims = new java.util.HashMap<>();
		java.util.Map<String, Object> realmAccess = new java.util.HashMap<>();
		java.util.List<String> roles = java.util.List.of("admin", "user", "manager");
		realmAccess.put("roles", roles);
		claims.put("realm_access", realmAccess);

		// When
		var authorities = converter.convert(claims);

		// Then
		assertNotNull(authorities);
		assertEquals(3, authorities.size());

		var authorityNames = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(java.util.stream.Collectors.toSet());

		assertTrue(authorityNames.contains("admin"));
		assertTrue(authorityNames.contains("user"));
		assertTrue(authorityNames.contains("manager"));
	}

	@Test
	void testRealmRolesAuthoritiesConverterWithEmptyRoles() {
		// Given
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		java.util.Map<String, Object> claims = new java.util.HashMap<>();
		java.util.Map<String, Object> realmAccess = new java.util.HashMap<>();
		java.util.List<String> roles = java.util.List.of();
		realmAccess.put("roles", roles);
		claims.put("realm_access", realmAccess);

		// When
		var authorities = converter.convert(claims);

		// Then
		assertNotNull(authorities);
		assertTrue(authorities.isEmpty());
	}

	@Test
	void testRealmRolesAuthoritiesConverterWithNullRealmAccess() {
		// Given
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		java.util.Map<String, Object> claims = new java.util.HashMap<>();
		claims.put("realm_access", null);

		// When
		var authorities = converter.convert(claims);

		// Then
		assertNotNull(authorities);
		assertTrue(authorities.isEmpty());
	}

	@Test
	void testRealmRolesAuthoritiesConverterWithMissingRealmAccess() {
		// Given
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		java.util.Map<String, Object> claims = new java.util.HashMap<>();

		// When
		var authorities = converter.convert(claims);

		// Then
		assertNotNull(authorities);
		assertTrue(authorities.isEmpty());
	}

	@Test
	void testRealmRolesAuthoritiesConverterWithNullRoles() {
		// Given
		SSOAuthConfig.AuthoritiesConverter converter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		java.util.Map<String, Object> claims = new java.util.HashMap<>();
		java.util.Map<String, Object> realmAccess = new java.util.HashMap<>();
		realmAccess.put("roles", null);
		claims.put("realm_access", realmAccess);

		// When
		var authorities = converter.convert(claims);

		// Then
		assertNotNull(authorities);
		assertTrue(authorities.isEmpty());
	}

	@Test
	void testAuthenticationConverter() {
		// Given
		SSOAuthConfig.AuthoritiesConverter authoritiesConverter = ssoAuthConfig.realmRolesAuthoritiesConverter();

		// When
		var mapper = ssoAuthConfig.authenticationConverter(authoritiesConverter);

		// Then
		assertNotNull(mapper);
	}

	@Test
	void testAuthenticationConverterWithOidcUserAuthority() {
		// Given
		SSOAuthConfig.AuthoritiesConverter authoritiesConverter = ssoAuthConfig.realmRolesAuthoritiesConverter();
		var mapper = ssoAuthConfig.authenticationConverter(authoritiesConverter);

		// Create mock OIDC user authority
		java.util.Map<String, Object> claims = new java.util.HashMap<>();
		java.util.Map<String, Object> realmAccess = new java.util.HashMap<>();
		java.util.List<String> roles = java.util.List.of("admin", "user");
		realmAccess.put("roles", roles);
		claims.put("realm_access", realmAccess);

		org.springframework.security.oauth2.core.oidc.OidcIdToken idToken =
				new org.springframework.security.oauth2.core.oidc.OidcIdToken(
						"token-value",
						java.time.Instant.now(),
						java.time.Instant.now().plusSeconds(3600),
						claims
				);

		org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority oidcAuthority =
				new org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority(idToken);

		java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
				java.util.List.of(oidcAuthority);

		// When
		var result = mapper.mapAuthorities(authorities);

		// Then
		assertNotNull(result);
		assertEquals(2, result.size());

		var authorityNames = result.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(java.util.stream.Collectors.toSet());

		assertTrue(authorityNames.contains("admin"));
		assertTrue(authorityNames.contains("user"));
	}

	@Test
	void testAuthenticationConverterWithNonOidcAuthority() {
		// Given
		SSOAuthConfig.AuthoritiesConverter authoritiesConverter = ssoAuthConfig.realmRolesAuthoritiesConverter();
		var mapper = ssoAuthConfig.authenticationConverter(authoritiesConverter);

		org.springframework.security.core.authority.SimpleGrantedAuthority simpleAuthority =
				new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER");

		java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
				java.util.List.of(simpleAuthority);

		// When
		var result = mapper.mapAuthorities(authorities);

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testAuthSuccessHandlerBeanCreation() {
		// When
		var handler = ssoAuthConfig.authSuccessHandler();

		// Then
		assertNotNull(handler);
		assertInstanceOf(com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler.class, handler);
	}

	@Test
	void testAuthSuccessHandlerWithNullAccessService() {
		// Given
		ReflectionTestUtils.setField(ssoAuthConfig, "accessService", null);

		// When
		var handler = ssoAuthConfig.authSuccessHandler();

		// Then
		assertNotNull(handler);
		assertInstanceOf(com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler.class, handler);
	}

	@Test
	void testAuthFailureHandlerBeanCreation() {
		// When
		var handler = ssoAuthConfig.authFailureHandler();

		// Then
		assertNotNull(handler);
		assertInstanceOf(com.almis.awe.security.handler.AweOauth2AuthenticationFailureHandler.class, handler);
	}

	@Test
	void testAuthFailureHandlerBehavior() throws Exception {
		// Given
		var handler = ssoAuthConfig.authFailureHandler();

		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
		AuthenticationException exception = new AuthenticationException("OAuth2 login failed") {
		};

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		org.mockito.Mockito.when(response.getWriter()).thenReturn(writer);

		// Mock ErrorPageService to return a valid HTML response
		String mockHtmlResponse = "<html><body><h1>Authentication Error</h1><p>OAuth2 login failed</p></body></html>";
		org.mockito.Mockito.when(errorPageService.generateErrorPageFromTemplate(
						com.almis.awe.model.type.ErrorTypology.AUTHENTICATION, null, "OAuth2 login failed"))
				.thenReturn(mockHtmlResponse);

		// When
		handler.onAuthenticationFailure(request, response, exception);

		// Then
		org.mockito.Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		org.mockito.Mockito.verify(response).setContentType("text/html; charset=UTF-8");

		writer.flush();
		String htmlResponse = stringWriter.toString();
		assertTrue(htmlResponse.contains("OAuth2 login failed"));
		assertTrue(htmlResponse.contains("Authentication Error"));
	}

	@Test
	void testAuthFailureHandlerWithNullMessage() throws Exception {
		// Given
		var handler = ssoAuthConfig.authFailureHandler();

		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
		AuthenticationException exception = new AuthenticationException(null) {
		};

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		org.mockito.Mockito.when(response.getWriter()).thenReturn(writer);

		// Mock ErrorPageService to return a valid HTML response for a null message
		String mockHtmlResponse = "<html><body><h1>Authentication Error</h1><p>Unknown authentication error</p></body></html>";
		org.mockito.Mockito.when(errorPageService.generateErrorPageFromTemplate(
						com.almis.awe.model.type.ErrorTypology.AUTHENTICATION, null, null))
				.thenReturn(mockHtmlResponse);

		// When
		handler.onAuthenticationFailure(request, response, exception);

		// Then
		org.mockito.Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		org.mockito.Mockito.verify(response).setContentType("text/html; charset=UTF-8");

		writer.flush();
		String htmlResponse = stringWriter.toString();
		assertTrue(htmlResponse.contains("Authentication Error"));
		assertTrue(htmlResponse.contains("Unknown authentication error"));
	}

	@Test
	void testSsoAndMultiTenantEnabled() {
		// When & Then
		this.runner.withPropertyValues(
				"awe.security.sso.enabled=true",
				"awe.security.sso.multitenant.enabled=true").run(context -> {
			// Verify that when both SSO and multitenant are enabled, the beans should exist
			// Note: This test verifies the conditional bean creation based on properties
			Assertions.assertThat(context).hasSingleBean(SSOAuthConfig.class);
			Assertions.assertThat(context).hasSingleBean(MultiTenantOAuth2Config.class);
			Assertions.assertThat(context).hasSingleBean(MultiTenantFilter.class);
		});
	}

	@Test
	void testSsoAndMultiTenantEnabledWithAutoLaunch() {
		// When & Then
		this.runner.withPropertyValues(
				"awe.security.sso.enabled=true",
				"awe.security.sso.auto-launch=true",
				"awe.security.sso.multitenant.enabled=true"
		).run(context -> {
			// Verify that when both SSO and multitenant are enabled, the beans should exist
			// Note: This test verifies the conditional bean creation based on properties
			Assertions.assertThat(context).hasSingleBean(SSOAuthConfig.class);
			Assertions.assertThat(context).hasSingleBean(MultiTenantOAuth2Config.class);
			Assertions.assertThat(context).hasSingleBean(MultiTenantFilter.class);

			// Get the SecurityFilterChain bean
			SecurityFilterChain filterChain = (SecurityFilterChain) context.getBean("oauth2FilterChain");

			Assertions.assertThat(filterChain).isNotNull();
		});
	}

	@Test
	void testSSOEnabledAndMultiTenantDisabled() {
		// When & Then
		this.runner.withPropertyValues(
						"awe.security.sso.enabled=true")
				.withBean(ClientRegistrationRepository.class, () -> {
					// Crear un mock simple que no requiere configuración de red
					ClientRegistration registration = ClientRegistration.withRegistrationId("test")
							.clientId("test-client")
							.clientSecret("test-secret")
							.authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
							.redirectUri("http://localhost:8080/login/oauth2/code/test")
							.authorizationUri("http://localhost:8081/auth")
							.tokenUri("http://localhost:8081/token")
							.userInfoUri("http://localhost:8081/userinfo")
							.userNameAttributeName("sub")
							.clientName("Test Client")
							.build();

					return new InMemoryClientRegistrationRepository(registration);
				})
				.run(context -> {
					// Verify that when SSO is enabled but multitenant is disabled,
					// only SSOAuthConfig should exist and MultiTenantFilter should not
					Assertions.assertThat(context).hasSingleBean(SSOAuthConfig.class);
					Assertions.assertThat(context).doesNotHaveBean(MultiTenantFilter.class);

					// Verify that a standard ClientRegistrationRepository is available
					Assertions.assertThat(context).hasSingleBean(ClientRegistrationRepository.class);

					// Verify it's the InMemory version (standard Spring Boot implementation)
					Assertions.assertThat(context.getBean(ClientRegistrationRepository.class))
							.isInstanceOf(InMemoryClientRegistrationRepository.class)
							.isNotInstanceOf(MultiTenantClientRegistrationRepository.class);
				});

	}
}