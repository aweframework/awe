package com.almis.awe.scheduler.enums;

/**
 * Selects the active {@code ExecutionLogStore} adapter for scheduler task execution logs.
 * <p>
 * {@link #FILE} reproduces the pre-existing on-disk execution log behavior exactly and is the
 * default. {@link #DATABASE} persists execution log lines to the shared scheduler datasource
 * instead, enabling cross-replica and remote-scheduler reads.
 *
 * @author awe
 */
public enum ExecutionLogStoreType {
  FILE,
  DATABASE;
}
