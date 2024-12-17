package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("Oracle-Flyway")
@TestPropertySource(locations = {"classpath:oracle-flyway.properties"})
class OracleFlywayTest extends AbstractSpringAppIntegrationTest {

  @Test
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
