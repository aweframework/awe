package com.almis.awe.scheduler.service.report;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ReportType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerNoneReport extends ServiceConfig implements ISchedulerReportService {

  @Override
  public ReportType getType() {
    return ReportType.NONE;
  }

  @Override
  public void execute(Task task, TaskExecution execution) {
  }
}
