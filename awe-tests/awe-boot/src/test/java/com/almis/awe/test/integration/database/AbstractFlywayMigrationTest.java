package com.almis.awe.test.integration.database;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Checks that the Flyway migrations of a dialect really ran against the test database.
 *
 * <p>For every module Flyway is configured to migrate ({@code awe.database.migration-modules}) the versioned scripts shipped
 * for the dialect ({@code db/migration/<dialect>/<MODULE>_V<version>__<description>.sql}) must be recorded as successfully
 * applied in the module's history table ({@code flyway_schema_<MODULE>}), and the schema they create must be usable.
 * The modules in {@link #requiredModules()} must be configured and must ship scripts for the dialect: a misspelled module,
 * a missing folder or a database that was not migrated fails these tests instead of passing silently.</p>
 *
 * <p>The other configured modules are the application's own ({@code awe-boot}) and are optional on purpose: the test
 * application ships scripts for some dialects only. When one ships none for the dialect its test is reported as skipped,
 * never as a pass.</p>
 */
@DisplayName("Flyway migrations")
public abstract class AbstractFlywayMigrationTest extends AbstractSpringAppIntegrationTest {

  /**
   * Flyway version of a script: {@code <MODULE>_V<version>__<description>.sql}, with underscores standing for dots
   */
  private static final String SCRIPT_NAME_PATTERN = "^%s_V(\\d+(?:[._]\\d+)*)__.+\\.sql$";

  /**
   * Flyway module of the test application's own scripts
   */
  private static final String APPLICATION_MODULE = "awe-boot";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private DatabaseConfigProperties databaseConfigProperties;

  /**
   * Name of the migration folder of the dialect under {@code db/migration}
   *
   * @return Dialect folder (for example {@code postgresql})
   */
  protected abstract String dialect();

  /**
   * Modules that must be migrated and must ship scripts for the dialect. The other configured modules (the application's own)
   * are optional: they are checked when they ship scripts and skipped otherwise.
   *
   * @return Module names
   */
  protected Set<String> requiredModules() {
    return Set.of("AWE", "SCHEDULER", "NOTIFIER");
  }

  @Test
  @DisplayName("Flyway is configured to migrate every required module")
  void testRequiredModulesAreConfigured() {
    Set<String> missing = new TreeSet<>(requiredModules());
    missing.removeAll(Arrays.asList(configuredModules()));

    assertTrue(missing.isEmpty(), "Modules " + missing + " are not in awe.database.migration-modules "
      + Arrays.toString(configuredModules()));
  }

  @TestFactory
  @DisplayName("Every shipped migration is recorded as successfully applied")
  Stream<DynamicTest> testShippedMigrationsAreApplied() {
    Set<String> modules = new LinkedHashSet<>(Arrays.asList(configuredModules()));
    modules.addAll(requiredModules());

    return modules.stream().map(module -> dynamicTest(module + " module has every shipped migration applied", () -> {
      Set<String> shipped = shippedVersions(module);
      if (requiredModules().contains(module)) {
        assertFalse(shipped.isEmpty(), "Module " + module + " ships no db/migration/" + dialect() + "/" + module + "_V*.sql script");
      } else {
        // An optional module without scripts for this dialect has no history table: nothing to check, and not a pass
        assumeFalse(shipped.isEmpty(), "Optional module " + module + " ships no scripts for " + dialect());
      }

      assertEquals(shipped, appliedVersions(module), "Versions of " + module + " applied successfully by Flyway");
    }));
  }

  @Test
  @DisplayName("The migrated schema is usable")
  void testMigratedSchemaIsUsable() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.2.2"), "The AWE user settings migration is shipped for " + dialect());

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweUserSettings")) {
      assertTrue(result.next(), "AweUserSettings, created by AWE_V1.2.2, answers a query");
    }
  }

  @Test
  @DisplayName("The security audit events table is migrated")
  void testSecurityAuditTableIsMigrated() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.3.0"), "The AWE security audit migration is shipped for " + dialect());

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweSecAudEvt")) {
      assertTrue(result.next(), "AweSecAudEvt, created by AWE_V1.3.0, answers a query");
    }
  }

  @Test
  @DisplayName("The security audit migration can be applied again over the existing objects")
  void testSecurityAuditMigrationCanBeAppliedAgain() throws Exception {
    // Forget that 1.3.0 was applied: Flyway runs the script again over the table and indexes it already created
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      statement.executeUpdate("DELETE FROM " + quote + "flyway_schema_AWE" + quote + " WHERE " + quote + "version" + quote + " = '1.3.0'");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/" + dialect())
      .sqlMigrationPrefix("AWE_V")
      .table("flyway_schema_AWE")
      .outOfOrder(true)
      .load()
      .migrate();

    assertTrue(appliedVersions("AWE").contains("1.3.0"), "AWE_V1.3.0 applied again successfully on " + dialect());
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweSecAudEvt")) {
      assertTrue(result.next(), "AweSecAudEvt still answers a query");
    }
  }

  @Test
  @DisplayName("The grid layout table is migrated")
  void testGridLayoutTableIsMigrated() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.3.1"), "The AWE grid layout migration is shipped for " + dialect());

    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweUsrGrdCnf WHERE Ope = 'nobody' AND Scr = 'none' AND GrdId = 'none'")) {
      assertTrue(result.next(), "AweUsrGrdCnf, created by AWE_V1.3.1, answers a query");
    }
  }

  @Test
  @DisplayName("The password hash columns hold BCrypt hashes")
  void testPasswordHashColumnsAreWidened() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.4.0"), "The AWE password hash migration is shipped for " + dialect());

    assertPasswordHashColumnIsWidened("ope");
    assertPasswordHashColumnIsWidened("HISope");
  }

  @Test
  @DisplayName("The password hash migration can be applied again over the widened columns")
  void testPasswordHashMigrationCanBeAppliedAgain() throws Exception {
    // Forget that 1.4.0 was applied: Flyway runs the script again over the columns it already widened
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      statement.executeUpdate("DELETE FROM " + quote + "flyway_schema_AWE" + quote + " WHERE " + quote + "version" + quote + " = '1.4.0'");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/" + dialect())
      .sqlMigrationPrefix("AWE_V")
      .table("flyway_schema_AWE")
      .outOfOrder(true)
      .load()
      .migrate();

    assertTrue(appliedVersions("AWE").contains("1.4.0"), "AWE_V1.4.0 applied again successfully on " + dialect());
    assertPasswordHashColumnIsWidened("ope");
    assertPasswordHashColumnIsWidened("HISope");
  }

  @Test
  @DisplayName("The grid layout migration can be applied again over the existing objects")
  void testGridLayoutMigrationCanBeAppliedAgain() throws Exception {
    // Forget that 1.3.1 was applied: Flyway runs the script again over the table and the key it already created
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      statement.executeUpdate("DELETE FROM " + quote + "flyway_schema_AWE" + quote + " WHERE " + quote + "version" + quote + " = '1.3.1'");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/" + dialect())
      .sqlMigrationPrefix("AWE_V")
      .table("flyway_schema_AWE")
      .outOfOrder(true)
      .load()
      .migrate();

    assertTrue(appliedVersions("AWE").contains("1.3.1"), "AWE_V1.3.1 applied again successfully on " + dialect());
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweUsrGrdCnf")) {
      assertTrue(result.next(), "AweUsrGrdCnf still answers a query");
    }
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM AweKey WHERE KeyNam = 'GrdCnfKey'")) {
      assertTrue(result.next() && result.getInt(1) == 1, "The GrdCnfKey sequence exists exactly once");
    }
  }

  @Test
  @DisplayName("The window state table is migrated")
  void testWindowStateTableIsMigrated() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.5.1"), "The AWE window state migration is shipped for " + dialect());

    assertEquals(0, count("SELECT COUNT(*) FROM AweUsrWinCnf WHERE Ope = 'nobody' AND Scr = 'none' AND WinId = 'none' AND AdvOpn = 1"),
      "AweUsrWinCnf, created by AWE_V1.5.1, answers a query on " + dialect());
    assertEquals(1, count("SELECT COUNT(*) FROM AweKey WHERE KeyNam = 'WinCnfKey'"), "The WinCnfKey sequence exists on " + dialect());
  }

  @Test
  @DisplayName("The window state migration can be applied again over the existing objects")
  void testWindowStateMigrationCanBeAppliedAgain() throws Exception {
    // Forget that 1.5.1 was applied: Flyway runs the script again over the table and the key it already created
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      statement.executeUpdate("DELETE FROM " + quote + "flyway_schema_AWE" + quote + " WHERE " + quote + "version" + quote + " = '1.5.1'");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/" + dialect())
      .sqlMigrationPrefix("AWE_V")
      .table("flyway_schema_AWE")
      .outOfOrder(true)
      .load()
      .migrate();

    assertTrue(appliedVersions("AWE").contains("1.5.1"), "AWE_V1.5.1 applied again successfully on " + dialect());
    assertEquals(0, count("SELECT COUNT(*) FROM AweUsrWinCnf WHERE Ope = 'nobody'"), "AweUsrWinCnf still answers a query");
    assertEquals(1, count("SELECT COUNT(*) FROM AweKey WHERE KeyNam = 'WinCnfKey'"), "The WinCnfKey sequence exists exactly once");
  }

  @Test
  @DisplayName("The saved search table is migrated")
  void testSavedSearchTableIsMigrated() throws Exception {
    assertTrue(shippedVersions("AWE").contains("1.5.2"), "The AWE saved search migration is shipped for " + dialect());

    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'nobody' AND Pro IS NULL AND Scr = 'none' AND WinId = 'none' "
      + "AND Nam = 'none' AND Val = '{}'"), "AweSavSch, created by AWE_V1.5.2, answers a query on " + dialect());
    assertEquals(1, count("SELECT COUNT(*) FROM AweKey WHERE KeyNam = 'SavSchKey'"), "The SavSchKey sequence exists on " + dialect());
  }

  @Test
  @DisplayName("The saved search migration can be applied again over the existing objects")
  void testSavedSearchMigrationCanBeAppliedAgain() throws Exception {
    // Forget that 1.5.2 was applied: Flyway runs the script again over the table and the key it already created
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      statement.executeUpdate("DELETE FROM " + quote + "flyway_schema_AWE" + quote + " WHERE " + quote + "version" + quote + " = '1.5.2'");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/" + dialect())
      .sqlMigrationPrefix("AWE_V")
      .table("flyway_schema_AWE")
      .outOfOrder(true)
      .load()
      .migrate();

    assertTrue(appliedVersions("AWE").contains("1.5.2"), "AWE_V1.5.2 applied again successfully on " + dialect());
    assertEquals(0, count("SELECT COUNT(*) FROM AweSavSch WHERE Ope = 'nobody'"), "AweSavSch still answers a query");
    assertEquals(1, count("SELECT COUNT(*) FROM AweKey WHERE KeyNam = 'SavSchKey'"), "The SavSchKey sequence exists exactly once");
  }

  @Test
  @DisplayName("The test application seeds the data of its totalization grids and chart examples")
  void testApplicationTestDataIsSeeded() throws Exception {
    // Only the dialects the application ships scripts for get its test data
    assumeFalse(shippedVersions(APPLICATION_MODULE).isEmpty(), "The application ships no scripts for " + dialect());

    // QrySitModDbsOrd (SQL and service totalization grids) joins every relation to its site, module and database
    assertTrue(count("SELECT COUNT(*) FROM AweSitModDbs r JOIN AweSit s ON s.IdeSit = r.IdeSit "
      + "JOIN AweMod m ON m.IdeMod = r.IdeMod JOIN AweDbs d ON d.IdeDbs = r.IdeDbs") > 0,
      "The site/module/database relations of the totalization grids reference existing rows on " + dialect());
    // The chart examples draw one point per distinct date of the database history
    assertTrue(count("SELECT COUNT(DISTINCT HISdat) FROM HISAweDbs") >= 12,
      "The database history has enough dates to draw the chart examples on " + dialect());
    // New sites and relations created from the screens must not collide with the seeded ids
    assertTrue(keyValue("SitKey") > count("SELECT MAX(IdeSit) FROM AweSit"), "SitKey is past the seeded sites on " + dialect());
    assertTrue(keyValue("SitModDbsKey") > count("SELECT MAX(IdeSitModDbs) FROM AweSitModDbs"),
      "SitModDbsKey is past the seeded relations on " + dialect());
  }

  private int keyValue(String key) throws SQLException {
    // A missing key would make every comparison with it false: it must exist
    assertEquals(1, count("SELECT COUNT(*) FROM AweKey WHERE KeyNam = '" + key + "'"), "Key " + key + " exists on " + dialect());
    return count("SELECT KeyVal FROM AweKey WHERE KeyNam = '" + key + "'");
  }

  private int count(String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery(sql)) {
      assertTrue(result.next(), "No result for " + sql);
      return result.getInt(1);
    }
  }

  private void assertPasswordHashColumnIsWidened(String table) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT l1_pas FROM " + table + " WHERE 1 = 0")) {
      int size = result.getMetaData().getPrecision(1);
      String type = result.getMetaData().getColumnTypeName(1);
      assertTrue(size >= 255, table + ".l1_pas holds 255 characters on " + dialect() + " (" + type + "(" + size + "))");
      assertFalse(type.equalsIgnoreCase("CHAR") || type.equalsIgnoreCase("CHARACTER") || type.equalsIgnoreCase("BPCHAR"),
        table + ".l1_pas is not a fixed length type on " + dialect() + " (" + type + ")");
    }
  }

  private String[] configuredModules() {
    String[] modules = databaseConfigProperties.getMigrationModules();
    return modules == null ? new String[0] : modules;
  }

  private Set<String> shippedVersions(String module) throws IOException {
    Set<String> versions = new TreeSet<>();
    Pattern scriptName = Pattern.compile(String.format(SCRIPT_NAME_PATTERN, Pattern.quote(module)));
    String location = "classpath*:db/migration/" + dialect() + "/" + module + "_V*.sql";
    for (Resource resource : new PathMatchingResourcePatternResolver().getResources(location)) {
      if (belongsToALongerModule(module, String.valueOf(resource.getFilename()))) {
        continue; // A script of another configured module whose name starts with this one
      }
      Matcher matcher = scriptName.matcher(String.valueOf(resource.getFilename()));
      assertTrue(matcher.matches(), "Migration script " + resource.getFilename() + " does not follow "
        + module + "_V<version>__<description>.sql");
      versions.add(matcher.group(1).replace('_', '.'));
    }
    return versions;
  }

  /**
   * Whether a script that matches this module's prefix really belongs to another configured module whose name
   * starts with this one (for example AWE_VENDOR next to AWE); every other matching script must follow the
   * naming rule, so a misnamed script of this module is reported instead of ignored
   */
  private boolean belongsToALongerModule(String module, String fileName) {
    return Arrays.stream(configuredModules())
      .filter(other -> other.length() > module.length() && other.startsWith(module))
      .anyMatch(other -> fileName.startsWith(other + "_V"));
  }

  private Set<String> appliedVersions(String module) throws SQLException {
    Set<String> versions = new TreeSet<>();
    try (Connection connection = dataSource.getConnection()) {
      String quote = connection.getMetaData().getIdentifierQuoteString();
      // The baseline row (version 0) Flyway writes in a schema that already has tables is not a migration
      String sql = "SELECT " + quote + "version" + quote + ", " + quote + "success" + quote
        + " FROM " + quote + "flyway_schema_" + module + quote + " WHERE " + quote + "type" + quote + " = 'SQL'";
      try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
        while (result.next()) {
          if (result.getBoolean(2)) {
            versions.add(result.getString(1));
          }
        }
      }
    }
    return versions;
  }
}
