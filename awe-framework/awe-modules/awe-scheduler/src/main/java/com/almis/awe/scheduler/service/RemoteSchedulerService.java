package com.almis.awe.scheduler.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskListCriteria;
import com.almis.awe.scheduler.bean.task.TaskVariable;
import com.almis.awe.scheduler.dao.task.FlowRunGraphRules;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import com.almis.awe.scheduler.util.TaskSchedulePreset;
import com.almis.awe.service.MaintainService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.scheduler.constant.CronConstants.*;

/**
 * @author pgarcia
 */
@Slf4j
public class RemoteSchedulerService extends ServiceConfig {

  // Locales
  private final SchedulerService schedulerService;
  private final RemoteScheduler remoteScheduler;
  private final ObjectMapper mapper;
  private final boolean remote;
  /**
   * Task editor: the runs of its preview, its preset and its launch type
   */
  private static final int EDITOR_RUNS = 5;
  private static final String EDITOR_PRESET = "SchPre";
  private static final String EDITOR_DAYS = "SchWkd";
  private static final String EDITOR_DAYS_PRESET = "SchWkdPre";
  private static final String EDITOR_LAUNCH_TYPE = "TypLch";
  private static final String EDITOR_WHEN = "TskWhn";
  private static final String EDITOR_SCHEDULE_MAINTAIN = "updateTaskEditorSchedule";

  /**
   * Constructor
   */
  public RemoteSchedulerService(SchedulerService schedulerService, RemoteScheduler remoteScheduler, ObjectMapper mapper,
                                boolean remote) {
    this.schedulerService = schedulerService;
    this.remoteScheduler = remoteScheduler;
    this.mapper = mapper;
    this.remote = remote;
  }

  /**
   * Retrieve the task list from service
   * Requires QUARTZ
   *
   * @return ServiceData
   * @throws AWException Error retrieving task list
   */
  public ServiceData getTaskList(TaskListCriteria taskListCriteria) throws AWException {
    return remote ? remoteScheduler.getTaskList(taskListCriteria) : schedulerService.getTaskList(taskListCriteria);
  }

  /**
   * Compute next fire times
   * Requires QUARTZ
   *
   * @param times # of fire times to compute
   * @return Service data
   * @throws AWException Error computing next fire times
   */
  public ServiceData computeNextFireTimes(Integer times) throws AWException {
    return remote ? remoteScheduler.getNextFireTimes(times, getSchedule()) : schedulerService.computeNextFireTimes(times, getSchedule());
  }

  /**
   * Live preview of the «When» of a scheduled task in the task editor (#993): the stored fields of the screen with the
   * preset of its «Repetition» applied, its next five runs (computed as the next fire times, in the remote scheduler
   * when there is one) and the repetition in a sentence. A repetition whose runs cannot be computed yet (a field to
   * fill) is an incomplete preview; a repetition whose end has passed is a complete one without runs (its trigger
   * cannot be built), so the preview tells it never runs again
   *
   * @return Title, description and pills of the preview card
   * @throws AWException The preview cannot be built
   */
  public ServiceData getTaskEditorSchedule() throws AWException {
    TaskSchedulePreset.Values values = getEditorPreset();
    Schedule stored = getSchedule();
    Schedule schedule = TaskSchedulePreset.apply(values, stored);
    List<Map<String, CellData>> next = List.of();
    boolean complete = TaskSchedulePreset.isComplete(values, stored);
    Date end = schedule.getEndDateTime();
    // An ended repetition has no next runs (the preview says why, TaskSchedulePreview)
    boolean ended = end != null && end.before(new Date());
    if (!complete) {
      log.debug("[SCHEDULER] The fields of the preset {} of the task editor are not filled", values.getPreset());
    } else if (!ended) {
      try {
        ServiceData runs = remote ? remoteScheduler.getNextFireTimes(EDITOR_RUNS, schedule)
          : schedulerService.computeNextFireTimes(EDITOR_RUNS, schedule);
        next = runs.getDataList() == null ? List.of() : runs.getDataList().getRows();
      } catch (Exception exc) {
        log.debug("[SCHEDULER] The repetition of the task editor is not complete yet", exc);
        complete = false;
      }
    }
    return schedulerService.getTaskEditorSchedule(schedule, Optional.ofNullable(values.getPreset())
      .orElse(TaskSchedulePreset.CUSTOM), next, complete);
  }

  /**
   * Save of a scheduled task in the task editor, after its launch row is written and before the commit: a preset of its
   * «Repetition» that is not the custom one stores the fields of the preset (repeat type and number, the lists, the
   * date and the time) in that row instead of the ones the screen sends (updateTaskEditorSchedule, in the same
   * transaction). A custom preset, a request without preset or another launch type keep the row as the screen sent it
   *
   * @param taskId Task saved
   * @return Empty service data
   * @throws AWException The launch row cannot be written
   */
  public ServiceData applyTaskEditorSchedule(Integer taskId) throws AWException {
    if (taskId != null && isEditorPreset(EDITOR_LAUNCH_TYPE)) {
      ObjectNode parameters = getBean(QueryUtil.class).getParameters();
      parameters.put("taskId", taskId);
      storedFields(getEditorSchedule()).forEach(parameters::set);
      getBean(MaintainService.class).launchPrivateMaintain(EDITOR_SCHEDULE_MAINTAIN, parameters);
    }
    return new ServiceData();
  }

  /**
   * Stored fields of the preset of the task editor, selected in their criteria (the custom preset shows them), so a
   * change of preset becomes the same repetition in «Custom». The day toggles follow the presets with days
   * (TaskSchedulePreset#syncDays): a daily or workdays preset just chosen selects its days, a change of those days
   * selects the weekly preset. Nothing for a custom preset or another launch type
   *
   * @return Select actions of the stored criteria (and of the preset and the days when they change)
   */
  public ServiceData getTaskEditorScheduleFields() {
    ServiceData serviceData = new ServiceData();
    // The «When» of the screen: the launch type follows it, but may not have changed yet when its change is sent
    if (isEditorPreset(EDITOR_WHEN)) {
      TaskSchedulePreset.Values values = getEditorPreset();
      TaskSchedulePreset.syncDays(values.getPreset(), readText(getRequest().getParameter(EDITOR_DAYS_PRESET)),
        values.getWeekDays()).ifPresent(sync -> {
        select(serviceData, EDITOR_DAYS_PRESET, List.of(sync.daysPreset()));
        select(serviceData, EDITOR_DAYS, sync.weekDays());
        if (!sync.preset().equals(values.getPreset())) {
          select(serviceData, EDITOR_PRESET, List.of(sync.preset()));
        }
        values.setPreset(sync.preset()).setWeekDays(sync.weekDays());
      });
      storedFields(TaskSchedulePreset.apply(values, getSchedule())).forEach((name, value) -> {
        List<String> selected = new ArrayList<>();
        if (value.isArray()) {
          value.forEach(item -> selected.add(item.asString()));
        } else if (!value.isNull()) {
          selected.add(value.asString());
        }
        select(serviceData, name, selected);
      });
    }
    return serviceData;
  }

  /**
   * Preset the stored fields of the task editor match (the custom repetition offers to go back to it), empty when none
   * does
   *
   * @return One row: the preset as its value
   */
  public ServiceData getTaskEditorScheduleMatch() {
    String preset = TaskSchedulePreset.detect(getSchedule()).getPreset();
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "value", TaskSchedulePreset.CUSTOM.equals(preset) ? "" : preset);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Back from the custom repetition to the preset its stored fields match: the preset and its fields selected in their
   * criteria. Nothing when no preset matches
   *
   * @return Select actions of the preset and its fields
   */
  public ServiceData getTaskEditorScheduleMatchFields() {
    ServiceData serviceData = new ServiceData();
    TaskSchedulePreset.Values values = TaskSchedulePreset.detect(getSchedule());
    if (!TaskSchedulePreset.CUSTOM.equals(values.getPreset())) {
      List<String> presetDays = TaskSchedulePreset.presetDays(values.getPreset());
      List<String> weekDays = presetDays.isEmpty() ? values.getWeekDays() : presetDays;
      select(serviceData, EDITOR_DAYS_PRESET, List.of(values.getPreset()));
      select(serviceData, EDITOR_DAYS, weekDays);
      select(serviceData, "SchEvy", values.getEvery() == null ? List.of() : List.of(String.valueOf(values.getEvery())));
      select(serviceData, "SchTim", values.getTime() == null ? List.of() : List.of(values.getTime()));
      select(serviceData, "SchMdy", values.getMonthDay() == null ? List.of() : List.of(String.valueOf(values.getMonthDay())));
      select(serviceData, EDITOR_PRESET, List.of(values.getPreset()));
    }
    return serviceData;
  }

  /**
   * Select some values in a criterion of the screen, silently
   */
  private static void select(ServiceData serviceData, String criterion, List<String> selected) {
    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    selected.forEach(values::add);
    serviceData.addClientAction(new ClientAction("select").setTarget(criterion).setSilent(true)
      .addParameter("values", new CellData(values)));
  }

  /**
   * The editor holds a scheduled task with a preset that is not the custom one
   *
   * @param launchTypeCriterion Criterion of the launch type: the saved one (TypLch) or the «When» of the screen (TskWhn)
   */
  private boolean isEditorPreset(String launchTypeCriterion) {
    String preset = readText(getRequest().getParameter(EDITOR_PRESET));
    Integer launchType = readInteger(getRequest().getParameter(launchTypeCriterion));
    return launchType != null && launchType == 1 && preset != null && !TaskSchedulePreset.CUSTOM.equals(preset);
  }

  /**
   * Stored fields of the screen with the preset of the editor applied
   */
  private Schedule getEditorSchedule() {
    return TaskSchedulePreset.apply(getEditorPreset(), getSchedule());
  }

  /**
   * Preset of the «Repetition» of the editor and its fields, as the screen sends them
   */
  private TaskSchedulePreset.Values getEditorPreset() {
    return new TaskSchedulePreset.Values()
      .setPreset(readText(getRequest().getParameter(EDITOR_PRESET)))
      .setEvery(readInteger(getRequest().getParameter("SchEvy")))
      .setTime(readText(getRequest().getParameter("SchTim")))
      .setWeekDays(readList(getRequest().getParameter(EDITOR_DAYS)))
      .setMonthDay(readInteger(getRequest().getParameter("SchMdy")));
  }

  /**
   * Request parameters of the stored fields of a repetition, named as the criteria of the screen
   */
  private Map<String, JsonNode> storedFields(Schedule schedule) {
    JsonNodeFactory factory = JsonNodeFactory.instance;
    Map<String, JsonNode> fields = new LinkedHashMap<>();
    fields.put(CRON_PARAMETER_REPEAT_TYPE, schedule.getRepeatType() == null ? factory.nullNode()
      : factory.numberNode(schedule.getRepeatType()));
    fields.put(CRON_PARAMETER_REPEAT_NUMBER, schedule.getRepeatNumber() == null ? factory.nullNode()
      : factory.numberNode(schedule.getRepeatNumber()));
    fields.put(CRON_PARAMETER_YEARS, array(schedule.getYearList()));
    fields.put(CRON_PARAMETER_MONTHS, array(schedule.getMonthList()));
    fields.put(CRON_PARAMETER_WEEKS, array(schedule.getWeekList()));
    fields.put(CRON_PARAMETER_WEEKDAYS, array(schedule.getWeekDayList()));
    fields.put(CRON_PARAMETER_DAYS, array(schedule.getDayList()));
    fields.put(CRON_PARAMETER_HOURS, array(schedule.getHourList()));
    fields.put(CRON_PARAMETER_MINUTES, array(schedule.getMinuteList()));
    fields.put(CRON_PARAMETER_SECONDS, array(schedule.getSecondList()));
    fields.put(CRON_PARAMETER_DATE, schedule.getDate() == null ? factory.nullNode()
      : factory.textNode(DateUtil.dat2WebDate(schedule.getDate())));
    fields.put(CRON_PARAMETER_TIME, schedule.getTime() == null ? factory.nullNode()
      : factory.textNode(schedule.getTime()));
    return fields;
  }

  private static ArrayNode array(List<String> values) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    if (values != null) {
      values.forEach(array::add);
    }
    return array;
  }

  /**
   * Text of a criterion, null when it is not sent or empty
   */
  private static String readText(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    String text = node.isArray() ? (node.isEmpty() ? "" : node.get(0).asString("")) : node.asString("");
    return text.isBlank() ? null : text.trim();
  }

  /**
   * Number of a criterion, null when it is not sent, empty or not a number
   */
  private static Integer readInteger(JsonNode node) {
    String text = readText(node);
    try {
      return text == null ? null : Integer.valueOf(text);
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  /**
   * Values of a criterion with several values (an array or one text), without empty ones
   */
  private static List<String> readList(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (node == null || node.isNull()) {
      return values;
    }
    if (node.isArray()) {
      node.forEach(item -> values.add(item.asString("")));
    } else {
      values.add(node.asString(""));
    }
    return new ArrayList<>(values.stream().filter(value -> !value.isBlank()).toList());
  }

  /**
   * Retrieve the task progress status
   * Requires LOG FILES
   *
   * @return ServiceData
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    return remote ? remoteScheduler.getTaskExecutionList(taskId) : schedulerService.getTaskExecutionList(taskId);
  }

  /**
   * Retrieve a page of the executions of all tasks (activity screen)
   *
   * @param criteria Filters and page
   * @return Execution list page
   * @throws AWException Error retrieving the executions
   */
  public ServiceData getExecutionList(ExecutionListCriteria criteria) throws AWException {
    if (remote) {
      // An invalid filter is refused here with the AWE validation error, before it reaches the remote scheduler
      schedulerService.validateExecutionListCriteria(criteria);
      return remoteScheduler.getExecutionList(criteria);
    }
    return schedulerService.getExecutionList(criteria);
  }

  /**
   * Retrieve a page of the executions of all tasks grouped by flow run (activity screen): the flow runs and the
   * executions without run are the top-level rows of the page, the executions of each run its children
   *
   * @param criteria Filters and page of the top-level rows
   * @return Page of runs with their executions
   * @throws AWException A value is not valid or the executions cannot be read
   */
  public ServiceData getRunList(ExecutionListCriteria criteria) throws AWException {
    if (remote) {
      // An invalid filter is refused here with the AWE validation error, before it reaches the remote scheduler
      schedulerService.validateExecutionListCriteria(criteria);
      return remoteScheduler.getRunList(criteria);
    }
    return schedulerService.getRunList(criteria);
  }

  /**
   * Retrieve the options of the state quick filter of the activity screen with their counts
   *
   * @param criteria Filters of the execution list (search, origin and start)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid or the executions cannot be counted
   */
  public ServiceData getExecutionStateCounts(ExecutionListCriteria criteria) throws AWException {
    if (remote) {
      // An invalid filter is refused here with the AWE validation error, before it reaches the remote scheduler
      schedulerService.validateExecutionListCriteria(criteria);
      return remoteScheduler.getExecutionStateCounts(criteria);
    }
    return schedulerService.getExecutionStateCounts(criteria);
  }

  /**
   * Retrieve a page of the task catalog (tasks screen)
   * Requires QUARTZ
   *
   * @param criteria Filters, sort and page
   * @return Page of tasks
   * @throws AWException A value is not valid or the tasks cannot be read
   */
  public ServiceData getTaskCatalog(TaskCatalogCriteria criteria) throws AWException {
    if (remote) {
      // An invalid filter is refused here with the AWE validation error, before it reaches the remote scheduler
      schedulerService.validateTaskCatalogCriteria(criteria);
      return remoteScheduler.getTaskCatalog(criteria);
    }
    return schedulerService.getTaskCatalog(criteria);
  }

  /**
   * Retrieve the options of the active filter of the task catalog with their counts
   *
   * @param criteria Filters of the catalog (search, launch and last status)
   * @return One row per option: value, label, count and tone
   * @throws AWException A value is not valid or the tasks cannot be read
   */
  public ServiceData getTaskCatalogActiveCounts(TaskCatalogCriteria criteria) throws AWException {
    if (remote) {
      schedulerService.validateTaskCatalogCriteria(criteria);
      return remoteScheduler.getTaskCatalogActiveCounts(criteria);
    }
    return schedulerService.getTaskCatalogActiveCounts(criteria);
  }

  /**
   * Tabs of the configuration screen: the servers and the calendars with how many there are, and the engine with its
   * state, read where the engine runs. An engine that cannot be read leaves its tab with only its name (logged as a
   * warning): the screen still opens
   *
   * @return One row per tab: value, label and icon
   * @throws AWException The servers or the calendars cannot be read
   */
  public ServiceData getConfigurationTabs() throws AWException {
    String status = null;
    try {
      ServiceData engine = getConfigurationEngine();
      if (engine != null && engine.getDataList() != null && !engine.getDataList().getRows().isEmpty()) {
        CellData cell = engine.getDataList().getRows().get(0).get("EngStaTxt");
        status = cell == null ? null : cell.getStringValue();
      }
    } catch (AWException | RuntimeException exc) {
      log.warn("[SCHEDULER] The engine could not be read for the tabs of the configuration screen", exc);
    }
    return schedulerService.getConfigurationTabs(status);
  }

  /**
   * «Motor» tab of the configuration screen: the card of the engine. A remote engine is told as such: it does not run
   * on this server (its facts are told again with it). A remote scheduler that cannot be reached leaves the card of an
   * engine that cannot be read (logged as a warning)
   * Requires QUARTZ
   *
   * @return One row
   * @throws AWException The engine card cannot be built
   */
  public ServiceData getConfigurationEngine() throws AWException {
    if (!remote) {
      return schedulerService.getConfigurationEngine();
    }
    ServiceData engine;
    try {
      engine = remoteScheduler.getConfigurationEngine();
    } catch (RuntimeException exc) {
      log.warn("[SCHEDULER] The remote engine could not be read for the configuration screen", exc);
      return schedulerService.getConfigurationEngineUnavailable();
    }
    if (engine != null && engine.getDataList() != null) {
      engine.getDataList().getRows().forEach(row -> {
        row.put("EngInsTxt", new CellData(getLocale("SCREEN_TEXT_SCHEDULER_ENGINE_INSTANCE_REMOTE")));
        row.put("EngTil", new CellData(SchedulerEnginePresenter.tiles(row, this::getLocale)));
      });
    }
    return engine;
  }

  /**
   * «Motor» tab of the configuration screen: the schedules of the engine, filtered by their state
   * Requires QUARTZ
   *
   * @param filter paused or error; every schedule when blank
   * @return One row per schedule; none when the engine cannot be read (a remote scheduler that cannot be reached is
   * logged as a warning)
   * @throws AWException The filter is not valid
   */
  public ServiceData getConfigurationEngineTriggers(String filter) throws AWException {
    if (remote) {
      // An invalid filter is refused here with the AWE validation error, before it reaches the remote scheduler
      schedulerService.validateConfigurationEngineFilter(filter);
      try {
        return remoteScheduler.getConfigurationEngineTriggers(filter);
      } catch (RuntimeException exc) {
        log.warn("[SCHEDULER] The schedules of the remote engine could not be read for the configuration screen", exc);
        DataList empty = new DataList();
        empty.setRecords(0);
        return new ServiceData().setDataList(empty);
      }
    }
    return schedulerService.getConfigurationEngineTriggers(filter);
  }

  /**
   * «Motor» tab of the configuration screen: the options of the state filter of the schedules with their counts
   * Requires QUARTZ
   *
   * @return One row per option; zero counts when the engine cannot be read (a remote scheduler that cannot be reached is
   * logged as a warning)
   */
  public ServiceData getConfigurationEngineTriggerCounts() {
    if (!remote) {
      return schedulerService.getConfigurationEngineTriggerCounts();
    }
    try {
      return remoteScheduler.getConfigurationEngineTriggerCounts();
    } catch (RuntimeException exc) {
      log.warn("[SCHEDULER] The schedules of the remote engine could not be counted for the configuration screen", exc);
      return schedulerService.getConfigurationEngineEmptyTriggerCounts();
    }
  }

  /**
   * Retrieve the summary of a flow run (flow run page), from the remote scheduler when it is remote
   *
   * @param runId Flow run identifier
   * @return One row, none without run or with an id that is not the shape of a run id (a UUID)
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunSummary(String runId) throws AWException {
    if (!FlowRunGraphRules.isRunId(runId)) {
      return new ServiceData().setDataList(new DataList());
    }
    return remote ? remoteScheduler.getFlowRunSummary(runId) : schedulerService.getFlowRunSummary(runId);
  }

  /**
   * Retrieve the graph of a flow run (flow run page), from the remote scheduler when it is remote
   *
   * @param runId Flow run identifier
   * @return Rows of the graph, none without run or with an id that is not the shape of a run id (a UUID)
   * @throws AWException The run cannot be read
   */
  public ServiceData getFlowRunGraph(String runId) throws AWException {
    if (!FlowRunGraphRules.isRunId(runId)) {
      return new ServiceData().setDataList(new DataList());
    }
    return remote ? remoteScheduler.getFlowRunGraph(runId) : schedulerService.getFlowRunGraph(runId);
  }

  /**
   * Retrieve the counters of the activity screen
   * Requires QUARTZ
   *
   * @return One row with the counters
   * @throws AWException Error retrieving the counters
   */
  public ServiceData getActivityCounters() throws AWException {
    return remote ? remoteScheduler.getActivityCounters() : schedulerService.getActivityCounters();
  }

  /**
   * Pause the selected task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error pausing task
   */
  public ServiceData pauseTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.pauseTask(taskId) : schedulerService.pauseTask(taskId);
  }

  /**
   * Stop the deferred executions of the selected task
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error stopping the executions
   */
  public ServiceData stopTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.stopTask(taskId) : schedulerService.stopTask(taskId);
  }

  /**
   * Resume the selected task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error resuming task
   */
  public ServiceData resumeTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.resumeTask(taskId) : schedulerService.resumeTask(taskId);
  }

  /**
   * Execute the selected task now
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @param user   Launch user
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user) throws AWException {
    // Backward-compatible signature bound by the AWE JavaConnector from Services.xml (LchTsk manual launch, 2 params).
    // Do not remove: the connector resolves the target method by EXACT parameter types.
    return executeTaskNow(taskId, user, Collections.emptyMap());
  }

  /**
   * Execute the selected task now, applying the operator supplied values for its variable parameters
   * Requires QUARTZ
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values for variable parameters
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user, Map<String, String> variables) throws AWException {
    Map<String, String> operatorValues = variables == null ? Collections.emptyMap() : variables;
    return remote ? remoteScheduler.executeTaskNow(taskId, user, operatorValues) : schedulerService.executeTaskNow(taskId, user, operatorValues);
  }

  /**
   * Execute the selected task now, applying the operator supplied values collected from the manual
   * launch modal grid.
   * Requires QUARTZ
   *
   * <p>Adapter overload bound by the AWE JavaConnector for the "executeTaskNowVariables" service
   * (Services.xml). A {@code bean-class} + {@code list="true"} service-parameter is bound as
   * {@code List.class}, so this exact (Integer, String, List) signature must exist for the
   * connector to resolve the method by exact parameter types.
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied variable rows (name/value) from the modal grid
   * @return ServiceData
   * @throws AWException Error executing task
   */
  public ServiceData executeTaskNow(Integer taskId, String user, List<TaskVariable> variables) throws AWException {
    return executeTaskNow(taskId, user, toVariableMap(variables));
  }

  /**
   * Build an ordered name-to-value map from the operator supplied variable rows.
   * Rows with a null or blank name are skipped, the last value wins on a duplicate name,
   * and a null or empty list yields an empty map.
   *
   * @param variables Variable rows collected from the modal grid
   * @return Ordered map of variable parameter name to operator supplied value
   */
  static Map<String, String> toVariableMap(List<TaskVariable> variables) {
    Map<String, String> values = new LinkedHashMap<>();
    if (variables != null) {
      for (TaskVariable variable : variables) {
        if (variable != null && variable.getName() != null && !variable.getName().isBlank()) {
          values.put(variable.getName(), variable.getValue());
        }
      }
    }
    return values;
  }


  /**
   * Validate the task parameters before launching, on the instance that resolves them.
   *
   * <p>Bound by the AWE JavaConnector for the "validateTaskParameters" service. A property that
   * does not resolve raises an AWException, which aborts the action chain and is rendered by AWE.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied variable rows (name/value) from the modal grid
   * @return ServiceData
   * @throws AWException A property key does not resolve, or the task could not be loaded
   */
  public ServiceData validateTaskParameters(Integer taskId, List<TaskVariable> variables) throws AWException {
    Map<String, String> operatorValues = toVariableMap(variables);
    ServiceData result = remote
      ? remoteScheduler.validateTaskParameters(taskId, operatorValues)
      : schedulerService.validateTaskParameters(taskId, operatorValues);

    if (AnswerType.ERROR.equals(result.getType())) {
      throw new AWException(result.getTitle(), result.getMessage());
    }
    return result;
  }

  /**
   * Insert and schedule a new task
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error inserting task
   */
  public ServiceData insertSchedulerTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.scheduleTask(taskId) : schedulerService.insertSchedulerTask(taskId);
  }

  /**
   * Update and reschedule a task
   * Requires QUARTZ
   *
   * @param taskId Task identifier list
   * @return ServiceData
   * @throws AWException Error updating task
   */
  public ServiceData updateSchedulerTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.rescheduleTask(taskId) : schedulerService.updateSchedulerTask(taskId);
  }

  /**
   * Reject the dependencies a task is saved with when they close a cycle. It reads the stored dependencies, so it
   * runs on this instance whether the scheduler is remote or not.
   *
   * @param taskId   Task identifier
   * @param children Tasks launched when the task ends
   * @throws AWException The dependencies close a cycle
   */
  public void validateTaskDependencies(Integer taskId, List<?> children) throws AWException {
    schedulerService.validateTaskDependencies(taskId, children);
  }

  /**
   * Refresh a task saved from the task editor in the scheduler with its stored launch, without purging its
   * executions, their logs nor the files it has seen
   * Requires QUARTZ
   *
   * @param taskId Task identifier
   * @return ServiceData
   * @throws AWException Error refreshing the task
   */
  public ServiceData refreshSchedulerTask(Integer taskId) throws AWException {
    return remote ? remoteScheduler.refreshTask(taskId) : schedulerService.refreshSchedulerTask(taskId);
  }

  /**
   * Delete a task from scheduler
   * Requires QUARTZ
   *
   * @param ideTsk Task identifier list
   * @return ServiceData
   * @throws AWException Error deleting task
   */
  public ServiceData deleteSchedulerTask(List<Integer> ideTsk) throws AWException {
    return remote ? remoteScheduler.unscheduleTask(ideTsk) : schedulerService.deleteSchedulerTask(ideTsk);
  }

  /**
   * Purge execution logs
   * Requires LOG FILES
   *
   * @param taskId     Task identifier
   * @param executions Number of executions to purge
   * @return Service data
   * @throws AWException Error purging execution logs
   */
  public ServiceData purgeExecutionLogs(Integer taskId, Integer executions) throws AWException {
    return remote ? remoteScheduler.purgeExecutions(taskId, executions) : schedulerService.purgeExecutionLogs(taskId, executions);
  }

  /**
   * Get currently executing jobs from the scheduler instance
   * Requires QUARTZ
   *
   * @return Service data
   * @throws AWException Error retrieving currently executing jobs
   */
  public ServiceData currentlyExecutingJobs() throws AWException {
    return remote ? remoteScheduler.getExecutingJobs() : schedulerService.currentlyExecutingJobs();
  }

  /**
   * Get configured
   * Requires QUARTZ
   *
   * @return Configured jobs
   * @throws AWException Error retrieving configured jobs
   */
  public ServiceData getConfiguredJobs() throws AWException {
    return remote ? remoteScheduler.getConfiguredJobs() : schedulerService.getConfiguredJobs();
  }


  /**
   * Insert and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarIde Calendar identifier
   * @return ServiceData
   * @throws AWException Error inserting scheduler calendar
   */
  public ServiceData insertSchedulerCalendar(Integer calendarIde) throws AWException {
    return remote ? remoteScheduler.insertCalendar(calendarIde) : schedulerService.insertSchedulerCalendar(calendarIde);
  }

  /**
   * Update and schedule a new calendar
   * Requires QUARTZ
   *
   * @param calendarId Calendar identifier
   * @return ServiceData
   * @throws AWException Error updating scheduler calendar
   */
  public ServiceData updateSchedulerCalendar(Integer calendarId) throws AWException {
    return remote ? remoteScheduler.updateCalendar(calendarId) : schedulerService.updateSchedulerCalendar(calendarId);
  }

  /**
   * Delete selected calendars
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deleting scheduler calendars
   */
  public ServiceData deleteSchedulerCalendar(List<Integer> calendarIdList) throws AWException {
    return remote ? remoteScheduler.deleteCalendars(calendarIdList) : schedulerService.deleteSchedulerCalendar(calendarIdList);
  }

  /**
   * Check if the scheduler contains the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error checking calendars existence
   */
  public ServiceData checkCalendarExist(List<Integer> calendarIdList) throws AWException {
    return remote ? remoteScheduler.checkCalendarInTriggers(calendarIdList) : schedulerService.checkCalendarExist(calendarIdList);
  }

  /**
   * Resume the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error activating calendars
   */
  public ServiceData activateCalendar(List<Integer> calendarIdList) throws AWException {
    return remote ? remoteScheduler.activateCalendar(calendarIdList) : schedulerService.activateCalendar(calendarIdList);
  }

  /**
   * Pause the selected calendar
   * Requires QUARTZ
   *
   * @param calendarIdList Calendar identifier list
   * @return ServiceData
   * @throws AWException Error deactivating calendars
   */
  public ServiceData deactivateCalendar(List<Integer> calendarIdList) throws AWException {
    return remote ? remoteScheduler.deactivateCalendar(calendarIdList) : schedulerService.deactivateCalendar(calendarIdList);
  }

  /**
   * A calendar saved from the side panel of the configuration screen: reload it in the engine with its stored dates,
   * then reschedule the tasks that use it with it when it is active or without it when it is not, as activating or
   * deactivating it from the row menu does
   * Requires QUARTZ
   *
   * @param calendarId Calendar identifier
   * @param active     1 when the calendar is active
   * @return ServiceData
   * @throws AWException Error reloading the calendar or rescheduling its tasks
   */
  public ServiceData updateConfigurationCalendar(Integer calendarId, Integer active) throws AWException {
    updateSchedulerCalendar(calendarId);
    List<Integer> calendars = List.of(calendarId);
    return Integer.valueOf(1).equals(active) ? activateCalendar(calendars) : deactivateCalendar(calendars);
  }

  /**
   * Start the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error starting scheduler
   */
  public ServiceData start() throws AWException {
    return remote ? remoteScheduler.startScheduler() : schedulerService.start();
  }

  /**
   * Stop the scheduler service
   * Requires QUARTZ
   *
   * @throws AWException Error stopping scheduler
   */
  public ServiceData stop() throws AWException {
    return remote ? remoteScheduler.stopScheduler() : schedulerService.stop();
  }

  /**
   * Scheduler's emergency reboot method
   * Requires QUARTZ
   *
   * @throws AWException Error restarting scheduler
   */
  public ServiceData restart() throws AWException {
    return remote ? remoteScheduler.restartScheduler() : schedulerService.restart();
  }

  /**
   * Clear all scheduled tasks and stop the scheduler.
   * Requires QUARTZ
   *
   * @return Service data
   */
  public ServiceData clearAndStop() throws AWException {
    return remote ? remoteScheduler.clearAndStopScheduler() : schedulerService.clearAndStop();
  }

  /**
   * Returns information about the configured scheduler
   * Requires QUARTZ
   *
   * @return Scheduler metadata
   * @throws AWException Error retrieving scheduler metadata
   */
  public ServiceData getSchedulerMetadata() throws AWException {
    return remote ? remoteScheduler.getSchedulerMetadata() : schedulerService.getSchedulerMetadata();
  }


  /**
   * get parameters for generating cron from context
   *
   * @return Schedule
   */
  private Schedule getSchedule() {
    return new Schedule()
      .setRepeatType(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_REPEAT_TYPE)))
      .setRepeatNumber(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_REPEAT_NUMBER)))
      .setCalendarId(readJsonAsInteger(getRequest().getParameter(CRON_PARAMETER_CALENDAR_IDE)))
      .setInitialDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_INITIAL_DATE)))
      .setInitialTime(getRequest().getParameterAsString(CRON_PARAMETER_INITIAL_TIME))
      .setEndDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_END_DATE)))
      .setEndTime(getRequest().getParameterAsString(CRON_PARAMETER_END_TIME))
      .setDate(DateUtil.web2Date(getRequest().getParameterAsString(CRON_PARAMETER_DATE)))
      .setTime(getRequest().getParameterAsString(CRON_PARAMETER_TIME))
      .setYearList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_YEARS)))
      .setMonthList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_MONTHS)))
      .setWeekList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_WEEKS)))
      .setDayList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_DAYS)))
      .setWeekDayList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_WEEKDAYS)))
      .setHourList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_HOURS)))
      .setMinuteList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_MINUTES)))
      .setSecondList(readJsonAsList(getRequest().getParameter(CRON_PARAMETER_SECONDS)));
  }

  /**
   * Read json node as integer. A criterion that the client does not send (React omits the empty ones) is a null value
   *
   * @param jsonNode Json node
   * @return Integer value
   */
  private Integer readJsonAsInteger(JsonNode jsonNode) {
    return jsonNode == null || jsonNode.isNull() || jsonNode.asString("").isBlank() ? null
      : Integer.parseInt(jsonNode.asString("").trim());
  }

  /**
   * Read json node as integer list. A criterion that the client does not send is a null list
   *
   * @param jsonNode Node
   * @return Integer list
   */
  private List<String> readJsonAsList(JsonNode jsonNode) {
    return jsonNode == null || jsonNode.isNull() ? null : mapper.convertValue(jsonNode, new TypeReference<List<String>>() {
    });
  }
}
