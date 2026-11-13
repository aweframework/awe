package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.QueryTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Mysql")
@AweDatabaseTest(TestDatabase.MYSQL)
@TestPropertySource("classpath:mysql.properties")
class QueryMySQLTest extends QueryTest {
}
