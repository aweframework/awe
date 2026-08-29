package com.almis.awe.scheduler.log;

import java.util.List;

/**
 * A display window of execution log lines returned by an {@link ExecutionLogStore}.
 *
 * @param lines        Display lines. In append mode ({@code replace} is {@code false}), these are
 *                     the new lines from the requested offset onward, meant to be concatenated to
 *                     the caller's existing content. In replace mode ({@code replace} is {@code
 *                     true}), these are the complete current window, meant to atomically overwrite
 *                     the caller's existing content in a single repaint
 * @param replace      {@code true} when the caller must atomically swap its content with {@code
 *                     lines} instead of appending them (the window was truncated, or re-ordered in
 *                     a way that makes a plain append unsafe, since the last read). Always {@code
 *                     false} when the requested offset is 0: an empty caller treats a full window
 *                     as an append
 * @param totalLines   Total number of lines the execution actually produced
 * @param omittedLines Number of lines dropped by the window (0 means the window is intact)
 * @param version      Fingerprint of the merged line order the caller now holds after applying
 *                     {@code lines} (multi-origin executions only); the caller echoes it back on
 *                     the next read so the store can decide append safety without keeping any
 *                     server-side per-execution state. {@code null} for single-origin executions,
 *                     where base ADR-6 semantics apply unchanged
 */
public record ExecutionLogPage(List<String> lines, boolean replace, long totalLines, long omittedLines, String version) {

  /**
   * Convenience constructor for single-origin pages, which never carry a version.
   *
   * @param lines        Display lines
   * @param replace      Replace flag
   * @param totalLines   Total number of lines the execution actually produced
   * @param omittedLines Number of lines dropped by the window
   */
  public ExecutionLogPage(List<String> lines, boolean replace, long totalLines, long omittedLines) {
    this(lines, replace, totalLines, omittedLines, null);
  }

  /**
   * Empty page: no lines, no replace, nothing produced yet.
   *
   * @return Empty execution log page
   */
  public static ExecutionLogPage empty() {
    return new ExecutionLogPage(List.of(), false, 0L, 0L, null);
  }
}
