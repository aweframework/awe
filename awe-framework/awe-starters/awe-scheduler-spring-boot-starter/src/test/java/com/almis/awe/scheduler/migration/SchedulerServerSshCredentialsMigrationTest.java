package com.almis.awe.scheduler.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Schema guard for the additive SSH credential columns on the scheduler
 * server table.
 *
 * <p>Verifies that EVERY supported Flyway dialect ships the same additive
 * migration adding {@code SshUsr}, {@code SshPwd}, {@code SshKey} and
 * {@code SshKeyPass} to BOTH {@code AweSchSrv} and its audit table
 * {@code HISAweSchSrv} (design
 * decision 3: new nullable, backward-compatible columns, no changes to
 * existing columns/rows).</p>
 */
class SchedulerServerSshCredentialsMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.4__Add_ssh_credentials_to_server.sql";

  private static final String[] DIALECTS = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"};

  private static final String[] REQUIRED_COLUMNS = {"SSHUSR", "SSHPWD", "SSHKEY", "SSHKEYPASS"};

  // Matches the base table name only when NOT prefixed by "HIS" (i.e. the
  // live AweSchSrv table, not its HISAweSchSrv audit counterpart).
  private static final Pattern BASE_TABLE_PATTERN = Pattern.compile("(?<!HIS)AWESCHSRV");

  /**
   * The migration file must exist, with the SAME name, for every dialect
   * folder the scheduler starter ships migrations for.
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationFileExistsForDialect(String dialect) {
    assertTrue(resourceExists(dialect), "Missing " + MIGRATION_FILE + " for dialect: " + dialect);
  }

  /**
   * Every dialect's migration must add all three SSH credential columns to
   * BOTH the server table and its audit table — never just one.
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationAddsColumnsToBothTables(String dialect) {
    String content = readMigration(dialect).toUpperCase(Locale.ROOT);

    assertTrue(content.contains("HISAWESCHSRV"), dialect + ": migration must alter HISAweSchSrv");
    assertTrue(BASE_TABLE_PATTERN.matcher(content).find(), dialect + ": migration must alter AweSchSrv");

    for (String column : REQUIRED_COLUMNS) {
      long occurrences = countOccurrences(content, column);
      assertTrue(occurrences >= 2,
          dialect + ": expected column " + column + " on both AweSchSrv and HISAweSchSrv, found " + occurrences + " occurrence(s)");
    }
  }

  /**
   * The migration must be strictly additive: no DROP/removal of the new
   * columns anywhere in the same file (backward-compatible schema guard).
   */
  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationDoesNotDropSshColumns(String dialect) {
    String content = readMigration(dialect).toUpperCase(Locale.ROOT);

    assertTrue(!content.contains("DROP COLUMN SSHUSR")
            && !content.contains("DROP COLUMN SSHPWD")
            && !content.contains("DROP COLUMN SSHKEY"),
        dialect + ": migration must not drop the newly added SSH columns");
  }

  /**
   * Sanity check: all six dialects add the exact same three column names
   * (case-insensitive) — no naming drift between dialects.
   */
  @Test
  void allDialectsAgreeOnColumnCount() {
    for (String dialect : DIALECTS) {
      String content = readMigration(dialect).toUpperCase(Locale.ROOT);
      for (String column : REQUIRED_COLUMNS) {
        assertTrue(content.contains(column), dialect + " is missing column " + column);
      }
    }
    assertEquals(6, DIALECTS.length);
  }

  private static boolean resourceExists(String dialect) {
    try (InputStream stream = classLoader().getResourceAsStream(resourcePath(dialect))) {
      return stream != null;
    } catch (IOException e) {
      return false;
    }
  }

  private static String readMigration(String dialect) {
    try (InputStream stream = classLoader().getResourceAsStream(resourcePath(dialect))) {
      if (stream == null) {
        fail("Missing " + MIGRATION_FILE + " for dialect: " + dialect);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String resourcePath(String dialect) {
    return "db/migration/" + dialect + "/" + MIGRATION_FILE;
  }

  private static ClassLoader classLoader() {
    return SchedulerServerSshCredentialsMigrationTest.class.getClassLoader();
  }

  private static long countOccurrences(String content, String needle) {
    long count = 0;
    int index = 0;
    while ((index = content.indexOf(needle, index)) != -1) {
      count++;
      index += needle.length();
    }
    return count;
  }
}
