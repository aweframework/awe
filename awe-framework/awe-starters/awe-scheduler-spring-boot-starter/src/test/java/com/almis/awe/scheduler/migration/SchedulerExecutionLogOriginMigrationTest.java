package com.almis.awe.scheduler.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guard for folding the origin dimension into the existing
 * {@code SCHEDULER_V1.0.6__Execution_log_window.sql} migration (no {@code V1.0.7}), across every
 * vendor dialect and behaviourally against a real HSQLDB apply.
 */
class SchedulerExecutionLogOriginMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.6__Execution_log_window.sql";

  private static final String[] DIALECTS = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"};

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationFileExistsForDialect(String dialect) {
    assertTrue(resourceExists(dialect), "Missing " + MIGRATION_FILE + " for dialect: " + dialect);
  }

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationDefinesTheSrcColumnAheadOfTheSectionColumn(String dialect) {
    String content = readStatements(dialect);

    int srcIndex = content.indexOf("SRC");
    int secIndex = content.indexOf("SEC");

    assertTrue(srcIndex >= 0, dialect + ": migration must define the Src origin column");
    assertTrue(secIndex >= 0, dialect + ": migration must still define the Sec column");
    assertTrue(srcIndex < secIndex, dialect + ": Src must precede Sec in the primary key column order");
  }

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationRebuildsThePrimaryKeyToIncludeSrc(String dialect) {
    String content = readStatements(dialect);

    assertTrue(content.contains("PRIMARY KEY (\"IDETSK\", \"EXETSK\", \"SRC\", \"SEC\", \"SLT\")")
            || content.contains("PRIMARY KEY (IDETSK, EXETSK, SRC, SEC, SLT)"),
        dialect + ": primary key must be rebuilt to (IdeTsk, ExeTsk, Src, Sec, Slt)");
  }

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationMovesTheSecondaryIndexToLogDat(String dialect) {
    String content = readStatements(dialect);

    assertTrue(content.contains("(\"IDETSK\", \"EXETSK\", \"LOGDAT\")") || content.contains("(IDETSK, EXETSK, LOGDAT)"),
        dialect + ": AWESCHEXELOGI1 must move to (IdeTsk, ExeTsk, LogDat)");
    assertFalse(content.contains("(\"IDETSK\", \"EXETSK\", \"LINNUM\")") || content.contains("(IDETSK, EXETSK, LINNUM)"),
        dialect + ": AWESCHEXELOGI1 must no longer index LinNum");
  }

  /**
   * No {@code V1.0.7} migration exists anywhere in this change: the origin dimension ships inside
   * the existing {@code V1.0.6} file for every dialect (spec: fresh schema apply creates the final
   * shape directly, with no later migration adding it).
   */
  @Test
  void noV107MigrationExistsForAnyDialect() {
    for (String dialect : DIALECTS) {
      assertFalse(resourceExists(dialect, "SCHEDULER_V1.0.7__Execution_log_window.sql"),
          dialect + ": must not introduce a V1.0.7 migration for the origin dimension");
    }
    assertEquals(6, DIALECTS.length);
  }

  /**
   * Fresh schema apply, against a real HSQLDB in-memory database, creates {@code AweSchExeLog}
   * with the {@code Src} column and the composite primary key in one step.
   *
   * @throws SQLException Test error
   */
  @Test
  void freshApplyCreatesTheCompositePrimaryKeyIncludingSrc() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      runMigration(connection);

      insertLogLine(connection, 1, 1, "S", "O", 0, 0);
      insertLogLine(connection, 1, 1, "A", "O", 0, 0);

      assertEquals(2, countLogLines(connection, 1, 1));
    }
  }

  /**
   * Two origins may occupy the exact same {@code (Sec, Slt)} slot for the same execution: the
   * primary key now discriminates on {@code Src} first (origins number lines independently).
   *
   * @throws SQLException Test error
   */
  @Test
  void sameSlotIsAllowedAcrossDifferentOrigins() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      runMigration(connection);

      insertLogLine(connection, 1, 1, "S", "H", 3, 7);
      insertLogLine(connection, 1, 1, "A", "H", 3, 7);

      assertEquals(2, countLogLines(connection, 1, 1));
    }
  }

  /**
   * The same origin may never repeat a slot for the same execution: the primary key still
   * rejects a duplicate {@code (IdeTsk, ExeTsk, Src, Sec, Slt)}.
   *
   * @throws SQLException Test error
   */
  @Test
  void duplicateSlotWithinTheSameOriginViolatesThePrimaryKey() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      runMigration(connection);

      insertLogLine(connection, 1, 1, "S", "H", 3, 7);

      assertThrows(SQLException.class, () -> insertLogLine(connection, 1, 1, "S", "H", 3, 7));
    }
  }

  /**
   * {@code LogDat} becomes the event timestamp and is mandatory on every row (no null case for
   * the read-path merge to define).
   *
   * @throws SQLException Test error
   */
  @Test
  void logDatIsMandatoryOnEveryRow() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      runMigration(connection);

      assertThrows(SQLException.class, () -> execute(connection,
          "INSERT INTO AweSchExeLog (IdeTsk, ExeTsk, Src, Sec, Slt, LinNum, LinTxt, LogDat)"
              + " VALUES (1, 1, 'S', 'H', 0, 0, 'line', NULL)"));
    }
  }

  private static Connection memoryDatabase() throws SQLException {
    return DriverManager.getConnection("jdbc:hsqldb:mem:" + UUID.randomUUID() + ";shutdown=true", "sa", "");
  }

  private static void runMigration(Connection connection) throws SQLException {
    for (String statement : readMigration("hsqldb").split(";")) {
      String sql = stripComments(statement);
      if (!sql.isEmpty()) {
        execute(connection, sql);
      }
    }
  }

  private static void insertLogLine(Connection connection, int taskId, int executionId, String origin, String section,
                                     int slot, int lineNumber) throws SQLException {
    execute(connection, "INSERT INTO AweSchExeLog (IdeTsk, ExeTsk, Src, Sec, Slt, LinNum, LinTxt, LogDat)"
        + " VALUES (" + taskId + ", " + executionId + ", '" + origin + "', '" + section + "', " + slot + ", "
        + lineNumber + ", 'line', CURRENT_TIMESTAMP)");
  }

  private static int countLogLines(Connection connection, int taskId, int executionId) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      var resultSet = statement.executeQuery(
          "SELECT COUNT(*) FROM AweSchExeLog WHERE IdeTsk = " + taskId + " AND ExeTsk = " + executionId);
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static String readStatements(String dialect) {
    return stripComments(readMigration(dialect)).toUpperCase(Locale.ROOT);
  }

  private static String stripComments(String statement) {
    StringBuilder builder = new StringBuilder();
    for (String line : statement.split("\n")) {
      if (!line.trim().startsWith("--")) {
        builder.append(line).append('\n');
      }
    }
    return builder.toString().trim();
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static boolean resourceExists(String dialect) {
    return resourceExists(dialect, MIGRATION_FILE);
  }

  private static boolean resourceExists(String dialect, String fileName) {
    try (InputStream stream = classLoader().getResourceAsStream("db/migration/" + dialect + "/" + fileName)) {
      return stream != null;
    } catch (IOException e) {
      return false;
    }
  }

  private static String readMigration(String dialect) {
    try (InputStream stream = classLoader().getResourceAsStream(resourcePath(dialect))) {
      if (stream == null) {
        fail("Missing " + MIGRATION_FILE + " for dialect: " + dialect);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String resourcePath(String dialect) {
    return "db/migration/" + dialect + "/" + MIGRATION_FILE;
  }

  private static ClassLoader classLoader() {
    return SchedulerExecutionLogOriginMigrationTest.class.getClassLoader();
  }
}
