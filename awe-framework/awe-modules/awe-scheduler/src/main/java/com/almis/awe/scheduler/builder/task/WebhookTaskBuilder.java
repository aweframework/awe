package com.almis.awe.scheduler.builder.task;

import com.almis.awe.scheduler.bean.task.Task;

import static com.almis.awe.scheduler.constant.TaskConstants.WEBHOOK_GROUP;

/**
 * Builder of the tasks launched by a webhook: like a manual task, it has no trigger of its own
 */
public class WebhookTaskBuilder extends TaskBuilder {

  /**
   * Constructor
   *
   * @param task Task data
   */
  public WebhookTaskBuilder(Task task) {
    setData(task);
    getTask().setGroup(WEBHOOK_GROUP);
  }
}
