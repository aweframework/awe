package com.almis.awe.scheduler.enums;

/**
 * Who closed a deferred execution (column CmpSrc of AweSchExeExt)
 */
public enum CompletionSource {
  /**
   * The external system, through the completion API or the completion service
   */
  API,
  /**
   * The deadline watcher, because the execution passed its due date
   */
  DEADLINE,
  /**
   * A user, with the Stop of the task
   */
  USER
}
