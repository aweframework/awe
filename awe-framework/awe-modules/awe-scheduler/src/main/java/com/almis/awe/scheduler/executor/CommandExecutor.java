package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;

/**
 * Strategy contract for running a scheduler command task, either locally
 * (via the JVM process API) or remotely (via SSH). Implementations must
 * never throw: any failure MUST be logged and reported as a non-zero exit
 * code so callers can keep the same fail-safe behaviour regardless of the
 * resolved execution path.
 */
public interface CommandExecutor {

  /**
   * Run a command task
   *
   * @param task    Task
   * @param envp    Environment variables
   * @param timeout Timeout (seconds)
   * @return exit code
   */
  Integer execute(Task task, String[] envp, long timeout);
}
