package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.AbstractFlywayMigrationTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Mysql-Flyway")
@AweDatabaseTest(value = TestDatabase.MYSQL, flyway = true)
@TestPropertySource(locations = {"classpath:mysql.properties", "classpath:test-flyway.properties"})
class MySQLFlywayTest extends AbstractFlywayMigrationTest {

  @Override
  protected String dialect() {
    return "mysql";
  }
}
