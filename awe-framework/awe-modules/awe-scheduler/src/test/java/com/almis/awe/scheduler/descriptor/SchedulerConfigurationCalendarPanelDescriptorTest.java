package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.dao.task.SchedulerConfigurationPresenter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the calendar side panel of the scheduler configuration screen ({@code scheduler-configuration},
 * #993, mock-up 06 artboard 2A): «Nuevo calendario» (screen header, only on the calendars tab), «Editar» (first entry of
 * the row menu) and the name of a calendar open it; its criteria have names of their own (the servers tab has Nom and
 * Act) and are the parameters of the new configuration targets, which read the calendar of the selected row
 * (IdeCal.selected); the dates are an editable grid that sends its changed rows, with «Añadir fecha», «Quitar» on each
 * row, «Importar…» and the month view of its rows; «En uso por» lists the tasks with the days they will not run on;
 * the footer deletes the calendar through the same flow as the row menu, cancels or saves.
 */
class SchedulerConfigurationCalendarPanelDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String PANEL = "PnlCal";
  private static final String LOAD = "loadConfigurationCalendar";
  private static final List<String> EDIT_ACTIONS = List.of("value:CalPnlMod=edit", "server:" + LOAD,
    "filter:GrdDatLst", "filter:GrdCalUse", "dialog:" + PANEL);

  private static Document screen;
  private static Document queries;
  private static Document maintain;
  private static Document services;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-configuration.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    services = parse("global/Services.xml");
  }

  @Test
  void newCalendarOpensThePanelWithNoSelectedCalendarOnlyFromTheCalendarsTab() {
    Element button = byId(screen, "button", "ButCalNew");
    assertEquals("buttons", ((Element) button.getParentNode()).getAttribute("source"));
    assertEquals("scheduler.update", button.getAttribute("permission"));
    assertEquals(List.of("unselect-all-rows:GrdCal", "value:CalPnlMod=new", "server:" + LOAD, "filter:GrdDatLst",
      "filter:GrdCalUse", "dialog:" + PANEL), actionsOf(button));
    assertEquals(List.of("show:CfgTab eq CfgCal"), dependenciesOf(button));
  }

  @Test
  void editIsTheFirstEntryOfTheRowMenuAndTheNameOfACalendarOpensItToo() {
    Element grid = byId(screen, "grid", "GrdCal");
    Element first = children(grid, "context-button").get(0);
    assertEquals("CtxCalEdt", first.getAttribute("id"));
    assertEquals(EDIT_ACTIONS, actionsOf(first));
    Element name = byAttribute(grid, "column", "name", "CalNom");
    assertEquals("link", name.getAttribute("component"));
    assertEquals(EDIT_ACTIONS, actionsOf(name));
    // The loader reads the selected calendar only, silently, and fills every field of the panel
    Element loader = target(LOAD);
    assertEquals("scheduler.read", loader.getAttribute("permission"));
    assertEquals(Set.of("IdeCal.selected"), variableNames(LOAD));
    for (String field : SchedulerConfigurationPresenter.CALENDAR_PANEL_FIELDS) {
      assertNotNull(byAttribute(panel(), "criteria", "id", field), field);
    }
  }

  @Test
  void thePanelShowsTheCalendarOfTheRowInItsHeader() {
    Element panel = panel();
    // Wider than the servers panel (40rem): the dates toolbar, the grid and the month view need the room
    assertEquals("54rem", panel.getAttribute("max-width"));
    assertEquals("40rem", byId(screen, "side-panel", "PnlSrv").getAttribute("max-width"));
    assertEquals("GrdCal", panel.getAttribute("header-grid"));
    assertEquals("CalNom", panel.getAttribute("header-title-field"));
    assertEquals("CalPnlSub", panel.getAttribute("header-subtitle-field"));
    assertEquals("CalPnlIcn", panel.getAttribute("header-avatar-icon-field"));
    assertEquals("SCREEN_TEXT_SCHEDULER_CALENDAR_NEW", panel.getAttribute("label"));
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationCalendars"));
    assertTrue(fields.containsAll(List.of("CalPnlSub", "CalPnlIcn")), fields.toString());
  }

  @Test
  void theCriteriaOfThePanelAreTheParametersOfTheTargetsThatSaveIt() {
    Set<String> gridColumns = Set.of("IdeDat", "Dat", "Nam");
    for (String name : List.of("NewConfigurationCalendar", "UpdConfigurationCalendar")) {
      assertEquals("scheduler.update", target(name).getAttribute("permission"), name);
      for (String parameter : variableNames(name)) {
        if (!gridColumns.contains(parameter) && !parameter.startsWith("IdeCal") && !"CalPnlNewIde".equals(parameter)) {
          assertNotNull(byAttribute(panel(), "criteria", "id", parameter), name + " reads " + parameter);
        }
      }
      assertEquals("GrdDatLst", children(target(name), "multiple").get(0).getAttribute("grid"), name);
    }
    // An edited calendar is the one of the selected row; a new one takes its id from the sequence, never from the screen
    assertTrue(variableNames("UpdConfigurationCalendar").contains("IdeCal.selected"));
    assertFalse(variableNames("NewConfigurationCalendar").stream().anyMatch(name -> name.startsWith("IdeCal")));
    // Saving reloads the calendar in the engine and reschedules its tasks with it or without it
    assertEquals("updateConfigurationCalendar", children(target("UpdConfigurationCalendar"), "serve").get(0)
      .getAttribute("service"));
    assertNotNull(byAttribute(services.getDocumentElement(), "service", "id", "updateConfigurationCalendar"));
    assertEquals(List.of("validate:PnlCalBdy", "server:NewConfigurationCalendar", "close:" + PANEL, "filter:GrdCal",
      "filter:CfgTab"), actionsOf(byId(screen, "button", "ButCalPnlNew")));
    assertEquals(List.of("check-one-selected:GrdCal", "validate:PnlCalBdy", "server:UpdConfigurationCalendar",
      "close:" + PANEL, "filter:GrdCal", "filter:CfgTab"), actionsOf(byId(screen, "button", "ButCalPnlUpd")));
    assertEquals(List.of("close-cancel:" + PANEL), actionsOf(byId(screen, "button", "ButCalPnlCnl")));
    for (String id : List.of("ButCalPnlNew", "ButCalPnlUpd")) {
      assertEquals("scheduler.update", byId(screen, "button", id).getAttribute("permission"), id);
    }
  }

  @Test
  void theDatesAreAnEditableGridThatSendsItsChangedRowsWithItsMonthView() {
    Element grid = byId(screen, "grid", "GrdDatLst");
    assertEquals("true", grid.getAttribute("editable"));
    assertEquals("true", grid.getAttribute("send-operations"));
    assertEquals("getConfigurationCalendarPanelDates", grid.getAttribute("target-action"));
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationCalendarPanelDates"));
    for (Element column : children(grid, "column")) {
      String name = column.getAttribute("name");
      assertTrue("DatDel".equals(name) || fields.contains(name), name);
    }
    assertEquals("required", byAttribute(grid, "column", "name", "Dat").getAttribute("validation"));
    assertEquals("required", byAttribute(grid, "column", "name", "Nam").getAttribute("validation"));
    assertEquals("false", byAttribute(grid, "column", "name", "DatWek").getAttribute("sendable"));
    assertEquals(List.of("delete-row:GrdDatLst"), actionsOf(byAttribute(grid, "column", "name", "DatDel")));
    assertEquals(List.of("add-row:GrdDatLst"), actionsOf(byId(screen, "button", "ButCalDatAdd")));
    // «Importar…»: the uploaded file goes to the importer, which adds its dates as new rows of the grid
    Element uploader = byId(screen, "criteria", "CalDatImp");
    assertEquals("uploader", uploader.getAttribute("component"));
    Element importer = (Element) uploader.getElementsByTagName("dependency-action").item(0);
    assertEquals("ImportConfigurationCalendarDates", importer.getAttribute("target-action"));
    assertEquals(Set.of("CalDatImp"), variableNames("ImportConfigurationCalendarDates"));
    // The uploader gets the room to show the chosen file; the toolbar row fills the 12 columns
    assertEquals("col-xs-12 col-sm-2", byId(screen, "criteria", "CalPnlCnt").getAttribute("style"));
    assertEquals("col-xs-8 col-sm-7", uploader.getAttribute("style"));
    assertEquals("col-xs-4 col-sm-3", ((Element) byId(screen, "button", "ButCalDatAdd").getParentNode())
      .getAttribute("style"));
    // The month view draws the rows of the grid, the ones not saved yet included
    Element month = byId(screen, "widget", "CalMth");
    assertEquals("month-view", month.getAttribute("type"));
    Map<String, String> parameters = new HashMap<>();
    for (Element parameter : children(month, "widget-parameter")) {
      parameters.put(parameter.getAttribute("name"), parameter.getAttribute("value"));
    }
    assertEquals(Map.of("grid", "GrdDatLst", "dateColumn", "Dat", "nameColumn", "Nam", "legend", "MONTH_VIEW_LEGEND"),
      parameters);
  }

  @Test
  void theDayOfANewOrEditedDateFollowsItsDate() {
    Element grid = byId(screen, "grid", "GrdDatLst");
    Element date = byAttribute(grid, "column", "name", "Dat");
    List<Element> dependencies = children(date, "dependency");
    assertEquals(1, dependencies.size());
    Element dependency = dependencies.get(0);
    // A server query whose answer fills the day of the row (an update-row action), so no target on the cell
    assertEquals("query", dependency.getAttribute("source-type"));
    assertEquals("none", dependency.getAttribute("target-type"));
    assertEquals("maintain-silent", dependency.getAttribute("server-action"));
    assertEquals("getConfigurationCalendarDateWeekday", dependency.getAttribute("target-action"));
    assertEquals("true", dependency.getAttribute("silent"));
    assertFalse(dependency.hasAttribute("initial"));
    // The date and the id of the edited row, by the aliases the target reads
    Map<String, String> elements = new HashMap<>();
    for (Element element : children(dependency, "dependency-element")) {
      assertEquals("GrdDatLst", element.getAttribute("id"));
      assertEquals("selectedRowValue", element.getAttribute("attribute"));
      elements.put(element.getAttribute("alias"), element.getAttribute("column"));
    }
    assertEquals(Map.of("CalDatWekDat", "Dat", "CalDatWekRow", "id"), elements);
    Element target = target("getConfigurationCalendarDateWeekday");
    assertEquals("scheduler.read", target.getAttribute("permission"));
    assertEquals(Set.of("CalDatWekDat", "CalDatWekRow"), variableNames("getConfigurationCalendarDateWeekday"));
    String service = children(target, "serve").get(0).getAttribute("service");
    Element java = byAttribute(services.getDocumentElement(), "service", "id", service);
    assertNotNull(java, service);
    assertEquals("scheduler.read", java.getAttribute("permission"));
    assertEquals("getConfigurationCalendarDateWeekday", children(java, "java").get(0).getAttribute("method"));
  }

  @Test
  void inUseByListsTheTasksOfTheCalendarWithTheDaysTheyWillNotRunOn() {
    Element grid = byId(screen, "grid", "GrdCalUse");
    assertEquals("getConfigurationCalendarPanelTasks", grid.getAttribute("target-action"));
    Element query = byId(queries, "query", "getConfigurationCalendarPanelTasks");
    assertEquals("getConfigurationCalendarDeleteCheck", query.getAttribute("service"));
    assertEquals("IdeCal.selected", children(query, "variable").get(0).getAttribute("name"));
    List<String> fields = fieldsOf(query);
    for (Element column : children(grid, "column")) {
      assertTrue(fields.contains(column.getAttribute("name")), column.getAttribute("name"));
      assertEquals("false", column.getAttribute("sendable"), column.getAttribute("name"));
    }
    Element days = byAttribute(grid, "column", "name", "CalTskSkp");
    assertEquals("CalTskSkpTon", days.getAttribute("status-field"));
    assertEquals("CalTskSkpIcn", days.getAttribute("icon-field"));
  }

  @Test
  void deleteInTheFooterIsTheDeleteFlowOfTheRowMenu() {
    assertEquals(List.of("confirm:CalDelMsg", "server:DeleteConfigurationCalendar", "close:" + PANEL, "filter:GrdCal",
      "filter:CfgTab"), actionsOf(byId(screen, "button", "ButCalPnlDel")));
    assertEquals(List.of("close:" + PANEL, "filter:CrdCalDel", "filter:GrdCalDel", "dialog:DlgCalDel"),
      actionsOf(byId(screen, "button", "ButCalPnlDelUse")));
    assertEquals(List.of("show:CalPnlMod eq edit", "show:GrdCal eq 0"), dependenciesOf(byId(screen, "button",
      "ButCalPnlDel")));
    assertEquals(List.of("show:CalPnlMod eq edit", "show:GrdCal gt 0"), dependenciesOf(byId(screen, "button",
      "ButCalPnlDelUse")));
  }

  private static Element panel() {
    return byId(screen, "side-panel", PANEL);
  }

  private static Element target(String name) {
    Element target = byAttribute(maintain.getDocumentElement(), "target", "name", name);
    assertNotNull(target, name);
    return target;
  }

  private static Set<String> variableNames(String target) {
    Set<String> names = new java.util.HashSet<>();
    NodeList variables = target(target).getElementsByTagName("variable");
    for (int index = 0; index < variables.getLength(); index++) {
      Element variable = (Element) variables.item(index);
      if (variable.hasAttribute("name")) {
        names.add(variable.getAttribute("name"));
      }
    }
    return names;
  }

  /**
   * Dependencies as «target-type:element condition value», one per dependency element
   */
  private static List<String> dependenciesOf(Element element) {
    List<String> dependencies = new ArrayList<>();
    for (Element dependency : children(element, "dependency")) {
      for (Element item : children(dependency, "dependency-element")) {
        dependencies.add(dependency.getAttribute("target-type") + ":" + item.getAttribute("id") + " "
          + item.getAttribute("condition") + " " + item.getAttribute("value"));
      }
    }
    return dependencies;
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
    try (InputStream stream = SchedulerConfigurationCalendarPanelDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
