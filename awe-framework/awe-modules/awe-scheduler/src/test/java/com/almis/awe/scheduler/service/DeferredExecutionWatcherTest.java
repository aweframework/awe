package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.CompletionSource;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeferredExecutionWatcherTest {

  @Mock
  private TaskDAO taskDAO;
  @Mock
  private SchedulerExecutionCompletionService completionService;

  private DeferredExecutionWatcher watcher;

  @BeforeEach
  void setUp() {
    watcher = new DeferredExecutionWatcher(taskDAO, completionService, Duration.ofSeconds(60));
  }

  @Test
  void theExecutionsPastTheirDueDateAreInterrupted() throws Exception {
    given(taskDAO.getRunningDeferredExecutions(isNull(), any(Instant.class)))
      .willReturn(List.of(overdue(1, 2), overdue(3, 4)));

    watcher.sweep();

    verify(completionService).complete(1, 2, TaskStatus.JOB_INTERRUPTED, null, null,
      CompletionSource.DEADLINE);
    verify(completionService).complete(3, 4, TaskStatus.JOB_INTERRUPTED, null, null,
      CompletionSource.DEADLINE);
  }

  @Test
  void onlyTheExecutionsDueBeforeNowAreRequested() throws Exception {
    given(taskDAO.getRunningDeferredExecutions(isNull(), any(Instant.class))).willReturn(List.of());
    Instant before = Instant.now();

    watcher.sweep();

    ArgumentCaptor<Instant> limit = ArgumentCaptor.forClass(Instant.class);
    verify(taskDAO).getRunningDeferredExecutions(isNull(), limit.capture());
    assertThat(limit.getValue()).isBetween(before, Instant.now());
  }

  @Test
  void aFailingExecutionDoesNotStopTheOthers() throws Exception {
    given(taskDAO.getRunningDeferredExecutions(isNull(), any(Instant.class)))
      .willReturn(List.of(overdue(1, 2), overdue(3, 4)));
    given(completionService.complete(1, 2, TaskStatus.JOB_INTERRUPTED, null, null,
      CompletionSource.DEADLINE)).willThrow(new AWException("boom"));

    watcher.sweep();

    verify(completionService).complete(3, 4, TaskStatus.JOB_INTERRUPTED, null, null,
      CompletionSource.DEADLINE);
  }

  @Test
  void nothingOverdueDoesNothing() throws Exception {
    given(taskDAO.getRunningDeferredExecutions(isNull(), any(Instant.class))).willReturn(List.of());

    watcher.sweep();

    verifyNoInteractions(completionService);
  }

  @Test
  void aFailingQueryNeverEscapesSoThePeriodicRunSurvives() throws Exception {
    given(taskDAO.getRunningDeferredExecutions(isNull(), any(Instant.class))).willThrow(new AWException("db down"));

    assertThatCode(watcher::sweep).doesNotThrowAnyException();
  }

  private DeferredExecution overdue(int taskId, int executionId) {
    return new DeferredExecution().setTaskId(taskId).setExecutionId(executionId);
  }
}
