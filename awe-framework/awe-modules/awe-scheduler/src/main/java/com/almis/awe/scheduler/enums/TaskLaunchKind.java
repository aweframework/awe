package com.almis.awe.scheduler.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * How a task is launched, as the task catalog shows and filters it: its launch type, except a manual task with
 * parents, which is launched after them (a dependency). Each kind has the icon of its «When» summary and the label
 * (locale key) of its option in the launch filter
 */
public enum TaskLaunchKind {
  MANUAL("manual", "lucide:hand", "ENUM_LAUNCH_MANUAL"),
  SCHEDULED("scheduled", "lucide:clock", "ENUM_LAUNCH_SCHEDULED"),
  FILE("file", "lucide:file-down", "ENUM_LAUNCH_FILE"),
  DEPENDENCY("dependency", "lucide:corner-down-right", "ENUM_LAUNCH_DEPENDENCY"),
  WEBHOOK("webhook", "lucide:webhook", "ENUM_LAUNCH_WEBHOOK");

  private final String code;
  private final String icon;
  private final String label;

  TaskLaunchKind(String code, String icon, String label) {
    this.code = code;
    this.icon = icon;
    this.label = label;
  }

  /**
   * Code of the kind (value of the launch filter)
   *
   * @return Code
   */
  public String getCode() {
    return code;
  }

  /**
   * Icon of the «When» summary of the tasks of this kind
   *
   * @return Icon
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Locale key of the label of the kind
   *
   * @return Label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Kind of a task
   *
   * @param launchType Launch type of the task ({@code TaskLaunchType} value)
   * @param hasParents The task runs after other tasks
   * @return Kind of the task
   */
  public static TaskLaunchKind of(Integer launchType, boolean hasParents) {
    if (TaskLaunchType.SCHEDULED.getValue().equals(launchType)) {
      return SCHEDULED;
    } else if (TaskLaunchType.FILE_TRACKING.getValue().equals(launchType)) {
      return FILE;
    } else if (TaskLaunchType.WEBHOOK.getValue().equals(launchType)) {
      return WEBHOOK;
    }
    return hasParents || TaskLaunchType.DEPENDENCY.getValue().equals(launchType) ? DEPENDENCY : MANUAL;
  }

  /**
   * Kind of a code
   *
   * @param code Kind code
   * @return Kind, empty when the code is unknown
   */
  public static Optional<TaskLaunchKind> fromCode(String code) {
    return Arrays.stream(values()).filter(kind -> kind.code.equals(code)).findFirst();
  }
}
