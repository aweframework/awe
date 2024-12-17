package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("Sqlserver-Flyway")
@TestPropertySource(locations = {"classpath:sqlserver.properties", "classpath:test-flyway.properties"})
class SQLServerFlywayTest extends AbstractSpringAppIntegrationTest {

  @Test
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
