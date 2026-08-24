package com.almis.awe.scheduler.service.scheduled;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.CommandDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.ExecutionService;
import com.almis.awe.service.MaintainService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import java.time.Duration;
import java.util.concurrent.Future;

/**
 * @author dfuentes
 */
@Slf4j
public class CommandJobService extends JobService {

  // Autowired services
  private final CommandDAO commandDAO;

  // Locales
  private static final String SCHEDULER_BATCH_LAUNCH_TITLE = "SCHEDULER_BATCH_LAUNCH_TITLE";
  private static final String SCHEDULER_BATCH_LAUNCH_MESSAGE = "SCHEDULER_BATCH_LAUNCH_MESSAGE";
  private static final String SCHEDULER_BATCH_LAUNCH_ERROR_TITLE = "SCHEDULER_BATCH_LAUNCH_ERROR_TITLE";
  private static final String SCHEDULER_BATCH_LAUNCH_ERROR_MESSAGE = "SCHEDULER_BATCH_LAUNCH_ERROR_MESSAGE";

  /**
   * Autowired constructor
   *
   * @param executionService Timeout service
   */
  public CommandJobService(ExecutionService executionService,
                           MaintainService maintainService,
                           QueryUtil queryUtil, TaskDAO taskDAO,
                           ApplicationEventPublisher eventPublisher,
                           CommandDAO commandDAO,
                           Duration defaultTimeout) {
    super(executionService, maintainService, queryUtil, taskDAO, eventPublisher, defaultTimeout);
    this.commandDAO = commandDAO;
  }

  @Async("schedulerJobPool")
  public Future<ServiceData> executeJob(Task task, TaskExecution execution, JobDataMap dataMap) {
    // Start logging
    startLogging(execution);

    // Log job start
    log.info("[{}] Command job started: {}", task.getTrigger().getKey().toString(), task.getAction());

    resolvePropertyParameters(task);

    ServiceData result = new ServiceData();
    // Execute task
    if (execute(task, getTimeout(task))) {
      result
        .setTitle(getLocale(SCHEDULER_BATCH_LAUNCH_TITLE))
        .setMessage(getLocale(SCHEDULER_BATCH_LAUNCH_MESSAGE));
    } else {
      result
        .setType(AnswerType.ERROR)
        .setTitle(SCHEDULER_BATCH_LAUNCH_ERROR_TITLE)
        .setMessage(SCHEDULER_BATCH_LAUNCH_ERROR_MESSAGE);
    }

    // End logging
    endLogging();

    return new AsyncResult<>(result);
  }

  /**
   * Execute shell command
   *
   * @param task
   * @param timeout
   * @return
   */
  /**
   * Replace the stored key of every PROPERTY parameter with its resolved property value, on the
   * per-execution task instance, so both the environment and the command line see the same value.
   *
   * @param task Task whose parameters are resolved
   */
  private void resolvePropertyParameters(Task task) {
    if (task.getParameterList() == null) {
      return;
    }

    for (TaskParameter parameter : task.getParameterList()) {
      if (String.valueOf(ParameterConstants.PROPERTY).equals(parameter.getSource())) {
        parameter.setValue(getProperty(parameter.getValue()));
      }
    }
  }

  private boolean execute(Task task, final long timeout) {
    return execute(task, task.getParameterList().stream().map(TaskParameter::getValue).toArray(String[]::new), timeout);
  }

  /**
   * Execute shell command
   *
   * @param commandTask
   * @param envp
   * @param timeout
   * @return
   */
  private boolean execute(Task commandTask, String[] envp, final long timeout) {
    return commandDAO.runCommand(commandTask, envp, timeout) == 0;
  }
}
