package com.almis.awe.testing.database;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the Spring properties AWE reads to connect to a test database.
 *
 * <p>AWE configures its data source from {@code DataSourceProperties}, so the connection has to be written as
 * {@code spring.datasource.*} properties; a Testcontainers {@code @ServiceConnection} does not reach it.</p>
 */
public final class AweDatabaseProperties {

  /**
   * Flyway modules migrated by default in the Flyway variant
   */
  public static final String[] DEFAULT_MIGRATION_MODULES = {"AWE", "SCHEDULER", "NOTIFIER", "awe-boot"};

  private AweDatabaseProperties() {
  }

  /**
   * Build the properties of a database
   *
   * @param database         Database
   * @param flyway           True to migrate with Flyway, false to run the schema and data scripts
   * @param migrationModules AWE modules to migrate (Flyway variant)
   * @param connection       Connection of the container; {@code null} to leave the connection (url, user and password) to the
   *                         environment, which is what external mode does for the server databases
   * @return Properties, in insertion order
   */
  public static Map<String, Object> build(TestDatabase database, boolean flyway, String[] migrationModules,
                                          DatabaseConnection connection) {
    Map<String, Object> properties = new LinkedHashMap<>();
    if (connection != null) {
      properties.put("spring.datasource.url", connection.jdbcUrl());
      properties.put("spring.datasource.username", connection.username());
      properties.put("spring.datasource.password", connection.password());
    } else if (!database.needsContainer()) {
      properties.put("spring.datasource.url", database.getEmbeddedUrl());
      properties.put("spring.datasource.username", "sa");
      properties.put("spring.datasource.password", "");
    }
    properties.put("spring.datasource.driver-class-name", database.getDriverClassName());

    if (flyway) {
      properties.put("spring.sql.init.mode", "never");
      properties.put("spring.flyway.enabled", "true");
      properties.put("awe.database.migration-modules", String.join(",", migrationModules));
    } else {
      properties.put("spring.sql.init.mode", "always");
      properties.put("spring.sql.init.schema-locations", database.getSchemaScript());
      properties.put("spring.sql.init.data-locations", database.getDataScript());
      properties.put("spring.sql.init.continue-on-error", String.valueOf(database.isContinueOnError()));
    }
    return properties;
  }

  /**
   * Build the properties of a database, starting its container when it needs one and external mode is off
   *
   * @param database         Database
   * @param flyway           True to migrate with Flyway
   * @param migrationModules AWE modules to migrate (Flyway variant)
   * @return Properties
   */
  public static Map<String, Object> resolve(TestDatabase database, boolean flyway, String[] migrationModules) {
    return build(database, flyway, migrationModules, DatabaseContainers.connectionFor(database).orElse(null));
  }

  /**
   * Register the properties of a database from a {@code @DynamicPropertySource} method. The container, if any, starts
   * when this is called.
   *
   * @param registry Registry
   * @param database Database
   * @param flyway   True to migrate with Flyway
   */
  public static void register(DynamicPropertyRegistry registry, TestDatabase database, boolean flyway) {
    resolve(database, flyway, DEFAULT_MIGRATION_MODULES).forEach((name, value) -> registry.add(name, () -> value));
  }
}
