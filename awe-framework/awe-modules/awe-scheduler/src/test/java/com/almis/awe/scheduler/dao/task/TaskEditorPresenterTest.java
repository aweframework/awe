package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskLaunchSummary;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.util.TaskSchedulePreview;
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Server side of the task editor (#993, S8): the parents a task can be launched after, with their condition, their
 * launch and the ones that cannot be chosen because they already run after the task, and the data of a task to
 * duplicate it. English texts of the module
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskEditorPresenterTest {

  private static final int EDITED = 5;

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private FlowRunRepository flowRunRepository;
  @Mock
  private TaskCatalogPresenter catalogPresenter;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskEditorPresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    Map<String, String> locales = englishLocales();
    // Labels of the stored fields, from the locales of the generic screens
    locales.putAll(Map.of("PARAMETER_DAYS_OF_WEEK", "Days of the week", "PARAMETER_DAYS", "Days", "PARAMETER_HOURS",
      "Hours", "PARAMETER_MINUTES", "Minutes", "PARAMETER_SECONDS", "Seconds", "PARAMETER_YEARS", "Years",
      "PARAMETER_MONTHS", "Months", "PARAMETER_CALENDAR", "Calendar"));
    given(serviceConfig.getLocale(anyString())).willAnswer(call -> locales.getOrDefault((String) call.getArgument(0),
      call.getArgument(0)));
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(call -> JsonNodeFactory.instance.objectNode().put("IdeTsk.selected", 99));
    // Task 5 runs after Delta (4) and Alpha (1), in this order; Beta (2) runs after 5 and Gamma (3) after Beta
    given(flowRunRepository.getDependencies()).willReturn(List.of(dependency(2, EDITED), dependency(EDITED, 4),
      dependency(3, 2), dependency(EDITED, 1)));
    given(catalogPresenter.getLaunches()).willReturn(List.of(
      launch(1, "Alpha", true, true, false, "After Import"),
      launch(2, "Beta", true, false, true, "After Edited"),
      launch(3, "Gamma", false, true, true, "After Beta"),
      launch(4, "Delta", true, false, false, "Every day at 03:00"),
      launch(EDITED, "Edited", true, false, false, "Launched by hand"),
      launch(6, "Epsilon", true, false, false, "Every hour")));
  }

  @Test
  void theParentsAreEveryOtherTaskChosenFirstAndTheTasksThatRunAfterItCannotBeChosen() throws Exception {
    List<Map<String, CellData>> rows = presenter().getParents(EDITED, null).getDataList().getRows();

    // Each group by name: the dependencies keep no order of the parents of a task
    assertThat(column(rows, "label")).containsExactly("Alpha", "Delta", "Epsilon", "Beta", "Gamma");
    assertThat(column(rows, "value")).containsExactly("1", "4", "6", "2", "3");
    assertThat(column(rows, "PrnGrp")).containsExactly("chosen", "chosen", "available", "blocked", "blocked");
    assertThat(column(rows, "PrnSel")).containsExactly("1", "1", "0", "0", "0");
    assertThat(rows).allSatisfy(row -> assertThat(row).doesNotContainKey("PrnOrd"));
    assertThat(column(rows, "PrnDis")).containsExactly("0", "0", "0", "1", "1");
    assertThat(column(rows, "PrnDisTxt")).containsExactly("", "", "",
      "Circular dependency: it already runs after this task", "Circular dependency: it already runs after this task");
    // The condition is the one of the parent: what its end has to be for its children to be launched
    assertThat(column(rows, "PrnCnd")).containsExactly("OK or warning", "Launches on OK", "Launches on OK",
      "OK or error", "OK, warning or error");
    assertThat(column(rows, "PrnWhnTxt")).containsExactly("After Import", "Every day at 03:00", "Every hour",
      "After Edited", "After Beta");
    assertThat(column(rows, "Act")).containsExactly("1", "1", "1", "1", "0");
  }

  @Test
  void aChosenParentThatAlreadyRunsAfterTheTaskStaysChosenSoItCanBeRemovedButNotAddedAgain() throws Exception {
    // Cyclic data saved before the editor: 5 runs after Delta (4) and Delta runs after 5; Gamma (3) runs after Delta
    given(flowRunRepository.getDependencies()).willReturn(List.of(dependency(EDITED, 4), dependency(4, EDITED),
      dependency(3, 4)));

    List<Map<String, CellData>> rows = presenter().getParents(EDITED, null).getDataList().getRows();

    assertThat(column(rows, "label")).containsExactly("Delta", "Alpha", "Beta", "Epsilon", "Gamma");
    assertThat(column(rows, "PrnGrp")).containsExactly("chosen", "available", "available", "available", "blocked");
    assertThat(column(rows, "PrnSel")).containsExactly("1", "0", "0", "0", "0");
    // Only adding is blocked: the chosen one can be unchecked, and it tells why it cannot be checked again
    assertThat(column(rows, "PrnDis")).containsExactly("0", "0", "0", "0", "1");
    assertThat(column(rows, "PrnDisTxt")).containsExactly("Circular dependency: it already runs after this task",
      "", "", "", "Circular dependency: it already runs after this task");
  }

  @Test
  void thePickerRowsCarryOnlyWhatEveryReaderMaySeeOfATask() throws Exception {
    List<Map<String, CellData>> rows = presenter().getParents(EDITED, null).getDataList().getRows();

    // Where a task runs, its files and who gets its report are for the editors only (the copy, scheduler.update)
    assertThat(rows).isNotEmpty().allSatisfy(row -> assertThat(row.keySet()).containsExactlyInAnyOrder("value",
      "label", "Act", "PrnCnd", "PrnWhnTxt", "PrnWhnIcn", "PrnWhnSub", "PrnGrp", "PrnSel", "PrnDis", "PrnDisTxt"));
  }

  @Test
  void aNewTaskCanRunAfterAnyTaskAndItsCopyStartsWithTheParentsOfTheOriginal() throws Exception {
    List<Map<String, CellData>> newTask = presenter().getParents(null, null).getDataList().getRows();
    List<Map<String, CellData>> copy = presenter().getParents(null, EDITED).getDataList().getRows();

    assertThat(column(newTask, "label")).containsExactly("Alpha", "Beta", "Delta", "Edited", "Epsilon", "Gamma");
    assertThat(column(newTask, "PrnDis")).containsOnly("0");
    assertThat(column(newTask, "PrnSel")).containsOnly("0");
    assertThat(column(copy, "label")).containsExactly("Alpha", "Delta", "Beta", "Edited", "Epsilon", "Gamma");
    assertThat(column(copy, "PrnGrp")).containsExactly("chosen", "chosen", "available", "available", "available",
      "available");
  }

  @Test
  void theCopyOfATaskKeepsItsDataAndParentsWithANewInactiveNameAndWithoutIdNorWebhookHook() throws Exception {
    Map<String, CellData> task = new HashMap<>();
    task.put("Ide", new CellData(EDITED));
    task.put("taskId", new CellData(EDITED));
    task.put("Nam", new CellData("Nightly import"));
    task.put("name", new CellData("Nightly import"));
    task.put("Act", new CellData(1));
    task.put("active", new CellData(true));
    task.put("Des", new CellData("Imports the statements"));
    task.put("TypLch", new CellData(4));
    task.put("HokKey", new CellData("bank-answer"));
    task.put("HokAct", new CellData(1));
    task.put("HokSec", new CellData("bank.secret"));
    task.put("HokTyp", new CellData("payment"));
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class))).willReturn(data(List.of(task)));

    List<Map<String, CellData>> rows = presenter().getCopy(EDITED).getDataList().getRows();

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("taskData"), parameters.capture());
    assertThat(parameters.getValue().get("taskId").asInt()).isEqualTo(EDITED);
    assertThat(parameters.getValue().has("IdeTsk.selected")).isFalse();
    assertThat(rows).hasSize(1);
    Map<String, CellData> copy = rows.get(0);
    assertThat(copy.get("Nam").getStringValue()).isEqualTo("Nightly import (copy)");
    assertThat(copy.get("name").getStringValue()).isEqualTo("Nightly import (copy)");
    assertThat(copy.get("Act").getStringValue()).isEqualTo("0");
    assertThat(copy.get("active").getObjectValue()).isEqualTo(false);
    assertThat(copy.get("Des").getStringValue()).isEqualTo("Imports the statements");
    assertThat(copy.get("TypLch").getStringValue()).isEqualTo("4");
    for (String field : List.of("Ide", "taskId", "HokKey", "HokAct", "HokSec", "HokTyp")) {
      assertThat(copy.get(field).getObjectValue()).as(field).isNull();
    }
    assertThat(copy.get("TskPrn").getObjectValue().toString()).isEqualTo("[1,4]");
  }

  @Test
  void theNameOfACopyNeverExceedsTheLengthOfATaskNameAndKeepsItsSuffix() throws Exception {
    String longName = "Reconcile the bank statements of the day";
    Map<String, CellData> task = new HashMap<>();
    task.put("Nam", new CellData(longName));
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class))).willReturn(data(List.of(task)));

    String copy = presenter().getCopy(EDITED).getDataList().getRows().get(0).get("Nam").getStringValue();

    assertThat(longName).hasSize(40);
    assertThat(copy).isEqualTo("Reconcile the bank statements of (copy)").hasSize(39);
  }

  @Test
  void aTaskThatDoesNotExistHasNoCopy() throws Exception {
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class))).willReturn(data(List.of()));

    assertThat(presenter().getCopy(EDITED).getDataList().getRows()).isEmpty();
  }

  @Test
  void theEditorOfAManualTaskWithParentsRunsAfterOtherTasksWithTheTitleOfItsMode() throws Exception {
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class)))
      .willAnswer(call -> data(List.of(task("Edited", 1, 0))));

    Map<String, CellData> edit = presenter().getEditor("edit", EDITED).getDataList().getRows().get(0);
    Map<String, CellData> copy = presenter().getEditor("copy", EDITED).getDataList().getRows().get(0);

    assertThat(edit.get("TskEdtMod").getStringValue()).isEqualTo("edit");
    assertThat(edit.get("TskPrn").getObjectValue().toString()).isEqualTo("[1,4]");
    // A manual task with parents runs after other tasks (3, the old dependency launch type)
    assertThat(edit.get("TskWhn").getStringValue()).isEqualTo("3");
    assertThat(edit.get("TskEdtTit").getStringValue()).isEqualTo("Edit task #5");
    assertThat(edit.get("TskEdtSub").getStringValue()).isEqualTo("Edited · the changes apply to the next execution");
    assertThat(edit.get("TskEdtChp").getStringValue()).isEqualTo("Active");
    assertThat(edit.get("TskEdtChpTon").getStringValue()).isEqualTo("success");
    assertThat(edit.get("Nam").getStringValue()).isEqualTo("Edited");
    // The copy: a new task with the data and the parents of the original, inactive
    assertThat(copy.get("TskEdtMod").getStringValue()).isEqualTo("copy");
    assertThat(copy.get("TskEdtTit").getStringValue()).isEqualTo("Duplicate task");
    assertThat(copy.get("Nam").getStringValue()).isEqualTo("Edited (copy)");
    assertThat(copy.get("Ide").getObjectValue()).isNull();
    assertThat(copy.get("TskWhn").getStringValue()).isEqualTo("3");
    assertThat(copy.get("TskEdtChp").getStringValue()).isEqualTo("Inactive");
    assertThat(copy.get("TskEdtChpTon").getStringValue()).isEqualTo("neutral");
  }

  @Test
  void theEditorKeepsTheLaunchTypeOfATaskAndANewTaskOrAMissingOneIsAManualNewTask() throws Exception {
    // A scheduled task with parents keeps its launch type (its parents are kept as they are)
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class)))
      .willReturn(data(List.of(task("Edited", 1, 1))));
    Map<String, CellData> scheduled = presenter().getEditor("edit", EDITED).getDataList().getRows().get(0);
    Map<String, CellData> created = presenter().getEditor("new", EDITED).getDataList().getRows().get(0);
    Map<String, CellData> unknown = presenter().getEditor("edit", null).getDataList().getRows().get(0);
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class))).willReturn(data(List.of()));
    Map<String, CellData> missing = presenter().getEditor("edit", 77).getDataList().getRows().get(0);

    assertThat(scheduled.get("TskWhn").getStringValue()).isEqualTo("1");
    for (Map<String, CellData> row : List.of(created, unknown, missing)) {
      assertThat(row.get("TskEdtMod").getStringValue()).isEqualTo("new");
      assertThat(row.get("TskWhn").getStringValue()).isEqualTo("0");
      assertThat(row.get("TskEdtTit").getStringValue()).isEqualTo("New task");
      assertThat(row).doesNotContainKey("Nam");
      // The initial values of the form: the target fills every field, an empty one would choose the first option
      assertThat(row.get("Act").getStringValue()).isEqualTo("1");
      assertThat(row.get("TypLch").getStringValue()).isEqualTo("0");
      assertThat(row.get("HokAct").getStringValue()).isEqualTo("1");
      assertThat(row.get("RepSndSta").getObjectValue().toString()).isEqualTo("[\"1\",\"2\"]");
    }
  }

  @Test
  void thePickerOptionsCarryTheirGroupWhetherTheyCanBeChosenAndTheirConditionWithTheReason() throws Exception {
    List<Map<String, CellData>> rows = presenter().getEditorParents("edit", EDITED).getDataList().getRows();
    List<Map<String, CellData>> copy = presenter().getEditorParents("copy", EDITED).getDataList().getRows();

    assertThat(column(rows, "label")).containsExactly("Alpha", "Delta", "Epsilon", "Beta", "Gamma");
    assertThat(column(rows, "group")).containsExactly("SCREEN_TEXT_TASK_EDITOR_GROUP_CHOSEN",
      "SCREEN_TEXT_TASK_EDITOR_GROUP_CHOSEN", "SCREEN_TEXT_TASK_EDITOR_GROUP_AVAILABLE",
      "SCREEN_TEXT_TASK_EDITOR_GROUP_BLOCKED", "SCREEN_TEXT_TASK_EDITOR_GROUP_BLOCKED");
    assertThat(column(rows, "disabled")).containsExactly("false", "false", "false", "true", "true");
    assertThat(column(rows, "description")).containsExactly("OK or warning", "Launches on OK", "Launches on OK",
      "OK or error · Circular dependency: it already runs after this task",
      "OK, warning or error · Circular dependency: it already runs after this task");
    // A copy is a new task: nothing runs after it, the parents of the original are chosen
    assertThat(column(copy, "label")).containsExactly("Alpha", "Delta", "Beta", "Edited", "Epsilon", "Gamma");
    assertThat(column(copy, "disabled")).containsOnly("false");
  }

  @Test
  void theParametersAreTheOnesOfTheTaskWithoutIdentifiersInACopyAndNoneForANewTask() throws Exception {
    Map<String, CellData> parameter = new HashMap<>();
    parameter.put("id", new CellData(31));
    parameter.put("IdePar", new CellData(31));
    parameter.put("ParNam", new CellData("secondsToWait"));
    parameter.put("ParVal", new CellData("3"));
    given(queryService.launchPrivateQuery(eq("taskParameters"), any(ObjectNode.class)))
      .willAnswer(call -> data(List.of(new HashMap<>(parameter))));

    List<Map<String, CellData>> edit = presenter().getEditorParameters("edit", EDITED).getDataList().getRows();
    List<Map<String, CellData>> copy = presenter().getEditorParameters("copy", EDITED).getDataList().getRows();
    List<Map<String, CellData>> created = presenter().getEditorParameters("new", EDITED).getDataList().getRows();

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, org.mockito.Mockito.times(2)).launchPrivateQuery(eq("taskParameters"), parameters.capture());
    assertThat(parameters.getValue().get("taskId").asInt()).isEqualTo(EDITED);
    assertThat(parameters.getValue().has("IdeTsk.selected")).isFalse();
    assertThat(column(edit, "IdePar")).containsExactly("31");
    assertThat(column(copy, "IdePar")).containsExactly("");
    assertThat(column(copy, "ParNam")).containsExactly("secondsToWait");
    assertThat(created).isEmpty();
  }

  @Test
  void theGraphDrawsTheChosenParentsJoiningIntoTheTaskAndTheTasksThatRunAfterItAsSaved() throws Exception {
    List<Map<String, CellData>> parents = presenter().getEditorGraph("edit", EDITED, List.of(6, 1, 6, 99), "parents")
      .getDataList().getRows();
    List<Map<String, CellData>> children = presenter().getEditorGraph("edit", EDITED, List.of(), "children")
      .getDataList().getRows();
    List<Map<String, CellData>> newParents = presenter().getEditorGraph("new", null, List.of(4), "parents")
      .getDataList().getRows();
    List<Map<String, CellData>> newChildren = presenter().getEditorGraph("copy", EDITED, List.of(4), "children")
      .getDataList().getRows();

    // The chosen parents, each once and only known tasks, joining into the task (its root)
    assertThat(column(parents, "id")).containsExactly("5-6", "6-", "5-1", "1-");
    assertThat(column(parents, "rootTask")).containsExactly("5", "", "5", "");
    assertThat(column(parents, "onWarning")).containsExactly("0", "0", "0", "1");
    assertThat(column(parents, "launchLabel")).containsExactly("Launched by hand", "Every hour", "Launched by hand",
      "After Import");
    // The tasks that run after it as saved: Beta (2) runs after the task
    assertThat(column(children, "id")).containsExactly("5-", "2-5");
    assertThat(column(children, "name")).containsExactly("Edited", "Beta");
    // A new task or a copy is the node 0, «This task», and has no task after it yet
    assertThat(column(newParents, "id")).containsExactly("0-4", "4-");
    assertThat(column(newParents, "name")).containsExactly("This task", "Delta");
    assertThat(newChildren).isEmpty();
  }

  @Test
  void aSaveAnswersWithItsMessageAndTheSavedTaskAsTheTaskOfTheScreen() {
    ServiceData saved = presenter().saved(42);

    assertThat(saved.getTitle()).isEqualTo("Task saved");
    assertThat(saved.getMessage()).isEqualTo("Task #42 has been saved");
    assertThat(saved.getClientActionList()).hasSize(1);
    assertThat(saved.getClientActionList().get(0).getType()).isEqualTo("select");
    assertThat(saved.getClientActionList().get(0).getTarget()).isEqualTo("IdeTsk.selected");
    assertThat(saved.getClientActionList().get(0).getParameters().get("values").toString()).contains("42");
  }

  @Test
  void theEditorOpensAScheduledTaskOnThePresetItsStoredFieldsMatchAndANewTaskOnDaily() throws Exception {
    Map<String, CellData> weekly = task("Edited", 1, 1);
    weekly.put("repeatType", new CellData(7));
    weekly.put("hourList", new CellData(List.of("8")));
    weekly.put("minuteList", new CellData(List.of("5")));
    weekly.put("weekDayList", new CellData(List.of("6", "0")));
    given(queryService.launchPrivateQuery(eq("taskData"), any(ObjectNode.class))).willReturn(data(List.of(weekly)));

    Map<String, CellData> edit = presenter().getEditor("edit", EDITED).getDataList().getRows().get(0);
    Map<String, CellData> created = presenter().getEditor("new", null).getDataList().getRows().get(0);

    assertThat(edit.get("SchPre").getStringValue()).isEqualTo("WEK");
    assertThat(edit.get("SchTim").getStringValue()).isEqualTo("08:05:00");
    assertThat(edit.get("SchWkd").getObjectValue().toString()).isEqualTo("[\"6\",\"0\"]");
    assertThat(edit.get("SchWkdPre").getStringValue()).isEqualTo("WEK");
    // What «When» shows comes with the task, so the fields it shows are in sight when the editor opens
    assertThat(edit.get("SchVw").getStringValue()).isEqualTo("WEK");
    assertThat(created.get("SchVw").getStringValue()).isEqualTo("NON");
    // A new task (manual until «Scheduled» is chosen) starts on a daily repetition, every day of the week
    assertThat(created.get("SchPre").getStringValue()).isEqualTo("DAY");
    assertThat(created.get("SchWkd").getObjectValue().toString()).isEqualTo("[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"0\"]");
    assertThat(created.get("SchWkdPre").getStringValue()).isEqualTo("DAY");
  }

  @Test
  void theSchedulePreviewTellsTheRepetitionWithItsKeyPartsBoldAndListsTheNextRunsAsTiles() {
    Schedule workdays = new Schedule().setRepeatType(7).setWeekDayList(List.of("1", "2", "3", "4", "5"))
      .setHourList(List.of("22")).setMinuteList(List.of("30")).setSecondList(List.of("0")).setCalendarId(3)
      .setInitialDate(java.sql.Date.valueOf("2026-11-17")).setInitialTime("00:00:00");
    List<Map<String, CellData>> next = List.of(run("17/11/2026", "22:30:00"), run("18/11/2026", "22:30:00"),
      run("19/11/2026", "22:30:00"), run("20/11/2026", "22:30:00"), run("23/11/2026", "22:30:00"));
    // A past holiday and a Saturday one are not skipped by a repetition from Monday to Friday
    List<LocalDate> holidays = List.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 8));

    Map<String, CellData> preview = presenter().schedulePreview(new TaskSchedulePreview.Input(workdays, "WRK",
      "Holidays ES", holidays, next, true), LocalDateTime.of(2026, 11, 17, 10, 0)).getDataList().getRows().get(0);

    assertThat(preview.get("title").getStringValue()).isEqualTo("[{\"text\":\"Runs \"},"
      + "{\"text\":\"from Monday to Friday at 22:30\",\"strong\":true},"
      + "{\"text\":\", except the holidays of the calendar \"},{\"text\":\"Holidays ES\",\"strong\":true},"
      + "{\"text\":\", from 17/11/2026 and with no end date.\"}]");
    assertThat(preview.get("tilesLabel").getStringValue()).isEqualTo("Next 5 runs");
    assertThat(preview.get("tiles").getStringValue()).isEqualTo("[{\"label\":\"Tue 17/11\",\"value\":\"22:30\",\"hint\":\"today\"},"
      + "{\"label\":\"Wed 18/11\",\"value\":\"22:30\",\"hint\":\"tomorrow\"},"
      + "{\"label\":\"Thu 19/11\",\"value\":\"22:30\"},{\"label\":\"Fri 20/11\",\"value\":\"22:30\"},"
      + "{\"label\":\"Mon 23/11\",\"value\":\"22:30\",\"hint\":\"skips Sat and Sun\"}]");
    assertThat(preview.get("description").getStringValue()).isEqualTo("First holiday it skips: Tue 08/12/2026 (Holidays ES)");
    assertThat(preview.get("detailsLabel").getStringValue()).isEqualTo("View details");
    assertThat(preview.get("details").getStringValue()).startsWith("Days of the week: Mon, Tue, Wed, Thu and Fri · Hours: 22"
      + " · Minutes: 30 · Seconds: 0 · Years, Months, Weeks and Days: all · Calendar: Holidays ES. Presets: ");
  }

  @Test
  void aRunAfterAHolidaySaysItSkipsItAndARunWithSecondsShowsThem() {
    Schedule daily = new Schedule().setRepeatType(7).setHourList(List.of("6")).setMinuteList(List.of("0"))
      .setSecondList(List.of("15"));
    List<Map<String, CellData>> next = List.of(run("07/12/2026", "06:00:15"), run("09/12/2026", "06:00:15"),
      run("10/01/2027", "06:00:15"));

    Map<String, CellData> preview = presenter().schedulePreview(new TaskSchedulePreview.Input(daily, "CUS",
      "Holidays ES", List.of(LocalDate.of(2026, 12, 8)), next, true), LocalDateTime.of(2026, 11, 17, 10, 0))
      .getDataList().getRows().get(0);

    // The seconds show when they are not 0, in the sentence and in the runs
    assertThat(preview.get("title").getStringValue()).startsWith("[{\"text\":\"Runs \"},"
      + "{\"text\":\"every day at 06:00:15\",\"strong\":true}");
    assertThat(preview.get("tiles").getStringValue()).isEqualTo("[{\"label\":\"Mon 07/12\",\"value\":\"06:00:15\"},"
      + "{\"label\":\"Wed 09/12\",\"value\":\"06:00:15\",\"hint\":\"skips a holiday\"},"
      + "{\"label\":\"Sun 10/01/2027\",\"value\":\"06:00:15\"}]");
  }

  @Test
  void aCustomRepetitionThatAPresetCanSaySaysWhichOne() {
    Schedule workdays = new Schedule().setRepeatType(7).setWeekDayList(List.of("1", "2", "3", "4", "5"))
      .setHourList(List.of("22")).setMinuteList(List.of("30")).setSecondList(List.of("0"));

    Map<String, CellData> custom = presenter().schedulePreview(new TaskSchedulePreview.Input(workdays, "CUS",
      null, List.of(), List.of(), true), LocalDateTime.of(2026, 11, 17, 10, 0)).getDataList().getRows().get(0);
    Map<String, CellData> preset = presenter().schedulePreview(new TaskSchedulePreview.Input(workdays, "WRK",
      null, List.of(), List.of(), true), LocalDateTime.of(2026, 11, 17, 10, 0)).getDataList().getRows().get(0);

    assertThat(custom.get("title").getStringValue()).endsWith("{\"text\":\". It matches the preset “Workdays”.\"}]");
    assertThat(preset.get("title").getStringValue()).endsWith("{\"text\":\".\"}]");
  }

  @Test
  void aSchedulePreviewWithoutRunsSaysWhy() {
    Schedule daily = new Schedule().setRepeatType(7).setHourList(List.of("6")).setEndDate(java.sql.Date.valueOf("2020-01-01"));

    Map<String, CellData> none = presenter().schedulePreview(new TaskSchedulePreview.Input(daily, "DAY", null,
      List.of(), List.of(), true), LocalDateTime.of(2026, 11, 17, 10, 0)).getDataList().getRows().get(0);
    Map<String, CellData> incomplete = presenter().schedulePreview(new TaskSchedulePreview.Input(
      new Schedule().setRepeatType(1), "MIN", null, List.of(), List.of(), false), LocalDateTime.of(2026, 11, 17, 10, 0))
      .getDataList().getRows().get(0);

    Map<String, CellData> future = presenter().schedulePreview(new TaskSchedulePreview.Input(
      daily.setEndDate(java.sql.Date.valueOf("2027-01-01")), "DAY", null, List.of(), List.of(), true),
      LocalDateTime.of(2026, 11, 17, 10, 0)).getDataList().getRows().get(0);

    assertThat(none.get("title").getStringValue()).isEqualTo("[{\"text\":\"Runs \"},"
      + "{\"text\":\"every day at 06:00\",\"strong\":true},{\"text\":\", until 01/01/2020.\"}]");
    // An end that has passed: it never runs again
    assertThat(none.get("description").getStringValue()).isEqualTo("It will not run again: its end (01/01/2020) has passed");
    assertThat(none.get("tiles").getStringValue()).isEqualTo("[]");
    assertThat(future.get("description").getStringValue()).isEqualTo("It has no next runs: check the range of dates");
    assertThat(incomplete.get("title").getStringValue()).isEqualTo("Fill in the repetition to see when it runs");
    assertThat(incomplete.get("description").getStringValue()).isEmpty();
    assertThat(incomplete.get("tiles").getStringValue()).isEqualTo("[]");
    assertThat(incomplete.get("details").getStringValue()).isEmpty();
  }

  @Test
  void theChoiceCardsOfTheEditorBringTheIconAndTheDescriptionOfEachOption() {
    List<Map<String, CellData>> when = presenter().getEditorOptions("when").getDataList().getRows();
    List<Map<String, CellData>> execution = presenter().getEditorOptions("execution").getDataList().getRows();

    assertThat(column(when, "value")).containsExactly("0", "1", "2", "3", "4");
    assertThat(column(when, "label")).containsExactly("ENUM_TASK_EDITOR_WHEN_MANUAL", "ENUM_TASK_EDITOR_WHEN_SCHEDULED",
      "ENUM_TASK_EDITOR_WHEN_FILE", "ENUM_TASK_EDITOR_WHEN_AFTER", "ENUM_TASK_EDITOR_WHEN_WEBHOOK");
    assertThat(column(when, "icon")).containsExactly("lucide:hand", "lucide:calendar-clock", "lucide:file-down",
      "lucide:git-merge", "lucide:webhook");
    assertThat(column(when, "description")).containsExactly("SCREEN_TEXT_TASK_EDITOR_WHEN_MANUAL_HELP",
      "SCREEN_TEXT_TASK_EDITOR_WHEN_SCHEDULED_HELP", "SCREEN_TEXT_TASK_EDITOR_WHEN_FILE_HELP",
      "SCREEN_TEXT_TASK_EDITOR_WHEN_AFTER_HELP", "SCREEN_TEXT_TASK_EDITOR_WHEN_WEBHOOK_HELP");
    assertThat(column(execution, "value")).containsExactly("1", "0");
    assertThat(column(execution, "label")).containsExactly("ENUM_EXECUTE_MAINTAIN", "ENUM_EXECUTE_COMMAND");
    assertThat(column(execution, "icon")).containsExactly("lucide:database", "lucide:terminal");
    assertThat(column(execution, "description")).containsExactly("SCREEN_TEXT_TASK_EDITOR_EXECUTE_MAINTAIN_HELP",
      "SCREEN_TEXT_TASK_EDITOR_EXECUTE_COMMAND_HELP");
    assertThat(presenter().getEditorOptions("other").getDataList().getRows()).isEmpty();
  }

  @Test
  void theDayTogglesOfTheEditorAreTheDaysFromMondayWithTheirInitialAndTheirFullName() {
    List<Map<String, CellData>> days = presenter().getEditorOptions("weekdays").getDataList().getRows();

    assertThat(column(days, "value")).containsExactly("1", "2", "3", "4", "5", "6", "0");
    assertThat(column(days, "label")).containsExactly("ENUM_TASK_EDITOR_DAY_INITIAL_1", "ENUM_TASK_EDITOR_DAY_INITIAL_2",
      "ENUM_TASK_EDITOR_DAY_INITIAL_3", "ENUM_TASK_EDITOR_DAY_INITIAL_4", "ENUM_TASK_EDITOR_DAY_INITIAL_5",
      "ENUM_TASK_EDITOR_DAY_INITIAL_6", "ENUM_TASK_EDITOR_DAY_INITIAL_0");
    assertThat(column(days, "title")).containsExactly("PARAMETER_WEEKDAY_MONDAY", "PARAMETER_WEEKDAY_TUESDAY",
      "PARAMETER_WEEKDAY_WEDNESDAY", "PARAMETER_WEEKDAY_THURSDAY", "PARAMETER_WEEKDAY_FRIDAY",
      "PARAMETER_WEEKDAY_SATURDAY", "PARAMETER_WEEKDAY_SUNDAY");
  }

  private static Map<String, CellData> run(String date, String time) {
    Map<String, CellData> row = new HashMap<>();
    row.put("nextDate", new CellData(date));
    row.put("nextTime", new CellData(time));
    return row;
  }

  private static Map<String, CellData> task(String name, int active, int launchType) {
    Map<String, CellData> task = new HashMap<>();
    task.put("Ide", new CellData(EDITED));
    task.put("Nam", new CellData(name));
    task.put("Act", new CellData(active));
    task.put("TypLch", new CellData(launchType));
    return task;
  }

  private TaskEditorPresenter presenter() {
    if (presenter == null) {
      presenter = new TaskEditorPresenter(queryService, queryUtil, flowRunRepository, catalogPresenter, serviceConfig);
    }
    return presenter;
  }

  private static TaskDependency dependency(int taskId, int parentId) {
    return new TaskDependency().setTaskId(taskId).setParentId(parentId);
  }

  private static TaskLaunchSummary launch(int id, String name, boolean active, boolean onWarning, boolean onError,
                                          String when) {
    return new TaskLaunchSummary(id, name, active, onWarning, onError,
      new TaskWhenSummary.When(TaskLaunchKind.MANUAL, "lucide:hand", when, "detail of " + name));
  }

  private static ServiceData data(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  private static List<String> column(List<Map<String, CellData>> rows, String field) {
    return rows.stream().map(row -> row.get(field) == null ? null : row.get(field).getStringValue()).toList();
  }

  private static Map<String, String> englishLocales() throws Exception {
    Map<String, String> locales = new HashMap<>();
    try (InputStream stream = TaskEditorPresenterTest.class.getClassLoader()
      .getResourceAsStream("application/awe-scheduler/locale/Locale-en-GB.xml")) {
      NodeList entries = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        .getElementsByTagName("locale");
      for (int index = 0; index < entries.getLength(); index++) {
        Element entry = (Element) entries.item(index);
        locales.put(entry.getAttribute("name"), entry.getAttribute("value"));
      }
    }
    return locales;
  }
}
