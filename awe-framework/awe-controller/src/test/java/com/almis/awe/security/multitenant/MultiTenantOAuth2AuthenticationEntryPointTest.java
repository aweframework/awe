package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiTenantOAuth2AuthenticationEntryPointTest {

  @Mock
  private SecurityConfigProperties securityConfigProperties;

  @Mock
  private MultiTenantOAuth2Config multiTenantConfig;

  @Mock
  private ClientRegistrationRepository clientRegistrationRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private AuthenticationException authException;

  private MultiTenantOAuth2AuthenticationEntryPoint entryPoint;
  private SecurityConfigProperties.Sso ssoConfig;

  @BeforeEach
  void setUp() {
    ssoConfig = new SecurityConfigProperties.Sso();
    lenient().when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    lenient().when(request.getContextPath()).thenReturn("");
    // Mock encodeRedirectURL to return the input URL (DefaultRedirectStrategy behavior)
    lenient().when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    entryPoint = new MultiTenantOAuth2AuthenticationEntryPoint(
        securityConfigProperties,
        multiTenantConfig,
        clientRegistrationRepository
    );
  }

  @Test
  void testCommence_WithAutoLaunchEnabled_AndValidRegistrationId() throws IOException {
    // Given
    String registrationId = "tenant1";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + registrationId);
    verify(clientRegistrationRepository).findByRegistrationId(registrationId);
  }

  @Test
  void testCommence_WithAutoLaunchDisabled() throws IOException {
    // Given
    String registrationId = "tenant1";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(false);
    // The implementation always validates the registration ID, even when autoLaunch is false
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(null);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/");
    // Verify that validation still happens even when autoLaunch is false
    verify(clientRegistrationRepository).findByRegistrationId(registrationId);
  }

  @Test
  void testCommence_WithAutoLaunchEnabled_AndNullRegistrationId() throws IOException {
    // Given
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    ssoConfig.setAutoLaunch(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + defaultRegistrationId);
    verify(multiTenantConfig).getDefaultTenant();
    verify(multiTenantConfig).getRegistrationId(defaultTenant);
  }

  @Test
  void testCommence_WithAutoLaunchEnabled_AndEmptyRegistrationId() throws IOException {
    // Given
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn("");
    ssoConfig.setAutoLaunch(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + defaultRegistrationId);
    verify(multiTenantConfig).getDefaultTenant();
    verify(multiTenantConfig).getRegistrationId(defaultTenant);
  }

  @Test
  void testCommence_WithAutoLaunchEnabled_AndInvalidRegistrationId() throws IOException {
    // Given
    String invalidRegistrationId = "invalid";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(invalidRegistrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(invalidRegistrationId)).thenReturn(null);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + defaultRegistrationId);
    verify(clientRegistrationRepository).findByRegistrationId(invalidRegistrationId);
    verify(multiTenantConfig).getDefaultTenant();
  }

  @Test
  void testCommence_WithAutoLaunchEnabled_AndExceptionDuringValidation() throws IOException {
    // Given
    String registrationId = "tenant1";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenThrow(new RuntimeException("Repository error"));
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + defaultRegistrationId);
    verify(multiTenantConfig).getDefaultTenant();
  }

  @Test
  void testCommence_WithAutoLaunchDisabled_AndValidRegistrationId() throws IOException {
    // Given
    String registrationId = "tenant1";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(false);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/");
  }

  @Test
  void testCommence_RedirectsToCorrectOAuth2Endpoint() throws IOException {
    // Given
    String registrationId = "custom-client";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/custom-client");
  }

  @Test
  void testCommence_WithMultipleCalls() throws IOException {
    // Given
    String registrationId1 = "tenant1";
    String registrationId2 = "tenant2";
    ClientRegistration clientRegistration1 = createClientRegistration(registrationId1);
    ClientRegistration clientRegistration2 = createClientRegistration(registrationId2);

    ssoConfig.setAutoLaunch(true);

    // First call
    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId1);
    when(clientRegistrationRepository.findByRegistrationId(registrationId1)).thenReturn(clientRegistration1);
    entryPoint.commence(request, response, authException);

    // Second call
    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId2);
    when(clientRegistrationRepository.findByRegistrationId(registrationId2)).thenReturn(clientRegistration2);
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/" + registrationId1);
    verify(response).sendRedirect("/oauth2/authorization/" + registrationId2);
  }

  @Test
  void testCommence_VerifyRedirectStrategyUsed() throws IOException {
    // Given
    String registrationId = "tenant1";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect(anyString());
  }

  @Test
  void testCommence_WithAutoLaunchTrue_RedirectsToOAuth2Authorization() throws IOException {
    // Given
    String registrationId = "azure";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/oauth2/authorization/azure");
    verify(response, never()).sendRedirect("/");
  }

  @Test
  void testCommence_WithAutoLaunchFalse_RedirectsToRoot() throws IOException {
    // Given
    String registrationId = "any-id";
    String defaultTenant = "default";
    String defaultRegistrationId = "default-registration";
    
    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(false);
    // The implementation always validates the registration ID, even when autoLaunch is false
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(null);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(response).sendRedirect("/");
    verify(response, never()).sendRedirect(contains("/oauth2/authorization/"));
  }

  @Test
  void testConstructor() {
    // When
    MultiTenantOAuth2AuthenticationEntryPoint newEntryPoint = new MultiTenantOAuth2AuthenticationEntryPoint(
        securityConfigProperties,
        multiTenantConfig,
        clientRegistrationRepository
    );

    // Then
    assertNotNull(newEntryPoint);
  }

  @Test
  void testCommence_GetValidRegistrationId_WithNullAndDefaultTenant() throws IOException {
    // Given
    String defaultTenant = "tenant-default";
    String defaultRegistrationId = "registration-default";

    when(request.getAttribute("oauth2.registration.id")).thenReturn(null);
    ssoConfig.setAutoLaunch(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(multiTenantConfig.getRegistrationId(defaultTenant)).thenReturn(defaultRegistrationId);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(multiTenantConfig).getDefaultTenant();
    verify(multiTenantConfig).getRegistrationId(defaultTenant);
    verify(response).sendRedirect("/oauth2/authorization/" + defaultRegistrationId);
  }

  @Test
  void testCommence_ValidatesRegistrationIdExists() throws IOException {
    // Given
    String registrationId = "valid-id";
    ClientRegistration clientRegistration = createClientRegistration(registrationId);

    when(request.getAttribute("oauth2.registration.id")).thenReturn(registrationId);
    ssoConfig.setAutoLaunch(true);
    when(clientRegistrationRepository.findByRegistrationId(registrationId)).thenReturn(clientRegistration);

    // When
    entryPoint.commence(request, response, authException);

    // Then
    verify(clientRegistrationRepository).findByRegistrationId(registrationId);
    verify(response).sendRedirect("/oauth2/authorization/" + registrationId);
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
