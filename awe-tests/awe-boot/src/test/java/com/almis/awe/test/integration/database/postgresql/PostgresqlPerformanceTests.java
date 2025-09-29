package com.almis.awe.test.integration.database.postgresql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.jupiter.extension.ParallelLoadExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ParallelLoadExtension.class})
@Tag("Postgresql")
@TestPropertySource("classpath:postgresql.properties")
class PostgresqlPerformanceTests extends AbstractSpringAppIntegrationTest {

  @Test
  @LoadWith("performance.properties")
  @TestMapping(testClass = QueryPostgresqlTest.class, testMethod = "testBigDataEvalPerformance")
  void testLoad() {
    // This space remains empty
    assertTrue(true);
  }
}
