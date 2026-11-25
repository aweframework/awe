package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter;
import com.almis.awe.scheduler.enums.TaskLastStatus;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the task catalog screen ({@code scheduler-task-catalog}, #993): its grid reads the catalog
 * service, its toolbar carries the search and the launch, active and last status quick filters, only the columns the
 * service sorts are sortable, the grid fits without horizontal scroll, its row menu launches the existing launch,
 * editor, activation and delete actions, and every stored execution has the start its history is ordered by.
 */
class SchedulerTaskCatalogScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String GRID = "GrdTskLst";

  private static Document screen;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-task-catalog.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
  }

  @Test
  void theGridIsThePageOfTheCatalogServiceAndOnlyTheColumnsItSortsAreSortable() {
    Element grid = byId(screen, "grid", GRID);
    assertEquals("getTaskCatalog", grid.getAttribute("target-action"));
    Element query = byId(queries, "query", "getTaskCatalog");
    assertEquals("true", query.getAttribute("managed-pagination"));
    assertEquals(String.valueOf(TaskCatalogPresenter.DEFAULT_PAGE_SIZE), grid.getAttribute("max"));

    Map<String, String> sortable = Map.of("TskStaTxt", "TskLstDat", "TskNam", "Nam", "TskWhnTxt", "TskKnd",
      "TskAvg", "TskAvgMs", "TskActTxt", "Act");
    List<String> fields = fieldsOf(query);
    Map<String, String> sorted = new LinkedHashMap<>();
    for (Element column : children(grid, "column")) {
      String name = column.getAttribute("name");
      // The «⋯» button reads no value of the row (sendable="false"): it opens the row menu
      if ("false".equals(column.getAttribute("sendable"))) {
        continue;
      }
      assertTrue(fields.contains(name), name + " is not a field of getTaskCatalog");
      if ("true".equals(column.getAttribute("sortable"))) {
        sorted.put(name, column.getAttribute("sort-field"));
      } else if (!"true".equals(column.getAttribute("hidden"))) {
        assertEquals("false", column.getAttribute("sortable"), name + " must not be sortable");
      }
    }
    assertEquals(sortable, sorted);
    for (String sortId : sortable.values()) {
      assertTrue(fields.contains(sortId), sortId + " is not a field of getTaskCatalog");
      assertTrue(TaskCatalogPresenter.SORT_FIELDS.containsKey(sortId), sortId + " is not sorted by the service");
    }
  }

  @Test
  void theToolbarCarriesTheSearchAndTheLaunchActiveAndLastStatusQuickFilters() {
    Element grid = byId(screen, "grid", GRID);
    List<String> variables = variablesOf("getTaskCatalog");
    List<String> counted = variablesOf("getTaskCatalogActiveCounts");

    assertEquals("CrtTskSch", children(grid, "search").get(0).getAttribute("id"));
    Map<String, String> targets = new LinkedHashMap<>();
    Map<String, String> displays = new LinkedHashMap<>();
    for (Element quickFilter : children(grid, "quick-filter")) {
      String id = quickFilter.getAttribute("id");
      targets.put(id, quickFilter.getAttribute("target-action"));
      displays.put(id, quickFilter.getAttribute("display"));
      assertEquals("query", quickFilter.getAttribute("initial-load"), id);
      assertEquals("", quickFilter.getAttribute("value"), id);
    }
    assertEquals(List.of("CrtTskLch", "CrtTskAct", "CrtTskSta"), List.copyOf(targets.keySet()));
    assertEquals(List.of("getTaskCatalogLaunchTypes", "getTaskCatalogActiveCounts", "getTaskCatalogLastStatuses"),
      List.copyOf(targets.values()));
    assertEquals(List.of("dropdown", "", "dropdown"), List.copyOf(displays.values()));
    for (String criterion : List.of("CrtTskSch", "CrtTskLch", "CrtTskAct", "CrtTskSta", "page", "max", "sort")) {
      assertTrue(variables.contains(criterion), criterion + " is not read by the catalog");
    }
    // The active counts are counted with the other filters (also the server and the calendar of the links of the
    // configuration screen), not with their own
    assertEquals(List.of("CrtTskSch", "CrtTskLch", "CrtTskSta", "CrtTskSrv", "CrtTskCal"), counted);
  }

  @Test
  void theOptionsOfTheLaunchAndLastStatusFiltersAreTheOnesTheServiceAccepts() {
    assertEquals(Stream.concat(Stream.of(""), Arrays.stream(TaskLaunchKind.values()).map(TaskLaunchKind::getCode))
      .toList(), optionsOf("getTaskCatalogLaunchTypes"));
    assertEquals(Stream.concat(Stream.of(""), Arrays.stream(TaskLastStatus.values()).map(TaskLastStatus::getCode))
      .toList(), optionsOf("getTaskCatalogLastStatuses"));
  }

  @Test
  void theGridFitsWithoutHorizontalScrollWithTheTaskAsTheFillColumn() {
    // Width of a column in the React engine: (charlength + 4) * 8 px; about 1240 px are left to the grid at 1600 px.
    // The task takes the rest, never less than its min-width, and «When» and the next execution wrap in two lines
    Element task = column("TskNam");
    int width = Integer.parseInt(task.getAttribute("min-width"));
    for (Element column : children(byId(screen, "grid", GRID), "column")) {
      if (!"true".equals(column.getAttribute("hidden")) && column != task) {
        width += (Integer.parseInt(column.getAttribute("charlength")) + 4) * 8;
      }
    }
    assertTrue(width <= 1240, "the visible columns need " + width + " px");
    assertEquals("*", task.getAttribute("width"));
    assertEquals("2", column("TskWhnTxt").getAttribute("max-lines"));
    assertEquals("2", column("TskNxtTxt").getAttribute("max-lines"));
  }

  @Test
  void theCellsAreStatusPillsTheWhenSummaryTheHistoryStripAndTheTaskLink() {
    assertColumn("TskStaTxt", "", "TskStaTon", "TskStaIcn", "TskStaSub");
    assertColumn("TskWhnTxt", "", "", "TskWhnIcn", "TskWhnSub");
    assertColumn("TskNxtTxt", "", "", "", "TskNxtSub");
    assertColumn("TskActTxt", "", "TskActTon", "TskActIcn", "");
    assertColumn("TskHis", "status-strip", "", "", "");
    Element task = column("TskNam");
    assertEquals("link", task.getAttribute("component"));
    assertEquals("Des", task.getAttribute("secondary-field"));
    assertEquals(List.of("screen:scheduler-task"), actionsOf(task));
  }

  @Test
  void theHeaderOpensTheNewTaskEditorAndTheHelp() {
    Element create = byId(screen, "button", "ButNewTsk");
    // The task editor in new mode: the mode travels in the hidden criterion the editor reads
    assertEquals(List.of("value:TskEdtMod=new", "screen:scheduler-task-editor"), actionsOf(create));
    assertEquals("hidden", byId(screen, "criteria", "TskEdtMod").getAttribute("component"));
    assertEquals("scheduler.update", create.getAttribute("permission"));
    assertTrue(create.getAttribute("style").contains("btn-primary"));
    assertEquals(List.of("dialog:HlpOpt"), actionsOf(byId(screen, "button", "ButHlp")));
  }

  @Test
  void theRowMenuOpensRunsEditsDuplicatesActivatesDeactivatesAndDeletesWithTheExistingActions() {
    Map<String, List<String>> expected = new LinkedHashMap<>();
    // First entry: the task screen, as the link of the task
    expected.put("CtxTskDet", List.of("screen:scheduler-task"));
    expected.put("CtxTskRun", List.of("server:LchTsk", "filter:GrdTskLst"));
    expected.put("CtxTskRunVar", List.of("filter:GrdTskVar", "dialog:task-variables"));
    // Edit and Duplicate: the task editor with the row task, in edit or copy mode
    expected.put("CtxTskEdt", List.of("value:TskEdtMod=edit", "screen:scheduler-task-editor"));
    expected.put("CtxTskDup", List.of("value:TskEdtMod=copy", "screen:scheduler-task-editor"));
    expected.put("CtxTskAct", List.of("server:ActSchTsk", "filter:GrdTskLst"));
    expected.put("CtxTskDea", List.of("server:DeaSchTsk", "filter:GrdTskLst"));
    expected.put("CtxTskDel", List.of("confirm:DelMsg", "server:DeleteSchedulerTask", "filter:GrdTskLst"));
    Map<String, List<String>> actual = new LinkedHashMap<>();
    for (Element button : children(byId(screen, "grid", GRID), "context-button")) {
      actual.put(button.getAttribute("id"), actionsOf(button));
    }
    assertEquals(expected, actual);
    // Run asks for the values of the parameters of a task that has them (the launch dialog), else it launches it
    assertEquals("ne", showCondition("CtxTskRun", "TskVar"));
    assertEquals("eq", showCondition("CtxTskRunVar", "TskVar"));
    assertEquals("ne", showCondition("CtxTskAct", "Act"));
    assertEquals("eq", showCondition("CtxTskDea", "Act"));
    assertEquals("scheduler.update", byId(screen, "context-button", "CtxTskDup").getAttribute("permission"));
    assertNotNull(byAttribute(screen.getDocumentElement(), "include", "target-screen", "scheduler-launch-variables"));
  }

  @Test
  void everyMaintainOfTheScreenAndEveryCatalogQueryDeclaresItsPermission() {
    NodeList actions = screen.getElementsByTagName("button-action");
    for (int index = 0; index < actions.getLength(); index++) {
      Element action = (Element) actions.item(index);
      if (action.getAttribute("server-action").startsWith("maintain")) {
        Element target = byAttribute(maintain.getDocumentElement(), "target", "name", action.getAttribute("target-action"));
        assertNotNull(target, action.getAttribute("target-action"));
        assertFalse(target.getAttribute("permission").isBlank(), action.getAttribute("target-action"));
      }
    }
    for (String query : List.of("getTaskCatalog", "getTaskCatalogActiveCounts", "getTaskCatalogLaunchTypes",
      "getTaskCatalogLastStatuses")) {
      assertEquals("scheduler.read", byId(queries, "query", query).getAttribute("permission"), query);
    }
    for (String query : List.of("getTaskCatalogRows", "getTaskCatalogHistory", "getTasksWithParameters")) {
      assertEquals("true", byId(queries, "query", query).getAttribute("server-only"), query);
    }
  }

  @Test
  void everyStoredExecutionHasItsStartSoTheHistoryOfTheCatalogCanOrderIt() {
    // getTaskCatalogHistory orders the executions of a task by their start (IniDat): the only way an execution is
    // stored sets it to the time of the database
    List<Element> inserts = new ArrayList<>();
    NodeList list = maintain.getElementsByTagName("insert");
    for (int index = 0; index < list.getLength(); index++) {
      Element insert = (Element) list.item(index);
      if (children(insert, "table").stream().anyMatch(table -> "AweSchExe".equals(table.getAttribute("id")))) {
        inserts.add(insert);
      }
    }
    assertEquals(1, inserts.size());
    Element start = children(inserts.get(0), "constant").stream()
      .filter(constant -> "IniDat".equals(constant.getAttribute("id"))).findFirst().orElseThrow();
    assertEquals("SYSTEM_TIMESTAMP", start.getAttribute("type"));
  }

  private static String showCondition(String button, String column) {
    Element context = byId(screen, "context-button", button);
    for (Element dependency : children(context, "dependency")) {
      if ("show".equals(dependency.getAttribute("target-type"))) {
        for (Element element : children(dependency, "dependency-element")) {
          if (column.equals(element.getAttribute("column"))) {
            return element.getAttribute("condition");
          }
        }
      }
    }
    return "";
  }

  private static void assertColumn(String name, String component, String status, String icon, String secondary) {
    Element column = column(name);
    assertEquals(component, column.getAttribute("component"), name);
    assertEquals(status, column.getAttribute("status-field"), name);
    assertEquals(icon, column.getAttribute("icon-field"), name);
    assertEquals(secondary, column.getAttribute("secondary-field"), name);
  }

  private static List<String> optionsOf(String query) {
    Element group = byId(enumerated, "group", byId(queries, "query", query).getAttribute("enumerated"));
    return children(group, "option").stream().map(option -> option.getAttribute("value")).toList();
  }

  private static List<String> variablesOf(String query) {
    return children(byId(queries, "query", query), "variable").stream().map(variable -> variable.getAttribute("name"))
      .toList();
  }

  private static Element column(String name) {
    Element column = byAttribute(byId(screen, "grid", GRID), "column", "name", name);
    assertNotNull(column, name);
    return column;
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      // A value action also tells the value it sets (the mode of the task editor, its section)
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target"))
        + ("value".equals(type) ? "=" + action.getAttribute("value") : ""));
    }
    return actions;
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
    try (InputStream stream = SchedulerTaskCatalogScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
