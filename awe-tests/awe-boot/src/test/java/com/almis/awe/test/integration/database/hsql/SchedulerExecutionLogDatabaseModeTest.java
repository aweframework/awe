package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogLine;
import com.almis.awe.scheduler.log.ExecutionLogPage;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration coverage for the database-mode {@link ExecutionLogStore}
 * ({@code DatabaseExecutionLogStore} + the real async {@code ExecutionLogWriter}) against a real
 * file-based HSQLDB, proving the {@code Queries.xml}/{@code Maintain.xml} descriptors introduced by
 * this change actually work end to end — the gap unit tests (mocked {@code QueryService}/
 * {@code MaintainService}) cannot close.
 * <p>
 * Tuning properties are set small and fast so the tests are deterministic without arbitrary sleeps:
 * {@code execution-log-flush-interval} is short, and {@code execution-log-max-lines} is small enough
 * to exercise truncation with a modest number of appended lines.
 */
@Tag("integration")
@TestPropertySource(properties = {
  "awe.scheduler.execution-log-store=database",
  "awe.scheduler.execution-log-max-lines=20",
  "awe.scheduler.execution-log-flush-interval=100ms",
  "awe.scheduler.execution-log-batch-size=5"
})
class SchedulerExecutionLogDatabaseModeTest extends AbstractSpringAppIntegrationTest {

  private static final long AWAIT_TIMEOUT_MILLIS = 10_000L;
  private static final long AWAIT_POLL_MILLIS = 50L;

  @Autowired
  private ExecutionLogStore executionLogStore;

  @Test
  void normalVolumeExecutionCompletesWithACompleteWindowAndHonorsTheOffsetContract() throws Exception {
    ExecutionKey key = new ExecutionKey(9001, 1);

    for (int i = 0; i < 5; i++) {
      appendSchedulerLine(key, "line" + i);
    }
    executionLogStore.complete(key, ExecutionLogOrigin.SCHEDULER);

    ExecutionLogPage page = awaitPage(key, page1 -> page1.totalLines() == 5L);

    assertEquals(List.of("line0", "line1", "line2", "line3", "line4"), page.lines());
    assertFalse(page.replace(), "An intact window must not signal a client replace");
    assertEquals(0L, page.omittedLines());
    assertEquals(5L, page.totalLines());

    // Polling again with an offset equal to the last delivered line count returns no new content
    // and does not error (spec: "Polling past the end of a completed execution returns no new content").
    ExecutionLogPage pastEnd = executionLogStore.read(key, page.lines().size(), null);
    assertTrue(pastEnd.lines().isEmpty());
    assertFalse(pastEnd.replace());
  }

  @Test
  void oversizedExecutionIsTruncatedWithAMarkerSurfacingTheOmittedCount() throws Exception {
    ExecutionKey key = new ExecutionKey(9001, 2);
    int emittedLines = 50; // exceeds execution-log-max-lines=20 (head=4, tail=16)

    for (int i = 0; i < emittedLines; i++) {
      appendSchedulerLine(key, "line" + i);
    }
    executionLogStore.complete(key, ExecutionLogOrigin.SCHEDULER);

    // Wait for the fully-flushed final window, not merely the first flush that already shows
    // omission -- an in-progress capture can transiently satisfy "omitted > 0" before every
    // emitted line has been persisted.
    ExecutionLogPage page = awaitPage(key, page1 -> page1.totalLines() == emittedLines);

    assertEquals(emittedLines, page.totalLines());
    assertFalse(page.replace(), "offset 0 must always be append mode, never replace");
    assertEquals(emittedLines - 20, page.omittedLines()); // 50 total - 20 retained (head 4 + tail 16)
    assertTrue(page.lines().stream().anyMatch(line ->
        line.contains("omitted") && line.contains(String.valueOf(page.omittedLines()))),
      "The truncation marker line must surface the omitted-line count: " + page.lines());
  }

  @Test
  void perTaskPurgeRemovesStoredRowsBeyondTheRetainedExecutionsWhileKeepingRetainedRowsIntact() throws Exception {
    ExecutionKey retained = new ExecutionKey(9002, 1);
    ExecutionKey purged = new ExecutionKey(9002, 2);

    appendSchedulerLine(retained, "keep-me");
    executionLogStore.complete(retained, ExecutionLogOrigin.SCHEDULER);
    appendSchedulerLine(purged, "delete-me");
    executionLogStore.complete(purged, ExecutionLogOrigin.SCHEDULER);

    awaitPage(retained, page -> page.totalLines() == 1L);
    awaitPage(purged, page -> page.totalLines() == 1L);

    executionLogStore.purge(purged.taskId(), List.of(purged.executionId()));

    assertTrue(executionLogStore.read(purged, 0, null).lines().isEmpty(), "Purged execution rows must be gone");
    assertEquals(List.of("keep-me"), executionLogStore.read(retained, 0, null).lines(),
      "Retained execution rows must be unaffected by a per-task purge");
  }

  @Test
  void startupOrphanPurgeRemovesRowsForExecutionsNoLongerInTheValidSetAndKeepsValidRowsIntact() throws Exception {
    ExecutionKey valid = new ExecutionKey(9003, 1);
    ExecutionKey orphan = new ExecutionKey(9003, 2);

    appendSchedulerLine(valid, "valid-row");
    executionLogStore.complete(valid, ExecutionLogOrigin.SCHEDULER);
    appendSchedulerLine(orphan, "orphan-row");
    executionLogStore.complete(orphan, ExecutionLogOrigin.SCHEDULER);

    awaitPage(valid, page -> page.totalLines() == 1L);
    awaitPage(orphan, page -> page.totalLines() == 1L);

    executionLogStore.purgeOrphans(Set.of(valid));

    assertTrue(executionLogStore.read(orphan, 0, null).lines().isEmpty(), "Orphaned execution rows must be deleted");
    assertEquals(List.of("valid-row"), executionLogStore.read(valid, 0, null).lines(),
      "Valid execution rows must survive orphan purge");
  }

  /**
   * Appends one scheduler-origin line with a monotonically increasing event timestamp, mirroring
   * the shape {@code ExecutionLogStoreAppender} builds in production.
   *
   * @param key  Execution key
   * @param text Rendered physical line text
   */
  private void appendSchedulerLine(ExecutionKey key, String text) {
    executionLogStore.append(new ExecutionLogLine(key, ExecutionLogOrigin.SCHEDULER, System.currentTimeMillis(), text));
  }

  /**
   * Polls {@link ExecutionLogStore#read} until the given predicate is satisfied or a bounded
   * timeout elapses, tolerating the async writer's flush latency without an arbitrary sleep.
   *
   * @param key       Execution key to read
   * @param satisfied Predicate the returned page must satisfy to stop polling
   * @return The first page satisfying the predicate
   * @throws Exception Propagates any read failure
   */
  private ExecutionLogPage awaitPage(ExecutionKey key, java.util.function.Predicate<ExecutionLogPage> satisfied) throws Exception {
    long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MILLIS;
    ExecutionLogPage page = executionLogStore.read(key, 0, null);
    while (!satisfied.test(page) && System.currentTimeMillis() < deadline) {
      Thread.sleep(AWAIT_POLL_MILLIS);
      page = executionLogStore.read(key, 0, null);
    }
    return page;
  }
}
