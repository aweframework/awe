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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guard for the migration that unifies the FTP credentials into the external
 * server row.
 *
 * <p>The launcher credential columns are dropped by this migration, so the
 * carry-over is irreversible: every dialect must ship the same migration, and
 * the credentials must reach {@code AweSchSrv} BEFORE the columns disappear.
 * The behaviour assertions run the real migration against HSQLDB.</p>
 */
class SchedulerFtpCredentialsMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.5__Unify_ftp_credentials_into_server.sql";

  private static final String[] DIALECTS = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"};

  /**
   * The migration file must exist, with the SAME name, for every dialect
   * folder the scheduler starter ships migrations for.
   *
   * @param dialect Dialect folder
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationFileExistsForDialect(String dialect) {
    assertTrue(resourceExists(dialect), "Missing " + MIGRATION_FILE + " for dialect: " + dialect);
  }

  /**
   * Every dialect must carry the credentials over before removing the source
   * columns, otherwise the data is lost.
   *
   * @param dialect Dialect folder
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationCarriesCredentialsBeforeDroppingColumns(String dialect) {
    String content = readStatements(dialect);

    int carryOver = content.indexOf("UPDATE");
    int firstDrop = content.indexOf("DROP");

    assertTrue(carryOver >= 0, dialect + ": migration must carry the launcher credentials over to the server row");
    assertTrue(firstDrop >= 0, dialect + ": migration must drop the launcher credential columns");
    assertTrue(carryOver < firstDrop, dialect + ": credentials must be carried over before the columns are dropped");
  }

  /**
   * Both the live launcher table and its audit counterpart must lose the
   * credential columns — never just one of the two.
   *
   * @param dialect Dialect folder
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationDropsColumnsFromBothLauncherTables(String dialect) {
    String content = readStatements(dialect);
    String afterCarryOver = content.substring(content.indexOf("DROP"));

    assertTrue(afterCarryOver.contains("HISAWESCHTSKLCH"), dialect + ": migration must drop the columns from HISAweSchTskLch");
    assertTrue(afterCarryOver.contains("AWESCHTSKLCH"), dialect + ": migration must drop the columns from AweSchTskLch");
    assertTrue(afterCarryOver.contains("SRVUSR"), dialect + ": migration must drop SrvUsr");
    assertTrue(afterCarryOver.contains("SRVPWD"), dialect + ": migration must drop SrvPwd");
  }

  /**
   * The carry-over must never touch the SSH private key columns: they are
   * meaningless for FTP and belong to the server's own configuration.
   */
  @Test
  void migrationNeverTouchesPrivateKeyColumns() {
    for (String dialect : DIALECTS) {
      assertFalse(readStatements(dialect).contains("SSHKEY"), dialect + ": migration must not touch the SSH key columns");
    }
    assertEquals(6, DIALECTS.length);
  }

  /**
   * When several tasks point at the same FTP server with different
   * credentials, the lowest launcher id with a non-empty user wins.
   *
   * @throws SQLException Test error
   */
  @Test
  void conflictingTaskCredentialsResolveToLowestLauncherId() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ftp", null, null);
      insertLauncher(connection, 30, 1, "userC", "pwdC");
      insertLauncher(connection, 10, 1, "userA", "pwdA");
      insertLauncher(connection, 20, 1, "userB", "pwdB");

      runMigration(connection);

      assertEquals("userA", serverUser(connection, 1));
      assertEquals("pwdA", serverPassword(connection, 1));
    }
  }

  /**
   * A launcher whose user is empty means anonymous access, so it must never
   * win over a later launcher that does carry credentials.
   *
   * @throws SQLException Test error
   */
  @Test
  void emptyLauncherUserIsSkippedInFavourOfARealOne() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ftp", null, null);
      insertLauncher(connection, 10, 1, "", "");
      insertLauncher(connection, 20, 1, "userB", "pwdB");

      runMigration(connection);

      assertEquals("userB", serverUser(connection, 1));
      assertEquals("pwdB", serverPassword(connection, 1));
    }
  }

  /**
   * A server that already carries credentials must keep them: the launcher
   * values never overwrite an explicit server configuration.
   *
   * @throws SQLException Test error
   */
  @Test
  void serverWithExistingCredentialsIsNotOverwritten() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ftp", "keepMe", "keepPwd");
      insertLauncher(connection, 10, 1, "ignored", "ignoredPwd");

      runMigration(connection);

      assertEquals("keepMe", serverUser(connection, 1));
      assertEquals("keepPwd", serverPassword(connection, 1));
    }
  }

  /**
   * Only FTP servers are migrated: an SSH server already owns its credentials
   * and must not inherit a launcher's.
   *
   * @throws SQLException Test error
   */
  @Test
  void nonFtpServersAreNeverMigrated() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ssh", null, null);
      insertLauncher(connection, 10, 1, "sshUser", "sshPwd");

      runMigration(connection);

      assertNull(serverUser(connection, 1));
      assertNull(serverPassword(connection, 1));
    }
  }

  /**
   * An FTP server whose launchers configure no user stays credential-free,
   * preserving anonymous access.
   *
   * @throws SQLException Test error
   */
  @Test
  void ftpServerWithoutLauncherCredentialsStaysEmpty() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ftp", null, null);
      insertLauncher(connection, 10, 1, "", "");
      insertServer(connection, 2, "ftp", null, null);
      insertLauncher(connection, 20, 2, null, null);
      insertServer(connection, 3, "ftp", null, null);

      runMigration(connection);

      for (int serverId : new int[]{1, 2, 3}) {
        assertNull(serverUser(connection, serverId), "server " + serverId + " must stay credential-free");
        assertNull(serverPassword(connection, serverId), "server " + serverId + " must stay credential-free");
      }
    }
  }

  /**
   * Once the credentials live on the server row, the launcher columns must be
   * gone so nothing can keep writing to them.
   *
   * @throws SQLException Test error
   */
  @Test
  void launcherCredentialColumnsAreRemoved() throws SQLException {
    try (Connection connection = memoryDatabase()) {
      createSchema(connection);
      insertServer(connection, 1, "ftp", null, null);
      insertLauncher(connection, 10, 1, "userA", "pwdA");

      runMigration(connection);

      for (String table : new String[]{"AweSchTskLch", "HISAweSchTskLch"}) {
        for (String column : new String[]{"SrvUsr", "SrvPwd"}) {
          assertThrows(SQLException.class,
              () -> query(connection, "SELECT " + column + " FROM " + table),
              table + "." + column + " must no longer exist");
        }
      }
    }
  }

  private static Connection memoryDatabase() throws SQLException {
    return DriverManager.getConnection("jdbc:hsqldb:mem:" + UUID.randomUUID() + ";shutdown=true", "sa", "");
  }

  private static void createSchema(Connection connection) throws SQLException {
    execute(connection, "CREATE TABLE AweSchSrv (Ide INTEGER NOT NULL, Nom VARCHAR(40), Pro VARCHAR(10), Hst VARCHAR(40),"
        + " Prt VARCHAR(10), Act INTEGER, SshUsr VARCHAR(200), SshPwd VARCHAR(200), SshKey VARCHAR(4000), SshKeyPass VARCHAR(200))");
    execute(connection, "CREATE TABLE AweSchTskLch (Ide INTEGER NOT NULL, IdeTsk INTEGER, IdSrv INTEGER, Pth VARCHAR(250),"
        + " SrvUsr VARCHAR(200), SrvPwd VARCHAR(200))");
    execute(connection, "CREATE TABLE HISAweSchTskLch (HISope VARCHAR(20), Ide INTEGER, IdSrv INTEGER,"
        + " SrvUsr VARCHAR(200), SrvPwd VARCHAR(200))");
  }

  private static void insertServer(Connection connection, int serverId, String protocol, String user, String password) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act, SshUsr, SshPwd) VALUES (?, ?, ?, 'host', '21', 1, ?, ?)")) {
      statement.setInt(1, serverId);
      statement.setString(2, "server-" + serverId);
      statement.setString(3, protocol);
      statement.setString(4, user);
      statement.setString(5, password);
      statement.executeUpdate();
    }
  }

  private static void insertLauncher(Connection connection, int launcherId, int serverId, String user, String password) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO AweSchTskLch (Ide, IdeTsk, IdSrv, Pth, SrvUsr, SrvPwd) VALUES (?, ?, ?, '/path', ?, ?)")) {
      statement.setInt(1, launcherId);
      statement.setInt(2, launcherId * 10);
      statement.setInt(3, serverId);
      statement.setString(4, user);
      statement.setString(5, password);
      statement.executeUpdate();
    }
  }

  private static void runMigration(Connection connection) throws SQLException {
    for (String statement : readMigration("hsqldb").split(";")) {
      String sql = stripComments(statement);
      if (!sql.isEmpty()) {
        execute(connection, sql);
      }
    }
  }

  /**
   * The migration's executable statements, upper-cased, with the leading
   * documentation comments removed so statement order can be asserted.
   *
   * @param dialect Dialect folder
   * @return Uppercase SQL without comment lines
   */
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

  private static String serverUser(Connection connection, int serverId) throws SQLException {
    return serverCredential(connection, serverId, "SshUsr");
  }

  private static String serverPassword(Connection connection, int serverId) throws SQLException {
    return serverCredential(connection, serverId, "SshPwd");
  }

  private static String serverCredential(Connection connection, int serverId, String column) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("SELECT " + column + " FROM AweSchSrv WHERE Ide = ?")) {
      statement.setInt(1, serverId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertTrue(resultSet.next(), "server " + serverId + " must still exist");
        return resultSet.getString(1);
      }
    }
  }

  private static void query(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeQuery(sql);
    }
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static boolean resourceExists(String dialect) {
    try (InputStream stream = classLoader().getResourceAsStream(resourcePath(dialect))) {
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
    return SchedulerFtpCredentialsMigrationTest.class.getClassLoader();
  }
}
