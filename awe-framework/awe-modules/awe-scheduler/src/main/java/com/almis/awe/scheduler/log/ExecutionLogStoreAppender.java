package com.almis.awe.scheduler.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.EvaluatorFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.almis.awe.scheduler.component.SchedulerTaskLogEvaluator;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;

import java.nio.charset.StandardCharsets;

/**
 * Programmatically-installed Logback appender that feeds captured execution log lines into the
 * database-mode {@link ExecutionLogStore} (ADR-1). Reuses the exact
 * {@link SchedulerTaskLogEvaluator} admission rule the file-mode {@code SCHEDULER_EXECUTION}
 * sifting appender already applies, so both stores observe the identical event set.
 * <p>
 * Installed and lifecycle-managed by {@code ExecutionLogAppenderInstaller}, not by
 * {@code scheduler-log.xml}.
 */
final class ExecutionLogStoreAppender extends AppenderBase<ILoggingEvent> {

  private final Encoder<ILoggingEvent> encoder;
  private final ExecutionLogStore store;
  private final int maxLineLength;

  /**
   * Constructor
   *
   * @param encoder       Already-started encoder, built from {@code awe.scheduler.execution-log-pattern}
   * @param store         Execution log store to feed
   * @param maxLineLength Defensive per-line character cap
   */
  ExecutionLogStoreAppender(Encoder<ILoggingEvent> encoder, ExecutionLogStore store, int maxLineLength) {
    this.encoder = encoder;
    this.store = store;
    this.maxLineLength = maxLineLength;

    EvaluatorFilter<ILoggingEvent> filter = new EvaluatorFilter<>();
    filter.setEvaluator(new SchedulerTaskLogEvaluator());
    filter.setOnMismatch(FilterReply.DENY);
    filter.setOnMatch(FilterReply.NEUTRAL);
    filter.start();
    addFilter(filter);
  }

  @Override
  protected void append(ILoggingEvent event) {
    String mdcKey = event.getMDCPropertyMap().get(TaskConstants.LOG_BY_TASK_EXECUTION);
    ExecutionKey key = ExecutionKey.of(mdcKey);
    if (key == null) {
      return;
    }

    ExecutionLogOrigin origin = ExecutionLogOrigin.fromCode(event.getMDCPropertyMap().get(TaskConstants.EXECUTION_LOG_ORIGIN));
    long timestampMillis = event.getTimeStamp();

    String rendered = new String(encoder.encode(event), StandardCharsets.UTF_8);
    for (String physicalLine : rendered.split("\\R")) {
      store.append(new ExecutionLogLine(key, origin, timestampMillis, cap(physicalLine, maxLineLength)));
    }
  }

  private static final String ELLIPSIS = "…";

  /**
   * Truncate a line to at most {@code maxLength} characters, appending an ellipsis marker to make
   * the truncation visible, and backing off by one extra character when the ellipsis boundary
   * would otherwise split a UTF-16 surrogate pair.
   *
   * @param line      Line to cap
   * @param maxLength Maximum character length
   * @return Capped line, ending with an ellipsis marker when truncated
   */
  private static String cap(String line, int maxLength) {
    if (line.length() <= maxLength) {
      return line;
    }

    if (maxLength <= 0) {
      return "";
    }

    int cutIndex = maxLength - 1;
    if (cutIndex > 0 && cutIndex < line.length()
      && Character.isHighSurrogate(line.charAt(cutIndex - 1))
      && Character.isLowSurrogate(line.charAt(cutIndex))) {
      cutIndex--;
    }
    return line.substring(0, cutIndex) + ELLIPSIS;
  }
}
