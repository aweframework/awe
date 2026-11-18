package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.CompletionResult.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulerExecutionCompletionServiceTest {

  @Mock
  private TaskDAO taskDAO;
  @Mock
  private ExecutionService executionService;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private DeferredCancellationNotifier cancellationNotifier;

  private SchedulerExecutionCompletionService service;
  private final Task task = new Task().setTaskId(1).setExecutionTimeout(60);
  private final TaskExecution running = new TaskExecution().setTaskId(1).setExecutionId(2)
    .setStatus(TaskStatus.JOB_RUNNING.getValue());
  private final TaskExecution closed = new TaskExecution().setTaskId(1).setExecutionId(2).setExecutionTime(500);

  @BeforeEach
  void setUp() throws Exception {
    service = spy(new SchedulerExecutionCompletionService(taskDAO, executionService, executionLogStore,
      eventPublisher, cancellationNotifier, Duration.ofHours(24)));
    when(taskDAO.getTaskExecution(1, 2)).thenReturn(running);
    when(taskDAO.getDeferredExecution(1, 2)).thenReturn(new DeferredExecution().setTaskId(1).setExecutionId(2));
    when(taskDAO.closeRunningExecution(eq(1), eq(2), any())).thenReturn(true);
    when(taskDAO.getTask(1)).thenReturn(CompletableFuture.completedFuture(task));
    when(taskDAO.endTask(any(), any())).thenReturn(closed);
  }

  @Test
  void aDeferredRunningExecutionIsClosedWithTheGivenStatusAndDescription() throws Exception {
    CompletionResult result = service.complete(1, 2, TaskStatus.JOB_WARNING, "partial", Map.of());

    assertThat(result.outcome()).isEqualTo(Outcome.CLOSED);
    assertThat(result.currentStatus()).isEqualTo(TaskStatus.JOB_WARNING);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.JOB_WARNING);
    assertThat(running.getDescription()).isEqualTo("partial");
    verify(taskDAO).closeRunningExecution(1, 2, TaskStatus.JOB_WARNING);
    verify(taskDAO).endTask(task, running);
  }

  @Test
  void theClosingCallStoresWhoCompletedTheExecution() throws Exception {
    service.complete(1, 2, TaskStatus.JOB_OK, "done", null);
    service.complete(1, 2, TaskStatus.JOB_INTERRUPTED, "late", null, CompletionSource.DEADLINE);

    verify(taskDAO).recordDeferredCompletion(eq(1), eq(2), eq(CompletionSource.API), any());
    verify(taskDAO).recordDeferredCompletion(eq(1), eq(2), eq(CompletionSource.DEADLINE), any());
  }

  @Test
  void aStopOrADeadlineNotifiesTheCancellationOnceAndTheApiDoesNot() throws Exception {
    DeferredExecution deferred = new DeferredExecution().setTaskId(1).setExecutionId(2).setExternalReference("ref");
    when(taskDAO.getDeferredExecution(1, 2)).thenReturn(deferred);

    service.complete(1, 2, TaskStatus.JOB_STOPPED, "x", null, CompletionSource.API);
    verifyNoInteractions(cancellationNotifier);

    service.complete(1, 2, TaskStatus.JOB_INTERRUPTED, "x", null, CompletionSource.DEADLINE);
    service.complete(1, 2, TaskStatus.JOB_STOPPED, "x", null, CompletionSource.USER);
    verify(cancellationNotifier, times(2)).notifyCancellation(task, deferred);
  }

  @Test
  void aCallThatDoesNotCloseTheExecutionDoesNotNotify() throws Exception {
    when(taskDAO.closeRunningExecution(eq(1), eq(2), any())).thenReturn(false);

    service.complete(1, 2, TaskStatus.JOB_INTERRUPTED, "x", null, CompletionSource.DEADLINE);

    verifyNoInteractions(cancellationNotifier);
  }

  @Test
  void aFailingTraceStillPublishesTheEvent() throws Exception {
    doThrow(new AWException("trace failed")).when(taskDAO).recordDeferredCompletion(any(), any(), any(), any());

    assertThat(service.complete(1, 2, TaskStatus.JOB_OK, "x", null).outcome()).isEqualTo(Outcome.CLOSED);

    verify(eventPublisher).publishEvent(any(SchedulerTaskFinishedEvent.class));
  }

  @Test
  void theOutputsBecomeValueParametersOfTheTaskTheDependenciesReceive() throws Exception {
    task.setParameterList(new ArrayList<>(List.of(
      new TaskParameter().setName("file").setSource("2").setValue("app.file"),
      new TaskParameter().setName("kept").setSource("0").setValue("same"))));
    Map<String, String> outputs = new HashMap<>();
    outputs.put("file", "result.csv");
    outputs.put("rows", "42");
    outputs.put("empty", null);

    service.complete(1, 2, TaskStatus.JOB_OK, "done", outputs);

    ArgumentCaptor<SchedulerTaskFinishedEvent> event = ArgumentCaptor.forClass(SchedulerTaskFinishedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().getTask().getParameterList())
      .extracting(TaskParameter::getName, TaskParameter::getSource, TaskParameter::getValue)
      .containsExactlyInAnyOrder(tuple("file", "0", "result.csv"), tuple("kept", "0", "same"), tuple("rows", "0", "42"));
  }

  @Test
  void theEndIsCompletedAsForANormalJob() throws Exception {
    service.complete(1, 2, TaskStatus.JOB_OK, "done", null);

    verify(executionService).startReportJob(task, closed);
    verify(executionLogStore).complete(new ExecutionKey(1, 2), ExecutionLogOrigin.SCHEDULER);
    ArgumentCaptor<SchedulerTaskFinishedEvent> event = ArgumentCaptor.forClass(SchedulerTaskFinishedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().getTask()).isSameAs(task);
    assertThat(event.getValue().getTaskExecution()).isSameAs(closed);
    assertThat(event.getValue().getElapsedTime()).isEqualTo(500);
  }

  @Test
  void everyAllowedStatusClosesTheExecution() throws Exception {
    for (TaskStatus status : List.of(TaskStatus.JOB_OK, TaskStatus.JOB_WARNING, TaskStatus.JOB_ERROR,
      TaskStatus.JOB_STOPPED, TaskStatus.JOB_INTERRUPTED, TaskStatus.JOB_INFO)) {
      assertThat(service.complete(1, 2, status, "x", null).outcome()).isEqualTo(Outcome.CLOSED);
    }
  }

  @Test
  void aStatusOutsideTheClosingOnesIsRefusedBeforeTouchingTheDatabase() {
    assertThatThrownBy(() -> service.complete(1, 2, TaskStatus.JOB_RUNNING, "x", null))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.complete(1, 2, TaskStatus.JOB_QUEUED, "x", null))
      .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(taskDAO);
  }

  @Test
  void anUnknownExecutionIsReportedAndNothingIsClosed() throws Exception {
    when(taskDAO.getTaskExecution(1, 9)).thenReturn(null);

    CompletionResult result = service.complete(1, 9, TaskStatus.JOB_OK, "x", null);

    assertThat(result.outcome()).isEqualTo(Outcome.UNKNOWN);
    assertThat(result.currentStatus()).isNull();
    verify(taskDAO, never()).closeRunningExecution(any(), any(), any());
  }

  @Test
  void anExecutionThatWasNeverDeferredIsNotClosed() throws Exception {
    when(taskDAO.getDeferredExecution(1, 2)).thenReturn(null);

    CompletionResult result = service.complete(1, 2, TaskStatus.JOB_OK, "x", null);

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_DEFERRED);
    assertThat(result.currentStatus()).isEqualTo(TaskStatus.JOB_RUNNING);
    verify(taskDAO, never()).closeRunningExecution(any(), any(), any());
  }

  @Test
  void aTaskThatCannotBeLoadedFailsBeforeTheGateAndLeavesTheExecutionRunning() throws Exception {
    when(taskDAO.getTask(1)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("db down")));

    assertThatThrownBy(() -> service.complete(1, 2, TaskStatus.JOB_OK, "x", null)).isInstanceOf(AWException.class);

    verify(taskDAO, never()).closeRunningExecution(any(), any(), any());
    verify(taskDAO, never()).endTask(any(), any());
    verifyNoInteractions(eventPublisher, executionLogStore, executionService);
  }

  @Test
  void aFailingEndAfterTheGateStillPublishesTheEventAndAnswersClosed() throws Exception {
    doThrow(new AWException("end failed")).when(taskDAO).endTask(any(), any());

    CompletionResult result = service.complete(1, 2, TaskStatus.JOB_OK, "x", null);

    assertThat(result.outcome()).isEqualTo(Outcome.CLOSED);
    verify(executionLogStore).complete(new ExecutionKey(1, 2), ExecutionLogOrigin.SCHEDULER);
    verify(eventPublisher).publishEvent(any(SchedulerTaskFinishedEvent.class));
  }

  @Test
  void aFailingReportStillCompletesTheLogAndPublishesTheEvent() throws Exception {
    doThrow(new IllegalStateException("report failed")).when(executionService).startReportJob(any(), any());

    CompletionResult result = service.complete(1, 2, TaskStatus.JOB_OK, "x", null);

    assertThat(result.outcome()).isEqualTo(Outcome.CLOSED);
    verify(executionLogStore).complete(new ExecutionKey(1, 2), ExecutionLogOrigin.SCHEDULER);
    verify(eventPublisher).publishEvent(any(SchedulerTaskFinishedEvent.class));
  }

  @Test
  void anExecutionAlreadyClosedReportsItsCurrentStatusAndDoesNothing() throws Exception {
    when(taskDAO.closeRunningExecution(eq(1), eq(2), any())).thenReturn(false);
    when(taskDAO.getTaskExecution(1, 2)).thenReturn(running, new TaskExecution().setTaskId(1).setExecutionId(2)
      .setStatus(TaskStatus.JOB_INTERRUPTED.getValue()));

    CompletionResult result = service.complete(1, 2, TaskStatus.JOB_OK, "late", null);

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_RUNNING);
    assertThat(result.currentStatus()).isEqualTo(TaskStatus.JOB_INTERRUPTED);
    verify(taskDAO, never()).endTask(any(), any());
    verify(taskDAO, never()).recordDeferredCompletion(any(), any(), any(), any());
    verifyNoInteractions(eventPublisher, executionLogStore, executionService);
  }

  @Test
  void theDescriptionIsTruncatedToTheColumnLength() throws Exception {
    service.complete(1, 2, TaskStatus.JOB_ERROR, "x".repeat(2500), Map.of());

    assertThat(running.getDescription()).hasSize(2000);
  }

  @Test
  void anInterruptionWithoutDescriptionUsesTheTimeoutText() throws Exception {
    doReturn("timed out after 1m").when(service).getLocale(eq("SCHEDULER_ERROR_MESSAGE_TIMEOUT"), anyString());

    service.complete(1, 2, TaskStatus.JOB_INTERRUPTED, null, null);

    assertThat(running.getDescription()).isEqualTo("timed out after 1m");
  }

  @Test
  void stopClosesEveryRunningDeferredExecutionOfTheTaskAsStopped() throws Exception {
    when(taskDAO.getRunningDeferredExecutions(1, null)).thenReturn(List.of(
      new DeferredExecution().setTaskId(1).setExecutionId(2), new DeferredExecution().setTaskId(1).setExecutionId(3)));
    doReturn("stopped by user").when(service).getLocale("SCHEDULER_MESSAGE_STOPPED_BY_USER");
    when(taskDAO.getTaskExecution(1, 3)).thenReturn(new TaskExecution().setTaskId(1).setExecutionId(3)
      .setStatus(TaskStatus.JOB_OK.getValue()));
    when(taskDAO.getDeferredExecution(1, 3)).thenReturn(new DeferredExecution().setTaskId(1).setExecutionId(3));
    when(taskDAO.closeRunningExecution(eq(1), eq(3), any())).thenReturn(false);

    assertThat(service.stopTask(1)).isEqualTo(1);

    verify(service).complete(1, 2, TaskStatus.JOB_STOPPED, "stopped by user", null, CompletionSource.USER);
    verify(service).complete(1, 3, TaskStatus.JOB_STOPPED, "stopped by user", null, CompletionSource.USER);
  }
}
