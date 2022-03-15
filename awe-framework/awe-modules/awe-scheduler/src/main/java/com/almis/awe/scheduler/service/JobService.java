package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import com.almis.awe.service.MaintainService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * <h2>Job Service Class</h2>
 * <p>
 * Used to launch a maintain process as a scheduled batch
 * </p>
 *
 * @author pvidal
 */
@Slf4j
@Getter
public abstract class JobService extends ServiceConfig {

  // Autowired services
  private final ExecutionService executionService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final TaskDAO taskDAO;
  private final ApplicationEventPublisher eventPublisher;
  private final Duration defaultTimeout;

  // Locales
  private static final String SCHEDULER_ERROR_MESSAGE_TIMEOUT = "SCHEDULER_ERROR_MESSAGE_TIMEOUT";

  /**
   * Job service constructor
   * @param executionService Execution service
   * @param maintainService Maintain service
   * @param queryUtil QueryUtil service
   * @param taskDAO Task DAO
   * @param eventPublisher Event publisher
   * @param defaultTimeout Task timeout
   */
  protected JobService(ExecutionService executionService, MaintainService maintainService, QueryUtil queryUtil, TaskDAO taskDAO,
                       ApplicationEventPublisher eventPublisher, Duration defaultTimeout) {
    this.executionService = executionService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.taskDAO = taskDAO;
    this.eventPublisher = eventPublisher;
    this.defaultTimeout = defaultTimeout;
  }

  /**
   * Execute job
   *
   * @param task Task
   * @param execution Task execution
   * @param dataMap Job data map
   * @return Future job result
   * @throws InterruptedException Error job
   */
  public abstract Future<ServiceData> executeJob(Task task, TaskExecution execution, JobDataMap dataMap) throws InterruptedException;

  /**
   * Start logging execution
   *
   * @param execution Execution to log
   */
  void startLogging(TaskExecution execution) {
    MDC.put(TaskConstants.LOG_BY_TASK_EXECUTION, execution.getKey());
    MDC.put(TaskConstants.EXECUTION, "[execution: " + execution.getKey() + "] ");
  }

  /**
   * End logging execution
   */
  void endLogging() {
    MDC.remove(TaskConstants.EXECUTION);
    MDC.remove(TaskConstants.LOG_BY_TASK_EXECUTION);
  }

  public TaskExecution startTask(Task task) throws AWException {
    // Mark tasks as started
    TaskExecution execution = taskDAO.startTask(task);

    // Start task progress
    startProgressThread(execution, taskDAO.getAverageTime(task.getTaskId()));

    // Return task execution
    return execution;
  }

  /**
   * Start timeout thread
   *
   * @param execution Task execution
   * @param timeout Task timeout
   * @param process Future task thread
   */
  private void startTimeoutThread(TaskExecution execution, long timeout, Future<ServiceData> process) {
    executionService.startTimeoutJob(execution, timeout, process);
    log.debug("[SCHEDULER][TIMEOUT] The timeout thread has been started {}.{}", execution.getTaskId(), execution.getExecutionId());
  }

  /**
   * Start progress thread
   *
   * @param execution Task execution
   * @param averageTime Average time
   */
  private void startProgressThread(TaskExecution execution, Integer averageTime) {
    executionService.startProgressJob(execution, averageTime);
    log.debug("[SCHEDULER][PROGRESS] The progress task has been started {}.{}", execution.getTaskId(), execution.getExecutionId());
  }

  /**
   * Get timeout value
   *
   * @param task Task
   * @return timeout of task or default timeout
   */
  protected Integer getTimeout(Task task) {
    Integer timeout = Math.toIntExact(defaultTimeout.getSeconds());
    if (task.getExecutionTimeout() != null) {
      timeout = task.getExecutionTimeout();
    }
    return timeout;
  }

  /**
   * Start batch process
   *
   * @return Batch status
   */
  public ServiceData launchBatch(SchedulerJob job, Future<ServiceData> process) throws AWException {
    ServiceData serviceData = new ServiceData();
    TaskStatus status;

    // Start task timeout
    startTimeoutThread(job.getExecution(), getTimeout(job.getTask()), process);

    // Get current date in milliseconds and in Date formats
    try {
      // Launch process
      serviceData = process.get();

      // Initialize job status
      switch (serviceData.getType()) {
        case WARNING:
          status = TaskStatus.JOB_WARNING;
          break;
        case ERROR:
          status = TaskStatus.JOB_ERROR;
          break;
        case INFO:
          status = TaskStatus.JOB_INFO;
          break;
        default:
          status = TaskStatus.JOB_OK;
      }

      job.getTask().setStatus(status);
      job.getExecution().setDescription(serviceData.getMessage());
    } catch (CancellationException | InterruptedException exc) {
      job.getTask().setStatus(TaskStatus.JOB_INTERRUPTED);
      job.getExecution().setDescription(getLocale(SCHEDULER_ERROR_MESSAGE_TIMEOUT, TimeUtil.formatTime(getTimeout(job.getTask()) * 1000, false)));
      Thread.currentThread().interrupt();
    } catch (Exception exc) {
      job.getTask().setStatus(TaskStatus.JOB_ERROR);
      job.getTask().getReport().setReportMessage(exc.getMessage());
      job.getExecution().setDescription(serviceData.getMessage());
      log.error("[SCHEDULER][TASK_QUERY {}] Error on batch, process {}" + job.getTask().getAction(), job.getTask().getTrigger().getKey(), exc);
      throw new AWException(exc.toString(), exc);
    } finally {
      // Interrupt timer
      executionService.interruptExecutionJobs(job.getExecution());

      // End task
      job.setExecution(taskDAO.endTask(job.getTask(), job.getExecution()));

      // Send report
      executionService.startReportJob(job.getTask(), job.getExecution());
    }
    return serviceData;
  }
}
