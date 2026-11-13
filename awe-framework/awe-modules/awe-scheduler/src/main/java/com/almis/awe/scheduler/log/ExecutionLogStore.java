package com.almis.awe.scheduler.log;

import com.almis.awe.builder.client.FilterActionBuilder;
import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.client.UpdateControllerActionBuilder;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Set;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;

/**
 * Captures, retrieves, and purges scheduler task execution logs, independent of the underlying
 * storage (on-disk file or database table).
 */
public interface ExecutionLogStore {

  /**
   * Log viewer widget of the execution screen
   */
  String EXECUTION_LOG_VIEWER_ID = "executionLogViewer";

  /**
   * Scheduler action that serves the execution log by task and execution id
   */
  String GET_EXECUTION_LOG_ACTION = "get-execution-log";

  /**
   * Append one already-formatted physical line for an execution, carrying the origin that
   * captured it and the event timestamp. Must be non-blocking and never throw.
   *
   * @param line Execution log line to append
   */
  void append(ExecutionLogLine line);

  /**
   * Signal that no further lines will arrive for the given origin's partition of this execution;
   * flush and release any in-memory state owned by that partition. Never throws.
   *
   * @param key    Execution key
   * @param origin Origin whose partition is being completed
   */
  void complete(ExecutionKey key, ExecutionLogOrigin origin);

  /**
   * Read the display window from a line offset.
   *
   * @param key           Execution key
   * @param offset        Line offset to read from
   * @param clientVersion Window version the caller currently holds, as previously returned in
   *                      {@link ExecutionLogPage#version()}; {@code null} when the caller holds
   *                      none yet (offset 0) or does not support it. Only meaningful for
   *                      multi-origin executions
   * @return Execution log page
   * @throws AWException Error reading the stored log
   */
  ExecutionLogPage read(ExecutionKey key, int offset, String clientVersion) throws AWException;

  /**
   * Build the execution log grid cell payload (locator) for this execution: an opaque
   * {@code "{taskId}-{executionId}"} token, never a storage location such as a file path.
   *
   * @param key Execution key
   * @return Locator node
   */
  default ObjectNode locatorNode(ExecutionKey key) {
    ObjectNode logNode = JsonNodeFactory.instance.objectNode();
    logNode.put(JSON_VALUE_PARAMETER, key.mdcKey());
    logNode.put(JSON_STYLE_PARAMETER, "no-btn");
    logNode.put(JSON_TITLE_PARAMETER, "SCHEDULER_SHOW_EXECUTION_LOG");
    logNode.put(JSON_ICON_PARAMETER, "file-text-o text-info");
    logNode.put(JSON_LABEL_PARAMETER, "");
    return logNode;
  }

  /**
   * Contribute the client actions that point the log viewer at this execution: the viewer reads
   * through the scheduler {@code get-execution-log} action (permission {@code scheduler.read}) by
   * task and execution id, which resolves the log through {@link #read(ExecutionKey, int, String)}.
   *
   * @param locatorValue Locator value produced by {@link #locatorNode(ExecutionKey)}
   * @param key          Execution key
   * @param serviceData  Service data to append client actions to
   */
  default void applyViewerSelection(String locatorValue, ExecutionKey key, ServiceData serviceData) {
    serviceData.addClientAction(new ClientAction("reset").setTarget(EXECUTION_LOG_VIEWER_ID).setSilent(true));
    serviceData.addClientAction(new UpdateControllerActionBuilder(EXECUTION_LOG_VIEWER_ID, "serverAction", GET_EXECUTION_LOG_ACTION).setSilent(true).build());
    serviceData.addClientAction(new SelectActionBuilder("execution-log-task-id", key.taskId()).setSilent(true).build());
    serviceData.addClientAction(new SelectActionBuilder("execution-log-execution-id", key.executionId()).setSilent(true).build());
    serviceData.addClientAction(new FilterActionBuilder(EXECUTION_LOG_VIEWER_ID).setSilent(true).build());
  }

  /**
   * Remove everything stored for the given executions of a task.
   *
   * @param taskId       Task identifier
   * @param executionIds Execution identifiers to purge
   * @throws AWException Error purging the stored log
   */
  void purge(Integer taskId, Collection<Integer> executionIds) throws AWException;

  /**
   * Remove stored content that no longer maps to a live execution (startup housekeeping).
   *
   * @param validExecutions Execution keys that are still valid and must be kept
   * @throws AWException Error purging orphaned content
   */
  void purgeOrphans(Set<ExecutionKey> validExecutions) throws AWException;
}
