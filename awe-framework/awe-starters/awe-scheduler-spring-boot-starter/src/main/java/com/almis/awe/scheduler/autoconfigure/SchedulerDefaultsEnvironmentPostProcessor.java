package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.scheduler.autoconfigure.config.SchedulerConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Publishes scheduler {@link SchedulerConfigProperties} defaults into the
 * Spring {@code Environment} as a lowest-precedence fallback.
 *
 * <p>Rationale: a {@code @ConfigurationProperties} field default (such as
 * {@link SchedulerConfigProperties#DEFAULT_REPORT_EMAIL_FROM_VALUE}) lives
 * only on the bound bean; it is never published into the
 * {@code Environment}. XML definitions like
 * {@code <variable property="awe.scheduler.report-email-from-value">} are
 * resolved by reading the raw {@code Environment}
 * ({@code environment.getProperty(id)} with no default), so without this
 * bridge the lookup returns {@code null} and the scheduler report email
 * sender ends up empty.</p>
 *
 * <p>The defaults are added with
 * {@link org.springframework.core.env.MutablePropertySources#addLast} so any
 * user-defined value (application.properties/yml, command line, environment
 * variables, ...) always takes precedence over these fallbacks.</p>
 */
public class SchedulerDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /**
   * Name of the property source that holds the scheduler default fallbacks.
   */
  static final String PROPERTY_SOURCE_NAME = "awe-scheduler-defaults";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> defaults = Map.of(
      SchedulerConfigProperties.REPORT_EMAIL_FROM_VALUE_PROPERTY,
      SchedulerConfigProperties.DEFAULT_REPORT_EMAIL_FROM_VALUE);

    environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
  }
}
