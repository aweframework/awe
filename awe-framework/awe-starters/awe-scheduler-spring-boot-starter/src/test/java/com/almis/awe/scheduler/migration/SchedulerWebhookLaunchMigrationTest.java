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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guard for the persistence of the webhook launch: every dialect creates the hook and inbox tables and adds the
 * pointer column of the task parameters.
 */
class SchedulerWebhookLaunchMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.10__Webhook_launch.sql";

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationCreatesTheHookTablesAndThePointerColumn(String dialect) {
    String content = stripComments(readMigration(dialect, MIGRATION_FILE)).toUpperCase(Locale.ROOT);

    for (String name : new String[]{"AWESCHTSKHOK", "AWESCHHOKMSG", "HOKKEY", "SECPRP", "TYPFLT", "MSGID", "RCVDAT",
      "NXTDAT", "PRCDAT", "ATT", "PAY", "STA", "ERR", "IDERUN", "WHKPTR", "ALTER TABLE"}) {
      assertTrue(content.contains(name), dialect + ": missing " + name);
    }
    assertEquals(2, content.split("CREATE TABLE", -1).length - 1, dialect + ": two tables");
  }

  @Test
  void theInboxDeduplicatesAndTheHookKeyIsUnique() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
      "jdbc:hsqldb:mem:" + UUID.randomUUID() + ";shutdown=true", "sa", "")) {
      execute(connection, "CREATE TABLE AweSchTskPar (Ide INTEGER NOT NULL, Nam VARCHAR(40) NOT NULL)");
      for (String statement : readMigration("hsqldb", MIGRATION_FILE).split(";")) {
        String sql = stripComments(statement);
        if (!sql.isEmpty()) {
          execute(connection, sql);
        }
      }

      execute(connection, "INSERT INTO AweSchTskPar (Ide, Nam, WhkPtr) VALUES (1, 'a', '/data/a')");
      execute(connection, "INSERT INTO AweSchTskPar (Ide, Nam) VALUES (2, 'b')");
      execute(connection, hook(1, "orders"));
      assertThrows(SQLException.class, () -> execute(connection, hook(2, "orders")), "one task per hook key");
      execute(connection, message("orders", "msg-1"));
      execute(connection, message("other", "msg-1"));
      assertThrows(SQLException.class, () -> execute(connection, message("orders", "msg-1")),
        "one row per source and message id");
    }
  }

  private static String hook(int task, String key) {
    return "INSERT INTO AweSchTskHok (IdeTsk, HokKey, Act, SecPrp) VALUES (" + task + ", '" + key
      + "', 1, 'x.secret')";
  }

  private static String message(String source, String id) {
    return "INSERT INTO AweSchHokMsg (Src, MsgId, RcvDat, Pay, Sta, Att, NxtDat) VALUES ('" + source + "', '" + id
      + "', CURRENT_TIMESTAMP, '{}', 'RECEIVED', 0, CURRENT_TIMESTAMP)";
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
    try (InputStream stream = SchedulerWebhookLaunchMigrationTest.class.getClassLoader()
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
