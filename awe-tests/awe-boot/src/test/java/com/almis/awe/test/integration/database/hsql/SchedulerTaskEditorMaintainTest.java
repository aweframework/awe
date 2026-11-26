package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maintains and queries of the single task editor (#993, S8) on a real database, through the action controller as the
 * editor launches them: the editor saves the parents a task runs after as the rows where the task is the child
 * (AweSchTskDpn, IdeTsk = task), replacing only its own rows, and reloads the task without purging its executions;
 * a task saved inactive is left without trigger until it is activated; a parent that already runs after the task is
 * refused and nothing changes; and the parents picker and the copy of a
 * task read what they show. Each test creates its own tasks and deletes them.
 */
@Tag("integration")
class SchedulerTaskEditorMaintainTest extends AbstractSchedulerMaintainTest {

  private static final String NEW_TARGET = "NewSchedulerTaskEditor";
  private static final String UPDATE_TARGET = "UpdateSchedulerTaskEditor";

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbc;

  @BeforeEach
  void connect() {
    jdbc = new JdbcTemplate(dataSource);
  }

  @Test
  void aNewTaskRunsAfterTheChosenParentsAsAManualTask() throws Exception {
    int first = createTask(manualTask(unique("Editor parent A")));
    int second = createTask(manualTask(unique("Editor parent B")));
    Integer child = null;
    try {
      child = createTask(editorTask(unique("Editor child"), second, first));

      assertThat(parentRows(child)).containsExactly(first, second);
      assertThat(taskData(child).get("TypLch").asString()).isEqualTo("0");
    } finally {
      deleteTasks(child == null ? new Integer[]{first, second} : new Integer[]{child, first, second});
    }
  }

  @Test
  void savingATaskFromTheEditorReplacesOnlyItsOwnParentRows() throws Exception {
    int first = createTask(manualTask(unique("Editor parent A")));
    int second = createTask(manualTask(unique("Editor parent B")));
    // A scheduled parent: saving its child does not touch it in the scheduler, it keeps its trigger
    int third = createTask(scheduledTask(unique("Editor parent C"), 6));
    int child = createTask(editorTask(unique("Editor child"), first, second));
    int sibling = createTask(editorTask(unique("Editor sibling"), first, second));
    try {
      String name = unique("Editor child updated");
      maintain(UPDATE_TARGET, editorTask(name, third, first).put("Ide", child));

      assertThat(configuredJobs()).contains(String.valueOf(third));
      assertThat(taskListRow(third).orElseThrow().get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 00:00:00");

      assertThat(parentRows(child)).containsExactly(first, third);
      assertThat(parentRows(sibling)).containsExactly(first, second);
      assertThat(taskData(child).get("Nam").asString()).isEqualTo(name);
      // The parents keep their own data: only the rows of the child change
      assertThat(taskData(first).get("LchDepWrn").asString()).isEqualTo("1");

      maintain(UPDATE_TARGET, editorTask(name).put("Ide", child));
      assertThat(parentRows(child)).isEmpty();
      assertThat(parentRows(sibling)).containsExactly(first, second);
    } finally {
      deleteTasks(sibling, child, first, second, third);
    }
  }

  @Test
  void editingATaskKeepsItsExecutionsAndTheirLogAndSchedulesItsNewLaunch() throws Exception {
    String name = unique("Editor history");
    int task = createTask(editorTask(name));
    try {
      jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy) "
        + "VALUES (?, 'MANUAL_TASK', 1, ?, ?, 60000, 0, 'test')", task,
        Timestamp.valueOf(LocalDateTime.of(2019, 3, 1, 8, 0)), Timestamp.valueOf(LocalDateTime.of(2019, 3, 1, 8, 1)));
      jdbc.update("INSERT INTO AweSchExeLog (IdeTsk, ExeTsk, Src, Sec, Slt, LinNum, LinTxt, LogDat) "
        + "VALUES (?, 1, 'J', 'H', 0, 1, 'probe line', ?)", task,
        Timestamp.valueOf(LocalDateTime.of(2019, 3, 1, 8, 0)));
      // The rows the task has now (the database may keep older rows of the same task id)
      int executions = count("AweSchExe", task);
      int logLines = count("AweSchExeLog", task);
      assertThat(executions).isPositive();
      assertThat(logLines).isPositive();

      // Manual to scheduled: the scheduler gets its trigger
      maintain(UPDATE_TARGET, editorTask(name).put("Ide", task).put("TypLch", 1).put("RptTyp", 2).put("RptNum", 6)
        .put("IniDat", FUTURE_DATE).put("IniTim", "00:00:00"));
      assertThat(configuredJobs()).contains(String.valueOf(task));
      assertThat(taskListRow(task).orElseThrow().get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 00:00:00");
      assertThat(count("AweSchExe", task)).isEqualTo(executions);
      assertThat(count("AweSchExeLog", task)).isEqualTo(logLines);

      // Scheduled to manual: the trigger is removed
      maintain(UPDATE_TARGET, editorTask(name).put("Ide", task));
      assertThat(configuredJobs()).doesNotContain(String.valueOf(task));
      assertThat(count("AweSchExe", task)).isEqualTo(executions);
      assertThat(count("AweSchExeLog", task)).isEqualTo(logLines);
    } finally {
      jdbc.update("DELETE FROM AweSchExeLog WHERE IdeTsk = ?", task);
      deleteTasks(task);
    }
  }

  @Test
  void aTaskSavedInactiveIsLeftWithoutTriggerAndActivatingItSchedulesIt() throws Exception {
    String name = unique("Editor inactive");
    int task = createTask(editorTask(name).put("TypLch", 1).put("RptTyp", 2).put("RptNum", 6)
      .put("IniDat", FUTURE_DATE).put("IniTim", "00:00:00"));
    try {
      assertThat(configuredJobs()).contains(String.valueOf(task));

      // Saved inactive: no trigger at all, the screens show it as an inactive task
      maintain(UPDATE_TARGET, editorTask(name).put("Ide", task).put("Act", 0).put("TypLch", 1).put("RptTyp", 2)
        .put("RptNum", 6).put("IniDat", FUTURE_DATE).put("IniTim", "00:00:00"));
      assertThat(configuredJobs()).doesNotContain(String.valueOf(task));
      assertThat(taskListRow(task).orElseThrow().get("NxtTim").asString()).isEqualTo("-");
      JsonNode catalog = find(rows("getTaskCatalog", params().put("CrtTskSch", name)), "Nam", name).orElseThrow();
      assertThat(catalog.get("TskNxtSub").asString()).isEqualTo("task inactive");
      JsonNode next = rows("getTaskPageNext", params().put("IdeTsk.selected", task)).get(0);
      assertThat(next.get("value").asString()).isEmpty();
      assertThat(next.get("description").asString()).contains("task inactive");

      // Activating it adds its trigger from the stored task
      maintain("ActSchTsk", params().put("IdeTsk.selected", task));
      assertThat(configuredJobs()).contains(String.valueOf(task));
      assertThat(taskListRow(task).orElseThrow().get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 00:00:00");
    } finally {
      deleteTasks(task);
    }
  }

  @Test
  void aParentThatAlreadyRunsAfterTheTaskIsRefusedAndNothingChanges() throws Exception {
    int parent = createTask(manualTask(unique("Editor parent A")));
    String name = unique("Editor middle");
    int middle = createTask(editorTask(name, parent));
    int grandchild = createTask(editorTask(unique("Editor grandchild"), middle));
    try {
      List<String> messages = errorTitles(perform("maintain", UPDATE_TARGET,
        editorTask(name + " renamed", grandchild).put("Ide", middle)));
      List<String> itself = errorTitles(perform("maintain", UPDATE_TARGET,
        editorTask(name, middle).put("Ide", middle)));

      assertThat(messages).containsExactly("Circular dependency");
      assertThat(itself).containsExactly("Circular dependency");
      assertThat(taskData(middle).get("Nam").asString()).isEqualTo(name);
      assertThat(parentRows(middle)).containsExactly(parent);
      assertThat(parentRows(grandchild)).containsExactly(middle);
    } finally {
      deleteTasks(grandchild, middle, parent);
    }
  }

  @Test
  void thePickerListsTheParentsOfTheTaskAndTheCopyKeepsThemWithANewInactiveName() throws Exception {
    int parent = createTask(manualTask(unique("Editor parent A")));
    String name = unique("Editor copied");
    int task = createTask(editorTask(name, parent));
    int child = createTask(editorTask(unique("Editor child"), task));
    try {
      List<JsonNode> parents = rows("getTaskEditorParents", params().put("taskId", task));
      JsonNode chosen = find(parents, "value", String.valueOf(parent)).orElseThrow();
      JsonNode blocked = find(parents, "value", String.valueOf(child)).orElseThrow();
      JsonNode copy = rows("getTaskEditorCopy", params().put("taskId", task)).get(0);

      assertThat(find(parents, "value", String.valueOf(task))).isEmpty();
      assertThat(chosen.get("PrnGrp").asString()).isEqualTo("chosen");
      assertThat(chosen.get("PrnCnd").asString()).isEqualTo("OK, warning or error");
      assertThat(chosen.get("PrnWhnTxt").asString()).isEqualTo("Launched by hand");
      assertThat(blocked.get("PrnDis").asString()).isEqualTo("1");
      assertThat(blocked.get("PrnDisTxt").asString()).isEqualTo("Circular dependency: it already runs after this task");
      assertThat(copy.get("Nam").asString()).isEqualTo(name + " (copy)");
      assertThat(copy.get("Act").asString()).isEqualTo("0");
      assertThat(copy.get("TskPrn").toString()).isEqualTo("[" + parent + "]");
    } finally {
      deleteTasks(child, task, parent);
    }
  }

  @Test
  void theEditorScreenReadsTheTaskOfItsModeAndASaveAnswersWithTheSavedTaskAsTheTaskOfTheScreen() throws Exception {
    int parent = createTask(manualTask(unique("Editor parent A")));
    String name = unique("Editor screen");
    JsonNode answer = perform("maintain", NEW_TARGET, editorTask(name, parent));
    int task = find(rows("getSchedulerTaskList", params()), "Nam", name).orElseThrow().get("IdeTsk").asInt();
    try {
      // The save selects the new task in the screen, so the task screen opened next shows it
      JsonNode select = find(toList(answer), "type", "select").orElseThrow();
      assertThat(select.get("target").asString()).isEqualTo("IdeTsk.selected");
      assertThat(select.get("parameters").get("values").toString()).contains(String.valueOf(task));

      ObjectNode edit = params().put("TskEdtMod", "edit").put("IdeTsk.selected", task);
      JsonNode screen = rows("getTaskEditor", edit).get(0);
      assertThat(screen.get("TskWhn").asString()).isEqualTo("3");
      assertThat(screen.get("TskPrn").toString()).isEqualTo("[" + parent + "]");
      assertThat(rows("getTaskEditorHeader", edit).get(0).get("title").asString()).isEqualTo("Edit task #" + task);
      JsonNode option = find(rows("getTaskEditorParentOptions", edit), "value", String.valueOf(parent)).orElseThrow();
      assertThat(option.get("group").asString()).isEqualTo("SCREEN_TEXT_TASK_EDITOR_GROUP_CHOSEN");
      assertThat(rows("getTaskEditorParameters", params().put("TskEdtMod", "copy").put("IdeTsk.selected", task)))
        .singleElement().satisfies(row -> assertThat(row.get("ParNam").asString()).isEqualTo("secondsToWait"));
      assertThat(rows("getTaskEditorParentGraph", edit.deepCopy().set("TskPrn", ints(parent))))
        .extracting(row -> row.get("id").asString()).containsExactly(task + "-" + parent, parent + "-");
    } finally {
      deleteTasks(task, parent);
    }
  }

  @Test
  void aScheduledTaskSavedWithAPresetStoresItsFieldsFiresAsItSaysAndReopensOnIt() throws Exception {
    String name = unique("Editor workdays");
    // The screen sends other stored fields too (a custom repetition before the preset was chosen): the preset wins
    ObjectNode workdays = editorTask(name).put("TypLch", 1).put("SchPre", "WRK").put("SchTim", "22:30")
      .put("RptTyp", 0).put("RptNum", 99).put("IniDat", FUTURE_DATE).put("IniTim", "00:00:00");
    workdays.putArray("hours").add("4");
    int task = createTask(workdays);
    try {
      JsonNode stored = taskData(task);
      assertThat(stored.get("RptTyp").asString()).isEqualTo("7");
      assertThat(stored.get("RptNum").asString()).isEmpty();
      assertThat(stored.get("weekDays").toString()).isEqualTo("[\"1\",\"2\",\"3\",\"4\",\"5\"]");
      assertThat(stored.get("hours").toString()).isEqualTo("[\"22\"]");
      assertThat(stored.get("minutes").toString()).isEqualTo("[\"30\"]");
      // 01/01/2099 is a Thursday: the first run is that day at 22:30
      assertThat(taskListRow(task).orElseThrow().get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 22:30:00");

      JsonNode screen = rows("getTaskEditor", params().put("TskEdtMod", "edit").put("IdeTsk.selected", task)).get(0);
      assertThat(screen.get("SchPre").asString()).isEqualTo("WRK");
      assertThat(screen.get("SchTim").asString()).isEqualTo("22:30:00");
    } finally {
      deleteTasks(task);
    }
  }

  @Test
  void anUpdateAnswersThatTheTaskIsSavedAndSelectsItAsTheTaskOfTheScreen() throws Exception {
    String name = unique("Editor saved");
    int task = createTask(editorTask(name));
    try {
      List<JsonNode> answer = toList(perform("maintain", UPDATE_TARGET, editorTask(name).put("Ide", task)));

      JsonNode message = find(answer, "type", "message").orElseThrow().get("parameters");
      assertThat(message.get("type").asString()).isEqualTo("ok");
      assertThat(message.get("title").asString()).isEqualTo("Task saved");
      assertThat(message.get("message").asString()).isEqualTo("Task #" + task + " has been saved");
      JsonNode select = find(answer, "type", "select").orElseThrow();
      assertThat(select.get("target").asString()).isEqualTo("IdeTsk.selected");
      assertThat(select.get("parameters").get("values").toString()).isEqualTo("[" + task + "]");
    } finally {
      deleteTasks(task);
    }
  }

  @Test
  void theChildGraphDrawsTheTaskTheTasksThatRunAfterItAndTheOtherTasksTheyWaitFor() throws Exception {
    String name = unique("Editor graph root");
    int task = createTask(editorTask(name));
    int other = createTask(manualTask(unique("Editor graph other")));
    int first = createTask(editorTask(unique("Editor graph child A"), task));
    int second = createTask(editorTask(unique("Editor graph child B"), task, other));
    try {
      List<JsonNode> graph = rows("getTaskEditorChildGraph",
        params().put("TskEdtMod", "edit").put("IdeTsk.selected", task));

      assertThat(graph).extracting(row -> row.get("id").asString()).containsExactlyInAnyOrder(task + "-",
        first + "-" + task, second + "-" + task, second + "-" + other, other + "-");
      JsonNode root = find(graph, "id", task + "-").orElseThrow();
      assertThat(root.get("name").asString()).isEqualTo(name);
      assertThat(root.get("rootTask").asInt()).isEqualTo(task);
      // A copy is a new task: it has no saved child yet
      assertThat(rows("getTaskEditorChildGraph", params().put("TskEdtMod", "copy").put("IdeTsk.selected", task)))
        .isEmpty();
    } finally {
      deleteTasks(second, first, other, task);
    }
  }

  private static List<JsonNode> toList(JsonNode actions) {
    List<JsonNode> list = new ArrayList<>();
    actions.forEach(list::add);
    return list;
  }

  /**
   * Parameters of a manual task as the editor sends them, with the parents it runs after in their order
   */
  private ObjectNode editorTask(String name, Integer... parents) {
    ObjectNode parameters = manualTask(name);
    parameters.set("TskPrn", ints(parents));
    return parameters;
  }

  /**
   * Parents of a task, by identifier. DpnOrd orders the children of a parent in its flows: the editor leaves it empty
   */
  private List<Integer> parentRows(int taskId) {
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AweSchTskDpn WHERE IdeTsk = ? AND DpnOrd IS NOT NULL",
      Integer.class, taskId)).isZero();
    return jdbc.queryForList("SELECT IdePrn FROM AweSchTskDpn WHERE IdeTsk = ? ORDER BY IdePrn", Integer.class,
      taskId);
  }

  private int count(String table, int taskId) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE IdeTsk = ?", Integer.class, taskId);
  }

  private static List<String> errorTitles(JsonNode actions) {
    List<String> titles = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("message".equals(action.get("type").asString())
        && "error".equals(action.get("parameters").get("type").asString())) {
        titles.add(action.get("parameters").get("title").asString());
      }
    }
    return titles;
  }
}
