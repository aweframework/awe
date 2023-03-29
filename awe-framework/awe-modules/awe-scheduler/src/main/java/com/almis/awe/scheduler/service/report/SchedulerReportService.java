package com.almis.awe.scheduler.service.report;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ReportType;
import com.almis.awe.scheduler.factory.ReportServiceFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerReportService {
  public ServiceData execute(Task task, TaskExecution execution) {

    // Execute report
    ReportServiceFactory
      .getInstance(ReportType.valueOf(task.getReportType()))
      .execute(task, execution);

    // Retrieve service data
    return new ServiceData();
  }
}
