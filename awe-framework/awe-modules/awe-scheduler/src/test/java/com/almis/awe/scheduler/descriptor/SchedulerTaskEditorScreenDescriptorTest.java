package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.util.TaskSchedulePreset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the task editor ({@code scheduler-task-editor}, #993 S8): one screen for a new task, its edition
 * and its copy, whose mode and task travel in hidden criteria kept on reload; its sections in order with a navigation
 * that scrolls to each of them; its saves (the new or the edit maintain of the editor, by mode, for the editors only)
 * send every value their maintain reads and open the task screen; «When» saves «After other tasks» as a manual task with
 * its parents, asks before clearing them and draws the chosen parents; and every query of the editor declares its
 * permission.
 */
class SchedulerTaskEditorScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String TASK = "IdeTsk.selected";
  private static final List<String> SECTIONS = List.of("SecTskWht", "SecTskPar", "SecTskWhn", "SecTskDep", "SecTskBeh",
    "SecTskNot");

  private static Document screen;
  private static Document fields;
  private static Document parameters;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-task-editor.xml");
    fields = parse("screen/scheduler-task-fields.xml");
    parameters = parse("screen/scheduler-parameters.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
  }

  @Test
  void theModeAndTheTaskTravelInHiddenCriteriaKeptOnReloadAndFillTheScreen() {
    for (String criterion : List.of(TASK, "TskEdtMod")) {
      Element hidden = byId(screen, "criteria", criterion);
      assertEquals("hidden", hidden.getAttribute("component"), criterion);
      assertEquals(criterion, hidden.getAttribute("variable"), criterion);
      assertEquals("true", hidden.getAttribute("keep-on-reload"), criterion);
    }
    assertEquals("getTaskEditor", screen.getDocumentElement().getAttribute("target"));
    Element target = byId(queries, "query", "getTaskEditor");
    assertEquals("scheduler.update", target.getAttribute("permission"));
    assertEquals(List.of("TskEdtMod", TASK), variableNamesOf(target));
    // The task travels only in its hidden criterion: no grid sends IdeTsk
    for (Element column : all(screen, "column")) {
      assertFalse("IdeTsk".equals(column.getAttribute("name")), "No grid sends IdeTsk");
    }
    // The header card reads the same service with the names of the card attributes
    Element header = byId(queries, "query", byId(screen, "card", "CrdTskEdt").getAttribute("target-action"));
    assertEquals("getTaskEditor", header.getAttribute("service"));
    assertEquals(List.of("title", "subtitle", "chip", "chipTone"), fieldsOf(header));
  }

  @Test
  void theSectionsAreCardsInOrderAndEachNavigationButtonScrollsToItsCard() {
    List<String> cards = new ArrayList<>();
    for (Element card : children(byClass(screen, "awe-editor-sections"), "card")) {
      cards.add(card.getAttribute("id"));
      assertFalse(card.getAttribute("title").isEmpty(), card.getAttribute("id"));
    }
    assertEquals(SECTIONS, cards);
    List<String> targets = new ArrayList<>();
    for (Element button : children(byId(screen, "tag", "TskEdtNav"), "button")) {
      List<String> actions = actionsOf(button);
      assertEquals(1, actions.size(), button.getAttribute("id"));
      targets.add(actions.get(0));
    }
    assertEquals(SECTIONS.stream().map(section -> "scroll-to:" + section).toList(), targets);
    // «Change what it depends on» opens the editor on «When»
    Element section = byId(screen, "criteria", "TskEdtSec");
    assertEquals("TskEdtSec", section.getAttribute("variable"));
    assertEquals(List.of("scroll-to:SecTskWhn"), dependencyActionsOf(children(section, "dependency").get(0)));
  }

  @Test
  void eachModeSavesWithItsMaintainForTheEditorsAndOpensTheTaskScreen() {
    Element create = byId(screen, "button", "ButTskEdtNew");
    Element update = byId(screen, "button", "ButTskEdtUpd");
    assertEquals(List.of("validate:", "server:NewSchedulerTaskEditor", "screen:scheduler-task"), actionsOf(create));
    assertEquals(List.of("validate:", "server:UpdateSchedulerTaskEditor", "screen:scheduler-task"), actionsOf(update));
    assertEquals("ne", showCondition(create, "TskEdtMod", "edit"));
    assertEquals("eq", showCondition(update, "TskEdtMod", "edit"));
    for (Element button : List.of(create, update)) {
      assertEquals("scheduler.update", button.getAttribute("permission"), button.getAttribute("id"));
    }
    // The save answers with the saved task as the task of the screen, so the task screen shows it
    for (String target : List.of("NewSchedulerTaskEditor", "UpdateSchedulerTaskEditor")) {
      Element save = byAttribute(maintain.getDocumentElement(), "target", "name", target);
      assertNotNull(save, target);
      assertEquals("scheduler.update", save.getAttribute("permission"), target);
      List<Element> steps = children(save, "serve");
      assertEquals("getTaskEditorSaved", steps.get(steps.size() - 1).getAttribute("service"), target);
      // The preset of the «Repetition» of a scheduled task stores its fields in the launch row, before the commit
      List<String> services = steps.stream().map(step -> step.getAttribute("service")).toList();
      assertTrue(services.indexOf("applyTaskEditorSchedule") >= 0
        && services.indexOf("applyTaskEditorSchedule") < services.indexOf("saveTaskParents"), target);
    }
    assertEquals(List.of("back:"), actionsOf(byId(screen, "button", "ButTskEdtCan")));
  }

  @Test
  void theScreenSendsEveryValueTheMaintainsOfTheEditorRead() {
    Set<String> sent = new HashSet<>(idsOf(screen));
    // The shared fields: what the task runs and the report of the old task screens (the editor lays out its launch fields)
    sent.addAll(idsOf(fields));
    sent.addAll(idsOf(source(parameters, "Report")));
    assertNotNull(byAttribute(screen.getDocumentElement(), "include", "target-source", "Execution"));
    assertNotNull(byAttribute(screen.getDocumentElement(), "include", "target-source", "Report"));
    for (String target : List.of("NewSchedulerTaskEditor", "UpdateSchedulerTaskEditor")) {
      Element save = byAttribute(maintain.getDocumentElement(), "target", "name", target);
      for (Element variable : all(save, "variable")) {
        String name = variable.getAttribute("name");
        // IdeTsk and IdePar are the keys the inserts generate
        if (!"true".equals(variable.getAttribute("optional")) && !Set.of("IdeTsk", "IdeTskLch").contains(name)) {
          assertTrue(sent.contains(name), target + " reads " + name + ", which the editor does not send");
        }
      }
    }
  }

  @Test
  void afterOtherTasksIsAManualTaskWithItsParentsAndLeavingItAsksBeforeClearingThem() {
    // The choices are cards with the icon and the description of each option (TaskEditorPresenter#getEditorOptions)
    Element when = byId(screen, "criteria", "TskWhn");
    assertTrue(when.getAttribute("style").contains("awe-choice-cards"));
    assertEquals("query", when.getAttribute("initial-load"));
    Element choices = byId(queries, "query", when.getAttribute("target-action"));
    assertEquals(List.of("value", "label", "icon", "description"), fieldsOf(choices));
    // The launch type saved: «After other tasks» (3) is a manual task (0), the others their own launch type
    assertEquals("hidden", byId(screen, "criteria", "TypLch").getAttribute("component"));
    Map<String, String> launchTypes = new LinkedHashMap<>();
    for (Element dependency : children(when, "dependency")) {
      Element set = byAttribute(dependency, "dependency-action", "target", "TypLch");
      if (set != null) {
        launchTypes.put(children(dependency, "dependency-element").get(0).getAttribute("value"), set.getAttribute("value"));
      }
    }
    assertEquals(Map.of("0", "0", "1", "1", "2", "2", "3", "0", "4", "4"), launchTypes);

    Element parents = byId(screen, "criteria", "TskPrn");
    assertEquals("select-multiple", parents.getAttribute("component"));
    // Optional: an empty select multiple that is not optional chooses its first value by itself
    assertEquals("true", parents.getAttribute("optional"));
    Element options = byId(queries, "query", parents.getAttribute("target-action"));
    assertTrue(fieldsOf(options).containsAll(List.of("value", "label", "group", "disabled", "description")));
    assertEquals(List.of("TskEdtMod", TASK), variableNamesOf(options));
    assertTrue(dependencyActionsOf(children(parents, "dependency").get(1)).contains("filter:TskEdtPrnGph"));
    assertTrue(variableNamesOf(byId(queries, "query", "getTaskEditorParentGraph")).contains("TskPrn"));

    // Leaving it with parents chosen opens the dialog; closing it without clearing goes back to «After other tasks»
    List<Element> dependencies = children(when, "dependency");
    Element ask = dependencies.stream().filter(dependency -> dependencyActionsOf(dependency)
      .equals(List.of("dialog:DlgTskPrnClr"))).findFirst().orElseThrow();
    assertEquals("ne", elementCondition(ask, "TskWhn"));
    Element back = dependencies.stream().filter(dependency -> dependencyActionsOf(dependency)
      .equals(List.of("value:TskWhn"))).findFirst().orElseThrow();
    assertEquals("close", byAttribute(back, "dependency-element", "id", "DlgTskPrnClr").getAttribute("event"));
    assertEquals("3", children(back, "dependency-action").get(0).getAttribute("value"));
    assertEquals(List.of("reset:TskPrn", "close:DlgTskPrnClr"), actionsOf(byId(screen, "button", "ButTskPrnClr")));
    assertEquals(List.of("value:TskWhn", "close:DlgTskPrnClr"), actionsOf(byId(screen, "button", "ButTskPrnKep")));
  }

  @Test
  void theRepetitionPresetsFillTheStoredFieldsAndThePreviewReadsTheScreen() {
    Element presets = byId(screen, "criteria", "SchPre");
    assertEquals("TskEdtPre", presets.getAttribute("target-action"));
    assertEquals(List.of(TaskSchedulePreset.MINUTES, TaskSchedulePreset.HOURS, TaskSchedulePreset.DAILY,
        TaskSchedulePreset.WORKDAYS, TaskSchedulePreset.WEEKLY, TaskSchedulePreset.MONTHLY, TaskSchedulePreset.ONCE,
        TaskSchedulePreset.CUSTOM),
      children(byId(enumerated, "group", "TskEdtPre"), "option").stream().map(option -> option.getAttribute("value"))
        .toList());
    // The fields of the presets and the stored fields they write (custom) are filled when a task opens
    assertTrue(fieldsOf(byId(queries, "query", "getTaskEditor")).containsAll(List.of("SchPre", "SchEvy", "SchTim",
      "SchWkd", "SchMdy", "RptTyp", "RptNum", "weekDays", "hours", "minutes", "seconds", "schExeDate", "schExeTime")));
    // A change of preset selects the stored fields it writes, without saving anything
    Element sync = byAttribute(maintain.getDocumentElement(), "target", "name", "SyncTaskEditorSchedule");
    assertEquals("scheduler.read", sync.getAttribute("permission"));
    assertEquals(List.of("getTaskEditorScheduleFields"), children(sync, "serve").stream()
      .map(serve -> serve.getAttribute("service")).toList());
    assertTrue(dependencyActionsOf(children(byId(screen, "criteria", "SchSyn"), "dependency").get(0))
      .contains("server:SyncTaskEditorSchedule"));
    // The preview card: the sentence as its title, the next runs as tiles, the first holiday it skips and how it is
    // saved, reloaded when a field of «When» changes
    Element preview = byId(screen, "card", "CrdTskEdtSch");
    assertEquals("card-preview", preview.getAttribute("style"));
    assertEquals(List.of("title", "tiles", "tilesLabel", "description", "details", "detailsLabel"),
      fieldsOf(byId(queries, "query", preview.getAttribute("target-action"))));
    // «Custom» goes back to the preset its fields match, without saving anything
    Element back = byAttribute(maintain.getDocumentElement(), "target", "name", "BackToTaskEditorSchedulePreset");
    assertEquals("scheduler.read", back.getAttribute("permission"));
    assertEquals(List.of("server:BackToTaskEditorSchedulePreset"), actionsOf(byId(screen, "button", "ButSchBck")));
    assertEquals("scheduler.read", byId(queries, "query", "getTaskEditorScheduleMatch").getAttribute("permission"));
    assertTrue(dependencyActionsOf(children(preview, "dependency").get(0)).contains("filter:CrdTskEdtSch"));
  }

  @Test
  void theRangeOfDatesIsOptionalAndEachTimeWaitsForItsDate() {
    for (String[] bound : new String[][]{{"IniDat", "IniTim"}, {"EndDat", "EndTim"}}) {
      Element date = byId(screen, "criteria", bound[0]);
      Element time = byId(screen, "criteria", bound[1]);
      // A task may have no start and no end: no field of the range is ever required
      for (Element field : List.of(date, time)) {
        assertFalse(field.getAttribute("validation").contains("required"), field.getAttribute("id"));
        assertTrue(children(field, "dependency").stream()
          .noneMatch(dependency -> "set-required".equals(dependency.getAttribute("target-type"))), field.getAttribute("id"));
      }
      // Its time is read only while its date is empty, and is emptied with it
      Element readonly = children(time, "dependency").stream()
        .filter(dependency -> "set-readonly".equals(dependency.getAttribute("target-type"))).findFirst()
        .orElseThrow(() -> new AssertionError(bound[1] + " waits for " + bound[0]));
      assertEquals("is empty", elementCondition(readonly, bound[0]), bound[1]);
      Element clear = children(date, "dependency").stream()
        .filter(dependency -> dependencyActionsOf(dependency).equals(List.of("value:" + bound[1]))).findFirst()
        .orElseThrow(() -> new AssertionError(bound[0] + " empties " + bound[1]));
      assertEquals("is empty", elementCondition(clear, bound[0]), bound[0]);
      assertEquals("", children(clear, "dependency-action").get(0).getAttribute("value"), bound[0]);
    }
  }

  @Test
  void theGraphsOpenTheTaskScreenOfTheirTasksAndEveryQueryOfTheEditorDeclaresItsPermission() {
    // Each graph draws one side of the task in two columns: the chosen parents, the tasks that run after it
    for (String graph : List.of("TskEdtPrnGph=parents", "TskEdtChlGph=children")) {
      Element widget = byId(screen, "widget", graph.split("=")[0]);
      assertEquals("flow-graph", widget.getAttribute("type"), graph);
      List<String> parameters = children(widget, "widget-parameter").stream()
        .map(parameter -> parameter.getAttribute("name") + "=" + parameter.getAttribute("value")).toList();
      assertEquals(List.of("mode=task", "target=scheduler-task", "criterion=" + TASK, "side=" + graph.split("=")[1]),
        parameters, graph);
    }
    Set<String> used = new HashSet<>();
    for (String tag : List.of("criteria", "grid", "card", "widget")) {
      all(screen, tag).stream().map(element -> element.getAttribute("target-action"))
        .filter(action -> action.startsWith("getTaskEditor")).forEach(used::add);
    }
    used.add("getTaskEditor");
    assertEquals(Set.of("getTaskEditor", "getTaskEditorHeader", "getTaskEditorParameters",
      "getTaskEditorParentOptions", "getTaskEditorParentGraph", "getTaskEditorChildGraph", "getTaskEditorWhenOptions",
      "getTaskEditorExecutionOptions", "getTaskEditorSchedule", "getTaskEditorWeekDayOptions"), used);
    for (String query : used) {
      String permission = byId(queries, "query", query).getAttribute("permission");
      assertTrue(Set.of("scheduler.read", "scheduler.update").contains(permission), query);
    }
    // What a task runs, its files and who gets its report, and its parameter values: for the editors only
    for (String query : List.of("getTaskEditor", "getTaskEditorHeader", "getTaskEditorParameters")) {
      assertEquals("scheduler.update", byId(queries, "query", query).getAttribute("permission"), query);
    }
  }

  private static String showCondition(Element element, String criterion, String value) {
    for (Element dependency : children(element, "dependency")) {
      if ("show".equals(dependency.getAttribute("target-type"))) {
        Element trigger = byAttribute(dependency, "dependency-element", "id", criterion);
        if (trigger != null && value.equals(trigger.getAttribute("value"))) {
          return trigger.getAttribute("condition");
        }
      }
    }
    return "";
  }

  private static String elementCondition(Element dependency, String criterion) {
    Element element = byAttribute(dependency, "dependency-element", "id", criterion);
    return element == null ? "" : element.getAttribute("condition");
  }

  /**
   * Identifiers of the criteria and grid columns of a document or a source: what a screen sends
   */
  private static Set<String> idsOf(Object root) {
    Element element = root instanceof Document document ? document.getDocumentElement() : (Element) root;
    Set<String> ids = new HashSet<>();
    all(element, "criteria").forEach(criterion -> ids.add(criterion.getAttribute("id")));
    all(element, "column").forEach(column -> ids.add(column.getAttribute("name")));
    return ids;
  }

  private static Element source(Document document, String source) {
    for (Element tag : children(document.getDocumentElement(), "tag")) {
      if (source.equals(tag.getAttribute("source"))) {
        return tag;
      }
    }
    throw new AssertionError("No source " + source);
  }

  private static List<String> variableNamesOf(Element query) {
    return children(query, "variable").stream().filter(variable -> variable.hasAttribute("name"))
      .map(variable -> variable.getAttribute("name")).toList();
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      actions.add(describe(action));
    }
    return actions;
  }

  private static List<String> dependencyActionsOf(Element dependency) {
    return children(dependency, "dependency-action").stream().map(SchedulerTaskEditorScreenDescriptorTest::describe)
      .toList();
  }

  private static String describe(Element action) {
    String type = action.getAttribute("type");
    return type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target"));
  }

  private static List<String> fieldsOf(Element query) {
    List<String> fields = new ArrayList<>();
    for (Element field : children(query, "field")) {
      fields.add(field.hasAttribute("alias") ? field.getAttribute("alias") : field.getAttribute("id"));
    }
    return fields;
  }

  private static List<Element> children(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && tag.equals(element.getTagName())) {
        list.add(element);
      }
    }
    return list;
  }

  private static List<Element> all(Object root, String tag) {
    Element parent = root instanceof Document document ? document.getDocumentElement() : (Element) root;
    List<Element> list = new ArrayList<>();
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      list.add((Element) nodes.item(index));
    }
    return list;
  }

  private static Element byClass(Document document, String style) {
    return all(document, "tag").stream().filter(tag -> List.of(tag.getAttribute("style").split(" ")).contains(style))
      .findFirst().orElseThrow(() -> new AssertionError("No tag with style " + style));
  }

  private static Element byId(Document document, String tag, String id) {
    Element element = byAttribute(document.getDocumentElement(), tag, "id", id);
    assertNotNull(element, tag + " " + id + " must exist");
    return element;
  }

  private static Element byAttribute(Element parent, String tag, String attribute, String value) {
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      Element element = (Element) nodes.item(index);
      if (value.equals(element.getAttribute(attribute))) {
        return element;
      }
    }
    return null;
  }

  private static Document parse(String path) throws Exception {
    try (InputStream stream = SchedulerTaskEditorScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
