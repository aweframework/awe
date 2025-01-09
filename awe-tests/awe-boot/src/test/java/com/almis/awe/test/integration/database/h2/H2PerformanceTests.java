package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;
@ExtendWith({ParallelLoadExtension.class})
@DisplayName("H2 Performance Tests")
@Tag("H2")
@TestPropertySource("classpath:h2.properties")
class H2PerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryH2Test.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    assertTrue(true);
  }

}
