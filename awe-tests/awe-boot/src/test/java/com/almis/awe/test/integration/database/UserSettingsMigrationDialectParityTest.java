package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the AWE_V1.2.2__user_settings.sql migration is present, and structurally equivalent,
 * across all six supported dialect folders (h2, hsqldb, mysql, oracle, postgresql, sqlserver).
 * <p>
 * The migration files themselves live in awe-spring-boot-starter; this test reads them directly
 * from the source tree so it stays close to the migrations it verifies without introducing a
 * module dependency cycle.
 */
@DisplayName("AweUserSettings migration dialect parity")
@Tag("integration")
class UserSettingsMigrationDialectParityTest {

  private static final String MIGRATION_FILE = "AWE_V1.2.2__user_settings.sql";

  private static final String MIGRATION_BASE_PATH =
    "../../awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/";

  @ParameterizedTest(name = "{0} dialect defines AweUserSettings with the expected columns")
  @ValueSource(strings = {"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"})
  @DisplayName("Each dialect migration defines the AweUserSettings columns and unique constraint")
  void testDialectMigrationContainsExpectedStructure(String dialect) throws IOException {
    String content = readMigrationFile(dialect);

    assertTrue(containsCaseInsensitive(content, "AweUserSettings"),
      dialect + ": migration must create table AweUserSettings");
    assertTrue(containsCaseInsensitive(content, "IdeUsrSet"),
      dialect + ": migration must define IdeUsrSet column");
    assertTrue(containsCaseInsensitive(content, "Ope"),
      dialect + ": migration must define Ope column");
    assertTrue(containsCaseInsensitive(content, "AvatarImage"),
      dialect + ": migration must define AvatarImage column");
    assertTrue(containsCaseInsensitive(content, "uk_AweUserSettings_Ope"),
      dialect + ": migration must define the uk_AweUserSettings_Ope unique constraint");
    assertTrue(containsCaseInsensitive(content, "UNIQUE"),
      dialect + ": migration must enforce uniqueness on Ope");
  }

  @Test
  @DisplayName("No dialect migration adds a HIS* mirror table for AweUserSettings")
  void testNoHistoricMirrorAdded() throws IOException {
    for (String dialect : new String[]{"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"}) {
      String content = readMigrationFile(dialect);
      assertTrue(!containsCaseInsensitive(content, "HISAweUserSettings"),
        dialect + ": migration must NOT create a HIS* mirror for AweUserSettings");
    }
  }

  @Test
  @DisplayName("No dialect migration alters the ope table")
  void testNoOpeTableAlteration() throws IOException {
    Pattern alterOpePattern = Pattern.compile("(?i)ALTER\\s+TABLE\\s+ope\\b");
    for (String dialect : new String[]{"h2", "hsqldb", "mysql", "oracle", "postgresql", "sqlserver"}) {
      String content = readMigrationFile(dialect);
      Matcher matcher = alterOpePattern.matcher(content);
      assertTrue(!matcher.find(), dialect + ": migration must NOT alter the ope table");
    }
  }

  private boolean containsCaseInsensitive(String content, String needle) {
    return content.toLowerCase().contains(needle.toLowerCase());
  }

  private String readMigrationFile(String dialect) throws IOException {
    java.io.File file = new java.io.File(
      new java.io.File(MIGRATION_BASE_PATH + dialect), MIGRATION_FILE);
    if (!file.exists()) {
      fail("Migration file not found for dialect '" + dialect + "': " + file.getAbsolutePath());
    }
    try (InputStream inputStream = new java.io.FileInputStream(file)) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
