package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Mysql")
@TestPropertySource("classpath:mysql.properties")
class QueryMySQLTest extends QueryTest {
}
