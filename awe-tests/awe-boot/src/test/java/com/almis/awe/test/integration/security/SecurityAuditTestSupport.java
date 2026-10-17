package com.almis.awe.test.integration.security;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers of the security audit integration tests: throw-away copies of the test user and access to the stored events
 */
final class SecurityAuditTestSupport {

  private final DataSource dataSource;

  SecurityAuditTestSupport(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Create a copy of the user {@code test} (password {@code test}) with another name and identifier
   *
   * @param name New user name
   * @return Identifier of the new user
   */
  int copyTestUser(String name) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      int id;
      try (PreparedStatement max = connection.prepareStatement("SELECT MAX(IdeOpe) FROM ope");
           ResultSet result = max.executeQuery()) {
        result.next();
        id = result.getInt(1) + 1000;
      }
      Map<String, Object> row = new HashMap<>();
      List<String> columns = new ArrayList<>();
      try (PreparedStatement select = connection.prepareStatement("SELECT * FROM ope WHERE l1_nom = 'test'");
           ResultSet result = select.executeQuery()) {
        result.next();
        ResultSetMetaData metaData = result.getMetaData();
        for (int column = 1; column <= metaData.getColumnCount(); column++) {
          columns.add(metaData.getColumnName(column));
          row.put(metaData.getColumnName(column).toUpperCase(), result.getObject(column));
        }
      }
      row.put("IDEOPE", id);
      row.put("L1_NOM", name);
      row.put("PWDLCK", 0);
      row.put("NUMLOG", 0);
      row.put("ENABLE2FA", 1);
      row.put("SECRET2FA", "SECRET");
      String sql = "INSERT INTO ope (" + String.join(", ", columns) + ") VALUES (" + "?, ".repeat(columns.size() - 1) + "?)";
      try (PreparedStatement insert = connection.prepareStatement(sql)) {
        for (int column = 0; column < columns.size(); column++) {
          insert.setObject(column + 1, row.get(columns.get(column).toUpperCase()));
        }
        insert.executeUpdate();
      }
      commit(connection);
      return id;
    }
  }

  /**
   * Delete a user copy and its events
   */
  void deleteUser(String name) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement user = connection.prepareStatement("DELETE FROM ope WHERE l1_nom = ?");
         PreparedStatement events = connection.prepareStatement("DELETE FROM AweSecAudEvt WHERE UsrNam = ?")) {
      user.setString(1, name);
      user.executeUpdate();
      events.setString(1, name);
      events.executeUpdate();
      commit(connection);
    }
  }

  /**
   * Delete the events of a user since a moment
   */
  void deleteEvents(String user, Instant since) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement events = connection.prepareStatement("DELETE FROM AweSecAudEvt WHERE UsrNam = ? AND EvtDat >= ?")) {
      events.setString(1, user);
      events.setTimestamp(2, Timestamp.from(since));
      events.executeUpdate();
      commit(connection);
    }
  }

  /**
   * Events of a user since a moment, oldest first, as TYPE/OUTCOME/CAUSE/CHANNEL
   */
  List<String> events(String user, Instant since) throws Exception {
    List<String> events = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT EvtTyp, EvtRes, EvtCau, Chn FROM AweSecAudEvt WHERE UsrNam = ? AND EvtDat >= ? ORDER BY EvtDat")) {
      statement.setString(1, user);
      statement.setTimestamp(2, Timestamp.from(since));
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          events.add(result.getString(1) + "/" + result.getString(2) + "/" + result.getString(3) + "/" + result.getString(4));
        }
      }
    }
    return events;
  }

  /**
   * Lock flag and failed logins of a user
   */
  int[] lockStatus(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT PwdLck, NumLog FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, user);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return new int[]{result.getInt(1), result.getInt(2)};
      }
    }
  }

  /**
   * Values of a user row by upper-case column name
   */
  Map<String, Object> user(String user) throws Exception {
    Map<String, Object> row = new HashMap<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, user);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
          row.put(result.getMetaData().getColumnName(column).toUpperCase(), result.getObject(column));
        }
      }
    }
    return row;
  }

  /**
   * Number of events of a type and cause since a moment, whatever the user
   */
  int count(String type, String cause, Instant since) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT COUNT(*) FROM AweSecAudEvt WHERE EvtTyp = ? AND EvtCau = ? AND EvtDat >= ?")) {
      statement.setString(1, type);
      statement.setString(2, cause);
      statement.setTimestamp(3, Timestamp.from(since));
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    }
  }

  private static void commit(Connection connection) throws Exception {
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }
}
