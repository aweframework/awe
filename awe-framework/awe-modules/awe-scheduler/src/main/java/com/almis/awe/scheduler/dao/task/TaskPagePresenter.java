package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskSummary;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASKS_WITH_PARAMETERS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_PAGE_EXECUTIONS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_PAGE_TASK;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_PAGE_TASK_RESTRICTED;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;

/**
 * Task screen of the scheduler («ficha», #993): one row with what the screen shows of a task. Its header (name, last
 * status, description and its facts as pills: id, what it runs, when it is launched, kept executions, maximum time and
 * report),
 * the figures of its summary over its saved executions (success rate with the count of each status, average time with
 * the maximum allowed and the durations of its last executions, next execution), the strip of its last saved
 * executions, its last execution as the activity screen shows it and a read-only summary of its configuration. The
 * texts are in the language of the user; a value the task does not have is «—». Where the task runs (its server and
 * its path) and who gets its report are only read for the users who hold the permission of their query (the one of
 * the task editor): for the others they are «—» or left out.
 * <p>
 * The screen (its target) and each of its cards read the page through a query of their own. The screen loads them all
 * in its request, in parallel threads that share the request: the page is computed once per request and task, so a
 * page load reads the catalog once and its cards cannot disagree. Each caller gets its own copy of the row.
 */
public class TaskPagePresenter {

  private static final String PAGE = "SCREEN_TEXT_TASK_PAGE_";
  private static final String WHEN = "SCREEN_TEXT_TASK_WHEN_";
  private static final String EMPTY = TaskWhenSummary.EMPTY_VALUE;
  private static final String SEPARATOR = " · ";
  private static final String TASK_ID = "IdeTsk";
  private static final String STATUS = "Sta";
  private static final String EXECUTION_TIME = "ExeTim";
  private static final String NEUTRAL_TONE = "neutral";
  private static final String TIMEOUT = "TimOutExe";
  private static final String LABEL = "label";
  private static final int MAINTAIN = 1;
  private static final Set<Integer> NOT_ENDED = Set.of(TaskStatus.JOB_RUNNING.getValue(),
    TaskStatus.JOB_QUEUED.getValue());
  /**
   * Order of the statuses in the counts of the summary: the ones that ended well first
   */
  private static final List<Integer> STATUS_ORDER = List.of(TaskStatus.JOB_OK.getValue(),
    TaskStatus.JOB_WARNING.getValue(), TaskStatus.JOB_ERROR.getValue(), TaskStatus.JOB_STOPPED.getValue(),
    TaskStatus.JOB_INTERRUPTED.getValue(), TaskStatus.JOB_INFO.getValue());
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  /**
   * Request attribute with the pages computed in the request, by task
   */
  private static final String PAGES = TaskPagePresenter.class.getName() + ".pages";
  private static final Object PAGES_LOCK = new Object();

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final TaskCatalogPresenter catalogPresenter;
  private final TaskExecutionPresenter executionPresenter;
  private final ServiceConfig serviceConfig;
  private final Predicate<String> canLaunchQuery;

  /**
   * Constructor
   *
   * @param queryService       Query service
   * @param queryUtil          Query utilities
   * @param catalogPresenter   Task catalog presenter (the summary of the task and its history strip)
   * @param executionPresenter Execution presenter (the last execution as the activity screen shows it)
   * @param serviceConfig      Service configuration giving access to the locales
   * @param canLaunchQuery     The session user holds the permission of a query (by its identifier)
   */
  public TaskPagePresenter(QueryService queryService, QueryUtil queryUtil, TaskCatalogPresenter catalogPresenter,
                           TaskExecutionPresenter executionPresenter, ServiceConfig serviceConfig,
                           Predicate<String> canLaunchQuery) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.catalogPresenter = catalogPresenter;
    this.executionPresenter = executionPresenter;
    this.serviceConfig = serviceConfig;
    this.canLaunchQuery = canLaunchQuery;
  }

  /**
   * Retrieve what the task screen shows of a task, computed once per request and task (the screen and its cards share
   * it); outside a request it is computed on every call
   *
   * @param taskId Task identifier
   * @return One row, none when the task does not exist
   * @throws AWException The task cannot be read
   */
  public ServiceData getPage(Integer taskId) throws AWException {
    RequestAttributes request = RequestContextHolder.getRequestAttributes();
    if (request == null) {
      return computePage(taskId);
    }
    CompletableFuture<ServiceData> page = new CompletableFuture<>();
    CompletableFuture<ServiceData> shared = getRequestPages(request).putIfAbsent(String.valueOf(taskId), page);
    if (shared == null) {
      try {
        page.complete(computePage(taskId));
      } catch (AWException | RuntimeException exc) {
        page.completeExceptionally(exc);
      } catch (Error error) {
        // Complete the shared page anyway, so the other threads of the request fail instead of waiting for ever
        page.completeExceptionally(error);
        throw error;
      }
    }
    return copy(Optional.ofNullable(shared).orElse(page));
  }

  /**
   * Pages computed in the request, by task: one map per request, shared by the threads of the request
   */
  @SuppressWarnings("unchecked")
  private static Map<String, CompletableFuture<ServiceData>> getRequestPages(RequestAttributes request) {
    synchronized (PAGES_LOCK) {
      Object pages = request.getAttribute(PAGES, RequestAttributes.SCOPE_REQUEST);
      if (pages == null) {
        pages = new ConcurrentHashMap<String, CompletableFuture<ServiceData>>();
        request.setAttribute(PAGES, pages, RequestAttributes.SCOPE_REQUEST);
      }
      return (Map<String, CompletableFuture<ServiceData>>) pages;
    }
  }

  /**
   * Copy of a computed page for one caller (the query of a card keeps only its fields)
   */
  private static ServiceData copy(CompletableFuture<ServiceData> page) throws AWException {
    try {
      return new ServiceData().setDataList(page.get().getDataList().copy());
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new AWException("Task page", exc.getMessage(), exc);
    } catch (ExecutionException exc) {
      if (exc.getCause() instanceof AWException error) {
        throw error;
      }
      if (exc.getCause() instanceof Error error) {
        throw error;
      }
      throw (RuntimeException) exc.getCause();
    }
  }

  /**
   * Compute what the task screen shows of a task
   */
  private ServiceData computePage(Integer taskId) throws AWException {
    DataList dataList = new DataList();
    Optional<TaskSummary> found = catalogPresenter.getSummary(taskId);
    if (found.isEmpty()) {
      return new ServiceData().setDataList(dataList);
    }
    TaskSummary task = found.get();
    Map<String, CellData> settings = getSettings(taskId);
    List<Map<String, CellData>> saved = getSavedExecutions(taskId);

    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(taskId));
    row.put(TASK_ID, new CellData(taskId));
    row.put("TskAct", new CellData(task.active() ? 1 : 0));
    row.put("TskVar", new CellData(hasParameters(taskId) ? 1 : 0));
    putHeader(row, task, settings);
    putSuccess(row, saved);
    putAverage(row, task, settings, saved);
    putNext(row, task);
    putHistory(row, saved);
    putLastExecution(row, taskId);
    putConfiguration(row, task, settings);
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Header: the name, the description, the last status as a chip and the facts of the task as pills (a JSON array of
   * {label, icon, code})
   */
  private void putHeader(Map<String, CellData> row, TaskSummary task, Map<String, CellData> settings)
    throws AWException {
    row.put("TskPagTit", new CellData(task.name()));
    row.put("TskPagSub", new CellData(Optional.ofNullable(task.description()).orElse("")));
    row.put("TskPagIcn", new CellData(task.when().icon()));
    if (task.lastStatus() == null) {
      row.put("TskPagChp", new CellData(text("SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS")));
      row.put("TskPagChpTon", new CellData(NEUTRAL_TONE));
    } else {
      String code = statusCode(task.lastStatus(), task.waiting());
      row.put("TskPagChp", new CellData(text(PAGE + "LAST_STATUS", statusLabel(code))));
      row.put("TskPagChpTon", new CellData(chipTone(queryService.findLabel("ActStaTon", code))));
    }
    // The facts, as the pills of the header: what the task runs with its name as code
    ArrayNode facts = JsonNodeFactory.instance.arrayNode();
    facts.add(pill("#" + task.id(), null, null));
    boolean maintain = isMaintain(settings);
    facts.add(pill(runType(maintain), maintain ? "lucide:database" : "lucide:terminal", task.command()));
    facts.add(pill(task.when().text(), task.when().icon(), null));
    Integer kept = integer(settings, "NumStoExe");
    if (kept != null && kept > 0) {
      facts.add(pill(text(PAGE + "KEEPS", String.valueOf(kept)), "lucide:archive", null));
    }
    Integer timeout = integer(settings, TIMEOUT);
    if (timeout != null && timeout > 0) {
      facts.add(pill(text(PAGE + "MAX_TIME", TaskWhenSummary.duration(timeout * 1000L)), "lucide:timer", null));
    }
    Optional<String> report = reportType(settings);
    if (report.isPresent()) {
      facts.add(pill(text(PAGE + "REPORT", report.get()), "lucide:mail", null));
    }
    row.put("TskPagFct", new CellData(facts.toString()));
  }

  /**
   * Pill of the header: its text, its icon and the code it shows in the monospace font (each one when it has it). The
   * flow run page (FlowRunPresenter) builds its facts with it too
   */
  static ObjectNode pill(String label, String icon, String code) {
    ObjectNode pill = JsonNodeFactory.instance.objectNode();
    pill.put(LABEL, label);
    if (icon != null) {
      pill.put("icon", icon);
    }
    if (code != null && !code.isBlank()) {
      pill.put("code", code);
    }
    return pill;
  }

  /**
   * Success rate over the saved executions that ended: the percent of OK ones, the count of each status and the same
   * counts as the segments of a stacked bar
   */
  private void putSuccess(Map<String, CellData> row, List<Map<String, CellData>> saved) throws AWException {
    List<Integer> ended = saved.stream().map(execution -> integer(execution, STATUS))
      .filter(TaskPagePresenter::ended).toList();
    if (ended.isEmpty()) {
      row.put("TskOkVal", new CellData(EMPTY));
      row.put("TskOkUni", new CellData(""));
      row.put("TskOkDes", new CellData(text(PAGE + "NO_SAVED_EXECUTIONS")));
      row.put("TskOkStk", new CellData(""));
      return;
    }
    long ok = ended.stream().filter(status -> status == TaskStatus.JOB_OK.getValue()).count();
    row.put("TskOkVal", new CellData(Math.round(ok * 100.0 / ended.size())));
    row.put("TskOkUni", new CellData("%"));
    row.put("TskOkDes", new CellData(counts(ended) + SEPARATOR + text(PAGE + "SAVED_EXECUTIONS",
      String.valueOf(ended.size()))));
    row.put("TskOkStk", new CellData(stack(ended).toString()));
  }

  /**
   * Segments of the stacked bar of the success rate: one per status of the executions, in the order of the counts,
   * with its tone (the one of the status in the activity screen), its label and its count
   */
  private ArrayNode stack(List<Integer> statuses) throws AWException {
    ArrayNode stack = JsonNodeFactory.instance.arrayNode();
    for (Integer status : STATUS_ORDER) {
      long count = statuses.stream().filter(status::equals).count();
      if (count > 0) {
        String code = String.valueOf(status);
        stack.addObject()
          .put("tone", chipTone(queryService.findLabel("ActStaTon", code)))
          .put(LABEL, statusLabel(code))
          .put("value", count);
      }
    }
    return stack;
  }

  /**
   * Average time of the OK executions, the maximum time allowed and the durations of the last saved executions that
   * ended (seconds, oldest first)
   */
  private void putAverage(Map<String, CellData> row, TaskSummary task, Map<String, CellData> settings,
                          List<Map<String, CellData>> saved) {
    row.put("TskAvgVal", new CellData(task.average() == null ? EMPTY : TaskWhenSummary.duration(task.average())));
    Integer timeout = integer(settings, TIMEOUT);
    row.put("TskAvgDes", new CellData(timeout != null && timeout > 0
      ? text(PAGE + "MAX_ALLOWED", TaskWhenSummary.duration(timeout * 1000L)) : text(PAGE + "NO_MAX_TIME")));
    List<Map<String, CellData>> last = saved.subList(0, Math.min(TaskCatalogPresenter.HISTORY_SIZE, saved.size()));
    ArrayNode durations = JsonNodeFactory.instance.arrayNode();
    last.reversed().stream()
      .filter(execution -> ended(integer(execution, STATUS)) && integer(execution, EXECUTION_TIME) != null)
      .forEach(execution -> durations.add(Math.round(integer(execution, EXECUTION_TIME) / 100.0) / 10.0));
    row.put("TskAvgSpk", new CellData(durations.size() > 1 ? durations.toString() : ""));
  }

  /**
   * Next execution: its relative date as the value when the scheduler fires the task, and in words what launches it
   * and the «When» summary
   */
  private void putNext(Map<String, CellData> row, TaskSummary task) {
    boolean fires = task.nextFire() != null;
    row.put("TskNxtVal", new CellData(fires ? task.next().text() : ""));
    List<String> detail = new ArrayList<>();
    // Without a fire time the next execution is already said in words (when its parent ends, on demand...): the «When»
    // text would repeat it, only its detail is added
    List<String> parts = fires ? Arrays.asList(task.next().detail(), task.when().text(), task.when().detail())
      : Arrays.asList(task.next().text(), task.next().detail(), task.when().detail());
    for (String part : parts) {
      if (part != null && !part.isBlank() && !EMPTY.equals(part) && !detail.contains(part)) {
        detail.add(part);
      }
    }
    row.put("TskNxtDes", new CellData(String.join(SEPARATOR, detail)));
  }

  /**
   * Strip of the last saved executions and the count of each status in it
   */
  private void putHistory(Map<String, CellData> row, List<Map<String, CellData>> saved) throws AWException {
    ArrayNode strip = catalogPresenter.getHistoryStrip(saved);
    row.put("TskHis", new CellData(strip.toString()));
    Map<String, Integer> byLabel = new LinkedHashMap<>();
    for (JsonNode item : strip) {
      byLabel.merge(item.get(LABEL).asString(), 1, Integer::sum);
    }
    List<String> counts = new ArrayList<>();
    byLabel.forEach((label, count) -> counts.add(count + " " + label));
    row.put("TskHisTxt", new CellData(counts.isEmpty() ? text(PAGE + "NO_SAVED_EXECUTIONS")
      : String.join(", ", counts)));
  }

  /**
   * Last execution of the task, as the activity screen shows it
   */
  private void putLastExecution(Map<String, CellData> row, Integer taskId) throws AWException {
    List<Map<String, CellData>> rows = executionPresenter.getExecutionList(new ExecutionListCriteria()
      .setTaskId(taskId).setMax(1)).getDataList().getRows();
    Map<String, CellData> last = rows.isEmpty() ? Map.of() : rows.get(0);
    row.put("TskLstHas", new CellData(last.isEmpty() ? 0 : 1));
    row.put("TskLstSta", new CellData(last.isEmpty() ? text("SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS")
      : text(value(last, "ExeStaTxt"))));
    row.put("TskLstTon", new CellData(last.isEmpty() ? NEUTRAL_TONE : value(last, "ExeStaTon")));
    row.put("TskLstIcn", new CellData(last.isEmpty() ? "lucide:circle-dashed" : value(last, "ExeStaIcn")));
    row.put("TskLstDes", new CellData(orEmpty(value(last, "ExeDes"))));
    row.put("TskLstExe", new CellData(orEmpty(value(last, "ExeTsk"))));
    row.put("TskLstRun", new CellData(orEmpty(value(last, "ExeRunLbl"))));
    String origin = value(last, "ExeOriTxt");
    row.put("TskLstOri", new CellData(origin == null ? EMPTY : text(origin)));
    row.put("TskLstBy", new CellData(orEmpty(value(last, TASK_LAUNCHED_BY_LIST))));
    CellData start = last.get("ExeLstTim");
    row.put("TskLstIni", new CellData(start == null || start.getObjectValue() == null ? EMPTY
      : DATE_TIME.format(LegacyDateConverter.asLocalDateTime(start.getDateValue()))));
    row.put("TskLstDur", new CellData(orEmpty(value(last, EXECUTION_TIME))));
  }

  /**
   * Read-only summary of the configuration: what it runs, when it is launched and after which tasks, what it launches
   * after it, what it keeps and its report
   */
  private void putConfiguration(Map<String, CellData> row, TaskSummary task, Map<String, CellData> settings)
    throws AWException {
    row.put("TskCfgRun", new CellData(runs(task, settings)));
    List<String> where = new ArrayList<>();
    Optional.ofNullable(text(settings, "SrvExeNam")).ifPresent(server -> where.add(text(PAGE + "ON_SERVER", server)));
    Optional.ofNullable(text(settings, "CmdExePth")).ifPresent(where::add);
    row.put("TskCfgWhr", new CellData(where.isEmpty() ? EMPTY : String.join(SEPARATOR, where)));
    row.put("TskCfgWhn", new CellData(join(task.when().text(), task.when().detail())));
    List<TaskLaunch.Parent> parents = Optional.ofNullable(task.launch().getParents()).orElse(List.of());
    row.put("TskCfgPrn", new CellData(parents.isEmpty() ? EMPTY
      : String.join(", ", parents.stream().map(TaskLaunch.Parent::name).toList())));
    row.put("TskCfgChl", new CellData(text(WHEN + condition(flag(settings, "LchDepWrn"),
      flag(settings, "LchDepErr")))));
    row.put("TskCfgSto", new CellData(orEmpty(text(settings, "NumStoExe"))));
    Integer timeout = integer(settings, TIMEOUT);
    row.put("TskCfgTim", new CellData(timeout == null || timeout <= 0 ? EMPTY
      : TaskWhenSummary.duration(timeout * 1000L)));
    List<String> report = new ArrayList<>();
    reportType(settings).ifPresent(report::add);
    Optional.ofNullable(text(settings, "RepEmaDst")).ifPresent(report::add);
    Optional.ofNullable(text(settings, "RepMntId")).ifPresent(report::add);
    row.put("TskCfgRpt", new CellData(report.isEmpty() ? EMPTY : String.join(SEPARATOR, report)));
  }

  /**
   * What the task runs: the maintain or the command
   */
  private String runs(TaskSummary task, Map<String, CellData> settings) {
    String type = runType(isMaintain(settings));
    return task.command() == null ? type : type + SEPARATOR + task.command();
  }

  /**
   * What kind of thing the task runs: a maintain or a command
   */
  private String runType(boolean maintain) {
    return text(maintain ? "ENUM_EXECUTE_MAINTAIN" : "ENUM_EXECUTE_COMMAND");
  }

  private static boolean isMaintain(Map<String, CellData> settings) {
    return Integer.valueOf(MAINTAIN).equals(integer(settings, "TypExe"));
  }

  /**
   * Report of the task, empty without one
   */
  private Optional<String> reportType(Map<String, CellData> settings) throws AWException {
    Integer report = integer(settings, "RepTyp");
    if (report == null || report == 0) {
      return Optional.empty();
    }
    return Optional.of(text(queryService.findLabel("RepTyp", String.valueOf(report))));
  }

  /**
   * Condition of the task on the ones it launches after it (the same words as the «When» of a dependent task)
   */
  private static String condition(boolean onWarning, boolean onError) {
    if (onWarning && onError) {
      return "AFTER_ANY";
    } else if (onWarning) {
      return "AFTER_IF_OK_WARNING";
    } else if (onError) {
      return "AFTER_IF_OK_ERROR";
    }
    return "AFTER_IF_OK";
  }

  /**
   * The execution ended: it has a status and it is not running or queued
   */
  private static boolean ended(Integer status) {
    return status != null && !NOT_ENDED.contains(status);
  }

  /**
   * Count of each status of the executions, in the order of the statuses: «17 OK, 2 Warning, 1 Error»
   */
  private String counts(List<Integer> statuses) throws AWException {
    List<String> counts = new ArrayList<>();
    for (Integer status : STATUS_ORDER) {
      long count = statuses.stream().filter(status::equals).count();
      if (count > 0) {
        counts.add(count + " " + statusLabel(String.valueOf(status)));
      }
    }
    return String.join(", ", counts);
  }

  private String statusLabel(String code) throws AWException {
    return text(queryService.findLabel("ActStaTyp", code));
  }

  /**
   * Tone of the chip of the header: the tones of a chip (a pending execution is shown as information)
   */
  private static String chipTone(String tone) {
    return "pending".equals(tone) ? "info" : Optional.ofNullable(tone).orElse(NEUTRAL_TONE);
  }

  private static String statusCode(Integer status, boolean waiting) {
    return waiting && status == TaskStatus.JOB_RUNNING.getValue() ? "deferred" : String.valueOf(status);
  }

  /**
   * Settings of the task the catalog does not read: what it runs, what it keeps, its maximum time, its dependencies and
   * its report; where it runs and who gets its report only when the user holds the permission of their query
   */
  private Map<String, CellData> getSettings(Integer taskId) throws AWException {
    Map<String, CellData> settings = new HashMap<>(firstRow(SCHEDULER_TASK_PAGE_TASK, taskId));
    if (!settings.isEmpty() && canLaunchQuery.test(SCHEDULER_TASK_PAGE_TASK_RESTRICTED)) {
      settings.putAll(firstRow(SCHEDULER_TASK_PAGE_TASK_RESTRICTED, taskId));
    }
    return settings;
  }

  private Map<String, CellData> firstRow(String query, Integer taskId) throws AWException {
    List<Map<String, CellData>> rows = queryService.launchPrivateQuery(query, getParameters(taskId)).getDataList()
      .getRows();
    return rows.isEmpty() ? Map.of() : rows.get(0);
  }

  /**
   * Saved executions of the task, newest first
   */
  private List<Map<String, CellData>> getSavedExecutions(Integer taskId) throws AWException {
    return queryService.launchPrivateQuery(SCHEDULER_TASK_PAGE_EXECUTIONS, getParameters(taskId)).getDataList()
      .getRows();
  }

  /**
   * The task has parameters: its launch asks for their values
   */
  private boolean hasParameters(Integer taskId) throws AWException {
    ObjectNode parameters = getParameters(taskId);
    parameters.putArray("catalogTaskIds").add(taskId);
    return !queryService.launchPrivateQuery(SCHEDULER_TASKS_WITH_PARAMETERS, parameters).getDataList().getRows()
      .isEmpty();
  }

  /**
   * Parameters of the queries of the screen: every row, in the order of the query (the sort of the request names the
   * columns of a grid, not the fields of the queries)
   */
  private ObjectNode getParameters(Integer taskId) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    parameters.put("taskPageTaskId", taskId);
    return parameters;
  }

  /**
   * Text of a locale with its parameters. The parameters are not escaped: the screen shows the texts as text
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }

  private static String join(String text, String detail) {
    return detail == null || detail.isBlank() ? text : text + SEPARATOR + detail;
  }

  private static String orEmpty(String value) {
    return value == null || value.isBlank() ? EMPTY : value;
  }

  private static String value(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    if (cell == null || cell.getObjectValue() == null) {
      return null;
    }
    String value = cell.getStringValue();
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String text(Map<String, CellData> row, String field) {
    return value(row, field);
  }

  private static Integer integer(Map<String, CellData> row, String field) {
    String value = value(row, field);
    if (value == null) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException exc) {
      CellData cell = row.get(field);
      return cell.getObjectValue() instanceof Number number ? number.intValue() : null;
    }
  }

  private static boolean flag(Map<String, CellData> row, String field) {
    return Integer.valueOf(1).equals(integer(row, field));
  }
}
