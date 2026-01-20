package com.almis.awe.autoconfigure;

import com.almis.ade.api.ADE;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.service.*;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.mockito.Mockito.mock;

/**
 * Test class for validating the behavior and context configuration of the AweWebSecurityConfig class.
 * <p>
 * This class uses a WebApplicationContextRunner to set up a Spring context for testing various
 * security-related configurations under different conditions, including the enabling and disabling of SSO (Single Sign-On).
 * <p>
 * The tests ensure that the expected beans are loaded or not loaded depending on the applied security configuration.
 */
class AuthenticationWithSSOConfigTest {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					AweWebSecurityConfig.class)
			)
			.withUserConfiguration(BaseConfigProperties.class,
					SecurityConfigProperties.class,
          MultiTenantOAuth2Config.class,
          ExtraConfig.class)
			.withPropertyValues("awe.application.module-list=awe",
					"spring.cache.type=NONE",
					"awe.database.enabled=false")
			.withBean(NoOpCacheManager.class)
			.withBean(SpringTemplateEngine.class)
			.withBean(DefaultConversionService.class);

	@Configuration
	static class ExtraConfig {
		@Bean public ClientRegistrationRepository clientRegistrationRepository() { return mock(ClientRegistrationRepository.class); }
		@Bean public ActionService actionService() { return mock(ActionService.class); }
		@Bean public AweElements aweElements() { return mock(AweElements.class); }
		@Bean public AweSessionDetails aweSessionDetails() { return mock(AweSessionDetails.class); }
		@Bean public ErrorPageService errorPageService() { return mock(ErrorPageService.class); }
		@Bean public MultiTenantFilter multiTenantFilter() { return mock(MultiTenantFilter.class); }
		@Bean public ObjectMapper objectMapper() { return new ObjectMapper(); }
		@Bean public AccessService accessService() { return mock(AccessService.class); }
		@Bean public XStreamSerializer xStreamSerializer() { return mock(XStreamSerializer.class); }
		@Bean public TemplateService templateService() { return mock(TemplateService.class); }
		@Bean public BroadcastService broadcastService() { return mock(BroadcastService.class); }
		@Bean public AweRequest aweRequest() { return mock(AweRequest.class); }
		@Bean public ADE ade() { return mock(ADE.class); }
		@Bean public ClientHttpRequestFactory clientHttpRequestFactory() { return mock(ClientHttpRequestFactory.class); }
		@Bean(name = "mvcHandlerMappingIntrospector")
		public HandlerMappingIntrospector mvcHandlerMappingIntrospector() { return new HandlerMappingIntrospector(); }
	}

	@Test
	void testSsoBeansLoadWhenSsoEnabled() {
		runner.withPropertyValues("awe.security.sso.enabled=true")
				.run(context -> {
					Assertions.assertThat(context).hasBean("multiTenantOAuth2AuthenticationEntryPoint");
					Assertions.assertThat(context).hasBean("realmRolesAuthoritiesConverter");
					Assertions.assertThat(context).hasBean("authenticationConverter");
					Assertions.assertThat(context).hasBean("authSuccessHandler");
					Assertions.assertThat(context).hasBean("authFailureHandler");
				});
	}

	@Test
	void testSsoBeansDoNotLoadWhenSsoDisabled() {
		runner.withPropertyValues("awe.security.sso.enabled=false")
				.run(context -> {
					Assertions.assertThat(context).doesNotHaveBean("multiTenantOAuth2AuthenticationEntryPoint");
					Assertions.assertThat(context).doesNotHaveBean("realmRolesAuthoritiesConverter");
					Assertions.assertThat(context).doesNotHaveBean("authenticationConverter");
					Assertions.assertThat(context).doesNotHaveBean("authSuccessHandler");
					Assertions.assertThat(context).doesNotHaveBean("authFailureHandler");
				});
	}

	@Test
	void testAutoLaunchConfig() {
		runner.withPropertyValues(
				"awe.security.sso.enabled=true",
				"awe.security.sso.auto-launch=true",
				"awe.security.sso.multitenant.enabled=true"
		).run(context -> {
			Assertions.assertThat(context).hasBean("aweSecurityFilterChain");
			Assertions.assertThat(context).hasBean("multiTenantOAuth2AuthenticationEntryPoint");
		});
	}

	@Test
	void testNoSsoAuthenticationBeans() {
		runner.withPropertyValues("awe.security.sso.enabled=false")
				.run(context -> {
					Assertions.assertThat(context).hasBean("jsonAuthenticationFilter");
					Assertions.assertThat(context).hasBean("securityContextRepository");
					Assertions.assertThat(context).hasBean("accessDeniedHandler");
					Assertions.assertThat(context).hasBean("actionAuthenticationEntryPoint");
					Assertions.assertThat(context).hasBean("logoutHandler");
				});
	}
}
