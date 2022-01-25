package com.almis.awe.scheduler.builder.task;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;

@Slf4j
public class FileTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   * @param file File
   */
  public FileTaskBuilder(Task task, File file) {
    setData(task);
    task.setGroup(FILE_TRACKING_GROUP);
    task.setFile(file);
  }
}
