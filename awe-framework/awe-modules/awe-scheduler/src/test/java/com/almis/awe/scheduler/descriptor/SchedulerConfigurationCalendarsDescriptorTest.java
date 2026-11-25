package com.almis.awe.scheduler.descriptor;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the Calendars tab of the scheduler configuration screen ({@code scheduler-configuration}, #993,
 * mock-up 06 artboards 2A and 2B): the calendars grid reads the calendars service with its search and active filter,
 * its «En uso por» link opens the task catalog filtered by the calendar, the row menu (the grid has no buttons)
 * activates, deactivates (with a confirmation) and deletes the calendar of the row through the configuration targets,
 * which read the calendar of the grid (IdeCal) and never a column of the servers grid. Deleting a calendar in use opens
 * the dialog with its tasks and the days they would run on; a calendar no task uses asks a simple confirmation. Every
 * column name is unique in the screen, as the schema requires: two grids with the same column would send it twice.
 */
class SchedulerConfigurationCalendarsDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String GRID = "GrdCal";

  private static Document screen;
  private static Document catalog;
  private static Document queries;
  private static Document maintain;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-configuration.xml");
    catalog = parse("screen/scheduler-task-catalog.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
  }

  @Test
  void theCalendarsGridOfTheTabReadsTheCalendarsServiceWithItsSearchAndItsActiveFilter() {
    Element grid = byId(screen, "grid", GRID);
    assertEquals("CfgCalBdy", ((Element) grid.getParentNode().getParentNode().getParentNode()).getAttribute("id"));
    assertEquals("getConfigurationCalendars", grid.getAttribute("target-action"));
    assertEquals("true", grid.getAttribute("show-maximize-button"));
    assertEquals("CrtCalSch", children(grid, "search").get(0).getAttribute("id"));
    Element quickFilter = children(grid, "quick-filter").get(0);
    assertEquals("CrtCalAct", quickFilter.getAttribute("id"));
    assertEquals("getConfigurationCalendarActiveCounts", quickFilter.getAttribute("target-action"));
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationCalendars"));
    for (Element column : children(grid, "column")) {
      String name = column.getAttribute("name");
      assertTrue("CalMorBtn".equals(name) || fields.contains(name), name + " is not a field of getConfigurationCalendars");
      // Only the calendar is sent: the other values of the row never reach a form of the screen
      assertEquals("IdeCal".equals(name), !"false".equals(column.getAttribute("sendable")), name);
    }
  }

  @Test
  void theRowsShowTheDatesTheActivePillAndTheTasksThatUseTheCalendarLinkedToTheCatalog() {
    assertEquals("CalDatSub", column("CalDatCnt").getAttribute("secondary-field"));
    assertEquals("CalActTon", column("CalActTxt").getAttribute("status-field"));
    Element usedBy = column("CalUseTxt");
    assertEquals("link", usedBy.getAttribute("component"));
    assertEquals("CalUseSub", usedBy.getAttribute("secondary-field"));
    // A selected server would filter the catalog too: its rows are unselected first (and the server link does the same
    // with the calendars)
    assertEquals(List.of("unselect-all-rows:GrdSrv", "screen:scheduler-task-catalog"), actionsOf(usedBy));
    assertEquals(List.of("unselect-all-rows:GrdCal", "screen:scheduler-task-catalog"),
      actionsOf(byAttribute(byId(screen, "grid", "GrdSrv"), "column", "name", "SrvUseTxt")));
    Element filter = byId(catalog, "criteria", "CrtTskCal");
    assertEquals("hidden", filter.getAttribute("component"));
    assertEquals("IdeCal.selected", filter.getAttribute("variable"));
    assertEquals(List.of("value:CrtTskCal=", "filter:GrdTskLst"), actionsOf(byId(catalog, "button", "ButTskCalClr")));
  }

  @Test
  void theRowMenuActivatesDeactivatesAndDeletesTheCalendarOfTheRowThroughTheConfigurationTargets() {
    Map<String, List<String>> expected = new LinkedHashMap<>();
    // «Editar» opens the side panel (SchedulerConfigurationCalendarPanelDescriptorTest)
    expected.put("CtxCalEdt", List.of("value:CalPnlMod=edit", "server:loadConfigurationCalendar", "filter:GrdDatLst",
      "filter:GrdCalUse", "dialog:PnlCal"));
    expected.put("CtxCalAct", List.of("server:ActConfigurationCalendar", "filter:GrdCal"));
    expected.put("CtxCalDea", List.of("confirm:CalDeaMsg", "server:DeaConfigurationCalendar", "filter:GrdCal"));
    expected.put("CtxCalDel", List.of("confirm:CalDelMsg", "server:DeleteConfigurationCalendar", "filter:GrdCal",
      "filter:CfgTab"));
    expected.put("CtxCalDelUse", List.of("filter:CrdCalDel", "filter:GrdCalDel", "dialog:DlgCalDel"));
    Element grid = byId(screen, "grid", GRID);
    Map<String, List<String>> actual = new LinkedHashMap<>();
    for (Element button : children(grid, "context-button")) {
      actual.put(button.getAttribute("id"), actionsOf(button));
    }
    assertEquals(expected, actual);
    assertEquals(0, grid.getElementsByTagName("button").getLength());
    // A calendar no task uses asks a simple confirmation; one in use opens the dialog with its tasks
    assertEquals("eq", usage("CtxCalDel").getAttribute("condition"));
    assertEquals("gt", usage("CtxCalDelUse").getAttribute("condition"));
    // The targets read the calendar of the grid, never Ide (a column of the servers grid)
    for (String target : List.of("ActConfigurationCalendar", "DeaConfigurationCalendar",
      "DeleteConfigurationCalendar")) {
      assertEquals("scheduler.update", byAttribute(maintain.getDocumentElement(), "target", "name", target)
        .getAttribute("permission"));
      assertEquals(Set.of("IdeCal"), variableNames(target), target);
    }
  }

  @Test
  void deletingACalendarInUseShowsItsTasksAndTheDaysTheyWouldRunOnBeforeDeletingIt() {
    Element card = byId(screen, "card", "CrdCalDel");
    assertEquals("getConfigurationCalendarDeleteSummary", card.getAttribute("target-action"));
    Element tasks = byId(screen, "grid", "GrdCalDel");
    assertEquals("getConfigurationCalendarDeleteCheck", tasks.getAttribute("target-action"));
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationCalendarDeleteCheck"));
    for (Element column : children(tasks, "column")) {
      assertTrue(fields.contains(column.getAttribute("name")), column.getAttribute("name"));
      assertEquals("false", column.getAttribute("sendable"), column.getAttribute("name"));
    }
    assertEquals(List.of("title", "description"), fieldsOf(byId(queries, "query",
      "getConfigurationCalendarDeleteSummary")));
    assertEquals(List.of("close-cancel:DlgCalDel"), actionsOf(byId(screen, "button", "ButCalDelCnl")));
    assertEquals(List.of("server:DeleteConfigurationCalendar", "close:DlgCalDel", "filter:GrdCal", "filter:CfgTab"),
      actionsOf(byId(screen, "button", "ButCalDelCnf")));
  }

  @Test
  void everyColumnNameIsUniqueInTheScreen() {
    Map<String, List<Element>> grids = new LinkedHashMap<>();
    NodeList columns = screen.getElementsByTagName("column");
    for (int index = 0; index < columns.getLength(); index++) {
      Element column = (Element) columns.item(index);
      grids.computeIfAbsent(column.getAttribute("name"), name -> new ArrayList<>())
        .add((Element) column.getParentNode());
    }
    grids.forEach((name, owners) -> {
      if (owners.size() > 1) {
        // Only grids that are never shown together may share a column: each one sends its values only while it is
        // shown (send-when-hidden="false"), and each one is shown on a tab of its own
        Set<String> tabs = new HashSet<>();
        for (Element grid : owners) {
          assertEquals("false", grid.getAttribute("send-when-hidden"), name + " is the name of two columns");
          assertTrue(tabs.add(shownOnTab(grid)), name + " is the name of two columns shown on the same tab");
        }
      }
    });
  }

  /**
   * The tab a grid is shown on: the value of the tab of its show dependency (empty when it has none)
   */
  private static String shownOnTab(Element grid) {
    for (Element dependency : children(grid, "dependency")) {
      if ("show".equals(dependency.getAttribute("target-type"))) {
        for (Element element : children(dependency, "dependency-element")) {
          if ("CfgTab".equals(element.getAttribute("id")) && "eq".equals(element.getAttribute("condition"))) {
            return element.getAttribute("value");
          }
        }
      }
    }
    return "";
  }

  /**
   * The dependency element of a delete entry of the row menu on the tasks that use the calendar
   */
  private static Element usage(String button) {
    for (Element dependency : children(byId(screen, "context-button", button), "dependency")) {
      for (Element element : children(dependency, "dependency-element")) {
        if ("CalUseCnt".equals(element.getAttribute("column"))) {
          assertEquals("0", element.getAttribute("value"), button);
          return element;
        }
      }
    }
    throw new AssertionError(button + " does not depend on CalUseCnt");
  }

  private static Set<String> variableNames(String target) {
    Element element = byAttribute(maintain.getDocumentElement(), "target", "name", target);
    assertNotNull(element, target);
    Set<String> names = new HashSet<>();
    NodeList variables = element.getElementsByTagName("variable");
    for (int index = 0; index < variables.getLength(); index++) {
      Element variable = (Element) variables.item(index);
      if (variable.hasAttribute("name")) {
        names.add(variable.getAttribute("name"));
      }
    }
    return names;
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
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target"))
        + ("value".equals(type) ? "=" + action.getAttribute("value") : ""));
    }
    return actions;
  }

  private static List<String> fieldsOf(Element query) {
    List<String> fields = new ArrayList<>();
    for (String tag : List.of("field", "computed", "compound")) {
      for (Element field : children(query, tag)) {
        fields.add(field.hasAttribute("alias") ? field.getAttribute("alias") : field.getAttribute("id"));
      }
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
    try (InputStream stream = SchedulerConfigurationCalendarsDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
