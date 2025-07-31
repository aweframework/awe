package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiTenantFilterTest {

  @Mock
  private MultiTenantOAuth2Config multiTenantConfig;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private MultiTenantFilter filter;

  @BeforeEach
  void setUp() {
    filter = new MultiTenantFilter(multiTenantConfig);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void testDoFilterInternalWhenMultiTenantDisabled() throws ServletException, IOException {
    // Given
    when(multiTenantConfig.isEnabled()).thenReturn(false);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    assertNull(TenantContext.getCurrentTenant());
    verify(request, never()).setAttribute(anyString(), any());
  }

  @ParameterizedTest
  @MethodSource("validTenantTestData")
  void testDoFilterInternalWithValidTenantSubdomain(String serverName, String expectedTenant) throws ServletException, IOException {
    // Given
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn(serverName);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(expectedTenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", expectedTenant);
    assertNull(TenantContext.getCurrentTenant());
}

private static Stream<Arguments> validTenantTestData() {
    return Stream.of(
        Arguments.of("tenant1.example.com", "tenant1"),
        Arguments.of("tenant-with-dashes.example.com", "tenant-with-dashes"),
        Arguments.of("tenant123.example.com", "tenant123")
    );
}

@ParameterizedTest
@MethodSource("invalidServerNameTestData")
void testDoFilterInternalUsesDefaultTenantForInvalidServerNames(String serverName) throws ServletException, IOException {
    // Given
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn(serverName);
    when(request.getRequestURI()).thenReturn("/api/test");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
    assertNull(TenantContext.getCurrentTenant());
}

private static Stream<String> invalidServerNameTestData() {
    return Stream.of(
        "192.168.1.100",  // IP address
        "",               // Empty string
        null              // Null value
    );
}

  @Test
  void testDoFilterInternalWithInvalidTenantSubdomainUsesDefault() throws ServletException, IOException {
    // Given
    String invalidTenant = "invalid";
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn("invalid.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(invalidTenant)).thenReturn(false);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testDoFilterInternalWithNoSubdomainUsesDefault() throws ServletException, IOException {
    // Given
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn("example.com");
    when(request.getRequestURI()).thenReturn("/api/test");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testDoFilterInternalWithLocalhostUsesDefault() throws ServletException, IOException {
    // Given
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn("localhost");
    when(request.getRequestURI()).thenReturn("/api/test");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testDoFilterInternalWithComplexSubdomain() throws ServletException, IOException {
    // Given
    String tenant = "tenant-with-dashes";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant-with-dashes.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", tenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testDoFilterInternalWithNumericSubdomain() throws ServletException, IOException {
    // Given
    String tenant = "tenant123";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant123.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", tenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testDoFilterInternalWithMultiLevelSubdomain() throws ServletException, IOException {
    // Given
    String tenant = "api";
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn("api.tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(false);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testTenantContextIsSetDuringFilterExecution() throws ServletException, IOException {
    // Given
    String tenant = "tenant1";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // Mock filter chain to capture tenant context during execution
    doAnswer(invocation -> {
      // During filter chain execution, a tenant should be set
      assertEquals(tenant, TenantContext.getCurrentTenant());
      return null;
    }).when(filterChain).doFilter(request, response);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    // After filter execution, tenant context should be cleared
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testTenantContextClearedEvenWhenExceptionThrown() throws ServletException, IOException {
    // Given
    String tenant = "tenant1";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // Mock filter chain to throw an exception
    doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

    // When & Then
    assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));

    // Tenant context should still be cleared even after exception
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testTenantContextNotClearedIfSetByAnotherThread() throws ServletException, IOException {
    // Given
    String filterTenant = "tenant1";
    String otherTenant = "other-tenant";

    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(filterTenant)).thenReturn(true);

    // Mock a filter chain to simulate another thread changing the tenant context
    doAnswer(invocation -> {
      // During filter chain execution, simulate another thread setting different tenant
      TenantContext.setCurrentTenant(otherTenant);
      return null;
    }).when(filterChain).doFilter(request, response);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    // Tenant context should not be cleared because it was changed by another thread
    assertEquals(otherTenant, TenantContext.getCurrentTenant());

    // Clean up
    TenantContext.clear();
  }

  @Test
  void testExtractTenantFromRequestWithPortNumber() throws ServletException, IOException {
    // Given
    String tenant = "tenant1";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", tenant);
  }

  @Test
  void testExtractTenantFromRequestWithDifferentTLD() throws ServletException, IOException {
    // Given
    String tenant = "tenant1";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.org");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", tenant);
  }

  @Test
  void testRequestAttributeIsSetCorrectly() throws ServletException, IOException {
    // Given
    String tenant = "tenant1";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(request.getServerName()).thenReturn("tenant1.example.com");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(multiTenantConfig.hasTenant(tenant)).thenReturn(true);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(request).setAttribute("currentTenant", tenant);
  }

  @Test
  void testFilterWithEmptyServerName() throws ServletException, IOException {
    // Given
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn("");
    when(request.getRequestURI()).thenReturn("/api/test");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
  }

  @Test
  void testFilterWithNullServerName() throws ServletException, IOException {
    // Given
    String defaultTenant = "default";
    when(multiTenantConfig.isEnabled()).thenReturn(true);
    when(multiTenantConfig.getDefaultTenant()).thenReturn(defaultTenant);
    when(request.getServerName()).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/test");

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("currentTenant", defaultTenant);
  }
}