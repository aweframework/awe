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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Structural checks on the manual launch dialog ({@code scheduler-launch-variables}, #993): its grid lists the
 * parameters of the selected task with their type and origin and sends only their names and values, it is loaded by the
 * screens that open it (never on its own, which would read every task), Reset reloads the stored values, Run validates
 * the values before launching with the launch permission, and the note reads who launches it.
 */
class SchedulerLaunchDialogDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String GRID = "GrdTskVar";

  private static Document dialog;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;

  @BeforeAll
  static void load() throws Exception {
    dialog = parse("screen/scheduler-launch-variables.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
  }

  @Test
  void theGridListsTheParametersWithTheirTypeAndOriginAndSendsOnlyTheirNamesAndValues() {
    Element grid = byId(dialog, "grid", GRID);
    assertEquals("taskVariableParameters", grid.getAttribute("target-action"));
    assertEquals("true", grid.getAttribute("send-all"));
    assertEquals("", grid.getAttribute("initial-load"));

    List<String> sent = new ArrayList<>();
    for (Element column : children(grid, "column")) {
      if (!"false".equals(column.getAttribute("sendable"))) {
        sent.add(column.getAttribute("name"));
      }
    }
    assertEquals(List.of("name", "value"), sent);
    assertEquals("text", column(grid, "value").getAttribute("component"));
    assertEquals("typeLabel", column(grid, "name").getAttribute("secondary-field"));
    assertEquals("sourceTone", column(grid, "sourceLabel").getAttribute("status-field"));
    assertEquals("sourceIcon", column(grid, "sourceLabel").getAttribute("icon-field"));

    Element query = byId(queries, "query", "taskVariableParameters");
    assertEquals("scheduler.read", query.getAttribute("permission"));
    for (String alias : List.of("typeLabel", "sourceIcon", "sourceTone", "sourceLabel")) {
      assertNotNull(byAttribute(query, "computed", "alias", alias), alias);
    }
    // One icon and one tone per source of the parameters
    for (String group : List.of("SchParSrcIcn", "SchParSrcTon")) {
      assertEquals(List.of("0", "1", "2"), children(byId(enumerated, "group", group), "option").stream()
        .map(option -> option.getAttribute("value")).toList(), group);
    }
  }

  @Test
  void resetReloadsTheStoredValuesAndRunValidatesThemBeforeLaunching() {
    assertEquals(List.of("filter:GrdTskVar"), actionsOf(byId(dialog, "button", "ButRstTskVar")));
    assertEquals(List.of("validate:task-variables", "check-records-saved:GrdTskVar", "server:VldTskPar",
        "server:LchTskVar", "close:task-variables", "filter:GrdTskLst", "filter:GrdTskExe"),
      actionsOf(byId(dialog, "button", "ButLchTskVar")));
    assertEquals(List.of("close:task-variables"), actionsOf(byId(dialog, "button", "ButCanTskVar")));

    for (String target : List.of("VldTskPar", "LchTskVar")) {
      Element maintainTarget = byAttribute(maintain.getDocumentElement(), "target", "name", target);
      assertNotNull(maintainTarget, target);
      assertEquals("scheduler.execute", maintainTarget.getAttribute("permission"), target);
    }
  }

  @Test
  void theNoteReadsTheUserOfTheSession() {
    Element note = byId(dialog, "card", "CrdTskLchNot");
    assertEquals("getTaskLaunchNote", note.getAttribute("target-action"));
    Element query = byId(queries, "query", "getTaskLaunchNote");
    assertEquals("scheduler.read", query.getAttribute("permission"));
    Element user = byAttribute(query, "variable", "id", "user");
    assertNotNull(user);
    assertEquals("user", user.getAttribute("session"));
  }

  @Test
  void everyScreenThatOpensTheDialogLoadsTheParametersOfItsTask() throws Exception {
    for (String[] opener : List.of(new String[]{"scheduler-task.xml", "button", "ButTskRunVar"},
      new String[]{"scheduler-task-catalog.xml", "context-button", "CtxTskRunVar"})) {
      Element element = byId(parse("screen/" + opener[0]), opener[1], opener[2]);
      assertEquals(List.of("filter:GrdTskVar", "dialog:task-variables"), actionsOf(element), opener[0]);
    }
  }

  private static Element column(Element grid, String name) {
    Element column = byAttribute(grid, "column", "name", name);
    assertNotNull(column, name);
    return column;
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target")));
    }
    return actions;
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
    try (InputStream stream = SchedulerLaunchDialogDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
