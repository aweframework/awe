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
 * Verifies the AWE_V1.5.2__saved_searches.sql migration is present, defines the same table of saved searches (#790),
 * and can be applied again (every object is guarded with the idiom of its dialect) in the six supported dialect folders (h2, hsqldb,
 * mysql, oracle, postgresql, sqlserver).
 */
@DisplayName("AweSavSch migration dialect parity")
@Tag("integration")
class SavedSearchMigrationDialectParityTest {

  private static final String MIGRATION_FILE = "AWE_V1.5.2__saved_searches.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  @ParameterizedTest(name = "{0} dialect defines AweSavSch with its columns, unique key and owner check")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  @DisplayName("Each dialect migration defines the table, its columns, the unique key, the owner check and the sequence key")
  void testDialectMigrationContainsExpectedStructure(String dialect) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase();

    for (String needle : new String[]{"awesavsch", "idesavsch", "ope", "pro", "scr", "winid", "nam", "val", "uk_awesavsch",
      "unique (ope, pro, scr, winid, nam)", "ck_awesavsch_own", "'savschkey'",
      "the script can be applied again: every object is created only when it does not exist."}) {
      assertTrue(content.contains(needle), dialect + ": migration must contain " + needle);
    }
    // A saved search belongs to a user or is published for a profile, never both nor none (an empty value is not set)
    String check = "check ((ope is null and pro is not null and pro <> '') or (ope is not null and ope <> '' and pro is null))";
    // Oracle creates the table inside a quoted statement, where each quote is doubled
    assertTrue(content.replaceAll("\\s+", " ").replace("''''", "''").contains(check), dialect + ": exactly one of Ope and Pro is set");
  }

  @ParameterizedTest(name = "{0} dialect guards the table and the key with its idiom")
  @CsvSource(delimiter = '|', value = {
    "h2|create table if not exists awesavsch|where not exists(select * from awekey where keynam = 'savschkey')",
    "hsqldb|create table if not exists awesavsch|where not exists(select * from awekey where keynam = 'savschkey')",
    "mysql|create table if not exists awesavsch|where not exists(select * from awekey where keynam = 'savschkey')",
    "postgresql|create table if not exists awesavsch|where not exists(select * from awekey where keynam = 'savschkey')",
    "oracle|if sqlcode != -955 then|where not exists(select * from awekey where keynam = 'savschkey')",
    "sqlserver|where name = 'awesavsch'|where keynam = 'savschkey')"
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
      assertTrue(content.contains("execute immediate 'create table awesavsch"), "oracle: the table is created inside the guarded block");
    }
    if ("sqlserver".equals(dialect)) {
      assertTrue(content.contains("if not exists(select * from sys.tables where name = 'awesavsch' and type = 'u') create table awesavsch"),
        "sqlserver: the table is created only when it does not exist");
      assertTrue(content.contains("if not exists(select * from awekey where keynam = 'savschkey') insert into awekey"),
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
