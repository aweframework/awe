package com.almis.awe.autoconfigure;

import com.querydsl.sql.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the QueryDSL configurations of each database type
 */
class SQLConfigTest {

  @Test
  void eachDatabaseTypeGivesANewConfigurationThatReusesItsTemplates() {
    SQLConfig config = new SQLConfig();
    List<Supplier<Configuration>> databaseTypes = List.of(config::oracleDatabaseConfiguration,
      config::sqlserverDatabaseConfiguration, config::postgresqlDatabaseConfiguration,
      config::sybaseDatabaseConfiguration, config::hsqldbDatabaseConfiguration, config::h2DatabaseConfiguration,
      config::mysqlDatabaseConfiguration);

    for (Supplier<Configuration> databaseType : databaseTypes) {
      Configuration first = databaseType.get();
      Configuration second = databaseType.get();

      assertThat(second).isNotSameAs(first);
      assertThat(second.getTemplates()).isSameAs(first.getTemplates());
    }
  }
}
