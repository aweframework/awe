package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the AWE_V1.5.1__window_advanced_state.sql migration is present, defines the same table, and can be applied
 * again (every object is guarded with the idiom of its dialect) in the six supported dialect folders (h2, hsqldb,
 * mysql, oracle, postgresql, sqlserver).
 */
@DisplayName("AweUsrWinCnf migration dialect parity")
@Tag("integration")
class WindowStateMigrationDialectParityTest {

  private static final String MIGRATION_FILE = "AWE_V1.5.1__window_advanced_state.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  @ParameterizedTest(name = "{0} dialect defines AweUsrWinCnf with its columns and unique key")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  @DisplayName("Each dialect migration defines the table, its columns, the unique key and the sequence key")
  void testDialectMigrationContainsExpectedStructure(String dialect) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase();

    for (String needle : new String[]{"aweusrwincnf", "ideusrwincnf", "ope", "scr", "winid", "advopn", "uk_aweusrwincnf",
      "unique (ope, scr, winid)", "'wincnfkey'",
      "the script can be applied again: every object is created only when it does not exist."}) {
      assertTrue(content.contains(needle), dialect + ": migration must contain " + needle);
    }
  }

  @ParameterizedTest(name = "{0} dialect guards the table and the key with its idiom")
  @CsvSource(delimiter = '|', value = {
    "h2|create table if not exists aweusrwincnf|where not exists(select * from awekey where keynam = 'wincnfkey')",
    "hsqldb|create table if not exists aweusrwincnf|where not exists(select * from awekey where keynam = 'wincnfkey')",
    "mysql|create table if not exists aweusrwincnf|where not exists(select * from awekey where keynam = 'wincnfkey')",
    "postgresql|create table if not exists aweusrwincnf|where not exists(select * from awekey where keynam = 'wincnfkey')",
    "oracle|if sqlcode != -955 then|where not exists(select * from awekey where keynam = 'wincnfkey')",
    "sqlserver|where name = 'aweusrwincnf'|where keynam = 'wincnfkey')"
  })
  @DisplayName("Each dialect migration can be applied again: the table and the key are created only when absent")
  void testDialectMigrationIsReapplicable(String dialect, String tableGuard, String keyGuard) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase().replaceAll("\\s+", " ");

    assertTrue(content.contains(tableGuard), dialect + ": the table must be guarded with " + tableGuard);
    assertTrue(content.contains(keyGuard), dialect + ": the sequence key must be guarded with " + keyGuard);
    // One table and one key, no unguarded statement
    assertEquals(1, count(content, "create table"), dialect + ": one table");
    assertEquals(1, count(content, "insert into awekey"), dialect + ": one sequence key");
    if ("oracle".equals(dialect)) {
      assertTrue(content.contains("execute immediate 'create table aweusrwincnf"), "oracle: the table is created inside the guarded block");
    }
    if ("sqlserver".equals(dialect)) {
      assertTrue(content.contains("if not exists(select * from sys.tables where name = 'aweusrwincnf' and type = 'u') create table aweusrwincnf"),
        "sqlserver: the table is created only when it does not exist");
      assertTrue(content.contains("if not exists(select * from awekey where keynam = 'wincnfkey') insert into awekey"),
        "sqlserver: the key is inserted only when it does not exist");
    }
  }

  private static int count(String content, String needle) {
    int count = 0;
    for (int index = content.indexOf(needle); index >= 0; index = content.indexOf(needle, index + 1)) {
      count++;
    }
    return count;
  }

  private String readMigrationFile(String dialect) throws IOException {
    File file = new File(new File(MIGRATION_BASE_PATH + dialect), MIGRATION_FILE);
    if (!file.exists()) {
      fail("Migration file not found for dialect '" + dialect + "': " + file.getAbsolutePath());
    }
    return Files.readString(file.toPath());
  }
}
