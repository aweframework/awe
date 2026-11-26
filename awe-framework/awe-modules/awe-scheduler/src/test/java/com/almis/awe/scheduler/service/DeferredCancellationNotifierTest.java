package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.scheduled.MaintainJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeferredCancellationNotifierTest {

  private static final String CANCEL_MAINTAIN = "cancelJob";

  @Mock
  private TaskDAO taskDAO;
  @Mock
  private MaintainJobService maintainJobService;

  private DeferredCancellationNotifier notifier;
  private final DeferredExecution deferred = new DeferredExecution().setTaskId(3).setExecutionId(7)
    .setExternalReference("job-77");

  @BeforeEach
  void setUp() {
    notifier = new DeferredCancellationNotifier(taskDAO, maintainJobService);
    given(maintainJobService.newParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
  }

  @Test
  void aTaskWithoutCancellationTargetIsNotNotified() {
    notifier.notifyCancellation(new Task().setTaskId(3), deferred);
    notifier.notifyCancellation(new Task().setTaskId(3).setCancellationMaintain(" "), deferred);

    verifyNoInteractions(maintainJobService, taskDAO);
  }

  @Test
  void theTargetGetsTheExecutionKeyAndTheReferenceAndTheOutcomeIsStored() throws Exception {
    given(maintainJobService.launchMaintain(eq(CANCEL_MAINTAIN), any(), any())).willReturn(new ServiceData());

    notifier.notifyCancellation(task(), deferred);

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    ArgumentCaptor<TaskExecution> execution = ArgumentCaptor.forClass(TaskExecution.class);
    verify(maintainJobService).launchMaintain(eq(CANCEL_MAINTAIN), parameters.capture(), execution.capture());
    assertThat(parameters.getValue().get("taskId").asInt()).isEqualTo(3);
    assertThat(parameters.getValue().get("executionId").asInt()).isEqualTo(7);
    assertThat(parameters.getValue().get("externalReference").asText()).isEqualTo("job-77");
    assertThat(execution.getValue().getTaskId()).isEqualTo(3);
    assertThat(execution.getValue().getExecutionId()).isEqualTo(7);
    verify(taskDAO).recordCancellationNotice(eq(3), eq(7), eq(DeferredCancellationNotifier.SENT), isNull());
  }

  @Test
  void anErrorAnswerIsStoredWithItsMessage() throws Exception {
    given(maintainJobService.launchMaintain(any(), any(), any()))
      .willReturn(new ServiceData().setType(AnswerType.ERROR).setMessage("unknown job"));

    notifier.notifyCancellation(task(), deferred);

    verify(taskDAO).recordCancellationNotice(3, 7, DeferredCancellationNotifier.FAILED, "unknown job");
  }

  @Test
  void aFailingCallIsStoredTruncatedAndNeverThrows() throws Exception {
    given(maintainJobService.launchMaintain(any(), any(), any()))
      .willThrow(new IllegalStateException("x".repeat(3000)));

    Task task = task();
    assertThatNoException().isThrownBy(() -> notifier.notifyCancellation(task, deferred));

    ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
    verify(taskDAO).recordCancellationNotice(eq(3), eq(7), eq(DeferredCancellationNotifier.FAILED), error.capture());
    assertThat(error.getValue()).hasSize(2000);
  }

  @Test
  void aFailureStoringTheOutcomeNeverThrows() throws Exception {
    given(maintainJobService.launchMaintain(any(), any(), any())).willReturn(new ServiceData());
    doThrow(new AWException("db down")).when(taskDAO).recordCancellationNotice(any(), any(), any(), any());

    Task task = task();
    assertThatNoException().isThrownBy(() -> notifier.notifyCancellation(task, deferred));
  }

  private Task task() {
    return new Task().setTaskId(3).setCancellationMaintain(CANCEL_MAINTAIN);
  }
}
