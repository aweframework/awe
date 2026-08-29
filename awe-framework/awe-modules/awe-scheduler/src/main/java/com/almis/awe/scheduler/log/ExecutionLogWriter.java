package com.almis.awe.scheduler.log;

import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded asynchronous boundary between the database-mode capture appender and
 * {@link DatabaseExecutionLogStore} (ADR-3). A single daemon consumer thread owns a per-execution
 * {@link ExecutionLogWindow} map and flushes only dirty slots, following the portable
 * insert/update split of ADR-4. The producer side (append/complete) is offer-only and never
 * blocks or throws (ADR-8): a saturated queue drops lines and increments a per-execution counter,
 * surfaced as a synthetic marker line on the execution's final flush.
 */
@Slf4j
public class ExecutionLogWriter {

  private static final String INSERT_TARGET = "insertExecutionLogLines";
  private static final String UPDATE_TARGET = "updateExecutionLogLines";
  private static final String PURGE_ORIGIN_TARGET = "purgeExecutionLogOrigin";
  private static final String THREAD_NAME = "awe-scheduler-execution-log-writer";

  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final int maxLines;
  private final int batchSize;
  private final long flushIntervalMillis;
  private final long taskTimeoutMillis;
  private final long shutdownDrainMillis;
  private final Clock clock;

  private final ArrayBlockingQueue<ExecutionLogEvent> queue;
  private final Map<ExecutionLogPartition, ExecutionLogWindow> windows = new ConcurrentHashMap<>();
  private final Map<ExecutionLogPartition, AtomicLong> dropsByKey = new ConcurrentHashMap<>();
  private final Map<ExecutionLogPartition, Long> lastFlushAtMillis = new ConcurrentHashMap<>();

  private volatile boolean running;
  private Thread consumerThread;

  /**
   * Constructor
   *
   * @param maintainService Maintain service, used to flush dirty slots
   * @param queryUtil       Query utilities, used to build fresh (never task-scoped) maintain parameters
   * @param maxLines        Window size (head + tail) per execution
   * @param queueCapacity   Bounded queue capacity
   * @param batchSize       Events drained per poll cycle, and dirty-slot flush trigger threshold
   * @param flushInterval   Time-based flush trigger; also the shutdown drain deadline is twice this
   * @param taskTimeout     Idle-eviction safety net when no {@code complete} event ever arrives
   */
  public ExecutionLogWriter(MaintainService maintainService, QueryUtil queryUtil, int maxLines, int queueCapacity,
                             int batchSize, Duration flushInterval, Duration taskTimeout) {
    this(maintainService, queryUtil, maxLines, queueCapacity, batchSize, flushInterval, taskTimeout, Clock.systemUTC());
  }

  ExecutionLogWriter(MaintainService maintainService, QueryUtil queryUtil, int maxLines, int queueCapacity,
                      int batchSize, Duration flushInterval, Duration taskTimeout, Clock clock) {
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.maxLines = maxLines;
    this.batchSize = batchSize;
    this.flushIntervalMillis = flushInterval.toMillis();
    this.taskTimeoutMillis = taskTimeout.toMillis();
    this.shutdownDrainMillis = flushIntervalMillis * 2;
    this.clock = clock;
    this.queue = new ArrayBlockingQueue<>(queueCapacity);
  }

  /**
   * Append one already-formatted physical line for an execution's origin partition. Non-blocking:
   * a saturated queue drops the line and increments the partition's drop counter instead. Never
   * throws.
   *
   * @param line Execution log line to append
   */
  public void append(ExecutionLogLine line) {
    ExecutionLogPartition partition = new ExecutionLogPartition(line.key(), line.origin());
    boolean offered = queue.offer(new ExecutionLogEvent(partition, line.text(), line.timestampMillis(), false));
    if (!offered) {
      dropsByKey.computeIfAbsent(partition, unused -> new AtomicLong()).incrementAndGet();
    }
  }

  /**
   * Signal that no further lines will arrive for the given origin's partition of this execution.
   * Best-effort: if the queue is saturated the signal itself may be dropped, in which case
   * idle-eviction (task-timeout) is the safety net that eventually finalizes the partition's
   * window. Never throws.
   *
   * @param key    Execution key
   * @param origin Origin whose partition is being completed
   */
  public void complete(ExecutionKey key, ExecutionLogOrigin origin) {
    ExecutionLogPartition partition = new ExecutionLogPartition(key, origin);
    boolean accepted = queue.offer(new ExecutionLogEvent(partition, null, 0L, true));
    if (!accepted) {
      dropsByKey.computeIfAbsent(partition, unused -> new AtomicLong()).incrementAndGet();
    }
  }

  /**
   * Start the daemon consumer thread. Idempotent.
   */
  public final synchronized void start() {
    if (running) {
      return;
    }
    running = true;
    consumerThread = new Thread(this::runLoop, THREAD_NAME);
    consumerThread.setDaemon(true);
    consumerThread.start();
  }

  /**
   * Stop the consumer thread: signal it to stop accepting new poll cycles, let it drain the
   * pending queue and final-flush every live window within a bounded deadline, then join.
   */
  public final synchronized void stop() {
    running = false;
    Thread thread = consumerThread;
    consumerThread = null;
    if (thread == null) {
      return;
    }
    thread.interrupt();
    try {
      thread.join(shutdownDrainMillis + 1000L);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Number of executions this writer currently holds an in-memory window for. Test/diagnostic
   * observability only.
   *
   * @return Live window count
   */
  public int liveWindowCount() {
    return windows.size();
  }

  private void runLoop() {
    while (running) {
      processOneCycle();
    }
    drainAndFinalFlush();
  }

  private void processOneCycle() {
    try {
      ExecutionLogEvent first = queue.poll(flushIntervalMillis, TimeUnit.MILLISECONDS);
      if (first != null) {
        applyEvent(first);
        for (int drained = 1; drained < batchSize; drained++) {
          ExecutionLogEvent next = queue.poll();
          if (next == null) {
            break;
          }
          applyEvent(next);
        }
      }
      flushDueWindows();
      evictIdleWindows();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException exc) {
      log.error("[SCHEDULER][EXECUTION_LOG] Unexpected error in the execution log writer loop", exc);
    }
  }

  private void drainAndFinalFlush() {
    long deadline = clock.millis() + shutdownDrainMillis;
    ExecutionLogEvent event;
    while (clock.millis() < deadline && (event = queue.poll()) != null) {
      try {
        applyEvent(event);
      } catch (RuntimeException exc) {
        log.error("[SCHEDULER][EXECUTION_LOG] Unexpected error draining the execution log writer queue on shutdown", exc);
      }
    }
    for (Map.Entry<ExecutionLogPartition, ExecutionLogWindow> entry : windows.entrySet()) {
      flushWindow(entry.getKey(), entry.getValue());
    }
  }

  private void applyEvent(ExecutionLogEvent event) {
    if (event.complete()) {
      finalizeExecution(event.partition());
    } else {
      ExecutionLogWindow window = windows.computeIfAbsent(event.partition(), unused -> new ExecutionLogWindow(maxLines));
      window.accept(event.line(), event.timestampMillis());
      window.touch(clock.millis());
    }
  }

  private void finalizeExecution(ExecutionLogPartition partition) {
    long drops = drainDropCount(partition);
    ExecutionLogWindow window = windows.get(partition);
    if (window == null && drops == 0) {
      return;
    }
    if (window == null) {
      window = windows.computeIfAbsent(partition, unused -> new ExecutionLogWindow(maxLines));
    }
    if (drops > 0) {
      window.accept(dropMarkerLine(drops), clock.millis());
    }
    flushWindow(partition, window);
    windows.remove(partition);
    lastFlushAtMillis.remove(partition);
  }

  private long drainDropCount(ExecutionLogPartition partition) {
    AtomicLong counter = dropsByKey.remove(partition);
    return counter == null ? 0 : counter.get();
  }

  private String dropMarkerLine(long drops) {
    return "... " + drops + " lines dropped (queue saturated) ...";
  }

  private void flushDueWindows() {
    long now = clock.millis();
    for (Map.Entry<ExecutionLogPartition, ExecutionLogWindow> entry : windows.entrySet()) {
      ExecutionLogPartition partition = entry.getKey();
      ExecutionLogWindow window = entry.getValue();
      if (!window.hasDirtySlots()) {
        continue;
      }
      long lastFlush = lastFlushAtMillis.getOrDefault(partition, 0L);
      boolean intervalElapsed = now - lastFlush >= flushIntervalMillis;
      boolean batchFull = window.dirtySlotCount() >= batchSize;
      if (batchFull || intervalElapsed) {
        flushWindow(partition, window);
      }
    }
  }

  private void evictIdleWindows() {
    long now = clock.millis();
    for (ExecutionLogPartition partition : windows.keySet()) {
      ExecutionLogWindow window = windows.get(partition);
      if (window != null && now - window.lastTouchedMillis() > taskTimeoutMillis) {
        finalizeExecution(partition);
      }
    }
  }

  private void flushWindow(ExecutionLogPartition partition, ExecutionLogWindow window) {
    if (!window.hasDirtySlots()) {
      return;
    }
    try {
      if (window.isFirstFlushPending()) {
        maintainService.launchPrivateMaintain(PURGE_ORIGIN_TARGET, singleKeyParameters(partition));
        window.markFirstFlushDone();
      }

      List<ExecutionLogWindow.LogSlot> toInsert = window.dirtyNeverPersistedSlots();
      if (!toInsert.isEmpty()) {
        maintainService.launchPrivateMaintain(INSERT_TARGET, slotParameters(partition, toInsert));
        window.markPersisted(toInsert);
      }

      List<ExecutionLogWindow.LogSlot> toUpdate = window.dirtyPersistedSlots();
      if (!toUpdate.isEmpty()) {
        maintainService.launchPrivateMaintain(UPDATE_TARGET, slotParameters(partition, toUpdate));
        window.markPersisted(toUpdate);
      }

      lastFlushAtMillis.put(partition, clock.millis());
    } catch (Exception exc) {
      if (!window.isDegraded()) {
        log.error("[SCHEDULER][EXECUTION_LOG] Failed to flush the execution log window for {}", partition, exc);
      }
      window.markDegraded();
    }
  }

  private ObjectNode singleKeyParameters(ExecutionLogPartition partition) {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.set("taskId", singleElementArray(partition.key().taskId()));
    parameters.set("executionId", singleElementArray(partition.key().executionId()));
    parameters.set("origin", singleElementStringArray(partition.origin().code()));
    return parameters;
  }

  private ArrayNode singleElementArray(Integer value) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    array.add(value);
    return array;
  }

  private ArrayNode singleElementStringArray(String value) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    array.add(value);
    return array;
  }

  private ObjectNode slotParameters(ExecutionLogPartition partition, List<ExecutionLogWindow.LogSlot> slots) {
    ArrayNode taskIds = JsonNodeFactory.instance.arrayNode();
    ArrayNode executionIds = JsonNodeFactory.instance.arrayNode();
    ArrayNode origins = JsonNodeFactory.instance.arrayNode();
    ArrayNode sections = JsonNodeFactory.instance.arrayNode();
    ArrayNode slotIndexes = JsonNodeFactory.instance.arrayNode();
    ArrayNode lineNumbers = JsonNodeFactory.instance.arrayNode();
    ArrayNode lineTexts = JsonNodeFactory.instance.arrayNode();
    ArrayNode logDates = JsonNodeFactory.instance.arrayNode();

    for (ExecutionLogWindow.LogSlot slot : slots) {
      taskIds.add(partition.key().taskId());
      executionIds.add(partition.key().executionId());
      origins.add(partition.origin().code());
      sections.add(String.valueOf(slot.section()));
      slotIndexes.add(slot.slot());
      lineNumbers.add(slot.lineNumber());
      lineTexts.add(slot.text());
      logDates.add(DateUtil.dat2WebTimestampMs(new Date(slot.timestampMillis())));
    }

    ObjectNode parameters = queryUtil.getParameters();
    parameters.set("taskId", taskIds);
    parameters.set("executionId", executionIds);
    parameters.set("origin", origins);
    parameters.set("section", sections);
    parameters.set("slot", slotIndexes);
    parameters.set("lineNumber", lineNumbers);
    parameters.set("lineText", lineTexts);
    parameters.set("logDate", logDates);
    return parameters;
  }

  private record ExecutionLogEvent(ExecutionLogPartition partition, String line, long timestampMillis, boolean complete) {
  }
}
