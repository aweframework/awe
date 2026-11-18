package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.enums.TaskLaunchType;

import static com.almis.awe.scheduler.constant.TaskConstants.*;
import static com.almis.awe.scheduler.enums.TaskLaunchType.*;

public class TaskUtil {
  private TaskUtil() {}

  /**
   * Returns the group for the given task
   *
   * @param taskLaunchType
   */
  public static String getGroupForLaunchType(Integer taskLaunchType) {
    switch (TaskLaunchType.valueOf(taskLaunchType)) {
      case SCHEDULED:
        return SCHEDULED_GROUP;
      case FILE_TRACKING:
        return FILE_TRACKING_GROUP;
      case WEBHOOK:
        return WEBHOOK_GROUP;
      default:
        return MANUAL_GROUP;
    }
  }

  /**
   * Returns the group for the given task
   *
   * @param group
   */
  public static TaskLaunchType getLaunchTypeForGroup(String group) {
    switch (group) {
      case SCHEDULED_GROUP:
        return SCHEDULED;
      case FILE_TRACKING_GROUP:
        return FILE_TRACKING;
      case WEBHOOK_GROUP:
        return WEBHOOK;
      default:
        return MANUAL;
    }
  }

  /**
   * A task launched on demand has no trigger of its own: it is not added to the scheduler, it only runs when
   * something launches it (an operator, a webhook)
   *
   * @param taskLaunchType Task launch type
   * @return True for the manual and webhook tasks
   */
  public static boolean isLaunchedOnDemand(Integer taskLaunchType) {
    return MANUAL.getValue().equals(taskLaunchType) || WEBHOOK.getValue().equals(taskLaunchType);
  }
}
