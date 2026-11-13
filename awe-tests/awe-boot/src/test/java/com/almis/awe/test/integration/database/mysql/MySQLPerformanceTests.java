package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.almis.awe.test.integration.LoadTestWarmUpExtension;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({LoadTestWarmUpExtension.class, ParallelLoadExtension.class})
// A load test (zerocode, 100 threads): a measurement, not a rule, so no build selects its tag and CI does not run it. To
// measure, run the HSQL one with -Dtest.tags=performance; a database profile fixes its own tag (<groups> in the pom)
@Tag("performance")
@AweDatabaseTest(TestDatabase.MYSQL)
@TestPropertySource("classpath:mysql.properties")
class MySQLPerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryMySQLTest.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
