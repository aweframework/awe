package com.almis.awe.scheduler.log;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ExecutionLogWindow}.
 * <p>
 * {@code display()}/the truncation marker text are not exercised here: they were removed as
 * production-unreachable ({@link DatabaseExecutionLogStore#read} composes its display window
 * directly from the queried rows, never from a live {@link ExecutionLogWindow}). Head/tail/ring
 * behavior is proven through the flush-facing slot API ({@link ExecutionLogWindow#dirtyNeverPersistedSlots()})
 * that production actually consumes.
 */
class ExecutionLogWindowTest {

  @Test
  void headCapacityAndTailCapacityAreDerivedFromMaxLines() {
    ExecutionLogWindow window = new ExecutionLogWindow(1000);

    assertEquals(200, window.headCapacity());
    assertEquals(800, window.tailCapacity());
  }

  @Test
  void headCapacityIsCappedAtTwoHundredForLargeWindows() {
    ExecutionLogWindow window = new ExecutionLogWindow(2000);

    assertEquals(200, window.headCapacity());
    assertEquals(1800, window.tailCapacity());
  }

  @Test
  void headFillsFirstThenTailFills() {
    ExecutionLogWindow window = new ExecutionLogWindow(10); // head=2, tail=8

    for (int i = 0; i < 5; i++) {
      window.accept("line" + i);
    }

    assertEquals(5, window.totalLines());
    assertEquals(0, window.omitted());
    List<String> texts = window.dirtyNeverPersistedSlots().stream().map(ExecutionLogWindow.LogSlot::text).toList();
    assertEquals(List.of("line0", "line1", "line2", "line3", "line4"), texts);
  }

  @Test
  void ringWrapOverwritesOnlyTheOldestTailSlot() {
    ExecutionLogWindow window = new ExecutionLogWindow(10); // head=2, tail=8

    for (int i = 0; i < 12; i++) { // 2 head + 10 tail attempts (8 capacity, 2 overwritten)
      window.accept("line" + i);
    }

    assertEquals(12, window.totalLines());
    assertEquals(2, window.omitted());

    Map<Integer, String> tailBySlot = tailTextsBySlot(window);
    assertEquals(8, tailBySlot.size());
    assertEquals("line10", tailBySlot.get(0)); // oldest tail slot (line2) overwritten by line10
    assertEquals("line11", tailBySlot.get(1)); // next-oldest tail slot (line3) overwritten by line11
    assertEquals("line4", tailBySlot.get(2));
    assertEquals("line5", tailBySlot.get(3));
    assertEquals("line6", tailBySlot.get(4));
    assertEquals("line7", tailBySlot.get(5));
    assertEquals("line8", tailBySlot.get(6));
    assertEquals("line9", tailBySlot.get(7));
  }

  @Test
  void ringWrapAdvancesOneSlotAtATime() {
    ExecutionLogWindow window = new ExecutionLogWindow(10); // head=2, tail=8

    for (int i = 0; i < 10; i++) {
      window.accept("line" + i); // fills head (2) + tail exactly (8), no wrap yet
    }
    assertEquals(0, window.omitted());

    window.accept("line10"); // one more line -> wraps and overwrites only the oldest tail entry (line2)

    assertEquals(11, window.totalLines());
    assertEquals(1, window.omitted());

    Map<Integer, String> tailBySlot = tailTextsBySlot(window);
    assertEquals("line10", tailBySlot.get(0)); // oldest tail slot (line2) overwritten
    assertEquals("line3", tailBySlot.get(1));
    assertEquals("line4", tailBySlot.get(2));
    assertEquals("line5", tailBySlot.get(3));
    assertEquals("line6", tailBySlot.get(4));
    assertEquals("line7", tailBySlot.get(5));
    assertEquals("line8", tailBySlot.get(6));
    assertEquals("line9", tailBySlot.get(7));
  }

  private Map<Integer, String> tailTextsBySlot(ExecutionLogWindow window) {
    return window.dirtyNeverPersistedSlots().stream()
      .filter(slot -> slot.section() == 'T')
      .collect(Collectors.toMap(ExecutionLogWindow.LogSlot::slot, ExecutionLogWindow.LogSlot::text));
  }

  @Test
  void omittedIsZeroWhenWindowFullyContainsTheExecution() {
    ExecutionLogWindow window = new ExecutionLogWindow(1000);

    for (int i = 0; i < 500; i++) {
      window.accept("line" + i);
    }

    assertEquals(0, window.omitted());
    assertEquals(500, window.totalLines());
  }

  @Test
  void omittedAccountsForLinesDroppedBetweenHeadAndTail() {
    ExecutionLogWindow window = new ExecutionLogWindow(10); // head=2, tail=8

    for (int i = 0; i < 100; i++) {
      window.accept("line" + i);
    }

    // total 100, head 2, tail 8 retained -> 90 omitted
    assertEquals(90, window.omitted());
    assertEquals(100, window.totalLines());
  }

  @Test
  void newlyAcceptedSlotsAreDirtyAndNeverPersisted() {
    ExecutionLogWindow window = new ExecutionLogWindow(10);
    window.accept("head-line");

    assertTrue(window.hasDirtySlots());
    assertEquals(1, window.dirtySlotCount());
    List<ExecutionLogWindow.LogSlot> neverPersisted = window.dirtyNeverPersistedSlots();
    assertEquals(1, neverPersisted.size());
    assertEquals('H', neverPersisted.get(0).section());
    assertEquals(0, neverPersisted.get(0).slot());
    assertEquals(0, neverPersisted.get(0).lineNumber());
    assertEquals("head-line", neverPersisted.get(0).text());
    assertTrue(window.dirtyPersistedSlots().isEmpty());
  }

  /**
   * A tail ring slot's (Sec, Slt) primary key never changes when the ring wraps and reuses the
   * slot for a new line: the row already exists in AweSchExeLog once persisted, so a
   * previously-persisted slot must stay an update candidate forever, never re-enter the
   * insert bucket (which would attempt a duplicate-key insert on the next flush).
   */
  @Test
  void markPersistedClearsDirtyAndMovesSlotToTheUpdateBucketOnNextEdit() {
    ExecutionLogWindow window = new ExecutionLogWindow(10); // head=2, tail=8

    window.accept("head0");
    window.accept("head1");
    window.accept("tail0");

    window.markPersisted(window.dirtyNeverPersistedSlots());
    assertFalse(window.hasDirtySlots());
    assertEquals(0, window.dirtySlotCount());

    // Overwrite the tail slot (ring wrap simulated by filling the rest of the tail then one more)
    for (int i = 0; i < 8; i++) {
      window.accept("tail" + (i + 1));
    }

    // The re-written slot(s) are dirty again, and since they were already persisted, they land
    // in the "already persisted" bucket (update candidates), not "never persisted" (insert).
    assertTrue(window.hasDirtySlots());
    assertFalse(window.dirtyPersistedSlots().isEmpty());
  }

  @Test
  void firstFlushIsPendingUntilMarkedDone() {
    ExecutionLogWindow window = new ExecutionLogWindow(10);

    assertTrue(window.isFirstFlushPending());
    window.markFirstFlushDone();
    assertFalse(window.isFirstFlushPending());
  }

  @Test
  void droppedByQueueAccumulates() {
    ExecutionLogWindow window = new ExecutionLogWindow(10);

    window.addDroppedByQueue(3);
    window.addDroppedByQueue(4);

    assertEquals(7, window.droppedByQueue());
  }

  @Test
  void degradedFlagStartsFalseAndCanBeMarked() {
    ExecutionLogWindow window = new ExecutionLogWindow(10);

    assertFalse(window.isDegraded());
    window.markDegraded();
    assertTrue(window.isDegraded());
  }

  @Test
  void lastTouchedMillisIsRecordedExplicitly() {
    ExecutionLogWindow window = new ExecutionLogWindow(10);

    assertEquals(0L, window.lastTouchedMillis());
    window.touch(12345L);
    assertEquals(12345L, window.lastTouchedMillis());
  }
}
