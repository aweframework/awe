package com.almis.awe.scheduler.factory;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.builder.task.*;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import org.quartz.Scheduler;

public class TaskFactory {
  // Private constructor
  private TaskFactory() {
  }

  /**
   * Generate a task using a task builder depending on task data
   *
   * @param task Task data
   * @return Task
   */
  public static TaskBuilder getInstance(Task task, File file, Scheduler scheduler) throws AWException {
    // Depending on task data, use a builder
    switch (TaskLaunchType.valueOf(task.getLaunchType())) {
      // Scheduled task
      case SCHEDULED:
        return new ScheduledTaskBuilder(task).setScheduler(scheduler);
      case FILE_TRACKING:
        return new FileTaskBuilder(task, file).setScheduler(scheduler);
      case DEPENDENCY:
        return new DependantTaskBuilder(task).setScheduler(scheduler);
      case MANUAL:
      default:
        return new ManualTaskBuilder(task).setScheduler(scheduler);
    }
  }
}
