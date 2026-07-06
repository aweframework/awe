package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.scheduler.autoconfigure.config.SchedulerConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link SchedulerDefaultsEnvironmentPostProcessor}.
 *
 * <p>The post-processor bridges the {@link SchedulerConfigProperties}
 * default into the Spring {@code Environment} so XML
 * {@code <variable property="awe.scheduler.report-email-from-value">}
 * resolution (which reads the raw Environment, not the bound bean) has a
 * non-null value, while still yielding to any user-defined value.</p>
 */
class SchedulerDefaultsEnvironmentPostProcessorTest {

  private final SchedulerDefaultsEnvironmentPostProcessor postProcessor =
    new SchedulerDefaultsEnvironmentPostProcessor();

  /**
   * When nothing else defines the property, the post-processor must expose
   * the built-in default so the Environment lookup no longer returns null.
   */
  @Test
  void injectsDefaultReportEmailFromValueWhenUndefined() {
    StandardEnvironment environment = new StandardEnvironment();

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertEquals(
      SchedulerConfigProperties.DEFAULT_REPORT_EMAIL_FROM_VALUE,
      environment.getProperty(SchedulerConfigProperties.REPORT_EMAIL_FROM_VALUE_PROPERTY));
  }

  /**
   * A higher-precedence property source must win: the injected default is a
   * fallback only, so a user-defined value takes precedence.
   */
  @Test
  void userDefinedValueWinsOverDefault() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource(
      "test",
      Map.of(SchedulerConfigProperties.REPORT_EMAIL_FROM_VALUE_PROPERTY, "custom@x.com")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertEquals(
      "custom@x.com",
      environment.getProperty(SchedulerConfigProperties.REPORT_EMAIL_FROM_VALUE_PROPERTY));
  }
}
