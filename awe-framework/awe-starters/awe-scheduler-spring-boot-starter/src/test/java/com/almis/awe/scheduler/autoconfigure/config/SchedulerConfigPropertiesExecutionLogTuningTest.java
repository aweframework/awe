package com.almis.awe.scheduler.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the five execution-log tuning properties (design section 8): window size, per-line
 * character cap, async queue capacity, flush batch size, and flush interval. Defaults reproduce
 * design's documented defaults; each has an enforced lower bound except the flush interval.
 */
class SchedulerConfigPropertiesExecutionLogTuningTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TestConfig.class);

  @Test
  void defaultsMatchDesignDocumentedValues() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
      assertThat(properties.getExecutionLogMaxLines()).isEqualTo(1000);
      assertThat(properties.getExecutionLogMaxLineLength()).isEqualTo(1000);
      assertThat(properties.getExecutionLogQueueCapacity()).isEqualTo(10000);
      assertThat(properties.getExecutionLogBatchSize()).isEqualTo(200);
      assertThat(properties.getExecutionLogFlushInterval()).isEqualTo(Duration.ofSeconds(2));
    });
  }

  @Test
  void allFivePropertiesBindExplicitValues() {
    contextRunner
      .withPropertyValues(
        "awe.scheduler.execution-log-max-lines=500",
        "awe.scheduler.execution-log-max-line-length=200",
        "awe.scheduler.execution-log-queue-capacity=2000",
        "awe.scheduler.execution-log-batch-size=50",
        "awe.scheduler.execution-log-flush-interval=5s")
      .run(context -> {
        assertThat(context).hasNotFailed();
        SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
        assertThat(properties.getExecutionLogMaxLines()).isEqualTo(500);
        assertThat(properties.getExecutionLogMaxLineLength()).isEqualTo(200);
        assertThat(properties.getExecutionLogQueueCapacity()).isEqualTo(2000);
        assertThat(properties.getExecutionLogBatchSize()).isEqualTo(50);
        assertThat(properties.getExecutionLogFlushInterval()).isEqualTo(Duration.ofSeconds(5));
      });
  }

  @Test
  void maxLinesBelowTenFailsClosed() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-max-lines=9")
      .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void maxLineLengthBelowEightyFailsClosed() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-max-line-length=79")
      .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void queueCapacityBelowOneHundredFailsClosed() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-queue-capacity=99")
      .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void batchSizeBelowOneFailsClosed() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-batch-size=0")
      .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void boundaryValuesAreAccepted() {
    contextRunner
      .withPropertyValues(
        "awe.scheduler.execution-log-max-lines=10",
        "awe.scheduler.execution-log-max-line-length=80",
        "awe.scheduler.execution-log-queue-capacity=100",
        "awe.scheduler.execution-log-batch-size=1")
      .run(context -> assertThat(context).hasNotFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SchedulerConfigProperties.class)
  static class TestConfig {
  }
}
