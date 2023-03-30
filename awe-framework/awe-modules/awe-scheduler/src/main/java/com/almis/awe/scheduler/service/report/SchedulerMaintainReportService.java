package com.almis.awe.scheduler.service.report;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ReportType;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;

@Slf4j
public class SchedulerMaintainReportService implements ISchedulerReportService {

  // Autowired services
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;

  /**
   * Autowired constructor
   *
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   */
  public SchedulerMaintainReportService(QueryUtil queryUtil, MaintainService maintainService) {
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
  }

  @Override
  public ReportType getType() {
    return ReportType.MAINTAIN;
  }

  public void execute(Task task, TaskExecution taskExecution) {
    // Store task and execution in parameters
    ObjectNode parameters = queryUtil.getParameters(task.getDatabase());
    parameters.set(TASK, JsonNodeFactory.instance.pojoNode(task));
    parameters.set(TASK_JOB_EXECUTION, JsonNodeFactory.instance.pojoNode(taskExecution));

    try {
      maintainService.launchPrivateMaintain(task.getReport().getReportMaintainId(), parameters);
    } catch (AWException exc) {
      // Log error
      log.error("Report generation error for task {}", task.getTaskId(), exc);
    }
  }
}
