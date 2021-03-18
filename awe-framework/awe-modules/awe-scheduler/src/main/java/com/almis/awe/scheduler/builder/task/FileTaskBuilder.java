package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.log4j.Log4j2;

import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;

@Log4j2
public class FileTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public FileTaskBuilder(Task task, File file) throws AWException {
    setData(task);
    task.setGroup(FILE_TRACKING_GROUP);
    task.setFile(file);
  }
}
