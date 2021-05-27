package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Sqlserver")
@TestPropertySource("classpath:sqlserver.properties")
class QuerySQLServerTest extends QueryTest {
}
