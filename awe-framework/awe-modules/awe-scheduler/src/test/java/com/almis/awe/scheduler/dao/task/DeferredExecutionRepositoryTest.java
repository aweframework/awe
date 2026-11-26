package com.almis.awe.scheduler.dao.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.enums.CompletionSource;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the persistence of the deferred executions
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeferredExecutionRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:15:30Z");

  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;

  private DeferredExecutionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DeferredExecutionRepository(queryService, maintainService, queryUtil);
    given(queryUtil.getParameters()).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters(null, "1", "0")).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
  }

  @Test
  void insertStoresTheReferenceAndTheDatesOfTheExecution() throws AWException {
    repository.insert(3, 7, "job-77", NOW, NOW.plusSeconds(60));

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertDeferredExecution"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertThat(parameters.get("taskId").asInt()).isEqualTo(3);
    assertThat(parameters.get("taskExecution").asInt()).isEqualTo(7);
    assertThat(parameters.get("externalReference").asText()).isEqualTo("job-77");
    assertThat(parameters.get("acceptedDate").asText()).startsWith("13/11/2026");
    assertThat(parameters.get("dueDate").asText()).startsWith("13/11/2026");
  }

  @Test
  void completeStoresTheSourceAndTheDateOfTheClose() throws AWException {
    repository.complete(3, 7, CompletionSource.DEADLINE, NOW);

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("updateDeferredCompletion"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertThat(parameters.get("taskId").asInt()).isEqualTo(3);
    assertThat(parameters.get("taskExecution").asInt()).isEqualTo(7);
    assertThat(parameters.get("completionSource").asText()).isEqualTo("DEADLINE");
    assertThat(parameters.get("completionDate").asText()).startsWith("13/11/2026");
  }

  @Test
  void findReturnsTheDeferredStateOrNull() throws AWException {
    given(queryService.launchPrivateQuery(eq("getDeferredExecution"), any(ObjectNode.class)))
      .willReturn(rows(List.of(row(3, 7))), rows(List.of()));

    DeferredExecution found = repository.find(3, 7);

    assertThat(found.getTaskId()).isEqualTo(3);
    assertThat(found.getExecutionId()).isEqualTo(7);
    assertThat(found.getExternalReference()).isEqualTo("job-77");
    assertThat(repository.find(3, 8)).isNull();
  }

  @Test
  void findRunningFiltersByTaskAndDueDateOnlyWhenGiven() throws AWException {
    given(queryService.launchPrivateQuery(eq("getRunningDeferredExecutions"), any(ObjectNode.class)))
      .willReturn(rows(List.of(row(3, 7))));

    assertThat(repository.findRunning(3, NOW)).hasSize(1);
    repository.findRunning(null, null);

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, times(2)).launchPrivateQuery(eq("getRunningDeferredExecutions"), parameters.capture());
    ObjectNode filtered = parameters.getAllValues().get(0);
    assertThat(filtered.get("taskId").asInt()).isEqualTo(3);
    assertThat(filtered.get("limitDate").asText()).startsWith("13/11/2026");
    ObjectNode all = parameters.getAllValues().get(1);
    assertThat(all.has("taskId")).isFalse();
    assertThat(all.has("limitDate")).isFalse();
  }

  private ServiceData rows(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  private Map<String, CellData> row(int taskId, int executionId) {
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(taskId));
    row.put("executionId", new CellData(executionId));
    row.put("externalReference", new CellData("job-77"));
    return row;
  }
}
