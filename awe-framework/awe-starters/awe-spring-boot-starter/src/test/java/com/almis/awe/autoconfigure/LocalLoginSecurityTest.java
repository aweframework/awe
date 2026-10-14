package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.security.authentication.LocalLoginAuthenticationManager;
import com.almis.awe.security.authentication.LocalLoginPolicy;
import com.almis.awe.security.authentication.filter.JsonAuthenticationFilter;
import com.almis.awe.service.ActionService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Local login switch under SSO: every local username and password path must apply the same policy and refused
 * users must be indistinguishable from users with a wrong password.
 */
@DisplayName("Local login switch with SSO")
class LocalLoginSecurityTest {

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(AweWebSecurityConfig.class))
    .withUserConfiguration(BaseConfigProperties.class, SecurityConfigProperties.class, MultiTenantOAuth2Config.class,
      AuthenticationWithSSOConfigTest.ExtraConfig.class, LocalProviderConfig.class)
    .withPropertyValues("awe.application.module-list=awe",
      "spring.cache.type=NONE",
      "awe.database.enabled=false",
      "awe.security.sso.enabled=true",
      "awe.security.sso.local-login=false",
      "awe.security.sso.break-glass-users=admin")
    .withBean(AuthenticationEventPublisher.class, () -> new DefaultAuthenticationEventPublisher(event -> { }))
    .withBean(NoOpCacheManager.class)
    .withBean(SpringTemplateEngine.class)
    .withBean(DefaultConversionService.class);

  /**
   * Local authentication provider: only the password "ok" is valid
   */
  @Configuration
  static class LocalProviderConfig {
    @Bean
    AuthenticationProvider localProvider() {
      return new AuthenticationProvider() {
        @Override
        public Authentication authenticate(Authentication authentication) {
          if (!"ok".equals(authentication.getCredentials())) {
            throw new BadCredentialsException("Bad credentials");
          }
          return UsernamePasswordAuthenticationToken.authenticated(authentication.getPrincipal(), null, List.of());
        }

        @Override
        public boolean supports(Class<?> authentication) {
          return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
      };
    }
  }

  private MockMvc mockMvc(AssertableWebApplicationContext context) {
    return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
      .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
      .build();
  }

  private Cookie csrfCookie(MockMvc mvc) throws Exception {
    Cookie cookie = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
    assertThat(cookie).as("CSRF cookie").isNotNull();
    return cookie;
  }

  private void jsonLogin(MockMvc mvc, Cookie csrf, String user, String password) throws Exception {
    mvc.perform(post("/action/login")
      .cookie(csrf)
      .header("X-XSRF-TOKEN", csrf.getValue())
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + user + "\",\"pwd_usr\":\"" + password + "\"}"));
  }

  private String formLogin(MockMvc mvc, Cookie csrf, String user, String password) throws Exception {
    return mvc.perform(post("/")
        .cookie(csrf)
        .header("X-XSRF-TOKEN", csrf.getValue())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("username", user)
        .param("password", password))
      .andReturn().getResponse().getRedirectedUrl();
  }

  private void stubLocales(AssertableWebApplicationContext context) {
    AweElements elements = context.getBean(AweElements.class);
    when(elements.getLocale(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    when(elements.getLocale(anyString(), any(Object[].class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("Only the three login entry points use the decorated manager; the shared one stays untouched")
  void decoratedManagerOnlyAtTheEntryPoints() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean("authenticationManager")).isNotInstanceOf(LocalLoginAuthenticationManager.class);
      assertThat(context.getBean("localLoginAuthenticationManager")).isInstanceOf(LocalLoginAuthenticationManager.class);
      assertThat(context).hasSingleBean(LocalLoginPolicy.class);
      assertThat(ReflectionTestUtils.getField(context.getBean(JsonAuthenticationFilter.class), "authenticationManager"))
        .isSameAs(context.getBean("localLoginAuthenticationManager"));
    });
  }

  @Test
  @DisplayName("No provider of the HttpSecurity builder supports user name and password tokens, or the decorated parent would be bypassed")
  void noLocalProviderInTheBuilder() {
    runner.run(context -> {
      SecurityFilterChain chain = context.getBean("aweSecurityFilterChain", SecurityFilterChain.class);
      List<UsernamePasswordAuthenticationFilter> formFilters = chain.getFilters().stream()
        .filter(filter -> filter.getClass().equals(UsernamePasswordAuthenticationFilter.class))
        .map(UsernamePasswordAuthenticationFilter.class::cast)
        .toList();
      assertThat(formFilters).hasSize(1);

      ProviderManager builderManager = (ProviderManager) ReflectionTestUtils.getField(formFilters.get(0), "authenticationManager");
      assertThat(builderManager.getProviders()).noneMatch(provider -> provider.supports(UsernamePasswordAuthenticationToken.class));
      assertThat(ReflectionTestUtils.getField(builderManager, "parent")).isSameAs(context.getBean("localLoginAuthenticationManager"));
    });
  }

  @Test
  @DisplayName("A policy bean supplied by the application replaces the default one")
  void applicationPolicyIsUsed() {
    LocalLoginPolicy custom = new LocalLoginPolicy(new SecurityConfigProperties());
    runner.withBean("myOwnLoginPolicy", LocalLoginPolicy.class, () -> custom)
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(LocalLoginPolicy.class);
        assertThat(((LocalLoginAuthenticationManager) context.getBean("localLoginAuthenticationManager")).getLocalLoginPolicy()).isSameAs(custom);

        // The custom policy leaves local login open, so a regular user can sign in with the form
        MockMvc mvc = mockMvc(context);
        assertThat(formLogin(mvc, csrfCookie(mvc), "john", "ok")).doesNotContain("error");
      });
  }

  @Test
  @DisplayName("/action/login answers a refused user exactly like a wrong password")
  void jsonLoginRefusalIsIndistinguishable() {
    runner.run(context -> {
      stubLocales(context);
      MockMvc mvc = mockMvc(context);
      Cookie csrf = csrfCookie(mvc);
      ActionService actionService = context.getBean(ActionService.class);

      jsonLogin(mvc, csrf, "admin", "wrong-password");
      jsonLogin(mvc, csrf, "john", "ok");

      ArgumentCaptor<AWException> errors = ArgumentCaptor.forClass(AWException.class);
      verify(actionService, times(2)).launchError(eq("afterLogin"), errors.capture());
      AWException wrongPassword = errors.getAllValues().get(0);
      AWException refused = errors.getAllValues().get(1);
      assertThat(refused.getTitle()).isEqualTo(wrongPassword.getTitle()).isEqualTo("ERROR_TITLE_INVALID_CREDENTIALS");
      assertThat(refused.getMessage()).isEqualTo(wrongPassword.getMessage()).isEqualTo("ERROR_MESSAGE_INVALID_CREDENTIALS");
      assertThat(refused.getType()).isEqualTo(wrongPassword.getType());
      verify(actionService, never()).launchAction("afterLogin");
    });
  }

  @Test
  @DisplayName("/action/login still lets a break-glass user in")
  void breakGlassUserCanSignInWithTheJsonLogin() {
    runner.run(context -> {
      MockMvc mvc = mockMvc(context);

      jsonLogin(mvc, csrfCookie(mvc), "admin", "ok");

      verify(context.getBean(ActionService.class)).launchAction("afterLogin");
    });
  }

  @Test
  @DisplayName("The standard form login refuses non break-glass users even with the right password")
  void standardFormLoginIsRefused() {
    runner.run(context -> {
      MockMvc mvc = mockMvc(context);
      Cookie csrf = csrfCookie(mvc);

      String wrongPassword = formLogin(mvc, csrf, "admin", "wrong-password");
      String refused = formLogin(mvc, csrf, "john", "ok");

      assertThat(wrongPassword).endsWith("/?error");
      assertThat(refused).isEqualTo(wrongPassword);
      assertThat(formLogin(mvc, csrf, "admin", "ok")).doesNotContain("error");
    });
  }

  @Test
  @DisplayName("An unauthenticated request is still sent to the AWE sign-in screen, not to the OAuth2 provider")
  void unauthenticatedRequestGoesToTheSigninScreen() {
    runner.run(context -> {
      String redirect = mockMvc(context).perform(get("/some/protected/page").header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
        .andReturn().getResponse().getRedirectedUrl();

      // Spring Security 7 favors relative login redirects (Location: /) over absolute ones
      assertThat(redirect).isEqualTo("/");
    });
  }

  @Test
  @DisplayName("A refused login publishes a bad credentials failure event that audit and lockout listeners can see")
  void refusalPublishesAFailureEvent() {
    List<AbstractAuthenticationEvent> events = new ArrayList<>();
    runner.withBean("eventCollector", ApplicationListener.class, () -> (ApplicationListener<AbstractAuthenticationEvent>) events::add)
      .run(context -> {
        MockMvc mvc = mockMvc(context);

        jsonLogin(mvc, csrfCookie(mvc), "john", "ok");

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(AuthenticationFailureBadCredentialsEvent.class);
        assertThat(events.get(0).getAuthentication().getName()).isEqualTo("john");
      });
  }

  @Test
  @DisplayName("Several break-glass users bind from a comma separated property, ignoring spaces and blank entries")
  void breakGlassUsersBindFromAProperty() {
    runner.withPropertyValues("awe.security.sso.break-glass-users= admin , ,root ,")
      .run(context -> {
        LocalLoginPolicy policy = context.getBean(LocalLoginPolicy.class);

        assertThat(policy.check("admin")).isEqualTo("admin");
        assertThat(policy.check(" root ")).isEqualTo("root");
        assertThatThrownBy(() -> policy.check("")).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> policy.check("john")).isInstanceOf(BadCredentialsException.class);
      });
  }
}
