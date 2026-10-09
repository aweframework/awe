package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.database.AbstractFlywayMigrationTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Oracle-Flyway")
@AweDatabaseTest(value = TestDatabase.ORACLE, flyway = true)
@TestPropertySource(locations = {"classpath:oracle-flyway.properties"})
class OracleFlywayTest extends AbstractFlywayMigrationTest {

  @Override
  protected String dialect() {
    return "oracle";
  }
}
