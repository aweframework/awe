package com.almis.awe.scheduler.builder.task;

import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;

@Slf4j
public class ManualTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public ManualTaskBuilder(Task task) {
    setData(task);
    getTask().setGroup(MANUAL_GROUP);
  }
}
