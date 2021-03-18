package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.log4j.Log4j2;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;

@Log4j2
public class DependantTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public DependantTaskBuilder(Task task) throws AWException {
    setData(task);
    getTask().setGroup(DEPENDENCY_GROUP);
  }
}
