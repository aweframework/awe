package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.database.QueryTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Oracle")
@AweDatabaseTest(TestDatabase.ORACLE)
@TestPropertySource("classpath:oracle.properties")
class QueryOracleTest extends QueryTest {
}
