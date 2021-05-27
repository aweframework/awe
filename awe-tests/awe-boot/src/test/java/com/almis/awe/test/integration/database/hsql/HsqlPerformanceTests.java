package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.almis.awe.test.integration.database.QueryTest;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith({ParallelLoadExtension.class})
@DisplayName("HSQL Performance Tests")
@Tag("integration")
@Sql(scripts = "classpath:sql/performancedata-hsqldb.sql")
class HsqlPerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryTest.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    assertTrue(true);
  }

}
