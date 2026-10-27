package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.database.AbstractUserTargetsSessionTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Favourites and avatar targets of the user of the session on a database migrated by Flyway. It uses its own in-memory
 * database, so Flyway always migrates it from scratch.
 */
@Tag("HSQL-Flyway")
@TestPropertySource(locations = {"classpath:test-flyway.properties"},
  properties = "spring.datasource.url=jdbc:hsqldb:mem:awe-user-targets-session")
class UserTargetsSessionHSQLFlywayTest extends AbstractUserTargetsSessionTest {
}
