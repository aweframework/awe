package com.almis.awe.test.integration.database.sqlserver;

import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SpringExtension.class, ParallelLoadExtension.class})
@Tag("Sqlserver")
@TestPropertySource("classpath:sqlserver.properties")
@Sql(scripts = "classpath:sql/performancedata-sqlserverdb.sql")
class SQLServerPerformanceTests {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QuerySQLServerTest.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
