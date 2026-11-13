package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.database.AbstractThemeMaintainTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Theme CRUD on a database migrated by Flyway. It uses its own in-memory database, so Flyway always migrates it from
 * scratch whatever the state of the file database of the other tests.
 */
@Tag("HSQL-Flyway")
@TestPropertySource(locations = {"classpath:test-flyway.properties"},
  properties = "spring.datasource.url=jdbc:hsqldb:mem:awe-theme-crud")
class ThemeMaintainHSQLFlywayTest extends AbstractThemeMaintainTest {
}
