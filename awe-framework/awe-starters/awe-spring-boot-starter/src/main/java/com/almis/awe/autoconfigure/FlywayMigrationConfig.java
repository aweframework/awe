package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweRoutingDataSource;
import com.almis.awe.config.DatabaseConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * Flyway configuration
 *
 * @author pvidal
 * Created by pvidal on 04/12/2019.
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class FlywayMigrationConfig {

  // Autowired components
  private final DataSource dataSource;
  private final Flyway flyway;
  private final DatabaseConfigProperties databaseConfigProperties;

  /**
   * Constructor
   *
   * @param flyway                   Flyway
   * @param dataSource               Awe routing datasource
   * @param databaseConfigProperties Database config properties
   */
  @Autowired
  public FlywayMigrationConfig(Flyway flyway, DataSource dataSource, DatabaseConfigProperties databaseConfigProperties) {
    this.flyway = flyway;
    this.dataSource = dataSource;
    this.databaseConfigProperties = databaseConfigProperties;
  }

  /**
   * Flyway migration strategy
   *
   * @return FlywayMigrationStrategy
   */
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flywayInstance -> {
      // Do nothing
    };
  }

  @PostConstruct
  public void initFlyway() {

    log.info("=======  Migrating default database  =======");
    for (String module : databaseConfigProperties.getMigrationModules()) {
      Flyway customFlyway = customizeFlywayConfig(module, dataSource);

      // Migrate first connection
      log.info("=======  Migrating database of {} module  =======", module);
      customFlyway.migrate();
      log.info("======= Current version of {} module: {}", module, customFlyway.info().current().getVersion());
    }

    if (dataSource instanceof AweRoutingDataSource) {
      // Load dataSources
      ((AweRoutingDataSource) dataSource).loadDataSources();
      // Spread scripts migration
      log.info("========== Migrating databases of [AweDbs] table defined in default database ... ==========");
      for (String module : databaseConfigProperties.getMigrationModules()) {
        ((AweRoutingDataSource) dataSource).getResolvedDataSources().forEach((key, value) -> {
            log.info("========== Migrating database {} for module {} ... ==========", key, module);
            Flyway customFlyway = customizeFlywayConfig(module, value);
            customFlyway.migrate();
            log.info("======= Current version of module {} from database {}: {}", module, key, customFlyway.info().current().getVersion());
          }
        );
      }
    }
  }

  /**
   * Customize configuration
   *
   * @param module Name of module to apply migration scripts
   */
  private Flyway customizeFlywayConfig(String module, DataSource dataSource) {

    final String scriptPrefix = String.format(flyway.getConfiguration().getSqlMigrationPrefix(), module);
    final String repeatableScriptPrefix = String.format(flyway.getConfiguration().getRepeatableSqlMigrationPrefix(), module);
    FluentConfiguration configuration = new FluentConfiguration()
      .baselineOnMigrate(flyway.getConfiguration().isBaselineOnMigrate())
      .baselineVersion(flyway.getConfiguration().getBaselineVersion())
      .sqlMigrationPrefix(scriptPrefix)
      .repeatableSqlMigrationPrefix(repeatableScriptPrefix)
      .table("flyway_schema_" + module)
      .locations(flyway.getConfiguration().getLocations())
      .dataSource(dataSource);
    return Flyway.configure()
      .configuration(configuration)
      .load();
  }
}
