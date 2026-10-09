package com.almis.awe.test.integration.database.postgresql;

import com.almis.awe.test.integration.database.AbstractFlywayMigrationTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Postgresql-Flyway")
@AweDatabaseTest(value = TestDatabase.POSTGRESQL, flyway = true)
@TestPropertySource(locations = {"classpath:postgresql.properties", "classpath:test-flyway.properties"})
class PostgresqlFlywayTest extends AbstractFlywayMigrationTest {

  @Override
  protected String dialect() {
    return "postgresql";
  }
}
