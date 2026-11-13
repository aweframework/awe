package com.almis.awe.testing.database;

/**
 * JDBC connection data of a test database
 *
 * @param jdbcUrl  JDBC url
 * @param username User name
 * @param password Password
 */
public record DatabaseConnection(String jdbcUrl, String username, String password) {

  @Override
  public String toString() {
    return "DatabaseConnection[jdbcUrl=" + jdbcUrl + ", username=" + username + ", password=****]";
  }
}
