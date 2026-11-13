package com.almis.awe.scheduler.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the default value of the {@code awe.scheduler.execution-log-path} property.
 */
class SchedulerConfigPropertiesExecutionLogPathTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TestConfig.class);

  @Test
  void defaultPathIsTheSchedulerFolderInTheTemporaryDirectory() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
      assertThat(properties.getExecutionLogPath()).isEqualTo(System.getProperty("java.io.tmpdir") + "/scheduler");
    });
  }

  @Test
  void defaultPathUsesTheLoggingFilePathWhenConfigured() {
    contextRunner
      .withPropertyValues("logging.file.path=/var/log/awe")
      .run(context -> {
        SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
        assertThat(properties.getExecutionLogPath()).isEqualTo("/var/log/awe/scheduler");
      });
  }

  @Test
  void defaultPathDoesNotContainBraces() {
    contextRunner.run(context -> {
      SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
      assertThat(properties.getExecutionLogPath()).doesNotContain("{", "}");
    });
  }

  @Test
  void configuredPathOverridesTheDefault() {
    contextRunner
      .withPropertyValues("awe.scheduler.execution-log-path=/custom/scheduler")
      .run(context -> {
        SchedulerConfigProperties properties = context.getBean(SchedulerConfigProperties.class);
        assertThat(properties.getExecutionLogPath()).isEqualTo("/custom/scheduler");
      });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SchedulerConfigProperties.class)
  static class TestConfig {
  }
}
