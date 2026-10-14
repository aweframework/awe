package com.almis.awe.observability.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Publishes the defaults that make the starter work with no further configuration: the actuator endpoints a
 * monitoring stack needs and a common {@code application} tag on every metric.
 *
 * <p>Nothing is added when {@code awe.observability.enabled=false}. The defaults are added with the lowest precedence, so any value of the project (properties files,
 * environment variables, command line) wins over them.</p>
 */
public class AweObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /**
   * Name of the property source that holds the defaults.
   */
  static final String PROPERTY_SOURCE_NAME = "awe-observability-defaults";

  private static final String FALLBACK_APPLICATION_NAME = "awe-application";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    if (!environment.getProperty("awe.observability.enabled", Boolean.class, true)) {
      // Switched off: the starter must not change the environment at all
      return;
    }
    String applicationName = environment.getProperty("spring.application.name", FALLBACK_APPLICATION_NAME);
    Map<String, Object> defaults = Map.of(
      "management.endpoints.web.exposure.include", "health,info,metrics,prometheus",
      "management.metrics.tags.application", applicationName);

    environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
  }
}
