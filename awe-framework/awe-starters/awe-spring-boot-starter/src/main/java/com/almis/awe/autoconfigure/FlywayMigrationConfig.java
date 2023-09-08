package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweRoutingDataSource;
import com.almis.awe.config.DatabaseConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    log.info("\u001B[32m==========  Migrating default database  ==========\u001B[0m");
    for (String module : databaseConfigProperties.getMigrationModules()) {
      Flyway customFlyway = customizeFlywayConfig(module.toUpperCase(), dataSource);

      // Migrate first connection
      log.info("\t\u001B[32m> > Migrating database of \u001B[35m{} \u001B[32mmodule  ...\u001B[0m", module);
      customFlyway.migrate();
      log.info("\t\u001B[32m> > Current version of \u001B[35m{} \u001B[32mmodule: {}\n\u001B[0m", module, customFlyway.info().current().getVersion());
    }

    if (dataSource instanceof AweRoutingDataSource aweRoutingDataSource) {
      // Load dataSources
      aweRoutingDataSource.loadDataSources();
      // Spread scripts migration
      log.info("\u001B[32m========== Migrating databases defined of [AweDbs] table ... ==========\u001B[0m");
      for (String module : databaseConfigProperties.getMigrationModules()) {
        aweRoutingDataSource.getResolvedDataSources().forEach((key, value) -> {
                  log.info("\t\u001B[32m> > Migrating database \u001B[35m{}\u001B[32m for module \u001B[35m{}\u001B[32m ...\u001B[0m", key, module);
                  Flyway customFlyway = customizeFlywayConfig(module.toUpperCase(), value);
                  customFlyway.migrate();
                  log.info("\t\u001B[32m> > Current version of module \u001B[35m{}\u001B[32m from database \u001B[35m{}: {}\u001B[0m", module, key, customFlyway.info().current().getVersion());
                }
        );
      }
    }
  }

  /**
   * Customize flyway configuration
   *
   * @param module       Name of module to apply migration scripts
   * @param dataSource   Datasource
   * @return flyway object
   */
  private Flyway customizeFlywayConfig(String module, DataSource dataSource) {

    // Custom flyway config fields
    final String scriptPrefix = String.format(flyway.getConfiguration().getSqlMigrationPrefix(), module);
    final String repeatableScriptPrefix = String.format(flyway.getConfiguration().getRepeatableSqlMigrationPrefix(), module);

    // Customize configuration
    FluentConfiguration configuration = new FluentConfiguration()
            .dataSource(dataSource)
            .locations(flyway.getConfiguration().getLocations())
            .baselineOnMigrate(flyway.getConfiguration().isBaselineOnMigrate())
            .baselineVersion(flyway.getConfiguration().getBaselineVersion())
            .baselineDescription(flyway.getConfiguration().getBaselineDescription())
            .cleanDisabled(flyway.getConfiguration().isCleanDisabled())
            .cleanOnValidationError(flyway.getConfiguration().isCleanOnValidationError())
            .connectRetries(flyway.getConfiguration().getConnectRetries())
            .connectRetriesInterval(flyway.getConfiguration().getConnectRetriesInterval())
            .createSchemas(flyway.getConfiguration().isCreateSchemas())
            .defaultSchema(flyway.getConfiguration().getDefaultSchema())
            .encoding(flyway.getConfiguration().getEncoding())
            .failOnMissingLocations(flyway.getConfiguration().isFailOnMissingLocations())
            .initSql(flyway.getConfiguration().getInitSql())
            .group(flyway.getConfiguration().isGroup())
            .sqlMigrationPrefix(scriptPrefix)
            .sqlMigrationSeparator(flyway.getConfiguration().getSqlMigrationSeparator())
            .sqlMigrationSuffixes(flyway.getConfiguration().getSqlMigrationSuffixes())
            .tablespace(flyway.getConfiguration().getTablespace())
            .target(flyway.getConfiguration().getTarget())
            .validateMigrationNaming(flyway.getConfiguration().isValidateMigrationNaming())
            .repeatableSqlMigrationPrefix(repeatableScriptPrefix)
            .installedBy(flyway.getConfiguration().getInstalledBy())
            .outOfOrder(flyway.getConfiguration().isOutOfOrder())
            .placeholderPrefix(flyway.getConfiguration().getPlaceholderPrefix())
            .placeholderReplacement(flyway.getConfiguration().isPlaceholderReplacement())
            .placeholderSeparator(flyway.getConfiguration().getPlaceholderSeparator())
            .placeholderSuffix(flyway.getConfiguration().getPlaceholderSuffix())
            .placeholders(flyway.getConfiguration().getPlaceholders())
            .skipDefaultCallbacks(flyway.getConfiguration().isSkipDefaultCallbacks())
            .skipDefaultResolvers(flyway.getConfiguration().isSkipDefaultResolvers())
            .mixed(flyway.getConfiguration().isMixed())
            .table("flyway_schema_" + module);

    return Flyway.configure()
            .configuration(configuration)
            .load();
  }
}
