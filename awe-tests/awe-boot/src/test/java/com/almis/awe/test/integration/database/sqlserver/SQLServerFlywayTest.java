package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.database.AbstractFlywayMigrationTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Sqlserver-Flyway")
@AweDatabaseTest(value = TestDatabase.SQLSERVER, flyway = true)
@TestPropertySource(locations = {"classpath:sqlserver.properties", "classpath:test-flyway.properties"})
class SQLServerFlywayTest extends AbstractFlywayMigrationTest {

  @Override
  protected String dialect() {
    return "sqlserver";
  }
}
