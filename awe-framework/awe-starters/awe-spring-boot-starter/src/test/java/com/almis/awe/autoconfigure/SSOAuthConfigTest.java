package com.almis.awe.autoconfigure;

import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.almis.awe.security.multitenant.MultiTenantClientRegistrationRepository;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.service.AccessService;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SSOAuthConfigTest {

  @Mock
  private SecurityConfigProperties securityConfigProperties;

  @Mock
  private MultiTenantOAuth2Config multiTenantConfig;

  @Mock
  private MultiTenantFilter multiTenantFilter;

  @Mock
  private AccessService accessService;

  @Mock
  private AweSessionDetails sessionDetails;

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
        Optional.of(multiTenantConfig),
        Optional.of(multiTenantFilter)
    );
  }

  @Test
  void testClientRegistrationRepositoryBeanCreation() {
    // When
    ClientRegistrationRepository repository = ssoAuthConfig.clientRegistrationRepository();

    // Then
    assertNotNull(repository);
    assertInstanceOf(MultiTenantClientRegistrationRepository.class, repository);
  }

  @Test
  void testClientRegistrationRepositoryWithNullConfig() {
    // Given
    ReflectionTestUtils.setField(ssoAuthConfig, "multiTenantConfig", null);

    // When
    ClientRegistrationRepository repository = ssoAuthConfig.clientRegistrationRepository();

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
    AuthenticationException exception = new AuthenticationException("OAuth2 login failed") {};

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    org.mockito.Mockito.when(response.getWriter()).thenReturn(writer);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    org.mockito.Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    org.mockito.Mockito.verify(response).setContentType("application/json");

    writer.flush();
    String jsonResponse = stringWriter.toString();
    assertTrue(jsonResponse.contains("OAuth2 login failed"));
    assertTrue(jsonResponse.contains("\"error\""));
  }

  @Test
  void testAuthFailureHandlerWithNullMessage() throws Exception {
    // Given
    var handler = ssoAuthConfig.authFailureHandler();

    HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
    AuthenticationException exception = new AuthenticationException(null) {};

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    org.mockito.Mockito.when(response.getWriter()).thenReturn(writer);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    org.mockito.Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    org.mockito.Mockito.verify(response).setContentType("application/json");

    writer.flush();
    String jsonResponse = stringWriter.toString();
    assertTrue(jsonResponse.contains("\"error\""));
    assertTrue(jsonResponse.contains("null"));
  }
}
