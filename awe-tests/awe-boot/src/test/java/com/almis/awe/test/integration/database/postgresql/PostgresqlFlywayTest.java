package com.almis.awe.test.integration.database.postgresql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("Postgresql-Flyway")
@TestPropertySource(locations = {"classpath:postgresql.properties", "classpath:test-flyway.properties"})
class PostgresqlFlywayTest extends AbstractSpringAppIntegrationTest {

  @Test
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
