package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Logs a task's command output stream (stdout/stderr) line by line.
 * Extracted from CommandDAO's former private logHandler method so that both
 * the local and remote (SSH) command executors emit output in the exact
 * same format: "[triggerKey] [type] line".
 */
@Slf4j
public class CommandStreamLogger {

  /**
   * Log a command output/error stream
   *
   * @param task        Task
   * @param inputStream Input stream
   * @param type        Handler type (e.g. OUTPUT, ERROR)
   */
  public void log(Task task, InputStream inputStream, String type) {
    String line;
    try {
      try (InputStreamReader isr = new InputStreamReader(inputStream);
           BufferedReader br = new BufferedReader(isr)) {
        while ((line = br.readLine()) != null) {
          log.info("[{}] [{}] {}", task.getTrigger().getKey(), type, line);
        }
      }
    } catch (Exception exc) {
      log.error("[Command] Failed to collect the command output: {}", task.getTrigger().getKey(), exc);
    }
  }
}
