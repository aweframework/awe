package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the AweUsrGrdCnf table created by the schema bootstrap scripts (and, in production, by the
 * AWE_V1.3.1__grid_layout.sql migrations): IdeUsrGrdCnf (PK), Ope, Scr, GrdId and Lay, unique on Ope + Scr + GrdId.
 */
@DisplayName("AweUsrGrdCnf schema tests")
@Tag("integration")
class GridLayoutSchemaTest extends AbstractSpringAppIntegrationTest {

  private static final String TABLE_NAME = "AweUsrGrdCnf";

  @Autowired
  private DataSource dataSource;

  @Test
  @DisplayName("The table exists with its columns")
  void tableExists() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      Set<String> columns = new HashSet<>();
      try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName(connection.getMetaData()), null)) {
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME").toUpperCase());
        }
      }

      assertFalse(columns.isEmpty(), "AweUsrGrdCnf table must exist");
      assertTrue(columns.containsAll(Set.of("IDEUSRGRDCNF", "OPE", "SCR", "GRDID", "LAY")), "Columns found: " + columns);
    }
  }

  @Test
  @DisplayName("There is one layout per user, screen and grid")
  void layoutIsUniquePerUserScreenAndGrid() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        insert(connection, 9001, "user", "Screen", "Grid");
        insert(connection, 9002, "user", "Screen", "OtherGrid");
        insert(connection, 9003, "user", "OtherScreen", "Grid");
        insert(connection, 9004, "other", "Screen", "Grid");

        assertThrows(SQLException.class, () -> insert(connection, 9005, "user", "Screen", "Grid"));
      } finally {
        connection.rollback();
      }
    }
  }

  private void insert(Connection connection, int id, String user, String screen, String grid) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
      "INSERT INTO AweUsrGrdCnf (IdeUsrGrdCnf, Ope, Scr, GrdId, Lay) VALUES (?, ?, ?, ?, '{}')")) {
      statement.setInt(1, id);
      statement.setString(2, user);
      statement.setString(3, screen);
      statement.setString(4, grid);
      statement.executeUpdate();
    }
  }

  /**
   * Name of the table as the database stores it: the case it was created with, upper or lower case, whichever the
   * metadata of the driver finds (MySQL with lower_case_table_names=0 keeps the case of the script)
   */
  private String tableName(DatabaseMetaData metaData) throws SQLException {
    for (String candidate : new String[]{TABLE_NAME, TABLE_NAME.toUpperCase(), TABLE_NAME.toLowerCase()}) {
      try (ResultSet resultSet = metaData.getColumns(null, null, candidate, null)) {
        if (resultSet.next()) {
          return candidate;
        }
      }
    }
    return TABLE_NAME;
  }
}
