package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.database.AbstractThemeMaintainTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Theme CRUD on the schema and data of the test application's H2 SQL scripts (no Flyway)
 */
@Tag("H2")
@TestPropertySource("classpath:h2.properties")
class ThemeMaintainH2Test extends AbstractThemeMaintainTest {
}
