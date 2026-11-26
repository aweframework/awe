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
 * Guard for the persistence of the deferred executions: every dialect creates the table and nothing else.
 */
class SchedulerDeferredExecutionsMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.8__Deferred_executions.sql";

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationCreatesOnlyTheDeferredExecutionsTable(String dialect) {
    String content = stripComments(readMigration(dialect, MIGRATION_FILE)).toUpperCase(Locale.ROOT);

    assertTrue(content.contains("AWESCHEXEEXT"), dialect + ": migration must create AweSchExeExt");
    for (String column : new String[]{"IDETSK", "EXETSK", "EXTREF", "DFRDAT", "DUEDAT"}) {
      assertTrue(content.contains(column), dialect + ": missing column " + column);
    }
    assertFalse(content.contains("ALTER TABLE"), dialect + ": the task tables are not changed");
    assertEquals(1, content.split("CREATE TABLE", -1).length - 1, dialect + ": one table only");
  }

  @Test
  void theTableKeepsOneRowPerExecutionAndAcceptsAnEmptyReference() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
        "jdbc:hsqldb:mem:" + UUID.randomUUID() + ";shutdown=true", "sa", "")) {
      for (String statement : readMigration("hsqldb", MIGRATION_FILE).split(";")) {
        String sql = stripComments(statement);
        if (!sql.isEmpty()) {
          execute(connection, sql);
        }
      }

      execute(connection, row(1, 1, "'job-77'"));
      execute(connection, row(1, 2, "NULL"));
      assertThrows(SQLException.class, () -> execute(connection, row(1, 1, "'job-78'")),
          "one deferred row per execution");
      assertThrows(SQLException.class, () -> execute(connection,
          "INSERT INTO AweSchExeExt (IdeTsk, ExeTsk, ExtRef, DfrDat) VALUES (2, 1, 'x', CURRENT_TIMESTAMP)"),
          "the due date is required");
    }
  }

  private static String row(int task, int execution, String reference) {
    return "INSERT INTO AweSchExeExt (IdeTsk, ExeTsk, ExtRef, DfrDat, DueDat) VALUES (" + task + ", " + execution
        + ", " + reference + ", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
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

  private static String readMigration(String dialect, String file) {
    String path = "db/migration/" + dialect + "/" + file;
    try (InputStream stream = SchedulerDeferredExecutionsMigrationTest.class.getClassLoader()
        .getResourceAsStream(path)) {
      if (stream == null) {
        fail("Missing " + file + " for dialect: " + dialect);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
