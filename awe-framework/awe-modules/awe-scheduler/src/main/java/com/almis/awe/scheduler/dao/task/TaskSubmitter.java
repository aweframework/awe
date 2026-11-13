package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;

/**
 * Hands a ready-to-run task to the scheduler. Passed on every launch call so the caller decides which
 * scheduling entry point is used (the {@code TaskDAO} facade passes its own {@code addTaskToScheduler}).
 */
@FunctionalInterface
public interface TaskSubmitter {

  /**
   * Submit a task to the scheduler
   *
   * @param task Task with its trigger and job already defined
   * @throws AWException Error scheduling the task
   */
  void submit(Task task) throws AWException;
}
