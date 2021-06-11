package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:mysql.properties")
@Tag("Mysql")
class QueryMySQLTest extends QueryTest {
}
