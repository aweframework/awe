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
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.ExecutionService;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Future;

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
  private final WebClient webClient;

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
   */

  public MaintainJobService(ExecutionService executionService,
                            MaintainService maintainService,
                            QueryUtil queryUtil,
                            TaskDAO taskDAO,
                            ApplicationEventPublisher eventPublisher,
                            ObjectMapper mapper,
                            Duration defaultTimeout,
                            boolean isSchedulerInstance,
                            URI remoteUrl,
                            boolean isRemoteSecureEnabled,
                            String remoteUser,
                            String remotePassword) {
    super(executionService, maintainService, queryUtil, taskDAO, eventPublisher, defaultTimeout);
    this.isSchedulerInstance = isSchedulerInstance;
    this.remoteUrl = remoteUrl;
    this.isRemoteSecureEnabled = isRemoteSecureEnabled;
    this.remoteUser = remoteUser;
    this.remotePassword = remotePassword;
    this.webClient = Optional.ofNullable(remoteUrl).map(URI::toString).map(WebClient::create).orElse(null);
    this.mapper = mapper;
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
    Future<ServiceData> result;

    // Start logging
    startLogging(execution);

    // Log job start
    log.info("[{}] Maintain job started: {}", task.getTrigger().getKey().toString(), task.getAction());

    // Initialize database to the one stored on the current task
    ObjectNode parameters = getQueryUtil().getParameters(task.getDatabase(), "1", "0");

    // Insert task parameters
    for (TaskParameter taskParameter : task.getParameterList()) {
      if ("2".equalsIgnoreCase(taskParameter.getSource())) {
        parameters.put(taskParameter.getName(), getProperty(taskParameter.getValue()));
      } else {
        parameters.put(taskParameter.getName(), taskParameter.getValue());
      }
    }

    // Set default parameters
    parameters.put("database", task.getDatabase());
    parameters.put("launcher", (String) dataMap.get(TASK_LAUNCHER));

    try {
      if (isSchedulerInstance) {
        result = launchRemoteMaintainRest(task.getAction(), parameters);
      } else {
        result = new AsyncResult<>(getMaintainService().launchPrivateMaintain(task.getAction(), parameters));
      }
    } catch (AWException exc) {
      log.error("Error launching maintain job", exc);
      result = new AsyncResult<>(new ServiceData()
        .setType(exc.getType())
        .setTitle(exc.getTitle())
        .setMessage(exc.getMessage()));
    }

    // End logging
    endLogging();

    return result;
  }

  /**
   * Launch a maintain to an instance of AWE from a remote instance of AWE-Scheduler
   *
   * @param maintain   Maintain identifier
   * @param parameters Maintain parameters
   * @return Service data
   * @throws AWException Error launching remote maintain
   */
  private Future<ServiceData> launchRemoteMaintainRest(String maintain, ObjectNode parameters) throws AWException {
    RequestParameter requestParameter = new RequestParameter();
    requestParameter.setParameters(mapper.convertValue(parameters, new TypeReference<>() {
    }));

    // Launch request
    log.info("Launching scheduler remote REST maintain call: {}/api/maintain/async/{}", remoteUrl, maintain);
    Mono<ServiceData> response = webClient.post()
      .uri("/api/maintain/async/{maintainId}", maintain)
      .headers(h -> Optional.ofNullable(authenticateRequest()).ifPresent(h::setBearerAuth))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(requestParameter))
      .retrieve()
      .bodyToMono(ServiceData.class);

    return response.toFuture();
  }

  private String authenticateRequest() {
    if (isRemoteSecureEnabled) {
      // Authenticate request
      log.info("Launching scheduler remote REST authentication call: {}/api/authenticate", remoteUrl);
      return webClient.post()
        .uri("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new LoginRequest()
          .setUsername(remoteUser)
          .setPassword(remotePassword)))
        .retrieve()
        .bodyToMono(LoginResponse.class)
        .blockOptional()
        .map(LoginResponse::getToken)
        .orElse(null);
    }
    return null;
  }
}
