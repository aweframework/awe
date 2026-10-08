package com.almis.awe.testing.database;

import java.util.function.Function;

/**
 * Switches of the database test utility.
 *
 * <p>Each one is read, in this order, from the JVM system property and from the environment variable with the same
 * name written in upper case with underscores ({@code db.external} becomes {@code DB_EXTERNAL}).</p>
 */
public final class TestDatabaseSettings {

  /**
   * Skip the containers and use the connection the environment already provides (CI service mode)
   */
  public static final String EXTERNAL = "db.external";

  /**
   * Explicit acceptance of the Microsoft SQL Server licence (EULA) required to run its container image
   */
  public static final String SQLSERVER_ACCEPT_EULA = "awe.testing.sqlserver.accept-eula";

  private TestDatabaseSettings() {
  }

  /**
   * Whether the databases are provided by the environment instead of by containers
   *
   * @return True when external mode is on
   */
  public static boolean isExternal() {
    return isExternal(System::getenv);
  }

  static boolean isExternal(Function<String, String> environment) {
    return "true".equalsIgnoreCase(read(EXTERNAL, environment));
  }

  /**
   * Whether the SQL Server licence was explicitly accepted
   *
   * @return True only when the setting is exactly {@code true}
   */
  public static boolean isSqlServerEulaAccepted() {
    return isSqlServerEulaAccepted(System::getenv);
  }

  static boolean isSqlServerEulaAccepted(Function<String, String> environment) {
    return "true".equalsIgnoreCase(read(SQLSERVER_ACCEPT_EULA, environment));
  }

  private static String read(String name, Function<String, String> environment) {
    String value = System.getProperty(name);
    if (value == null) {
      value = environment.apply(name.replace('.', '_').replace('-', '_').toUpperCase());
    }
    return value == null ? null : value.trim();
  }
}
