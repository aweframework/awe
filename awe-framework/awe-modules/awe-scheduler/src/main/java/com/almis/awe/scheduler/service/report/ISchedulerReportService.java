package com.almis.awe.scheduler.service.report;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ReportType;

public interface ISchedulerReportService {
  default ReportType getType() {
    return null;
  }

  void execute(Task task, TaskExecution execution);
}
