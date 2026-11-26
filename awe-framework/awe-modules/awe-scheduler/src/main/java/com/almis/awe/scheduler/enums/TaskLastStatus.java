package com.almis.awe.scheduler.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Option of the last status filter of the task catalog: the status of the last execution of a task. An error is also
 * an execution stopped or interrupted, as in the activity screen; running excludes the executions waiting for the
 * confirmation of a deferred answer, which are waiting; none is a task without saved executions
 */
public enum TaskLastStatus {
  OK("ok", "ENUM_STATUS_OK"),
  WARNING("warning", "ENUM_STATUS_WARNING"),
  ERROR("error", "ENUM_STATUS_ERROR"),
  RUNNING("running", "ENUM_STATUS_RUNNING"),
  WAITING("waiting", "SCREEN_TEXT_ACTIVITY_WAITING"),
  QUEUED("queued", "ENUM_STATUS_QUEUED"),
  NONE("none", "SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS");

  private final String code;
  private final String label;

  TaskLastStatus(String code, String label) {
    this.code = code;
    this.label = label;
  }

  /**
   * Code of the option (value of the filter)
   *
   * @return Code
   */
  public String getCode() {
    return code;
  }

  /**
   * Locale key of the label of the option
   *
   * @return Label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Option of the last execution of a task
   *
   * @param status  Status of the last execution ({@link TaskStatus} value), null without executions
   * @param waiting The last execution is waiting for the confirmation of a deferred answer
   * @return Option, empty for a status no option keeps (information)
   */
  public static Optional<TaskLastStatus> of(Integer status, boolean waiting) {
    if (status == null) {
      return Optional.of(NONE);
    }
    return switch (TaskStatus.valueOf(status)) {
      case JOB_OK -> Optional.of(OK);
      case JOB_WARNING -> Optional.of(WARNING);
      case JOB_ERROR, JOB_STOPPED, JOB_INTERRUPTED -> Optional.of(ERROR);
      case JOB_RUNNING -> Optional.of(waiting ? WAITING : RUNNING);
      case JOB_QUEUED -> Optional.of(QUEUED);
      default -> Optional.empty();
    };
  }

  /**
   * Option of a code
   *
   * @param code Option code
   * @return Option, empty when the code is unknown
   */
  public static Optional<TaskLastStatus> fromCode(String code) {
    return Arrays.stream(values()).filter(option -> option.code.equals(code)).findFirst();
  }
}
