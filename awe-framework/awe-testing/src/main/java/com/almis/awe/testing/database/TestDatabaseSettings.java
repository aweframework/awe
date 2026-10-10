package com.almis.awe.testing.database;

import java.util.function.Function;

/**
 * Switches of the database test utility.
 *
 * <p>Each one is read, in this order, from the JVM system property and from the environment variable with the same
 * name written in upper case with underscores ({@code awe.testing.sqlserver.accept-eula} becomes {@code AWE_TESTING_SQLSERVER_ACCEPT_EULA}).</p>
 */
public final class TestDatabaseSettings {

  /**
   * Explicit acceptance of the Microsoft SQL Server licence (EULA) required to run its container image
   */
  public static final String SQLSERVER_ACCEPT_EULA = "awe.testing.sqlserver.accept-eula";

  private TestDatabaseSettings() {
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
