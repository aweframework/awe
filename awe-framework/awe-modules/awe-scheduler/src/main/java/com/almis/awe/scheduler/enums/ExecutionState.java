package com.almis.awe.scheduler.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Option of the state quick filter of the activity screen: a status of the executions and, for the running ones,
 * whether they are waiting for the confirmation of a deferred answer. Each option has the label (locale key) and the
 * tone of its count in the quick filter
 */
public enum ExecutionState {
  RUNNING("running", TaskStatus.JOB_RUNNING, 0, "ENUM_STATUS_RUNNING", ""),
  WAITING("waiting", TaskStatus.JOB_RUNNING, 1, "SCREEN_TEXT_ACTIVITY_WAITING", "warning"),
  QUEUED("queued", TaskStatus.JOB_QUEUED, null, "ENUM_STATUS_QUEUED", ""),
  ERROR("error", TaskStatus.JOB_ERROR, null, "ENUM_STATUS_ERROR", "danger"),
  OK("ok", TaskStatus.JOB_OK, null, "ENUM_STATUS_OK", "");

  private final String code;
  private final TaskStatus status;
  private final Integer deferred;
  private final String label;
  private final String tone;

  ExecutionState(String code, TaskStatus status, Integer deferred, String label, String tone) {
    this.code = code;
    this.status = status;
    this.deferred = deferred;
    this.label = label;
    this.tone = tone;
  }

  /**
   * Code of the option (value of the quick filter)
   *
   * @return Code
   */
  public String getCode() {
    return code;
  }

  /**
   * Status of the executions of the option
   *
   * @return Status
   */
  public TaskStatus getStatus() {
    return status;
  }

  /**
   * Deferred filter of the option: 1 waiting for a confirmation, 0 not waiting, null either
   *
   * @return Deferred filter
   */
  public Integer getDeferred() {
    return deferred;
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
   * Tone of the count of the option, empty for the neutral one
   *
   * @return Tone
   */
  public String getTone() {
    return tone;
  }

  /**
   * Option of a code
   *
   * @param code Option code
   * @return Option, empty when the code is not an option
   */
  public static Optional<ExecutionState> fromCode(String code) {
    return Arrays.stream(values()).filter(state -> state.code.equals(code)).findFirst();
  }
}
