package com.almis.awe.scheduler.autoconfigure.config;

import com.almis.awe.scheduler.enums.ExecutionLogStoreType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code awe.scheduler.execution-log-store} selection property.
 * <p>
 * Typed as an enum (not a raw {@code String}) so an unrecognized value fails the application
 * context at startup instead of silently falling back or starting half-configured.
 */
class SchedulerConfigPropertiesExecutionLogStoreTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TestConfig.class);

  @Test
  void executionLogStoreDefaultsToFileWhenPropertyIsAbsent() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
      assertThat(properties.getExecutionLogStore()).isEqualTo(ExecutionLogStoreType.FILE);
    });
  }

  @Test
  void executionLogStoreBindsFileCaseInsensitively() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=FILE")
      .run(context -> {
        assertThat(context).hasNotFailed();
        SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
        assertThat(properties.getExecutionLogStore()).isEqualTo(ExecutionLogStoreType.FILE);
      });
  }

  @Test
  void executionLogStoreBindsDatabaseCaseInsensitively() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=database")
      .run(context -> {
        assertThat(context).hasNotFailed();
        SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
        assertThat(properties.getExecutionLogStore()).isEqualTo(ExecutionLogStoreType.DATABASE);
      });
  }

  @Test
  void executionLogStoreFailsClosedOnAnUnknownValue() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-store=bogus")
      .run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SchedulerConfigProperties.class)
  static class TestConfig {
  }
}
