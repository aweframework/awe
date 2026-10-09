package com.almis.awe.developer.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the default value of the {@code awe.developer.path} property.
 */
class DeveloperConfigPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TestConfig.class);

  @Test
  void defaultPathIsTheAweDeveloperFolderInTheUserHome() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      DeveloperConfigProperties properties = context.getBean(DeveloperConfigProperties.class);
      assertThat(properties.getPath()).isEqualTo(System.getProperty("user.home") + "/awe-developer");
    });
  }

  @Test
  void defaultPathDoesNotContainBraces() {
    contextRunner.run(context -> {
      DeveloperConfigProperties properties = context.getBean(DeveloperConfigProperties.class);
      assertThat(properties.getPath()).doesNotContain("{", "}");
    });
  }

  @Test
  void configuredPathOverridesTheDefault() {
    contextRunner
      .withPropertyValues("awe.developer.path=/custom/developer")
      .run(context -> {
        DeveloperConfigProperties properties = context.getBean(DeveloperConfigProperties.class);
        assertThat(properties.getPath()).isEqualTo("/custom/developer");
      });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DeveloperConfigProperties.class)
  static class TestConfig {
  }
}
