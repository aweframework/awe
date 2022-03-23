package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Database configuration properties
 */
@ConfigurationProperties(prefix = "awe.database")
@Validated
@Data
public class DatabaseConfigProperties {
  /**
   * Enable awe database loading beans.
   * Default value true
   */
  private boolean enabled = true;

  /**
   * Database criterion name. Configure the database parameter to retrieve the value from screen context.
   * Default value _database_
   */
  private String parameterName = "_database_";

  /**
   * Limit log size when a query is printed (0 to disable)
   * Default value 0
   */
  private int limitLogSize = 0;

  /**
   * Enable multi database. Enable a custom datasource to allow connections to multiple database.
   * Default value false
   */
  private boolean multidatabaseEnable = false;

  /**
   * List of modules separated by comma to be migrated by flyway.
   * Default value AWE module
   */
  private String[] migrationModules = {"AWE"};

  /**
   * Script migration prefix pattern name.
   * Default value [Module]_V (Ex: AWE_V)
   */
  private String migrationPrefix = "%s_V";

  /**
   * Repeatable script migration prefix pattern name.
   * Default value [Module]_R (Ex: AWE_R)
   */
  private String migrationRepeatablePrefix = "%s_R";

  /**
   * Enable audit operations in AWE database engine.
   * Default value true
   */
  private boolean auditEnable = true;

  /**
   * Name of Date column in audit tables.
   * Default value HISdat
   */
  private String auditDate = "HISdat";

  /**
   * Name of User column in audit tables.
   * Default value HISope
   */
  private String auditUser = "HISope";

  /**
   * Name of Action column in audit tables.
   * Default value HISact
   */
  private String auditAction = "HISact";

  /**
   * Audit lag in milliseconds. Used to built a timestamp in audit process.
   * Default value 100ms
   */
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration auditLag = Duration.ofMillis(100);

  /**
   * Chunk size for batch operations.
   * Default value 100
   */
  private int batchMax = 100;
}
