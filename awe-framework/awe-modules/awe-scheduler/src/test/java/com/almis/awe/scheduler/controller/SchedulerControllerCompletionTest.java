package com.almis.awe.scheduler.controller;

import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.service.CompletionResult;
import com.almis.awe.scheduler.service.CompletionResult.Outcome;
import com.almis.awe.scheduler.service.SchedulerExecutionCompletionService;
import com.almis.awe.scheduler.service.SchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Contract of the completion api of a deferred execution (statuses 200, 400, 404 and 409)
 */
class SchedulerControllerCompletionTest {

  private final SchedulerExecutionCompletionService completionService = mock(SchedulerExecutionCompletionService.class);
  private final SchedulerController controller = new SchedulerController(mock(SchedulerService.class),
    completionService);

  private ResponseEntity<Map<String, Object>> complete(String status, String description) throws Exception {
    return controller.completeExecution(3, 7, new SchedulerController.CompletionRequest(status, description, null));
  }

  @Test
  void aClosedExecutionAnswers200WithTheAppliedStatus() throws Exception {
    when(completionService.complete(eq(3), eq(7), eq(TaskStatus.JOB_WARNING), eq("partial"), any()))
      .thenReturn(new CompletionResult(Outcome.CLOSED, TaskStatus.JOB_WARNING));

    ResponseEntity<Map<String, Object>> response = complete("warning", "partial");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).containsEntry("taskId", 3).containsEntry("executionId", 7)
      .containsEntry("status", "JOB_WARNING");
  }

  @Test
  void everyStatusNameIsAcceptedCaseInsensitively() throws Exception {
    when(completionService.complete(eq(3), eq(7), any(), any(), any()))
      .thenReturn(new CompletionResult(Outcome.CLOSED, TaskStatus.JOB_OK));

    for (String status : new String[]{"OK", "Warning", "error", "STOPPED", "interrupted", "INFO"}) {
      assertThat(complete(status, null).getStatusCode().value()).as(status).isEqualTo(200);
    }
    verify(completionService).complete(3, 7, TaskStatus.JOB_INTERRUPTED, null, null);
  }

  @Test
  void anExecutionNotRunningAnswers409WithItsCurrentStatus() throws Exception {
    when(completionService.complete(eq(3), eq(7), any(), any(), any()))
      .thenReturn(new CompletionResult(Outcome.NOT_RUNNING, TaskStatus.JOB_INTERRUPTED));

    ResponseEntity<Map<String, Object>> response = complete("OK", null);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).containsEntry("type", "execution-not-running")
      .containsEntry("currentStatus", "JOB_INTERRUPTED");
  }

  @Test
  void anExecutionNotDeferredAnswers409() throws Exception {
    when(completionService.complete(eq(3), eq(7), any(), any(), any()))
      .thenReturn(new CompletionResult(Outcome.NOT_DEFERRED, TaskStatus.JOB_RUNNING));

    ResponseEntity<Map<String, Object>> response = complete("OK", null);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).containsEntry("type", "execution-not-deferred")
      .containsEntry("currentStatus", "JOB_RUNNING");
  }

  @Test
  void anUnknownExecutionAnswers404() throws Exception {
    when(completionService.complete(eq(3), eq(7), any(), any(), any()))
      .thenReturn(new CompletionResult(Outcome.UNKNOWN, null));

    assertThat(complete("OK", null).getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void aMissingOrInvalidStatusAnswers400WithoutTouchingTheExecution() throws Exception {
    assertThat(complete(null, null).getStatusCode().value()).isEqualTo(400);
    assertThat(complete("RUNNING", null).getStatusCode().value()).isEqualTo(400);
    assertThat(complete("DONE", null).getStatusCode().value()).isEqualTo(400);

    verifyNoInteractions(completionService);
  }

  @Test
  void aDescriptionLongerThan2000CharactersAnswers400() throws Exception {
    assertThat(complete("OK", "x".repeat(2001)).getStatusCode().value()).isEqualTo(400);

    verifyNoInteractions(completionService);
  }
}
