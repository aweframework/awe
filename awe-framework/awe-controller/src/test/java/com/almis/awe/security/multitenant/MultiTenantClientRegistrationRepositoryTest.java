package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiTenantClientRegistrationRepositoryTest {

  @Mock
  private MultiTenantOAuth2Config multiTenantConfig;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private ServletRequestAttributes servletRequestAttributes;

  private MultiTenantClientRegistrationRepository repository;

  @BeforeEach
  void setUp() {
    repository = new MultiTenantClientRegistrationRepository(multiTenantConfig);
    when(multiTenantConfig.getDefaultTenant()).thenReturn("default");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void testFindByRegistrationIdWithTenantInContext() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals("client-id", result.getClientId());
    assertEquals("client-secret", result.getClientSecret());
  }

  @Test
  void testFindByRegistrationIdWithoutTenantInContextUsesDefault() {
    // Given
    String registrationId = "keycloak";
    String defaultTenant = "default";

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(defaultTenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(defaultTenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals("client-id", result.getClientId());
    verify(multiTenantConfig).getRegistration(defaultTenant);
  }

  @Test
  void testFindByRegistrationIdWithTenantFromRequest() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String registrationId = "keycloak";
      String tenant = "tenant1";

      when(httpServletRequest.getServerName()).thenReturn("tenant1.example.com");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      assertEquals("client-id", result.getClientId());
      verify(multiTenantConfig).getRegistration(tenant);
    }
  }

  @Test
  void testFindByRegistrationIdWithInvalidTenantFromRequestUsesDefault() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String registrationId = "keycloak";
      String invalidTenant = "invalid";
      String defaultTenant = "default";

      when(httpServletRequest.getServerName()).thenReturn("invalid.example.com");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      when(multiTenantConfig.hasTenant(invalidTenant)).thenReturn(false);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(defaultTenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(defaultTenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      verify(multiTenantConfig).getRegistration(defaultTenant);
    }
  }

  @Test
  void testFindByRegistrationIdWithNoConfiguration() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(null);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(null);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNull(result);
  }

  @Test
  void testFindByRegistrationIdWithMissingRegistration() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(null);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNull(result);
  }

  @Test
  void testFindByRegistrationIdWithMissingProvider() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(null);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNull(result);
  }

  @Test
  void testFindByRegistrationIdWithManualConfiguration() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    OAuth2ClientProperties.Provider provider = createMockProviderWithoutIssuerUri();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals("client-id", result.getClientId());
    assertEquals("client-secret", result.getClientSecret());
    assertEquals("https://auth.example.com/auth", result.getProviderDetails().getAuthorizationUri());
    assertEquals("https://auth.example.com/token", result.getProviderDetails().getTokenUri());
  }

  @Test
  void testResolveRedirectUriWithAbsoluteUrl() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String tenant = "tenant1";
      String registrationId = "keycloak";
      TenantContext.setCurrentTenant(tenant);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      registration.setRedirectUri("https://absolute.example.com/callback");
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      assertEquals("https://absolute.example.com/callback", result.getRedirectUri());
    }
  }

  @Test
  void testResolveRedirectUriWithPlaceholder() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String tenant = "tenant1";
      String registrationId = "keycloak";
      TenantContext.setCurrentTenant(tenant);

      when(httpServletRequest.getScheme()).thenReturn("https");
      when(httpServletRequest.getServerName()).thenReturn("tenant1.example.com");
      when(httpServletRequest.getServerPort()).thenReturn(443);
      when(httpServletRequest.getContextPath()).thenReturn("");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      registration.setRedirectUri("{baseUrl}/login/oauth2/code/keycloak");
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      assertEquals("https://tenant1.example.com/login/oauth2/code/keycloak", result.getRedirectUri());
    }
  }

  @Test
  void testResolveRedirectUriWithPlaceholderAndCustomPort() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String tenant = "tenant1";
      String registrationId = "keycloak";
      TenantContext.setCurrentTenant(tenant);

      when(httpServletRequest.getScheme()).thenReturn("http");
      when(httpServletRequest.getServerName()).thenReturn("tenant1.example.com");
      when(httpServletRequest.getServerPort()).thenReturn(8080);
      when(httpServletRequest.getContextPath()).thenReturn("/app");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      registration.setRedirectUri("{baseUrl}/login/oauth2/code/keycloak");
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      assertEquals("http://tenant1.example.com:8080/app/login/oauth2/code/keycloak", result.getRedirectUri());
    }
  }

  @Test
  void testResolveRedirectUriWithNullRedirectUri() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    registration.setRedirectUri(null);
    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals("http://localhost:8080/login/oauth2/code/keycloak", result.getRedirectUri());
  }

  @Test
  void testClientAuthenticationMethodConversion() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    registration.setClientAuthenticationMethod("client_secret_post");
    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_POST, result.getClientAuthenticationMethod());
  }

  @Test
  void testAuthorizationGrantTypeConversion() {
    // Given
    String tenant = "tenant1";
    String registrationId = "keycloak";
    TenantContext.setCurrentTenant(tenant);

    OAuth2ClientProperties.Registration registration = createMockRegistration();
    registration.setAuthorizationGrantType("client_credentials");
    OAuth2ClientProperties.Provider provider = createMockProvider();

    when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
    when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

    // When
    ClientRegistration result = repository.findByRegistrationId(registrationId);

    // Then
    assertNotNull(result);
    assertEquals(AuthorizationGrantType.CLIENT_CREDENTIALS, result.getAuthorizationGrantType());
  }

  @Test
  void testExtractTenantFromServerNameWithValidSubdomain() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String registrationId = "keycloak";
      String tenant = "tenant1";

      when(httpServletRequest.getServerName()).thenReturn("tenant1.example.com");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration(tenant)).thenReturn(registration);
      when(multiTenantConfig.getProvider(tenant)).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      verify(multiTenantConfig).getRegistration(tenant);
    }
  }

  @Test
  void testExtractTenantFromServerNameWithInvalidSubdomain() {
    try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
      // Given
      String registrationId = "keycloak";

      when(httpServletRequest.getServerName()).thenReturn("localhost");
      when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
      mockedRequestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      OAuth2ClientProperties.Registration registration = createMockRegistration();
      OAuth2ClientProperties.Provider provider = createMockProvider();

      when(multiTenantConfig.getRegistration("default")).thenReturn(registration);
      when(multiTenantConfig.getProvider("default")).thenReturn(provider);

      // When
      ClientRegistration result = repository.findByRegistrationId(registrationId);

      // Then
      assertNotNull(result);
      verify(multiTenantConfig).getRegistration("default");
    }
  }

  private OAuth2ClientProperties.Registration createMockRegistration() {
    OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
    registration.setClientId("client-id");
    registration.setClientSecret("client-secret");
    registration.setRedirectUri("{baseUrl}/login/oauth2/code/keycloak");
    registration.setScope(java.util.Set.of("openid", "profile"));
    registration.setClientName("Test Client");
    registration.setClientAuthenticationMethod("client_secret_basic");
    registration.setAuthorizationGrantType("authorization_code");
    return registration;
  }

  private OAuth2ClientProperties.Provider createMockProvider() {
    OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
    // Use manual configuration instead of issuer URI to avoid network calls
    provider.setAuthorizationUri("https://auth.example.com/auth");
    provider.setTokenUri("https://auth.example.com/token");
    provider.setUserInfoUri("https://auth.example.com/userinfo");
    provider.setUserNameAttribute("preferred_username");
    provider.setJwkSetUri("https://auth.example.com/jwks");
    return provider;
  }

  private OAuth2ClientProperties.Provider createMockProviderWithoutIssuerUri() {
    OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
    provider.setAuthorizationUri("https://auth.example.com/auth");
    provider.setTokenUri("https://auth.example.com/token");
    provider.setUserInfoUri("https://auth.example.com/userinfo");
    provider.setUserNameAttribute("preferred_username");
    provider.setJwkSetUri("https://auth.example.com/jwks");
    return provider;
  }
}
