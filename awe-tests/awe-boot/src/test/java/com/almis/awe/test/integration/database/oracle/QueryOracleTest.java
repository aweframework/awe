package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Oracle")
@TestPropertySource("classpath:oracle.properties")
class QueryOracleTest extends QueryTest {
}
