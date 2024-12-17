package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("HSQL-Flyway")
@TestPropertySource(locations = {"classpath:test-flyway.properties"})
class HSQLFlywayTest extends AbstractSpringAppIntegrationTest {

  @Test
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
