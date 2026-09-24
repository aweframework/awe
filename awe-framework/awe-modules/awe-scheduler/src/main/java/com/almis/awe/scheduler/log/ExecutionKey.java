package com.almis.awe.scheduler.log;

import com.almis.awe.scheduler.constant.TaskConstants;

import java.util.Objects;

/**
 * Identifies a single scheduler task execution (task identifier + execution identifier).
 * <p>
 * {@link #mdcKey()} renders the same {@code "{taskId}-{executionId}"} shape used today as the
 * MDC discriminator value ({@code logByTaskExecution}) and as the execution log file name suffix.
 *
 * @param taskId      Task identifier
 * @param executionId Execution identifier
 */
public record ExecutionKey(Integer taskId, Integer executionId) {

  public ExecutionKey {
    Objects.requireNonNull(taskId, "taskId must not be null");
    Objects.requireNonNull(executionId, "executionId must not be null");
  }

  /**
   * Render the MDC/file-name discriminator value for this execution.
   *
   * @return {@code "{taskId}-{executionId}"}
   */
  public String mdcKey() {
    return taskId + TaskConstants.TASK_SEPARATOR + executionId;
  }

  /**
   * Parse an MDC/file-name discriminator value back into an execution key.
   *
   * @param mdcKey Discriminator value, as rendered by {@link #mdcKey()}
   * @return Parsed execution key, or {@code null} when {@code mdcKey} is null, blank, or does not
   *         match the {@code "{taskId}-{executionId}"} shape
   */
  public static ExecutionKey of(String mdcKey) {
    if (mdcKey == null || mdcKey.isBlank()) {
      return null;
    }

    String[] parts = mdcKey.split(TaskConstants.TASK_SEPARATOR);
    if (parts.length != 2) {
      return null;
    }

    try {
      return new ExecutionKey(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
    } catch (NumberFormatException exc) {
      return null;
    }
  }
}
