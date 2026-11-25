package com.almis.awe.scheduler.controller;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.service.SchedulerExecutionCompletionService;
import com.almis.awe.scheduler.service.SchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract of the task catalog of the scheduler api (#993): the page and the counts of the active filter answer 200,
 * a filter value that is not valid answers 400 instead of a wider list
 */
class SchedulerControllerTaskCatalogTest {

  private final SchedulerService schedulerService = mock(SchedulerService.class);
  private final SchedulerController controller = new SchedulerController(schedulerService,
    mock(SchedulerExecutionCompletionService.class));

  @Test
  void theCatalogAndItsActiveCountsAnswer200OrAnInvalidFilterValue400() throws Exception {
    TaskCatalogCriteria valid = new TaskCatalogCriteria().setSearch("nightly");
    TaskCatalogCriteria invalid = new TaskCatalogCriteria().setLaunchType("api");
    ServiceData page = new ServiceData();
    ServiceData counts = new ServiceData();
    InvalidExecutionFilterException refused = new InvalidExecutionFilterException("Invalid filter",
      "The value api of the filter launchType is not valid");
    when(schedulerService.getTaskCatalog(valid)).thenReturn(page);
    when(schedulerService.getTaskCatalog(invalid)).thenThrow(refused);
    when(schedulerService.getTaskCatalogActiveCounts(valid)).thenReturn(counts);
    when(schedulerService.getTaskCatalogActiveCounts(invalid)).thenThrow(refused);

    for (ResponseEntity<Object> ok : new ResponseEntity[]{controller.getTaskCatalog(valid),
      controller.getTaskCatalogActiveCounts(valid)}) {
      assertThat(ok.getStatusCode().value()).isEqualTo(200);
    }
    assertThat(controller.getTaskCatalog(valid).getBody()).isSameAs(page);
    assertThat(controller.getTaskCatalogActiveCounts(valid).getBody()).isSameAs(counts);
    for (ResponseEntity<Object> bad : new ResponseEntity[]{controller.getTaskCatalog(invalid),
      controller.getTaskCatalogActiveCounts(invalid)}) {
      assertThat(bad.getStatusCode().value()).isEqualTo(400);
      assertThat(bad.getBody()).isEqualTo(Map.of("type", "invalid-request",
        "detail", "The value api of the filter launchType is not valid"));
    }
  }
}
