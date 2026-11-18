package com.almis.awe.scheduler.service.scheduled;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.ExecutionService;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import com.almis.awe.service.MaintainService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.almis.awe.scheduler.constant.JobConstants.EXECUTION_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_LAUNCHER;

/**
 * @author dfuentes
 */
@Slf4j
public class MaintainJobService extends JobService {

  private final boolean isSchedulerInstance;
  private final URI remoteUrl;
  private final boolean isRemoteSecureEnabled;
  private final String remoteUser;
  private final String remotePassword;
  private final RestTemplate restTemplate;

  private final ObjectMapper mapper;

  /**
   * MaintainJobService constructor
   *
   * @param executionService      Execution service
   * @param maintainService       Maintain service
   * @param queryUtil             QueryUtil
   * @param taskDAO               Task DAO
   * @param eventPublisher        Event publisher
   * @param defaultTimeout        Default timeout
   * @param isSchedulerInstance   Is a remote scheduler instance
   * @param remoteUrl             Remote URL
   * @param isRemoteSecureEnabled Remote is secured enabled
   * @param remoteUser            Remote user
   * @param remotePassword        Remote password
   * @param mapper                Object mapper
   * @param executionLogStore     Active execution log store, completed at the end of every execution
   */

  public MaintainJobService(ExecutionService executionService,
                            MaintainService maintainService,
                            QueryUtil queryUtil,
                            TaskDAO taskDAO,
                            ApplicationEventPublisher eventPublisher,
                            ObjectMapper mapper,
                            ExecutionLogStore executionLogStore,
                            Duration defaultTimeout,
                            boolean isSchedulerInstance,
                            URI remoteUrl,
                            boolean isRemoteSecureEnabled,
                            String remoteUser,
                            String remotePassword, RestTemplate restTemplate) {
    super(executionService, maintainService, queryUtil, taskDAO, eventPublisher, executionLogStore, defaultTimeout);
    this.isSchedulerInstance = isSchedulerInstance;
    this.remoteUrl = remoteUrl;
    this.isRemoteSecureEnabled = isRemoteSecureEnabled;
    this.remoteUser = remoteUser;
    this.remotePassword = remotePassword;
    this.mapper = mapper;
    this.restTemplate = restTemplate;
  }

  /**
   * Execute Job
   *
   * @param task      Task to execute
   * @param execution Execution
   * @param dataMap   Job data map
   * @return Service data with execution data
   */
  @Async("schedulerJobPool")
  public Future<ServiceData> executeJob(Task task, TaskExecution execution, JobDataMap dataMap) {
    ServiceData jobResult = null;

    // Start logging
    startLogging(execution);

    try {
      // Log job start
      log.info("[{}] Maintain job started: {}", task.getTrigger().getKey().toString(), task.getAction());

      // Initialize query parameters (no per-task datasource routing - see #685)
      ObjectNode parameters = newParameters();

      // Insert task parameters
      for (TaskParameter taskParameter : task.getParameterList()) {
        if ("2".equalsIgnoreCase(taskParameter.getSource())) {
          parameters.put(taskParameter.getName(), getProperty(taskParameter.getValue()));
        } else {
          parameters.put(taskParameter.getName(), taskParameter.getValue());
        }
      }

      // Set default parameters
      parameters.put("launcher", (String) dataMap.get(TASK_LAUNCHER));

      // Reserved parameters: the execution key, so the maintain can complete the execution later (they win over a
      // task parameter of the same name)
      if (execution.getTaskId() != null && execution.getExecutionId() != null) {
        parameters.put(TASK_ID, execution.getTaskId());
        parameters.put(EXECUTION_ID, execution.getExecutionId());
      }

      try {
        jobResult = launchMaintain(task.getAction(), parameters, execution);
      } catch (AWException exc) {
        log.error("Error launching maintain job", exc);
        jobResult = new ServiceData()
          .setType(exc.getType())
          .setTitle(exc.getTitle())
          .setMessage(exc.getMessage());
      }

      return CompletableFuture.completedFuture(jobResult);
    } finally {
      if (SchedulerDeferred.isDeferred(jobResult)) {
        // The execution keeps running: its log is completed when the execution is closed
        releaseLogging();
      } else {
        // Log completion summary and end logging
        logExecutionSummary(execution, jobResult);
        endLogging(execution);
      }
    }
  }

  /**
   * Build the parameters of a maintain launched by the scheduler
   *
   * @return Empty parameters (no per-task datasource routing - see #685)
   */
  public ObjectNode newParameters() {
    return getQueryUtil().getParameters(null, "1", "0");
  }

  /**
   * Launch a maintain where the maintain tasks run: on this application, or on the AWE application when this is a
   * remote scheduler instance
   *
   * @param maintain   Maintain identifier
   * @param parameters Maintain parameters
   * @param execution  Execution the maintain is launched on behalf of
   * @return Answer of the maintain
   * @throws AWException Error launching the maintain
   */
  public ServiceData launchMaintain(String maintain, ObjectNode parameters, TaskExecution execution)
    throws AWException {
    return isSchedulerInstance ? launchRemoteMaintainRest(maintain, parameters, execution)
      : getMaintainService().launchPrivateMaintain(maintain, parameters);
  }

  /**
   * Launch a maintain to an instance of AWE from a remote instance of AWE-Scheduler
   *
   * @param maintain   Maintain identifier
   * @param parameters Maintain parameters
   * @param execution  Execution the callback is issued on behalf of
   * @return Service data
   * @throws AWException Error launching remote maintain
   */
  private ServiceData launchRemoteMaintainRest(String maintain, ObjectNode parameters, TaskExecution execution) throws AWException {
    RequestParameter requestParameter = new RequestParameter();
    requestParameter.setParameters(mapper.convertValue(parameters, new TypeReference<>() {
    }));

    // Launch request
    log.info("Launching scheduler remote REST maintain call: {}/api/maintain/{}", remoteUrl, maintain);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String token = authenticateRequest();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    if (execution.getTaskId() != null && execution.getExecutionId() != null) {
      headers.set(TaskConstants.EXECUTION_KEY_HEADER, new ExecutionKey(execution.getTaskId(), execution.getExecutionId()).mdcKey());
    }

    ResponseEntity<ServiceData> response = restTemplate.postForEntity(remoteUrl + "/api/maintain/{maintainId}",
        new HttpEntity<>(requestParameter, headers), ServiceData.class, maintain);

    return response.getBody();
  }

  private String authenticateRequest() {
    if (isRemoteSecureEnabled) {
      // Authenticate request
      log.info("Launching scheduler remote REST authentication call: {}/api/authenticate", remoteUrl);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      ResponseEntity<LoginResponse> response = restTemplate.postForEntity(remoteUrl + "/api/authenticate",
          new HttpEntity<>(new LoginRequest()
              .setUsername(remoteUser)
              .setPassword(remotePassword), headers), LoginResponse.class);

      return Optional.ofNullable(response.getBody()).map(LoginResponse::getToken).orElse(null);
    }
    return null;
  }
}
