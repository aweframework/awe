package com.almis.awe.scheduler.builder.task;

import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;

@Slf4j
public class ScheduledTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public ScheduledTaskBuilder(Task task) {
    setData(task);
    getTask().setGroup(SCHEDULED_GROUP);
    getTask().setLauncher("scheduler");
  }
}
