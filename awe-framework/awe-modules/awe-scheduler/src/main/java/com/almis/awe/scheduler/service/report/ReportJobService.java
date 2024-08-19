package com.almis.awe.scheduler.service.report;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_JOB_EXECUTION;

/**
 * @author pgarcia
 */
@Slf4j
public class ReportJobService {

  private final SchedulerReportService schedulerReportService;
  private final boolean isSchedulerInstance;
  private final URI remoteUrl;
  private final boolean isRemoteSecureEnabled;
  private final String remoteUser;
  private final String remotePassword;
  private final WebClient webClient;

  /**
   * ReportJobService constructor
   *
   * @param schedulerReportService Scheduler Report Service
   * @param isSchedulerInstance        Remote scheduler is enabled
   * @param remoteUrl              Remote URL
   * @param isRemoteSecureEnabled  Remote is secured enabled
   * @param remoteUser             Remote user
   * @param remotePassword         Remote password
   */

  public ReportJobService(SchedulerReportService schedulerReportService,
                          boolean isSchedulerInstance,
                          URI remoteUrl,
                          boolean isRemoteSecureEnabled,
                          String remoteUser,
                          String remotePassword) {
    this.schedulerReportService = schedulerReportService;
    this.isSchedulerInstance = isSchedulerInstance;
    this.remoteUrl = remoteUrl;
    this.isRemoteSecureEnabled = isRemoteSecureEnabled;
    this.remoteUser = remoteUser;
    this.remotePassword = remotePassword;
    this.webClient = Optional.ofNullable(remoteUrl).map(URI::toString).map(WebClient::create).orElse(null);
  }

  public void execute(Task task, TaskExecution execution) {
    if (isSchedulerInstance) {
      launchRemoteReportService(task, execution);
    } else {
      schedulerReportService.execute(task, execution);
    }
  }

  /**
   * Launch a maintain to an instance of AWE from a remote instance of AWE-Scheduler
   *
   * @param task          Task
   * @param taskExecution Task execution
   * @return Service data
   */
  private Future<ServiceData> launchRemoteReportService(Task task, TaskExecution taskExecution) {
    RequestParameter requestParameter = new RequestParameter();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TASK, task);
    parameters.put(TASK_JOB_EXECUTION, taskExecution);
    requestParameter.setParameters(parameters);

    // Launch request
    log.info("Launching scheduler remote REST maintain call: {}/api/maintain/reportSchedulerTaskFinished", remoteUrl);

    Mono<ServiceData> response = webClient.post()
      .uri("/api/maintain/reportSchedulerTaskFinished")
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
