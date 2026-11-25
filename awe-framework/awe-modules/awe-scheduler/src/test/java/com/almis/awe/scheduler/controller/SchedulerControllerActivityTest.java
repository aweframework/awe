package com.almis.awe.scheduler.controller;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.service.SchedulerExecutionCompletionService;
import com.almis.awe.scheduler.service.SchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract of the execution list of the scheduler api: a filter value that is not valid answers 400, as the other
 * validation errors of the api, instead of a wider list
 */
class SchedulerControllerActivityTest {

  private final SchedulerService schedulerService = mock(SchedulerService.class);
  private final SchedulerController controller = new SchedulerController(schedulerService,
    mock(SchedulerExecutionCompletionService.class));

  @Test
  void aValidCriteriaAnswers200WithThePage() throws Exception {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setOrigins(List.of("manual"));
    ServiceData page = new ServiceData();
    when(schedulerService.getExecutionList(criteria)).thenReturn(page);

    ResponseEntity<Object> response = controller.getExecutionList(criteria);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(page);
  }

  @Test
  void anInvalidFilterValueAnswers400WithTheValidationMessage() throws Exception {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setOrigins(List.of("api"));
    when(schedulerService.getExecutionList(criteria))
      .thenThrow(new InvalidExecutionFilterException("Invalid filter", "The value api of the filter origins is not valid"));

    ResponseEntity<Object> response = controller.getExecutionList(criteria);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo(Map.of("type", "invalid-request",
      "detail", "The value api of the filter origins is not valid"));
  }

  @Test
  void theStateCountsAnswer200WithTheCountsOr400ForAnInvalidFilterValue() throws Exception {
    ExecutionListCriteria valid = new ExecutionListCriteria().setTaskName("nightly");
    ExecutionListCriteria invalid = new ExecutionListCriteria().setState("api");
    ServiceData counts = new ServiceData();
    when(schedulerService.getExecutionStateCounts(valid)).thenReturn(counts);
    when(schedulerService.getExecutionStateCounts(invalid))
      .thenThrow(new InvalidExecutionFilterException("Invalid filter", "The value api of the filter state is not valid"));

    ResponseEntity<Object> ok = controller.getExecutionStateCounts(valid);
    ResponseEntity<Object> refused = controller.getExecutionStateCounts(invalid);

    assertThat(ok.getStatusCode().value()).isEqualTo(200);
    assertThat(ok.getBody()).isSameAs(counts);
    assertThat(refused.getStatusCode().value()).isEqualTo(400);
    assertThat(refused.getBody()).isEqualTo(Map.of("type", "invalid-request",
      "detail", "The value api of the filter state is not valid"));
  }

  @Test
  void theExecutionsGroupedByFlowRunAnswer200WithThePageOr400ForAnInvalidFilterValue() throws Exception {
    ExecutionListCriteria valid = new ExecutionListCriteria().setTaskName("nightly");
    ExecutionListCriteria invalid = new ExecutionListCriteria().setMax(501);
    ServiceData runs = new ServiceData();
    when(schedulerService.getRunList(valid)).thenReturn(runs);
    when(schedulerService.getRunList(invalid))
      .thenThrow(new InvalidExecutionFilterException("Invalid filter", "The value 501 of the filter max is not valid"));

    ResponseEntity<Object> ok = controller.getRunList(valid);
    ResponseEntity<Object> refused = controller.getRunList(invalid);

    assertThat(ok.getStatusCode().value()).isEqualTo(200);
    assertThat(ok.getBody()).isSameAs(runs);
    assertThat(refused.getStatusCode().value()).isEqualTo(400);
    assertThat(refused.getBody()).isEqualTo(Map.of("type", "invalid-request",
      "detail", "The value 501 of the filter max is not valid"));
  }
}
