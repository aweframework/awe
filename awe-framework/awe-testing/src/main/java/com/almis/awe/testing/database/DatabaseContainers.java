package com.almis.awe.testing.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shared database containers of the test run.
 *
 * <p>Each database starts at most once per JVM, the first time a test asks for it, and stays up until the JVM ends
 * (Testcontainers' Ryuk removes it). With {@code testcontainers.reuse.enable=true} in the developer's
 * {@code ~/.testcontainers.properties} the container also survives between runs; CI must not set it.</p>
 */
public final class DatabaseContainers {

  static final String DATABASE_NAME = "awetestdb";
  private static final String SQLSERVER_EULA_URL = "https://go.microsoft.com/fwlink/?linkid=857698";
  // Throwaway credentials of a container that only lives for the test run (SQL Server requires a strong password)
  private static final String PASSWORD = "Awe_test_1234";

  private static final Map<TestDatabase, DatabaseConnection> STARTED = new EnumMap<>(TestDatabase.class);

  private DatabaseContainers() {
  }

  /**
   * Get the connection of the container of a database, starting it if needed.
   *
   * @param database Database
   * @return The connection, or empty when the database is embedded
   * @throws IllegalStateException when the SQL Server licence was not accepted explicitly
   */
  public static Optional<DatabaseConnection> connectionFor(TestDatabase database) {
    return connectionFor(database, System::getenv);
  }

  static synchronized Optional<DatabaseConnection> connectionFor(TestDatabase database, Function<String, String> environment) {
    if (!database.needsContainer()) {
      return Optional.empty();
    }
    if (database == TestDatabase.SQLSERVER && !TestDatabaseSettings.isSqlServerEulaAccepted(environment)) {
      throw new IllegalStateException("The SQL Server test container needs the Microsoft SQL Server EULA to be accepted explicitly ("
        + SQLSERVER_EULA_URL + "). Set " + TestDatabaseSettings.SQLSERVER_ACCEPT_EULA + "=true as a system property "
        + "(-D" + TestDatabaseSettings.SQLSERVER_ACCEPT_EULA + "=true) or as the environment variable AWE_TESTING_SQLSERVER_ACCEPT_EULA=true.");
    }
    DatabaseConnection connection = STARTED.get(database);
    if (connection == null) {
      connection = start(database);
      STARTED.put(database, connection);
    }
    return Optional.of(connection);
  }

  /**
   * Build, without starting it, the container of a database from its pinned image
   *
   * @param database Database that runs in a container
   * @return Configured container
   */
  static JdbcDatabaseContainer<?> createContainer(TestDatabase database) {
    DockerImageName image = DockerImageName.parse(TestDatabaseImages.imageFor(database));
    return switch (database) {
      case POSTGRESQL -> new PostgreSQLContainer<>(image.asCompatibleSubstituteFor("postgres"))
        .withDatabaseName(DATABASE_NAME).withUsername("postgres").withPassword(PASSWORD);
      case MYSQL -> new MySQLContainer<>(image.asCompatibleSubstituteFor("mysql"))
        .withDatabaseName(DATABASE_NAME).withUsername("root").withPassword(PASSWORD);
      case ORACLE -> new OracleContainer(image.asCompatibleSubstituteFor("gvenzl/oracle-free"))
        .withUsername("awe").withPassword(PASSWORD);
      case SQLSERVER -> new MSSQLServerContainer<>(image.asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
        .acceptLicense().withPassword(PASSWORD);
      default -> throw new IllegalArgumentException(database + " is an embedded database and has no container");
    };
  }

  private static DatabaseConnection start(TestDatabase database) {
    JdbcDatabaseContainer<?> container = createContainer(database);
    container.withReuse(true).start();
    if (database == TestDatabase.SQLSERVER) {
      return prepareSqlServer(container);
    }
    return new DatabaseConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  private static DatabaseConnection prepareSqlServer(JdbcDatabaseContainer<?> container) {
    try (Connection connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
         Statement statement = connection.createStatement()) {
      statement.execute("IF DB_ID('" + DATABASE_NAME + "') IS NULL CREATE DATABASE " + DATABASE_NAME);
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot create the " + DATABASE_NAME + " database in the SQL Server container "
        + "(is the mssql-jdbc driver in the test classpath?)", exception);
    }
    String url = "jdbc:sqlserver://" + container.getHost() + ":" + container.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT)
      + ";databaseName=" + DATABASE_NAME + ";encrypt=false;trustServerCertificate=true";
    return new DatabaseConnection(url, container.getUsername(), container.getPassword());
  }
}
