package com.almis.awe.test.integration.database;

import com.almis.awe.model.util.theme.ThemePresets;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the AWE_V1.5.0__Theme_primary_surface.sql migration (#885, S7).
 * <p>
 * The H2 and HSQLDB scripts run with Flyway on an in-memory database: the schema is migrated up to V1.4.0, the test inserts
 * the rows a running application may have (a customised classic theme, a custom theme with colours, custom themes without
 * valid colours, a user setting), then V1.5.0 runs and the theme rows must carry their primary, surface and mode.
 * The scripts of the other dialects can not run here: their structure is checked against the same rules.
 * <p>
 * The light colour hex is copied as #RRGGBB in upper case, a short #RGB expanded (#885, S13).
 * <p>
 * The script can be applied again (#885, S11): a second run adds no column and only fills the empty values, so the
 * values a user changed after the first run are kept.
 * <p>
 * The scripts are read from the source tree of awe-spring-boot-starter (as {@link UserSettingsMigrationDialectParityTest}
 * does), so the test checks the scripts being changed and not the starter jar of the local repository.
 */
@DisplayName("AweThm primary, surface and mode migration")
@Tag("integration")
class ThemePrimarySurfaceMigrationTest {

  private static final String MIGRATION_FILE = "AWE_V1.5.0__Theme_primary_surface.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  private static final String[] DIALECTS = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"};

  private static final String[] CLASSIC_THEMES = {"default", "asphalt", "purple-hills", "dust", "frost", "fresh", "silver",
    "clean", "white", "adminflare", "orange", "sunset", "grass", "sky", "eclipse", "sunny", "sea", "ferrari", "amazonia"};

  @ParameterizedTest(name = "{0}: classic themes take their preset")
  @ValueSource(strings = {"h2", "hsqldb"})
  void classicThemesTakeTheirPreset(String dialect) throws SQLException {
    try (Connection connection = migrateWithThemeData(dialect)) {
      assertThat(theme(connection, "sunset")).containsExactly("red", "auto", "system");
      assertThat(theme(connection, "sky")).containsExactly("blue", "auto", "system");
      assertThat(theme(connection, "frost")).containsExactly("cyan", "slate", "system");
      assertThat(theme(connection, "dust")).containsExactly("#948787", "stone", "system");
      assertThat(theme(connection, "orange")).containsExactly("red", "stone", "system");
      assertThat(theme(connection, "default")).containsExactly("sky", "auto", "system");
      assertThat(theme(connection, "purple-hills")).containsExactly("violet", "auto", "system");
    }
  }

  @ParameterizedTest(name = "{0}: themes with a light colour set keep its primary colour")
  @ValueSource(strings = {"h2", "hsqldb"})
  void themesWithLightColoursKeepTheirPrimaryColour(String dialect) throws SQLException {
    try (Connection connection = migrateWithThemeData(dialect)) {
      // Custom theme: the light primary colour, not the dark one, in upper case (S13)
      assertThat(theme(connection, "corporate")).containsExactly("#1A73E8", "auto", "system");
      // Short hex: expanded to #RRGGBB in upper case (S13)
      assertThat(theme(connection, "short-hex")).containsExactly("#AABBCC", "auto", "system");
      // Customised classic theme: its colour wins over the preset primary, the preset surface stays
      assertThat(theme(connection, "sea")).containsExactly("#0E7490", "slate", "system");
    }
  }

  @ParameterizedTest(name = "{0}: themes without a valid light colour take the defaults")
  @ValueSource(strings = {"h2", "hsqldb"})
  void themesWithoutValidColoursTakeTheDefaults(String dialect) throws SQLException {
    try (Connection connection = migrateWithThemeData(dialect)) {
      assertThat(theme(connection, "plain")).containsExactly("emerald", "auto", "system");
      assertThat(theme(connection, "named-colour")).containsExactly("emerald", "auto", "system");
      assertThat(theme(connection, "five-digits")).containsExactly("emerald", "auto", "system");
      assertThat(theme(connection, "only-dark")).containsExactly("emerald", "auto", "system");
      // A classic theme whose light colour is not a hex keeps its preset
      assertThat(theme(connection, "ferrari")).containsExactly("red", "auto", "system");
    }
  }

  @ParameterizedTest(name = "{0}: theme colours, history and user settings are preserved")
  @ValueSource(strings = {"h2", "hsqldb"})
  void themeColoursAndUserSettingsArePreserved(String dialect) throws SQLException {
    try (Connection connection = migrateWithThemeData(dialect);
         Statement statement = connection.createStatement()) {
      assertThat(count(statement, "SELECT COUNT(*) FROM AweThmCol")).isEqualTo(8);
      assertThat(string(statement, "SELECT primaryColor FROM AweThmCol WHERE theme = 'corporate' AND dark = 1"))
        .isEqualTo("#000000");
      assertThat(count(statement, "SELECT COUNT(*) FROM AweThm WHERE Pri IS NULL OR Srf IS NULL OR Mde IS NULL"))
        .isZero();

      // Density is a new optional user setting: existing users keep their mode and use the application density
      assertThat(string(statement, "SELECT themeMode FROM AweUsrSet WHERE userName = 'test'")).isEqualTo("dark");
      assertThat(string(statement, "SELECT density FROM AweUsrSet WHERE userName = 'test'")).isNull();
      statement.executeUpdate("UPDATE AweUsrSet SET density = 'compact' WHERE userName = 'test'");
      assertThat(string(statement, "SELECT density FROM AweUsrSet WHERE userName = 'test'")).isEqualTo("compact");

      // The theme maintain audits every field in the history table
      statement.executeUpdate("INSERT INTO HISAweThm (HISope, HISdat, HISact, IdeThm, Nam, Act, Pri, Srf, Mde) "
        + "VALUES ('test', CURRENT_DATE, 'U', 100, 'corporate', 1, '#1A73E8', 'auto', 'dark')");
      assertThat(string(statement, "SELECT Mde FROM HISAweThm WHERE Nam = 'corporate'")).isEqualTo("dark");
    }
  }

  @ParameterizedTest(name = "{0}: V1.5.0 is recorded as applied")
  @ValueSource(strings = {"h2", "hsqldb"})
  void migrationIsRecordedAsApplied(String dialect) throws SQLException {
    String url = newUrl(dialect);
    try (Connection ignored = migrateWithThemeData(dialect, url)) {
      MigrationInfo current = flyway(url, null).info().current();
      assertThat(current.getVersion().getVersion()).isEqualTo("1.5.0");
      assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);
    }
  }

  @ParameterizedTest(name = "{0}: V1.5.0 can be applied again without overwriting the values already set")
  @ValueSource(strings = {"h2", "hsqldb"})
  void migrationCanBeAppliedAgain(String dialect) throws SQLException {
    String url = newUrl(dialect);
    try (Connection connection = migrateWithThemeData(dialect, url);
         Statement statement = connection.createStatement()) {
      Map<String, List<String>> expected = themes(connection);

      // The values a user changes after the first run
      statement.executeUpdate("UPDATE AweThm SET Pri = 'green', Srf = 'zinc', Mde = 'dark' WHERE Nam = 'sunset'");
      statement.executeUpdate("UPDATE AweThm SET Pri = 'indigo' WHERE Nam = 'corporate'");
      statement.executeUpdate("UPDATE AweThm SET Srf = 'neutral' WHERE Nam = 'sea'");
      statement.executeUpdate("UPDATE AweUsrSet SET density = 'compact' WHERE userName = 'test'");
      // A theme created afterwards without values
      insertTheme(statement, 106, "late");
      expected.put("sunset", List.of("green", "zinc", "dark"));
      expected.put("corporate", List.of("indigo", "auto", "system"));
      expected.put("sea", List.of("#0E7490", "neutral", "system"));
      expected.put("late", List.of("emerald", "auto", "system"));

      reapplyMigration(url);

      assertThat(themes(connection)).isEqualTo(expected);
      assertThat(string(statement, "SELECT density FROM AweUsrSet WHERE userName = 'test'")).isEqualTo("compact");
      assertThat(string(statement, "SELECT themeMode FROM AweUsrSet WHERE userName = 'test'")).isEqualTo("dark");
      assertThat(count(statement, "SELECT COUNT(*) FROM AweThmCol")).isEqualTo(8);
    }
  }

  @ParameterizedTest(name = "{0}: V1.5.0 only adds the missing columns and only fills the empty values")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  void everyDialectCanApplyTheMigrationAgain(String dialect) throws IOException {
    String content = readMigration(dialect);
    assertThat(content).as(dialect + " documents that the script can be applied again")
      .contains("The script can be applied again:");

    // Columns: every column is added only when it is missing
    assertThat(guardedColumns(dialect, content)).as(dialect + " guarded columns")
      .containsExactlyInAnyOrder("AWETHM.PRI", "AWETHM.SRF", "AWETHM.MDE", "HISAWETHM.PRI", "HISAWETHM.SRF",
        "HISAWETHM.MDE", "AWEUSRSET.DENSITY");
    int alters = count("(?i)ALTER\\s+TABLE\\b", content);
    assertThat(alters).as(dialect + " has no unguarded ALTER TABLE").isEqualTo("oracle".equals(dialect) ? 1 : 7);

    // Data: every update only fills empty values
    List<String> updates = themeUpdates(content);
    assertThat(updates).as(dialect + " updates: hex, " + CLASSIC_THEMES.length + " presets, 3 defaults")
      .hasSize(1 + CLASSIC_THEMES.length + 3);
    for (String update : updates) {
      Matcher assigned = Pattern.compile("(?i)(?:\\bSET|,)\\s*(Pri|Srf|Mde)\\s*=").matcher(update);
      int columns = 0;
      while (assigned.find()) {
        String column = assigned.group(1);
        columns++;
        assertThat(Pattern.compile("(?is)COALESCE\\(\\s*" + column + "\\s*,|\\bWHERE\\b.*\\b" + column + "\\s+IS\\s+NULL")
          .matcher(update).find()).as(dialect + " only fills an empty " + column + ": " + update).isTrue();
      }
      assertThat(columns).as(dialect + " assigned columns of " + update).isPositive();
    }

    // Order: the light colour hex first, then the presets, then the defaults
    assertThat(updates.get(0)).as(dialect + " hex first").contains("AweThmCol");
    assertThat(updates.get(updates.size() - 3)).as(dialect + " defaults last").contains("'emerald'");

    if ("sqlserver".equals(dialect)) {
      // Flyway sends a SQL Server script as one batch up to each GO: the updates of the new columns need their own batch
      int lastAlter = content.toUpperCase(Locale.ROOT).lastIndexOf("ALTER TABLE");
      int firstUpdate = content.indexOf(updates.get(0));
      assertThat(Pattern.compile("(?m)^GO\\s*$").matcher(content.substring(lastAlter, firstUpdate)).find())
        .as("sqlserver separates the new columns from their updates with GO").isTrue();
    }
  }

  @ParameterizedTest(name = "{0}: the light colour hex is copied as #RRGGBB in upper case")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  void everyDialectCopiesTheHexAsUpperCaseRrggbb(String dialect) throws IOException {
    String hexUpdate = themeUpdates(readMigration(dialect)).get(0);
    String colour = "colors\\.primaryColor";

    // #RGB: each digit twice, in upper case
    assertThat(hexUpdate).as(dialect + " detects a short hex by its length")
      .containsPattern("(?i)\\b(CHAR_LENGTH|LENGTH|LEN)\\(\\s*" + colour + "\\s*\\)\\s*=\\s*4\\b");
    for (int digit = 2; digit <= 4; digit++) {
      assertThat(count("(?i)\\bSUBSTR(ING)?\\(\\s*" + colour + "\\s*,\\s*" + digit + "\\s*,\\s*1\\s*\\)", hexUpdate))
        .as(dialect + " repeats the digit at " + digit).isEqualTo(2);
    }
    assertThat(hexUpdate).as(dialect + " upper-cases the expanded hex")
      .containsPattern("(?i)\\bUPPER\\(\\s*(CONCAT\\()?'#'");
    // #RRGGBB: the colour in upper case
    assertThat(hexUpdate).as(dialect + " upper-cases a #RRGGBB hex")
      .containsPattern("(?i)\\bUPPER\\(\\s*" + colour + "\\s*\\)");

    // The concatenation of each engine (|| is a logical OR in MySQL)
    switch (dialect) {
      case "mysql" -> assertThat(hexUpdate).contains("CONCAT('#'").doesNotContain("||");
      case "sqlserver" -> assertThat(hexUpdate).containsPattern("'#'\\s*\\+").doesNotContain("||");
      default -> assertThat(hexUpdate).containsPattern("'#'\\s*\\|\\|").doesNotContain("CONCAT(");
    }
  }

  @ParameterizedTest(name = "{0} dialect ships the same migration")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  void everyDialectShipsTheMigration(String dialect) throws IOException {
    String content = readMigration(dialect);
    String upper = content.toUpperCase(Locale.ROOT);

    for (String column : new String[]{"PRI", "SRF", "MDE", "DENSITY"}) {
      assertThat(Pattern.compile("\\b" + column + "\\b").matcher(upper).find())
        .as(dialect + " adds column " + column).isTrue();
    }
    assertThat(upper).as(dialect + " alters the theme history table").contains("HISAWETHM");
    assertThat(upper).as(dialect + " alters the user settings table").contains("AWEUSRSET");
    for (String theme : CLASSIC_THEMES) {
      assertThat(content).as(dialect + " sets the preset of " + theme).contains("'" + theme + "'");
    }
    // AweThmCol is kept untouched until a later version drops it
    assertThat(upper).as(dialect + " keeps AweThmCol")
      .doesNotContainPattern("(DROP|ALTER)\\s+TABLE\\s+(IF\\s+EXISTS\\s+)?AWETHMCOL\\b")
      .doesNotContainPattern("(UPDATE|DELETE\\s+FROM|INSERT\\s+INTO)\\s+AWETHMCOL\\b");
  }

  @Test
  @DisplayName("Every dialect sets the same presets")
  void everyDialectSetsTheSamePresets() throws IOException {
    List<String> reference = presetLines(readMigration("h2"));
    assertThat(reference).hasSize(CLASSIC_THEMES.length);
    for (String dialect : DIALECTS) {
      assertThat(presetLines(readMigration(dialect))).as(dialect).isEqualTo(reference);
    }
  }

  @Test
  @DisplayName("The migration sets the presets of the theme model")
  void migrationSetsThePresetsOfTheThemeModel() throws IOException {
    List<String> javaPresets = ThemePresets.all().entrySet().stream()
      .map(preset -> preset.getKey() + "=" + preset.getValue().primary() + "/" + preset.getValue().surface())
      .toList();
    assertThat(presetLines(readMigration("h2"))).isEqualTo(javaPresets);
  }

  /**
   * Migrates a new in-memory database up to V1.4.0, inserts the theme data and migrates to the last version
   *
   * @param dialect Dialect
   * @return Open connection to the migrated database
   */
  private Connection migrateWithThemeData(String dialect) throws SQLException {
    return migrateWithThemeData(dialect, newUrl(dialect));
  }

  private Connection migrateWithThemeData(String dialect, String url) throws SQLException {
    // Keep a connection open so the in-memory database lives until the test ends
    Connection connection = DriverManager.getConnection(url, "sa", "");
    flyway(url, "1.4.0").migrate();
    insertThemeData(connection);
    flyway(url, null).migrate();
    return connection;
  }

  /**
   * Applies V1.5.0 again: the history row is removed and Flyway runs the script with its own statement splitting
   *
   * @param url Database url
   */
  private void reapplyMigration(String url) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, "sa", "");
         Statement statement = connection.createStatement()) {
      assertThat(statement.executeUpdate("DELETE FROM \"flyway_schema_AWE\" WHERE \"version\" = '1.5.0'")).isEqualTo(1);
    }
    MigrateResult result = flyway(url, null).migrate();
    assertThat(result.migrationsExecuted).isEqualTo(1);
  }

  private void insertThemeData(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      insertTheme(statement, 100, "corporate");
      insertTheme(statement, 101, "plain");
      insertTheme(statement, 102, "short-hex");
      insertTheme(statement, 103, "named-colour");
      insertTheme(statement, 104, "five-digits");
      insertTheme(statement, 105, "only-dark");
      insertColours(statement, 1, "corporate", 0, "#1a73e8");
      insertColours(statement, 2, "corporate", 1, "#000000");
      insertColours(statement, 3, "short-hex", 0, "#AbC");
      insertColours(statement, 4, "named-colour", 0, "blue");
      insertColours(statement, 5, "five-digits", 0, "#12345");
      insertColours(statement, 6, "only-dark", 1, "#123456");
      insertColours(statement, 7, "sea", 0, "#0E7490");
      // A classic theme with a light colour set that is not a hex
      insertColours(statement, 8, "ferrari", 0, "red");
      statement.executeUpdate("INSERT INTO AweUsrSet (userName, themeMode) VALUES ('test', 'dark')");
    }
  }

  private void insertTheme(Statement statement, int id, String name) throws SQLException {
    statement.executeUpdate("INSERT INTO AweThm (IdeThm, Nam, Act) VALUES (" + id + ", '" + name + "', 1)");
  }

  private void insertColours(Statement statement, int id, String theme, int dark, String primaryColor) throws SQLException {
    statement.executeUpdate("INSERT INTO AweThmCol (IdeThmCol, theme, dark, primaryColor) VALUES ("
      + id + ", '" + theme + "', " + dark + ", '" + primaryColor + "')");
  }

  private Flyway flyway(String url, String target) {
    var configuration = Flyway.configure()
      .dataSource(url, "sa", "")
      .locations("filesystem:" + new File(MIGRATION_BASE_PATH + dialectFolder(url)).getAbsolutePath())
      .sqlMigrationPrefix("AWE_V")
      .repeatableSqlMigrationPrefix("AWE_R")
      .table("flyway_schema_AWE")
      .baselineOnMigrate(true)
      .baselineVersion("0");
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  private String newUrl(String dialect) {
    String name = "themes_" + UUID.randomUUID().toString().replace("-", "");
    return "h2".equals(dialect)
      ? "jdbc:h2:mem:" + name + ";NON_KEYWORDS=value,user,day,year,month,hour,minute,second"
      : "jdbc:hsqldb:mem:" + name;
  }

  private String dialectFolder(String url) {
    return url.startsWith("jdbc:h2:") ? "h2" : "hsqldb";
  }

  private List<String> theme(Connection connection, String name) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("SELECT Pri, Srf, Mde FROM AweThm WHERE Nam = ?")) {
      statement.setString(1, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).as("theme " + name + " exists").isTrue();
        return List.of(String.valueOf(resultSet.getString(1)), String.valueOf(resultSet.getString(2)),
          String.valueOf(resultSet.getString(3)));
      }
    }
  }

  private Map<String, List<String>> themes(Connection connection) throws SQLException {
    Map<String, List<String>> themes = new TreeMap<>();
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT Nam FROM AweThm")) {
      while (resultSet.next()) {
        themes.put(resultSet.getString(1), null);
      }
    }
    for (String name : themes.keySet()) {
      themes.put(name, theme(connection, name));
    }
    return themes;
  }

  /**
   * Columns a script adds only when they are missing, as {@code TABLE.COLUMN}
   *
   * @param dialect Dialect
   * @param content Script
   * @return Guarded columns
   */
  private List<String> guardedColumns(String dialect, String content) {
    String guard = switch (dialect) {
      // ALTER TABLE <table> ADD COLUMN IF NOT EXISTS <column>
      case "h2", "hsqldb", "postgresql" -> "ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+COLUMN\\s+IF\\s+NOT\\s+EXISTS\\s+(\\w+)";
      // The ALTER is prepared only when information_schema does not have the column
      case "mysql" -> "IF\\(COUNT\\(\\*\\)\\s*=\\s*0,\\s*'ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+COLUMN\\s+(\\w+)\\s[^']*',\\s*'DO 0'\\)"
        + "\\s+FROM\\s+information_schema\\.columns\\s+WHERE\\s+table_schema\\s*=\\s*DATABASE\\(\\)"
        + "\\s+AND\\s+LOWER\\(table_name\\)\\s*=\\s*'\\1'\\s+AND\\s+LOWER\\(column_name\\)\\s*=\\s*'\\2'\\)";
      // A PL/SQL procedure adds each column and ignores ORA-01430 (column already exists)
      case "oracle" -> "add_column\\('(\\w+)',\\s*'(\\w+)'";
      // IF NOT EXISTS on INFORMATION_SCHEMA.COLUMNS before each ALTER
      case "sqlserver" -> "IF\\s+NOT\\s+EXISTS\\s*\\(SELECT\\s+\\*\\s+FROM\\s+INFORMATION_SCHEMA\\.COLUMNS\\s+"
        + "WHERE\\s+TABLE_NAME\\s*=\\s*'(\\w+)'\\s+AND\\s+COLUMN_NAME\\s*=\\s*'(\\w+)'\\)\\s+"
        + "ALTER\\s+TABLE\\s+\\1\\s+ADD\\s+\\2\\s";
      default -> throw new IllegalArgumentException(dialect);
    };
    List<String> columns = new ArrayList<>();
    Matcher matcher = Pattern.compile("(?i)" + guard).matcher(content);
    while (matcher.find()) {
      columns.add((matcher.group(1) + "." + matcher.group(2)).toUpperCase(Locale.ROOT));
    }
    if ("oracle".equals(dialect)) {
      assertThat(content).as("oracle ignores only ORA-01430").contains("SQLCODE != -1430");
      assertThat(content).as("oracle ends the PL/SQL block").containsPattern("(?m)^END;\\s*\\R/\\s*$");
    }
    return columns;
  }

  /**
   * Updates of AweThm in a script, in order
   *
   * @param content Script
   * @return Each {@code UPDATE AweThm SET ...;} statement
   */
  private List<String> themeUpdates(String content) {
    List<String> updates = new ArrayList<>();
    Matcher matcher = Pattern.compile("(?is)UPDATE\\s+AweThm\\s+SET\\s.*?;").matcher(content);
    while (matcher.find()) {
      updates.add(matcher.group());
    }
    return updates;
  }

  private int count(String regex, String content) {
    Matcher matcher = Pattern.compile(regex).matcher(content);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  private int count(Statement statement, String sql) throws SQLException {
    try (ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private String string(Statement statement, String sql) throws SQLException {
    try (ResultSet resultSet = statement.executeQuery(sql)) {
      assertThat(resultSet.next()).as(sql).isTrue();
      return resultSet.getString(1);
    }
  }

  /**
   * Presets of a script: the {@code SET Pri = COALESCE(Pri, '<primary>'), Srf = COALESCE(Srf, '<surface>')
   * WHERE Nam = '<theme>'} updates
   */
  private List<String> presetLines(String content) {
    List<String> lines = new ArrayList<>();
    var matcher = Pattern.compile("(?i)SET\\s+Pri\\s*=\\s*COALESCE\\(\\s*Pri\\s*,\\s*'([^']+)'\\s*\\)\\s*,"
        + "\\s*Srf\\s*=\\s*COALESCE\\(\\s*Srf\\s*,\\s*'([^']+)'\\s*\\)\\s+WHERE\\s+Nam\\s*=\\s*'([^']+)'")
      .matcher(content);
    while (matcher.find()) {
      lines.add(matcher.group(3) + "=" + matcher.group(1) + "/" + matcher.group(2));
    }
    return lines;
  }

  private String readMigration(String dialect) throws IOException {
    File file = new File(MIGRATION_BASE_PATH + dialect, MIGRATION_FILE);
    assertThat(file).as("Migration file of " + dialect).exists();
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }
}
