package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.log4j.Log4j2;

import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;

@Log4j2
public class ScheduledTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public ScheduledTaskBuilder(Task task) throws AWException {
    setData(task);
    getTask().setGroup(SCHEDULED_GROUP);
    getTask().setLauncher("scheduler");
  }
}
