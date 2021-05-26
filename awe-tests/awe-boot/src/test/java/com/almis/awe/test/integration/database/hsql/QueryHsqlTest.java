package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ExcludeTags;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Sql(scripts = "classpath:sql/performancedata-hsqldb.sql")
@Transactional
@Tag("integration")
@ExcludeTags({"CIDatabase", "NotHSQLDatabase"})
public class QueryHsqlTest extends QueryTest {
}
