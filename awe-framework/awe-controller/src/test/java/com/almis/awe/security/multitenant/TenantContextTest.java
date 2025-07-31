package com.almis.awe.security.multitenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TenantContextTest {

  @AfterEach
  void tearDown() {
    // Clean up after each test to avoid interference
    TenantContext.clear();
  }

  @Test
  void testSetAndGetCurrentTenant() {
    // Given
    String tenant = "test-tenant";

    // When
    TenantContext.setCurrentTenant(tenant);

    // Then
    assertEquals(tenant, TenantContext.getCurrentTenant());
  }

  @Test
  void testGetCurrentTenantWhenNotSet() {
    // When
    String result = TenantContext.getCurrentTenant();

    // Then
    assertNull(result);
  }

  @Test
  void testClearCurrentTenant() {
    // Given
    String tenant = "test-tenant";
    TenantContext.setCurrentTenant(tenant);

    // When
    TenantContext.clear();

    // Then
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testSetCurrentTenantWithNull() {
    // Given
    String tenant = null;

    // When
    TenantContext.setCurrentTenant(tenant);

    // Then
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testSetCurrentTenantWithEmptyString() {
    // Given
    String tenant = "";

    // When
    TenantContext.setCurrentTenant(tenant);

    // Then
    assertEquals("", TenantContext.getCurrentTenant());
  }

  @Test
  void testThreadLocalIsolation() throws ExecutionException, InterruptedException {
    // Given
    String mainThreadTenant = "main-tenant";
    String otherThreadTenant = "other-tenant";

    // Set tenant in main thread
    TenantContext.setCurrentTenant(mainThreadTenant);

    // When - Execute in another thread
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
      // Set different tenant in other thread
      TenantContext.setCurrentTenant(otherThreadTenant);
      return TenantContext.getCurrentTenant();
    });

    String otherThreadResult = future.get();

    // Then
    assertEquals(mainThreadTenant, TenantContext.getCurrentTenant()); // Main thread should be unchanged
    assertEquals(otherThreadTenant, otherThreadResult); // Other thread should have its own value
  }

  @Test
  void testMultipleSetOperations() {
    // Given
    String tenant1 = "tenant1";
    String tenant2 = "tenant2";
    String tenant3 = "tenant3";

    // When
    TenantContext.setCurrentTenant(tenant1);
    assertEquals(tenant1, TenantContext.getCurrentTenant());

    TenantContext.setCurrentTenant(tenant2);
    assertEquals(tenant2, TenantContext.getCurrentTenant());

    TenantContext.setCurrentTenant(tenant3);
    assertEquals(tenant3, TenantContext.getCurrentTenant());

    // Then - Last set value should be current
    assertEquals(tenant3, TenantContext.getCurrentTenant());
  }

  @Test
  void testClearWhenNotSet() {
    // When - Clear without setting anything
    TenantContext.clear();

    // Then - Should not throw exception and should remain null
    assertNull(TenantContext.getCurrentTenant());
  }

  @Test
  void testClearAfterMultipleOperations() {
    // Given
    TenantContext.setCurrentTenant("tenant1");
    TenantContext.setCurrentTenant("tenant2");
    assertEquals("tenant2", TenantContext.getCurrentTenant());

    // When
    TenantContext.clear();

    // Then
    assertNull(TenantContext.getCurrentTenant());
  }
}