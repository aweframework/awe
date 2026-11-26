package com.almis.awe.scheduler.service.scheduled;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.ExecutionService;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * A deferred answer keeps the execution running: the job is not ended, no report is sent and the due date is stored
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobServiceDeferredTest {

  private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

  @Mock
  private ExecutionService executionService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskDAO taskDAO;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private AweElements aweElements;
  @Mock
  private ApplicationContext context;

  private JobService service;
  private final TaskExecution execution = new TaskExecution().setTaskId(8).setExecutionId(2)
    .setInitialDate(Date.from(START));
  private SchedulerJob job;

  @BeforeEach
  void setUp() {
    service = new JobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher, executionLogStore,
      Duration.ofSeconds(5)) {
      @Override
      public Future<ServiceData> executeJob(Task task, TaskExecution execution, JobDataMap dataMap) {
        return null;
      }
    };
    service.setApplicationContext(context);
    service.setDeferredDefaultTimeout(Duration.ofHours(24));
    doReturn(aweElements).when(context).getBean(AweElements.class);
    job = mock(SchedulerJob.class);
  }

  private void givenJobOf(Task task) {
    doReturn(task).when(job).getTask();
    doReturn(execution).when(job).getExecution();
  }

  private Future<ServiceData> deferred(String reference) {
    return CompletableFuture.completedFuture(SchedulerDeferred.accepted(reference, "Sent to the engine"));
  }

  @Test
  void aDeferredAnswerStoresTheReferenceAndADueDateStartPlusTheTaskTimeout() throws Exception {
    givenJobOf(new Task().setTaskId(8).setExecutionTimeout(600));

    service.launchBatch(job, deferred("job-77"));

    ArgumentCaptor<Instant> accepted = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> due = ArgumentCaptor.forClass(Instant.class);
    verify(taskDAO).insertDeferredExecution(eq(8), eq(2), eq("job-77"), accepted.capture(), due.capture());
    assertThat(due.getValue()).isEqualTo(START.plusSeconds(600));
    assertThat(accepted.getValue()).isAfterOrEqualTo(START);
  }

  @Test
  void withoutTaskTimeoutTheDeferredDefaultTimeoutApplies() throws Exception {
    givenJobOf(new Task().setTaskId(8));

    service.launchBatch(job, deferred(null));

    verify(taskDAO).insertDeferredExecution(eq(8), eq(2), eq(null), any(Instant.class),
      eq(START.plus(Duration.ofHours(24))));
  }

  @Test
  void theExecutionStaysRunningWithTheDescriptionAndTheJobIsReleased() throws Exception {
    Task task = new Task().setTaskId(8);
    givenJobOf(task);

    ServiceData answer = service.launchBatch(job, deferred("job-77"));

    assertThat(SchedulerDeferred.isDeferred(answer)).isTrue();
    verify(taskDAO).changeStatus(task, execution, TaskStatus.JOB_RUNNING, "Sent to the engine");
    verify(executionService).interruptExecutionJobs(execution);
  }

  @Test
  void theExecutionIsNotEndedAndNoReportIsStarted() throws Exception {
    givenJobOf(new Task().setTaskId(8));

    service.launchBatch(job, deferred("job-77"));

    verify(taskDAO, never()).endTask(any(), any());
    verify(executionService, never()).startReportJob(any(), any());
  }

  @Test
  void aNormalAnswerEndsTheExecutionAsBefore() throws Exception {
    Task task = new Task().setTaskId(8);
    givenJobOf(task);
    doReturn(execution).when(taskDAO).endTask(task, execution);

    service.launchBatch(job, CompletableFuture.completedFuture(new ServiceData()));

    verify(taskDAO).endTask(task, execution);
    verify(executionService).startReportJob(task, execution);
    verify(taskDAO, never()).insertDeferredExecution(any(), any(), any(), any(), any());
  }
}
