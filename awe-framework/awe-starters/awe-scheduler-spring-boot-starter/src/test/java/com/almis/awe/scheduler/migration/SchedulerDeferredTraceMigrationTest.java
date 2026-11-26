package com.almis.awe.scheduler.migration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guard for the trace of the deferred executions: every dialect adds the trace columns and the cancellation target
 * of the task (and of its history), and drops nothing.
 */
class SchedulerDeferredTraceMigrationTest {

  private static final String MIGRATION_FILE = "SCHEDULER_V1.0.11__Deferred_execution_trace.sql";

  @ParameterizedTest
  @ValueSource(strings = {"h2", "mysql", "postgresql", "oracle", "sqlserver", "hsqldb"})
  void migrationAddsTheTraceAndTheCancellationTarget(String dialect) {
    String content = readMigration(dialect).toUpperCase(Locale.ROOT);

    assertTrue(content.contains("AWESCHEXEEXT"), dialect + ": trace columns go in AweSchExeExt");
    for (String column : new String[]{"CMPDAT", "CMPSRC", "CNCSTA", "CNCERR"}) {
      assertTrue(content.contains(column), dialect + ": missing column " + column);
    }
    assertTrue(content.contains("HISAWESCHTSK"), dialect + ": the task history gets the cancellation target");
    assertTrue(content.split("CNCMNT", -1).length - 1 >= 2, dialect + ": CncMnt in the task and its history");
    assertFalse(content.contains("DROP"), dialect + ": additive migration");
  }

  private static String readMigration(String dialect) {
    String path = "db/migration/" + dialect + "/" + MIGRATION_FILE;
    try (InputStream stream = SchedulerDeferredTraceMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        fail("Missing " + MIGRATION_FILE + " for dialect: " + dialect);
      }
      StringBuilder builder = new StringBuilder();
      for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
        if (!line.trim().startsWith("--")) {
          builder.append(line).append('\n');
        }
      }
      return builder.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
