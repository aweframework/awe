package com.almis.awe.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MultiTenantOAuth2ConfigTest {

  private MultiTenantOAuth2Config config;

  @BeforeEach
  void setUp() {
    config = new MultiTenantOAuth2Config();
    config.setEnabled(true);
    config.setDefaultTenant("default");
  }

  @Test
  void testDefaultValues() {
    // Given
    MultiTenantOAuth2Config newConfig = new MultiTenantOAuth2Config();

    // Then
    assertFalse(newConfig.isEnabled());
    assertEquals("default", newConfig.getDefaultTenant());
    assertNotNull(newConfig.getRegistrations());
    assertNotNull(newConfig.getProviders());
    assertTrue(newConfig.getRegistrations().isEmpty());
    assertTrue(newConfig.getProviders().isEmpty());
  }

  @Test
  void testSettersAndGetters() {
    // Given
    Map<String, OAuth2ClientProperties.Registration> registrations = new HashMap<>();
    Map<String, OAuth2ClientProperties.Provider> providers = new HashMap<>();

    // When
    config.setEnabled(false);
    config.setDefaultTenant("custom-default");
    config.setRegistrations(registrations);
    config.setProviders(providers);

    // Then
    assertFalse(config.isEnabled());
    assertEquals("custom-default", config.getDefaultTenant());
    assertEquals(registrations, config.getRegistrations());
    assertEquals(providers, config.getProviders());
  }

  @Test
  void testGetRegistrationExistingTenant() {
    // Given
    String tenantId = "tenant1";
    OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
    registration.setClientId("client1");
    config.getRegistrations().put(tenantId, registration);

    // When
    OAuth2ClientProperties.Registration result = config.getRegistration(tenantId);

    // Then
    assertNotNull(result);
    assertEquals("client1", result.getClientId());
    assertEquals(registration, result);
  }

  @Test
  void testGetRegistrationNonExistingTenantFallsBackToDefault() {
    // Given
    String nonExistingTenant = "non-existing";
    String defaultTenant = "default";
    OAuth2ClientProperties.Registration defaultRegistration = new OAuth2ClientProperties.Registration();
    defaultRegistration.setClientId("default-client");
    config.getRegistrations().put(defaultTenant, defaultRegistration);

    // When
    OAuth2ClientProperties.Registration result = config.getRegistration(nonExistingTenant);

    // Then
    assertNotNull(result);
    assertEquals("default-client", result.getClientId());
    assertEquals(defaultRegistration, result);
  }

  @Test
  void testGetRegistrationNonExistingTenantAndNoDefault() {
    // Given
    String nonExistingTenant = "non-existing";

    // When
    OAuth2ClientProperties.Registration result = config.getRegistration(nonExistingTenant);

    // Then
    assertNull(result);
  }

  @Test
  void testGetProviderExistingTenant() {
    // Given
    String tenantId = "tenant1";
    OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
    provider.setIssuerUri("https://tenant1.example.com");
    config.getProviders().put(tenantId, provider);

    // When
    OAuth2ClientProperties.Provider result = config.getProvider(tenantId);

    // Then
    assertNotNull(result);
    assertEquals("https://tenant1.example.com", result.getIssuerUri());
    assertEquals(provider, result);
  }

  @Test
  void testGetProviderNonExistingTenantFallsBackToDefault() {
    // Given
    String nonExistingTenant = "non-existing";
    String defaultTenant = "default";
    OAuth2ClientProperties.Provider defaultProvider = new OAuth2ClientProperties.Provider();
    defaultProvider.setIssuerUri("https://default.example.com");
    config.getProviders().put(defaultTenant, defaultProvider);

    // When
    OAuth2ClientProperties.Provider result = config.getProvider(nonExistingTenant);

    // Then
    assertNotNull(result);
    assertEquals("https://default.example.com", result.getIssuerUri());
    assertEquals(defaultProvider, result);
  }

  @Test
  void testGetProviderNonExistingTenantAndNoDefault() {
    // Given
    String nonExistingTenant = "non-existing";

    // When
    OAuth2ClientProperties.Provider result = config.getProvider(nonExistingTenant);

    // Then
    assertNull(result);
  }

  @Test
  void testHasTenantWithBothRegistrationAndProvider() {
    // Given
    String tenantId = "tenant1";
    OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
    OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
    config.getRegistrations().put(tenantId, registration);
    config.getProviders().put(tenantId, provider);

    // When
    boolean result = config.hasTenant(tenantId);

    // Then
    assertTrue(result);
  }

  @Test
  void testHasTenantWithOnlyRegistration() {
    // Given
    String tenantId = "tenant1";
    OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
    config.getRegistrations().put(tenantId, registration);

    // When
    boolean result = config.hasTenant(tenantId);

    // Then
    assertFalse(result);
  }

  @Test
  void testHasTenantWithOnlyProvider() {
    // Given
    String tenantId = "tenant1";
    OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
    config.getProviders().put(tenantId, provider);

    // When
    boolean result = config.hasTenant(tenantId);

    // Then
    assertFalse(result);
  }

  @Test
  void testHasTenantWithNeitherRegistrationNorProvider() {
    // Given
    String tenantId = "non-existing";

    // When
    boolean result = config.hasTenant(tenantId);

    // Then
    assertFalse(result);
  }

  @Test
  void testHasTenantWithNullTenant() {
    // When
    boolean result = config.hasTenant(null);

    // Then
    assertFalse(result);
  }

  @Test
  void testGetRegistrationWithNullTenant() {
    // Given
    OAuth2ClientProperties.Registration defaultRegistration = new OAuth2ClientProperties.Registration();
    defaultRegistration.setClientId("default-client");
    config.getRegistrations().put("default", defaultRegistration);

    // When
    OAuth2ClientProperties.Registration result = config.getRegistration(null);

    // Then
    assertNotNull(result);
    assertEquals("default-client", result.getClientId());
  }

  @Test
  void testGetProviderWithNullTenant() {
    // Given
    OAuth2ClientProperties.Provider defaultProvider = new OAuth2ClientProperties.Provider();
    defaultProvider.setIssuerUri("https://default.example.com");
    config.getProviders().put("default", defaultProvider);

    // When
    OAuth2ClientProperties.Provider result = config.getProvider(null);

    // Then
    assertNotNull(result);
    assertEquals("https://default.example.com", result.getIssuerUri());
  }

  @Test
  void testMultipleTenants() {
    // Given
    OAuth2ClientProperties.Registration reg1 = new OAuth2ClientProperties.Registration();
    reg1.setClientId("client1");
    OAuth2ClientProperties.Registration reg2 = new OAuth2ClientProperties.Registration();
    reg2.setClientId("client2");

    OAuth2ClientProperties.Provider prov1 = new OAuth2ClientProperties.Provider();
    prov1.setIssuerUri("https://tenant1.example.com");
    OAuth2ClientProperties.Provider prov2 = new OAuth2ClientProperties.Provider();
    prov2.setIssuerUri("https://tenant2.example.com");

    config.getRegistrations().put("tenant1", reg1);
    config.getRegistrations().put("tenant2", reg2);
    config.getProviders().put("tenant1", prov1);
    config.getProviders().put("tenant2", prov2);

    // When & Then
    assertTrue(config.hasTenant("tenant1"));
    assertTrue(config.hasTenant("tenant2"));
    assertFalse(config.hasTenant("tenant3"));

    assertEquals("client1", config.getRegistration("tenant1").getClientId());
    assertEquals("client2", config.getRegistration("tenant2").getClientId());

    assertEquals("https://tenant1.example.com", config.getProvider("tenant1").getIssuerUri());
    assertEquals("https://tenant2.example.com", config.getProvider("tenant2").getIssuerUri());
  }
}