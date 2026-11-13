package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.database.MaintainTransactionTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Sqlserver")
@AweDatabaseTest(TestDatabase.SQLSERVER)
@TestPropertySource("classpath:sqlserver.properties")
class MaintainTransactionSQLServerTest extends MaintainTransactionTest {
}
