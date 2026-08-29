package com.almis.awe.scheduler.enums;

/**
 * Origin of a captured execution-log line: which process wrote it.
 * <p>
 * {@link #SCHEDULER} tags lines produced on the scheduler's own job thread.
 * {@link #APPLICATION} tags lines produced on an application HTTP worker thread while handling a
 * scheduler-initiated remote maintain callback. Each origin owns a disjoint {@code Src} row space
 * (single-writer invariant preserved per partition).
 *
 * @author awe
 */
public enum ExecutionLogOrigin {
  SCHEDULER("S"),
  APPLICATION("A");

  private final String code;

  ExecutionLogOrigin(String code) {
    this.code = code;
  }

  /**
   * Storage code persisted in the {@code Src} column.
   *
   * @return Single-character storage code
   */
  public String code() {
    return code;
  }

  /**
   * Resolve the origin for a stored {@code Src} code, defaulting to {@link #SCHEDULER} when the
   * code is {@code null} or unrecognized.
   *
   * @param code Storage code, as persisted in the {@code Src} column
   * @return Matching origin, or {@link #SCHEDULER} when unresolved
   */
  public static ExecutionLogOrigin fromCode(String code) {
    for (ExecutionLogOrigin origin : values()) {
      if (origin.code.equals(code)) {
        return origin;
      }
    }
    return SCHEDULER;
  }
}
