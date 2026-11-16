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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guard for the flow run migration (V1.0.9): the three run tables and the run id column of the executions ship in
 * every dialect, and the HSQLDB script applies on top of the base execution table.
 */
class SchedulerFlowRunMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.9__Flow_runs.sql";

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void everyDialectCreatesTheRunTablesAndTheExecutionColumn(String dialect) {
    String sql = read(dialect).toUpperCase(Locale.ROOT).replace("\"", "").replaceAll("[\\s]+", " ");

    assertEquals(1, count(sql, "CREATE TABLE IF NOT EXISTS AWESCHRUN (") + count(sql, "CREATE TABLE AWESCHRUN ("),
      dialect + ": AweSchRun");
    assertEquals(1, count(sql, "CREATE TABLE IF NOT EXISTS AWESCHRUNPAR (") + count(sql, "CREATE TABLE AWESCHRUNPAR ("),
      dialect + ": AweSchRunPar");
    assertEquals(1, count(sql, "CREATE TABLE IF NOT EXISTS AWESCHRUNTSK (") + count(sql, "CREATE TABLE AWESCHRUNTSK ("),
      dialect + ": AweSchRunTsk");
    assertEquals(1, count(sql, "ALTER TABLE AWESCHEXE"), dialect + ": AweSchExe.IdeRun");
  }

  @Test
  void hsqldbScriptAppliesOnTopOfTheExecutionTable() throws SQLException {
    String url = "jdbc:hsqldb:mem:flowrun" + UUID.randomUUID();
    try (Connection connection = DriverManager.getConnection(url, "SA", "");
         Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE AweSchExe (IdeTsk INTEGER NOT NULL, ExeTsk INTEGER NOT NULL)");
      for (String command : read("hsqldb").split(";")) {
        if (!command.replaceAll("(?m)^--.*$", "").isBlank()) {
          statement.execute(command);
        }
      }
      statement.execute("INSERT INTO AweSchRunTsk VALUES ('r', 1, 'WAITING', NULL, CURRENT_TIMESTAMP)");
      assertThrows(SQLException.class,
        () -> statement.execute("INSERT INTO AweSchRunTsk VALUES ('r', 1, 'WAITING', NULL, CURRENT_TIMESTAMP)"));
      assertNotNull(statement.executeQuery("SELECT IdeRun FROM AweSchExe"));
    }
  }

  private static long count(String content, String needle) {
    return content.split(java.util.regex.Pattern.quote(needle), -1).length - 1L;
  }

  private static String read(String dialect) {
    try (InputStream stream = SchedulerFlowRunMigrationTest.class.getClassLoader()
      .getResourceAsStream("db/migration/" + dialect + "/" + MIGRATION_FILE)) {
      assertNotNull(stream, "Missing " + MIGRATION_FILE + " for " + dialect);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
