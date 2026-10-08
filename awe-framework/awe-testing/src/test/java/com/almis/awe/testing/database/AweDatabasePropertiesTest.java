package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AweDatabasePropertiesTest {

  private static final String[] MODULES = {"AWE", "SCHEDULER", "NOTIFIER", "awe-boot"};

  @Test
  void embeddedH2UsesItsOwnUrlAndTheScriptConvention() {
    Map<String, Object> properties = AweDatabaseProperties.build(TestDatabase.H2, false, MODULES, null);

    assertThat(properties)
      .containsEntry("spring.datasource.url", TestDatabase.H2.getEmbeddedUrl())
      .containsEntry("spring.datasource.username", "sa")
      .containsEntry("spring.datasource.password", "")
      .containsEntry("spring.datasource.driver-class-name", "org.h2.Driver")
      .containsEntry("spring.sql.init.mode", "always")
      .containsEntry("spring.sql.init.schema-locations", "classpath:sql/schema-h2.sql")
      .containsEntry("spring.sql.init.data-locations", "classpath:sql/testdata-h2.sql")
      .containsEntry("spring.sql.init.continue-on-error", "false")
      .doesNotContainKey("spring.flyway.enabled");
  }

  @Test
  void aServerDatabaseTakesTheConnectionOfItsContainer() {
    DatabaseConnection connection = new DatabaseConnection("jdbc:postgresql://localhost:5555/awetestdb", "postgres", "secret");

    Map<String, Object> properties = AweDatabaseProperties.build(TestDatabase.POSTGRESQL, false, MODULES, connection);

    assertThat(properties)
      .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5555/awetestdb")
      .containsEntry("spring.datasource.username", "postgres")
      .containsEntry("spring.datasource.password", "secret")
      .containsEntry("spring.datasource.driver-class-name", "org.postgresql.Driver")
      .containsEntry("spring.sql.init.schema-locations", "classpath:sql/schema-postgresql.sql")
      .containsEntry("spring.sql.init.continue-on-error", "true");
  }

  @Test
  void theFlywayVariantMigratesTheModulesInsteadOfRunningTheScripts() {
    DatabaseConnection connection = new DatabaseConnection("jdbc:mysql://localhost:3306/awetestdb", "root", "secret");

    Map<String, Object> properties = AweDatabaseProperties.build(TestDatabase.MYSQL, true, MODULES, connection);

    assertThat(properties)
      .containsEntry("spring.sql.init.mode", "never")
      .containsEntry("spring.flyway.enabled", "true")
      .containsEntry("awe.database.migration-modules", "AWE,SCHEDULER,NOTIFIER,awe-boot")
      .doesNotContainKeys("spring.sql.init.schema-locations", "spring.sql.init.data-locations");
  }

  @Test
  void externalModeLeavesTheConnectionToTheEnvironment() {
    Map<String, Object> properties = AweDatabaseProperties.build(TestDatabase.SQLSERVER, false, MODULES, null);

    assertThat(properties)
      .doesNotContainKeys("spring.datasource.url", "spring.datasource.username", "spring.datasource.password")
      .containsEntry("spring.datasource.driver-class-name", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
      .containsEntry("spring.sql.init.schema-locations", "classpath:sql/schema-sqlserverdb.sql");
  }

  @Test
  void theMigrationModulesAreConfigurable() {
    Map<String, Object> properties = AweDatabaseProperties.build(TestDatabase.H2, true, new String[]{"AWE"}, null);

    assertThat(properties).containsEntry("awe.database.migration-modules", "AWE");
  }
}
