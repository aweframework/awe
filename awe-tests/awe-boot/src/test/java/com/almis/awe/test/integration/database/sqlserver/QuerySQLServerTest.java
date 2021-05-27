package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Tag("Sqlserver")
@TestPropertySource("classpath:sqlserver.properties")
@Sql(scripts = "classpath:sql/performancedata-sqlserverdb.sql")
@Transactional
class QuerySQLServerTest extends QueryTest {
}
