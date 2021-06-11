package com.almis.awe.test.integration.spring;

import com.almis.awe.service.DummyService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class for testing spring application context loading.
 * Useful to checking if some bean, property or config has been loaded.
 */
@Tag("integration")
@DisplayName("Spring Context Tests")
class ContextLoadTest extends AbstractSpringAppIntegrationTest {

  // Defined property
  @Value("${awe.database.enabled:false}")
  private String definedProperty;

  @Value("${MinPwd:false}")
  private String databaseProperty;

  @Autowired
  private DummyService dummyService;

  /**
   * Test an autowired defined property (with @Value)
   */
  @Test
  void testAutowiredDefinedProperty() {
    assertEquals("true", definedProperty);
  }

  /**
   * Test an autowired database property (with @Value)
   */
  @Test
  void testAutowiredDatabaseProperty() {
    assertEquals("3", databaseProperty);
  }

  /**
   * Test an autowired service bean
   */
  @Test
  void testAutowiredService() {
    assertNotNull(dummyService);
  }

  /**
   * Test a defined property (with getProperty)
   */
  @Test
  void testDefinedProperty() {
    assertEquals("true", dummyService.getProperty("awe.database.enabled"));
  }

  /**
   * Test a database property (with getProperty)
   */
  @Test
  void testDatabaseProperty() {
    assertEquals("3", dummyService.getProperty("MinPwd"));
  }
}
