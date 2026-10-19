package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the AWE_V1.3.1__grid_layout.sql migration is present, and defines the same table, in the six supported
 * dialect folders (h2, hsqldb, mysql, oracle, postgresql, sqlserver).
 */
@DisplayName("AweUsrGrdCnf migration dialect parity")
@Tag("integration")
class GridLayoutMigrationDialectParityTest {

  private static final String MIGRATION_FILE = "AWE_V1.3.1__grid_layout.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  @ParameterizedTest(name = "{0} dialect defines AweUsrGrdCnf with its columns and unique key")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  @DisplayName("Each dialect migration defines the table, its columns and the unique key")
  void testDialectMigrationContainsExpectedStructure(String dialect) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase();

    for (String needle : new String[]{"aweusrgrdcnf", "ideusrgrdcnf", "ope", "scr", "grdid", "lay", "uk_aweusrgrdcnf", "unique (ope, scr, grdid)"}) {
      assertTrue(content.contains(needle), dialect + ": migration must contain " + needle);
    }
  }

  private String readMigrationFile(String dialect) throws IOException {
    File file = new File(new File(MIGRATION_BASE_PATH + dialect), MIGRATION_FILE);
    if (!file.exists()) {
      fail("Migration file not found for dialect '" + dialect + "': " + file.getAbsolutePath());
    }
    return Files.readString(file.toPath());
  }
}
