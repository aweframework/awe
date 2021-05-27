package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith({ParallelLoadExtension.class})
@Tag("Oracle")
@TestPropertySource("classpath:oracle.properties")
@Sql(scripts = "classpath:sql/performancedata-oracledb.sql")
class OraclePerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryOracleTest.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
