package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.oidc.session.InMemoryOidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.HttpSessionIdChangedEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * OIDC back-channel logout: the identity provider can end AWE sessions. Off by default and only with SSO.
 */
@DisplayName("OIDC back-channel logout")
class BackChannelLogoutSecurityTest {

  private static final String BACK_CHANNEL_FILTER = "OidcBackChannelLogoutFilter";
  private static final String SSO_ENABLED = "awe.security.sso.enabled=true";
  private static final String BACK_CHANNEL_ENABLED = "awe.security.sso.back-channel-logout.enabled=true";
  private static final String HANDLER_BEAN = "oidcBackChannelLogoutHandler";
  private static final String CONNECT_TIMEOUT = "connectTimeout";
  private static final String READ_TIMEOUT = "readTimeout";

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(AweWebSecurityConfig.class))
    .withUserConfiguration(BaseConfigProperties.class, SecurityConfigProperties.class, MultiTenantOAuth2Config.class,
      AuthenticationWithSSOConfigTest.ExtraConfig.class)
    .withPropertyValues("awe.application.module-list=awe",
      "spring.cache.type=NONE",
      "awe.database.enabled=false")
    .withBean(NoOpCacheManager.class)
    .withBean(SpringTemplateEngine.class)
    .withBean(DefaultConversionService.class);

  private static List<String> filterNames(AssertableWebApplicationContext context) {
    return context.getBean("aweSecurityFilterChain", SecurityFilterChain.class).getFilters().stream()
      .map(filter -> filter.getClass().getSimpleName())
      .toList();
  }

  private static MockMvc mockMvc(AssertableWebApplicationContext context) {
    return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
      .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
      .build();
  }

  @Test
  @DisplayName("Is off by default: no back-channel endpoint and no registry bean")
  void offByDefault() {
    runner.withPropertyValues(SSO_ENABLED).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(filterNames(context)).doesNotContain(BACK_CHANNEL_FILTER);
      assertThat(context).doesNotHaveBean(HANDLER_BEAN);
      assertThat(new SecurityConfigProperties.Sso().getBackChannelLogout().isEnabled()).isFalse();
    });
  }

  @Test
  @DisplayName("Is ignored when SSO is disabled")
  void ignoredWithoutSso() {
    runner.withPropertyValues(BACK_CHANNEL_ENABLED).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(filterNames(context)).doesNotContain(BACK_CHANNEL_FILTER);
      assertThat(context).doesNotHaveBean(OidcSessionRegistry.class);
      assertThat(context).doesNotHaveBean(HANDLER_BEAN);
    });
  }

  @Test
  @DisplayName("Registers the back-channel endpoint with an in-memory session registry when enabled")
  void enabledRegistersEndpoint() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(filterNames(context)).contains(BACK_CHANNEL_FILTER);
      assertThat(context).getBean(OidcSessionRegistry.class).isInstanceOf(InMemoryOidcSessionRegistry.class);
      assertThat(context).hasSingleBean(AweOidcBackChannelLogoutHandler.class);
    });
  }

  @Test
  @DisplayName("Answers the provider itself: a request without a valid logout token is a bad request")
  void endpointRejectsMissingToken() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      int status = mockMvc(context).perform(post("/logout/connect/back-channel/awe")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .param("logout_token", "not-a-token"))
        .andReturn().getResponse().getStatus();
      assertThat(status).isEqualTo(400);
    });
  }

  @Test
  @DisplayName("Ends the session with the AWE session cookie name and the default internal logout address")
  void handlerUsesSessionCookieName() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED, "session.cookie.name=AWESESSION").run(context -> {
      Object handler = context.getBean(HANDLER_BEAN);
      assertThat(ReflectionTestUtils.getField(handler, "sessionCookieName")).isEqualTo("AWESESSION");
      assertThat(ReflectionTestUtils.getField(handler, "logoutUri"))
        .isEqualTo(AweOidcBackChannelLogoutHandler.DEFAULT_LOGOUT_URI);
    });
  }

  @Test
  @DisplayName("Uses the configured internal logout address")
  void handlerUsesConfiguredLogoutUri() {
    String uri = "http://localhost:8080/app/logout/connect/back-channel/{registrationId}";
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED, "awe.security.sso.back-channel-logout.logout-uri=" + uri)
      .run(context -> assertThat(ReflectionTestUtils.getField(context.getBean(HANDLER_BEAN), "logoutUri")).isEqualTo(uri));
  }

  private static Object timeout(Object handler, String name) {
    RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(handler, "restOperations");
    return ReflectionTestUtils.getField(restTemplate.getRequestFactory(), name);
  }

  @Test
  @DisplayName("Bounds the internal logout call with short default timeouts, so an unreachable address cannot hang the provider")
  void internalCallHasDefaultTimeouts() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      Object handler = context.getBean(HANDLER_BEAN);
      assertThat(timeout(handler, CONNECT_TIMEOUT)).isEqualTo(2000);
      assertThat(timeout(handler, READ_TIMEOUT)).isEqualTo(5000);
    });
  }

  @Test
  @DisplayName("Uses the configured timeouts for the internal logout call")
  void internalCallUsesConfiguredTimeouts() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED,
        "awe.security.sso.back-channel-logout.connect-timeout=500ms",
        "awe.security.sso.back-channel-logout.read-timeout=1s")
      .run(context -> {
        Object handler = context.getBean(HANDLER_BEAN);
        assertThat(timeout(handler, CONNECT_TIMEOUT)).isEqualTo(500);
        assertThat(timeout(handler, READ_TIMEOUT)).isEqualTo(1000);
      });
  }

  private static OidcSessionInformation sessionInformation(String sessionId) {
    OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
      Map.of("sub", "user", "iss", "https://idp.example.com", "sid", "provider-" + sessionId));
    return new OidcSessionInformation(sessionId, Map.of(), new DefaultOidcUser(List.of(), idToken));
  }

  @Test
  @DisplayName("Forgets a session in the registry when the HTTP session ends (expiry or invalidation), so the registry does not grow")
  void destroyedSessionsLeaveTheRegistry() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      assertThat(context).hasSingleBean(HttpSessionEventPublisher.class);
      OidcSessionRegistry registry = context.getBean(OidcSessionRegistry.class);
      registry.saveSessionInformation(sessionInformation("ended"));
      registry.saveSessionInformation(sessionInformation("alive"));

      context.publishEvent(new HttpSessionDestroyedEvent(new MockHttpSession(null, "ended")));

      assertThat(registry.removeSessionInformation("ended")).isNull();
      assertThat(registry.removeSessionInformation("alive")).isNotNull();
    });
  }

  @Test
  @DisplayName("Follows the new session id when the session id changes at login")
  void changedSessionIdIsFollowed() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      OidcSessionRegistry registry = context.getBean(OidcSessionRegistry.class);
      registry.saveSessionInformation(sessionInformation("before"));

      context.publishEvent(new HttpSessionIdChangedEvent(new MockHttpSession(null, "after"), "before"));

      assertThat(registry.removeSessionInformation("before")).isNull();
      assertThat(registry.removeSessionInformation("after")).isNotNull();
    });
  }

  @Test
  @DisplayName("Honours a session registry provided by the application (for example a shared one in a cluster)")
  void applicationRegistryIsHonoured() {
    OidcSessionRegistry registry = mock(OidcSessionRegistry.class);
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED)
      .withBean("sharedOidcSessionRegistry", OidcSessionRegistry.class, () -> registry)
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context.getBean(OidcSessionRegistry.class)).isSameAs(registry);
        assertThat(ReflectionTestUtils.getField(context.getBean(HANDLER_BEAN), "sessionRegistry")).isSameAs(registry);
      });
  }

  @Test
  @DisplayName("Ends the user's session through the AWE logout chain, so per-session AWE state is released")
  void internalLogoutRunsAweLogoutChain() {
    runner.withPropertyValues(SSO_ENABLED, BACK_CHANNEL_ENABLED).run(context -> {
      Filter filter = context.getBean("aweSecurityFilterChain", SecurityFilterChain.class).getFilters().stream()
        .filter(candidate -> BACK_CHANNEL_FILTER.equals(candidate.getClass().getSimpleName()))
        .findFirst().orElseThrow();
      LogoutHandler logoutHandler = (LogoutHandler) ReflectionTestUtils.getField(filter, "logoutHandler");
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout/connect/back-channel/awe");
      request.setParameter("_spring_security_internal_logout", "true");
      MockHttpSession session = new MockHttpSession();
      request.setSession(session);

      logoutHandler.logout(request, new MockHttpServletResponse(), null);

      verify(context.getBean(AweSessionDetails.class)).onLogoutSuccess();
      assertThat(session.isInvalid()).isTrue();
    });
  }
}
