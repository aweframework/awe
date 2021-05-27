package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Sql(scripts = "classpath:sql/performancedata-hsqldb.sql")
@Transactional
@TestPropertySource("classpath:mysql.properties")
@Tag("Mysql")
class QueryMySQLTest extends QueryTest {
}
