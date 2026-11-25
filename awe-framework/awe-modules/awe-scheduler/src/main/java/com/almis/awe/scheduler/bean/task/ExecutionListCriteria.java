package com.almis.awe.scheduler.bean.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Filters and page of the execution list of all tasks (Activity screen). Every filter is optional.
 */
@Data
@Accessors(chain = true)
public class ExecutionListCriteria {
  /**
   * Only the executions of this task (the history of the task screen)
   */
  private Integer taskId;
  /**
   * Text contained in the task name, case insensitive
   */
  private String taskName;
  /**
   * Execution statuses ({@code TaskStatus} values)
   */
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> statuses;
  /**
   * Execution origins ({@code ExecutionOrigin} codes: scheduled, manual, file, dependency)
   */
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> origins;
  /**
   * 1: running executions waiting for the confirmation of a deferred answer; 0: running executions that are not
   * waiting for it
   */
  private Integer deferred;
  /**
   * Option of the state quick filter ({@code ExecutionState} codes: running, waiting, queued, error, ok); every state
   * when blank. It sets the status and the deferred filter, so it cannot be combined with statuses nor deferred
   */
  private String state;
  /**
   * First start day (web date, dd/MM/yyyy), included
   */
  private String startFrom;
  /**
   * Last start day (web date, dd/MM/yyyy), included
   */
  private String startTo;
  /**
   * Only executions started in the last hours
   */
  private Integer withinHours;
  /**
   * Page number (from 1)
   */
  private Integer page;
  /**
   * Rows per page, from 1 to 500; without it, 50
   */
  private Integer max;
  /**
   * Sort of the list, applied in the database before the page: the columns in their order; without it, newest first.
   * A column or a direction that is not accepted is refused
   */
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ExecutionSort> sort;
}
