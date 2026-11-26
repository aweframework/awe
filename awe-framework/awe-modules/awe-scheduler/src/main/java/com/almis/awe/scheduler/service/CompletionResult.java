package com.almis.awe.scheduler.service;

import com.almis.awe.scheduler.enums.TaskStatus;

/**
 * Result of the completion of a deferred execution
 *
 * @param outcome       What happened to the completion
 * @param currentStatus Status of the execution: the applied one when it was closed, the one it has otherwise
 *                      (null for an unknown execution)
 */
public record CompletionResult(Outcome outcome, TaskStatus currentStatus) {

  /**
   * Outcome of the completion
   */
  public enum Outcome {
    /** This call closed the execution */
    CLOSED,
    /** The deferred execution was already closed (by another call, its due date or a manual stop) */
    NOT_RUNNING,
    /** The execution never answered deferred */
    NOT_DEFERRED,
    /** There is no such execution */
    UNKNOWN
  }
}
