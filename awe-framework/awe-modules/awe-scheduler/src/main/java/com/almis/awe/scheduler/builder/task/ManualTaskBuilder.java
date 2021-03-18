package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.log4j.Log4j2;

import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;

@Log4j2
public class ManualTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public ManualTaskBuilder(Task task) throws AWException {
    setData(task);
    getTask().setGroup(MANUAL_GROUP);
  }
}
