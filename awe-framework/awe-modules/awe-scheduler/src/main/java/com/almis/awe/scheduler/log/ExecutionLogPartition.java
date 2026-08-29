package com.almis.awe.scheduler.log;

import com.almis.awe.scheduler.enums.ExecutionLogOrigin;

/**
 * Write-path key identifying one origin's slot space for one execution. Each origin remains a
 * single writer over its own {@link ExecutionLogWindow} instance, keyed by this partition
 * (single-writer invariant preserved per partition, not weakened by the second origin).
 *
 * @param key    Execution key
 * @param origin Process that owns this partition's slot space
 */
public record ExecutionLogPartition(ExecutionKey key, ExecutionLogOrigin origin) {
}
