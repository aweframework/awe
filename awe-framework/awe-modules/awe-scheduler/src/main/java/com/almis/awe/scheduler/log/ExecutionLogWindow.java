package com.almis.awe.scheduler.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, in-memory head+tail window for a single execution's captured log lines.
 * <p>
 * A window retains the first {@code headCapacity} lines (head) and the last
 * {@code tailCapacity} lines (tail, ring-buffer semantics); everything emitted between the two
 * is dropped and accounted for by {@link #omitted()}. {@code headCapacity} and
 * {@code tailCapacity} are derived from the configured {@code maxLines}, never configured
 * independently.
 * <p>
 * Pure domain object: no Spring, no SQL, no I/O. Not thread-safe by design — a window is owned
 * exclusively by the single writer consumer thread that mutates it.
 */
public final class ExecutionLogWindow {

  private final int headCapacity;
  private final int tailCapacity;

  private final String[] head;
  private final String[] tail;
  private final long[] headTimestampMillis;
  private final long[] tailTimestampMillis;
  private final long[] tailLineNumber;
  private final boolean[] dirtyHead;
  private final boolean[] dirtyTail;
  private final boolean[] persistedHead;
  private final boolean[] persistedTail;

  private long totalLines;
  private boolean firstFlushDone;
  private long droppedByQueue;
  private boolean degraded;
  private long lastTouchedMillis;

  /**
   * Create a window bounded to {@code maxLines} total retained lines.
   *
   * @param maxLines Total window size (head + tail); {@code headCapacity = min(200, maxLines / 5)},
   *                 {@code tailCapacity = maxLines - headCapacity}
   */
  public ExecutionLogWindow(int maxLines) {
    if (maxLines < 1) {
      throw new IllegalArgumentException("maxLines must be at least 1");
    }
    this.headCapacity = Math.min(200, maxLines / 5);
    this.tailCapacity = maxLines - headCapacity;
    this.head = new String[headCapacity];
    this.tail = new String[tailCapacity];
    this.headTimestampMillis = new long[headCapacity];
    this.tailTimestampMillis = new long[tailCapacity];
    this.tailLineNumber = new long[tailCapacity];
    java.util.Arrays.fill(tailLineNumber, -1L);
    this.dirtyHead = new boolean[headCapacity];
    this.dirtyTail = new boolean[tailCapacity];
    this.persistedHead = new boolean[headCapacity];
    this.persistedTail = new boolean[tailCapacity];
  }

  /**
   * Head segment capacity (derived, not configured).
   *
   * @return Head capacity
   */
  public int headCapacity() {
    return headCapacity;
  }

  /**
   * Tail segment capacity (derived, not configured).
   *
   * @return Tail capacity
   */
  public int tailCapacity() {
    return tailCapacity;
  }

  /**
   * Accept one physical line with an unknown/irrelevant event timestamp (defaults to epoch 0),
   * placing it in the head while there is room, otherwise writing it into the tail ring
   * (overwriting the oldest tail slot once the ring is full).
   *
   * @param line Line to accept
   */
  public void accept(String line) {
    accept(line, 0L);
  }

  /**
   * Accept one physical line and its event timestamp, placing it in the head while there is room,
   * otherwise writing it into the tail ring (overwriting the oldest tail slot once the ring is
   * full).
   *
   * @param line            Line to accept
   * @param timestampMillis Event timestamp (logging event time, not flush time), epoch millis
   */
  public void accept(String line, long timestampMillis) {
    long lineNumber = totalLines++;
    if (lineNumber < headCapacity) {
      int index = (int) lineNumber;
      head[index] = line;
      headTimestampMillis[index] = timestampMillis;
      dirtyHead[index] = true;
    } else {
      int slot = tailCapacity == 0 ? 0 : (int) ((lineNumber - headCapacity) % tailCapacity);
      tail[slot] = line;
      tailTimestampMillis[slot] = timestampMillis;
      tailLineNumber[slot] = lineNumber;
      dirtyTail[slot] = true;
    }
  }

  /**
   * Total number of lines the execution actually produced, including omitted ones.
   *
   * @return Total line count
   */
  public long totalLines() {
    return totalLines;
  }

  /**
   * Number of lines dropped between the head and the tail because the window is smaller than the
   * execution's total output. Zero means the window is intact.
   *
   * @return Omitted line count
   */
  public long omitted() {
    int tailCount = filledTailCount();
    return Math.max(0, totalLines - headCapacity - tailCount);
  }

  private int filledTailCount() {
    int count = 0;
    for (long lineNumber : tailLineNumber) {
      if (lineNumber >= 0) {
        count++;
      }
    }
    return count;
  }

  /**
   * Whether any slot has been written since the last successful flush.
   *
   * @return {@code true} when at least one head or tail slot is dirty
   */
  public boolean hasDirtySlots() {
    return dirtySlotCount() > 0;
  }

  /**
   * Number of dirty slots (head + tail combined).
   *
   * @return Dirty slot count
   */
  public int dirtySlotCount() {
    return (int) (countTrue(dirtyHead) + countTrue(dirtyTail));
  }

  private long countTrue(boolean[] flags) {
    long count = 0;
    for (boolean flag : flags) {
      if (flag) {
        count++;
      }
    }
    return count;
  }

  /**
   * Dirty slots never flushed before (ADR-4 insert candidates).
   *
   * @return Never-persisted dirty slots
   */
  public List<LogSlot> dirtyNeverPersistedSlots() {
    return collectDirtySlots(false);
  }

  /**
   * Dirty slots that were already persisted in a previous flush (ADR-4 update candidates).
   *
   * @return Already-persisted dirty slots
   */
  public List<LogSlot> dirtyPersistedSlots() {
    return collectDirtySlots(true);
  }

  private List<LogSlot> collectDirtySlots(boolean persisted) {
    List<LogSlot> slots = new ArrayList<>();
    for (int i = 0; i < headCapacity; i++) {
      if (dirtyHead[i] && persistedHead[i] == persisted) {
        slots.add(new LogSlot('H', i, i, head[i], headTimestampMillis[i]));
      }
    }
    for (int slot = 0; slot < tailCapacity; slot++) {
      if (dirtyTail[slot] && persistedTail[slot] == persisted) {
        slots.add(new LogSlot('T', slot, tailLineNumber[slot], tail[slot], tailTimestampMillis[slot]));
      }
    }
    return slots;
  }

  /**
   * Clear the dirty bit and mark the given slots as persisted, following a successful flush.
   *
   * @param slots Slots that were just flushed
   */
  public void markPersisted(List<LogSlot> slots) {
    for (LogSlot slot : slots) {
      if (slot.section() == 'H') {
        dirtyHead[slot.slot()] = false;
        persistedHead[slot.slot()] = true;
      } else {
        dirtyTail[slot.slot()] = false;
        persistedTail[slot.slot()] = true;
      }
    }
  }

  /**
   * Whether the one-shot first-flush delete (ADR-4) is still pending for this execution.
   *
   * @return {@code true} when no flush has happened yet
   */
  public boolean isFirstFlushPending() {
    return !firstFlushDone;
  }

  /**
   * Record that the first-flush delete has been issued.
   */
  public void markFirstFlushDone() {
    firstFlushDone = true;
  }

  /**
   * Add to the queue-saturation drop counter (ADR-8), transferred from the writer's producer-side
   * counter at consumption time.
   *
   * @param count Additional dropped-line count
   */
  public void addDroppedByQueue(long count) {
    droppedByQueue += count;
  }

  /**
   * Number of lines dropped because the async queue was saturated.
   *
   * @return Dropped-by-queue count
   */
  public long droppedByQueue() {
    return droppedByQueue;
  }

  /**
   * Mark this window degraded following a writer-side failure (ADR-8): the task outcome is
   * unaffected, but stored content for this execution may be incomplete.
   */
  public void markDegraded() {
    degraded = true;
  }

  /**
   * Whether this window has been marked degraded.
   *
   * @return {@code true} when a writer-side failure affected this execution's capture
   */
  public boolean isDegraded() {
    return degraded;
  }

  /**
   * Last time (epoch millis) this window was touched, for idle-eviction bookkeeping.
   *
   * @return Last-touched timestamp
   */
  public long lastTouchedMillis() {
    return lastTouchedMillis;
  }

  /**
   * Record the current time as the last-touched timestamp.
   *
   * @param nowMillis Current time (epoch millis)
   */
  public void touch(long nowMillis) {
    this.lastTouchedMillis = nowMillis;
  }

  /**
   * One ring-addressed log slot: section ('H'/'T'), slot index within the section, absolute line
   * number, and rendered text. Mirrors the {@code AweSchExeLog} row shape (ADR-2).
   *
   * @param section         'H' (head) or 'T' (tail)
   * @param slot            Ring slot index within the section
   * @param lineNumber      Absolute 0-based line number
   * @param text            Rendered line text
   * @param timestampMillis Event timestamp (logging event time, not flush time), epoch millis
   */
  public record LogSlot(char section, int slot, long lineNumber, String text, long timestampMillis) {
    public LogSlot {
      Objects.requireNonNull(text, "text must not be null");
    }
  }
}
