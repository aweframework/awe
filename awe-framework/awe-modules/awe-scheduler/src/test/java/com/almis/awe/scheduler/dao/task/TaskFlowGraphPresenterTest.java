package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskSummary;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.util.TaskWhenSummary;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * One-level flow graph of the task screen (#993): every task of the level (the task, the tasks it runs after and the
 * ones it launches) tells when its last execution started, in the relative words of the catalog, and a scheduled one
 * its next execution; the other rows of the graph go as the getFlowGraph query reads them
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskFlowGraphPresenterTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 10, 0);
  private static final int PARENT = 1;
  private static final int TASK = 2;
  private static final int CHILD = 3;
  private static final int OTHER_PARENT = 4;

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskCatalogPresenter catalogPresenter;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskFlowGraphPresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    presenter = new TaskFlowGraphPresenter(queryService, queryUtil, catalogPresenter, serviceConfig,
      Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(serviceConfig.getLocale(anyString())).willAnswer(invocation -> switch ((String) invocation.getArgument(0)) {
      case "SCREEN_TEXT_TASK_CATALOG_TODAY" -> "Hoy {0}";
      case "SCREEN_TEXT_TASK_CATALOG_TOMORROW" -> "Mañana {0}";
      case "SCREEN_TEXT_FLOW_GRAPH_NEXT" -> "Próxima: {0}";
      default -> invocation.getArgument(0);
    });
    // The parent → the task → the child, which also waits for another parent
    given(queryService.launchPrivateQuery(eq("getFlowGraph"), any(ObjectNode.class))).willReturn(data(
      row(PARENT, null), row(TASK, PARENT), row(CHILD, TASK), row(CHILD, OTHER_PARENT), row(OTHER_PARENT, null)));
    given(catalogPresenter.getSummaries(anyCollection())).willReturn(Map.of(
      PARENT, summary(PARENT, TaskLaunchKind.SCHEDULED, NOW.minusHours(2), NOW.plusDays(1).withHour(3)),
      TASK, summary(TASK, TaskLaunchKind.DEPENDENCY, null, null),
      CHILD, summary(CHILD, TaskLaunchKind.FILE, NOW.minusMinutes(5), NOW.plusMinutes(5))));
  }

  @Test
  void theTasksOfTheLevelTellWhenTheyLastRanAndAScheduledOneItsNextExecution() throws Exception {
    Map<Integer, Map<String, CellData>> rows = byTask(presenter.getGraph(TASK));

    assertThat(text(rows.get(PARENT), "lastWhen")).isEqualTo("hoy 08:00");
    assertThat(text(rows.get(PARENT), "nextRun")).isEqualTo("Próxima: mañana 03:00");
    // Without executions, and a file task: no next execution in the node
    assertThat(text(rows.get(TASK), "lastWhen")).isEmpty();
    assertThat(text(rows.get(TASK), "nextRun")).isEmpty();
    assertThat(text(rows.get(CHILD), "lastWhen")).isEqualTo("hoy 09:55");
    assertThat(text(rows.get(CHILD), "nextRun")).isEmpty();
    // The other parent of the child is out of the level: its row goes as the query reads it
    assertThat(text(rows.get(OTHER_PARENT), "lastWhen")).isEmpty();
    assertThat(rows.get(OTHER_PARENT).get("name").getStringValue()).isEqualTo("Task " + OTHER_PARENT);
  }

  @Test
  void theGraphIsReadForTheTaskAndOnlyTheTasksOfTheLevelAreSummarized() throws Exception {
    presenter.getGraph(TASK);

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("getFlowGraph"), parameters.capture());
    assertThat(parameters.getValue().get("IdeTsk.selected").asInt()).isEqualTo(TASK);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<Integer>> level = ArgumentCaptor.forClass(Collection.class);
    verify(catalogPresenter).getSummaries(level.capture());
    assertThat(Set.copyOf(level.getValue())).containsExactlyInAnyOrder(PARENT, TASK, CHILD);
  }

  private static TaskSummary summary(int id, TaskLaunchKind kind, LocalDateTime last, LocalDateTime next) {
    return new TaskSummary(id, "Task " + id, null, null, true, kind, new TaskLaunch(),
      new TaskWhenSummary.When(kind, kind.getIcon(), "", ""), new TaskWhenSummary.Line("", ""), next, null,
      last == null ? null : 0, false, last == null ? null : Date.from(last.atZone(ZoneId.systemDefault()).toInstant()));
  }

  private static Map<String, CellData> row(int taskId, Integer parentId) {
    Map<String, CellData> row = new HashMap<>();
    row.put("taskId", new CellData(taskId));
    row.put("parentId", parentId == null ? new CellData() : new CellData(parentId));
    row.put("name", new CellData("Task " + taskId));
    return row;
  }

  @SafeVarargs
  private static ServiceData data(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  private static Map<Integer, Map<String, CellData>> byTask(ServiceData data) {
    Map<Integer, Map<String, CellData>> rows = new HashMap<>();
    data.getDataList().getRows().forEach(row -> rows.putIfAbsent(row.get("taskId").getIntegerValue(), row));
    return rows;
  }

  private static String text(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null ? null : cell.getStringValue();
  }
}
