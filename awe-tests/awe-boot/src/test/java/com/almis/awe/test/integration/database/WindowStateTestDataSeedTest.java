package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test data of every dialect keeps the sequence key of the advanced block states (#790): a script that empties
 * AweKey seeds WinCnfKey again exactly once, and a script that keeps the keys of the schema does not insert it twice
 */
@DisplayName("AweUsrWinCnf sequence key in the test data")
@Tag("integration")
class WindowStateTestDataSeedTest {

  private static final Pattern EMPTIES_AWE_KEY = Pattern.compile("(?i)(delete\\s+from|truncate)\\s+(table\\s+)?AweKey\\b");
  private static final Pattern SEEDS_WIN_CNF_KEY = Pattern.compile("'WinCnfKey'");

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"testdata-h2.sql", "testdata-hsqldb.sql", "testdata-mysql.sql", "testdata-oracledb.sql",
    "testdata-postgresql.sql", "testdata-sqlserverdb.sql"})
  void seedsTheWindowStateKeyOnlyWhenTheScriptEmptiesAweKey(String script) throws IOException {
    String sql = Files.readString(Path.of("src/test/resources/sql", script));
    long seeds = count(SEEDS_WIN_CNF_KEY.matcher(sql));
    long expected = EMPTIES_AWE_KEY.matcher(sql).find() ? 1 : 0;

    assertEquals(expected, seeds, script + " seeds WinCnfKey " + seeds + " times");
  }

  private static long count(Matcher matcher) {
    long count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
