package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Flyway migration {@code SCHEDULER_V1.0.6__Execution_log_window.sql} applies
 * cleanly on top of {@code SCHEDULER_V1.0.5} (FTP credential unification) and that the
 * {@code AweSchExeLog} table lands with its unique and secondary indexes (design section 6).
 */
@Tag("HSQL-Flyway")
@TestPropertySource(locations = {"classpath:test-flyway.properties"})
class SchedulerExecutionLogSchemaTest extends AbstractSpringAppIntegrationTest {

  private static final String TABLE_NAME = "AweSchExeLog";

  @Autowired
  private DataSource dataSource;

  @Test
  void schedulerFlywayHistoryRecordsTheExecutionLogWindowMigrationAsSuccessful() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    Integer success = jdbcTemplate.queryForObject(
      "SELECT \"success\" FROM \"flyway_schema_SCHEDULER\" WHERE \"version\" = ?", Integer.class, "1.0.6");

    assertEquals(1, success, "SCHEDULER_V1.0.6 must be recorded as a successfully applied migration");
  }

  @Test
  void aweSchExeLogTableExistsWithTheExpectedColumns() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      Set<String> columns = new HashSet<>();

      try (ResultSet resultSet = metaData.getColumns(null, null, resolveTableNamePattern(metaData), null)) {
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME").toUpperCase());
        }
      }

      assertTrue(columns.contains("IDETSK"), "AweSchExeLog must contain IdeTsk");
      assertTrue(columns.contains("EXETSK"), "AweSchExeLog must contain ExeTsk");
      assertTrue(columns.contains("SEC"), "AweSchExeLog must contain Sec");
      assertTrue(columns.contains("SLT"), "AweSchExeLog must contain Slt");
      assertTrue(columns.contains("LINNUM"), "AweSchExeLog must contain LinNum");
      assertTrue(columns.contains("LINTXT"), "AweSchExeLog must contain LinTxt");
    }
  }

  @Test
  void aweSchExeLogHasTheUniquePrimaryKeyIndexOnTaskExecutionSectionSlot() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      boolean hasUniqueIndex = false;

      try (ResultSet resultSet = metaData.getIndexInfo(null, null, resolveTableNamePattern(metaData), true, false)) {
        while (resultSet.next()) {
          String indexName = resultSet.getString("INDEX_NAME");
          if (indexName != null && indexName.equalsIgnoreCase("PK_AWESCHEXELOG")) {
            hasUniqueIndex = true;
          }
        }
      }

      assertTrue(hasUniqueIndex, "AweSchExeLog must have the PK_AWESCHEXELOG unique index");
    }
  }

  @Test
  void aweSchExeLogHasTheSecondaryIndexOnTaskExecutionLineNumber() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      boolean hasSecondaryIndex = false;

      try (ResultSet resultSet = metaData.getIndexInfo(null, null, resolveTableNamePattern(metaData), false, false)) {
        while (resultSet.next()) {
          String indexName = resultSet.getString("INDEX_NAME");
          if (indexName != null && indexName.equalsIgnoreCase("AWESCHEXELOGI1")) {
            hasSecondaryIndex = true;
          }
        }
      }

      assertTrue(hasSecondaryIndex, "AweSchExeLog must have the AWESCHEXELOGI1 secondary index");
    }
  }

  /**
   * Resolves the table name pattern in the exact case the driver's metadata stores identifiers
   * in, following {@code UserSettingsSchemaTest}'s precedent.
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
