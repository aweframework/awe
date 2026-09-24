package com.almis.awe.scheduler.log;

import com.almis.awe.scheduler.enums.ExecutionLogOrigin;

/**
 * One captured physical execution-log line, carrying the event timestamp and the origin it was
 * captured under. Produced by {@code ExecutionLogStoreAppender} and consumed by the write-path
 * port ({@code ExecutionLogStore#append(ExecutionLogLine)}).
 *
 * @param key             Execution key
 * @param origin          Process that captured this line
 * @param timestampMillis Event timestamp (logging event time, not flush time), epoch millis
 * @param text            Rendered physical line text
 */
public record ExecutionLogLine(ExecutionKey key, ExecutionLogOrigin origin, long timestampMillis, String text) {
}
