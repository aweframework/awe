package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskSummary;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_FLOW_GRAPH;

/**
 * One-level flow graph of the task screen (#993): the rows of the getFlowGraph query (one per task and parent, the
 * task as its root), where every task of the level (the task, the tasks it runs after and the tasks it launches) also
 * tells when its last execution started (lastWhen) and, when it is a scheduled task, its next execution (nextRun), in
 * the relative words of the task catalog ({@link TaskWhenSummary#relative(LocalDateTime, LocalDateTime)}) and its next
 * fire time. The other rows (the other parents a child waits for) go as the query reads them, with both texts empty
 */
public class TaskFlowGraphPresenter {

  /**
   * When the last execution of a task of the level started, empty without executions
   */
  public static final String LAST_WHEN = "lastWhen";
  /**
   * Next execution of a scheduled task of the level («Next: tomorrow 03:00»), empty for the other tasks
   */
  public static final String NEXT_RUN = "nextRun";
  private static final String NEXT = "SCREEN_TEXT_FLOW_GRAPH_NEXT";
  private static final String TASK_ID = "taskId";
  private static final String PARENT_ID = "parentId";

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final TaskCatalogPresenter catalogPresenter;
  private final ServiceConfig serviceConfig;
  private final Clock clock;
  private final TaskWhenSummary summary;

  /**
   * Constructor
   *
   * @param queryService     Query service
   * @param queryUtil        Query utilities
   * @param catalogPresenter Task catalog presenter (the last execution and the next fire time of the tasks)
   * @param serviceConfig    Service configuration giving access to the locales
   * @param clock            Clock of the relative dates
   */
  public TaskFlowGraphPresenter(QueryService queryService, QueryUtil queryUtil, TaskCatalogPresenter catalogPresenter,
                                ServiceConfig serviceConfig, Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.catalogPresenter = catalogPresenter;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
    this.summary = new TaskWhenSummary(this::text);
  }

  /**
   * Retrieve the one-level flow graph of a task
   *
   * @param taskId Task identifier (the root of the graph)
   * @return Rows of the graph, the ones of the level with when they last ran and their next execution
   * @throws AWException The graph or the tasks cannot be read
   */
  public ServiceData getGraph(Integer taskId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("IdeTsk.selected", taskId);
    DataList dataList = queryService.launchPrivateQuery(SCHEDULER_FLOW_GRAPH, parameters).getDataList();
    List<Map<String, CellData>> rows = dataList.getRows();

    Map<Integer, TaskSummary> summaries = taskId == null ? Map.of() : catalogPresenter.getSummaries(getLevel(taskId, rows));
    LocalDateTime now = LocalDateTime.now(clock);
    for (Map<String, CellData> row : rows) {
      TaskSummary task = summaries.get(integer(row, TASK_ID));
      String lastWhen = task == null || task.lastDate() == null ? ""
        : lowerFirst(summary.relative(LegacyDateConverter.asLocalDateTime(task.lastDate()), now));
      String nextRun = task == null || task.kind() != TaskLaunchKind.SCHEDULED || task.nextFire() == null ? ""
        : text(NEXT, lowerFirst(summary.relative(task.nextFire(), now)));
      row.put(LAST_WHEN, new CellData(lastWhen));
      row.put(NEXT_RUN, new CellData(nextRun));
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Tasks of the level of a task: the task, the tasks it runs after and the tasks it launches
   */
  private static Set<Integer> getLevel(Integer taskId, List<Map<String, CellData>> rows) {
    Set<Integer> level = new LinkedHashSet<>();
    level.add(taskId);
    for (Map<String, CellData> row : rows) {
      Integer task = integer(row, TASK_ID);
      Integer parent = integer(row, PARENT_ID);
      if (taskId.equals(task) && parent != null) {
        level.add(parent);
      } else if (taskId.equals(parent) && task != null) {
        level.add(task);
      }
    }
    return level;
  }

  /**
   * The relative date goes inside a line («OK · today 08:00», «Next: tomorrow 03:00»), so it starts in lower case
   */
  private static String lowerFirst(String text) {
    return text.isEmpty() ? text : text.substring(0, 1).toLowerCase(Locale.ROOT) + text.substring(1);
  }

  /**
   * Text of a locale with its parameters
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }

  private static Integer integer(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null || cell.getStringValue().isBlank() ? null
      : cell.getIntegerValue();
  }
}
