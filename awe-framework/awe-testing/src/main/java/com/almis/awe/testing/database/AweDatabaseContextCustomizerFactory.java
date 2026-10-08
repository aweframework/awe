package com.almis.awe.testing.database;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Applies {@link AweDatabaseTest} to the Spring test context. Registered in {@code META-INF/spring.factories}.
 */
public class AweDatabaseContextCustomizerFactory implements ContextCustomizerFactory {

  static final String PROPERTY_SOURCE_NAME = "aweDatabaseTest";

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
    AweDatabaseTest annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, AweDatabaseTest.class);
    if (annotation == null) {
      return null;
    }
    return new AweDatabaseContextCustomizer(annotation.value(), annotation.flyway(), annotation.migrationModules());
  }

  /**
   * Writes the database properties in the environment of the context
   */
  static class AweDatabaseContextCustomizer implements ContextCustomizer {

    private final TestDatabase database;
    private final boolean flyway;
    private final String[] migrationModules;

    AweDatabaseContextCustomizer(TestDatabase database, boolean flyway, String[] migrationModules) {
      this.database = database;
      this.flyway = flyway;
      this.migrationModules = migrationModules.clone();
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      context.getEnvironment().getPropertySources().addFirst(
        new MapPropertySource(PROPERTY_SOURCE_NAME, AweDatabaseProperties.resolve(database, flyway, migrationModules)));
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof AweDatabaseContextCustomizer that
        && database == that.database && flyway == that.flyway && Arrays.equals(migrationModules, that.migrationModules);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * database.hashCode() + Boolean.hashCode(flyway)) + Arrays.hashCode(migrationModules);
    }
  }
}
