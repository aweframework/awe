package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.MaintainType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the single close of a running execution in {@link TaskExecutionRepository}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskExecutionRepositoryCloseTest {

  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private FlowRunRepository flowRuns;

  private TaskExecutionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TaskExecutionRepository(queryService, maintainService, queryUtil, executionLogStore, 10, flowRuns);
    given(queryUtil.getParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
  }

  @Test
  void closeIsTrueWhenExactlyOneRunningRowWasUpdated() throws AWException {
    given(maintainService.launchPrivateMaintain(eq("closeRunningExecution"), any(ObjectNode.class)))
      .willReturn(updated(1L));

    assertThat(repository.closeRunningExecution(3, 7, TaskStatus.JOB_WARNING)).isTrue();

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("closeRunningExecution"), parameters.capture());
    assertThat(parameters.getValue().get("taskId").asInt()).isEqualTo(3);
    assertThat(parameters.getValue().get("taskExecution").asInt()).isEqualTo(7);
    assertThat(parameters.getValue().get("newStatus").asInt()).isEqualTo(TaskStatus.JOB_WARNING.getValue());
  }

  @Test
  void closeIsFalseWhenTheExecutionWasNotRunning() throws AWException {
    given(maintainService.launchPrivateMaintain(eq("closeRunningExecution"), any(ObjectNode.class)))
      .willReturn(updated(0L));

    assertThat(repository.closeRunningExecution(3, 7, TaskStatus.JOB_OK)).isFalse();
  }

  private ServiceData updated(long rows) {
    return new ServiceData().addResultDetails(new MaintainResultDetails(MaintainType.UPDATE, rows));
  }
}
