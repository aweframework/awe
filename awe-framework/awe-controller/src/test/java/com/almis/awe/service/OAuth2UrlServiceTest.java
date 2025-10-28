package com.almis.awe.service;

import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.security.multitenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2UrlServiceTest {

  @Mock
  private MultiTenantOAuth2Config multiTenantConfig;

  @Mock
  private ClientRegistrationRepository clientRegistrationRepository;

  @Mock
  private HttpServletRequest request;

  private OAuth2UrlService oauth2UrlService;
  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    oauth2UrlService = new OAuth2UrlService(multiTenantConfig, clientRegistrationRepository);
    tenantContextMock = mockStatic(TenantContext.class);
  }

  @AfterEach
  void tearDown() {
    if (tenantContextMock != null) {
      tenantContextMock.close();
    }
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithRequestAttribute() {
    // Given
    String registrationId = "tenant1";
    String contextPath = "/myapp";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    when(request.getContextPath()).thenReturn(contextPath);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/myapp/oauth2/authorization/tenant1", url);
    verify(request).getAttribute("oauth2.registration.id");
    verify(clientRegistrationRepository).findByRegistrationId(registrationId);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithEmptyContextPath() {
    // Given
    String registrationId = "tenant1";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    when(request.getContextPath()).thenReturn("");
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/tenant1", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_FromTenantContext() {
    // Given
    String tenant = "tenant2";
    String registrationId = "tenant2-registration";
    String contextPath = "/app";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn(contextPath);
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenant);
    when(multiTenantConfig.getRegistrationId(tenant)).thenReturn(registrationId);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/app/oauth2/authorization/tenant2-registration", url);
    tenantContextMock.verify(TenantContext::getCurrentTenant);
    verify(multiTenantConfig).getRegistrationId(tenant);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithEmptyRequestAttribute() {
    // Given
    String tenant = "tenant2";
    String registrationId = "tenant2-registration";
    String contextPath = "/app";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn("");
    when(request.getContextPath()).thenReturn(contextPath);
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenant);
    when(multiTenantConfig.getRegistrationId(tenant)).thenReturn(registrationId);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/app/oauth2/authorization/tenant2-registration", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithDefaultRegistrationId() {
    // Given
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";
    String contextPath = "";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn(contextPath);
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/default-registration", url);
    verify(multiTenantConfig).getDefaultTenant();
    verify(multiTenantConfig).getRegistrationId(defaultTenant);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithInvalidRegistrationId_FallsBackToDefault() {
    // Given
    String invalidRegistrationId = "invalid";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(invalidRegistrationId);
    when(request.getContextPath()).thenReturn("");
    when(clientRegistrationRepository.findByRegistrationId(invalidRegistrationId)).thenReturn(null);
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/default-registration", url);
    verify(clientRegistrationRepository).findByRegistrationId(invalidRegistrationId);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithExceptionDuringValidation() {
    // Given
    String registrationId = "tenant1";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    when(request.getContextPath()).thenReturn("");
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenThrow(new RuntimeException("Repository error"));
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/default-registration", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithNullMultiTenantConfig_UsesInMemoryRepository() {
    // Given
    String registrationId = "client1";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);
    InMemoryClientRegistrationRepository inMemoryRepo = new InMemoryClientRegistrationRepository(List.of(clientRegistration));
    OAuth2UrlService serviceWithInMemoryRepo = new OAuth2UrlService(null, inMemoryRepo);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn("");
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);

    // When
    String url = serviceWithInMemoryRepo.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/client1", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithMultiTenantConfigDisabled_UsesInMemoryRepository() {
    // Given
    String registrationId = "client1";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);
    InMemoryClientRegistrationRepository inMemoryRepo = new InMemoryClientRegistrationRepository(List.of(clientRegistration));
    OAuth2UrlService serviceWithInMemoryRepo = new OAuth2UrlService(multiTenantConfig, inMemoryRepo);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn("");
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);
    when(multiTenantConfig.isEnabled()).thenReturn(false);

    // When
    String url = serviceWithInMemoryRepo.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/client1", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_WithNoRegistrations_ThrowsException() {
    // Given
    OAuth2UrlService serviceWithoutRegistrations = new OAuth2UrlService(null, clientRegistrationRepository);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);

    // When & Then
    assertThrows(IllegalStateException.class, () -> serviceWithoutRegistrations.getOAuth2AuthorizationUrl(request));
  }

  @Test
  void testGetOAuth2AuthorizationUrl_FromTenantContext_WithEmptyTenant() {
    // Given
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn("");
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("");
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/oauth2/authorization/default-registration", url);
  }

  @Test
  void testGetOAuth2AuthorizationUrl_FromTenantContext_WithValidTenant() {
    // Given
    String tenant = "tenant3";
    String registrationId = "tenant3-reg";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    when(request.getContextPath()).thenReturn("/mycontext");
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenant);
    when(multiTenantConfig.getRegistrationId(tenant)).thenReturn(registrationId);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    String url = oauth2UrlService.getOAuth2AuthorizationUrl(request);

    // Then
    assertEquals("/mycontext/oauth2/authorization/tenant3-reg", url);
    verify(multiTenantConfig).getRegistrationId(tenant);
  }

  @Test
  void testConstructor_WithNullMultiTenantConfig() {
    // When
    OAuth2UrlService service = new OAuth2UrlService(null, clientRegistrationRepository);

    // Then
    assertNotNull(service);
  }

  @Test
  void testConstructor_WithValidParameters() {
    // When
    OAuth2UrlService service = new OAuth2UrlService(multiTenantConfig, clientRegistrationRepository);

    // Then
    assertNotNull(service);
  }

  private ClientRegistration createClientRegistration(String registrationId) {
    return ClientRegistration.withRegistrationId(registrationId)
        .clientId("client-id")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("https://provider.com/oauth/authorize")
        .tokenUri("https://provider.com/oauth/token")
        .build();
  }
}
