package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.almis.awe.test.integration.LoadTestWarmUpExtension;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith({LoadTestWarmUpExtension.class, ParallelLoadExtension.class})
@DisplayName("H2 Performance Tests")
// A load test (zerocode, 100 threads): a measurement, not a rule, so no build selects its tag and CI does not run it. To
// measure, run the HSQL one with -Dtest.tags=performance; a database profile fixes its own tag (<groups> in the pom)
@Tag("performance")
@TestPropertySource("classpath:h2.properties")
class H2PerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryH2Test.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    assertTrue(true);
  }

}
