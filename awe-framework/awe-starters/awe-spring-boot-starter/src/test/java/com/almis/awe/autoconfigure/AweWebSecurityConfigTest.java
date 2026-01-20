package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ActionService;
import com.almis.awe.service.ErrorPageService;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AweWebSecurityConfigTest {

  private ApplicationContext context;
  private BaseConfigProperties baseConfig;
  private SecurityConfigProperties securityConfigProperties;
  private AweSessionDetails sessionDetails;
  private AweElements elements;
  private ActionService actionService;
  private ObjectMapper objectMapper;
  private AccessService accessService;
  private ErrorPageService errorPageService;
  private MultiTenantOAuth2Config multiTenantOAuth2Config;
  private MultiTenantFilter multiTenantFilter;

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);
    baseConfig = new BaseConfigProperties();
    securityConfigProperties = new SecurityConfigProperties();
    SecurityConfigProperties.Sso sso = new SecurityConfigProperties.Sso();
    sso.setEnabled(true);
    sso.setAutoLaunch(false);
    securityConfigProperties.setSso(sso);
    sessionDetails = mock(AweSessionDetails.class);
    elements = mock(AweElements.class);
    actionService = mock(ActionService.class);
    objectMapper = new ObjectMapper();
    accessService = mock(AccessService.class);
    errorPageService = mock(ErrorPageService.class);
    multiTenantOAuth2Config = new MultiTenantOAuth2Config();
    multiTenantFilter = mock(MultiTenantFilter.class);

    // Mock client registration repository for handler creation
    when(context.getBean(ClientRegistrationRepository.class)).thenReturn(mock(ClientRegistrationRepository.class));
  }

  private AweWebSecurityConfig buildConfig() {
    return new AweWebSecurityConfig(
      context,
      baseConfig,
      securityConfigProperties,
      sessionDetails,
      elements,
      actionService,
      objectMapper,
      accessService,
      errorPageService,
      multiTenantOAuth2Config,
      multiTenantFilter
    );
  }

  @Test
  void oidcLogoutSuccessHandler_shouldUseBaseUrl_whenAutoLaunchDisabled() throws Exception {
    // Given
    securityConfigProperties.getSso().setAutoLaunch(false);
    AweWebSecurityConfig config = buildConfig();

    // When
    LogoutSuccessHandler handler = invokeOidcLogoutSuccessHandler(config);

    // Then
    String postLogoutRedirect = extractPostLogoutRedirect(handler);
    assertThat(postLogoutRedirect).isEqualTo("{baseUrl}");
  }

  @Test
  void oidcLogoutSuccessHandler_shouldAppendLogoutSso_whenAutoLaunchEnabled() throws Exception {
    // Given
    securityConfigProperties.getSso().setAutoLaunch(true);
    AweWebSecurityConfig config = buildConfig();

    // When
    LogoutSuccessHandler handler = invokeOidcLogoutSuccessHandler(config);

    // Then
    String postLogoutRedirect = extractPostLogoutRedirect(handler);
    assertThat(postLogoutRedirect).isEqualTo("{baseUrl}/screen/public/logout-sso");
  }

  @Test
  void configureLogout_shouldRegisterLogoutAndOidcHandler_whenSsoEnabled() throws Exception {
    // Given a spy config to verify internal call
    AweWebSecurityConfig config = Mockito.spy(buildConfig());

    // Mock HttpSecurity to verify logout configuration is applied
    HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
    when(http.logout(any(Customizer.class))).thenReturn(http);

    // Access private method via reflection
    Method m = AweWebSecurityConfig.class.getDeclaredMethod("configureLogout", HttpSecurity.class);
    m.setAccessible(true);

    // When
    m.invoke(config, http);

    // Then
    verify(http, atLeastOnce()).logout(any());
  }

  private LogoutSuccessHandler invokeOidcLogoutSuccessHandler(AweWebSecurityConfig config) throws Exception {
    Method m = AweWebSecurityConfig.class.getDeclaredMethod("oidcLogoutSuccessHandler");
    m.setAccessible(true);
    return (LogoutSuccessHandler) m.invoke(config);
  }

  private String extractPostLogoutRedirect(LogoutSuccessHandler handler) throws Exception {
    // The field is private in Spring's handler, but named 'postLogoutRedirectUri'
    Field f = handler.getClass().getDeclaredField("postLogoutRedirectUri");
    f.setAccessible(true);
    Object value = f.get(handler);
    return value != null ? value.toString() : null;
  }
}
