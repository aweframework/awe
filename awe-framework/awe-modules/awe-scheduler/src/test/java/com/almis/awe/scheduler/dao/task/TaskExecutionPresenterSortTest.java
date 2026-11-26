package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.ExecutionSort;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Sort of the execution list of all tasks of the activity screen ({@link TaskExecutionPresenter#getExecutionList}):
 * the sort of the grid becomes the sort of the page query, so the database sorts before it pages, and only the columns
 * that the database can sort are accepted
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionPresenterSortTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
  private static final String QUERY = "getActivityExecutionRows";

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private TaskExecutionRepository executionRepository;
  @Mock
  private ServiceConfig serviceConfig;
  @Mock
  private DeferredExecutionRepository deferredRepository;

  private TaskExecutionPresenter presenter;

  @BeforeEach
  void initPresenter() throws Exception {
    presenter = new TaskExecutionPresenter(queryService, queryUtil, executionLogStore, executionRepository,
      deferredRepository, serviceConfig, Clock.fixed(NOW, ZoneOffset.UTC));
    // The request parameters copied by getParameters carry the sort of the grid with its column names
    lenient().when(queryUtil.getParameters(any(), any(), any())).thenAnswer(call -> {
      ObjectNode parameters = JsonNodeFactory.instance.objectNode();
      parameters.putArray("sort").addObject().put("id", "ExeStaTxt").put("direction", "asc");
      return parameters;
    });
    lenient().when(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
  }

  @Test
  void theSortOfTheGridIsTheSortOfThePageQueryWithItsPageAndMax() throws Exception {
    presenter.getExecutionList(new ExecutionListCriteria().setPage(2).setMax(25)
      .setSort(List.of(new ExecutionSort("ExeEla", "desc"), new ExecutionSort("TskNam", "ASC"))));

    verify(queryUtil).getParameters(null, "2", "25");
    ObjectNode parameters = launchedParameters();
    assertEquals("[{\"id\":\"ExeEla\",\"direction\":\"DESC\"},{\"id\":\"TskNam\",\"direction\":\"ASC\"}]",
      parameters.get("sort").toString());
    // The elapsed time of a running execution is measured until now
    assertEquals(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS").format(NOW.atZone(ZoneId.systemDefault())),
      parameters.get("exeNow").asString());
  }

  @ParameterizedTest
  @CsvSource({"ExeLstTim, ExeLstTim", "TskNam, TskNam", "Sta, Sta", "ExeStaTxt, Sta", "ExeEla, ExeEla",
    "ExePrg, ExeEla", "ExeTim, ExeEla", "ExeLchBy, LchBy", "TypLch, TypLch", "ExeOriTxt, TypLch"})
  void eachSortableColumnIsSortedByItsDatabaseField(String column, String field) throws Exception {
    presenter.getExecutionList(new ExecutionListCriteria().setSort(List.of(new ExecutionSort(column, "asc"))));

    assertEquals("[{\"id\":\"" + field + "\",\"direction\":\"ASC\"}]", launchedParameters().get("sort").toString());
  }

  @Test
  void withoutSortThePageQueryKeepsItsNewestFirstOrderInsteadOfTheSortOfTheRequest() throws Exception {
    presenter.getExecutionList(new ExecutionListCriteria());

    assertEquals("[]", launchedParameters().get("sort").toString());
  }

  @Test
  void anUnknownColumnOrDirectionIsRejected() throws Exception {
    assertRejected(new ExecutionSort("ExeDes", "asc"), "ExeDes");
    assertRejected(new ExecutionSort("TskNam", "up"), "up");
    assertRejected(new ExecutionSort(null, "asc"), "null");
    assertRejected(new ExecutionSort("TskNam", null), "null");
  }

  @Test
  void theSortOfTheGridRequestIsBoundToTheCriteria() {
    ObjectNode sort = JsonNodeFactory.instance.objectNode().put("id", "ExeEla").put("direction", "desc").put("order", -1);
    Map<String, Object> request = new HashMap<>();
    request.put("sort", List.of(sort));
    request.put("page", 2);

    ExecutionListCriteria criteria = DataListUtil.getParameterBeanValue(ExecutionListCriteria.class, request);

    assertEquals(List.of(new ExecutionSort("ExeEla", "desc")), criteria.getSort());
    assertEquals(2, criteria.getPage());
  }

  private void assertRejected(ExecutionSort sort, String value) throws Exception {
    given(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_TITLE")).willReturn("Invalid filter");
    given(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", "sort", value)).willReturn("Invalid sort " + value);
    ExecutionListCriteria criteria = new ExecutionListCriteria().setSort(List.of(sort));

    InvalidExecutionFilterException error = assertThrows(InvalidExecutionFilterException.class,
      () -> presenter.getExecutionList(criteria));

    assertEquals("Invalid filter", error.getTitle());
    assertEquals("Invalid sort " + value, error.getMessage());
    verify(queryService, never()).launchPrivateQuery(eq(QUERY), any(ObjectNode.class));
  }

  private ObjectNode launchedParameters() throws Exception {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq(QUERY), captor.capture());
    return captor.getValue();
  }
}
