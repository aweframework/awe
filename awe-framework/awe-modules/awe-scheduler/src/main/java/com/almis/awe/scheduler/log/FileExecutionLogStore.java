package com.almis.awe.scheduler.log;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.almis.awe.scheduler.constant.TaskConstants.TASK_SEPARATOR;

/**
 * On-disk {@link ExecutionLogStore} adapter. The log viewer reads these files through the scheduler
 * {@code get-execution-log} action by task and execution id ({@link #read(ExecutionKey, int, String)}),
 * so no file path is ever sent to or accepted from the client.
 */
@Slf4j
public class FileExecutionLogStore implements ExecutionLogStore {

  private final String executionLogPath;

  /**
   * Constructor
   *
   * @param executionLogPath Execution log directory path
   */
  public FileExecutionLogStore(String executionLogPath) {
    this.executionLogPath = executionLogPath;
  }

  /**
   * {@inheritDoc}
   * <p>
   * No-op: the {@code SiftingAppender} already owns the write side in file mode, unchanged.
   */
  @Override
  public void append(ExecutionLogLine line) {
    // Intentionally empty
  }

  /**
   * {@inheritDoc}
   * <p>
   * No-op: nothing to flush in file mode.
   */
  @Override
  public void complete(ExecutionKey key, ExecutionLogOrigin origin) {
    // Intentionally empty
  }

  @Override
  public ExecutionLogPage read(ExecutionKey key, int offset, String clientVersion) throws AWException {
    Path logFilePath = getExecutionLogFilePath(key.taskId(), key.executionId());
    if (!logFilePath.toFile().exists()) {
      return ExecutionLogPage.empty();
    }

    List<String> content = new ArrayList<>();
    int totalLines = 0;
    try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
      String lineString;
      while ((lineString = reader.readLine()) != null) {
        if (offset <= totalLines) {
          content.add(lineString);
        }
        totalLines++;
      }
    } catch (IOException exc) {
      throw new AWException("Error reading the log of task " + key.taskId() + " and execution " + key.executionId(), exc);
    }

    return new ExecutionLogPage(content, false, totalLines, 0L);
  }

  @Override
  public void purge(Integer taskId, Collection<Integer> executionIds) {
    for (Integer executionId : executionIds) {
      try {
        Path logFilePath = getExecutionLogFilePath(taskId, executionId);
        if (logFilePath.toFile().exists()) {
          Files.delete(logFilePath);
        }
      } catch (Exception exc) {
        log.warn("Could not delete log file for task {} and execution {}", taskId, executionId, exc);
      }
    }
  }

  @Override
  public void purgeOrphans(Set<ExecutionKey> validExecutions) {
    Set<String> validLogFiles = validExecutions
      .stream()
      .map(key -> "execution_" + key.taskId() + TASK_SEPARATOR + key.executionId() + ".log")
      .collect(Collectors.toSet());

    Path logPath = Path.of(executionLogPath);
    if (logPath.toFile().exists()) {
      try (Stream<Path> pathStream = Files.list(logPath)) {
        pathStream.forEach(path -> {
          String name = path.toFile().getName();
          if (!validLogFiles.contains(name)) {
            try {
              Files.delete(path);
            } catch (Exception exc) {
              log.error("Error deleting log file: {}", name, exc);
            }
          }
        });
      } catch (IOException exc) {
        log.warn("Error reading execution log path: {}", executionLogPath, exc);
      }
    }
  }

  /**
   * Get execution log file path
   *
   * @param taskId      Task id
   * @param executionId Execution id
   * @return Log file path
   */
  private Path getExecutionLogFilePath(Integer taskId, Integer executionId) {
    return Path.of(executionLogPath, "execution_" + taskId + TASK_SEPARATOR + executionId + ".log");
  }
}
