package com.almis.awe.scheduler.enums;

import java.util.Arrays;
import java.util.Optional;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.SCHEDULED_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.WEBHOOK_GROUP;

/**
 * Origin of an execution, derived from the launch group stored with it ({@code AweSchExe.GrpTsk}): its schedule, a
 * manual launch, a file, the end of a parent task in a flow or a webhook
 */
public enum ExecutionOrigin {
  SCHEDULED("scheduled", SCHEDULED_GROUP),
  MANUAL("manual", MANUAL_GROUP),
  FILE("file", FILE_TRACKING_GROUP),
  DEPENDENCY("dependency", DEPENDENCY_GROUP),
  WEBHOOK("webhook", WEBHOOK_GROUP);

  private final String code;
  private final String group;

  ExecutionOrigin(String code, String group) {
    this.code = code;
    this.group = group;
  }

  /**
   * Language independent code of the origin
   *
   * @return Code
   */
  public String getCode() {
    return code;
  }

  /**
   * Launch group stored with the executions of this origin
   *
   * @return Launch group
   */
  public String getGroup() {
    return group;
  }

  /**
   * Origin of the executions launched by a group
   *
   * @param group Launch group
   * @return Origin, empty when the group is not a launch group
   */
  public static Optional<ExecutionOrigin> fromGroup(String group) {
    return Arrays.stream(values()).filter(origin -> origin.group.equalsIgnoreCase(String.valueOf(group))).findFirst();
  }

  /**
   * Origin of a code
   *
   * @param code Origin code
   * @return Origin, empty when the code is unknown
   */
  public static Optional<ExecutionOrigin> fromCode(String code) {
    return Arrays.stream(values()).filter(origin -> origin.code.equalsIgnoreCase(String.valueOf(code))).findFirst();
  }
}
