package com.almis.awe.scheduler.bean.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Filters, sort and page of the task catalog (tasks screen). Every filter is optional
 */
@Data
@Accessors(chain = true)
public class TaskCatalogCriteria {
  /**
   * Text contained in the name, the description or what the task runs, case insensitive
   */
  private String search;
  /**
   * How the task is launched ({@code TaskLaunchKind} codes: manual, scheduled, file, dependency, webhook); every one
   * when blank
   */
  private String launchType;
  /**
   * 1 active tasks, 0 inactive tasks; every task when blank
   */
  private String active;
  /**
   * Status of the last execution ({@code TaskLastStatus} codes: ok, warning, error, running, waiting, queued, none);
   * every one when blank
   */
  private String lastStatus;
  /**
   * Server used by the task: the one it runs on or the one where it checks its files; every task when null
   */
  private Integer serverId;
  /**
   * Calendar of the launch of the task; every task when null
   */
  private Integer calendarId;
  /**
   * Page number (from 1)
   */
  private Integer page;
  /**
   * Rows per page, from 1 to 500; without it, 50
   */
  private Integer max;
  /**
   * Sort of the catalog, applied to every task before the page: the columns in their order; without it, by name.
   * A column or a direction that is not accepted is refused
   */
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ExecutionSort> sort;
}
