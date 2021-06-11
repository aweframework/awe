package com.almis.awe.test.performance;

import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.Scenario;
import org.jsmart.zerocode.core.domain.TargetEnv;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@LoadWith("performance_load.properties")
@TargetEnv("performance_connection.properties")
@ExtendWith({ParallelLoadExtension.class})
class PerformanceTest {

  @Test
  @Scenario("performance/test-eval-big-performance.yml")
  void testEvalBigPerformance() {
    assertTrue(true);
  }
}
