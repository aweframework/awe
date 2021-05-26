package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ExcludeTags;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Tag("Oracle")
@ExcludeTags({"NotCIDatabase", "NotOracleDatabase"})
@TestPropertySource("classpath:oracle.properties")
@Sql(scripts = "classpath:sql/performancedata-oracledb.sql")
@Transactional
class QueryOracleTest extends QueryTest {
}
