package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the AweUserSettings table structure created by the schema bootstrap scripts
 * (and, in production, by the AWE_V1.2.2__user_settings.sql migrations).
 * <p>
 * Table contract: IdeUsrSet (PK), Ope varchar(20) UNIQUE NOT NULL, AvatarImage varchar(4000) NULL.
 */
@DisplayName("AweUserSettings schema tests")
@Tag("integration")
class UserSettingsSchemaTest extends AbstractSpringAppIntegrationTest {

  private static final String TABLE_NAME = "AweUserSettings";

  @Autowired
  private DataSource dataSource;

  @Test
  @DisplayName("AweUserSettings table exists with IdeUsrSet, Ope and AvatarImage columns")
  void testAweUserSettingsTableExists() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      Map<String, Integer> columns = new HashMap<>();

      try (ResultSet resultSet = metaData.getColumns(null, null, resolveTableNamePattern(metaData), null)) {
        while (resultSet.next()) {
          columns.put(resultSet.getString("COLUMN_NAME").toUpperCase(), resultSet.getInt("DATA_TYPE"));
        }
      }

      assertFalse(columns.isEmpty(), "AweUserSettings table must exist");
      assertTrue(columns.containsKey("IDEUSRSET"), "AweUserSettings must contain IdeUsrSet column");
      assertTrue(columns.containsKey("OPE"), "AweUserSettings must contain Ope column");
      assertTrue(columns.containsKey("AVATARIMAGE"), "AweUserSettings must contain AvatarImage column");
    }
  }

  @Test
  @DisplayName("AweUserSettings.Ope has a unique constraint")
  void testOpeColumnIsUnique() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      boolean opeIsUnique = false;

      try (ResultSet resultSet = metaData.getIndexInfo(null, null, resolveTableNamePattern(metaData), true, false)) {
        while (resultSet.next()) {
          String columnName = resultSet.getString("COLUMN_NAME");
          if (columnName != null && "OPE".equalsIgnoreCase(columnName)) {
            opeIsUnique = true;
          }
        }
      }

      assertTrue(opeIsUnique, "AweUserSettings.Ope must be covered by a unique constraint/index");
    }
  }

  @Test
  @DisplayName("ope table remains structurally unchanged (no avatar column added)")
  void testOpeTableUnchanged() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      boolean hasAvatarColumn = false;

      String opeTableName = metaData.storesLowerCaseIdentifiers() ? "ope" : "OPE";
      try (ResultSet resultSet = metaData.getColumns(null, null, opeTableName, null)) {
        while (resultSet.next()) {
          String columnName = resultSet.getString("COLUMN_NAME").toLowerCase();
          if (columnName.contains("avatar")) {
            hasAvatarColumn = true;
          }
        }
      }

      assertFalse(hasAvatarColumn, "ope table must not gain any avatar-related column");
    }
  }

  /**
   * Resolves the table name pattern in the exact case the driver's metadata stores identifiers in.
   * Most dialects (HSQLDB, H2) fold unquoted identifiers to upper case, while some drivers report
   * {@link DatabaseMetaData#storesLowerCaseIdentifiers()} instead.
   *
   * @param metaData Database metadata
   * @return Table name pattern matching the stored identifier case
   * @throws java.sql.SQLException Error reading metadata
   */
  private String resolveTableNamePattern(DatabaseMetaData metaData) throws java.sql.SQLException {
    if (metaData.storesLowerCaseIdentifiers()) {
      return TABLE_NAME.toLowerCase();
    }
    return TABLE_NAME.toUpperCase();
  }
}
