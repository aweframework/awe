package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.database.AbstractUserThemeSettingsTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * User density and mode on a database migrated by Flyway (the SQL scripts of the test application have no AweUsrSet).
 * It uses its own in-memory database, so Flyway always migrates it from scratch.
 */
@Tag("HSQL-Flyway")
@TestPropertySource(locations = {"classpath:test-flyway.properties"},
  properties = "spring.datasource.url=jdbc:hsqldb:mem:awe-user-theme-settings")
class UserThemeSettingsHSQLFlywayTest extends AbstractUserThemeSettingsTest {
}
