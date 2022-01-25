package com.almis.awe.scheduler.builder.task;

import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;

@Slf4j
public class DependantTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public DependantTaskBuilder(Task task) {
    setData(task);
    getTask().setGroup(DEPENDENCY_GROUP);
  }
}
