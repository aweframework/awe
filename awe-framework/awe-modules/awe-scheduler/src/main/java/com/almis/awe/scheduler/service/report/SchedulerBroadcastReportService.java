package com.almis.awe.scheduler.service.report;

import com.almis.awe.builder.client.MessageActionBuilder;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.ReportType;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerBroadcastReportService implements ISchedulerReportService {

  // Autowired services
  private final BroadcastService broadcastService;

  /**
   * Constructor
   *
   * @param broadcastService Broadcast service
   */
  public SchedulerBroadcastReportService(BroadcastService broadcastService) {
    this.broadcastService = broadcastService;
  }

  public void execute(Task task, TaskExecution taskExecution) {
    // Generate the message action
    ClientAction message = new MessageActionBuilder(getMessageType(taskExecution), task.getReport().getReportMessage(), taskExecution.getDescription()).build();
    broadcastService.broadcastMessageToUsers(message, task.getReport().getReportUserDestination().toArray(new String[0]));
  }

  @Override
  public ReportType getType() {
    return ReportType.BROADCAST;
  }

  /**
   * Get message type for broadcast message type
   *
   * @return String
   */
  private AnswerType getMessageType(TaskExecution execution) {
    switch (TaskStatus.valueOf(execution.getStatus())) {
      case JOB_OK:
        return AnswerType.OK;
      case JOB_ERROR:
        return AnswerType.ERROR;
      case JOB_STOPPED:
      case JOB_QUEUED:
      case JOB_RUNNING:
        return AnswerType.INFO;
      case JOB_WARNING:
      case JOB_INTERRUPTED:
      default:
        return AnswerType.WARNING;
    }
  }
}
