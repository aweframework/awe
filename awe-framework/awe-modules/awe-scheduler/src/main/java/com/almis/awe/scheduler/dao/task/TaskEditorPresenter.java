package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskLaunchSummary;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.util.DependencyCycles;
import com.almis.awe.scheduler.util.TaskSchedulePreset;
import com.almis.awe.scheduler.util.TaskSchedulePreview;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;

/**
 * Server side of the task editor: the tasks a task can run after (its parents, edited from the task), and the data of
 * a task to duplicate it. The dependencies are the rows of AweSchTskDpn: IdeTsk is the task that runs after IdePrn
 */
public class TaskEditorPresenter {

  /**
   * Group of a parent of the picker: chosen, available or not eligible (it already runs after the task)
   */
  public static final String CHOSEN = "chosen";
  public static final String AVAILABLE = "available";
  public static final String BLOCKED = "blocked";
  /**
   * Modes of the editor screen: a new task, the edition of a task and the copy of a task (a new task prefilled with it)
   */
  public static final String MODE_NEW = "new";
  public static final String MODE_EDIT = "edit";
  public static final String MODE_COPY = "copy";
  /**
   * «When» of the editor: the launch type, except a manual task with parents, which runs after other tasks (it is
   * saved as a manual task with its parents)
   */
  public static final String WHEN_AFTER = String.valueOf(TaskLaunchType.DEPENDENCY.getValue());
  /**
   * Side of the one-level graph of the editor: the tasks it runs after (the chosen ones) or the tasks it launches
   */
  public static final String SIDE_PARENTS = "parents";
  public static final String SIDE_CHILDREN = "children";
  /**
   * Criterion of the screens that holds the task they show (kept on reload)
   */
  public static final String SELECTED_TASK = "IdeTsk.selected";
  private static final String EDITOR = "SCREEN_TEXT_TASK_EDITOR_";
  private static final String TASK_DATA = "taskData";
  private static final String TASK_PARAMETERS = "taskParameters";
  private static final String TASK_ID = "taskId";
  private static final String PARENT_ID = "parentId";
  private static final String PARENT_GROUP = "PrnGrp";
  private static final String TASK_PARENTS = "TskPrn";
  private static final String LAUNCH_TYPE = "TypLch";
  private static final String TONE_SUCCESS = "success";
  private static final String TONE_NEUTRAL = "neutral";
  private static final List<String> WEBHOOK_HOOK_FIELDS = List.of("HokKey", "HokAct", "HokSec", "HokTyp");
  /**
   * Longest name of a task (AweSchTsk.Nam)
   */
  private static final int NAME_LENGTH = 40;

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final FlowRunRepository flowRunRepository;
  private final TaskCatalogPresenter catalogPresenter;
  private final ServiceConfig serviceConfig;

  /**
   * Constructor
   *
   * @param queryService      Query service
   * @param queryUtil         Query utilities
   * @param flowRunRepository Flow run repository (the dependencies of the tasks)
   * @param catalogPresenter  Task catalog presenter (the launch of every task)
   * @param serviceConfig     Service configuration giving access to the locales
   */
  public TaskEditorPresenter(QueryService queryService, QueryUtil queryUtil, FlowRunRepository flowRunRepository,
                             TaskCatalogPresenter catalogPresenter, ServiceConfig serviceConfig) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.flowRunRepository = flowRunRepository;
    this.catalogPresenter = catalogPresenter;
    this.serviceConfig = serviceConfig;
  }

  /**
   * Tasks a task can run after: every other task, the chosen ones first, then the available ones and then the ones
   * that cannot be chosen because they already run after the task (a cycle), each group by name (the dependencies keep
   * no order of the parents of a task). Each
   * row tells when the parent launches the tasks that run after it (its own flags, the same for all of them), its
   * «When» summary and whether it is active. A chosen task that already runs after the task (cyclic data saved before
   * the editor) stays chosen and enabled so it can be removed, with the reason in PrnDisTxt: only adding it is blocked.
   * The rows carry nothing restricted to the editors (where a task runs, its files nor who gets its report)
   *
   * @param taskId     Task edited, null for a new task (nothing runs after it yet)
   * @param copyTaskId Task duplicated by a new task, whose parents are the chosen ones; ignored with a task
   * @return One row per task: value, label, Act, PrnCnd, PrnWhnTxt, PrnWhnIcn, PrnWhnSub, PrnGrp, PrnSel, PrnDis and
   * PrnDisTxt
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getParents(Integer taskId, Integer copyTaskId) throws AWException {
    List<TaskDependency> dependencies = flowRunRepository.getDependencies();
    Integer source = taskId != null ? taskId : copyTaskId;
    List<Integer> chosen = parentsOf(source, dependencies);
    Set<Integer> descendants = taskId == null ? Set.of()
      : DependencyCycles.descendants(taskId, childrenByTask(dependencies));

    List<Map<String, CellData>> rows = new ArrayList<>();
    for (TaskLaunchSummary task : catalogPresenter.getLaunches()) {
      if (taskId == null || taskId != task.id()) {
        rows.add(parentRow(task, chosen.contains(task.id()), descendants.contains(task.id())));
      }
    }
    List<String> groups = List.of(CHOSEN, AVAILABLE, BLOCKED);
    rows.sort(Comparator.<Map<String, CellData>>comparingInt(
      row -> groups.indexOf(row.get(PARENT_GROUP).getStringValue()))
      .thenComparing(row -> row.get(JSON_LABEL_PARAMETER).getStringValue().toLowerCase(Locale.ROOT))
      .thenComparingInt(row -> row.get(JSON_VALUE_PARAMETER).getIntegerValue()));

    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Row of a task of the parents picker
   *
   * @param task     Task
   * @param selected The task is a chosen parent
   * @param blocked  The task already runs after the task edited (a cycle)
   * @return Row of the picker
   */
  private Map<String, CellData> parentRow(TaskLaunchSummary task, boolean selected, boolean blocked) {
    String group;
    if (selected) {
      group = CHOSEN;
    } else {
      group = blocked ? BLOCKED : AVAILABLE;
    }
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(task.id()));
    row.put(JSON_LABEL_PARAMETER, new CellData(task.name()));
    row.put("Act", new CellData(task.active() ? 1 : 0));
    row.put("PrnCnd", new CellData(text(EDITOR + "PARENT_" + condition(task))));
    row.put("PrnWhnTxt", new CellData(task.when().text()));
    row.put("PrnWhnIcn", new CellData(task.when().icon()));
    row.put("PrnWhnSub", new CellData(task.when().detail()));
    row.put(PARENT_GROUP, new CellData(group));
    row.put("PrnSel", new CellData(selected ? 1 : 0));
    // Only adding a blocked task is disabled: a chosen one (cyclic data saved before) can still be removed
    row.put("PrnDis", new CellData(blocked && !selected ? 1 : 0));
    row.put("PrnDisTxt", new CellData(blocked ? text(EDITOR + "PARENT_CYCLE") : ""));
    return row;
  }

  /**
   * Data of a task to duplicate it (the fields of the taskData query): the same execution, launch, parameters,
   * behaviour and notifications, with the name of a copy («name (copy)» in the language of the user, the name cut so
   * that it fits in the 40 characters of a task name), inactive, without identifier nor webhook hook (its key cannot
   * repeat), and with the parents of the task by identifier (TskPrn). The tasks that run after it are not copied
   *
   * @param taskId Task to duplicate
   * @return One row, none when the task does not exist
   * @throws AWException The task cannot be read
   */
  public ServiceData getCopy(Integer taskId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    // The task is the one asked for, not the row selected in the screen
    parameters.remove(SELECTED_TASK);
    parameters.put(TASK_ID, taskId);
    parameters.putArray("sort");
    DataList found = queryService.launchPrivateQuery(TASK_DATA, parameters).getDataList();
    DataList dataList = new DataList();
    if (found != null && !found.getRows().isEmpty()) {
      Map<String, CellData> copy = new HashMap<>(found.getRows().get(0));
      String name = copyName(copy.getOrDefault("Nam", new CellData("")).getStringValue());
      copy.put("Nam", new CellData(name));
      copy.put("name", new CellData(name));
      copy.put("Act", new CellData(0));
      copy.put("active", new CellData(false));
      copy.put("Ide", new CellData());
      copy.put(TASK_ID, new CellData());
      WEBHOOK_HOOK_FIELDS.forEach(field -> copy.put(field, new CellData()));
      ArrayNode parents = JsonNodeFactory.instance.arrayNode();
      parentsOf(taskId, flowRunRepository.getDependencies()).stream().sorted().forEach(parents::add);
      copy.put(TASK_PARENTS, new CellData(parents));
      dataList.addRow(copy);
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Data of the editor screen (its target) for a mode: the task with its parents (edit), its copy (copy) or nothing but
   * the fields of the mode (new). Every mode brings the mode (TskEdtMod), its «When» (TskWhn: the launch type, or
   * {@link #WHEN_AFTER} for a manual task with parents) and the header of the screen: its title (TskEdtTit), the line
   * under it (TskEdtSub) and the active state of the task as a chip (TskEdtChp, TskEdtChpTon). An unknown mode is a
   * new task; an edit or copy of a task that does not exist brings only the fields of the mode
   *
   * @param mode   Mode of the editor (new, edit or copy)
   * @param taskId Task edited or duplicated (the task of the screen that opens the editor), ignored for a new task
   * @return One row
   * @throws AWException The task cannot be read
   */
  public ServiceData getEditor(String mode, Integer taskId) throws AWException {
    String editorMode = mode(mode, taskId);
    Map<String, CellData> row = editorTask(editorMode, taskId);
    boolean hasTask = row.containsKey("Nam");
    if (!hasTask) {
      // The target fills every field of the screen, so a new task brings the initial values of the form (an empty field
      // would make a select choose its first option): active, a maintain, manual, no report but on error and warning
      row.putAll(newTaskDefaults());
    }
    putHeader(row, editorMode, taskId, hasTask);
    putPreset(row, hasTask);
    row.put("TskEdtMod", new CellData(hasTask ? editorMode : MODE_NEW));
    String whenValue = hasTask ? when(row) : String.valueOf(TaskLaunchType.MANUAL.getValue());
    row.put("TskWhn", new CellData(whenValue));
    row.put("SchVw", new CellData(view(whenValue, row.get("SchPre").getStringValue())));

    DataList dataList = new DataList();
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Stored data of the task of the editor: the task with its parents (edit), its copy (copy), nothing for a new task or
   * a task that does not exist
   */
  private Map<String, CellData> editorTask(String editorMode, Integer taskId) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    if (MODE_EDIT.equals(editorMode)) {
      List<Map<String, CellData>> found = taskData(taskId).getRows();
      if (!found.isEmpty()) {
        row.putAll(found.get(0));
        row.put(TASK_PARENTS, new CellData(idArray(parentsOf(taskId, flowRunRepository.getDependencies()))));
      }
    } else if (MODE_COPY.equals(editorMode)) {
      List<Map<String, CellData>> found = getCopy(taskId).getDataList().getRows();
      if (!found.isEmpty()) {
        row.putAll(found.get(0));
      }
    }
    return row;
  }

  /**
   * Header of the editor screen: its title (TskEdtTit), the line under it (TskEdtSub) and the active state of the task
   * as a chip (TskEdtChp, TskEdtChpTon)
   */
  private void putHeader(Map<String, CellData> row, String editorMode, Integer taskId, boolean hasTask) {
    String name = hasTask ? row.get("Nam").getStringValue() : "";
    boolean active = !hasTask || (row.get("Act") != null && "1".equals(row.get("Act").getStringValue()));
    boolean edit = MODE_EDIT.equals(editorMode);
    String title;
    String subtitle;
    if (!hasTask) {
      title = text(EDITOR + "TITLE_NEW");
      subtitle = text(EDITOR + "SUBTITLE_NEW");
    } else if (edit) {
      title = text(EDITOR + "TITLE_EDIT", String.valueOf(taskId));
      subtitle = text(EDITOR + "SUBTITLE_EDIT", name, String.valueOf(taskId));
    } else {
      title = text(EDITOR + "TITLE_COPY");
      subtitle = text(EDITOR + "SUBTITLE_COPY", name, String.valueOf(taskId));
    }
    row.put("TskEdtTit", new CellData(title));
    row.put("TskEdtSub", new CellData(subtitle));
    row.put("TskEdtChp", new CellData(text(EDITOR + (active ? "ACTIVE" : "INACTIVE"))));
    row.put("TskEdtChpTon", new CellData(active ? TONE_SUCCESS : TONE_NEUTRAL));
  }

  /**
   * Options of the parents picker of the editor: the rows of {@link #getParents(Integer, Integer)} for the task of the
   * mode (the task edited, or the task duplicated by a copy), with the fields the select of the engines reads: the
   * locale of its group (group: chosen, available or not eligible), whether it cannot be chosen (disabled) and the line
   * under its name (description: its condition, and why it cannot be chosen when it already runs after the task)
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return One row per task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getEditorParents(String mode, Integer taskId) throws AWException {
    String editorMode = mode(mode, taskId);
    ServiceData parents = getParents(MODE_EDIT.equals(editorMode) ? taskId : null,
      MODE_COPY.equals(editorMode) ? taskId : null);
    for (Map<String, CellData> row : parents.getDataList().getRows()) {
      String reason = row.get("PrnDisTxt").getStringValue();
      String condition = row.get("PrnCnd").getStringValue();
      row.put("group", new CellData(EDITOR + "GROUP_" + row.get(PARENT_GROUP).getStringValue().toUpperCase(Locale.ROOT)));
      row.put("disabled", new CellData("1".equals(row.get("PrnDis").getStringValue())));
      row.put("description", new CellData(reason.isEmpty() ? condition : condition + " · " + reason));
    }
    return parents;
  }

  /**
   * Parameters of the task of the editor: none for a new task, the ones of the task edited, and the ones of the task
   * duplicated by a copy without their identifiers (they are inserted as new parameters of the copy)
   *
   * @param mode   Mode of the editor
   * @param taskId Task of the screen that opened the editor
   * @return The parameters, as the taskParameters query reads them
   * @throws AWException The parameters cannot be read
   */
  public ServiceData getEditorParameters(String mode, Integer taskId) throws AWException {
    String editorMode = mode(mode, taskId);
    DataList dataList = new DataList();
    if (!MODE_NEW.equals(editorMode)) {
      ObjectNode parameters = taskParameters(taskId);
      DataList found = queryService.launchPrivateQuery(TASK_PARAMETERS, parameters).getDataList();
      if (found != null) {
        for (Map<String, CellData> row : found.getRows()) {
          Map<String, CellData> parameter = new HashMap<>(row);
          if (MODE_COPY.equals(editorMode)) {
            parameter.put("id", new CellData());
            parameter.put("IdePar", new CellData());
          }
          dataList.addRow(parameter);
        }
      }
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * One-level graph of the editor (the rows of the flow-graph widget in task mode), one side at a time. The parents
   * side draws the parents chosen in the editor (not saved yet) joining into the task, each edge with the condition of
   * its parent (its own flags, read only); the children side draws the task and the tasks that run after it, as saved,
   * with the other tasks each of them waits for. A new task or a copy has no saved child: its children side is empty.
   * The task of a new task or a copy is the node 0 (it has no identifier yet), named «This task»
   *
   * @param mode    Mode of the editor
   * @param taskId  Task of the screen that opened the editor
   * @param parents Parents chosen in the editor (the parents side only)
   * @param side    Side of the graph: parents or children
   * @return Rows of the graph, none when the side has no task
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getEditorGraph(String mode, Integer taskId, List<Integer> parents, String side)
    throws AWException {
    String editorMode = mode(mode, taskId);
    boolean edit = MODE_EDIT.equals(editorMode);
    int root = edit ? taskId : 0;
    Map<Integer, TaskLaunchSummary> tasks = new HashMap<>();
    for (TaskLaunchSummary task : catalogPresenter.getLaunches()) {
      tasks.put(task.id(), task);
    }
    TaskLaunchSummary self = edit ? tasks.get(taskId) : null;
    String rootName = self == null ? text(EDITOR + "THIS_TASK") : self.name();

    List<Map<String, CellData>> rows = SIDE_CHILDREN.equals(side)
      ? childrenGraph(taskId, rootName, self, tasks)
      : parentsGraph(parents, root, rootName, self, tasks);
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Children side of the one-level graph of the editor: the task edited and the tasks that run after it, as saved,
   * with the other tasks each of them waits for; none for a task without children or a task not saved yet
   *
   * @param taskId   Task edited (the root of the graph)
   * @param rootName Name of the task edited
   * @param self     Launch of the task edited, null when it is not saved
   * @param tasks    Launch of every task by identifier
   * @return Rows of the graph
   * @throws AWException The dependencies cannot be read
   */
  private List<Map<String, CellData>> childrenGraph(Integer taskId, String rootName, TaskLaunchSummary self,
                                                    Map<Integer, TaskLaunchSummary> tasks) throws AWException {
    if (self == null) {
      return List.of();
    }
    List<TaskDependency> dependencies = flowRunRepository.getDependencies();
    Set<Integer> children = new LinkedHashSet<>();
    dependencies.stream().filter(dependency -> Objects.equals(taskId, dependency.getParentId()))
      .forEach(dependency -> children.add(dependency.getTaskId()));
    children.retainAll(tasks.keySet());
    if (children.isEmpty()) {
      return List.of();
    }
    List<Map<String, CellData>> rows = new ArrayList<>();
    rows.add(graphRow(taskId, null, rootName, self, taskId));
    Set<Integer> others = new LinkedHashSet<>();
    for (TaskDependency dependency : dependencies) {
      if (children.contains(dependency.getTaskId()) && tasks.containsKey(dependency.getParentId())) {
        rows.add(graphRow(dependency.getTaskId(), dependency.getParentId(),
          tasks.get(dependency.getTaskId()).name(), tasks.get(dependency.getTaskId()), null));
        if (!Objects.equals(taskId, dependency.getParentId())) {
          others.add(dependency.getParentId());
        }
      }
    }
    others.forEach(other -> rows.add(graphRow(other, null, tasks.get(other).name(), tasks.get(other), null)));
    return rows;
  }

  /**
   * Parents side of the one-level graph of the editor: the parents chosen in the editor (not saved yet) joining into
   * the task
   *
   * @param parents  Parents chosen in the editor
   * @param root     Node of the task (its identifier, 0 when it is not saved)
   * @param rootName Name of the task
   * @param self     Launch of the task, null when it is not saved
   * @param tasks    Launch of every task by identifier
   * @return Rows of the graph
   */
  private static List<Map<String, CellData>> parentsGraph(List<Integer> parents, int root, String rootName,
                                                          TaskLaunchSummary self,
                                                          Map<Integer, TaskLaunchSummary> tasks) {
    List<Integer> chosen = parents == null ? List.of() : parents.stream().filter(Objects::nonNull)
      .filter(tasks::containsKey).filter(parent -> parent != root).distinct().toList();
    List<Map<String, CellData>> rows = new ArrayList<>();
    for (Integer parent : chosen) {
      rows.add(graphRow(root, parent, rootName, self, root));
      rows.add(graphRow(parent, null, tasks.get(parent).name(), tasks.get(parent), null));
    }
    return rows;
  }

  /**
   * Answer of a save of the editor: its message, and the task saved as the task of the screen (IdeTsk.selected), so the
   * task screen opened next shows it (a new task or a copy has just got its identifier)
   *
   * @param taskId Task saved
   * @return Message and select action
   */
  public ServiceData saved(Integer taskId) {
    return new ServiceData()
      .setTitle(text(EDITOR + "SAVED_TITLE"))
      .setMessage(text(EDITOR + "SAVED_MESSAGE", String.valueOf(taskId)))
      .addClientAction(new ClientAction("select")
        .setTarget(SELECTED_TASK)
        .setSilent(true)
        .addParameter("values", new CellData(List.of(new CellData(taskId)))));
  }

  /**
   * Options of the choice cards of the editor (#993): what launches the task (when) and what it runs (execution), each
   * with its value, its label, its icon and its description (locale keys, translated by the client); and the day
   * toggles of a weekly repetition (weekdays), from Monday, each with its initial and its full name as its title
   *
   * @param group Choice: when, execution or weekdays
   * @return One row per option, none for another choice
   */
  public ServiceData getEditorOptions(String group) {
    if ("weekdays".equals(group)) {
      return weekDayOptions();
    }
    List<String[]> options = switch (Optional.ofNullable(group).orElse("")) {
      case "when" -> List.of(
        new String[]{"0", "ENUM_TASK_EDITOR_WHEN_MANUAL", "lucide:hand", EDITOR + "WHEN_MANUAL_HELP"},
        new String[]{"1", "ENUM_TASK_EDITOR_WHEN_SCHEDULED", "lucide:calendar-clock", EDITOR + "WHEN_SCHEDULED_HELP"},
        new String[]{"2", "ENUM_TASK_EDITOR_WHEN_FILE", "lucide:file-down", EDITOR + "WHEN_FILE_HELP"},
        new String[]{"3", "ENUM_TASK_EDITOR_WHEN_AFTER", "lucide:git-merge", EDITOR + "WHEN_AFTER_HELP"},
        new String[]{"4", "ENUM_TASK_EDITOR_WHEN_WEBHOOK", "lucide:webhook", EDITOR + "WHEN_WEBHOOK_HELP"});
      case "execution" -> List.of(
        new String[]{"1", "ENUM_EXECUTE_MAINTAIN", "lucide:database", EDITOR + "EXECUTE_MAINTAIN_HELP"},
        new String[]{"0", "ENUM_EXECUTE_COMMAND", "lucide:terminal", EDITOR + "EXECUTE_COMMAND_HELP"});
      default -> List.of();
    };
    DataList dataList = new DataList();
    for (String[] option : options) {
      Map<String, CellData> row = new HashMap<>();
      row.put(JSON_VALUE_PARAMETER, new CellData(option[0]));
      row.put(JSON_LABEL_PARAMETER, new CellData(option[1]));
      row.put("icon", new CellData(option[2]));
      row.put("description", new CellData(option[3]));
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Live preview of the «When» of a scheduled task in the editor: the repetition in a sentence with its key parts bold,
   * its next runs as tiles, the first holiday it skips and how it is saved
   *
   * @param input What the preview is built from
   * @param now   Current date
   * @return One row: title, tiles and their label, description, details and their label
   * @see TaskSchedulePreview
   */
  public ServiceData schedulePreview(TaskSchedulePreview.Input input, LocalDateTime now) {
    DataList dataList = new DataList();
    dataList.addRow(new TaskSchedulePreview(this::text).build(input, now));
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Preset of the «Repetition» of the editor that the stored fields of a scheduled task match (the custom one when
   * none does) with its fields: SchPre, SchEvy, SchTim, SchWkd and SchMdy. Any other task starts on a daily one
   */
  private static void putPreset(Map<String, CellData> row, boolean hasTask) {
    TaskSchedulePreset.Values values = new TaskSchedulePreset.Values().setPreset(TaskSchedulePreset.DAILY);
    CellData launchType = row.get(LAUNCH_TYPE);
    if (hasTask && launchType != null && String.valueOf(TaskLaunchType.SCHEDULED.getValue())
      .equals(launchType.getStringValue())) {
      DataList stored = new DataList();
      stored.addRow(row);
      values = TaskSchedulePreset.detect(DataListUtil.asBeanList(stored, Schedule.class).stream().findFirst()
        .orElse(new Schedule()));
    }
    ArrayNode weekDays = JsonNodeFactory.instance.arrayNode();
    List<String> presetDays = TaskSchedulePreset.presetDays(values.getPreset());
    (presetDays.isEmpty() ? values.getWeekDays() : presetDays).forEach(weekDays::add);
    row.put("SchPre", new CellData(values.getPreset()));
    row.put("SchEvy", new CellData(values.getEvery()));
    row.put("SchTim", new CellData(values.getTime()));
    row.put("SchWkd", new CellData(weekDays));
    row.put("SchMdy", new CellData(values.getMonthDay()));
    // The day toggles show the days of a daily or workdays preset, and remember the preset they were filled for
    row.put("SchWkdPre", new CellData(TaskSchedulePreset.syncDays(values.getPreset(), null, values.getWeekDays())
      .map(TaskSchedulePreset.DaysSync::daysPreset).orElse("")));
  }

  /**
   * Day toggles of a weekly repetition: the days from Monday as the editor numbers them (Sunday 0), their initial as
   * the label and their full name as the title (the accessible name of the toggle)
   */
  private static ServiceData weekDayOptions() {
    List<String> names = List.of("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
    DataList dataList = new DataList();
    for (int day : List.of(1, 2, 3, 4, 5, 6, 0)) {
      Map<String, CellData> row = new HashMap<>();
      row.put(JSON_VALUE_PARAMETER, new CellData(String.valueOf(day)));
      row.put(JSON_LABEL_PARAMETER, new CellData("ENUM_TASK_EDITOR_DAY_INITIAL_" + day));
      row.put("title", new CellData("PARAMETER_WEEKDAY_" + names.get(day)));
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Initial values of a new task in the editor, the ones of the old new task screen
   */
  private static Map<String, CellData> newTaskDefaults() {
    Map<String, CellData> defaults = new HashMap<>();
    defaults.put("Act", new CellData(1));
    defaults.put("TypExe", new CellData(1));
    defaults.put(LAUNCH_TYPE, new CellData(TaskLaunchType.MANUAL.getValue()));
    defaults.put("RepTyp", new CellData(0));
    defaults.put("HokAct", new CellData(1));
    ArrayNode reportStatuses = JsonNodeFactory.instance.arrayNode().add("1").add("2");
    defaults.put("RepSndSta", new CellData(reportStatuses));
    for (String flag : List.of("LchDepWrn", "LchDepErr", "LchSetWrn")) {
      defaults.put(flag, new CellData(0));
    }
    return defaults;
  }

  /**
   * What «When» shows for the «When» of a task (SchVw): the preset of a scheduled task, the fields of a file (FIL) or a
   * webhook (WHK) task, none for the others. The screen computes it as «When» changes; it comes with the task so the
   * fields it shows are in sight when the editor opens
   */
  private static String view(String when, String preset) {
    return switch (when) {
      case "1" -> preset;
      case "2" -> "FIL";
      case "4" -> "WHK";
      default -> "NON";
    };
  }

  /**
   * Mode of the editor: edit and copy need a task, anything else is a new task
   */
  private static String mode(String mode, Integer taskId) {
    return taskId != null && (MODE_EDIT.equals(mode) || MODE_COPY.equals(mode)) ? mode : MODE_NEW;
  }

  /**
   * «When» of the editor for the data of a task: a manual task with parents (or an old dependency task) runs after
   * other tasks, a task of another type keeps its type (its parents, if any, are kept as they are)
   */
  private static String when(Map<String, CellData> row) {
    CellData launchType = row.get(LAUNCH_TYPE);
    Integer type = launchType == null || launchType.getObjectValue() == null || launchType.getStringValue().isBlank()
      ? TaskLaunchType.MANUAL.getValue() : launchType.getIntegerValue();
    CellData parents = row.get(TASK_PARENTS);
    boolean hasParents = parents != null && parents.getObjectValue() instanceof ArrayNode list && !list.isEmpty();
    return TaskLaunchKind.of(type, hasParents) == TaskLaunchKind.DEPENDENCY ? WHEN_AFTER : String.valueOf(type);
  }

  /**
   * Row of the one-level graph of the editor
   */
  private static Map<String, CellData> graphRow(int task, Integer parent, String name, TaskLaunchSummary summary,
                                                Integer root) {
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(task + "-" + (parent == null ? "" : parent)));
    row.put(TASK_ID, new CellData(task));
    row.put(PARENT_ID, parent == null ? new CellData() : new CellData(parent));
    row.put("name", new CellData(name));
    row.put("launchIcon", new CellData(summary == null ? "" : summary.when().icon()));
    row.put("launchLabel", new CellData(summary == null ? "" : summary.when().text()));
    row.put("active", new CellData(summary == null || summary.active() ? 1 : 0));
    row.put("onWarning", new CellData(summary != null && summary.onWarning() ? 1 : 0));
    row.put("onError", new CellData(summary != null && summary.onError() ? 1 : 0));
    row.put("rootTask", root == null ? new CellData() : new CellData(root));
    return row;
  }

  private DataList taskData(Integer taskId) throws AWException {
    DataList found = queryService.launchPrivateQuery(TASK_DATA, taskParameters(taskId)).getDataList();
    return found == null ? new DataList() : found;
  }

  /**
   * Parameters of a query of the task asked for, not of the row selected in the screen
   */
  private ObjectNode taskParameters(Integer taskId) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.remove(SELECTED_TASK);
    parameters.put(TASK_ID, taskId);
    parameters.putArray("sort");
    return parameters;
  }

  private static ArrayNode idArray(List<Integer> ids) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    ids.stream().sorted().forEach(array::add);
    return array;
  }

  /**
   * Name of the copy of a task: the suffix of the language of the user is always kept, the name is cut so that the
   * whole fits in a task name
   */
  private String copyName(String name) {
    int room = Math.max(0, NAME_LENGTH - text(EDITOR + "COPY_NAME", "").length());
    String kept = name.length() > room ? name.substring(0, room).stripTrailing() : name;
    return text(EDITOR + "COPY_NAME", kept);
  }

  /**
   * Parents of a task, each once
   */
  private static List<Integer> parentsOf(Integer taskId, List<TaskDependency> dependencies) {
    if (taskId == null) {
      return List.of();
    }
    return dependencies.stream()
      .filter(dependency -> taskId.equals(dependency.getTaskId()))
      .map(TaskDependency::getParentId)
      .distinct()
      .toList();
  }

  private static Map<Integer, List<Integer>> childrenByTask(List<TaskDependency> dependencies) {
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (TaskDependency dependency : dependencies) {
      children.computeIfAbsent(dependency.getParentId(), key -> new ArrayList<>()).add(dependency.getTaskId());
    }
    return children;
  }

  /**
   * Condition of a parent: the ends that launch the tasks that run after it
   */
  private static String condition(TaskLaunchSummary task) {
    if (task.onWarning() && task.onError()) {
      return "ON_ANY";
    } else if (task.onWarning()) {
      return "ON_OK_WARNING";
    } else if (task.onError()) {
      return "ON_OK_ERROR";
    }
    return "ON_OK";
  }

  /**
   * Text of a locale with its parameters
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }
}
