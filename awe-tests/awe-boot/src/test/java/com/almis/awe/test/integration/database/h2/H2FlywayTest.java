package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("H2-Flyway")
@TestPropertySource(locations = {"classpath:h2.properties", "classpath:test-flyway.properties"})
class H2FlywayTest extends AbstractSpringAppIntegrationTest {

  @Test
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
