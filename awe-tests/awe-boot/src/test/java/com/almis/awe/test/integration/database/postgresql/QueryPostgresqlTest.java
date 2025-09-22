package com.almis.awe.test.integration.database.postgresql;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Postgresql")
@TestPropertySource("classpath:postgresql.properties")
class QueryPostgresqlTest extends QueryTest {
}
