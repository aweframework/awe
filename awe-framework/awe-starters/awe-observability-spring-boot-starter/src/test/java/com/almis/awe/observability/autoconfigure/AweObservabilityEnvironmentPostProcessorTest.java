package com.almis.awe.observability.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AweObservabilityEnvironmentPostProcessorTest {

  private final AweObservabilityEnvironmentPostProcessor processor = new AweObservabilityEnvironmentPostProcessor();

  @Test
  @DisplayName("It exposes health, info, metrics and prometheus and tags the metrics with the application name")
  void publishesDefaults() {
    MockEnvironment environment = new MockEnvironment().withProperty("spring.application.name", "my-app");

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,info,metrics,prometheus");
    assertThat(environment.getProperty("management.metrics.tags.application")).isEqualTo("my-app");
  }

  @Test
  @DisplayName("The metrics are tagged with a fallback name when the application has none")
  void fallsBackToAGenericApplicationName() {
    MockEnvironment environment = new MockEnvironment();

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.metrics.tags.application")).isEqualTo("awe-application");
  }

  @Test
  @DisplayName("Any value the project sets wins over the defaults")
  void projectValuesWin() {
    MockEnvironment environment = new MockEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("project",
      Map.of("management.endpoints.web.exposure.include", "health")));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
  }

  @Test
  @DisplayName("When the starter is disabled the environment is not changed at all")
  void disabledDoesNotTouchTheEnvironment() {
    MockEnvironment environment = new MockEnvironment().withProperty("awe.observability.enabled", "false");
    int sources = environment.getPropertySources().size();

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getPropertySources()).hasSize(sources);
    assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isNull();
    assertThat(environment.getProperty("management.metrics.tags.application")).isNull();
  }

  @Test
  @DisplayName("The post processor is registered in the factories file")
  void isRegistered() {
    assertThat(SpringFactoriesLoader.forDefaultResourceLocation(getClass().getClassLoader())
      .loadFactoryNames(EnvironmentPostProcessor.class, getClass().getClassLoader()))
      .contains(AweObservabilityEnvironmentPostProcessor.class.getName());
  }
}
