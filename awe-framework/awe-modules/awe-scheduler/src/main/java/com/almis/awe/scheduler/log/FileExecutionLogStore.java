package com.almis.awe.scheduler.log;

import com.almis.awe.builder.client.FilterActionBuilder;
import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.service.EncodeService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_SEPARATOR;

/**
 * On-disk {@link ExecutionLogStore} adapter. Pure extraction of the pre-existing execution log
 * logic from {@code TaskDAO}: every method reproduces the current behavior verbatim, so file mode
 * stays byte-for-byte unchanged.
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
    try (BufferedReader reader = Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8)) {
      String lineString;
      while ((lineString = reader.readLine()) != null) {
        if (offset <= totalLines) {
          content.add(lineString);
        }
        totalLines++;
      }
    } catch (IOException exc) {
      throw new AWException("Error reading execution log file: " + logFilePath, exc);
    }

    return new ExecutionLogPage(content, false, totalLines, 0L);
  }

  @Override
  public ObjectNode locatorNode(ExecutionKey key) throws AWException {
    Path executionLogFilePath = getExecutionLogFilePath(key.taskId(), key.executionId());
    ObjectNode logFileNode = JsonNodeFactory.instance.objectNode();
    logFileNode.put(JSON_VALUE_PARAMETER, EncodeService.encodeSymmetric(executionLogFilePath.toString()));
    logFileNode.put(JSON_STYLE_PARAMETER, "no-btn");
    logFileNode.put(JSON_TITLE_PARAMETER, "SCHEDULER_SHOW_EXECUTION_LOG");
    logFileNode.put(JSON_ICON_PARAMETER, "file-text-o text-info");
    logFileNode.put(JSON_LABEL_PARAMETER, "");
    return logFileNode;
  }

  @Override
  public void applyViewerSelection(String locatorValue, ExecutionKey key, ServiceData serviceData) {
    serviceData.addClientAction(new ClientAction("reset").setTarget("executionLogViewer").setSilent(true));
    serviceData.addClientAction(new SelectActionBuilder("path", locatorValue).setSilent(true).build());
    serviceData.addClientAction(new FilterActionBuilder("executionLogViewer").setSilent(true).build());
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

    Path logPath = Paths.get(executionLogPath);
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
    return Paths.get(executionLogPath, "execution_" + taskId + TASK_SEPARATOR + executionId + ".log");
  }
}
