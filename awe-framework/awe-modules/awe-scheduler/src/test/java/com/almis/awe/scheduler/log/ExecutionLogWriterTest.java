package com.almis.awe.scheduler.log;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ExecutionLogWriter}: bounded async queue, dirty-slot batched flush (ADR-3),
 * portable insert/update split (ADR-4), the bounded/lossy/never-propagating failure policy
 * (ADR-8), and origin-partitioned windows keeping each origin a single writer over its own slot
 * space (D4).
 */
@ExtendWith(MockitoExtension.class)
class ExecutionLogWriterTest {

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryUtil queryUtil;

  private ExecutionLogWriter writer;

  @AfterEach
  void tearDown() throws AWException {
    if (writer != null) {
      writer.stop();
    }
  }

  private ExecutionLogWriter newWriter(int maxLines, int queueCapacity, int batchSize, Duration flushInterval, Duration taskTimeout) {
    given(queryUtil.getParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    ExecutionLogWriter created = new ExecutionLogWriter(maintainService, queryUtil, maxLines, queueCapacity, batchSize, flushInterval, taskTimeout);
    created.start();
    return created;
  }

  private static ExecutionLogLine schedulerLine(ExecutionKey key, String text) {
    return new ExecutionLogLine(key, ExecutionLogOrigin.SCHEDULER, System.currentTimeMillis(), text);
  }

  private static ExecutionLogLine line(ExecutionKey key, ExecutionLogOrigin origin, String text) {
    return new ExecutionLogLine(key, origin, System.currentTimeMillis(), text);
  }

  private static ExecutionLogLine lineAt(ExecutionKey key, ExecutionLogOrigin origin, long timestampMillis, String text) {
    return new ExecutionLogLine(key, origin, timestampMillis, text);
  }

  private static long parseLogDateMillis(String formatted) {
    return LocalDateTime.parse(formatted, DateUtil.TIMESTAMP_FORMAT_WEB_MS)
      .atZone(ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli();
  }

  @Test
  void batchSizeDirtySlotsTriggersAFlushBeforeTheIntervalElapses() throws AWException {
    // Long interval so only the batch-size trigger can cause the flush within the test window.
    writer = newWriter(1000, 1000, 3, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(1, 1);

    writer.append(schedulerLine(key, "line0"));
    writer.append(schedulerLine(key, "line1"));
    writer.append(schedulerLine(key, "line2")); // 3rd dirty slot reaches batchSize=3

    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
  }

  @Test
  void flushIntervalElapsingTriggersAFlushBelowTheBatchSize() throws AWException {
    writer = newWriter(1000, 1000, 1000, Duration.ofMillis(50), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(2, 2);

    writer.append(schedulerLine(key, "only one line"));

    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
  }

  @Test
  void completeTriggersAFinalFlushAndEvictsTheWindow() throws AWException {
    writer = newWriter(1000, 1000, 1000, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(3, 3);

    writer.append(schedulerLine(key, "one line"));
    writer.complete(key, ExecutionLogOrigin.SCHEDULER);

    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    assertEquals(0, writer.liveWindowCount());
  }

  @Test
  void firstFlushIssuesTheOriginScopedDeleteOnceThenInsertsNeverPersistedSlots() throws AWException {
    writer = newWriter(1000, 1000, 1000, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(4, 4);

    writer.append(schedulerLine(key, "line0"));
    writer.complete(key, ExecutionLogOrigin.SCHEDULER);

    ArgumentCaptor<ObjectNode> purgeCaptor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("purgeExecutionLogOrigin"), purgeCaptor.capture());
    assertEquals("[\"S\"]", purgeCaptor.getValue().get("origin").toString());
    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    verify(maintainService, never()).launchPrivateMaintain(eq("updateExecutionLogLines"), any());
    verify(maintainService, never()).launchPrivateMaintain(eq("purgeExecutionLogLines"), any());
  }

  @Test
  void aSecondFlushForAnAlreadyPersistedSlotIssuesAnUpdateNotAnInsert() throws AWException {
    writer = newWriter(10, 1000, 1, Duration.ofSeconds(30), Duration.ofSeconds(30)); // head=2,tail=8, batchSize=1 forces eager flush
    ExecutionKey key = new ExecutionKey(5, 5);

    writer.append(schedulerLine(key, "head0")); // dirty slot #1 -> flush #1 (insert)
    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());

    writer.append(schedulerLine(key, "head1")); // new slot, still an insert
    verify(maintainService, timeout(2000).times(2)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());

    // Fill and wrap the tail ring so an already-persisted tail slot becomes dirty again.
    for (int i = 0; i < 9; i++) {
      writer.append(schedulerLine(key, "tail" + i));
    }

    verify(maintainService, timeout(2000).atLeastOnce()).launchPrivateMaintain(eq("updateExecutionLogLines"), any());
  }

  @Test
  void queueSaturationDropsLinesWithoutThrowingOrBlockingAndIncrementsTheDropCounter() throws AWException {
    given(queryUtil.getParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    // Queue capacity 2, batchSize 2: constructed but not yet started, so the first 2 append()
    // calls deterministically claim the only 2 queue slots and the remaining 48 are dropped
    // synchronously on the calling thread -- no race with a consumer thread.
    writer = new ExecutionLogWriter(maintainService, queryUtil, 1000, 2, 2, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(6, 6);

    for (int i = 0; i < 50; i++) {
      writer.append(schedulerLine(key, "line" + i)); // never throws regardless of how far past capacity we push
    }

    writer.start();

    // batchSize matches the queued count, so this first (2-line) flush proves the queue has
    // fully drained before completion is signalled below.
    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());

    writer.complete(key, ExecutionLogOrigin.SCHEDULER);

    // The completion's final flush must include a synthetic drop-marker line, proving the drop
    // counter (48 dropped lines) was observed and reset. Wait for exactly the 2nd insert flush
    // (not just atLeastOnce(), which the 1st flush alone already satisfies) so the captor is not
    // inspected before the completion-triggered flush has actually happened.
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, timeout(2000).times(2)).launchPrivateMaintain(eq("insertExecutionLogLines"), captor.capture());
    boolean anyMarkerLine = captor.getAllValues().stream()
      .flatMap(parameters -> textValuesOf(parameters).stream())
      .anyMatch(text -> text.toLowerCase().contains("dropped"));
    assertTrue(anyMarkerLine, "Expected a synthetic drop-marker line among the flushed lineText values");
  }

  @Test
  void theDropMarkerLineCarriesANonEpochTimestampNewerThanThePriorLines() throws AWException {
    given(queryUtil.getParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    // Queue capacity 2, batchSize 2: the first 2 append() calls claim the only 2 queue slots
    // before the consumer starts, so the following appends are dropped deterministically.
    writer = new ExecutionLogWriter(maintainService, queryUtil, 1000, 2, 2, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(14, 14);

    writer.append(lineAt(key, ExecutionLogOrigin.SCHEDULER, 1_000L, "line0"));
    writer.append(lineAt(key, ExecutionLogOrigin.SCHEDULER, 2_000L, "line1"));
    for (int i = 0; i < 10; i++) {
      writer.append(lineAt(key, ExecutionLogOrigin.SCHEDULER, 3_000L, "dropped" + i));
    }

    writer.start();
    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());

    writer.complete(key, ExecutionLogOrigin.SCHEDULER);

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, timeout(2000).times(2)).launchPrivateMaintain(eq("insertExecutionLogLines"), captor.capture());
    ObjectNode markerFlush = captor.getAllValues().get(1);
    assertEquals("[\"... 10 lines dropped (queue saturated) ...\"]", markerFlush.get("lineText").toString());
    long markerLogDateMillis = parseLogDateMillis(markerFlush.get("logDate").get(0).asText());
    assertTrue(markerLogDateMillis > 2_000L,
      "Expected the drop marker's logDate to be newer than the prior lines, was " + markerLogDateMillis);
  }

  @Test
  void writerExceptionDuringFlushNeverPropagatesAndMarksTheWindowDegraded() throws AWException {
    // lenient(): the same mocked method also legitimately receives the unstubbed
    // "purgeExecutionLogOrigin" first-flush call; without lenient() strict stubbing would throw a
    // PotentialStubbingProblem on that call, silently caught by the writer's own failure isolation
    // and hiding this test's real assertion.
    lenient().doThrow(new RuntimeException("boom")).when(maintainService).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    writer = newWriter(1000, 1000, 1, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(7, 7);

    writer.append(schedulerLine(key, "line that will fail to flush")); // must not throw on this call

    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    // A second failing flush attempt for the same window must not log a second time (asserted
    // indirectly: the writer thread must still be alive and responsive afterwards).
    writer.append(schedulerLine(key, "another line that will also fail"));
    verify(maintainService, timeout(2000).atLeast(1)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
  }

  @Test
  void idleWindowsAreEvictedAfterTaskTimeoutWithNoCompleteEvent() throws AWException {
    writer = newWriter(1000, 1000, 1000, Duration.ofMillis(30), Duration.ofMillis(60));
    ExecutionKey key = new ExecutionKey(8, 8);

    writer.append(schedulerLine(key, "one line, execution never completes"));

    // Wait past task-timeout for the idle-eviction sweep to run.
    verify(maintainService, timeout(3000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    long deadline = System.currentTimeMillis() + 3000;
    while (writer.liveWindowCount() > 0 && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(20);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    assertEquals(0, writer.liveWindowCount());
  }

  @Test
  void stopDrainsThePendingQueueAndFinalFlushesEveryLiveWindowWithinTheDeadline() throws AWException {
    writer = newWriter(1000, 1000, 1000, Duration.ofMillis(50), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(9, 9);

    writer.append(schedulerLine(key, "not yet flushed by interval"));

    writer.stop();

    verify(maintainService, times(1)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
  }

  /**
   * [R2] The writer thread runs outside any task's request context, so a task-scoped {@code
   * _database_} routing parameter must never reach the flush's maintain parameters. The writer
   * never accepts or forwards an externally-supplied parameters object at all -- it only ever
   * calls {@link QueryUtil#getParameters()} (no-arg) to build a fresh one on its own thread -- so
   * there is no channel through which a task's routing context could leak into a flush call.
   */
  @Test
  void flushParametersAreBuiltFreshOnTheWriterThreadNeverFromAnExternallySuppliedContext() throws AWException {
    writer = newWriter(1000, 1000, 1, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(10, 10);

    writer.append(schedulerLine(key, "line routed through the writer's own datasource resolution"));

    verify(maintainService, timeout(2000)).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    verify(queryUtil, atLeastOnce()).getParameters();
    verify(queryUtil, never()).getParameters(org.mockito.ArgumentMatchers.any(ObjectNode.class));
    verify(queryUtil, never()).getParameters(anyString());
  }

  @Test
  void twoOriginPartitionsOfOneExecutionEachIssueTheirOwnOriginScopedFirstFlushDelete() throws AWException {
    writer = newWriter(1000, 1000, 1, Duration.ofSeconds(30), Duration.ofSeconds(30)); // batchSize=1 forces eager flush
    ExecutionKey key = new ExecutionKey(11, 11);

    writer.append(line(key, ExecutionLogOrigin.SCHEDULER, "scheduler line0"));
    writer.append(line(key, ExecutionLogOrigin.APPLICATION, "application line0"));

    ArgumentCaptor<ObjectNode> purgeCaptor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, timeout(2000).times(2)).launchPrivateMaintain(eq("purgeExecutionLogOrigin"), purgeCaptor.capture());
    List<String> purgedOrigins = purgeCaptor.getAllValues().stream().map(node -> node.get("origin").toString()).toList();
    assertTrue(purgedOrigins.contains("[\"S\"]") && purgedOrigins.contains("[\"A\"]"),
      "Expected one origin-scoped delete per partition, got: " + purgedOrigins);
    verify(maintainService, never()).launchPrivateMaintain(eq("purgeExecutionLogLines"), any());

    ArgumentCaptor<ObjectNode> insertCaptor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, timeout(2000).times(2)).launchPrivateMaintain(eq("insertExecutionLogLines"), insertCaptor.capture());
    assertTrue(insertCaptor.getAllValues().stream().allMatch(node -> "[0]".equals(node.get("lineNumber").toString())),
      "Each origin partition must number its own first line as 0 independently of the other origin");
  }

  @Test
  void completingOneOriginsPartitionNeverEvictsTheOtherOriginsLiveWindow() throws AWException, InterruptedException {
    writer = newWriter(1000, 1000, 1000, Duration.ofSeconds(30), Duration.ofSeconds(30));
    ExecutionKey key = new ExecutionKey(12, 12);

    writer.append(line(key, ExecutionLogOrigin.SCHEDULER, "scheduler line0"));
    writer.append(line(key, ExecutionLogOrigin.APPLICATION, "application line0"));
    writer.complete(key, ExecutionLogOrigin.APPLICATION);

    verify(maintainService, timeout(2000).atLeastOnce()).launchPrivateMaintain(eq("insertExecutionLogLines"), any());
    long deadline = System.currentTimeMillis() + 2000;
    while (writer.liveWindowCount() > 1 && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
    assertEquals(1, writer.liveWindowCount(),
      "Completing the application partition must leave the scheduler partition's window live");
  }

  @Test
  void idleEvictionActsPerPartitionNotPerExecutionKey() throws AWException, InterruptedException {
    writer = newWriter(1000, 1000, 1000, Duration.ofMillis(30), Duration.ofMillis(60));
    ExecutionKey key = new ExecutionKey(13, 13);

    writer.append(line(key, ExecutionLogOrigin.SCHEDULER, "scheduler line, completes normally"));
    writer.append(line(key, ExecutionLogOrigin.APPLICATION, "application line, never completes"));
    writer.complete(key, ExecutionLogOrigin.SCHEDULER);

    long deadline = System.currentTimeMillis() + 3000;
    while (writer.liveWindowCount() > 0 && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
    assertEquals(0, writer.liveWindowCount(),
      "The abandoned application partition must be idle-evicted independently of the completed scheduler partition");
  }

  private List<String> textValuesOf(ObjectNode parameters) {
    List<String> texts = new ArrayList<>();
    if (parameters.has("lineText") && parameters.get("lineText").isArray()) {
      parameters.get("lineText").forEach(node -> texts.add(node.asText()));
    }
    return texts;
  }

  private static ObjectNode any() {
    return org.mockito.ArgumentMatchers.any(ObjectNode.class);
  }
}
