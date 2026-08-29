package com.almis.awe.scheduler.log;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Set;

/**
 * Captures, retrieves, and purges scheduler task execution logs, independent of the underlying
 * storage (on-disk file or database table).
 */
public interface ExecutionLogStore {

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
   * Build the execution log grid cell payload (locator) for this execution.
   *
   * @param key Execution key
   * @return Locator node
   * @throws AWException Error building the locator node
   */
  ObjectNode locatorNode(ExecutionKey key) throws AWException;

  /**
   * Contribute the client actions that point the log viewer at this execution.
   *
   * @param locatorValue Locator value produced by {@link #locatorNode(ExecutionKey)}
   * @param key          Execution key
   * @param serviceData  Service data to append client actions to
   * @throws AWException Error building the viewer selection actions
   */
  void applyViewerSelection(String locatorValue, ExecutionKey key, ServiceData serviceData) throws AWException;

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
