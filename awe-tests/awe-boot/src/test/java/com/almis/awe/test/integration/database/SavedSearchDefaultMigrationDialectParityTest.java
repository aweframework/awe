package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the AWE_V1.6.1__default_saved_search.sql migration is present, adds the same columns and check for the default
 * saved searches (#915), and can be applied again (every change is guarded with the idiom of its dialect) in the six
 * supported dialect folders (h2, hsqldb, mysql, oracle, postgresql, sqlserver).
 */
@DisplayName("Default saved search migration dialect parity")
@Tag("integration")
class SavedSearchDefaultMigrationDialectParityTest {

  private static final String MIGRATION_FILE = "AWE_V1.6.1__default_saved_search.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  @ParameterizedTest(name = "{0} dialect adds DefSavSch, makes AdvOpn nullable and adds Def with its check")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  @DisplayName("Each dialect migration adds the columns of the default searches and the check of the profile default")
  void testDialectMigrationContainsExpectedStructure(String dialect) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase().replaceAll("\\s+", " ");

    for (String needle : new String[]{"aweusrwincnf", "defsavsch", "advopn", "awesavsch", "ck_awesavsch_def",
      "check (def = 0 or (def = 1 and pro is not null))",
      "the script can be applied again: a column or a constraint is only added when it is missing"}) {
      assertTrue(content.contains(needle), dialect + ": migration must contain " + needle);
    }
    // A search is not the default of its profile unless it is marked
    String defaultColumn = switch (dialect) {
      case "oracle" -> "def number(1) default 0 not null";
      case "sqlserver" -> "def int not null constraint df_awesavsch_def default 0";
      default -> "def int default 0 not null";
    };
    assertTrue(content.contains(defaultColumn), dialect + ": migration must add " + defaultColumn);
  }

  @ParameterizedTest(name = "{0} dialect guards the columns, the nullable AdvOpn and the check with its idiom")
  @CsvSource(delimiter = '|', value = {
    "h2|add column if not exists defsavsch|alter column advopn set null|add column if not exists def |add constraint if not exists ck_awesavsch_def",
    "hsqldb|add column if not exists defsavsch|alter column advopn set null|add column if not exists def |add constraint if not exists ck_awesavsch_def",
    "postgresql|add column if not exists defsavsch|alter column advopn drop not null|add column if not exists def |if not exists(select * from pg_constraint where lower(conname) = 'ck_awesavsch_def')",
    "mysql|lower(column_name) = 'defsavsch'|modify advopn int null|lower(column_name) = 'def'|lower(constraint_name) = 'ck_awesavsch_def'",
    "oracle|-1430|if sqlcode != ignored_code then|-1451|-2264",
    "sqlserver|column_name = 'defsavsch'|alter column advopn int null|column_name = 'def'|from sys.check_constraints where name = 'ck_awesavsch_def'"
  })
  @DisplayName("Each dialect migration can be applied again: every change is guarded or changes nothing the second time")
  void testDialectMigrationIsReapplicable(String dialect, String defaultSearchGuard, String advancedOpen, String defaultGuard,
                                          String checkGuard) throws IOException {
    String content = readMigrationFile(dialect).toLowerCase().replaceAll("\\s+", " ");

    for (String needle : new String[]{defaultSearchGuard, advancedOpen, defaultGuard, checkGuard}) {
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
