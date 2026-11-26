package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.ExecutionSort;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Task catalog of the scheduler (#993): the history strip of the tasks of a page is read with one query for the whole
 * page, it keeps the last 20 saved executions of each task from the oldest to the newest, a filter value that is
 * not valid is refused before anything is read, and the summary of a task that does not exist is empty
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskCatalogPresenterTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private FlowRunRepository flowRunRepository;
  @Mock
  private TaskListPresenter taskListPresenter;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskCatalogPresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    presenter = new TaskCatalogPresenter(queryService, queryUtil, flowRunRepository, taskListPresenter, serviceConfig,
      Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(flowRunRepository.getDependencies()).willReturn(List.of());
    given(taskListPresenter.getNextFireTime(anyInt(), anyInt())).willReturn(Optional.empty());
    given(serviceConfig.getLocale(anyString())).willAnswer(invocation -> invocation.getArgument(0) + "[{0}|{1}|{2}]");
    given(queryService.findLabel(anyString(), anyString())).willAnswer(invocation -> {
      String value = invocation.getArgument(1);
      return switch ((String) invocation.getArgument(0)) {
        case "ActStaTon" -> "1".equals(value) ? "danger" : "success";
        case "ActStaIco" -> "icon-" + value;
        default -> "STATUS_" + value;
      };
    });
    given(queryService.launchPrivateQuery(eq("getTaskCatalogRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(task(3, "Gamma"), task(1, "Alpha"), task(2, "Beta"))));
    given(queryService.launchPrivateQuery(eq("getTasksWithParameters"), any(ObjectNode.class)))
      .willReturn(data(List.of()));
    // 25 saved executions of Alpha, newest first as the query sorts them: the last one ended with error
    List<Map<String, CellData>> history = new ArrayList<>();
    for (int execution = 25; execution >= 1; execution--) {
      history.add(execution(1, execution, execution == 25 ? 1 : 0));
    }
    given(queryService.launchPrivateQuery(eq("getTaskCatalogHistory"), any(ObjectNode.class)))
      .willReturn(data(history));
  }

  @Test
  void theHistoryOfThePageTasksIsReadWithOneQueryAndKeepsTheLast20ExecutionsOldestFirst() throws Exception {
    ServiceData page = presenter.getCatalog(new TaskCatalogCriteria().setPage(1).setMax(2));

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, times(1)).launchPrivateQuery(eq("getTaskCatalogHistory"), parameters.capture());
    assertThat(parameters.getValue().get("catalogTaskIds").toString()).isEqualTo("[1,2]");

    List<Map<String, CellData>> rows = page.getDataList().getRows();
    assertThat(rows).extracting(row -> row.get("Nam").getStringValue()).containsExactly("Alpha", "Beta");
    assertThat(page.getDataList().getRecords()).isEqualTo(3);
    JsonNode strip = new JsonMapper().readTree(rows.get(0).get("TskHis").getStringValue());
    assertThat(strip).hasSize(20);
    assertThat(strip.get(0).get("title").asString()).startsWith("SCREEN_TEXT_TASK_CATALOG_HISTORY_TITLE[6|");
    assertThat(strip.get(19).get("tone").asString()).isEqualTo("danger");
    assertThat(strip.get(19).get("label").asString()).isEqualTo("STATUS_1[{0}|{1}|{2}]");
    assertThat(new JsonMapper().readTree(rows.get(1).get("TskHis").getStringValue())).isEmpty();
  }

  static Stream<TaskCatalogCriteria> invalidCriteria() {
    return Stream.of(
      new TaskCatalogCriteria().setLaunchType("api"),
      new TaskCatalogCriteria().setLastStatus("lost"),
      new TaskCatalogCriteria().setActive("2"),
      new TaskCatalogCriteria().setSort(List.of(new ExecutionSort("Des", "asc"))),
      // The next execution is not sortable: it mixes fire times with what launches the task
      new TaskCatalogCriteria().setSort(List.of(new ExecutionSort("TskNxtTxt", "asc"))),
      new TaskCatalogCriteria().setSort(List.of(new ExecutionSort("TskNxtDat", "desc"))),
      new TaskCatalogCriteria().setSort(List.of(new ExecutionSort("Nam", "up"))),
      new TaskCatalogCriteria().setMax(501),
      new TaskCatalogCriteria().setPage(0));
  }

  @ParameterizedTest
  @MethodSource("invalidCriteria")
  void aFilterValueThatIsNotValidIsRefusedBeforeAnythingIsRead(TaskCatalogCriteria criteria) throws Exception {
    assertThrows(InvalidExecutionFilterException.class, () -> presenter.getCatalog(criteria));
    assertThrows(InvalidExecutionFilterException.class, () -> presenter.getActiveCounts(criteria));
    verify(queryService, never()).launchPrivateQuery(eq("getTaskCatalogRows"), any(ObjectNode.class));
  }

  @Test
  void theSummaryOfATaskThatDoesNotExistIsEmptyAndWithoutATaskNothingIsRead() throws Exception {
    assertThat(presenter.getSummary(99)).isEmpty();
    assertThat(presenter.getSummary(null)).isEmpty();

    verify(queryService, times(1)).launchPrivateQuery(eq("getTaskCatalogRows"), any(ObjectNode.class));
    assertThat(presenter.getSummary(1)).hasValueSatisfying(task -> assertThat(task.name()).isEqualTo("Alpha"));
  }

  private static ServiceData data(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, CellData> task(int id, String name) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(id));
    row.put("Nam", new CellData(name));
    row.put("Act", new CellData(1));
    row.put("TypLch", new CellData(0));
    return row;
  }

  private static List<String> names(ServiceData page) {
    return page.getDataList().getRows().stream().map(row -> row.get("Nam").getStringValue()).toList();
  }

  private static Map<String, CellData> scheduled(int id, String name) {
    Map<String, CellData> row = task(id, name);
    row.put("TypLch", new CellData(1));
    return row;
  }

  private static Map<String, CellData> execution(int taskId, int executionId, int status) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(taskId));
    row.put("ExeTsk", new CellData(executionId));
    row.put("Sta", new CellData(status));
    row.put("IniDat", new CellData(Date.from(NOW.minusDays(30L - executionId).atZone(ZoneId.systemDefault())
      .toInstant())));
    row.put("ExeTim", new CellData(60_000));
    return row;
  }
}
